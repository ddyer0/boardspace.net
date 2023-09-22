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

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.net.URL;
import java.security.AccessControlException;
import bridge.Config;
import bridge.Utf8OutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.StringTokenizer;


import lib.TextContainer.Op;
import online.common.OnlineConstants;
import online.common.exCanvas;
import online.common.exHashtable;

/* plug-in replacement for commonChatWindow that will not use real windows */

// TODO: change the cursor when hovering over the chat window?
// TODO: make it possible to copy/paste images
// TODO: make chat windows float or pop out so they can be large without encumbering layout of the overall screen
// TODO: make the chat windows for marginally small screens use the "framed" paradigm
//
public class ChatWidget
	implements ChatInterface,OnlineConstants,
	SimpleObserver,ActionListener,MenuParentInterface,FocusListener
{
	enum ChatId implements CellId
	{	InputField,
		NameField,
		ShortNameField,
		SendButton,
		Messages,			// messages from the chat
		Comments,			// comments from the current move
		Pencil,				// toggle input mode
		MessageLabel;
			
	@Override
	public String shortName() {
		return(name());
	}};
	
	Keyboard keyboard = null;
	boolean useKeyboard = G.isCodename1();
	boolean hasFocus = false;
	int flipInterval = 500;
	boolean inputVisible = false;
	
	void changeFocus(boolean has,ChatId id)
	{	hasFocus = has;
		if(id!=null && has)
		{
		inputField.setFocus((id==ChatId.InputField),flipInterval);
		nameField.setFocus((id==ChatId.NameField),flipInterval);
		shortNameField.setFocus((id==ChatId.ShortNameField),flipInterval);
		messages.setFocus((id==ChatId.Messages),flipInterval);
		comments.setFocus((id==ChatId.Comments),flipInterval);
		setActivePane(id); 
		}
	}
	private boolean embedded=false;
	boolean PRESPLIT_LINES = false; 
	public boolean embedded() { return(embedded); }
	boolean allowPM = true;
	public void setAllowPM(boolean v) { allowPM = v; }
	
    static private final String MESSAGEPROMPT = "To ALL:";
	static private final String MESSAGETOPROMPT = "To #1:";
	static private final String MESSAGEFROM = "From #1";
	static private final String SENDPROMPT = "Send";
	static private final String USERPROMPT = "User ";
    static private final String InitMessage = "Type your message here.";
    static private final String EditMessage = "Edit the text";
	public static String[] ChatStrings = 
		{	EditMessage,
			InitMessage,
			MESSAGEPROMPT,
			MESSAGEFROM,
			MESSAGETOPROMPT,
			SENDPROMPT,
			USERPROMPT,
	       	HintChannel,
	        GameChannel,
	        NewsChannel,
	        ErrorChannel,
	        LobbyChannel,
		};
   
    
	public boolean isWindow() { return(false); }	// we're free of the window system
    private InternationalStrings s = null;
    private ExtendedHashtable sharedInfo = null;
    private int floodStrings = 0;
    private int prevStringCount = 0;
    private long knockTimer=0;
    public LFrameProtocol theFrame = null;
    private Font basicFont;
    private Font boldFont;
    static private final String Spaces = "     ";
    private int MAXLENGTH = 10000;
    private int MINTEXTHEIGHT = 30;
	public Color buttonColor = new Color(0.8f,0.8f,0.8f);
	private Color reddishColor = new Color(1.0f,0.7f,0.7f);

	private boolean hasUnseenContent = false;
	private long lastActiveTime = 0;
	private int unseenContentMark = 0;
	
	public boolean hasUnseenContent() {		return hasUnseenContent;	}
	public void setHasUnseenContent(boolean v) { hasUnseenContent = v; }
	
	private int x=0,y=0,w=1,h=1;
	public int getX() {	return x; }
	public int getY() { return y; }
	public Rectangle getBounds() {	return new Rectangle(x,y,w,h); }
	public void setBounds(int i, int j, int width, int height) 
	{	
		x = i;
		y = j;
		w = width;
		h = height;
		setLocalBounds(x,y,w,h);
		setVisible(h>0 && w>0);
	}
	
	
	private boolean visible = false;
	public boolean isVisible() { return visible; }
	public void setVisible(boolean b) { visible = b; }
	
	public Color backgroundColor = Color.white;
	public Color foregroundColor = Color.black;

    public void setForegroundColor(Color c)
    {	foregroundColor = c;
    	messages.setForeground(foregroundColor);
    	comments.setForeground(foregroundColor);
    	inputField.setForeground(foregroundColor);
    }
    public void setButtonColor(Color c)
    {	buttonColor = c;
   		messageLabel.setBackground(buttonColor);
    	sendButton.setBackground(buttonColor);
    	messages.setButtonColor(buttonColor);
    	comments.setButtonColor(buttonColor);
    }
	public void setBackgroundColor(Color chatCol) 
	{	backgroundColor = chatCol;
	    messages.setBackground(backgroundColor);
	    comments.setBackground(backgroundColor);
	    inputField.setBackground(backgroundColor);
	}
	Rectangle pencilRect = new Rectangle();
    TextContainer nameField = new TextContainer(ChatId.NameField);
    TextContainer shortNameField = new TextContainer(ChatId.ShortNameField);
	public String nameField() {	return nameField.getText(); }
	public void setNameField(String sf) { nameField.setText(sf); nameField.setVisible(sf!=null);	}
	public String shortNameField() { return shortNameField.getText(); }
	public void setShortNameField(String string) { shortNameField.setText(string);	shortNameField.setVisible(string!=null);}
	
	private ConnectionManager theConn=null;
	public void setConn(ConnectionManager myNetConn) 
	{ theConn = myNetConn;	
	  inputField.setText(s.get(InitMessage));
	  inputField.clearBeforeAppend = true;
 	}
	

	// the main text area
	boolean messagesOnTop = true;
	TextContainer messages = new TextContainer(ChatId.Messages);
    TextContainer comments = new TextContainer(ChatId.Comments);
    TextContainer activePane() { return(messagesOnTop ? messages : comments); }
    void setActivePane(ChatId id) 
    {	switch(id)
    	{
    	case InputField:
    	case Messages: messagesOnTop = true; break;
    	
    	case NameField:
    	case ShortNameField:
    	case Comments: messagesOnTop = false; break;
    	default: G.Advise(false,"Not expecting %s",id);
    	}
    }

    
    // note on standard java, jbutton renders unicode
    // names correctly, button does not.
    public TextContainer messageLabel = new TextContainer(ChatId.MessageLabel);
    public TextContainer sendButton = new TextContainer(ChatId.SendButton);

	int stringCount = 0;
	
    public void addAMessage(String inStr) { addAMessage(inStr,true); }
    
    public void setMessage(String str)
    {	
    	setHideInputField(true);
        comments.setText((str == null) ? "" : str);
    }

    public void addAMessage(String inStr,boolean see)
    {
    	int mw = G.Width(activePane());
        int useWidth = mw - 15;

        if (useWidth < 400)
        {
            useWidth = 400;
        }
        if(PRESPLIT_LINES)
        {
        // old style, try to break lines before adding them
        FontMetrics myFM = G.getFontMetrics(basicFont);
        AddMessage(s.lineSplit(G.replaceAll(inStr,"\n", " <br> "), myFM, useWidth, Spaces),see);
        }
        else
        {	// new style, allow the textcontainer to do the presentation
        	AddMessage(inStr,see);
 
        }
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
        {	TextContainer field = activePane();
            if (newstr != null)
            {	String oldText = field.getText();
            	int mark = oldText.length();
            	
            	field.append(newstr);
            	field.finishLine();
            	int messageSize = field.messageSize();
                 
                if (messageSize > MAXLENGTH)
                    {	String message = field.getText();
                        String newText = (message.substring(message.length()-2*MAXLENGTH / 3));
                        // note; there is a deep seated problem with setText on linux
                        // calling setText triggers massive validation of the window hierarchy,
                        // which tends to cause thread lockups.  The best medicine is to avoid
                        // setText.
                        field.setText(newText);
                        mark -= mark-newText.length();
                     }
             
                field.doToEnd();
           	 	lastActiveTime = G.Date();
           	 	if(!hasUnseenContent) { unseenContentMark = mark; }
           	 	
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
	public String getMessages() {
		return comments.getText();
	}
	/** clear the main messages window, and set editability */
	public void clearMessages(boolean b) {
		comments.setText("");
		unseenContentMark = 0;
	}

	private TextContainer inputField = new TextContainer(ChatId.InputField);
	private boolean hideInputField = false;
	public void setHideInputField(boolean b) 
		{ 
			boolean oldval = hideInputField;
			hideInputField = b;
			setActivePane(b ? ChatId.Comments : ChatId.Messages);
			if(oldval!=b) { setLocalBounds(x,y,w,h); }
			inputVisible |= !b;
		
		}

	UserBank users = new UserBank();

	private synchronized SimpleUser getUser(int inNum)
    {	return(users.getUser(inNum));
    }
	public String getUserName(int inNum)
	{	SimpleUser u = getUser(inNum);
		return((u==null)?null:u.name());
	}
	public synchronized void removeUser(int inNum)
	{	SimpleUser single = users.toSingleUser;
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
	
    public boolean isSpectator = true;
	public void setSpectator(boolean b) { isSpectator = b; }

	public boolean muted = false;
	public void setMuted(boolean b) { muted = b; }
	
	public int eventcount = 0;
	public boolean resetEventCount()
    {
        boolean b = eventcount > 0;
        eventcount = 0;

        return (b);
    }
  
    /* constructor */
    public ChatWidget (LFrameProtocol frame,ExtendedHashtable shared,boolean emb)
    {	
        //sharedInfo = info;
    	embedded = emb;
    	sharedInfo = shared;
        s = G.getTranslations();
        theFrame = frame;
        basicFont = G.getFont(s.get("fontfamily"), G.Style.Plain, G.standardizeFontSize(G.defaultFontSize));
        setUser(NEWSCHANNEL, s.get(NewsChannel));
        setUser(LOBBYCHANNEL, s.get("Lobby"));
        setUser(ERRORCHANNEL, s.get("Error"));
        setUser(GAMECHANNEL, s.get(GameChannel));
        setUser(BLANKCHANNEL, "");
        setUser(HINTCHANNEL, s.get(HintChannel));

        sendButton.setText(s.get(SENDPROMPT));
        sendButton.setBackground(buttonColor);
        sendButton.setFont(basicFont);
        sendButton.setForeground(Color.black);
        sendButton.renderAsButton = true;
      
        
        messageLabel.setText(s.get(MESSAGEPROMPT));
        messageLabel.setFont(basicFont); /* basicFont */
        messageLabel.setBackground(buttonColor);
        messageLabel.renderAsButton = true;
        
        inputField.singleLine = true;
        inputField.setFont(basicFont);
        nameField.singleLine = true;
        nameField.setFont(basicFont);
       
        shortNameField.singleLine = true;
        shortNameField.setFont(basicFont);
        messages.setFont(basicFont);
        messages.multiLine = true;
        comments.setFont(basicFont);
        comments.multiLine = true;
        inputField.addObserver(this);
        messages.addObserver(this);
        
    }

    public void postMessage(int userNum, String command, String theMessage)
    {	SimpleUser u = getUser(userNum);
    	String name = command.equals(KEYWORD_LOBBY_CHAT)
    			? s.get("Lobby")
    			: (u==null)
    						? s.get(USERPROMPT) + userNum
    						: u.name();
    	
       if(KEYWORD_PPCHAT.equals(command) 
       		|| KEYWORD_PSCHAT.equals(command))
       {	name = s.get(MESSAGEFROM,name);
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
    		 int me = theMessage.indexOf(myName+" ");
    		 if(me>=0)
    		 {
    			long now = G.Date();;
     	  	    if((now-knockTimer)>KNOCKINTERVAL) 
     	  	    	{ itsme=true; }
     	  	    knockTimer = now;
    		 }
        	}
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
                    clipname = challengeSoundName;
                    clipTime = 2000;	// 2 seconds
                }
                else if (KEYWORD_PPCHAT.equals(command) 
                		|| KEYWORD_PSCHAT.equals(command))
                {
                    clipname = itsme ? knockSoundName : gameSoundName;
                }
                else if (command.equals(KEYWORD_QCHAT))
                {
                    /* quiet chat */
				}
                else
                {	
                    clipname = itsme ? knockSoundName
                    		: (fromRealUser ? chatSoundName : lobbySoundName );
                    
                }

                if (clipname != null && theFrame.doSound())
                {
                	SoundManager.playASoundClip(clipname,clipTime);	
                }
            }
            else if (command.equals(KEYWORD_GOODHINT))
            {
            	SoundManager.playASoundClip(goodHintSoundName);
            }
            else if (command.equals(KEYWORD_BADHINT))
            {
            	SoundManager.playASoundClip(badHintSoundName);
            }
        }
        addAMessage(name + ("".equals(name) ? "" : ": ") + theMessage,see);
 
    }

    // this is synchronized so only one "post" process will be running
    // normally, this will only be called from a "newsreader" process
	public synchronized void PostNews(String showNews)
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
    // this is synchronized so only one "post" process will be running
    // normally, this will only be called from a "newsreader" process
	public synchronized void postHostMessages(String host)
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
	
    public ShellProtocol getPrintStream()
    {	return(new TextPrintStream(new Utf8OutputStream(),messages));
    }
    
    private String prevstr[]={"","",""};
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

    public void setLocalBounds(int l,int t,int inWidth,int inHeight)
    {	
        //System.out.println("layout " + inWidth+"x"+inHeight);	
    	int fs = G.standardizeFontSize((int)(G.defaultFontSize*(G.isCodename1()?1.2:1)));
        basicFont = G.getFont(s.get("fontfamily"), G.Style.Plain, fs);
        boldFont = G.getFont(basicFont,G.Style.Bold,fs);

        //G.print("font "+fs+" "+G.defaultFontSize);
        sendButton.setFont(boldFont);
        inputField.setFont(basicFont);
        messageLabel.setFont(boldFont);
        messages.setFont(basicFont);
        comments.setFont(basicFont);
        
        FontMetrics myFM = G.getFontMetrics(basicFont);
        int messageTop = t;
        int textHeight = (int)(myFM.getHeight()*1.8);
        if (textHeight < MINTEXTHEIGHT)
        {
            textHeight = MINTEXTHEIGHT;
        }

        if(hideInputField)
        	{ // if we ever hide the input, set the name and shortname visible.
        	  nameField.setVisible(true);
        	  shortNameField.setVisible(true);
        	}
        
        if (nameField.isVisible())
        {	// if the top bar is visible, size the windows and reduce the chat height.
        	shortNameField.setBounds(x, y, inWidth / 4, textHeight);
            nameField.setFont(basicFont);
            shortNameField.setFont(basicFont);
            int nameWidth = inWidth - (inWidth / 4)-textHeight;
        	int namex = x+inWidth / 4;
        	nameField.setBounds(namex, y, nameWidth, textHeight);
        	G.SetRect(pencilRect,namex+nameWidth,y,textHeight,textHeight);
            messageTop += textHeight;
         }

        int bottom = inHeight;
        if (hideInputField)
        {	// if we have input no input, hide them
        	sendButton.setVisible(false);
        	inputField.setVisible(false);
        	messageLabel.setVisible(false);
        	inputField.setEditable(canvas,false);
        }
        else 
        {	int charW = myFM.stringWidth("X");
            int mwidth = charW*12+40;
            int bwidth = myFM.stringWidth(s.get("Send")+charW*2);
            bottom = bottom - textHeight;
            sendButton.setBounds( x+inWidth - bwidth, y+bottom, bwidth, textHeight);
            inputField.setBounds( x+mwidth, y+bottom-1,
                inWidth - bwidth - mwidth, inHeight - bottom);
            messageLabel.setBounds( x+2, y+bottom, mwidth ,
                inHeight - bottom);
           	sendButton.setVisible(true);
        	inputField.setVisible(true);
        	messageLabel.setVisible(true);
           	inputField.setEditable(canvas,true);
          }


        inputField.setVisible(!hideInputField);
        comments.setVisible(true);
        messages.setVisible(true);
    	comments.setEditable(canvas, true);
    	messages.setEditable(canvas,false);
        messages.setBounds(l, messageTop, inWidth, bottom- (messageTop-t));
        comments.setBounds(l, messageTop, inWidth, bottom- (messageTop-t));
        setActivePane(hideInputField ? ChatId.Comments : ChatId.Messages);
        
        if(keyboard!=null) { keyboard.resizeAndReposition(); }
    }
    
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

	/* below here definitely work in progress */

    public void sendAndPostMessage(int channel, String how, String msg)
    {
        if (theConn != null)
        {	SimpleUser toSingleUser = users.getToSingleUser(); 
			boolean priv = (toSingleUser!=null) ;
			StringBuilder base = new StringBuilder();
			if(priv)
			{
				G.append(base,NetConn.SEND_MESSAGE_TO , toSingleUser.channel()," ");
			}
			else {
				base.append(NetConn.SEND_GROUP);
			}
			theConn.na.getLock();
			if(theConn.hasSequence) 		
				{
				 // add sequence number and keep the accounting straight.
				 // note that this is deliberately duplicative and poorly
				 // structured, to make it more likely to trip up hackers
				 // using advanced tools to mess with our communications.
				 StringBuilder seq = new StringBuilder("x");
				 seq.append(theConn.na.seq++);
				 if(!priv)
				 {
					 @SuppressWarnings("unchecked")
					 Hashtable<String,String>xm = (Hashtable<String,String>)sharedInfo.getObj(exHashtable.MYXM);
					 xm.put(seq.toString(),base.toString());
				 }
				 seq.append(' ');
				 base.insert(0,seq.toString());
				}

			theConn.count(1);
			G.append(base,how," ",msg);		
			theConn.sendMessage(base.toString());
			theConn.na.Unlock();			
        }

        postMessage(channel, KEYWORD_QCHAT, msg);
    }
    
	public void addTo(Container commonPanel) {

	}

	public void addto(XFrame f) {

	}

	public void moveToFront() {

	}
	public boolean keyboardUpOrContainsPoint(HitPoint p)
	{	// if the keyboard is up, capture the mouse all over the window.
		boolean in = (keyboard!=null) || G.pointInRect(p, x,y,w,h);
		return(in);
	}

	public void redrawBoard(Graphics g,HitPoint p)
	{	if(visible)
		{
		if(g!=null) { setHasUnseenContent(false); }
		GC.fillRect(g, backgroundColor,x,y,w,h);
		GC.frameRect(g,Color.black, x, y,w,h);
		GC.setFont(g,basicFont);

		HitPoint ap = p;
		if(keyboard!=null && keyboard.containsPoint(ap)) { ap=null; }
		{
		inputField.redrawBoard(g,ap);
		shortNameField.redrawBoard(g,ap);
		nameField.redrawBoard(g,ap);
		if(nameField.isVisible())
			{ StockArt.Pencil.drawChip(g,canvas,pencilRect,ap,ChatId.Pencil,EditMessage);	
			  if(!hideInputField) 
			  	{ int w = G.Width(pencilRect)/2;
			  	  StockArt.Exmark.drawChip(g, canvas, w,G.Left(pencilRect)+w/2,G.Top(pencilRect)+w/2,null); }
			}
		activePane().redrawBoard(g,ap);
		messageLabel.redrawBoard(g,ap);
		sendButton.redrawBoard(g,ap);
		}}
		if(g==null)
		{
		Keyboard k = getKeyboard();
		if(k!=null)
		{	// this captures the key strokes, the actual drawing will happen last
			k.draw(g, p);
		}}

		// this is something of a mess, redraw sometimes captures the pointer
		// and chat hitcodes leak out to the general population
		if(p!=null && p.hitCode instanceof ChatId) { p.inStandard=true; }
		
	}

	private void sendInput()
	{
       	String str = inputField.getText();
       	if(!inputField.clearBeforeAppend)
       	{
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
    				String base = priv 
    								? NetConn.SEND_MESSAGE_TO + toSingleUser.channel()+" "
            						: NetConn.SEND_GROUP;
    				
    				theConn.na.getLock();
    				if(theConn.hasSequence) 		
    					{
    					 // add sequence number and keep the accounting straight.
    					 // note that this is deliberately duplicative and poorly
    					 // structured, to make it more likely to trip up hackers
    					 // using advanced tools to mess with our communications. 					
    					 String seq = "x"+theConn.na.seq++;
    					 base  = seq + " "+base;	
    					 if(!priv)
    					 {
    						 @SuppressWarnings("unchecked")
							 Hashtable<String,String>xm = (Hashtable<String,String>)sharedInfo.getObj(exHashtable.MYXM);
    						 xm.put(seq,base);
    					 }
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
       				{ str=s.get(DisconnectedString,"??");
    				}}
     				theConn.na.Unlock();
    				}
    		}
    		postMessageWithName(messageLabel.getText(),true,
    				isSpectator?KEYWORD_CHAT:KEYWORD_PCHAT,str,true);
    		
    		for(int i=1; i<len; i++) { prevstr[i-1]=prevstr[i]; }
    		prevstr[len-1]= str;
    		prevStringCount=scount;
    		}}
    	inputField.setText("");
    	canvas.requestFocus(inputField);
    	canvas.repaint();
 	}
	
	private exCanvas canvas = null;
	public void setCanvas(exCanvas can) 
	{ 	
		if(canvas!=can)
			{
			canvas = can; 
			// if using our own keyboard, the standard focus mechanism is irrelevant
			if(!useKeyboard) { canvas.addFocusListener(this); }
			}
	}
	
	// keyboard activates on mouse down 
	public void MouseDown(HitPoint hp)
    {	CellId hc = hp.hitCode;
    	if(hc instanceof CalculatorButton.id)
		{	Keyboard k = getKeyboard();
			if(k!=null)
			{	k.MouseDown(hp);
			}
		}
	}
    public void StartDragging(HitPoint hp)
    {	
    	CellId hc = hp.hitCode;
    	if(hc instanceof CalculatorButton.id)
		{	Keyboard k = getKeyboard();
			if(k!=null)
			{
				k.StartDragging(hp); 
			}
		} 
    }
    private void sendToFields(int ex,int ey,MouseState upcode)
    {	
    	activePane().doMouseMove(ex,ey,upcode);
		nameField.doMouseMove(ex, ey, upcode);
		shortNameField.doMouseMove(ex, ey, upcode);
		inputField.doMouseMove(ex,ey,upcode);
		createKeyboard();		
    }
    private void loseFocus()
    {
    	if(useKeyboard)
    	{
    		inputField.setFocus(false);
    		messages.setFocus(false);
    		comments.setFocus(false);
    		nameField.setFocus(false);
    		shortNameField.setFocus(false);
    	}
    }
    private boolean draggingOutside = false;
    public HitPoint MouseMotion(int ex, int ey,MouseState upcode)
    {	HitPoint p =  new HitPoint(ex, ey,upcode);
		if(keyboardUpOrContainsPoint(p) && !draggingOutside)
		{	
			if(keyboard!=null && keyboard.containsPoint(p))
			{ keyboard.doMouseMove(ex,ey,upcode);
			}
			else {
			boolean drag = (upcode==MouseState.LAST_IS_DRAG);
			p.dragging = drag;
			sendToFields(ex,ey,upcode);
			
			if(drag) { canvas.repaintSprites(); }
			}
			redrawBoard(null,p);
			return canvas.setHighlightPoint(p);
		}
		else 
		{ if(upcode==MouseState.LAST_IS_EXIT) 
			{ sendToFields(ex,ey,upcode);
			}
		  draggingOutside = upcode==MouseState.LAST_IS_DRAG; 
		}
    	return(null);
    }
    public boolean doRepeat()
    {  	return(activePane().doRepeat());
    }
    public void closeKeyboard()
    {
    	Keyboard kb = keyboard;
    	if(kb!=null) { kb.setClosed(); }
    }
    
    public Keyboard getKeyboard() 
    { Keyboard k = keyboard;
      if(k!=null && k.closed) 
      	{ k = keyboard = null; 
      	  loseFocus();
      	}
      return(k); 
    }
    public void createKeyboard()
    {	if(useKeyboard)
    	{
    	keyboard = comments.makeKeyboardIfNeeded(canvas,keyboard);
    	keyboard = messages.makeKeyboardIfNeeded(canvas,keyboard);
		keyboard = nameField.makeKeyboardIfNeeded(canvas,keyboard);
		keyboard = shortNameField.makeKeyboardIfNeeded(canvas,keyboard);
		keyboard = inputField.makeKeyboardIfNeeded(canvas,keyboard);
    	}
    }
	public boolean StopDragging(HitPoint hp)
	{	
		CellId hc = hp.hitCode;
	    if(hc instanceof CalculatorButton.id)
		{	Keyboard k = getKeyboard();
			if(k!=null)
			{
				k.StopDragging(hp); 
			}
			return(true);
		}
		else if(hc instanceof ChatId)
		{

	    	ChatId id = (ChatId)hc;
	    	boolean key = false;
	    	switch(id)
	    	{
	    	case Pencil:
	    		setHideInputField(!hideInputField);
	    		break;
	    	case Comments:
	    		key |= comments.editable();
	    		break;
	    	case Messages:
	    		key |= messages.editable();
	    		break;
	    	case NameField:
	    		key = nameField.editable(); 
	    		break;
	    	case ShortNameField:
	    		key |= shortNameField.editable();
	    		break;
	    	case InputField: 
	    		key |= inputField.editable(); 
	    		break;
	    	case MessageLabel:
	    		if(allowPM)
	    		{
	    		Point loc = messageLabel.getLocation();
	    		users.selectDestination(this,this,s.get(MESSAGEPROMPT),G.Left(loc),G.Top(loc));
	    		}
	    		break;
	    	case SendButton:
	    		sendInput();
	    		break;
	    	default: break;
	    	}	
    		if(key)
    		{
    		changeFocus(true,id);
    		if(useKeyboard) {
    			keyboard = new Keyboard(canvas,inputField);
    		}
    		else 
    		{	canvas.requestFocus(inputField); 
    			canvas.repaint(flipInterval);
    		}}
	    	return(true);
		}
	else
		{	return(false);
		}
	}
	public void update(SimpleObservable o, Object eventType, Object arg) {
		if(canvas!=null)
		{
			if(arg ==Op.Send)
			{
			//G.addLog("send");
			sendInput();
			}
		//G.addLog("repaint");
		//changeFocus(true,windowId(o));
		canvas.repaint(); 
		}
	}
	public ChatId windowId(SimpleObservable o)
	{	Object target = o.getTarget();
		if(target==inputField) { return(ChatId.InputField); }
		if(target==nameField) { return(ChatId.NameField); }
		if(target==shortNameField) { return(ChatId.ShortNameField); }
		if(target==messages) { return(ChatId.Messages); }
		if(target==comments) { return(ChatId.Comments); }
		return(null);
	}
	public void actionPerformed(ActionEvent e) {
	   	PopupManager destMenu = users.getDestMenu();
		Object target = e.getSource();
    	if(destMenu.selectMenuTarget(target))
    	{	setSingleSend((SimpleUser)(destMenu.rawValue),false);
    	}
    	else
    		{ G.print("action performed "+e);
    		}
	}
	public void show(MenuInterface menu, int x, int y) throws AccessControlException {
		canvas.show(menu,x,y);
	}
	public void focusGained(FocusEvent e) 
	{
		changeFocus(true,null); 
		canvas.repaint(flipInterval);
	}
	public void focusLost(FocusEvent e) {
		
		changeFocus(false,null); 
		canvas.repaint(flipInterval);
	}
	public int getHeight() { return(h); }
	public int getWidth() { return(w); }
	public boolean activelyScrolling() {
		return(activePane().activelyScrolling());
	}
	public boolean doMouseWheel(int xx,int yy,double amount)
	{	return activePane().doMouseWheel(xx,yy,amount);
	
	}
	public long getIdleTime() {
		return(G.Date()-lastActiveTime);
	}
	public String getUnseenContent() {
		if(hasUnseenContent())
		{
		String text = activePane().getText();
		int mark = unseenContentMark;
		if(mark<text.length())
			{
			unseenContentMark = text.length();
			return(text.substring(mark));
			}
		}
		
		return(null);
	}
	
	public void getEncodedContents(StringBuilder b) {
		b.append(SIMPLETEXT);
		b.append(" ");
		b.append(Base64.encodeSimple(activePane().getText()));
	}
	/** change the text as shared over the network */
	public void setEncodedContents(StringTokenizer contents) {
		String kind = contents.nextToken();
		if(SIMPLETEXT.equals(kind))
		{
		activePane().setText(Base64.decodeString(contents.nextToken()));
		}
	}

}
