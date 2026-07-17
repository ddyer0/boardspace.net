# Boardspace game server (cserver / BoardSpaceServer / migs.c)
#
# Lives in server/cserver, builds from this directory (build context = ".").
# See DOCKER.md in this same directory for full build/run notes, the
# known strict-mode/IP-trust limitation, and the /data/www volume-sharing
# note relevant to the future Apache/PHP container.

########################################
# Stage 1: build
########################################
FROM debian:bookworm-slim AS builder

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        build-essential \
        libssl-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /build

# Only the files the makefile's BoardSpaceServer target actually needs.
COPY migs.c migs-lib.c migs-lib.h savegame.c websocket.c websocket.h \
     migs-declarations.h threadmacros.h makefile ./

RUN make BoardSpaceServer

########################################
# Stage 2: runtime
########################################
FROM debian:bookworm-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libssl3 \
        ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --home-dir /home/boardspace --shell /usr/sbin/nologin boardspace

WORKDIR /home/boardspace

COPY --from=builder /build/BoardSpaceServer /usr/local/bin/BoardSpaceServer

# Persistent/mounted state:
#   /data/conf   - the server's .conf file (bind-mount your own)
#   /data/logs   - Server.log, Chat.log, Security.log
#   /data/games  - gamecachedir: cached game files
#   /data/status - Server_Status.html (statusfile) -- if Apache's admin
#                  page needs to read this, share this volume with that
#                  container (read-only there).
RUN mkdir -p /data/conf /data/logs /data/games /data/status \
    && chown -R boardspace:boardspace /data /home/boardspace

VOLUME ["/data/logs", "/data/games", "/data/status"]

USER boardspace

# Game port. Adjust/expose the websocket port too if websocketPortNum
# (the "websocket" conf key) is set > 0 in your conf file.
EXPOSE 2255

ENTRYPOINT ["/usr/local/bin/BoardSpaceServer"]
CMD ["/data/conf/BoardSpaceServer.conf"]
