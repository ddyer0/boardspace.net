package net.boardspace.migs;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

/**
 * One client connection.  Everything here is non-blocking: read and write
 * return 0 when the socket isn't ready, and the main loop's selector says when
 * to try again.
 * <p>
 * A plain connection (the main port) just moves bytes.  A websocket connection
 * (the websocket port) first sniffs the first byte for TLS (0x16 or 0x80, as
 * the C server did), optionally runs TLS through an SSLEngine, then performs
 * the HTTP upgrade handshake, then carries the same line protocol inside
 * websocket frames.
 * <p>
 * A connection on the main port can also be TLS, decided by the first byte as
 * for websockets.  It then carries the line protocol directly, with no websocket layer.
 */
final class Connection
{
    /** a websocket connection failed; the server closes it (and bans the ip if asked) */
    static final class WsFailure extends IOException
    {
        private static final long serialVersionUID = 1L;
        final boolean ban;
        WsFailure(String msg, boolean ban) { super(msg); this.ban = ban; }
    }

    /** a plain connection on the standard port when sslrequired is set */
    static final class PlainRefused extends IOException
    {
        private static final long serialVersionUID = 1L;
        PlainRefused() { super("plain connection refused"); }
    }

    /** the peer sent a websocket close frame */
    static final class OrderlyClose extends EOFException
    {
        private static final long serialVersionUID = 1L;
        OrderlyClose() { super("client sent orderly close frame"); }
    }

    static final int WEBSOCKET_BUFSIZE = 65536;
    static final int MAX_OUT_SIZE = 65535;
    private static final String HYBI_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final ByteBuffer EMPTY = ByteBuffer.allocate(0);

    private enum Phase { PREAMBLE, HANDSHAKE, MAIN }

    final SocketChannel ch;
    SelectionKey key;
    boolean websocket;				// true for websocket and raw TLS: everything that isn't a plain socket
    boolean rawTls;					// TLS on the standard port: no http upgrade, no framing
    boolean tlsUndecided = false;	// standard port: plain or TLS, decided by the first byte
    boolean tlsDecided = false;		// set when the standard port connection turns out to be TLS, for the log
    private final Migs0 srv;
    private final int sock;

    // ---- websocket state
    private Phase phase = Phase.PREAMBLE;
    private ByteBuffer rawIn = null;							// plain bytes read while sniffing, read mode
    private final StringBuilder hsBuf = new StringBuilder();	// handshake text accumulated
    private ByteBuffer frameIn = ByteBuffer.allocate(4096);		// undecoded frame bytes, write mode
    private ByteBuffer decoded = ByteBuffer.allocate(4096);		// decoded payload not yet returned, write mode
    private ByteBuffer frameOut = ByteBuffer.allocate(0);		// encoded bytes not yet accepted by the link, read mode

    // ---- TLS state (null engine = plain)
    private SSLEngine ssl = null;
    private ByteBuffer netIn, appIn, netOut;
    private boolean tlsClosed = false;
    private boolean netInStarved = true;

    boolean local = false;			// connected from this machine, exempt from sslrequired

    Connection(SocketChannel ch, boolean websocket, Migs0 srv, int sock)
    {
        this.ch = ch;
        this.websocket = websocket;
        this.srv = srv;
        this.sock = sock;
    }

    // =====================================================================
    // public interface used by the server
    // =====================================================================

