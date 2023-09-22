/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.

    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/.
 */
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