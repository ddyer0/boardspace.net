/*
	Copyright 2006-2023 by Dave Dyer

	This file is part of the Boardspace project.

	Boardspace is free software: you can redistribute it and/or modify it under the terms of
	the GNU General Public License as published by the Free Software Foundation,
	either version 3 of the License, or (at your option) any later version.

	Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
	without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
	See the GNU General Public License for more details.

	You should have received a copy of the GNU General Public License along with Boardspace.
	If not, see https://www.gnu.org/licenses/.
 */

// websockets not supported is the legacy default
// if on, accept websocket connections and magically treat them the same as regular sockets
#define WEBSOCKET 1

#ifndef __MIGS__
#define __MIGS__
#ifndef X64
#define X64 1
#endif

#include <stdio.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/timeb.h>

#define STRCASECMP _strcasecmp
#define STRNCASECMP _strncasecmp

#if WIN32
#include <process.h>
#define UNLINK _unlink
#define STRICMP _stricmp
#define STRNICMP _strnicmp
#else
#include <stdlib.h>
#include <stdarg.h>
#include <pthread.h>
#include <string.h>
#include <sys/dir.h> 
#include <sys/types.h>
#include <dirent.h>
#include <netinet/tcp.h>
#include <sys/socket.h>
#define INVALID_SOCKET -1
#include <sys/sysinfo.h>
#define SOCKET unsigned int
#endif
#include <assert.h>

#ifndef PROXY
#define PROXY 0
#endif

#define NODELAY 0


#include "threadmacros.h"	// support for single writer thread variables



// todo: add QOS metrics on a per connection basis

/* @doc generic game server

@module internals |

 This documents the generic game server.  The server is crafted in C for maxumum
 transparency and effeciency.  Great care is taken to make the server as reliable
 as humanly possible.
   @normal structures
   @index struct | server
   @normal enumerated types
   @index enum | server
  @normal functions
   @index func | server
   @normal Global Variables
   @index global | server
   @normal examples
   @index example | server
   @normal related topics
   @index topic | server

*/
#define ARRAY_NUMBER_OF_ELEMENTS(x) (sizeof(x)/sizeof(x[0]))

#define LASTREALSESSION (MAXSESSIONS-1)

#define LOBBYSESSION (&Sessions[0])			// the lobby is where everyone sees everyone
#define WAITINGSESSION (&WaitingSession)
#define PROXYSESSION (&ProxySession)	//
#define IDLESESSION (&Idle_Users)
#define MAXWAITTIME 30		/* "select" wait time in select (seconds) */

#define MAXWAITINGTIME 30	/* maximum idle time while waiting is 30 seconds. */
#define SESSIONTIMEOUT 300	/* session setup time is 5 minutes */
#define MAXPROXYTIME 60		/* proxy timeout */
#define SESSIONCLEARTIMEOUT 10	/* timeout for clear sessions is 10 seconds */
#define REGISTERED_USER_TIMEOUT 600		/* 10 minutes for user to load classes and start his lobby, or to keep his lobby closed */
#define BUFFER_ALLOC_STEP 1024		// should never smaller than 16
#define BUFFER_ALLOC_SLOP 8			// always ask for a little more
#define MAX_INPUT_BUFFER_SIZE (1024*1024)	// a megabyte.  There has to be SOME limit on input to prevent suffocation.

#define SecondsInMinute 60.0
#define SecondsInHour 3600.0
#define SecondsInDay 86400.0
#define SecondsInYear 31536000.0


#define ERR_NOLISTEN -2		//client not listening,buffer filled
#define MAX_ERRORS_LOGGED 100
#if WIN32
#include <signal.h>
#include <winsock2.h>
#include <time.h>
#include <fcntl.h>
#include <io.h>
#define fcntl ioctlsocket
#ifdef EWOULDBLOCK
#undef EWOULDBLOCK
#endif

#define EWOULDBLOCK WSAEWOULDBLOCK 
#define FILESEPARATOR '\\'
#else
#define UNLINK unlink
#define STRICMP strcasecmp
#define STRNICMP strncasecmp
#include <sys/resource.h>
#define closesocket close
#define FILESEPARATOR '/'
#include <time.h>
#include <sys/types.h>
#include <sys/time.h>
#include <sys/uio.h>
#include <sys/socket.h>
#include <sys/param.h>
#include <netinet/in.h>

