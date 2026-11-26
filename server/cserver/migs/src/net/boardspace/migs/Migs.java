/*
 * TWO IMPLEMENTATIONS
 *
 *   This server exists twice: in C (migs.c, migs-lib.c, savegame.c, websocket.c, in the
 *   tantrix-server project) and in Java (Migs.java, Migs0.java, Migs1.java, Connection.java
 *   and friends, in the migs project).  The two are meant to be interchangeable:
 *
 *   - the same config file, read the same way
 *   - the same wire protocol, byte for byte: line messages, checksums, sequence numbers,
 *     the obfuscation cipher, websockets with and without TLS, and TLS on the standard port
 *   - the same game cache files; each server reads the other's, so a live site can switch
 *     back and forth with games in progress
 *   - the same logs in the same formats, and the same status page
 *
 *   The Java code follows the C function by function and keeps its names (process_send_intro,
 *   closeClient, markAsDirty...), so a change in one can be found and made in the other.
 *   Every change should be made in both.  tools/compare.py in the migs project runs the same
 *   client scripts against both servers and compares what each client receives; the Java
 *   README lists the few deliberate differences.
 *
 *   Merits of each:
 *
 *   C
 *   + speed and small size: a small memory footprint, nothing between the protocol and
 *     the sockets, and no garbage collector.
 *   + known reliability: it has run boardspace.net and tantrix.com for decades.  Its
 *     behavior under real load, bad networks and hostile clients is known, not predicted.
 *   + few dependencies: gcc and OpenSSL; no runtime to install or keep updated.
 *   - memory safety is up to the programmer.  Fixed-size buffers, manual frees and pointer
 *     arithmetic are where the bugs have been (buffer overruns, use after free); the
 *     defenses here (poisoned frees, deferred frees, guard words) catch them but don't
 *     prevent them.
 *   - harder to read and to extend: fixed buffers and their sizes thread through everything,
 *     and a mistake shows up as a crash or as quietly wrong data.
 *
 *   Java
 *   + easier to understand and to extend: strings and collections instead of fixed buffers,
 *     exceptions instead of error codes, and a mistake is an exception with a stack trace,
 *     not memory corruption.
 *   + memory safe: no overruns or use after free by construction.  An internal consistency
 *     failure closes only the client involved, and the server carries on.
 *   + TLS, certificate handling and non-blocking I/O come from the standard library.
 *   - more memory: the JVM's baseline is tens of megabytes, and each connection costs more.
 *   - somewhat lower performance, and garbage collection pauses; both well within what the
 *     current load needs, but not free.
 *   - needs a Java runtime on the server, kept up to date, and starts more slowly.
 *   - new: it has passed the comparison tests but hasn't yet had the C version's years of
 *     service.
 */
package net.boardspace.migs;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The migs game server: configuration, the event loop, and startup.
 * <p>
 * One thread runs the whole protocol, exactly as the C server did, using a
 * non-blocking selector in place of select().  That thread never blocks on
 * anything but the selector itself: sockets are non-blocking, and all file
 * writing (logs, game cache, completed games, status page, certificate
 * reloads) happens on background threads.
 * <p>
 * Usage: java -jar migs.jar &lt;config file&gt;  (the same config file as the C server)
 */
public final class Migs extends Migs1
{
    private Selector selector;
    private ServerSocketChannel serversd;
    private ServerSocketChannel webserversd;
    private final User[] activeClients = new User[MAXCLIENTS];
    private final byte[] readBuffer = new byte[64 * 1024];

    // =====================================================================
    // configuration (C ReadConfigFile / GetParm)
    // =====================================================================

    private String configFile;
    private final List<String[]> params = new ArrayList<>();

    // the C server keeps the config strings in a fixed buffer; keep the same limit
    // so a config file that works with one server works with the other
    static final int PAIRBUFSIZE = 20000;
    private int pairidx = 0;

    private String saveString(String str)
    {
        int len = str.length();
        if ((len + pairidx + 1) >= PAIRBUFSIZE)
        {
            error3("config file too long, string is %s : %d", str, len);
        }
        pairidx += len + 1;
        return str;
    }

