import java.util.*;
import com.tektools.kawa.plugin.*;

/** This command is a sample for Kawa editor manipulation */
public class KawaPlugin8
{
	public static void main(String[] args)
	{
		KawaApp.out.showWindow(true);
		KawaApp.out.clearWindow();
		for (int i=0;i<args.length;i++)
			KawaApp.out.println("Arg["+i+"] - "+args[i]);
		String[] files = new String[100];	
		int count=0;
		Enumeration enum = KawaApp.enumerateProjects();
		while (enum.hasMoreElements())
		{
			KawaProject proj = (KawaProject)enum.nextElement();			
			if (proj != null)
			{
				if (!proj.isOpen()) continue;
				KawaApp.build.println("Name - "+proj.getName()+" Path - "+proj.getPath());
				// Enumerate all files in the project...
				Enumeration fileEnum = proj.enumerateAllFiles();
				while(fileEnum.hasMoreElements())
				{
					KawaFile file = (KawaFile)fileEnum.nextElement();
//					if (file != null)
//					{
//						KawaEditor editor = file.getEditor(true);
//						if (editor != null)
//						{
//							editor.setSelect(0, 100);
//							String text = editor.getSelectText();
//							KawaApp.out.println("Start - "+editor.getSelectStart()+"End - "+editor.getSelectEnd());
//							KawaApp.out.println(text);
//							editor.paste("test");
//						}
//						break;
//					}
				}
			}
		}
		KawaEditor editor = new KawaEditor(args[0]);
		editor.open();
	}
}