#include <netdb.h>
#include <signal.h>
#include <unistd.h>
#endif


typedef unsigned char BOOLEAN;
typedef long UPTIME;	//seconds since system boot
typedef long DAYS;		//days since forever

#define FALSE 0
#define TRUE 1

#define STRICT 1		// include code for strict login enforcement
#define TIMEOUTS 1		// include code for session timeouts
#define HISTORY 1		// keep a transaction history for debugging

#if WEBSOCKET
#define SERVERID 19
#else
#define SERVERID 18             //time filter in game records
#endif
						//server protocol id.  Increment when we change or add protocols, so clients can tell who
						//they're talking too.  Serverid 4 makes 335 a broadcast message
						// 5 adds private message protocol 230
						// 6 adds support for UIDs
						// 7 adds game type to 305 and 334
						// 8 adds multiple game directories
						// 9 adds a protocol to communicate buffer size
						// 10 adds initial session population to op 201
						// 11 adds send_lobby_info
						// 12 adds password info to 203 opcode
						// 13 adds re-recording games, multi-move commands, and retrieving active game states
						// 14 adds game codes for resumed games
						// 15 adds communication obfuscation and uses dynamic memory allocation
						// 16 adds sequence numbers to commands
						// 17 adds the session lock commands
						// 18 makes the old fetch game "filtered", adds new fetch game for the unfiltered version
						// 19 adds support for direct websocket connections
						// 20 (unimplemented) will add proxy service.

#define THREADSLEEP_US 1000000		// microseconds for status thread to sleep
#define LOG_AGE_MINUTES (7*24*60)	//max time before creating a new log file
#define MAXGAMES 800				//max number of robot games to save
#define GAMEBUFFER_WRITE_POOL_SIZE 20	// games pending write
#define GAMESUFFIX "cachedgame"
#define UIDSIZE 64					//max length of a game id
#define GAMEEXPIRED 60				/* save robot games for 60 days */
#define MAXCONNECTIONS 30			/* max simultaneous connections from one ip (anti-dns measure) */ 
#define MAXCONNECTIONSPERSESSION 15 /* connections allowed accociated with one login */
#define MAXCONNECTIONSPERUID 20		/* connections allowed associated with one UID */
#define MAXSESSIONS 200				/* maxumum number of sumultaneous games we support */
#define MAXCLIENTS 500				/* maximum number of connections open */

#define MAXBANNED 100				/* max banned users we can handle */
#define BANSIZE 20					/* size of the baning id string */
#define BANEXPIRED (60*60)			/* ban an ip address for 1 hour */
#define NAMESIZE 20					/* length of user names */
#define INFOSIZE 256				/* length of auxiliary info */
#define CLIENTPUBLICNAMESIZE (NAMESIZE*6)	/* public names can be long due to translations and unicode */
#define FIRSTUSERNUM 1000			/* arbitrary number of the first user */

// string opcodes
#define SEND_INTRO "200 "		// send establish basic connection info
#define ECHO_INTRO_SELF "201 "	// echo inro to self
#define ECHO_INTRO_OTHERS "203 "// echo intro to others
#define SEND_NAME "204 "		// send player name
#define ECHO_NAME "205 "		// echo to others
#define SEND_SEAT "206 "		// set seat (player order) (obsolete)
#define ECHO_SEAT "207 "		// reply to all in group (obsolete)
#define SEND_GROUP "210 "		// send message to all in group
#define ECHO_GROUP_SELF "211 "	// echo to self
#define ECHO_GROUP_OTHERS "213 "	// echo to the rest
#define ECHO_GROUP_LOCAL "214 "		// echo to this client as user, no special code needed in the server
#define SEND_CHECK_SCORE "218 "	// check if scoring is ok
#define ECHO_CHECK_SCORE " 219 "	// echo from check score.  Note the leading space is expected.
#define SEND_TAKEOVER "220 "	// taking over player or quitting
#define ECHO_I_QUIT "221 "		// quit message to self
#define ECHO_PLAYER_QUIT "223 "	// player connection closed
#define SEND_MESSAGE_TO "230 "	// send message to one client in session
#define SEND_PING "302 "		// round trip, also set game status for lobby's use
#define ECHO_PING "303 "		// echo from ping
#define SEND_SUMMARY "304 "
#define ECHO_SUMMARY "305 "
#define SEND_LOGMESSAGE "308 "
#define SEND_ASK_PASSWORD "310 "
#define ECHO_PASSWORD "311 "

