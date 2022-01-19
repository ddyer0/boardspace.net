import java.util.*;
import com.tektools.kawa.plugin.*;

/** 
 * This class sets the file and project images to checkin or checkout state. Please 
 * recompile the file after changing the value of the variable checkFile from 0 to
 * to 1 or 2.
 */
public class KawaPlugin5
{
	public static void main(String[] args)
	{
		KawaApp.out.clearWindow();
		KawaApp.out.showWindow(true);
		KawaProject project = KawaApp.getCurrentProject();
		if (project != null)
		{
			int status = project.getSCMStatus();
			status++;
			if (status > KawaApp.SCMSTATUS_CHECKEDOUT)
				status=0;
			project.setSCMStatus(status);	
			Enumeration fileEnum = project.enumerateAllFiles();
			while(fileEnum.hasMoreElements())
			{
				KawaFile file = (KawaFile)fileEnum.nextElement();				
				if (file != null)
					file.setSCMStatus(status);
			}
		}
	}
}