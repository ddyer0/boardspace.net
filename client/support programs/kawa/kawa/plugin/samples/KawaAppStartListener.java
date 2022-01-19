import com.tektools.kawa.plugin.*;

/** This is a App event listener object */
public class KawaAppStartListener implements KawaEventListener
{
	public void onEvent(KawaEvent event)
	{
		if(!(event instanceof KawaAppEvent))
			return;
		KawaAppEvent AppEvent = (KawaAppEvent)event;
		if(AppEvent.getID() == KawaAppEvent.APP_OPENED)
		{
			KawaApp.out.showWindow(true);
			KawaApp.out.println("This is a response to the app event.");
			AppEvent.consume();
		}
	}
}