#define SEND_ALL_GROUP "312 "		// send a message to all in group
#define SEND_REGISTER_PLAYER "314 "	// register an extra (robot) player

#define SEND_LOBBY_INFO "330 "	// set info per user about lobby whereabouts
#define SEND_ASK_DETAIL "306 "	// ask for detail about a session
#define SEND_ECHO_DETAIL "307 " // reply to ask_detail
#define SEND_END_DETAIL "309 "	// end of detailed info for a session
#define SEND_WRITE_GAME "316 "	// save a game file
#define SEND_STATE_KEY "328 "	// client reports a game state
#define ECHO_STATE_KEY "329 "	// reply from send state key
#define QUERY_GAME "318 "		// ask if there is a game record
#define ECHO_QUERY_GAME "319 "	// reply to 318
#define FETCH_GAME_FILTERED "320 "		// get game record
#define ECHO_FETCH_GAME_FILTERED "321 "	// result from 320
#define SAVE_GAME "322 "		// save a game record
#define ECHO_SAVE_GAME "323 "	// saved echo
#define REMOVE_GAME "324 "		// remove a game record
#define ECHO_REMOVE_GAME "325 "	// result of remove game
#define SEND_LOGSHORTNOTE "326 "// log a note
#define SEND_RESERVE_ROOM "332 "	// reserve a room for launching a game
#define ECHO_RESERVE_ROOM "333 "	// echo results of reserve
#define SEND_SET_STATE "334 "		// set a room state (room type, game type etc)
#define ECHO_SET_STATE "335 "		// echo results to room
#define APPEND_GAME "336 "		// append to a saved game
#define ECHO_APPEND_GAME "337 "	// failure notice
#define SEND_MULTIPLE "338 "			// multiple simultaneous commands as one
#define FETCH_ACTIVE_GAME_FILTERED "340 "	// request record of active game
#define ECHO_ACTIVE_GAME_FILTERED "341 "		// result
#define LOCK_GAME "342 "			// request locking the game record
#define FETCH_GAME "344 "		// get game record
#define ECHO_FETCH_GAME "345 "	// result from 344
#define FETCH_ACTIVE_GAME "346 "	// get the active game
#define ECHO_ACTIVE_GAME "347 "	// result from 246
#if PROXY

#define SEND_PROXY_OP "361 "
#define ECHO_PROXY_OP "362 "
#define SEND_PROXY_DATA "363 "
#define ECHO_PROXY_DATA "364 "

#endif

#define ECHO_FAILED "999 "			// prefix for failed commands

#define KEYWORD_SPECTATE "spectate"
#define KEYWORD_PLAYING "playing"


#define SMALLBUFSIZE 256
#define LARGEBUFFER 4096
#define FILENAMESIZE 256

#define LBSIZE (MAX_INPUT_BUFFER_SIZE*10)			//size of the print buffer.  No reason to make this small
#define LOGSLOP (MAX_INPUT_BUFFER_SIZE*2)			//slop at the end so we don't overrun the buffer.  This should be
									// at least the size of an incoming message, since we may log one.



/* major data structures.  */

typedef struct User User;						// user structures, one per socket. 
typedef struct Session Session;					// session structures, one per game room
typedef struct registeredUser registeredUser;	// user registered by the web server
typedef struct GameBuffer GameBuffer;			// state of a robot game
typedef struct bannedUser bannedUser;			// user banned by the web server




