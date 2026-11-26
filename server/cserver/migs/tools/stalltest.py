#!/usr/bin/env python3
"""
Stall / load test: one lobby client stops reading while another floods the lobby
with chat, so the server's output to the stalled client backs up.  Meanwhile the
test checks that the server still accepts new connections and answers promptly.
Then 60 clients log in and chat at once.

    stalltest.py <port>
"""
import socket
import sys
import time

sys.path.insert(0, __import__('os').path.dirname(__file__))
import compare


def timed_login(R, name, user, uid):
    t0 = time.time()
    c = R.login(name, user, uid)
    return c, time.time() - t0


def main():
    port = int(sys.argv[1])
    R = compare.Run('127.0.0.1', port, port + 1)
    R.register('stall', 30)
    R.register('flood', 31)
    stalled = R.login('S', 'stall', 30)
    stalled.s.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, 4096)
    R.clients.remove(stalled)		# never read from it again
    flood = R.login('F', 'flood', 31)
    big = 'z' * 4000
    t0 = time.time()
    for i in range(1500):		# ~6MB of chat echoed to the stalled client
        flood.send('210 xchat %d %s' % (i, big))
        if i % 100 == 0:
            flood.pump(0.01)
            flood.take()
    flood.pump(0.5)
    print('flooded 6MB in %.1fs, flood client still connected: %s' % (time.time() - t0, not flood.closed))
    worst = 0
    for i in range(5):
        R.register('probe%d' % i, 40 + i)
        c, dt = timed_login(R, 'P%d' % i, 'probe%d' % i, 40 + i)
        worst = max(worst, dt)
        ok = c.usernum is not None
        print('new login %d during stall: %s in %.2fs' % (i, 'ok' if ok else 'FAILED', dt))
        c.send('302 ')
        c.pump(1.0)
        print('   ping answered:', any(l.startswith('303') for l in c.lines))
    # load: 60 clients at once
    clients = []
    t0 = time.time()
    for i in range(60):
        R.register('load%d' % i, 100 + i)
    for i in range(60):
        c = compare.Client('L%d' % i, '127.0.0.1', port)
        c.send('200 0 load%d#%d 1.2.3.4 <none> cookieL%d N' % (i, 100 + i, i))
        clients.append(c)
    for c in clients:
        c.pump(1.0)
    logged = sum(1 for c in clients if any(l.startswith('201') for l in c.lines))
    for c in clients:
        c.send('210 xchat load message')
    time.sleep(1.0)
    got = 0
    for c in clients:
        c.pump(0.2)
        got += sum(1 for l in c.lines if l.startswith('213'))
    print('load: %d/60 logged in, %d chat deliveries in %.1fs' % (logged, got, time.time() - t0))
    stalled.close()
    for c in clients + R.clients:
        c.close()


if __name__ == '__main__':
    main()
