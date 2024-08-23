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
#include "migs-declarations.h"
#if 0

  This is the server that provides communications among clients for boardspace.net and tantrix.com.  It evolvedsession reserved
  from a very simple server that did very little more than transmit lines of text back and forth, as you might do
  in a student proof of concept client/server.  Over time, many more features were added, but never enough at one
  time to trigger a complete rewrite.  So remember, don't fix what's not broken.  This server is very reliable
  and much higher performance than required by the current or any likely sserver load.

    The basic transmission is still based on a line of text, but over time, the lines have acquired the ability
    to restart games (both after crashes and when interrupted by the clients), log events and rotate the logs,
    sequence numbers, checksums, encryption, and support for binary transmission.  

    The key uglyness is that a "packet" is still terminated by a newline.

    The server runs a few threads; one to multiplex I/O among the game clients, one to write logs, one to save games
    in progress as an insurance against crashes or shutdowns, and one to save completed games.
				     
    The server maintains activity logs, by default at the level of connections, and rotates the logs based on a size
    limit.   There are actually 3 separate logs, the general log, a security alert log, and a chat log.

    The server maintains a "canonical state" for each game, which is used to start spectators, restart games that
    were abandoned, or were in progress when the server is deliberately shut down or crashes.  Crashes are very rare.
    These game states are dribbled out to a directory at a fixed rate, so if there were a zillion active games, the
    rewrite activity in the game cache would remain the same, and potentially the saved state would lag behind 



coventry 9/26/2008
Files analyzed                 : 3
Total LoC input to cov-analyze : 82401
Functions analyzed             : 166
Classes/structs analyzed       : 185
Paths analyzed                 : 26027
New defects found              : 108 Total
                                   1 BUFFER OVERFLOW
                                   3 LOGIC ERROR
                                  85 MEMORY CORRUPTION

    the played state.   This lag is unimportant given that the server never crashes and is rarely shut down deliberately.

    The server does not interact with the mysql database in any way.  There are some vestiges of a plan to bundle
    this interactions into the server, but they have never been completed.

    Completed games are written to a directory specific to the game, but of course the location of those directories
    is not a parameter supplied from the clients.

Access Control

     An external perl script validates the users login, and creates a time-limited token that will allow a client
     to connect.  Communications that do not start by presenting a token are closed and banned.  One of the irritants in the
     network environment is random clients connecting, looking for proxy ports.
				    
#endif
#if 0
// coventry 9/26/2008
Files analyzed                 : 3
Total LoC input to cov-analyze : 82401
Functions analyzed             : 166
Classes/structs analyzed       : 185
Paths analyzed                 : 26027
New defects found              : 108 Total
                                   1 BUFFER OVERFLOW
                                   3 LOGIC ERROR
                                  85 MEMORY CORRUPTION
                                   3 NULL POINTER DEREFERENCE
                                  15 PREVENT COMPILER WARNING
                                   1 SECURITY VULNERABILITY


#endif

/* runtime parameters set from the config file */
static unsigned int security_key=0;	//user supplied security code
static int strict_login=1;				//enforce strictly login from server host
static int require_rng = 0;				// require random number obfuscation
static int require_seq = 0;				// require sequence numbers
static int strict_score=1;				//enforce strict scoring
static int portNum;						//the sock number the server listens on
static int maxSessions=0;				//the nunber of sessions (0-n) we will run.  Must be less than MAXSESSIONS
static int maxClients=0;				//the number of clients we will run, must be bess than MAXCLIENTS
static int maxConnections=MAXCONNECTIONS;	//max connections in the "waiting" session per client ip address
static int maxConnectionsPerSession=MAXCONNECTIONSPERSESSION;
static int maxConnectionsPerUID=MAXCONNECTIONSPERUID;
static char *statusfile=NULL;			//file name for status (html)
static char *playerInfoUrl="";		//file url for status file
static int ngamedirs=0;		        //number of game dirs
static int ngametypes=0;		//number of game types
static int nroomtypes=0;		//number of room types
static int use_threads=0;
static int crash_test=0;				//if nonzero, include doomsday code for crash testing

loglevel logging=0;

#if WEBSOCKET
static int websocketPortNum = 0;
char* websocketSslKey = NULL;
char* websocketSslCert = NULL;
#endif

/* runtime parameters set from supervisor commends */

BOOLEAN isClosed=FALSE;		// closed to new connections
char Exempt_User[NAMESIZE];		// name of user (or ip) to exempt from login controls


static SaveGameInfo saveGameInfo;

static User Users[MAXCLIENTS];	//@global | Users | the array of all User structures

static User *activeClients[MAXCLIENTS];			//keep track of sockets active at the top
Session Sessions[MAXSESSIONS];	//@global | Sessions | the array of all Session structures
static Session *sessionStateHash[MAXSESSIONS*2];	//@global | sessionStateHash | hash index of session states
static Session Idle_Users;		//@global | Idle_Users | an extra session that anchors the freelist of <t User> structures
static Session ProxySession;	//@global | ProxySession | sessions using a proxy connection to elsewhere
static Session WaitingSession;	//@global | WaitingSession | sessions newly connected, not in a regular session yet.
static User *DeadUsers;			//@global | DeadUsers | dead users who were playing, waiting to notify the rest of the session

static registeredUser *freeRegisteredUsers=NULL;
static registeredUser *activeRegisteredUsers=NULL;
static registeredUser all_registered_users[MAXCLIENTS];
static GameBuffer *GameHash[MAXGAMES*2];		//@global | GameHash | the hash table to help locate games
static GameBuffer *all_gamebuffers = NULL;
static int n_gamebuffers = 0;

bannedUser	bannedUsers[MAXBANNED];


// forward references
static User *processCheckSummedMessages(char *cmd,User *u,char *rawcmd,char *seq);
BOOLEAN logErrorFor(User *u);
User *removeProxyClient(User *u,int sub);
void closeProxyClient(User *u,int sub);
int safe_scanstr(char *cmd,char *dest,unsigned int destsize,int line);
#define scanstr(cmd,dest,destsize) safe_scanstr(cmd,dest,destsize,__LINE__)
int safe_scanint(char *cmd,int *dest);
void freeGameBuffer(GameBuffer *rval);


/*

New features:

	Writes a "server status" html file
	Send opcode "309" to indicate the end of a list of users in an opcode 307 sequence.

	For game client version 2.9, add the concept of a "fake socket", which has
	all ancillary data but no real socket.  This allows embedded robots to appear
	as players, even though they do not use a socket.  It also allows disconnected
	players to remain visible until they time out or reconnect.
	
	For game client version 2.10, added features to the "220" disconnection code, to
	allow players to switch between player and spectator status. 


Bugs fixed & features added:

9/17/98		Closed sockets can appear as "zero bytes read", so handle it correctly.
	This gave the impression of clients stuffing lots of nulls into a conection.

1/5/98	Revamped buffering to cope with the possibility that not all data sent is 
		received at the same time.
2/12/98 Fixed a race in launching games by requiring that a session be empty to set its password
		added "session info" taken from heartbeat, transmitted with session status
2/22	Added checksum to transactions, message history.
3/2/99	Reduced active game timeout from 1 hour to 2 minutes.  Current versions of the game client
		ping ever 5 seconds or so.  This should fix a problem where half-dead connections accept data
		but never return any, and so count as live.

3/14/99 Added simple admin controls.

3/18/99 Concerted scattered arrays of length MAXCLIENTS to User structure

4/11/99 Added game recording stuff, to support robot restarts

7/1/99	Fixed a protocol glitch while allowed multiple robot restarts to all finish a game.
7/3/99  Changed "333" protocol to echo the entire line. This is part of the 'final solution'
	to the 'bad start' problem.

9/16/99 Added "idle time" "uptime".  Changed logging to gmtime

10/3/99  Added "session State" info in 305 message
		Added opcode 334 "set session state"
		Changed server version to 2

11/10/99	Added code for buffering output.  The upshot of a lot of work to try to improve on the
	killed process problem is not much improvement.  The substantive improvements are to change
	the polling loop to favor input over output (read first, then write) and to buffer output
	starting when write fails due to the "would block" condition.  The buffer is not absurdly
	large.  WIth these changes, most of the "kill"s appear to be initiated by the client committing
	suicide.

1/10/00	Added enforcement of "strict" protocol.

5/5/2000	Added a small bit of logic to preserve foreign translation of "guest" in the display name.

6/10/2000	Bumped version to 3
			Added an opcode (326) to allow simple logging of messages
			Added logic "sessionClear" to give a little grace to empty sessions not scored
			Added sessionDescribe flag
			Added some reporting (robot_irregular) of possible robot-fraud attempts 

9/26/2000	Changed input buffering to grab i/o in maximum size chunks.  Unfortunately this doesn't
	seem to help the random disconneciton problem.

4/19/2002	Added support for banning troublesome users
	Changed status file to use threads instead of inline activity
4/20/2002	Added support for logging in alternate thread
9/14/2002	fixed a minor bug with reconnection, reconnected user not marked as "player"
			fixed minor problems with "close" and "open" commands, which now work
			tweaked supervisor commands to say "oops" if password not recognized, and not transmit the command.
			added "230" command to send a "213" to a specific user.
1/30/2003	added logic to create new log files on demand or every 7 days.  Fixed
			some bugs in the "zap" command.  Added command logFile -1 to create 
			a new log manually.

6/2003		added support for uids rather than nicknames
3/2004		added 218 check uid for scoring
3/2004		added session game type to 305 and 334 strings
5/2005		added "exempt" command
5/2005		expanded banned user reporting, added "supervisor" user
11/2005		major revision to banish "firstFreeSlot" and "loopCtr" and 
			make the iterations all use a spaghetti list of users and sessions
2/2006		add some flavor to the status file to show types of games in progress
			add the option to run without threads
			tweak the scoring logic to treat "strict 0" more uniformly with "strict 1"
			fix a real bug involving overrunning the log buffer
10/2006		added chat log file
10/2006		added security log file and unban supervisor command.  Also changed the 
			file initialization to start by renaming the old log files.
10/2006		added detach option so we're liberated from the apache server
11/2006		added SEND_LOBBY_INFO opcode
7/2007		added session state logic to detect follower fraud
5/2008		added ping stats to message 302
6/2008		added "server remembers" features to allow the server to remember all games in progress
			added save game thread and game restoration on restart
			fixed a buffer overflow bug in input, which probably accounts for random "checksum error"
			and "invalid user id" bugs that have been extant forever.
7/2013		Switched to using some dynamic memory allocation, so input buffers could be effectively
			unlimited in size without preallocating an unreasonable amount of space.
8/2013		Added in-stream encryption and some covert signaling to combat data injection attacks.
2/2014		made GameBuffer dynamically allocated and reference counted, which
			fixed a longstanding problem with bookkeeping and deleting finished games.
8/2024		added direct support for websocket connections on a separate listening port
			repurposed maxconnections to apply only to the waiting session, and fixed a bug
			where waiting session was accidentally excluded from the check.
			reduced the timeout time for the waiting session
Message protocols

General:

connections are identified by an index into "clients".  When these "connection numbers" are made
visible to the outside world, use index+1.  Each conneciton uses a unique socket.  Socket numbers
are never shown to the outside world.  Each connection is also assigned to a "session".  All connections
in the same session are part of message group, and some messages are echoed to all members of a session.
The lobby is session 0, but for the most part this is just a convention; the server doesn't do anything
special for session 0.

All transactions are started with a 3 digit "opcode".  Even opcodes are incoming.  Odd opcodes are outgoing.
Generally, response opcodes to the sender are incoming+1, response opcodes to the rest of the session
are +3.

All messages are terminated by a linefeed. Carraige returns are ignored.  Characters outside the range
32-127 are encoded as \nnn (3 decimal digits).  \ is encoded as \\.  So messages will never contain unusual
ascii characters, but the one "line" per message can contain many characters, and when decoded, can contain
linefeeds.


opcodes:

  500	checksum, acts as a "wrapper" for any other message.  format is "500 AAAA nnn ...". AAAA is a 
		16 bit checksum encoded as 4 letters. nnn is the "real" opcode followed by all the usual stuff
		for that opcode.
 
  501	outgoing checksum  (as above)

		Also, to combat the observed failure modes, outgoing messages are preceeded by a leading space,
		which is ignored by the recepient, and incoming messages of "00 " are recognised as "500 "

  999	Ignored on input (normally this would be an outgoing message)
		on output, 999 is a general " I didn't understand what follows". 
		The server sends one for any uninterpretable string

  200	first message after conneciton.  format is "200 n" where n is the desired session to join.
		if "strict" is in force, 200 n name a.b.c.d must be presented, where a.b.c.d is the server key
		supplied from the web site launch page.  if session is -1 and the real ip address is the
		server's real ip address, a.b.c.d is the key to register.
		following server ip and password, is the banner (machine id) and the banner key (am i banned)
  201	(response) format is "201 n clientnum serverID gamekey buffsize sessionpop
  203	(response to others) format is "203 clientnum" announcing new member of the session
 
  218	218 n uid1 uid2 a.b.c.d
		query if session n is a game between uid1 and uid2 by user with key a.b.c.d
  219	219 1 if ok OR 219 0 if not.


  302	inquire number of sessions and users
  303	(response) format is "303 n k seconds milliseconds"
		seconds and milliseconds is the servers estimate of gmtime
  304	inquire all sessions
  305	one response for each session. Format is "305 sessionnum numin state passworded info"
		numin is the number of connections currently in the session
		state is the user-set state of the session (used to be "capacity")
		passworded is 1 if the session is locked 
		info (indefinite length) is "session info" set by clients.

  306	describe a session in detail. format is "306 n"
  307	(response) one response for each client in the session
		format is "307 sessionn clientn color name"
  309	(final response) marks the end of description of a session
		format is "309 sessionn nclients"

  308	logFile request.  The content of the message is entered in the error logFile,
		along with a history of recent messages.  This should be used to serious errors, possibly
		involving communication errors.

  310	get session password format is "310 session"
  311	(response) "311 sessionnum password"

  312	is a pseudo client, the rest of the message is sent to all members of the session
		as though it came from the specified member.  This is used by robots (sharing a connection)
		to send messages that will be seen as "from the robot" instead of from the client who actually
		owns the conneciton.  Format is typically "312 213 nn whatever".  Clients in the session see
		"213 nn whatever" which is what they would receieve if client nn sent a "210 whatever" .

  314	register a pseudo client slot.  format is "314 client color name"  a new client is 
		initialized, in the same session as the current client.  (the "client" arg is ignored).
		and with color and name set.  This new client
		is marked so no actual output will be sent to it, bit otherwise it is a normal participant
		in the same session as the original client.

  316	write file (nominally a game record file) to the games area specified in the conf file.
		format is 316 filename stuff.  Data is unescaped and written to file.

	318 Query game (sessionid + idstring) 

	320 Fetch Game (idstring)

	322 SaveGame (idstring)

	324 RemoveGame (idstring)

  326	logFile request.  The content of the message is entered in the logFile.
		This should be used to log information not related to communication errors.

  330	set lobby info for player (where he is waiting to play, etc)

  332	set session password (more)

  334	set session state
  335	(response)

  210	echo to session.  This is the workhorse.  format is "210 xxx"
		special case! chat messages are checked for the supervisor password, and if
		found, for supervisor commands.  See "doSupervisor" for details.
  211	(reponse) format is "211 xxx"
  213	(response to session) format is "213 n xxx" where n is the client who send 210

  204	set client name
  205	(response)

  206	set client color.  Only players (not spectators) set their colors.  Colors are
		0-n, spectators are -1.  The color information is used in various ways.  It appears
		in the "307" response to describe sessions in detail.  It is also used to identify 
		the client to be taken over when reconnecting to a game..

  207	(response)

  220	shutdown (close client) request.  Special cases for "220 spectate" and "220 playing" which are
		used when leaving or joining a game without changing the conneciton itself.  In general, 221 x
		is passed to the other players in a session, to interpret as they will.  Conventionally, "suicide"
		is sent to indicate voluntary disconnetion.

		Except in the lobby (session 0) or for spectators, the slot for players is not reclaimed immedately, 
		but is marked to save the place in case the player.  The name for these disconnected players is
		changed from "name" to "(name)", so the lobby can identify the slots available for reconnection.

		220 playing nn	//where nn is the "color" set by a "206" searches for a slot in the session 
		with the indicated color.  If found, it takes over the slot and expects the client to resume
		acting like a player.

		220 spectate		// does te opposite; it changes the slot currently occupied by a player
		into an abandoned player slot, and allocatges a new "spectator" slot 

  221	(response)
		Sent in response to a 220 to all members of the session, but also sent spontaneously
		if the connection is lost for any reason. Recepients should look for "playing" and "spectate"
		messages especially, but more generally should do bookkeeping associated with departing players.

  230	echo to one client in current session
		230 x message
		client x receives 
		213 y message
		this is the same format as most 213 messages arrive, but is narrowcast rather than broadcast



*/


void error2(char *inStr,char *inStr2);

char host_name[64];


/**

  Encrypted streams, overview.  

  First, this isn't a cryptograpically secure encryption, only a random number generator based
  substitution code, which will discourage casual snoopers and hackers.

  Two slightly different random number generators are used for input and output, which are
  used to shift input and output charcters in the standard symmetric encryption manner.

  The two essential facts are that this generator (and the consumers of the sequences) have
  to be absolutely the same here and for the java client, and the sequence of characters
  encrupted has to be absolutely in sync with the decrypted sequence.

  This means the communication has to be reliable, and also that injection attacks which
  do not go through the intended endpoints will mess up the communication and cause it to 
  fail completely.  This is intentional.

*/
//m_w = <choose-initializer>;    /* must not be zero */
//m_z = <choose-initializer>;    /* must not be zero */
 
int get_random_in(User *u)
{
    u->m_z_in = 36969 * (u->m_z_in & 65535) + (u->m_z_in >> 16);
    u->m_w_in = 18000 * (u->m_w_in & 65535) + (u->m_w_in >> 16);
    return  u->m_w_in&0xffff;  /* 16-bit result */
}
int get_random_out(User *u)
{
    u->m_z_out = 36969 * (u->m_z_out & 65535) + (u->m_z_out >> 16);
    u->m_w_out = 18000 * (u->m_w_out & 65535) + (u->m_w_out >> 16);
    return u->m_w_out&0xffff;  /* 16-bit result */
}
//
// the random sequences are initialized from an ascii string
// which has to be exactly the same here and in the java clients.
//
void init_rng_in(User *u,char *instr)
{	u->rng_in_chars = 0; 
	u->rng_in_seq = 1;				// first sequence number will be 1 if the client supplies them
	u->rng_in_seq_errors = 0;
	if(!(instr && *instr))
	{
		u->m_w_in = u->m_z_in = u->use_rng_in = 0;
	}
	else
	{	char *str1 = instr;
		char *str2 = instr;
		while(*str1) { u->m_w_in = u->m_w_in*13+*str1++; }
		while(*str2) { u->m_z_in = u->m_z_in*31+ *str2++; }
		u->use_rng_in = TRUE;
	}
}
//
// the random sequences are initialized from an ascii string
// which has to be exactly the same here and in the java clients.
//
void init_rng_out(User *u,char *outstr)
{	u->rng_out_chars = 0;
	u->rng_out_seq = 1;			// first sequence number will be 1
	if(!(outstr && *outstr))
	{
		u->m_w_out = u->m_z_out = u->use_rng_out = 0;
	}
	else
	{	char *str1 = outstr;
		char *str2 = outstr;
		while(*str1) { u->m_w_out = u->m_w_out*17+*str1++; }
		while(*str2) { u->m_z_out = u->m_z_out*23+ *str2++; }
		u->use_rng_out = TRUE;
	}
}

#if HISTORY
/* @topic history |
the server records a transaction history of the last n (=200 currently) I/O transactions
to assist debugging sequences of I/O events.  The hisory is kept as a ring of recent
data which is recycled continuously.  Various errors and log requests from users trigger
dumping the current history into the log.
  */
#define HISTORYSIZE 500							//number of messages to remember
#define HISTORYCHARS 120						//number of chars per message to save
char History[HISTORYSIZE][HISTORYCHARS];		//message history buffer
int history_index=0;							//next position to save into

#endif

char **gamedirs=NULL;		// game directory names
char **gametypes=NULL;		// game type names
char **roomtypes=NULL;		// room type names


BOOLEAN logFilesEnabled=FALSE;
int max_logfile_size=1024*1024;


/* @enum loglevel |
how much logging to do.  These integers correspond to values
found in the config file and in the "logFile" command
 <nl>Overview: <t internals>
 */



/* Important motivation message 9/17/2008

 Using the ordinary time(NULL) as the reference time turned
 out to be bad idea, as the time of day is changeable.  This
 happened today on Tantrix.com, where the time got set back
 by 5 hours and this server started seriously misbehaving.

 So use uptime, which cannot decrease
*/
UPTIME Uptime()	// return uptime in seconds
{
#if WIN32
  return(GetTickCount()/1000);
#else
	struct sysinfo info;
	sysinfo(&info);
	return(info.uptime);
#endif
}

DAYS DaysSinceForever()
{	// standard unix time will expire in 2035 or so, so to avoid
	// a dependency on a day I may live to see, use "days since" instead
	// of "seconds since" as a long term time.
	time_t ltime;
    time(&ltime);
	return((DAYS)(ltime/(60*60*24)));

}


/* @topic Logging |
logging is done to large buffer, which is periodically flushed out to 
a file   If the emptier catches up to the filler, the buffer is reset back to start.  
If the filler approaches the end of the buffer, it stops filling.  
There are no "wait" states to interlock them.  Instead,
if the filler and emptier both reach a critical section, the second party
just skips his task.  Log files are periodically closed and renamed, so the
active log doesn't get infinitely large.
*/

/* @func
create or append to a log file
<nl>Overview: <l server>
*/
void openLogFile
	(fileBuffer *B)	//@parm the <t fileBuffer>
{ 
  if(B->logFileName && strcmp(B->logFileName,"none")!=0)
  {
	B->logStart = Uptime();
	B->writtenSize = 0;
	B->logStream = fopen(B->logFileName,"a");
	if(!B->logStream) {error2("Can't open log file ",B->logFileName); }
  } 
}

/* @func
output the accumulated log data.  This normally runs in a separate
emptier thread, and will do nothing if the filler is in a critical
section.
<nl>Overview: <l server>
*/
void flushLog(fileBuffer *B)
{  B->flushLock=TRUE;				// prevent the filler from resetting the buffer
   if(!B->writeLock)
	{//skip this cycle if we're in a critical period
	int idx = B->logIndex;
	int take = B->logTake;
	int len = idx-take;
	assert((idx>=0)&&(idx<=sizeof(B->logBuffer)));
	assert((take>=0)&&(take<=sizeof(B->logBuffer)));
	if(len!=0)
	{assert(len>0);
	 fwrite(B->logBuffer+take,1,len,B->logStream);		//write, maybe block
	 B->writtenSize += len;
	 if(B->writtenSize>max_logfile_size) { B->renameLogFile = TRUE; }
	 fflush(B->logStream);
	 B->logTake = idx;

	 if(logFilesEnabled)	// master logging switch
	 {
	 //see how old the log file is, maybe force a new one
	 if(B->notflushable == 0)
	 {UPTIME now = Uptime();
	  int upseconds = now - B->logStart;
	  int upminutes = upseconds/60;
	  if(upminutes>LOG_AGE_MINUTES) { B->renameLogFile=TRUE; }
	 }
	 // create a new log file if requested. This can be set above
 	 // or by console command
	 if(B->renameLogFile)
	 {	char newlog[SMALLBUFSIZE];
		int idx=0;
		char *stamp = timestamp();
		B->renameLogFile=FALSE;
		{ 
		  char *dot = strrchr(B->logFileName,'.');
		  if(dot)
		  { size_t idx = dot-B->logFileName;
			lsprintf(sizeof(newlog),newlog,"%s",B->logFileName);
			lsprintf(sizeof(newlog)-idx,newlog+idx,"-%s%s",stamp,dot);
		  }
		  else
		  {
			lsprintf(sizeof(newlog),newlog,"%s-%s",B->logFileName,stamp);
		  }
		}
		while(newlog[idx]!=0) { if(newlog[idx]==':') { newlog[idx]='-'; } idx++; }
		fclose(B->logStream);
		rename(B->logFileName,newlog);

		openLogFile(B);
	 }
	 }
	}
  }
	B->flushLock=FALSE;	// let the filler back in
}
/* @func
convert a string to lower case
<nl>Overview: <l server>
*/
char *toLowercase
	(char *str)			//@parm the string
{	char *lstr=str;
	char ch;
	while((ch=*lstr)!=0) 
	{ if((ch>='A')&&(ch<='Z')) { *lstr=ch+('a'-'A'); } 
		lstr++; 
	}
	return(str);
}

int  sendSingle(char *inStr, User *u);
void sendMulti(char *inStr,Session *s);
void doEchoSelf(User *ru, char *toSource,  int ignoreF);
void doEchoOthers(User *ru, char *toOthers, int ignoreF);
void doEchoAll(User *ru, char *toSource,  int ignoreF);

int bannedIndex = 0;
int totalBanned = 0;
int totalAttempts = 0;
bannedUser *banned_user = NULL;	// banned user that caught somebody

/* @enum bancode |
internal codes for reasons why an ip is banned
<nl>Overview: <t internals>
  */
typedef enum bancode
{	bc_none = 0,		//@emem bc_none = 0 | not banned
	bc_unban = 1,		//@emem bc_unban = 1 | un-ban this user
	bc_same_ip =  2,	//@emem bc_same_ip =  2 | banned for having the same IP
	bc_same_id = 3,		//@emem bc_same_id = 3 | banned for having the same identity
	bc_same_name = 4,	//@emem bc_same_name = 4 | banned for having the same user name
	bc_super = 5,		//@emem bc_super = 5 | banned by supervisor command
	bc_blank = 6		//@emem bc_blank = 6 | used to print a nice blank in reports
} bancode;

char *banreason[] = 
{	"ok",							// 0
	"un banned",					// 1
	"banned for matching ip",		// 2
	"banned for matching identity",	// 3
	"banned by name",				// 4
	"supervisor override",			// 5
	"",								// 6 
};
// [Mar 01 20:35:26] new banned user babyluv uid 13866 identity 1197194) 
// banned by name (ref #0: babyluv Y ip: 127.0.0.1  id: 1197194 )
char *banInfoFor(bannedUser *B,bancode code)
{
	static char msgbuf[256];
	unsigned int ip = B->banned_ip;
	int b4 = (ip>>0)&0xff;
	int b3 = (ip>>8)&0xff;
	int b2 = (ip>>16)&0xff;
	int b1 = (ip>>24)&0xff;
	msgbuf[sizeof(msgbuf)-1]=(char)0;
	msgbuf[0]=(char)0;
	lsprintf(sizeof(msgbuf),msgbuf,
		"%s (ref #%d: %s %c ip: %d.%d.%d.%d  cookie: %s uid: %d )",
		banreason[code],
		B->eventid,
		B->userName,
		B->serverBancode,
		b1,b2,b3,b4,
		B->cookie,
		B->userUid);
	assert(msgbuf[sizeof(msgbuf)-1]==(char)0);	// check for buffer overflow
	return(msgbuf);
}

// return a static string with info about the current banning
char *banInfo(bancode code)
{	if(banned_user==NULL) { return(banreason[code]); }
	else
	{ return(banInfoFor(banned_user,code));
	}
}

void describeBans(User *SU)
{	UPTIME now = Uptime();
	int limit = totalBanned>=MAXBANNED ? MAXBANNED : totalBanned;
	int i;
	int shown = 0;

	for(i=0;i<limit;i++)	
	{	bannedUser *bu = &bannedUsers[i];
		char obuf[SMALLBUFSIZE];
		if(	(now - bu->startTime) < BANEXPIRED)
		{
		lsprintf(sizeof(obuf),obuf,ECHO_GROUP_OTHERS "%d schat %s",SU->userNUM,banInfoFor(bu,bc_blank));
		sendSingle(&obuf[0],SU);
		shown++;
		}
	}
	if(shown==0)
	{
		char obuf[SMALLBUFSIZE];
		lsprintf(sizeof(obuf),obuf,ECHO_GROUP_OTHERS "%d schat No active bans",SU->userNUM);
		sendSingle(&obuf[0],SU);
	}
	
}
bannedUser *unBanEvent(int id)
{	
	int limit = totalBanned>=MAXBANNED ? MAXBANNED : totalBanned;
	int i;
	for(i=0;i<limit;i++)
	{	bannedUser *bu = &bannedUsers[i];
		if(bu->eventid==id)
		{  
		logEntry(&securityLog,"[%s] supervisor unban %s\n",timestamp(),banInfoFor(bu,bc_blank));
		bu->banned_ip=0;
		bu->userUid=0;
		bu->cookie[0]=(char)0;
		bu->userName[0]=(char)0;
		bu->startTime = 0;
		return(bu);
		}
	}
	return(NULL);
}
/* @func
Check the user name, ip, and uid against the list of banned users, possibly
ban or unban the user, and return a <t bancode> describing the new status.
<nl><nl>
The effect of the action may be to newly ban or unban this user.  These
letter codes originate in the database, or in supervisor commands.
<nl>'Z'		ban this user (from supervisor command)
<nl>'Y'		ban this user (from database)
<nl>'S'		unban this user, he's a supervisor
<nl>'U'		unban this user (from database)
the intent of the 'S' code is to prevent supervisors being accidentally locked
out.
<nl>Overview: <t internals>
  */
bancode isBanned
	(char *username,				//@parm the user name
	int uid,					//@parm the UID
	char serverBancode,			//@parm the <t serverBancode>
	char *cookie,				//@parm the user's identity cookie
	unsigned int ip)			//@parm the current IP or 0
{	UPTIME now = Uptime();
	enum bancode why = bc_none;
	int limit = totalBanned>=MAXBANNED ? MAXBANNED : totalBanned;
	int newban = (serverBancode=='Z')||(serverBancode=='Y')||(serverBancode=='y');
	int super =  (serverBancode=='S') || (serverBancode=='s');
	int unban = super || (serverBancode=='U')||(serverBancode=='u') ;

	banned_user = NULL;		// in case we generate an error return

	{
	int i;
	for(i=0;i<limit;i++)
	{	int unban_now = 0;
		int newban_now = 0;
		bannedUser *bu = &bannedUsers[i];

		if( (now - bu->startTime) < BANEXPIRED)
		{
		BOOLEAN samecookie = (cookie[0]!=0)
								&& (strcmp(bu->cookie,cookie)==0) 
								&& (strcmp(cookie,"-1")!=0) 
								&& (strcmp(cookie,"0")!=0);
		BOOLEAN sameip = (ip!=0) && (ip==bu->banned_ip);
		BOOLEAN samename = (username[0]!=0) && (strcmp(username,bu->userName)==0);
		BOOLEAN sameuid = (uid!=0) && (uid==bu->userUid);
		if(samename)
		{	newban_now = 1;
			why = bc_same_name;
			banned_user = bu;
		}
		else if(sameuid)
		{	newban_now = 1;
			why = bc_same_id;
			banned_user = bu;
		}
		else if(samecookie)
			{// same cookie as a banned user
			 why = bc_same_id;
			 banned_user = bu;
			 newban_now=1;
			}
		else if(sameip)
			{ // same IP address as a banned user
			  why = bc_same_ip;
			  banned_user = bu;
			  newban_now = 1;
			}

		if((newban||unban)
				&& (samename
					|| sameuid
					|| samecookie
					|| sameip))
		{	why = super ? bc_super : unban ? bc_unban : bc_same_name;
			newban_now = newban;
			unban_now = unban;
			banned_user = bu;
		}
			
		if(unban_now)
			{
			logEntry(&securityLog,"[%s] unban %s\n",timestamp(),banInfo(why));
			 bu->banned_ip=0;
			 bu->userUid=0;
			 bu->cookie[0]=(char)0;
			 bu->userName[0]=(char)0;
			 bu->startTime=0;
			 return(why);
			}
		if(newban_now)
			{
			 bu->attempts++;
			 bu->banned_ip = ip;
			 bu->startTime = now;
			 MyStrncpy(bu->cookie,cookie,sizeof(bu->cookie));
			 bu->userUid=0;
			 bu->userName[0]=(char)0;
			 if(strcmp("guest",username)!=0)
					{ //careful not to permanantly ban guests!
					 bu->userUid=uid;
					 MyStrncpy(bu->userName, username, sizeof(bu->userName));
					}
			logEntry(&securityLog,"[%s] renew ban %s\n",timestamp(),banInfo(why));
			totalAttempts++;
			return(why);
			}
		}
	}}

	//new ban, and not previously found
	if(super) { return(bc_super); }
	else if(newban)
	{	bannedUser *B = &bannedUsers[bannedIndex];
		MyStrncpy(B->userName, username, sizeof(B->userName));
		MyStrncpy(B->cookie,cookie,sizeof(B->cookie));
		B->banned_ip = ip;
		B->serverBancode = serverBancode;
		B->startTime = now;
		B->attempts=0;
		B->userUid=uid;
		B->eventid = bannedIndex;
		banned_user = B;
		bannedIndex++;
		totalBanned++;
		if(bannedIndex>=MAXBANNED) { bannedIndex=0; }
		logEntry(&securityLog,"[%s] new banned user %s uid %d identity (%s) %s\n",timestamp(),
			username,uid,cookie,
			banInfo(bc_same_name));
		logEntry(&mainLog,"[%s] new banned user %s uid %d identity (%s) %s\n",timestamp(),
			username,uid,cookie,
			banInfo(bc_same_name));

		return(bc_same_name);
	}

	return(bc_none);
}
/* @func
ban the user, but not the IP he's currently using 
<nl>Overview: <t internals>
*/
void banUser
	(User *U)	//@parm the <t User>
{	// ban to the cookie but not the ip
	U->expectEof=TRUE;
	isBanned(U->clientRealName,U->clientUid,'Z',U->cookie,0);
	logEntry(&mainLog,"[%s] unusual banned user %d cookie %d\n",
					timestamp(),U->clientUid,U->cookie);

}
void banIP(unsigned int ip)
{
	int b4 = (ip >> 0) & 0xff;
	int b3 = (ip >> 8) & 0xff;
	int b2 = (ip >> 16) & 0xff;
	int b1 = (ip >> 24) & 0xff;
	isBanned("", 0, 'Z', "", ip);
	logEntry(&mainLog, "[%s] unusual banned IP %d.%d.%d.%d\n",
		timestamp(), b1, b2, b3, b4);

}

