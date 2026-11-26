#!/usr/bin/env python3
"""
Drive two migs servers (e.g. the C server and the Java server) through the same
scripted client sessions and compare what they send back.

    compare.py <hostA:port:wsport> <hostB:port:wsport> [scenario ...]

Each server must be started with a config made by mkconf.sh (bindip 127.0.0.1,
password xyzzy, strict 1).  Scenarios: lobby game ws wss crypto ban.  The crypto
scenario needs servers started with obfuscation 1 and sequencenumbers 1.

The client implements the protocol's checksums, sequence numbers and the
obfuscation cipher, so encrypted sessions are compared after decryption.
Values that are random or time based (session keys, clock values) are masked
before comparison.
"""
import base64
import hashlib
import os
import re
import select
import socket
import ssl
import sys
import time

WAIT = 0.35


def i32(x):
    x &= 0xffffffff
    return x - 0x100000000 if x & 0x80000000 else x


class Rng:
    """the protocol's obfuscation generator (get_random_in / get_random_out)"""
    def __init__(self, seed, mul_w, mul_z):
        self.w = 0
        self.z = 0
        for ch in seed.encode('latin1'):
            self.w = i32(self.w * mul_w + ch)
        for ch in seed.encode('latin1'):
            self.z = i32(self.z * mul_z + ch)

    def next(self):
        self.z = i32(36969 * (self.z & 65535) + (self.z >> 16))
        self.w = i32(18000 * (self.w & 65535) + (self.w >> 16))
        return self.w & 0xffff


def encrypt(rng, text):
    out = []
    for c in text:
        ch = ord(c)
        if ch > 32:
            r = rng.next() & 0x3f
            ch = ((ch - 32 - 1) + r) % 95 + 32 + 1
        out.append(chr(ch))
    return ''.join(out)


def decrypt(rng, text):
    out = []
    for c in text:
        ch = ord(c)
        if ch > 32:
            r = rng.next() & 0x3f
            ch = ((ch - 32 - 1) + 95 - r) % 95 + 32 + 1
        out.append(chr(ch))
    return ''.join(out)


def checksum(text):
    """line checksum over unsigned bytes, as NetConn.checksumString computes it"""
    s = 0
    for i, c in enumerate(text):
        if c in '\0\r\n':
            break
        s += (ord(c) & 0xff) ^ i
    return s & 0xffff


def hashn(text, n):
    h = 5381
    b = text.encode('latin1')
    for i in range(n):
        c = b[i] if i < len(b) else 0
        h = i32(h * 33 + c)
    return h