/* @struct 
 one session structure per game room.  Session 0 is somewhat special
 because it is used as the lobby.  Otherwise, sessions know nothing
 about the games they are serving.
<nl>Overview: <t internals>
*/
struct Session
{	UPTIME sessionTimes;			// @field last activity by any user in this session
	int sessionNUM;				// @field index into the session array
	int sessionStates;				// @field  active, idle, connecting..
	int sessionGameID;				// @field  what game type 
	User *first_user;				// @field  first user in the session;
	User *locker;					// @field user locking the session game record
	BOOLEAN poisoned;				// @field true if no players are allowed
	BOOLEAN sessionDescribe;		// @field  session needs to be described to session zero
	int population;			// @field  number of users in this session
	GameBuffer *sessionGame;		/* @field  saved game associated with this session */
	int sessionStateKeyIndex;			//@field hash index for sessionStateKey
	char sessionStateKey[SMALLBUFSIZE];	//@field self-reported "state" key used in fraud detection
	char sessionURLs[SMALLBUFSIZE]; /* @field  passwords to join session */
	char sessionInfo[SMALLBUFSIZE]; /* @field  info for the lobby */
	int sessionHasGame;		// @field  nonzero if the session was set up as a new game
	int sessionIsPrivate;			// @field  session is restricted to no new users
	int sessionScored;				// @field  nonzero if the session has been scored 
	int sessionClear;				// @field  session is empty, awaiting clearing
	int sessionFileWritten;		// @field  session has requested a writefile
	unsigned int sessionKey;		// @field  we generate a session key for each active session
};

/* @struct
one User structure per connection from a user.  Each user is
located in one session, and has a socket stream.  Associated
I/O buffers and so on are here.
<nl>Overview: <t internals>
*/
struct User
{	int topGuard;
	int userNUM;				// @field the handle by which users know each other	
	Session *session;			// @field the session associated with this user.  This is never null
	User *next_in_session;		// @field users in the session are threaded through this pointer 
	User *nextDead;				// @field temporary for dead users list
	Session *deadFromSession;	// @field previous session, for obituary notices
#if PROXY
	User *proxyOut;				// @field send proxy to another user which we are using as a proxy
	User *proxyFor;				// @field user to send our data to
	BOOLEAN connecting;			// @field true if expecting a connection to complete
#endif
	char reasonClosed[32];		// @field and the cause of death....
	char pingStats[64];			// @field ping start, P:last,min,average,max
	char cookie[BANSIZE];		// @field cookie presented
	int errorsLogged;			// @field errors logged since connection
	SOCKET socket;				// @field unix socket number
	int badReadCount;			// @field count of consecutive unsuccessful read attempts
	int badWriteCount;			// @field count of consecutive unsuccessful write attempts;
	int goodReadCount;			// @field count of successful reads
	int goodWriteCount;			// @field count of successful writes
	unsigned int ip;			// @field user ip address
	unsigned int serverKey;		// @field user server supplied key.  See comments about "strict" logins
	registeredUser *keyIndex;	// @field our index in the registered user structure

