package lib;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;

@SuppressWarnings("serial")
public class TextPane extends Canvas implements AppendInterface,SimpleObserver
{
	TextContainer text;
	public TextPane() { this(new TextContainer((CellId)null)); }
	public TextPane(TextContainer t) { text = t; }
	public void setBounds(int l,int t,int w,int h)
	{	super.setBounds(l,t,w,h);
		text.setBounds(l, t, w, h);
		text.addObserver(this);
	}
	public void paint(Graphics g)
	{
		text.redrawBoard(g,null);
	}
	public void setEditable(boolean b) {
		text.setEditable(null,b);
	}
	public void append(String msg) {
		text.append(msg);
	}
	public void update(SimpleObservable o, Object eventType, Object arg) {
		repaint();
	}
	public void setFont(Font f) { text.setFont(f); }
	public void setForeground(Color f) { text.setForeground(f); }
	public void setBackground(Color f) { text.setBackground(f); }
}
