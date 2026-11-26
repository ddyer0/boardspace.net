#!/bin/sh
# Game cache compatibility test between the C server (/tmp/cmigs) and the Java server.
#  1. each server saves the same games into its own cache directory
#  2. the cache files are compared
#  3. each server is restarted on the OTHER server's cache and the games are fetched
B=/tmp/mt
T="$(dirname "$0")"
JAR=/home/claude/jmigs/migs.jar

stop() { pkill -f "cmigs[_a-z]* $B" ; pkill -f "migs.jar $B"; sleep 1; }
startc() { ( cd $B/c; for i in $(seq 1 40); do /tmp/cmigs $B/c/test.conf > $B/c/out.txt 2>&1 && break; grep -q "Failed bind" $B/c/out.txt || break; sleep 3; done ) &
           for i in $(seq 1 60); do grep -q "main process started" $B/c/main.log 2>/dev/null && break; sleep 2; done; }
startj() { ( cd $B/j; setsid java -jar $JAR $B/j/test.conf > $B/j/out.txt 2>&1 & ); sleep 2; }

stop
rm -rf $B/c $B/j
"$T/mkconf.sh" $B/c 3001 3002
"$T/mkconf.sh" $B/j 4001 4002
startc; startj

python3 - <<'EOF'
import sys, time
sys.path.insert(0, '/home/claude/jmigs/tools')
import compare
for port in (3001, 4001):
    R = compare.Run('127.0.0.1', port, port + 1)
    R.register('ivy', 19)
    lobby = R.login('L', 'ivy', 19)
    lobby.send('332 7 pw7'); R.settle()
    c = R.login('C', 'ivy', 19, session=7, pw='pw7')
    c.send('322 CacheGame-One first game, with \\001 control and \\200 high bytes \\\\ backslash'); R.settle()
    stored = ' second'
    c.send('322 cachegame-two' + stored); R.settle()
    c.send('336 cachegame-two %d %d  appended' % (len(stored), compare.hashn(stored, len(stored)))); R.settle()
    c.send('322 temp-game to be removed'); R.settle()
    c.send('324 temp-game'); R.settle()
    print(port, c.lines)
    time.sleep(4)		# dirty games move to the write pool once a second, and the save thread writes once a second
    for x in R.clients: x.close()
EOF
stop
echo "== cache files"
ls -la $B/c/cache $B/j/cache
python3 - <<'EOF'
import os, struct
B = '/tmp/mt'
def parse(path):
    d = open(path, 'rb').read()
    magic, = struct.unpack_from('<I', d, 0)
    ident = d[4:68].split(b'\0')[0]
    days, = struct.unpack_from('<q', d, 72)
    game = d[80:].split(b'\0')[0]
    return (hex(magic), ident, days, game)	# not the length: C writes the whole buffer, Java only the game
names = sorted(set(os.listdir(B + '/c/cache')) | set(os.listdir(B + '/j/cache')))
ok = True
for n in names:
    pc, pj = B + '/c/cache/' + n, B + '/j/cache/' + n
    if not (os.path.exists(pc) and os.path.exists(pj)):
        print('MISSING', n, os.path.exists(pc), os.path.exists(pj)); ok = False; continue
    a, b = parse(pc), parse(pj)
    same_hdr = open(pc, 'rb').read()[:80] == open(pj, 'rb').read()[:80]
    print(n, 'same' if a == b else 'DIFFERENT', 'header-bytes-identical' if same_hdr else 'header-bytes-differ')
    if a != b:
        print('   C:', a); print('   J:', b); ok = False
print('cache files', 'MATCH' if ok else 'MISMATCH')
EOF
echo "== swap caches and fetch"
mv $B/c/cache $B/swap; mv $B/j/cache $B/c/cache; mv $B/swap $B/j/cache
rm -f $B/c/main.log
startc; startj
python3 - <<'EOF'
import sys
sys.path.insert(0, '/home/claude/jmigs/tools')
import compare
res = {}
for port in (3001, 4001):
    R = compare.Run('127.0.0.1', port, port + 1)
    R.register('ivy', 19)
    lobby = R.login('L', 'ivy', 19)
    lobby.send('332 7 pw7'); R.settle()
    c = R.login('C', 'ivy', 19, session=7, pw='pw7')
    for g in ('cachegame-one', 'CACHEGAME-TWO', 'temp-game'):
        c.send('344 ' + g); R.settle()
    res[port] = [l for l in c.lines if l.startswith('345')]
    for x in R.clients: x.close()
print('C server reading the Java cache:'); print('  ' + '\n  '.join(res[3001]))
print('Java server reading the C cache:'); print('  ' + '\n  '.join(res[4001]))
strip = lambda ls: [' '.join(l.split(' ')[2:]) for l in ls]
print('fetched games', 'MATCH' if strip(res[3001]) == strip(res[4001]) else 'MISMATCH')
EOF
grep -h "games reloaded" $B/c/main.log $B/j/main.log
stop
