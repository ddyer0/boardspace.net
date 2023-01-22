package online.common;

import lib.Graphics;

import com.codename1.ui.Font;
import com.codename1.ui.geom.Rectangle;

import bridge.Color;
import bridge.FontMetrics;
import bridge.JCheckBoxMenuItem;
import bridge.JOptionPane;
import bridge.MouseWheelEvent;
import bridge.Preferences;
import bridge.URL;
import common.GameInfo;
import common.GameInfoStack;
import common.GameInfo.ES;
import lib.AR;
import lib.Bitset;
import lib.CellId;
import lib.DefaultId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.Image;
import lib.InternationalStrings;
import lib.Keyboard;
import lib.LFrameProtocol;
import lib.MouseState;
import lib.OfflineGames;
import lib.PopupManager;
import lib.Random;
import lib.RootAppletProtocol;
import lib.StockArt;
import lib.TextButton;
import lib.TextContainer;
import online.common.SeatingChart.Seating;
import rpc.RpcService;
import udp.UDPService;
import vnc.VNCService;
import static util.PasswordCollector.VersionMessage;

@SuppressWarnings("serial")
public class SeatingViewer extends exCanvas implements LobbyConstants
{	
	static final String FAVORITES = "SeatingFavorites";
	static final String RECENTS = "SeatingRecents";
	
	Color buttonBackgroundColor = new Color(0.7f,0.7f,0.7f);
	Color buttonEmptyColor = new Color(0.5f,0.5f,0.5f);
	Color buttonHighlightColor = new Color(1.0f,0.5f,0.5f);
	Color buttonSelectedColor = new Color(0.6f,0.6f,0.8f);
	Color chartEven = new Color(0.7f,0.7f,0.7f);
	Color chartOdd = new Color(0.65f,0.65f,0.65f);
	private JCheckBoxMenuItem autoDone=null;          		//if on, chat up a storm
	boolean portraitLayout = false;
	Rectangle seatingSelectRect = addRect("seating select");
	Rectangle seatingChart = addRect("seatingChart");
	Rectangle gameSelectionRect = addRect("gameSelection");
	Rectangle startStopRect = addRect("StartStop");
	Rectangle onlineRect = addRect("Online");
	TextButton helpRect = addButton(HelpMessage,SeatId.HelpButton,ExplainHelpMessage,
			buttonHighlightColor, buttonBackgroundColor);
	Rectangle tableNameRect = addRect("TableName");
	Rectangle addNameTextRect = addRect("AddName");
	Rectangle versionRect = addRect("version");
	Rectangle gearRect = addRect("Gear");
	TextContainer selectedInputField = null;
	private int respawnNewName = 0;
	TextContainer namefield = (TextContainer)addRect("namefield",new TextContainer(SeatId.TableName));
	TextContainer newNameField = (TextContainer)addRect("newname",new TextContainer(SeatId.NewName));
	TextContainer messageArea = new TextContainer(SeatId.MessageArea);
	GameInfoStack recentGames = new GameInfoStack();
	GameInfoStack favoriteGames = new GameInfoStack();

	SeatingChart selectedChart = SeatingChart.defaultPassAndPlay;
	UserManager users = new UserManager();
	int numberOfUsers = 0;
	Session sess = new Session(1);
	int changeSlot = 0;
	int firstPlayerIndex = 0;
	int colorIndex[] = null;
	boolean changeRecurse = false;
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
	MainMode mainMode = MainMode.Category;
	
	String selectedCategory = "";
	String selectedLetter = "*";
	GameInfo selectedGame = null;
	GameInfo selectedVariant = null;
	int pickedSource = -1;
	int colorW = -1;
	boolean square = G.isRealLastGameBoard();
	PopupManager userMenu = new PopupManager();
	
