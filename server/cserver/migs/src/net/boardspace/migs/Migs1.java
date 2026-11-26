package net.boardspace.migs;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * The protocol: message handlers, supervisor commands and message dispatch.
 * This is the second half of migs.c, ported function by function.
 */
class Migs1 extends Migs0
{
    // =====================================================================
    // logging requests
    // =====================================================================

    String unescape(String s)
    {
        return C.unescapeString(s, ch -> {
            if (logging >= LOG_ERRORS)
            {
                logEntry(mainLog, "[%s] out of range hex digit %c\n", timestamp(), ch);
            }
        });
    }

    void process_logshortnote(String data, User u, String seq)
    {
        data = unescape(data);
        if (logging >= LOG_ERRORS)
        {
            logEntry(mainLog, "[%s] note from C%d (%s#%d) on S%d session %d\n",
                timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, u.session.sessionNUM);
            logEntry(mainLog, "\"%s\"\n", data);
        }
    }

    void process_logmessage(String data, User u, String seq)
    {
        data = unescape(data);
        client_messages++;
        if (logErrorFor(u))
        {
            if (logging >= LOG_ERRORS)
            {
                logEntry(mainLog, "[%s] logFile request #%d from C%d (%s#%d) on S%d session %d\n",
                    timestamp(), client_messages, u.userNUM, u.clientRealName, u.clientUid, u.socket,
                    u.session.sessionNUM);
                logEntry(mainLog, "\"%s\"\nend of C%d (%s#%d) S%d\n", data, u.userNUM, u.clientRealName, u.clientUid, u.socket);
                DumpHistory();
            }
        }
    }

    /** logGame {number} file body: write a completed game record.  The write itself happens on the file writer thread. */
    void logGame(String message, Session s, User u)
    {
        message = unescape(message);
        int idx = 0;
        int gamen = 0;
        while (idx < message.length() && message.charAt(idx) >= '0' && message.charAt(idx) <= '9')
        {
            gamen = gamen * 10 + message.charAt(idx) - '0';
            idx++;
        }
        while (idx < message.length() && message.charAt(idx) <= ' ' && message.charAt(idx) > 0) { idx++; }
        Tok t = scanstr(message.substring(idx), SMALLBUFSIZE);
        String buf = t.s;
        int sep = buf.lastIndexOf('/');
        String b = sep >= 0 ? buf.substring(sep + 1) : buf;
        if (t.n > 0 && gamen >= 0 && gamen < ngamedirs)
        {
            final String namebuf = C.lsprintf(SMALLBUFSIZE, "%s%s.sgf", gamedirs[gamen], b);
            int bodyStart = Math.min(message.length(), idx + buf.length());
            final String body = message.substring(bodyStart) + "\n";
            final boolean logIt = logging >= LOG_CONNECTIONS && logErrorFor(u);
            final boolean logFail = logging >= LOG_ERRORS;
            final String shortName = buf;
            fileWriter.execute(() -> {
                try (FileOutputStream wr = new FileOutputStream(namebuf))
                {
                    wr.write(C.bytes(body));
                    if (logIt) { logEntry(mainLog, "[%s] game %s saved\n", timestamp(), namebuf); }
                }
                catch (IOException e)
                {
                    if (logFail)
                    {
                        logEntry(mainLog, "[%s] failed (%s) to save game %s\n", timestamp(), e.toString(), shortName);
                        save_failed.incrementAndGet();
                    }
                }
            });
        }
        else
        {
            if (logErrorFor(u))
            {
                logEntry(mainLog, "[%s] save game failed, game number %d out of range\n", timestamp(), gamen);
                save_failed.incrementAndGet();
            }
        }
        games_completed++;
    }

    // =====================================================================
    // game records
    // =====================================================================

    void ZapDuplicateSession(GameBuffer g, User u)
    {
        Session owner = g.ownerSession;
        if (owner != null && owner != u.session)
        {
            if (logging >= LOG_CONNECTIONS)
            {
                logEntry(mainLog, "[%s] irregular re-record cached game %s already busy in session %d zap\n",
                    timestamp(), g.idString, owner.sessionNUM);
            }
            robot_irregular++;
            clearSessionPlayers(owner);
        }
        Session s = u.session;
        GameBuffer oldGame = s.sessionGame;
        if (oldGame != g)
        {
            if (oldGame != null)
            {
                if (logging >= LOG_CONNECTIONS)
                {
                    logEntry(mainLog, "[%s] session zap refcount %d down %s#%d\n", timestamp(), oldGame.refCount, oldGame.idString, oldGame.uid);
                }
                oldGame.ownerSession = null;
                s.sessionGame = null;
                if (oldGame.refCount-- == 1) { freeGameBuffer(oldGame); }
            }
            if (logging >= LOG_CONNECTIONS)
            {
                logEntry(mainLog, "[%s] session refcount %d up %s#%d\n", timestamp(), g.refCount, g.idString, g.uid);
            }
            g.refCount++;
            s.sessionGame = g;
            g.ownerSession = s;
        }
    }

    GameBuffer findNamedGame(User u, String id)
    {
        if ("*".equals(id))
        {
            Session s = u.session;
            if (s != null && s.sessionNUM != 0)
            {
                if (s.sessionGame == null)
                {
                    GameBuffer g = makeGameBuffer();
                    if (logging >= LOG_CONNECTIONS)
                    {
                        logEntry(mainLog, "[%s] find refcount %d up %s#%d\n", timestamp(), g.refCount, g.idString, g.uid);
                    }
                    g.refCount++;
                    s.sessionGame = g;
                    g.ownerSession = s;
                }
                return s.sessionGame;
            }
            return null;
        }
        String lower = C.toLowercase(id);
        return findGame(C.hashString(lower), lower);
    }

    GameBuffer recordGame(User u, String id, String msg)
    {
        GameBuffer g = findNamedGame(u, id);
        if (g == null)
        {
            g = makeGameBuffer();
            if (!"*".equalsIgnoreCase(id))
            {
                String lower = C.toLowercase(id);
                g.hashCode = C.hashString(lower);
                g.idString = C.trunc(lower, UIDSIZE);
                preserveGame(g);
            }
            else
            {
                g.hashCode = 0;
            }
        }
        ZapDuplicateSession(g, u);
        byte[] m = C.bytes(C.cstr(msg));
        int msglen = m.length + 1;
        if (msglen >= g.gamePtrSize) { setGameBufferSize(g, msglen, false); }
        check(g.gamePtrSize > msglen, "recordGame: buffer size");
        C.myStrncpy(g.gamePtr, 0, m, 0, msglen);
        g.gamePtrOffset = msglen;
        markAsDirty(g);
        return g;
    }

    int removeNamedGame(User u, String id)
    {
        GameBuffer g = findNamedGame(u, id);
        if (g != null)
        {
            int val = g.uid;
            logEntry(mainLog, "[%s] removegame '%s' found C%d (%s#%d) S%d\n", timestamp(), id, u.userNUM, u.clientRealName, u.clientUid, u.socket);
            unpreserveGame(g);
            return val;
        }
        logEntry(mainLog, "[%s] removegame '%s' not found C%d (%s#%d) S%d\n", timestamp(), id, u.userNUM, u.clientRealName, u.clientUid, u.socket);
        return 0;
    }

    GameBuffer reRecordGame(User u, String id, int offset, int checksum, String msg)
    {
        if (u.session.sessionHasGame != 0 && !u.isAPlayer)
        {
            if (logErrorFor(u))
            {
                unusual_events++;
                logEntry(mainLog, "[%s] Unusual: Spectator C%d (%s#%d) S%d  session %d tried to re record Game %s\n",
                    timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, u.session.sessionNUM, id);
            }
            return null;
        }
        if (offset == 0) { return recordGame(u, id, msg); }

        GameBuffer g = findNamedGame(u, id);
        if (g == null)
        {
            if (logErrorFor(u))
            {
                unusual_events++;
                logEntry(mainLog, "[%s] Unusual: C%d (%s#%d) S%d  session %d Re Record %s is missing off %d check %d",
                    timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, u.session.sessionNUM, id, offset, checksum);
            }
            return null;
        }
        ZapDuplicateSession(g, u);
        if (offset > g.gamePtrOffset)
        {
            unusual_events++;
            logEntry(mainLog, "[%s] Unusual: C%d (%s#%d) S%d  session %d  Game %s offset %d is greater than stored offset %d\ncontents: %s\nadd: %s\n",
                timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, u.session.sessionNUM,
                id, offset, g.gamePtrOffset, C.cstr(g.gamePtr, 0), msg);
        }
        else if (offset != -1)
        {
            int hashval = C.hashNString(g.gamePtr, offset);
            if (hashval != checksum)
            {
                if (logErrorFor(u))
                {
                    unusual_events++;
                    logEntry(mainLog, "[%s] Unusual: C%d (%s#%d) S%d  session %d  Game %s appended checksum incorrect; off %d check %d is %d\ncontents: %s\nadd: %s\n",
                        timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, u.session.sessionNUM,
                        id, offset, checksum, hashval, C.cstr(g.gamePtr, 0), msg);
                }
                sendSingle(C.lsprintf(SMALLBUFSIZE, ECHO_APPEND_GAME + " 1 "), u);
                return null;
            }
        }
        byte[] m = C.bytes(C.cstr(msg));
        int newpart = m.length + 1;
        int whole = newpart + offset;
        if (whole >= g.gamePtrSize) { setGameBufferSize(g, whole, true); }
        check(whole < g.gamePtrSize, "reRecordGame: buffer size");
        int off = offset == -1 ? 0 : offset;		// (sic) -1 is documented as "append" but writes at 0, like the C
        C.myStrncpy(g.gamePtr, off, m, 0, newpart);
        g.gamePtrOffset = offset + newpart - 1;
        markAsDirty(g);
        return g;
    }

    // =====================================================================
    // rooms
    // =====================================================================

