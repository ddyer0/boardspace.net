package bridge;

import lib.AwtComponent;
import lib.G;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;

public class TextField extends com.codename1.ui.TextField implements AwtComponent
{
	MouseAdapter mouse = new MouseAdapter(this);
	public void addMouseListener(MouseListener m) 
		{ mouse.addMouseListener(m); }
	public void addMouseMotionListener(MouseMotionListener m) 
		{ mouse.addMouseMotionListener(m); }
	public void addActionListener(ActionListener m) 
		{ mouse.addActionListener(m); }

	public Color getBackground() { return(new Color(getStyle().getBgColor())); }
	public Color getForeground() { return(new Color(getStyle().getFgColor())); }
	
	public TextField(int i) {
		super(i);
		setDoneListener(mouse);
	}

	public TextField(String string) {
		super(string);
		setDoneListener(mouse);
	}

	public TextField() { 
		super(); 
		setDoneListener(mouse);
	}
	public TextField(String string, int i) 
	{ super(string,i); 
	  setDoneListener(mouse);
	}
	public void keyPressed(int keycode)
	{	
		if(keycode==-90) // where does this number come from?
		{ 
		  fireDoneEvent(); 
		} 
	}

	public void setBackground(Color color) { getStyle().setBgColor(color.getRGB()); }
	public void setForeground(Color color) { getStyle().setFgColor(color.getRGB()); }
	public Font getFont() { return(G.getFont(getStyle())); }
	public void setFont(Font f) { getStyle().setFont(f); getSelectedStyle().setFont(f); }

	private String actionCommand = "Done";
	public void setActionCommand(String oK) {
		//G.print("setaction "+this+" ("+oK+")");
		actionCommand = oK;
	}
	public String getCommand() { return(actionCommand); }

	public void setBounds(int i, int top, int j, int textHeight) {
		setX(i); 
		setY(top);
		setWidth(j);
		setHeight(textHeight);
	}
	public void requestFocusInWindow() {
	}

	public void repaint() 
	{ 	if(MasterForm.isInFront(this))
		{
		  super.repaint();
		} 
	}
	private String pendingText = null;
	public String getText() 
	{ if(pendingText!=null) 
		{ return(pendingText); } 
		else { return(super.getText()); }
	}
	public void setText(String s)
	{
		pendingText = (s==null)?"":s;
		repaint();
	}
	public void paint(Graphics g)
	{	{ String p = pendingText; if(p!=null) { pendingText=null; super.setText(p); }} 
		if(MasterForm.isInFront(this))
		{	g.setFont(getFont());
			super.paint(g);
		}
	}
	public FontMetrics getFontMetrics(Font f) {
		return G.getFontMetrics(f);
	}
}