    private void readConfigFile()
    {
        params.clear();
        pairidx = 0;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.ISO_8859_1)))
        {
            String line;
            while ((line = r.readLine()) != null)
            {
                if (line.startsWith("#")) { continue; }
                int split = line.indexOf(',');
                if (split >= 0)
                {
                    int len = line.length();
                    while (len > 0 && C.sc(line.charAt(len - 1)) <= ' ') { len--; }
                    line = line.substring(0, len);
                    if (split > line.length()) { split = line.length(); }
                    String key = line.substring(0, split);
                    String val = split + 1 <= line.length() ? line.substring(split + 1) : "";
                    params.add(new String[] { saveString(key), saveString(val) });
                }
            }
        }
        catch (IOException e)
        {
            error2("Couldn't open config file", configFile);
        }
    }

    String GetOptionalParm(String param, String def)
    {
        if (params.isEmpty()) { readConfigFile(); }
        for (String[] p : params)
        {
            if (p[0].equals(param)) { return p[1]; }
        }
        return def;
    }

    String GetParm(String param)
    {
        String v = GetOptionalParm(param, null);
        if (v == null)
        {
            error2("missing config parameter", param);
            return "";
        }
        return v;
    }

    static int atoi(String s)
    {
        Object[] o = new Object[1];
        return C.sscanf(s, "%d", o) == 1 ? (Integer) o[0] : 0;
    }

    // =====================================================================
    // listening sockets
    // =====================================================================

    private ServerSocketChannel listen(int port, String what) throws IOException
    {
        ServerSocketChannel s = ServerSocketChannel.open(StandardProtocolFamily.INET);
        s.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        s.configureBlocking(false);
        try
        {
            // backlog from "listen-backlog"; the kernel caps large values at net.core.somaxconn
            s.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port), listenBacklog);
        }
        catch (IOException e)
        {
            s.close();
            error("Failed bind for " + what + " (" + e.getMessage() + ")", port);
        }
        s.register(selector, SelectionKey.OP_ACCEPT);
        return s;
    }

    private void client_socket_init(int port) throws IOException
    {
        try
        {
            host_name = InetAddress.getLocalHost().getHostName();
        }
        catch (IOException e)
        {
            host_name = "localhost";
        }
        logEntry(mainLog, "Host is %s\n", host_name);
        logEntry(mainLog, "[%s] Server on %s, address %d.%d.%d.%d, using port %d.\n",
            timestamp(), host_name, IP1, IP2, IP3, IP4, port);
        serversd = listen(port, "main port");
    }

    private void client_websocket_init(int port) throws IOException
    {
        logEntry(mainLog, "[%s] Real Websocket Server using port %d.\n", timestamp(), port);
        logEntry(mainLog, "[%s] Websocket Server using port %d.\n", timestamp(), port);
        webserversd = listen(port, "websocket port");
    }

    @Override
    void closeListeners()
    {
        try
        {
            if (webserversd != null) { webserversd.close(); webserversd = null; }
            if (serversd != null) { serversd.close(); serversd = null; }
        }
        catch (IOException e)
        {
            // closing anyway
        }
    }

    // =====================================================================
    // connections
    // =====================================================================

    private static int ipOf(SocketChannel sc)
    {
        try
        {
            InetSocketAddress a = (InetSocketAddress) sc.getRemoteAddress();
            byte[] b = a.getAddress().getAddress();
            if (b.length == 4)
            {
                return ((b[0] & 0xff) << 24) | ((b[1] & 0xff) << 16) | ((b[2] & 0xff) << 8) | (b[3] & 0xff);
            }
        }
        catch (IOException | ClassCastException e)
        {
            // fall through
        }
        return 0;
    }

    private static void quietClose(SocketChannel sc)
    {
        try
        {
            sc.setOption(StandardSocketOptions.SO_LINGER, 0);
        }
        catch (IOException e)
        {
            // ignore
        }
        try
        {
            sc.close();
        }
        catch (IOException e)
        {
            // ignore
        }
    }

    void closePendingConnections(int ip)
    {
        Session S = WaitingSession;
        User U = S.first_user;
        while (U != null)
        {
            User next = U.next_in_session;
            if (U.ip == ip)
            {
                simpleCloseClientOnly(U, "one of too many");
                removeUserFromSession(U);
                putUserInSession(U, Idle_Users);
            }
            U = next;
        }
    }

    private void acceptNewConnections(ServerSocketChannel insoc, boolean websock)
    {
        String kind = websock ? "websocket " : "";
        // the C accepted one connection per pass; drain a few so a burst doesn't wait on select
        for (int n = 0; n < 16; n++)
        {
            SocketChannel newsocket;
            try
            {
                newsocket = insoc.accept();
            }
            catch (IOException e)
            {
                if (logging >= LOG_ERRORS)
                {
                    logEntry(mainLog, "[%s] Connection attempted, %saccept failed, %s.\n", timestamp(), kind, e.toString());
                }
                return;
            }
            if (newsocket == null) { return; }
            int ip = ipOf(newsocket);
            if (checkBannedIP(ip, websock))
            {
                quietClose(newsocket);
                continue;
            }
            if (checkTooMany(ip, websock))
            {
                banIP(ip);
                quietClose(newsocket);
                closePendingConnections(ip);
                continue;
            }
            User u = null;
            boolean ok = false;
            try
            {
                newsocket.configureBlocking(false);
                newsocket.setOption(StandardSocketOptions.TCP_NODELAY, false);
                ok = true;
            }
            catch (IOException e)
            {
                unusual_events++;
                if (logging >= LOG_ERRORS)
                {
                    logEntry(securityLog, "[%s] UNUSUAL: ioctl failed for new socket\n", timestamp());
                }
            }
            if (ok) { u = findAslot(WaitingSession); }
            if (u != null)
            {
                int sock = allocSocketId();
                Connection conn = new Connection(newsocket, websock, this, sock);
                // standard port: plain or TLS, decided by the first byte the client sends
                conn.tlsUndecided = !websock && stdPortTls;
                conn.local = ip == 0x7f000001;		// local connections, such as scripts on this machine, may be plain
                try
                {
                    conn.key = newsocket.register(selector, SelectionKey.OP_READ, u);
                }
                catch (IOException e)
                {
                    quietClose(newsocket);
                    removeUserFromSession(u);
                    putUserInSession(u, Idle_Users);
                    continue;
                }
                u.socket = sock;
                u.conn = conn;
                u.websocket = websock;
                u.ip = ip;
                u.clientTime = uptime();
                sockets_open++;
                if (maxsockets < sockets_open) { maxsockets = sockets_open; }
                if (sockets_open == 1)
                {
                    long time_now = uptime();
                    long idle = time_now - start_idle_time;
                    idle_time += idle;
                    if (logging >= LOG_CONNECTIONS && idle > 30)
                    {
                        logEntry(mainLog, "[%s] was idle %d minutes\n", timestamp(), (idle + 30) / 60);
                    }
                }
                if (logging >= LOG_CONNECTIONS)
                {
                    logEntry(mainLog, "[%s] Call C%d S%d from %d.%d.%d.%d queued to join a %ssession.\n",
                        timestamp(), u.userNUM, u.socket, (ip >>> 24) & 0xff, (ip >> 16) & 0xff, (ip >> 8) & 0xff, ip & 0xff,
                        kind);
                }
                continue;
            }
            if (logging >= LOG_CONNECTIONS)
            {
                logEntry(mainLog, "[%s] Call from %d.%d.%d.%d refused %s(no room or iofail)\n",
                    timestamp(), (ip >>> 24) & 0xff, (ip >> 16) & 0xff, (ip >> 8) & 0xff, ip & 0xff, kind);
            }
            quietClose(newsocket);
        }
    }

    /**
     * read what's available (C fillBuffer).  Returns true if a complete line
     * is buffered.  On errors the client is closed.
     */
    private boolean fillBuffer(User u)
    {
        Connection conn = u.conn;
        while (conn != null && isRealSocket(u.socket))
        {
            if (u.inbufCount >= MAX_INPUT_BUFFER_SIZE - 1 && !u.hasLine())
            {
                int put = u.inbufCount;
                doEchoSelf(u, C.lsprintf(0, ECHO_I_QUIT + "tooMuchData %d", put), true);
                doEchoAll(u, C.lsprintf(0, ECHO_PLAYER_QUIT + "%d tooMuchData %d", u.userNUM, put), true);
                u.wasZapped = true;
                if (logging >= LOG_ERRORS)
                {
                    logEntry(mainLog, "[%s] Zap C%d (%s#%d) S%d supplied too much data=%d bytes\n",
                        timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, put);
                }
                client_errors++;
                closeClient(u, "tooMuchData", grace_forbidden);
                return false;
            }
            int readResult;
            try
            {
                // like the C buffer, which always kept one byte for the NUL terminator
                int room = Math.min(readBuffer.length, MAX_INPUT_BUFFER_SIZE - 1 - u.inbufCount);
                byte[] dst = room == readBuffer.length ? readBuffer : new byte[Math.max(1, room)];
                readResult = conn.read(dst);
                if (conn.tlsDecided)
                {
                    conn.tlsDecided = false;
                    if (logging >= LOG_CONNECTIONS)
                    {
                        logEntry(mainLog, "[%s] C%d S%d using TLS on the standard port\n", timestamp(), u.userNUM, u.socket);
                    }
                }
                if (readResult > 0)
                {
                    u.badReadCount = 0;
                    u.goodReadCount++;
                    u.appendInput(dst, 0, readResult);
                }
            }
            catch (Connection.PlainRefused e)
            {
                if (logging >= LOG_CONNECTIONS)
                {
                    int ip = u.ip;
                    logEntry(mainLog, "[%s] C%d S%d from %d.%d.%d.%d refused, plain connection and sslrequired is set\n",
                        timestamp(), u.userNUM, u.socket, (ip >> 24) & 0xff, (ip >> 16) & 0xff, (ip >> 8) & 0xff, ip & 0xff);
                }
                simpleCloseClient(u, "plain connection refused");
                return false;
            }
            catch (Connection.WsFailure e)
            {
                logEntry(mainLog, "[%s] websocket failure: %s S%d\n", timestamp(), e.getMessage(), u.socket);
                if (e.ban) { banUserByIP(u); }
                simpleCloseClient(u, "error in websocket handshake");
                return false;
            }
            catch (Connection.OrderlyClose e)
            {
                u.expectEof = true;
                HandleReadError(u, -1);
                return false;
            }
            catch (EOFException e)
            {
                u.expectEof = true;
                HandleReadError(u, 0);
                return false;
            }
            catch (IOException e)
            {
                if (u.session != LOBBYSESSION() && u.session != ProxySession && u.session != WaitingSession)
                {
                    client_errors++;
                    u.wasZapped = true;
                    if (logging >= LOG_ERRORS)
                    {
                        logEntry(mainLog, "[%s] Zap C%d (%s#%d) S%d error reading, %s\n",
                            timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, e.toString());
                    }
                }
                HandleReadError(u, 104);
                return false;
            }
            if (readResult < 0)
            {
                // end of stream: the peer closed the connection
                u.expectEof = true;
                HandleReadError(u, 0);
                return false;
            }
            if (readResult == 0)
            {
                readBreak++;
                if (!conn.isHandshaking() && !conn.hasPending()) { u.badReadCount++; }
                break;
            }
            u.session.sessionTimes = u.clientTime = uptime();
            u.totalread += readResult;
            totalread += readResult;
            if (u.hasLine()) { break; }
        }
        if (isRealSocket(u.socket) && (u.badWriteCount >= 100 || u.badReadCount >= 100))
        {
            if (logging >= LOG_ERRORS)
            {
                logEntry(mainLog, "[%s] Emergency Close C%d (%s#%d) S%d error reading, bad reads=%d bad writes=%d good reads=%d good writes=%d\n",
                    timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket,
                    u.badReadCount, u.badWriteCount, u.goodReadCount, u.goodWriteCount);
            }
            simpleCloseClient(u, "emergency close");
            return false;
        }
        return isRealSocket(u.socket) && u.hasLine();
    }

    // =====================================================================
    // housekeeping
    // =====================================================================

    void clearOrphanedSessions()
    {
        boolean some = false;
        long tempTime = uptime();
        for (int sess = 1; sess <= maxSessions; sess++)
        {
            Session s = Sessions[sess];
            int clear = s.sessionClear;
            int sesst = clear != 0 ? SESSIONCLEARTIMEOUT : SESSIONTIMEOUT;
            if ((s.sessionKey != 0 || !s.sessionURLs.isEmpty() || clear != 0)
                && (tempTime - s.sessionTimes) > sesst
                && sessionIsEmpty(s))
            {
                if (logging >= LOG_CONNECTIONS)
                {
                    if (clear != 0) { logEntry(mainLog, "[%s] clearing abandoned session %d\n", timestamp(), sess); }
                    else { logEntry(mainLog, "[%s] clearing session password for abandoned session %d\n", timestamp(), sess); }
                }
                some = true;
                if (clear != 0) { clearSession(s); }
                else { clearEmptySession(s); }
            }
            if (Sessions[sess].sessionDescribe)
            {
                DescribeSession(sess);
                s.sessionDescribe = false;
            }
        }
        if (some) { saveConfig(); }
    }

    int sessionTimeout(Session s)
    {
        if (s == LOBBYSESSION()) { return lobby_timeout; }
        if (s == WaitingSession) { return MAXWAITINGTIME; }
        if (s == ProxySession) { return MAXPROXYTIME; }
        return client_timeout;
    }

    private int setSessionSocks(Session S, int nActive)
    {
        User u = S.first_user;
        int nfound = 0;
        int pop = S.population;
        while (u != null)
        {
            User next = u.next_in_session;
            check(u.session == S, "setSessionSocks: user in wrong session");
            if (isRealSocket(u.socket) && u.conn != null && u.conn.key != null && u.conn.key.isValid())
            {
                activeClients[nActive++] = u;
                int ops = SelectionKey.OP_READ;
                if (u.outputPending() > 0 || u.conn.wantsWrite()) { ops |= SelectionKey.OP_WRITE; }
                u.conn.key.interestOps(ops);
                if (u.conn.hasPending()) { inputPending = true; }
            }
            u = next;
            nfound++;
            check(nfound <= pop, "setSessionSocks: population");
        }
        check(nfound == pop, "setSessionSocks: population count");
        return nActive;
    }

    private boolean inputPending = false;

    // =====================================================================
    // the main loop (C main_thread)
    // =====================================================================

    private void main_thread()
    {
        logEntry(mainLog, "[%s] main process started\n", timestamp());
        for (User u : Users) { ClearUser(u); }

        // the logger thread: write the log buffers once a second (the C status thread)
        Thread logger = new Thread(() -> {
            loggerThreadRunning = true;
            while (!killThreads)
            {
                flushAllLogs();
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {
                    break;
                }
            }
        }, "logger");
        logger.setDaemon(true);
        logger.start();
        startSaveGameThread();

        start_idle_time = start_time = bktime = uptime();
        saveConfig();
        idle_time = 0;
        int round_robin_salt = 0;

        for (;;)
        {
            try
            {
                int nActive = 0;
                inputPending = false;
                HandleDeadUsers();
                for (int actCtr = 0; actCtr <= maxSessions; actCtr++)
                {
                    nActive = setSessionSocks(Sessions[actCtr], nActive);
                }
                nActive = setSessionSocks(WaitingSession, nActive);
                nActive = setSessionSocks(ProxySession, nActive);

                if (inputPending) { selector.selectNow(); }
                else { selector.select(MAXWAITTIME * 1000L); }
                Set<SelectionKey> ready = selector.selectedKeys();

                if (nActive > 0)
                {
                    round_robin_salt++;
                    if (round_robin_salt >= maxSessions + 1) { round_robin_salt = 0; }
                    for (int actCtr = round_robin_salt, idx = 0; idx < nActive; idx++, actCtr++)
                    {
                        if (actCtr >= nActive) { actCtr = 0; }
                        User u = activeClients[actCtr];
                        try
                        {
                            serviceUser(u, ready);
                        }
                        catch (RuntimeException e)
                        {
                            recoverFrom(u, e);
                        }
                    }
                }
                for (SelectionKey k : ready)
                {
                    if (!k.isValid() || !k.isAcceptable()) { continue; }
                    if (serversd != null && k.channel() == serversd) { acceptNewConnections(serversd, false); }
                    else if (webserversd != null && k.channel() == webserversd) { acceptNewConnections(webserversd, true); }
                }
                ready.clear();

                long now = uptime();
                if (now > bktime)		// once per second
                {
                    bktime = now;
                    clearOrphanedSessions();
                    if (gameCacheRate > 0) { saveDirtyGames(); }
                    if (configChange)
                    {
                        configChange = false;
                        realSaveConfig();
                    }
                }
            }
            catch (IOException e)
            {
                logEntry(mainLog, "[%s] select failed: %s\n", timestamp(), e.toString());
                error("Failed select", 0);
            }
            catch (RuntimeException e)
            {
                logEntry(mainLog, "[%s] unexpected error in main loop: %s\n", timestamp(), stackTrace(e));
            }
        }
    }

    private void serviceUser(User u, Set<SelectionKey> ready)
    {
        long tempTime = uptime();
        if (!isRealSocket(u.socket) || u.conn == null) { return; }
        Connection conn = u.conn;
        SelectionKey key = conn.key;
        boolean selected = key != null && key.isValid() && ready.contains(key);
        boolean readable = (selected && key.isReadable()) || conn.hasPending();
        boolean writable = selected && key.isValid() && key.isWritable();

        if (readable)
        {
            if (fillBuffer(u))
            {
                u.clientTime = tempTime;
                String line = u.takeLine();
                if (line != null && !u.inputClosed)
                {
                    u = ProcessMessages(line, u);
                }
                while (isRealSocket(u.socket) && (line = u.takeLine()) != null)
                {
                    if (!u.inputClosed) { u = ProcessMessages(line, u); }
                }
            }
        }
        else if (writable)
        {
            try
            {
                conn.onWritable();
            }
            catch (IOException e)
            {
                HandleWriteError(u, "Zap restart output");
                return;
            }
            if (u.outputPending() > 0 && restartOutput(u) > 0)
            {
                u.clientTime = tempTime;
            }
        }
        else
        {
            long dif = tempTime - u.clientTime;
            Session S = u.session;
            int tim = sessionTimeout(S);
            if (dif > tim)
            {
                if (S != WaitingSession)
                {
                    doEchoOthers(u, C.lsprintf(SMALLBUFSIZE, ECHO_PLAYER_QUIT + "%d timeout", u.userNUM), true);
                    doEchoSelf(u, ECHO_I_QUIT + "timeout", true);
                    errMsg(0, u.socket, C.lsprintf(0, "C%d (%s#%d) S%d timeout, (%d>%d)",
                        u.userNUM, u.clientRealName, u.clientUid, u.socket, dif, tim));
                    closeClient(u, "timeout", grace_mandatory);
                }
                else
                {
                    simpleCloseClientOnly(u, "waiting session timeout");
                    removeUserFromSession(u);
                    putUserInSession(u, Idle_Users);
                }
            }
        }
        closeIfMarked(u);
    }

    /** a user marked for closure is closed once its output has drained */
    private void closeIfMarked(User u)
    {
        if (isRealSocket(u.socket) && u.outputClosed && u.outputPending() == 0)
        {
            closeClient(u, "internal close request", grace_forbidden);
        }
    }

    /** a handler failed an internal check or threw: log it, and drop that client rather than the server */
    private void recoverFrom(User u, RuntimeException e)
    {
        unusual_events++;
        logEntry(mainLog, "[%s] UNUSUAL: error processing C%d S%d: %s\n", timestamp(), u.userNUM, u.socket, stackTrace(e));
        DumpHistory();
        try
        {
            if (isRealSocket(u.socket)) { closeClient(u, "internal error", grace_optional); }
        }
        catch (RuntimeException e2)
        {
            logEntry(mainLog, "[%s] UNUSUAL: error closing C%d after error: %s\n", timestamp(), u.userNUM, e2.toString());
        }
    }

    static String stackTrace(Throwable e)
    {
        StringWriter w = new StringWriter();
        e.printStackTrace(new PrintWriter(w));
        return w.toString();
    }

    // =====================================================================
    // startup
    // =====================================================================

    private void run(String[] argv) throws IOException
    {
        C.fmtBugHook = s -> logEntry(securityLog, "%s\n", s);
        mainLog.renameLogFile = true;
        chatLog.renameLogFile = true;
        securityLog.renameLogFile = true;
        securityLog.notflushable = true;
        WaitingSession.sessionNUM = -1;
        Idle_Users.sessionNUM = -2;
        ProxySession.sessionNUM = -3;
        if (argv.length != 1)
        {
            logEntry(mainLog, "<echoServer command> <configFile>\n");
            ExitWith(1);
        }
        for (int i = 0; i < MAXSESSIONS; i++)
        {
            Session s = new Session();
            s.sessionNUM = i;
            Sessions[i] = s;
        }
        for (int i = 0; i < Users.length; i++)
        {
            Users[i] = new User(FIRSTUSERNUM + i);
            putUserInSession(Users[i], Idle_Users);
        }
        for (int i = 0; i < MAXCLIENTS; i++)
        {
            RegisteredUser r = new RegisteredUser();
            r.next = freeRegisteredUsers;
            freeRegisteredUsers = r;
        }
        for (int i = 0; i < MAXBANNED; i++) { bannedUsers[i] = new BannedUser(); }

        configFile = argv[0];
        selector = Selector.open();

        mainLog.logFileName = GetParm("logfile");
        chatLog.logFileName = GetParm("chatfile");
        securityLog.logFileName = GetParm("securityfile");
        maxLogfileSize = (1024L * 1024L) * atoi(GetParm("max_logfile_megabytes"));
        gameCacheDir = GetParm("gamecachedir");
        gameCacheRate = atoi(GetParm("gamecacherate"));
        Object[] o = new Object[4];
        if (C.sscanf(GetParm("bindip"), "%d.%d.%d.%d", o) != 4)
        {
            error1("Couldn't find IP address in config file param \"bindip\"");
        }
        IP1 = (Integer) o[0];
        IP2 = (Integer) o[1];
        IP3 = (Integer) o[2];
        IP4 = (Integer) o[3];
        server_ip = ((IP1 & 0xff) << 24) | ((IP2 & 0xff) << 16) | ((IP3 & 0xff) << 8) | (IP4 & 0xff);
        alt_server_ip = 0x7f000001;
        portNum = atoi(GetParm("port"));
        logging = atoi(GetParm("loglevel"));
        supervisor = GetParm("password");
        lobby_timeout = atoi(GetParm("lobby-timeout"));
        client_timeout = atoi(GetParm("client-timeout"));

        // connections the kernel will hold for accept().  A small value drops real clients
        // during bursts; the kernel caps large values at net.core.somaxconn.
        listenBacklog = atoi(GetOptionalParm("listen-backlog", "128"));
        if (listenBacklog <= 0) { listenBacklog = 128; }
        logEntry(mainLog, "[%s] listen backlog %d\n", timestamp(), listenBacklog);
        client_socket_init(portNum);
        websocketPortNum = atoi(GetParm("websocket"));
        if (websocketPortNum > 0)
        {
            client_websocket_init(websocketPortNum);
        }
        String sslKey = GetOptionalParm("websocketsslkey", null);
        String sslCert = GetOptionalParm("websocketsslcert", null);
        websocketSslOnly = atoi(GetOptionalParm("websocketsslonly", "0")) != 0;
        // with a key and cert, the standard port accepts TLS as well as plain connections.
        // sslrequired makes TLS mandatory there.
        stdPortTls = sslKey != null && sslCert != null;
        sslRequired = atoi(GetOptionalParm("sslrequired", "0")) != 0;
        if (sslRequired && !stdPortTls)
        {
            error1("sslrequired needs websocketsslkey and websocketsslcert");
        }

        mainLog.open();
        chatLog.open();
        securityLog.open();
        logFilesEnabled = true;
        logEntry(mainLog, "[%s] started\n", timestamp());
        logEntry(mainLog, "[%s] port %d accepts %s connections\n", timestamp(), portNum,
            !stdPortTls ? "plain" : sslRequired ? "only TLS" : "plain and TLS");
        logEntry(mainLog, "Server Java version %s, %s %s\n", "1.0", System.getProperty("java.vm.name"), System.getProperty("java.version"));
        logEntry(mainLog, "Compiled limits: buffer %d, User %d, Session %d, GameBuffers %d\n",
            MAX_INPUT_BUFFER_SIZE, MAXCLIENTS, MAXSESSIONS, MAXGAMES);
        logEntry(mainLog, "[%s] logging is %d, password is \"%s\",software key is %x\n", timestamp(), logging, supervisor, security_key);
        logEntry(chatLog, "[%s] started\n", timestamp());
        logEntry(securityLog, "[%s] started\n", timestamp());

        if ((websocketPortNum > 0 || stdPortTls) && sslCert != null && !sslCert.isEmpty())
        {
            sslProvider = new SslProvider(this, sslCert, sslKey);
            sslProvider.load();
        }

        statusfile = GetParm("statusfile");
        playerInfoUrl = GetParm("player-info-url");
        ngamedirs = atoi(GetParm("ngamedirs"));
        gamedirs = new String[Math.max(0, ngamedirs)];
        for (int i = 0; i < ngamedirs; i++) { gamedirs[i] = GetParm(C.lsprintf(15, "gamedir%d", i)); }
        ngametypes = atoi(GetParm("ngametypes"));
        gametypes = new String[Math.max(0, ngametypes)];
        for (int i = 0; i < ngametypes; i++) { gametypes[i] = GetParm(C.lsprintf(15, "gametype%d", i)); }
        nroomtypes = atoi(GetParm("nroomtypes"));
        roomtypes = new String[Math.max(0, nroomtypes)];
        for (int i = 0; i < nroomtypes; i++) { roomtypes[i] = GetParm(C.lsprintf(15, "roomtype%d", i)); }

        maxClients = atoi(GetParm("players"));
        maxSessions = atoi(GetParm("rooms"));
        maxConnections = atoi(GetParm("connections"));
        maxConnectionsPerSession = atoi(GetParm("connections-per-session"));
        maxConnectionsPerUID = atoi(GetParm("connections-per-uid"));
        strict_login = atoi(GetParm("strict"));
        strict_score = atoi(GetParm("strict-score"));
        require_rng = atoi(GetParm("obfuscation"));
        require_seq = atoi(GetParm("sequencenumbers"));

        logEntry(mainLog, "Max of %d users and %d sessions and %d pending connections per IP\n%d connections per session per IP\n%d connections per UID per IP\n",
            maxClients, maxSessions, maxConnections, maxConnectionsPerSession, maxConnectionsPerUID);
        if (strict_login != 0) { logEntry(mainLog, "strict logins are being enforced\n"); }
        if (maxClients > MAXCLIENTS) { error("Exceeded maximum number of users; max value is ", MAXCLIENTS); }
        if (maxSessions > LASTREALSESSION) { error("Exceeded maximum number of sessions; max value is ", MAXSESSIONS - 2); }

        atoi(GetParm("use-threads"));		// always threaded here
        crash_test = atoi(GetParm("crash-test"));

        reloadGames();
        gameHashCleanup();
        if (atoi(GetParm("detatch")) != 0)
        {
            logEntry(mainLog, "[%s] detatch is ignored; run the server under systemd instead\n", timestamp());
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            killThreads = true;
            loggerThreadRunning = false;
            logEntry(mainLog, "[%s] stopping (signal)\n", timestamp());
            flushAllLogs();
        }, "shutdown"));

        main_thread();
    }

    public static void main(String[] argv)
    {
        Migs m = new Migs();
        try
        {
            m.run(argv);
        }
        catch (IOException e)
        {
            m.logEntry(m.mainLog, "[%s] startup failed: %s\n", timestamp(), e.toString());
            m.ExitWith(1);
        }
    }
}
