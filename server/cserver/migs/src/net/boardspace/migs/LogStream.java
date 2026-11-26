package net.boardspace.migs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * One log stream (the C fileBuffer): the main log, the chat log or the security log.
 * <p>
 * Entries are appended to an in-memory buffer by any thread; {@link #flush} writes
 * them out and is called only by the logger thread (or synchronously before the
 * threads start, and at exit).  The main thread therefore never waits on disk.
 * Rotation follows the C rules: a new file when the current one exceeds the size
 * limit, is older than LOG_AGE_MINUTES (except for "notflushable" streams), or a
 * rename is requested.
 */
final class LogStream
{
    static final int LBSIZE = Migs0.MAX_INPUT_BUFFER_SIZE * 10;
    static final int LOGSLOP = Migs0.MAX_INPUT_BUFFER_SIZE * 2;
    static final int LOG_AGE_MINUTES = 7 * 24 * 60;

    final String label;
    String logFileName = null;		// null until configured; "none" means stderr
    volatile boolean renameLogFile = false;
    boolean notflushable = false;

    private StringBuilder pending = new StringBuilder();
    private int logBreak = 0;

    // writer side, touched only under writeLock
    private final Object writeLock = new Object();
    private OutputStream stream = null;		// null means stderr
    private long writtenSize = 0;
    private long logStart = 0;

    LogStream(String label) { this.label = label; }

    /** add text to the buffer.  Never blocks on I/O. */
    void add(String text)
    {
        synchronized (this)
        {
            if (pending.length() + LOGSLOP < LBSIZE)
            {
                pending.append(text);
            }
            else
            {
                logBreak++;
            }
        }
    }

    private String takePending()
    {
        synchronized (this)
        {
            if (pending.length() == 0 && logBreak == 0) { return null; }
            String s = pending.toString();
            pending = new StringBuilder();
            if (logBreak > 0)
            {
                s = s + "<<break in log, " + logBreak + " entries missing>>\n";
                logBreak = 0;
            }
            return s;
        }
    }

    /** create or append to the log file (C openLogFile) */
    void open() throws IOException
    {
        synchronized (writeLock)
        {
            if (logFileName != null && !logFileName.equals("none"))
            {
                logStart = Migs0.uptime();
                writtenSize = 0;
                stream = new FileOutputStream(logFileName, true);
            }
        }
    }

    void close()
    {
        synchronized (writeLock)
        {
            if (stream != null)
            {
                try { stream.close(); } catch (IOException e) { /* nothing to do */ }
                stream = null;
            }
        }
    }

    /**
     * write the accumulated log data, then rotate if needed (C flushLog).
     * Called from the logger thread, or before threads start / at exit.
     */
    void flush(long maxLogfileSize, boolean logFilesEnabled)
    {
        synchronized (writeLock)
        {
            String s = takePending();
            if (s == null) { return; }
            byte[] b = C.bytes(s);
            try
            {
                OutputStream os = (stream != null) ? stream : System.err;
                os.write(b);
                os.flush();
            }
            catch (IOException e)
            {
                System.err.println("[" + label + "] log write failed: " + e);
            }
            writtenSize += b.length;
            if (writtenSize > maxLogfileSize) { renameLogFile = true; }

            if (logFilesEnabled)
            {
                if (!notflushable)
                {
                    long upminutes = (Migs0.uptime() - logStart) / 60;
                    if (upminutes > LOG_AGE_MINUTES) { renameLogFile = true; }
                }
                if (renameLogFile && logFileName != null && !logFileName.equals("none"))
                {
                    renameLogFile = false;
                    String stamp = Migs0.timestamp();
                    String newlog;
                    int dot = logFileName.lastIndexOf('.');
                    if (dot >= 0)
                    {
                        newlog = logFileName.substring(0, dot) + "-" + stamp + logFileName.substring(dot);
                    }
                    else
                    {
                        newlog = logFileName + "-" + stamp;
                    }
                    newlog = newlog.replace(':', '-');
                    if (stream != null)
                    {
                        try { stream.close(); } catch (IOException e) { /* ignore */ }
                        stream = null;
                    }
                    if (!new File(logFileName).renameTo(new File(newlog)))
                    {
                        System.err.println("[" + label + "] rename " + logFileName + " to " + newlog + " failed");
                    }
                    try
                    {
                        open();
                    }
                    catch (IOException e)
                    {
                        System.err.println("[" + label + "] can't reopen log file " + logFileName + ": " + e);
                    }
                }
            }
        }
    }

    /** used for the fatal error path before the log file exists */
    static void stderr(String s)
    {
        PrintStream p = System.err;
        p.print(s);
        p.flush();
    }
}
