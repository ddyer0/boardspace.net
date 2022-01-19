package lib;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/* cribbed from stackoverflow 
 * 
 * https://myadventuresincoding.wordpress.com/2016/01/02/java-simple-gzip-utility-to-compress-and-decompress-a-string/
 * */
public class GzipUtil {
	static final String UTF_8 = "UTF-8";
	static final public int GZIP_MAGIC = 0x8b1f;
  // note, this works but on codename1 it appears to be 1000x slower than on regular hardware.
  // the culprit appears to be the implementation of GZIPOutputStream
  public static byte[] zip(final String str) {
    if ((str == null) || (str.length() == 0)) {
      throw new IllegalArgumentException("Cannot zip null or empty string");
    }
  
    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
      try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
        gzipOutputStream.write(str.getBytes(UTF_8));
      }
      byteArrayOutputStream.close();
      return byteArrayOutputStream.toByteArray();
    } catch(IOException e) {
      throw new RuntimeException("Failed to zip content", e);
    }
  }
  
  public static String unzip(final byte[] compressed) {
    if ((compressed == null) || (compressed.length == 0)) {
      throw new IllegalArgumentException("Cannot unzip null or empty bytes");
    }
    if (!isZipped(compressed)) {
      return new String(compressed);
    }
    try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressed)) {
      try (GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream)) {
        try (InputStreamReader inputStreamReader = new InputStreamReader(gzipInputStream, UTF_8)) {
          try (BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            StringBuilder output = new StringBuilder();
            String line;
            while((line = bufferedReader.readLine()) != null){
              output.append(line);
            }
            inputStreamReader.close();
            return output.toString();
          }
        }
      }
    } catch(IOException e) {
      throw new RuntimeException("Failed to unzip content", e);
    }
  }
  
  public static boolean isZipped(final byte[] compressed) {
    return (compressed[0] == (byte) (GZIP_MAGIC)) 
           && (compressed[1] == (byte) (GZIP_MAGIC >> 8));
  }
}