    void process_reserve_room(String data, User u, String seq)
    {
        boolean sent = false;
        int[] si = safe_scanint(data);
        int idx = si[0];
        int sessionNum = si[1];
        Tok t = (idx >= 0) ? scanstr(data.substring(idx), SMALLBUFSIZE) : new Tok(-1, "");
        int idx2 = t.n;
        String reserve_string = t.s;
        if (idx2 > 0)
        {
            if (sessionNum >= 0 && sessionNum <= maxSessions)
            {
                boolean nopw = reserve_string.equals("<no_password>");
                Session S = Sessions[sessionNum];
                boolean isEmpty = sessionIsEmpty(S);
                if (isEmpty ? (S.sessionKey == 0)
                    : (u.session == S && (S.sessionHasGame == 0 || u.isAPlayer)))
                {
                    if (nopw)
                    {
                        S.sessionURLs = "";
                        S.sessionDescribe = true;
                        if (S.sessionIsPrivate != 0)
                        {
                            S.sessionIsPrivate = 0;
                            if (logging >= LOG_CONNECTIONS)
                            {
                                logEntry(mainLog, "[%s] Session %d made public by C%d (%s#%d) S%d\n",
                                    timestamp(), sessionNum, u.userNUM, u.clientRealName, u.clientUid, u.socket);
                            }
                        }
                        else
                        {
                            games_launched++;
                            if (logging >= LOG_CONNECTIONS)
                            {
                                logEntry(mainLog, "[%s] Game Launched on session %d by C%d (%s#%d) S%d\n",
                                    timestamp(), sessionNum, u.userNUM, u.clientRealName, u.clientUid, u.socket);
                            }
                        }
                    }
                    else
                    {
                        S.sessionURLs = C.trunc(reserve_string, SMALLBUFSIZE);
                        S.sessionDescribe = true;
                        if (isEmpty)
                        {
                            if (isClosed || u.gagged)
                            {
                                sendSingle(C.lsprintf(0, ECHO_FAILED + SEND_RESERVE_ROOM + "%s %s", data, "no new games allowed"), u);
                                sent = true;
                            }
                            else
                            {
                                int r1 = rand.nextInt(0x7fffffff) & 0xff;
                                int r2 = rand.nextInt(0x7fffffff) & 0xff;
                                int r3 = rand.nextInt(0x7fffffff) & 0xff;
                                int r4 = rand.nextInt(0x7fffffff) & 0xff;
                                S.sessionKey = r1 | (r2 << 8) | (r3 << 16) | (r4 << 24);
                                S.sessionIsPrivate = 0;
                                if (logging >= LOG_CONNECTIONS)
                                {
                                    logEntry(mainLog, "[%s] Session %d reserved by C%d (%s#%d) S%d using key %d.%d.%d.%d (%s)\n",
                                        timestamp(), sessionNum, u.userNUM, u.clientRealName, u.clientUid, u.socket, r1, r2, r3, r4, data);
                                }
                            }
                        }
                        else
                        {
                            if (logging >= LOG_CONNECTIONS)
                            {
                                logEntry(mainLog, "[%s] Session %d made private by C%d (%s#%d) S%d\n",
                                    timestamp(), sessionNum, u.userNUM, u.clientRealName, u.clientUid, u.socket);
                            }
                            S.sessionIsPrivate = 1;
                        }
                    }
                    saveConfig();
                    if (!sent)
                    {
                        S.sessionTimes = uptime();
                        String msg = C.lsprintf(0, ECHO_RESERVE_ROOM + "%s", data);
                        if (isEmpty) { sendSingle(msg, u); }
                        else { sendMulti(msg, S); }
                        sent = true;
                    }
                }
            }
        }
        if (!sent)
        {
            if (logging >= LOG_CONNECTIONS)
            {
                logEntry(mainLog, "[%s] Session %d reserve failed by C%d (%s#%d) S%d\n",
                    timestamp(), sessionNum, u.userNUM, u.clientRealName, u.clientUid, u.socket);
            }
            sendSingle(C.lsprintf(0, ECHO_FAILED + SEND_RESERVE_ROOM + "%s", data), u);
        }
    }

    void process_set_state(String data, User u, String seq)
    {
        boolean sent = false;
        Object[] o = new Object[3];
        int nargs = C.sscanf(data, "%d %d %d", o);
        int sessionNum = o[0] == null ? 0 : (Integer) o[0];
        int sessionState = o[1] == null ? 0 : (Integer) o[1];
        int sessionGame = o[2] == null ? 0 : (Integer) o[2];
        if (nargs < 3) { sessionGame = 0; }
        if (nargs >= 2)
        {
            if (sessionNum >= 0 && sessionNum <= maxSessions)
            {
                Session S = Sessions[sessionNum];
                if (sessionIsEmpty(S) && S.sessionKey == 0
                    && (S.sessionStates != sessionState || S.sessionGameID != sessionGame))
                {
                    S.sessionStates = sessionState;
                    S.sessionGameID = sessionGame;
                    S.sessionDescribe = true;
                    clearSessionGame(S);
                    if (logging >= LOG_CONNECTIONS)
                    {
                        logEntry(mainLog, "[%s] Session %d Set to state %d:%d by C%d (%s#%d) S%d\n",
                            timestamp(), sessionNum, sessionState, sessionGame, u.userNUM, u.clientRealName, u.clientUid, u.socket);
                    }
                    sendMulti(C.lsprintf(0, ECHO_SET_STATE + "%s", data), S);
                    sent = true;
                }
            }
        }
        if (!sent)
        {
            sendSingle(C.lsprintf(0, ECHO_FAILED + SEND_SET_STATE + "%s", data), u);
        }
    }

    // =====================================================================
    // supervisor
    // =====================================================================

    void doShutdown(User u, String buf)
    {
        logEntry(mainLog, "[%s] shutdown request by supervisor C%d (%s#%d) S%d : %s\n",
            timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, buf);
        closeListeners();
        for (int i = 0; i <= maxSessions; i++)
        {
            Session s = Sessions[i];
            User uu = s.first_user;
            int pop = s.population;
            int nfound = 0;
            while (uu != null)
            {
                User next = uu.next_in_session;
                simpleCloseClient(uu, "shutdown");
                uu = next;
                nfound++;
                check(nfound <= pop, "doShutdown: population");
            }
        }
        isshutdown = 1;
        realSaveConfig();
        fileWriter.shutdown();
        try
        {
            fileWriter.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            // exiting anyway
        }
        ExitWith(0);
    }

    /** overridden by the I/O layer */
    void closeListeners() { }

    boolean userNameMatches(User U, String uname)
    {
        // (sic) the C tested "(uname,"guest",5)==0", which is always false, so the
        // real name is always used
        return uname.equalsIgnoreCase(U.clientRealName);
    }

