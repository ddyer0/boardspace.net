import com.tektools.kawa.plugin.*;

/** This class adds the listeners for various Kawa events. This class needs to be added
  * added as a plugin command under Customize and also it needs to be set to auto execute
  * at Kawa launch time so the listeners will be registered
  */
public class KawaListeners
{
	public static void main(String[] args)
	{
		KawaApp.addListener(new KawaAppStartListener());
		KawaProject.addListener(new KawaProjectListener());
		KawaFile.addListener(new KawaFileListener());
	}
}  