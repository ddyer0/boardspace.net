package bridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;

import lib.G;
import lib.Plog;
import lib.SocketProxy;
/**
 * this is used in the javascript port.  And is PFA (pretty fucking amazing).
 * I gave claude a cut and paste of codename1's   com.codename1.io.WebSocket
 * and similarly a paste of my SocketProxy class, and it produced this
 * class instantly.  Only a few minor modifications were needed.
 *
 */
public class WebSocket implements SocketProxy
{
    private final Object lock = new Object();
    private final LinkedList<byte[]> queue = new LinkedList<byte[]>();
    private final com.codename1.io.WebSocket sock;
    private boolean open = false;
    private boolean closed = false;
    private IOException failure = null;
    private byte[] current = null;
    private int pos = 0;

    public WebSocket(String server,int port) throws IOException
    {	
        if (!com.codename1.io.WebSocket.isSupported())
        {
            G.Error("WebSocket is not supported");
        }
        String prefix = (lib.Http.getDefaultProtocol().equalsIgnoreCase("https:") ? "wss://":"ws:") ;
        // /gameserver is boilerplate that the server uses to exclude random connections
        String finalurl = prefix + server + ":"+port+"/gameserver";
        Plog.log.addLog("Final websocket "+finalurl);
        sock = com.codename1.io.WebSocket.build(finalurl)
            .onConnect(w -> { synchronized (lock) { open = true; lock.notifyAll(); } })
            .onTextMessage((w, m) -> push(utf8(m)))
            .onBinaryMessage((w, m) -> push(m))
            .onClose((w, code, reason) ->
                { synchronized (lock) { closed = true; lock.notifyAll(); } })
            .onError((w, e) ->
                {
                    synchronized (lock)
                    {
                        failure = new IOException(String.valueOf(e));
                        closed = true;
                        lock.notifyAll();
                    }
                });
        sock.connect(10000);
        waitOpen(12000);
    }

    private static byte[] utf8(String s)
    {
        try { return s.getBytes("UTF-8"); }
        catch (java.io.UnsupportedEncodingException e) { return s.getBytes(); }
    }

    private void push(byte[] data)
    {
        synchronized (lock) { queue.addLast(data); lock.notifyAll(); }
    }

    private void waitOpen(long ms) throws IOException
    {
        long end = System.currentTimeMillis() + ms;
        synchronized (lock)
        {
            while (!open)
            {
                if (failure != null) { throw failure; }
                if (closed) { throw new IOException("closed before open"); }
                long left = end - System.currentTimeMillis();
                if (left <= 0) { throw new IOException("connect timeout"); }
                try { lock.wait(left); }
                catch (InterruptedException e) { throw new IOException("interrupted"); }
            }
        }
    }

    private final InputStream in = new InputStream()
    {
        public int read() throws IOException
        {
            byte[] b = new byte[1];
            return (read(b, 0, 1) < 0) ? -1 : (b[0] & 0xff);
        }

        public int read(byte[] b, int off, int len) throws IOException
        {
            synchronized (lock)
            {
                while (current == null || pos >= current.length)
                {
                    current = null;
                    if (!queue.isEmpty()) { current = queue.removeFirst(); pos = 0; continue; }
                    if (failure != null) { throw failure; }
                    if (closed) { return -1; }
                    try { lock.wait(); }
                    catch (InterruptedException e) { throw new IOException("interrupted"); }
                }
                int n = Math.min(len, current.length - pos);
                System.arraycopy(current, pos, b, off, n);
                pos += n;
                return n;
            }
        }
    };

    private final OutputStream out = new OutputStream()
    {
        public void write(int b) throws IOException
        {
            write(new byte[] { (byte) b }, 0, 1);
        }

        public void write(byte[] b, int off, int len) throws IOException
        {
            if (!isConnected()) { throw new IOException("socket closed"); }
            byte[] copy = new byte[len];
            System.arraycopy(b, off, copy, 0, len);
            try { sock.send(copy); }
            catch (IllegalStateException e) { throw new IOException(e.toString()); }
        }
    };

    public InputStream getInputStream() { return in; }
    public OutputStream getOutputStream() { return out; }
    public boolean isConnected() { synchronized (lock) { return open && !closed; } }
    public void close() throws IOException { sock.close(); }
    public InetAddress getInetAddress() { return null; }
    public InetAddress getLocalAddress() { return null; }
}