    void doSupervisor(User SU, String buf)
    {
        if (buf.startsWith("cached "))
        {
            Tok t = scanstr(buf.substring(7), 64);
            String matching = t.s;
            boolean all = matching.equals("*");
            if (t.n > 0)
            {
                GameBuffer g = all_gamebuffers;
                while (g != null)
                {
                    if (g.preserved && (all || g.idString.contains(matching)))
                    {
                        sendSingle(C.lsprintf(SMALLBUFSIZE, ECHO_GROUP_OTHERS + "%d schat %d: %s", SU.userNUM, g.uid, g.idString), SU);
                    }
                    g = g.next;
                }
            }
        }
        else if (buf.regionMatches(true, 0, "uncache ", 0, 8))
        {
            Object[] o = new Object[1];
            int n = C.sscanf(buf.substring(8), "%d", o);
            int nth = n == 1 ? (Integer) o[0] : 0;
            if (n == 1 && nth >= 0)
            {
                GameBuffer g = all_gamebuffers;
                while (g != null)
                {
                    if (g.uid == nth)
                    {
                        if (g.preserved)
                        {
                            sendSingle(C.lsprintf(SMALLBUFSIZE, ECHO_GROUP_OTHERS + "%d schat %d: %s removed", SU.userNUM, nth, g.idString), SU);
                            unpreserveGame(g);
                        }
                        g = null;
                    }
                    else
                    {
                        g = g.next;
                    }
                }
            }
        }
        else if (buf.startsWith("logF"))
        {
            String obuf;
            Object[] o = new Object[1];
            String rest = buf.length() > 8 ? buf.substring(8) : "";
            if (C.sscanf(rest, "%d", o) == 1)
            {
                int ll = (Integer) o[0];
                if (ll == -1)
                {
                    obuf = C.lsprintf(SMALLBUFSIZE, ECHO_GROUP_OTHERS + "%d schat reopening log file", SU.userNUM);
                    mainLog.renameLogFile = true;
                    chatLog.renameLogFile = true;
                    securityLog.renameLogFile = true;
                }
                else
                {
                    logging = ll;
                    obuf = C.lsprintf(SMALLBUFSIZE, ECHO_GROUP_OTHERS + "%d schat logFile level set to %d", SU.userNUM, logging);
                }
            }
            else
            {
                obuf = C.lsprintf(SMALLBUFSIZE, ECHO_GROUP_OTHERS + "%d schat logFile level is %d", SU.userNUM, logging);
            }
            sendSingle(obuf, SU);
        }
        else if (buf.startsWith("score"))
        {
            Object[] o = new Object[1];
            String obuf;
            String rest = buf.length() > 6 ? buf.substring(6) : "";
            if (C.sscanf(rest, "%d", o) == 1)
            {
                strict_score = (Integer) o[0];
                obuf = C.lsprintf(SMALLBUFSIZE, ECHO_GROUP_OTHERS + "%d  schat strict-score set to %d", SU.userNUM, strict_score);
            }
            else
            {
                obuf = C.lsprintf(SMALLBUFSIZE, ECHO_GROUP_OTHERS + "%d  schat strict-score is %d", SU.userNUM, strict_score);
            }
            sendSingle(obuf, SU);
        }
        else if (buf.startsWith("strict"))
        {
            Object[] o = new Object[1];
            String obuf;
            String rest = buf.length() > 7 ? buf.substring(7) : "";
            if (C.sscanf(rest, "%d", o) == 1)
            {
                strict_login = (Integer) o[0];
                obuf = C.lsprintf(SMALLBUFSIZE, ECHO_GROUP_OTHERS + "%d  schat strict set to %d", SU.userNUM, strict_login);
            }
            else
            {
                obuf = C.lsprintf(SMALLBUFSIZE, ECHO_GROUP_OTHERS + "%d  schat strict is %d", SU.userNUM, strict_login);
            }
            sendSingle(obuf, SU);
        }
        else if (buf.startsWith("shutdown"))
        {
            sendSingle(C.lsprintf(SMALLBUFSIZE, ECHO_GROUP_OTHERS + "%d schat shutting down", SU.userNUM), SU);
            doShutdown(SU, buf.length() > 9 ? buf.substring(9) : "");
        }
        else if (crash_test != 0 && buf.startsWith("crash"))
        {
            logEntry(mainLog, "[%s] crash test requested by supervisor C%d\n", timestamp(), SU.userNUM);
            flushAllLogs();
            Runtime.getRuntime().halt(134);
        }
        else if (buf.startsWith("close"))
        {
            sendSingle(C.lsprintf(SMALLBUFSIZE, ECHO_GROUP_OTHERS + "%d schat closing to new connections", SU.userNUM), SU);
            isClosed = true;
        }
        else if (buf.startsWith("open"))
        {
            String obuf = C.lsprintf(SMALLBUFSIZE, ECHO_GROUP_OTHERS + "%d schat opening to new connections", SU.userNUM);
            isClosed = false;
            sendSingle(obuf, SU);
        }
        else if (buf.startsWith("kill ") || buf.startsWith("zap "))
        {
            boolean kill = buf.startsWith("kill ");
            Tok t1 = scanstr(buf, 10);
            Tok t2 = (t1.n > 0) ? scanstr(buf.substring(t1.n), SMALLBUFSIZE) : new Tok(-1, "");
            if (t2.n > 0)
            {
                String uname = t2.s;
                for (int i = 0; i <= maxSessions; i++)
                {
                    Session s = Sessions[i];
                    User u = s.first_user;
                    int pop = s.population;
                    int nfound = 0;
                    while (u != null)
                    {
                        User next = u.next_in_session;
                        check(u.session == s, "kill: user in wrong session");
                        if (userNameMatches(u, uname))
                        {
                            String name = u.clientRealName;
                            String obuf = C.lsprintf(SMALLBUFSIZE, ECHO_PLAYER_QUIT + "%d killed by supervisor", u.userNUM);
                            banUser(u);
                            doEchoAll(u, obuf, true);
                            if (kill && isRealSocket(u.socket))
                            {
                                HandleKill(u);
                            }
                            obuf = C.lsprintf(0, ECHO_GROUP_OTHERS + "%d schat User %d %s zapped", SU.userNUM, u.userNUM, name);
                            logEntry(mainLog, "[%s] C%d (%s) S%d killed C%d (%s) S%d\n",
                                timestamp(), SU.userNUM, SU.clientRealName, SU.socket, u.userNUM, name, u.socket);
                            sendSingle(obuf, SU);
                        }
                        u = next;
                        nfound++;
                        // the echoes above can close users and clear the session
                        if (u != null && u.session != s) { break; }
                        check(nfound <= pop, "kill: population");
                    }
                }
            }
        }
        else if (buf.startsWith("exempt"))
        {
            String rest = buf.length() > 7 ? buf.substring(7) : "";
            Tok t = scanstr(rest);
            if (t.n > 0)
            {
                Exempt_User = t.s;
                logEntry(mainLog, "[%s] C%d (%s) S%d future user %s exempted from strict login\n",
                    timestamp(), SU.userNUM, SU.clientRealName, SU.socket, Exempt_User);
                sendSingle(C.lsprintf(SMALLBUFSIZE, ECHO_GROUP_OTHERS + "%d schat future user %s exempted from strict login",
                    SU.userNUM, Exempt_User), SU);
            }
        }
        else if (buf.startsWith("ungag ") || buf.startsWith("gag "))
        {
            boolean isgag = buf.startsWith("gag ");
            Tok t = scanstr(buf.substring(isgag ? 4 : 6), SMALLBUFSIZE);
            if (t.n > 0)
            {
                String uname = t.s;
                for (int i = 0; i < maxSessions; i++)
                {
                    Session S = Sessions[i];
                    User U = S.first_user;
                    int pop = S.population;
                    int nfound = 0;
                    while (U != null)
                    {
                        User next = U.next_in_session;
                        check(U.session == S, "gag: user in wrong session");
                        if (userNameMatches(U, uname))
                        {
                            U.expectEof = true;
                            // (sic) the C passes isgag and userNUM in swapped order
                            doEchoOthers(U, C.lsprintf(SMALLBUFSIZE, ECHO_PLAYER_QUIT + "%d gagged=%d by supervisor", isgag ? 1 : 0, U.userNUM), true);
                            U.gagged = isgag;
                            if (isgag) { banUser(U); }
                            else { unBanUser(U); }
                            sendSingle(C.lsprintf(SMALLBUFSIZE, ECHO_GROUP_OTHERS + "%d schat User %d %s gagged=%d",
                                SU.userNUM, U.userNUM, U.clientRealName, isgag ? 1 : 0), SU);
                            logEntry(mainLog, "[%s] C%d (%s) S%d gagged=%d C%d (%s) S%d\n",
                                timestamp(), U.userNUM, SU.clientRealName, SU.socket, isgag ? 1 : 0,
                                U.userNUM, U.clientRealName, U.socket);
                        }
                        U = next;
                        nfound++;
                        check(nfound <= pop, "gag: population");
                    }
                }
            }
        }
        else if (buf.startsWith("unban"))
        {
            Object[] o = new Object[1];
            String rest = buf.length() > 6 ? buf.substring(6) : "";
            int na = C.sscanf(rest, "%d", o);
            if (na == 1)
            {
                int eventid = (Integer) o[0];
                BannedUser B = unBanEvent(eventid);
                String obuf = (B != null)
                    ? C.lsprintf(SMALLBUFSIZE, ECHO_GROUP_OTHERS + "%d schat Removed %s", SU.userNUM, banInfoFor(B, bc_blank))
                    : C.lsprintf(SMALLBUFSIZE, ECHO_GROUP_OTHERS + "%d schat Event %d not found", SU.userNUM, eventid);
                sendSingle(obuf, SU);
            }
            describeBans(SU);
        }
        else if (buf.startsWith("ban"))
        {
            Object[] o = new Object[4];
            String rest = buf.length() > 4 ? buf.substring(4) : "";
            int nban = C.sscanf(rest, "%d.%d.%d.%d", o);
            if (nban == 4)
            {
                int ip1 = (Integer) o[0], ip2 = (Integer) o[1], ip3 = (Integer) o[2], ip4 = (Integer) o[3];
                int bannedip = (ip1 & 0xff) << 24 | (ip2 & 0xff) << 16 | (ip3 & 0xff) << 8 | (ip4 & 0xff);
                isBanned("", 0, 'Z', "", bannedip);
                logEntry(mainLog, "[%s] unusual banned by admin IP %d.%d.%d.%d\n", timestamp(), ip1, ip2, ip3, ip4);
            }
            else if (nban == 1)
            {
                int ip1 = (Integer) o[0];
                isBanned("", ip1, 'Z', "", 0);
                logEntry(mainLog, "[%s] unusual banned by UID %d\n", timestamp(), ip1);
            }
            else
            {
                Tok t = scanstr(rest, 64);
                if (t.n > 2)
                {
                    isBanned(t.s, 0, 'Z', "", 0);
                    logEntry(mainLog, "[%s] unusual banned by name %s\n", timestamp(), t.s);
                }
            }
        }
        else if (buf.startsWith("help"))
        {
            sendSingle(C.lsprintf(SMALLBUFSIZE, ECHO_GROUP_OTHERS + "%d schat commands are ban {} cached {} uncache {} logFile {0-3} strict {0,1} exempt {user} score {0-2} gag {user} ungag {user} unban {evenid} kill {user} zap {user} close, open, and \"shutdown\"", SU.userNUM), SU);
        }
    }

    // =====================================================================
    // sessions described to the lobby
    // =====================================================================

    String DescribeSessionString(Session s)
    {
        int numActive = 0;
        int sessState = (s == null) ? MAXCLIENTS : s.sessionStates;
        int sessionGame = (s == null) ? 0 : s.sessionGameID;
        User u = s.first_user;
        int pop = s.population;
        int nfound = 0;
        while (u != null)
        {
            User next = u.next_in_session;
            check(u.session == s, "DescribeSessionString: user in wrong session");
            if (isRealSocket(u.socket) || u.isARobot) { numActive++; }
            u = next;
            nfound++;
            check(nfound <= pop, "DescribeSessionString: population");
        }
        check(nfound == pop, "DescribeSessionString: population count");
        int pass = s.sessionClear != 0 ? 2 : !s.sessionURLs.isEmpty() ? 1 : 0;
        if (pass == 1 && s.sessionIsPrivate != 0) { pass = 3; }
        return C.lsprintf(0, ECHO_SUMMARY + "%d %d %d %d %d %s",
            s.sessionNUM, numActive, sessState, pass, sessionGame, s.sessionInfo);
    }

    void DescribeSession(int sessn)
    {
        if (sessn >= 0 && sessn <= maxSessions)
        {
            Session L = LOBBYSESSION();
            User U = L.first_user;
            int pop = L.population;
            int nfound = 0;
            String temp = DescribeSessionString(Sessions[sessn]);
            while (U != null)
            {
                User next = U.next_in_session;
                check(U.session == L, "DescribeSession: user in wrong session");
                sendSingle(temp, U);
                U = next;
                nfound++;
                check(nfound <= pop, "DescribeSession: population");
            }
            check(nfound == pop, "DescribeSession: population count");
        }
    }

    // =====================================================================
    // takeover (220)
    // =====================================================================

    User process_takeover(String data, User u, String seq)
    {
        Tok t = scanstr(data, 64);
        if (t.n > 0)
        {
            String kb = t.s;
            boolean spectating = kb.equals(KEYWORD_SPECTATE);
            boolean playing = kb.equals(KEYWORD_PLAYING);
            if (!(playing || spectating))
            {
                u.expectEof = true;
                doEchoOthers(u, C.lsprintf(0, ECHO_PLAYER_QUIT + "%d %s", u.userNUM, data), true);
                doEchoSelf(u, C.lsprintf(0, ECHO_I_QUIT + "%s%s", seq, data), true);
                closeClient(u, "user request", grace_forbidden);
            }
            else if (playing)
            {
                User to = null;
                Session S = u.session;
                int nfound = 0;
                int pop = S.population;
                int[] si = safe_scanint(data.substring(t.n));
                int toseat = si[0] > 0 ? si[1] : -1;
                // (sic) the C tested the scan result for truth, which is true for -1 too
                if (si[0] != 0 && toseat >= 0) { to = S.first_user; }
                while (to != null)
                {
                    check(to.session == S, "takeover: user in wrong session");
                    if (to.isAPlayer && to.clientSeat == toseat) { break; }
                    to = to.next_in_session;
                    nfound++;
                    check(nfound <= pop, "takeover: population");
                }
                if (to == u)
                {
                    // nothing to do
                }
                else if (to != null)
                {
                    if (isRealSocket(to.socket))
                    {
                        if (logging >= LOG_CONNECTIONS)
                        {
                            logEntry(mainLog, "[%s] client C%d (%s#%d) S%d  session %d closed before reconnect C%d\n",
                                timestamp(), to.userNUM, to.clientRealName, to.clientUid, to.socket, to.session.sessionNUM, to.userNUM);
                        }
                        closeClient(to, "reconnect", grace_forbidden);
                    }
                    doEchoOthers(u, C.lsprintf(0, ECHO_PLAYER_QUIT + "%d %s", u.userNUM, data), true);
                    doEchoSelf(u, C.lsprintf(0, ECHO_I_QUIT + "%s%s", seq, data), true);
                    // if that send failed, u has been closed; there is no connection left to hand over
                    if (!isRealSocket(u.socket)) { return u; }
                    if (logging >= LOG_CONNECTIONS)
                    {
                        logEntry(mainLog, "[%s] spectator C%d (%s#%d) S%d in session %d is now C%d\n",
                            timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, u.session.sessionNUM, to.userNUM);
                    }
                    u.clientSeat = toseat;
                    u.clientOrder = to.clientOrder;
                    u.clientRev = to.clientRev;
                    u.isAPlayer = true;
                    CopyUser(u, to);
                    removeUserFromSession(u);
                    putUserInSession(u, Idle_Users);
                    u = to;
                }
                else
                {
                    String stamp = timestamp();
                    logEntry(securityLog, "[%s] UNUSUAL: spectator C%d (%s#%d) S%d in session %d  failed to take over as %d\n",
                        stamp, u.userNUM, u.clientRealName, u.clientUid, u.socket, u.session.sessionNUM, toseat);
                    sendSingle(C.lsprintf(0, ECHO_FAILED + SEND_TAKEOVER + "%s", data), u);
                    unusual_events++;
                    if (logging >= LOG_CONNECTIONS)
                    {
                        logEntry(mainLog, "[%s] UNUSUAL: spectator C%d (%s#%d) S%d in session %d  failed to take over as %d\n",
                            stamp, u.userNUM, u.clientRealName, u.clientUid, u.socket, u.session.sessionNUM, toseat);
                    }
                }
            }
            else
            {
                doEchoOthers(u, C.lsprintf(0, ECHO_PLAYER_QUIT + "%d %s", u.userNUM, data), true);
                doEchoSelf(u, C.lsprintf(0, ECHO_I_QUIT + "%s%s", seq, data), true);
                // if that send failed, u has been closed and may be the next free slot itself
                if (!isRealSocket(u.socket)) { return u; }
                User newu = findAslot(u.session);
                if (newu != null)
                {
                    if (logging >= LOG_CONNECTIONS)
                    {
                        logEntry(mainLog, "[%s] C%d (%s#%d) S%d in session %d is now spectator C%d\n",
                            timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, u.session.sessionNUM, newu.userNUM);
                    }
                    newu.clientPublicName = u.clientPublicName;
                    newu.clientRealName = u.clientRealName;
                    newu.clientUid = u.clientUid;
                    u.clientPublicName = "(vacancy)";
                    newu.socket = u.socket;
                    newu.conn = u.conn;
                    newu.ip = u.ip;		// (the C left ip 0 here; keep the per-ip limits meaningful)
                    newu.outbuf = u.outbuf;
                    newu.outTake = u.outTake;
                    newu.outPut = u.outPut;
                    newu.inbuf = u.inbuf;
                    newu.inbufCount = u.inbufCount;
                    u.clearBuffers();
                    u.socket = -1;
                    u.conn = null;
                    u.checksums = false;
                }
            }
        }
        return u;
    }

