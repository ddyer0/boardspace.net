package bridge;

import com.codename1.ui.Command;
import lib.Image;

public class JButton extends Button 
{	Command command = null;
	public JButton(String label) { super(label); }

	public JButton(Image label) { super(label); }
	
	public JButton(String com,Image image) { super(image); command = new Command(com); }
	
	public Command getCommand() 
	{	if(command!=null) { return(command); } 
		return super.getCommand(); 
	}
	
	public void setVisible(boolean v)
	{
		boolean change = v!=isVisible();
		super.setVisible(v);
		if(change) { repaint(); }
	}
}
