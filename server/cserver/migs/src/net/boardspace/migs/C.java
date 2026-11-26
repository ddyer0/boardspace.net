package net.boardspace.migs;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Helpers that reproduce the C server's string handling exactly.
 * <p>
 * All protocol text is handled as ISO-8859-1 Strings, so one char is one byte,
 * and data passes through byte-for-byte the way it did through C char buffers.
 * Where the C code relied on plain (signed) char arithmetic, {@link #sc} gives
 * the signed value.
 */
final class C
{
    private C() { }

    /** called when lsprintf drops output because the buffer was too small (C: fmtbug) */
    static Consumer<String> fmtBugHook = null;

    static byte[] bytes(String s) { return s.getBytes(StandardCharsets.ISO_8859_1); }
    static String str(byte[] b, int off, int len) { return new String(b, off, len, StandardCharsets.ISO_8859_1); }

    /** signed value of a char, as C sees a plain char */
    static int sc(char c) { return (byte) c; }

    static boolean isspace(char c)
    {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == 0x0b || c == 0x0c;
    }

    static boolean isdigit(char c) { return c >= '0' && c <= '9'; }

    /** a C string ends at the first NUL */
    static String cstr(String s)
    {
        int nul = s.indexOf('\0');
        return nul >= 0 ? s.substring(0, nul) : s;
    }

    /** MyStrncpy into a field of the given size: at most size-1 chars, stops at NUL */
    static String trunc(String s, int size)
    {
        if (s == null || size <= 0) { return ""; }
        s = cstr(s);
        return s.length() > size - 1 ? s.substring(0, size - 1) : s;
    }

    /** strlen of a NUL terminated byte buffer */
    static int strlen(byte[] b, int off)
    {
        int i = off;
        while (i < b.length && b[i] != 0) { i++; }
        return i - off;
    }

    /** the NUL terminated string at off in a byte buffer */
    static String cstr(byte[] b, int off)
    {
        if (b == null) { return ""; }
        return str(b, off, strlen(b, off));
    }

    /**
     * MyStrncpy(dest+destOff, src, n): strncpy (copy, zero pad to n) then force
     * dest[destOff+n-1] = 0.  Bytes beyond the end of dest are ignored.
     */
    static void myStrncpy(byte[] dest, int destOff, byte[] src, int srcOff, int n)
    {
        if (n <= 0) { return; }
        int i = 0;
        boolean ended = false;
        for (; i < n; i++)
        {
            int di = destOff + i;
            if (di >= dest.length) { break; }
            byte v = 0;
            if (!ended)
            {
                int si = srcOff + i;
                v = (si < src.length) ? src[si] : 0;
                if (v == 0) { ended = true; }
            }
            dest[di] = v;
        }
        int last = destOff + n - 1;
        if (last >= 0 && last < dest.length) { dest[last] = 0; }
    }

    static String toLowercase(String s)
    {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++)
        {
            char ch = s.charAt(i);
            if (ch >= 'A' && ch <= 'Z') { ch = (char) (ch + ('a' - 'A')); }
            b.append(ch);
        }
        return b.toString();
    }

    /** hashString: djb2 over unsigned chars, 32 bit unsigned (kept in an int) */
    static int hashString(String s)
    {
        int hash = 5381;
        for (int i = 0; i < s.length(); i++)
        {
            int c = s.charAt(i) & 0xff;
            if (c == 0) { break; }
            hash = ((hash << 5) + hash) + c;
        }
        return hash;
    }

    /** hashNString: djb2 over exactly n bytes of a buffer (reads past NULs, like the C) */
    static int hashNString(byte[] b, int n)
    {
        int hash = 5381;
        for (int i = 0; i < n; i++)
        {
            int c = (b != null && i < b.length) ? (b[i] & 0xff) : 0;
            hash = ((hash << 5) + hash) + c;
        }
        return hash;
    }

    static int unsignedMod(int v, int m) { return Integer.remainderUnsigned(v, m); }

    // ------------------------------------------------------------------
    // lsprintf: sprintf with the C server's buffer-size rules (lvsprintf_err)
    // ------------------------------------------------------------------

    /**
     * Format like the C lsprintf(space, buf, fmt, ...).  space is the C buffer
     * size; space <= 0 means unlimited.  When an item does not fit, it is
     * dropped entirely (not truncated) and nothing further is written, exactly
     * as lvsprintf_err does.
     */
    static String lsprintf(int space, String fmt, Object... args)
    {
        StringBuilder out = new StringBuilder();
        long spaceleft = (space <= 0) ? Long.MAX_VALUE : (long) space - 1;
        int ai = 0;
        int i = 0;
        int n = fmt.length();
        while (spaceleft > 0 && i < n)
        {
            char c = fmt.charAt(i++);
            if (c != '%')
            {
                out.append(c);
                spaceleft--;
                continue;
            }
            StringBuilder spec = new StringBuilder("%");
            boolean done = false;
            while (!done)
            {
                if (i >= n) { done = true; break; }
                c = fmt.charAt(i++);
                switch (c)
                {
                case '-': case '+': case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9': case '.': case ' ': case '#':
                    spec.append(c);
                    break;
                case 'h':
                case 'l':
                    break;		// length modifiers mean nothing here
                case '*':
                    spec.append(toLong(ai < args.length ? args[ai++] : null));
                    break;
                case 's':
                {
                    Object a = ai < args.length ? args[ai++] : null;
                    if (a != null)
                    {
                        String str = cstr(a.toString());
                        if (str.length() > spaceleft)
                        {
                            fmtbug(space, fmt);
                            spaceleft = 0;
                        }
                        else
                        {
                            String f = javaFormat(spec + "s", str);
                            out.append(f);
                            spaceleft -= f.length();
                        }
                    }
                    done = true;
                    break;
                }
                case 'd': case 'i': case 'c': case 'u': case 'x': case 'X': case 'o':
                {
                    long v = toLong(ai < args.length ? args[ai++] : null);
                    String f;
                    switch (c)
                    {
                    case 'c': f = javaFormat(spec + "c", (char) (v & 0xff)); break;
                    case 'u': f = javaFormat(spec + "d", Integer.toUnsignedLong((int) v)); break;
                    case 'x': case 'X': case 'o': f = javaFormat(spec.toString() + c, (int) v); break;
                    default: f = javaFormat(spec + "d", (int) v); break;
                    }
                    if (f.length() > spaceleft)
                    {
                        fmtbug(space, fmt);
                        spaceleft = 0;
                    }
                    else
                    {
                        out.append(f);
                        spaceleft -= f.length();
                    }
                    done = true;
                    break;
                }
                case 'f': case 'e': case 'E': case 'g': case 'G':
                {
                    Object a = ai < args.length ? args[ai++] : null;
                    double d = (a instanceof Number) ? ((Number) a).doubleValue() : 0.0;
                    String f = javaFormat(spec.toString() + c, d);
                    if (f.length() > spaceleft) { fmtbug(space, fmt); spaceleft = 0; }
                    else { out.append(f); spaceleft -= f.length(); }
                    done = true;
                    break;
                }
                case 'p': case 'n':
                    if (ai < args.length) { ai++; }
                    done = true;
                    break;
                default:		// includes '%%'
                    if (spaceleft <= 0) { fmtbug(space, fmt); }
                    else { out.append(c); spaceleft--; }
                    done = true;
                    break;
                }
            }
        }
        return out.toString();
    }

    private static void fmtbug(int space, String fmt)
    {
        if (fmtBugHook != null)
        {
            String f = fmt.length() > 29 ? fmt.substring(0, 29) : fmt;
            fmtBugHook.accept("lvsprintf: buffer size " + space + " exceeded, format \"" + f + "\"");
        }
    }

    private static String javaFormat(String spec, Object v)
    {
        try
        {
            return String.format(spec, v);
        }
        catch (java.util.IllegalFormatException e)
        {
            return String.valueOf(v);
        }
    }

    static long toLong(Object o)
    {
        if (o == null) { return 0; }
        if (o instanceof Number) { return ((Number) o).longValue(); }
        if (o instanceof Character) { return (Character) o; }
        if (o instanceof Boolean) { return ((Boolean) o) ? 1 : 0; }
        return 0;
    }

    // ------------------------------------------------------------------
    // sscanf: the subset the server uses (%d %s %*s %n, literals, whitespace)
    // ------------------------------------------------------------------

    /**
     * glibc compatible sscanf subset.  Assigned values go to out in order
     * (%d and %n as Integer, %s as String).  Returns the number of assigned
     * conversions (not counting %n), or -1 if the input ended before the
     * first conversion.
     */
    static int sscanf(String in, String fmt, Object[] out)
    {
        int ip = 0;
        int fp = 0;
        int oi = 0;
        int count = 0;
        int nin = in.length();
        int nfmt = fmt.length();
        while (fp < nfmt)
        {
            char f = fmt.charAt(fp);
            if (isspace(f))
            {
                while (fp < nfmt && isspace(fmt.charAt(fp))) { fp++; }
                while (ip < nin && isspace(in.charAt(ip))) { ip++; }
                continue;
            }
            if (f != '%')
            {
                if (ip >= nin) { return count == 0 ? -1 : count; }
                if (in.charAt(ip) != f) { return count; }
                ip++;
                fp++;
                continue;
            }
            fp++;
            boolean suppress = false;
            if (fp < nfmt && fmt.charAt(fp) == '*') { suppress = true; fp++; }
            if (fp >= nfmt) { break; }
            char conv = fmt.charAt(fp++);
            if (conv == 'n')
            {
                if (!suppress) { out[oi++] = ip; }
                continue;
            }
            while (ip < nin && isspace(in.charAt(ip))) { ip++; }
            if (ip >= nin) { return count == 0 ? -1 : count; }
            if (conv == 'd')
            {
                boolean neg = false;
                if (in.charAt(ip) == '+' || in.charAt(ip) == '-') { neg = in.charAt(ip) == '-'; ip++; }
                int ds = ip;
                long v = 0;
                boolean overflow = false;
                while (ip < nin && isdigit(in.charAt(ip)))
                {
                    if (!overflow)
                    {
                        long nv = v * 10 + (in.charAt(ip) - '0');
                        if (nv < v || v > Long.MAX_VALUE / 10) { overflow = true; v = Long.MAX_VALUE; }
                        else { v = nv; }
                    }
                    ip++;
                }
                if (ip == ds) { return count; }
                if (neg) { v = overflow ? Long.MIN_VALUE : -v; }
                if (!suppress) { out[oi++] = (int) v; count++; }
            }
            else if (conv == 's')
            {
                int st = ip;
                while (ip < nin && !isspace(in.charAt(ip))) { ip++; }
                if (!suppress) { out[oi++] = in.substring(st, ip); count++; }
            }
            else
            {
                break;
            }
        }
        return count;
    }

    // ------------------------------------------------------------------
    // escaping
    // ------------------------------------------------------------------

    static int hexDig(char ch, Runnable complaint)
    {
        if (ch >= 'a' && ch <= 'f') { return ch - 'a' + 10; }
        if (ch >= 'A' && ch <= 'F') { return ch - 'A' + 10; }
        if (ch >= '0' && ch <= '9') { return ch - '0'; }
        if (complaint != null) { complaint.run(); }
        return 0;
    }

    /** unescapeString: \\ -> \, \nnn -> one byte, \#hhhh -> one byte (low 8 bits) */
    static String unescapeString(String message, java.util.function.IntConsumer badHex)
    {
        message = cstr(message);
        StringBuilder out = new StringBuilder(message.length());
        int n = message.length();
        for (int i = 0; i < n; i++)
        {
            int ch = sc(message.charAt(i));
            if (ch == '\\')
            {
                ch = (++i < n) ? sc(message.charAt(i)) : 0;
                if (ch != '\\')
                {
                    if (ch == '#')
                    {
                        char c1 = (++i < n) ? message.charAt(i) : 0;
                        char c2 = (++i < n) ? message.charAt(i) : 0;
                        char c3 = (++i < n) ? message.charAt(i) : 0;
                        char c4 = (++i < n) ? message.charAt(i) : 0;
                        ch = (hex(c1, badHex) << 12) + (hex(c2, badHex) << 8) + (hex(c3, badHex) << 4) + hex(c4, badHex);
                    }
                    else
                    {
                        int hundreds = ch - '0';
                        int tens = ((++i < n) ? sc(message.charAt(i)) : 0) - '0';
                        int ones = ((++i < n) ? sc(message.charAt(i)) : 0) - '0';
                        ch = hundreds * 100 + tens * 10 + ones;
                    }
                }
            }
            out.append((char) (ch & 0xff));
        }
        return cstr(out.toString());
    }

    private static int hex(char c, java.util.function.IntConsumer badHex)
    {
        return hexDig(c, badHex == null ? null : () -> badHex.accept(c));
    }

    /** escapedStringSize over a NUL terminated buffer, including the NUL */
    static int escapedStringSize(byte[] str)
    {
        int i = 1;
        if (str != null)
        {
            for (int k = 0; k < str.length && str[k] != 0; k++)
            {
                int ch = str[k] & 0xff;
                if (ch == '\\') { i += 2; }
                else if (ch < ' ' || ch > 126) { i += 4; }		// everything the client escapes (>=127)
                else { i++; }
            }
        }
        return i;
    }

    /** skipToken, with the C's signed char comparisons, bounded at the end of the buffer */
    private static int skipToken(byte[] b, int p)
    {
        while (p < b.length && b[p] <= ' ') { p++; }		// signed: bytes >127 count as white space
        while (p < b.length && b[p] > ' ') { p++; }
        return p;
    }

    /**
     * escapeAndFilterString(outsize, out, str, filter): escape the NUL terminated
     * game buffer.  With filter, timestamp tokens (",+ tok payload") are removed.
     */
    static String escapeAndFilterString(int outsize, byte[] str, boolean filter)
    {
        StringBuilder out = new StringBuilder();
        int size3 = outsize - 3;
        boolean comma = false;
        if (str != null)
        {
            int p = 0;
            int ch;
            while (p < str.length && (ch = str[p++] & 0xff) != 0 && out.length() < size3)
            {
                if (filter && comma && ch == '+')
                {
                    p = skipToken(str, p);
                    p = skipToken(str, p);
                    ch = (p < str.length) ? (str[p] & 0xff) : 0;
                }
                else if (ch == '\\')
                {
                    out.append('\\').append('\\');
                }
                else if (ch < ' ' || ch > 126)
                {
                    // must cover everything the client escapes (>=127).  Raw bytes
                    // 127-129 used to go out unescaped, and the client rejected the
                    // line because its checksum treats them as unsigned
                    out.append('\\');
                    out.append((char) ('0' + ch / 100));
                    out.append((char) ('0' + (ch % 100) / 10));
                    out.append((char) ('0' + ch % 10));
                }
                else
                {
                    if (ch > ' ') { comma = false; }
                    out.append((char) ch);
                }
                if (ch == ',') { comma = true; }
                if (ch == 0) { break; }
            }
        }
        return out.toString();
    }

    /** skip_escaped_text: offset in the escaped text covering len unescaped chars */
    static int skipEscapedText(String s, int start, int len)
    {
        int n = 0;
        int i = 0;
        while (n < len)
        {
            int idx = start + i++;
            char ch = idx < s.length() ? s.charAt(idx) : 0;
            n++;
            if (ch == '\\')
            {
                int idx2 = start + i++;
                ch = idx2 < s.length() ? s.charAt(idx2) : 0;
                if (ch >= '0' && ch <= '9') { i += 2; }
                else if (ch == '#') { i += 4; }
            }
        }
        return i;
    }
}