    /**
     * read decoded protocol bytes into dst.
     * @return bytes read, 0 if nothing is available now, -1 at end of stream
     * @throws IOException on errors (WsFailure for websocket protocol failures,
     *         OrderlyClose when a websocket close frame arrives)
     */
    @SuppressWarnings("fallthrough")
    int read(byte[] dst) throws IOException
    {
        if (tlsUndecided)
        {	// the standard port accepts both plain and TLS connections.  The client always
            // speaks first: a TLS client with a handshake record (0x16, or 0x80 for an old
            // style hello), a plain client with a protocol line, which starts with a digit.
            ByteBuffer b = ByteBuffer.wrap(dst);
            int n = ch.read(b);
            if (n <= 0) { return n; }		// nothing yet, or eof
            tlsUndecided = false;
            int first = dst[0] & 0xff;
            if (first == 0x16 || first == 0x80)
            {	// from here on, TLS with no websocket layer
                websocket = true;
                rawTls = true;
                tlsDecided = true;
                ByteBuffer initial = ByteBuffer.allocate(n);
                initial.put(dst, 0, n);
                initial.flip();
                startTls(initial);
                phase = Phase.MAIN;
                return linkRead(dst);
            }
            if (srv.sslRequired && !local) { throw new PlainRefused(); }
            return n;			// plain: these are the first protocol bytes
        }
        if (!websocket)
        {
            return ch.read(ByteBuffer.wrap(dst));
        }
        if (rawTls)
        {
            return linkRead(dst);
        }
        if (decoded.position() > 0) { return drainDecoded(dst); }
        switch (phase)
        {
        case PREAMBLE:
            if (!preamble()) { return 0; }
            // fall through to try the handshake with whatever arrived
        case HANDSHAKE:
            handshake();
            if (phase != Phase.MAIN) { return 0; }
            // fall through, frames may have followed the handshake
        case MAIN:
        default:
            readFrames();
            return drainDecoded(dst);
        }
    }

    /**
     * write protocol bytes.
     * @return number of bytes taken (they will all be delivered), 0 if blocked
     */
    int write(byte[] src, int off, int len) throws IOException
    {
        if (!websocket)
        {
            return ch.write(ByteBuffer.wrap(src, off, len));
        }
        if (phase != Phase.MAIN) { return 0; }
        if (!flushFrameOut()) { return 0; }		// finish the previous frame first
        int siz = len;
        if (siz > MAX_OUT_SIZE)
        {
            siz = MAX_OUT_SIZE;
            srv.logEntry(srv.mainLog, "[%s] output buffer trimmed from %d to %d S%d\n", Migs0.timestamp(), len, MAX_OUT_SIZE, sock);
        }
        if (rawTls)
        {	// no framing, just keep the bytes until the link takes them
            byte[] copy = new byte[siz];
            System.arraycopy(src, off, copy, 0, siz);
            frameOut = ByteBuffer.wrap(copy);
        }
        else
        {
            frameOut = encodeFrame(src, off, siz);
        }
        flushFrameOut();
        return siz;		// the frame is ours now, like the C websocketSend
    }

    /** true if the selector should watch for writability even with no protocol output pending */
    boolean wantsWrite()
    {
        if (!websocket) { return false; }
        if (frameOut.hasRemaining()) { return true; }
        if (ssl != null)
        {
            if (netOut.hasRemaining()) { return true; }
            return ssl.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP;
        }
        return false;
    }

    /** true if input is already buffered here (decrypted or decoded), which select can't see */
    boolean hasPending()
    {
        if (!websocket) { return false; }
        if (decoded.position() > 0) { return true; }
        if (rawIn != null && rawIn.hasRemaining()) { return true; }
        if (ssl != null && (appIn.position() > 0 || (!netInStarved && netIn.position() > 0))) { return true; }
        return !rawTls && phase == Phase.MAIN && completeFrameBuffered();
    }

    /** the socket is writable: push out anything pending (TLS records, partial frames, handshake replies) */
    void onWritable() throws IOException
    {
        if (!websocket) { return; }
        if (ssl != null)
        {
            flushNetOut();
            tlsProgress(false);
        }
        flushFrameOut();
    }

    boolean isHandshaking()
    {
        if (rawTls)
        {
            return ssl == null || ssl.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
        }
        return websocket && phase != Phase.MAIN;
    }

    void close()
    {
        try
        {
            if (key != null) { key.cancel(); }
            ch.socket().setSoLinger(true, 0);		// abortive close, like the C simpleCloseSocket
        }
        catch (IOException e)
        {
            // ignore
        }
        try
        {
            ch.close();
        }
        catch (IOException e)
        {
            // ignore
        }
    }

    // =====================================================================
    // websocket
    // =====================================================================

