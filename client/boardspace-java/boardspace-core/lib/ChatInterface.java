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
import java.awt.Rectangle;
import java.util.StringTokenizer;

import bridge.Config;
import online.common.exCanvas;

public interface ChatInterface extends Config {
	
    static final String KEYWORD_GOODHINT = "goodhint";
    static final String CHATWIDGET = "chatwidget";

	// note: these "channels" correspond to the server communications IDs for
	// player connections.  They have to be greater than all "real" channels
	// so the sounds can be customized for "real" players vs system announcements.
	// the rest are registered in commonChatApplet to be used to identify chat lines
	static String SIMPLETEXT = "simpletext";

	int KNOCKINTERVAL = 60000;	// minimum time between knocks
	int LASTUCHANNEL = 1000000; //first non-user channel, first in this group
	int BLANKCHANNEL = 1000992;	// unadorned text
	int HINTCHANNEL = 1000994;		// used for game hints
	int LOBBYCHANNEL = 1000995;	// used for general lobby announcements
	int ERRORCHANNEL = 1000996;	// used to present information about errors
	int NEWSCHANNEL = 1000997;		// used to present lobby news
	int GAMECHANNEL = 1000999;		// used to present properties from game review
	int CONTROLCHANNEL = 1002000;	// used for silent control strings
	String chatSoundName = SOUNDPATH + "rchatchimes" + Config.SoundFormat;
	String goodHintSoundName = SOUNDPATH + "goodhint" + Config.SoundFormat;
	String badHintSoundName = SOUNDPATH + "badhint" + Config.SoundFormat;
	String gameSoundName = SOUNDPATH + "rgamechat" + Config.SoundFormat;
    String lobbySoundName = SOUNDPATH + "rlobbychimes" + Config.SoundFormat;
    String knockSoundName=SOUNDPATH + "knock" + Config.SoundFormat;

	String KEYWORD_CHAT = "chat";
	String KEYWORD_CCHAT = "cchat";
	String KEYWORD_QCHAT = "qchat";
	String KEYWORD_PCHAT = "pchat";
	String KEYWORD_PPCHAT = "ppchat";
	String KEYWORD_TMCHAT = "tmchat";
	String KEYWORD_SCHAT = "schat";
	String KEYWORD_PSCHAT = "pschat";
	String KEYWORD_TRANSLATE_CHAT = "tchat";
    static final int CHATSIZELIMIT = 300;		// maximum size of chat string to send
    static final String HintChannel = "Hint";
    static final String GameChannel = "Game";
    static final String NewsChannel =  "News"; //header for news lines
    static final String ErrorChannel = "Error";
    static final String LobbyChannel = "Lobby";
    
	static final int FLOODLIMIT = 3;			// maximum number of over sized strings to transmit
	void postMessage(int lobbychannel, String keywordLobbyChat, String string);
	/**
	 * return true if there is content in the chat that has
	 * not been seen.  This is used to trigger blinking of the
	 * show chat icond
	 * @return
	 */
	boolean hasUnseenContent();
	/**
	 * set the unseen content status
	 * @param v
	 */
	void setHasUnseenContent(boolean v);
	
	/**
	 * this is part of the interface to provide pop-up previews
	 * of the unseen chat content.  Return the time
	 * in milliseconds since something was added to the chat.
	 * @return milliseconds since something was added to the chat
	 */
	long getIdleTime();
	/**
	 * get the unseen content that has not been got yet.  This 
	 * provides a 1 time peek at the most recent unseen content.
	 * @return
	 */
	public String getUnseenContent();
	
	void setVisible(boolean b);

	void setBackgroundColor(Color chatCol);

	void setButtonColor(Color butCol);

	int getX();
	int getY();
	Rectangle getBounds();
	void setBounds(int i, int j, int width, int height);


	String nameField();
	String shortNameField();
	void setNameField(String sf);
	void setShortNameField(String string);

	void addAMessage(String string);

	void clearMessages(boolean b);

	String getMessages();


	void setMuted(boolean b);

	String getUserName(int playerID);

	void removeUser(int playerID);

	boolean resetEventCount();

	void sendAndPostMessage(int gamechannel, String keywordLobbyChat, String string);

	void setConn(ConnectionManager myNetConn);

	void setHideInputField(boolean b);

	SimpleUser setUser(int tempID, String name);

	void setMessage(String newMessage);

	void setSpectator(boolean b);

	String whatISaid();

	void setMyUser(int serverIndex, String publicName);
	
    void PostNews(String showNews);

	void addTo(Container commonPanel);
    void addto(XFrame f);
	void moveToFront();
	boolean isVisible();
	public boolean isWindow();	// return false if this window should be painted directly
	public void redrawBoard(Graphics g,HitPoint p);
	public void MouseDown(HitPoint hp);
	public boolean StopDragging(HitPoint p);
	public void StartDragging(HitPoint hp);
	public void setCanvas(exCanvas can);
	public boolean keyboardUpOrContainsPoint(HitPoint p);
	public HitPoint MouseMotion(int ex0, int ey0, MouseState upcode);
	public boolean doRepeat();
	public boolean embedded();
	public int getHeight();
	public void closeKeyboard();
	public Keyboard getKeyboard();
	public void setAllowPM(boolean v);
	public boolean activelyScrolling();
	public boolean doMouseWheel(int x,int y,double amount);
	public void postHostMessages(String host);
	public void getEncodedContents(StringBuilder b);
	public void setEncodedContents(StringTokenizer contents);
}