class Client:
    def __init__(self, name, host, port, ws=False, tls=False, src='127.0.0.1', log=None, rcvbuf=0):
        self.name = name
        self.paused = False		# a stalled reader: pump does nothing
        self.log = log
        self.ws = ws
        self.buf = b''
        self.lines = []
        self.closed = False
        self.checksums = False
        self.seq = None
        self.rng_in = None		# decrypts what the server sends
        self.rng_out = None		# encrypts what we send
        self.crypto = False		# set up the cipher when the 201 arrives
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        if rcvbuf:
            s.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, rcvbuf)
        s.bind((src, 0))
        s.connect((host, port))
        if tls:
            ctx = ssl.create_default_context()
            ctx.check_hostname = False
            ctx.verify_mode = ssl.CERT_NONE
            s.settimeout(5)		# a server that never answers the handshake fails the test, not hangs it
            s = ctx.wrap_socket(s, server_hostname='localhost')
            s.settimeout(None)
        self.s = s
        if ws:
            self._handshake(host)

    def _handshake(self, host):
        key = base64.b64encode(os.urandom(16)).decode()
        req = ('GET /gameserver HTTP/1.1\r\nHost: %s\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n'
               'Sec-WebSocket-Key: %s\r\nSec-WebSocket-Version: 13\r\n\r\n' % (host, key))
        self.s.sendall(req.encode())
        resp = b''
        deadline = time.time() + 3
        while b'\r\n\r\n' not in resp and time.time() < deadline:
            d = self.s.recv(4096)
            if not d:
                break
            resp += d
        head, _, rest = resp.partition(b'\r\n\r\n')
        expect = base64.b64encode(hashlib.sha1((key + '258EAFA5-E914-47DA-95CA-C5AB0DC85B11').encode()).digest()).decode()
        self.handshake_ok = (b'101' in head.split(b'\r\n')[0] and ('Sec-WebSocket-Accept: ' + expect).encode() in head)
        self.frames = rest

    def _raw_send(self, data, split=False):
        if self.ws:
            payload = data.encode('latin1')
            mask = os.urandom(4)
            n = len(payload)
            hdr = bytes([0x81]) + (bytes([0x80 | n]) if n < 126 else bytes([0x80 | 126, n >> 8, n & 0xff]))
            body = bytes(b ^ mask[i % 4] for i, b in enumerate(payload))
            frame = hdr + mask + body
            if split:
                cut = max(1, len(frame) // 2)
                self.s.sendall(frame[:cut])
                time.sleep(0.15)
                self.s.sendall(frame[cut:])
            else:
                self.s.sendall(frame)
        else:
            self.s.sendall(data.encode('latin1'))

    def send(self, line, split=False):
        text = line
        if self.seq is not None:
            text = 'x%d %s' % (self.seq, text)
            self.seq += 1
        if self.rng_out:
            text = encrypt(self.rng_out, text)
        if self.checksums:
            s = checksum(text)
            pre = '500 ' + ''.join(chr(ord('A') + ((s >> sh) & 0xf)) for sh in (12, 8, 4, 0)) + ' '
            text = pre + text
        if self.log is not None:
            self.log.append(('>', self.name, line))
        self._raw_send(text + '\n', split)

    def _read_available(self, timeout):
        got = False
        while True:
            pending = getattr(self.s, 'pending', None)
            if not (pending and pending() > 0):
                try:
                    r, _, _ = select.select([self.s], [], [], timeout)
                except ValueError:
                    return got
                if not r:
                    return got
            try:
                d = self.s.recv(65536)
            except (ConnectionResetError, ssl.SSLError, OSError):
                d = b''
            if not d:
                self.closed = True
                return got
            got = True
            if self.ws:
                self.frames += d
                self._decode_frames()
            else:
                self.buf += d
            timeout = 0.05

    def _decode_frames(self):
        f = self.frames
        while len(f) >= 2:
            n = f[1] & 0x7f
            h = 2
            if n == 126:
                if len(f) < 4:
                    break
                n = (f[2] << 8) | f[3]
                h = 4
            if len(f) < h + n:
                break
            self.buf += f[h:h + n]
            f = f[h + n:]
        self.frames = f

    def pump(self, timeout=WAIT):
        if self.paused:
            return self.lines
        self._read_available(timeout)
        while b'\n' in self.buf:
            line, _, self.buf = self.buf.partition(b'\n')
            text = line.decode('latin1')
            if text.startswith('501 ') and len(text) >= 9:
                claimed = sum((ord(text[4 + k]) - 65) << (12 - 4 * k) for k in range(4))
                if claimed != checksum(text[9:]):
                    text = 'CHECKSUM REJECTED BY CLIENT: ' + text[9:]
                else:
                    text = text[9:]
            if self.rng_in:
                text = decrypt(self.rng_in, text)
            elif self.crypto and SEQ.sub('', text).startswith('201 '):
                self.start_crypto(SEQ.sub('', text).split(' '))
            self.lines.append(text)
            if self.log is not None:
                self.log.append(('<', self.name, text))
        return self.lines

    def start_crypto(self, intro):
        r = [int(x) for x in intro[4].split('.')]
        nowtime = i32(int(intro[6]))
        self.rng_out = Rng('%d.%d.%d.%d.%d' % (r[0] + 1, r[1] + 2, r[2] + 3, r[3] + 4, i32(nowtime + 2)), 13, 31)
        self.rng_in = Rng('%d.%d.%d.%d.%d' % (r[0] + 3, r[1] + 6, r[2] + 9, r[3] + 12, i32(nowtime + 1)), 17, 23)

    def take(self):
        out = self.lines
        self.lines = []
        return out

    def close(self):
        try:
            self.s.close()
        except OSError:
            pass


SEQ = re.compile(r'^(x-?\d+ )')


def mask(line):
    """hide values that legitimately differ between runs"""
    m = SEQ.match(line)
    if m:
        return m.group(1) + mask(line[m.end():])
    t = line.split(' ')
    if line.startswith('201 ') and len(t) >= 7:
        t[4] = 'KEY'
        t[6] = 'TIME'
    if line.startswith('218 ') and len(t) >= 5:
        t[4] = 'KEY'
    if line.startswith('303 ') and len(t) >= 5:
        t[3] = 'SEC'
        t[4] = 'MS'
    return ' '.join(t)


class Run:
    def __init__(self, host, port, wsport, sslport=0):
        self.host, self.port, self.wsport, self.sslport = host, port, wsport, sslport
        self.log = []
        self.clients = []
        self.info = {}

    def client(self, name, ws=False, tls=False, src='127.0.0.1', ssl=False, rcvbuf=0):
        # ssl: the "sslport", TLS carrying the plain line protocol
        port = self.sslport if ssl else self.wsport if ws else self.port
        c = Client(name, self.host, port, ws=ws, tls=tls or ssl, src=src, log=self.log, rcvbuf=rcvbuf)
        self.clients.append(c)
        return c

    def settle(self, t=WAIT):
        time.sleep(t)
        for c in self.clients:
            c.pump(0.05)

    def register(self, user, uid):
        r = self.client('reg')
        r.send('200 -1 %s#%d 1.2.3.4' % (user, uid))
        r.pump()
        r.close()
        self.clients.remove(r)

    def login(self, name, user, uid, session=0, pw='<none>', bankey='N', ws=False, tls=False, ssl=False, rcvbuf=0):
        c = self.client(name, ws=ws, tls=tls, ssl=ssl, rcvbuf=rcvbuf)
        c.send('200 %d %s#%d 1.2.3.4 %s cookie%s %s' % (session, user, uid, pw, name, bankey))
        self.settle()
        intro = [l for l in c.lines if l.startswith('201 ')]
        if intro:
            t = intro[0].split(' ')
            c.usernum = int(t[2])
            c.key = t[4]
            c.intro = t
        else:
            c.usernum = None
        return c

    def transcript(self):
        """the ordered transcript.  The lobby's once-a-second "305" session
        broadcasts arrive whenever the server's clock ticks, so those are listed
        separately, sorted, at the end; 305 lines answering a 304 stay in place."""
        out = []
        unsolicited = []
        expect = {}
        for d, who, text in self.log:
            line = '%s %s %s' % (d, who, mask(text))
            if d == '>' and text.startswith('304'):
                expect[who] = 0
            if d == '<' and text.startswith('305 '):
                n = int(text.split(' ')[1])
                if expect.get(who) == n:
                    expect[who] = n + 1
                else:
                    unsolicited.append(line)
                    continue
            out.append(line)
        for c in self.clients:
            out.append('= %s closed=%s' % (c.name, c.closed))
        # broadcasts snapshot whatever state existed when the clock ticked, so
        # only the final state of each session, as each client last saw it, is compared
        last = {}
        for line in unsolicited:
            parts = line.split(' ')
            last[(parts[1], parts[3])] = line
        out.append('-- final lobby broadcasts --')
        out.extend(sorted(last.values()))
        return out


# ---------------------------------------------------------------- scenarios

def sc_lobby(R):
    R.register('alice', 11)
    R.register('bob', 12)
    a = R.login('A', 'alice', 11, bankey='S')
    b = R.login('B', 'bob', 12)
    for line in ['302 P:12,8,20,30 lobby info text', '304 ', '306 0', '204 Alice 11', '210 xchat hello everyone',
                 '230 %d xchat just to bob' % b.usernum, '330 waiting for tablut', '310 0', '310 999', '206 3',
                 '999 junk', '210 xchat xyzzy|help', '210 xchat xyzzy|logFile', '210 xchat xyzzy|strict',
                 '210 xchat xyzzy|score', '210 xchat xyzzy|unban', '210 xchat xyzzy|cached *', '210 xchat wrong|help',
                 '306 0', '334 3 1 2001', '304 ']:
        a.send(line)
        R.settle()
    b.send('210 xchat hi alice')
    R.settle()
    b.send('777 not a command')
    R.settle()
    b.send('210 xchat this is ignored now')
    R.settle()
    a.send('306 0')
    R.settle()


def sc_game(R):
    R.register('carol', 13)
    R.register('dave', 14)
    cl = R.login('CL', 'carol', 13)
    dl = R.login('DL', 'dave', 14)
    cl.send('334 5 1 2001')
    R.settle()
    cl.send('332 5 mypw')
    R.settle()
    c = R.login('C', 'carol', 13, session=5, pw='mypw')
    d = R.login('D', 'dave', 14, session=5, pw='mypw')
    key = c.key
    stored = ' the game text here'
    steps = [
        (c, '206 0'), (d, '206 1'),
        (c, '314 0 0 robot 99 0 1'),
        (c, '322 Game1' + stored),
        (c, '336 game1 %d %d ,more moves' % (len(stored), hashn(stored, len(stored)))),
        (c, '336 game1 5 12345 ,bad checksum'),
        (c, '318 5 game1'), (c, '318 5 nosuchgame'),
        (c, '344 game1'), (c, '320 game1'), (c, '344 nosuch'),
        (c, '346 '),
        (c, '322 * start ,+ T1 99 move1 ,+ T2 98 move2 \\001\\\\x'),
        (c, '340 '), (c, '346 '), (d, '340 '),
        (c, '342 1'), (d, '342 1'), (c, '342 0'), (d, '342 0'),
        (c, '338 12 210 xchat a12 210 xchat b '),
        (c, '230 %d xchat private note' % d.usernum),
        (c, '310 5'), (c, '328 statekeyA'), (d, '328 statekeyA'),
        (c, '306 5'), (cl, '306 5'), (cl, '304 '),
        (c, '326 a short note'), (c, '308 a log message'),
        (c, '220 spectate'),
        (c, '306 5'),
        (c, '220 playing 0'),
        (c, '306 5'),
        (c, '316 0 game1 the final game record'),
        (c, '316 0 game1 a second write is ignored'),
    ]
    for who, line in steps:
        who.send(line)
        R.settle()
    sc = R.client('SCORE')
    k = key.split('.')
    sc.send('218 5 13 14 %s.%s.%s.%s' % tuple(k))
    R.settle()
    sc2 = R.client('SCORE2')
    sc2.send('218 5 13 14 %s.%s.%s.%s' % tuple(k))
    R.settle()
    c.send('324 game1')
    R.settle()
    c.send('344 game1')
    R.settle()
    d.send('220 suicide')
    R.settle()
    c.close()
    R.settle(0.6)
    cl.send('306 5')
    R.settle()
    cl.send('304 ')
    R.settle()


def sc_ws(R, tls=False):
    R.register('erin', 15)
    R.register('fred', 16)
    e = R.login('E', 'erin', 15, ws=True, tls=tls)
    R.log.append(('=', 'E', 'handshake_ok=%s' % getattr(e, 'handshake_ok', None)))
    f = R.login('F', 'fred', 16)
    e.send('210 xchat hello from websocket')
    R.settle()
    e.send('210 xchat this frame arrives in two pieces', split=True)
    R.settle(0.5)
    long = 'x' * 300
    e.send('210 xchat ' + long)
    R.settle()
    f.send('210 xchat ' + 'y' * 70000)
    R.settle(0.8)
    e.send('306 0')
    R.settle()
    e.send('220 suicide')
    R.settle()


def sc_crypto(R):
    R.register('gina', 17)
    R.register('hank', 18)
    clients = []
    for name, user, uid in (('G', 'gina', 17), ('H', 'hank', 18)):
        c = R.client(name)
        c.crypto = True
        c.checksums = (name == 'G')		# real clients checksum from the first message
        c.send('200 0 %s#%d 1.2.3.4 <none> cookie%s N' % (user, uid, name))
        R.settle()
        c.seq = 1
        clients.append(c)
    g, h = clients
    for line in ['302 P:1,2,3 info', '210 xchat secret hello', '204 Gina 17', '306 0']:
        g.send(line)
        R.settle()
    h.send('210 xchat reply without checksums')
    R.settle()
    g.seq += 5
    g.send('210 xchat out of sequence')
    R.settle()


def sc_ban(R):
    x = R.client('X', src='127.0.0.2')
    x.send('GET / HTTP/1.1')
    R.settle(0.6)
    y = R.client('Y', src='127.0.0.2')
    y.send('302 ')
    R.settle(0.6)


def sc_utf8(R):
    R.register('una', 60)
    lob = R.login('L', 'una', 60)
    lob.send('332 9 pw')
    R.settle()
    c = R.client('U')
    c.checksums = True
    c.send('200 9 una#60 1.2.3.4 pw cookie N')
    R.settle()
    c.seq = 1					# current clients number every message after the 201
    # Russian "\u0440\u0441" (D1 80 D1 81), "\u00c0" (C3 80) and a DEL, escaped as NetConn sends them
    c.send('322 utf8game names: \\#00d1\\#0080\\#00d1\\#0081 \\#00c3\\#0080 del\\#007f')
    R.settle()
    c.send('344 utf8game')
    R.settle()
    c.send('210 xchat \\#00d1\\#0080')
    R.settle()


def sc_stall(R):
    # readers that stop reading, so the server's sends are partial and would-block.
    # Run with a small net.ipv4.tcp_wmem (e.g. "4096 16384 16384") or the kernel absorbs it all.
    # The volume stays small because the C output ring zaps a stalled client once it wraps.
    R.register('vic', 72)
    R.register('wes', 73)
    R.register('xia', 74)
    readers = [R.login('V', 'vic', 72, tls=True, rcvbuf=4096),
               R.login('W', 'wes', 73, ws=True, tls=True, rcvbuf=4096)]
    x = R.login('X', 'xia', 74)
    for c in readers:
        c.paused = True
    for i in range(40):
        x.send('210 xchat %03d %s' % (i, chr(ord('a') + i % 26) * 600))
        time.sleep(0.02)
        x.pump(0.01)
    time.sleep(1)
    for c in readers:
        c.paused = False
    for i in range(6):
        R.settle(0.5)
    readers[0].send('210 xchat done')
    R.settle()


def sc_stdtls(R):
    # TLS on the standard port, alongside a plain client on the same port
    R.register('una', 72)
    R.register('vic', 73)
    u = R.login('U', 'una', 72, tls=True)
    v = R.login('V', 'vic', 73)
    R.log.append(('=', 'U', 'intro level=%s tokens=%d' % (u.intro[3], len(u.intro)) if u.usernum else 'no intro'))
    u.send('210 xchat hello over tls on the standard port')
    R.settle()
    u.send('210 xchat this line arrives in two pieces', split=True)
    R.settle(0.5)
    v.send('210 xchat ' + 'z' * 70000)
    R.settle(0.8)
    u.send('306 0')
    R.settle()


SCENARIOS = {'stdtls': sc_stdtls, 'stall': sc_stall, 'lobby': sc_lobby, 'game': sc_game, 'ws': sc_ws, 'wss': lambda R: sc_ws(R, True),
             'crypto': sc_crypto, 'ban': sc_ban, 'utf8': sc_utf8}


def run(spec, name):
    p = spec.split(':')
    R = Run(p[0], int(p[1]), int(p[2]), int(p[3]) if len(p) > 3 else 0)
    SCENARIOS[name](R)
    R.settle(1.5)
    for c in R.clients:
        c.close()
    return R.transcript()


def main():
    a, b = sys.argv[1], sys.argv[2]
    names = sys.argv[3:] or ['lobby', 'game', 'ws', 'wss', 'utf8', 'stdtls', 'stall', 'ban']
    failures = 0
    for n in names:
        ta = run(a, n)
        tb = run(b, n)
        if ta == tb:
            print('%-7s same (%d lines)' % (n, len(ta)))
        else:
            failures += 1
            print('%-7s DIFFERENT' % n)
            import difflib
            for l in difflib.unified_diff(ta, tb, a, b, lineterm='', n=2):
                print('   ', l[:220])
    sys.exit(1 if failures else 0)


if __name__ == '__main__':
    main()
