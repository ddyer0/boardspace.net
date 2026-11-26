# migs — Java port of the Boardspace / Tantrix game server

A full-parity rewrite of the C `migs` server (migs.c, migs-lib.c, savegame.c,
websocket.c) in Java 16. It is a drop-in replacement:

- **Same config file.** Every key the C server reads is read the same way.
- **Same wire protocol**, including checksums, sequence numbers and the obfuscation
  cipher, and the websocket port with plain and TLS connections.
- **Same game cache files.** The C and Java servers read each other's
  `*.cachedgame` files, so you can switch back and forth.
- **Same logs**: the main, chat and security logs, with the same rotation and the
  same line formats.
- **Same status page.**

The code follows migs.c function by function and keeps the C names
(`process_send_intro`, `closeClient`, `markAsDirty`…), so the two are easy to
compare side by side:

| file | contents |
|---|---|
| `Migs0.java` | state, users and sessions, bans, game cache, output (first half of migs.c, savegame.c) |
| `Migs1.java` | message handlers, supervisor commands, dispatch (second half of migs.c) |
| `Migs.java` | config, the event loop (`main_thread`), accept, input, startup (`main`) |
| `Connection.java` | one socket: plain, websocket, websocket over TLS (websocket.c) |
| `SslProvider.java` | loads the PEM key and certificate, reloads them when they change |
| `LogStream.java` | one log (the C `fileBuffer`) |
| `C.java` | C-compatible `lsprintf`, `sscanf`, escaping and hashing, so output matches byte for byte |
| `User`, `Session`, `GameBuffer`, `BannedUser`, `RegisteredUser` | the C structs |

## Build (Eclipse)

This is a plain Eclipse Java project named `migs`, with no Maven:

- **Import:** File → Import → General → Existing Projects into Workspace, then
  select this folder. The sources are in `src/` and the class files go to `bin/`.
- **JRE:** the project uses the `JavaSE-16` execution environment. If Eclipse shows
  it as unbound, add a Java 16 (or later) JDK under Window → Preferences → Java →
  Installed JREs → Execution Environments.
- **Running from Eclipse:** `migs.launch` runs `net.boardspace.migs.Migs` and asks
  for the config file each time.
- **Building the jar:** right-click `migs.jardesc` → Create JAR. That writes a
  runnable `migs.jar` with the main class in its manifest. The path in the jardesc
  is relative to the workspace (`migs/migs.jar`). If the project lives outside the
  workspace folder, set the path in the export wizard and save the description.
- **Command line:** `tools/build.sh` (Linux) or `tools\build.bat` (Windows) builds the same `migs.jar` with plain `javac`
  (Java 16 or later).

The project has no dependencies, only the JDK.

## Run

```
java -Xmx256m -jar migs.jar /path/to/server.conf
```

The server stays in the foreground and does not fork. The `detatch` setting is read
but ignored, so run it under systemd, for example:

```
[Unit]
Description=Tantrix Game Server
After=network-online.target

[Service]
User=<user>
WorkingDirectory=/home/<user>/server
ExecStart=/usr/bin/java -Xmx256m -jar /home/<user>/server/migs.jar /home/<user>/server/tantrix.conf
Restart=on-failure
StandardOutput=append:/home/<user>/server/tantrix-stdout.log
StandardError=inherit

[Install]
WantedBy=multi-user.target
```

When systemd stops the server with SIGTERM, the buffered log entries are written out
before it exits.

## Non-blocking design

One thread runs the whole protocol, as in the C server. It waits only in
`Selector.select()`, and nothing else on that thread can block:

- **Sockets** are non-blocking channels. Output that can't be sent yet is queued per
  client. A client that stops reading is disconnected once its backlog passes 2 MB.
- **File writing happens on background threads:** the logs (a logger thread writes
  them every second), the game cache (the save thread, still rate-limited by
  `gamecacherate`), completed game records (opcode 316), the status page, and
  reloading the TLS certificate. The main thread only queues the data.
- **Startup reads files on the main thread** (config, cache directory, certificate),
  before any client is accepted.

## Configuration

The settings are the same as the C server's. Three settings are new:

| key | meaning |
|---|---|
| `listen-backlog` | optional, default 128. Connections the kernel holds for `accept()` on each port; also read by the C server |
| `websocketsslonly` | optional, default 0. With 1, plain (non-TLS) websocket connections are refused |
| `sslrequired` | optional, default 0. With 1, the standard port refuses plain connections and accepts only TLS. Connections from 127.0.0.1 (scripts on the server itself) may still be plain. Needs `websocketsslkey` / `websocketsslcert`; without them the server won't start. Also read by the C server |

