package bridge;

import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Dialog;

public class JOptionPane extends Component {
	public static int INFORMATION_MESSAGE = 0;

	public static void showMessageDialog(Object object, String infoMessage,
			String caption, int iNFORMATION_MESSAGE2) 
	{
	Dialog.show(caption, infoMessage,  "Ok", null);
	}
	public static void showMessageDialog(Object object, Component infoMessage,
			String caption, int iNFORMATION_MESSAGE2) 
	{
	Dialog.show(caption, infoMessage, makeCommands("Ok"));
	}

	public static Command[] makeCommands(String... options)
	{
	Command[]cmds = new Command[options.length];
	for(int i=0;i<options.length;i++) { cmds[i] = new Command(options[i]); }
	return(cmds);
	}
	public static int showOptionDialog(Object parent,String infoMessage,String caption,
	int optiontype,
	int messagetype,
	Object icon,
	String[] options,
	String selectedOption
	)
	{	Command cmds[] = makeCommands(options);
	Command v = Dialog.show(caption, infoMessage, cmds[0],cmds, Dialog.TYPE_INFO,null,0);
	if(v!=null)
	{	for(int i=0;i<cmds.length;i++) { if(cmds[i]==v) { return(i);}}
	}
	return(-1);
	}
	public static String showInputDialog(String message)
	{
	TextArea text = new TextArea(message);
	text.setSingleLineTextArea(true);		// used as a flag to cause the textarea to activate on newline
	Command cmds[] = makeCommands("Ok","Cancel");
	text.okCommand = cmds[0];	
	Command v = Dialog.show(message,text,cmds,Dialog.TYPE_INFO,null);
	if(v==cmds[0])
	{
	String msg = text.getText();
	return(msg.trim());
	}
	return(null);
	}

}