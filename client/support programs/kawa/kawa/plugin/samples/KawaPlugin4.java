import java.util.*;
import com.tektools.kawa.plugin.*;

/** This enumerates each project in the Kawa project tree and opens each project after
  *	 s set amount of time
  */
public class KawaPlugin4
{
	public static void main(String[] args)
	{
		KawaApp.out.showWindow(true);
		KawaApp.out.clearWindow();
		Enumeration enum = KawaApp.enumerateProjects();
		int count = 0;
		while(enum.hasMoreElements() && count < 2)
		{
			KawaProject proj = (KawaProject)enum.nextElement();
			if(proj != null && !proj.equals(KawaApp.getCurrentProject()))
			{
				KawaProject openProj = KawaApp.openProject(proj.getPath(), false);
				KawaApp.out.println(openProj.getPath());
				count++;
				try
				{
					Thread.sleep(5000);
				}
				catch(InterruptedException e)
				{
				}
			}
		}
	}
}