void banUserByIP(User* U)
{
	U->expectEof = TRUE;

	banIP(U->ip);
}

int roundUp(int val,int r)
{	int sz = ((val+r)/r)*r;
        return(sz>=0?sz:1);
}

/* @func
unban this partucular user
<nl>Overview: <t internals>
*/
void unBanUser
	(User *U)	//@parm the <t User>
{	isBanned(U->clientRealName,U->clientUid,'U',U->cookie,U->ip);
}

SOCKET maxfd=-1;

/* @func
return the number of output bytes pending
<nl>Overview: <t internals>
*/
static int output_pending
	(User *u)	//@parm the <t User>
{	int dif=u->outbuf_put_index-u->outbuf_take_index;
	if(dif>=0) { return(dif); }
	else { return(u->outbufSize+dif);}
}

// min is the new size required
void setGameBufferSize(GameBuffer *g,int min,BOOLEAN copy)
{	int newsize = roundUp(min+BUFFER_ALLOC_SLOP,BUFFER_ALLOC_STEP);
	void *newbuf = ALLOC(newsize);
	void *oldbuf = g->gamePtr;
	if(oldbuf!=NULL)
	{	// we don't need to preserve the data from the old buffer
		if(copy)
		{	CHECK(oldbuf,g->gamePtrSize);
			MEMCPY(newbuf,oldbuf,g->gamePtrSize);
		}
		FREE(oldbuf,g->gamePtrSize);
	}
	g->gamePtr = newbuf;
	g->gamePtrSize = newsize;
}


// min is the new size required.  Never decrease the size of the buffer, since
// it's also guaranteed to be larger than the input buffer.
void setTempBufSize(User *u,int min)
{	int newsize = roundUp(min+BUFFER_ALLOC_SLOP,BUFFER_ALLOC_STEP);
	if(newsize>u->tempBufSize)
	{
	FREE(u->tempBufPtr,u->tempBufSize);
	u->tempBufPtr = ALLOC(newsize);
	u->tempBufSize = newsize;
	}
}

// min is the additional size needed
void increaseOutputBufferSize(User *u,int min)
{	int newsize = u->outbufSize + roundUp(min+BUFFER_ALLOC_SLOP,BUFFER_ALLOC_STEP);
	void *newbuf = ALLOC(newsize);
	void *oldbuf = u->outbufPtr;
	if(oldbuf!=NULL)
	{	CHECK(oldbuf,u->outbufSize);
		MEMCPY(newbuf,oldbuf,u->outbufSize);
		FREE(oldbuf,u->outbufSize);
	}
	u->outbufPtr = newbuf;
	u->outbufSize = newsize;
}
// min is the additional size needed
void increaseInputBufferSize(User *u,int min)
{	
	int newsize = u->inbufSize + roundUp(min+BUFFER_ALLOC_SLOP,BUFFER_ALLOC_STEP);
	CHECK(u->inbufPtr,u->inbufSize);
	CHECK(u->tempBufPtr,u->tempBufSize);
	FREE(u->tempBufPtr,u->tempBufSize);
	// tempbuf is for constructing replies.  It's always larger than input, so
	// if the reply has to quote the input, it is big enough.  It's also checked.
	u->tempBufSize = newsize+1024;
	u->tempBufPtr =  ALLOC(u->tempBufSize);

	{
	void *oldbuf = u->inbufPtr;
	void *newbuf = ALLOC(newsize);
	if(oldbuf!=NULL)
	{	MEMCPY(newbuf,oldbuf,u->inbufSize);
		FREE(oldbuf,u->inbufSize);
	}
	u->inbufPtr =  newbuf;
	u->inbufSize = newsize;
	}
}


static void ClearUser(User *u)
{		u->socket = -1;
		u->ip=0;
#if PROXY
		u->proxyFor = NULL;
		u->proxyOut = NULL;
		u->connecting = FALSE;
#endif
		u->errorsLogged=0;
		u->serverKey=0;
		u->keyIndex=NULL;
		u->inbuf_put_index=0;
		u->inbuf_take_index=0;
		u->totalread=0;
		u->totalwrite=0;
		u->nread=0;
		u->nwrite=0;
		u->outbuf_take_index=0;
		u->outbuf_put_index=0;
		u->blocked=FALSE;
		u->supervisor=FALSE;
		u->isARobot = FALSE;
		u->checksums=FALSE;
		u->sequencenumbers=FALSE;
		u->wasZapped=FALSE;
		u->reasonClosed[0]=(char)0;
		u->pingStats[0]=(char)0;
		u->isAPlayer=FALSE;
		u->gagged=FALSE;
		u->expectEof=FALSE;
		u->inputClosed = FALSE;
		u->outputClosed = FALSE;
		u->requestingLock = FALSE;
#if WEBSOCKET
		freeWebsocket(u);
		u->websocket = FALSE;
		u->websocket_errno = 0;
		u->websocket_data = NULL;	// just to be sure
#endif
		u->oopsCount=0;
		u->injectedOutput = 0;
		u->rogueOutput = 0;

		u->m_z_in = 0;
		u->m_w_in = 0;
		u->use_rng_in  = 0;
		u->m_z_out = 0;
		u->m_w_out = 0;
		u->use_rng_out  = FALSE;

		u->unexpectedCount=0;
		u->clientPublicName[0]=(char)0;
		u->clientRealName[0]=(char)0;
		u->lobbyInfo[0]=(char)0;
		u->clientUid = 0;
		u->clientSeat =-1;
		u->clientOrder=-1;
		u->clientRev=-1;
		u->clientTime=0;
		FREE(u->outbufPtr,u->outbufSize);
		// make sure the buffer appears to be empty when it has been freed.
		u->outbuf_put_index = u->outbuf_take_index = 0;
		u->outbufPtr = NULL;
		u->outbufSize = 0;

		FREE(u->tempBufPtr,u->tempBufSize);
		u->tempBufPtr = NULL;
		u->tempBufSize = 0;

		FREE(u->inbufPtr,u->inbufSize);
		// make sure the buffer appears to be empty when it has been freed.
		u->inbuf_put_index = u->inbuf_take_index = 0;
		u->inbufPtr = NULL;
		u->inbufSize = 0;
		u->cookie[0]=0;
		// don't touch session or next in session
		// don't clear deadFromSession either
		// u->deadFromSession=NULL;
}
static void CopyUser(User *from,User *to)
{
		if(to!=from)
		{to->socket = from->socket;
		 to->ip=from->ip;
		 to->serverKey=from->serverKey;
		 to->keyIndex=from->keyIndex;
		 to->totalread=from->totalread;
		 to->nread=from->nread;
		 to->nwrite=from->nwrite;
		 to->totalwrite=from->totalwrite;
		 to->blocked=from->blocked;
		 to->supervisor=from->supervisor;
		 to->isARobot = from->isARobot;
		 to->wasZapped=from->wasZapped;
		 to->nextDead=from->nextDead;
		 to->isAPlayer=from->isAPlayer;
		 to->expectEof=FALSE;
		 to->inputClosed = FALSE;
		 to->outputClosed = FALSE;

		 FREE(to->outbufPtr,to->outbufSize);
		 CHECK(from->outbufPtr,from->outbufSize);
		 to->outbufSize = from->outbufSize;
		 to->outbufPtr = from->outbufPtr;
		 to->outbuf_put_index=from->outbuf_put_index;
		 to->outbuf_take_index=from->outbuf_take_index;
		 from->outbufPtr = NULL;
		 from->outbufSize = 0;
		 from->outbuf_take_index = 0;
		 from->outbuf_put_index = 0;

		 FREE(to->inbufPtr,to->inbufSize);
		 CHECK(from->inbufPtr,from->inbufSize);
		 to->inbufSize = from->inbufSize;
		 to->inbufPtr = from->inbufPtr;
		 to->inbuf_take_index=from->inbuf_take_index;
		 to->inbuf_put_index=from->inbuf_put_index;
		 from->inbufPtr = NULL;
		 from->inbufSize = 0;
		 from->inbuf_take_index = 0;
		 from->inbuf_put_index = 0;
		 
		 to->oopsCount = from->oopsCount;
		 to->requestingLock = from->requestingLock;
#if WEBSOCKET
		 freeWebsocket(to);
		 to->websocket = from->websocket;
		 to->websocket_data = from->websocket_data;
		 to->websocket_errno = from->websocket_errno;
		 from->websocket = FALSE;
		 from->websocket_data = NULL;
#endif
		 from->requestingLock = FALSE;
		 from->oopsCount = 0;
		 to->unexpectedCount = from->unexpectedCount;
		 from->unexpectedCount = 0;

		 //
		 // copy the state of the random number sequences to the new user
		 //
		 to->use_rng_in = from->use_rng_in;
		 from->use_rng_in = 0;
		 to->m_z_in = from->m_z_in;
		 to->m_w_in = from->m_w_in;
		 to->rng_in_seq = from->rng_in_seq;
		 to->rng_in_seq_errors = from->rng_in_seq_errors;
		 from->m_z_in = 0;
		 from->m_w_in = 0;
		 from->rng_in_seq = 0;
		 from->rng_in_seq_errors = 0;

		 to->use_rng_out = from->use_rng_out;
		 from->use_rng_out = FALSE;
		 to->m_z_out = from->m_z_out;
		 to->m_w_out = from->m_w_out;
		 to->rng_out_seq = from->rng_out_seq;
		 from->m_z_out = 0;
		 from->m_w_out = 0;
		 from->rng_out_seq = 0;

		 FREE(to->tempBufPtr,to->tempBufSize);
		 CHECK(from->tempBufPtr,from->tempBufSize);
		 to->tempBufPtr = from->tempBufPtr;
		 to->tempBufSize = from->tempBufSize;
		 from->tempBufPtr = NULL;
		 from->tempBufSize = 0;

		 to->checksums=from->checksums;
		 to->sequencenumbers = from->sequencenumbers;
		 MEMCPY(&to->clientPublicName[0],&from->clientPublicName[0],sizeof(from->clientPublicName));
		 //
		 // this is not a bug.  CopyUser is called when a spectator is taking
		 // over for a player who quit.  The player's "realname" and "public name"
		 // should be initially both the same,  This only matters for guests, whose
		 // real name is "guest" but whose public name is translated and with channel added.
		 //
		 MEMCPY(to->clientRealName,from->clientPublicName,sizeof(from->clientRealName));
		 MEMCPY(&to->lobbyInfo[0],&from->lobbyInfo[0],sizeof(from->lobbyInfo));
		 to->clientUid = from->clientUid;
		 to->clientSeat=from->clientSeat;
		 to->clientOrder=from->clientOrder;
		 to->clientRev = from->clientRev;
		 to->clientTime = from->clientTime;
		 to->errorsLogged = to->errorsLogged/2;	// give some credit in case that's what caused the disconnection
		 ClearUser(from);
		}

}
/* socket descriptors */
static UPTIME start_time,start_idle_time,bktime;
static UPTIME idle_time;
static int sockets_open=0;
static int maxsockets=0;
static int isshutdown=0;
static int client_errors = 0;
static int client_messages = 0;
static int unusual_events = 0;
static int save_failed = 0;
static int robot_irregular = 0;
static int games_completed=0;
static int clients_connected = 0;
static int maxlobby = 0;
static int games_launched = 0;
static int games_cached = 0;
static int player_games_relaunched = 0;
static int robot_games_relaunched = 0;
static int transactions=0;
static int readBreak=0;
static int writeBreak=0;
static int blocked_clients=0;
static int unblocked_clients=0;
static int closed_blocked_clients=0;
static unsigned int totalwrite=0;
static unsigned int totalread=0;
static int checksum_errors=0;						//number of times we found a back checksum


static int GameUIDs=1;

#define HASHSIZE ARRAY_NUMBER_OF_ELEMENTS(GameHash)

SOCKET serversd=0;
unsigned int server_ip=0;				//the server's real ip address
unsigned int alt_server_ip=0;// localhost ip ie; 127.0.0.1
static int refusing;
static char *supervisor=NULL;					//supervisor password
static struct sockaddr_in iclient;        /* socket addresses */
static int IP1,IP2,IP3,IP4;
static char configFile[SMALLBUFSIZE];
static fd_set rfds,efds,wfds;        /* file descriptor sets */
static int lobby_timeout;
static int client_timeout;

// scan an integer using scanf, return the next index
int safe_scanint(char *cmd,int *destn)
{	int nchars = 0;
	int nints = sscanf(cmd,"%d%n",destn,&nchars);
	if(nints==1) { return(nchars); }
	else { return(-1); }
}

// scan a string into a dest buffer, being careful about buffer sizes.
// return the next index 


int safe_scanstr(char *cmd,char *dest,unsigned int destsize,int line)
{
	unsigned int nchars=0;
	int nstrings = sscanf(cmd,"%*s%n",&nchars);		// just parse
	if(destsize>0) { *dest = (char)0;}
	// minor unix differences when the input is a null string, unix
	// returns -1 for nstrings and nchars is unchanged
	if(nstrings == -1) { return(0); }
	if((nstrings == 0)&&(nchars>=0)&&((nchars+1)<destsize))
	  {
	    sscanf(cmd,"%s",dest);				// really scan
		assert(strlen(dest)<=nchars);
	  }
	else
	{	

		if(logging>=log_none)
		{	MyStrncpy(dest,cmd,destsize);
			logEntry(&mainLog,"[%s] from line %d buffer too small for string \"%s\", buffer %d needs %d\n",
				timestamp(),
				 line, 
				dest,
				destsize,
				nchars);
		}

		nchars = -1;		// buffer too small
		
	}
	return(nchars);

}


/*-----------------------------------------------------------------*/


static void putUserInSession(User *u,Session *s)
{	assert(u->session==NULL);
	u->session=s;
	u->next_in_session = s->first_user;
	s->first_user = u;
	s->population++;
}

static void removeUserFromSession(User *u)
{	Session *s = u->session;
	assert(s!=NULL);
	if(u==s->first_user) 
	{ s->first_user = u->next_in_session; 
	}
	else 
	{ User *fu = s->first_user;
	  while(fu->next_in_session!=u) { fu=fu->next_in_session; assert(fu!=NULL); }
	  fu->next_in_session = u->next_in_session ;
	}
	u->session=NULL;
	if(s->locker==u) { s->locker = NULL; }
	u->next_in_session=NULL;
	s->population--;
}

#if WIN32

/** this is "fig leaf" level security against copying the server to 
unauthorized machines.  We read a few registry keys, munch them, 
use the result as a challenge key.  Munch them a while more, use
that as the enabling key.
*/
char *SERVER_KEYS[] =
{	"SYSTEM\\CurrentControlSet\\Control\\ComputerName\\ComputerName\\ComputerName",
	"SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\ProductId",
	"SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\ProductId",
	0

};
#define SERVER_SEED 0x3f452954

#define VERBOSE _DEBUG
#include "security.c"

int CheckSecurity(unsigned int testval)
{	unsigned int key = final_key(machine_key(SERVER_SEED,SERVER_KEYS));
	printf("config file key is %lx\n",testval);
	logEntry(&mainLog,"machine key is %lx\n",key);
	return(key==testval);
}
#endif


#if HISTORY
/* @func
record a message in the history.
<nl>Overview: <t internals>
*/
void recordInHistory
	(const char *cxt,	//@parm context of the i/o
	SOCKET socket,			//@parm the socked associated with the text
	const char *str,	//@parm the text, not null terminated
	size_t len)			//@parm length of the text
{	char *buf=&History[history_index][0];
	int sock = (int)socket;
	int idx = lsprintf(HISTORYCHARS,buf,"%3d %s: ",sock,cxt);
	int nrem=HISTORYCHARS-idx-4;
	size_t alen = (len<nrem)?len:nrem;
	MyStrncpy(&buf[idx],str,alen);
	buf[idx+alen]=(char)0;
	{size_t last = strlen(buf);
	if(buf[last-2]=='\r') { buf[last-2]=buf[last-1]; buf[last-1]=(char)0; last--; }
	if(buf[last-1]!='\n') { buf[last++]='\n'; buf[last]=(char)0; }
	history_index++;
	if(history_index>=HISTORYSIZE) { history_index=0; }
	}
}
/* @func
 dump the history buffer to the log file, and clear as we go so the same
elements won't be printed again if multiple dump requests
arrive in quick sucession 
<nl>Overview: <t internals>
*/
void DumpHistory()
{	int idx=history_index;
	int i=0;
	logEntry(&mainLog,"\nRecent messages:\n");
	do {	//walk the whole ring, printing oldest first
		if(History[idx][0]!=0)
		{	i++;
			logEntry(&mainLog,"%3d %s",i,&History[idx][0]);
			History[idx][0]=(char)0;		//clear so we won't print again
		}
	idx++;
	if(idx==HISTORYSIZE) { idx=0; }
	} while(idx!=history_index);

}

#endif /* history */
/*-----------------------------------------------------------------*/
int ErrNo()
{
#if WIN32
	static int lastError=0;
	int err = WSAGetLastError();
	if(err!=0) {lastError=err;}
	return(lastError);
#else
	return(errno);
#endif
}

int hexDig(int ch)
{	if((ch>='a')&&(ch<='f')) { return(ch-'a'+10); }
	if((ch>='A')&&(ch<='F')) { return(ch-'A'+10); }
	if((ch>='0')&&(ch<='9')) { return(ch-'0'); }
	if(logging>=log_errors)
	{
	logEntry(&mainLog,"[%s] out of range hex digit %c\n",timestamp(),ch); 
	}
	return(0);
}

//
//remove the escape coding on a message buffer.  \\ becomes \nnn becomes an ascii char
//
// also note that skip_escaped_text has to use exactly the same algorighm
//
void unescapeString(char *message)
{	int ch,i,j;
	for(i=0,j=0; (ch=message[i])!=0; i++)
	{ if(ch=='\\') 
		{ ch=message[++i]; 
		  if(ch!='\\')
		  { 
			  if(ch=='#')
			  {	// note that this is a late addition, which the server never 
			    // needed to handle because all the strings we were given
				// to unescape were thought to be ascii rather than unicode

			    // this would lose data if the strings were actually 16 bit unicode, 
			    // but we are using this format only as an intermediate stage toward
			    // using utf-8 everywhere, and in fact all the strings we get here
			    // are utf-8
				  char c1 = message[++i];
				  char c2 = message[++i];
				  char c3 = message[++i];
				  char c4 = message[++i];
				ch =
					(hexDig(c1)<<12)
					+ (hexDig(c2)<<8)
				    + (hexDig(c3)<<4)
					+ hexDig(c4);
			  }
			  else
			  {
			  int hundreds = ch-'0';
		      int tens = message[++i]-'0';
			  int ones = message[++i]-'0';
			  ch = hundreds*100+tens*10+ones;
			  }
		  }}
	message[j++]=(char)ch;
	}
	message[j++]=(char)0;
}

void process_logshortnote(char *data,User *u,char *seq)
{	unescapeString(data);
	//just a quick note, not an error.  This would cause disconnection if too many notes were passed
	//if(logErrorFor(u))
	{
	if(logging>=log_errors)
		{
		logEntry(&mainLog,"[%s] note from C%d (%s#%d) on S%d session %d\n",
			timestamp(),u->userNUM,u->clientRealName,u->clientUid,u->socket,u->session->sessionNUM);
			 logEntry(&mainLog,"\"%s\"\n",data);
		}
	}
}

void process_logmessage(char *data,User *u,char *seq)
{	unescapeString(data);
	client_messages++;
	if(logErrorFor(u))
	{
	if(logging>=log_errors)
		{logEntry(&mainLog,"[%s] logFile request #%d from C%d (%s#%d) on S%d session %d\n",
				timestamp(),client_messages,
				u->userNUM,u->clientRealName,u->clientUid,u->socket,
				u->session->sessionNUM);
		 logEntry(&mainLog,"\"%s\"\nend of C%d (%s#%d) S%d\n",
			 data,u->userNUM,u->clientRealName,
			 u->clientUid,u->socket);
		#if HISTORY
		DumpHistory();
		#endif
		}
	}
}
//
// logGame {number}{N} file body
// if N is supplied, session is not expected to be scored
//
void logGame(char *message,Session *s,User *u)
{	unescapeString(message);
	{char buf[SMALLBUFSIZE];
	 char namebuf[SMALLBUFSIZE];
	 int idx=0;
	 int gamen=0;
	 while((message[idx]>='0')&&(message[idx]<='9'))
	 {	gamen = gamen*10 + message[idx]-'0';
		idx++;
	 }
	 while((message[idx]<=' ') && (message[idx]>(char)0)) { idx++; };
	 {
	 int nameidx=scanstr(message+idx,buf,sizeof(buf));
	 char *b = (char *)strrchr(buf,FILESEPARATOR);
	 if(!b) { b = &buf[0]; } else { b++; };									//dont allow path components
	 if((nameidx>0) && (gamen>=0) && (gamen<ngamedirs))
	 {
	 lsprintf(sizeof(namebuf),namebuf,"%s%s.sgf",gamedirs[gamen],b);
	 {FILE *wr = fopen(namebuf,"w");
	 if(wr!=0) 
		{ fprintf(wr,"%s\n",&message[idx+strlen(buf)]);
		  fclose(wr); 
		  //chmod(namebuf,0444);
		  if(logging>=log_connections) 
		  {
			if(logErrorFor(u))
			{
			  logEntry(&mainLog,"[%s] game %s saved\n",timestamp(),namebuf); 
			}
		  }
		}
		else if(logging>=log_errors)
			{
			if(logErrorFor(u))
			{
			logEntry(&mainLog,"[%s] failed (errno %d) to save game %s\n",timestamp(),ErrNo(),buf);
			  save_failed++;
			}
			}
	}}
	else
	{
		if(logErrorFor(u))
		{
		logEntry(&mainLog,"[%s] save game failed, game number %d out of range\n",timestamp(),gamen);
		save_failed++;
		}
	}
	}}
	games_completed++;
}

/*-----------------------------------------------------------------*/

void error2(char *inStr,char *inStr2)
{       /* reports fatal errors */
	fileBuffer *B = &mainLog;
	if(B->logStream==NULL) { B->logStream=stderr; }
	if(inStr==NULL) {inStr="";}
	if(inStr2==NULL) { inStr2=""; }

  logEntry(B,"[%s] %s %s.\n", timestamp(),inStr,inStr2);
#if HISTORY
  if(logging>=log_errors) { DumpHistory();}
#endif
  ExitWith(1);
}

void error1(char *inStr)
{
	error2(inStr,NULL);
}

/*-----------------------------------------------------------------*/

void mybcopy(char *theSrc, struct in_addr *theDst, int length) {

	char *src, *dst;
	int loopCounter;

	src = theSrc;
	dst = (char *)theDst;

	for (loopCounter=0;loopCounter<length;loopCounter++)
		dst[loopCounter] = src[loopCounter];
}

/*-----------------------------------------------------------------*/
//
// set nonblocking io
// the current belief is that this unnecessary when using "select", which we do
// but this is a belt-and-suspenders failsafe to be sure the main event loop
// never blocks.
//
BOOLEAN setNBIO(SOCKET sock)
{
#if WIN32
		unsigned int nb=1;
		if (ioctlsocket(sock,FIONBIO,&nb))
#else
		  // printf("non block flags %x %x %x\n",O_NDELAY,O_NONBLOCK,FNONBLOCK); they're all the same
		  if (fcntl(sock,F_SETFL,_POSIX_FSYNC|O_NDELAY)<0) 
#endif
		{ unusual_events++;
		  if(logging>=log_errors)
					{logEntry(&securityLog,"[%s] UNUSUAL: ioctl failed for %d\n",
							timestamp(),sock);
					}
			return(FALSE);
    } 
#if NODELAY
// turning on NODELAY had no noticable effect, but coincided with a rash
// of router crashes, possibly related to packet fragments.
		{char tv=1;
		if(setsockopt(sock,IPPROTO_TCP,TCP_NODELAY,&tv,sizeof(&tv))!=0)
		{	int err=ErrNo();
			if(logging>=log_errors)
			{ logEntry(&mainLog,"[%s] Error setting NODELAY, errno=%d",
							timestamp(),err);
			}
		}}
#endif
	return(TRUE);
}
//
// set blocking io
//
BOOLEAN setBIO(SOCKET sock)
{
#if WIN32
		unsigned int nb=0;
		if (ioctlsocket(sock,FIONBIO,&nb))
#else
		if (fcntl(sock,F_SETFL,_POSIX_FSYNC)<0) 
#endif
		{ unusual_events++;
		  if(logging>=log_errors)
					{logEntry(&securityLog,"[%s] UNUSUAL: ioctl failed for %d\n",
							timestamp(),sock);
					}
			return(FALSE);
    } 
	return(TRUE);
}


/** initialize the main listening socket.  If any significant modifications are ever made,
then the websocket implementation client_websocket_init should be modified too
*/
static void client_socket_init(int portNum) 
{
  int actCtr=0;
  struct hostent *hp;
#if WIN32
  Start_Winsock();
#endif
  {int err=gethostname(host_name,64);
   if(err<0)
   {
#if WIN32
    error("Failed gethostname",WSAGetLastError());
#else
    error("Failed gethostname",err);
#endif
  }}
#if !WIN32
  signal(SIGPIPE,SIG_IGN);
#endif
  logEntry(&mainLog,"Host is %s\n",host_name);
  hp = gethostbyname(host_name);
  if (!hp)
  {
#if WIN32
	  logEntry(&mainLog,"Failed gethostbyname %d\n",WSAGetLastError());
#else
	  error("Failed gethostbyname",h_errno);

#endif
  }else
  {
  if (hp->h_addrtype != AF_INET)
    error1("Address type not AF_INET");
  }


  serversd = socket(AF_INET,SOCK_STREAM,0);
  if (serversd == -1)
    error("Failed socket",ErrNo());
  if(!setNBIO(serversd))
    { error("Setting NBIO for accept socket failed",ErrNo());
    }
  {
  int loopCtr;
  for (loopCtr=0;loopCtr<8;loopCtr++) {
    iclient.sin_zero[loopCtr] = 0;
  }}

// this didn't work, reason unknown
// 	{char on=1;
//	 int ok;
//	 if ((ok=setsockopt(serversd, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on))) < 0) {
//     logEntry("[%s] Reuseaddr failed, err = %d\n",timestamp(),ok);
//	}}

#if 0
//
// this is removed because it is disasterous to linux 2.2
// the effect was that nonblocking close operations would block.
//
 { struct linger lin;
	lin.l_onoff=1;
	lin.l_linger=0;
	/* turn off lingering so restarting the server can be brisk */
	{int ok = setsockopt(serversd,SOL_SOCKET,SO_LINGER,(char *)&lin,sizeof(lin));
	 if(ok!=0)
	 {
	  logEntry("[%s] Disabling linger failed, err = %d\n",timestamp(),ok);
	 }
  }}
#endif
 
/*
  mybcopy(hp->h_addr,&client.sin_addr,hp->h_length);
*/

  iclient.sin_addr.s_addr = INADDR_ANY;
  // was Little_Long((IP1<<0)|(IP2<<8)|(IP3<<16)|(IP4<<24));
  iclient.sin_family = AF_INET;
  iclient.sin_port = htons((short)portNum);
  //
  // here's a story for the records.  Originally this code bound
  // a specific IP specified in the config file.  I think the motivation
  // was to not clog the port on shared server hosts.  This worked fine
  // until one day, boardspace.net started refusing all connections which
  // originated from localhost (ie; the "register user" "probe" and "score"
  // connections.   After a while this odd behavior went away.  My best
  // guess what had really gone on, was that "socket" started presenting
  // 127.0.0.1 instead of the IP due to some DNS glitch.  The evidence for
  // this is that normally telnet localhost xx and telnet boardspace.net 
  // both present 127.0.0.1 but telnet www.boardspace.net presents the
  // correct ip.
  //
  logEntry(&mainLog, "[%s] Server on %s, address %d.%d.%d.%d, using port %d.\n",
	  timestamp(), host_name, IP1, IP2, IP3, IP4, portNum);

  if (bind(serversd,(struct sockaddr *)&iclient,sizeof(iclient)) == -1)
    {error("Failed bind for main port",ErrNo());
    };
  if (listen(serversd,4) == -1)
    {error("Failed listen on main port",ErrNo());
    }
  {
  socklen_t csize=sizeof(iclient);
  getsockname(serversd,(struct sockaddr *)&iclient,&csize);

 
}
}
static char *plural(int n)
{
	return((n==1)?"":"s");
}

void printSessionHeader(FILE *tempfp,Session *s)
{	int numInSession = s->population;
	int session = s->sessionNUM;
	if((numInSession>0)||(session==0))
	  {char *pl = plural(numInSession);
	   if(session==0)
		{ fprintf(tempfp,"<p>%d player%s in the Lobby\n",numInSession,(numInSession==1)?"":"s");
		  if(maxlobby<numInSession) { maxlobby = numInSession; }
		}
	    else 
		{ int mode = s->sessionStates;
		  char *descr = ((mode>=0)&&(mode<nroomtypes)) ? roomtypes[mode] : "Nroom";
		  BOOLEAN isgame = ((*descr)=='G');
		  int gameid = (s->sessionGameID/1000)-1;	// this encoding of game type is 
				// arbitrary, but what is used by the boardspace applet.
		  if(*descr) { descr++; }
		  fprintf(tempfp,"<br>%d player%s in %s %d ",numInSession,pl,descr,session);
		  if(isgame && (gameid>=0)&&(gameid<ngametypes))
		  {	char *name = gametypes[gameid];
		  if(name && *name) { fprintf(tempfp,"(%s)",name); }
		  }
		}
	}
}


int isRealSocket(SOCKET s)
{
	return(s != -1);
}


/* @func
save a web formatted page summarizing the server's state and activity
<nl><nl>
this runs in an alternate thread with no interlocking, so needs to be very careful about what it believes
in the data structures it inspects
*/
void realSaveConfig() 
{
 	int localMaxSessions = maxSessions;
	SaveGameInfo *G = &saveGameInfo;

	FILE *tempfp = fopen(statusfile,"w");
	if (tempfp != NULL) 
	{
	int session;


	fprintf(tempfp,
		"<html><head><META HTTP-EQUIV=REFRESH CONTENT=300></head><title>Server Status on %s</title>"
		"<h1>Server Status on %s</h1>\n",
		host_name,host_name);
    
    for (session=0;session<=localMaxSessions;session++) 
	{ 
	  Session *s = &Sessions[session];
	  char playerbuf[LARGEBUFFER] = {0};
	  User *u = s->first_user;
	  int playerindex=0;
	  int someprint=0;
	  
	  printSessionHeader(tempfp,s);

	  while(u && (u->session==s))	
			// notice if we link into a new session
			// there ought to be a window where this can happen, if the 
			// user is deleted from a session and gets relinked into the idle
			// list.
	  { User *next = u->next_in_session;
	    if(isRealSocket(u->socket))
		{ char clientname[CLIENTPUBLICNAMESIZE+1];
		  {if(someprint) { playerindex+=lsprintf(sizeof(playerbuf)-playerindex,playerbuf+playerindex,", ");}
		  someprint=1;
		   /* copy then check, because it may change under us */
		   MyStrncpy(clientname,u->clientPublicName,sizeof(clientname));
		   if(clientname[0]==(char)0) { MyStrncpy(clientname,"(unknown)",sizeof(clientname)); }
		   playerindex+=lsprintf(sizeof(playerbuf)-playerindex,playerbuf+playerindex,"<a target=_NEW href=");
		   playerindex+=lsprintf(sizeof(playerbuf)-playerindex,playerbuf+playerindex,playerInfoUrl,clientname);
		   playerindex+=lsprintf(sizeof(playerbuf)-playerindex,playerbuf+playerindex,">%s</a>",clientname);
		  }
		}
	    if((playerindex+1000)>sizeof(playerbuf))
	      { // allow lots of slop, prevent buffer overflow
			fprintf(tempfp,"%s",playerbuf);
			playerbuf[0]=(char)0;
	        playerindex=0;
	      }
		u=next;
	  }
	// note it's not a good idea to check populations
	// becuase the sands could be shifting

	if(playerindex>0) { fprintf(tempfp,": %s",playerbuf); }
	if(session==0) { fprintf(tempfp,"<br>"); }
	}

	fprintf(tempfp,"<p>%d players ever connected\n<br>  %d games ever launched\n"
				"<br>%d games completed,  %d player games relaunched, %d robot games relaunched\n"
				"<br>%d games cached, %d allocated, %d saves pending (%d max), : %d saves completed %d problems\n",
					clients_connected,games_launched,
					games_completed,player_games_relaunched,robot_games_relaunched,
					games_cached,n_gamebuffers,
					THREAD_READ(G,n_dirty_gamebuffers),
					THREAD_READ(G,max_dirty_gamebuffers),
					THREAD_READ(G,n_dirty_writes),
					THREAD_READ(G,n_dirty_problems));
	fprintf(tempfp,"<br>%d clients zapped by errors, %d-%d+%d clients blocked\n",client_errors,
			blocked_clients,unblocked_clients,closed_blocked_clients);
	fprintf(tempfp,"<br>%d checksum errors %dk transactions %um in %um out \n",	// %u us unsigned desimal
		checksum_errors,transactions/1000,totalread/1000000,totalwrite/1000000);
	fprintf(tempfp,"<br>%d client messages %d unusual events %d robot irregularities\n",
			client_messages,unusual_events,robot_irregular);
	if(save_failed)
		{
		fprintf(tempfp,"<br>%d save game failed\n",save_failed);
		}
	fprintf(tempfp,"<br>%d users banned, %d additional connection attempts<br>",
			totalBanned,totalAttempts);
	if(isshutdown) { fprintf(tempfp,"<br><b>Server has been shut down by the supervisor\n</b><br>");}
	fprintf(tempfp,"%d/%d allocations, %dk allocated<br>",totalAllocations,allocations,(int)(allocatedSize/1024));
	{UPTIME now = Uptime();
	 int up = now - start_time;
	 int upminutes = up/60;
	 int uphours = upminutes/60;
	 int updays = uphours/24;
	 int idleminutes = idle_time/60;
	 int idlehours = idleminutes/60;
	 upminutes=upminutes%60;
	 uphours = uphours%24;
	 idleminutes=idleminutes%60;
	 fprintf(tempfp,"<br>Up for %d Days %2d:%02d  Max Players %d  Max Socks %d Idle %d:%02d\n",
					updays,uphours,upminutes,maxlobby,maxsockets,idlehours,idleminutes);
	}
	fprintf(tempfp,"\n</html>\n");
	fflush(tempfp);
    fclose(tempfp);
  }
}

