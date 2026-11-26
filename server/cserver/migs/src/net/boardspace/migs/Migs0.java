package net.boardspace.migs;

import java.io.File;
import java.nio.file.Files;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;

/**
 * Core state and logic of the server: users and sessions, bans, the game cache,
 * and output.  This is the first half of migs.c, ported function by function
 * with the same names so the two can be compared side by side.
 * <p>
 * Everything here runs on the single main thread except where noted.  The main
 * thread never does file I/O once the server is running: logs, game cache
 * saves, completed game records and the status page are all written by
 * background threads.
 */
class Migs0
{
    // ---------------------------------------------------------------- constants
    static final int MAXWAITTIME = 30;
    static final int MAXWAITINGTIME = 30;
    static final int SESSIONTIMEOUT = 300;
    static final int MAXPROXYTIME = 60;
    static final int SESSIONCLEARTIMEOUT = 10;
    static final int REGISTERED_USER_TIMEOUT = 600;
    static final int BUFFER_ALLOC_STEP = 1024;
    static final int BUFFER_ALLOC_SLOP = 8;
    static final int MAX_INPUT_BUFFER_SIZE = 1024 * 1024;
    static final int ERR_NOLISTEN = -2;
    static final int MAX_ERRORS_LOGGED = 100;
    static final int SERVERID = 19;
    static final int SERVERID_SSL = 20;		// reported instead of SERVERID when the standard port also accepts TLS
    static final int MAXGAMES = 800;
    static final int GAMEBUFFER_WRITE_POOL_SIZE = 20;
    static final String GAMESUFFIX = "cachedgame";
    static final int UIDSIZE = 64;
    static final int GAMEEXPIRED = 60;
    static final int MAXCONNECTIONS = 30;
    static final int MAXCONNECTIONSPERSESSION = 15;
    static final int MAXCONNECTIONSPERUID = 20;
    static final int MAXSESSIONS = 200;
    static final int LASTREALSESSION = MAXSESSIONS - 1;
    static final int MAXCLIENTS = 500;
    static final int MAXBANNED = 100;
    static final int BANSIZE = 20;
    static final int BANEXPIRED = 60 * 60;
    static final int INFOSIZE = 256;
    // player names (public, real, registered, banned, exempt) are plain java strings,
    // kept as received (still escaped), with no fixed size.  The C server's NAMESIZE and
    // CLIENTPUBLICNAMESIZE fields have no counterpart here.
    static final int FIRSTUSERNUM = 1000;
    static final int SMALLBUFSIZE = 256;
    static final int LARGEBUFFER = 4096;
    /** cap on queued output for one client; the C ring buffer disconnected on overrun */
    static final int MAX_OUTPUT_PENDING = 2 * MAX_INPUT_BUFFER_SIZE;

    static final int LOG_NONE = 0, LOG_ERRORS = 1, LOG_CONNECTIONS = 2, LOG_ALL = 3;

    // string opcodes
    static final String SEND_INTRO = "200 ";
    static final String ECHO_INTRO_SELF = "201 ";
    static final String ECHO_INTRO_OTHERS = "203 ";
    static final String SEND_NAME = "204 ";
    static final String ECHO_NAME = "205 ";
    static final String SEND_SEAT = "206 ";
    static final String ECHO_SEAT = "207 ";
    static final String SEND_GROUP = "210 ";
    static final String ECHO_GROUP_SELF = "211 ";
    static final String ECHO_GROUP_OTHERS = "213 ";
    static final String SEND_CHECK_SCORE = "218 ";
    static final String ECHO_CHECK_SCORE = " 219 ";
    static final String SEND_TAKEOVER = "220 ";
    static final String ECHO_I_QUIT = "221 ";
    static final String ECHO_PLAYER_QUIT = "223 ";
    static final String SEND_MESSAGE_TO = "230 ";
    static final String SEND_PING = "302 ";
    static final String ECHO_PING = "303 ";
    static final String SEND_SUMMARY = "304 ";
    static final String ECHO_SUMMARY = "305 ";
    static final String SEND_LOGMESSAGE = "308 ";
    static final String SEND_ASK_PASSWORD = "310 ";
    static final String ECHO_PASSWORD = "311 ";
    static final String SEND_ALL_GROUP = "312 ";
    static final String SEND_REGISTER_PLAYER = "314 ";
    static final String SEND_LOBBY_INFO = "330 ";
    static final String SEND_ASK_DETAIL = "306 ";
    static final String SEND_ECHO_DETAIL = "307 ";
    static final String SEND_END_DETAIL = "309 ";
    static final String SEND_WRITE_GAME = "316 ";
    static final String SEND_STATE_KEY = "328 ";
    static final String ECHO_STATE_KEY = "329 ";
    static final String QUERY_GAME = "318 ";
    static final String ECHO_QUERY_GAME = "319 ";
    static final String FETCH_GAME_FILTERED = "320 ";
    static final String ECHO_FETCH_GAME_FILTERED = "321 ";
    static final String SAVE_GAME = "322 ";
    static final String ECHO_SAVE_GAME = "323 ";
    static final String REMOVE_GAME = "324 ";
    static final String ECHO_REMOVE_GAME = "325 ";
    static final String SEND_LOGSHORTNOTE = "326 ";
    static final String SEND_RESERVE_ROOM = "332 ";
    static final String ECHO_RESERVE_ROOM = "333 ";
    static final String SEND_SET_STATE = "334 ";
    static final String ECHO_SET_STATE = "335 ";
    static final String APPEND_GAME = "336 ";
    static final String ECHO_APPEND_GAME = "337 ";
    static final String SEND_MULTIPLE = "338 ";
    static final String FETCH_ACTIVE_GAME_FILTERED = "340 ";
    static final String ECHO_ACTIVE_GAME_FILTERED = "341 ";
    static final String LOCK_GAME = "342 ";
    static final String FETCH_GAME = "344 ";
    static final String ECHO_FETCH_GAME = "345 ";
    static final String FETCH_ACTIVE_GAME = "346 ";
    static final String ECHO_ACTIVE_GAME = "347 ";
    static final String ECHO_FAILED = "999 ";
    static final String KEYWORD_SPECTATE = "spectate";
    static final String KEYWORD_PLAYING = "playing";

    // ---------------------------------------------------------------- logs
    final LogStream mainLog = new LogStream("main");
    final LogStream chatLog = new LogStream("chat");
    final LogStream securityLog = new LogStream("security");
    volatile boolean logFilesEnabled = false;
    volatile long maxLogfileSize = 1024 * 1024;
    volatile boolean loggerThreadRunning = false;
    volatile boolean killThreads = false;
    int logging = 0;

    // ---------------------------------------------------------------- config
    int strict_login = 1;
    int require_rng = 0;
    int require_seq = 0;
    int strict_score = 1;
    int portNum;
    int maxSessions = 0;
    int maxClients = 0;
    int maxConnections = MAXCONNECTIONS;
    int maxConnectionsPerSession = MAXCONNECTIONSPERSESSION;
    int maxConnectionsPerUID = MAXCONNECTIONSPERUID;
    String statusfile = null;
    String playerInfoUrl = "";
    int ngamedirs = 0, ngametypes = 0, nroomtypes = 0;
    String[] gamedirs = new String[0];
    String[] gametypes = new String[0];
    String[] roomtypes = new String[0];
    int crash_test = 0;
    int listenBacklog = 128;		// listen() backlog for both ports, config "listen-backlog"
    String supervisor = "";
    int lobby_timeout, client_timeout;
    int websocketPortNum = 0;
    boolean websocketSslOnly = false;
    boolean stdPortTls = false;				// the standard port also accepts TLS (the key and cert are configured)
    boolean sslRequired = false;			// config "sslrequired": the standard port refuses plain connections
    SslProvider sslProvider = null;
    String host_name = "";
    int server_ip = 0;
    int alt_server_ip = 0x7f000001;
    int security_key = 0;
    int IP1, IP2, IP3, IP4;

    boolean isClosed = false;
    String Exempt_User = "";

    // ---------------------------------------------------------------- state
    final User[] Users = new User[MAXCLIENTS];
    final Session[] Sessions = new Session[MAXSESSIONS];
    final Session[] sessionStateHash = new Session[MAXSESSIONS * 2];
    final Session Idle_Users = new Session();
    final Session ProxySession = new Session();
    final Session WaitingSession = new Session();
    User DeadUsers = null;

    RegisteredUser freeRegisteredUsers = null;
    RegisteredUser activeRegisteredUsers = null;
    final GameBuffer[] GameHash = new GameBuffer[MAXGAMES * 2];
    GameBuffer all_gamebuffers = null;
    int n_gamebuffers = 0;
    final BannedUser[] bannedUsers = new BannedUser[MAXBANNED];
    int bannedIndex = 0, totalBanned = 0, totalAttempts = 0;
    BannedUser banned_user = null;

    long start_time, start_idle_time, bktime, idle_time;
    int sockets_open = 0, maxsockets = 0, isshutdown = 0;
    int client_errors = 0, client_messages = 0, unusual_events = 0, robot_irregular = 0;
    final AtomicInteger save_failed = new AtomicInteger();
    int games_completed = 0, clients_connected = 0, maxlobby = 0, games_launched = 0;
    int games_cached = 0, player_games_relaunched = 0, robot_games_relaunched = 0;
    int transactions = 0, readBreak = 0, writeBreak = 0;
    int blocked_clients = 0, unblocked_clients = 0, closed_blocked_clients = 0;
    long totalwrite = 0, totalread = 0;
    int checksum_errors = 0;
    int GameUIDs = 1;
    boolean configChange = false;
    final Random rand = new Random();

    // ---------------------------------------------------------------- save game state
    String gameCacheDir = "";
    int gameCacheRate = 0;
    int n_dirty_gamebuffers = 0, max_dirty_gamebuffers = 0;
    final AtomicInteger n_dirty_writes = new AtomicInteger();
    final AtomicInteger n_dirty_problems = new AtomicInteger();
    GameBuffer first_dirty_gamebuffer = null, last_dirty_gamebuffer = null;

    /** a snapshot of a game, handed to the save thread (the C write pool entry) */
    static final class GameCopy
    {
        String idString;
        long startTime;
        boolean preserved;
        byte[] game;
    }

    /** the C write pool: a ring of GAMEBUFFER_WRITE_POOL_SIZE slots holds at most size-1 */
    final ArrayBlockingQueue<GameCopy> writePool = new ArrayBlockingQueue<>(GAMEBUFFER_WRITE_POOL_SIZE - 1);