    /** sniff the first byte to decide TLS or plain.  true when decided */
    private boolean preamble() throws IOException
    {
        ByteBuffer b = ByteBuffer.allocate(16 * 1024 + 512);
        int n = ch.read(b);
        if (n == 0) { return false; }
        if (n < 0) { throw new WsFailure("Empty handshake.", false); }
        b.flip();
        int first = b.get(0) & 0xff;
        if (first == 0x16 || first == 0x80)
        {
            startTls(b);
        }
        else
        {
            if (srv.websocketSslOnly)
            {
                throw new WsFailure("non-SSL connection disallowed.", false);
            }
            rawIn = b;
        }
        phase = Phase.HANDSHAKE;
        return true;
    }

    /** create the TLS engine; initial holds bytes already read from the socket (read mode) */
    private void startTls(ByteBuffer initial) throws IOException
    {
        SSLContext ctx = srv.sslContext();
        if (ctx == null)
        {
            throw new WsFailure("SSL connection but no usable websocketSslKey / websocketSslCert", false);
        }
        ssl = ctx.createSSLEngine();
        ssl.setUseClientMode(false);
        int pkt = ssl.getSession().getPacketBufferSize();
        int app = ssl.getSession().getApplicationBufferSize();
        netIn = ByteBuffer.allocate(Math.max(pkt, initial.remaining()) + 1024);
        netIn.put(initial);
        appIn = ByteBuffer.allocate(app + 1024);
        netOut = ByteBuffer.allocate(pkt + 1024);
        netOut.flip();		// read mode, empty
        netInStarved = netIn.position() == 0;
        ssl.beginHandshake();
    }

    private void handshake() throws IOException
    {
        byte[] tmp = new byte[4096];
        int n;
        while ((n = linkRead(tmp)) > 0)
        {
            hsBuf.append(C.str(tmp, 0, n));
            if (hsBuf.length() >= WEBSOCKET_BUFSIZE - 1)
            {
                throw new WsFailure("Oversized handshake. size=" + hsBuf.length(), false);
            }
        }
        String buffer = hsBuf.toString();
        int len = buffer.length();
        if ((len >= 4 && !buffer.startsWith("GET "))
            || (len >= 16 && !buffer.startsWith("GET /gameserver ")))
        {
            if (srv.logging >= Migs0.LOG_CONNECTIONS)
            {
                srv.logEntry(srv.mainLog, "[%s] invalid websocket handshake, starts with %s S%d.\n", Migs0.timestamp(), buffer, sock);
            }
            throw new WsFailure("invalid websocket handshake", true);
        }
        int end = buffer.indexOf("\r\n\r\n");
        if (end < 0)
        {
            if (n < 0) { throw new WsFailure("Empty handshake.", false); }
            return;		// not all there yet
        }
        if (srv.logging >= Migs0.LOG_ALL)
        {
            srv.logEntry(srv.mainLog, "[%s] parsing websocket handshake %s S%d.\n", Migs0.timestamp(), buffer, sock);
        }
        String head = buffer.substring(0, end + 4);
        String response = parseHandshake(head);
        frameOut = ByteBuffer.wrap(C.bytes(response));
        phase = Phase.MAIN;
        // anything after the header is the start of the frame stream
        String rest = buffer.substring(end + 4);
        if (!rest.isEmpty()) { appendFrameIn(C.bytes(rest), rest.length()); }
        flushFrameOut();
    }

