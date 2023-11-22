package bridge;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JTextField;

import lib.G;
import lib.XFrame;

@SuppressWarnings("serial")
public class XTextField extends JTextField implements MouseListener
{
	public XTextField(int size)
	{
		super(size);
		addMouseListener(this);
	}
	XFrame key = null;
	public void setText(String text)
	{
		super.setText(text);
		key = null;
	}
	public void mouseClicked(MouseEvent e) { }

	public void mousePressed(MouseEvent e) 
	{ if((key==null) && G.defaultUseKeyboard())
	{
		key = G.createInputFrame(this);
	}}
	public void mouseReleased(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) {	}
	
}