static int configChange = FALSE;
#if WIN32


uintptr_t statusThreadId = 0;
uintptr_t saveGameThreadId = 0;
void statusthread(void  *args)
{
  //printf("In StatusThread\n");
	statusThreadRunning = TRUE;
	while (!killThreads)
	{
	 flushLog(&mainLog);			//send entries to the log file
	 flushLog(&chatLog);
	 flushLog(&securityLog);

	 if(configChange)
	 { configChange=FALSE;
	   realSaveConfig();
	   //fprintf(stdout,"saved status\n");
	 }
	 usleep(THREADSLEEP_US);
	}
	statusThreadRunning = FALSE;
}
#else
pthread_t statusThreadId;
pthread_t saveGameThreadId;
void *statusthread(void  *args);
void *statusthread(void  *args)
{
  //printf("In StatusThread\n");
	statusThreadRunning=TRUE;
	while (!killThreads)
	{
	 flushLog(&mainLog);			//send entries to the log file
	 flushLog(&chatLog);
	 flushLog(&securityLog);

	 if(configChange)
	 { configChange=FALSE;
	   realSaveConfig();
	   //fprintf(stdout,"saved status\n");
	 }
	usleep(THREADSLEEP_US);
	}
	statusThreadRunning=FALSE;
	return(NULL);
}
#endif

void saveConfig()
{	if(statusThreadRunning) { configChange++; }
	else { realSaveConfig(); }
}



/* return true if there are no real sockets in the session */
static int sessionIsEmpty(Session *sess)
{ 


	{
 	int nfound=0;
	int pop = sess->population;
	User *u = sess->first_user;
	while(u)
	{	assert(u->session==sess);
		if(isRealSocket(u->socket)) 
			{ return(0); 
			}
		u=u->next_in_session;
		nfound++;
		assert(nfound<=pop);
	}
    assert(nfound==pop);
	}

   return(1);
}

//
// close a socket and do any ancillary bookeeping
//
void simpleCloseSocket(SOCKET sock)
{
	if(isRealSocket(sock))
	{



		// added 2/22/2005 to attempt to prevent sockets from being reused with data in place
		 { struct linger lin;
			lin.l_onoff=1;
			lin.l_linger=0;
			/* turn off lingering so restarting the server can be brisk */
			{int ok = setsockopt(sock,SOL_SOCKET,SO_LINGER,(char *)&lin,sizeof(lin));
			 if(ok!=0)
			 {
			  logEntry(&mainLog,"[%s] Disabling linger failed, err = %d\n",timestamp(),ok);
			 }
		  }
		 }
	
	closesocket(sock);
	sockets_open--;
	if(sockets_open==0)
		{ start_idle_time=Uptime();
		}
	}
}

// open a nonblocking socket (used by proxy service to create it's sockets)
static SOCKET simpleOpenSocket()
{
	SOCKET sock = socket(AF_INET,SOCK_STREAM,0);
	if(sock!=INVALID_SOCKET)
	{
	sockets_open++;
	if(setNBIO(sock))	// this logs a complaint if it fails
		{
		return(sock);
		}
	simpleCloseSocket(sock);
	}
	return(INVALID_SOCKET);
}
static void simpleCloseClientOnly(User *u,char *cxt)
{	
#if PROXY
	if(u->proxyFor) 
		{
		// make sure we don't leave any dangling proxies
		// if this is a proxy user, remove it from the owning user
		removeProxyClient(u->proxyFor,u->userNUM); 
		}
	while(u->proxyOut)
	{	// if this user has proxies, close them
		closeProxyClient(u,u->proxyOut->userNUM);
	}
#endif
	if(isRealSocket(u->socket))
	{	BOOLEAN blocked = u->blocked;
		u->deadFromSession=u->session;
		if(blocked){u->blocked=FALSE; closed_blocked_clients++; }

		if(logging>=log_connections)
		{char *bmsg=(blocked?"blocked ":"");
		 logEntry(&mainLog,"[%s] %s %s Closing C%d (%s#%d) S%d Session %d, ping %s %d in %d out, %d sockets still in use\n",
			timestamp(),cxt,bmsg,u->userNUM,u->clientRealName,u->clientUid,
			u->socket,u->session->sessionNUM,
			u->pingStats,
			u->nread,u->nwrite,sockets_open);
		}

		//do not clear other information here, just close
		u->expectEof = TRUE;
		simpleCloseSocket(u->socket);
		u->socket = -1;


	}
}

static void update_user_timestamps();

/* just close a client */
void simpleCloseClient(User *u,char *cxt)
{	BOOLEAN isSoc = isRealSocket(u->socket);	// remember it was
	simpleCloseClientOnly(u,cxt);			// closes socket, so after this isSoc would be false
	update_user_timestamps();
	if(!u->isAPlayer)
	{/* not a player. Either a spectator or a lobby member, so we can really clear it */
		if(u->session) 
			{ removeUserFromSession(u); 
			  putUserInSession(u,IDLESESSION); 
			}
		ClearUser(u);
	}else 
	if(isSoc)
	{//closed a player, so preserve the connection info for reconnection
	 Session *s = u->session;
	 if(s) 
		{ // if the password is currently set, this is either a disconnection
		  // by one of the players during the setup phaze, or it is a disconneciton
		  // after a session is made private 
		  s->sessionURLs[0]=(char)0; // clear password
		}	
	 if(STRICMP("guest",u->clientRealName)==0)
	 {//preserve the identity of guests
	  char nam[CLIENTPUBLICNAMESIZE+13];
	  lsprintf(sizeof(nam),&nam[0],"(%s#%d)",&u->clientPublicName[0],u->clientUid);
	  MyStrncpy(u->clientPublicName,&nam[0],sizeof(u->clientPublicName));
	 }else
	 {
	 lsprintf(sizeof(u->clientPublicName),&u->clientPublicName[0],"(%s)",u->clientRealName);
	 }
	 u->checksums=FALSE;
	 u->sequencenumbers = FALSE;
	 u->use_rng_in = FALSE;
	 u->use_rng_out = FALSE;
	}
}


BOOLEAN logErrorFor(User *u)
{
	if(u->errorsLogged++ >= MAX_ERRORS_LOGGED)
	{	// this is inspired by an instance where the user got into a ping-pong
		// loop reporting errors in communication, which resulted in an infinite
		// number of errors, which filled up the disk and brought down the
		// server
		simpleCloseClient(u,"too many errors");
		return(FALSE);
	}
	return(TRUE);	//contine
}


static void clearSessionGame(Session *s)
{	GameBuffer *g = s->sessionGame;
	s->locker=NULL;
	if(g!=NULL)
	 {
	  s->sessionGame = NULL;	//disassociate this session from the game
	  g->ownerSession=NULL;		//so reconnection will be normal
	  s->locker = NULL;
	if(logging>=log_connections)
		{
			logEntry(&mainLog,"[%s] clearsession refcount %d down %s#%d\n",
					timestamp(),THREAD_READ(g,refCount),g->idString,g->uid);
		}
	  if(THREAD_DECREMENT(g,refCount)==1)
	  { // old count was 1, new count is zero

		freeGameBuffer(g);
	  }
	}
}


static void clearEmptySession(Session *s)
{	//clear a session known to be empty
	if(s!=IDLESESSION)
	{
	 int nfound=0;
	 int pop=s->population;
	 while(s->first_user)
	 { User *u = s->first_user;
	   removeUserFromSession(u);
	   putUserInSession(u,IDLESESSION);
	   nfound++;
	   assert(nfound<=pop);
	 }
	 assert(nfound==pop);
	 assert(s->population==0);
	

	// clear the hash
	s->sessionStateKey[0]=(char)0;
	if(s->sessionStateKeyIndex>=0) { sessionStateHash[s->sessionStateKeyIndex]=NULL; }
	s->sessionStateKeyIndex = -1;

	s->sessionURLs[0]=(char)0;
	s->sessionInfo[0]=(char)0;
	s->sessionHasGame=0;
	s->sessionKey=0;
	s->sessionIsPrivate=0;
	s->poisoned = FALSE;
	s->sessionScored=0;
	s->sessionFileWritten=0;
	s->sessionClear=0;
	s->population=0;
	s->first_user = NULL;
	s->sessionDescribe=TRUE;		//need to describe this
	clearSessionGame(s);
	}

}

// clear a session of players, making them be NOT players, leaving the spectators.
// this is punishment for apparent attempts at fraud by starting the same game
// in more than one session.
static void clearSessionPlayers(Session *S)
{
	User *u = S->first_user;
	int pop = S->population;
	int nfound=0;
	S->sessionIsPrivate=0;
	S->sessionURLs[0]=(char)0;
	while(u)
	{
	User *next = u->next_in_session;
	assert(u->session==S);
	if( u->isAPlayer || u->isARobot)
		{char xB[SMALLBUFSIZE];
		 u->clientSeat=-1;
		 u->clientOrder=-1;
		 u->clientRev=-1;
		 u->isAPlayer=FALSE;
		 u->isARobot=FALSE;
		 u->expectEof=TRUE;
		 if(isRealSocket(u->socket))
			{
			lsprintf(sizeof(xB),xB,ECHO_PLAYER_QUIT "%d %s",u->userNUM,"server");
			sendSingle(xB,u);
		    u->inputClosed = TRUE;
		 }
		 else	// not a real socket, get rid of it immediately
		 {
		  simpleCloseClient(u,"server");
		 }
		}
	u=next;
	nfound++;
	assert(nfound<=pop);
	}
	assert(nfound==pop);
	S->poisoned=TRUE;
	clearSessionGame(S);
	if(sessionIsEmpty(S)) { clearEmptySession(S); }

}
/* close all the clients in a session */
static void clearSession(Session *S)
{	
	if(S!=IDLESESSION)
	{
	User *u = S->first_user;
	int pop = S->population;
	int nfound=0;
	S->locker = NULL;
	while(u)
	{
	User *next = u->next_in_session;
	assert(u->session==S);
	u->clientSeat=-1;
	u->clientOrder=-1;
	u->clientRev = -1;
	u->isAPlayer = FALSE;
	u->isARobot = FALSE;
	simpleCloseClient(u,"clearsession");
	
	u=next;
	nfound++;
	assert(nfound<=pop);
	}
	assert(nfound==pop);
	
	clearEmptySession(S);
	}
}
/*--------------------------------------------------------------------*/
// grace should be grace_mandatory if the user is likely to try to reconnect.
// in that case, if he is alone in the room, we should preserve the
// game record for him.   If this "close" is due to a user request
// or otherwise the end of the interaction, then no grace time is needed.
// in any case, grace time is not a relevant concept unless there
// are no other players in the room.
//
typedef enum grace_state
{	grace_optional = 0,
	grace_mandatory = 1,
	grace_forbidden = 2} grace_state;

static void closeClient(User *U,char *cxt,grace_state grace)
{ if(isRealSocket(U->socket))
	{ Session *S = U->session;
	  simpleCloseClient(U,cxt);
	   if((S!=NULL) && (S!=WAITINGSESSION) && (S!=PROXYSESSION))
	   {
	    S->sessionDescribe=TRUE;		//need to describe this
	    S->sessionIsPrivate = 0;	//no longer private
		S->sessionURLs[0]=0;
	     if( (S!=LOBBYSESSION) &&  sessionIsEmpty(S)) /* session has no active clients */
			{
			 if((grace==grace_mandatory)
				 || 
				 ((grace!=grace_forbidden)
				   && (S->sessionHasGame || (S->sessionFileWritten!=0) )
				   && (strict_score!=2)	// if not scoring, no grace period is needed
				   && (S->sessionScored==0)))
				{	//this handles the race condition between closing off the session
					//and recording the final score, which is done by a separate process
					//if the scoring hasn't yet completed, we give the session a little 
					//bit of grace to allow the scoring process to catch up.
				if(S->sessionClear==0)
				{//sometimes we can try to clear a session more than once, it's ok, just be quiet about it
				S->sessionClear=1;	//give a little grace
				S->sessionTimes=Uptime();	//start the time now
				if(logging>=log_connections)
				 { logEntry(&mainLog,"[%s] granting grace time to session %d\n",timestamp(),S->sessionNUM);
				 }
				}
				}
			 else {
				clearSession(S);
			 }
	   }}
    saveConfig();
}}

static void putInDeadPlayers(Session *S,User* U)
{
	if (S != WAITINGSESSION)
	{
		User* du = DeadUsers;
		while (du)
		{
			if (U == du) { return; }
			du = du->nextDead;
		}
		// add to the dead list, make very sure we're not already on it.
		U->nextDead = DeadUsers;
		U->deadFromSession = S;
		DeadUsers = U;
	}
}


//
// special considerations in error closes; we want to notify
// the other members of the session, but need to avoid losses
// if there are more bodies on the floor.  So we build a list
// of the deceased, and send notifications as a separate phase.
//
void HandleWriteError(User *U,char *context)
{
	Session* S = U->session;
	client_errors++;

	if (S == WAITINGSESSION) {
		simpleCloseClientOnly(U, context);
		U->deadFromSession = NULL;
		removeUserFromSession(U);
		putUserInSession(U, IDLESESSION);
		return;
	}
	lsprintf(sizeof(U->reasonClosed), U->reasonClosed, context);
	U->expectEof = TRUE;
	closeClient(U, context, grace_optional);
	putInDeadPlayers(S, U);
}

// close this client and add them to the dead users lists
void HandleReadError(User *U,int err)
{
	Session* S = U->session;
	lsprintf(sizeof(U->reasonClosed), U->reasonClosed, "readerr %d", err);

	if (S == WAITINGSESSION)
	{
		U->deadFromSession = NULL;
		simpleCloseClientOnly(U, U->reasonClosed);
		removeUserFromSession(U);
		putUserInSession(U, IDLESESSION);
		return;
	}
	
	SOCKET sock = U->socket;
	U->expectEof = TRUE;
	closeClient(U, U->reasonClosed, grace_optional);
	putInDeadPlayers(S,U);
	
}


// try to notify members of the same session that someone left
void HandleDeadUsers()
{	char dB[SMALLBUFSIZE];
 	while(DeadUsers)
	{
	User *U = DeadUsers;
	Session *DS=U->deadFromSession;

	DeadUsers = U->nextDead;
	U->nextDead=NULL;
	U->deadFromSession=NULL;

    lsprintf(sizeof(dB),dB,ECHO_PLAYER_QUIT "%d %s",U->userNUM,U->reasonClosed);	
    sendMulti(dB,DS);
	}
}

void HandleKill(User *U)
{	
	lsprintf(U->tempBufSize,U->tempBufPtr,ECHO_PLAYER_QUIT "%d server",U->userNUM);	//yes, both are sent as 223, so the luser thinks
    doEchoAll(U,U->tempBufPtr,1);
}

/* hash a string */
unsigned int hashString(const char *str0)
{	//recommeded by http://www.cse.yorku.ca/~oz/hash.html
    unsigned int hash = 5381;
	unsigned char *str = (unsigned char *)str0;
	int c;
    while ((c = *str++)!=0)
        hash = ((hash << 5) + hash) + c; /* hash * 33 + c */
     return hash;
}

/* hash n chars of a string, matching the algorithm used by the java applet.
   this is signed, but numerically produces the same results as the unhashed.
   But - don't use this directly to index a hash table, because it may be negative!
*/
int hashNString(const char *str0,int n)
{	//recommeded by http://www.cse.yorku.ca/~oz/hash.html
	unsigned char *str = (unsigned char *)str0;
    int hash = 5381;
    int c;
    while (n-- > 0)
	{	c = *str++;
        hash = ((hash << 5) + hash) + c; /* hash * 33 + c */
	}
    return hash;
}

/*--------------------------------------------------------------------*/
/* checksum a string from the client, and if we expect it to be encrypted, decrypt it. 
   this is called to get the decription even if we're not interested in the checksum.
*/
int checksumInputString(User *u,char *instr)
{	char *str = instr;
	int sum=0;
	int i=0;
	int ch;
	while((ch=*str)!=0 && (ch!='\r') && (ch!='\n'))
	{	sum+=(ch)^i;
		if(u->use_rng_in)
		{	// this is the actual decryption/unobfuscation
			// characters are expected to be in 32-127 both before and after
			// leave spaces and control characters unchanged
			if(ch>' ')
			{
			int rnd = get_random_in(u) & 0x3f;
			int och = ((ch-' '-1)+(127-' ')-rnd)%(127-' ')+' '+1;
			u->rng_in_chars++;
			*str = (unsigned char)och;		// save back into the input buffer
			//printf("i %d %d %d\n",ch,rnd,och);
			}
		}
		i++;
		str++;
	}
	return(sum&0xffff);
}


#ifdef _DEBUG
static void check_count()
{	SaveGameInfo *G = &saveGameInfo;
	int nb=0;
	int ct = THREAD_READ(G,n_dirty_gamebuffers);
	GameBuffer *last = THREAD_READ(G,last_dirty_gamebuffer);
	GameBuffer *g = THREAD_READ(G,first_dirty_gamebuffer);
	while(g!=NULL) 
		{
		  GameBuffer *next = THREAD_READ(g,next_dirty_gamebuffer); 
		   nb++; 
		  if(next==NULL) { assert(g==last); }
		  g = next;
		}
	assert(nb==ct);
}
#else
#define check_count()
#endif
//
// mark as dirty so the sweeper will save it for crash/restart protection.
// only games that are hashed (ie; restartable) should be marked.  Games
// marked as dirty are queued at the end of a list headed by "first_dirty_gamebuffer"
// games that were already in the list are not promoted, so games start at the
// end of the queue and migrate gradually toward to the top.
//
static void markAsDirty(GameBuffer *g)
{	SaveGameInfo *G = &saveGameInfo;
	check_count();
	if(THREAD_READ(g,preserved)
		&& !THREAD_READ(g,dirty) 
		&& THREAD_READ(G,gameCacheRate)>0)
	{
	THREAD_WRITE(g,dirty,TRUE);
	if(logging>=log_all)
		{
			logEntry(&mainLog,"[%s] dirty refcount %d up %s#%d\n",
					timestamp(),THREAD_READ(g,refCount),g->idString,g->uid);
	}
	THREAD_INCREMENT(g,refCount);
	THREAD_INCREMENT(G,n_dirty_gamebuffers);
	THREAD_WRITE(g,next_dirty_gamebuffer,NULL);
	if(THREAD_READ(G,first_dirty_gamebuffer)==NULL) 
		{ THREAD_WRITE(G,first_dirty_gamebuffer,g); 
		}
		else
		{	THREAD_WRITE(THREAD_READ(G,last_dirty_gamebuffer),next_dirty_gamebuffer,g);
		}
	THREAD_WRITE(G,last_dirty_gamebuffer,g);
	{ int ndirt = THREAD_READ(G,n_dirty_gamebuffers);
 
	  if(ndirt>THREAD_READ(G,max_dirty_gamebuffers)) 
	  { THREAD_WRITE(G,max_dirty_gamebuffers,ndirt);
	  }
	}}
	check_count();
}
//
// this is called from the sweeper after the game is copied to the write queue
// normally this is also g==first_dirty_gamebuffer, but under extraordinary
// circumstances it may be some other game in the pending list
//
static void markAsClean(GameBuffer *g)
{	SaveGameInfo *G = &saveGameInfo;
	BOOLEAN found = FALSE;
	GameBuffer *game = THREAD_READ(G,first_dirty_gamebuffer);
	GameBuffer *prev = NULL;
	GameBuffer *gnext = THREAD_READ(g,next_dirty_gamebuffer);
	check_count();
	while(game!=NULL)
	{
		if(game==g)
		{
		THREAD_DECREMENT(G,n_dirty_gamebuffers);
		THREAD_WRITE(g,dirty,FALSE);
		found = TRUE;
		if(prev==NULL) 
			{ 
			THREAD_WRITE(G,first_dirty_gamebuffer,gnext);
			}
			else
			{	
			THREAD_WRITE(prev,next_dirty_gamebuffer,gnext);
			if(THREAD_READ(G,last_dirty_gamebuffer)==g)
				{ THREAD_WRITE(G,last_dirty_gamebuffer,prev);
				}
			}
		break;
		}
		else 
		{ prev = game;
		  game = THREAD_READ(game,next_dirty_gamebuffer); 
		}
	}
	if(!found)
	{	THREAD_DECREMENT(G,n_dirty_gamebuffers);
		THREAD_WRITE(g,dirty,FALSE);
		logEntry(&mainLog,"[%s] Emergency decrement of GameBuffer dirty count %s#%d\n",
				  timestamp(),
				  g->idString,
				  g->uid);
	}
	check_count();
 	if(logging>=log_all)
		{
			logEntry(&mainLog,"[%s] clean refcount %d down %s#%d\n",
					timestamp(),THREAD_READ(g,refCount),g->idString,g->uid);
	}
   if(THREAD_DECREMENT(g,refCount)==1)
	{	//old value 1, new value 0
		if(g->ownerSession==NULL) // really ought to be null here, unless the ref counts got messed up
		{ freeGameBuffer(g);	  // which happened with a bug that showed up when we fixed the "zombie game" bug
		}
	}
}
GameBuffer *hashGame(GameBuffer *game);


//
// remove this game from the hash table, so it can't be found by name
// again. This is done when the game is over.  It still stays in the
// session table until the session is cleared.
//
void unhashGame(GameBuffer *game)
{	if(game->hashed)
	{
	int idx = game->hashCode%HASHSIZE;
	GameBuffer *target;

	while ((target=GameHash[idx])!=NULL)
		{	
		target->hashed = FALSE;
		GameHash[idx]=NULL;			// remove from the hashtable
		games_cached--;

		if(target==game)
		 {
			if(logging>=log_connections)
			{
				logEntry(&mainLog,"[%s] removing cached game %s#%d, hashcode %d slot %d games_cached=%d\n",
							timestamp(),
							target->idString,
							target->uid,
							target->hashCode,
							idx,
							games_cached);
			}
		}
		else
		{
		// remove the target, and rehash all the games that might have been 
		// been forced down by a hash collision
		hashGame(target); 
		}
		idx++;
		if(idx>=HASHSIZE) {idx=0; }
		}
	game->hashed = FALSE;
	}
}

void unpreserveGame(GameBuffer *game)
{	// games can be unpreserved several times if clients are restarted
	// and therefore not sure what has already happened.  This led to
	// the "zombie game" bug, when when the zombies were fixed, the
	// absence of this thread_read caused a server crash because the
	// count became incorrect. [ddyer 7/2020]
	if(THREAD_READ(game,preserved))
	{
	markAsDirty(game);		// triggers deletion of the file
	THREAD_WRITE(game,preserved,FALSE);
	if(logging>=log_connections)
	{
		logEntry(&mainLog,"[%s] unpreserve refCount %d down %s#%d\n",
				timestamp(),THREAD_READ(game,refCount),game->idString,game->uid);
	}
	THREAD_DECREMENT(game,refCount);
	unhashGame(game);
	}
}
// protect the hash table by throwing things over the side.
// this ought to happen rarely or never
void gameHashCleanup()
{	while(games_cached>=MAXGAMES)
	{	GameBuffer *all = all_gamebuffers;
		GameBuffer *min_game = NULL;
		DAYS now = DaysSinceForever();
		DAYS min_time = 0;
		while(all!=NULL)
		{	
		GameBuffer *next = all->next;
		if(THREAD_READ(all,preserved) && (all->ownerSession==NULL))
		{
		if((min_game==NULL) && all->hashed)
			{ min_game=all; // always have something to kill
			  min_time = all->startTime; 
			}
		if((now - all->startTime) > GAMEEXPIRED)
			{				// try to recycle expired games promptly
			if(logging>=log_connections)
			{
			logEntry(&mainLog,"[%s] unpreserve expired game refCount %d %s#%d\n",
				timestamp(),THREAD_READ(all,refCount),all->idString,all->uid);
			}
			unpreserveGame(all);	// this makes the game dirty, and triggers deletion of the game file
			}
		else if(all->startTime<min_time)
			{
			min_time = all->startTime;
			min_game = all; 
			}
		}
		all = next;
		}
		// if we didn't find any expired games, pick on the oldest
		if(games_cached>=MAXGAMES && min_game!=NULL)
		{
			if(logging>=log_connections)
			{
			logEntry(&mainLog,"[%s] unpreserve random victim game refCount %d %s#%d\n",
				timestamp(),THREAD_READ(min_game,refCount),min_game->idString,min_game->uid);
			}			
			unpreserveGame(min_game);
		}
	}
}

//
// add a game to the hash table so it can be found again.
//
GameBuffer *hashGame(GameBuffer *game)
{	
	{
	int idx = game->hashCode%HASHSIZE;
	GameBuffer *target;
	while( (target=GameHash[idx])!=NULL)
		{idx++;
		 if(idx>=HASHSIZE) {idx=0; }
		}
	GameHash[idx]=game;
	game->hashed = TRUE;
	games_cached++;
	}

	return(game);
}
GameBuffer *preserveGame(GameBuffer *game)
{	if(!THREAD_READ(game,preserved))
	{
	gameHashCleanup();
	if(logging>=log_connections)
	{
		logEntry(&mainLog,"[%s] preserve refcount %d up %s#%d\n",
				timestamp(),THREAD_READ(game,refCount),game->idString,game->uid);
	}
	THREAD_INCREMENT(game,refCount);
	THREAD_WRITE(game,preserved,TRUE);
	hashGame(game);
	}
	return(game);
}
GameBuffer *findGame(unsigned int hash,char *str)
{	int idx = hash%HASHSIZE;
	GameBuffer *target=NULL;
	while( (target=GameHash[idx])!=NULL)
		{if(strcmp(str,target->idString)==0) 
			{DAYS now = DaysSinceForever();
			// GAMEEXPIRED is a configurable parameter, typically 60 days.
			// the idea is that robot games should stick around int enough
			// to discourage players from abandoning robot games they are losing
			 if((target->startTime > 0) && ((now - target->startTime) > GAMEEXPIRED))
				{ //remove expired game if we finally asked for it but haven't
				  // reused its slot yet.
				    markAsDirty(target);
					unpreserveGame(target);
					target=NULL;
				}
				 else { 
					 target->startTime = now;
				 }
				return(target);
			}
		 idx++;
		 if(idx>=HASHSIZE) {idx=0; }
		}
	return(NULL);
}

// 
// make a copy of the game and gamebuffer data which need to be saved.
// This is executed in the main thread.
//
void CopyGame(GameBuffer *dest,GameBuffer *src)
{	int srcSize = src->gamePtrSize;
	void *srcPtr = src->gamePtr;
	if(srcSize>dest->gamePtrSize)
	{	setGameBufferSize(dest,srcSize,FALSE);
	}
	assert(dest->gamePtrSize>=srcSize);
	MEMCPY(dest->gamePtr,srcPtr,srcSize);
	MyStrncpy(dest->idString,src->idString,sizeof(dest->idString));
	dest->magic = src->magic;
	dest->startTime = src->startTime;
	dest->hashed = src->hashed;
	THREAD_WRITE(dest,preserved,THREAD_READ(src,preserved));
	dest->uid = src->uid;		//not needed, just for info
}

//
// copy dirty games to the write buffer until we got them all, or until we have no more room.
// this is executed in the main thread.  Return TRUE if we make some progress.  We might
// fail to make progress if there are no games to save, or if the write pool is full.
//
// the write pool is emptied at a fixed maximum rate, so if the server becomes too
// busy, the length of time between saves will increase, but the load on the file
// system will not increase.
//
BOOLEAN saveDirtyGames()
{	SaveGameInfo *G = &saveGameInfo;
	BOOLEAN progress = FALSE;
	while(THREAD_READ(G,first_dirty_gamebuffer)!=NULL)
	{	// here we copy the game to the pending write queue, if there's room,  We serve the games
		// in oldest-first order so if the process becomes slow, the most critical will be saved first.
		// this task is performed in the main thread
		int next_index = THREAD_READ(G,GameBuffer_Write_Pool_Filler)+1;
		if(next_index==ARRAY_NUMBER_OF_ELEMENTS(THREAD_READ(G,GameBuffer_Write_Pool))) 
			{ next_index = 0; 
			}
		// if the filler would catch up to the emptier, give up for now
		if(next_index==THREAD_READ(G,GameBuffer_Write_Pool_Emptier)) { break; }	
		if(THREAD_READ(THREAD_READ(G,first_dirty_gamebuffer),dirty))
			{
			// note, under extreme circumstances, dirty games may be marked clean when they are
			// recycled.  This is better than picking the new identity of the game to be picked on
			// here, there's room in the buffer for another item
			CopyGame(&THREAD_READ(G,GameBuffer_Write_Pool)[THREAD_READ(G,GameBuffer_Write_Pool_Filler)],
					THREAD_READ(G,first_dirty_gamebuffer));
			THREAD_WRITE(G,GameBuffer_Write_Pool_Filler,next_index);
			}
		markAsClean(THREAD_READ(G,first_dirty_gamebuffer));
		progress = TRUE;
	}
	if(!use_threads)
	{ saveDirtyGamesNow(&saveGameInfo);
	}
	return(progress);
}


//
// gamebuffer management:  There are lots more game buffers than there
// are game sessions.   There's always a buffer associated with an active
// game, which is usually referred to using the name "*".  Except for unranked
// games, games are also referred to by unique name and located using a hash table.
// 
// games that have been abandoned are kept around for possible reuse as long
// as possible, but games which are hashed are reclaimed when reach their expiration
// time, or on an oldest-first basis.  
//
// the overall life of GameBuffers is sufficiently complex that they are reference
// counted and freed when the reference count becomes zero.  The places that are
// currently expected to maintain references are 
// 1) the game hashtable (for named games)
// 2) the "dirty" pool (for games being played, or games in the process of being finished or forgotten)
// 3) one or more sessions for which this game is the current game.
//

//
// allocate a new GameBuffer
//
//
GameBuffer *makeGameBuffer()
{	
	GameBuffer *rval = ALLOC(sizeof(GameBuffer));
	memset(rval,0,sizeof(*rval));
	THREAD_WRITE(rval,preserved,FALSE);
	THREAD_WRITE(rval,refCount,0);
	rval->hashed = FALSE;
	rval->magic = GAMEBUFMAGIC;
	rval->uid=GameUIDs++;
	rval->startTime = DaysSinceForever();
	rval->idString[0]=(char)0;
	rval->hashCode = 0;
	rval->gamePtr = NULL;
	rval->gamePtrSize = 0;
	rval->gamePtrOffset = 0;
	rval->next = all_gamebuffers;
	rval->prev = NULL;
	if(all_gamebuffers) { all_gamebuffers->prev = rval; }
	n_gamebuffers++;
	all_gamebuffers = rval;
	return(rval);
}
void freeGameBuffer(GameBuffer *rval)
{
	if(logging>=log_connections)
		{
			logEntry(&mainLog,"[%s] freegamebuffer refcount %d %s#%d freegamebuffer\n",
					timestamp(),THREAD_READ(rval,refCount),rval->idString,rval->uid);
		}

	assert(rval->magic==GAMEBUFMAGIC);
	assert(THREAD_READ(rval,preserved)==FALSE);
	assert(THREAD_READ(rval,dirty)==FALSE);
	assert(rval->ownerSession==NULL);
	rval->magic = 0xDeadbeef;
	
	{
	char *gp = rval->gamePtr;
	if(gp!=NULL)
	{	int sz = rval->gamePtrSize;
		rval->gamePtr=NULL;
		FREE(gp,sz);

	}}
	{
	GameBuffer *next = rval->next;
	GameBuffer *prev = rval->prev;
	rval->next = NULL;
	rval->prev = NULL;
	if(next) { next->prev = prev; }
	if(prev) { prev->next = next; }
	if(all_gamebuffers == rval) { all_gamebuffers=next; }
	}
	FREE(rval,sizeof(GameBuffer));
	n_gamebuffers--;
}


