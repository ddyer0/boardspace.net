import java.util.*;
import com.tektools.kawa.plugin.*;

/** This sample program enumerates all the projects and prints the project path */
public class KawaPlugin2
{
	public static void main(String[] args)
	{
		KawaApp.out.showWindow(true);
		KawaApp.out.clearWindow();
		Enumeration enum = KawaApp.enumerateProjects();
		//KawaApp.out.println("Projects - ");
		while(enum.hasMoreElements())
		{
			KawaProject proj = (KawaProject)enum.nextElement();
			if(proj != null && proj.isOpen())
			{
				KawaApp.out.println("Name - "+proj.getName()+"Path - "+proj.getPath());
				KawaApp.out.println("Classpath - "+proj.getClasspath(true));
			}
		}
		String str = KawaApp.out.getWindowText();
		KawaApp.out.clearWindow();
		KawaApp.out.println(str);
	}
}