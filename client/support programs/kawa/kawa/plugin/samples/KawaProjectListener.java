import com.tektools.kawa.plugin.*;

/**
 * This a project event listener.
 */
public class KawaProjectListener implements KawaEventListener
{
	public void onEvent(KawaEvent event)
	{
		if(!(event instanceof KawaProjectEvent))
			return;
		KawaProjectEvent ProjectEvent = (KawaProjectEvent)event;
		String outputString = "";
		switch(ProjectEvent.getID())
		{
			case KawaProjectEvent.PROJECT_OPENING:
				outputString = "This is a response to the project opening event.";
				break;
			case KawaProjectEvent.PROJECT_OPENED:
				outputString = "This is a response to the project openened event.";
				break;
			case KawaProjectEvent.PROJECT_CLOSING:
				outputString = "This is a response to the project closing event.";
				break;
			case KawaProjectEvent.PROJECT_CLOSED:
				outputString = "This is a response to the project closed event.";
				break;
			case KawaProjectEvent.PROJECT_BUILD_DONE:
				outputString = "This is a response to the project compile done.";
				break;
		}					
		KawaApp.out.showWindow(true);
		KawaApp.out.println(outputString);
	}
}