	//  each user has his own input and output buffer preallocated
	int inbuf_put_index;		// @field input filler index, where input will go
	int inbuf_take_index;		// @field emptier index
	int outbuf_put_index;		// @field output filler index
	int outbuf_take_index;		// @field output emptier index
	BOOLEAN supervisor;			// @field true if the user is god, used for command interpretation
	BOOLEAN blocked;			// @field true if output was incomplete
	BOOLEAN wasZapped;			// @field true if the connection unexpectedly failed
	BOOLEAN isAPlayer;			// @field true if we're a player rather than a spectator
	BOOLEAN checksums;			// @field client is using checksums
	BOOLEAN sequencenumbers;	// @field true if client is supplying sequence numbers
	BOOLEAN gagged;				// @field client has been gagged
	BOOLEAN expectEof;			// @field connection is expected to be closing or closed
	BOOLEAN inputClosed;		// @field if no input is being accepted from this user
	BOOLEAN outputClosed;		// @field if no more output is being accepted
	BOOLEAN isARobot;			// @field connection is a fake, registered for a robot player
	BOOLEAN requestingLock;		// @field if we want the session lock eventually
#if WEBSOCKET
	BOOLEAN websocket;			// @field true if this socket is using websocket protocols
	int  websocket_errno;		// @field error overrides other errno
	void *websocket_data;		// @field in websocket preamble
#endif
	UPTIME clientTime;			// @field last active time
	int oopsCount;				// @field count of missed supervisor commands (fraudcatcher)
	int unexpectedCount;		// @field unexpected message count (hack catcher)
	int clientSeat;			// @field the client seat position
	int clientOrder;			// @field the clients order (ordinal)
	int clientRev;				// @field clients per-game revision level
	//
	// clientPublicName and clientRealName are normally the same, but can be
	// different for non-playing guests, and for players who have quit
	//
	char clientPublicName[CLIENTPUBLICNAMESIZE];	// @field the client's declared name.  Can be long due to translations and unicode
	char clientRealName[CLIENTPUBLICNAMESIZE];		// @field same as clientname, but never changes
	char lobbyInfo[64];					// @field info about the lobby location
	int clientUid;						// @field client unique id
	unsigned int totalread;			// @field stats
	unsigned int totalwrite;			// @field stats
	unsigned int nread;				// @field stats
	unsigned int nwrite;				// @field stats
	char *outbufPtr;	// @field output buffer (very large)
	int outbufSize;
	int rogueOutput;			// mismatch between counters in output process (probably hacking)
	int injectedOutput;			// commands injected at sendmessage (probably hacking);
	int m_z_in;					// input random number generator state
	int m_w_in;	
	int m_z_out;				// output random number generator state
	int m_w_out;	
	int use_rng_in;				// encrypting input
	int use_rng_out;			// encrypting output
	int rng_out_chars;			// number of encrypted chars out 
	int rng_in_chars;			// number of encrypted chars in
	int rng_in_seq;				// number of commands processed
	int rng_in_seq_errors;		// number of times it was wrong
	int rng_out_seq;			// number of commands processed
	int midGuard;
	char *inbufPtr;		// @field the input buffer
	char *tempBufPtr;		// @field temp output buffer, larger than input buffer
	int tempBufSize;
	int inbufSize;
	int bottomGuard;
};


/* @struct
users are banned initially because they tell us so, originating with the "isbanned" entry
in their database entry.  Once a banned user presents himself for punishment, his IP is banned
for an hour,  and his 'cookie' is banned indefinitely. The cookie is derived from his
browser's cached value for a cookie.  This cookie value is refreshed from the database if 
it's not found in the browser, so tends to be pretty permanant.
Victims of banning can be cleared by changing their ban code to "U"
Supervisors (dave and bdot) can be designated by setting their ban code to "S"

If there get to be too many banned users, slots are reused round robin.  This means
that the secondary banning due to same ip or same name or same cookie (without the
"please kill me" notice) are not completely reliable, but that's fine - they're only
intended to catch clowns who are banned but persist in trying to sneak in through
another identity.
<nl>Overview: <t internals>
*/
struct bannedUser
{	unsigned int banned_ip;		// @field the IP address associated with this ban event
	UPTIME startTime;				// @field timestamp
	char cookie[BANSIZE];			// @field the identity ccokie
	char serverBancode;				// @field the type of ban
	char userName[NAMESIZE];		// @field the banned user name
	int userUid;					// @field the banned user ID
	int attempts;					// @field number of additional log in attempty
	int eventid;
};

/* @struct
	the registered user scheme works like this
	when the login script polls the server to see if it is up, it has the
	side effect of "registering" the approved user name and a 32bit key.

	The server will accept connecitons only from "registered" users, and only with
	the name that matches the registered ip and name.  This helps prevent rogue applets
	from connecting under arbitrary names and possibly spoofing somebody else.  It also
	has the effect of making it harder to develop third party clients, legitimately or
	otherwise.

	A second tack is that various CGI scripts that leave footprints, ie; score changes
	will also poke the server to see if the name and ip they are associated with are
	registered.  This will inhibit hackers from making rank the easy way, by simply
	executing "game finished" requests.

<nl>Overview: <t internals>

  */