BOOLEAN reloadOneFile(char *name)
{	char *dot = strrchr(name,'.');
	BOOLEAN result = FALSE;
	if(dot && (STRICMP(dot+1,GAMESUFFIX)==0))
	{
	GameBuffer *g = makeGameBuffer();
	if(g)
	{
	FILE *fn = fopen(name,"rb");
	long fsize;
	fpos_t tpos;
	if(fn)
	  { 
	    fgetpos(fn,&tpos);
	    fseek(fn,0L,SEEK_END);
	    fsize = ftell(fn);
	    fsetpos(fn,&tpos);
	    setGameBufferSize(g,fsize,FALSE);
		{	
		size_t sz = fread(g->gamePtr,1,g->gamePtrSize,fn);
		if(sz>0)
		{	
			// determine if this is a legacy "oldgamebuffer" or a new one.
			OldGameBuffer *oldformat = (OldGameBuffer *)g->gamePtr;
			GameBuffer *newformat = (GameBuffer *)g->gamePtr;
			if(newformat->magic!=GAMEBUFMAGIC)
			{
			// old format
			MyStrncpy(g->idString,oldformat->idString,
				  (sz<sizeof(g->idString))?sz:sizeof(g->idString));
			g->magic = GAMEBUFMAGIC;
			g->hashCode = 0;
			if(sz>(((size_t)&((OldGameBuffer*)0)->startTime)+sizeof(oldformat->startTime)))
				{ g->startTime = oldformat->startTime; 
				}
			// copy into the same buffer, which is at at offset so there's
			// no problem with overlap
			MyStrncpy(g->gamePtr,oldformat->Game,g->gamePtrSize);
			}
			else
			{	// new format.  copy the game id and so on out of the buffer,
				// then move the actual game data down.
				MyStrncpy(g->idString,newformat->idString,sizeof(g->idString));
				g->magic = newformat->magic;
				g->startTime = newformat->startTime;
				// copy into the same buffer, which is at at offset so there's
				// no problem with overlap
				MyStrncpy(g->gamePtr,(char *)&newformat->gamePtr,g->gamePtrSize);
			}

			THREAD_WRITE(g,dirty,FALSE);
			THREAD_WRITE(g,next_dirty_gamebuffer,NULL);
			THREAD_WRITE(g,preserved,FALSE);
			g->ownerSession = NULL;
			g->uid = GameUIDs++;
			g->hashCode=hashString(toLowercase(g->idString));	// allow the hash algorithm to change
			preserveGame(g);
			result = TRUE;
		}}
	  fclose(fn);

#if 0
	// temporary correction factor.  For a long time, games were stored with "uptime"
	// as their timestamp.  Any times that aren't sensible are reset to now.  The day
	// this was discovered and corrected is Feb 19 2014, which is effectively "Day 0"
	// for sensible results.
    // the effect of stating both servers with this hack in place
    // will be to reset the clock on all the cached games and start
    // them again on their 60 day countdown.
	{
#define DAYZERO 16120	// Feb 19 2014
	DAYS now = DaysSinceForever();
	if((g->startTime>(DAYZERO+365))||(g->startTime<DAYZERO))
		{
		g->startTime = now;
		markAsDirty(g);
		}
	}
#endif
	}

	  }
	}
	return(result);
}

void reloadGames()
{	SaveGameInfo *G = &saveGameInfo;
	if(THREAD_READ(G,gameCacheDir)
		&& *THREAD_READ(G,gameCacheDir))
	{
	int reloaded = 0;
#if WIN32
	WIN32_FIND_DATAA find_data;
	char gameDir[SMALLBUFSIZE];
	// gameDir should end with a backslash
	lsprintf(sizeof(gameDir),gameDir,"%s*.*",THREAD_READ(G,gameCacheDir));
	{
	HANDLE val = FindFirstFileA(gameDir,&find_data);
	if(val != INVALID_HANDLE_VALUE)
	{
	while(FindNextFileA(val,&find_data))
		{
		char fname[SMALLBUFSIZE];
		char fullpath[SMALLBUFSIZE];
		// convert wide chars to narrow chars
		//wcstombs(fname, &find_data.cFileName, sizeof(fname));
		strncpy(fname, find_data.cFileName, SMALLBUFSIZE);
		lsprintf(sizeof(fullpath),fullpath,"%s%s",THREAD_READ(G,gameCacheDir), fname);
		if(reloadOneFile(fullpath)) { reloaded++; }
		}
	FindClose(val);
	}
	}
#else
	DIR *dir = opendir(THREAD_READ(G,gameCacheDir)); 
	if(dir)
	 {
	 struct dirent *nextdir = NULL;
	 while((nextdir = readdir(dir))!=NULL) // read directory stream
	 {
	 char *dname = nextdir->d_name;
	 char fullpath[SMALLBUFSIZE];
	 lsprintf(sizeof(fullpath),fullpath,"%s%s",THREAD_READ(G,gameCacheDir),dname);
         if(reloadOneFile(fullpath)) {  reloaded++; }
	 }
	 closedir(dir);
	 }
#endif
	logEntry(&mainLog,"[%s] %d games reloaded from %s\n",
			timestamp(),
			reloaded,
			THREAD_READ(G,gameCacheDir));
	}
}
static GameBuffer GameBuffer_Write_Pool[GAMEBUFFER_WRITE_POOL_SIZE];
static int GameBuffer_Write_Pool_Filler;
static int GameBuffer_Write_Pool_Empter;


// watch for the common fraud of restoring the same game
// from multiple sessions, trying different moves in each session.
void ZapDuplicateSession(GameBuffer *g,User *u)
{	Session *owner = g->ownerSession;
	if((owner!=NULL)
		&& (owner!=u->session))
		{ //zap the old session.  In this case, the user is trying to open two sessions
			//on one game.  Maybe the first session is damaged somehow, or maybe he's trying 
			//to cheat.
			 if(logging>=log_connections)
				{logEntry(&mainLog,"[%s] irregular re-record cached game %s already busy in session %d zap\n",
					timestamp(),
					&g->idString[0],owner->sessionNUM);
				}

			robot_irregular++;
			clearSessionPlayers(owner);
		}

	{
	Session *s = u->session;
	GameBuffer *oldGame = s->sessionGame;
	if(oldGame==g) {	/* no effect on reference counts */ }
	else
	{
	if(oldGame!=NULL)
		{	// some other game was referenced.
		if(logging>=log_connections)
		{
			logEntry(&mainLog,"[%s] session zap refcount %d down %s#%d\n",
				timestamp(),THREAD_READ(oldGame,refCount),oldGame->idString,oldGame->uid);
		}
		oldGame->ownerSession = NULL;
		s->sessionGame = NULL;
		if(THREAD_DECREMENT(oldGame,refCount)==1)
		{	freeGameBuffer(oldGame);
		}
	 }
		
	if(logging>=log_connections)
		{
			logEntry(&mainLog,"[%s] session refcount %d up %s#%d\n",
				timestamp(),THREAD_READ(g,refCount),g->idString,g->uid);
		}
	THREAD_INCREMENT(g,refCount);
	s->sessionGame = g;
	g->ownerSession = s;
	}}


}


//
// find a game with name "id".  The special name "*" means
// the game associated with the current session.
//
GameBuffer *findNamedGame(User *u,char *id)
{	if(strcmp("*",id)==0)
	{	Session *s = u->session;		// allow a game id of * to mean "the game associated with this session"
		if(s && (s->sessionNUM!=0))
		{
		// allocate an unhashed game for this session only
		if(s->sessionGame==NULL)
			{ GameBuffer *g = makeGameBuffer();
			  if(logging>=log_connections)
			  {
				  logEntry(&mainLog,"[%s] find refcount %d up %s#%d\n",
					timestamp(),THREAD_READ(g,refCount),g->idString,g->uid);
			  }
			  THREAD_INCREMENT(g,refCount);
			  s->sessionGame=g; 
			  g->ownerSession = s;
			}
		return(s->sessionGame);
		}
		return(NULL);
	}
	else
	{
	unsigned int hashCode = hashString(toLowercase(id));
	return(findGame(hashCode,id));
	}
}


//
// record a complete game record using name id for user u.
//
GameBuffer *recordGame(User *u,char *id,const char *msg)
{	GameBuffer *g = findNamedGame(u,id);
	if(g==NULL)
		{	g = makeGameBuffer();
			if(STRICMP(id,"*")!=0)
			{
			g->hashCode=hashString(toLowercase(id));
			MyStrncpy(g->idString,id,sizeof(g->idString));
			preserveGame(g);
			}
			else
			{
			g->hashCode = 0;
			}
			
	}

	ZapDuplicateSession(g,u); 

	{ int msglen = (int)(strlen(msg)+1);	 // 1 for the null
	  if(msglen>=g->gamePtrSize) 
	  {	 setGameBufferSize(g,msglen,FALSE);
	  }
	  assert(g->gamePtrSize>msglen);
	  MyStrncpy(g->gamePtr,msg,msglen);
	  g->gamePtrOffset = msglen;
	  markAsDirty(g);
	}
	return(g);
}

//
// forget about a game.  It stays in the session
// until the session is cleared, but is removed from
// the hash immediately, so it can't be found again.
//
int removeNamedGame(User *u,char *id)
{	GameBuffer *g = findNamedGame(u,id);
	if(g!=NULL)
		{ int val = g->uid;
			logEntry(&mainLog,"[%s] removegame '%s' found C%d (%s#%d) S%d\n",timestamp(),id,
						u->userNUM,u->clientRealName,u->clientUid,u->socket);
			unpreserveGame(g);
			return(val);
		}
	else {
		logEntry(&mainLog,"[%s] removegame '%s' not found C%d (%s#%d) S%d\n",timestamp(),id,
			u->userNUM,u->clientRealName,u->clientUid,u->socket);
	}
	return(0);
}

// re-record the trailing end of a game.  Paranoia dictates that we
// chechsum the unchanged part, and insist that the transmitted checksum
// matches the stored data.  This is a minimal assurance that various
// distributed processes agree with the untransmitted contents of the buffer
// we're appending to.
GameBuffer *reRecordGame(User *u,char *id,int offset,int checksum,const char *msg)
{	
	if(u->session->sessionHasGame && !u->isAPlayer)
	{
		if(logErrorFor(u))
		{
		unusual_events++;
		logEntry(&mainLog,"[%s] Unusual: Spectator C%d (%s#%d) S%d  session %d tried to re record Game %s\n",
			 timestamp(),
			 u->userNUM,u->clientRealName,u->clientUid,u->socket,							
			 u->session->sessionNUM,
			 id);
		}
		return(NULL);
	}

	if(offset==0)
	{	// first time, just record
		return(recordGame(u,id,msg));
	}
	else
	{
	// look up the saved game
	GameBuffer *g = findNamedGame(u,id);


	if(g==NULL) 
		{ 
		if(logErrorFor(u))
		{
		unusual_events++;
		logEntry(&mainLog,"[%s] Unusual: C%d (%s#%d) S%d  session %d Re Record %s is missing off %d check %d",
			 timestamp(),
			 u->userNUM,u->clientRealName,u->clientUid,u->socket,							
			 u->session->sessionNUM,
			 id,
			 offset,checksum);
		}
		return(NULL);
	
		}

	ZapDuplicateSession(g,u);

	if(offset>g->gamePtrOffset)
	{	unusual_events++;
		logEntry(&mainLog,"[%s] Unusual: C%d (%s#%d) S%d  session %d  Game %s offset %d is greater than stored offset %d\ncontents: %s\nadd: %s\n",
			 timestamp(),
			 u->userNUM,u->clientRealName,u->clientUid,u->socket,							
			 u->session->sessionNUM,
			 id,
			 offset,g->gamePtrOffset,
			 g->gamePtr,
			 msg);
	}
	else
	// verify the hash for the saved game
	if(offset!=-1)
	{
	int hashval = hashNString(g->gamePtr,offset);
	if(hashval==checksum)
		{
		}
		else
		{	
		if(logErrorFor(u))
		{
		unusual_events++;
		logEntry(&mainLog,"[%s] Unusual: C%d (%s#%d) S%d  session %d  Game %s appended checksum incorrect; off %d check %d is %d\ncontents: %s\nadd: %s\n",
			 timestamp(),
			 u->userNUM,u->clientRealName,u->clientUid,u->socket,							
			 u->session->sessionNUM,
			 id,
			 offset,checksum,hashval,
			 g->gamePtr,
			 msg);
		}
		{
		char xB[SMALLBUFSIZE];
		lsprintf(sizeof(xB),xB,ECHO_APPEND_GAME " 1 ");	// code 1 is bad checksum resend
		sendSingle(xB,u);
		return(NULL);
		}}
	}

	// verify that there's space for the addition
	{
	size_t newpart = strlen(msg)+1;
	int whole = (int)(newpart+offset);
	{ 
	  if(whole>=g->gamePtrSize)
	  {	  setGameBufferSize(g,whole,TRUE);
	  }
	  assert(whole < g->gamePtrSize);
	  // offset -1 is special indicating just append
	  int off = offset == -1 ? 0 : offset;
	  MyStrncpy(g->gamePtr+off,msg,newpart);
	  g->gamePtrOffset = (int)(offset+newpart-1);
	  }
	  markAsDirty(g);
	}
	return(g);
	}
}
//
// get the size of this string after escaping, including the null
//
static int escapedStringSize(char *str0)
{	unsigned char *str = (unsigned char *)str0;
	int i=1;
	int ch;
	if(str)
	{
	while((ch=*str++)!=0)
	{
		if(ch=='\\') 
			{ i+=2;
			}	// double up
		else if ((ch<' ') || (ch>129))
			{	// encode as three char sequence
				i+= 4;
			}
		else 
			{ 
			  i++;
			}
	}}
	return(i);	// we added one char using *out++
}

static char *skipToken(char *src)
{	while(*src<=' ') { src++; }	// skip white space
	while(*src>' ') { src++; }	// skip nonwhite space
	return(src);
}
// escape and checksum a string.  Terminate the escaped and checksummed string with a \n\0
static int escapeAndFilterString(int outsize,char *out,char *str0,BOOLEAN filter)
{	unsigned char *str = (unsigned char *)str0;
	int i=0;
	int ch;
	int size3 = outsize-3;
	BOOLEAN comma = FALSE;
	if(str)
	{
	while(((ch=*str++)!=0) && (i<size3))
	{	if(filter && comma && (ch=='+'))
			{
			str = skipToken(str);	// skip the rest of the + token
			str = skipToken(str);	// skip the payload token
			ch = *str;
			}
		else if(ch=='\\') 
			{ 
			  out[i++] = (char)ch;
  			  out[i++] = (char)ch;
			}	// double up
		else if ((ch<' ') || (ch>129))
			{	// encode as three char sequence
			  char hundreds = '0'+ch/100;
			  char tens = '0'+(ch%100)/10;
			  char ones = '0'+ch%10;
			  out[i++] = '\\';
			  out[i++] = hundreds;
			  out[i++] = tens;
			  out[i++] = ones;
			}
		else 
			{ if(ch>' ') { comma = FALSE; }
			  out[i++] = (char)ch;
			}

		if(ch==',')
			{ comma = TRUE; 
			}
	}
	}
	out[i]=(char)0;		//terminate
	return(i+1);	// we added one char using *out++
}
static int escapeString(int outsize,char *out,char *str0)
{	return escapeAndFilterString(outsize,out,str0,FALSE);
}
//
// escape and checksum a string.  Terminate the escaped and checksummed string with a \n\0
// if encryption is in effect, this also encrypts as it copies.
//
static int localStringCopy(User *u,size_t outsize,char *out,char *inseq,char *instr,int *checksum)
{	char *str = instr;
	char *seq = inseq;
	int sum=0;
	int i=0;
	int ch;
	size_t size3 = outsize-3;

	while(((ch=*seq++)!=0) && (i<size3))
			{ 
			  if(u->use_rng_out)
			  {	// this is the actual encryption/obfuscation for output
			    // characters are 0-127 both before and after.  Leave 
			    // spaces and control characters unchanged.
			    if(ch>' ') 
				{	int rnd = get_random_out(u)&0x3f;
				    int nv = ((ch-' '-1)+rnd)%(127-' ')+' '+1;
					//printf("o: %d %d %d\n",ch,rnd,nv);
					u->rng_out_chars++;
					ch = nv;
					
				}
			  }
			  sum+=(ch)^i;
			  out[i++] = (char)ch;
			}

	while(((ch=*str++)!=0) && (i<size3))
			{ 
			  if(u->use_rng_out)
			  {	// this is the actual encryption/obfuscation for output
			    // characters are 0-127 both before and after.  Leave 
			    // spaces and control characters unchanged.
			    if(ch>' ') 
				{	int rnd = get_random_out(u)&0x3f;
				    int nv = ((ch-' '-1)+rnd)%(127-' ')+' '+1;
					//printf("o: %d %d %d\n",ch,rnd,nv);
					u->rng_out_chars++;
					ch = nv;
					
				}
			  }
			  sum+=(ch)^i;
			  out[i++] = (char)ch;
			}
	out[i++]='\n';
	out[i]=(char)0;		//terminate
	*checksum = sum&0xffff;
	return(i);	
}


/* add a escape and add a checksum to a string that's otherwise ready to go. 
16 bits of checksum are added to 4 digits AAAA.

  checksums are added to sockets that are supplying them.
*/
char *addChecksum(User *u,char *inStr,int *outsize)
{	static char *tempFB = NULL;
	static size_t currentSize = 0;
	size_t maxsize = strlen(inStr)+BUFFER_ALLOC_STEP;
	char seq[30]={0};

	if(require_seq || u->sequencenumbers)
		{ BOOLEAN isScoreCheck = strncmp(ECHO_CHECK_SCORE,inStr,strlen(ECHO_CHECK_SCORE))==0;
			// two bits of obscurity about the score check.  First, it has a leading space.
			// the leading space has been eliminated from all other communication.  Second,
			// it never has a sequence number.  This check prevents the sequence number.
		  if(!isScoreCheck)
		  {
		  lsprintf(sizeof(seq),seq,"x%d ",u->rng_out_seq);
		  u->rng_out_seq++;
		  }
		}

	if(currentSize<maxsize)
	{	FREE(tempFB,currentSize);
		tempFB = ALLOC(maxsize);
		currentSize = maxsize;
	}
	if(u->checksums)
	{	
	int len0 = lsprintf(currentSize,tempFB,"501 AAAA ");
	{
	// note, escapeString can't be used directly here because some pre-escaped
	// strings might be embedded in the main string to be transmitted.
	int sum=0;
	int len = localStringCopy(u,currentSize-len0,tempFB+len0,seq,inStr,&sum);
	char sum1 = (sum>>12)&0xf;
	char sum2 = (sum>>8)&0xf;
	char sum3 = (sum>>4)&0xf;
	char sum4 = (sum>>0)&0xf;
	tempFB[4]+=sum1;
	tempFB[5]+=sum2;
	tempFB[6]+=sum3;
	tempFB[7]+=sum4;
	*outsize = len0+len;
	}
  }else
 {/* just add a leading space */
	int checksum = 0;
	*outsize = localStringCopy(u,currentSize,tempFB,seq,inStr,&checksum);
  }
  return(tempFB);
}



/* start output for a user, return positive if some was sent, negative if error
   or zero if not sent, but not error either */
int startOutput(User *u)
{	int from = u->outbuf_take_index;
	int to = u->outbuf_put_index;
	int err=0;
	if(to<from) 
	{ 
		to=u->outbufSize;		//wrap around the buf
		if(logging>=log_all) 
			{logEntry(&mainLog,"[%s] start wrap for C%d (%s#%d) S%d %d\n",
				timestamp(),u->userNUM,u->clientRealName,u->clientUid,u->socket,to-from); 
			}
	}
	if(from==to)
	{ if(logging>=log_all)
		{logEntry(&mainLog,"[%s] start but no data for C%d (%s#%d) S%d %d\n",
				timestamp(),u->userNUM,u->clientRealName,u->clientUid,u->socket,to-from); 
		}
	}
	else if(from!=to)
	{ 
	  int slen = to-from;
	  SOCKET sock = u->socket;
	  int len = err = isRealSocket(sock)
			?u->websocket
				? websocketSend(u, u->outbufPtr + from, slen)
				: send(sock,u->outbufPtr+from,slen,0)
		    :-2;
	  if(slen==u->outbufSize)
	  {	// improbable, full buffer
		logEntry(&mainLog,"[%s] full buffer for  C%d (%s#%d) S%d %d\n",
			timestamp(),u->userNUM,u->clientRealName,u->clientUid,u->socket,to-from);
	  }
	  assert((from+slen)<=u->outbufSize);
	  if(len>=0) 
	  { BOOLEAN blocked=u->blocked;
		u->outbuf_take_index+=len; 
		  u->totalwrite+=len;
		  totalwrite+=len;
		  if(len==slen) 
		  { if (blocked) { unblocked_clients++; u->blocked=FALSE; } 
		    if(logging>=log_all)
				{logEntry(&mainLog,"[%s] send %d (all) for C%d (%s#%d) S%d %d\n",
					timestamp(),len,u->userNUM,u->clientRealName,u->clientUid,u->socket,to-from); 
					}
		  }
		  else
		  {
		  writeBreak++;
		  if( !blocked)
			  { logEntry(&mainLog,"[%s] blocked write for C%d (%s#%d) S%d, %d<%d\n",
					timestamp(),u->userNUM,u->clientRealName,u->clientUid,u->socket,len,slen); 
				blocked_clients++;
				u->blocked=TRUE;
			  }
		  }
		  if( u->outbuf_take_index==u->outbufSize)
		  {	u->outbuf_take_index = 0;
			if(u->outbuf_put_index==u->outbufSize)
			{ u->outbuf_put_index = 0;
			}
		  }
	  }
	  else if(len==-1) 
	  { int errn=ErrNo();
	  err=len;
	  switch (errn)
	  { 
	   default:
		if(logging>=log_errors)
		{ 
		logEntry(&mainLog,"[%s] write err C%d (%s#%d) S%d  errno=%d\n",
					timestamp(),u->userNUM,u->clientRealName,u->clientUid,u->socket,errn);
		}
		break;
	   case EWOULDBLOCK:
		err=0; /* will try again */ 
	    writeBreak++;
		if(!u->blocked) 
		{ u->blocked=TRUE;
		  blocked_clients++; 
		  if(logging>=log_errors)
			{ 
			logEntry(&mainLog,"[%s] blocked C%d (%s#%d) S%d , %d pending\n",
					timestamp(),u->userNUM,u->clientRealName,u->clientUid,u->socket,output_pending(u));
			}
		}
	  }}
	  else { err=len; }
	}

	if (u->outputClosed && (u->outbuf_put_index == u->outbuf_take_index))
	{
		closeClient(u,"internal close request",grace_forbidden);
	}
	return(err);
}


//
//copy from buf to the user's output buffer, if necessary
//copying around the end of the buffer.  Watch out for
//overrunning the get pointer.
//
// return 0 if ok
//
int doSplitCopy(User *u,char *buf,int len)
{
	int available= u->outbufSize-u->outbuf_put_index;
	int part1_len = (available<len)?available:len;
	int part2_len = len-part1_len;
	int part1_end = u->outbuf_put_index+part1_len;
	if(logging>=log_all) 
		{logEntry(&mainLog,"[%s] split copy %d,%d\n",timestamp(),part1_len,part2_len); 
		}
	if(part1_len>0) 
	{ if( (u->outbuf_put_index<u->outbuf_take_index)
			&& (u->outbuf_take_index<=part1_end))
		{ return(ERR_NOLISTEN); //overrun
		}
		else
		{ 
		  assert(part1_end<=u->outbufSize);	
		  MEMCPY(u->outbufPtr+u->outbuf_put_index,buf,part1_len); 
		  u->outbuf_put_index=part1_end;
		  if(part1_end==u->outbufSize)
			{ //exactly filled the buffer
			  u->outbuf_put_index=0;
			}
		}
	}
	if(part2_len>0) 
		{
		 if(u->outbuf_take_index<=part2_len)	// need more buffer space?
		 {	// not enough room overall, so increase buffer size and add to end
		    int part2_start = u->outbufSize;
			int part2_end = part2_start + part2_len;	// new end point
			increaseOutputBufferSize(u,part2_len);				// get at least that much space
			assert(part2_end <= u->outbufSize);				// make sure we got it.
			MEMCPY(u->outbufPtr+part2_start,buf+part1_len,part2_len);	// copy
			u->outbuf_put_index=part2_end;
			if(part2_end==u->outbufSize)
			{ //exactly filled the buffer
			  u->outbuf_put_index=0;
			}

		 }
		 else
			{ //copying around the corner
			  assert(part2_len<u->outbufSize);	
			  MEMCPY(u->outbufPtr+0,buf+part1_len,part2_len);
			  u->outbuf_put_index=part2_len;
			}
		}
	return(0);
}

int restartOutput(User *u)
{   int writeResult = 0;
    if(isRealSocket(u->socket))
	{
	BOOLEAN blocked = u->blocked;
	writeResult = startOutput(u);
	if (writeResult < 0)
	{
		if (!u->expectEof)
		{
			u->wasZapped = TRUE;
			if (logging >= log_errors)
			{
				logEntry(&mainLog, "[%s] Zap At %d:(restart) Closing C%d (%s#%d) S%d  due to error %d, errno=%d, pending=%d\n",
					timestamp(), __LINE__, u->userNUM, u->clientRealName, u->clientUid, u->socket, writeResult, ErrNo(), output_pending(u));
			}
		}
		HandleWriteError(u, "Zap restart output");
	}
	else if(blocked)
	{
	 if(logging>=log_errors)
		{logEntry(&mainLog,"[%s] restarting C%d (%s#%d) S%d sent %d rem %d\n",
		timestamp(),u->userNUM,u->clientRealName,u->clientUid,u->socket,writeResult,output_pending(u));
		}
	}
	}
    return writeResult;
}
/* local send */
int lsend0(User *u,char *tempb,int len2)
{	/* add a leading space to all messages, as a hack to avoid the "lost character" bug */
	
	assert((u->topGuard==0xDEADBEEF)&&(u->bottomGuard==0xDEADBEEF)&&(u->midGuard==0xDEADBEEF));

	{
	int copyend = u->outbuf_put_index+len2;
	if(copyend<=u->outbufSize)
	{ //simple way, store directly
	 char *putat = u->outbufPtr+u->outbuf_put_index;
	 if((u->outbuf_put_index < u->outbuf_take_index)
		 && (u->outbuf_take_index <= (copyend+1) )) 
		{ return(ERR_NOLISTEN); //overrun. The +1 is because add checksum adds a null to the string
		}
	 assert(copyend<=u->outbufSize);	

	 MEMCPY(u->outbufPtr+u->outbuf_put_index,tempb,len2); 
	 u->outbuf_put_index=copyend;
	}
	else
	{//split across the buffer end
	{int err=doSplitCopy(u,tempb,len2);
	 if(err!=0) { return(err); }
	}}}
	//return(send(Users[index].socket,&lbuf[0],len,flags));
	u->nwrite++;

	assert((u->topGuard==0xDEADBEEF)&&(u->bottomGuard==0xDEADBEEF)&&(u->midGuard==0xDEADBEEF));

	if(u->blocked) 
		{ return(0); 
		}
	else return(startOutput(u));
}

int lsend(User *u,char *buffer)
{	int len1 = 0;
	char *tempb = addChecksum(u,buffer,&len1);

	int fput = u->outbuf_put_index;
	int ftake = u->outbuf_take_index;
	int res = lsend0(u,tempb,len1);
#if HISTORY
	// record the pre-encryption data
	recordInHistory("out",u->socket,buffer,len1);
#endif
	if(logging>=log_all)
	{
	int lput = u->outbuf_put_index;
	int ltake = u->outbuf_take_index;

	logEntry(&mainLog,"[%s] sending C%d (%s#%d) S%d: %d=(%d,%d)(%d,%d) %s\n",
		timestamp(),u->userNUM,u->clientRealName,u->clientUid,u->socket,
		res,fput,ftake,lput,ltake,
		buffer);
	}
	return(res);
}

/* find a vacant slot in a session */
static User *findAslot(Session *s)
{	
	User *U = IDLESESSION->first_user;
	if(U)
		{
		 removeUserFromSession(U);
		 ClearUser(U);
		 putUserInSession(U,s);
		 return(U);
		}
	if(logging>=log_errors)
	{
	unusual_events++;
	 logEntry(&securityLog,"[%s] UNUSUAL: couldn't find a user, Nleft=%d\n",	timestamp(),IDLESESSION->population);
	
	}

	return(NULL);
}
/*
return the number of existing connections in the session that match the ip
or -1 if the limit is exceeded.
*/
static int checkSession(unsigned int ip, Session* S, int max)
{


	User* U = S->first_user;
	int pop = S->population;
	int nfound = 0;
	int nhits = 0;
	while (U)
	{
		User* next = U->next_in_session;
		assert(U->session == S);
		if (U->ip == ip)
		{
			nhits++;
			if (nhits > max)
			{	// not session 0, but all others
				char* stamp = timestamp();
				int b4 = (ip >> 0) & 0xff;
				int b3 = (ip >> 8) & 0xff;
				int b2 = (ip >> 16) & 0xff;
				int b1 = (ip >> 24) & 0xff;
				unusual_events++;
				if (logging >= log_errors)
				{
					logEntry(&mainLog, "[%s] UNUSUAL: ip %d.%d.%d.%d rejected (too many connections in Session %d from ip)\n",
						stamp, b1, b2, b3, b4, S->sessionNUM);

				}
				logEntry(&securityLog, "[%s] UNUSUAL: ip %d.%d.%d.%d rejected (too many connections in Session %d from ip)\n",
					stamp, b1, b2, b3, b4, S->sessionNUM);

				return(-1);
			}



		}
		U = next;
		nfound++;
		assert(nfound <= pop);
	}
	assert(nfound == pop);
	return nfound;
}

static BOOLEAN checkBannedIP(unsigned int ip,BOOLEAN websock)
{

	if (isBanned("", 0, 0, "", ip) != bc_none)
	{	// banned by IP, do a quick rejection
		if (logging >= log_errors)
		{
			char* stamp = timestamp();
			int b4 = (ip >> 0) & 0xff;
			int b3 = (ip >> 8) & 0xff;
			int b2 = (ip >> 16) & 0xff;
			int b1 = (ip >> 24) & 0xff;
			logEntry(&mainLog, "[%s] UNUSUAL: ip %d.%d.%d.%d rejected - connection from banned %sIP\n",
				stamp, b1, b2, b3, b4,
				websock ? "websocket " : "");
		}
		return(TRUE);
	}
	return FALSE;
}
static BOOLEAN checkTooMany(unsigned int ip,BOOLEAN websock)
{
	int n = checkSession(ip, WAITINGSESSION, maxConnections);
	if (n < 0)
	{
		if (logging >= log_errors)
		{
			int b4 = (ip >> 0) & 0xff;
			int b3 = (ip >> 8) & 0xff;
			int b2 = (ip >> 16) & 0xff;
			int b1 = (ip >> 24) & 0xff;
			logEntry(&mainLog, "[%s] Call from %d.%d.%d.%d refused and banned %s too many connections\n",
				timestamp(), b1, b2, b3, b4,
				websock ? "websocket " : "");
		}
		return TRUE;
	}
	return FALSE;
}


void doEchoAll(User *ru, char *toOthers, int ignoreF) 
{ Session *S = ru->session;
  ignoreF |= ru->expectEof;

  if (		(S!=WAITINGSESSION)						//don't echo to the other waiting sockets
			&& (S!=PROXYSESSION)					//or to proxies
			&& !ru->gagged 
			&& (isRealSocket(ru->socket)  || (ignoreF == 1)))
	{

	User *su = S->first_user;
	int pop = S->population; //zapped users can be removed from the session, so take special
							 // care to maintain the original list.
	int nfound=0;
	while(su)
	{ User *next = su->next_in_session;
	  assert(su->session==S);
      if( isRealSocket(su->socket) && !su->gagged)
 	   {int writeResult = lsend(su, toOthers);
        if (writeResult < 0) 
		{ //zapped users can be removed from the session
		  if(!su->expectEof)
			{su->wasZapped=TRUE;
			if(logging>=log_errors)
				{logEntry(&mainLog,"[%s] Zap At %d:(echo2) Closing C%d (%s#%d) S%d  due to error %d,errno=%d\n",
					  timestamp(),__LINE__,su->userNUM,su->clientRealName,su->clientUid,su->socket,writeResult,ErrNo());
						}

		  }
		  HandleWriteError(su, "echo other zap");
		}
	  }
	  nfound++;
	  su=next;
	  assert(nfound<=pop);
	}
	assert(nfound==pop);
  }  
}

void doEchoOthers(User *ru, char *toOthers, int ignoreF) 
{ Session *S = ru->session;
  ignoreF |= ru->expectEof;

  if (toOthers 
			&& (S!=WAITINGSESSION)						//don't echo to the other waiting sockets
			&& (S!=PROXYSESSION)						//or to proxies
			&& !ru->gagged 
			&& (isRealSocket(ru->socket)  || (ignoreF == 1)))
	{

	User *su = S->first_user;
	int pop = S->population; //zapped users can be removed from the session, so take special
							 // care to maintain the original list.
	int nfound=0;
	while(su)
	{ User *next = su->next_in_session;
	  assert(su->session==S);
      if((ru!=su) && isRealSocket(su->socket) && !su->gagged)
 	   {int writeResult = lsend(su, toOthers);
        if (writeResult < 0) 
		{ //zapped users can be removed from the session
		  if(!su->expectEof)
			{su->wasZapped=TRUE;
			if(logging>=log_errors)
				{logEntry(&mainLog,"[%s] Zap At %d:(echo2) Closing C%d (%s#%d) S%d  due to error %d,errno=%d\n",
					  timestamp(),__LINE__,su->userNUM,su->clientRealName,su->clientUid,su->socket,writeResult,ErrNo());
						}

		  }
		  HandleWriteError(su, "echo other zap");
		}
	  }
	  nfound++;
	  su=next;
	  assert(nfound<=pop);
	}
	assert(nfound==pop);
  }  
}

