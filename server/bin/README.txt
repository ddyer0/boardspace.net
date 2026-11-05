
This folder contains the actively running game server and related files.

BoardSpaceServer is the game server binary, also TestServer and frequently
other "last known good" versions.

*.conf are configuration files used to start the server.  Static.conf
contains the fixed part, which is combined with data pulled from the
database to form BoardSpaceServer.conf

-- Obsolete --
the actual startup script is BoardSpaceServer.start
which is invoked by various perl scripts which detect the server is not running
this has the undesirable effect that if apache is restarted, the server is restarted too.
-- Current 11/2026
boardspace is install as a service in /etc/systemd/system/boardspace.service
which runs two scripts derived from the old BoardSpaceServer.start,
 buildconf.sh updates the config file
 runserver.sh starts the service.
 the other difference to make this work is that static.conf sets detatch,0


the Games/ directory contains copies of the game state for active games.
It's loaded and trimmed when the server starts, which supplies the newly
start server with the last known state of games in progress.  New copies
of these files are trickled out as the server runs, to server as a restart
checkpoint in the unlikely event that the server crashes or reboots.

the keys/ folder contains copies of the current SSL keys, for use by
the websocket branch of the server connections.   This copy is kept
up to date by the copycert script, which is invoked by the ssl update
code.

---

When any client is activated, it uses a HTTP request to check the status
of the game server and retrieve the basic parameters - host and socket
to connect to the game server.  As a side effect of this, a quick connection
to the server is made and if it fails, the startup script is invoked.

When the login process (another HTTP interaction) is complete, the login
script contacts the server again, and passes the identity of the logged
in user.  The user's client then connects directly to the game server
and presents the credentials that the login script has provided to the
server.

---

The startup script + config files + actual server + active game records
are more or less duplicated by the testserver versions, useful in some
debugging and development scenarios, but not usually active.