**TLS on the standard port.** When `websocketsslkey` and `websocketsslcert` are set,
the standard port accepts both plain and TLS connections. The client always speaks
first, so the server looks at the first byte: `0x16` (a TLS handshake record, or
`0x80` for an old-style hello) starts TLS, anything else (a protocol line starts with
a digit) is plain. A TLS connection then carries the ordinary line protocol with no
websocket layer. The client chooses: it connects with an `SSLSocket` instead of a
plain `Socket`.

**Server level 20.** When the standard port accepts TLS, the `201` intro reports
server level 20 instead of 19; the message is otherwise unchanged. Without the key and
certificate the server reports level 19 exactly as before. (The separate `sslport` of
an earlier version is gone; that setting is ignored.)

`use-threads` and `detatch` are read but have no effect.

**Game cache:** at startup both servers check `gamecachedir` and log a plain
`UNUSUAL` line if it can't be written in (missing, not a directory, no
permission) or doesn't end with `/` (file names are made by appending to it, so
the games would land beside the directory). They also log when caching is off
because `gamecachedir` is empty or `gamecacherate` is 0. Failures to save or
delete a cached game include the reason, e.g. "(No such file or directory)".

**TLS:** `websocketsslkey` and `websocketsslcert` are the Let's Encrypt
`privkey.pem` and `fullchain.pem`, as before. Supported key formats are PKCS#8
(`BEGIN PRIVATE KEY`), PKCS#1 RSA and SEC1 EC, unencrypted.

The key and certificate are read at startup. After that the event loop never touches
them: the save-games thread, which already does the game cache's disk i/o, checks
once a day whether the files have changed and, if so, builds a new SSL context, so a
certificate renewal needs no restart. Renewed certificates overlap the old ones by
weeks, so a daily check is plenty, but it must not catch a renewal half done:

- Files changed in the last 5 minutes are left to settle, and looked at again once
  they have, rather than a day later.
- A new context is released only if it passes every check: the key and certificate
  chain load, the key matches the certificate, the certificate is within its
  validity dates, and the chain verifies against the system's trusted CAs, as a
  client would check it. A `fullchain.pem` cut short after the first certificate
  therefore fails. (If the certificate in use doesn't verify, as with a
  self-signed test certificate, the new one needn't either; a warning is logged at
  startup.)
- A rejected version is logged with the reason and checked again hourly, in case
  the files change again; meanwhile the previous certificate stays in use. If there
  was no usable certificate at startup, the files are also checked hourly.