void doEchoSelf(User *ru, char *toSource, int ignoreF) 
{ Session *S = ru->session;
  ignoreF |= ru->expectEof;
//Sleep(1000);
  if (toSource && isRealSocket(ru->socket)) 
  {
    int writeResult = lsend(ru,toSource);
    if (writeResult < 0) 
	{
		if (!ignoreF)
		{
			ru->wasZapped = TRUE;
			if (logging >= log_errors)
			{
				logEntry(&mainLog, "[%s] Zap At %d:(echo) Closing C%d (%s#%d) S%d  due to error %d, errno=%d\n",
					timestamp(), __LINE__, ru->userNUM, ru->clientRealName, ru->clientUid, ru->socket, writeResult, ErrNo());
			}
		}
	HandleWriteError(ru, "echo zap");
    }
  }
}
void markForClosure(User* u)
{
	u->inputClosed = TRUE;
	u->outputClosed = TRUE;
}
int sendSingleLen(char *inStr, User *u)
{
   int err=0;
   if (isRealSocket(u->socket) && !u->outputClosed)
   {
	   if ((err = lsend(u, inStr)) < 0)
	   {
		   if (!u->expectEof)
		   {
			   u->wasZapped = TRUE;
			   if (logging >= log_errors)
			   {
				   logEntry(&mainLog, "[%s] Zap At %d: Closing C%d (%s#%d) S%d due to error %d, errno=%d.\n",
					   timestamp(), __LINE__, u->userNUM, u->clientRealName, u->clientUid, u->socket, err, ErrNo());
			   }
		   }
		   HandleWriteError(u, "send error");
	   }
  }
   else
   {  logEntry(&mainLog,"[%s] Unusual: send to already closed C%d session %d msg %s",
				timestamp(),
				u->userNUM,
				u->session->sessionNUM,
				inStr);
	   unusual_events++;
   }

   return(err<0?-1:0);
}

/*--------------------------------------------------------------------*/
/* send a line to an active client */
int sendSingle(char *inStr, User *u) 
{	int err = 0;
	if(isRealSocket(u->socket))
	{
	err = sendSingleLen(inStr,u);
	}
	return(err);
}

// Send to everyone in a session
void sendMulti(char *inStr,Session *s)
{	assert(s!=NULL);
	{
	User *U=s->first_user;
	int pop = s->population;
	int nfound=0;
	
	while(U)
	{
	User *next = U->next_in_session;
	assert(U->session==s);
	if (isRealSocket(U->socket)) 
		{
		sendSingleLen(inStr,U);
		}
	U=next;
	nfound++;
	assert(nfound<=pop);
	}
	assert(nfound==pop);
	}

}
void errMsg(int line,SOCKET stream,char *msg)
{	if(logging>=log_errors)
	{logEntry(&mainLog,"[%s] At line %d, closing S%d due to %s\n",timestamp(),line,stream,msg);
	}
}
void errClose(int line,SOCKET stream,char *why)
{	errMsg(line,stream,why);
	if(logging>=log_connections)
		{logEntry(&mainLog,"[%s] Closing S%d\n",timestamp(),stream);
		}
	if(isRealSocket(stream))
		{
		simpleCloseSocket(stream);
		}
}

/*--------------------------------------------------------------------*/
static void process_reserve_room(char *data,User *u,char *seq)
{
	/* 332 <sessionnum> <password> */
	/* 332 <sessionnum> <no_password> */
	/* 332 <sessionnum> <private> */
	/* set session password */
   	int sessionNum;
	int sent=0;
	char reserve_string[SMALLBUFSIZE];
	int idx = safe_scanint(data,&sessionNum);
	int idx2 = (idx>=0) 
	  ? scanstr(data+idx,reserve_string,sizeof(reserve_string)) 
	  : -1;

    if (idx2>0) 
		 {
           if ((sessionNum >= 0) && (sessionNum <= maxSessions)) 
		   { int nopw=(strcmp(reserve_string,"<no_password>")==0);
			 Session *S = &Sessions[sessionNum];
			 int isEmpty = sessionIsEmpty(S);
			 if ( isEmpty 
					? (S->sessionKey==0)					//not reserved for a game, open season
					: ((u->session==S) && (!S->sessionHasGame || u->isAPlayer))	//or I'm a player in this session
				  )
			 {	//setting password
				
				 if(nopw) 
					{ S->sessionURLs[0]=(char)0; 
				 	  S->sessionDescribe=TRUE;
					  if(S->sessionIsPrivate)
					  {
						 S->sessionIsPrivate=0;
						 if(logging>=log_connections)
							{logEntry(&mainLog,"[%s] Session %d made public by C%d (%s#%d) S%d\n",
								timestamp(),sessionNum,u->userNUM,u->clientRealName,u->clientUid,u->socket);
							}

					  }else
					  {
						games_launched++; 
						if(logging>=log_connections)
							{logEntry(&mainLog,"[%s] Game Launched on session %d by C%d (%s#%d) S%d\n",
								timestamp(),sessionNum,u->userNUM,u->clientRealName,u->clientUid,u->socket);
							}
					  }
					}
				 else 
					{	//only allow setting pw for empty sessions. This avoids a problem where
						//two independant groups try to launch at the same time, and one completely
						//finishes before the second starts, resulting in both thinking they own
						//the session
					  MyStrncpy(S->sessionURLs,reserve_string,sizeof(S->sessionURLs));
					  S->sessionDescribe=TRUE;

					  if(isEmpty)
					  {
					   if(isClosed || u->gagged)
					   {
						lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_FAILED  SEND_RESERVE_ROOM "%s %s",data,"no new games allowed");	//reject
						sendSingle(u->tempBufPtr,u);
						sent = 1;
					   }
					   else
					   {
					   unsigned int r1,r2,r3,r4;
					   r1=rand()&0xff;
					   r2=rand()&0xff;
					   r3=rand()&0xff;
					   r4=rand()&0xff;

					   S->sessionKey=r1|(r2<<8)|(r3<<16)|(r4<<24);

					   S->sessionIsPrivate = 0;
					   if(logging>=log_connections)
							{logEntry(&mainLog,"[%s] Session %d reserved by C%d (%s#%d) S%d using key %d.%d.%d.%d (%s)\n",
								timestamp(),sessionNum,u->userNUM,u->clientRealName,
								u->clientUid,u->socket,r1,r2,r3,r4,data);
							}
					   }
					  }
					  else
					  {	if(logging>=log_connections)
							{logEntry(&mainLog,"[%s] Session %d made private by C%d (%s#%d) S%d\n",
								timestamp(),sessionNum,u->userNUM,u->clientRealName,u->clientUid,u->socket);
							}
					    S->sessionIsPrivate = 1;
					  }
					 }
                    saveConfig();
					if(!sent)
					{
					S->sessionTimes=Uptime();
					/* echo back the whole line */
                    lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_RESERVE_ROOM "%s",data);
                    if(isEmpty)
					{sendSingle(u->tempBufPtr, u);
					}else
					{sendMulti(u->tempBufPtr,S);	//send to everyone in the session
					}
                    sent = 1;
			 }}
				 }}
            if (sent == 0) {

					   lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_FAILED SEND_RESERVE_ROOM "%s",data);
								if(logging>=log_connections)
								{logEntry(&mainLog,"[%s] Session %d reserve failed by C%d (%s#%d) S%d\n",
									timestamp(),sessionNum,u->userNUM,u->clientRealName,u->clientUid,u->socket);
								}

                sendSingle(u->tempBufPtr,u);
              }

}


/*--------------------------------------------------------------------*/
static void process_set_state(char *data,User *u,char *seq)
{
	/* 334 <sessionnum> <state> <game> */
	/* set session state code */
   	  int sessionNum=0;
	  int sessionState=0;
	  int sent=0;
	  int sessionGame=0;
	  int nargs = sscanf(data,"%d %d %d",&sessionNum,&sessionState,&sessionGame);
 	  if(nargs<3) { sessionGame=0; }
      if (nargs >= 2) 
		 {
           if ((sessionNum >= 0) 
			   && (sessionNum <= maxSessions))
		   { Session *S=&Sessions[sessionNum];
			if(sessionIsEmpty(S)
			   && (S->sessionKey==0)	//not reserved for launch
			   && ((S->sessionStates!=sessionState)
						|| (S->sessionGameID!=sessionGame))
			   )
		   { 
			 S->sessionStates=sessionState;
			 S->sessionGameID=sessionGame;
			 S->sessionDescribe=TRUE;
			 clearSessionGame(S);			// forget about any cached game
			 if(logging>=log_connections)
				{logEntry(&mainLog,"[%s] Session %d Set to state %d:%d by C%d (%s#%d) S%d\n",
								timestamp(),sessionNum,sessionState,sessionGame,
								u->userNUM,u->clientRealName,u->clientUid,u->socket);

				}
			/* echo back the whole line */
            lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_SET_STATE "%s",data);
			sendMulti(u->tempBufPtr, S);
            sent = 1;
	  }}}
        if (sent == 0)
		{ lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_FAILED SEND_SET_STATE "%s",data);
          sendSingle(u->tempBufPtr,u);
        }
}

static void doShutdown(User *u,const char *buf)
{		
		logEntry(&mainLog,"[%s] shutdown request by supervisor C%d (%s#%d) S%d : %s\n",
			timestamp(),u->userNUM,u->clientRealName,u->clientUid,u->socket,buf); 
		
#if WEBSOCKET
		closeWebSocket();
#endif
		closesocket(serversd);		//first close the server accept.  This doesn't count in the active tally
		serversd=0;
		{ int i;
		  for(i=0;i<=maxSessions;i++)
		  {	Session *s = &Sessions[i];
			User *u=s->first_user;
			int pop = s->population;
			int nfound=0;
			while(u)
			{
			User *next = u->next_in_session;
			assert(u->session==s);
			simpleCloseClient(u,"shutdown");
			u=next;
			nfound++;
			assert(nfound<=pop);
			}
			assert(nfound==pop);
		  }
		}

		isshutdown=1;
		saveConfig();
		ExitWith(0);			//exit gracefully

}

BOOLEAN userNameMatches(User *U,char *uname)
{	if((uname,"guest",5)==0)
	{  
	 return(STRICMP(uname,U->clientPublicName)==0);
	}
	else 
	{ return(STRICMP(uname,U->clientRealName)==0);
	}
}

/* a few simple supervisor commands.  Syntax is password!command args
in any chat window
*/
static void doSupervisor(User *SU,char *buf)
{	
	if(strncmp(buf,"cached ",7)==0)
	{	// show games cached matching a substring
		char matching[64];
		int matchidx = scanstr(buf+7,matching,sizeof(matching));
		int matchn=0;
		BOOLEAN all = strcmp(matching,"*")==0;
		if(matchidx>0)
		{	GameBuffer *g = all_gamebuffers;
			while(g!=NULL)
			{	if(THREAD_READ(g,preserved))
				{ if(all || strstr(g->idString,matching)!=NULL)
					{
					char msg[SMALLBUFSIZE];
					matchn++;
					lsprintf(sizeof(msg),msg,ECHO_GROUP_OTHERS "%d schat %d: %s",SU->userNUM,g->uid,g->idString);
					sendSingle(msg,SU);
					}
				}
				g = g->next;
			}

		}
	}
	else if(STRNICMP(buf,"uncache ",8)==0)
	{	// remove a cached game, identified by a number seen in the "cached" display
		int nth = 0;
		int n = sscanf(buf+8,"%d",&nth);
		if((n==1) && (nth>=0))
		{
		GameBuffer *g = all_gamebuffers;
		while(g!=NULL)
		{	if(g->uid==nth)
			{
			if(THREAD_READ(g,preserved))
			{
			char msg[SMALLBUFSIZE];
			lsprintf(sizeof(msg),msg,ECHO_GROUP_OTHERS "%d schat %d: %s removed",SU->userNUM,nth,g->idString);
			sendSingle(msg,SU);
			unpreserveGame(g);
			g = NULL;
			}
			}
			else 
			{
			g = g->next;
			}
		}
		}}
	else if(strncmp(buf,"logFile ",4)==0)
		{	char obuf[SMALLBUFSIZE];
			int ll=0;
			if(sscanf(buf+8,"%d",&ll)==1)
			{
			if(ll==-1)
			{
			lsprintf(sizeof(obuf),&obuf[0],ECHO_GROUP_OTHERS "%d schat reopening log file",SU->userNUM);
			mainLog.renameLogFile = TRUE;
			chatLog.renameLogFile = TRUE;
			securityLog.renameLogFile = TRUE;
			}else
			{
			logging=ll;
			lsprintf(sizeof(obuf),&obuf[0],ECHO_GROUP_OTHERS "%d schat logFile level set to %d",SU->userNUM,logging);
			}
			}else
			{lsprintf(sizeof(obuf),&obuf[0],ECHO_GROUP_OTHERS "%d schat logFile level is %d",SU->userNUM,logging);
			}
			sendSingle(&obuf[0],SU);
		}
	else if(strncmp(buf,"score",5)==0)
	{	int val=0;
		char obuf[SMALLBUFSIZE];
		if(sscanf(buf+6,"%d",&val)==1)
		{strict_score=val;
		 lsprintf(sizeof(obuf),&obuf[0],ECHO_GROUP_OTHERS "%d  schat strict-score set to %d",SU->userNUM,strict_score);
		}else
		{ lsprintf(sizeof(obuf),&obuf[0],ECHO_GROUP_OTHERS "%d  schat strict-score is %d",SU->userNUM,strict_score);
		}
		sendSingle(&obuf[0],SU);
	}
	else if(strncmp(buf,"strict",6)==0)
	{	int val=0;
		char obuf[SMALLBUFSIZE];
		if(sscanf(buf+7,"%d",&val)==1)
		{strict_login=val;
		 lsprintf(sizeof(obuf),&obuf[0],ECHO_GROUP_OTHERS "%d  schat strict set to %d",SU->userNUM,strict_login);
		}else
		{ lsprintf(sizeof(obuf),&obuf[0],ECHO_GROUP_OTHERS "%d  schat strict is %d",SU->userNUM,strict_login);
		}
		sendSingle(&obuf[0],SU);
	}
	else if(strncmp(buf,"shutdown",8)==0)
		{char obuf[SMALLBUFSIZE];
		 lsprintf(sizeof(obuf),&obuf[0],ECHO_GROUP_OTHERS "%d schat shutting down",SU->userNUM);
		 sendSingle(&obuf[0],SU);
		 doShutdown(SU,buf+9);
		}
	else if(crash_test && strncmp(buf,"crash",5)==0)
		{ // deliberately cause a memory violation, for testing purposes
		char* die = 0;
		die += 0xdeadbeef;
		  *die = 'a';
		}
	else if(strncmp(buf,"close",5)==0)
		{char obuf[SMALLBUFSIZE];
		 lsprintf(sizeof(obuf),&obuf[0],ECHO_GROUP_OTHERS "%d schat closing to new connections",SU->userNUM);
		 sendSingle(&obuf[0],SU);
		 isClosed = TRUE;
		}
	else if(strncmp(buf,"open",4)==0)
		{char obuf[SMALLBUFSIZE];
		 lsprintf(sizeof(obuf),&obuf[0],ECHO_GROUP_OTHERS "%d schat opening to new connections",SU->userNUM);
		 isClosed = FALSE;
		 sendSingle(&obuf[0],SU);
		}
	else if((strncmp(buf,"kill ",5)==0)
			|| (strncmp(buf,"zap ",4)==0))
		{//zap some luser.  They hang for a while, then commit suicide
			int kill = (strncmp(buf,"kill ",5)==0);
			char cmd[10];
			char uname[SMALLBUFSIZE];
			int idx = scanstr(buf,cmd,sizeof(cmd));
			int idx2 = (idx>0) ? scanstr(buf+idx,uname,sizeof(uname)) : -1;
			if(idx2>0)
			{
			int i;
			for(i=0;i<=maxSessions;i++)
			{ Session *s=&Sessions[i];
			  User *u=s->first_user;
			  int pop = s->population;
			  int nfound=0;
			  while(u)
			  {	User *next = u->next_in_session;
				assert(u->session==s);
			    if(userNameMatches(u,uname))
				{ char obuf[SMALLBUFSIZE];
				  char name[sizeof(u->clientRealName)];
				  strcpy(name,u->clientRealName);
				  lsprintf(sizeof(obuf),obuf,ECHO_PLAYER_QUIT "%d killed by supervisor",u->userNUM);
				  banUser(u);		// and ban him
				  doEchoAll(u,obuf,TRUE);							//tell everybody else he's gone
				  if(kill)
				  {
					// [10/2005] now that clients auto-reconnect, it's pointless to really kill
				    // them.  Instead, allow them to receive the "you're dead" message
					// and they will respect it by not reviving themselves
				    if(isRealSocket(u->socket))
					{
					  HandleKill(u);
					  // don't actually do anything else, just let them 
					  // receive the message and become silent
					  // simpleCloseClient(u,"operator");
					}
				  }
				  lsprintf(sizeof(obuf),obuf,ECHO_GROUP_OTHERS "%d schat User %d %s zapped",SU->userNUM,u->userNUM,name);
				  logEntry(&mainLog,"[%s] C%d (%s) S%d killed C%d (%s) S%d\n",
							timestamp(),
							SU->userNUM,
							SU->clientRealName,SU->socket,
							u->userNUM,	name,u->socket);
				  sendSingle(&obuf[0],SU);

				}
				 u=next;
				 nfound++;
				 assert(nfound<=pop);
			  }
			  assert(nfound==pop);
			}

	}}
	else if(strncmp(buf,"exempt ",6)==0)
		{//exempt some user from strict login checking.
		char uname[SMALLBUFSIZE];
		int idx = scanstr(buf+7,uname,sizeof(uname));
		if(idx>0) 
		{	char obuf[SMALLBUFSIZE];
			MyStrncpy(Exempt_User,uname,sizeof(Exempt_User));
			logEntry(&mainLog,"[%s] C%d (%s) S%d future user %s exempted from strict login\n",
					timestamp(),
					SU->userNUM,
					SU->clientRealName,
					SU->socket,
					Exempt_User);
			lsprintf(sizeof(obuf),obuf,ECHO_GROUP_OTHERS "%d schat future user %s exempted from strict login",
				SU->userNUM,Exempt_User);

		    sendSingle(&obuf[0],SU);
		}}
	else if( (strncmp(buf,"ungag ",6)==0)
			 || (strncmp(buf,"gag ",4)==0))
		{//gag some luser.  They hang for a while, then commit suicide
			BOOLEAN isgag = (strncmp(buf,"gag ",4)==0)?1:0;
			char uname[SMALLBUFSIZE];
			int idx = scanstr(buf+(isgag?4:6),uname,sizeof(uname));
			if(idx>0)
			{
			int i;
			for(i=0;i<maxSessions;i++)
			{ Session *S=&Sessions[i];
			  User *U=S->first_user;
			  int pop = S->population;
			  int nfound=0;
			  while(U)
			  { User *next = U->next_in_session;
				assert(U->session==S);
				if(userNameMatches(U,uname))
				{ char obuf[SMALLBUFSIZE];
				  U->expectEof=TRUE;
				  lsprintf(sizeof(obuf),obuf,ECHO_PLAYER_QUIT "%d gagged=%d by supervisor",isgag,U->userNUM);
				  doEchoOthers(U,obuf,TRUE);							//tell everybody else he's gone
				  U->gagged=isgag;	// gag him
				  if(isgag) 
				  {banUser(U);		// and ban him
				  }else
				  {unBanUser(U);
				  }
				  lsprintf(sizeof(obuf),obuf,ECHO_GROUP_OTHERS "%d schat User %d %s gagged=%d",
					  SU->userNUM,U->userNUM,U->clientRealName,isgag);
				  sendSingle(&obuf[0],SU);
				  logEntry(&mainLog,"[%s] C%d (%s) S%d gagged=%d C%d (%s) S%d\n",
							timestamp(),U->userNUM,SU->clientRealName,SU->socket,isgag,
							U->userNUM,U->clientRealName,U->socket);
				}
				U=next;
				nfound++;
				assert(nfound<=pop);
				}
			  assert(nfound==pop);
			}
	
			}
		}
	else if(strncmp(buf,"ban",3)==0)
	{
		int ip1,ip2,ip3,ip4;
		int nban = sscanf(buf+4,"%d.%d.%d.%d",&ip1,&ip2,&ip3,&ip4);
		if(nban==4)
		{
		unsigned int bannedip = (ip1&0xff)<<24 | (ip2&0xff)<<16 | (ip3&0xff)<<8 | (ip4&0xff);
		isBanned("",0,'Z',"",bannedip);
		logEntry(&mainLog,"[%s] unusual banned by admin IP %d.%d.%d.%d\n",
					timestamp(),ip1,ip2,ip3,ip4);

		}
		else if(nban==1)
		{	isBanned("",ip1,'Z',"",0);	// ban uid
			logEntry(&mainLog,"[%s] unusual banned by UID %d\n",
					timestamp(),ip1);
		}
		else
		{	char namebuf[64];
			int nban2 = scanstr(buf+4,namebuf,sizeof(namebuf));
			if(nban2>2)
			{	isBanned(namebuf,0,'Z',"",0);
				logEntry(&mainLog,"[%s] unusual banned by name %s\n",
					timestamp(),namebuf);

			}
		}

	}
	else if(strncmp(buf,"unban",5)==0)
	{	// unban somebody.  Unban x removed ban event #x.  Just Unban lists the 
		// currently active ban events, with name/IP/ID currently associted with
		// each event.
	int eventid=-1;
	int na = sscanf(buf+6,"%d",&eventid);
	char obuf[SMALLBUFSIZE];

	if(na==1)
		{	bannedUser *B = unBanEvent(eventid);
			if(B) { lsprintf(sizeof(obuf),obuf,ECHO_GROUP_OTHERS "%d schat Removed %s",SU->userNUM,banInfoFor(B,bc_blank)); }
			else { lsprintf(sizeof(obuf),obuf,ECHO_GROUP_OTHERS "%d schat Event %d not found",SU->userNUM,eventid); }
			sendSingle(&obuf[0],SU);
		}
	// now describe bans in place
	describeBans(SU);
	}
	else if(strncmp(buf,"help",4)==0)
		{char obuf[SMALLBUFSIZE];
	lsprintf(sizeof(obuf),&obuf[0],ECHO_GROUP_OTHERS "%d schat commands are ban {} cached {} uncache {} logFile {0-3} strict {0,1} exempt {user} score {0-2} gag {user} ungag {user} unban {evenid} kill {user} zap {user} close, open, and \"shutdown\"",SU->userNUM);
		 sendSingle(&obuf[0],SU);
		}
}



#if STRICT
/* "strict" mode tries to restrict connections to the server to those who
come in through the legal login page.  It is supposed to work like this:
The login page and web server is running on the same host as the game server,
so connections from that IP address are "trusted".  The login page probes the
server to see if it is up, and as a side effect "registers" the user name and IP
address who is connecting.   The connector then loads his java classes and opens a new
connection to the server, which checks the IP address from the registration list.
Registrations are kep alive by maintaining a connection in the lobby.

The intent is that completely rogue logins are not permitted at all, and permitted
logins are only for a particular name that has been checked with the database by
the login page.

This isn't foolproof, but should discourage hackers, and provide advance
warning as they try to circumvent it.
*/


//
// search the registered users for one who matches both IP and NAME
//
registeredUser *findRegisteredUser(unsigned int regkey,char *name,int uid)
{	registeredUser *ru = activeRegisteredUsers;
	while(ru)
	{
	if((ru->regkey==regkey)		// regkey matches
		&& (strcmp(name,ru->clientName)==0)	// nickname matches
		&& ((ru->clientUid==0)		// either was registered without a uid, or it matches
				|| (uid==ru->clientUid)))
		{ return(ru); }
	ru = ru->next;
	}
	return(NULL);
}

//
//update the registration timestamps for all players still present in the lobby
//
void update_user_timestamps()
{	
	UPTIME now=Uptime();
	int sessn;
	for(sessn=0;sessn<=maxSessions;sessn++)
	{	Session *S=&Sessions[sessn];
		User *u=S->first_user;
		int pop=S->population;
		int nfound=0;
		while(u)
		{
		User *next = u->next_in_session;
		if(isRealSocket(u->socket) /* && (u->session==0) */)
		{registeredUser *idx=u->keyIndex;
		 if (idx)//user still active
		 { idx->timestamp=now;
		 }
		}	
		u=next;
		nfound++;
		assert(nfound<=pop);
		}
		assert(nfound==pop);
	}


}
//
// update timestamps, then purge anyone who is expired 
//
void purge_registered_users()
{	update_user_timestamps();
	{ registeredUser *ru = activeRegisteredUsers;
	  registeredUser *prev = NULL;
	  UPTIME now=Uptime();
	  while(ru)
	  { registeredUser *next = ru->next;
		int dif= (now - ru->timestamp);
		if(dif>REGISTERED_USER_TIMEOUT)
			{
			 if(logging>=log_connections)
				{unsigned int regkey=ru->regkey;
				 logEntry(&mainLog,"[%s] purge registered user %s #%8x \n",
						timestamp(),ru->clientName,regkey);
				}
			
			  ru->clientName[0]=0;
			  ru->ip=0;
			  ru->regkey=0;
			  ru->next = freeRegisteredUsers;
			  freeRegisteredUsers = ru;
			  if(prev==NULL) 
				{ activeRegisteredUsers=next; }
				else { prev->next = next; }
			}
			else
			{ prev = ru;
			}
	  ru = next;
	  }
	  
	}
}

void DescribeSessionString(Session *s,char *xB,int xbsize)
{
  int sockCtr = 0;
  int numActive = 0;
  int numPoss=maxClients;
  int sessState=(s==NULL)?MAXCLIENTS:s->sessionStates;
  int sessionGame=(s==NULL)?0:s->sessionGameID;
  User *u = s->first_user;
	int pop = s->population;
	int nfound=0;
	while(u)
	{
	User *next = u->next_in_session;
	assert(u->session==s);
	if(isRealSocket(u->socket) || u->isARobot)
	{
			numActive = numActive + 1;
	}
	u=next;
	nfound++;
	assert(nfound<=pop);
	}
	assert(nfound==pop);
  

    {	  //int sess = Users[sockCtr].session;
  int pass = s->sessionClear ? 2 : s->sessionURLs[0] ? 1 : 0;
  if((pass==1) && s->sessionIsPrivate) { pass = 3; }
  lsprintf(xbsize,xB,ECHO_SUMMARY "%d %d %d %d %d %s",
		s->sessionNUM,numActive,sessState,pass,
		sessionGame,
		&s->sessionInfo[0]);
	}
}

//
//tell all the members of session 0 about the state of some other session
//
void DescribeSession(int sessn)
{ char temp[SMALLBUFSIZE];
  if((sessn>=0) && (sessn<=maxSessions))
  {//avoid using WAITINGSESSIONNUM or any other number greater than the server limit
  Session *L = LOBBYSESSION;
  User *U = L->first_user;
  int pop = L->population;
  int nfound=0;

  DescribeSessionString(&Sessions[sessn],&temp[0],sizeof(temp));
  while(U)
  { char temp2[SMALLBUFSIZE];		//recopy because each send adds a crlf
    User *next =  U->next_in_session;
	assert(U->session==L);
	strcpy(temp2,&temp[0]);
	sendSingle(&temp2[0], U);
	U = next;
	nfound++;
	assert(nfound<=pop);
  }
  assert(nfound==pop);

  }
}

//
// return null if not registered. user structure if registered 
//
registeredUser *isRegisteredUser(unsigned int regkey,unsigned int real_ip,char *name,int uid)
{	purge_registered_users();
	{registeredUser *v=findRegisteredUser(regkey,name,uid);
	 if(v) { v->ip=real_ip; }
	return(v);
	}
}

//
// register a new user 
//
void registerUser(unsigned int regkey,char *name,int uid,char *auxinfo)
{	registeredUser *isreg=isRegisteredUser(regkey,0,name,uid);
	if(isreg==NULL)
	{
	if(freeRegisteredUsers)
		{isreg = freeRegisteredUsers;
		freeRegisteredUsers= isreg->next;
		isreg->next = activeRegisteredUsers;
		activeRegisteredUsers = isreg;
		MyStrncpy(isreg->clientName,name,sizeof(isreg->clientName));
		isreg->clientUid = uid;
		isreg->regkey=regkey;
		}
	}

	if(isreg)
	{
	 isreg->timestamp=Uptime();
	 if(logging>=log_connections)
		{
		 logEntry(&mainLog,"[%s] registering user %s#%d with key #%8x and \"%s\"\n",
				timestamp(),name,uid,regkey,auxinfo);
		}
	}
	else
	{	logEntry(&mainLog,"Failed to register user %s (no free slots\n",name);
	}
}

#endif

static User *process_takeover(char *data,User *u,char *seq)
{
	char kb[64];
	int idx = scanstr(data,kb,sizeof(kb));

	if(idx>0)
	{
	int spectating=strcmp(kb,KEYWORD_SPECTATE)==0;
	int playing=strcmp(kb,KEYWORD_PLAYING)==0;

	if(! (playing || spectating))
	{	// this is a suicide note
		u->expectEof=TRUE;
		lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_PLAYER_QUIT "%d %s",u->userNUM,data);
		doEchoOthers(u,u->tempBufPtr,1);
		// echo to self after
		lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_I_QUIT "%s%s",seq,data);
		doEchoSelf(u,u->tempBufPtr,1);
		closeClient(u,"user request",grace_forbidden);
	}
	else if(playing)
	{	User *to=NULL;
		Session *S=u->session;
		int nfound=0;
		int pop = S->population;
		int toseat=-1;
		if(safe_scanint(data+idx,&toseat) && (toseat>=0))
		to=S->first_user;
		while(to)
		{
		assert(to->session==S);
		if((to->isAPlayer)
		   && (to->clientSeat==toseat))
			{ break;
			}
		to=to->next_in_session;
		nfound++;
		assert(nfound<=pop);
		}

		if(to==u) {}
		else if(to)
		{ // ignore this if not really a reconnection
		if(isRealSocket(to->socket))
			{	
			if(logging>=log_connections)
				{
				logEntry(&mainLog,"[%s] client C%d (%s#%d) S%d  session %d closed before reconnect C%d\n",
				timestamp(),
				to->userNUM,to->clientRealName,to->clientUid,to->socket,							
				to->session->sessionNUM,
				to->userNUM);
				}
			closeClient(to,"reconnect",grace_forbidden);
			}

		// tell them first
		lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_PLAYER_QUIT "%d %s",u->userNUM,data);
		doEchoOthers(u,u->tempBufPtr,1);
		// tell us
		lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_I_QUIT "%s%s",seq,data);
		doEchoSelf(u,u->tempBufPtr,1);

		if(logging>=log_connections)
			{logEntry(&mainLog,"[%s] spectator C%d (%s#%d) S%d in session %d is now C%d\n",
			timestamp(),
			u->userNUM,u->clientRealName,u->clientUid,u->socket,							
			u->session->sessionNUM,
			to->userNUM);
			}
		u->clientSeat=toseat;
		u->clientOrder = to->clientOrder;
		u->clientRev = to->clientRev;
		u->isAPlayer=TRUE;
		CopyUser(u,to);						// from u to to
		removeUserFromSession(u);			// dispose of the old user object
		putUserInSession(u,IDLESESSION);	// make it idle again
		u=to;
		}
		else
		{	char *stamp = timestamp();
			logEntry(&securityLog,
				// this message used to be "failed to reconnect "
				"[%s] UNUSUAL: spectator C%d (%s#%d) S%d in session %d  failed to take over as %d\n",
				stamp,
				u->userNUM,u->clientRealName,u->clientUid,u->socket,							
				u->session->sessionNUM,
				toseat);

			lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_FAILED SEND_TAKEOVER "%s",data);	//reject
			sendSingle(u->tempBufPtr,u);
			unusual_events++;
			
			if(logging>=log_connections)
			{
			// this message used to be "failed to reconnect ".  This failure probably means the 
			// user came in with the wrong seat number
			logEntry(&mainLog,"[%s] UNUSUAL: spectator C%d (%s#%d) S%d in session %d  failed to take over as %d\n",
				stamp,
				u->userNUM,u->clientRealName,u->clientUid,u->socket,							
				u->session->sessionNUM,
				toseat);

			}
		}
		}
	else if(spectating)		//become a spectator
		{
		// tell others
		lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_PLAYER_QUIT "%d %s",u->userNUM,data);
		doEchoOthers(u,u->tempBufPtr,1);
		// then tell us
		lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_I_QUIT "%s%s",seq,data);
		doEchoSelf(u,u->tempBufPtr,1);
		{
		User *newu=findAslot(u->session);

		//reassign a spectator slot immediately
		if(newu)
			{ if(logging>=log_connections)
			{logEntry(&mainLog,"[%s] C%d (%s#%d) S%d in session %d is now spectator C%d\n",
			timestamp(),u->userNUM,u->clientRealName,u->clientUid,u->socket,
			u->session->sessionNUM,newu->userNUM);
			}
		MyStrncpy(newu->clientPublicName,u->clientPublicName,sizeof(u->clientPublicName));
		MyStrncpy(newu->clientRealName,u->clientRealName,sizeof(u->clientRealName));
		newu->clientUid=u->clientUid;
		MyStrncpy(u->clientPublicName,"(vacancy)",sizeof(u->clientPublicName));
		newu->socket=u->socket;
		u->socket=-1;
		u->checksums=FALSE;
		}}
	}
	}
	return(u);
}