    // =====================================================================
    // fetching games
    // =====================================================================

    void process_fetch_game_f(String data, User u, String seq, boolean filter)
    {
        Tok t = scanstr(data, SMALLBUFSIZE);
        GameBuffer g = null;
        String out;
        if (t.n > 0 && (g = findNamedGame(u, t.s)) != null)
        {
            ZapDuplicateSession(g, u);
            if (logging >= LOG_CONNECTIONS)
            {
                logEntry(mainLog, "[%s] resume cached game %s#%d\n", timestamp(), g.idString, g.uid);
            }
            Session S = u.session;
            int players = 0;
            User au = S.first_user;
            while (au != null)
            {
                if (au.isAPlayer && !au.isARobot) { players++; }
                au = au.next_in_session;
            }
            if (players == 1) { robot_games_relaunched++; }
            else if (u == S.first_user) { player_games_relaunched++; }
            String tb = C.lsprintf(SMALLBUFSIZE, "%s%d ", filter ? ECHO_FETCH_GAME_FILTERED : ECHO_FETCH_GAME, g.uid);
            out = tb + C.escapeAndFilterString(Integer.MAX_VALUE, g.gamePtr, filter);
        }
        else
        {
            out = C.lsprintf(0, "%s 0", filter ? ECHO_FETCH_GAME_FILTERED : ECHO_FETCH_GAME);
        }
        sendSingle(out, u);
    }

    void process_fetch_active_game_f(String data, User u, String seq, boolean filter)
    {
        GameBuffer g = findNamedGame(u, "*");
        if (g != null)
        {
            StringBuilder tb = new StringBuilder(C.lsprintf(0, "%s%d ", filter ? ECHO_ACTIVE_GAME_FILTERED : ECHO_ACTIVE_GAME, g.uid));
            Session s = u.session;
            User su = s.first_user;
            while (su != null)
            {
                if (su.isAPlayer)
                {
                    String add = C.lsprintf(0, " %d %d %d %d %c %s",
                        su.userNUM, su.clientSeat, su.clientOrder, su.clientUid,
                        su.isARobot ? 'R' : (isRealSocket(su.socket) ? 'P' : 'Q'), encodeName(su.clientRealName));
                    tb.append(add);
                }
                su = su.next_in_session;
            }
            int gamecode = -1;
            if (s.sessionHasGame != 0) { gamecode ^= 1; }
            if (s.sessionScored != 0) { gamecode ^= 2; }
            if (s.sessionFileWritten != 0) { gamecode ^= 4; }
            tb.append(C.lsprintf(0, " %d ", gamecode));
            sendSingle(tb + C.escapeAndFilterString(Integer.MAX_VALUE, g.gamePtr, filter), u);
        }
        else
        {
            sendSingle(C.lsprintf(0, "%s 0", filter ? ECHO_ACTIVE_GAME_FILTERED : ECHO_ACTIVE_GAME), u);
        }
    }

    // =====================================================================
    // fraud detection state keys
    // =====================================================================

    void process_recordStateKey(String cmd, User U, String seq)
    {
        Session S = U.session;
        Tok t = scanstr(cmd);
        if (S.sessionStateKeyIndex >= 0) { sessionStateHash[S.sessionStateKeyIndex] = null; }
        S.sessionStateKey = C.trunc(t.s, SMALLBUFSIZE);
        int newhashcode = C.hashString(S.sessionStateKey);
        // (int) % in the C: a negative hash gives a negative index there; use the unsigned value
        int newhashindex = C.unsignedMod(newhashcode, sessionStateHash.length);
        Session leader = sessionStateHash[newhashindex];
        if (leader == null)
        {
            sessionStateHash[newhashindex] = S;
            S.sessionStateKeyIndex = newhashindex;
        }
        else if (S != leader && S.sessionStateKey.equals(leader.sessionStateKey))
        {
            String stamp = timestamp();
            if (logging >= LOG_ERRORS)
            {
                logEntry(mainLog, "[%s] UNUSUAL possible follower fraud state %s leader Session %d follower Session %d C%d (%s#%d)\n",
                    stamp, S.sessionStateKey, leader.sessionNUM, S.sessionNUM, U.userNUM, U.clientRealName, U.clientUid);
            }
            unusual_events++;
            logEntry(securityLog, "[%s] UNUSUAL possible follower fraud state %s leader Session %d follower Session %d C%d (%s#%d)\n",
                stamp, S.sessionStateKey, leader.sessionNUM, S.sessionNUM, U.userNUM, U.clientRealName, U.clientUid);
            sendSingle(ECHO_STATE_KEY + " follow", U);
        }
    }

    // =====================================================================
    // login (200)
    // =====================================================================

    void doSessionIntro(User to)
    {
        Session s = to.session;
        User u = s.first_user;
        while (u != null)
        {
            if (u != to)
            {
                sendSingle(C.lsprintf(0, ECHO_INTRO_OTHERS + "%d %d %d %d %s %d %d",
                    u.userNUM, u.isARobot ? 2 : u.isAPlayer ? 1 : 0, u.clientSeat, u.clientUid,
                    encodeName(u.clientRealName), u.clientOrder, u.clientRev), to);
            }
            u = u.next_in_session;
        }
    }

    boolean checkTooManyU(int userid, int ip)
    {
        int numconnections = 0;
        for (int i = 0; i <= maxSessions; i++)
        {
            User U = Sessions[i].first_user;
            while (U != null)
            {
                if (U.clientUid == userid && U.ip == ip) { numconnections++; }
                U = U.next_in_session;
            }
        }
        return numconnections >= maxConnectionsPerUID;
    }

