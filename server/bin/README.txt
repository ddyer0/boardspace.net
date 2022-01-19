
This folder contains the running server, backup versions of the server, test versions of the server
and configuration files for the server.

Games contains the active games that are dribbled from the running server
TestGames contains the same for the test server.

Generally, the test server is used to test new versions of the server, not for testing new games,
but it can be used that way.

The actual configuration for the server is partly derived from the database "variations" table,
so that when a new game is added to the database, all that is necessary is to restart the server.
The main thing is that the mapping between directory number known to the games, and the actual
location of the games archives, is determined by the database and the server config file derived
from it.

Copies of files from the active games directories are sometimes useful for low level debugging
of things like the game restart process.

The contents of the Games directory is maintained by the server.  Old items are deleted after
a pretty long sunset period, or (normally) when the game is finished and saved to the game archives.

Note that the regular game archives are in ".sgf" format, but the active games directories are 
in a digital wrapper used by the server, followed by a "game state" in a format that is only
known to the individual game.