/*
This is a funciton to facilitate the transisiton to game records with timestamps
on every move.  The problem is that "legacy" clients don't expect the timestamps.
The solution has three parts  

First, The old "fetch game" and "fetch active game" requests
become "fetch game filtered" and "fetch active game filtered".  These come here,
to the special copy funciton that knows how to filter out the new information. 
Consequently, the old clients never see the unexpected information.  

Second, the replacement requests for fetching games just pass everything in
the way the old ones do.

Finally, the serverID changes to 18 so the new clients can know that the server
accepts the new format requests.  This backs the unlikely event that the server
has to be reverted to the old behavior.

*/


// fetch information about a game that is not associated with a room,
// so has no player information.  This is used to reestablish robot
// games, and eventually to reestablish player games too.
static void process_fetch_game_f(char *data,User *u,char *seq,BOOLEAN filter)
{
	//return 321 + game uid + game
  char nameBuf[SMALLBUFSIZE];
  int idx=scanstr(data,nameBuf,sizeof(nameBuf));
  GameBuffer *g=NULL;
  if((idx>0)&& ((g=findNamedGame(u,nameBuf))!=NULL)) 
	{

	 ZapDuplicateSession(g,u);

	 if(logging>=log_connections)
	 {logEntry(&mainLog,"[%s] resume cached game %s#%d\n",
			timestamp(),
			g->idString,g->uid);
		}
	 {Session *S = u->session;
	  int players = 0;
	  User *au = S->first_user;
	  while(au!=NULL) 
		{ if(au->isAPlayer && !au->isARobot) { players++; }
	      au = au->next_in_session;
		}
	  if(players==1) { robot_games_relaunched++; }
	  else if(u==S->first_user) { player_games_relaunched++; }
	 }

	 { char tb[SMALLBUFSIZE];
	 int tbidx = lsprintf(sizeof(tb),tb, "%s%d ",(filter ? ECHO_FETCH_GAME_FILTERED : ECHO_FETCH_GAME),g->uid,tb);
	   int escsize = escapedStringSize(g->gamePtr);
	   int totsize = tbidx+escsize+10;	// some slop
	   if(totsize>=u->tempBufSize)
	   {	// must be careful because the size of the game we're fetching
		    // is unrelated to the current size of tempbuf.
		   setTempBufSize(u,totsize);
	   }
	   assert(totsize<u->tempBufSize);
	   MyStrncpy(u->tempBufPtr,tb,tbidx+1); 
	   escapeAndFilterString(u->tempBufSize-tbidx,u->tempBufPtr+tbidx,g->gamePtr,filter);
	 }
	}else
	{ 
		lsprintf(u->tempBufSize,u->tempBufPtr,"%s 0",filter ? ECHO_FETCH_GAME_FILTERED : ECHO_FETCH_GAME);
	}
	 sendSingle(u->tempBufPtr,u);
}
static void process_fetch_game(char *data,User *u,char *seq)
{	process_fetch_game_f(data,u,seq,FALSE);
}
static void process_fetch_game_filtered(char *data,User *u,char *seq)
{	process_fetch_game_f(data,u,seq,TRUE);
}
// fetch the active game, including the identities of the players.  This
// is intended to be used by spectators and by disconnected players to get
// reestablished
static void process_fetch_active_game_f(char *data,User *u,char *seq,BOOLEAN filter)
{
	//return 341 + game uid + channel + status ... + story ...
  GameBuffer *g=findNamedGame(u,"*");
  if((g!=NULL)) 
	{
	char tb[SMALLBUFSIZE];
	int tbidx = lsprintf(sizeof(tb),tb,"%s%d ",(filter ? ECHO_ACTIVE_GAME_FILTERED : ECHO_ACTIVE_GAME),g->uid);
	 Session *s = u->session;
	 User *su = s->first_user;

	  // insert information about the players
	 while(su!=NULL)
	 {
	 if(su->isAPlayer)
		{tbidx += lsprintf(sizeof(tb)-tbidx,tb+tbidx," %d %d %d %d %c %s",
				su->userNUM,
				su->clientSeat,
				su->clientOrder,
				su->clientUid,
				(su->isARobot? 'R' : (isRealSocket(su->socket)?'P':'Q')),
				su->clientRealName
				);
		}
	 su = su->next_in_session;
	 }
	 // add the game status code
	{  
	int gamecode = -1;			// default, game in progress
	if(s->sessionHasGame) { gamecode ^= 1; }		// was a real game
	if(s->sessionScored) { gamecode ^= 2; }			// scored
	if(s->sessionFileWritten) { gamecode ^= 4; }	// file was saved
	tbidx += lsprintf(sizeof(tb)-tbidx,tb+tbidx," %d ",gamecode);
	}
	{
	int escsize = escapedStringSize(g->gamePtr);
	int totsize = tbidx + escsize+10;	// plus some slop
	if(totsize>=u->tempBufSize)
		{
		// must be careful because the size of the game we're fetching
		// is unrelated to the current size of tempbuf.
		setTempBufSize(u,totsize);
		}
	assert(totsize < u->tempBufSize);
	MyStrncpy(u->tempBufPtr,tb,tbidx+1); 
	escapeAndFilterString(u->tempBufSize-tbidx,u->tempBufPtr+tbidx,g->gamePtr,filter);
	sendSingle(u->tempBufPtr,u);
	}
	}else
  { lsprintf(u->tempBufSize,u->tempBufPtr,"%s 0",filter ? ECHO_ACTIVE_GAME_FILTERED : ECHO_ACTIVE_GAME);
	  sendSingle(u->tempBufPtr,u);
	}
	 
}
static void process_fetch_active_game(char *data,User *u,char *seq)
{	process_fetch_active_game_f(data,u,seq,FALSE);
}
static void process_fetch_active_game_filtered(char *data,User *u,char *seq)
{	process_fetch_active_game_f(data,u,seq,TRUE);
}
//
// state keys are part of the fraud detection program.  Suppose you wanted
// to beat the bot by playing it against itself - you would run two games,
// and feed the moves from one game to the other.  But in doing so, the "follower"
// game necessarily goes through the same states as the "leader" game.
// Our scheme is that each game reports the state hash after every move,
// so the follower can be detected as following the leader.
//
// sessionStateHash is a hash table containing pointers to the current
// hash index of active games.  We use it as a single proble, ignore failure
// hash table, since collisions should be rare, and to be effective there
// will be many real duplicates in the course of a game.
//
static void process_recordStateKey(char *cmd,User *U,char *seq)
{	Session *S = U->session;
	int idx = scanstr(cmd,U->tempBufPtr,U->tempBufSize);

	if(S->sessionStateKeyIndex>=0) { sessionStateHash[S->sessionStateKeyIndex]=NULL; }
	MyStrncpy(S->sessionStateKey,U->tempBufPtr,sizeof(S->sessionStateKey));
	{
	int newhashcode = hashString(S->sessionStateKey);
	int newhashindex = newhashcode % ARRAY_NUMBER_OF_ELEMENTS(sessionStateHash);
	assert(newhashindex>=0 && newhashindex<ARRAY_NUMBER_OF_ELEMENTS(sessionStateHash));
	{
	Session *leader = sessionStateHash[newhashindex];
	if(leader==NULL)
	{	// all clear, move in
		sessionStateHash[newhashindex]=S;
		S->sessionStateKeyIndex = newhashindex;
	}
	else if((S!=leader) && (strcmp(S->sessionStateKey,leader->sessionStateKey)==0))
	{
	char *stamp = timestamp();
	if(logging>=log_errors)
	{
	logEntry(&mainLog,
		"[%s] UNUSUAL possible follower fraud state %s leader Session %d follower Session %d C%d (%s#%d)\n",
		stamp,
		S->sessionStateKey,
		leader->sessionNUM,
		S->sessionNUM,
		U->userNUM,
		U->clientRealName,
		U->clientUid);
	}
	unusual_events++;
	logEntry(&securityLog,
		"[%s] UNUSUAL possible follower fraud state %s leader Session %d follower Session %d C%d (%s#%d)\n",
		stamp,
		S->sessionStateKey,
		leader->sessionNUM,
		S->sessionNUM,
		U->userNUM,
		U->clientRealName,
		U->clientUid);
	// echo a follower hit
	lsprintf(U->tempBufSize,U->tempBufPtr,ECHO_STATE_KEY " follow");
	sendSingle(U->tempBufPtr,U);
	}
	}
	}
}


void process999(char *cmd,User *u,char *seq)		// just ignore
{

}

// tell "to" about the current members of the session
void doSessionIntro(User* to)
{
	Session* s = to->session;
	User* u = s->first_user;
	while (u != NULL)
	{
		if (u != to)
		{
			char xB[SMALLBUFSIZE];
			lsprintf(sizeof(xB), xB, ECHO_INTRO_OTHERS "%d %d %d %d %s %d %d",
				u->userNUM,
				(u->isARobot ? 2 : u->isAPlayer ? 1 : 0),				// 1 for a player 0 for a spectator
				u->clientSeat,
				u->clientUid,
				u->clientRealName,
				u->clientOrder,
				u->clientRev);			// play order is of interest only to the other players
			sendSingle(xB, to);
		}
		u = u->next_in_session;
	}

}

BOOLEAN checkTooManyU(int userid,unsigned int ip)
{	int numconnections=0;
	int i;
	for(i=0;i<=maxSessions;i++)	// all sessions, except waiting session
	{	Session *S=&Sessions[i];
		User *U=S->first_user;
		while(U)
		{
		if( (U->clientUid==userid) && (U->ip==ip))
			{
			numconnections++;
			}
		U = U->next_in_session;
		}
	}
	return(numconnections>=maxConnectionsPerUID);
}

//
// process introduction and request for connection.  Check the IP against
// ban list and permission lists.
//
void process_send_intro(char *data,User *u,char *seq)
{
   int sessionNum;
   int sent=0;
   char username[SMALLBUFSIZE] = {0};
   char password[SMALLBUFSIZE] = {0};
   char cookie[BANSIZE];
   char bankey[SMALLBUFSIZE];
   char info[INFOSIZE] = {0};
   int IP1=0,IP2=0,IP3=0,IP4=0,usernum=0;

   int idx1 = safe_scanint(data,&sessionNum);
   int idx2 = (idx1>0) ? scanstr(data+idx1,username,sizeof(username)) : -1;
   int idx3 =0;
   int ips = (idx2>0) ? sscanf(data+idx1+idx2,"%d.%d.%d.%d%n",&IP1,&IP2,&IP3,&IP4,&idx3) : -1;
   int idx4 = ((ips==4)&&(idx3>0))?scanstr(data+idx1+idx2+idx3,password,sizeof(password)) : -1;
   int idx5 = (idx4>0) ? scanstr(data+idx1+idx2+idx3+idx4,cookie,sizeof(cookie)) : -1;
   int idx6 = (idx5>0) ? scanstr(data+idx1+idx2+idx3+idx4+idx5,bankey,sizeof(bankey)) : -1;
   unsigned int client_real_ip = u->ip;
	int b4 = (client_real_ip>>0)&0xff;
	int b3 = (client_real_ip>>8)&0xff;
	int b2 = (client_real_ip>>16)&0xff;
	int b1 = (client_real_ip>>24)&0xff;

#if STRICT
   {
		char client_ip_string[20];
		 unsigned int serverKey = (IP1<<0)|(IP2<<8)|(IP3<<16)|(IP4<<24);
		 lsprintf(sizeof(client_ip_string),client_ip_string,"%d.%d.%d.%d",b1,b2,b3,b4);
		 { char *ni = strchr(username,'#');
		   if(ni!=NULL) 
		   {	*ni++ = 0;
				if(sscanf(ni,"%d",&usernum)!=1) { usernum=0; }
		   }
		 }
		 //register users even if strict in not in effect


		if(Exempt_User[0] && 
			( (STRICMP(username,Exempt_User)==0)
			  || (STRICMP(Exempt_User,client_ip_string)==0)))
		{
		 registerUser(serverKey,&username[0],usernum,info);
		}
		else 
		  if((ips==4)&&(sessionNum == -1))
		    {
		      if((client_real_ip==server_ip)
			   ||(client_real_ip==alt_server_ip))
			{ registerUser(serverKey,&username[0],usernum,info);		// intro comes from the real server
			  closeClient(u,"user registered by server",grace_forbidden);
			  return;
			}
		      else
			{ logEntry(&securityLog,"[%s] cmd=(%s) connect not from server ip, is %x not %x or %x \n",
				   timestamp(),data,client_real_ip,server_ip,alt_server_ip);
			  closeClient(u,"not server ip",grace_forbidden);
			  return;
			}
		    }

		u->supervisor=FALSE;	// just to be sure
		 if(idx6>0)
		 {	//ok so far, check for banned addresses
			enum bancode ban = isBanned(username,usernum,bankey[0],cookie,client_real_ip);
			MyStrncpy(u->cookie,cookie,sizeof(u->cookie));
			switch(ban)
			 {	
				case bc_super:
					{
					u->supervisor=TRUE;
					if(logging>=log_errors)
					{	logEntry(&mainLog,"[%s] %s cmd=(%s) connecting from %d.%d.%d.%d: cookie %s status %s\n",
							timestamp(),banreason[ban],data,b1,b2,b3,b4,cookie,bankey);
					}}
					break;
				case bc_unban:
					{
					if(logging>=log_errors)
					{	logEntry(&mainLog,"[%s]  %s cmd=(%s) connecting from %d.%d.%d.%d: cookie %s status %s\n",
							timestamp(),banreason[ban],data,b1,b2,b3,b4,cookie,bankey);
					}}
					break;

				case bc_none:
				default: break;

				case bc_same_ip:
				case bc_same_name:
				case bc_same_id:
					{
					 
					 //not formated correctly
					lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_I_QUIT "bad-banner-id ");
					sendSingle(u->tempBufPtr,u);
					if(logging>=log_errors)
					{	logEntry(&mainLog,"[%s] %d %s cmd=(%s) attempted to connect from %d.%d.%d.%d: cookie %s status %s\n",
							timestamp(),u->userNUM,banInfo(ban),data,b1,b2,b3,b4,cookie,bankey);
					}
					// close after log
					closeClient(u,"failed login (banned)",grace_forbidden);
					return;
					}
			 }
		 }
		 if(strict_login)
		 {int ok=0;
		  int logged=0;
		  if(ips == 4)		// we got an ip
		  {	registeredUser *regid=isRegisteredUser(serverKey,client_real_ip,username,usernum);
			//we got the full name and ip
			//this is some code to counteract attemtps to connect to the server
			//using cracked clients.  This hasn't happened yet, but it will.
			//if this is from the same host as the sever, believe it, and register the
			//user name with the specified ip.  In this case, the IP is the users remote IP addr.
			//otherwise, if the users REAL ip and usename match one previously registered, go ahead.
			//otherwise, someone is making up a name and connecting to the server without going through
			//the real login procedure.
			if(regid==NULL)
			{
			char *stamp = timestamp();
			logEntry(&securityLog,"[%s] UNUSUAL: unregistered user at %d.%d.%d.%d attempted to connect to session : %s\n",
						stamp,b1,b2,b3,b4,data);
			logged++;
			if(logging>=log_errors)
					{
					logEntry(&mainLog,"[%s] unregistered user at %d.%d.%d.%d attempted to connect to session : %s\n",
								stamp,b1,b2,b3,b4,data);
					}
			}else
			{
			u->serverKey=serverKey;
			u->keyIndex=regid;
			ok=1;
			}
		 }
		 if(!ok) 
		 {	//not formated correctly
			lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_I_QUIT "bad-id ");
			sendSingle(u->tempBufPtr,u);
			if(!logged)
				{
				unusual_events++;
				logEntry(&securityLog,"[%s] UNUSUAL: Malformed login from ip %d.%d.%d.%d: %s\n",
					timestamp(),
					b1,b2,b3,b4,
					data);
				}
			closeClient(u,"failed login",grace_forbidden);
			return;
		 }
		 }
}
		 /* technically, we should also check the session password here */
#endif

		// check for too many connections for this UID/IP pair
		 if(checkTooManyU(usernum,client_real_ip))
		 {
			lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_I_QUIT "bad-banner-id ");
			sendSingle(u->tempBufPtr,u);

			unusual_events++;
			logEntry(&securityLog,"[%s] UNUSUAL:< too many connections from %s(%d) ip %d.%d.%d.%d: %s\n",
					timestamp(),
					username,usernum,
					b1,b2,b3,b4,
					data);
			markForClosure(u);
		 }

		 if ((idx1>0)			// we got a session number 
			 && (sessionNum>=0) 
			 && (sessionNum<=maxSessions))
		 {	  Session *s = &Sessions[sessionNum];
			  int passw=(s->sessionURLs[0]==0)?0:1;		//nonzero if password required
			  int key = s->sessionKey;
			  int r1=key&0xff;
			  int r2=(key>>8)&0xff;
			  int r3=(key>>16)&0xff;
			  int r4=(key>>24)&0xff;
			  BOOLEAN passwordOk = ((idx4>0) && (strcmp(password,s->sessionURLs)==0));
			  time_t realtime = time(NULL);
			  int hitime = (int)(realtime /  1000000);
			  int lowtime = (int)(realtime % 1000000);		// time_t mught be a long long, and will overflow on 19 January 2038
			  unsigned int nowtime = (unsigned int)(realtime & 0xffffffff);	// overflow on jan 19 2035
			  BOOLEAN passwordSupplied = strcmp(password,"<none>")!=0;

			  if (sessionNum>0 && checkSession(client_real_ip, s, maxConnectionsPerSession)<0)	// too many!
			  {
				  unusual_events++;
				  lsprintf(u->tempBufSize, u->tempBufPtr, ECHO_I_QUIT "bad-banner-id ");
				  sendSingle(u->tempBufPtr, u);
				  logEntry(&securityLog, "[%s] UNUSUAL:< too many connections from %s(%d) ip %d.%d.%d.%d: %s\n",
					  timestamp(),
					  username, usernum,
					  b1, b2, b3, b4,
					  data);
				  markForClosure(u);
			  }
			  // [3/2013]
			  // don't allow connections as a player to sessions that are
			  // not playing sessions.  This happened, rarely, when one of
			  // the players quit before the other connected.  The second player
			  // connected and kept acting as if a valid game was in progress.
			  //
			  if(!s->poisoned && (passw ? passwordOk : !passwordSupplied))
			  {
			  removeUserFromSession(u);	//remove him from the waiting session
			  putUserInSession(u,s);	//assign the session he requested
			  s->sessionClear = 0;		// no longer a clearing session
			  s->sessionHasGame |= passw;	//at least one guy got in with a game key
			  s->sessionDescribe=TRUE;			//needs to notice
			  u->isAPlayer = passw?TRUE:FALSE;
			  u->isARobot = FALSE;
			  //u->clientPublicName[0]=(char)0;
			  //u->clientRealName[0]=(char 0)
			  //u->clientUid=0;
			  MyStrncpy(u->clientRealName,&username[0],sizeof(u->clientRealName));
			  MyStrncpy(u->clientPublicName,&username[0],sizeof(u->clientPublicName));
			  u->clientUid = usernum;
			  u->clientSeat=-1;
			  u->clientOrder=-1;
			  u->clientRev=-1;
			  {

			  // yow! time became a 64 bit quantity, which made "now" put two values on the stack,
			  // which caused lsprintf to print an extra zero.
			  int b4 = (client_real_ip>>0)&0xff;
			  int b3 = (client_real_ip>>8)&0xff;
			  int b2 = (client_real_ip>>16)&0xff;
			  int b1 = (client_real_ip>>24)&0xff;

			   // 
			   // a bit of obfuscation, encode the requirement for
			   // encryption into the time. If serverlevel is 15
			   // the java clients use this to decide.
			  if (require_rng) 
				{ nowtime |= 1; lowtime |= 1; }
				 else { nowtime &= ~1; lowtime &= ~1;  }

			  lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_INTRO_SELF "%d %d %d %d.%d.%d.%d %d.%d.%d.%d %d%06d %d %d %d",
				  sessionNum,u->userNUM,SERVERID,
				  r1,r2,r3,r4,b1,b2,b3,b4,hitime,lowtime,MAX_INPUT_BUFFER_SIZE,s->population,passw);
			  }
			  doEchoSelf(u,u->tempBufPtr,0);	// no sequence number here
		
			  if(isRealSocket(u->socket))
			  {
			  if(require_rng)
			  {	  char rngbuf[SMALLBUFSIZE];
			   
				  // java clients have to construct identical strings. nowtime must be an int
				  lsprintf(sizeof(rngbuf),rngbuf,"%d.%d.%d.%d.%d",r1+1,r2+2,r3+3,r4+4,nowtime+2);
				  init_rng_in(u,rngbuf);
				  lsprintf(sizeof(rngbuf),rngbuf,"%d.%d.%d.%d.%d",r1+3,r2+6,r3+9,r4+12,nowtime+1);
				  init_rng_out(u,rngbuf);
			  }

			  lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_INTRO_OTHERS "%d %d %d %d %s %d %d",
					u->userNUM,
					u->isARobot?2:passw ? 1 : 0,				// 1 for a player 0 for a spectator
					u->clientSeat,
					u->clientUid,
					u->clientRealName,
					u->clientOrder,
					u->clientRev);			// play order is of interest only to the other players

			  doEchoOthers(u,u->tempBufPtr,0);
			  
			  doSessionIntro(u);

			  if(sessionNum==0) { clients_connected++; }
			  if(logging>=log_connections)
					{	unsigned int key = u->serverKey;
						int k1=key&0xff;
						int k2=(key>>8)&0xff;
						int k3=(key>>16)&0xff;
						int k4=(key>>24)&0xff;
						logEntry(&mainLog,"[%s] C%d (%s#%d) S%d from queue is now in session %d using server key %d.%d.%d.%d session key %d.%d.%d.%d. session id %s\n",
						timestamp(),u->userNUM,&username[0],u->clientUid,u->socket,sessionNum,k1,k2,k3,k4,r1,r2,r3,r4,
						passw ? password :"<n/a>");
					}
			  s->sessionTimes = u->clientTime = Uptime();

			  } // end of if U connected ok

			 // 7/30/2008 I can't imagine what these were for, but they at least would
			 // have to be conditional on isRealSocket
			 // FD_CLR(u->socket,&rfds);
			 // FD_CLR(u->socket,&efds);
			  saveConfig();
			  sent = 1;
		 }
			else {
			if(logging>=log_errors)
				{
				logEntry(&mainLog,"[%s] connection rejected poison=%d passw=%d ok %d sup %d",
						timestamp(),s->poisoned,passw,passwordOk,passwordSupplied);
				}
			}
		}
		else {
			if(logging>=log_errors)
				{
				logEntry(&mainLog,"[%s] connection rejected bad session number %d",
						timestamp(),sessionNum);
				}
		}
		  if(sessionNum==-1) 
			{ sent=1; //a fib, but we want to ignore the registration attempt if we get here 
			}
		  if (sent == 0) {
			lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_FAILED SEND_INTRO "%s",data);
			sendSingle(u->tempBufPtr,u);
		  }
}

//
// check if scoring is ok.  Called from scoring CGI scripts
//
void process_check_score(char *data,User *u,char *seq)
{	// score by uid
#if STRICT
		if(strict_score==2)
		{
		  lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_CHECK_SCORE "0");
		  sendSingle(u->tempBufPtr,u);	//scoring not allowed, "just say no"
		  closeClient(u,"scoring disabled",grace_forbidden);
		}
		else
		{
		int uid1 = -1;
		int uid2 = -1;
		int uid3 = -1;
		int uid4 = -1;
		int uid5 = -1;
		int uid6 = -1;

		int uid7 = -1;
		int uid8 = -1;
		int uid9 = -1;
		int uid10 = -1;
		int uid11 = -1;
		int uid12 = -1;

		int sess=0;
		int ip1=0,ip2=0,ip3=0,ip4=0;
		//
		// Sep 2009, expanded to allow 2 more uids after the key
		// April 2011, added 2 more so 6 players are supported
		//
		int nc=sscanf(data,"%d %d %d %d.%d.%d.%d %d %d %d %d %d %d %d %d %d %d",
				&sess,	// session number is arg 1
				&uid1,	// uid 1 is arg 2
				&uid2,	// uid 2 is arg 3
				&ip1,&ip2,&ip3,&ip4,	// 4 ip segments are uids 4 5 6 7
				&uid3,	// uid3 is arg 8
				&uid4,	// uid4 is arg 9
				&uid5,	// uid5 is arg 10
				&uid6,	// uid6 is arg 11
				&uid7,
				&uid8,
				&uid9,	// uid7 through uid12 args 12-17
				&uid10,
				&uid11,
				&uid12
				);
		int sent=0;
		if(((u->ip==server_ip)		//we're asked from the server
			||(u->ip==alt_server_ip))
			&&(nc>=7)						//we got a complete query
			&&(sess>0)						//and a valid session number
			&&(sess<=maxSessions))
		{	//there is a game with that ID 
		 unsigned int claimed_remote_ip = (ip1<<0)|(ip2<<8)|(ip3<<16)|(ip4<<24);
		Session *S=&Sessions[sess];

		 if((S->sessionKey==claimed_remote_ip)
			&& (S->sessionScored==0))
		 { 
		   int nameok=0;
		   int okon1=(strict_score==0);
		   int okon2=okon1;
		   int okon3=(nc<8) || okon1;
		   int okon4=(nc<9) || okon1;
		   int okon5=(nc<10) || okon1;
		   int okon6=(nc<11) || okon1;

		   int okon7 = (nc < 12) || okon1;
		   int okon8 = (nc < 13) || okon1;
		   int okon9 = (nc < 14) || okon1;
		   int okon10 = (nc < 15) || okon1;
		   int okon11 = (nc < 16) || okon1;
		   int okon12 = (nc < 17) || okon1;

		   int nplayers = 0;
		   {
		   User *u=S->first_user;
		   int pop = S->population;
		   int nfound=0;
		   while(u)
		   { User *next = u->next_in_session;
		   assert(u->session==S);
		   if(u->isAPlayer)
				{ 
			    nplayers++;
				// if uid1 == uid2, get them both
				if(!okon1 && (uid1==u->clientUid))
				{ okon1=1;
				}
				else if((uid2>0)&& (uid2==u->clientUid))
				{ okon2=1;
				}
				else if((uid3>0)&& (uid3==u->clientUid))
				{ okon3=1;
				}
				else if((uid4>0)&& (uid4==u->clientUid))
				{ okon4=1;
				}
				else if((uid5>0)&& (uid5==u->clientUid))
				{ okon5=1;
				}
				else if((uid6>0)&& (uid6==u->clientUid))
				{ okon6=1;
				}

				else if ((uid7 > 0) && (uid7 == u->clientUid))
				{
					okon7 = 1;
				}
				else if ((uid8 > 0) && (uid8 == u->clientUid))
				{
					okon8 = 1;
				}
				else if ((uid9 > 0) && (uid9 == u->clientUid))
				{
					okon9 = 1;
				}
				else if ((uid10 > 0) && (uid10 == u->clientUid))
				{
					okon10 = 1;
				}
				else if ((uid11 > 0) && (uid11 == u->clientUid))
				{
					okon11 = 1;
				}
				else if ((uid12 > 0) && (uid12 == u->clientUid))
				{
					okon12 = 1;
				}
				}
			u=next;
			nfound++;
			assert(nfound<=pop);
			}
		   assert(nfound==pop);
		   }
		   if (nplayers == 1) {
			   // special case for single player games.  This enables the server check
			   // for crosswordle.  Note that the check is suppressed after the first
			   // puzzle in a session so that the rest of the mechanism doesn't have
			   // to be messed with.
			   okon2 = 1;
		   }
		   if(okon1 && okon2 && okon3 && okon4 && okon5 && okon6
			   && okon7 && okon8 && okon9 && okon10 && okon11 && okon12)
			{ 

			 if(logging>=log_connections)
				{
				 if(Sessions[sess].sessionClear)
				 { logEntry(&mainLog,"[%s] scoring check: %s (saved by grace time)\n",timestamp(),data);
				 }else
				 {logEntry(&mainLog,"[%s] scoring check: %s\n",timestamp(),data);
				 }
				
				}
			  if(S->sessionClear)
			  {	 S->sessionClear=0;
				 clearSession(S);

			  }else
			  {
				 S->sessionScored++;	//only get to ask once
				 S->sessionDescribe=TRUE;
			  }

			  lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_CHECK_SCORE "1");
			  sendSingle(u->tempBufPtr,u);			//say it's ok
			  closeClient(u,"scoring check ok",grace_forbidden);
			  sent=1;
		   
		   }
		   else if(logging>=log_errors)
			{//log some data about the reason for the failure
			 logEntry(&mainLog,"[%s] scoring check data iii, ok1=%d ok2=%d\n",timestamp(), okon1,okon2);
			}

		   }
		  else if(logging>=log_errors)
		  {
			logEntry(&mainLog,"[%s] scoring check data ii, scored=%d key=%x serverkey=%x\n",
			timestamp(),
			S->sessionScored,claimed_remote_ip,S->sessionKey);
		  }


		 }
		
		if(!sent)
			{ //fail if no one sent any result.  This represents a serious bug or
			  //a hacking attempt
			unusual_events++;
			if(logging>=log_errors)
			{
			 logEntry(&mainLog,"[%s] scoring check data, nc=%d sess=%d u->ip=%x server_ip=%x\n",timestamp(),
				nc,sess,u->ip,server_ip);
			 logEntry(&mainLog,"[%s] UNUSUAL: scoring check failed, %s\n",timestamp(),data);
			
			}
			lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_CHECK_SCORE "0");
			sendSingle(u->tempBufPtr,u);
			closeClient(u,"scoring check, not ok",grace_forbidden);
			}
		}
#else
		//strict not supported, just say ok
		lsprintf(sizeof(xB),&xB[0],ECHO_CHECK_SCORE "1");
		sendSingle(xB,u);	//not strict, just say it's ok
		closeClient(u,"scoring check not compiled",grace_forbidden);
#endif
}

 // 302 + session description
 // if session description changes, lobbies are updated
 // return is 303 <sessions> <users> <hightime> <lowtime>
void process_ping(char *message,User *u,char *seq)
{
    Session *S=u->session;
	struct timeb hnow;
	ftime(&hnow);
	lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_PING "%d %d %d %d",maxSessions+1, MAXCLIENTS,hnow.time,hnow.millitm);

	// look for ping stats
	if((message[0]=='P')&&(message[1]==':'))
	{	char *out = &u->pingStats[0];
	int siz = sizeof(u->pingStats)-2;
	int nc = 0;
	char *comma = NULL;
	char *some = NULL;
	message+=2;
	while(*message && (*message!=' ') && (siz-->0))
	{	char ch = *message++;
		nc++;
		*out++ = ch;
		if(ch==',') { comma = message; }
		if(ch!=0) { some = message; }
	}
	if(comma && nc>2)
	{	char key1 = some[-1];
		char key2 = comma[-2];
		if(key1 & 1) 
			{ u->rogueOutput++; 
			// security through obscurity.  several counters in the output process
			// should be in synch, and if not, probably someone is injecting commands
			// from outside the normal communications sequence.
			  if(u->rogueOutput==1)
			  {	unusual_events++;
				if(logging>=log_errors)
				{
				DumpHistory();
				logEntry(&mainLog,"[%s] unusual rogue output flagged from C%d (%s#%d) on S%d session %d\n",
				timestamp(),u->userNUM,
				u->clientRealName,u->clientUid,u->socket,
				u->session->sessionNUM);
				}
			  }
			}
		if( (key2 & 1))
		{	u->injectedOutput++; 
			// security through obscurity.  several counters in the output process
			// should be in synch, and if not, probably someone is injecting commands
			// from outside the normal communications sequence.
			  if(u->injectedOutput==1)
			  {	unusual_events++;
				if(logging>=log_errors)
				{
				DumpHistory();
				logEntry(&mainLog,"[%s] unusual rogue injected output flagged from C%d (%s#%d) on S%d session %d\n",
				timestamp(),u->userNUM,
				u->clientRealName,u->clientUid,u->socket,
				u->session->sessionNUM);
				}
			  }
		}
	}
	*out = (char)0;
	
	}

	// copy session info from game
	if(strncmp(&S->sessionInfo[0],message,SMALLBUFSIZE)!=0)
	{ MyStrncpy(S->sessionInfo,message,SMALLBUFSIZE);
	S->sessionDescribe=TRUE;
	}
	sendSingle(u->tempBufPtr, u);
}

void process_send_lobby_info(char *data,User *u,char *seq)
{	// no response from this one, just set the info in the server
	MyStrncpy(u->lobbyInfo,data,sizeof(u->lobbyInfo));
}

void process_send_summary(char *data,User *u,char *seq)
{	
	int currSess;
	int err=0;
 	for(currSess=0;(currSess<=maxSessions)&&(err==0);currSess++) 
	  {	DescribeSessionString(&Sessions[currSess],u->tempBufPtr,u->tempBufSize);
		err=sendSingle(u->tempBufPtr, u);			
      } 
}