Rebuilds are logged ("SSL key or certificate changed on disk, SSL context
rebuilt..."). Connections already open are not affected. The C server does the
same in its save-game thread (or in the main loop with `use-threads 0`, alongside
the game saving).

For testing, the Java intervals can be shortened with `-Dmigs.sslcheck`,
`-Dmigs.sslretry` and `-Dmigs.sslsettle` (milliseconds), and the C ones by building
with `-DSSL_FILE_CHECK_INTERVAL`, `-DSSL_FILE_RETRY_INTERVAL` and `-DSSL_FILE_SETTLE`
(seconds).

## Differences from the C server

Behavior a client can see is the same, except where noted below. The tests that show
it are under "Verification".

1. **Internal consistency failures don't take the server down.** Where the C server
   would abort on a failed `assert`, the Java server logs the failure with a stack
   trace and the message history, closes only the client involved, and carries on.
2. **Restarts are immediate.** The listening sockets use `SO_REUSEADDR`, so the
   server can rebind its port right away. The C server failed with "Failed bind 98"
   for up to a minute after a restart.
3. **The listen backlog is configurable** (`listen-backlog`, default 128). The C server
   had 4 compiled in; it now reads the same setting.
4. **Websocket fixes.** A frame split across TCP packets is kept rather than dropped.
   A peer that closes the TCP connection is disconnected rather than spinning. A
   requested `Sec-WebSocket-Protocol` is answered with a proper header.
5. **"220 spectate" moves the connection's buffered input and output to the new
   spectator slot.** The C server left them behind, and for a websocket client it
   also left the websocket state behind. The encryption state is still reset, as in
   C.
6. **Each pass of the event loop accepts up to 16 waiting connections** instead of 1.
7. **A few log lines show exception text** where C printed an errno value. An end of
   stream is reported to the other players as `readerr 0`.
8. **The status page's memory line** shows game buffers and JVM heap use instead of
   the C allocator counters.
9. **Cache file sizes differ.** The format is identical, but the Java server doesn't
   write the unused space after the game text, so its files are smaller. Pre-2014
   ("OldGameBuffer") files are read, and their start time is reset to today.
10. **The supervisor `crash` command** (only with `crash-test 1`) halts the JVM with
    exit code 134 instead of segfaulting.
11. **Player names are plain strings** with no fixed size, kept as received
    (escaped). Names longer than 50 characters are refused, not cut. The C server
    still has 320-byte name fields and reads names up to 640 characters.
12. **The game progress text from the ping (`302`) is kept whole.** The C server cuts
    it to `SMALLBUFSIZE`, which can split an encoded name.

Kept from the C server on purpose, quirks included: the password is logged at
startup, `reRecordGame` treats offset -1 as "replace", `userNameMatches` always
compares real names, and the `gag` echo has its arguments swapped.

## Use-after-free fixes (both servers)

Testing the C server under AddressSanitizer found reads and writes of freed memory
that a client can trigger. All were in the original code:

- a `338` multi-command whose subcommand closes the user (`220 suicide`) went on
  using the rest of the line in the freed input buffer;
- a login refused for too many connections was closed inside the send of its
  reply, and the send then read the freed message;
- a broadcast (`312`, and others) of the sender's own buffer read freed memory
  when the send back to the sender failed and closed it;
- `220 spectate` after a failed echo reused the closed user as its own new slot.

Fixes: buffers of a closed user are made unusable at once (NULL, size 0) but freed
only at the top of the main loop; a user marked for closure is closed by the main
loop, not inside a send; the multi-command and takeover handlers stop when the user
is gone; walks over a session stop if a close cleared the session. `FREE` now
poisons freed memory, so a stale use or double free fails an assert. The Java server
has no use-after-free as such (freeing is just dropping the reference), but it
mirrors the logic fixes; a remaining bug there throws, and only that client is
closed.

For testing, build the C server with `-fsanitize=address`; `restart.sh` runs it
with `CMIGS=/path/to/binary tools/restart.sh`.

## Verification (tools/)

Built against the C server on the same machine:

- **`compare.py`** runs scripted client sessions against two servers and compares
  everything each client receives. It covers registration, the lobby, chat, private
  messages, supervisor commands, rooms, game save and append with the hash check,
  fetch and filtered fetch, the lock protocol, multi-commands, robots, spectate and
  takeover, scoring checks, the oversized-input guard, IP banning, plain and TLS
  websockets (including split frames and a 70 KB relay), the ssl port (`ssl`),
  readers that stop reading so sends are partial (`stall`), and the encrypted
  protocol with checksums and sequence numbers. All scenarios match the C server line for
  line.
- **`cachetest.sh`** has each server save games, compares the cache files, then
  starts each server on the other's cache and fetches the games. Both directions
  match.
- **`stalltest.py`** floods a client that has stopped reading while checking that new
  logins and pings are still answered, then logs in 60 clients at once.
- **`mkconf.sh`** and **`restart.sh`** start throwaway test servers on ports
  3001/3002/3003 (C) and 4001/4002/4003 (Java), the third being the ssl port. The paths in them, `/tmp/mt` and
  `/tmp/cmigs`, are for the test machine; adjust them for yours.

```
tools/restart.sh                 # plain protocol
python3 tools/compare.py 127.0.0.1:3001:3002:3003 127.0.0.1:4001:4002:4003 lobby game ws wss utf8 ssl ban
sudo sysctl -w net.ipv4.tcp_wmem="4096 16384 16384"   # small send buffers, so stall really stalls
python3 tools/compare.py 127.0.0.1:3001:3002:3003 127.0.0.1:4001:4002:4003 stall
tools/restart.sh 1 1             # obfuscation + sequence numbers
python3 tools/compare.py 127.0.0.1:3001:3002:3003 127.0.0.1:4001:4002:4003 crypto
tools/cachetest.sh
python3 tools/stalltest.py 4001
```

The comparison found one bug in the C server, fixed in the accompanying C sources.
Relaying a line of more than 65,532 bytes to a websocket client overran the 64 KB
frame buffer by 3 bytes. That corrupted the heap, and the server aborted with
`FREE_actual: Assertion ... GUARDINT failed`.