    /** returns the 101 response, or throws (C parse_handshake + do_handshake response) */
    private String parseHandshake(String h) throws IOException
    {
        if (h.length() < 92 || !h.startsWith("GET ")) { throw new WsFailure("bad websocket handshake", false); }
        if (h.indexOf(" HTTP/1.1") < 0) { throw new WsFailure("bad websocket handshake", false); }
        if (header(h, "\r\nHost: ") == null) { throw new WsFailure("bad websocket handshake", false); }
        String version = header(h, "\r\nSec-WebSocket-Version: ");
        if (version == null)
        {
            srv.logEntry(srv.mainLog, "[%s] older HIXIE protocols not supported. S%d\n", Migs0.timestamp(), sock);
            throw new WsFailure("older HIXIE protocols not supported", false);
        }
        String key1 = header(h, "\r\nSec-WebSocket-Key: ");
        if (key1 == null) { throw new WsFailure("bad websocket handshake", false); }
        if (header(h, "\r\nConnection: ") == null) { throw new WsFailure("bad websocket handshake", false); }
        String protocols = header(h, "\r\nSec-WebSocket-Protocol: ");
        if (srv.logging >= Migs0.LOG_CONNECTIONS)
        {
            // ws or wss, as decided by the first byte of the connection
            srv.logEntry(srv.mainLog, "[%s] using websockets (%s). S%d\n", Migs0.timestamp(), ssl != null ? "wss" : "ws", sock);
        }
        String protocolHeader = "";
        if (protocols != null && !protocols.isEmpty())
        {
            String first = protocols.split(",")[0].trim();
            if (!first.isEmpty())
            {
                protocolHeader = "Sec-WebSocket-Protocol: " + first + "\r\n";
                if (srv.logging >= Migs0.LOG_ALL)
                {
                    srv.logEntry(srv.mainLog, "[%s] websocket response protocol %s S%d\n", Migs0.timestamp(), first, sock);
                }
            }
        }
        return "HTTP/1.1 101 Switching Protocols\r\n"
            + "Upgrade: websocket\r\n"
            + "Connection: Upgrade\r\n"
            + "Sec-WebSocket-Accept: " + acceptKey(key1) + "\r\n"
            + protocolHeader
            + "\r\n";
    }

    private static String header(String h, String name)
    {
        int s = h.indexOf(name);
        if (s < 0) { return null; }
        s += name.length();
        int e = h.indexOf("\r\n", s);
        return e < 0 ? h.substring(s) : h.substring(s, e);
    }