    void process_send_intro(String data, User u, String seq)
    {
        boolean sent = false;
        String username;
        String password = "";
        String cookie = "";
        String bankey = "";
        String info = "";
        int usernum = 0;
        int ip1 = 0, ip2 = 0, ip3 = 0, ip4 = 0;

        int[] si = safe_scanint(data);
        int idx1 = si[0];
        int sessionNum = si[1];
        Tok t2 = idx1 > 0 ? scanstr(data.substring(idx1)) : new Tok(-1, "");
        int idx2 = t2.n;
        if (idx2 > 0)
        {   // the name part of name#uid
            int hash = t2.s.indexOf('#');
            if (nameTooLong(hash >= 0 ? t2.s.substring(0, hash) : t2.s)) { idx2 = -1; }
        }
        username = idx2 > 0 ? t2.s : "";
        int idx3 = 0;
        int ips = -1;
        if (idx2 > 0)
        {
            Object[] o = new Object[5];
            ips = C.sscanf(data.substring(idx1 + idx2), "%d.%d.%d.%d%n", o);
            if (o[0] != null) { ip1 = (Integer) o[0]; }
            if (o[1] != null) { ip2 = (Integer) o[1]; }
            if (o[2] != null) { ip3 = (Integer) o[2]; }
            if (o[3] != null) { ip4 = (Integer) o[3]; }
            if (ips == 4 && o[4] != null) { idx3 = (Integer) o[4]; }
        }
        int idx4 = -1, idx5 = -1, idx6 = -1;
        if (ips == 4 && idx3 > 0)
        {
            Tok t4 = scanstr(data.substring(idx1 + idx2 + idx3), SMALLBUFSIZE);
            idx4 = t4.n;
            password = t4.s;
        }
        if (idx4 > 0)
        {
            Tok t5 = scanstr(data.substring(idx1 + idx2 + idx3 + idx4), BANSIZE);
            idx5 = t5.n;
            cookie = t5.s;
        }
        if (idx5 > 0)
        {
            Tok t6 = scanstr(data.substring(idx1 + idx2 + idx3 + idx4 + idx5), SMALLBUFSIZE);
            idx6 = t6.n;
            bankey = t6.s;
        }
        int client_real_ip = u.ip;
        int b4 = (client_real_ip >> 0) & 0xff;
        int b3 = (client_real_ip >> 8) & 0xff;
        int b2 = (client_real_ip >> 16) & 0xff;
        int b1 = (client_real_ip >>> 24) & 0xff;

        // ---- STRICT
        {
            String client_ip_string = C.lsprintf(20, "%d.%d.%d.%d", b1, b2, b3, b4);
            int serverKey = (ip1 << 0) | (ip2 << 8) | (ip3 << 16) | (ip4 << 24);
            int ni = username.indexOf('#');
            if (ni >= 0)
            {
                Object[] o = new Object[1];
                if (C.sscanf(username.substring(ni + 1), "%d", o) != 1) { usernum = 0; }
                else { usernum = (Integer) o[0]; }
                username = username.substring(0, ni);
            }

            if (!Exempt_User.isEmpty()
                && (username.equalsIgnoreCase(Exempt_User) || Exempt_User.equalsIgnoreCase(client_ip_string)))
            {
                registerUser(serverKey, username, usernum, info);
            }
            else if (ips == 4 && sessionNum == -1)
            {
                if (client_real_ip == server_ip || client_real_ip == alt_server_ip)
                {
                    registerUser(serverKey, username, usernum, info);
                    closeClient(u, "user registered by server", grace_forbidden);
                    return;
                }
                logEntry(securityLog, "[%s] cmd=(%s) connect not from server ip, is %x not %x or %x \n",
                    timestamp(), data, client_real_ip, server_ip, alt_server_ip);
                closeClient(u, "not server ip", grace_forbidden);
                return;
            }

            u.supervisor = false;
            if (idx6 > 0)
            {
                int ban = isBanned(username, usernum, bankey.isEmpty() ? (char) 0 : bankey.charAt(0), cookie, client_real_ip);
                u.cookie = C.trunc(cookie, BANSIZE);
                switch (ban)
                {
                case bc_super:
                    u.supervisor = true;
                    if (logging >= LOG_ERRORS)
                    {
                        logEntry(mainLog, "[%s] %s cmd=(%s) connecting from %d.%d.%d.%d: cookie %s status %s\n",
                            timestamp(), banreason[ban], data, b1, b2, b3, b4, cookie, bankey);
                    }
                    break;
                case bc_unban:
                    if (logging >= LOG_ERRORS)
                    {
                        logEntry(mainLog, "[%s]  %s cmd=(%s) connecting from %d.%d.%d.%d: cookie %s status %s\n",
                            timestamp(), banreason[ban], data, b1, b2, b3, b4, cookie, bankey);
                    }
                    break;
                case bc_same_ip:
                case bc_same_name:
                case bc_same_id:
                    sendSingle(ECHO_I_QUIT + "bad-banner-id ", u);
                    if (logging >= LOG_ERRORS)
                    {
                        logEntry(mainLog, "[%s] %d %s cmd=(%s) attempted to connect from %d.%d.%d.%d: cookie %s status %s\n",
                            timestamp(), u.userNUM, banInfo(ban), data, b1, b2, b3, b4, cookie, bankey);
                    }
                    closeClient(u, "failed login (banned)", grace_forbidden);
                    return;
                default:
                    break;
                }
            }
            if (strict_login != 0)
            {
                boolean ok = false;
                boolean logged = false;
                if (ips == 4)
                {
                    RegisteredUser regid = isRegisteredUser(serverKey, client_real_ip, username, usernum);
                    if (regid == null)
                    {
                        String stamp = timestamp();
                        logEntry(securityLog, "[%s] UNUSUAL: unregistered user at %d.%d.%d.%d attempted to connect to session : %s\n",
                            stamp, b1, b2, b3, b4, data);
                        logged = true;
                        if (logging >= LOG_ERRORS)
                        {
                            logEntry(mainLog, "[%s] unregistered user at %d.%d.%d.%d attempted to connect to session : %s\n",
                                stamp, b1, b2, b3, b4, data);
                        }
                    }
                    else
                    {
                        u.serverKey = serverKey;
                        u.keyIndex = regid;
                        ok = true;
                    }
                }
                if (!ok)
                {
                    sendSingle(ECHO_I_QUIT + "bad-id ", u);
                    if (!logged)
                    {
                        unusual_events++;
                        logEntry(securityLog, "[%s] UNUSUAL: Malformed login from ip %d.%d.%d.%d: %s\n",
                            timestamp(), b1, b2, b3, b4, data);
                    }
                    closeClient(u, "failed login", grace_forbidden);
                    return;
                }
            }
        }

        if (checkTooManyU(usernum, client_real_ip))
        {
            sendSingle(ECHO_I_QUIT + "bad-banner-id ", u);
            unusual_events++;
            logEntry(securityLog, "[%s] UNUSUAL:< too many connections from %s(%d) ip %d.%d.%d.%d: %s\n",
                timestamp(), username, usernum, b1, b2, b3, b4, data);
            markForClosure(u);
        }

        if (idx1 > 0 && sessionNum >= 0 && sessionNum <= maxSessions)
        {
            Session s = Sessions[sessionNum];
            int passw = s.sessionURLs.isEmpty() ? 0 : 1;
            int key = s.sessionKey;
            int r1 = key & 0xff;
            int r2 = (key >> 8) & 0xff;
            int r3 = (key >> 16) & 0xff;
            int r4 = (key >>> 24) & 0xff;
            boolean passwordOk = idx4 > 0 && password.equals(s.sessionURLs);
            long realtime = System.currentTimeMillis() / 1000;
            int hitime = (int) (realtime / 1000000);
            int lowtime = (int) (realtime % 1000000);
            int nowtime = (int) (realtime & 0xffffffffL);
            boolean passwordSupplied = !password.equals("<none>");

            if (sessionNum > 0 && checkSession(client_real_ip, s, maxConnectionsPerSession) < 0)
            {
                unusual_events++;
                sendSingle(ECHO_I_QUIT + "bad-banner-id ", u);
                logEntry(securityLog, "[%s] UNUSUAL:< too many connections from %s(%d) ip %d.%d.%d.%d: %s\n",
                    timestamp(), username, usernum, b1, b2, b3, b4, data);
                markForClosure(u);
            }
            if (!s.poisoned && (passw != 0 ? passwordOk : !passwordSupplied))
            {
                removeUserFromSession(u);
                putUserInSession(u, s);
                s.sessionClear = 0;
                s.sessionHasGame |= passw;
                s.sessionDescribe = true;
                u.isAPlayer = passw != 0;
                u.isARobot = false;
                u.clientRealName = username;
                u.clientPublicName = username;
                u.clientUid = usernum;
                u.clientSeat = -1;
                u.clientOrder = -1;
                u.clientRev = -1;
                if (require_rng != 0) { nowtime |= 1; lowtime |= 1; }
                else { nowtime &= ~1; lowtime &= ~1; }
                // server level 20: this port also accepts TLS connections
                String intro = C.lsprintf(0, ECHO_INTRO_SELF + "%d %d %d %d.%d.%d.%d %d.%d.%d.%d %d%06d %d %d %d",
                        sessionNum, u.userNUM, stdPortTls ? SERVERID_SSL : SERVERID, r1, r2, r3, r4, b1, b2, b3, b4, hitime, lowtime,
                        MAX_INPUT_BUFFER_SIZE, s.population, passw);
                doEchoSelf(u, intro, false);

                if (isRealSocket(u.socket))
                {
                    if (require_rng != 0)
                    {
                        init_rng_in(u, C.lsprintf(SMALLBUFSIZE, "%d.%d.%d.%d.%d", r1 + 1, r2 + 2, r3 + 3, r4 + 4, nowtime + 2));
                        init_rng_out(u, C.lsprintf(SMALLBUFSIZE, "%d.%d.%d.%d.%d", r1 + 3, r2 + 6, r3 + 9, r4 + 12, nowtime + 1));
                    }
                    doEchoOthers(u, C.lsprintf(0, ECHO_INTRO_OTHERS + "%d %d %d %d %s %d %d",
                        u.userNUM, u.isARobot ? 2 : passw != 0 ? 1 : 0, u.clientSeat, u.clientUid,
                        encodeName(u.clientRealName), u.clientOrder, u.clientRev), false);
                    doSessionIntro(u);
                    if (sessionNum == 0) { clients_connected++; }
                    if (logging >= LOG_CONNECTIONS)
                    {
                        int k = u.serverKey;
                        logEntry(mainLog, "[%s] C%d (%s#%d) S%d from queue is now in session %d using server key %d.%d.%d.%d session key %d.%d.%d.%d. session id %s\n",
                            timestamp(), u.userNUM, username, u.clientUid, u.socket, sessionNum,
                            k & 0xff, (k >> 8) & 0xff, (k >> 16) & 0xff, (k >>> 24) & 0xff, r1, r2, r3, r4,
                            passw != 0 ? password : "<n/a>");
                    }
                    s.sessionTimes = u.clientTime = uptime();
                }
                saveConfig();
                sent = true;
            }
            else
            {
                if (logging >= LOG_ERRORS)
                {
                    logEntry(mainLog, "[%s] connection rejected poison=%d passw=%d ok %d sup %d",
                        timestamp(), s.poisoned ? 1 : 0, passw, passwordOk ? 1 : 0, passwordSupplied ? 1 : 0);
                }
            }
        }
        else
        {
            if (logging >= LOG_ERRORS)
            {
                logEntry(mainLog, "[%s] connection rejected bad session number %d", timestamp(), sessionNum);
            }
        }
        if (sessionNum == -1) { sent = true; }
        if (!sent)
        {
            sendSingle(C.lsprintf(0, ECHO_FAILED + SEND_INTRO + "%s", data), u);
        }
    }

    // =====================================================================
    // scoring check (218), from the scoring CGI scripts
    // =====================================================================

    void process_check_score(String data, User u, String seq)
    {
        if (strict_score == 2)
        {
            sendSingle(ECHO_CHECK_SCORE + "0", u);
            closeClient(u, "scoring disabled", grace_forbidden);
            return;
        }
        Object[] o = new Object[17];
        int nc = C.sscanf(data, "%d %d %d %d.%d.%d.%d %d %d %d %d %d %d %d %d %d %d", o);
        int[] v = new int[17];
        int[] dflt = { 0, -1, -1, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };
        for (int i = 0; i < 17; i++) { v[i] = o[i] == null ? dflt[i] : (Integer) o[i]; }
        int sess = v[0], uid1 = v[1], uid2 = v[2];
        int ip1 = v[3], ip2 = v[4], ip3 = v[5], ip4 = v[6];
        int[] uidx = { v[7], v[8], v[9], v[10], v[11], v[12], v[13], v[14], v[15], v[16] };	// uid3..uid12
        boolean sent = false;
        if ((u.ip == server_ip || u.ip == alt_server_ip) && nc >= 7 && sess > 0 && sess <= maxSessions)
        {
            int claimed_remote_ip = (ip1 << 0) | (ip2 << 8) | (ip3 << 16) | (ip4 << 24);
            Session S = Sessions[sess];
            if (S.sessionKey == claimed_remote_ip && S.sessionScored == 0)
            {
                boolean okon1 = strict_score == 0;
                boolean okon2 = okon1;
                boolean[] okx = new boolean[10];		// okon3..okon12
                for (int i = 0; i < 10; i++) { okx[i] = (nc < 8 + i) || okon1; }
                int nplayers = 0;
                User uu = S.first_user;
                int pop = S.population;
                int nfound = 0;
                while (uu != null)
                {
                    User next = uu.next_in_session;
                    check(uu.session == S, "check_score: user in wrong session");
                    if (uu.isAPlayer)
                    {
                        nplayers++;
                        if (!okon1 && uid1 == uu.clientUid) { okon1 = true; }
                        else if (uid2 > 0 && uid2 == uu.clientUid) { okon2 = true; }
                        else
                        {
                            for (int i = 0; i < 10; i++)
                            {
                                if (uidx[i] > 0 && uidx[i] == uu.clientUid) { okx[i] = true; break; }
                            }
                        }
                    }
                    uu = next;
                    nfound++;
                    check(nfound <= pop, "check_score: population");
                }
                if (nplayers == 1) { okon2 = true; }
                boolean allok = okon1 && okon2;
                for (boolean b : okx) { allok &= b; }
                if (allok)
                {
                    if (logging >= LOG_CONNECTIONS)
                    {
                        if (S.sessionClear != 0) { logEntry(mainLog, "[%s] scoring check: %s (saved by grace time)\n", timestamp(), data); }
                        else { logEntry(mainLog, "[%s] scoring check: %s\n", timestamp(), data); }
                    }
                    if (S.sessionClear != 0)
                    {
                        S.sessionClear = 0;
                        clearSession(S);
                    }
                    else
                    {
                        S.sessionScored++;
                        S.sessionDescribe = true;
                    }
                    sendSingle(ECHO_CHECK_SCORE + "1", u);
                    closeClient(u, "scoring check ok", grace_forbidden);
                    sent = true;
                }
                else if (logging >= LOG_ERRORS)
                {
                    logEntry(mainLog, "[%s] scoring check data iii, ok1=%d ok2=%d\n", timestamp(), okon1 ? 1 : 0, okon2 ? 1 : 0);
                }
            }
            else if (logging >= LOG_ERRORS)
            {
                logEntry(mainLog, "[%s] scoring check data ii, scored=%d key=%x serverkey=%x\n",
                    timestamp(), S.sessionScored, claimed_remote_ip, S.sessionKey);
            }
        }
        if (!sent)
        {
            unusual_events++;
            if (logging >= LOG_ERRORS)
            {
                logEntry(mainLog, "[%s] scoring check data, nc=%d sess=%d u->ip=%x server_ip=%x\n", timestamp(), nc, sess, u.ip, server_ip);
                logEntry(mainLog, "[%s] UNUSUAL: scoring check failed, %s\n", timestamp(), data);
            }
            sendSingle(ECHO_CHECK_SCORE + "0", u);
            closeClient(u, "scoring check, not ok", grace_forbidden);
        }
    }

