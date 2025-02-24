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
package online.common;

import lib.Graphics;

import com.codename1.ui.Font;
import com.codename1.ui.geom.Rectangle;

import bridge.ActionEvent;
import bridge.ActionListener;
import bridge.Color;
import bridge.FontMetrics;
import bridge.JCheckBoxMenuItem;
import bridge.JMenuItem;
import bridge.JOptionPane;
import bridge.URL;
import common.CommonConfig;
import common.GameInfo;
import common.GameInfoStack;
import common.GameInfo.ES;
import lib.AR;
import lib.Bitset;
import lib.CellId;
import lib.DefaultId;
import lib.DrawableImage;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.GearMenu;
import lib.HitPoint;
import lib.Image;
import lib.ImageStack;
import lib.InternationalStrings;
import lib.Keyboard;
import lib.LFrameProtocol;
import lib.MenuParentInterface;
import lib.MouseState;
import lib.OfflineGames;
import lib.PopupManager;
import lib.Random;
import lib.ScrollArea;
import lib.SeatingChart;
import lib.StockArt;
import lib.TextButton;
import lib.TextContainer;
import lib.TimeControl;
import lib.XFrame;
import lib.commonPanel;
import lib.exCanvas;
import lib.SeatingChart.Seating;
import rpc.RpcReceiver;
import rpc.RpcService;
import rpc.RpcServiceClient;
import udp.UDPService;
import vnc.AuxViewer;
import vnc.VNCService;
import static util.PasswordCollector.VersionMessage;

@SuppressWarnings("serial")
public class SeatingViewer extends exCanvas implements LobbyConstants,MenuParentInterface,ActionListener
{	
	private static final String FAVORITES = "SeatingFavorites";
	private static final String RECENTS = "SeatingRecents";
	private static final String GameTimerMessage = "Game Timer";
	private static final String GameTimerHelpMessage = "Use this device as a game timer";
	private static final int RECENT_LIST_SIZE = 12;
	private Color buttonBackgroundColor = new Color(0.7f,0.7f,0.7f);
	private Color buttonEmptyColor = new Color(0.5f,0.5f,0.5f);
	private Color buttonHighlightColor = new Color(1.0f,0.5f,0.5f);
	private Color buttonSelectedColor = new Color(0.6f,0.6f,0.8f);
	private Color chartEven = new Color(0.7f,0.7f,0.7f);
	private Color chartOdd = new Color(0.65f,0.65f,0.65f);
	private JCheckBoxMenuItem autoDone=null;          		//if on, chat up a storm
	private Rectangle seatingSelectRect = addRect("seating select");
	private Rectangle seatingChart = addRect("seatingChart");
	private Rectangle gameSelectionRect = addRect("gameSelection");
	private Rectangle startStopRect = addRect("StartStop");
	private TextButton onlineButton = addButton(PlayOnlineMessage,SeatId.PlayOnline,PlayOnlineExplanation,buttonHighlightColor, buttonBackgroundColor);
	private TextButton helpRect = addButton(HelpMessage,SeatId.HelpButton,ExplainHelpMessage,
			buttonHighlightColor, buttonBackgroundColor);
	private Rectangle tableNameRect = addRect("TableName");
	private Rectangle versionRect = addRect("version");
	private Rectangle clockRect = addRect("Clock");
	private TextContainer selectedInputField = null;
	private int respawnNewName = 0;
	private TextContainer namefield = (TextContainer)addRect("namefield",new TextContainer(SeatId.TableName));
	private TextContainer newNameField = (TextContainer)addRect("newname",new TextContainer(SeatId.NewName));
	private TextContainer messageArea = new TextContainer(SeatId.MessageArea);
	private TextButton timerMode = new TextButton(GameTimerMessage, SeatId.GameTimer, GameTimerHelpMessage,
			buttonHighlightColor, buttonBackgroundColor, buttonBackgroundColor);
	private GameInfoStack recentGames = new GameInfoStack();
	private GameInfoStack favoriteGames = new GameInfoStack();
	private boolean portrait = false;
	private SeatingChart selectedChart = SeatingChart.defaultPassAndPlay;
	private UserManager users = new UserManager();
	private Session sess = new Session(1);
	private int changeSlot = 0;
	private int firstPlayerIndex = 0;
	private int colorIndex[] = null;
	private boolean changeRecurse = false;
	private GearMenu gearMenu = new GearMenu(this);
	
	enum MainMode { Category(CategoriesMode,SeatId.CategoriesSelected),
					AZ(A_ZMode,SeatId.A_Zselected),
					Recent(RecentMode,SeatId.RecentSelected);
		String title = "";
		SeatId id;
		MainMode(String m,SeatId i)
		{ title = m;
		  id = i;
		}
	};
	private MainMode mainMode = MainMode.Category;
	
	private String selectedCategory = "";
	private String selectedLetter = "*";
	private GameInfo selectedGame = null;
	private GameInfo selectedVariant = null;
	private boolean gameTimerMode = false;
	private boolean needNewLayout = false;
	private int pickedSource = -1;
	private int colorW = -1;
	private boolean square = G.isRealLastGameBoard();
	private PopupManager userMenu = new PopupManager();
	
	enum SeatId implements CellId
	{	
		ChartSelected,
		NameSelected,
		CategoriesSelected,
		A_Zselected,
		SelectCategory,
		SelectLetter,
		SelectGame,
		SelectVariant,
		SelectFirst,
		SelectColor,
		ToggleServer,
		StartButton,
		DiscardButton,
		PlayOnline,
		HelpButton,
		TableName,
		NewName, MessageArea, RecentSelected, GameTimer, Scroll;
	}
	public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	super.init(info,frame);
    	Image icon = Image.getImage(IMAGEPATH+CommonConfig.icon_image_name);
    	frame.setIconAsImage(icon);

        users.loadOfflineUsers();
        painter.drawLockRequired = false;
        sess.setMode(Session.Mode.Review_Mode,true,false);
        sess.setCurrentGame(GameInfo.firstGame,false,true,false);
        if(G.debug()) {
        	GameInfo.putStrings();
        	SeatingChart.putStrings();
        	InternationalStrings.put(SeatingViewer.SeatingStrings);
        	InternationalStrings.put(SeatingViewer.SeatingStringPairs);
        }
        startserver = myFrame.addAction((VNCService.isVNCServer()||RpcService.isRpcServer()) ? "stop server" : "start server",deferredEvents);
        if(G.isCheerpj()) 
        {
        	testswitch = myFrame.addAction("test switch",deferredEvents);
        }
        String name = UDPService.getPlaytableName();
        namefield.setText(name);
        namefield.singleLine = true;
        namefield.setEditable(this,true);
        namefield.setVisible(true);
        autoDone = myFrame.addOption(s.get(AutoDoneEverywhere),Default.getBoolean(Default.autodone),deferredEvents);
        autoDone.setForeground(Color.blue);
        // this starts the servers that listen for connections from side screens
        if(G.debug())
        	{ if(REMOTEVNC) { startVncViewer = myFrame.addAction("start vnc viewer",deferredEvents); }
        	  if(REMOTERPC) { startRpcViewer = myFrame.addAction("start rpc viewer",deferredEvents); }
        	}
        
