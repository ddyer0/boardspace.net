package net.informaticalibera.cn1.nativelogreader;


/**
 *  <p>
 *  Native Logs Reader</p>
 *  <p>
 *  Usage: first of all, remember to invoke the
 *  {@link #initNativeLogs() initNativeLogs()} method in the init(). After that,
 *  {@link #getNativeLogs() getNativeLogs()} can be used in any point of the app
 *  to retrive the native logs of Android (provided by Logcat) and the native
 *  logs of iOS (written in the stderr and stdout).</p>
 */
public class NativeLogs {

	/**
	 *  Call this method in the init() to initialize the Native Logs Reader
	 */
	public static void initNativeLogs() {
	}

	/**
	 *  Gets the native log of iOS and Android, remember to invoke
	 *  {@link #initNativeLogs() initNativeLogs()} in the init() of the main
	 *  class.
	 * 
	 *  @return the native log, or a warning if the native log isn't available
	 */
	public static String getNativeLogs() {
	}
}
