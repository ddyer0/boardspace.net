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

import lib.AppendInterface;
import lib.AwtComponent;
import lib.G;
import lib.Http;
import lib.ShellProtocol;
import lib.TextPrintStream;

import com.codename1.ui.Command;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;


// this is unused as long as we don't reinstate the window based chat

public class TextArea extends com.codename1.ui.TextArea 
	implements ActionProvider,AwtComponent,AppendInterface,ActionListener
{	
	public Color getBackground() { return(new Color(getStyle().getBgColor())); }
	public Color getForeground() { return(new Color(getStyle().getFgColor())); }
	public void startEditing() { }
	public void startEditingAsync() { }
	/*
	public void showInfo(String msg)
	{
        Form form = Display.getInstance().getCurrent();
            Container contentPane = form.getContentPane();
            if (!contentPane.contains(this)) {
                contentPane = form;
            }
        Style contentPaneStyle = contentPane.getStyle();
        int vkbHeight = form.getInvisibleAreaUnderVKB();
        int h = getHeight();
        int scroll = contentPane.getScrollY();
        int absolute = contentPane.getAbsoluteY();
        int pad = contentPaneStyle.getPaddingTop();
        int minY = absolute + scroll + pad;
        int disph = Display.getInstance().getDisplayHeight();
        int maxH = disph - minY - vkbHeight;
        int y = getAbsoluteY() + getScrollY();
        G.infoBox(msg,"vpbHeight "+vkbHeight+"\nHeight = "+h
        		+ "\nminY = "+minY
        		+ "\nmaxH = "+maxH
        		+ "\ny = "+y
        		+ "\nabsolute = "+absolute
        		+ "\nscroll = "+scroll
        		+ "\npad = "+pad
        		+ "\ndispH = "+disph
        		+ "\nthis = "+this
        		+ "\ncontent = "+contentPane
        		
        		);
        		 
	}
	*/
	public void pointerReleased(int x ,int y) 
	{ if(isEditable()) 
		{ 

        //showInfo("Start editing");
 		super.pointerReleased(x,y);  
 		//showInfo("In Editing");

		
		}
	}
	
	// the pendingText object is used as a synchronizer, to 
	// avoid locking the entire component while manipulating its contents
	private StringBuilder pendingText = null;
	// this guarantees that pendingText exists, and returns it to
	// be used as a synchronizer.  This slightly odd convention
	// is used because using a static initializer doesn't work
	private Object pendingTextSync() 
	{ if(pendingText==null) { pendingText=new StringBuilder(); }
	  return(pendingText);
	}
	public Dimension getMinimumSize() { return(new Dimension(100,40)); } 

	MouseAdapter mouse = new MouseAdapter(this);
	public void addActionListener(ActionListener t) { mouse.addActionListener(t); }
	public void addMouseListener(MouseListener m) { mouse.addMouseListener(m); }
	public void addMouseMotionListener(MouseMotionListener m) { mouse.addMouseMotionListener(m); }
	public void setWidth(int w) 
	{ final int fw = w;
		G.runInEdt(new Runnable() { public void run() { actualSetWidth(fw); }});
	}
	private void actualSetWidth(int w) 
	{ try { super.setWidth(w); }
	  catch(Throwable err) { Http.postError(this, "setWidth", err); }
	}
		
	public void setText(String m)
	{	synchronized (pendingTextSync())
		{
		pendingText = new StringBuilder();
		pendingText.append(m);
		}
	   repaint();
	}
	
	public String getText()
	{	synchronized (pendingTextSync())
		{
		return ( (pendingText.length()==0)
				?super.getText()
				:super.getText()+pendingText.toString() );
		}
	}

	public TextArea(String m) 
	{ super(m);
	  //putClientProperty("ios.asyncEditing", Boolean.FALSE);
	  addActionListener(this);
	}
	
	public TextArea(String string, int i, int j)
	{ 	super(string,i,j);
		//putClientProperty("ios.asyncEditing", Boolean.FALSE);
	}
	public TextArea() 
	{	super();
		getStyle().setOpacity(255);
		//putClientProperty("ios.asyncEditing", Boolean.FALSE);
	}
	public void setBackground(Color color) { getStyle().setBgColor(color.getRGB()); }
	public void setForeground(Color color) { getStyle().setFgColor(color.getRGB()); }

	public Font getFont() { return(G.getFont(getStyle())); }
	public Font myFont = null;
	public void setFont(Font f) { myFont = f; getStyle().setFont(f); getSelectedStyle().setFont(f);  }
	public void selectAll() { System.out.println("TextArea.SelectAll() not implemented");	}
	
	public void setBounds(int l,int t,int w,int h)
	{
		setX(l);
		setY(t);
		setWidth(w);
		setHeight(h);
	}

	public void repaint() 
	{ 	if(MasterForm.isInFront(this))
		{ 
		  super.repaint();
		} 
	}
	public void paint(Graphics g)
	{	// font pops to double size unless this is overridden
		//useProxy=true;
		if(MasterForm.isInFront(this))
		{
		
		if(myFont!=null) { setFont(myFont); }
		actualAppend();
		if(setPositionIfNeeded()) { return; }
		
		boolean rotated = MasterForm.rotateNativeCanvas(this,g); 
		super.paint(g);
		if(rotated) { MasterForm.unrotateNativeCanvas(this,g); }
		}
	}
	private int linesToPosition(String text,int pos)
	{	int n = 0;
		int curpos = 0;
		int nextPos = 0;
		do 
		{ nextPos = text.indexOf('\n',curpos);
		  if(nextPos>=0) 
		  	{ curpos = nextPos+1; n++; }
		} while(curpos<=pos && nextPos>=0);
		return(n);
	}
	
	// this is not very satisfactory.  The scroll areas don't auto-scroll
	// when you add text, and they break if you set the scroll position from
	// any non-edt thread.  So we defer the setting until paint.  Then the
	// problem is with auto-scrolling the container due to the new text which
	// does kick in when you select the input area.
	private boolean setPositionNeeded = false;
	private int setPositionRequested = 0;
	public void setCaretPosition(int i) 
	{	
		setPositionNeeded = true;
		setPositionRequested = i;
		//repaint();
	}
	public boolean setPositionIfNeeded()
	{	if(setPositionNeeded)
		{
		setPositionNeeded = false;
		Font f = getFont();
		int fontHeight = f.getHeight();
		int linePos = linesToPosition(getText(),setPositionRequested);
		setScrollY(fontHeight*linePos-getHeight()/2);
		//repaint();
		return(true);
		}
		return(false);
	}
	public void actualAppend()
	{	String message = null;
		synchronized (pendingTextSync())
		{
		int len = pendingText.length();

		if(len>0)
			{message = pendingText.toString();
			pendingText = new StringBuilder();
			super.setText(message);
			}
		}
		if(message!=null)
			{ setCaretPosition(message.length());
			}
	}
	
	public void append(String message)
	{	synchronized (pendingTextSync())
		{
		if(pendingText.length()==0) { pendingText.append(super.getText()); }
		pendingText.append(message);
		}
		repaint();
	}
	public ShellProtocol getPrintStream()
    {	return(new TextPrintStream(new Utf8OutputStream(),this));
    }
	public FontMetrics getFontMetrics(Font f) {
		return G.getFontMetrics(f);
	}
	// this hard won bit of business allows a text area
	// to activate when you type a newline on the virtual keyboard
	Command okCommand = null;
	public void actionPerformed(ActionEvent evt) {
		if(isSingleLineTextArea())	// only if we're a single line
		{
		//showInfo("close editing");
		if(okCommand!=null)	// and we know what the ok command is.
		{	Form upd = Display.getInstance().getCurrent();		// get the current form
		    upd.dispatchCommand(okCommand,new com.codename1.ui.events.ActionEvent(okCommand));
		}}
	}
}