        favoriteGames.reloadGameList(FAVORITES);
        recentGames.reloadGameList(RECENTS);
        UDPService.stop();
        if(G.isTable() && !G.isCheerpj())
        {
        UDPService.start(true);	// listen for tables
        if(REMOTEVNC) { VNCService.runVNCServer(true); }
        if(REMOTERPC) { RpcService.runRpcServer(true); }
        G.timedWait(this, 200);
        }
        
    }
	public void selectGame(GameInfo g)
	{	if(g!=selectedGame)
		{
		selectedGame = selectedVariant = g;
		colorIndex = null;
		pickedSource = -1;
		if(g!=null)
		{	if(g.colorMap!=null)
		{
			colorIndex = new int[g.colorMap.length];
			for(int i=0;i<colorIndex.length;i++) { colorIndex[i]=i; }
			}
		if(g.variableColorMap) { firstPlayerIndex = 0; }
		}}
		boolean prevMode = gameTimerMode;
		gameTimerMode = (selectedGame==GameInfo.GameTimer);
		if(gameTimerMode && ((selectedChart==null)||(selectedChart==SeatingChart.defaultPassAndPlay)))
		{
			selectedChart = SeatingChart.faceLandscape ;
		}
		needNewLayout = gameTimerMode!=prevMode;

	}
	public void setLocalBounds(int l, int t, int w, int h) 
	{	needNewLayout = false;
		G.SetRect(fullRect,l,t,w,h); 
		// to benefit lastgameboard, don't switch to portrait if the board is nearly square
		portrait = w<(h*0.9);	
		double ratio = Math.abs((double)w/h);
		square = !portrait && ratio>0.9 && ratio<1.1;
		int stripHeight ;
		int fh = G.getFontSize(standardPlainFont());
		int clockHeight = fh*3;
		int clockWidth = fh*25;
		if(portrait)
		{
			stripHeight = w/7;
			G.SetRect(seatingSelectRect, l, t,stripHeight, h-fh*2);
			int gameH = 3*h/5;
			int left = l+stripHeight;
			int margin = stripHeight/4;
			int gamew = w-l-stripHeight-margin;
			G.SetRect(gameSelectionRect, l+stripHeight,t,gamew,gameH);
			int seatingWidth =  w-left-margin;
			G.SetRect(versionRect,l+fh,t+h-fh*3,fh*30,fh*3);
			if(gameTimerMode)
				{
				G.SetRect(seatingChart, left+margin, t+stripHeight,seatingWidth-margin*2, h-t-stripHeight*2);
				}
				else 
				{G.SetRect(seatingChart, left, t+gameH,seatingWidth, h/3-clockHeight);
				}
			int gearLeft = w-margin*3;
			if(gameTimerMode)
			 {
				 G.SetRect(clockRect,G.Left(seatingChart),G.Top(seatingChart),G.Width(seatingChart),clockHeight*2);
			 }
			 else
			 {
			G.SetRect(clockRect,left,G.Bottom(seatingChart),Math.min(clockWidth,seatingWidth),clockHeight);
			 }
			G.SetRect(gearMenu,gearLeft,t+margin,margin*2,margin*2);
			G.SetRect(helpRect,gearLeft-w/4+w/30,t+(int)(stripHeight*0.43),w/5,stripHeight/2);
		}
		else 
		{
		G.SetRect(versionRect,l+fh,t+h-fh*2,w/3,fh*2);
		stripHeight = h/7;
		G.SetRect(seatingSelectRect, l, t, w,stripHeight);
		G.SetRect(gameSelectionRect, l,t+stripHeight,w/2,h-stripHeight);
		G.SetRect(helpRect,G.Right(gameSelectionRect)-w/6,t+(int)(stripHeight*1.4),w/7,stripHeight/2);
		int left = l+w/2+w/40;
		int margin = stripHeight/2;
		int seatingWidth = w-left-margin;
		if(gameTimerMode)
		{
			G.SetRect(seatingChart, l+margin, t+stripHeight+margin, w-l-margin, h-stripHeight-stripHeight/2-margin-clockHeight);
		}
		else
		{
		G.SetRect(seatingChart, left, t+stripHeight+margin, seatingWidth, h-stripHeight-stripHeight/2-margin-clockHeight);
		}
		int gearLeft = w-margin*2;
		int gearTop = t+stripHeight;
		G.SetRect(gearMenu,gearLeft,gearTop,margin,margin);
		if(gameTimerMode)
		{

			 G.SetRect(clockRect,G.Left(seatingChart),G.Top(seatingChart)-clockHeight*2,clockWidth*2,clockHeight*2);
			 
			
		}
		else
		{
		G.SetRect(clockRect,left,G.Bottom(seatingChart),Math.min(clockWidth,seatingWidth),clockHeight);
		}
		}
		int buttonw = w/9;
		int margin = 3;
		int buttonh = stripHeight/2-margin; 
		int buttonspace = buttonw/10;
		int btop = h-buttonh-margin;
		int buttonX = Math.max(G.Right(versionRect),G.Left(seatingChart));
		G.SetRect(onlineButton, buttonX, btop, buttonw, buttonh);
		buttonX += buttonw+buttonspace;
		G.SetRect(startStopRect, buttonX, btop, buttonw, buttonh);
		timerMode.setBounds(w-buttonw,btop,buttonw-margin,buttonh);

		buttonX += buttonw+buttonspace;
		G.SetRect(tableNameRect,buttonX,btop,buttonw,buttonh/3);
		namefield.setBounds(buttonX, btop+buttonh/3+margin, buttonw, buttonh*2/3-margin);
		

        if(keyboard!=null) { keyboard.resizeAndReposition(); }

	}
	public void MouseDown(HitPoint p)
	{	
		if(keyboard!=null) 
			{ keyboard.MouseDown(p);
			  //Plog.log.addLog("Down "+p+" and repaint");
			  repaint();
			}			
	}
	public HitPoint MouseMotion(int eventX, int eventY,MouseState upcode)
	{
		HitPoint p = super.MouseMotion(eventX, eventY, upcode);
		if(keyboard!=null && keyboard.containsPoint(eventX,eventY))
		{	
		keyboard.doMouseMove(eventX,eventY,upcode);
		}
		else if(messageArea.isVisible())
		{
			messageArea.doMouseMove(eventX,eventY,upcode);
		}
		else if(selectedInputField!=null)
		{ selectedInputField.doMouseMove(eventX, eventY, upcode); 
		  p.dragging = upcode==MouseState.LAST_IS_DRAG;
		} 
		
		
		//if(upcode==MouseState.LAST_IS_DOWN && p!=null) { StartDragging(p); }
		repaint(10,"mouse motion");
		return(p);
	}	

	@Override
	public void StartDragging(HitPoint hp) {
		CellId hitCode = hp.hitCode;
		if(hitCode instanceof SeatId)
		 {
			 SeatId hitId = (SeatId)hitCode;
			 switch(hitId)
			 {
			 default: break;
			 case SelectColor:
				 if(pickedSource<0)
				 	{	pickedSource = hp.row; 
				 		hp.dragging = true;
				 	}

			 }
		
	}
	}
	public void prepareLaunch()
	{
		sess.password = "start";
		int seedValue = sess.seedValue = new Random().nextInt();
		sess.seatingChart = selectedChart;
		int nseats = sess.startingNplayers = selectedChart.getNSeats();
		LaunchUserStack lusers = new LaunchUserStack();

		if(sess.players[0]==null) { sess.players[0]=users.primaryUser(); }

		for(int i=0;i<nseats;i++)
		{	lusers.addUser(sess.players[i],i,i);
		}
		
		sess.selectedFirstPlayerIndex = selectedVariant.randomizeFirstPlayer 
					? new Random(seedValue).nextInt(Math.max(nseats,1))
					: firstPlayerIndex;
		sess.launchUser = sess.startingPlayer = lusers.size()>0 ? lusers.elementAt(0) : null;
		sess.launchUsers = lusers.toArray();	 
		sess.setCurrentGame(selectedVariant, false,true,false);
		sess.startingName = sess.launchName(null,true);
	}
		
	
	@Override
	public void StopDragging(HitPoint hp) {
		CellId hitCode = hp.hitCode;
		if(hitCode==DefaultId.HitNoWhere) { pickedSource = -1; }
		if(performStandardButtons(hitCode, hp)) {}
		else if(gearMenu.StopDragging(hp)) {}
		else if(keyboard!=null && keyboard.StopDragging(hp)) {  } 
		else if(selectedVariant!=null && selectedVariant.handleGameLinks(hp)) {}
		else if(hitCode instanceof TimeControl.TimeId)
		{
			TimeControl.TimeId id = (TimeControl.TimeId)hitCode;
			if(sess.timeControl().handleMenus(id,G.Left(hp),G.Top(hp),this,this)) {}
			switch(id)
			{
			default: 	G.print("Hit "+id);
			}
		}
		else if(hitCode instanceof SeatId)
		{	
			SeatId id = (SeatId)hitCode;
			if((id!=SeatId.TableName) && (id!=SeatId.NewName)) 
				{ loseFocus(); 
				} 
			switch(id)
			{
			case Scroll: break;
			case GameTimer:
				selectGame(gameTimerMode ? null : GameInfo.GameTimer);
				break;
			case HelpButton:
				{
				URL u = G.getUrl(seatingHelpUrl,true);
				G.showDocument(u);
				}
				break;
			case TableName:
				{
				namefield.setFocus(true);
				selectedInputField = namefield;
				if(useKeyboard) {
					keyboard = new Keyboard(this,namefield);
				}
				else 
				{	requestFocus(namefield);
					repaint(namefield.flipInterval);
				}}
				break;
			case NewName:
				{
					newNameField.setFocus(true);
					selectedInputField = newNameField;
					if(useKeyboard)
					{
						createKeyboard();
					}
					else {
						requestFocus(selectedInputField);
					}
					
				}
				break;
			case PlayOnline:
				G.setOffline(false);
				shutDown();
				break;
			case ToggleServer:
				{boolean running = serviceRunning();   	  
				 if(!running) { UDPService.start(true); } else { UDPService.stop(); }
	    	   	 if(REMOTEVNC) { VNCService.runVNCServer(!running); }
	    	   	 if(REMOTERPC) { RpcService.runRpcServer(!running); }
				}
				break;
			case SelectColor:
				if(pickedSource>=0)
				{
				int saved = colorIndex[hp.row];
				colorIndex[hp.row]=  colorIndex[pickedSource];
				colorIndex[pickedSource] = saved;
				pickedSource = -1;
				}
				else
				{
				pickedSource = hp.row;
				}
				break;
			case SelectFirst:
				firstPlayerIndex = hp.row;
				break;
			case StartButton:
				prepareLaunch();
				User user = sess.players[0];
				User players[] = new User[sess.players.length];
				AR.copy(players,sess.players);
				
				recentGames.recordRecentGame(sess.currentGame,RECENTS,RECENT_LIST_SIZE);
				if(mainMode==MainMode.Recent)
				{	// if you select something from the recent tab, it becomes a favorite
					favoriteGames.recordRecentGame(sess.currentGame,FAVORITES,RECENT_LIST_SIZE);
				}
				sess.startingTimeControl = sess.timeControl();
				sess.launchGame(user,myFrame.doSound(),colorIndex,getCanvasRotation(),sess.currentGame,true);
				for(int i=0;i<players.length;i++) { sess.putInSess(players[i],i); }
				break;
			case DiscardButton:
				OfflineGames.removeOfflineGame(sess.launchName(null,true));
				break;
			case SelectVariant:
				selectedVariant = (GameInfo)hp.hitObject;
				break;
			case SelectGame:
				selectGame((GameInfo)hp.hitObject);				
				break;
			case SelectLetter:
				selectedLetter = (String)hp.hitObject;
				selectGame(null);
				break;
			case SelectCategory:
				selectedCategory = (String)hp.hitObject;
				selectGame(null);
				break;
			case CategoriesSelected:
				mainMode = MainMode.Category;
				break;
			case A_Zselected:
				mainMode = MainMode.AZ;
				break;
			case RecentSelected:
				mainMode = MainMode.Recent;
				break;
			case ChartSelected:
				selectedChart = (SeatingChart)hp.hitObject;
				sess.startingNplayers = selectedChart.getNSeats();
				firstPlayerIndex = Math.min(firstPlayerIndex, Math.max(0, selectedChart.getNSeats()-1));
				break;
			case NameSelected:
				{
				changeUser(false,hp.row,G.Left(hp),G.Top(hp));
				}
				break;
				
			default:
				G.print("Hit unknown target "+id);
			}
		}
		else { G.print("Hit unknown "+hitCode); }
	}
	
	private void drawColorBox(Graphics gc,int index,HitPoint bubbleSelect,Color targetColor,Rectangle r,boolean expand)
	{	int left = G.Left(r);	
		int top = G.Top(r);
		int width = G.Width(r);
		int height = G.Height(r);
		//
		// if expand is true, expand the sensitive area by 50%.  Useful when
		// there's known to be nothing else in the neighborhood that might be
		// selectable.
		//
		int ll = left + (expand ? -width/2 : 0);
		int tt = top + (expand ? -height/2 : 0);
		int ww = width + (expand ? width : 0);
		int hh = height + (expand ? height : 0);

		boolean myColor = G.pointInRect(bubbleSelect, ll,tt,ww,hh);
		if(myColor) 
	{
			  int inc = width/10; 
			  G.SetRect(r,left-inc,top-inc,width+2*inc,height+inc*2); 
			}
		GC.fillRect(gc, (index==pickedSource)?Color.gray:targetColor,r);
		GC.frameRect(gc, Color.black,r);
		if(myColor)
			{
			colorW = 3*G.Width(r)/4;
			bubbleSelect.hitCode = SeatId.SelectColor;
			bubbleSelect.row = index;
			bubbleSelect.spriteRect = r;
			bubbleSelect.spriteColor = Color.red;
			}
	
	}
	private boolean drawTable(Graphics gc,HitPoint pt,int tableSize,int centerX,int centerY,String text)
	{	Image im = StockArt.Playtable_h.getImage();
		double bas = 0.75;
		double imr = bas*im.getWidth()/im.getHeight();
		double hscale = imr*(square ? 0.7 : 1);
		double vscale = bas;
		int xo = (int)(tableSize*hscale/2);
		int yo = (int)(tableSize*vscale/2);
		int left = (centerX-xo);
		int top = (centerY-yo);
		int right = (centerX+xo);
		int bottom = (centerY+yo);
		Rectangle clip = GC.combinedClip(gc,left+2,top+2,right-left-4,bottom-top-4);
		im.drawImage(gc,
						left,top,right,bottom,
						0,0,im.getWidth(),im.getHeight());
		if(text!=null)
			{ 	Font f = GC.getFont(gc);
				GC.setFont(gc,getDefaultFont());
				GC.Text(gc,true,left,top,right-left,bottom-top,Color.lightGray,null,text); 
				GC.setFont(gc,f);
			}
		GC.setClip(gc,clip);
		return G.pointInRect(pt,left,top,right-left,bottom-top);
	}
	private void drawSeatingSchematic(Graphics gc,SeatingChart chart,HitPoint mainSelect,int tableSize,int centerX,int centerY,HitPoint bubbleSelect,boolean portrait)
	{	Seating seats[] = chart.getSeats();
		int nPlayers = seats.length;
		if(portrait)
		{
			GC.setRotatedContext(gc,centerX,centerY,bubbleSelect,Math.PI/2);
		}
		boolean selectingColor = pickedSource>=0;
		HitPoint tableSelect = selectingColor ? null : mainSelect;
		String msg = (bubbleSelect==null)
						? (nPlayers==0) ? "Review" : ""+nPlayers 
						: null;
		if(drawTable(gc,tableSelect,tableSize,centerX,centerY,msg))
		{	tableSelect.hitCode = SeatId.ChartSelected;
			tableSelect.hitObject = chart;
			tableSelect.spriteColor = Color.red;
			tableSelect.awidth = tableSize;
			tableSelect.hit_x = centerX;
			tableSelect.hit_y = centerY;
		}
		if(portrait)
		{
			GC.unsetRotatedContext(gc,bubbleSelect);
		}
		int colorStep = tableSize/30;
		Color []map = selectedGame!=null ? selectedGame.colorMap : null;
		for(int i=0;i<nPlayers;i++)
		{	Seating position = seats[i];
			double xpos = position.x_position;
			double ypos = position.y_position;
			int bubbleX = (int)(tableSize*xpos-tableSize/2);
			int xb = portrait ? (int)(0.68*bubbleX) : bubbleX ;
			int bubbleY = (int)(tableSize*ypos-tableSize/2);
			int yb = portrait ? (int)(bubbleY*0.8) : (int)(0.68*bubbleY);
			int xc = (int)(centerX+xb*(square?0.8:1));
			int yc = centerY-yb;
			int playerNumber = 1+((i+nPlayers-firstPlayerIndex)%nPlayers);
			Color color = Color.yellow;
			if((nPlayers==2) 
					&& map!=null 
					&& !selectedGame.variableColorMap)
			{	// two player games where a particular color starts
				playerNumber = 1+colorIndex[i];
			}
			String name = (bubbleSelect==null) ? null : User.prettyName(sess.players[i]);
			if(name==null) { name = s.get(PlayerNumber,playerNumber); color = Color.red; } 
			
			labelColor = color;
			boolean bubbleOffset = (i==0) 
					&& (bubbleSelect!=null) 
					&& (chart==SeatingChart.faceLandscape);
			if(bubbleOffset)
			{	// special hack for 2 player landscape face to face
				yc += tableSize/8;
			}

			if(StockArt.SmallO.drawChip(gc,this,(int)(tableSize*0.8),xc,
					yc,selectingColor ? null : bubbleSelect,SeatId.NameSelected,
					bubbleSelect==null ? null : DrawableImage.NotHelpDraw+name,0.3,1.2)
					)
			{
				bubbleSelect.row = i;
			}
			if(selectedGame!=null && bubbleSelect!=null)
			{
			int xo = xc - (int)(colorStep*1.5);
			int yo = (int)( yc+ ((yc>centerY)? -colorStep*2.6 : colorStep*2.6));
			if(selectedGame.variableColorMap && !selectedGame.randomizeFirstPlayer)
				{
				if(StockArt.SmallO.drawChip(gc,this,tableSize/4,xo,yo,bubbleSelect,SeatId.SelectFirst,
						DrawableImage.NotHelpDraw+s.get(OrdinalSelector,playerNumber),0.3,1.2))
				{
				bubbleSelect.row = i;
				}
				}
			if(colorIndex!=null && i<colorIndex.length)
				{
				int xo1 = xc + (int)(colorStep*0.4);
				int yo1 = yo-(int)(colorStep);
				Rectangle r = new Rectangle(xo1,yo1,colorStep*3,colorStep*3);
				drawColorBox(gc,i,bubbleSelect,map[colorIndex[i]],r,pickedSource>0);

				}
			}
			
		}
		if(bubbleSelect!=null)
		{
			// draw the other available colors
			if(map!=null)
			{
			double [][]pos = {{0.33,.19},{0.33,-0.25},{-0.4,0.19},{-0.4,-0.25},{-0.4,0},{0.33,0}};
			int nsteps = Math.min(pos.length,map.length);
			if(nsteps>0)
			{
			// draw the unassigned colors
			for(int i=seats.length,idx=0; i<nsteps;i++,idx++)
			{
				double ps[] = pos[idx];
				int xo = centerX+(int)(ps[0]*tableSize*(square?0.8:1));
				int yo = centerY+(int)(ps[1]*tableSize);
				Rectangle r = new Rectangle(xo,yo,colorStep*3,colorStep*3);
				drawColorBox(gc,i,bubbleSelect,map[colorIndex[i]],r,true);
				yo += colorStep*3;
			}}}
			
			String mainMessage = 
					(selectedChart==SeatingChart.blank)
						? SelectChartMessage
						: (selectedVariant==null)
							? SelectGameMessage
							: (gameTimerMode||allPlayersNamed())
								? StartMessage
								: NamePlayersMessage;
			int xs0 = (int)(tableSize*0.2);
			int ys0 = (int)(tableSize*0.075);
			int xs = xs0 ;
			int ys = ys0 ;
			Rectangle r = new Rectangle(centerX-xs,centerY-ys,xs*2,ys*2);		// start
			Rectangle dr = new Rectangle(centerX-xs*3/2,centerY+ys*5/4,xs*3,ys*3/2);	// discard 

			if((selectedGame==null) && (chart.explanation!=null))
			{	GC.frameRect(gc,Color.black,dr);
				GC.Text(gc,true,dr,Color.black,null,s.get(chart.explanation));
			}

			if(mainMessage==StartMessage)
			{
			sess.setCurrentGame(selectedVariant,false,true,false);
			String lname = sess.launchName(null,true);
			boolean restartable = OfflineGames.restoreOfflineGame(lname)!=null; 
			String finalMessage = nPlayers==0 ? Session.ReviewRoom : restartable ? RestartMessage : mainMessage;
			if(GC.handleRoundButton(gc,r,bubbleSelect,s.get(finalMessage),
					buttonHighlightColor, buttonBackgroundColor))
				{
					bubbleSelect.hitCode = SeatId.StartButton;
				}
			if(restartable)
			{
				if(GC.handleRoundButton(gc,dr,bubbleSelect,s.get(DiscardGameMessage),
					buttonHighlightColor, buttonBackgroundColor))
				{
					bubbleSelect.hitCode = SeatId.DiscardButton;
					}
				}
				}
			else
			{
			GC.Text(gc,true, r, Color.black, null, s.get(mainMessage));
			}
			if(selectedGame!=null && G.isTable())
				{String longMessage = selectedGame.longMessage;
				if(longMessage!=null)
				{	int xp = centerX-(int)(xs*1.5);
					int yp = centerY+ys;
					int xw = xs*3;
					int yh = ys;
					GC.Text(gc, true, xp,yp,xw,yh,
							Color.yellow, null, s.get(longMessage));
					GC.Text(gc,true, xp,yp+yh,xw,yh,Color.black,null,s.get(SideScreenMessage));
				}
			}


		}
		if((chart==selectedChart) && (bubbleSelect==null))
		{	int w = (int)(tableSize*1.3);
			int h = tableSize;
			GC.frameRect(gc, Color.red,centerX-w/2,centerY-h/2,w,h);
		}


	}
	private boolean allPlayersNamed()
	{
		if((selectedChart.getNSeats()<=1)) { return(true); }
		User players[] = sess.players;
		for(int i=0,lim=selectedChart.getNSeats(); i<lim;i++)
		{
			if(players[i]==null) { return(false); }
		}
		return(true);
	}
	private SeatingChart[] portaitCharts(SeatingChart[]charts)
	{	int n = 0;
		for(SeatingChart c : charts) { if(!c.id.landscapeOnly) { n++; }}
		SeatingChart res[] = new SeatingChart[n];
		n = 0;
		for(SeatingChart c : charts) { if(!c.id.landscapeOnly) { res[n++] = c; }}
		return res;
	}
	private SeatingChart[] landscapeCharts(SeatingChart[]charts)
	{	int n = 0;
		for(SeatingChart c : charts) { if(!c.id.portraitOnly) { n++; }}
		SeatingChart res[] = new SeatingChart[n];
		n = 0;
		for(SeatingChart c : charts) { if(!c.id.portraitOnly) { res[n++] = c; }}
		return res;
	}
	private void drawSeatingCharts(Graphics gc,Rectangle r,HitPoint hp)
	{	SeatingChart charts[] = serviceRunning() ? SeatingChart.ServerCharts : SeatingChart.NonServerCharts;
		int w = G.Width(r);
		int h = G.Height(r);
		if(w>h*1.6) { charts = landscapeCharts(charts); }
		else if(w*1.6<h) { charts = portaitCharts(charts); }
	
		int ncharts = charts.length;
		boolean portrait = w<h;
		int majorAxis = portrait ? h : w;
		int minorAxis = portrait ? w : h;
		int left = G.Left(r);
		int top = G.Top(r);
		int fastAxisSize = portrait ? h : w;
		int step =(int)( majorAxis/(ncharts+1.5));
		int slowAxisPos = (portrait ? G.centerX(r) : G.centerY(r));
		int slowAxisStep = portrait ? w/2 : h/2;
		if(step*3<minorAxis)
		{
			step = step*3/2;
			slowAxisPos = portrait ? G.Left(r)+G.Width(r)/4 : G.Top(r)+G.Height(r)/4;
		}
		
		int slowAxisTop = slowAxisPos - step/2;
		int fastAxisPos = (portrait ? top : left);
		int fastAxisStart = fastAxisPos+step;
		
		int tableSize = 2*step/3;
		GC.setFont(gc, largeBoldFont());
		if(!G.arrayContains(charts,selectedChart)) 
			{ selectedChart = charts[0];
			}
		if(selectedChart!=null) { sess.startingNplayers = selectedChart.getNSeats(); }
		
		GC.Text(gc, true,portrait ? slowAxisTop : fastAxisPos,portrait ? fastAxisPos : slowAxisTop,
				step,step,Color.black,null,s.get(SeatPositionMessage));
		fastAxisPos += step;
		Color fillColor = chartEven;
		int np = charts[0].getNSeats();
		for(int i=1;i<=ncharts;i++)
		{	SeatingChart chart = charts[i-1];
			int newseats = chart.getNSeats();
			if(np!=newseats) 
				{ fillColor = (fillColor==chartEven) ? chartOdd : chartEven; 
				}
			np = newseats;			
			GC.fillRect(gc, fillColor, portrait?slowAxisTop : fastAxisPos,portrait?fastAxisPos:slowAxisTop,step,step);
			
			int centerX = fastAxisPos+step/2;
			int centerY = slowAxisPos;
			drawSeatingSchematic(gc,charts[i-1],hp,tableSize,
					portrait ? centerY : centerX,
					portrait ? centerX : centerY,
					null,portrait);
			fastAxisPos += step;
			if(fastAxisPos+step>=fastAxisSize)
			{
				fastAxisPos = fastAxisStart;
				slowAxisTop += slowAxisStep;
				slowAxisPos += slowAxisStep;
			}
		}
	}
	private GameInfo[] subgames(GameInfo[]gameNames,char selection)
	{	GameInfoStack gi = new GameInfoStack();
		// count matches
		for(GameInfo g : gameNames)
		{	
			String name = g.gameName;
			char match = (""+(name.charAt(0))).toUpperCase().charAt(0);
			if(selection=='?' || (selection==match)) { gi.push(g); }
		}
		return(gi.toArray());
	}
	private void drawCatButton(Graphics gc,HitPoint hp,MainMode m,int left,int top,int w,int h)
	{	
		Color fgColor = mainMode==m ? buttonSelectedColor : buttonBackgroundColor;
		if (GC.handleRoundButton(gc, new Rectangle(left, top, w, h), 
        		hp, s.get(m.title),
                lastButton==m.id ? fgColor : buttonHighlightColor, fgColor))
		{
			hp.hitCode = m.id;
		}
	}
	
	private boolean selectedGameSeen=false;
	ScrollArea leftScrollbar = new ScrollArea();	// the scrollbar for the display
	ScrollArea midScrollbar = new ScrollArea();	// the scrollbar for the display
	ScrollArea catScrollbar = new ScrollArea();
	ScrollArea variationScrollbar = new ScrollArea();
	
	// draw a column of game names
	private boolean drawGameColumn(Graphics gc,HitPoint hp,ScrollArea scroll,
			boolean autoSelect,int margin,GameInfo gameNames[],int gameX,int gameY,int gameColumnWidth,int h)
	{	
		boolean hit = false;
		if(gameNames!=null)
		{
		int fh = G.getFontSize(standardPlainFont());
		int vspace = (int)(fh*3.5);
		int half = vspace*5/6;
		int lessThanHalf = half-half/10;
		boolean barvisible = vspace*gameNames.length>h;
		if(barvisible) 	{ h = (h/vspace)*vspace; }
		
	   	//int barWidth = barvisible ? scrollbar.getScrollbarWidth() : 0;
	   	//boolean scrolled = scrollbar.mouseIsActive();   
		int scrollPos = scroll.drawScrollBar(gc,hp,SeatId.Scroll,new Rectangle(gameX,gameY,gameColumnWidth,h),barvisible,gameNames.length,vspace);

		if(barvisible)
		{	gameColumnWidth -= scroll.getScrollbarWidth()+margin;
			
		}

		// display games within a category
		if(autoSelect && gameNames.length==1) { selectGame(gameNames[0]); }
		for(int i=0;i+scrollPos<gameNames.length && (i+1)*vspace<=h;i++)
		{
			GameInfo g = gameNames[i+scrollPos];
			String name = s.get(g.gameName);
			boolean selected = g==selectedGame;
			selectedGameSeen |= selected;
			Color fgColor = selected ? buttonSelectedColor : buttonHighlightColor;
			Color bgColor = selected ? buttonSelectedColor : buttonBackgroundColor;
			int gtop = gameY+i*vspace;
			Rectangle gameRect = new Rectangle(gameX+half,gtop,gameColumnWidth-half,half);
			Rectangle iconRect = new Rectangle(gameX,gtop+margin/2,lessThanHalf,lessThanHalf);
			if(GC.handleRoundButton(gc, gameRect,hp,
					name,
					fgColor,bgColor))
				{
					hp.hitCode = SeatId.SelectGame;
					hp.hitObject = g;
					hit = true;
				}
			Image icon = g.getIcon();
			if(icon!=null)
			{
			icon.centerImage(gc,iconRect);
			GC.frameRect(gc, Color.black, iconRect);
			if(G.pointInRect(hp,iconRect))
			{
				hp.hitCode = SeatId.SelectGame;
				hp.hitObject = g;
				hp.spriteRect = iconRect;
				hp.spriteColor = Color.red;
			}
			}
		}}
		return hit;
	}
	
	private CellId lastButton = null;
	private void drawGameSelector(Graphics gc,Rectangle r,HitPoint hp)
	{	GC.setFont(gc,largeBoldFont());
		selectedGameSeen = false;
		GameInfo game = sess.currentGame;
		if(game!=null)
		{
		int nplayers = selectedChart.getNSeats();
		Bitset<ES> typeClass = sess.getGameTypeClass(false,true,false);
		int w = G.Width(r);
		int l = G.Left(r);
		int t = G.Top(r);
		int h = G.Height(r);
		int fh = G.getFontSize(standardPlainFont());
		
		GameInfo categoryNames[] = GameInfo.groupMenu(typeClass,nplayers);
		int az_cols = 3;
		int step = fh*4;
		int half = 2*step/3;
		int margin = half/10;
		int vspace = fh*3;
		boolean gameListSeen = false;
		sess.setMode(nplayers==0?Session.Mode.Review_Mode:Session.Mode.Unranked_Mode,true,false);
		
		String msg = (nplayers==0) 
						? s.get(SoloMode) 
						: s.get(NPlayerMode,nplayers);
		GC.Text(gc, true, l, t, w*2/3, step/2,Color.black,null,msg);
		
		int catColumnLeft = l+w/25;
		int catColumnWidth = w/4;
		int gameX = catColumnLeft+catColumnWidth+w/8;
		int gameColumnWidth = w/4;
		int variantX = gameX+gameColumnWidth+w/25;
		int catButtonW = (gameColumnWidth+catColumnWidth)/3;
		int third = half*2/3;
		
		drawCatButton(gc,hp,MainMode.Category,catColumnLeft, t+half, catButtonW, step);
		drawCatButton(gc,hp,MainMode.AZ,catColumnLeft+catButtonW+third, t+half, catButtonW, step);
		drawCatButton(gc,hp,MainMode.Recent,catColumnLeft+catButtonW*2+third*2, t+half, catButtonW, step);
		int mainX = catColumnLeft+catButtonW*3+catButtonW*2/3;
		int gameY = t+half*3;
		int gameboxH = h-(gameY-t)-fh*3;
		int variantY = gameY;
		int spaces = (h-step*6)/half;
		switch(mainMode)
		{
		default: throw G.Error("Main mode %s not handled",mainMode);
		case Recent:
			{
			GameInfo games[] = recentGames.filterGames(nplayers);
			GameInfo favs[] = favoriteGames.filterGames(nplayers);
			if(favs!=null)
			{	// don't autoselect if there's a single game, it gets stuck!
				drawGameColumn(gc,hp,leftScrollbar,games==null||games.length==1,margin,favs,catColumnLeft,gameY,gameColumnWidth,gameboxH);
				gameListSeen = true;
			}
			
			if(games!=null)
			{
				drawGameColumn(gc,hp,midScrollbar,(favs==null||favs.length==1),margin,games,gameX,gameY,gameColumnWidth,gameboxH);
				gameListSeen = true;
			}
			
			}
			break;
		case Category:
		{	// game categories
			int ncats = categoryNames.length;
			boolean barvisible = vspace*ncats>gameboxH;
			int gameW = w/3;
			int boxh = barvisible ? (gameboxH/vspace)*vspace : gameboxH;
			int scrollPos = leftScrollbar.drawScrollBar(gc,hp,SeatId.Scroll,new Rectangle(catColumnLeft,gameY,gameW,boxh),barvisible,ncats,vspace);
			if(barvisible) { gameW -= leftScrollbar.getScrollbarWidth()+margin; }
			for(int i=0;i+scrollPos<ncats && (i+1)*vspace<=gameboxH;i++)
			{	String catName = categoryNames[i+scrollPos].groupName;
				boolean selected = catName.equals(selectedCategory);
				Color bgColor =  selected ? buttonSelectedColor : buttonBackgroundColor;
				Color fgColor = selected ? buttonSelectedColor : buttonHighlightColor;
				if(GC.handleRoundButton(gc, new Rectangle(catColumnLeft,gameY+i*vspace,gameW,vspace-4),hp,
					s.get(catName),fgColor,bgColor))
				{
					hp.hitCode = SeatId.SelectCategory;
					hp.hitObject = catName;
				}
			}
			{
			GameInfo gameNames[] = GameInfo.gameMenu(selectedCategory,typeClass,nplayers);		
			gameListSeen = gameNames!=null && gameNames.length>0;
			drawGameColumn(gc,hp,midScrollbar,true,margin,gameNames,gameX,gameY,gameColumnWidth,gameboxH);
			}
		}
		break;
		
		case AZ:
		{	// a-z selection
			GameInfo gameNames[] = GameInfo.gameMenu(null, typeClass,nplayers);
			char selection = selectedLetter.charAt(0);			
			GameInfo sub[][] = new GameInfo['Z'-'A'+1][];
			for(char ch = 'A'; ch<='Z'; ch++) { sub[ch-'A'] = subgames(gameNames,ch); }
			int row = 0;
			int col = 0;
			catColumnLeft += vspace/2;
			int hspace = vspace*5/6;
			if(gameNames.length>spaces)
			{
				for(int i=0;i<26;i++)
				{	
					String letter = (""+(char)('A'+i));
					int nmatches = sub[i].length;
					boolean selected = letter.equals(selectedLetter);
					Color bgColor = selected 
								? buttonSelectedColor 
								: nmatches==0
									? buttonEmptyColor
									: buttonBackgroundColor;
					Color fgColor = selected ? buttonSelectedColor : buttonHighlightColor;
	
					int xp = catColumnLeft+col*vspace;
					int yp = gameY+row*vspace;
					Rectangle re = new Rectangle(xp,yp,hspace,hspace);
					if(GC.handleRoundButton(gc, re ,hp,
							letter,
							fgColor,bgColor))
					{
						hp.hitCode = SeatId.SelectLetter;
						hp.hitObject = letter;
					}
					col++;
					if(col>=az_cols) { col=0; row++; }
				}
			}
			else { selection = '?'; }
			// games with the selected letter, or none
			GameInfo matches[] = selection=='*'
						? null 
						: (selection>='A' && selection<='Z') 
							? sub[selection-'A'] 
							: gameNames;
			
		
			gameListSeen = matches!=null;
			gameX = catColumnLeft+vspace*3+vspace/4;
			variantX = gameX+gameColumnWidth+vspace/4;
			drawGameColumn(gc,hp,midScrollbar,true,margin,matches,gameX,gameY,gameColumnWidth,gameboxH);

				}
			}

		  if(gameListSeen)
		  { messageArea.setVisible(false);
		  }
		  else 
		  {	
			helpRect.draw(gc,hp);
  
			int gameW = w-(gameX-l)-w/20;
		    int gameH = h-(gameY-t)-h/20;
		    Font lb = largeBoldFont();
		    // have to be very careful because GC may be null, and in rare
		    // circumstances, this.getFont() may also be null.
		    GC.setFont(gc,lb);
		    FontMetrics fm = G.getFontMetrics(lb);
		    int fonth = fm.getHeight();
		    int topPart = fonth*3;
		    int messageY = G.Bottom(helpRect)+topPart/3;
		    Rectangle ur = new Rectangle(gameX,messageY,gameW,topPart);
			  GC.frameRect(gc,Color.blue,ur);
		    GC.Text(gc,true,gameX,messageY,gameW,topPart,Color.black,null,"Play Offline");
			  GC.setFont(gc,standardPlainFont());
		    messageArea.setBounds(gameX,messageY+topPart+1,gameW,gameH-topPart-fonth);
			  if(!messageArea.isVisible())
			  {
			  messageArea.setVisible(true);
			  messageArea.flagExtensionLines = false;
			  messageArea.setBackground(Color.lightGray);
			  messageArea.setText(s.get(MessageAreaMessage));
			  }
			  messageArea.redrawBoard(gc, hp);
			  
		}
		
		// if the selections have moved under the selected game, make it unselected.
		if(!selectedGameSeen && !gameTimerMode) { selectGame(null); }
		
		if(selectedGame!=null)
		{	
			int hw = half*4;
			Rectangle iconRect = new Rectangle(Math.max(variantX,mainX),t,hw,hw);
			variantY = G.Bottom(iconRect);
			Image icon = selectedGame.getIcon2();
			if(!gameTimerMode && (icon!=null))
			{
				icon.centerImage(gc, iconRect);
				GC.frameRect(gc,Color.black,iconRect);
			}
			int boxh = vspace/2;
			GameInfo variations[] = selectedGame.variationMenu(selectedGame.gameName,typeClass,nplayers);
			if((variations!=null) && (variations.length>1) 
					&& (selectedChart!=null))					
			{	
			GC.Text(gc,false,variantX,variantY,gameColumnWidth,vspace,Color.black,null,
						s.get(VariantMsg,s.get(selectedGame.gameName),""+variations.length));
			variantY += vspace;
			boxh = ((gameboxH-vspace*3)/vspace)*vspace;
			boolean barvisible = variations.length*vspace>boxh;
			
			int gameW = gameColumnWidth;
			int scrollPos = variationScrollbar.drawScrollBar(gc,hp,SeatId.Scroll,new Rectangle(variantX,variantY,gameW,boxh),barvisible,variations.length,vspace);
			if(barvisible) { gameW -= variationScrollbar.getScrollbarWidth()+margin; }
			else { boxh = variations.length*vspace; }

			for(int i=0;i+scrollPos<variations.length && (i+1)*vspace<=boxh;i++)
				{	GameInfo variant = variations[i+scrollPos];
					String name = s.get(variant.variationName+"_variation");
					boolean selected = variant==selectedVariant ;
					Color fgColor = selected ? buttonSelectedColor : buttonHighlightColor;
					Color bgColor = selected ? buttonSelectedColor : buttonBackgroundColor;
					if(GC.handleRoundButton(gc, new Rectangle(variantX,variantY+i*vspace,gameW,vspace-4),hp,
							name,fgColor,bgColor))
					{	hp.hitCode = SeatId.SelectVariant;
						hp.hitObject = variant;
					}
				}
			variantY += half/3;
			}
			if(selectedVariant!=null)
				{ 
				selectedVariant.drawAuxGameLinks(gc,this,hp,new Rectangle(variantX,variantY+boxh,gameColumnWidth,half));
				}

		}
	  }
	  if(hp!=null) { lastButton = hp.hitCode; }
			}
	
	private void drawMainSelector(Graphics gc,Rectangle mainr,Rectangle gamer,HitPoint hp,boolean portrait)
	{	if(selectedChart!=null)
		{
		drawSeatingSchematic(gc,selectedChart,null,
				(int)(Math.min(G.Width(mainr),G.Height(mainr))),
				G.centerX(mainr),
				G.centerY(mainr) - ((serviceRunning()|portrait) ? 0 : G.Height(mainr)/6),
				hp,portrait);
		
		if(!gameTimerMode) { drawGameSelector(gc,gamer,pickedSource>=0 ? null : hp); }
		}
	}
	
		 
	private void changeUser(boolean recurse,int slot,int ex,int ey)
	{	changeRecurse = recurse;
		changeSlot = slot;
	  	users.changeUserMenu(userMenu,!recurse,this,slot,ex,ey);
	  }

	private boolean serviceRunning()
	{
		return (VNCService.isVNCServer()||RpcService.isRpcServer());
	}
	
	TimeControl timeControl = new TimeControl(TimeControl.Kind.None);
	
	public void drawTimeControl(Graphics gc,Rectangle r,HitPoint hp)
	{	GC.setFont(gc,largeBoldFont());
		sess.timeControl().drawTimeControl(gc,this,true,hp,r);
	}

		
	static public void drawVersion(Graphics gc,Rectangle versionRect)
	{	String appversion = G.getAppVersion();
	 	String platform = G.getPlatformPrefix();
	 	String prefVersion = G.getString(platform+"_version",null);
	 	InternationalStrings s = G.getTranslations();
		String va = s.get(VersionMessage,appversion);

		if((prefVersion!=null)
	 		&&	G.isCodename1())
	 	{
	 	Double prefVersionD = G.DoubleToken(prefVersion);
	 	double appversionD = G.DoubleToken(appversion);
		// 
		// 8/2017 apple is now in a snit about prompting for updates
		//
		if(prefVersion!=null && !appversion.equals(prefVersion) 
				&& (!G.isIOS() || (appversionD<prefVersionD)))
			{
			va += " ("+s.get(util.PasswordCollector.VersionPreferredMessage,prefVersion)+")";
			}
	 	}
		va += " "+G.build;
		GC.Text(gc,false,versionRect,Color.black,null,va);
	}

	public void drawCanvas(Graphics gc, boolean complete, HitPoint pt0) 
	{	//Plog.log.addLog("drawcanvas ",gc," ",pt0," ",pt0.down);
		if(needNewLayout)
		{
			setLocalBounds(0,0,getWidth(),getHeight());
		}
		Keyboard kb = getKeyboard();
		HitPoint pt = pt0;
		if(kb!=null )
	        {  pt = null;
	        }
		HitPoint unPt = pickedSource>=0 ? null : pt;
		
		if(complete) { fillUnseenBackground(gc); }
		
		GC.fillRect(gc, Color.lightGray,fullRect);
		GC.setFont(gc,largePlainFont());
		drawVersion(gc,versionRect);

		drawSeatingCharts(gc,seatingSelectRect,unPt);
		int w = G.Width(seatingSelectRect);
		int h = G.Height(seatingSelectRect);
		boolean portrait = w<h;

		drawMainSelector(gc,seatingChart,gameSelectionRect,pt,portrait);
		if(G.debug()||G.isTable()) 
			{ 
			gearMenu.includeExit = !fromLobby;
			gearMenu.draw(gc,unPt);
			}
		
		drawTimeControl(gc,clockRect,unPt);
		
		if(!G.isIOS() 
				&& GC.handleRoundButton(gc,startStopRect,unPt,
						s.get(serviceRunning() ? StopTableServerMessage : StartTableServerMessage),
				buttonHighlightColor, buttonBackgroundColor))
			{
			unPt.hitCode = SeatId.ToggleServer;
			}
		
		if(!fromLobby) { onlineButton.draw(gc,unPt); }

		if(!G.isIOS())
		{
		GC.Text(gc,true,tableNameRect,Color.black,null,s.get(TableNameMessage));
		namefield.setFont(largeBoldFont());
		namefield.redrawBoard(gc, pt);
		}
		
		timerMode.draw(gc,pt);
		
		
		if(useKeyboard && (selectedInputField==newNameField))
		{
			newNameField.redrawBoard(gc,unPt);
		}
		if(kb!=null)
		{
			kb.draw(gc, pt0);
		}
		drawUnmagnifier(gc,pt0);
		if(respawnNewName==1 && gc!=null) { respawnNewName++; }
	}

	public void drawCanvasSprites(Graphics gc, HitPoint pt) 
	{
		if(pickedSource>=0)
		{
			Rectangle r = new Rectangle(G.Left(pt),G.Top(pt),colorW,colorW);
			GC.fillRect(gc, selectedGame.colorMap[colorIndex[pickedSource]],r);
			GC.frameRect(gc, Color.black, r);
		}
		if(mouseTrackingAvailable(pt) || pt.down) 
			{ drawTileSprite(gc,pt); 
			}
	}
	//
	// oldway pops up a dialog, new way edits the name in the window
	// this avoids the bug lastgameboard has with the vkb
	//
	boolean oldway = G.isCodename1() ? false : true;
	private void showAddName(int x,int y)
	{
		newNameField.setFont(largeBoldFont());        
		newNameField.setText(TypeinMessage);
		newNameField.setEditable(this,true);
		newNameField.singleLine = true;
		newNameField.setVisible(true);
		newNameField.setBackground(bsBlue);

		FontMetrics fm = G.getFontMetrics(newNameField.getFont());
		
		int w = (int)(fm.stringWidth(TypeinMessage)*1.2);
		int h = fm.getHeight()*2;
		
		newNameField.setBounds(x-w/2,y-h/2,w,h);		// start
		
		selectedInputField = newNameField;
		newNameField.clearBeforeAppend = true;
		createKeyboard();
	}
	private void addNewUser(String name)
	{
		if(name!=null)
		{ name = name.trim();
		  if(!"".equals(name)) { users.changeOfflineUser(name.trim(),false); }
		}
	}
	private void handleUserMenu()
	{
		Object val = userMenu.rawValue;
		boolean add = AddAName==val;
		boolean remove = RemoveAName==val;
		boolean empty = EmptyName==val;
		if(changeRecurse)
		{	// second pass, remove now
			if(val instanceof User) 
			{	User newchoice = (User)val;
				users.removeUserFromUserlist(newchoice);
				users.changeOfflineUser(newchoice.name,true);
				sess.putInSess(newchoice,-1);
				changeUser(false,changeSlot,userMenu.showAtX,userMenu.showAtY);
			}
		}
		else if(empty)
		{
			sess.clearUserSlot(changeSlot);
		}
		else if(add)
		{	if(oldway)
			{
			String name = JOptionPane.showInputDialog(AddAName);
			addNewUser(name);
			changeUser(false,changeSlot,userMenu.showAtX,userMenu.showAtY);
			}
			else 
			{
				showAddName(userMenu.showAtX,userMenu.showAtY);
			}
		}
		else if(remove)
		{	// recurse
			changeUser(true,changeSlot,userMenu.showAtX,userMenu.showAtY);
		}
		else if (val instanceof User)
		{
		User newchoice = (User)userMenu.rawValue;
		if(newchoice!=null)
		{	sess.putInSess(newchoice,changeSlot); 
		}
		}
		repaint();
	}

	public boolean handleDeferredEvent(Object target, String command)
	{	if(super.handleDeferredEvent(target,command)) { return(true); }
		if(userMenu.selectMenuTarget(target))
		{	handleUserMenu();
			return(true);
		}
		if(target==autoDone)
		{
	    	Default.setBoolean((Default.autodone),autoDone.getState());
	    	return true;
		}
				
		if(gearMenu.handleDeferredEvent(target,command)) { }
		if(target == testswitch)
		{
  			 PopupManager.useSwing = !PopupManager.useSwing;
  			 G.infoBox(null,"swing popups "+PopupManager.useSwing);
		}
	    if (target == startserver)  
	 			{ 
	    	   	 boolean running = VNCService.isVNCServer()||RpcService.isRpcServer(); 
	    	   	 if(!running) { UDPService.start(true); } else { UDPService.stop(); }
	    	   	 VNCService.runVNCServer(!running);
	    	   	 RpcService.runRpcServer(!running);
	    	     startserver.setText(running ? "start server" : "stop server");
	    	   	 return(true);
	 			}
	    if (target == startVncViewer)  
				{ 
	    	   	vncViewer = doVncViewer(sharedInfo); 
				return(true);
				}
	    if (target == startRpcViewer)  
				{ 
	    	   	rpcViewer = doRpcViewer(sharedInfo); 
				 return(true);
				}
		return(false);
	}
    private AuxViewer doVncViewer(ExtendedHashtable sharedInfo)
    {  
    	commonPanel panel = (commonPanel)new commonPanel();
    	XFrame frame = new XFrame("VNC viewer");
    	AuxViewer viewer = (AuxViewer)new vnc.AuxViewer();
    	if(viewer!=null)
    	{
    	viewer.init(sharedInfo,frame);
    	panel.setCanvas(viewer);
    	frame.setContentPane(panel);
    	viewer.setVisible(true);
    	double scale = G.getDisplayScale();
    	frame.setInitialBounds(100,100,(int)(scale*800),(int)(scale*600));
    	frame.setVisible(true);
    	panel.start();
    	}
    	return(viewer);
    }
    
    private RpcServiceClient doRpcViewer(ExtendedHashtable sharedInfo)
    {  
    	XFrame frame = new XFrame("RPC viewer");
    	commonPanel panel = (commonPanel)new commonPanel();
    	panel.init(sharedInfo,frame);
    	RpcReceiver.start("localhost",RpcPort,sharedInfo, panel,frame);
    	
    	frame.setContentPane(panel);
    	double scale = G.getDisplayScale();
    	frame.setInitialBounds(100,100,(int)(scale*800),(int)(scale*600));
    	frame.setVisible(true);
 	 	G.print("running "+panel);
        panel.start();

    	return (RpcServiceClient)(panel.getCanvas());
    }
	
	static String SoloMode = "Solo review of games";
	static String NPlayerMode = "Games for #1 Players";
	static String CategoriesMode = "Categories";
	static String RecentMode = "Recent";
	static String A_ZMode = "Games A-Z";
	static String VariantMsg = "#1 has #2 variations";
	
	static String SelectChartMessage = "select the seating arrangement";
	static String SelectGameMessage = "select the game to play";
	static String NamePlayersMessage = "set the player names";
	static String SideScreenMessage = "use the boardspace.net app on any mobile as a side screen";
	static String OrdinalSelector = "#1{,'st,'nd,'rd,'th}";
	static String StartTableServerMessage = "Start table server";
	static String StopTableServerMessage = "Stop table server";
	static String PlayOnlineMessage = "Play Online";
	static String PlayOnlineExplanation = "Log in to play online at Boardspace";
	static String TableNameMessage = "Table Name";
	static String SeatPositionMessage = "SeatPositionMessage";
	static String TypeinMessage = "type the name here";
	static String MessageAreaMessage = "MessageAreaMessage";
	static String HelpMessage = "Get More Help";
	static String ExplainHelpMessage = "show the documentation for this screen";
	
	public static String[]SeatingStrings =
		{	SelectChartMessage,
			HelpMessage,
			PlayOnlineExplanation,
			ExplainHelpMessage,
			TableNameMessage,
			TypeinMessage,
			PlayOnlineMessage,
			StartTableServerMessage,
			StopTableServerMessage,
			OrdinalSelector,
			SideScreenMessage,
			StartMessage,
			NamePlayersMessage,
			SelectGameMessage,
			SoloMode,
			NPlayerMode,
			VariantMsg,
			CategoriesMode,
			A_ZMode,
			RecentMode,
			GameTimerMessage,
			GameTimerHelpMessage,
		};
	
	 public static String[][] SeatingStringPairs =
		 {
			{SeatPositionMessage,"Where Are\nYou Sitting?"}	 ,
			{MessageAreaMessage,
				"This panel launches games played with other people sharing this device.\n\n"
				+ "If you want to play robots, or people who are not in the same room, use the 'Play Online' button and log into the server.\n\n"
				+ "If you and friends are playing on this device, start by selecting the seating chart that best approximates were you will be sitting, then browse the Categories or A-Z list of games and select the game to play.\n\n"
			
			},
		 };
	 public void shutDown()
	 {	
		 super.shutDown();
		 SeatingViewer sv = seatingViewer;
		 if(sv!=null) { seatingViewer = null; sv.shutDown(); }

		 AuxViewer v = vncViewer;
     	 if(v!=null) { vncViewer = null; v.shutDown(); }
     	 RpcServiceClient r = rpcViewer;
     	 if(r!=null) { rpcViewer = null; r.shutDown(); }

		 if(REMOTEVNC) { VNCService.stopVNCServer(); }
		 if(REMOTERPC) { RpcService.stopRpcServer(); }	
		 LFrameProtocol f = myFrame;
		 if(f!=null) { f.killFrame(); }
	 }
	boolean fromLobby = false;
	// this is used to start a seating viewer from the lobby
    static public SeatingViewer doSeatingViewer(ExtendedHashtable sharedInfo)
    {  
    	commonPanel panel = new commonPanel();
    	XFrame frame = new XFrame("Offline Launcher");
    	SeatingViewer viewer = new SeatingViewer();
    	if(viewer!=null)
    	{
    	viewer.init(sharedInfo,frame);
    	viewer.fromLobby = true;
    	panel.setCanvas(viewer);
    	viewer.setVisible(true);
    	double scale = G.getDisplayScale();
    	frame.setContentPane(panel);
    	frame.setInitialBounds(100,100,(int)(scale*800),(int)(scale*600));
    	frame.setVisible(true);
    	panel.start();
    	}
    	return(viewer);
    }
    
    public void ViewerRun(int waitTime)
    {
    	super.ViewerRun(waitTime);
    	if(respawnNewName>1)
    	{
    		respawnNewName = 0;
     		changeUser(false,changeSlot,userMenu.showAtX,userMenu.showAtY);
    	}
    }
    private Keyboard keyboard = null;
	private boolean useKeyboard = G.defaultUseKeyboard();
	private JMenuItem startserver = null; //for testing, disable the transmitter
	private JMenuItem testswitch = null;
	private SeatingViewer seatingViewer = null;
	private AuxViewer vncViewer = null;
	private RpcServiceClient rpcViewer = null;
	private JMenuItem startVncViewer = null; //for testing, start a remote viewer
	private JMenuItem startRpcViewer = null; // for testing, start a remote viewer
    public void createKeyboard()
    {	if(useKeyboard)
    	{
    	keyboard = selectedInputField.makeKeyboardIfNeeded(this,keyboard);
    	}
    }
    public void closeKeyboard()
    {
    	Keyboard kb = keyboard;
    	if(kb!=null) { kb.setClosed(); }
    }
    private void loseFocus()
    {
    	if(selectedInputField!=null)
    	{	TextContainer sel = selectedInputField;
    		selectedInputField = null;
    		keyboard = null;
    		sel.setFocus(false);
    		if(sel==namefield) 
    			{ UDPService.setPlaytableName(namefield.getText().trim()); 
    			}
    		else if(sel==newNameField)
    		{	
    			addNewUser(newNameField.getText());
    			repaint();
    			respawnNewName = 1;
    		}
     	}
    }
    public Keyboard getKeyboard() 
    { Keyboard k = keyboard;
      if(k!=null && k.closed) 
      	{ k = keyboard = null; 
      	  loseFocus();
      	}
      return(k); 
    }
	public void Wheel(int x,int y,int button,double amount)
	{

		boolean done = messageArea.isVisible()
					&& G.pointInRect(x,y,messageArea);
    	if(done)
    		{
    		messageArea.doMouseWheel(x,y,amount);
    		}
    	else
    		{ super.Wheel(x,y,button,amount);
    		}
    }
	public double imageSize(ImageStack im)
	{
		return (super.imageSize(im)+Image.registeredImageSize(im));
	}

	public void actionPerformed(ActionEvent e) {
		if(sess.timeControl().handleDeferredEvent(e.getSource(), e.getActionCommand()))
		{
			// note that this is passed through the canvas because in live games, it needs
			// to be passed on to the other players.
		}
		else { super.actionPerformed(e); }
		
	}

}
