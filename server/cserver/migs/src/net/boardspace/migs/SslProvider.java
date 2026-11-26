package net.boardspace.migs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Builds the server's SSLContext from PEM files (the same websocketsslkey /
 * websocketsslcert files the C server used, typically from /etc/letsencrypt/live).
 * <p>
 * The context is built once at startup.  After that the main thread never touches
 * the files: the save-games thread, which already does the game cache's disk i/o,
 * calls check() about once a second, and once a day it looks at the files and
 * builds a new context when they have changed (a certificate renewal).  Renewed
 * certificates overlap the old ones by weeks, so a daily check is plenty, but a
 * check must not catch a renewal half done:
 * <ul>
 * <li>files changed in the last few minutes are left to settle, and looked at again
 *     once they have (not a day later).
 * <li>a new context is released only if it passes every check: the key and chain
 *     load, the key matches the certificate, the certificate is within its validity
 *     dates, and the chain verifies against the JVM's trusted CAs, as a client would
 *     check it.  (If the certificate in use doesn't verify, as with a self-signed
 *     test certificate, the new one needn't either.)
 * <li>a rejected version is logged once and retried hourly, in case the files change
 *     again; meanwhile the previous context stays in use.
 * </ul>
 * The main thread only ever reads the current context from an AtomicReference.
 * <p>
 * Supported key formats: PKCS#8 ("BEGIN PRIVATE KEY"), PKCS#1 RSA
 * ("BEGIN RSA PRIVATE KEY") and SEC1 EC ("BEGIN EC PRIVATE KEY"), unencrypted.
 */
final class SslProvider
{
    private final String certFile;
    private final String keyFile;
    private final Migs0 srv;
    private final AtomicReference<SSLContext> current = new AtomicReference<>();

    // the intervals can be overridden with system properties for testing
    static final long CHECK_INTERVAL_MS = Long.getLong("migs.sslcheck", 24L * 60 * 60 * 1000);	// routine checks
    static final long RETRY_INTERVAL_MS = Long.getLong("migs.sslretry", 60L * 60 * 1000);		// after a rejection, or with no context
    static final long SETTLE_MS = Long.getLong("migs.sslsettle", 5L * 60 * 1000);				// files must be this old to be used

    // all of this belongs to whichever thread calls load() and check(): main at
    // startup, then only the save-games thread
    private long certTime = 0, keyTime = 0;					// the files the current context came from
    private boolean currentVerified = false;				// the certificate in use verifies against the trusted CAs
    private boolean rejected = false;
    private long rejectedCertTime = 0, rejectedKeyTime = 0;	// the last version that failed
    private boolean settleLogged = false;
    private long nextCheck = 0;

    SslProvider(Migs0 srv, String certFile, String keyFile)
    {
        this.srv = srv;
        this.certFile = certFile;
        this.keyFile = (keyFile != null && !keyFile.isEmpty()) ? keyFile : certFile;
    }

    SSLContext get() { return current.get(); }

    /** load at startup.  The files needn't have settled; there is nothing else to use. */
    void load()
    {
        loadFiles(new File(certFile).lastModified(), new File(keyFile).lastModified(), System.currentTimeMillis());
    }

    /** called by the save-games thread about once a second.  Usually returns at once. */
    void check()
    {
        long now = System.currentTimeMillis();
        if (now < nextCheck) { return; }
        long ct = new File(certFile).lastModified();		// 0 if missing
        long kt = new File(keyFile).lastModified();
        boolean have = current.get() != null;
        if (have && ct == certTime && kt == keyTime)
        {	// unchanged
            nextCheck = now + CHECK_INTERVAL_MS;
            return;
        }
        if (rejected && ct == rejectedCertTime && kt == rejectedKeyTime)
        {	// the version that was already rejected; wait for it to change
            nextCheck = now + RETRY_INTERVAL_MS;
            return;
        }
        long newest = Math.max(ct, kt);
        if (ct != 0 && kt != 0 && now - newest < SETTLE_MS)
        {	// a renewal may still be writing them
            if (!settleLogged)
            {
                srv.logEntry(srv.mainLog, "[%s] SSL key or certificate changed on disk, waiting for the files to settle\n", Migs0.timestamp());
                settleLogged = true;
            }
            nextCheck = newest + SETTLE_MS + 1000;
            return;
        }
        settleLogged = false;
        loadFiles(ct, kt, now);
    }

    private void loadFiles(long ct, long kt, long now)
    {
        boolean reload = current.get() != null;
        try
        {
            boolean[] verified = new boolean[1];
            SSLContext ctx = build(reload && currentVerified, verified);
            current.set(ctx);
            certTime = ct;
            keyTime = kt;
            currentVerified = verified[0];
            rejected = false;
            nextCheck = now + CHECK_INTERVAL_MS;
            srv.logEntry(srv.mainLog, reload
                ? "[%s] SSL key or certificate changed on disk, SSL context rebuilt from %s and %s\n"
                : "[%s] SSL context built from %s and %s\n",
                Migs0.timestamp(), certFile, keyFile);
        }
        catch (Exception e)
        {
            rejected = true;
            rejectedCertTime = ct;
            rejectedKeyTime = kt;
            nextCheck = now + RETRY_INTERVAL_MS;
            srv.logEntry(srv.mainLog, "[%s] Unable to use TLS key %s / certificate %s: %s\n", Migs0.timestamp(), keyFile, certFile, e.getMessage());
            srv.logEntry(srv.mainLog, reload
                ? "[%s] SSL key or certificate changed but was not usable, still using the previous ones; checking again in %d seconds\n"
                : "[%s] no usable SSL key and certificate, checking again in %d seconds\n",
                Migs0.timestamp(), RETRY_INTERVAL_MS / 1000);
        }
    }

    /**
     * build a context from the files and check it; throws with the reason unless it
     * passes.  If mustVerify, the chain must verify against the trusted CAs;
     * verified[0] says whether it did.
     */
    private SSLContext build(boolean mustVerify, boolean[] verified) throws Exception
    {
        String certPem = new String(Files.readAllBytes(new File(certFile).toPath()), java.nio.charset.StandardCharsets.US_ASCII);
        String keyPem = new String(Files.readAllBytes(new File(keyFile).toPath()), java.nio.charset.StandardCharsets.US_ASCII);

        List<Certificate> chain = new ArrayList<>();
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        for (byte[] der : pemBlocks(certPem, "CERTIFICATE"))
        {
            Collection<? extends Certificate> cs = cf.generateCertificates(new ByteArrayInputStream(der));
            chain.addAll(cs);
        }
        if (chain.isEmpty()) { throw new IOException("no certificates in " + certFile); }

        PrivateKey key = readKey(keyPem);
        checkKeyMatches(key, chain.get(0));
        X509Certificate leaf = (X509Certificate) chain.get(0);
        try
        {
            leaf.checkValidity();
        }
        catch (CertificateExpiredException e)
        {
            throw new IOException("certificate has expired");
        }
        catch (CertificateNotYetValidException e)
        {
            throw new IOException("certificate is not valid yet");
        }
        String why = verifyChain(chain);
        verified[0] = why == null;
        if (mustVerify && why != null) { throw new IOException("certificate does not verify: " + why); }
        if (why != null)
        {
            srv.logEntry(srv.mainLog, "[%s] Warning: certificate %s does not verify against the trusted CAs (%s); renewals won't be required to\n",
                Migs0.timestamp(), certFile, why);
        }

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        char[] pw = "migs".toCharArray();
        ks.setKeyEntry("server", key, pw, chain.toArray(new Certificate[0]));
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, pw);
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), null, null);
        return ctx;
    }

    /** null if the chain verifies against the JVM's trusted CAs, else the reason */
    private static String verifyChain(List<Certificate> chain)
    {
        try
        {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);		// the JVM's default trust store
            Set<TrustAnchor> anchors = new HashSet<>();
            for (TrustManager tm : tmf.getTrustManagers())
            {
                if (tm instanceof X509TrustManager)
                {
                    for (X509Certificate ca : ((X509TrustManager) tm).getAcceptedIssuers()) { anchors.add(new TrustAnchor(ca, null)); }
                }
            }
            // the path excludes a trust anchor the file may include at the end
            List<Certificate> path = new ArrayList<>(chain);
            while (path.size() > 1)
            {
                X509Certificate last = (X509Certificate) path.get(path.size() - 1);
                if (!last.getSubjectX500Principal().equals(last.getIssuerX500Principal())) { break; }
                path.remove(path.size() - 1);
            }
            PKIXParameters params = new PKIXParameters(anchors);
            params.setRevocationEnabled(false);
            CertPathValidator.getInstance("PKIX").validate(CertificateFactory.getInstance("X.509").generateCertPath(path), params);
            return null;
        }
        catch (Exception e)
        {
            return e.getMessage();
        }
    }

    /** a renewal can briefly leave a new certificate with the old key; refuse that pair */
    private static void checkKeyMatches(PrivateKey key, Certificate cert) throws Exception
    {
        String alg;
        switch (key.getAlgorithm())
        {
        case "RSA": alg = "SHA256withRSA"; break;
        case "EC": alg = "SHA256withECDSA"; break;
        case "Ed25519":
        case "EdDSA": alg = "Ed25519"; break;
        default: return;		// can't check, let the handshake find out
        }
        byte[] probe = "migs key check".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        java.security.Signature sig = java.security.Signature.getInstance(alg);
        sig.initSign(key);
        sig.update(probe);
        byte[] signed = sig.sign();
        sig.initVerify(cert.getPublicKey());
        sig.update(probe);
        if (!sig.verify(signed)) { throw new IOException("private key " + "does not match the certificate"); }
    }

    private static List<byte[]> pemBlocks(String pem, String type)
    {
        List<byte[]> out = new ArrayList<>();
        Pattern p = Pattern.compile("-----BEGIN " + Pattern.quote(type) + "-----(.*?)-----END " + Pattern.quote(type) + "-----", Pattern.DOTALL);
        Matcher m = p.matcher(pem);
        while (m.find())
        {
            out.add(Base64.getMimeDecoder().decode(m.group(1)));
        }
        return out;
    }

    private static PrivateKey readKey(String pem) throws Exception
    {
        List<byte[]> pk8 = pemBlocks(pem, "PRIVATE KEY");
        if (!pk8.isEmpty()) { return pkcs8(pk8.get(0)); }
        List<byte[]> rsa = pemBlocks(pem, "RSA PRIVATE KEY");
        if (!rsa.isEmpty())
        {
            // PKCS#1 RSA -> PKCS#8: SEQ { INT 0, SEQ { OID rsaEncryption, NULL }, OCTET STRING { pkcs1 } }
            byte[] algId = { 0x30, 0x0d, 0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00 };
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(wrapPkcs8(algId, rsa.get(0))));
        }
        List<byte[]> ec = pemBlocks(pem, "EC PRIVATE KEY");
        if (!ec.isEmpty())
        {
            byte[] curve = sec1CurveOid(ec.get(0));
            // AlgorithmIdentifier: SEQ { OID ecPublicKey, OID curve }
            byte[] ecOid = { 0x06, 0x07, 0x2a, (byte) 0x86, 0x48, (byte) 0xce, 0x3d, 0x02, 0x01 };
            byte[] algId = der(0x30, concat(ecOid, curve));
            return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(wrapPkcs8(algId, ec.get(0))));
        }
        if (pem.contains("ENCRYPTED")) { throw new IOException("encrypted private keys are not supported"); }
        throw new IOException("no private key found");
    }

    private static PrivateKey pkcs8(byte[] der) throws Exception
    {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
        for (String alg : new String[] { "RSA", "EC", "Ed25519", "RSASSA-PSS" })
        {
            try
            {
                return KeyFactory.getInstance(alg).generatePrivate(spec);
            }
            catch (Exception e)
            {
                // try the next algorithm
            }
        }
        throw new IOException("unsupported PKCS#8 key");
    }

    private static byte[] wrapPkcs8(byte[] algId, byte[] keyDer)
    {
        byte[] version = { 0x02, 0x01, 0x00 };
        return der(0x30, concat(version, algId, der(0x04, keyDer)));
    }

    /** find the [0] parameters (curve OID, full TLV) inside a SEC1 ECPrivateKey */
    private static byte[] sec1CurveOid(byte[] sec1) throws IOException
    {
        int[] p = { 0 };
        if ((sec1[p[0]++] & 0xff) != 0x30) { throw new IOException("bad EC key"); }
        int end = readLen(sec1, p) + p[0];
        while (p[0] < end)
        {
            int tag = sec1[p[0]++] & 0xff;
            int len = readLen(sec1, p);
            if (tag == 0xa0)
            {
                byte[] oid = new byte[len];
                System.arraycopy(sec1, p[0], oid, 0, len);
                return oid;
            }
            p[0] += len;
        }
        throw new IOException("EC key has no curve parameters; convert it with: openssl pkcs8 -topk8 -nocrypt");
    }

    private static int readLen(byte[] b, int[] p)
    {
        int l = b[p[0]++] & 0xff;
        if (l < 0x80) { return l; }
        int n = l & 0x7f;
        int v = 0;
        for (int i = 0; i < n; i++) { v = (v << 8) | (b[p[0]++] & 0xff); }
        return v;
    }

    private static byte[] der(int tag, byte[] content)
    {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(tag);
        int len = content.length;
        if (len < 0x80) { o.write(len); }
        else if (len < 0x100) { o.write(0x81); o.write(len); }
        else if (len < 0x10000) { o.write(0x82); o.write(len >> 8); o.write(len & 0xff); }
        else { o.write(0x83); o.write(len >> 16); o.write((len >> 8) & 0xff); o.write(len & 0xff); }
        o.write(content, 0, content.length);
        return o.toByteArray();
    }

    private static byte[] concat(byte[]... parts)
    {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        for (byte[] p : parts) { o.write(p, 0, p.length); }
        return o.toByteArray();
    }
}