    static String acceptKey(String key)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] d = md.digest(C.bytes(key + HYBI_GUID));
            return Base64.getEncoder().encodeToString(d);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private void readFrames() throws IOException
    {
        byte[] tmp = new byte[16 * 1024];
        int n;
        boolean eof = false;
        while (true)
        {
            n = linkRead(tmp);
            if (n > 0) { appendFrameIn(tmp, n); }
            else { eof = n < 0; break; }
            if (frameIn.position() >= WEBSOCKET_BUFSIZE) { break; }
        }
        decodeFrames();
        if (eof && decoded.position() == 0) { throw new EOFException(); }
    }

    private void appendFrameIn(byte[] b, int n)
    {
        if (frameIn.remaining() < n)
        {
            ByteBuffer nb = ByteBuffer.allocate(Math.max(frameIn.capacity() * 2, frameIn.position() + n + 1024));
            frameIn.flip();
            nb.put(frameIn);
            frameIn = nb;
        }
        frameIn.put(b, 0, n);
    }

    /** decode complete frames from frameIn into decoded; partial frames stay for next time */
    private void decodeFrames() throws IOException
    {
        frameIn.flip();
        try
        {
            while (frameIn.remaining() >= 2)
            {
                int start = frameIn.position();
                int b0 = frameIn.get(start) & 0xff;
                int b1 = frameIn.get(start + 1) & 0xff;
                int opcode = b0 & 0x0f;
                int masked = (b1 & 0x80) >> 7;
                if (opcode == 0x8)
                {
                    if (srv.logging >= Migs0.LOG_CONNECTIONS)
                    {
                        srv.logEntry(srv.mainLog, "[%s] client sent orderly close frame. S%d\n", Migs0.timestamp(), sock);
                    }
                    throw new OrderlyClose();
                }
                int payloadLength = b1 & 0x7f;
                int hdr;
                if (payloadLength < 126)
                {
                    hdr = 2;
                }
                else if (payloadLength == 126)
                {
                    if (frameIn.remaining() < 4) { break; }
                    payloadLength = ((frameIn.get(start + 2) & 0xff) << 8) + (frameIn.get(start + 3) & 0xff);
                    hdr = 4;
                }
                else
                {
                    srv.logEntry(srv.mainLog, "[%s] Receiving frames larger than 65535 bytes not supported size=%d S%d\n", Migs0.timestamp(), payloadLength, sock);
                    throw new WsFailure("frame too large", false);
                }
                int total = hdr + 4 * masked + payloadLength;
                if (frameIn.remaining() < total) { break; }		// incomplete, keep it
                int payload = start + hdr + 4 * masked;
                if (opcode != 0x1 && opcode != 0x2)
                {
                    srv.logEntry(srv.mainLog, "[%s] Ignoring non-data frame, opcode 0x%x S%d\n", Migs0.timestamp(), opcode, sock);
                }
                else if (payloadLength == 0)
                {
                    srv.logEntry(srv.mainLog, "[%s] Ignoring empty frame, len=0 S%d\n", Migs0.timestamp(), sock);
                }
                else
                {
                    if (decoded.remaining() < payloadLength)
                    {
                        ByteBuffer nb = ByteBuffer.allocate(Math.max(decoded.capacity() * 2, decoded.position() + payloadLength + 1024));
                        decoded.flip();
                        nb.put(decoded);
                        decoded = nb;
                    }
                    for (int i = 0; i < payloadLength; i++)
                    {
                        byte v = frameIn.get(payload + i);
                        if (masked != 0) { v ^= frameIn.get(payload - 4 + (i % 4)); }
                        decoded.put(v);
                    }
                }
                frameIn.position(start + total);
            }
        }
        finally
        {
            frameIn.compact();
        }
    }

    private boolean completeFrameBuffered()
    {
        int pos = frameIn.position();
        if (pos < 2) { return false; }
        int b1 = frameIn.get(1) & 0xff;
        int masked = (b1 & 0x80) >> 7;
        int plen = b1 & 0x7f;
        int hdr = 2;
        if (plen == 126)
        {
            if (pos < 4) { return false; }
            plen = ((frameIn.get(2) & 0xff) << 8) + (frameIn.get(3) & 0xff);
            hdr = 4;
        }
        else if (plen == 127) { return true; }		// will fail when decoded
        return pos >= hdr + 4 * masked + plen;
    }

    private int drainDecoded(byte[] dst)
    {
        decoded.flip();
        int n = Math.min(dst.length, decoded.remaining());
        decoded.get(dst, 0, n);
        decoded.compact();
        return n;
    }

    /** a text frame, as the C server always sent */
    private static ByteBuffer encodeFrame(byte[] src, int off, int len)
    {
        int hdr = len <= 125 ? 2 : 4;
        ByteBuffer f = ByteBuffer.allocate(hdr + len);
        f.put((byte) (0x80 | 0x01));
        if (len <= 125)
        {
            f.put((byte) len);
        }
        else
        {
            f.put((byte) 126);
            f.put((byte) ((len >> 8) & 0xff));
            f.put((byte) (len & 0xff));
        }
        f.put(src, off, len);
        f.flip();
        return f;
    }

    private boolean flushFrameOut() throws IOException
    {
        if (frameOut.hasRemaining())
        {
            linkWrite(frameOut);
        }
        return !frameOut.hasRemaining();
    }

    // =====================================================================
    // link: plain socket or TLS
    // =====================================================================

    /** read bytes from the peer.  >0, 0 = nothing now, -1 = end of stream */
    private int linkRead(byte[] dst) throws IOException
    {
        if (ssl == null)
        {
            if (rawIn != null && rawIn.hasRemaining())
            {
                int n = Math.min(dst.length, rawIn.remaining());
                rawIn.get(dst, 0, n);
                return n;
            }
            return ch.read(ByteBuffer.wrap(dst));
        }
        if (appIn.position() == 0)
        {
            int r = tlsProgress(true);
            if (r < 0 && appIn.position() == 0) { return -1; }
        }
        appIn.flip();
        int n = Math.min(dst.length, appIn.remaining());
        appIn.get(dst, 0, n);
        appIn.compact();
        return n;
    }

    /** write as much of src as the link will take now */
    private void linkWrite(ByteBuffer src) throws IOException
    {
        if (ssl == null)
        {
            ch.write(src);
            return;
        }
        if (!flushNetOut()) { return; }
        SSLEngineResult.HandshakeStatus hs = ssl.getHandshakeStatus();
        if (hs != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING && hs != SSLEngineResult.HandshakeStatus.FINISHED)
        {
            tlsProgress(false);
            if (!flushNetOut()) { return; }
        }
        while (src.hasRemaining())
        {
            netOut.compact();
            SSLEngineResult r;
            try
            {
                r = ssl.wrap(src, netOut);
            }
            finally
            {
                netOut.flip();
            }
            if (r.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW)
            {
                ByteBuffer nb = ByteBuffer.allocate(netOut.capacity() + ssl.getSession().getPacketBufferSize());
                nb.put(netOut);
                nb.flip();
                netOut = nb;
                continue;
            }
            if (r.getStatus() == SSLEngineResult.Status.CLOSED) { throw new EOFException("TLS closed"); }
            runTasks();
            if (!flushNetOut()) { return; }
            if (r.bytesConsumed() == 0) { return; }
        }
    }

    private boolean flushNetOut() throws IOException
    {
        while (netOut.hasRemaining())
        {
            if (ch.write(netOut) == 0) { return false; }
        }
        return true;
    }

    private void runTasks()
    {
        Runnable r;
        while ((r = ssl.getDelegatedTask()) != null) { r.run(); }
    }

    /**
     * read from the socket and run the engine as far as it will go: handshake
     * steps and decryption into appIn.  Returns -1 when the peer is gone and no
     * more data can come, else 0.
     */
    private int tlsProgress(boolean readSocket) throws IOException
    {
        boolean eof = false;
        if (readSocket && !tlsClosed)
        {
            if (netIn.remaining() == 0) { growNetIn(); }
            int n = ch.read(netIn);
            if (n < 0) { eof = true; }
            if (n > 0) { netInStarved = false; }
        }
        for (int guard = 0; guard < 1000; guard++)
        {
            SSLEngineResult.HandshakeStatus hs = ssl.getHandshakeStatus();
            if (hs == SSLEngineResult.HandshakeStatus.NEED_TASK)
            {
                runTasks();
                continue;
            }
            if (hs == SSLEngineResult.HandshakeStatus.NEED_WRAP)
            {
                if (!flushNetOut()) { break; }
                netOut.compact();
                SSLEngineResult r;
                try
                {
                    r = ssl.wrap(EMPTY, netOut);
                }
                catch (SSLException e)
                {
                    netOut.flip();
                    throw e;
                }
                netOut.flip();
                if (r.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW)
                {
                    ByteBuffer nb = ByteBuffer.allocate(netOut.capacity() + ssl.getSession().getPacketBufferSize());
                    nb.put(netOut);
                    nb.flip();
                    netOut = nb;
                    continue;
                }
                flushNetOut();
                if (r.getStatus() == SSLEngineResult.Status.CLOSED) { tlsClosed = true; break; }
                continue;
            }
            if (tlsClosed) { break; }
            netIn.flip();
            SSLEngineResult r;
            try
            {
                r = ssl.unwrap(netIn, appIn);
            }
            finally
            {
                netIn.compact();
            }
            SSLEngineResult.Status st = r.getStatus();
            if (st == SSLEngineResult.Status.BUFFER_UNDERFLOW)
            {
                netInStarved = true;
                if (netIn.remaining() == 0) { growNetIn(); }
                if (readSocket && !eof)
                {
                    int n = ch.read(netIn);
                    if (n > 0) { netInStarved = false; continue; }
                    if (n < 0) { eof = true; }
                }
                break;
            }
            if (st == SSLEngineResult.Status.BUFFER_OVERFLOW)
            {
                if (appIn.position() > 0) { break; }		// let the caller drain first
                ByteBuffer nb = ByteBuffer.allocate(appIn.capacity() + ssl.getSession().getApplicationBufferSize());
                appIn.flip();
                nb.put(appIn);
                appIn = nb;
                continue;
            }
            if (st == SSLEngineResult.Status.CLOSED)
            {
                tlsClosed = true;
                break;
            }
            if (r.bytesConsumed() == 0 && r.bytesProduced() == 0
                && r.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NEED_TASK
                && r.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NEED_WRAP)
            {
                break;
            }
        }
        return (eof || tlsClosed) ? -1 : 0;
    }

    private void growNetIn()
    {
        ByteBuffer nb = ByteBuffer.allocate(netIn.capacity() + ssl.getSession().getPacketBufferSize());
        netIn.flip();
        nb.put(netIn);
        netIn = nb;
    }
}