    /** background writer for completed game records and the status page */
    final ExecutorService fileWriter = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "file-writer");
        t.setDaemon(true);
        return t;
    });

    // ---------------------------------------------------------------- history
    static final int HISTORYSIZE = 500;
    static final int HISTORYCHARS = 120;
    final String[] History = new String[HISTORYSIZE];
    int history_index = 0;

    // =====================================================================
    // time
    // =====================================================================

    /**
     * nanoTime value at system boot, so uptime() counts seconds since boot like the
     * C server's sysinfo() uptime and its once-a-second housekeeping ticks on the same
     * boundaries.  /proc/uptime is read once, at class load; if it isn't available the
     * count starts from a large arbitrary value instead.
     */
    private static final long BOOT_NANOS = bootNanos();

    private static long bootNanos()
    {
        long now = System.nanoTime();
        try
        {
            String s = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("/proc/uptime")),
                java.nio.charset.StandardCharsets.US_ASCII).trim().split("\\s+")[0];
            return now - (long) (Double.parseDouble(s) * 1e9);
        }
        catch (IOException | RuntimeException e)
        {
            return now - 1_000_000L * 1_000_000_000L;
        }
    }

    /** seconds since boot, monotonic (the C used uptime, which can't go backwards) */
    static long uptime()
    {
        return (System.nanoTime() - BOOT_NANOS) / 1_000_000_000L;
    }

    /** days since 1970 (the C DaysSinceForever) */
    static long daysSinceForever()
    {
        return (System.currentTimeMillis() / 1000) / (60 * 60 * 24);
    }

    private static final String[] MONTHS = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    /** "Mon dd hh:mm:ss" in GMT, as the C asctime based timestamp */
    static String timestamp()
    {
        ZonedDateTime t = ZonedDateTime.now(ZoneOffset.UTC);
        return String.format("%s%3d %02d:%02d:%02d", MONTHS[t.getMonthValue() - 1], t.getDayOfMonth(),
            t.getHour(), t.getMinute(), t.getSecond());
    }

    // =====================================================================
    // logging
    // =====================================================================

    /** C logEntry: format and buffer.  Never blocks. */
    void logEntry(LogStream b, String fmt, Object... args)
    {
        b.add(C.lsprintf(0, fmt, args));
        if (!loggerThreadRunning) { b.flush(maxLogfileSize, logFilesEnabled); }
    }

    void flushAllLogs()
    {
        mainLog.flush(maxLogfileSize, logFilesEnabled);
        chatLog.flush(maxLogfileSize, logFilesEnabled);
        securityLog.flush(maxLogfileSize, logFilesEnabled);
    }

    /** fatal error: log, dump history, exit */
    void error(String inStr, int err)
    {
        logEntry(mainLog, "[%s] %s %d.\n", timestamp(), inStr, err);
        DumpHistory();
        ExitWith(1);
    }

    void error2(String inStr, String inStr2)
    {
        logEntry(mainLog, "[%s] %s %s.\n", timestamp(), inStr == null ? "" : inStr, inStr2 == null ? "" : inStr2);
        if (logging >= LOG_ERRORS) { DumpHistory(); }
        ExitWith(1);
    }

    void error1(String inStr) { error2(inStr, null); }

    void error3(String inStr, String inStr2, int l)
    {
        logEntry(mainLog, "[%s] %s %s %d.\n", timestamp(), inStr == null ? "" : inStr, inStr2 == null ? "" : inStr2, l);
        if (logging >= LOG_ERRORS) { DumpHistory(); }
        ExitWith(1);
    }

    void ExitWith(int v)
    {
        killThreads = true;
        loggerThreadRunning = false;
        flushAllLogs();
        mainLog.close();
        chatLog.close();
        securityLog.close();
        System.out.flush();
        System.err.flush();
        Runtime.getRuntime().halt(v);
    }

    /** internal consistency check (the C asserts).  Logs and throws; the main loop recovers. */
    void check(boolean cond, String what)
    {
        if (!cond)
        {
            String msg = "assertion failed: " + what;
            logEntry(mainLog, "[%s] %s\n", timestamp(), msg);
            throw new IllegalStateException(msg);
        }
    }

    // =====================================================================
    // history
    // =====================================================================

    void recordInHistory(String cxt, int socket, String str, int len)
    {
        String head = C.lsprintf(HISTORYCHARS, "%3d %s: ", socket, cxt);
        int nrem = HISTORYCHARS - head.length() - 4;
        int alen = Math.min(len, nrem);
        String body = C.cstr(str);
        if (alen > 0)
        {
            // MyStrncpy(&buf[idx], str, alen) keeps alen-1 chars
            body = body.length() > alen - 1 ? body.substring(0, alen - 1) : body;
        }
        else
        {
            body = "";
        }
        String buf = head + body;
        int last = buf.length();
        if (last >= 2 && buf.charAt(last - 2) == '\r') { buf = buf.substring(0, last - 2) + buf.charAt(last - 1); }
        if (!buf.endsWith("\n")) { buf = buf + "\n"; }
        History[history_index] = buf;
        history_index++;
        if (history_index >= HISTORYSIZE) { history_index = 0; }
    }

    void DumpHistory()
    {
        int idx = history_index;
        int i = 0;
        logEntry(mainLog, "\nRecent messages:\n");
        do
        {
            if (History[idx] != null && !History[idx].isEmpty())
            {
                i++;
                logEntry(mainLog, "%3d %s", i, History[idx]);
                History[idx] = null;
            }
            idx++;
            if (idx == HISTORYSIZE) { idx = 0; }
        } while (idx != history_index);
    }

    // =====================================================================
    // scanning
    // =====================================================================

    static final class Tok
    {
        final int n;		// chars consumed, 0 if empty input, -1 on failure
        final String s;
        Tok(int n, String s) { this.n = n; this.s = s; }
    }

    /** safe_scanint: returns {chars consumed or -1, value} */
    static int[] safe_scanint(String cmd)
    {
        Object[] out = new Object[2];
        int nints = C.sscanf(cmd, "%d%n", out);
        if (nints == 1) { return new int[] { (Integer) out[1], (Integer) out[0] }; }
        return new int[] { -1, out[0] == null ? 0 : (Integer) out[0] };
    }

    /** safe_scanstr: scan one token into a buffer of destsize */
    Tok safe_scanstr(String cmd, int destsize, int line)
    {
        Object[] out = new Object[2];
        int nstrings = C.sscanf(cmd, "%*s%n", out);
        if (nstrings == -1) { return new Tok(0, ""); }
        int nchars = out[0] == null ? 0 : (Integer) out[0];
        if (nstrings == 0 && nchars >= 0 && (nchars + 1) < destsize)
        {
            Object[] o2 = new Object[1];
            C.sscanf(cmd, "%s", o2);
            return new Tok(nchars, (String) o2[0]);
        }
        String dest = C.trunc(cmd, destsize);
        if (logging >= LOG_NONE)
        {
            logEntry(mainLog, "[%s] from line %d buffer too small for string \"%s\", buffer %d needs %d\n",
                timestamp(), line, dest, destsize, nchars);
        }
        return new Tok(-1, dest);
    }

    Tok scanstr(String cmd, int destsize) { return safe_scanstr(cmd, destsize, 0); }

    /** a scanstr target that is "unlimited" (the C tempBufPtr) */
    Tok scanstr(String cmd) { return safe_scanstr(cmd, Integer.MAX_VALUE, 0); }

    // =====================================================================
    // bans
    // =====================================================================

    static final String[] banreason = {
        "ok", "un banned", "banned for matching ip", "banned for matching identity",
        "banned by name", "supervisor override", "" };
    static final int bc_none = 0, bc_unban = 1, bc_same_ip = 2, bc_same_id = 3, bc_same_name = 4, bc_super = 5, bc_blank = 6;

    String banInfoFor(BannedUser B, int code)
    {
        int ip = B.banned_ip;
        int b4 = (ip >> 0) & 0xff;
        int b3 = (ip >> 8) & 0xff;
        int b2 = (ip >> 16) & 0xff;
        int b1 = (ip >>> 24) & 0xff;
        return C.lsprintf(256, "%s (ref #%d: %s %c ip: %d.%d.%d.%d  cookie: %s uid: %d )",
            banreason[code], B.eventid, B.userName, B.serverBancode, b1, b2, b3, b4, B.cookie, B.userUid);
    }

    String banInfo(int code)
    {
        if (banned_user == null) { return banreason[code]; }
        return banInfoFor(banned_user, code);
    }

    void describeBans(User SU)
    {
        long now = uptime();
        int limit = totalBanned >= MAXBANNED ? MAXBANNED : totalBanned;
        int shown = 0;
        for (int i = 0; i < limit; i++)
        {
            BannedUser bu = bannedUsers[i];
            if ((now - bu.startTime) < BANEXPIRED)
            {
                sendSingle(C.lsprintf(SMALLBUFSIZE, ECHO_GROUP_OTHERS + "%d schat %s", SU.userNUM, banInfoFor(bu, bc_blank)), SU);
                shown++;
            }
        }
        if (shown == 0)
        {
            sendSingle(C.lsprintf(SMALLBUFSIZE, ECHO_GROUP_OTHERS + "%d schat No active bans", SU.userNUM), SU);
        }
    }

    BannedUser unBanEvent(int id)
    {
        int limit = totalBanned >= MAXBANNED ? MAXBANNED : totalBanned;
        for (int i = 0; i < limit; i++)
        {
            BannedUser bu = bannedUsers[i];
            if (bu.eventid == id)
            {
                logEntry(securityLog, "[%s] supervisor unban %s\n", timestamp(), banInfoFor(bu, bc_blank));
                bu.banned_ip = 0;
                bu.userUid = 0;
                bu.cookie = "";
                bu.userName = "";
                bu.startTime = 0;
                return bu;
            }
        }
        return null;
    }

    /** check / ban / unban (C isBanned).  serverBancode is a char, 0 for none */
    int isBanned(String username, int uid, char serverBancode, String cookie, int ip)
    {
        long now = uptime();
        int why = bc_none;
        int limit = totalBanned >= MAXBANNED ? MAXBANNED : totalBanned;
        boolean newban = (serverBancode == 'Z') || (serverBancode == 'Y') || (serverBancode == 'y');
        boolean superv = (serverBancode == 'S') || (serverBancode == 's');
        boolean unban = superv || (serverBancode == 'U') || (serverBancode == 'u');

        banned_user = null;

        for (int i = 0; i < limit; i++)
        {
            boolean unban_now = false;
            boolean newban_now = false;
            BannedUser bu = bannedUsers[i];
            if ((now - bu.startTime) < BANEXPIRED)
            {
                boolean samecookie = !cookie.isEmpty() && bu.cookie.equals(cookie)
                    && !cookie.equals("-1") && !cookie.equals("0");
                boolean sameip = (ip != 0) && (ip == bu.banned_ip);
                boolean samename = !username.isEmpty() && username.equals(bu.userName);
                boolean sameuid = (uid != 0) && (uid == bu.userUid);
                if (samename) { newban_now = true; why = bc_same_name; banned_user = bu; }
                else if (sameuid) { newban_now = true; why = bc_same_id; banned_user = bu; }
                else if (samecookie) { why = bc_same_id; banned_user = bu; newban_now = true; }
                else if (sameip) { why = bc_same_ip; banned_user = bu; newban_now = true; }

                if ((newban || unban) && (samename || sameuid || samecookie || sameip))
                {
                    why = superv ? bc_super : unban ? bc_unban : bc_same_name;
                    newban_now = newban;
                    unban_now = unban;
                    banned_user = bu;
                }
                if (unban_now)
                {
                    logEntry(securityLog, "[%s] unban %s\n", timestamp(), banInfo(why));
                    bu.banned_ip = 0;
                    bu.userUid = 0;
                    bu.cookie = "";
                    bu.userName = "";
                    bu.startTime = 0;
                    return why;
                }
                if (newban_now)
                {
                    bu.attempts++;
                    bu.banned_ip = ip;
                    bu.startTime = now;
                    bu.cookie = C.trunc(cookie, BANSIZE);
                    bu.userUid = 0;
                    bu.userName = "";
                    if (!"guest".equals(username))
                    {
                        bu.userUid = uid;
                        bu.userName = username;
                    }
                    logEntry(securityLog, "[%s] renew ban %s\n", timestamp(), banInfo(why));
                    totalAttempts++;
                    return why;
                }
            }
        }

        if (superv) { return bc_super; }
        else if (newban)
        {
            BannedUser B = bannedUsers[bannedIndex];
            B.userName = username;
            B.cookie = C.trunc(cookie, BANSIZE);
            B.banned_ip = ip;
            B.serverBancode = serverBancode;
            B.startTime = now;
            B.attempts = 0;
            B.userUid = uid;
            B.eventid = bannedIndex;
            banned_user = B;
            bannedIndex++;
            totalBanned++;
            if (bannedIndex >= MAXBANNED) { bannedIndex = 0; }
            logEntry(securityLog, "[%s] new banned user %s uid %d identity (%s) %s\n", timestamp(),
                username, uid, cookie, banInfo(bc_same_name));
            logEntry(mainLog, "[%s] new banned user %s uid %d identity (%s) %s\n", timestamp(),
                username, uid, cookie, banInfo(bc_same_name));
            return bc_same_name;
        }
        return bc_none;
    }

    void banUser(User U)
    {
        U.expectEof = true;
        isBanned(U.clientRealName, U.clientUid, 'Z', U.cookie, 0);
        logEntry(mainLog, "[%s] unusual banned user %d cookie %s\n", timestamp(), U.clientUid, U.cookie);
    }

    void banIP(int ip)
    {
        int b4 = (ip >> 0) & 0xff;
        int b3 = (ip >> 8) & 0xff;
        int b2 = (ip >> 16) & 0xff;
        int b1 = (ip >>> 24) & 0xff;
        if (ip == 0x7f000001)
        {
            logEntry(mainLog, "[%s] unusual tried to self ban IP %d.%d.%d.%d\n", timestamp(), b1, b2, b3, b4);
        }
        else
        {
            isBanned("", 0, 'Z', "", ip);
            logEntry(mainLog, "[%s] unusual banned IP %d.%d.%d.%d\n", timestamp(), b1, b2, b3, b4);
        }
    }

    void banUserByIP(User U)
    {
        U.expectEof = true;
        banIP(U.ip);
    }

    void unBanUser(User U)
    {
        isBanned(U.clientRealName, U.clientUid, 'U', U.cookie, U.ip);
    }

    static int roundUp(int val, int r)
    {
        int sz = ((val + r) / r) * r;
        return sz >= 0 ? sz : 1;
    }

    // =====================================================================
    // users
    // =====================================================================

    void ClearUser(User u)
    {
        u.socket = -1;
        u.conn = null;
        u.ip = 0;
        u.badReadCount = u.badWriteCount = u.goodReadCount = u.goodWriteCount = 0;
        u.errorsLogged = 0;
        u.serverKey = 0;
        u.keyIndex = null;
        u.totalread = u.totalwrite = 0;
        u.nread = u.nwrite = 0;
        u.blocked = false;
        u.supervisor = false;
        u.isARobot = false;
        u.checksums = false;
        u.sequencenumbers = false;
        u.wasZapped = false;
        u.reasonClosed = "";
        u.pingStats = "";
        u.isAPlayer = false;
        u.gagged = false;
        u.expectEof = false;
        u.inputClosed = false;
        u.outputClosed = false;
        u.requestingLock = false;
        u.websocket = false;
        u.oopsCount = 0;
        u.injectedOutput = 0;
        u.rogueOutput = 0;
        u.m_z_in = u.m_w_in = 0;
        u.use_rng_in = false;
        u.m_z_out = u.m_w_out = 0;
        u.use_rng_out = false;
        u.unexpectedCount = 0;
        u.clientPublicName = "";
        u.clientRealName = "";
        u.lobbyInfo = "";
        u.clientUid = 0;
        u.clientSeat = -1;
        u.clientOrder = -1;
        u.clientRev = -1;
        u.clientTime = 0;
        u.clearBuffers();
        u.cookie = "";
        // sequence numbers start fresh for every connection.  The C server left
        // them stale unless obfuscation was on (init_rng_in reset them).
        u.rng_in_seq = 1;
        u.rng_in_seq_errors = 0;
        u.rng_out_seq = 1;
    }

    void CopyUser(User from, User to)
    {
        if (to == from) { return; }
        to.socket = from.socket;
        to.conn = from.conn;
        to.badReadCount = from.badReadCount;
        to.badWriteCount = from.badWriteCount;
        to.goodReadCount = from.goodReadCount;
        to.goodWriteCount = from.goodWriteCount;
        to.ip = from.ip;
        to.serverKey = from.serverKey;
        to.keyIndex = from.keyIndex;
        to.totalread = from.totalread;
        to.nread = from.nread;
        to.nwrite = from.nwrite;
        to.totalwrite = from.totalwrite;
        to.blocked = from.blocked;
        to.supervisor = from.supervisor;
        to.isARobot = from.isARobot;
        to.wasZapped = from.wasZapped;
        to.nextDead = from.nextDead;
        to.isAPlayer = from.isAPlayer;
        to.expectEof = false;
        to.inputClosed = false;
        to.outputClosed = false;

        to.outbuf = from.outbuf;
        to.outTake = from.outTake;
        to.outPut = from.outPut;
        to.inbuf = from.inbuf;
        to.inbufCount = from.inbufCount;
        from.inbuf = new byte[0];
        from.inbufCount = 0;
        from.outbuf = new byte[0];
        from.outTake = from.outPut = 0;

        to.oopsCount = from.oopsCount;
        to.requestingLock = from.requestingLock;
        to.websocket = from.websocket;
        from.websocket = false;
        from.requestingLock = false;
        from.oopsCount = 0;
        to.unexpectedCount = from.unexpectedCount;
        from.unexpectedCount = 0;

        to.use_rng_in = from.use_rng_in;
        from.use_rng_in = false;
        to.m_z_in = from.m_z_in;
        to.m_w_in = from.m_w_in;
        to.rng_in_seq = from.rng_in_seq;
        to.rng_in_seq_errors = from.rng_in_seq_errors;
        from.m_z_in = from.m_w_in = from.rng_in_seq = from.rng_in_seq_errors = 0;

        to.use_rng_out = from.use_rng_out;
        from.use_rng_out = false;
        to.m_z_out = from.m_z_out;
        to.m_w_out = from.m_w_out;
        to.rng_out_seq = from.rng_out_seq;
        from.m_z_out = from.m_w_out = from.rng_out_seq = 0;

        to.checksums = from.checksums;
        to.sequencenumbers = from.sequencenumbers;
        to.clientPublicName = from.clientPublicName;
        // not a bug: real name and public name start out the same (see the C comment)
        to.clientRealName = from.clientPublicName;
        to.lobbyInfo = from.lobbyInfo;
        to.clientUid = from.clientUid;
        to.clientSeat = from.clientSeat;
        to.clientOrder = from.clientOrder;
        to.clientRev = from.clientRev;
        to.clientTime = from.clientTime;
        to.errorsLogged = to.errorsLogged / 2;
        from.conn = null;		// the connection now belongs to "to"
        ClearUser(from);
        if (to.conn != null && to.conn.key != null) { to.conn.key.attach(to); }
    }

    static boolean isRealSocket(int s) { return s != -1; }

    void putUserInSession(User u, Session s)
    {
        check(u.session == null, "putUserInSession: user already in a session");
        u.session = s;
        u.next_in_session = s.first_user;
        s.first_user = u;
        s.population++;
    }

    void removeUserFromSession(User u)
    {
        Session s = u.session;
        check(s != null, "removeUserFromSession: no session");
        if (u == s.first_user)
        {
            s.first_user = u.next_in_session;
        }
        else
        {
            User fu = s.first_user;
            while (fu != null && fu.next_in_session != u) { fu = fu.next_in_session; }
            check(fu != null, "removeUserFromSession: user not in its session");
            fu.next_in_session = u.next_in_session;
        }
        u.session = null;
        if (s.locker == u) { s.locker = null; }
        u.next_in_session = null;
        s.population--;
    }

    Session LOBBYSESSION() { return Sessions[0]; }

    boolean sessionIsEmpty(Session sess)
    {
        int nfound = 0;
        int pop = sess.population;
        User u = sess.first_user;
        while (u != null)
        {
            check(u.session == sess, "sessionIsEmpty: user in wrong session");
            if (isRealSocket(u.socket)) { return false; }
            u = u.next_in_session;
            nfound++;
            check(nfound <= pop, "sessionIsEmpty: population");
        }
        check(nfound == pop, "sessionIsEmpty: population count");
        return true;
    }

    // =====================================================================
    // closing
    // =====================================================================

    /** small integer ids for sockets, reused lowest first like unix fds */
    private final java.util.BitSet socketIds = new java.util.BitSet();

    int allocSocketId()
    {
        int id = socketIds.nextClearBit(5);
        socketIds.set(id);
        return id;
    }

    void simpleCloseSocket(User u)
    {
        int sock = u.socket;
        if (isRealSocket(sock))
        {
            if (u.conn != null) { u.conn.close(); }
            socketIds.clear(sock);
            sockets_open--;
            if (sockets_open == 0) { start_idle_time = uptime(); }
        }
    }

    void simpleCloseClientOnly(User u, String cxt)
    {
        if (isRealSocket(u.socket))
        {
            boolean blocked = u.blocked;
            u.deadFromSession = u.session;
            if (blocked) { u.blocked = false; closed_blocked_clients++; }
            if (logging >= LOG_CONNECTIONS)
            {
                String bmsg = blocked ? "blocked " : "";
                logEntry(mainLog, "[%s] %s %s Closing C%d (%s#%d) S%d Session %d, ping %s %d in %d out, %d sockets still in use\n",
                    timestamp(), cxt, bmsg, u.userNUM, u.clientRealName, u.clientUid,
                    u.socket, u.session.sessionNUM, u.pingStats, u.nread, u.nwrite, sockets_open);
            }
            u.expectEof = true;
            simpleCloseSocket(u);
            u.socket = -1;
            u.conn = null;
        }
    }

    void simpleCloseClient(User u, String cxt)
    {
        boolean isSoc = isRealSocket(u.socket);
        simpleCloseClientOnly(u, cxt);
        update_user_timestamps();
        if (!u.isAPlayer)
        {
            if (u.session != null)
            {
                removeUserFromSession(u);
                putUserInSession(u, Idle_Users);
            }
            ClearUser(u);
        }
        else if (isSoc)
        {
            Session s = u.session;
            if (s != null) { s.sessionURLs = ""; }
            if ("guest".equalsIgnoreCase(u.clientRealName))
            {
                u.clientPublicName = "(" + u.clientPublicName + "#" + u.clientUid + ")";
            }
            else
            {
                u.clientPublicName = "(" + u.clientRealName + ")";
            }
            u.checksums = false;
            u.sequencenumbers = false;
            u.use_rng_in = false;
            u.use_rng_out = false;
        }
    }

    boolean logErrorFor(User u)
    {
        if (u.errorsLogged++ >= MAX_ERRORS_LOGGED)
        {
            simpleCloseClient(u, "too many errors");
            return false;
        }
        return true;
    }

    void clearSessionGame(Session s)
    {
        GameBuffer g = s.sessionGame;
        s.locker = null;
        if (g != null)
        {
            s.sessionGame = null;
            g.ownerSession = null;
            s.locker = null;
            if (logging >= LOG_CONNECTIONS)
            {
                logEntry(mainLog, "[%s] clearsession refcount %d down %s#%d\n", timestamp(), g.refCount, g.idString, g.uid);
            }
            if (g.refCount-- == 1) { freeGameBuffer(g); }
        }
    }

    void clearEmptySession(Session s)
    {
        if (s != Idle_Users)
        {
            int nfound = 0;
            int pop = s.population;
            while (s.first_user != null)
            {
                User u = s.first_user;
                removeUserFromSession(u);
                putUserInSession(u, Idle_Users);
                nfound++;
                check(nfound <= pop, "clearEmptySession: population");
            }
            check(nfound == pop, "clearEmptySession: population count");
            s.sessionStateKey = "";
            if (s.sessionStateKeyIndex >= 0) { sessionStateHash[s.sessionStateKeyIndex] = null; }
            s.sessionStateKeyIndex = -1;
            s.sessionURLs = "";
            s.sessionInfo = "";
            s.sessionHasGame = 0;
            s.sessionKey = 0;
            s.sessionIsPrivate = 0;
            s.poisoned = false;
            s.sessionScored = 0;
            s.sessionFileWritten = 0;
            s.sessionClear = 0;
            s.population = 0;
            s.first_user = null;
            s.sessionDescribe = true;
            clearSessionGame(s);
        }
    }

    void clearSessionPlayers(Session S)
    {
        User u = S.first_user;
        int pop = S.population;
        int nfound = 0;
        S.sessionIsPrivate = 0;
        S.sessionURLs = "";
        while (u != null)
        {
            User next = u.next_in_session;
            check(u.session == S, "clearSessionPlayers: user in wrong session");
            if (u.isAPlayer || u.isARobot)
            {
                u.clientSeat = -1;
                u.clientOrder = -1;
                u.clientRev = -1;
                u.isAPlayer = false;
                u.isARobot = false;
                u.expectEof = true;
                if (isRealSocket(u.socket))
                {
                    sendSingle(C.lsprintf(SMALLBUFSIZE, ECHO_PLAYER_QUIT + "%d %s", u.userNUM, "server"), u);
                    u.inputClosed = true;
                }
                else
                {
                    simpleCloseClient(u, "server");
                }
            }
            u = next;
            nfound++;
            check(nfound <= pop, "clearSessionPlayers: population");
        }
        check(nfound == pop, "clearSessionPlayers: population count");
        S.poisoned = true;
        clearSessionGame(S);
        if (sessionIsEmpty(S)) { clearEmptySession(S); }
    }

    void clearSession(Session S)
    {
        if (S != Idle_Users)
        {
            User u = S.first_user;
            int pop = S.population;
            int nfound = 0;
            S.locker = null;
            while (u != null)
            {
                User next = u.next_in_session;
                check(u.session == S, "clearSession: user in wrong session");
                u.clientSeat = -1;
                u.clientOrder = -1;
                u.clientRev = -1;
                u.isAPlayer = false;
                u.isARobot = false;
                simpleCloseClient(u, "clearsession");
                u = next;
                nfound++;
                check(nfound <= pop, "clearSession: population");
            }
            check(nfound == pop, "clearSession: population count");
            clearEmptySession(S);
        }
    }

    static final int grace_optional = 0, grace_mandatory = 1, grace_forbidden = 2;

    void closeClient(User U, String cxt, int grace)
    {
        if (isRealSocket(U.socket))
        {
            Session S = U.session;
            simpleCloseClient(U, cxt);
            if (S != null && S != WaitingSession && S != ProxySession)
            {
                S.sessionDescribe = true;
                S.sessionIsPrivate = 0;
                S.sessionURLs = "";
                if (S != LOBBYSESSION() && sessionIsEmpty(S))
                {
                    if (grace == grace_mandatory
                        || (grace != grace_forbidden
                            && (S.sessionHasGame != 0 || S.sessionFileWritten != 0)
                            && strict_score != 2
                            && S.sessionScored == 0))
                    {
                        if (S.sessionClear == 0)
                        {
                            S.sessionClear = 1;
                            S.sessionTimes = uptime();
                            if (logging >= LOG_CONNECTIONS)
                            {
                                logEntry(mainLog, "[%s] granting grace time to session %d\n", timestamp(), S.sessionNUM);
                            }
                        }
                    }
                    else
                    {
                        clearSession(S);
                    }
                }
            }
            saveConfig();
        }
    }

    void putInDeadPlayers(Session S, User U)
    {
        if (S != WaitingSession)
        {
            User du = DeadUsers;
            while (du != null)
            {
                if (U == du) { return; }
                du = du.nextDead;
            }
            U.nextDead = DeadUsers;
            U.deadFromSession = S;
            DeadUsers = U;
        }
    }

    void HandleWriteError(User U, String context)
    {
        Session S = U.session;
        client_errors++;
        if (S == WaitingSession)
        {
            simpleCloseClientOnly(U, context);
            U.deadFromSession = null;
            removeUserFromSession(U);
            putUserInSession(U, Idle_Users);
            return;
        }
        U.reasonClosed = C.lsprintf(32, context);
        U.expectEof = true;
        closeClient(U, context, grace_optional);
        putInDeadPlayers(S, U);
    }

    void HandleReadError(User U, int err)
    {
        Session S = U.session;
        U.reasonClosed = C.lsprintf(32, "readerr %d", err);
        if (S == WaitingSession)
        {
            U.deadFromSession = null;
            simpleCloseClientOnly(U, U.reasonClosed);
            removeUserFromSession(U);
            putUserInSession(U, Idle_Users);
            return;
        }
        U.expectEof = true;
        closeClient(U, U.reasonClosed, grace_optional);
        putInDeadPlayers(S, U);
    }

    void HandleDeadUsers()
    {
        while (DeadUsers != null)
        {
            User U = DeadUsers;
            Session DS = U.deadFromSession;
            DeadUsers = U.nextDead;
            U.nextDead = null;
            U.deadFromSession = null;
            String dB = C.lsprintf(SMALLBUFSIZE, ECHO_PLAYER_QUIT + "%d %s", U.userNUM, U.reasonClosed);
            if (DS != null) { sendMulti(dB, DS); }
        }
    }

    void HandleKill(User U)
    {
        doEchoAll(U, C.lsprintf(0, ECHO_PLAYER_QUIT + "%d server", U.userNUM), true);
    }

    // =====================================================================
    // encryption and checksums
    // =====================================================================

    static int get_random_in(User u)
    {
        u.m_z_in = 36969 * (u.m_z_in & 65535) + (u.m_z_in >> 16);
        u.m_w_in = 18000 * (u.m_w_in & 65535) + (u.m_w_in >> 16);
        return u.m_w_in & 0xffff;
    }

    static int get_random_out(User u)
    {
        u.m_z_out = 36969 * (u.m_z_out & 65535) + (u.m_z_out >> 16);
        u.m_w_out = 18000 * (u.m_w_out & 65535) + (u.m_w_out >> 16);
        return u.m_w_out & 0xffff;
    }

    static void init_rng_in(User u, String instr)
    {
        u.rng_in_chars = 0;
        u.rng_in_seq = 1;
        u.rng_in_seq_errors = 0;
        if (instr == null || instr.isEmpty())
        {
            u.m_w_in = u.m_z_in = 0;
            u.use_rng_in = false;
        }
        else
        {
            for (int i = 0; i < instr.length(); i++) { u.m_w_in = u.m_w_in * 13 + C.sc(instr.charAt(i)); }
            for (int i = 0; i < instr.length(); i++) { u.m_z_in = u.m_z_in * 31 + C.sc(instr.charAt(i)); }
            u.use_rng_in = true;
        }
    }

    static void init_rng_out(User u, String outstr)
    {
        u.rng_out_chars = 0;
        // rng_out_seq is not reset: it was set to 1 when the connection started,
        // and the 201 that precedes this has already used it
        if (outstr == null || outstr.isEmpty())
        {
            u.m_w_out = u.m_z_out = 0;
            u.use_rng_out = false;
        }
        else
        {
            for (int i = 0; i < outstr.length(); i++) { u.m_w_out = u.m_w_out * 17 + C.sc(outstr.charAt(i)); }
            for (int i = 0; i < outstr.length(); i++) { u.m_z_out = u.m_z_out * 23 + C.sc(outstr.charAt(i)); }
            u.use_rng_out = true;
        }
    }

    /**
     * checksum a string from the client, decrypting it in place if needed.
     * Returns {checksum} and stores the decrypted text in out[0].
     */
    static int checksumInputString(User u, StringBuilder str, int start)
    {
        int sum = 0;
        int i = 0;
        for (int p = start; p < str.length(); p++)
        {
            int ch = C.sc(str.charAt(p));
            if (ch == 0 || ch == '\r' || ch == '\n') { break; }
            sum += (ch & 0xff) ^ i;		// unsigned, as the client computes it
            if (u.use_rng_in && ch > ' ')
            {
                int rnd = get_random_in(u) & 0x3f;
                int och = ((ch - ' ' - 1) + (127 - ' ') - rnd) % (127 - ' ') + ' ' + 1;
                u.rng_in_chars++;
                str.setCharAt(p, (char) (och & 0xff));
            }
            i++;
        }
        return sum & 0xffff;
    }

    /** localStringCopy: encrypt (if needed) seq and str, append \n, compute the checksum */
    static String localStringCopy(User u, String seq, String str, int[] checksum)
    {
        StringBuilder out = new StringBuilder(seq.length() + str.length() + 2);
        int sum = 0;
        int i = 0;
        for (String part : new String[] { seq, str })
        {
            for (int k = 0; k < part.length(); k++)
            {
                int ch = C.sc(part.charAt(k));
                if (ch == 0) { break; }
                if (u.use_rng_out && ch > ' ')
                {
                    int rnd = get_random_out(u) & 0x3f;
                    ch = ((ch - ' ' - 1) + rnd) % (127 - ' ') + ' ' + 1;
                    u.rng_out_chars++;
                }
                sum += (ch & 0xff) ^ i;		// unsigned, as the client computes it
                out.append((char) (ch & 0xff));
                i++;
            }
        }
        out.append('\n');
        checksum[0] = sum & 0xffff;
        return out.toString();
    }

    /** addChecksum: sequence number, encryption and checksum for one outgoing line */
    String addChecksum(User u, String inStr)
    {
        String seq = "";
        if (require_seq != 0 || u.sequencenumbers)
        {
            boolean isScoreCheck = inStr.startsWith(ECHO_CHECK_SCORE);
            if (!isScoreCheck)
            {
                seq = C.lsprintf(30, "x%d ", u.rng_out_seq);
                u.rng_out_seq++;
            }
        }
        int[] sum = new int[1];
        if (u.checksums)
        {
            String body = localStringCopy(u, seq, inStr, sum);
            int s = sum[0];
            char[] pre = "501 AAAA ".toCharArray();
            pre[4] += (char) ((s >> 12) & 0xf);
            pre[5] += (char) ((s >> 8) & 0xf);
            pre[6] += (char) ((s >> 4) & 0xf);
            pre[7] += (char) (s & 0xf);
            return new String(pre) + body;
        }
        return localStringCopy(u, seq, inStr, sum);
    }

    // =====================================================================
    // output
    // =====================================================================

    /** start output for a user: >0 if some was sent, <0 on error, 0 if nothing sent */
    int startOutput(User u)
    {
        int err = 0;
        int slen = u.outputPending();
        if (slen == 0)
        {
            if (logging >= LOG_ALL)
            {
                logEntry(mainLog, "[%s] start but no data for C%d (%s#%d) S%d %d\n",
                    timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, 0);
            }
        }
        else
        {
            int len;
            if (!isRealSocket(u.socket) || u.conn == null)
            {
                len = -2;
            }
            else
            {
                try
                {
                    len = u.conn.write(u.outbuf, u.outTake, slen);
                    if (len <= 0) { u.badWriteCount++; }
                    else { u.badWriteCount = 0; u.goodWriteCount++; }
                }
                catch (IOException e)
                {
                    u.badWriteCount++;
                    len = -1;
                    if (logging >= LOG_ERRORS)
                    {
                        logEntry(mainLog, "[%s] write err C%d (%s#%d) S%d  error=%s\n",
                            timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, e.toString());
                    }
                }
            }
            err = len;
            if (len >= 0)
            {
                boolean blocked = u.blocked;
                u.consumeOutput(len);
                u.totalwrite += len;
                totalwrite += len;
                if (len == slen)
                {
                    if (blocked) { unblocked_clients++; u.blocked = false; }
                    if (logging >= LOG_ALL)
                    {
                        logEntry(mainLog, "[%s] send %d (all) for C%d (%s#%d) S%d %d\n",
                            timestamp(), len, u.userNUM, u.clientRealName, u.clientUid, u.socket, slen);
                    }
                }
                else
                {
                    writeBreak++;
                    if (!blocked)
                    {
                        logEntry(mainLog, "[%s] blocked write for C%d (%s#%d) S%d, %d<%d\n",
                            timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, len, slen);
                        blocked_clients++;
                        u.blocked = true;
                    }
                }
            }
        }
        // a user marked for closure whose output has drained is closed by the main loop
        // (serviceUser), not here in the middle of whatever is sending, as in the C server
        return err;
    }

    int restartOutput(User u)
    {
        int writeResult = 0;
        if (isRealSocket(u.socket))
        {
            boolean blocked = u.blocked;
            writeResult = startOutput(u);
            if (writeResult < 0)
            {
                if (!u.expectEof)
                {
                    u.wasZapped = true;
                    if (logging >= LOG_ERRORS)
                    {
                        logEntry(mainLog, "[%s] Zap At %d:(restart) Closing C%d (%s#%d) S%d  due to error %d, pending=%d\n",
                            timestamp(), 0, u.userNUM, u.clientRealName, u.clientUid, u.socket, writeResult, u.outputPending());
                    }
                }
                HandleWriteError(u, "Zap restart output");
            }
            else if (blocked)
            {
                if (logging >= LOG_ERRORS)
                {
                    logEntry(mainLog, "[%s] restarting C%d (%s#%d) S%d sent %d rem %d\n",
                        timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, writeResult, u.outputPending());
                }
            }
        }
        return writeResult;
    }

    int lsend0(User u, String tempb)
    {
        byte[] b = C.bytes(tempb);
        if (u.outputPending() + b.length > MAX_OUTPUT_PENDING)
        {
            return ERR_NOLISTEN;		// client isn't reading
        }
        u.appendOutput(b, 0, b.length);
        u.nwrite++;
        if (u.blocked) { return 0; }
        return startOutput(u);
    }

    int lsend(User u, String buffer)
    {
        String tempb = addChecksum(u, buffer);
        int fput = u.outPut;
        int ftake = u.outTake;
        int res = lsend0(u, tempb);
        recordInHistory("out", u.socket, buffer, tempb.length());
        if (logging >= LOG_ALL)
        {
            logEntry(mainLog, "[%s] sending C%d (%s#%d) S%d: %d=(%d,%d)(%d,%d) %s\n",
                timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket,
                res, fput, ftake, u.outPut, u.outTake, buffer);
        }
        return res;
    }

    User findAslot(Session s)
    {
        User U = Idle_Users.first_user;
        if (U != null)
        {
            removeUserFromSession(U);
            ClearUser(U);
            putUserInSession(U, s);
            return U;
        }
        if (logging >= LOG_ERRORS)
        {
            unusual_events++;
            logEntry(securityLog, "[%s] UNUSUAL: couldn't find a user, Nleft=%d\n", timestamp(), Idle_Users.population);
        }
        return null;
    }

    int checkSession(int ip, Session S, int max)
    {
        User U = S.first_user;
        int pop = S.population;
        int nfound = 0;
        int nhits = 0;
        while (U != null)
        {
            User next = U.next_in_session;
            check(U.session == S, "checkSession: user in wrong session");
            if (U.ip == ip)
            {
                nhits++;
                if (nhits > max)
                {
                    String stamp = timestamp();
                    int b4 = (ip >> 0) & 0xff;
                    int b3 = (ip >> 8) & 0xff;
                    int b2 = (ip >> 16) & 0xff;
                    int b1 = (ip >>> 24) & 0xff;
                    unusual_events++;
                    if (logging >= LOG_ERRORS)
                    {
                        logEntry(mainLog, "[%s] UNUSUAL: ip %d.%d.%d.%d rejected (too many connections in Session %d from ip)\n",
                            stamp, b1, b2, b3, b4, S.sessionNUM);
                    }
                    logEntry(securityLog, "[%s] UNUSUAL: ip %d.%d.%d.%d rejected (too many connections in Session %d from ip)\n",
                        stamp, b1, b2, b3, b4, S.sessionNUM);
                    return -1;
                }
            }
            U = next;
            nfound++;
            check(nfound <= pop, "checkSession: population");
        }
        check(nfound == pop, "checkSession: population count");
        return nfound;
    }

    boolean checkBannedIP(int ip, boolean websock)
    {
        if (isBanned("", 0, (char) 0, "", ip) != bc_none)
        {
            if (logging >= LOG_ERRORS)
            {
                int b4 = (ip >> 0) & 0xff;
                int b3 = (ip >> 8) & 0xff;
                int b2 = (ip >> 16) & 0xff;
                int b1 = (ip >>> 24) & 0xff;
                logEntry(mainLog, "[%s] UNUSUAL: ip %d.%d.%d.%d rejected - connection from banned %sIP\n",
                    timestamp(), b1, b2, b3, b4, websock ? "websocket " : "");
            }
            return true;
        }
        return false;
    }

    boolean checkTooMany(int ip, boolean websock)
    {
        int n = checkSession(ip, WaitingSession, maxConnections);
        if (n < 0)
        {
            if (logging >= LOG_ERRORS)
            {
                int b4 = (ip >> 0) & 0xff;
                int b3 = (ip >> 8) & 0xff;
                int b2 = (ip >> 16) & 0xff;
                int b1 = (ip >>> 24) & 0xff;
                logEntry(mainLog, "[%s] Call from %d.%d.%d.%d refused and banned %s too many connections\n",
                    timestamp(), b1, b2, b3, b4, websock ? "websocket " : "");
            }
            return true;
        }
        return false;
    }

    void doEchoAll(User ru, String toOthers, boolean ignoreF)
    {
        Session S = ru.session;
        ignoreF |= ru.expectEof;
        if (S != WaitingSession && S != ProxySession && !ru.gagged && (isRealSocket(ru.socket) || ignoreF))
        {
            User su = S.first_user;
            int pop = S.population;
            int nfound = 0;
            while (su != null)
            {
                User next = su.next_in_session;
                check(su.session == S, "doEchoAll: user in wrong session");
                if (isRealSocket(su.socket) && !su.gagged)
                {
                    int writeResult = lsend(su, toOthers);
                    if (writeResult < 0)
                    {
                        if (!su.expectEof)
                        {
                            su.wasZapped = true;
                            if (logging >= LOG_ERRORS)
                            {
                                logEntry(mainLog, "[%s] Zap At %d:(echo2) Closing C%d (%s#%d) S%d  due to error %d\n",
                                    timestamp(), 0, su.userNUM, su.clientRealName, su.clientUid, su.socket, writeResult);
                            }
                        }
                        HandleWriteError(su, "echo other zap");
                    }
                }
                nfound++;
                su = next;
                // closing a user can clear the whole session; stop rather than walk the list it moved to
                if (su != null && su.session != S) { break; }
                check(nfound <= pop, "doEchoAll: population");
            }
            check(su != null || nfound == pop, "doEchoAll: population count");
        }
    }

    void doEchoOthers(User ru, String toOthers, boolean ignoreF)
    {
        Session S = ru.session;
        ignoreF |= ru.expectEof;
        if (toOthers != null && S != WaitingSession && S != ProxySession && !ru.gagged
            && (isRealSocket(ru.socket) || ignoreF))
        {
            User su = S.first_user;
            int pop = S.population;
            int nfound = 0;
            while (su != null)
            {
                User next = su.next_in_session;
                check(su.session == S, "doEchoOthers: user in wrong session");
                if (ru != su && isRealSocket(su.socket) && !su.gagged)
                {
                    int writeResult = lsend(su, toOthers);
                    if (writeResult < 0)
                    {
                        if (!su.expectEof)
                        {
                            su.wasZapped = true;
                            if (logging >= LOG_ERRORS)
                            {
                                logEntry(mainLog, "[%s] Zap At %d:(echo2) Closing C%d (%s#%d) S%d  due to error %d\n",
                                    timestamp(), 0, su.userNUM, su.clientRealName, su.clientUid, su.socket, writeResult);
                            }
                        }
                        HandleWriteError(su, "echo other zap");
                    }
                }
                nfound++;
                su = next;
                // closing a user can clear the whole session; stop rather than walk the list it moved to
                if (su != null && su.session != S) { break; }
                check(nfound <= pop, "doEchoOthers: population");
            }
            check(su != null || nfound == pop, "doEchoOthers: population count");
        }
    }

    void doEchoSelf(User ru, String toSource, boolean ignoreF)
    {
        ignoreF |= ru.expectEof;
        if (toSource != null && isRealSocket(ru.socket))
        {
            int writeResult = lsend(ru, toSource);
            if (writeResult < 0)
            {
                if (!ignoreF)
                {
                    ru.wasZapped = true;
                    if (logging >= LOG_ERRORS)
                    {
                        logEntry(mainLog, "[%s] Zap At %d:(echo) Closing C%d (%s#%d) S%d  due to error %d\n",
                            timestamp(), 0, ru.userNUM, ru.clientRealName, ru.clientUid, ru.socket, writeResult);
                    }
                }
                HandleWriteError(ru, "echo zap");
            }
        }
    }

    void markForClosure(User u)
    {
        u.inputClosed = true;
        u.outputClosed = true;
    }

    int sendSingleLen(String inStr, User u)
    {
        int err = 0;
        if (isRealSocket(u.socket) && !u.outputClosed)
        {
            if ((err = lsend(u, inStr)) < 0)
            {
                if (!u.expectEof)
                {
                    u.wasZapped = true;
                    if (logging >= LOG_ERRORS)
                    {
                        logEntry(mainLog, "[%s] Zap At %d: Closing C%d (%s#%d) S%d due to error %d.\n",
                            timestamp(), 0, u.userNUM, u.clientRealName, u.clientUid, u.socket, err);
                    }
                }
                HandleWriteError(u, "send error");
            }
        }
        else
        {
            logEntry(mainLog, "[%s] Unusual: send to already closed C%d session %d msg %s",
                timestamp(), u.userNUM, u.session == null ? -1 : u.session.sessionNUM, inStr);
            unusual_events++;
        }
        return err < 0 ? -1 : 0;
    }

    int sendSingle(String inStr, User u)
    {
        int err = 0;
        if (isRealSocket(u.socket)) { err = sendSingleLen(inStr, u); }
        return err;
    }

    void sendMulti(String inStr, Session s)
    {
        User U = s.first_user;
        int pop = s.population;
        int nfound = 0;
        while (U != null)
        {
            User next = U.next_in_session;
            check(U.session == s, "sendMulti: user in wrong session");
            if (isRealSocket(U.socket)) { sendSingleLen(inStr, U); }
            U = next;
            nfound++;
            // closing a user can clear the whole session; stop rather than walk the list it moved to
            if (U != null && U.session != s) { break; }
            check(nfound <= pop, "sendMulti: population");
        }
        check(U != null || nfound == pop, "sendMulti: population count");
    }

    void errMsg(int line, int stream, String msg)
    {
        if (logging >= LOG_ERRORS)
        {
            logEntry(mainLog, "[%s] At line %d, closing S%d due to %s\n", timestamp(), line, stream, msg);
        }
    }

    // =====================================================================
    // game cache
    // =====================================================================

    static final int HASHSIZE = MAXGAMES * 2;

    void setGameBufferSize(GameBuffer g, int min, boolean copy)
    {
        int newsize = roundUp(min + BUFFER_ALLOC_SLOP, BUFFER_ALLOC_STEP);
        byte[] newbuf = new byte[newsize];
        if (g.gamePtr != null && copy)
        {
            System.arraycopy(g.gamePtr, 0, newbuf, 0, Math.min(g.gamePtrSize, newsize));
        }
        g.gamePtr = newbuf;
        g.gamePtrSize = newsize;
    }

    void markAsDirty(GameBuffer g)
    {
        if (g.preserved && !g.dirty && gameCacheRate > 0)
        {
            g.dirty = true;
            if (logging >= LOG_ALL)
            {
                logEntry(mainLog, "[%s] dirty refcount %d up %s#%d\n", timestamp(), g.refCount, g.idString, g.uid);
            }
            g.refCount++;
            n_dirty_gamebuffers++;
            g.next_dirty_gamebuffer = null;
            if (first_dirty_gamebuffer == null) { first_dirty_gamebuffer = g; }
            else { last_dirty_gamebuffer.next_dirty_gamebuffer = g; }
            last_dirty_gamebuffer = g;
            if (n_dirty_gamebuffers > max_dirty_gamebuffers) { max_dirty_gamebuffers = n_dirty_gamebuffers; }
        }
    }

    void markAsClean(GameBuffer g)
    {
        boolean found = false;
        GameBuffer game = first_dirty_gamebuffer;
        GameBuffer prev = null;
        GameBuffer gnext = g.next_dirty_gamebuffer;
        while (game != null)
        {
            if (game == g)
            {
                n_dirty_gamebuffers--;
                g.dirty = false;
                found = true;
                if (prev == null)
                {
                    first_dirty_gamebuffer = gnext;
                }
                else
                {
                    prev.next_dirty_gamebuffer = gnext;
                    if (last_dirty_gamebuffer == g) { last_dirty_gamebuffer = prev; }
                }
                break;
            }
            prev = game;
            game = game.next_dirty_gamebuffer;
        }
        if (!found)
        {
            n_dirty_gamebuffers--;
            g.dirty = false;
            logEntry(mainLog, "[%s] Emergency decrement of GameBuffer dirty count %s#%d\n", timestamp(), g.idString, g.uid);
        }
        if (logging >= LOG_ALL)
        {
            logEntry(mainLog, "[%s] clean refcount %d down %s#%d\n", timestamp(), g.refCount, g.idString, g.uid);
        }
        if (g.refCount-- == 1)
        {
            if (g.ownerSession == null) { freeGameBuffer(g); }
        }
    }

    void unhashGame(GameBuffer game)
    {
        if (game.hashed)
        {
            int idx = C.unsignedMod(game.hashCode, HASHSIZE);
            GameBuffer target;
            while ((target = GameHash[idx]) != null)
            {
                target.hashed = false;
                GameHash[idx] = null;
                games_cached--;
                if (target == game)
                {
                    if (logging >= LOG_CONNECTIONS)
                    {
                        logEntry(mainLog, "[%s] removing cached game %s#%d, hashcode %d slot %d games_cached=%d\n",
                            timestamp(), target.idString, target.uid, target.hashCode, idx, games_cached);
                    }
                }
                else
                {
                    hashGame(target);
                }
                idx++;
                if (idx >= HASHSIZE) { idx = 0; }
            }
            game.hashed = false;
        }
    }

    void unpreserveGame(GameBuffer game)
    {
        if (game.preserved)
        {
            markAsDirty(game);
            game.preserved = false;
            if (logging >= LOG_CONNECTIONS)
            {
                logEntry(mainLog, "[%s] unpreserve refCount %d down %s#%d\n", timestamp(), game.refCount, game.idString, game.uid);
            }
            game.refCount--;
            unhashGame(game);
        }
    }

    void gameHashCleanup()
    {
        int passes = 0;
        while (games_cached >= MAXGAMES)
        {
            GameBuffer all = all_gamebuffers;
            GameBuffer min_game = null;
            long now = daysSinceForever();
            long min_time = 0;
            int before = games_cached;
            passes++;
            logEntry(mainLog, "[%s] gameHashCleanup pass %d, games_cached=%d n_gamebuffers=%d\n",
                timestamp(), passes, games_cached, n_gamebuffers);
            while (all != null)
            {
                GameBuffer next = all.next;
                if (all.preserved && all.ownerSession == null)
                {
                    if (min_game == null && all.hashed)
                    {
                        min_game = all;
                        min_time = all.startTime;
                    }
                    if ((now - all.startTime) > GAMEEXPIRED)
                    {
                        if (logging >= LOG_CONNECTIONS)
                        {
                            logEntry(mainLog, "[%s] unpreserve expired game refCount %d %s#%d\n",
                                timestamp(), all.refCount, all.idString, all.uid);
                        }
                        unpreserveGame(all);
                    }
                    else if (all.startTime < min_time)
                    {
                        min_time = all.startTime;
                        min_game = all;
                    }
                }
                all = next;
            }
            if (games_cached >= MAXGAMES && min_game != null)
            {
                if (logging >= LOG_CONNECTIONS)
                {
                    logEntry(mainLog, "[%s] unpreserve random victim game refCount %d %s#%d\n",
                        timestamp(), min_game.refCount, min_game.idString, min_game.uid);
                }
                unpreserveGame(min_game);
            }
            if (games_cached >= before)
            {
                unusual_events++;
                logEntry(mainLog, "[%s] UNUSUAL: gameHashCleanup made no progress, games_cached=%d n_gamebuffers=%d\n",
                    timestamp(), games_cached, n_gamebuffers);
                break;
            }
        }
    }

    GameBuffer hashGame(GameBuffer game)
    {
        int idx = C.unsignedMod(game.hashCode, HASHSIZE);
        int probes = 0;
        while (GameHash[idx] != null)
        {
            idx++;
            if (idx >= HASHSIZE) { idx = 0; }
            if (++probes > HASHSIZE) { check(false, "hashGame: table full"); }
        }
        GameHash[idx] = game;
        game.hashed = true;
        games_cached++;
        return game;
    }

    GameBuffer preserveGame(GameBuffer game)
    {
        if (!game.preserved)
        {
            gameHashCleanup();
            if (logging >= LOG_CONNECTIONS)
            {
                logEntry(mainLog, "[%s] preserve refcount %d up %s#%d\n", timestamp(), game.refCount, game.idString, game.uid);
            }
            game.refCount++;
            game.preserved = true;
            hashGame(game);
        }
        return game;
    }

    GameBuffer findGame(int hash, String str)
    {
        int idx = C.unsignedMod(hash, HASHSIZE);
        GameBuffer target;
        int probes = 0;
        while ((target = GameHash[idx]) != null && probes++ <= HASHSIZE)
        {
            if (str.equals(target.idString))
            {
                long now = daysSinceForever();
                if (target.startTime > 0 && (now - target.startTime) > GAMEEXPIRED)
                {
                    markAsDirty(target);
                    unpreserveGame(target);
                    target = null;
                }
                else
                {
                    target.startTime = now;
                }
                return target;
            }
            idx++;
            if (idx >= HASHSIZE) { idx = 0; }
        }
        return null;
    }

    /** copy the parts of a game the save thread needs (C CopyGame) */
    static GameCopy CopyGame(GameBuffer src)
    {
        GameCopy d = new GameCopy();
        d.idString = src.idString;
        d.startTime = src.startTime;
        d.preserved = src.preserved;
        d.game = (src.gamePtr == null) ? new byte[1] : src.gamePtr.clone();
        return d;
    }

    /** move dirty games to the write pool (main thread).  The save thread empties it. */
    boolean saveDirtyGames()
    {
        boolean progress = false;
        while (first_dirty_gamebuffer != null)
        {
            if (writePool.remainingCapacity() == 0) { break; }
            if (first_dirty_gamebuffer.dirty)
            {
                writePool.offer(CopyGame(first_dirty_gamebuffer));
            }
            markAsClean(first_dirty_gamebuffer);
            progress = true;
        }
        return progress;
    }

    GameBuffer makeGameBuffer()
    {
        GameBuffer rval = new GameBuffer();
        rval.preserved = false;
        rval.refCount = 0;
        rval.hashed = false;
        rval.magic = GameBuffer.GAMEBUFMAGIC;
        rval.uid = GameUIDs++;
        rval.startTime = daysSinceForever();
        rval.idString = "";
        rval.hashCode = 0;
        rval.gamePtr = null;
        rval.gamePtrSize = 0;
        rval.gamePtrOffset = 0;
        rval.next = all_gamebuffers;
        rval.prev = null;
        if (all_gamebuffers != null) { all_gamebuffers.prev = rval; }
        n_gamebuffers++;
        all_gamebuffers = rval;
        return rval;
    }

    void freeGameBuffer(GameBuffer rval)
    {
        if (logging >= LOG_CONNECTIONS)
        {
            logEntry(mainLog, "[%s] freegamebuffer refcount %d %s#%d freegamebuffer\n", timestamp(), rval.refCount, rval.idString, rval.uid);
        }
        check(rval.magic == GameBuffer.GAMEBUFMAGIC, "freeGameBuffer: magic");
        check(!rval.preserved, "freeGameBuffer: still preserved");
        check(!rval.dirty, "freeGameBuffer: still dirty");
        check(rval.ownerSession == null, "freeGameBuffer: still owned");
        rval.magic = 0xDeadbeef;
        rval.gamePtr = null;
        rval.gamePtrSize = 0;
        GameBuffer next = rval.next;
        GameBuffer prev = rval.prev;
        rval.next = null;
        rval.prev = null;
        if (next != null) { next.prev = prev; }
        if (prev != null) { prev.next = next; }
        if (all_gamebuffers == rval) { all_gamebuffers = next; }
        n_gamebuffers--;
    }

    // ---------------------------------------------------------------- cache files
    //
    // File format, identical to the C server's on 64 bit Linux (and Windows) so the
    // two servers can read each other's cache:
    //   offset 0   int32  magic 0xfafe9193 (little endian)
    //   offset 4   char[64] idString, NUL terminated
    //   offset 72  int64  startTime in days since 1970 (Windows C wrote 32 bits here)
    //   offset 80  the game text, NUL terminated, followed by unused buffer space
    // Files written before 2014 ("OldGameBuffer") start directly with the 64 byte id
    // and the game text at offset 64.
    //
    static final int CACHE_ID_OFFSET = 4;
    static final int CACHE_TIME_OFFSET = 72;
    static final int CACHE_GAME_OFFSET = 80;

    static String cacheFileName(String dir, String idString)
    {
        StringBuilder sanitized = new StringBuilder();
        for (int i = 0; i < idString.length() && sanitized.length() < SMALLBUFSIZE - 1; i++)
        {
            char ch = idString.charAt(i);
            switch (ch)
            {
            case '\\': case ':': case '/': case '<': case '>': case '|':
                sanitized.append('-');
                break;
            default:
                sanitized.append(ch);
            }
        }
        return C.lsprintf(SMALLBUFSIZE, "%sG%s." + GAMESUFFIX, dir, sanitized.toString());
    }

    /** encode a game for the cache file */
    static byte[] encodeCacheFile(GameCopy g)
    {
        int glen = g.game.length;
        ByteBuffer b = ByteBuffer.allocate(CACHE_GAME_OFFSET + glen).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(0, GameBuffer.GAMEBUFMAGIC);
        byte[] id = C.bytes(C.trunc(g.idString, UIDSIZE));
        b.position(CACHE_ID_OFFSET);
        b.put(id);
        b.putLong(CACHE_TIME_OFFSET, g.startTime);
        b.position(CACHE_GAME_OFFSET);
        b.put(g.game);
        return b.array();
    }

    boolean reloadOneFile(File f)
    {
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || !name.substring(dot + 1).equalsIgnoreCase(GAMESUFFIX)) { return false; }
        byte[] data;
        try (RandomAccessFile r = new RandomAccessFile(f, "r"))
        {
            data = new byte[(int) r.length()];
            r.readFully(data);
        }
        catch (IOException e)
        {
            return false;
        }
        if (data.length == 0) { return false; }
        GameBuffer g = makeGameBuffer();
        setGameBufferSize(g, data.length, false);
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        if (data.length >= 4 && bb.getInt(0) == GameBuffer.GAMEBUFMAGIC)
        {
            g.idString = C.trunc(C.cstr(data, CACHE_ID_OFFSET), UIDSIZE);
            g.magic = GameBuffer.GAMEBUFMAGIC;
            if (data.length >= CACHE_TIME_OFFSET + 4)
            {
                // read 32 bits: correct for files from both the Linux and Windows builds
                g.startTime = bb.getInt(CACHE_TIME_OFFSET);
            }
            if (data.length > CACHE_GAME_OFFSET)
            {
                C.myStrncpy(g.gamePtr, 0, data, CACHE_GAME_OFFSET, g.gamePtrSize);
            }
        }
        else
        {
            // legacy OldGameBuffer: id string then game text
            g.idString = C.trunc(C.cstr(data, 0), Math.min(UIDSIZE, data.length));
            g.magic = GameBuffer.GAMEBUFMAGIC;
            g.hashCode = 0;
            if (data.length > UIDSIZE)
            {
                C.myStrncpy(g.gamePtr, 0, data, UIDSIZE, g.gamePtrSize);
            }
        }
        g.dirty = false;
        g.next_dirty_gamebuffer = null;
        g.preserved = false;
        g.ownerSession = null;
        g.uid = GameUIDs++;
        g.idString = C.toLowercase(g.idString);
        g.hashCode = C.hashString(g.idString);
        preserveGame(g);
        return true;
    }

    /**
     * check the game cache directory at startup, so a missing or unwritable one is
     * reported once, plainly, instead of only as failures when games are saved
     */
    void checkGameCacheDir(String dir)
    {
        char last = dir.charAt(dir.length() - 1);
        if (last != '/' && last != '\\')
        {	// file names are made by appending to the directory name
            logEntry(mainLog, "[%s] UNUSUAL: gamecachedir %s doesn't end with a / so cached games are written beside it, as %sG<name>." + GAMESUFFIX + "\n",
                timestamp(), dir, dir);
        }
        File probe = new File(dir + "migs-write-test.tmp");
        try
        {
            Files.write(probe.toPath(), new byte[0]);
            Files.deleteIfExists(probe.toPath());
        }
        catch (IOException e)
        {
            logEntry(mainLog, "[%s] UNUSUAL: can't write in gamecachedir %s (%s); games will not be saved\n",
                timestamp(), dir, ioReason(e));
        }
    }

    /** a short reason for an i/o failure, like strerror */
    static String ioReason(IOException e)
    {
        if (e instanceof java.nio.file.NoSuchFileException) { return "No such file or directory"; }
        if (e instanceof java.nio.file.AccessDeniedException) { return "Permission denied"; }
        if (e instanceof java.nio.file.FileSystemException && ((java.nio.file.FileSystemException) e).getReason() != null)
        {
            return ((java.nio.file.FileSystemException) e).getReason();
        }
        String m = e.getMessage();
        int paren = m == null ? -1 : m.lastIndexOf(" (");
        if (paren >= 0 && m.endsWith(")")) { return m.substring(paren + 2, m.length() - 1); }	// FileOutputStream: "name (reason)"
        return m == null ? e.getClass().getSimpleName() : m;
    }

    /** read the cache directory.  Startup only, before the main loop starts. */
    void reloadGames()
    {
        if (gameCacheDir == null || gameCacheDir.isEmpty())
        {
            logEntry(mainLog, "[%s] gamecachedir is empty, games will not be cached\n", timestamp());
        }
        else if (gameCacheRate <= 0)
        {
            logEntry(mainLog, "[%s] gamecacherate is %d, games will not be cached\n", timestamp(), gameCacheRate);
        }
        if (gameCacheDir != null && !gameCacheDir.isEmpty())
        {
            int reloaded = 0;
            checkGameCacheDir(gameCacheDir);
            File dir = new File(gameCacheDir);
            File[] files = dir.listFiles();
            if (files != null)
            {
                for (File f : files)
                {
                    if (f.isFile() && reloadOneFile(f)) { reloaded++; }
                }
            }
            logEntry(mainLog, "[%s] %d games reloaded from %s\n", timestamp(), reloaded, gameCacheDir);
        }
    }

    /** the save thread: write or delete up to gameCacheRate games per second */
    void startSaveGameThread()
    {
        Thread t = new Thread(() -> {
            while (!killThreads)
            {
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {
                    return;
                }
                int rate = gameCacheRate;
                GameCopy g;
                while (rate-- > 0 && (g = writePool.poll()) != null)
                {
                    saveDirtyGameWriteStep(g);
                }
                if (sslProvider != null) { sslProvider.check(); }	// daily: pick up a renewed certificate
            }
        }, "save-games");
        t.setDaemon(true);
        t.start();
    }

    /** runs in the save thread: save one game, or delete it if no longer preserved */
    void saveDirtyGameWriteStep(GameCopy g)
    {
        if (gameCacheDir == null || gameCacheDir.isEmpty()) { return; }
        String gameName = cacheFileName(gameCacheDir, g.idString);
        if (g.preserved)
        {
            FileOutputStream of;
            try
            {
                of = new FileOutputStream(gameName);
            }
            catch (IOException e)
            {
                logEntry(mainLog, "[%s] open for save GameBuffer failed file %s (%s)\n", timestamp(), gameName, ioReason(e));
                n_dirty_problems.incrementAndGet();
                return;
            }
            try (FileOutputStream o = of)
            {
                o.write(encodeCacheFile(g));
                n_dirty_writes.incrementAndGet();
            }
            catch (IOException e)
            {
                logEntry(mainLog, "[%s] write for save GameBuffer failed file %s (%s)\n", timestamp(), gameName, ioReason(e));
                n_dirty_problems.incrementAndGet();
            }
        }
        else
        {
            String why = null;
            try
            {
                Files.delete(new File(gameName).toPath());
            }
            catch (IOException e)
            {
                why = ioReason(e);
            }
            if (logging >= LOG_ERRORS)
            {
                logEntry(mainLog, "[%s] unlink for %s\n", timestamp(), gameName);
            }
            if (why != null)
            {
                n_dirty_problems.incrementAndGet();
                logEntry(mainLog, "[%s] unlink for GameBuffer failed file %s (%s)\n", timestamp(), gameName, why);
            }
        }
    }

    // =====================================================================
    // status page
    // =====================================================================

    void saveConfig() { configChange = true; }

    /** build the status page (main thread) and hand it to the file writer */
    void realSaveConfig()
    {
        if (statusfile == null) { return; }
        StringBuilder tempfp = new StringBuilder();
        tempfp.append(String.format("<html><head><META HTTP-EQUIV=REFRESH CONTENT=300></head><title>Server Status on %s</title>"
            + "<h1>Server Status on %s</h1>\n", host_name, host_name));
        for (int session = 0; session <= maxSessions; session++)
        {
            Session s = Sessions[session];
            StringBuilder playerbuf = new StringBuilder();
            User u = s.first_user;
            boolean someprint = false;
            printSessionHeader(tempfp, s);
            while (u != null && u.session == s)
            {
                User next = u.next_in_session;
                if (isRealSocket(u.socket))
                {
                    if (someprint) { playerbuf.append(", "); }
                    someprint = true;
                    String clientname = u.clientPublicName;
                    if (clientname.isEmpty()) { clientname = "(unknown)"; }
                    playerbuf.append("<a target=_NEW href=");
                    playerbuf.append(C.lsprintf(LARGEBUFFER, playerInfoUrl, clientname));
                    playerbuf.append(">").append(clientname).append("</a>");
                }
                u = next;
            }
            if (playerbuf.length() > 0) { tempfp.append(": ").append(playerbuf); }
            if (session == 0) { tempfp.append("<br>"); }
        }
        tempfp.append(String.format("<p>%d players ever connected\n<br>  %d games ever launched\n"
            + "<br>%d games completed,  %d player games relaunched, %d robot games relaunched\n"
            + "<br>%d games cached, %d allocated, %d saves pending (%d max), : %d saves completed %d problems\n",
            clients_connected, games_launched, games_completed, player_games_relaunched, robot_games_relaunched,
            games_cached, n_gamebuffers, n_dirty_gamebuffers, max_dirty_gamebuffers,
            n_dirty_writes.get(), n_dirty_problems.get()));
        tempfp.append(String.format("<br>%d clients zapped by errors, %d-%d+%d clients blocked\n", client_errors,
            blocked_clients, unblocked_clients, closed_blocked_clients));
        tempfp.append(String.format("<br>%d checksum errors %dk transactions %dm in %dm out \n",
            checksum_errors, transactions / 1000, totalread / 1000000, totalwrite / 1000000));
        tempfp.append(String.format("<br>%d client messages %d unusual events %d robot irregularities\n",
            client_messages, unusual_events, robot_irregular));
        if (save_failed.get() != 0)
        {
            tempfp.append(String.format("<br>%d save game failed\n", save_failed.get()));
        }
        tempfp.append(String.format("<br>%d users banned, %d additional connection attempts<br>", totalBanned, totalAttempts));
        if (isshutdown != 0) { tempfp.append("<br><b>Server has been shut down by the supervisor\n</b><br>"); }
        Runtime rt = Runtime.getRuntime();
        tempfp.append(String.format("%d/%d gamebuffers, %dk allocated<br>", GameUIDs - 1, n_gamebuffers,
            (rt.totalMemory() - rt.freeMemory()) / 1024));
        long now = uptime();
        long up = now - start_time;
        long upminutes = up / 60;
        long uphours = upminutes / 60;
        long updays = uphours / 24;
        long idleminutes = idle_time / 60;
        long idlehours = idleminutes / 60;
        upminutes = upminutes % 60;
        uphours = uphours % 24;
        idleminutes = idleminutes % 60;
        tempfp.append(String.format("<br>Up for %d Days %2d:%02d  Max Players %d  Max Socks %d Idle %d:%02d\n",
            updays, uphours, upminutes, maxlobby, maxsockets, idlehours, idleminutes));
        tempfp.append("\n</html>\n");
        final String page = tempfp.toString();
        final String file = statusfile;
        fileWriter.execute(() -> {
            try (FileOutputStream o = new FileOutputStream(file))
            {
                o.write(C.bytes(page));
            }
            catch (IOException e)
            {
                // the C server silently skipped the status page if it couldn't open the file
            }
        });
    }

    void printSessionHeader(StringBuilder tempfp, Session s)
    {
        int numInSession = s.population;
        int session = s.sessionNUM;
        if (numInSession > 0 || session == 0)
        {
            String pl = numInSession == 1 ? "" : "s";
            if (session == 0)
            {
                tempfp.append(String.format("<p>%d player%s in the Lobby\n", numInSession, pl));
                if (maxlobby < numInSession) { maxlobby = numInSession; }
            }
            else
            {
                int mode = s.sessionStates;
                String descr = (mode >= 0 && mode < nroomtypes) ? roomtypes[mode] : "Nroom";
                boolean isgame = descr.startsWith("G");
                int gameid = (s.sessionGameID / 1000) - 1;
                if (!descr.isEmpty()) { descr = descr.substring(1); }
                tempfp.append(String.format("<br>%d player%s in %s %d ", numInSession, pl, descr, session));
                if (isgame && gameid >= 0 && gameid < ngametypes)
                {
                    String name = gametypes[gameid];
                    if (name != null && !name.isEmpty()) { tempfp.append("(").append(name).append(")"); }
                }
            }
        }
    }

    // =====================================================================
    // registered users (STRICT)
    // =====================================================================

    RegisteredUser findRegisteredUser(int regkey, String name, int uid)
    {
        RegisteredUser ru = activeRegisteredUsers;
        while (ru != null)
        {
            if (ru.regkey == regkey && name.equals(ru.clientName) && (ru.clientUid == 0 || uid == ru.clientUid))
            {
                return ru;
            }
            ru = ru.next;
        }
        return null;
    }

    void update_user_timestamps()
    {
        long now = uptime();
        for (int sessn = 0; sessn <= maxSessions; sessn++)
        {
            Session S = Sessions[sessn];
            User u = S.first_user;
            int pop = S.population;
            int nfound = 0;
            while (u != null)
            {
                User next = u.next_in_session;
                if (isRealSocket(u.socket) && u.keyIndex != null) { u.keyIndex.timestamp = now; }
                u = next;
                nfound++;
                check(nfound <= pop, "update_user_timestamps: population");
            }
            check(nfound == pop, "update_user_timestamps: population count");
        }
    }

    void purge_registered_users()
    {
        update_user_timestamps();
        RegisteredUser ru = activeRegisteredUsers;
        RegisteredUser prev = null;
        long now = uptime();
        while (ru != null)
        {
            RegisteredUser next = ru.next;
            long dif = now - ru.timestamp;
            if (dif > REGISTERED_USER_TIMEOUT)
            {
                if (logging >= LOG_CONNECTIONS)
                {
                    logEntry(mainLog, "[%s] purge registered user %s #%8x \n", timestamp(), ru.clientName, ru.regkey);
                }
                ru.clientName = "";
                ru.ip = 0;
                ru.regkey = 0;
                ru.next = freeRegisteredUsers;
                freeRegisteredUsers = ru;
                if (prev == null) { activeRegisteredUsers = next; }
                else { prev.next = next; }
            }
            else
            {
                prev = ru;
            }
            ru = next;
        }
    }

    RegisteredUser isRegisteredUser(int regkey, int real_ip, String name, int uid)
    {
        purge_registered_users();
        RegisteredUser v = findRegisteredUser(regkey, name, uid);
        if (v != null) { v.ip = real_ip; }
        return v;
    }

    void registerUser(int regkey, String name, int uid, String auxinfo)
    {
        RegisteredUser isreg = isRegisteredUser(regkey, 0, name, uid);
        if (isreg == null && freeRegisteredUsers != null)
        {
            isreg = freeRegisteredUsers;
            freeRegisteredUsers = isreg.next;
            isreg.next = activeRegisteredUsers;
            activeRegisteredUsers = isreg;
            isreg.clientName = name;
            isreg.clientUid = uid;
            isreg.regkey = regkey;
        }
        if (isreg != null)
        {
            isreg.timestamp = uptime();
            if (logging >= LOG_CONNECTIONS)
            {
                logEntry(mainLog, "[%s] registering user %s#%d with key #%8x and \"%s\"\n", timestamp(), name, uid, regkey, auxinfo);
            }
        }
        else
        {
            logEntry(mainLog, "Failed to register user %s (no free slots\n", name);
        }
    }

    SSLContext sslContext() { return sslProvider == null ? null : sslProvider.get(); }
}
