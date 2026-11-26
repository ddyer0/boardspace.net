package net.boardspace.migs;

/** one per game room (C struct Session) */
final class Session
{
    long sessionTimes;
    int sessionNUM;
    int sessionStates;
    int sessionGameID;
    User first_user;
    User locker;
    boolean poisoned;
    boolean sessionDescribe;
    int population;
    GameBuffer sessionGame;
    int sessionStateKeyIndex = -1;
    String sessionStateKey = "";	// SMALLBUFSIZE
    String sessionURLs = "";		// SMALLBUFSIZE, the session password
    String sessionInfo = "";		// from ping, kept whole
    int sessionHasGame;
    int sessionIsPrivate;
    int sessionScored;
    int sessionClear;
    int sessionFileWritten;
    int sessionKey;
}
