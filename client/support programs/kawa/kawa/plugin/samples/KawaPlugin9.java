import java.util.*;
import com.tektools.kawa.plugin.*;

/** This sample program enumerates all the editors and prints some editor values */
public class KawaPlugin9
{
	public static void main(String[] args)
	{
		KawaApp.out.showWindow(true);
		KawaApp.out.clearWindow();
		Enumeration enum = KawaApp.enumerateEditors();
		while(enum.hasMoreElements())
		{
			KawaEditor editor = (KawaEditor)enum.nextElement();
			if (editor != null)
			{
				KawaApp.out.println("Path - "+editor.getPath());
				KawaApp.out.println("	Lines - "+editor.getLineCount());
				for (int i=0;i<editor.getLineCount();i++)
				{
					KawaApp.out.println(" "+editor.getLineIndex(i)+"-"+editor.getLineLength(i));					
				}
				editor.gotoLine(10, false);
				KawaApp.out.println(editor.getEditorText());	
			}
		}
	}
}