void process_ask_detail(char *data,User *u,char *seq)
{	//describe a particular session in detail
 int sessionNum=-1;
 int sent=0;
 if (sscanf(data,"%d",&sessionNum) == 1) 
 {
 if((sessionNum>=0) && (sessionNum<=maxSessions))
 {
  int totalC=0,err=0;
  Session *S=&Sessions[sessionNum];
  User *tu=S->first_user;
  int pop = S->population;
  int nfound=0;
  while(tu && (err==0))
  { User *next = tu->next_in_session;
	assert(tu->session==S);
	if((!tu->gagged)									//make the gagees disappear
		&& (tu->clientPublicName[0]!=(char)0))
		{
		totalC++;

		if(sessionNum==0)
		{	//lobby 
		lsprintf(u->tempBufSize,u->tempBufPtr,SEND_ECHO_DETAIL "%d %d %d %s %d %s",
			sessionNum,
			tu->userNUM,
			0,	// no information here
			tu->clientPublicName,
			tu->clientUid,
			&tu->lobbyInfo);
		}
		else
		{
		lsprintf(u->tempBufSize,u->tempBufPtr,SEND_ECHO_DETAIL "%d %d %d %s %d",
			sessionNum,
			tu->userNUM,
			((tu->clientSeat>=0)
					?(100+tu->clientSeat)	//100 + seat code
					:tu->isAPlayer),	//just "present"
			&tu->clientPublicName[0],
			tu->clientUid);
		}
		err=sendSingle(u->tempBufPtr, u);
	  }
	tu=next;
	nfound++;
	assert( nfound<=pop);
  }
  assert(err || (nfound==pop));

 if(err==0)
 { 
	lsprintf(u->tempBufSize,u->tempBufPtr,SEND_END_DETAIL "%d %d",sessionNum,totalC);		//send a final summary
	sendSingle(u->tempBufPtr, u);
  }
  	sent = 1;

 }};
  if (sent == 0) {
    lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_FAILED SEND_ASK_DETAIL "%s",data);
    sendSingle(u->tempBufPtr,u);
  }
}

/* 310 <sessionnum> */
/* get session password */
void process_ask_password(char *data,User *u,char *seq)
{
	int sessionNum=-1;
	int sent=0;
	if (sscanf(data,"%d",&sessionNum) == 1) 
	  {
	   if((sessionNum>=0) && (sessionNum<=maxSessions))
	   {Session *S=&Sessions[sessionNum];
		lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_PASSWORD "%d %s",sessionNum,&S->sessionURLs[0]);
		sendSingle(u->tempBufPtr, u);
		sent = 1;
	   }};
	if (sent == 0) {
		lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_FAILED SEND_ASK_PASSWORD "%s",data);
		sendSingle(u->tempBufPtr,u);
	  }
} 

//312 is a pseudo client (aka robotantrx). send whatever he said to everybody
void process_send_all_group(char *data,User *u,char *seq)
{
	doEchoAll(u,data,0);
}
	
// register a fake (ie; robot) player.  Echos as if some other player joined.
// or register the essential info about a real player
void process_register_player(char *data,User *u,char *seq)
{/* register a fake slot for robot players */
	if(logging>=log_connections)
	{	logEntry(&mainLog,"[%s]Register Player C%d (%s#%d) S%d  session %d : %s\n",
		 timestamp(),
		 u->userNUM,u->clientRealName,u->clientUid,u->socket,							
		 u->session->sessionNUM,
		 data);
	}
	{
	  Session *s=u->session;
	  int order;
	  int uid = 0;
	  int seat;
	  char name[100];
	  int chan = 0;
	  int rev = -1;
	  int orderidx = safe_scanint(data,&order);
	  int seatidx = (orderidx>0) ? safe_scanint(data+orderidx,&seat) : -1;
	  int nameidx = (seatidx>0) ? scanstr(data+orderidx+seatidx,name,sizeof(name)) : -1;
	  int uididx = (nameidx>0) ? safe_scanint(data+orderidx+seatidx+nameidx,&uid) : -1;
	  int chanidx = (uididx>0) ? safe_scanint(data+orderidx+seatidx+nameidx+uididx,&chan) : -1;
	  int revidx = (chanidx>0) ? safe_scanint(data+orderidx+seatidx+nameidx+uididx+chanidx,&rev) : -1;
	  BOOLEAN useMe = (chanidx>0) && (chan!=0);
	  if(revidx<0) { rev = 0; }
	  if(nameidx>0)
	  { 
	    User *newu = NULL;
		if((chanidx>0)  && (chan>0))
			{ newu = s->first_user;
	  		  while((newu!=NULL)&& (newu->userNUM!=chan)) { newu = newu->next_in_session; } 
			  if(newu==NULL) 
			  { newu = u; // shouldn't happen, but victimize the current user if he doesn't know
			  }
			  newu->isARobot=isRealSocket(newu->socket)?FALSE:TRUE;
			}
			else
			{
			newu = findAslot(s);	//user supplied socket not used
			newu->socket=-1;
			newu->checksums=FALSE;
			newu->isARobot = TRUE;
			}

		newu->clientUid = uid;
		MyStrncpy(newu->clientPublicName,&name[0],sizeof(newu->clientPublicName));
		MyStrncpy(newu->clientRealName,&name[0],sizeof(newu->clientRealName));
		newu->clientSeat=seat;
		newu->clientOrder=order;
		newu->clientRev = rev;
		newu->isAPlayer=TRUE;
		lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_INTRO_OTHERS "%d %d %d %d %s %d %d",
			newu->userNUM,
			newu->isARobot?2:1,				// 1 for a player 2 as code for robot players
			newu->clientSeat,
			newu->clientUid,
			newu->clientRealName,
			newu->clientOrder,				// play order is of interest only to the other players
			newu->clientRev					// client per-game revision level
			);	
		// echo the 213 to everyone
	    doEchoAll(u,u->tempBufPtr,0);
	  }else
	  { 
		lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_FAILED SEND_REGISTER_PLAYER "%s",data);
		sendSingle(u->tempBufPtr,u);
	  }
	}
}

void process_write_game(char *data,User *u,char *seq)
{	Session *sess = u->session;
	if((sess!=LOBBYSESSION) && (sess->sessionFileWritten==0))
	{
	 sess->sessionFileWritten=1;
	 logGame(data,sess,u);
	}
}


void process_query_game(char *data,User *u,char *seq)
 {	//return 319+ sesion id + game uid
	int sess=0;
	int idx1 = safe_scanint(data,&sess);
	int idx2 = (idx1>0) ? scanstr(data+idx1,u->tempBufPtr,u->tempBufSize) : -1;
	GameBuffer *g=NULL;
	if((idx2>0)&& ((g=findNamedGame(u,u->tempBufPtr))!=NULL) ) 
		{ lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_QUERY_GAME "%d %d",sess,g->uid);
		}else
		{ lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_QUERY_GAME "%d 0",sess);
		}
	sendSingle(u->tempBufPtr,u);
 }

void process_save_game(char *data,User *u,char *seq)
{	unescapeString(data);
	if(u->session->sessionHasGame && !u->isAPlayer)
	{
	if(logErrorFor(u))
	{	unusual_events++;
		logEntry(&securityLog,"[%s] Unusual: Spectator C%d (%s#%d) S%d  session %d tried to record Game\n",
			 timestamp(),
			 u->userNUM,u->clientRealName,u->clientUid,u->socket,							
			 u->session->sessionNUM);
	}
	}
	else
	{
	char namebuf[SMALLBUFSIZE] = {0};
	int idx=scanstr(data,namebuf,sizeof(namebuf));
	// return format is "323 0" or "323 gameid"
	if(idx>0)
	{ GameBuffer *g = recordGame(u,namebuf,data+strlen(namebuf));
	  lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_SAVE_GAME "%d",g->uid);
	}else
	{	lsprintf(u->tempBufSize,u->tempBufPtr,"%s",ECHO_SAVE_GAME "0");
	}
	sendSingle(u->tempBufPtr,u);
	}
}
void lockSomeOtherUserForSession(Session *S)
{	User *U=S->locker;
	S->locker = NULL;
	if(U!=NULL)
	{
	// take this guy out
	U->requestingLock = FALSE;
	lsprintf(U->tempBufSize,U->tempBufPtr,"%s",LOCK_GAME "0");	//no we don't have it
	sendSingle(U->tempBufPtr,U);

	// iterate over the other users in the session, grant the lock
	// to the next (in round robin fashion) who is asking.
	{User *candidate = U;
	do
	{	if(candidate->requestingLock)
		{
			lsprintf(candidate->tempBufSize,candidate->tempBufPtr,"%s",LOCK_GAME "1");	//yes we have it
			sendSingle(candidate->tempBufPtr,candidate);
			candidate->requestingLock = FALSE;
			S->locker = candidate;
			return;
		}
		candidate = candidate->next_in_session;
		if(candidate==NULL) { candidate = S->first_user; }
	} while(candidate!=U);
	}

	}
}
//
// get a process lock on the session
// format "342 1" or "342 0"
// always get back immediately either 338 0 or 338 1.
// if you requested lock on and didn't get it, you will eventually
// receive another 342 1.
// you must relinquish the lock when you are done.
//
void process_lock_game(char *data,User *u,char *seq)
{	int onoff=0;
	Session *S = u->session;
	int idx = safe_scanint(data,&onoff);
	u->requestingLock = onoff!=0;
	if((onoff!=0) && ((S->locker==NULL)||(S->locker==u)))
	{	lsprintf(u->tempBufSize,u->tempBufPtr,"%s",LOCK_GAME "1");	//yes we have it
		u->requestingLock = FALSE;
		S->locker = u;
		sendSingle(u->tempBufPtr,u);
	}
	else	// turning off what we don't own, or someone else owns it
	{	if(S->locker==u)
		{	lockSomeOtherUserForSession(S);
		}
		else
		{
		lsprintf(u->tempBufSize,u->tempBufPtr,"%s",LOCK_GAME "0");	//n
		sendSingle(u->tempBufPtr,u);
		}
	}
}

void process_append_game(char *data,User *u,char *seq)
{	unescapeString(data);
	{char namebuf[SMALLBUFSIZE] = {0};
	char xB[SMALLBUFSIZE];
	int offset = 0;
	int checksum = 0;
	int next=0;
	int idx1 = scanstr(data,namebuf,sizeof(namebuf));
	int idx2 = (idx1>0)?safe_scanint(data+idx1,&offset) : -1;
	int idx3 = (idx2>0)?safe_scanint(data+idx1+idx2,&checksum) : -1;
	const char *spec = data+idx1+idx2+idx3+1;
	// return format is "323 0" or "323 gameid"
	if((idx3>0)&&(spec!=NULL))
	{ GameBuffer *g = reRecordGame(u,namebuf,offset,checksum,spec);
	  lsprintf(sizeof(xB),xB,ECHO_SAVE_GAME "%d",g?g->uid:0);
	}else
	{	lsprintf(sizeof(xB),xB,"%s",ECHO_SAVE_GAME "0");
	}
	sendSingle(xB,u);
}}

void process_remove_game(char *data,User *u,char *seq)
{	//return 325 + game id of removed game 
	int idx=scanstr(data,u->tempBufPtr,u->tempBufSize);
	int v=0;
	if(idx>0) { v=removeNamedGame(u,u->tempBufPtr); }
	lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_REMOVE_GAME "%d",v);
	sendSingle(u->tempBufPtr,u);
}

void process_send_group(char *data,User *u,char *seq)
{ /* echo to members of the session */
	size_t slen=strlen(&supervisor[0]);
	int wordlen=0;
	// key string is "210 xchat xyzzy|command"
	while(u->supervisor && (data[6+wordlen]>' ') && (data[6+wordlen]!='|')) { wordlen++; }

	if((slen>3)
			&& (u->oopsCount<5)	//not really bad at guessing
			&& (strncmp(data+1,"chat ",5)==0) 
			&& (data[6+wordlen]=='|'))
	{
	 if(u->supervisor 
		 && (data[6+slen]=='|') 
		 && (strncmp(data+6,&supervisor[0],slen)==0))
	 {	doSupervisor(u,data+7+slen);
	 }else
	 {	lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_GROUP_OTHERS "%d schat oops",u->userNUM);
		u->oopsCount++;	//count misses, to prevent rampant guessing
		sendSingle(u->tempBufPtr,u);
	 }
	}else
	{
	// log chats
	if((STRNICMP(data+1,"chat ",5)==0)
		&& !u->session->sessionIsPrivate
		&& ((data[0]!='t') 
			&& (data[0]!='T')
			&& (data[0]!='l')
			&& (data[0]!='L')
			)) // not tchat
	{	logEntry(&chatLog,"[%s] S%d %s: %s\n",
				timestamp(),
				u->session->sessionNUM,
				u->clientRealName,
				data+6);

	}
	 lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_GROUP_OTHERS "%d %s",u->userNUM,data);
	 doEchoOthers(u,u->tempBufPtr,0);
	}
	 // then tell us, in all cases
	 lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_GROUP_SELF "%s%s", seq,data);
	 doEchoSelf(u,u->tempBufPtr,0);

}

BOOLEAN endsIn(char *str,char *end)
{	size_t len = strlen(end);
	size_t slen = strlen(str);
	return((slen>=len) && (STRNICMP(str+slen-len,end,len)==0));
}
void process_sendmessageto(char *data,User *u,char *seq)
	{ /* echo to one member of the session */
	int user=-1;
	int usridx = safe_scanint(data,&user);
	int msgidx = scanstr(data+usridx,u->tempBufPtr,u->tempBufSize);
//	int slen = 0;
	BOOLEAN isPM = (msgidx>0)		
				&& endsIn(u->tempBufPtr,"chat")
				//&& ((slen=strlen(cmdString))>=6)
				//&& (strnicmp(cmdString+slen-4,"chat",4)==0)
				;
	if(usridx>=1)
	{
		Session *su = u->session;
		User *tu = su->first_user;
		int pop=su->population;
		int nfound=0;
		int idx=0;
		while(tu)
		{	User *next = tu->next_in_session;
			if(tu->userNUM==user)
			{ 
			if(isRealSocket(tu->socket)) 
				{ 
				while(data[idx]>' ') { idx++; };
				lsprintf(u->tempBufSize,u->tempBufPtr, ECHO_GROUP_OTHERS "%d %s", u->userNUM,data+idx);
				sendSingle(u->tempBufPtr,tu); 
				if(isPM)
				{	while((data[idx]<=' ')&&(data[idx]>(char)0)) { idx++; }
					while(data[idx]>' ') { idx++; }	// skip chat command
					while((data[idx]<=' ')&&(data[idx]>(char)0)) { idx++; }
					logEntry(&chatLog,"[%s] S%d PM %s to %s: %s\n",
						timestamp(),
						u->session->sessionNUM,
						u->clientRealName,
						tu->clientRealName,
						data+idx);
				}

				} 
			}
			tu=next;
			nfound++;
			assert(nfound<=pop);
		}
		assert(nfound==pop);
	   }


	}


void process_send_name( char *data,User *u,char *seq)
{	//remember the client names
 char tempString[SMALLBUFSIZE];
 int uid=0;
 int color=0;
 int order=0;
 int nameidx = scanstr(data,tempString,sizeof(tempString));
 int uididx = (nameidx>0) ? safe_scanint(data+nameidx,&uid) : -1;
 if(nameidx>0)
  { 
	lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_NAME "%s",tempString);
	MyStrncpy(u->clientPublicName,tempString,sizeof(u->clientPublicName));	//record his name
	if(u->clientRealName[0]==(char)0)
	{ //only set the real client name once
	  MyStrncpy(u->clientRealName,tempString,sizeof(u->clientRealName));	//record his name
	}
	if(uididx>0) { u->clientUid=uid ; }
	saveConfig();
  }else
  { lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_FAILED ECHO_NAME "%s",data);
  }
   sendSingle(u->tempBufPtr, u);
}

void process_send_seat( char *data,User *u,char *seq)
{
 int seat=-1;
 if(sscanf(data,"%d",&seat)>=1)
  { 
	lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_SEAT "%d",seat);
	u->clientSeat=seat;
	  }else
  { lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_FAILED ECHO_SEAT "%s",data);
  }
  sendSingle(u->tempBufPtr, u);
}



static int skip_escaped_text(char *start,int len)
{	// this is a bit ugly to solve an order of encoding problem.  The "len" which
	// determines the offset to the next subcommand is computed before the text is
	// escaped, but we have not unescaped it yet, so some extra characters may have been
	// added.  Rather than try to reengineer the whole encoding/transmission pipeline,
	// we skip forward counting encoded character sequences as one.
	//
	// this has to exactly echo how unescapeString will process the string
	int n = 0;
	int i = 0;
	while(n<len)
	{
	char ch = start[i++];
	n++;
	if(ch=='\\')
		{	ch = start[i++];					// next char is quoted, nominally another backslash
			if((ch>='0')&&(ch<='9')) { i+=2; }	// \nnn for one byte.  2 more chars
			else if(ch=='#') { i+= 4; }			// \#hhhh for 4 hex digits
		}
	}
	return(i);
}
void process_multiple(char *cmd,User *u,char *seq)
{	
	int subcommand = 0;
	size_t total = strlen(cmd)-1;
	size_t cmdidx = 0;
	// process multiple commands packaged as an atomic op
	while((cmdidx+1)<total)		// skip out when only a space remains
	{
		int len = 0;
		int idx = 0;
		int nc = sscanf(cmd+cmdidx,"%d%n",&len,&idx);
		subcommand ++;
		if(nc==1)
		{	if((len>=0) && ((len+idx+cmdidx)<=total))
			{
			// don't require the subcommands to be padded with extra spaces
			size_t cindex = idx+cmdidx;
			size_t nextidx = cindex+skip_escaped_text(cmd+idx+cmdidx,len);
			char saved = cmd[nextidx];
			cmd[nextidx]=(char)0;
			if(cmd[cindex]==' ') { cindex++; }	// skip a space if its there 
			u = processCheckSummedMessages(cmd+cindex,u,NULL,seq);
			cmd[nextidx]=saved;
			cmdidx = nextidx;
			}
			else
			{	if(logErrorFor(u))
				{
				logEntry(&securityLog,"[%s] overrun multiple command C%d (%s#%d) S%d #%d %s\n",
					timestamp(),u->userNUM,u->clientRealName,u->clientUid,u->socket,
					subcommand,cmd); 
				}
				cmdidx=total;		//punt out
			}

		}
		else 
		{	// should be another offset/command pair
			cmdidx = total;
			if(logErrorFor(u))
			{
			logEntry(&securityLog,"[%s] malformed multiple command C%d (%s#%d) S%d #%d %s\n",
					timestamp(),u->userNUM,u->clientRealName,u->clientUid,u->socket,
					subcommand,cmd); 
			}
		}

	}
}

#if PROXY

User *removeProxyClient(User *u,int sub)
{	User *prev = NULL;
	User *curr = u->proxyOut;
	while(curr!=NULL)
	{
	// proxyOut is a chain of proxy channels

	if(curr->userNUM==sub)
	{	// completely remove the proxy user
		if(prev==NULL)
		{	u->proxyOut = curr->proxyOut;
		}
		else
		{	prev->proxyOut = curr->proxyOut;
		}
		curr->proxyFor = NULL;
		curr->proxyOut = NULL;
		return(curr);
	}

	prev = curr;
	curr = curr->proxyOut;
	}
	return(NULL);
}
#endif

#if PROXY
void closeProxyClient(User *u,int sub)
{	User *prox = removeProxyClient(u,sub);
	if(prox)
	{	simpleCloseClient(prox,"owner request");
	}
}
#endif

#if PROXY
void doSocketConnected(User *pu)
{	User *u = pu->proxyFor;
	SOCKET sock = pu->socket;
	pu->connecting = FALSE;
	if(u && isRealSocket(sock))
	{
	char xb[SMALLBUFSIZE];
	lsprintf(sizeof(xb),xb,ECHO_PROXY_OP "connect %d %s",sock,pu->clientRealName);
	sendSingle(xb,u);
	}
	if(logging>=log_connections)
	{
	  logEntry(&mainLog,"[%s] Call C%d Session %d opened %d as a proxy session.\n",
	  timestamp(),u->userNUM,u->session->sessionNUM,pu->userNUM);
	}
}
#endif

#if PROXY
void doSocketConnectFailed(User *pu,int err)
{	User *u = pu->proxyFor;
	SOCKET sock = pu->socket;
	pu->connecting = FALSE;
	if(u && isRealSocket(sock))
	{
	lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_PROXY_OP "connect_failed %d",err);
	sendSingle(u->tempBufPtr,u);
	}
	if(logging>=log_connections)
	{	logEntry(&mainLog,"[%s] Call C%d Session %d proxy connect failed: %d",
				timestamp(),u->userNUM,u->session->sessionNUM,err);
	}
	closeClient(pu,"connection failed",grace_forbidden);
}
#endif
#if PROXY
void process_proxy_op(char *data,User *u,char *seq)
{	char command[64]={0};
	char name[CLIENTPUBLICNAMESIZE]={0};
	int idx = scanstr(data,command,sizeof(command));
	if(idx>0)
	{
	if(STRICMP(command,"connect")==0)
	{	// connect to localhost on port 80
		SOCKET sock = simpleOpenSocket();
		char *phase = "create socket";
		BOOLEAN ok = FALSE;

		if(sock!=INVALID_SOCKET )
		{
		struct sockaddr_in serv_addr;
		memset(&serv_addr,0,sizeof(serv_addr));
		serv_addr.sin_family = AF_INET;
		serv_addr.sin_port = htons(80);
		serv_addr.sin_addr.s_addr = htonl(0x7f000001);		// localhost
		phase = "socket connect";
		{	// this implements a very restricted proxy stream which
			// only connects to localhost on port 80.  It's used to
			// replace the applet making the same connections to 
			// perform routine operations such as getting rankings,
			// registering game starts, and scoring games
			//
		  User *pu = findAslot(PROXYSESSION);
		  scanstr(data+idx,name,sizeof(name));			// get some id for this 
		  MyStrncpy(pu->clientRealName,name,sizeof(pu->clientRealName));
		  MyStrncpy(pu->clientPublicName,name,sizeof(pu->clientPublicName));
		  pu->socket = sock;
		  ok = TRUE;
		  pu->checksums=FALSE;
		  pu->ip = u->ip;
		  pu->proxyFor = u;
		  pu->proxyOut = u->proxyOut;
		  u->proxyOut = pu;
		  pu->clientTime = Uptime();
		  pu->connecting = TRUE;
		  //
		  // according to the doc, a nonblocking connect will fail with
		  // an event in the error fds, and succeed with an event in the
		  // write fds
		  {
		  int connected = connect(sock,(const struct sockaddr *)&serv_addr,sizeof(serv_addr));
		  if(connected==0) 
			{ doSocketConnected(pu); 
			}
		  else if(connected==SOCKET_ERROR)
		  {	
		    if(logging>=log_connections)
			{
			logEntry(&mainLog,"[%s] Call C%d Session %d proxy connection started: %d",
					timestamp(),u->userNUM,u->session->sessionNUM,socket);
			}
		  }

		}
		}
		}	// end of valid socket

		if(!ok)
		{
		// make sure the caller gets a response message if we failed up front
		lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_PROXY_OP "connect_failed 0 %s",name);
		sendSingle(u->tempBufPtr,u);
		}

	    if(!ok && (logging>=log_errors))
		 {	int err = GetLastError();
			logEntry(&mainLog,"[%s] Call C%d Session %d proxy request %s failed: %d",
					timestamp(),u->userNUM,u->session->sessionNUM,phase,err);

		 }

	}	// end of connect command
	else if(STRICMP(command,"close")==0)
	{	int sub=0;
		int n = sscanf(data+idx,"%d",&sub);
		if(n==1)
		{
			closeProxyClient(u,sub);
		}
	}}
	else
	{
	lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_PROXY_OP "bad_format %s",data);
	sendSingle(u->tempBufPtr,u);
	}
}
#endif

#if PROXY
void process_proxy_data(char *cmd,User *u,char *seq)
{	int channel = 0;
	int idx = 0;
	int n = sscanf(cmd,"%d %n",&channel,&idx);
	if(n==1)
	{
	size_t len = strlen(cmd+idx);
	User *prox=u->proxyOut;
	while(prox!=NULL) 
		{ if(prox->userNUM==channel) { break; } else { prox = prox->proxyOut; }
		}

	if(prox && len>0)
		{
		lsend(prox,cmd+idx);
		}
		else
		{

		}
	}
}
#endif

typedef void (*cmdHandler)(char *cmd,User *u,char *seq);
typedef struct cmdp
{	char *cmd;
	cmdHandler disp;
} cmdp;

			
cmdp commands[] = 
{	{SEND_GROUP,process_send_group},			// send to all in the room, from "me"
	{SEND_ASK_PASSWORD,process_ask_password},		// is this room accepting spectators
	{SEND_ALL_GROUP,process_send_all_group},		// send to all in the room from someone else (ie; a robot I control)
	{SEND_REGISTER_PLAYER,process_register_player},		// register a new player (or spectator)
	{SEND_WRITE_GAME,process_write_game},			// save the game record to a file
	{SEND_RESERVE_ROOM,process_reserve_room},		// reserve this room for a game
	{SEND_SET_STATE,process_set_state},			// set the state (idle, running, setup ...)
	{SEND_NAME,process_send_name},				// set a player name for public use
	{SEND_SEAT,process_send_seat},				// set the seat position for me (taking over for a disconnected player)
	{SEND_MULTIPLE,process_multiple},			// send multiple commands as an atomic operation
	{SEND_STATE_KEY,process_recordStateKey},		// replace the state of the game
	{SEND_MESSAGE_TO,process_sendmessageto},		// send a message to a particular client
	{QUERY_GAME,process_query_game},			// is there an incomplete game with this id
	{FETCH_GAME,process_fetch_game},			// fetch the incomplete game with this id
	{FETCH_GAME_FILTERED,process_fetch_game_filtered},			// fetch the incomplete game with this id
	{FETCH_ACTIVE_GAME,process_fetch_active_game},		// fetch the game associated with this room
	{FETCH_ACTIVE_GAME_FILTERED,process_fetch_active_game_filtered},		// fetch the game associated with this room
	{SAVE_GAME,process_save_game},				// save the state of the active game
	{APPEND_GAME,process_append_game},			// append to the state of the active game
	{LOCK_GAME,process_lock_game},				// give us the control token for this game
	{REMOVE_GAME,process_remove_game},			// this game is no longer active
	{SEND_SUMMARY,process_send_summary},			// set the "info about" all the active games
	{SEND_ASK_DETAIL,process_ask_detail},			// get the details of who is in this room
	{SEND_LOBBY_INFO,process_send_lobby_info},		// describe all the active games
	{SEND_INTRO,process_send_intro},			// try to join the server (first message from a new client)
	{SEND_PING,process_ping},				// ping back to this client
	{SEND_CHECK_SCORE,process_check_score},			// has this game been finalized yet?
	{SEND_LOGSHORTNOTE,process_logshortnote},		// add a note to the log
	{SEND_LOGMESSAGE,process_logmessage},			// add to the log with server internals appended


#if PROXY
	{SEND_PROXY_OP,process_proxy_op},
	{SEND_PROXY_DATA,process_proxy_data},
#endif

	{ECHO_FAILED,process999}	// whatever you just sent me, I don't understand
};

// const in char*cmd because it is the live inpt buffer and 
// shouldn't be modified
// return new value of loopCtr, which is usually the same as the old value

static User *processCheckSummedMessages(char *cmd,User *u,char *rawcmd,char *seq)
{
	if (u->unexpectedCount > 0)
	{
		// security measure, if the user has sent garage, continue to accept and ignore
		// input until he times out.
		char shortCmd[100];
		char* dots = "";
		MyStrncpy(shortCmd, cmd, sizeof(shortCmd));
		if (strlen(shortCmd) < strlen(cmd))
		{
			dots = " ...";
		}
		if (u->use_rng_in && rawcmd)
		{
			logEntry(&mainLog, "[%s] unusual ignored encrypted message from C%d (%s#%d) S%d : %s%s\n",
				timestamp(), u->userNUM, u->clientRealName, u->clientUid, u->socket, rawcmd, dots);
		}
		else
		{
			logEntry(&mainLog, "[%s] unusual ignored message from C%d (%s#%d) S%d : %s%s\n",
			timestamp(), u->userNUM, u->clientRealName, u->clientUid, u->socket, shortCmd, dots);
		}
		return(u);
	}
	// process messages from the dispatch table
	{	int cmdidx=0;
		for(cmdidx=0;
			cmdidx<ARRAY_NUMBER_OF_ELEMENTS(commands);
			cmdidx++)
			{
			cmdp *pp = &commands[cmdidx];
			const char *str = pp->cmd;
			size_t len = strlen(str);
			cmdHandler fn = pp->disp;
			if(strncmp(cmd,str,len)==0)
				{	fn(cmd+len,u,seq);
					return(u);
				}
			}
	}
	if (strncmp(cmd,SEND_TAKEOVER,4) == 0)		// ugly special case, we want to change U
		   {	// 220 is the shutdown code
			   u=process_takeover(cmd+4,u,seq);
		   }
	else
	// If we get here, we got something unexpected instead of a command
	 {		unusual_events++;
			u->unexpectedCount++;
			if( (strncmp("GET ",cmd,4)==0)
				|| (strncmp("POST ",cmd,5)==0))
			{	// somebody pointed a browser at our port.  Not an accident.
				banUserByIP(u);
			}
			if(logging>=log_errors)
			{	
				if(u->use_rng_in && rawcmd)
				{
				DumpHistory();
				logEntry(&mainLog,"[%s] unusual encrypted message from C%d (%s#%d) S%d : %s\n",
					timestamp(),u->userNUM,u->clientRealName,u->clientUid,u->socket,rawcmd);
				}
				else
				{
				// this is where "zombie connections" are registered, just ban the sucker
				if(u->nread<=1)
				{	logEntry(&mainLog,"[%s] unusual unexpected message from C%d (%s#%d) S%d : %s\n",
						timestamp(),
						u->userNUM,
						u->clientRealName,
						u->clientUid,
						u->socket,cmd);
					banUserByIP(u);
				}else
				{
				DumpHistory();
				logEntry(&mainLog,"[%s] unusual unexpected message from C%d (%s#%d) S%d : %s\n",
					timestamp(),
					u->userNUM,
					u->clientRealName,
					u->clientUid,
					u->socket,cmd);
				}}
			}
		// this used to be "ECHO FAILED" but changed in order to not give information
		// to hackers about what is acceptable.
			if (u->session != WAITINGSESSION)
			{
				lsprintf(u->tempBufSize, u->tempBufPtr, ECHO_GROUP_SELF "%s", cmd);
				sendSingle(u->tempBufPtr, u);
			}
			else {
				simpleCloseClientOnly(u,"bad input from waiting session");
				removeUserFromSession(u);
				putUserInSession(u,IDLESESSION);
			}
       }
	return(u);	//loopCtr can change when reconnecting

}

User *lastUsers[3];	// for emergency debugging
static User *ProcessMessages(char *cmd,User *u)
{  char rawcmd[120];
   int ischecksummed=0;
   lastUsers[2]=lastUsers[1];
   lastUsers[1]=lastUsers[0];
   lastUsers[0]=u;

   if(*cmd==(char)0) { return(u); }

   MyStrncpy(rawcmd,cmd,sizeof(rawcmd));
   u->nread++;			//count lines read

	transactions++;

#if PROXY
	{ User *pfor = u->proxyFor;
	  if(pfor)
	  {	
		lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_PROXY_DATA "%d %s",u->userNUM,cmd);
		sendSingle(u->tempBufPtr,pfor);
		return(u);
	  }
	}
#endif
	if(u->use_rng_in && !u->checksums)
	{	// just decrypt, we don't care about the checksum
		checksumInputString(u,cmd);
		recordInHistory(" in",u->socket,cmd,strlen(cmd));
		if(logging>=log_all) 
			{
			logEntry(&mainLog,"[%s] received from C%d (%s#%d) S%d : %s\n",
				timestamp(),u->userNUM,u->clientRealName,u->clientUid,u->socket,cmd); 
			}
	}
	else if(strncmp(cmd,"500 ",4)==0)
	{	int sum1 = cmd[4]-'A';
		int sum2 = cmd[5]-'A';
		int sum3 = cmd[6]-'A';
		int sum4 = cmd[7]-'A';
		int sum = (sum1<<12)+(sum2<<8)+(sum3<<4)+sum4;
		int asum= checksumInputString(u,&cmd[9]);			//actual checksum
		recordInHistory(" in",u->socket,cmd,strlen(cmd));
		u->checksums=TRUE;					//remember that this guy understands checksums
	
		if(logging>=log_all) 
		{
		logEntry(&mainLog,"[%s] received from C%d (%s#%d) S%d : %s\n",
			timestamp(),u->userNUM,u->clientRealName,u->clientUid,u->socket,cmd); 
		}

		if(asum==sum)
		{ cmd+=9; ischecksummed=1;					//checksum matches
		}else
		{checksum_errors++;
		 if(logging>=log_errors)
		 {logEntry(&mainLog,"[%s] C%d (%s#%d) S%d in session %d checksum error\nMessage was %s\n",
						timestamp(),u->userNUM,u->clientRealName,u->clientUid,u->socket,
						u->session->sessionNUM,cmd);

#if HISTORY
		DumpHistory();
#endif
		 }
		lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_FAILED "%s",cmd);
		sendSingle(u->tempBufPtr,u);
		return(u);
		}
	}
	else if(u->checksums)
		{	//if this client normally sends checksums, regard this as trash
		 checksum_errors++;
		 if(u->use_rng_in) { checksumInputString(u,cmd); }	// decrypt only	
		 recordInHistory(" in",u->socket,cmd,strlen(cmd));
		 if(logging>=log_all) 
			{
			logEntry(&mainLog,"[%s] received from C%d (%s#%d) S%d : %s\n",
				timestamp(),u->userNUM,u->clientRealName,u->clientUid,u->socket,cmd); 
			}

		 if(logging>=log_errors)
		 {logEntry(&mainLog,"[%s] C%d (%s#%d) S%d in session %d  unchecksummed string\nMessage was %s\n",
						timestamp(),u->userNUM,u->clientRealName,u->clientUid,u->socket,
						u->session->sessionNUM,cmd);
		#if HISTORY
		DumpHistory();
		#endif
		 }
		 return(u);			//punt out
		}
	else {
		recordInHistory(" in",u->socket,cmd,strlen(cmd));
		if (logging >= log_all)
			{
				logEntry(&mainLog, "[%s] received from C%d (%s#%d) S%d : %s\n",
					timestamp(), u->userNUM, u->clientRealName, u->clientUid, u->socket, cmd);
			}
		}

	// this shouldlnt' be here because strings that are
	// passed through should be in their encoded form.
	// unescapeString(cmd);			// remove escapes, produce the real message
	{
	char sequenceNumber[64] = {0};
	if(cmd[0]=='x')
		{
		// sequence numbers
		int seqn = 0;
		int idx = safe_scanint(cmd+1,&seqn);	
		if(idx>0)
		{ MyStrncpy(sequenceNumber,cmd,sizeof(sequenceNumber)<idx+3?sizeof(sequenceNumber):idx+3);
		  cmd += 2+idx;
		}
		if(seqn != u->rng_in_seq)
		{	if(u->rng_in_seq_errors==0)
			{
			unusual_events++;
			DumpHistory();
			logEntry(&mainLog,"[%s] Unusual: C%d (%s#%d) S%d  session %d input seqence error, is %d expected %d\n",
				 timestamp(),
				 u->userNUM,u->clientRealName,u->clientUid,u->socket,							
				 u->session->sessionNUM,
				seqn,u->rng_in_seq )	;

			}
			u->rng_in_seq_errors++;
		}
		u->rng_in_seq++;
		}

	return(processCheckSummedMessages(cmd,u,rawcmd,sequenceNumber));
	}
}