    // =====================================================================
    // the rest of the opcodes
    // =====================================================================

    void process_ping(String message, User u, String seq)
    {
        Session S = u.session;
        long ms = System.currentTimeMillis();
        String reply = C.lsprintf(0, ECHO_PING + "%d %d %d %d", maxSessions + 1, MAXCLIENTS, (int) (ms / 1000), (int) (ms % 1000));
        if (message.length() >= 2 && message.charAt(0) == 'P' && message.charAt(1) == ':')
        {
            StringBuilder out = new StringBuilder();
            int nc = 0;
            int comma = -1;
            int some = -1;
            int p = 2;
            while (p < message.length() && message.charAt(p) != ' ')
            {
                char ch = message.charAt(p++);
                nc++;
                out.append(ch);
                if (ch == ',') { comma = p; }
                if (ch != 0) { some = p; }
            }
            if (comma >= 0 && nc > 2)
            {
                char key1 = message.charAt(some - 1);
                char key2 = message.charAt(comma - 2);
                if ((key1 & 1) != 0)
                {
                    u.rogueOutput++;
                    if (u.rogueOutput == 1)
                    {
                        unusual_events++;
                        if (logging >= LOG_ERRORS)
                        {
                            DumpHistory();
                            logEntry(mainLog, "[%s] unusual rogue output flagged from C%d (%s#%d) on S%d session %d\n",
                                timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, u.session.sessionNUM);
                        }
                    }
                }
                if ((key2 & 1) != 0)
                {
                    u.injectedOutput++;
                    if (u.injectedOutput == 1)
                    {
                        unusual_events++;
                        if (logging >= LOG_ERRORS)
                        {
                            DumpHistory();
                            logEntry(mainLog, "[%s] unusual rogue injected output flagged from C%d (%s#%d) on S%d session %d\n",
                                timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, u.session.sessionNUM);
                        }
                    }
                }
            }
            u.pingStats = out.toString();
            message = message.substring(p);
        }
        // the game's progress string, kept whole; the C server cuts it to SMALLBUFSIZE.
        // It's bounded by the input buffer like any other message.
        String info = message;
        if (!S.sessionInfo.equals(info))
        {
            S.sessionInfo = info;
            S.sessionDescribe = true;
        }
        sendSingle(reply, u);
    }

    void process_send_lobby_info(String data, User u, String seq)
    {
        u.lobbyInfo = C.trunc(data, 64);
    }

    void process_send_summary(String data, User u, String seq)
    {
        int err = 0;
        for (int currSess = 0; currSess <= maxSessions && err == 0; currSess++)
        {
            err = sendSingle(DescribeSessionString(Sessions[currSess]), u);
        }
    }

    void process_ask_detail(String data, User u, String seq)
    {
        boolean sent = false;
        Object[] o = new Object[1];
        if (C.sscanf(data, "%d", o) == 1)
        {
            int sessionNum = (Integer) o[0];
            if (sessionNum >= 0 && sessionNum <= maxSessions)
            {
                int totalC = 0;
                int err = 0;
                Session S = Sessions[sessionNum];
                User tu = S.first_user;
                int pop = S.population;
                int nfound = 0;
                while (tu != null && err == 0)
                {
                    User next = tu.next_in_session;
                    check(tu.session == S, "ask_detail: user in wrong session");
                    if (!tu.gagged && !tu.clientPublicName.isEmpty())
                    {
                        totalC++;
                        String msg;
                        if (sessionNum == 0)
                        {
                            msg = C.lsprintf(0, SEND_ECHO_DETAIL + "%d %d %d %s %d %s",
                                sessionNum, tu.userNUM, 0, encodeName(tu.clientPublicName), tu.clientUid, tu.lobbyInfo);
                        }
                        else
                        {
                            msg = C.lsprintf(0, SEND_ECHO_DETAIL + "%d %d %d %s %d",
                                sessionNum, tu.userNUM, tu.clientSeat >= 0 ? (100 + tu.clientSeat) : (tu.isAPlayer ? 1 : 0),
                                encodeName(tu.clientPublicName), tu.clientUid);
                        }
                        err = sendSingle(msg, u);
                    }
                    tu = next;
                    nfound++;
                    check(nfound <= pop, "ask_detail: population");
                }
                if (err == 0)
                {
                    sendSingle(C.lsprintf(0, SEND_END_DETAIL + "%d %d", sessionNum, totalC), u);
                }
                sent = true;
            }
        }
        if (!sent)
        {
            sendSingle(C.lsprintf(0, ECHO_FAILED + SEND_ASK_DETAIL + "%s", data), u);
        }
    }

    void process_ask_password(String data, User u, String seq)
    {
        boolean sent = false;
        Object[] o = new Object[1];
        if (C.sscanf(data, "%d", o) == 1)
        {
            int sessionNum = (Integer) o[0];
            if (sessionNum >= 0 && sessionNum <= maxSessions)
            {
                sendSingle(C.lsprintf(0, ECHO_PASSWORD + "%d %s", sessionNum, Sessions[sessionNum].sessionURLs), u);
                sent = true;
            }
        }
        if (!sent)
        {
            sendSingle(C.lsprintf(0, ECHO_FAILED + SEND_ASK_PASSWORD + "%s", data), u);
        }
    }

    void process_send_all_group(String data, User u, String seq)
    {
        doEchoAll(u, data, false);
    }

    void process_register_player(String data, User u, String seq)
    {
        if (logging >= LOG_CONNECTIONS)
        {
            logEntry(mainLog, "[%s]Register Player C%d (%s#%d) S%d  session %d : %s\n",
                timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, u.session.sessionNUM, data);
        }
        Session s = u.session;
        int uid = 0;
        int chan = 0;
        int rev = -1;
        int[] r1 = safe_scanint(data);
        int orderidx = r1[0];
        int order = r1[1];
        int seat = 0;
        int seatidx = -1, nameidx = -1, uididx = -1, chanidx = -1, revidx = -1;
        String name = "";
        if (orderidx > 0)
        {
            int[] r = safe_scanint(data.substring(orderidx));
            seatidx = r[0];
            seat = r[1];
        }
        if (seatidx > 0)
        {
            Tok t = scanstr(data.substring(orderidx + seatidx));
            nameidx = nameTooLong(t.s) ? -1 : t.n;
            name = t.s;
        }
        if (nameidx > 0)
        {
            int[] r = safe_scanint(data.substring(orderidx + seatidx + nameidx));
            uididx = r[0];
            if (uididx > 0) { uid = r[1]; }
        }
        if (uididx > 0)
        {
            int[] r = safe_scanint(data.substring(orderidx + seatidx + nameidx + uididx));
            chanidx = r[0];
            if (chanidx > 0) { chan = r[1]; }
        }
        if (chanidx > 0)
        {
            int[] r = safe_scanint(data.substring(orderidx + seatidx + nameidx + uididx + chanidx));
            revidx = r[0];
            if (revidx > 0) { rev = r[1]; }
        }
        if (revidx < 0) { rev = 0; }
        if (nameidx > 0)
        {
            User newu;
            if (chanidx > 0 && chan > 0)
            {
                newu = s.first_user;
                while (newu != null && newu.userNUM != chan) { newu = newu.next_in_session; }
                if (newu == null) { newu = u; }
                newu.isARobot = !isRealSocket(newu.socket);
            }
            else
            {
                newu = findAslot(s);
                if (newu == null) { return; }
                newu.socket = -1;
                newu.conn = null;
                newu.checksums = false;
                newu.isARobot = true;
            }
            newu.clientUid = uid;
            newu.clientPublicName = name;
            newu.clientRealName = name;
            newu.clientSeat = seat;
            newu.clientOrder = order;
            newu.clientRev = rev;
            newu.isAPlayer = true;
            doEchoAll(u, C.lsprintf(0, ECHO_INTRO_OTHERS + "%d %d %d %d %s %d %d",
                newu.userNUM, newu.isARobot ? 2 : 1, newu.clientSeat, newu.clientUid, encodeName(newu.clientRealName),
                newu.clientOrder, newu.clientRev), false);
        }
        else
        {
            sendSingle(C.lsprintf(0, ECHO_FAILED + SEND_REGISTER_PLAYER + "%s", data), u);
        }
    }

    void process_write_game(String data, User u, String seq)
    {
        Session sess = u.session;
        if (sess != LOBBYSESSION() && sess.sessionFileWritten == 0)
        {
            sess.sessionFileWritten = 1;
            logGame(data, sess, u);
        }
    }

    void process_query_game(String data, User u, String seq)
    {
        int[] r = safe_scanint(data);
        int idx1 = r[0];
        int sess = r[1];
        Tok t = idx1 > 0 ? scanstr(data.substring(idx1)) : new Tok(-1, "");
        GameBuffer g = null;
        String out;
        if (t.n > 0 && (g = findNamedGame(u, t.s)) != null)
        {
            out = C.lsprintf(0, ECHO_QUERY_GAME + "%d %d", sess, g.uid);
        }
        else
        {
            out = C.lsprintf(0, ECHO_QUERY_GAME + "%d 0", sess);
        }
        sendSingle(out, u);
    }