struct registeredUser
{	unsigned int ip;			// @field the ip address that connected to the server
	unsigned int regkey;		// @field an arbitrary registration key
	UPTIME timestamp;			// @field the time last active
	char clientName[NAMESIZE];	// @field client's username
	int clientUid;				// @field client's uid
	char clientInfo[INFOSIZE];	// @field auxiliary info about the client
	registeredUser *next;		// @field the next registered user
};


/* @struct 
gamebuffers hold the state of incomplete games, which may be either single
player games against a robot or games with real players.  These games are
remembered until they are complete, or until they 30 days old.

The underlying concept is that users can't abandon games when they are losing.
Once users get used to the idea, they don't try.

Normally there is a GameBuffer associated with each active session, and
a GameBuffer associated with any game which might need to be restarted.
Generally, unranked games and games against guests are not restartable, 
but everything else is.

Gamebuffers for restartable games are saved to disk files using a simple
disk image, but on a clock which limits the rate of saves.  This provides
restart after crashes (a rare event...) at a nominal cost.

<nl>Overview: <t internals>
*/
#ifdef BOARDSPACE
#define OLDGAMEBUFSIZE (4096*12)	/* maxumum size of a saved game.  Tablut games can be very long */
#else
#define OLDGAMEBUFSIZE 2000
#endif

// gamebuffer from the static game buffer era.  Note that
// this is different size for tantrix and boardspace.
typedef struct OldGameBuffer
{	char idString[UIDSIZE];		// @field the unique id string naming this game
	char Game[OLDGAMEBUFSIZE];		// @field the actual game replay data
	UPTIME startTime;			// @field the time last active
	//
	// put the rest after the UID and game, so minor changes in format won't invalidate cached games.
	//
	unsigned int hashCode;		// @field hashcode for lookup
	int uid;					// @field the owner of the game's UID
	SINGLE_WRITER(MAIN,GameBuffer *,next_dirty_gamebuffer);		// @field rememebers gamebuffers that need to be written
	Session *ownerSession;		// @field session which owns this game record
	SINGLE_WRITER(MAIN,BOOLEAN,hashed);				// @field true if this game is in the hash table (vs only in the session)
	SINGLE_WRITER(MAIN,BOOLEAN,dirty);				// @field true if the game is pending write 
} OldGameBuffer;

/* @struct GameBuffer |

From the viewpoint of the server, the gamebuffer is just a long string that describes the game.
The server doesn't know or care what the internal structure of the string is.  Server operations
to manipulate the string are only to establish, forget, and append a new trailing segment to
the string.
<nl>
When the user appends to the gamebuffer, the server arranges to (eventually) save a shadow
copy of the modified gamebuffer, which is used to recover from crashes, or (preferably) planned shutdowns.
<nl>
Saving is normally done by a background process, so the main process never blocks for file I/O.
<nl>
Memory management for gamebufferes is rather complicated, especially when in the process
of discarding a completed game, so a reference count is used.
*/
#define GAMEBUFMAGIC 0xfafe9193
struct GameBuffer
{	int magic;
	char idString[UIDSIZE];		// @field the unique id string naming this game
#if WIN32
	long junk;	// ad hoc repack to keep the game records the same as gcc makes them
#endif
	DAYS startTime;			// @field the time last active

	char *gamePtr;				// @field the actual game replay data
	//
	// put the rest after the UID and game, so minor changes in format won't invalidate cached games.
	//
	GameBuffer *next;
	GameBuffer *prev;
	SINGLE_WRITER(MAIN,int,refCount);
	int gamePtrSize;
	unsigned int hashCode;		// @field hashcode for lookup
	int uid;					// @field the game's unique number
	int gamePtrOffset;
	SINGLE_WRITER(MAIN,GameBuffer *,next_dirty_gamebuffer);		// @field rememebers gamebuffers that need to be written
	Session *ownerSession;		// @field session which owns this game record
	BOOLEAN hashed;				// @field true if the game is current in the hash table
	BOOLEAN deleteme;
	SINGLE_WRITER(MAIN,BOOLEAN,preserved);			// @field true if this game is to be preserved ina file
	SINGLE_WRITER(MAIN,BOOLEAN,dirty);				// @field true if the game is pending write 
};

