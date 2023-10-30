package com.boardspace;

import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
//
// demonstrates text input disappearing immediately on ios
//
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.List;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.DefaultLookAndFeel;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;


import com.codename1.ui.Graphics;
import com.codename1.ui.Label;

@SuppressWarnings("deprecation")
class BSLookAndFeel extends DefaultLookAndFeel 
{	public BSLookAndFeel(UIManager m) { super(m); }
	
	public Dimension getListPreferredSize(@SuppressWarnings("rawtypes") List l)
	{
		Dimension dim = super.getListPreferredSize(l);
		dim.setWidth(dim.getWidth()+(int)(20*1.333));
		return(dim);
	}

}
class BoxLayout extends com.codename1.ui.layouts.BoxLayout
{
	public BoxLayout(com.codename1.ui.Container c,int form)
		{ super(form);
		  c.setLayout(this);
		}
}

// this is unused as long as we don't reinstate the window based chat

class TextArea extends com.codename1.ui.TextArea 
	implements ActionListener
{	
	public String infoMessage()
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
	    int y = getAbsoluteY()+getScrollY();
		return "vpbHeight "+vkbHeight+"\nHeight = "+h
		+ "\nminY = "+minY
		+ "\nmaxH = "+maxH
		+ "\ny = "+y
		+ "\nabsolute = "+absolute
		+ "\nscroll = "+scroll
		+ "\npad = "+pad
		+ "\ndispH = "+disph
		+ "\nthis = "+this
		+ "\ncontent = "+contentPane;
	}
	public void showInfo(String msg)
	{
        JOptionPane.showMessageDialog(null,
        		infoMessage(),
        		msg, JOptionPane.INFORMATION_MESSAGE
        		);
        		
	}
	public void startEditing() { }
	public void startEditingAsync() { }
	public void pointerReleased(int x ,int y) 
	{ 		
		//showInfo("Dtest before");
		if(isEditable()) 
		{ 
		super.pointerReleased(x,y);  
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

	public TextArea(String m) 
	{ super(m);
	  //addPointerReleasedListener(this);
	  setDoneListener(this);
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

	public TextArea(String string, int i, int j)
	{ 	super(string,i,j);
	}
	public TextArea() 
	{	super();
		getStyle().setOpacity(255);
	}

	public Font myFont = null;
	public void setFont(Font f) 
		{ myFont = f; getStyle().setFont(f); getSelectedStyle().setFont(f);  
		}
	public void selectAll() { System.out.println("TextArea.SelectAll() not implemented");	}
	
	public void setBounds(int l,int t,int w,int h)
	{
		setX(l);
		setY(t);
		setWidth(w);
		setHeight(h);
	}
	public void setWidth(int w)
	{
		super.setWidth(w);
	}
	public void setHeight(int h)
	{
		super.setHeight(h);
	}
	
	public void paint(Graphics g)
	{	// font pops to double size unless this is overridden
		//useProxy=true;
		if(myFont!=null) { setFont(myFont); }
		actualAppend();
		if(setPositionIfNeeded()) { return; }
		
		super.paint(g);
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
		int fontHeight = 20;
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
	// this hard won bit of business allows a text area
	// to activate when you type a newline on the virtual keyboard
	Command okCommand = null;
	public void actionPerformed(ActionEvent evt) {
			
		if(isSingleLineTextArea())	// only if we're a single line
		{
			showInfo("Dtest event");

		if(okCommand!=null)	// and we know what the ok command is.
		{	Form upd = Display.getInstance().getCurrent();		// get the current form
		    upd.dispatchCommand(okCommand,new com.codename1.ui.events.ActionEvent(okCommand));
		}}
	}
}
//

//
// present a messagebox
//

class JOptionPane extends Component {
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
	Dform.input = text;
	text.setSingleLineTextArea(true);		// used as a flag to cause the textarea to activate on newline
	Command cmds[] = makeCommands("Ok","Cancel");
	text.okCommand = cmds[0];	
	Command v = Dialog.show(message,text,cmds,Dialog.TYPE_INFO,null);
	if(v==cmds[0])
	{
	String msg = text.getText();
	return(msg.trim());
	}
	Dform.input = null;
	return(null);
	}

}
class FixedTopLayout extends BorderLayout
{
	int height;
	FixedTopLayout(int h) { height=h;}
	public Dimension getPreferredSize(Component parent) {
		return(new Dimension(parent.getWidth(),(int)(height*1.3333)));
	}
	
}
class Dform extends Form
{	public Dform(String m) 
	{ super(m);
	  new BoxLayout(this,BoxLayout.Y_AXIS);
	  UIManager man = UIManager.getInstance();
	  man.setLookAndFeel(new BSLookAndFeel(man));
	}
	static public TextArea input = null;
	static public String info = "";
}

public class Dtest implements ActionListener
{
	public static Command[] makeCommands(String... options)
	{
	Command[]cmds = new Command[options.length];
	for(int i=0;i<options.length;i++) { cmds[i] = new Command(options[i]); }
	return(cmds);
	}


private Form current;
@SuppressWarnings("unused")
private Resources res;

public void init(Object context) {
    try {
        res = Resources.openLayered("/theme");
        UIManager.getInstance().setThemeProps(res.getTheme(res.getThemeResourceNames()[0]));
    } catch (Throwable e) {
        e.printStackTrace();
    }
	}

public int loops = 0;
public String errorMessage;
public static String keyMessage = "none yet";
public void start() {
    if(current != null){
        current.show();
        return;
    }
    Form hi = new Dform("Hi >>0 World");
     current = hi;
 
    hi.show();
    
	 hi.setScrollable(false);
	 com.codename1.ui.Container titleBar = hi.getTitleArea();
	 titleBar.setLayout(new FixedTopLayout(25));
	 titleBar.add("Center",new Label("Center"));
	 titleBar.add("East",new Label("East"));
	 hi.setLayout(new BorderLayout());
	 
	 Component show = new Component() { 
		 
		 private String[] split(String msg,char ch,int depth)
		    {
		    	int idx = msg.indexOf(ch);
		    	if((idx <= 0) || (idx==msg.length()-1))
		    		{ String res[] = new String[depth+1];
		    		  res[depth] = (idx<=0) ? msg : msg.substring(0,idx);
		    		  return(res);
		    		}
		    	else
		    	{	String [] res = split(msg.substring(idx+1),ch,depth+1);
		    		res[depth] = msg.substring(0,idx);
		        	return(res);
		    	}
		    }

		 public void paint(Graphics g) {
		 int w = getWidth();
		 int h = getHeight();
		 g.setColor(0);
		 g.fillRect(0, 0, w, h);
		 g.setColor(0xffffff);
		 int y = 10;
		 int step = g.getFont().getHeight();
		 String lines[] = split(Dform.info,'\n',0);
		 for(String l : lines)
		 	{
			 g.drawString(l,10,y);
			 y += step;
		 	}
	 	}};
	 	
	 hi.addComponent(BorderLayout.CENTER,show);
	 show.setPreferredSize(new Dimension(1000,1000));
	 hi.setVisible(true);
	 show.setVisible(true);
	 
	Runnable rr = new Runnable (){ 
		public void run() {
			System.out.println("running");
			Runnable r = new Runnable() { public void run() { JOptionPane.showInputDialog("Add a Name"); }};
			new Thread(r).start();
			long nextTime = 0;
			while(true) 
			{ 
			try { Thread.sleep(20); 
			TextArea in =Dform.input;
			if(in!=null && System.currentTimeMillis()>nextTime) 
				{
				Dform.info = in.infoMessage();
				nextTime = System.currentTimeMillis()+2000;
				Container par = show.getParent();
				show.setWidth(par.getWidth());
				show.setHeight(par.getHeight());
				show.repaint();
				}
			}
			catch (InterruptedException e) { };
		     
		}}
	};
		
	new Thread(rr).start();
}
public void stop() {
    current = Display.getInstance().getCurrent();
}

public void destroy() {
	}
@Override
public void actionPerformed(ActionEvent evt) {
	// TODO Auto-generated method stub
	
}
	}
