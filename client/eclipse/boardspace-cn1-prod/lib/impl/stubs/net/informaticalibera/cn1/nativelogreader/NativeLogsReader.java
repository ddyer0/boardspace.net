package net.informaticalibera.cn1.nativelogreader;


/**
 *  Native Code interface
 */
public interface NativeLogsReader extends com.codename1.system.NativeInterface {

	public void clearAndRestartLog();

	public String readLog();
}
