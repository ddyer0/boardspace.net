package online.common;

import lib.Graphics;
import com.codename1.ui.geom.Rectangle;

import bridge.Color;
import bridge.JOptionPane;
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
import lib.TextContainer;
import online.common.SeatingChart.Seating;
import rpc.RpcService;
import udp.UDPService;
import vnc.VNCService;
import static util.PasswordCollector.VersionMessage;

@SuppressWarnings("serial")
public class SeatingViewer extends exCanvas implements LobbyConstants
{	Color buttonBackgroundColor = new Color(0.7f,0.7f,0.7f);
	Color buttonEmptyColor = new Color(0.5f,0.5f,0.5f);
	Color buttonHighlightColor = new Color(1.0f,0.5f,0.5f);
	Color buttonSelectedColor = new Color(0.6f,0.6f,0.8f);
	Color chartEven = new Color(0.7f,0.7f,0.7f);
	Color chartOdd = new Color(0.65f,0.65f,0.65f);
	boolean portraitLayout = false;
	Rectangle seatingSelectRect = addRect("seating select");
	Rectangle seatingChart = addRect("seatingChart");
	Rectangle gameSelectionRect = addRect("gameSelection");
	Rectangle startStopRect = addRect("StartStop");
	Rectangle onlineRect = addRect("Online");
	Rectangle tableNameRect = addRect("TableName");
	Rectangle tableNameTextRect = addRect("TableNameText");
	Rectangle versionRect = addRect("version");
	Rectangle gearRect = addRect("Gear");
	TextContainer namefield = new TextContainer(SeatId.TableName);
	SeatingChart selectedChart = SeatingChart.defaultPassAndPlay;
	UserManager users = new UserManager();
	int numberOfUsers = 0;
	Session sess = new Session(1);
	int changeSlot = 0;
	int firstPlayerIndex = 0;
	int colorIndex[] = null;
	boolean changeRecurse = false;
	boolean categoryMode = true;
	String selectedCategory = "";
	String selectedLetter = "*";
	GameInfo selectedGame = null;
	GameInfo selectedVariant = null;
	int pickedSource = -1;
	int colorW = -1;
	
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
		PlayOnline,
		TableName,
		;
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
        }
        String name = UDPService.getPlaytableName();
        namefield.setText(name);
        namefield.singleLine = true;
        namefield.setEditable(this,true);
        namefield.setVisible(true);
        
        // this starts the servers that listen for connections from side screens
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
	public void setLocalBounds(int l, int t, int w, int h) {
		G.SetRect(fullRect,l,t,w,h); 
		boolean portrait = w<h;
		portraitLayout = portrait;
		int stripHeight ;
		int fh = G.getFontSize(standardPlainFont());
		G.SetRect(versionRect,l+fh,t+h-fh*2,w/8,fh*2);
		if(portrait)
		{
			stripHeight = w/7;
			G.SetRect(seatingSelectRect, l, t,stripHeight, h-fh*2);
			int gameH = 3*h/5;
			int left = l+stripHeight*2;
			G.SetRect(gameSelectionRect, l+stripHeight,t,w-stripHeight,gameH);
			int margin = stripHeight/4;
			G.SetRect(seatingChart, left, t+gameH, w-margin, h/3);
			G.SetRect(gearRect,w-margin*2,t,margin*2,margin*2);
		}
		else 
		{
		stripHeight = h/7;
		G.SetRect(seatingSelectRect, l, t, w,stripHeight);
		G.SetRect(gameSelectionRect, l,t+stripHeight,w/2,h-stripHeight);
		int left = l+w/2;
		int margin = stripHeight/2;
		G.SetRect(seatingChart, left, t+stripHeight, w-left-margin, h-stripHeight-stripHeight/2);
		G.SetRect(gearRect,w-margin,t+stripHeight,margin,margin);
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
		G.SetRect(tableNameTextRect, buttonX, btop, buttonw, buttonh);
		namefield.setBounds(tableNameTextRect);
        if(keyboard!=null) { keyboard.resizeAndReposition(); }

	}

	public HitPoint MouseMotion(int eventX, int eventY,MouseState upcode)
	{
		
		if(keyboard!=null && keyboard.containsPoint(eventX,eventY))
		{	
		keyboard.doMouseMove(eventX,eventY,upcode);
		}
		else 
		{ namefield.doMouseMove(eventX, eventY, upcode); }
		
		HitPoint p = super.MouseMotion(eventX, eventY, upcode);
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
				 if(pickedSource<0) {	pickedSource = hp.row; }

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
	
	@Override
	public void StopDragging(HitPoint hp) {
		CellId hitCode = hp.hitCode;
		if(hitCode==DefaultId.HitNoWhere) { pickedSource = -1; }
		if(performStandardButtons(hitCode)) {}
		else if(keyboard!=null && keyboard.StopDragging(hp)) {  } 
		else if(hitCode instanceof SeatId)
		{	
			SeatId id = (SeatId)hitCode;
			if(id!=SeatId.TableName) { loseFocus(); } 
			switch(id)
			{
			case TableName:
				{
				namefield.setFocus(true);
				if(useKeyboard) {
					keyboard = new Keyboard(this,namefield);
				}
				else 
				{	requestFocus(namefield);
					repaint(namefield.flipInterval);
				}}
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
				sess.launchGame(user,true,colorIndex);
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
				categoryMode = true;
				break;
			case A_Zselected:
				categoryMode = false;
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
	private void drawSeatingSchematic(Graphics gc,SeatingChart chart,HitPoint mainSelect,int tableSize,int centerX,int centerY,HitPoint bubbleSelect)
	{	Seating seats[] = chart.getSeats();
		int nPlayers = seats.length;
		double rotation = portraitLayout ? Math.PI/2 : 0;
		G.setRotation(bubbleSelect, rotation,centerX,centerY);
		GC.setRotation(gc,rotation,centerX,centerY);
		
		if(StockArt.Playtable_h.drawChip(gc, this,mainSelect,SeatId.ChartSelected,tableSize,centerX,centerY,null))
		{
			mainSelect.hitObject = chart;
		}
		int colorStep = tableSize/30;
		Color []map = selectedGame!=null ? selectedGame.colorMap : null;
		for(int i=0,nplayers=seats.length;i<nplayers;i++)
		{	Seating position = seats[i];
			
			int xb = (int)(tableSize*position.x_position-tableSize/2);
			int bubbleY = (int)(tableSize*position.y_position-tableSize/2);
			int yb = (int)(0.68*bubbleY);
			int xc = centerX+xb;
			int yc = centerY-yb;
			int playerNumber = 1+((i+nplayers-firstPlayerIndex)%nplayers);
			if((nplayers==2) 
					&& map!=null 
					&& !selectedGame.variableColorMap)
			{	// two player games where a particular color starts
				playerNumber = 1+colorIndex[i];
			}
			String name = (bubbleSelect==null) ? null : User.prettyName(sess.players[i]);
			if(name==null) { name = s.get(PlayerNumber,playerNumber); } 
			GC.setRotation(gc, -rotation, xc,yc);
			boolean bubbleOffset = (i==0) && (bubbleSelect!=null) && (chart==SeatingChart.faceLandscape);
			if(bubbleOffset)
			{	// special hack for 2 player landscape face to face
				yc += tableSize/8;
			}
			
			if(StockArt.SmallO.drawChip(gc,this,bubbleSelect,SeatId.NameSelected,(int)(tableSize*0.8),xc,yc,
					bubbleSelect==null ? null : name,0.3,1.2)
					)
			{
				bubbleSelect.row = i;
			}
			GC.setRotation(gc, rotation, xc,yc);
			if(selectedGame!=null && bubbleSelect!=null)
			{
				boolean center = yb==0;
				int xo = centerX+xb-colorStep+(center ? -G.signum(xb)*tableSize/8: tableSize/30 );
				int yo = centerY-yb-(int)(colorStep*2.6);
			if(selectedGame.variableColorMap)
				{
				GC.setRotation(gc, -rotation,xo,yo);
				if(StockArt.SmallO.drawChip(gc,this,bubbleSelect,SeatId.SelectFirst,tableSize/4,xo,yo,s.get(OrdinalSelector,playerNumber),0.3,1.2))
				{
				bubbleSelect.row = i;
				}
				GC.setRotation(gc, rotation,xo,yo);
				}
			if(colorIndex!=null && i<colorIndex.length)
				{
				int xo1 = xo + (center ? 0 : tableSize/15);
				int yo1 = yo + (center ? Math.abs(xb/9) : 0);
				if(bubbleOffset) { yo1+=tableSize/8; }
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
				int xo = centerX+(int)(ps[0]*tableSize);
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
					buttonHighlightColor, categoryMode ? buttonSelectedColor : buttonBackgroundColor))
				{
					bubbleSelect.hitCode = SeatId.StartButton;
				}
			if(restartable)
			{
				if(GC.handleRoundButton(gc,dr,bubbleSelect,s.get(DiscardGameMessage),
					buttonHighlightColor, categoryMode ? buttonSelectedColor : buttonBackgroundColor))
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
		G.setRotation(bubbleSelect, -rotation,centerX,centerY);
		GC.setRotation(gc,-rotation,centerX,centerY);
	}
	boolean allPlayersNamed()
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
		int left = G.Left(r);
		int top = G.Top(r);
		int step = majorAxis/(ncharts+1);
		int slowAxisPos = (portrait ? G.centerX(r) : G.centerY(r));
		int slowAxisTop = slowAxisPos - step/2;
		int fastAxisPos = (portrait ? top : left)+(majorAxis-step*(ncharts+1))/2;
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
	
	private CellId lastButton = null;
	private void drawGameSelector(Graphics gc,Rectangle r,HitPoint hp)
	{	GC.setFont(gc,largeBoldFont());
		boolean selectedGameSeen = false;
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
		int rows = 1+ (categoryMode ? categoryNames.length : (26+az_cols-1)/az_cols);
		int step = Math.max(4, Math.min(G.getFontSize(largeBoldFont())*3,(h+rows-1)/rows));
		int half = 2*step/3;
		int margin = half/10;
		int lessThanHalf = half-margin;
		int vspace = half+half/4;
		
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
		{Color fgColor = categoryMode ? buttonSelectedColor : buttonBackgroundColor;
		if(GC.handleRoundButton(gc, new Rectangle(catColumnLeft, t+half, catColumnWidth, step), 
        		hp, s.get(CategoriesMode),
                lastButton==SeatId.CategoriesSelected ? fgColor : buttonHighlightColor, fgColor))
		{	someButton = SeatId.CategoriesSelected;
		}
		}
		{
		Color fgColor = !categoryMode ? buttonSelectedColor : buttonBackgroundColor;
		if(GC.handleRoundButton(gc, new Rectangle(gameX, t+half, gameColumnWidth, step), 
        		hp, s.get(A_ZMode),
                lastButton==SeatId.A_Zselected ? fgColor : buttonHighlightColor, fgColor))
		{	someButton = SeatId.A_Zselected;
		}}
		int gameY = t+half*2+step;
		int variantY = gameY;
		int spaces = (h-step*6)/half;
		if(categoryMode)
		{	// game categories
			int ncats = categoryNames.length;
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
			}
			// display games within a category
			GameInfo gameNames[] = game.gameMenu(selectedCategory,typeClass,nplayers);
			if(gameNames.length==1) { selectGame(gameNames[0]); }
			for(int i=0;i<gameNames.length;i++)
			{
				GameInfo g = gameNames[i];
				String name = s.get(g.gameName);
				selectedGameSeen |= g==selectedGame;
				Color fgColor = g==selectedGame ? buttonSelectedColor : buttonBackgroundColor;
				int gtop = gameY+i*avspace;
				Rectangle gameRect = new Rectangle(gameX+half,gtop,gameColumnWidth-half,half);
				Rectangle iconRect = new Rectangle(gameX,gtop+margin/2,lessThanHalf,lessThanHalf);
				if(GC.handleRoundButton(gc, gameRect,hp,
						name,
						lastButton==SeatId.SelectGame ? fgColor : buttonHighlightColor, fgColor
						))
					{
						someButton = SeatId.SelectGame;
						hp.hitObject = g;
					}
				Image icon = g.getIcon();
				if(icon!=null)
				{
				icon.centerImage(gc,iconRect);
				GC.frameRect(gc, Color.black, iconRect);
				}
			}
		}
		else
		{	// a-z selection
			GameInfo gameNames[] = game.gameMenu(null, typeClass,nplayers);
			GameInfo sub[][] = new GameInfo['Z'-'A'+1][];
			for(char ch = 'A'; ch<='Z'; ch++) { sub[ch-'A'] = subgames(gameNames,ch); }
			
			int row = 0;
			int col = 0;
			char selection = selectedLetter.charAt(0);
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
					Rectangle re = new Rectangle(xp,yp,half,half);
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
			row = 0;
			GameInfo lastGame = null;
			int ngames = 0;
			GameInfo matches[] = (selection>='A' && selection<='Z') ? sub[selection-'A'] : gameNames;
			int avspace = Math.min(vspace, h/(matches.length+3));
			
			for(GameInfo g : matches)
			{	
				String name = g.gameName;
				char match = (""+(name.charAt(0))).toUpperCase().charAt(0);
				if(selection=='?' || (selection==match))
				{	selectedGameSeen |= g==selectedGame;
					Color fgColor = g==selectedGame ? buttonSelectedColor : buttonBackgroundColor;
					int gtop = gameY+row*avspace;
					Rectangle gameRect = new Rectangle(gameX+half,gtop,gameColumnWidth-half,half);
					Rectangle iconRect = new Rectangle(gameX,gtop+margin/2,lessThanHalf,lessThanHalf);
					if(GC.handleRoundButton(gc, gameRect,hp,
							s.get(name),
							lastButton==SeatId.SelectGame ? fgColor : buttonHighlightColor, fgColor
							))
					{
						someButton = SeatId.SelectGame;
						hp.hitObject = g;
					}
					Image icon = g.getIcon();
					if(icon!=null)
					{
					icon.centerImage(gc,iconRect);
					GC.frameRect(gc, Color.black, iconRect);
					}
					row++;
					lastGame = g;
					ngames++;
				}
			}
			if(ngames==1) { selectGame(lastGame); }

		}
		
		// if the selections have moved under the selected game, make it unselected.
		if(!selectedGameSeen) { selectGame(null); }
		
		if(selectedGame!=null)
		{	
			variantY = gameY;
			int hw = half*4-margin;
			Rectangle iconRect = new Rectangle(variantX+(gameColumnWidth-hw)/2,t-margin,hw,hw);
			Image icon = selectedGame.getIcon2();
			if(icon!=null)
			{
				icon.centerImage(gc, iconRect);
			}
			GC.frameRect(gc,Color.black,iconRect);
			GameInfo variations[] = selectedGame.variationMenu(selectedGame.gameName,typeClass,nplayers);
			if((variations!=null) && (variations.length>1) 
					&& (selectedChart!=null))					
			{	int avspace = Math.min(vspace, (h-vspace*4)/(variations.length+2));
				if(variantY + (avspace*(variations.length+4))>h) { variantY = t+avspace; }
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
		
		drawGameSelector(gc,gamer,hp);
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
		gearMenu.show(x,y);
	}
	private boolean serviceRunning()
	{
		return (VNCService.isVNCServer()||RpcService.isRpcServer());
	}
	public void drawCanvas(Graphics gc, boolean complete, HitPoint pt0) 
	{
		Keyboard kb = getKeyboard();
		HitPoint pt = pt0;
		if(kb!=null )
	        {  pt = null;
	        }

		if(complete) { fillUnseenBackground(gc); }
		
		GC.fillRect(gc, Color.lightGray,fullRect);
		
		String va = s.get(VersionMessage, G.getAppVersion());
		GC.Text(gc,false,versionRect,Color.black,null,va);

		drawSeatingCharts(gc,seatingSelectRect,pt);
		drawMainSelector(gc,seatingChart,gameSelectionRect,pt);
		if(G.debug()||G.isTable()) { StockArt.Gear.drawChip(gc, this, gearRect, pt,SeatId.GearMenu,s.get(ExitOptions)); }
		if(!G.isIOS() 
				&& GC.handleRoundButton(gc,startStopRect,pt,
						s.get(serviceRunning() ? StopTableServerMessage : StartTableServerMessage),
				buttonHighlightColor, buttonBackgroundColor))
			{
				pt.hitCode = SeatId.ToggleServer;
			}
		
		if(GC.handleRoundButton(gc,onlineRect,pt,s.get(PlayOnlineMessage),
				buttonHighlightColor, buttonBackgroundColor))
			{
				pt.hitCode = SeatId.PlayOnline;
			}
		if(!G.isIOS())
		{
		GC.TextRight(gc,tableNameRect,Color.black,null,s.get(TableNameMessage));
		namefield.setFont(largeBoldFont());
		namefield.redrawBoard(gc, pt);
		}
		if(kb!=null)
		{
			kb.draw(gc, pt0);
		}
		drawUnmagnifier(gc,pt0);
	}

	public void drawCanvasSprites(Graphics gc, HitPoint pt) 
	{
		if(pickedSource>=0)
		{
			Rectangle r = new Rectangle(G.Left(pt),G.Top(pt),colorW,colorW);
			GC.fillRect(gc, selectedGame.colorMap[colorIndex[pickedSource]],r);
			GC.frameRect(gc, Color.black, r);
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
		{
			String name = JOptionPane.showInputDialog(AddAName);
			if(name!=null)
			{ name = name.trim();
			  if(!"".equals(name)) { users.changeOfflineUser(name.trim(),remove); }
			}
			changeUser(false,changeSlot,userMenu.showAtX,userMenu.showAtY);
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
		if(gearMenu.selectMenuTarget(target))
		{
			SeatId me = (SeatId)gearMenu.rawValue;
			if(me!=null)
			{
				switch(me)
				{
				default: G.Error("Hit unexpected gear item %s",me);
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
	static String ExitOptions = "Exit options";
	public static String[]SeatingStrings =
		{	SelectChartMessage,
			TableNameMessage,
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
		};
	 public static String[][] SeatingStringPairs =
		 {
			{SeatPositionMessage,"Where Are\nYou Sitting?"}	 
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

    private Keyboard keyboard = null;
	private boolean useKeyboard = G.isCodename1();
    public void createKeyboard()
    {	if(useKeyboard)
    	{
    	keyboard = namefield.makeKeyboardIfNeeded(this,keyboard);
    	}
    }
    public void closeKeyboard()
    {
    	Keyboard kb = keyboard;
    	if(kb!=null) { kb.setClosed(); }
    }
    private void loseFocus()
    {
    	if(useKeyboard)
    	{
    		namefield.setFocus(false);
    	}
    	UDPService.setPlaytableName(namefield.getText().trim());
    }
    public Keyboard getKeyboard() 
    { Keyboard k = keyboard;
      if(k!=null && k.closed) 
      	{ k = keyboard = null; 
      	  loseFocus();
      	}
      return(k); 
    }



}