    void process_save_game(String data, User u, String seq)
    {
        data = unescape(data);
        if (u.session.sessionHasGame != 0 && !u.isAPlayer)
        {
            if (logErrorFor(u))
            {
                unusual_events++;
                logEntry(securityLog, "[%s] Unusual: Spectator C%d (%s#%d) S%d  session %d tried to record Game\n",
                    timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, u.session.sessionNUM);
            }
            return;
        }
        Tok t = scanstr(data, SMALLBUFSIZE);
        String out;
        if (t.n > 0)
        {
            // the C used data+strlen(namebuf): the text after the name
            String body = data.substring(Math.min(data.length(), t.s.length()));
            GameBuffer g = recordGame(u, t.s, body);
            out = C.lsprintf(0, ECHO_SAVE_GAME + "%d", g.uid);
        }
        else
        {
            out = ECHO_SAVE_GAME + "0";
        }
        sendSingle(out, u);
    }

    void lockSomeOtherUserForSession(Session S)
    {
        User U = S.locker;
        S.locker = null;
        if (U != null)
        {
            U.requestingLock = false;
            sendSingle(LOCK_GAME + "0", U);
            User candidate = U;
            int guard = 0;
            do
            {
                if (++guard > MAXCLIENTS)
                {
                    unusual_events++;
                    logEntry(mainLog, "[%s] UNUSUAL: lock holder C%d not found in session %d, lock released\n",
                        timestamp(), U.userNUM, S.sessionNUM);
                    return;
                }
                if (candidate.requestingLock)
                {
                    sendSingle(LOCK_GAME + "1", candidate);
                    candidate.requestingLock = false;
                    S.locker = candidate;
                    return;
                }
                candidate = candidate.next_in_session;
                if (candidate == null) { candidate = S.first_user; }
            } while (candidate != U);
        }
    }

    void process_lock_game(String data, User u, String seq)
    {
        Session S = u.session;
        int[] r = safe_scanint(data);
        int onoff = r[0] > 0 ? r[1] : 0;
        u.requestingLock = onoff != 0;
        if (onoff != 0 && (S.locker == null || S.locker == u))
        {
            u.requestingLock = false;
            S.locker = u;
            sendSingle(LOCK_GAME + "1", u);
        }
        else
        {
            if (S.locker == u) { lockSomeOtherUserForSession(S); }
            else { sendSingle(LOCK_GAME + "0", u); }
        }
    }

    void process_append_game(String data, User u, String seq)
    {
        data = unescape(data);
        Tok t1 = scanstr(data, SMALLBUFSIZE);
        int idx1 = t1.n;
        int idx2 = -1, idx3 = -1;
        int offset = 0, checksum = 0;
        if (idx1 > 0)
        {
            int[] r = safe_scanint(data.substring(idx1));
            idx2 = r[0];
            if (idx2 > 0) { offset = r[1]; }
        }
        if (idx2 > 0)
        {
            int[] r = safe_scanint(data.substring(idx1 + idx2));
            idx3 = r[0];
            if (idx3 > 0) { checksum = r[1]; }
        }
        String out;
        if (idx3 > 0)
        {
            int specAt = idx1 + idx2 + idx3 + 1;
            String spec = specAt <= data.length() ? data.substring(specAt) : "";
            GameBuffer g = reRecordGame(u, t1.s, offset, checksum, spec);
            out = C.lsprintf(SMALLBUFSIZE, ECHO_SAVE_GAME + "%d", g != null ? g.uid : 0);
        }
        else
        {
            out = ECHO_SAVE_GAME + "0";
        }
        sendSingle(out, u);
    }

    void process_remove_game(String data, User u, String seq)
    {
        Tok t = scanstr(data);
        int v = 0;
        if (t.n > 0) { v = removeNamedGame(u, t.s); }
        sendSingle(C.lsprintf(0, ECHO_REMOVE_GAME + "%d", v), u);
    }

    void process_send_group(String data, User u, String seq)
    {
        int slen = supervisor.length();
        int wordlen = 0;
        while (u.supervisor && 6 + wordlen < data.length() && C.sc(data.charAt(6 + wordlen)) > ' ' && data.charAt(6 + wordlen) != '|')
        {
            wordlen++;
        }
        if (slen > 3
            && u.oopsCount < 5
            && data.length() > 6 && data.startsWith("chat ", 1)
            && 6 + wordlen < data.length() && data.charAt(6 + wordlen) == '|')
        {
            if (u.supervisor
                && 6 + slen < data.length() && data.charAt(6 + slen) == '|'
                && data.startsWith(supervisor, 6))
            {
                doSupervisor(u, data.substring(7 + slen));
            }
            else
            {
                u.oopsCount++;
                sendSingle(C.lsprintf(0, ECHO_GROUP_OTHERS + "%d schat oops", u.userNUM), u);
            }
        }
        else
        {
            if (data.length() >= 6 && data.regionMatches(true, 1, "chat ", 0, 5)
                && u.session.sessionIsPrivate == 0
                && data.charAt(0) != 't' && data.charAt(0) != 'T'
                && data.charAt(0) != 'l' && data.charAt(0) != 'L')
            {
                logEntry(chatLog, "[%s] S%d %s: %s\n", timestamp(), u.session.sessionNUM, u.clientRealName, data.substring(6));
            }
            doEchoOthers(u, C.lsprintf(0, ECHO_GROUP_OTHERS + "%d %s", u.userNUM, data), false);
        }
        doEchoSelf(u, C.lsprintf(0, ECHO_GROUP_SELF + "%s%s", seq, data), false);
    }

    static boolean endsIn(String str, String end)
    {
        return str.length() >= end.length() && str.regionMatches(true, str.length() - end.length(), end, 0, end.length());
    }

    void process_sendmessageto(String data, User u, String seq)
    {
        int[] r = safe_scanint(data);
        int usridx = r[0];
        int user = usridx >= 0 ? r[1] : -1;
        Tok t = usridx >= 0 ? scanstr(data.substring(usridx)) : new Tok(-1, "");
        boolean isPM = t.n > 0 && endsIn(t.s, "chat");
        if (usridx >= 1)
        {
            Session su = u.session;
            User tu = su.first_user;
            int pop = su.population;
            int nfound = 0;
            while (tu != null)
            {
                User next = tu.next_in_session;
                if (tu.userNUM == user && isRealSocket(tu.socket))
                {
                    int idx = 0;
                    while (idx < data.length() && data.charAt(idx) > ' ') { idx++; }
                    sendSingle(C.lsprintf(0, ECHO_GROUP_OTHERS + "%d %s", u.userNUM, data.substring(idx)), tu);
                    if (isPM)
                    {
                        while (idx < data.length() && data.charAt(idx) <= ' ' && data.charAt(idx) > 0) { idx++; }
                        while (idx < data.length() && data.charAt(idx) > ' ') { idx++; }
                        while (idx < data.length() && data.charAt(idx) <= ' ' && data.charAt(idx) > 0) { idx++; }
                        logEntry(chatLog, "[%s] S%d PM %s to %s: %s\n", timestamp(), u.session.sessionNUM,
                            u.clientRealName, tu.clientRealName, data.substring(idx));
                    }
                }
                tu = next;
                nfound++;
                check(nfound <= pop, "sendmessageto: population");
            }
        }
    }

    /**
     * the length in the escaped text of the character at s[i], with the byte value it
     * stands for in value[0].  Text from the client is escaped: \\\\ for a backslash,
     * \\nnn for a control character, and \\#hhhh for each byte of a multibyte utf-8
     * character.  Anything else counts as itself.
     */
    static int escapedCharAt(String s, int i, int[] value)
    {
        int len = s.length();
        char c = s.charAt(i);
        if (c == '\\' && i + 1 < len)
        {
            char n = s.charAt(i + 1);
            if (n == '#')
            {
                int v = 0;
                int k = 2;
                while (k < 6 && i + k < len && Character.digit(s.charAt(i + k), 16) >= 0)
                {
                    v = v * 16 + Character.digit(s.charAt(i + k), 16);
                    k++;
                }
                value[0] = v;
                return k;
            }
            if (n >= '0' && n <= '9')
            {
                int v = 0;
                int k = 1;
                while (k < 4 && i + k < len && s.charAt(i + k) >= '0' && s.charAt(i + k) <= '9')
                {
                    v = v * 10 + (s.charAt(i + k) - '0');
                    k++;
                }
                value[0] = v;
                return k;
            }
            value[0] = n;		// \\\\ and any other quoted character
            return 2;
        }
        value[0] = c;
        return 1;
    }

    /** the most characters a player name may have; longer names are refused, not truncated */
    static final int MAXNAMECHARS = 50;

    /**
     * true if a name, as received (escaped), has more than MAXNAMECHARS characters.
     * Each escape sequence counts as the byte it stands for, and the continuation bytes
     * of a utf-8 character don't count, so this counts characters, not bytes or escapes.
     */
    static boolean nameTooLong(String s)
    {
        int len = s.length();
        int[] value = new int[1];
        int count = 0;
        int i = 0;
        while (i < len)
        {
            i += escapedCharAt(s, i, value);
            boolean continuation = value[0] >= 0x80 && value[0] < 0xc0;	// utf-8 continuation byte
            if (!continuation && ++count > MAXNAMECHARS) { return true; }
        }
        return false;
    }

    /**
     * encode a name for sending to clients, as the client's Base64.encodeAlphaNumeric does:
     * ascii letters and digits as themselves, anything else as %nnn% (its code), so whatever
     * a name contains stays one token.  The transport's \\\\, \\nnn and \\#hhhh escapes
     * are passed through unchanged.  As in the C server; names are kept as received.
     */
    static String encodeName(String in)
    {
        StringBuilder out = new StringBuilder();
        int len = in.length();
        int[] value = new int[1];
        int i = 0;
        while (i < len)
        {
            char c = in.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9'))
            {
                out.append(c);
                i++;
            }
            else if (c == '\\' && i + 1 < len)
            {
                int n = escapedCharAt(in, i, value);
                out.append(in, i, i + n);
                i += n;
            }
            else
            {
                out.append('%').append(c & 0xff).append('%');
                i++;
            }
        }
        return out.toString();
    }

    void process_send_name(String data, User u, String seq)
    {
        Tok t = scanstr(data);
        int nameidx = nameTooLong(t.s) ? -1 : t.n;
        String out;
        if (nameidx > 0)
        {
            String tempString = t.s;
            int[] r = safe_scanint(data.substring(nameidx));
            int uididx = r[0];
            out = C.lsprintf(0, ECHO_NAME + "%s", encodeName(tempString));
            u.clientPublicName = tempString;
            if (u.clientRealName.isEmpty())
            {
                u.clientRealName = tempString;
            }
            if (uididx > 0) { u.clientUid = r[1]; }
            saveConfig();
        }
        else
        {
            out = C.lsprintf(0, ECHO_FAILED + ECHO_NAME + "%s", data);
        }
        sendSingle(out, u);
    }

    void process_send_seat(String data, User u, String seq)
    {
        Object[] o = new Object[1];
        String out;
        if (C.sscanf(data, "%d", o) >= 1)
        {
            int seat = (Integer) o[0];
            out = C.lsprintf(0, ECHO_SEAT + "%d", seat);
            u.clientSeat = seat;
        }
        else
        {
            out = C.lsprintf(0, ECHO_FAILED + ECHO_SEAT + "%s", data);
        }
        sendSingle(out, u);
    }