/* @struct fileBuffer |
this structure controls a logging stream, of which there
are currently three.  Logged text is stored here by the
main thread, and eventually emptied by a separate logger
thread
<nl><nl>
logging streams are periodically closed, renamed, and restarted
*/
typedef struct fileBuffer
{	int logIndex;						//@field input pointer
	int logTake;						//@field output pointer
	int logBreak;						//@field count of missed log entries
	int logChange;						//@field exclusion counter
	char logBuffer[LBSIZE];				//@field print buffer
	int logXtend;						//@field overrun detection for logbuffer
	int writtenSize;					//@field amount added to the file so far
	BOOLEAN flushLock;					//@field if true, don't try to reset this buffer
	BOOLEAN writeLock;					//@field if true, don't write to this buffer
	BOOLEAN renameLogFile;				//@field if true, rename the log file now
	BOOLEAN notflushable;				//@field if true, autoflush
	FILE *logStream;					//@field the log file stream is always open
	char *logFileName;					//@field name of the logFile file
	UPTIME logStart;					//@field the starting time for this log since last rename
} fileBuffer;


// save game thread
typedef struct SaveGameInfo {
	SINGLE_WRITER(MAIN,int,n_dirty_gamebuffers);		// number of gamebuffers modified and marked for writing
	SINGLE_WRITER(MAIN,int,max_dirty_gamebuffers);		// max value of the above
	SINGLE_WRITER(SAVEGAME,int,n_dirty_writes);		// total of writes performed
	SINGLE_WRITER(SAVEGAME,int,n_dirty_problems);				// number of problems (errors) encountered saving.
	SINGLE_WRITER(MAIN,GameBuffer *,first_dirty_gamebuffer);		// gamebuffers which are dirty are kept in a linked list
	SINGLE_WRITER(MAIN,GameBuffer *,last_dirty_gamebuffer);		// which grows from the end, so first is the oldest dirty

	// Dirty gamebuffers are copied to a temporary pool and then marked as clean.  The 
	// pool is emptied in a separate thread, so the main server is not affected by I/O delays.
	// if the dirty pool isn't emptying fast enough, the pending queue will get long with no other
	// side effects.
	STATIC_ARRAY(GameBuffer,GameBuffer_Write_Pool,GAMEBUFFER_WRITE_POOL_SIZE);
	SINGLE_WRITER(MAIN,int,GameBuffer_Write_Pool_Filler);
	SINGLE_WRITER(SAVEGAME,int,GameBuffer_Write_Pool_Emptier);

	SINGLE_WRITER(MAIN,char *,gameCacheDir);		// game cache directory. where saved games are saved
	SINGLE_WRITER(MAIN,int,gameCacheRate);			// number of games per second to save per second
} SaveGameInfo;

#if WIN32
void saveDirtyGameThread(void *args);
#else
void *saveDirtyGameThread(void *args);
#endif
void saveDirtyGamesNow(SaveGameInfo *G);

// log buffer emptier
void flushLog(fileBuffer *B);

typedef enum loglevel
{	log_none = 0,					//@emem log_none = 0 | only extremely serious or fatal errors
	log_errors = 1,					//@emem log_errors = 1 | minor errors and client messages
	log_connections = 2,			//@emem log_connections = 2 | routine connections
	log_all = 3						//@emem log_all = 3 | all traffic
} loglevel;

extern loglevel logging;

void DumpHistory();


#include "migs-lib.h"

// used by websockets
#if WIN32
typedef int socklen_t;
#endif
int ErrNo();
BOOLEAN setNBIO(SOCKET sock);

#if WEBSOCKET
void banUserByIP(User* U);
void client_websocket_init(int portNum);
void closeWebSocket();
extern SOCKET webserversd;
int websocketRecv(User *u, unsigned char* buf, int siz);
int websocketSend(User* u, unsigned char* buf, int siz);
void freeWebsocket(User* u);
void simpleCloseClient(User* u, char* cxt);
extern char* websocketSslKey;
extern char* websocketSslCert;
BOOLEAN setBIO(SOCKET sock);
#endif

// end of file
#endif