/*--------------------------------------------------------------------*/
static int containsNewline(User *u,int count,int end)
{	char *buffer=u->inbufPtr;
	int eol=0;
	while((count<end) && !eol)
	{unsigned char ch = buffer[count];
	switch(ch)
		{case 10:	
			buffer[count++]=(char)0;
			eol = count;
			break;
		 case 13:	
			buffer[count++]=(char)0;; /* remove cr */ 
			eol = count;
			break;
		 default:	
			count++;
		}
	}
	return(eol);
}

// fill the input buffer when a socket is believed to be active.
// the command structure is uniformly 1 line = 1 command.  Sometimes
// this results in very int lines (with embedded linefeeds encoded)
//
// if the reader has caught up with the filler, reset the buffer
// to the beginning.  If the buffer is in danger of running over
// the end, shuffle the buffer (this should happen rarely).  In
// any case, lines in the completed buffer are always in one piece.
//
static int fillBuffer(User *u)
{
	int doneReading=0;
	int eol=0;
	assert((u->topGuard==0xDEADBEEF)&&(u->bottomGuard==0xDEADBEEF)&&(u->midGuard==0xDEADBEEF));
    while (doneReading == 0)
	{ int put = u->inbuf_put_index;
	  int take = u->inbuf_take_index;
	  int siz = (u->inbufSize-put)-1;
	  if(put==take) 
		{ //buffer empty, reset to the start
		  u->inbuf_put_index=u->inbuf_take_index=take=put=0; 
		  siz = u->inbufSize-1;		// leave room for a null
		}
	  if((take>0) &&(siz<100))
	  {	//shuffle the buffer so we don't hit the end
		char *buffer = u->inbufPtr;
		int nchars = put-take;
		MEMMOVE(buffer,buffer+take,nchars);
		u->inbuf_take_index=take = 0;
		u->inbuf_put_index = put= nchars;
		siz = u->inbufSize-put-1;
#if HISTORY
		recordInHistory("shf",u->socket,buffer+take,nchars);
#endif

	  }
	  if(((siz+siz) < BUFFER_ALLOC_STEP) && (u->inbufSize<MAX_INPUT_BUFFER_SIZE))
	  {
		increaseInputBufferSize(u,siz);
		siz = u->inbufSize - u->inbuf_put_index -1;	// leave room for a null
	  }
	  // kill any socket the tries to overstuff the buffer.
	  if(siz <=0)
		{ 
		  lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_I_QUIT "tooMuchData %d",put);
		  doEchoSelf(u,u->tempBufPtr,1);
		  lsprintf(u->tempBufSize,u->tempBufPtr,ECHO_PLAYER_QUIT "%d tooMuchData %d",u->userNUM,put);
		  doEchoAll(u,u->tempBufPtr,1);
		  put=0;
		  doneReading=1;
		  u->wasZapped=TRUE;
		  if(logging>=log_errors)
			{logEntry(&mainLog,"[%s] Zap C%d (%s#%d) S%d supplied too much data=%d bytes\n",
				timestamp(),u->userNUM,u->clientRealName,u->clientUid,u->socket,put);
			}
		client_errors++;
  		closeClient(u,"tooMuchData",grace_forbidden);
		}
	  else {
	  char *buffer = u->inbufPtr;
	  buffer[put]=(char)0;
	  {
	  SOCKET sock = u->socket;
	  int readResult = u->websocket
						? websocketRecv(u,buffer+put,siz)
						: recv(sock, buffer+put, siz,0);
	  switch(readResult)
	  { case 0:	
			// note that reading zero bytes always seems to be an error.  Sometimes
			// it doesn't immediately progress into real errors (returning -1) but it
			// never recovers into reading real data.  Some sources claim reading 0 means
			// the other side closed the stream normally
		  if (u->websocket)
		  {	  // no more read required, but not necessarily an error
			  doneReading = 1;
		  }
		  else
		  {
			  int err = ErrNo();
			  doneReading = 1;
			  if (!u->wasZapped
				  && (err != EWOULDBLOCK)
				  && (err != EAGAIN)
				  && (u->session != LOBBYSESSION)
				  && (u->session != WAITINGSESSION)
				  && (u->session != PROXYSESSION))
			  {
				  client_errors++;
				  u->wasZapped = TRUE;
				  if (logging >= log_errors)
				  {
					  logEntry(&mainLog, "[%s] Zap C%d (%s#%d) S%d reading 0 bytes errno=%d\n",
						  timestamp(), u->userNUM, u->clientRealName, u->clientUid, u->socket, err);
				  }
			  }
			  put = 0;
			  u->expectEof = TRUE;
			  HandleReadError(u, err);
		  }
		break;

	   case -1:	//various error possibilities
		doneReading=1;
		{int err = u->websocket_errno;
		if (err == 0) { err = ErrNo(); }
		 switch(err)
			 {
		 
		     case EWOULDBLOCK:			//would block; not an error
#if (EWOULDBLOCK!=EAGAIN)
			 case EAGAIN:
#endif
				 	// not applicable to websockets
					 readBreak++;
#if HISTORY
					 if (!u->use_rng_in)
					 { // not very informative if encrypted.
						 recordInHistory("iXX", u->socket, buffer + take, put - take);
					 }
#endif
					 break;
				 
			default:	//other errors
			  {
				if((u->session!=LOBBYSESSION) 
					&& (u->session!=PROXYSESSION)
					&& (u->session!=WAITINGSESSION))
				{ client_errors++;
				  u->wasZapped=TRUE;
				  if(logging>=log_errors)
					{ logEntry(&mainLog,"[%s] Zap C%d (%s#%d) S%d error reading, errno=%d\n",
								timestamp(),u->userNUM,u->clientRealName,u->clientUid,u->socket,err);
					}
				}
			  }
#if WIN32
			 case WSAECONNRESET:
				 // client closed the connection 
#endif
				put=0;
				doneReading = 1;
				HandleReadError(u,err);
				}
			}
			break;
		default:
			buffer[put+readResult]=(char)0;
			u->session->sessionTimes = u->clientTime=Uptime();
			u->totalread+=readResult;
			totalread+=readResult;
			u->inbuf_put_index += readResult;
			eol=containsNewline(u,u->inbuf_take_index,put+readResult);
			doneReading = (eol!=0);
			}
	  }
	  }
	  /* end of actually reading */
	}/* end of while */
	assert((u->topGuard==0xDEADBEEF)&&(u->bottomGuard==0xDEADBEEF)&&(u->midGuard==0xDEADBEEF));

	return(eol);
}
void closePendingConnections(unsigned int ip)
{
	Session *S = WAITINGSESSION;
	User* U = S->first_user;
	while (U != NULL)
	{
		User* next = U->next_in_session;
		if (U->ip == ip)
		{
			simpleCloseClientOnly(U, "one of too many");
			removeUserFromSession(U);
			putUserInSession(U, IDLESESSION);
		}

		U = next;
	}

}
static void acceptNewConnections(SOCKET insoc,BOOLEAN websock)
{	 
	socklen_t fl=sizeof(iclient);
	SOCKET newsocket = accept(insoc,(struct sockaddr *)&iclient,&fl);
	 unsigned int ip = htonl(iclient.sin_addr.s_addr);

	 if(newsocket==INVALID_SOCKET ) 
	 {
	   if(logging>=log_errors)
			{ logEntry(&mainLog,"[%s] Connection attempted, %saccept failed, code %d errno %d.\n",
				  timestamp(),websock?"websocket ":"",newsocket, ErrNo());
			}
	 }
	 else if (checkBannedIP(ip,websock)) {
		 closesocket(newsocket);
	 }
	 else if (checkTooMany(ip,websock))
	 {
		 banIP(ip);
		 closesocket(newsocket);
		 closePendingConnections(ip);
	 }
	 else 
	 { 
	 if(setNBIO(newsocket))		//possibly fail ioctl
	 { 
	 User *u= findAslot(WAITINGSESSION);	//possibly fail to find a slot
	 if(u)
	   {
	 u->socket = newsocket;
	 u->websocket = websock;
	 u->websocket_data = NULL;
	 u->websocket_errno = 0;
	 u->ip = ip;
	 u->clientTime = Uptime();
	 sockets_open++;
	 if(maxsockets<sockets_open) { maxsockets = sockets_open; }
	 if(sockets_open==1)
		{UPTIME time_now=Uptime();
		 UPTIME idle = time_now - start_idle_time;
		 idle_time += idle;
		  if(logging>=log_connections && (idle>30.0))
		  {  logEntry(&mainLog,"[%s] was idle %d minutes\n",timestamp(),(idle+30)/60);
		  }

		}

          if(logging>=log_connections)
			{int b4 = (ip>>0)&0xff;
			 int b3 = (ip>>8)&0xff;
			 int b2 = (ip>>16)&0xff;
			 int b1 = (ip>>24)&0xff;
			  logEntry(&mainLog,"[%s] Call C%d S%d from %d.%d.%d.%d queued to join a %ssession.\n",
			  timestamp(),u->userNUM,u->socket,
			  b1,b2,b3,b4,websock?"websocket ":"");
			}

           // succeeded
           return;
	   }
	 }
	 // failed for some reason, no U allocated
	 {//no room at the end, send a curt refusal
	  if(logging>=log_connections)
		{int b4 = (ip>>0)&0xff;
		 int b3 = (ip>>8)&0xff;
		 int b2 = (ip>>16)&0xff;
	         int b1 = (ip>>24)&0xff;
		 logEntry(&mainLog,"[%s] Call from %d.%d.%d.%d refused %s(no room or iofail)\n",
			  timestamp(),b1,b2,b3,b4,
			 websock?"websocket ":"");
		}
	  simpleCloseSocket(newsocket);

	 }}
}

static void clearOrphanedSessions()
{
	/* look for orphaned sessions */
 int sess;
 BOOLEAN some=FALSE;
 UPTIME tempTime = Uptime();
 for(sess=1;sess<=maxSessions;sess++)
 { Session *s = &Sessions[sess];
   int clear = s->sessionClear;
   int sesst = clear?SESSIONCLEARTIMEOUT:SESSIONTIMEOUT;
   if ((((s->sessionKey!=0) || (s->sessionURLs[0]!=(char)0))||(clear))
		&&((tempTime - s->sessionTimes) > sesst)
		&& sessionIsEmpty(s))
   { 
	 /* we didn't find a client in this session */
	  if(logging>=log_connections)
		{if(clear)
		{logEntry(&mainLog,"[%s] clearing abandoned session %d\n",timestamp(),sess);
		}else
		{logEntry(&mainLog,"[%s] clearing session password for abandoned session %d\n",timestamp(),sess);
		}
	  }
	  some=TRUE;
	  if(clear) {clearSession(s); } else { clearEmptySession(s); }
   }
   if(Sessions[sess].sessionDescribe)
   {
	DescribeSession(sess);
	s->sessionDescribe=FALSE;
   }

 }
 if(some) { saveConfig(); }
}
static int sessionTimeout(Session *s)
{	if(s==LOBBYSESSION) { return(lobby_timeout); }
	if(s==WAITINGSESSION) { return(MAXWAITINGTIME); }
	if(s==PROXYSESSION) { return(MAXPROXYTIME); }
	return(client_timeout);
}

/* config parameters */
typedef struct pair
{	char *name;
	char *value;
} pair;

#define MAXPARAM 2000
#define PAIRBUFSIZE 10000
char pairbuf[PAIRBUFSIZE];
size_t pairidx=0;
pair params[MAXPARAM];
int nparam=0;



char *save_string(char *str)
{	size_t len = strlen(str);
	char *start = pairbuf+pairidx;

	if((len+pairidx+1)>=sizeof(pairbuf)) 
		{
		error1("config file too long"); 
		}
	pairidx += len+1;
	strcpy(start,str);
	return(start);
}
	
void save_param(char *key,char *val)
{	if(nparam<MAXPARAM)
	{
		params[nparam].name=key;
		params[nparam].value=val;
		nparam++;
		printf("Adding K: %s V: %s\n",key,val);
	}
	else
	{
		error1("Too many parameters in config file");
	}
}

void DisposeConfigFile()
{
	pairidx=0;
	nparam=0;
}

void ReadConfigFile()
{
	static char line[256]={0};
	FILE *stream;

	DisposeConfigFile();		//dispose of the old cache

	if((stream=fopen(configFile,"r")))
	{
		while(fgets(line,sizeof(line),stream))
		{	if(line[0]!='#')
			{ char *split = strchr(line,',');
			  if(split)
			  { size_t len=strlen(line);
				while((len>0) && line[len-1]<=' ') { line[--len] = (char)0; }
				*split=(char)0;
				save_param(save_string(line),save_string(split+1));
			  }
		}}
	fclose(stream);
	}
	else
	{error2("Couldn't open config file",configFile);
	}
}

char* GetOptionalParm(char* param,char *def)
{
	static char dummy[4] = { 0 };
	if (nparam == 0)
	{
		ReadConfigFile();
	}
	{int i;
	for (i = 0; i < nparam; i++)
	{
		if (strcmp(params[i].name, param) == 0)
		{
			return(params[i].value);
		}
	}
	}
	return def;
}

char *GetParm(char *param)
{	static char dummy[4]={0};
	if(nparam==0)
		{ ReadConfigFile();
		}
	{int i;
	 for(i=0; i<nparam; i++)
	 {
		if(strcmp(params[i].name,param)==0)
		{	return(params[i].value);
		}
	 }
	}
	error2("missing config parameter",param);
	return("");
}

#if !WIN32
static void changelim_max(int which,char *name)
{   struct rlimit limit;
    int getrv=getrlimit(which,&limit);
    if(getrv==0)
	{
	 int cur=limit.rlim_cur;
	 limit.rlim_cur=limit.rlim_max;
	 if((getrv=setrlimit(which,&limit))!=0)
	 {char buf[SMALLBUFSIZE];
	  lsprintf(sizeof(buf),buf,"setrlimit %s from %d to %d failed with code %d\n",name,cur,limit.rlim_max,getrv);
	  logEntry(&mainLog,buf);
	 }
	 else 
	 { logEntry(&mainLog,"limit %s set from %d to %d\n",name,cur,limit.rlim_max);
	 }
	 int getrv2=getrlimit(which,&limit);
         logEntry(&mainLog,"new rlimit %s %d\n",name,limit);
	}else
	{ error("getrlimit failed with error ",getrv);
	}

  }

#endif

int setSessionSocks(Session *S,int nActive)
{	User *u=S->first_user;
	int nfound=0;
	int pop = S->population;
	while(u)
	{ SOCKET sock = u->socket;
	  User *next = u->next_in_session;
	  assert(u->session==S);
      if (isRealSocket(sock)) 
	  {	if(sock>maxfd) { maxfd=sock; };
		assert(nActive<ARRAY_NUMBER_OF_ELEMENTS(activeClients));
		activeClients[nActive++]=u;
        FD_SET(sock,&rfds);
        if((u->outbuf_put_index!=u->outbuf_take_index) 
#if PROXY
	   || u->connecting
#endif
	   )
		{ FD_SET(sock,&wfds);
		}
        FD_SET(sock,&efds);
      }
	  u=next;
	  nfound++;
	  assert(nfound<=pop);
	}
	assert(nfound==pop);
	return(nActive);
}

void main_thread()
{
  int actCtr=0;
  int round_robin_salt=0;

  logEntry(&mainLog,"[%s] main process started\n",timestamp());

	{	int loopCtr;
		for (loopCtr=0;loopCtr<MAXCLIENTS;loopCtr++) { ClearUser(&Users[loopCtr]); };
	}

  if(use_threads)
	{
#if WIN32
	statusThreadId = _beginthread(statusthread,0,0);
#else
	pthread_create(&statusThreadId,NULL,statusthread,NULL);
 //       int  pthread_create(pthread_t  *  thread, pthread_attr_t * attr, void * (*start_routine)(void *), void * arg);

#endif

#if WIN32
	saveGameThreadId = _beginthread(saveDirtyGameThread,0,&saveGameInfo);
#else
	pthread_create(&saveGameThreadId,NULL,saveDirtyGameThread,&saveGameInfo);
 //       int  pthread_create(pthread_t  *  thread, pthread_attr_t * attr, void * (*start_routine)(void *), void * arg);

#endif

	}
 
#if !WIN32
	  { changelim_max(RLIMIT_CORE,"Core Dump Size");
		changelim_max(RLIMIT_NOFILE,"Max Files");
	  }
#endif
	
  start_idle_time = start_time = bktime = Uptime();
  saveConfig();
  idle_time=0;
  for(;;) 
  {	int nActive=0;
	/* we keep track of the sockets involved in the main select, so changes in their
	status while processing don't confuse us */
    FD_ZERO(&rfds);
    FD_ZERO(&wfds);
    FD_ZERO(&efds);
    maxfd=serversd;
    if(serversd!=0)
	{FD_SET(serversd,&rfds);
         FD_SET(serversd,&efds);
	}
#if WEBSOCKET
	if (webserversd != 0)
	{       if(webserversd>maxfd) { maxfd = webserversd; }
		FD_SET(webserversd, &rfds);
		FD_SET(webserversd, &efds);
	}
#endif

	{
	//
	// this was moved up here because "dead" users were being
	// processed in ProcessMessages if there were some scraps
	// of input and output left in their buffers.
	//
	HandleDeadUsers();		// first spread the bad news

	{
    for (actCtr=0;actCtr<=maxSessions;actCtr++) 
	{ nActive=setSessionSocks(&Sessions[actCtr],nActive);
	}
	nActive=setSessionSocks(WAITINGSESSION,nActive);
	nActive=setSessionSocks(PROXYSESSION,nActive);
	}	
	
	{ 
	struct timeval waittime;
	  waittime.tv_sec = MAXWAITTIME;
	  waittime.tv_usec=0;
    while (select((unsigned int)maxfd+1,&rfds,&wfds,&efds,(struct timeval *)&waittime) == -1) 
	{
      if (errno != EINTR) {
        error("Failed select",ErrNo());
      }
    }}}
    if ((serversd!=0) && FD_ISSET(serversd,&efds)) {
      error("Server socket exception", ErrNo());
    }
#if WEBSOCKET
	if ((webserversd != 0) && FD_ISSET(webserversd, &efds)) {
		error("Server web socket exception", ErrNo());
	}
#endif
    if(nActive>0)
	{int idx;
	round_robin_salt++;
	if(round_robin_salt>=actCtr) { round_robin_salt=0; }
    for (actCtr=round_robin_salt,idx=0;idx<nActive;idx++,actCtr++) 
	{ if(actCtr>=nActive) { actCtr=0; }
	{UPTIME tempTime=Uptime();
	  User *u=activeClients[actCtr];
	  int dif=0,tim=0;
	  SOCKET sock = u->socket;
	  int errval = 0;

      if (isRealSocket(sock)) 
	  {
        if (FD_ISSET(sock,&efds)) 
		{	//check for errors first
		  socklen_t errsiz=sizeof(errval);
		  getsockopt(u->socket,SOL_SOCKET,SO_ERROR,(char *)&errval,&errsiz);

#if PROXY
	  if(u->connecting)
	  {
		doSocketConnectFailed(u,errval);
	  }
	  else
#endif
      if(logging>=log_none)
			{logEntry(&mainLog,"[%s] C%d (%s#%d) S%d error in select err = %d\n",
			timestamp(),u->userNUM,u->clientRealName,u->clientUid,u->socket,errval);
			}
 		  HandleReadError(u,errval);
        } 
		else if (FD_ISSET(sock,&rfds) != 0) 
			{	//read data from a socket
				int newline=0;
				if((newline=fillBuffer(u))!=0)
				{
				u->clientTime = tempTime;		// only reset the timer if we finish a buffer
				char *inbuf = u->inbufPtr+u->inbuf_take_index;
				 u->inbuf_take_index = newline;
				 if(!u->inputClosed)
				 {
				 // for misbehaving clients, we tell them to die but let them fall on their
				 // swords gracefully.  ->inputClosed is the backstop to make sure that even
				 // if they don't die, they can't do anything.
				 u=ProcessMessages(inbuf,u);
				 }
				 //be very careful here, as processmessages can rearrange
				 //the users, close sockets, etc.
				 while(isRealSocket(u->socket)
						&& (newline=containsNewline(u,u->inbuf_take_index,u->inbuf_put_index)))
				 {	/* process additional lines */
					inbuf = u->inbufPtr+u->inbuf_take_index;
					u->inbuf_take_index = newline;
					if(!u->inputClosed) { u=ProcessMessages(inbuf,u); }
				 }
				}
			}
		else if (FD_ISSET(sock,&wfds) != 0)
			{//write data to a socket
#if PROXY
			if(u->connecting)
			{
			doSocketConnected(u);
			}
			else
#endif
			{
			  if((restartOutput(u))>0)
			    {
			      // reset timeout if progress was made
			      u->clientTime=tempTime;
			    }
			}
			}
#if TIMEOUTS
		else 
		{int dif= (tempTime - u->clientTime);
		 Session* S = u->session;
		 int tim = sessionTimeout(S);
		 if ( dif > tim)
		{ 
			 if (S != WAITINGSESSION)
			 {
				 char buf[SMALLBUFSIZE];
				 lsprintf(sizeof(buf), buf, ECHO_PLAYER_QUIT "%d timeout", u->userNUM);
				 doEchoOthers(u, buf, 1);
				 lsprintf(sizeof(buf), buf, ECHO_I_QUIT "timeout");
				 doEchoSelf(u, buf, 1);
				 lsprintf(sizeof(buf), &buf[0], "C%d (%s#%d) S%d timeout, (%d>%d)", u->userNUM, u->clientRealName, u->clientUid, u->socket, dif, tim);
				 errMsg(__LINE__, u->socket, &buf[0]);
				 closeClient(u, "timeout", grace_mandatory);
			 }
			 else
			 {
				 simpleCloseClientOnly(u, "waiting session timeout");
				 removeUserFromSession(u);
				 putUserInSession(u, IDLESESSION);
			 }
          
        }}
#endif
      }
	}}}
    if ((serversd!=0) && FD_ISSET(serversd,&rfds))	
		{acceptNewConnections(serversd,FALSE); }
#if WEBSOCKET
	if((webserversd!=0) && FD_ISSET(webserversd, &rfds))
	{
		acceptNewConnections(webserversd, TRUE);
	}
#endif
	{	SaveGameInfo *G = &saveGameInfo;
		UPTIME now = Uptime();
		if(now>bktime)	// once per second
		{bktime = now;
		 clearOrphanedSessions();
		 if(THREAD_READ(G,gameCacheRate)>0) 
			{ saveDirtyGames();		// move some dirty games to the write buffer
			}
		}
	}
	
	}

  logEntry(&mainLog,"[%s] main process exited",timestamp());
}


int main(int argc, char **argv)
{
  SaveGameInfo *G = &saveGameInfo;
  mainLog.logStream = stderr;
  mainLog.renameLogFile = TRUE;
  chatLog.logStream = stderr;
  chatLog.renameLogFile = TRUE;
  securityLog.logStream = stderr;
  securityLog.renameLogFile = TRUE;
  securityLog.notflushable=TRUE;
  WAITINGSESSION->sessionNUM = -1;
  if (argc != 2) {
    logEntry(&mainLog,"<echoServer command> <configFile>\n");
    ExitWith(1);
  }
  srand((unsigned)time( NULL ));				//initialize random sequence

  {int loopCtr;
   for (loopCtr=0;loopCtr<MAXSESSIONS;loopCtr++) 
   {Session *s = &Sessions[loopCtr];
    s->sessionNUM=loopCtr;
    s->sessionGame=NULL;
	s->sessionStateKey[0] = (char)0;
	s->sessionStateKeyIndex=-1;
    s->sessionURLs[0] = 0;
	s->sessionInfo[0] = 0;
	s->sessionHasGame = 0;
	s->sessionIsPrivate = 0;
	s->sessionStates=0;
	s->poisoned = FALSE;
	s->sessionGameID=0;
	s->sessionKey=0;
	s->sessionScored=0;
	s->sessionFileWritten=0;
	s->sessionClear=0;
	s->sessionDescribe=0;		//need to describe this
	s->first_user=0;
   }

   for(loopCtr=0;loopCtr<ARRAY_NUMBER_OF_ELEMENTS(Users);loopCtr++)
   {	Users[loopCtr].userNUM=FIRSTUSERNUM+loopCtr;
		Users[loopCtr].topGuard=0xDEADBEEF;
		Users[loopCtr].bottomGuard=0xDEADBEEF;
		Users[loopCtr].midGuard=0xDEADBEEF;
		//put all the users in the idle session
		putUserInSession(&Users[loopCtr],IDLESESSION);
   }
   for(loopCtr=0;loopCtr<ARRAY_NUMBER_OF_ELEMENTS(all_registered_users);loopCtr++)
   {
		// create  a linked list of available registered users
		all_registered_users[loopCtr].next = freeRegisteredUsers;
		freeRegisteredUsers=&all_registered_users[loopCtr];
   }
  
  };
 
  strcpy(configFile,argv[1]);


  mainLog.logFileName = GetParm("logfile");
  chatLog.logFileName = GetParm("chatfile");
  securityLog.logFileName = GetParm("securityfile");
  max_logfile_size = (1024*1024)*atoi(GetParm("max_logfile_megabytes"));
  THREAD_WRITE(G,gameCacheDir,GetParm("gamecachedir"));
  THREAD_WRITE(G,gameCacheRate,atoi(GetParm("gamecacherate")));

   if (sscanf(GetParm("bindip"),"%d.%d.%d.%d",&IP1,&IP2,&IP3,&IP4) != 4) 
   {
     error1("Couldn't find IP address in config file param \"bindip\"");
   };
  server_ip = htonl((IP1<<0)|(IP2<<8)|(IP3<<16)|(IP4<<24));
  alt_server_ip = htonl((127<<0)|(1<<24)); 
  portNum=atoi(GetParm("port"));
  logging = atoi(GetParm("loglevel"));
  supervisor = GetParm("password");
  lobby_timeout = atoi(GetParm("lobby-timeout"));
  client_timeout = atoi(GetParm("client-timeout"));

#if 0
  sscanf(GetParm("key"),"%x",&security_key);
  if(!CheckSecurity(security_key))
  { 
    error1("Keycode mismatch");
  }
#endif

  client_socket_init(portNum);
#if WEBSOCKET
  websocketPortNum = atoi(GetParm("websocket"));
  if (websocketPortNum > 0)
  {
	  client_websocket_init(websocketPortNum);
  }
  websocketSslKey = GetOptionalParm("websocketsslkey",NULL);
  websocketSslCert = GetOptionalParm("websocketsslcert",NULL);
#endif
  //
  // get here (past the socket init) only if we actually get the socked.
  //
  openLogFile(&mainLog);
  openLogFile(&chatLog);
  openLogFile(&securityLog);

  logFilesEnabled=TRUE;
  logEntry(&mainLog,"[%s] started\n",timestamp());
#if X64
  {
  char *p = NULL;
  logEntry(&mainLog,"Server 64 bit version, sizeof int = %d sizeof long = %d sizeof char * = %d\n",sizeof(int),sizeof(long),sizeof(char*));
  if(sizeof(p)!=8) { logEntry(&mainLog,"Size mismatch, compiled sizeof pointer is %d\n",sizeof(p)); }
  }
#else
  {
  char *p = NULL;
  logEntry(&mainLog,"Server 32 bit version, sizeof int = %d sizeof long = %d sizeof char * = %d\n",sizeof(int),sizeof(long),sizeof(char*));
  if(sizeof(p)!=4) { logEntry(&mainLog,"Size mismatch, compiled sizeof pointer is %d\n",sizeof(p)); }
  }
#endif
  logEntry(&mainLog,"Compiled limits: buffer %d, User %d(=%dm), Session %d(=%dm), GameBuffers %d(=%dm)\n",
	  MAX_INPUT_BUFFER_SIZE,
	  MAXCLIENTS,((MAXCLIENTS*sizeof(User))/(1024*1024)),
	  MAXSESSIONS,((MAXSESSIONS*sizeof(Session))/(1024*1024)),
	  MAXGAMES,((MAXGAMES*sizeof(GameBuffer))/(1024*1024))
	  );

  logEntry(&mainLog,"[%s] logging is %d, password is \"%s\",software key is %x\n",
	  timestamp(),logging,&supervisor[0],security_key);
  logEntry(&chatLog,"[%s] started\n",timestamp());
  logEntry(&securityLog,"[%s] started\n",timestamp());



  statusfile = GetParm("statusfile");
  playerInfoUrl = GetParm("player-info-url");
  ngamedirs = atoi(GetParm("ngamedirs"));
  gamedirs = ALLOC(ngamedirs*sizeof(char *));
  { int i;
	for(i=0;i<ngamedirs;i++)
	{	char temp[15];
		lsprintf(sizeof(temp),temp,"gamedir%d",i);
		gamedirs[i]=GetParm(temp);
	}
  }

  ngametypes = atoi(GetParm("ngametypes"));
  gametypes = ALLOC(ngametypes*sizeof(char *));
  { int i;
	for(i=0;i<ngametypes;i++)
	{	char temp[15];
		lsprintf(sizeof(temp),temp,"gametype%d",i);
		gametypes[i]=GetParm(temp);
	}
  }

  nroomtypes = atoi(GetParm("nroomtypes"));
  roomtypes = ALLOC(nroomtypes*sizeof(char *));
  { int i;
	for(i=0;i<nroomtypes;i++)
	{	char temp[15];
		lsprintf(sizeof(temp),temp,"roomtype%d",i);
		roomtypes[i]=GetParm(temp);
	}
  }

  maxClients = atoi(GetParm("players"));
  maxSessions = atoi(GetParm("rooms"));
  maxConnections = atoi(GetParm("connections"));
  maxConnectionsPerSession = atoi(GetParm("connections-per-session"));
  maxConnectionsPerUID = atoi(GetParm("connections-per-uid"));
  strict_login = atoi(GetParm("strict"));
  strict_score = atoi(GetParm("strict-score"));

  // if nonzero, the server will expect clients to encrypt everything after
  // the initial connection dance.
  require_rng = atoi(GetParm("obfuscation"));
  require_seq = atoi(GetParm("sequencenumbers"));

  {	logEntry(&mainLog,"Max of %d users and %d sessions and %d pending connections per IP\n%d connections per session per IP\n%d connections per UID per IP\n",
			maxClients,
			maxSessions,
			maxConnections,
			maxConnectionsPerSession,
			maxConnectionsPerUID);
    if(strict_login) { logEntry(&mainLog,"strict logins are being enforced\n");}
	if (maxClients > MAXCLIENTS) 
		{
        error("Exceeded maximum number of users; max value is ",MAXCLIENTS);
      };
 	if (maxSessions > LASTREALSESSION)	//reserve 1 for WAITINGSESSIONNUM PROXYSESSION
		{
        error("Exceeded maximum number of sessions; max value is ",MAXSESSIONS-2);
      };

  }

  use_threads = atoi(GetParm("use-threads"));
  crash_test = atoi(GetParm("crash-test"));

  reloadGames();
  gameHashCleanup();
  {
  int detatch = atoi(GetParm("detatch")) && use_threads;

#if !WIN32
  if(detatch)
	{
	  int pid = fork ();
	  if (pid == 0) // if we're the child
	   { setsid(); // Start new process group.
	     pid = fork(); //Second fork will start detatched process.
	     if(pid==0) { main_thread(); }
	   }
	  else if(pid==-1)
	  { // if this actually occurs, the server is probably in trouble
		// error 12 is "failed to allocate memory"
	      logEntry(&mainLog,"fork failed!, errno=%d (will run attached)\n",errno);
	      logEntry(&securityLog,"fork failed!, errno=%d (will run attached)\n",errno);
	      printf("fork failed!, errno=%d (will run attached)\n",errno);
	      main_thread();
	    }
	    else
	    {printf("child thread is %d\n",pid);
	    }
	}
	else
#endif
	{
	// hashtest();
	main_thread();
	}
  }
	return(0);

}