    void process_multiple(String cmd, User u, String seq)
    {
        int subcommand = 0;
        int total = cmd.length() - 1;
        int cmdidx = 0;
        StringBuilder work = new StringBuilder(cmd);
        while (cmdidx + 1 < total)
        {
            Object[] o = new Object[2];
            int nc = C.sscanf(work.substring(cmdidx), "%d%n", o);
            subcommand++;
            if (nc == 1)
            {
                int len = (Integer) o[0];
                int idx = (Integer) o[1];
                if (len >= 0 && (len + idx + cmdidx) <= total)
                {
                    int cindex = idx + cmdidx;
                    int nextidx = cindex + C.skipEscapedText(work.toString(), idx + cmdidx, len);
                    if (nextidx > work.length()) { nextidx = work.length(); }
                    if (cindex < work.length() && work.charAt(cindex) == ' ') { cindex++; }
                    String sub = cindex <= nextidx ? work.substring(cindex, nextidx) : "";
                    u = processCheckSummedMessages(sub, u, null, seq);
                    cmdidx = nextidx;
                }
                else
                {
                    if (logErrorFor(u))
                    {
                        logEntry(securityLog, "[%s] overrun multiple command C%d (%s#%d) S%d #%d %s\n",
                            timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, subcommand, cmd);
                    }
                    cmdidx = total;
                }
            }
            else
            {
                cmdidx = total;
                if (logErrorFor(u))
                {
                    logEntry(securityLog, "[%s] malformed multiple command C%d (%s#%d) S%d #%d %s\n",
                        timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, subcommand, cmd);
                }
            }
        }
    }

    void process999(String cmd, User u, String seq) { }

    // =====================================================================
    // dispatch
    // =====================================================================

    @FunctionalInterface
    interface Handler { void run(String cmd, User u, String seq); }

    private final Object[][] commands = {
        { SEND_GROUP, (Handler) this::process_send_group },
        { SEND_ASK_PASSWORD, (Handler) this::process_ask_password },
        { SEND_ALL_GROUP, (Handler) this::process_send_all_group },
        { SEND_REGISTER_PLAYER, (Handler) this::process_register_player },
        { SEND_WRITE_GAME, (Handler) this::process_write_game },
        { SEND_RESERVE_ROOM, (Handler) this::process_reserve_room },
        { SEND_SET_STATE, (Handler) this::process_set_state },
        { SEND_NAME, (Handler) this::process_send_name },
        { SEND_SEAT, (Handler) this::process_send_seat },
        { SEND_MULTIPLE, (Handler) this::process_multiple },
        { SEND_STATE_KEY, (Handler) this::process_recordStateKey },
        { SEND_MESSAGE_TO, (Handler) this::process_sendmessageto },
        { QUERY_GAME, (Handler) this::process_query_game },
        { FETCH_GAME, (Handler) (c, u, s) -> process_fetch_game_f(c, u, s, false) },
        { FETCH_GAME_FILTERED, (Handler) (c, u, s) -> process_fetch_game_f(c, u, s, true) },
        { FETCH_ACTIVE_GAME, (Handler) (c, u, s) -> process_fetch_active_game_f(c, u, s, false) },
        { FETCH_ACTIVE_GAME_FILTERED, (Handler) (c, u, s) -> process_fetch_active_game_f(c, u, s, true) },
        { SAVE_GAME, (Handler) this::process_save_game },
        { APPEND_GAME, (Handler) this::process_append_game },
        { LOCK_GAME, (Handler) this::process_lock_game },
        { REMOVE_GAME, (Handler) this::process_remove_game },
        { SEND_SUMMARY, (Handler) this::process_send_summary },
        { SEND_ASK_DETAIL, (Handler) this::process_ask_detail },
        { SEND_LOBBY_INFO, (Handler) this::process_send_lobby_info },
        { SEND_INTRO, (Handler) this::process_send_intro },
        { SEND_PING, (Handler) this::process_ping },
        { SEND_CHECK_SCORE, (Handler) this::process_check_score },
        { SEND_LOGSHORTNOTE, (Handler) this::process_logshortnote },
        { SEND_LOGMESSAGE, (Handler) this::process_logmessage },
        { ECHO_FAILED, (Handler) this::process999 },
    };

    User processCheckSummedMessages(String cmd, User u, String rawcmd, String seq)
    {
        if (u.unexpectedCount > 0)
        {
            String shortCmd = C.trunc(cmd, 100);
            String dots = shortCmd.length() < cmd.length() ? " ..." : "";
            if (u.use_rng_in && rawcmd != null)
            {
                logEntry(mainLog, "[%s] unusual ignored encrypted message from C%d (%s#%d) S%d : %s%s\n",
                    timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, rawcmd, dots);
            }
            else
            {
                logEntry(mainLog, "[%s] unusual ignored message from C%d (%s#%d) S%d : %s%s\n",
                    timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, shortCmd, dots);
            }
            return u;
        }
        for (Object[] pp : commands)
        {
            String str = (String) pp[0];
            if (cmd.startsWith(str))
            {
                ((Handler) pp[1]).run(cmd.substring(str.length()), u, seq);
                return u;
            }
        }
        if (cmd.startsWith(SEND_TAKEOVER))
        {
            u = process_takeover(cmd.substring(4), u, seq);
        }
        else
        {
            unusual_events++;
            u.unexpectedCount++;
            if (cmd.startsWith("GET ") || cmd.startsWith("POST "))
            {
                banUserByIP(u);
            }
            if (logging >= LOG_ERRORS)
            {
                if (u.use_rng_in && rawcmd != null)
                {
                    DumpHistory();
                    logEntry(mainLog, "[%s] unusual encrypted message from C%d (%s#%d) S%d : %s\n",
                        timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, rawcmd);
                }
                else if (u.nread <= 1)
                {
                    logEntry(mainLog, "[%s] unusual unexpected message from C%d (%s#%d) S%d : %s\n",
                        timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, cmd);
                    banUserByIP(u);
                }
                else
                {
                    DumpHistory();
                    logEntry(mainLog, "[%s] unusual unexpected message from C%d (%s#%d) S%d : %s\n",
                        timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, cmd);
                }
            }
            if (u.session != WaitingSession)
            {
                sendSingle(C.lsprintf(SMALLBUFSIZE, ECHO_GROUP_SELF + "%s", cmd), u);
            }
            else
            {
                simpleCloseClientOnly(u, "bad input from waiting session");
                removeUserFromSession(u);
                putUserInSession(u, Idle_Users);
            }
        }
        return u;
    }

    User ProcessMessages(String cmdIn, User u)
    {
        if (cmdIn.isEmpty()) { return u; }
        String rawcmd = C.trunc(cmdIn, 120);
        u.nread++;
        transactions++;
        StringBuilder cmd = new StringBuilder(cmdIn);
        int start = 0;
        if (u.use_rng_in && !u.checksums)
        {
            checksumInputString(u, cmd, 0);
            recordInHistory(" in", u.socket, cmd.toString(), cmd.length());
            if (logging >= LOG_ALL)
            {
                logEntry(mainLog, "[%s] received from C%d (%s#%d) S%d : %s\n",
                    timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, cmd.toString());
            }
        }
        else if (cmdIn.startsWith("500 "))
        {
            int sum1 = (cmd.length() > 4 ? cmd.charAt(4) : 0) - 'A';
            int sum2 = (cmd.length() > 5 ? cmd.charAt(5) : 0) - 'A';
            int sum3 = (cmd.length() > 6 ? cmd.charAt(6) : 0) - 'A';
            int sum4 = (cmd.length() > 7 ? cmd.charAt(7) : 0) - 'A';
            int sum = (sum1 << 12) + (sum2 << 8) + (sum3 << 4) + sum4;
            int asum = cmd.length() >= 9 ? checksumInputString(u, cmd, 9) : 0;
            recordInHistory(" in", u.socket, cmd.toString(), cmd.length());
            u.checksums = true;
            if (logging >= LOG_ALL)
            {
                logEntry(mainLog, "[%s] received from C%d (%s#%d) S%d : %s\n",
                    timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, cmd.toString());
            }
            if (asum == sum)
            {
                start = 9;
            }
            else
            {
                checksum_errors++;
                if (logging >= LOG_ERRORS)
                {
                    logEntry(mainLog, "[%s] C%d (%s#%d) S%d in session %d checksum error\nMessage was %s\n",
                        timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, u.session.sessionNUM, cmd.toString());
                    DumpHistory();
                }
                sendSingle(C.lsprintf(0, ECHO_FAILED + "%s", cmd.toString()), u);
                return u;
            }
        }
        else if (u.checksums)
        {
            checksum_errors++;
            if (u.use_rng_in) { checksumInputString(u, cmd, 0); }
            recordInHistory(" in", u.socket, cmd.toString(), cmd.length());
            if (logging >= LOG_ALL)
            {
                logEntry(mainLog, "[%s] received from C%d (%s#%d) S%d : %s\n",
                    timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, cmd.toString());
            }
            if (logging >= LOG_ERRORS)
            {
                logEntry(mainLog, "[%s] C%d (%s#%d) S%d in session %d  unchecksummed string\nMessage was %s\n",
                    timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, u.session.sessionNUM, cmd.toString());
                DumpHistory();
            }
            return u;
        }
        else
        {
            recordInHistory(" in", u.socket, cmd.toString(), cmd.length());
            if (logging >= LOG_ALL)
            {
                logEntry(mainLog, "[%s] received from C%d (%s#%d) S%d : %s\n",
                    timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, cmd.toString());
            }
        }

        String c = cmd.substring(Math.min(start, cmd.length()));
        String sequenceNumber = "";
        if (!c.isEmpty() && c.charAt(0) == 'x')
        {
            int[] r = safe_scanint(c.substring(1));
            int idx = r[0];
            int seqn = idx > 0 ? r[1] : 0;
            if (idx > 0)
            {
                // MyStrncpy(sequenceNumber, cmd, idx+3): the "x" the digits and the space
                sequenceNumber = C.trunc(c, Math.min(64, idx + 3));
                c = (2 + idx) <= c.length() ? c.substring(2 + idx) : "";
            }
            if (seqn != u.rng_in_seq)
            {
                if (u.rng_in_seq_errors == 0)
                {
                    unusual_events++;
                    DumpHistory();
                    logEntry(mainLog, "[%s] Unusual: C%d (%s#%d) S%d  session %d input seqence error, is %d expected %d\n",
                        timestamp(), u.userNUM, u.clientRealName, u.clientUid, u.socket, u.session.sessionNUM, seqn, u.rng_in_seq);
                }
                u.rng_in_seq_errors++;
            }
            u.rng_in_seq++;
        }
        return processCheckSummedMessages(c, u, rawcmd, sequenceNumber);
    }
}