	enum SeatId implements CellId
	{	ShowRules,
		ShowVideo,
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
		GearMenu,
		Exit,
		Feedback,
		DrawersOff,
		DrawersOn,
		PlayOnline,
		HelpButton,
		TableName,
		NewName, MessageArea, RecentSelected;
		public String shortName() {
			return(name());
		}
	}
	public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	super.init(info,frame);
        users.loadOfflineUsers();
        painter.drawLockRequired = false;
        sess.setMode(Session.Mode.Review_Mode,isPassAndPlay());
        sess.setCurrentGame(GameInfo.firstGame,false,isPassAndPlay());
        if(G.debug()) {
        	InternationalStrings.put(GameInfo.GameInfoStringPairs);
        	SeatingChart.putStrings();
        	InternationalStrings.put(SeatingViewer.SeatingStrings);
        	InternationalStrings.put(SeatingViewer.SeatingStringPairs);
        }
        String name = UDPService.getPlaytableName();
        namefield.setText(name);
        namefield.singleLine = true;
        namefield.setEditable(this,true);
        namefield.setVisible(true);
        autoDone = myFrame.addOption(s.get(AutoDoneEverywhere),Default.getBoolean(Default.autodone),deferredEvents);
        autoDone.setForeground(Color.blue);
        // this starts the servers that listen for connections from side screens
        
        reloadGameList(favoriteGames,FAVORITES);
        reloadGameList(recentGames,RECENTS);
        
