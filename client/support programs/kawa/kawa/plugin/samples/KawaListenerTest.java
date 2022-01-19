import com.tektools.kawa.plugin.*;

class KawaListenerTest implements KawaEventListener
{
	static public void main(String args[])
	{
		KawaListenerTest test = new KawaListenerTest();
		KawaApp.addListener(test);
		KawaProject.addListener(test);
		KawaFile.addListener(test);
	}
	
	public void onEvent(KawaEvent e)
	{
		if (e instanceof KawaAppEvent)
			KawaApp.println("App event..");
		if (e instanceof KawaFileEvent)
			KawaApp.println("File event..");
		if (e instanceof KawaProjectEvent)
			KawaApp.println("Project event..");
	}
}