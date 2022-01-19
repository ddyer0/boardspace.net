import com.tektools.kawa.plugin.*;

/** This is a file event listener object */
public class KawaFileListener implements KawaEventListener
{
	public void onEvent(KawaEvent event)
	{
		if(!(event instanceof KawaFileEvent))
			return;
		KawaFileEvent FileEvent = (KawaFileEvent)event;
		String outputString = "";
		switch(FileEvent.getID())
		{
			case KawaFileEvent.FILE_OPENING:
				outputString = "This is a response to the file opening event.";
				break;
			case KawaFileEvent.FILE_OPENED:
				outputString = "This is a response to the file openened event.";
				break;
			case KawaFileEvent.FILE_CLOSING:
				outputString = "This is a response to the file closing event.";
				break;
			case KawaFileEvent.FILE_CLOSED:
				outputString = "This is a response to the file closed event.";
				break;
			case KawaFileEvent.FILE_SAVING:
				outputString = "This is a response to the file saving event.";
				break;
			case KawaFileEvent.FILE_SAVED:
				outputString = "This is a response to the file saved event.";
				break;
		}					
		KawaApp.out.showWindow(true);
		KawaApp.out.println(outputString);
	}
}