        if(G.isCodename1())
        {
        	frame.setCanvasRotater(this);
        }
        if(G.isTable())
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
	}
	public void setLocalBounds(int l, int t, int w, int h) 
	{
		G.SetRect(fullRect,l,t,w,h); 
		// to benefit lastgameboard, don't switch to portrait if the board is nearly square
		boolean portrait = w<(h*0.9);		
		double ratio = Math.abs((double)w/h);
		square = !portrait && ratio>0.9 && ratio<1.1;
		portraitLayout = portrait;
		int stripHeight ;
		int fh = G.getFontSize(standardPlainFont());
		G.SetRect(versionRect,l+fh,t+h-fh*2,w/3,fh*2);
		if(portrait)
		{
			stripHeight = w/7;
			G.SetRect(seatingSelectRect, l, t,stripHeight, h-fh*2);
			int gameH = 3*h/5;
			int left = l+stripHeight;
			int margin = stripHeight/4;
			G.SetRect(gameSelectionRect, l+stripHeight,t,w-l-stripHeight-margin,gameH);
			G.SetRect(seatingChart, left, t+gameH, w-left-margin, h/3);
			G.SetRect(gearRect,w-margin*3,t+margin,margin*2,margin*2);
			G.SetRect(helpRect,G.Left(gearRect)-w/4+w/30,t+stripHeight/4,w/5,stripHeight/2);
		}
		else 
		{
		stripHeight = h/7;
		G.SetRect(seatingSelectRect, l, t, w,stripHeight);
		G.SetRect(gameSelectionRect, l,t+stripHeight,w/2,h-stripHeight);
		G.SetRect(helpRect,G.Right(gameSelectionRect)-w/6,t+stripHeight+stripHeight/4,w/7,stripHeight/2);
		int left = l+w/2+w/40;
		int margin = stripHeight/2;
		G.SetRect(seatingChart, left, t+stripHeight+margin, w-left-margin, h-stripHeight-stripHeight/2-margin);
		G.SetRect(gearRect,w-margin*2,t+stripHeight,margin,margin);
		}
		int buttonw = w/10;
		int buttonh = stripHeight/2; 
		int buttonspace = buttonw/5;
		int btop = h-buttonh;
		int buttonX = G.Left(seatingChart);
		G.SetRect(onlineRect, buttonX, btop, buttonw, buttonh);
		buttonX += buttonw+buttonspace;
		G.SetRect(startStopRect, buttonX, btop, buttonw, buttonh);
		buttonX += buttonw+buttonspace;
		btop += buttonh/8;
		buttonh -= buttonh/4;
		G.SetRect(tableNameRect,buttonX,btop,buttonw*2/3,buttonh);
		buttonX += buttonw*2/3;
		namefield.setBounds(buttonX, btop, buttonw, buttonh);
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
		if(keyboard!=null && keyboard.containsPoint(eventX,eventY))
		{	
		keyboard.doMouseMove(eventX,eventY,upcode);
		}
		else if(messageArea.isVisible())
		{
			messageArea.doMouseMove(eventX,eventY,upcode);
		}
		else if(selectedInputField!=null) { selectedInputField.doMouseMove(eventX, eventY, upcode); } 
		
		
		HitPoint p = super.MouseMotion(eventX, eventY, upcode);
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
		sess.seedValue = new Random().nextInt();
		sess.seatingChart = selectedChart;
		int nseats = sess.startingNplayers = selectedChart.getNSeats();
		LaunchUserStack lusers = new LaunchUserStack();

		if(sess.players[0]==null) { sess.players[0]=users.primaryUser(); }

		for(int i=0;i<nseats;i++)
		{
			LaunchUser u = new LaunchUser();
			u.order = i;
			u.seat = i;
			u.user = sess.players[i];
			lusers.push(u);
		}
		
		sess.selectedFirstPlayerIndex = firstPlayerIndex;
		sess.launchUser = sess.startingPlayer = lusers.size()>0 ? lusers.elementAt(0) : null;
		sess.launchUsers = lusers.toArray();	 
		sess.setCurrentGame(selectedVariant, false,isPassAndPlay());
		sess.startingName = sess.launchName(null,true);
	}
	//reload a game list from preferences
	private void reloadGameList(GameInfoStack stack,String key)
	{	stack.clear();
		Preferences prefs = Preferences.userRoot();
		String games = prefs.get(key,null);
		if(games!=null)
		{
			String all[] = G.split(games,',');
			if(all!=null)
			{
			for(String name : all)
				{
				GameInfo g = GameInfo.findByVariation(name);
				if(g!=null) { stack.push(g); }
				}
			}
		}
		
	}
	//modify the "recent" list
	private void recordRecentGame(GameInfoStack stack,GameInfo in,String key)
	{	// remove any games with the same name - variations are considered to be the same
		for(int lim = stack.size()-1; lim>=0; lim--)
			{
			GameInfo g = stack.elementAt(lim);
			if(g.gameName.equalsIgnoreCase(in.gameName)) { stack.remove(lim,true); }
			}
		// reduce size to 10
		while(stack.size()>10) { stack.remove(0,false); }
		// insert at the beginning
		stack.insertElementAt(in,0);		// move to the most recent position
		// save as a property
		StringBuilder b = new StringBuilder();
		for(int i=0,lim=stack.size(); i<lim; i++)
		{	GameInfo g = stack.elementAt(i);
			b.append(g.variationName);
			b.append(",");
		}
		Preferences prefs = Preferences.userRoot();
		prefs.put(key,b.toString());
	}
	
	@Override
	public void StopDragging(HitPoint hp) {
		CellId hitCode = hp.hitCode;
		if(hitCode==DefaultId.HitNoWhere) { pickedSource = -1; }
		if(performStandardButtons(hitCode, hp)) {}
		else if(keyboard!=null && keyboard.StopDragging(hp)) {  } 
		else if(hitCode instanceof SeatId)
		{	
			SeatId id = (SeatId)hitCode;
			if((id!=SeatId.TableName) && (id!=SeatId.NewName)) 
				{ loseFocus(); 
				} 
			switch(id)
			{
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
			case GearMenu:
				doGearMenu(G.Left(hp),G.Top(hp));
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
				
				recordRecentGame(recentGames,sess.currentGame,RECENTS);
				if(mainMode==MainMode.Recent)
				{	// if you select something from the recent tab, it becomes a favorite
					recordRecentGame(favoriteGames,sess.currentGame,FAVORITES);
				}
				sess.launchGame(user,true,colorIndex,getCanvasRotation(),sess.currentGame);
				for(int i=0;i<players.length;i++) { sess.putInSess(players[i],i); }
				break;
			case DiscardButton:
				OfflineGames.removeOfflineGame(sess.launchName(null,true));
				break;
			case ShowVideo:
				{
				GameInfo g = (GameInfo)hp.hitObject;
				String rules = g.howToVideo;
				if(rules!=null)
		 		  {
		 		  URL u = G.getUrl(rules,true);
				  G.showDocument(u);
		 		  }}
				break;

			case ShowRules:
				{
				GameInfo g = (GameInfo)hp.hitObject;
				String rules = g.rules;
				if(rules!=null)
		 		  {
		 		  URL u = G.getUrl(rules,true);
				  G.showDocument(u);
		 		  }}
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
	}
	
	private void drawColorBox(Graphics gc,int index,HitPoint bubbleSelect,Color targetColor,Rectangle r)
	{
		boolean myColor = G.pointInRect(bubbleSelect, r);
		if(myColor) 
			{ int colorStep = G.Width(r);
			  int inc = colorStep/10; 
			  int xo = G.Left(r);
			  int yo = G.Top(r);
			  G.SetRect(r,xo-inc,yo-inc,(colorStep+2*inc),(colorStep+2*inc)); 
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
	private void drawSeatingSchematic(Graphics gc,SeatingChart chart,HitPoint mainSelect,int tableSize,int centerX,int centerY,HitPoint bubbleSelect)
	{	Seating seats[] = chart.getSeats();
		int nPlayers = seats.length;
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
		int colorStep = tableSize/30;
		Color []map = selectedGame!=null ? selectedGame.colorMap : null;
		for(int i=0;i<nPlayers;i++)
		{	Seating position = seats[i];
			double xpos = position.x_position;
			double ypos = position.y_position;
			int xb = (int)(tableSize*xpos-tableSize/2);
			int bubbleY = (int)(tableSize*ypos-tableSize/2);
			int yb = (int)(0.68*bubbleY);
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

			if(StockArt.SmallO.drawChip(gc,this,selectingColor ? null : bubbleSelect,SeatId.NameSelected,
					(int)(tableSize*0.8),xc,yc,
					bubbleSelect==null ? null : name,0.3,1.2)
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
				if(StockArt.SmallO.drawChip(gc,this,bubbleSelect,SeatId.SelectFirst,tableSize/4,xo,yo,s.get(OrdinalSelector,playerNumber),0.3,1.2))
				{
				bubbleSelect.row = i;
				}
				}
			if(colorIndex!=null && i<colorIndex.length)
				{
				int xo1 = xc + (int)(colorStep*0.4);
				int yo1 = yo-(int)(colorStep*0.75);
				Rectangle r = new Rectangle(xo1,yo1,colorStep*2,colorStep*2);
				drawColorBox(gc,i,bubbleSelect,map[colorIndex[i]],r);

				}
			}
			
		}
		if(bubbleSelect!=null)
		{
			// draw the other available colors
			if(map!=null)
			{
			int nsteps = map.length-seats.length;
			double [][]pos = {{0.33,.19},{0.33,-0.25},{-0.4,0.19},{-0.4,-0.25},{-0.4,0},{0.33,0}};
			if(nsteps>0)
			{
			// draw the unassigned colors
			for(int i=seats.length,idx=0; i<map.length;i++,idx++)
			{
				double ps[] = pos[idx];
				int xo = centerX+(int)(ps[0]*tableSize*(square?0.8:1));
				int yo = centerY+(int)(ps[1]*tableSize);
				Rectangle r = new Rectangle(xo,yo,colorStep*2,colorStep*2);
				drawColorBox(gc,i,bubbleSelect,map[colorIndex[i]],r);
				yo += colorStep*3;
			}}}
			
			String mainMessage = 
					(selectedChart==SeatingChart.blank)
						? SelectChartMessage
						: (selectedVariant==null)
							? SelectGameMessage
							: allPlayersNamed()
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
			sess.setCurrentGame(selectedVariant,false,isPassAndPlay());
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
	private void drawSeatingCharts(Graphics gc,Rectangle r,HitPoint hp)
	{	SeatingChart charts[] = serviceRunning() ? SeatingChart.ServerCharts : SeatingChart.NonServerCharts;
		int ncharts = charts.length;
		int w = G.Width(r);
		int h = G.Height(r);
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
			  sess.startingNplayers = selectedChart.getNSeats();
			}
		
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
					null);
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
	private boolean drawCatButton(Graphics gc,HitPoint hp,MainMode m,int left,int top,int w,int h)
	{	
		Color fgColor = mainMode==m ? buttonSelectedColor : buttonBackgroundColor;
		return (GC.handleRoundButton(gc, new Rectangle(left, top, w, h), 
        		hp, s.get(m.title),
                lastButton==m.id ? fgColor : buttonHighlightColor, fgColor));
	}
	
	private boolean selectedGameSeen=false;
	
	// draw a column of game names
	private boolean drawGameColumn(Graphics gc,HitPoint hp,boolean autoSelect,int margin,GameInfo gameNames[],int gameX,int gameY,int gameColumnWidth,int h)
	{	
		boolean hit = false;
		if(gameNames!=null)
		{
		int vspace = h/Math.max(13,gameNames.length+3);
		int half = vspace*5/6;
		int lessThanHalf = half-half/10;
		// display games within a category
		if(autoSelect && gameNames.length==1) { selectGame(gameNames[0]); }
		for(int i=0;i<gameNames.length;i++)
		{
			GameInfo g = gameNames[i];
			String name = s.get(g.gameName);
			selectedGameSeen |= g==selectedGame;
			Color fgColor = g==selectedGame ? buttonSelectedColor : buttonBackgroundColor;
			int gtop = gameY+i*vspace;
			Rectangle gameRect = new Rectangle(gameX+half,gtop,gameColumnWidth-half,half);
			Rectangle iconRect = new Rectangle(gameX,gtop+margin/2,lessThanHalf,lessThanHalf);
			if(GC.handleRoundButton(gc, gameRect,hp,
					name,
					buttonHighlightColor, fgColor
					))
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
			}
		}}
		return hit;
	}
	
	private CellId lastButton = null;
	private void drawGameSelector(Graphics gc,Rectangle r,HitPoint hp)
	{	GC.setFont(gc,largeBoldFont());
		selectedGameSeen = false;
		SeatId someButton = null;
		GameInfo game = sess.currentGame;
		if(game!=null)
		{
		int nplayers = selectedChart.getNSeats();
		Bitset<ES> typeClass = sess.getGameTypeClass(false,isPassAndPlay());
		int w = G.Width(r);
		int l = G.Left(r);
		int t = G.Top(r);
		int h = G.Height(r);
		GameInfo categoryNames[] = game.groupMenu(typeClass,nplayers);
		int az_cols = 3;
		int rows = 1+ (mainMode==MainMode.Category ? categoryNames.length : (26+az_cols-1)/az_cols);
		int step = Math.max(4, Math.min(G.getFontSize(largeBoldFont())*3,(h+rows-1)/rows));
		int half = 2*step/3;
		int margin = half/10;
		int vspace = half+half/4;
		boolean gameListSeen = false;
		sess.setMode(nplayers==0?Session.Mode.Review_Mode:Session.Mode.Unranked_Mode,isPassAndPlay());
		
		String msg = (nplayers==0) 
						? s.get(SoloMode) 
						: s.get(NPlayerMode,nplayers);
		GC.Text(gc, true, l, t, w, step/2,Color.black,null,msg);
		
		int catColumnLeft = l+w/25;
		int catColumnWidth = w/4;
		int gameX = catColumnLeft+catColumnWidth+w/8;
		int gameColumnWidth = w/4;
		int variantX = gameX+gameColumnWidth+w/25;
		int catButtonW = (gameColumnWidth+catColumnWidth)/3;
		int third = half*2/3;
		
		if(drawCatButton(gc,hp,MainMode.Category,catColumnLeft, t+half, catButtonW, step))
		{
			someButton = MainMode.Category.id;
		}
		if(drawCatButton(gc,hp,MainMode.AZ,catColumnLeft+catButtonW+third, t+half, catButtonW, step))
		{
			someButton = MainMode.AZ.id;
		}
		if(drawCatButton(gc,hp,MainMode.Recent,catColumnLeft+catButtonW*2+third*2, t+half, catButtonW, step))
		{
			someButton = MainMode.Recent.id;
		}

		
		int gameY = t+half*2+step;
		int variantY = gameY;
		int spaces = (h-step*6)/half;
		switch(mainMode)
		{
		default: throw G.Error("Main mode %s not handled",mainMode);
		case Recent:
			{
			GameInfo games[] = recentGames.toArray();
			GameInfo favs[] = favoriteGames.toArray();
			if(favs!=null)
			{	// don't autoselect if there's a single game, it gets stuck!
				if(drawGameColumn(gc,hp,games==null||games.length==1,margin,favs,catColumnLeft,gameY,gameColumnWidth,h))
					{
					someButton = SeatId.SelectGame;
					}			
				gameListSeen = true;
			}
			
			if(games!=null)
			{
			if(drawGameColumn(gc,hp,(favs==null||favs.length==1),margin,games,gameX,gameY,gameColumnWidth,h))
				{
				someButton = SeatId.SelectGame;
				}
				gameListSeen = true;
			}
			
			}
			break;
		case Category:
		{	// game categories
			int ncats = categoryNames.length;
			{
			int avspace = Math.min(vspace,h/ncats);
			for(int i=0;i<ncats;i++)
			{	String catName = categoryNames[i].groupName;
				Color fgColor =  catName.equals(selectedCategory) ? buttonSelectedColor : buttonBackgroundColor;
				if(GC.handleRoundButton(gc, new Rectangle(catColumnLeft,gameY+i*avspace,w/3,half),hp,
					s.get(catName),
					lastButton==SeatId.SelectCategory ? fgColor : buttonHighlightColor,fgColor
					))
				{
					someButton = SeatId.SelectCategory;
					hp.hitObject = catName;
				}
			}}
			{
			GameInfo gameNames[] = game.gameMenu(selectedCategory,typeClass,nplayers);
			gameListSeen = gameNames!=null && gameNames.length>0;
			if(drawGameColumn(gc,hp,true,margin,gameNames,gameX,gameY,gameColumnWidth,h))
					{
						someButton = SeatId.SelectGame;
				}
			}
		}
		break;
		
		case AZ:
		{	// a-z selection
			GameInfo gameNames[] = game.gameMenu(null, typeClass,nplayers);
			char selection = selectedLetter.charAt(0);			
			GameInfo sub[][] = new GameInfo['Z'-'A'+1][];
			for(char ch = 'A'; ch<='Z'; ch++) { sub[ch-'A'] = subgames(gameNames,ch); }
			int row = 0;
			int col = 0;
			catColumnLeft += vspace/2;
			vspace = h/Math.max(13,(26+2)/3);
			int hspace = vspace*5/6;

			if(gameNames.length>spaces)
			{
				for(int i=0;i<26;i++)
				{	
					String letter = (""+(char)('A'+i));
					int nmatches = sub[i].length;
					Color fgColor = letter.equals(selectedLetter)
								? buttonSelectedColor 
								: nmatches==0
									? buttonEmptyColor
									: buttonBackgroundColor;
					int xp = catColumnLeft+col*vspace;
					int yp = gameY+row*vspace;
					Rectangle re = new Rectangle(xp,yp,hspace,hspace);
					if(GC.handleRoundButton(gc, re ,hp,
							letter,
							lastButton==SeatId.SelectLetter ? fgColor : buttonHighlightColor, fgColor
							))
					{
						someButton = SeatId.SelectLetter;
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
			if(drawGameColumn(gc,hp,true,margin,matches,gameX,gameY,gameColumnWidth,h))
					{
						someButton = SeatId.SelectGame;
					}
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
		    int topPart = fm.getHeight()*3/2;
		    Rectangle ur = new Rectangle(gameX,gameY,gameW,topPart);
			  GC.frameRect(gc,Color.blue,ur);
		    GC.Text(gc,true,gameX,gameY,gameW,topPart,Color.black,null,"Play Offline");
			  GC.setFont(gc,standardPlainFont());
		    messageArea.setBounds(gameX,gameY+topPart+1,gameW,gameH-topPart);
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
		if(!selectedGameSeen) { selectGame(null); }
		
		if(selectedGame!=null)
		{	
			int hw = half*4-margin;
			Rectangle iconRect = new Rectangle(variantX+(gameColumnWidth-hw)/2,t-margin,hw,hw);
			variantY = G.Bottom(iconRect);
			Image icon = selectedGame.getIcon2();
			if(icon!=null)
			{
				icon.centerImage(gc, iconRect);
			}
			GC.frameRect(gc,Color.black,iconRect);
			GameInfo variations[] = selectedGame.variationMenu(selectedGame.gameName,typeClass,nplayers);
			if((variations!=null) && (variations.length>1) 
					&& (selectedChart!=null))					
			{	int avspace = Math.min(vspace, (h-vspace*4)/(variations.length+4));

				GC.Text(gc,false,variantX,variantY,gameColumnWidth,avspace,Color.black,null,
							s.get(VariantMsg,s.get(selectedGame.gameName),""+variations.length));
				variantY += avspace;
				for(int i=0;i<variations.length;i++)
				{	GameInfo variant = variations[i];
					String name = s.get(variant.variationName+"_variation");
					Color fgColor = variant==selectedVariant ? buttonSelectedColor : buttonBackgroundColor;
					if(GC.handleRoundButton(gc, new Rectangle(variantX,variantY,gameColumnWidth,avspace-4),hp,
							name,
							lastButton==SeatId.SelectVariant ? fgColor : buttonHighlightColor, fgColor
							))
					{	someButton = SeatId.SelectVariant;
						hp.hitObject = variant;
					}
					variantY += avspace;
				}
				}
			{
			Rectangle variantRect = new Rectangle(variantX,variantY+half,gameColumnWidth,half);
			if(GC.handleRoundButton(gc, variantRect,hp,
					s.get(RulesMessage),
					lastButton==SeatId.ShowRules ? buttonBackgroundColor  : buttonHighlightColor, buttonBackgroundColor)
					)
			{
				someButton = SeatId.ShowRules;
				hp.hitObject = selectedVariant;
			}
			variantY += half*3/2;
			}
			
			if(selectedGame.howToVideo!=null)
			{
				Rectangle variantRect = new Rectangle(variantX,variantY+half,gameColumnWidth,half);
				if(GC.handleRoundButton(gc, variantRect,hp,
						s.get(VideoMessage),
						lastButton==SeatId.ShowVideo ? buttonBackgroundColor : buttonHighlightColor, buttonBackgroundColor)
						)
				{
					someButton = SeatId.ShowVideo;
					hp.hitObject = selectedVariant;
				}
			}
		}
		}
	  if(someButton!=null)
	  { hp.hitCode = someButton;
	    if(hp.upCode==MouseState.LAST_IS_UP) { lastButton = someButton; }
	  }
	  else { lastButton = null; }
	}
	private void drawMainSelector(Graphics gc,Rectangle mainr,Rectangle gamer,HitPoint hp)
	{	if(selectedChart!=null)
		{
		drawSeatingSchematic(gc,selectedChart,null,
				Math.min(G.Width(mainr),
				G.Height(mainr)),
				G.centerX(mainr),
				G.centerY(mainr) - (serviceRunning() ? 0 : G.Height(mainr)/6),
				hp);
		
		drawGameSelector(gc,gamer,pickedSource>=0 ? null : hp);
		}
	}
	
		 
	private void changeUser(boolean recurse,int slot,int ex,int ey)
	{	changeRecurse = recurse;
		changeSlot = slot;
	  	users.changeUserMenu(userMenu,!recurse,this,slot,ex,ey);
	  }
	PopupManager gearMenu = new PopupManager();
	public void doGearMenu(int x,int y)
	{
		gearMenu.newPopupMenu(this,deferredEvents);
		gearMenu.addMenuItem("Exit",SeatId.Exit);
		gearMenu.addMenuItem(SendFeedbackMessage,SeatId.Feedback);
		if(G.isRealLastGameBoard())
		{
			gearMenu.addMenuItem(DrawerOffMessage,SeatId.DrawersOff);
			gearMenu.addMenuItem(DrawersOnMessage,SeatId.DrawersOn);
		}
		gearMenu.show(x,y);
	}
	private boolean serviceRunning()
	{
		return (VNCService.isVNCServer()||RpcService.isRpcServer());
	}
	public void drawCanvas(Graphics gc, boolean complete, HitPoint pt0) 
	{	//Plog.log.addLog("drawcanvas ",gc," ",pt0," ",pt0.down);
		Keyboard kb = getKeyboard();
		HitPoint pt = pt0;
		if(kb!=null )
	        {  pt = null;
	        }
		HitPoint unPt = pickedSource>=0 ? null : pt;

		if(complete) { fillUnseenBackground(gc); }
		
		GC.fillRect(gc, Color.lightGray,fullRect);
		
		String appversion = G.getAppVersion();
	 	String platform = G.getPlatformPrefix();
	 	String prefVersion = G.getString(platform+"_version",null);
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
		drawSeatingCharts(gc,seatingSelectRect,unPt);
		drawMainSelector(gc,seatingChart,gameSelectionRect,pt);
		if(G.debug()||G.isTable()) 
			{ StockArt.Gear.drawChip(gc, this, gearRect, unPt,SeatId.GearMenu,s.get(ExitOptions)); 
			}
		if(!G.isIOS() 
				&& GC.handleRoundButton(gc,startStopRect,unPt,
						s.get(serviceRunning() ? StopTableServerMessage : StartTableServerMessage),
				buttonHighlightColor, buttonBackgroundColor))
			{
			unPt.hitCode = SeatId.ToggleServer;
			}
		
		if(GC.handleRoundButton(gc,onlineRect,unPt,s.get(PlayOnlineMessage),
				buttonHighlightColor, buttonBackgroundColor))
			{
				unPt.hitCode = SeatId.PlayOnline;
			}
		if(!G.isIOS())
		{
		GC.TextRight(gc,tableNameRect,Color.black,null,s.get(TableNameMessage));
		namefield.setFont(largeBoldFont());
		namefield.redrawBoard(gc, pt);
		
		}
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
			{ magnifier.DrawTileSprite(gc,pt); 
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
		if(gearMenu.selectMenuTarget(target))
		{
			SeatId me = (SeatId)gearMenu.rawValue;
			if(me!=null)
			{
				switch(me)
				{
				default: G.Error("Hit unexpected gear item %s",me);
					break;
				case Feedback:
				  	G.getFeedback();
				  	break;
				case DrawersOff:
					G.setDrawers(false);
					break;
				case DrawersOn:
					G.setDrawers(true);
					break;
				
				case Exit:	
						G.hardExit();
						break;
				}
			}
			
		}

		return(false);
	}
	
	static String SoloMode = "Solo review of games";
	static String NPlayerMode = "Games for #1 Players";
	static String CategoriesMode = "Categories";
	static String RecentMode = "Recent";
	static String A_ZMode = "Games A-Z";
	static String VariantMsg = "#1 has #2 variations";
	static String RulesMessage = "read the rules";
	static String VideoMessage = "how to play video";
	static String SelectChartMessage = "select the seating arrangement";
	static String SelectGameMessage = "select the game to play";
	static String NamePlayersMessage = "set the player names";
	static String SideScreenMessage = "use the boardspace.net app on any mobile as a side screen";
	static String OrdinalSelector = "#1{,'st,'nd,'rd,'th}";
	static String StartTableServerMessage = "Start table server";
	static String StopTableServerMessage = "Stop table server";
	static String PlayOnlineMessage = "Play Online";
	static String TableNameMessage = "Table Name: ";
	static String SeatPositionMessage = "SeatPositionMessage";
	static String ExitOptions = "Options";
	static String TypeinMessage = "type the name here";
	static String SendFeedbackMessage = "Send Feedback";
	static String DrawerOffMessage = "Player Drawers OFF";
	static String DrawersOnMessage = "Player Drawers ON";
	static String MessageAreaMessage = "MessageAreaMessage";
	static String HelpMessage = "Get More Help";
	static String ExplainHelpMessage = "show the documentation for this screen";
	
	public static String[]SeatingStrings =
		{	SelectChartMessage,
			HelpMessage,
			ExplainHelpMessage,
			TableNameMessage,
			SendFeedbackMessage,
			DrawerOffMessage,
			DrawersOnMessage,
			TypeinMessage,
			ExitOptions,
			PlayOnlineMessage,
			StartTableServerMessage,
			StopTableServerMessage,
			OrdinalSelector,
			VideoMessage,
			SideScreenMessage,
			StartMessage,
			NamePlayersMessage,
			SelectGameMessage,
			SoloMode,
			RulesMessage,
			NPlayerMode,
			VariantMsg,
			CategoriesMode,
			A_ZMode,
			RecentMode,
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
		 if(REMOTEVNC) { VNCService.stopVNCServer(); }
		 if(REMOTERPC) { RpcService.stopRpcServer(); }	
		 LFrameProtocol f = myFrame;
		 if(f!=null) { f.killFrame(); }
	 }

    static public SeatingViewer doSeatingViewer(ExtendedHashtable sharedInfo)
    {   RootAppletProtocol theRoot = G.getRoot();
    	commonPanel panel = (commonPanel)G.MakeInstance("online.common.commonPanel");
    	LFrameProtocol frame;
    	SeatingViewer viewer = (SeatingViewer)G.MakeInstance("online.common.SeatingViewer");
    	if(viewer!=null)
    	{
    	frame = theRoot.NewLFrame("Game Selector",panel);
    	viewer.init(sharedInfo,frame);
    	panel.setCanvas(viewer);
    	viewer.setVisible(true);
    	double scale = G.getDisplayScale();
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
	private boolean useKeyboard = G.isCodename1();
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
	public void handleMouseWheel(MouseWheelEvent e)
	{
		int amount = e.getWheelRotation();

		boolean done = messageArea.isVisible()
					&& G.pointInRect(e.getX(),e.getY(),messageArea);
    	if(done) { messageArea.doMouseWheel(amount);}
    	else
    		{ super.handleMouseWheel(e);
    		}
    }

}
