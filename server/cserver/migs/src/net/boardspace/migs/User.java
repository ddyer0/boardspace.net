package net.boardspace.migs;

/** one per connection (C struct User) */
final class User
{
    final int userNUM;
    Session session;			// never null once initialized
    User next_in_session;
    User nextDead;
    Session deadFromSession;
    String reasonClosed = "";		// 32
    String pingStats = "";			// from ping, kept whole
    String cookie = "";				// BANSIZE
    int errorsLogged;
    int socket = -1;				// small integer id shown in logs as S%d; -1 = no socket
    Connection conn;				// the network connection, when socket != -1
    int badReadCount, badWriteCount, goodReadCount, goodWriteCount;
    int ip;							// a.b.c.d as (a<<24)|(b<<16)|(c<<8)|d
    int serverKey;
    RegisteredUser keyIndex;

    // input: bytes received but not yet consumed as lines
    byte[] inbuf = new byte[0];
    int inbufCount = 0;
    // output: bytes queued but not yet sent
    byte[] outbuf = new byte[0];
    int outTake = 0;
    int outPut = 0;

    boolean supervisor, blocked, wasZapped, isAPlayer, checksums, sequencenumbers, gagged;
    boolean expectEof, inputClosed, outputClosed, isARobot, requestingLock;
    boolean websocket;
    long clientTime;
    int oopsCount, unexpectedCount;
    int clientSeat, clientOrder, clientRev;
    String clientPublicName = "";	// as received, escaped
    String clientRealName = "";		// as received, escaped
    String lobbyInfo = "";			// 64
    int clientUid;
    long totalread, totalwrite;
    int nread, nwrite;
    int rogueOutput, injectedOutput;
    int m_z_in, m_w_in, m_z_out, m_w_out;
    boolean use_rng_in, use_rng_out;
    int rng_out_chars, rng_in_chars, rng_in_seq, rng_in_seq_errors, rng_out_seq;

    User(int num) { userNUM = num; }

    int outputPending() { return outPut - outTake; }

    void appendOutput(byte[] b, int off, int len)
    {
        if (outPut + len > outbuf.length)
        {
            int live = outPut - outTake;
            int need = live + len;
            byte[] nb = (need <= outbuf.length) ? outbuf : new byte[Math.max(need, Math.max(1024, outbuf.length * 2))];
            System.arraycopy(outbuf, outTake, nb, 0, live);
            outbuf = nb;
            outTake = 0;
            outPut = live;
        }
        System.arraycopy(b, off, outbuf, outPut, len);
        outPut += len;
    }

    void consumeOutput(int n)
    {
        outTake += n;
        if (outTake >= outPut) { outTake = 0; outPut = 0; }
    }

    void appendInput(byte[] b, int off, int len)
    {
        if (inbufCount + len > inbuf.length)
        {
            byte[] nb = new byte[Math.max(inbufCount + len, Math.max(1024, inbuf.length * 2))];
            System.arraycopy(inbuf, 0, nb, 0, inbufCount);
            inbuf = nb;
        }
        System.arraycopy(b, off, inbuf, inbufCount, len);
        inbufCount += len;
    }

    /** if a complete line is buffered, remove and return it (without the terminator); else null */
    String takeLine()
    {
        for (int i = 0; i < inbufCount; i++)
        {
            byte ch = inbuf[i];
            if (ch == '\n' || ch == '\r')
            {
                String line = C.cstr(C.str(inbuf, 0, i));
                System.arraycopy(inbuf, i + 1, inbuf, 0, inbufCount - i - 1);
                inbufCount -= i + 1;
                return line;
            }
        }
        return null;
    }

    boolean hasLine()
    {
        for (int i = 0; i < inbufCount; i++)
        {
            if (inbuf[i] == '\n' || inbuf[i] == '\r') { return true; }
        }
        return false;
    }

    void clearBuffers()
    {
        inbuf = new byte[0];
        inbufCount = 0;
        outbuf = new byte[0];
        outTake = outPut = 0;
    }
}
