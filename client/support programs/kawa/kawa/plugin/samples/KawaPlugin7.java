import java.util.*;
import com.tektools.kawa.plugin.*;

/** Creates a new project and adds files to the project. */
public class KawaPlugin7
{
	public static void main(String[] args)
	{
		KawaApp.out.showWindow(true);
		KawaApp.out.clearWindow();
		KawaProject proj=KawaApp.openProject("c:\\temp\\plugin.kawa", true);
		if (proj != null)
		{
			proj.addFile("c:\\temp\\InputTest.java");
			proj.setCompilerCustomOption("-deprecated");
			proj.rebuildFolder();
		}
	}
}