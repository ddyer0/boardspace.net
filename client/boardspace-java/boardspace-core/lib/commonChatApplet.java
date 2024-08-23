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
package lib;

import java.net.URL;
import javax.swing.JButton;
import javax.swing.JTextField;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;

import java.awt.event.*;
import bridge.*;

import java.util.StringTokenizer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * this class is the "old school" chat window for boardspace, which uses standard AWT windows and text input.
 * It caches information about player names, and presents chat messages from other players or the system.
 * 
 * It's not currently used by default.
 * 
 * @author ddyer
 *
 */

public class commonChatApplet extends FullscreenPanel
//
// if this is JPanel instead of Panel, windows works fine but macs have blank white areas
// where the chat areas should be.  It's mysterious why.  The chat also seems to never get
// focus on macs.  The paint problem can be fixed by adding explicit chat drawing to exCanvas.paint,
// but that still leaves the focus problem.   The sweet spot seems to be to leave this a "Panel"
// and make commonPanel based on JPanel.
//
	implements ChatInterface,ActionListener,MouseListener,MouseMotionListener,NullLayoutProtocol
{	
	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
	boolean allowPM = true;
	public void setAllowPM(boolean v) { allowPM = v; }

    static private final String MESSAGEPROMPT = "Message:";
	static private final String MESSAGETOPROMPT = "MessageTo:";
	static private final String MESSAGEFROM = "From #1";
	static private final String InitMessage = "Type your message here.";
    private boolean hasInitMessage = false;
    private int floodStrings = 0;
    private int MAXLENGTH = 10000;
	private Color reddishColor = new Color(1.0f,0.7f,0.7f);
	public Color backgroundColor = Color.white;
	public Color foregroundColor = Color.black;
	public Color buttonColor = new Color(0.8f,0.8f,0.8f);
	private boolean hasUnseenContent = false;
	public boolean hasUnseenContent()
	{	return(hasUnseenContent);
	}
	public void setHasUnseenContent(boolean v) { hasUnseenContent =v; }
	UserBank users = new UserBank();
	
    public void setSingleSend(SimpleUser n,boolean temp) 
		{ users.setSingleSend(n,temp);
		  Color bb = (n!=null)
				  ? reddishColor
				  : buttonColor;
		  messageLabel.setBackground(bb);
		  sendButton.setBackground(bb);
		  messageLabel.setText((n!=null) 
		  	? s.get(MESSAGETOPROMPT,n.name()) 
		  	: s.get(MESSAGEPROMPT));
		}
 
    static private final String Spaces = "     ";
    private int MINTEXTHEIGHT = 45;
    private TextArea messages = null;
    // changing this from TextField to JTextField fixed a glitch in the cheerpj presentation
    // and had no apparent bad effects in the Tantrix-java branch.  We don't currently use this at all.
    public  JTextField inputField = null;
    private JTextField nameField = null;
    private JTextField shortNameField = null;
    
    // note on standard java, jbutton renders unicode
    // names correctly, button does not.
    public JButton messageLabel;
    public JButton sendButton;
    
    public ConnectionManager theConn = null;
    private long knockTimer=0;
    //private CheckboxMenuItem seePlayers, seeSpectators, seeHints, seeMice;
    public boolean isSpectator = true;
    private boolean hideInputField = false;
    private Font basicFont;
    public LFrameProtocol theFrame = null;
    private InternationalStrings s = null;
    private int eventcount = 0;
    private int stringCount = 0;
    public boolean muted = false;
    private String prevstr[]={"","",""};
    private int prevStringCount=0;
    private String soundNames[]={ knockSoundName,chatSoundName,ChatInterface.challengeSoundName,lobbySoundName,
    		goodHintSoundName,badHintSoundName,gameSoundName};


	public void setBackgroundColor(Color c)
    {	backgroundColor = c;
    	setBackground(backgroundColor);
    	messages.setBackground(backgroundColor);
    	inputField.setBackground(backgroundColor);
    }
    public void setForegroundColor(Color c)
    {	foregroundColor = c;
    	messages.setForeground(foregroundColor);
    	inputField.setForeground(foregroundColor);
    }
    public void setButtonColor(Color c)
    {	buttonColor = c;
   		messageLabel.setBackground(buttonColor);
    	sendButton.setBackground(buttonColor);
    }

    public commonChatApplet(LFrameProtocol frame,ExtendedHashtable info,boolean emb)
    {	embedded = emb;
        s = G.getTranslations();
        basicFont = G.getFont(s.get("fontfamily"), G.Style.Plain, G.standardizeFontSize(G.defaultFontSize));
        theFrame = frame;
        for(String sn : soundNames) { SoundManager.loadASoundClip(sn); }

        if (G.isUnix()) {  MINTEXTHEIGHT = 20;  }
        else if(G.isCodename1()) { MINTEXTHEIGHT = (int)(35*G.getDisplayScale());  }
        else { MINTEXTHEIGHT = 25; }

        setLayout(new NullLayout(this));
        messages = new TextArea("");
        
        messages.setFont(basicFont);
        messages.setEditable(false);
        messages.setBackground(backgroundColor);
        add(messages);

        inputField = new JTextField("Connection uninitialized.");
        inputField.setFont(basicFont);
        inputField.setEditable(true);
        add(inputField);

        shortNameField = new JTextField("");
        add(shortNameField);
        shortNameField.setEditable(true);
        shortNameField.setVisible(false);

        nameField = new JTextField("");
        add(nameField);
        nameField.setEditable(true);
        nameField.setVisible(false);
        messageLabel = new JButton(s.get(MESSAGEPROMPT));
        messageLabel.setFont(basicFont); /* basicFont */
        messageLabel.setBackground(buttonColor);
        add(messageLabel);
        sendButton = new JButton(s.get("Send"));
        sendButton.setBackground(buttonColor);
        sendButton.setFont(basicFont);
        sendButton.setForeground(Color.black);
        add(sendButton);
        setUser(NEWSCHANNEL, s.get(NewsChannel));
        setUser(LOBBYCHANNEL, s.get(LobbyChannel));
        setUser(ERRORCHANNEL, s.get(ErrorChannel));
        setUser(GAMECHANNEL, s.get(GameChannel));
        setUser(BLANKCHANNEL, "");
        setUser(HINTCHANNEL, s.get(HintChannel));
        setBackground(backgroundColor);
        setForeground(foregroundColor);
  	    knockTimer = G.Date(); 	// no knocks at first
        inputField.addActionListener(this);
        sendButton.addActionListener(this);
        messageLabel.addActionListener(this);
 
        // listen for other kinds of mouse activity, pass on to the panel
        messages.addMouseMotionListener(this);
        messages.addMouseListener(this);
        inputField.addMouseMotionListener(this);
        inputField.addMouseListener(this);
 
  // this works in jdk but not in applets
  //      JMenu edits = new JMenu(s.get(EditAction));
  //      edits.setMnemonic(KeyEvent.VK_E);
  //      JMenuItem menuItem = new JMenuItem(new DefaultEditorKit.CutAction());
  //      menuItem.setText(s.get("Cut"));
  //      menuItem.setMnemonic(KeyEvent.VK_T);
  //      edits.add(menuItem);

  //      menuItem = new JMenuItem(new DefaultEditorKit.CopyAction());
  //      menuItem.setText(s.get("Copy"));
  //      menuItem.setMnemonic(KeyEvent.VK_C);
  //      edits.add(menuItem);
         
  //      menuItem = new JMenuItem(new DefaultEditorKit.PasteAction());
  //      menuItem.setText(s.get("Paste"));
  //      menuItem.setMnemonic(KeyEvent.VK_P);
  //      edits.add(menuItem);
  //      theFrame.addToMenuBar(edits);

    }


    public boolean resetEventCount()
    {
        boolean b = eventcount > 0;
        eventcount = 0;

        return (b);
    }

    public void setFrameBounds(int l,int t,int w,int h)
    	{   int oldw = getWidth();
    		int oldh = getHeight();
    		super.setBounds(l,t,w,h);
    		if((oldw != w) || (oldh !=h))
    		 { setVisible(h>0 && w>0);
    		   revalidate();
    		 }
    	}

    public void setLocalBounds(int l,int t,int inWidth,int inHeight)
    {
        //System.out.println("layout " + inWidth+"x"+inHeight);	
        FontMetrics myFM = G.getFontMetrics(this,basicFont);
        int textHeight = myFM.getAscent() + myFM.getDescent()+4;
        int top = 0;
        if (textHeight < MINTEXTHEIGHT)
        {
            textHeight = MINTEXTHEIGHT;
        }


        if ((nameField!=null) && nameField.isVisible())
        {
        	shortNameField.setBounds(0, 0, inWidth / 4, textHeight);
        	nameField.setBounds(inWidth / 4, 0,
                inWidth - (inWidth / 4), textHeight);
            top += textHeight;
        }

        int bottom = inHeight;

        if (hideInputField)
        {
        	sendButton.setVisible(false);
        	inputField.setVisible(false);
        	messageLabel.setVisible(false);;
        }
        else 
        {	
            int mwidth = myFM.stringWidth("X")*12+40;
            Dimension dim = sendButton.getPreferredSize();
            int bwidth = G.Width(dim);
            bottom = bottom - textHeight;
            sendButton.setBounds( inWidth - bwidth, bottom, bwidth, textHeight);
            inputField.setBounds( mwidth, bottom-1,
                inWidth - bwidth - mwidth, inHeight - bottom);
            messageLabel.setBounds( 2, bottom, mwidth ,
                inHeight - bottom);
           	sendButton.setVisible(true);
        	inputField.setVisible(true);
        	messageLabel.setVisible(true);;
        }


        inputField.setVisible(!hideInputField);
        messages.setBounds(0, top, inWidth, bottom - top-1);
    }

    public void setHideInputField(boolean inVal)
    {
        if (hideInputField != inVal)
        {
            hideInputField = inVal;
            messages.setEditable(inVal);
            repaint();
        }
    }
 

    public void setConn(ConnectionManager inConn)
    {
        theConn = inConn;
        inputField.setText(s.get(InitMessage));
        hasInitMessage = true;
    }

    private synchronized SimpleUser getUser(int inNum)
    {	return(users.getUser(inNum));
    }
    
    public String getUserName(int inNum)
    {	SimpleUser u = getUser(inNum);
    	return((u==null)?null:u.name());
    }
    public synchronized void removeUser(int inNum)
    {	SimpleUser single = users.getToSingleUser();
    	SimpleUser u = users.removeUser(inNum);
    	if(u==single)
			{ setSingleSend(null,false); 
			} 
   }
    public void setMyUser(int inNum,String name)
    {	users.setMyUser(inNum,name);
    }
    public synchronized SimpleUser setUser(int inNum, String inName)
    {	return(users.setUser(inNum, inName));
    }

    public void setSpectator(boolean inState)
    {
        isSpectator = inState;
    }

    public String nameField()
    {
        return (nameField.getText());
    }

    public String shortNameField()
    {
        return (shortNameField.getText());
    }

    public synchronized void setNameField(String ss)
    {
        nameField.setText(ss);
        setNameFields(ss != null);
    }

    public synchronized void setShortNameField(String ss)
    {
        shortNameField.setText(ss);
        setNameFields(ss != null);
    }
    public void setVisible(boolean see)
    {	
    	if(see!=isVisible())
    	{	
    	super.setVisible(see);
    	if(see) 
    		{ hasUnseenContent=false; }
			  //theFrame.revalidate();
    		}
    }
    private void setNameFields(boolean on)
    {
        if (on != shortNameField.isVisible())
        {
        	shortNameField.setVisible( on);
        	nameField.setVisible(on);
        }
    }

    public void clearMessages(boolean editable)
    {
        messages.setText("");
       // messages.setEditable(editable);
    }

    public String getMessages()
    {
        return (messages.getText());
    }
    public void addAMessage(String inStr) { addAMessage(inStr,true); }
    public void addAMessage(String inStr,boolean see)
    {
        FontMetrics myFM = G.getFontMetrics(this,basicFont);

        Dimension sz = getSize();
        int useWidth = G.Width(sz) - 15;

        if (useWidth < 400)
        {
            useWidth = 400;
        }

        AddMessage(s.lineSplit(G.replaceAll(inStr,"\n", " <br> "), myFM, useWidth, Spaces),see);
    }

    // 3/18/2003 added "synchronized" despite misgivings that I may have
    // previously removed it. Humber presented a message log that clearly
    // indicated two message streams had been interleaved, and this is the
    // only way that could have happened.
    public void AddMessage(String newstr,boolean see)
    {
        stringCount++;
        AddMessageInternal(newstr,see);
    }
    private synchronized void AddMessageInternal(String newstr,boolean see)
    {
        try
        {
            if (newstr != null)
            {
            	messages.append(newstr);
               
                String newText = messages.getText();
                
                if (newText.length() > MAXLENGTH)
                    {
                        newText = (newText.substring(MAXLENGTH / 3));
                        // note; there is a deep seated problem with setText on linux
                        // calling setText triggers massive validation of the window hierarchy,
                        // which tends to cause thread lockups.  The best medicine is to avoid
                        // setText.
                        messages.setText(newText);
                     }
             
                messages.setCaretPosition(newText.length()-1);
                if(see) 
                	{hasUnseenContent |= !isVisible();
                	}
             }
        }
    	catch (ThreadDeath err) { throw err;}
        catch (Throwable e)
        {
            G.print("chat addmessage : " + e);
        }
    }

    public void setMessage(String str)
    {
        messages.setText((str == null) ? "" : str);
        //messages.setEditable(true);
    }
    


    public void postMessage(int userNum, String command, String theMessage)
     {	SimpleUser u = getUser(userNum);
     	String name = command.equals(ChatInterface.KEYWORD_LOBBY_CHAT)
     			? s.get("Lobby")
     			: (u==null)
     						? s.get("User ") + userNum
     						: u.name();
     	
        if(KEYWORD_PPCHAT.equals(command) 
        		|| KEYWORD_PSCHAT.equals(command))
        {	name = s.get("From #1",name);
        }
        postMessageWithName(name,userNum<LASTUCHANNEL,command,theMessage,userNum!=HINTCHANNEL);
     }
    public void postMessageWithName(String name,boolean fromRealUser,String command,String theMessage,boolean see)
    {
        boolean itsme = false;
    	// guess if our name is mentioned and maybe knock knock instead of ding ding
        SimpleUser my = users.getMyUser();
    	if((my!=null) && fromRealUser)
    		{
    		 String myName = my.name();
    		 StringTokenizer tok = new StringTokenizer(theMessage);
        	while (tok.hasMoreTokens())
        	{ String atok = tok.nextToken();
        	  if(myName.equalsIgnoreCase(atok)) 
        	  { long now = G.Date();;
    	  	    if((now-knockTimer)>KNOCKINTERVAL) 
    	  	    	{ itsme=true; }
    	  	    knockTimer = now;
        	  }
        	}}
    	//System.out.println("c "+command+" "+theMessage);
        // translate message is asked
        if (command.equals(KEYWORD_TMCHAT))
        {	
        	theMessage = s.getS(theMessage);
        }
        
        if ((theFrame == null) || (theFrame.doSound()) || itsme)
        {	
            if (command.endsWith(KEYWORD_CHAT))
            {
                String clipname = null;
                int clipTime = 500;	// 1/2 second
                //System.out.println(this + " root " + theRoot);
                if (command.equals(KEYWORD_CCHAT))
                {
                    clipname = ChatInterface.challengeSoundName;
                    clipTime = 2000;	// 2 seconds
                }
                else if (KEYWORD_PPCHAT.equals(command) 
                		|| KEYWORD_PSCHAT.equals(command))
                {
                    clipname = itsme ? knockSoundName : gameSoundName;
                }
                else if (command.equals(KEYWORD_QCHAT))
                {
                    /* no clip */
				}
                else
                {	
                    clipname = itsme ? knockSoundName
                    		: (fromRealUser ? chatSoundName : lobbySoundName );
                    
                }

                if (clipname != null)
                {
                	SoundManager.playASoundClip(clipname,clipTime);	
                }
            }
            else if (command.equals(KEYWORD_GOODHINT))
            {
            	SoundManager.playASoundClip(goodHintSoundName);
            }
            else if (command.equals(ChatInterface.KEYWORD_BADHINT))
            {
            	SoundManager.playASoundClip(badHintSoundName);
            }
        }

        addAMessage(name + ("".equals(name) ? "" : ": ") + theMessage,see);
    }

    public void sendAndPostMessage(int channel, String how, String msg)
    {
        if (theConn != null)
        {	SimpleUser toSingleUser = users.getToSingleUser(); 
			boolean priv = (toSingleUser!=null) ;
			StringBuilder base = new StringBuilder(priv 
				? NetConn.SEND_MESSAGE_TO + toSingleUser.channel()+" "
				: NetConn.SEND_GROUP);				
			theConn.na.getLock();
			if(theConn.hasSequence) 		
				{
				 // add sequence number and keep the accounting straight.
				 // note that this is deliberately duplicative and poorly
				 // structured, to make it more likely to trip up hackers
				 // using advanced tools to mess with our communications.
				 theConn.pendingMessage(priv,base);
				}

			theConn.count(1);
			theConn.sendMessage(base + how + " " + msg);
			theConn.na.Unlock();
			
        }

        postMessage(channel, KEYWORD_CHAT, msg);
    }

    public String whatISaid()
    {	int len = prevstr.length;
    	String val="";
    	String sep="";
    	for(int i=0;i<len;i++)
    	{	val += sep+prevstr[i];
    		sep = " / ";
    	}
    	return(val);
    }


    public boolean handleActionEvent(Object target,String command)
    	{
    	PopupManager destMenu = users.getDestMenu();
    	if(destMenu.selectMenuTarget(target))
    	{	setSingleSend((SimpleUser)(destMenu.rawValue),false);
    	}
    	else if(target==messageLabel)
    	{	if(allowPM)
    		{
    		Point loc = messageLabel.getLocation();
    		users.selectDestination(this,this,s.get(MESSAGEPROMPT),G.Left(loc),G.Top(loc));
    	}
    	}
    	else if(hasInitMessage) { inputField.setText(""); hasInitMessage=false; }
    	else if ( ((target == inputField) && !"".equals(command)) || (target == sendButton)) 
    	{
    	String str = inputField.getText();
    	int scount = stringCount;
    	eventcount++;
    	if (str!=null && (str.length()>0)) 
    		{
    		int len = prevstr.length;
    		if ((theConn != null))
    		{
    			if(str.equals(prevstr[len-1]) && (prevStringCount==scount)) {}
    			else if(!muted)
    				{
    				SimpleUser toSingleUser = users.getToSingleUser();
    				boolean priv = (toSingleUser!=null) ;
    				String chatKey = (priv ? (isSpectator? KEYWORD_PSCHAT : KEYWORD_PPCHAT)
    									: (isSpectator? KEYWORD_SCHAT : KEYWORD_PCHAT));
    				StringBuilder base = new StringBuilder(priv 
    								? NetConn.SEND_MESSAGE_TO + toSingleUser.channel()+" "
            						: NetConn.SEND_GROUP);
    				
    				theConn.na.getLock();
    				if(theConn.hasSequence) 		
    					{
    					 theConn.pendingMessage(priv,base);
    					}
     				if(str.length()>CHATSIZELIMIT)
    					{ // prevent children from flooding the lobby
    						str = str.substring(0,CHATSIZELIMIT)+" ..."; 
    						floodStrings++;
    					}
     				if(floodStrings<FLOODLIMIT)
     				{
     				// keep the noisy children from annoying the others. 
     					theConn.count(1);
       				if(!theConn.sendMessage(base 
    						+ chatKey
    						+" "
    						+ str))
       				{ str=s.get(ChatInterface.DisconnectedString,"??");
    				}}
     				theConn.na.Unlock();
    				}
    		}
    		postMessageWithName(messageLabel.getText(),true,
    				isSpectator?KEYWORD_CHAT:KEYWORD_PCHAT,str,true);
    		
    		for(int i=1; i<len; i++) { prevstr[i-1]=prevstr[i]; }
    		prevstr[len-1]= str;
    		prevStringCount=scount;
    		}
    	inputField.setText("");
    	inputField.requestFocus();
    	repaint();
    	return(true);
    	}
    	return(false); 
     }

    public void mouseExited(MouseEvent e)
    {
         // System.out.println("Mouse x");
    }

    public void mouseReleased(MouseEvent e)
    {	
    }

    public void mouseEntered(MouseEvent e)
    {
         //System.out.println("Mouse e");
    }

    public void mouseClicked(MouseEvent e)
    {
         //System.out.println("Mouse c"+inputField.getText());
    }

    public void mousePressed(MouseEvent e)
    { // System.out.println("Mouse p"+inputField.getText());
        handleActionEvent(e.getSource(),"");		// selection events
    }
    public void actionPerformed(ActionEvent e)
    {
        Object target = e.getSource();
        handleActionEvent(target,e.getActionCommand());
    }
    public ShellProtocol getPrintStream()
    {	return(new TextPrintStream(new Utf8OutputStream(),messages));
    }

    public void mouseDragged(MouseEvent e) 
    {	
	}
	public void mouseMoved(MouseEvent e) {
		
	}
	public void mousePinched(PinchEvent e) {
	}
	public void setMuted(boolean b) {
		muted = b;
	}
	public void addTo(Container c) { c.add(this); }
	public void addto(XFrame f) { f.addC(this); }
	public void moveToFront() { G.print("move to front"); }
	
	public void PostNews(String showNews)
    {
        try
        {	
            URL newsu = G.getUrl(showNews, true);
            InputStream fs = newsu.openStream();
            
            if(fs!=null)
            {
            postNewsFrom(fs);
            fs.close();
            }
        }
        catch (IOException err) {}
        catch (SecurityException err)
        { // pro forma catch
        }
    }
	public void postHostMessages(String host)
	{
		UrlResult info = Http.postEncryptedURL(host,Config.getEncryptedURL,"&tagname=messageboard",null);
        if(info.error==null && info.data!=null && info.data.length()>1)
        {	
        	InputStream fs = new ByteArrayInputStream(info.text.getBytes());
        	postNewsFrom(fs);
        }
	}
	private void postNewsFrom(InputStream fs)
	{
		try 
            {
            Utf8Reader fsb = new Utf8Reader(fs);
            do {
            	String line = fsb.readLine();
            	String ss = G.utfDecode(line);
            	if(ss==null) { break; }
                postMessage(NEWSCHANNEL, KEYWORD_QCHAT, ss);
            }  while (true);
            fsb.close();
            }
		catch (IOException err)
		{
         /* System.out.println("news: " + err); */
        }
		finally
			{
            setHasUnseenContent(false);
			}
        
	}
	public boolean keyboardUpOrContainsPoint(HitPoint p) { return(G.pointInRect(p, getX(),getY(),getWidth(),getHeight())); }
	
	public boolean isWindow() { return(true); }
	public void redrawBoard(Graphics g,HitPoint p) {}		// this does nothing, the actual drawing is handled by the window system
	public boolean StopDragging(HitPoint p) { return(false); }
	public void setCanvas(exCanvas can) {}
	public void StartDragging(HitPoint hp) {
	}
	public HitPoint MouseMotion(int ex0, int ey0, MouseState upcode) {
		return null;
	}
	public boolean doRepeat() {
		return false;
	}
	boolean embedded = false;
	public boolean embedded() { return(embedded); }
	public void closeKeyboard() {}
	public Keyboard getKeyboard() { return(null); }
	public boolean activelyScrolling() {
		return false;
	}

	public void doNullLayout() {
		//System.out.println("chat layout");
		setLocalBounds(0,0,getWidth(),getHeight());
	}
	public boolean doMouseWheel(int ex,int ey,double amount)
	{
		// needs to scroll
		return(false);
	}
	
	// these are for the interface to generate pop-up tips
	// that preview the unseen chat content.  Not implemented
	// in this class, since we don't actually use it.  See
	// the implementation inf chatWidget
	public long getIdleTime() {
		return 0;
	}
	public String getUnseenContent() {
		return null;
	}
	public void getEncodedContents(StringBuilder b) {
		b.append(SIMPLETEXT);
		b.append(Base64.encodeSimple(messages.getText()));
	}
	public void setEncodedContents(StringTokenizer contents) {
		String kind = contents.nextToken();
		if(SIMPLETEXT.equals(kind))
		{
		messages.setText(Base64.decodeString(contents.nextToken()));
		}
	}
	public void MouseDown(HitPoint hp) {
		
	}

	public void setSingleUser(String name) {
	  	SimpleUser user =setUser(0,name);
    	setSingleSend(user,false);
    	messageLabel.setText(s.get(MESSAGEFROM,name));		
	}
}