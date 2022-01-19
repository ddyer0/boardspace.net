import java.util.*;
import com.tektools.kawa.plugin.*;

/** This command enumerates all the files in each project */
public class KawaPlugin3
{
	public static void main(String[] args)
	{
		KawaApp.build.showWindow(true);
		KawaApp.build.clearWindow();
		for (int i=0;i<args.length;i++)
			KawaApp.build.println("Arg["+i+"] - "+args[i]);
		String[] files = new String[100];	
		int count=0;
		Enumeration enum = KawaApp.enumerateProjects();
		while(enum.hasMoreElements())
		{
			KawaProject proj = (KawaProject)enum.nextElement();			
			if(proj != null)
			{
				if (!proj.isOpen()) continue;
				KawaApp.build.println("Name - "+proj.getName()+" Path - "+proj.getPath());
				// Enumerate all files in the project...
				Enumeration fileEnum = proj.enumerateAllFiles();
				while(fileEnum.hasMoreElements())
				{
					KawaFile file = (KawaFile)fileEnum.nextElement();
					if(file != null)
					{
						KawaApp.build.println("\t\t" + file.getPath() + "(" + file.isDirty() + ")");
						if (proj.isOpen())
						{
							files[count++]=file.getPath();
							KawaApp.build.println("Package - "+file.getPackageName());
							String[] classes = file.getClassNames();
							for (int j=0;classes != null && j<classes.length;j++)
								 KawaApp.build.println("		Class - ["+classes[j]+"]");
							file.setDirty(false);	 
						}
					}
				}
				// Enumerate all folders in the project...
				Enumeration folderEnum = proj.enumerateFolders();
				while(folderEnum.hasMoreElements())
				{
					KawaFolder folder = (KawaFolder)folderEnum.nextElement();
					//enumerateFolder(folder);
				}
			}
		}
		System.out.println("End...");
	}
	
	static protected void enumerateFolder(KawaFolder folder)
	{
		if(folder != null)
		{
			KawaApp.build.println("\tFolder - [" + folder.getName() + "]");
			// Enumerate all files in the folder...
			Enumeration fileEnum = folder.enumerateRootFiles();
			while(fileEnum.hasMoreElements())
			{
				KawaFile file = (KawaFile)fileEnum.nextElement();
				if(file != null)
				{
					KawaApp.build.println("\t\t" + file.getPath());
				}
			}
			// Enumerate all folders in the project...
			Enumeration folderEnum = folder.enumerateFolders();
			while(folderEnum.hasMoreElements())
			{
				KawaFolder fld = (KawaFolder)folderEnum.nextElement();
				enumerateFolder(fld);
			}
		}
	}
}