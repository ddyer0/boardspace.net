# Docker container for BoardSpaceServer

Containerizes this directory's C game server (`BoardSpaceServer`, built
from `migs.c` and friends via the existing `makefile`).

## Validated

The exact source in this directory (`migs.c`, `migs-lib.c`, `savegame.c`,
`websocket.c`) was compiled as-is in a Debian/gcc + OpenSSL 3.0 environment
while developing this Dockerfile. It builds clean with only expected
deprecation warnings (`ftime`, `TLSv1_2_server_method`, `SHA1_Init`/
`Update`/`Final` -- OpenSSL 3.0/glibc noting older APIs still work, not new
problems). Runtime library dependencies are exactly `libssl.so.3`,
`libcrypto.so.3`, `libc.so.6`. The binary was also run directly and
correctly printed its usage message.

## Build

From this directory:

```
docker build -t boardspace-cserver .
```

## Run

```
docker run -d --name cserver \
  -p 2255:2255 \
  -v boardspace-logs:/data/logs \
  -v boardspace-games:/data/games \
  -v boardspace-status:/data/status \
  -v boardspace-www:/data/www \
  -v $(pwd)/BoardSpaceServer.docker.conf:/data/conf/BoardSpaceServer.conf:ro \
  boardspace-cserver
```

## Config file

`BoardSpaceServer.docker.conf` is the shipped `BoardSpaceServer.conf` with
paths remapped for containers:

| original | container |
|---|---|
| `/home/boardspa/logs/` | `/data/logs/` |
| `/home/boardspa/public_html/admin/` | `/data/status/` |
| `/home/boardspa/bin/Games/` | `/data/games/` |
| `/home/boardspa/www/<game>/<game>games/` | `/data/www/<game>/<game>games/` |

The `gamedirN` entries (49 of them) point into what was the Apache web
root's per-game subdirectories -- this is where the C server reads/writes
saved game records that the PHP/CGI game-review pages also read directly.
**The eventual Apache/PHP container needs to mount the same `/data/www`
volume** (read-write if it also writes there, read-only if it's purely a
viewer) so both containers see the same game files.

This conf uses the "static" (baked-in) `gametype`/`gamedir` list, which is
the usual approach for development. In production, `cgi-bin/tlib/
serverconf.pl` regenerates that section of the conf from the `variation`
table in the database (and creates any missing game directories) before a
restart -- that's a separate, DB-driven step outside this container, not
something this Dockerfile replicates.

## Known limitation: strict-mode trust (not fixed here)

`migs.c`'s `client_socket_init()` binds its listen socket to `INADDR_ANY`
regardless of the `bindip` config value -- there's even a comment in the
source noting an older version bound to the specific configured IP and
that was deliberately changed. `bindip` is used *only* as the "trusted IP"
value for strict-mode login checking: a connection is trusted if its
source IP matches `bindip` or the hardcoded `127.0.0.1` fallback (`migs.c`
~line 5214 and ~5536).

That assumption held when Apache and the game server shared a host or at
least a loopback interface. It breaks once they're separate containers on
a Docker network, since neither value will match the real container-to-
container source IP. This conf sets `strict,0` for now with a comment
explaining why -- this is exactly the gap the planned HMAC-based
replacement (shared secret between the C server and Apache/CGI) is meant
to close. Re-enable `strict,1` only once that's implemented.
