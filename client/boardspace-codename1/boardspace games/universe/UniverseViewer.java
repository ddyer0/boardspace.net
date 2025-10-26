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
package universe;


import bridge.Color;
import bridge.JMenuItem;
import common.GameInfo;

import com.codename1.ui.geom.Rectangle;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

import online.common.*;
import online.game.*;
import online.game.sgf.sgf_game;
import online.game.sgf.sgf_gamestack;
import online.game.sgf.export.sgf_names;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.CommonDriver;
import online.search.SimpleRobotProtocol;
import lib.*;

/**
 * 
 * Change History
 *
*/
public class UniverseViewer extends CCanvas<UniverseCell,UniverseBoard> implements UniverseConstants
{
    /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// for the polysolver    
	Hashtable<Long,CommonMoveStack >solutions = new Hashtable<Long,CommonMoveStack >();
	Hashtable<Integer,sgf_gamestack> givens = new Hashtable<Integer,sgf_gamestack>();
	Hashtable<sgf_game,int[]>gameKeys = new Hashtable<sgf_game,int[]>();
	JMenuItem findGivensItem = null;
	int number_of_solutions = 0;
	double lineStrokeWidth = 1;
    //
    // combine the givens at the specified indexes, and return true
    // if that set of givens uniquely identifies the game.  This is part
    // of the logic to find minimal sets of givens for a nudoku solution
    //
    boolean combineGivens(sgf_game game,int keys[],IStack indexes)
    {	
    	int target_count = indexes.size();
    	Hashtable<sgf_game,Integer>counts = new Hashtable<sgf_game,Integer>();
    	for(int idx = 0; idx<target_count; idx++)
    	{
    		int key = keys[indexes.elementAt(idx)];
    		// games is the list of games with a particular key as one of their cells.
    		// for example key A 1 3 means A 1 contains 3  
    		sgf_gamestack games = givens.get(key);
    		int ngames = games.size();				// number of games which have this key
    		for(int gi = 0;gi<ngames; gi++)
    		{
    			sgf_game keygame = games.elementAt(gi);
    			Integer count = counts.get(keygame);
    			// count the number of times this game is found
    			int countval = count==null ? 1 : count.intValue()+1;
    			counts.put(keygame,countval);
    		}
    	}
    	//
    	// now each cell in counts is the number of times a game is found among the
    	// set of solutions.  For the game of interest, this ought to be all the time.
    	// if we've found a unique set of keys, it will be the only one that has all
    	//
    	int actual_count = 0;
    	boolean found_target_game = false;
    	for(Enumeration<sgf_game> keyenum = counts.keys(); keyenum.hasMoreElements();)
    	{
    		sgf_game keygame = keyenum.nextElement();
    		int keycount = counts.get(keygame);
    		if(keycount==target_count) { actual_count++; }
    		if(keygame==game) { found_target_game = true; }
    		
    	}
    	G.Assert(found_target_game,"didn't find the original game every time");
    	return(actual_count==1);
    }
    
    // construct a permutation from the permutation number.  This is used to select
    // a unique set of cells as a possible set of givens with a unique solution.
    IStack makeKeySet(IStack result,int keys[],int permutation[],int idx,int depth)
    {	int len = keys.length-depth;
    	int rem = idx% len;
    	int div = idx/len;
    	int save = keys[rem+depth];
    	result.push(permutation[save]);
    	keys[rem+depth] = keys[depth];
    	keys[depth] = save;			// reorder the indexes
    	if((div>0)&&(len>1))
    	{	makeKeySet(result,keys,permutation,div,depth+1);
    	}
    	return(result);
    }
    //
    // find the best available givens by brute force, trying 1 key, then 2 keys, then 3 etc.
    //
    void findBestGivens(sgf_game game, int ntofind, int ntotry)
    {	int keys[] = gameKeys.get(game);
    	int keyIndexes[] = new int[keys.length];
    	Random r = new Random(game.toString().hashCode());
    	int randomPermuation[] = r.shuffle(AR.intArray(keys.length));
     	int nresults = 0;
    	int permutations = 0;
    	String messages = "";
    	IStack keyset = new IStack();
    	int keylen = 0;
    	while((nresults<ntofind) && (keylen<=ntotry))
    	{	keyset.clear();
    		for(int i=0;i<keyIndexes.length; i++) { keyIndexes[i]=i; }
    		// the purpose of the random permutation is so the keys are actually used in a random order,
    		// and therefore the givens we find won't be clustered anywhere.  It would be boring if all
    		// the givens started with A1...
    		makeKeySet(keyset,keyIndexes,randomPermuation,permutations,0);
    		keylen = keyset.size();
    		if((keylen<=ntotry) && combineGivens(game,keys,keyset))
    		{	int ngivens = keyset.size();
    			String msg = "  Givens("+ngivens+"): ";
    			String comma = "";
     			for(int i=0;i<ngivens;i++)
    			{
    				int key = keys[keyset.elementAt(i)];
    				// decode the key back into it's component column, row, and sudoku value
    				int value = key>>16;
    				char col = (char)((key>>8)&0xff);
    				int row = key&0xff;
    				msg += comma+col+row+"="+value;
    				comma = ", ";
      			}
     			msg += "\n";
  				System.out.println(""+game+" "+msg);
				nresults++;
  				messages += msg;
    		}
    		permutations++;
    	}
    	if(nresults==0)
    	{ 	// this generally means that the same set of numbers was achieved by several
    		// different geometries.  Unavoidable when the set of tiles contains chains of
    		// 2's for example 2 5 over 5 3 can also be 3 5 over 5 2
    		messages = ""+game+" no unique given sets <= "+ntotry; 
    		//System.out.println(messages);
    	}
    	game.getRoot().set_property(sgf_names.gametitle_property,messages);
    }
    void findBestGivens(int ntofind,int ntotry)
    {	for(Enumeration<sgf_game> gameenum = gameKeys.keys(); gameenum.hasMoreElements(); )
    	{	sgf_game game = gameenum.nextElement();
    		findBestGivens(game,ntofind,ntotry);
    	}
    }
    public void findBestGivens()
    {
    	findBestGivens(3,3);
    }
    // record all the cells for a particular board and game, so the givens table
    // for a particular key will contain all the games that have that cell filled 
    // with that value.
    void recordGivens(UniverseBoard evboard,sgf_game game)
    {	int keys[] = new int[evboard.boardColumns*evboard.boardRows];
    	int keyindex = 0;
    	for(UniverseCell c = evboard.allCells; c!=null; c=c.next)
    	{	int val = c.sudokuValue;
    		int key = val<<16 | c.col<<8 | c.row;
    		sgf_gamestack current = givens.get(key);
    		if(current==null) { current = new sgf_gamestack(); }
    		keys[keyindex++] = key;
    		current.push(game);
    		givens.put(key,current);
    	}
    	gameKeys.put(game,keys);
    }
    //
    // record this solution if it appears to be unique.
    //
    void recordSolution(UniverseBoard evboard,CommonDriver search_state)
    {	if(evboard.rules.isNudoku())
    	{
    	long digest = evboard.Digest();
    	if(solutions.get(digest)==null)
		{ 	// TODO: determine why some apparently identical games end up here.
			number_of_solutions++;
			String exclude = "";
			commonMove var = search_state.getCurrentVariation();
			//var.showPV("Solution "+number_of_solutions+" ");
			CommonMoveStack hist = new CommonMoveStack();
			
			{
				CommonMoveStack vhist = History;
				for(int i=0,lim=vhist.size();i<lim;i++)
				{	// copy the game before starting
					UniverseMovespec elem = (UniverseMovespec)vhist.elementAt(i);
					hist.push(elem.Copy(null));
				}
			}
			while(var!=null)
			{	// add the moves in this solution
				hist.push(var);
				//UniverseMovespec svar = (UniverseMovespec)var;
				//if(svar.declined!=null) 
				//{ exclude += " "+svar.declined; }
				var = var.best_move();
			}
			solutions.put(digest,hist);
			sgf_game game = addGame(hist,"Solution #"+number_of_solutions+exclude);
			recordGivens(evboard,game);				
			}
    	}
	}
	
     // colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color rackBackGroundColor = new Color(194,175,148);
    private Color boardBackgroundColor = new Color(220,165,155);
    
    // images
    private static Image[] textures = null;// background textures
    private static StockArt[] images = null;	// random artwork
    // private undoInfo
    private UniverseBoard b = null; 	// the board from which we are displaying
    private int CELLSIZE; 			// size of the layout cell.  
    private int SQUARESIZE;			// size of a board square
    private int DISPLAYSQUARE;
    
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle[] playerRacks = addRect("Rack",4);
    private Rectangle[] playerScore = addRect("Score",4);

    private Rectangle resignRect = addRect("resignRect");
    private Rectangle rotatorRect = addRect(".rotator");
    private Rectangle rotator2Rect = addRect(".rotator2");
    private Rectangle rotators[] = { rotatorRect,rotator2Rect };
    private Rectangle generateRect = addRect("generate");
    private Rectangle patternRect = addRect("patternRect");
    private Rectangle validRect = addRect("validRect");
    private Rectangle givensRect = addRect("givensRect");
    private Rectangle takensRect = addRect("takensRect");
     
  
    public commonMove EditHistory(commonMove m)
    {	if(m.op!=MOVE_ASSIGN) { m = super.EditHistory(m); }
    	return(m);
    }
    public synchronized void preloadImages()
    {	
       	UniverseChip.preloadImages(loader,ImageDir);
       	
        if (images == null)
    	{ // note that for this to work correctly, the images and masks must be the same size.  
          // Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
        textures = loader.load_images(ImageDir,TextureNames);
        images = StockArt.preLoadArt(loader, ImageDir, ArtNames,ArtScales); 
     	}
        gameIcon = textures[ICON_INDEX];
    }


	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	// for games with more than two players, the default players list should be 
    	// adjusted to the actual number, adjusted by the min and max
       	// int players_in_game = Math.max(3,info.getInt(exHashtable.PLAYERS_IN_GAME,4));
    	enableAutoDone = true;
    	super.init(info,frame);
       	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
    	//
        int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
        int np = Math.max(1,info.getInt(OnlineConstants.PLAYERS_IN_GAME,2));
        String gameType = info.getString(GameInfo.GAMETYPE, variation.Universe.name);
        b = new UniverseBoard(gameType,randomKey,np,getStartingColorMap(),UniverseBoard.REVISION);
        GameInfo gi = info.getGameInfo();
        adjustPlayers(b.nPlayers());	// players can be adjusted in init
        if(gi!=null) 
        	{ MouseColors = gi.colorMap; 
        	}
        if(b.rules.isNudoku())
        {
        	findGivensItem = myFrame.addAction("find givens",deferredEvents);
        }
        useDirectDrawing(true);
        doInit(false);
         
     }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {	//System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        b.doInit(b.gametype,b.randomKey,b.nPlayers(),b.revision);			// initialize the board
        if(!preserve_history)
    	{ startFirstPlayer();
    	}

    }


    private double aspect = 1.0;
    private boolean flatten = false;
    
    public Rectangle createPlayerGroup(int player,int x,int y,double rotation,int unit)
    {	
    	int rows = b.rules.numberOfShapes()<20 ? 2 : 3;
    	commonPlayer pl0 = getPlayerOrTemp(player);
    	int u2 = unit/2;
		int sz = unit*2;
		Rectangle rack = playerRacks[player];
		Rectangle done = doneRects[player];
		int doneW = plannedSeating() ? sz*2 : 0;
		Rectangle score = playerScore[player];
		Rectangle box = pl0.createRectangularPictureGroup(x+sz,y,unit);
		G.SetRect(score, x, y, sz, sz);
		if(flatten)
		{
			G.SetRect(done,x+u2,G.Bottom(box)+u2,doneW,doneW/2);		
		}
		else 
		{
		G.SetRect(done, G.Right(box)+u2,y+u2,doneW,doneW/2);
		}
		G.union(box,done,score);
		int rackw = unit*22;
		int rackh = (int)((unit*rows*5/2));
		if(aspect>1) 
			{	
			int area = rackw*rackh;
			rackh = Math.max(G.Height(box),(int)(unit*rows*2));
			rackw = area/rackh;
			}
		else if (aspect<1)
			{
			int area = rackw*rackh;
			rackw = G.Width(box);
			rackh = area/rackw;
			}
		if(flatten)
		{
			G.SetRect(rack,G.Right(box)+u2,y,rackw,rackh);			
		}
		else
		{
		G.SetRect(rack,x+u2,G.Bottom(box),rackw,rackh);
		}
		pl0.displayRotation = rotation;
		G.union(box, rack);
	    return(box);
    }
    double aspects[] = {0.9,1.0,1.1,-0.9,-1,-1.1};
    public void setLocalBounds(int x,int y,int width,int height)
    {
    	setLocalBoundsV(x,y,width,height,aspects) ;
    }
    public double setLocalBoundsA(int x,int y,int width,int height,double v)
    {	flatten = v<0;
    
    	// this is slightly nonstandard, we use the variable parameter v to change
    	// the aspect ratio of the player's rack, rather than of the board.
    	aspect = Math.abs(v);
    	G.SetRect(fullRect, x, y, width, height);
        boolean isNudoku = b.rules.isNudoku();
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();		// always 3 for triad
        int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogW = fh*14;	
       	int minChatW = fh*35;	
        int minLogH = fh*10;	
        int margin = fh/2;
        int ncols = 20;
        int buttonW = fh*8;

        // this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,	
    			0.5,		// % of space allocated to the board
    			0.9,		// aspect ratio for the board
    			fh*2.5,
    			fh*3.5,		// maximum cell size based on font size
    			0.1		// preference for the designated layout, if any
    			);
    	
    	// place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
    	// however, if that doesn't work out the main rectangle will shrink.
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,logRect,
    			minLogW, minLogH, minLogW*3/2, minLogH*2);

        layout.placeTheVcr(this,minLogW,minLogW*3/2);
        layout.placeDoneEditRep(buttonW, buttonW*2, doneRect,editRect,resignRect);
        
        if(isNudoku)
        	{
        int unitsize = fh;
        layout.placeRectangle(patternRect, unitsize*10, unitsize*3,BoxAlignment.Edge);        	
        layout.placeRectangle(validRect, unitsize*10, unitsize*3,BoxAlignment.Edge);        	
        layout.placeRectangle(generateRect, unitsize*10, unitsize*3,BoxAlignment.Edge);        	
        layout.placeRectangle(givensRect, unitsize*30, unitsize*3,BoxAlignment.Edge);        	
        layout.placeRectangle(takensRect, unitsize*30, unitsize*3,BoxAlignment.Edge);        	
        }

        Rectangle main = layout.getMainRectangle();
        int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
        int stateH = fh*5/2;
    	
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)(mainH-stateH*2)/ncols);
    	CELLSIZE = (int)cs;
        SQUARESIZE = CELLSIZE;
    	// center the board in the remaining space
    	int boardW = (int)(ncols*cs);
    	int boardH = (int)(ncols*cs);
    	
    	int extraW = (mainW-CELLSIZE*2-boardW)/2;
    	int extraH = (mainH-boardH-stateH*2)/2;
    	int boardX = mainX+extraW+stateH;
    	int boardY = mainY+stateH+extraH;
    	int boardBottom = boardY+boardH;
    	int stateY = boardY-stateH;
    	int boardRight = boardX+boardW;
    	layout.returnFromMain(extraW,extraH);
    	placeStateRow( boardX+stateH,stateY,boardW-stateH,stateH,iconRect,stateRect,annotationMenu,noChatRect);

    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	lineStrokeWidth = boardW/400.0;
    	int sz = CELLSIZE*6;
        int cx = boardX+boardW/2;
        int cy = boardY+boardH/2;
        boolean success = false;
     	for(int edge=0,rindex=0;edge<4&&rindex<rotators.length;edge++)
     	{	Rectangle target = rotators[rindex];
     		G.SetHeight(target, 0);
      		switch(edge)
     		{
     		default:
     		case 0:		
     			{
     			if(width-boardRight > sz)
     				{
     				G.SetRect(target,boardRight,cy-sz/2,sz,sz);
     				success = true;
     				rindex++;
     				}
     			}
     			break;
     		case 1:
     			{
     			if(boardX > sz)
     				{
     				G.SetRect(target,boardX-sz,cy-sz/2,sz,sz);
     				success = true;
     				rindex++;
     				}
     			}
     			break;
     		case 2:
     			{
     			if(boardY > sz)
     				{
     				G.SetRect(target, cx-sz/2, boardY-sz,sz,sz);
     				success = true;
     				rindex++;
     				}
     			}
     			break;
     		case 3:
 				{
 				if(height-boardBottom > sz)
 					{
 					G.SetRect(target, cx-sz/2, boardBottom,sz,sz);
     				success = true;
 					rindex++;
 					}
 				}
 			break;
     		}
     	}
 
     	if(!success) { return(0); }	// not acceptable, no place for the rotators
     	
    	placeRow( boardX, boardBottom, boardW, stateH,goalRect,viewsetRect);
        setProgressRect(progressRect,goalRect);
        	
        positionTheChat(chatRect,Color.white,Color.white);
        Rectangle box = getPlayerOrTemp(0).playerBox;
        return(boardW*boardH+G.Width(box)*G.Height(box));
    }

    
    private void drawScore(Graphics gc,Rectangle r,UniverseBoard gb,int pl)
    {	GC.setFont(gc,largeBoldFont());
    	UniverseChip ch = UniverseChip.getChip(gb.playerColor[pl],0);
    	ch.drawChip(gc, this, r, ""+gb.scoreForPlayer(pl));
    	GC.frameRect(gc,Color.black,r);
    }
    private boolean drawPickedTile(Graphics gc,HitPoint hgh,Rectangle r,UniverseBoard gb,UniverseChip ch)
    {	UniverseChip picked = gb.pickedObject;
    	UniverseCell ps = gb.getSource();
    	if((picked!=null) && (ps!=null) && (ps.rackLocation!=UniverseId.TakensRack) /*&& (picked.nVariations()>1*/)
    	{
        boolean ourTurn = OurMove();
        HitPoint highlight = ourTurn ? hgh : null;
        boolean inrotator = G.pointInRect(highlight,r);
         if(inrotator)
         {
        	 highlight.hitCode = UniverseId.RotateNothing;
         }
    	int ss = gb.cellSize();
    	Rectangle bound = ch.boundingBox(ss,0,0);
    	int arrowSize = G.Width(r)/4;
    	int drawX = G.Left(r) - G.Left(bound) + (G.Width(r)-G.Width(bound)+ss)/2;
    	int drawY = G.Top(r) - G.Top(bound) + (G.Height(r)-G.Height(bound)+ss)/2 ;
    	GC.setColor(gc,rackBackGroundColor);
    	GC.fillRect(gc,r);
    	ch.drawChip(gc,this,ss,drawX,drawY,null);
    	// mark the attachment point on the floating chip
    	StockArt.SmallO.drawChip(gc,this,ss,drawX,drawY,null);
    	if((ch.getVariation(ch.rotated+1,ch.flipped)!=ch) 
    		&& images[ROTATECW_INDEX].drawChip(gc,this,arrowSize/2,G.Right(r)-arrowSize/2,G.Top(r)+arrowSize/2,
    				highlight,ch.flipped?UniverseId.RotateCCW:UniverseId.RotateCW,null,2.5,1.33))
    	{	
    		GC.frameRect(gc,HighlightColor,G.Right(r)-arrowSize,G.Top(r)+1,arrowSize-2,arrowSize-2);
     	}
    	if((ch.getVariation(ch.rotated+3,ch.flipped)!=ch)
    			&& images[ROTATECCW_INDEX].drawChip(gc,this,arrowSize/2,G.Left(r)+arrowSize/2,G.Top(r)+arrowSize/2,
    					highlight,ch.flipped?UniverseId.RotateCW:UniverseId.RotateCCW,null,2.5,1.33))
    	{
    		GC.frameRect(gc,HighlightColor,G.Left(r)+1,G.Top(r)+1,arrowSize-2,arrowSize-2);
    	}
    	if(gb.LegalToFlip() && ch.getFlippedPattern(!ch.flipped)!=ch)
    	{
    	if(images[FLIP_INDEX].drawChip(gc,this,arrowSize/2,G.Left(r)+G.Width(r)/2,G.Top(r)+arrowSize/2,highlight,UniverseId.FlipCell,null,2.5,1.33))
    	{
    		GC.frameRect(gc,HighlightColor,G.centerX(r)-arrowSize/2,G.Top(r)+1,arrowSize-2,arrowSize-2);
    	}}

    	GC.frameRect(gc,Color.black,r);
     	return(inrotator);
    	}
    	else {    return(false); }
    }
    
    private Rectangle drawCommonChipPool(int square,Graphics gc, UniverseBoard gb,commonPlayer player,UniverseCell chips[], Rectangle r, HitPoint highlight)
	{	Rectangle brect = new Rectangle();
		int chipOffset = gb.chipOffset;
		int forPlayer = player.boardIndex;
		int nchips = chips.length;
		int maxHeight = square*2;
		int maxHeightThisRow = 0;
		int rightBound = G.Right(r);
		int resetCx = G.Left(r);
		int cx = resetCx;				// beginning of the x row
		int cy = G.Top(r)+square;			// top of the first y row
		boolean canHit = gb.LegalToHitChips(forPlayer);
		UniverseChip picked = gb.pickedObject;
		    for(int i=0;i<nchips;i++)
		    {
		   	UniverseCell thisCell = chips[i];
		    UniverseChip thisChip = thisCell.topChip();
		    UniverseChip protoChip = (thisChip==null)
		    			?UniverseChip.getChip(gb.playerColor[forPlayer],chipOffset+i) 
		    			: thisChip;
		    
		    Rectangle bounds = protoChip.boundingBox(square,0,0);
		    int sw = G.Width(bounds);
		    int sh = G.Height(bounds);
		    maxHeightThisRow = maxHeight = Math.max(Math.min(sw, sh),maxHeight);	// keep track of the row height and current x position
		    if(cx +sw>rightBound)
		    {	// skip down to the next row
		    	G.SetWidth(brect, Math.max(G.Width(brect),cx-resetCx));
		    	cy += maxHeight+square/2;
		    	cx = resetCx;
		    	// max height increases as we go, so don't reset it.
		    	// maxHeight = 0;
		    	maxHeightThisRow = sh;
		    }
		//   G.print("b ",i,protoChip," ",cx," ",cy," ",sw,"x",sh);
		//     { GC.frameRect(gc,Color.blue,cx,cy,sw,sh);}
		    int yoff = (maxHeight-sh)/2;
		    int myx = cx - G.Left(bounds);		// x and y where we will draw
		    int myy = cy  +yoff- G.Top(bounds);
		    //G.print(""+i+" "+protoChip+bounds+" "+myx+" "+myy);
		    player.rotateCurrentCenter(thisCell,myx,myy);
		    boolean canPick = (picked==null) && (thisChip!=null);
		    boolean canDrop = (picked != null) && (thisChip==null);
		    UniverseChip top = thisCell.topChip();
		    boolean canMove = (top!=null) && gb.canBePlayed(thisCell);
		    HitPoint pt = (canHit && canPick && (canMove || (thisCell.rackLocation==UniverseId.TakensRack)))? highlight : null; 
		    String msg = canMove ? null : "x"; 
		    Rectangle targetSquare = new Rectangle(cx-square/2,cy-square/2,sw,sh);
		    boolean hit = thisCell.closestPointToCell(pt,targetSquare);
		    if(thisCell.drawStack(gc,pt,myx,myy,this,0,square,0,msg) || hit)
		    {	pt.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
		    	pt.awidth = square*2;
		    	pt.spriteRect = targetSquare;
		    	pt.spriteColor = Color.red;
		    }
		     
		    if( (picked!=null) 
		    		&& (gb.playerColor[forPlayer]==picked.color)
		    		&& (i==(picked.getPatternIndex()-gb.rackOffset)))
		    	{
		    	int xpos = cx+sw/2;
		    	int ypos = cy+sh/2;
		    	StockArt.SmallO.drawChip(gc,this,square*2,xpos,ypos,null);
		    	if(canDrop 
		    			&& G.pointInRect(highlight,r)
		    			&& !G.pointInRect(highlight,rotatorRect)
		    			&& !G.pointInRect(highlight,rotator2Rect))
		    	{
		    		thisCell.registerChipHit(highlight,xpos,ypos,square*2,square*2);
		    		highlight.arrow = StockArt.DownArrow;
		        	highlight.awidth = square*2;
		        	highlight.spriteRect = r;
		        	highlight.spriteColor = Color.red;
		    	}
		    	}
		    
		    cx += sw+square/2;
		  }
		G.SetHeight(brect, cy-G.Top(r) + maxHeightThisRow);
		
		G.SetWidth(brect,Math.max(G.Width(brect),cx-resetCx));
		return(brect);
	}
	// draw a box of spare chips. Notice if any are being pointed at.  Highlight those that are.
    private void DrawCommonChipPool(Graphics gc, UniverseBoard gb,int playerIndex,
    		UniverseCell rack[],Rectangle r,Rectangle done, int who, HitPoint highlight)
    {	commonPlayer player = getPlayerOrTemp(playerIndex);
   	
    switch(gb.rules)
    {	
 		case Phlip:
 		case Diagonal_Blocks:
 		case Blokus_Duo:
 		case Diagonal_Blocks_Duo:
		case Blokus:
 		case Blokus_Classic:
 		case Diagonal_Blocks_Classic:
 			drawScore(gc,playerScore[player.boardIndex],gb,player.boardIndex); 
 			break;
 		default: break;
 		}

    	// it proved to be difficult to decide what the largest possible size to draw the chips
    	// in the allowed space, so we start with full size and try all sizes until one fits.
    	// this is hugely inefficient. Ho hum.
     	for(int i = SQUARESIZE;  i>1; i--)
    	{	
    		Rectangle area = drawCommonChipPool(i,null,gb,player,rack,r,null);
    		if(G.Height(area)<G.Height(r)) 
    		{
    			drawCommonChipPool(i,gc,gb,player,rack,r,highlight);
    			DISPLAYSQUARE = i;
    			break;
    		}
    	} 
     	if((done!=null) && plannedSeating() && (who==playerIndex))
     		{
      		handleDoneButton(gc,done,(gb.DoneState() ? highlight : null),HighlightColor,rackBackGroundColor);
     		}
    }

    //
    // sprites are normally a game piece that is "in the air" being moved
    // around.  This is called when dragging your own pieces, and also when
    // presenting the motion of your opponent's pieces, and also during replay
    // when a piece is picked up and not yet placed.  While "obj" is nominally
    // a game piece, it is really whatever is associated with b.movingObject()
    //
    public void drawSprite(Graphics g,int obj,int xp,int yp)
    {  	// draw an object being dragged
    	UniverseChip ch = UniverseChip.getIsomer(obj);// Tiles have zero offset
    	boolean inBoard = G.pointInRect(xp,yp,boardRect);
    	int size = inBoard?b.cellSize():DISPLAYSQUARE;
    	ch.drawChip(g,this,size,xp,yp,null);
    	// mark the attachment point on the floating chip
    	StockArt.SmallO.drawChip(g,this,size,xp,yp,null);
     }



    //** this is used by the game controller to supply entertainment strings to the lobby */
    // public String gameProgressString()
    // {	// this is what the standard method does
    // 	// return ((mutable_game_record ? Reviewing : ("" + viewMove)));
    // 	return(super.gameProgressString());
    // }


    /* draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    public void drawFixedElements(Graphics gc)
    {boolean review = reviewMode() && !mutable_game_record;
     UniverseBoard gb = disB(gc);
      // erase
      GC.setColor(gc,review ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
      if(review)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
      setDisplayParameters(gb,boardRect);
      
      for(UniverseCell c = gb.allCells; c!=null;  c=c.next)
      {	int ypos = G.Bottom(boardRect) - gb.cellToY(c.col, c.row);
      	int xpos = G.Left(boardRect) + gb.cellToX(c.col,c.row);
      	images[c.cellImageIndex].drawChip(gc,this,gb.cellSize(),xpos,ypos,null);
      	c.rotateCurrentCenter(gc,xpos,ypos);
      }
    	 
      gb.DrawGrid(gc,boardRect,use_grid,Color.white,Color.black,Color.blue,Color.black);
    }
    private void showSudokuValue(int value,Graphics gc,int SQUARE,int xpos,int ypos)
    {
        
        String msg = ""+value;// ""+(cell.regionNumber);//+" "+(cell.sweep_counter%100);
        GC.setFont(gc,largeBoldFont());
        StockArt.SmallO.drawChip(gc,this,SQUARE*2,xpos,ypos,msg); 
    }
    
    public Rectangle drawImage(Graphics gc, Image im, double scale[],int x, int y, 
    		double boxw,double xscale,double jitter,String text,boolean artCenterText)
    {	boolean sudo = (text!=null) && (text.length()>=1) && (text.charAt(0)=='#');
    	Rectangle rr = super.drawImage(gc,im,scale,x,y,boxw,xscale,jitter,sudo ? null : text, artCenterText);
    	if(sudo)
    	{
    		showSudokuValue((text.charAt(1)-'0'),gc,(int)(boxw*scale[2]),x,y);
    	}
    	return(rr);
    }
   /* draw the board and the chips on it. */
    private void drawBoardElements(Graphics gc, UniverseBoard gb, Rectangle brect, HitPoint high)
    {
     	//
        // now draw the contents of the board and anything it is pointing at
        //    	
        UniverseCell hitCell = null;
        variation rules = gb.rules;
        int SQUARE = gb.cellSize();
        int bottom = G.Bottom(brect);
        int left = G.Left(brect);
        boolean moving = gb.pickedObject !=null; 
        if(moving&&(high!=null))
        {	UniverseCell close = gb.closestCell(high,brect);
        	if(close!=null)
        	{	if(gb.LegalToHitBoard(close))
        		{	hitCell = close;
        			close.registerChipHit(high,left+gb.cellToX(close),bottom-gb.cellToY(close),
        					SQUARE,SQUARE);

        		}
        	}
        }
        // draw any givens that can be seen
        {
        CellStack givens = b.givens;
        for(int idx = 0,limit = givens.size(); idx<limit; idx++)
        {	UniverseCell c = givens.elementAt(idx);
        	UniverseChip given = c.getGiven();
        	if(given!=null && (c.topChip()==null))
        	{	int ypos =bottom - gb.cellToY(c);
            	int xpos = left + gb.cellToX(c);
            	
        		if(given.drawChip(gc,this,SQUARE,xpos,ypos, high,UniverseId.GivenOnBoard,null))
        		{	hitCell = c;
        			high.hitObject = c;
        		}
        	}
        }}
        // conventionally light source is to the right and shadows to the 
        // left, so we want to draw in right-left top-bottom order so the
        // solid parts will fall on top of existing shadows. 
        // when the rotate view is in effect, top and bottom, left and right switch
        // but this iterator still draws everything in the correct order for occlusion
        // and shadows to work correctly.
        boolean some  = false;
        for (UniverseCell orow[] : gb.chipLocation)
        {	for(UniverseCell cell : orow)
        	{
            if((cell!=null) && cell.onBoard)
            {
            some = true;
            HitPoint highlight = gb.LegalToHitBoard(cell) ? high : null;
            int ypos = bottom - gb.cellToY(cell);
            int xpos = left + gb.cellToX(cell);
            if( cell.drawStack(gc,highlight,xpos,ypos,this,0,SQUARE,0.0,null)) 
            	{ 
            	
            	hitCell = cell; 
            	}
            //StockArt.SmallO.drawChip(gc,this,SQUARESIZE,xpos,ypos,null);

            }
        	}
    	}
        // this sets the position for animation of cells which are occupied
        // but haven't been displayed at the time the animation is started
        if(!some)
        	{ for(UniverseCell c = gb.allCells; c!=null; c=c.next)
        		{
                int ypos = bottom - gb.cellToY(c);
                int xpos = left + gb.cellToX(c);
                c.rotateCurrentCenter(gc,xpos,ypos);
        		}
        	}
        
        // draw dots on the most recently played piece 
        {
        UniverseCell last = gb.lastMove;
        if(last!=null)
        {
        UniverseChip lastTop = last.topChip();
        if(lastTop!=null)
        {
        for(UniverseCell cell = gb.allCells; cell!=null; cell=cell.next)
         {	
         	int ypos = bottom - gb.cellToY(cell);
         	int xpos = left + gb.cellToX(cell);
         	cell.setCurrentCenter(xpos,ypos);
        	if(cell.topChip()==lastTop)
        	{	
        		StockArt.SmallO.drawChip(gc,this,SQUARE,xpos,ypos,null);
        	}
         }}}}
        switch(rules)
        {
        
		case Diagonal_Blocks:
		case Diagonal_Blocks_Duo:
		case Blokus:
		case Blokus_Duo:
			if(high!=null)
			{
			for(UniverseCell cell = gb.allCells; cell!=null; cell=cell.next )
			{
			if(gb.LegalToHitBoard(cell))
			{
			int xpos = left + gb.cellToX(cell);
			int ypos = bottom - gb.cellToY(cell);
			// show the touch points for placing a piece.  These can be 
			// hard to guess on mobiles
			StockArt.SmallO.drawChip(gc,this,b.cellSize(),xpos,ypos,null);
			}}}
			break;
        case Nudoku_9x9:
        case Nudoku_6x6:
        default:
        	// draw the sudoku values
            for(UniverseCell cell = gb.allCells; cell!=null; cell=cell.next)
            {	
                int ypos = bottom - gb.cellToY(cell);
                int xpos = left + gb.cellToX(cell);
                int v = cell.sudokuValue;
                if(v==0) { v = cell.nMoves; }
                if(v>0) { showSudokuValue(v,gc,SQUARE,xpos,ypos); }
           }        	
        	break;
        case Sevens_7:
        	// draw the sudoku values
            for(UniverseCell cell = gb.allCells; cell!=null; cell=cell.next)
            {	
                int ypos = bottom - gb.cellToY(cell);
                int xpos = left + gb.cellToX(cell);
                int v = cell.sudokuValue;
                if(v>0)
                	{ showSudokuValue(v,gc,SQUARE,xpos,ypos);
                	  UniverseCell next = cell.nextInBox;
                	  if((gc!=null) && (next!=null) && next.isAdjacentTo(cell))
                	  	{
                		  int ypos2 = bottom - gb.cellToY(next);
                		  int xpos2 = left + gb.cellToX(next);
                		  if(use_grid) { GC.drawArrow(gc,xpos,ypos,xpos2,ypos2,10,lineStrokeWidth); }
                	  	}
                	}
            }
            break;
        case Universe:
        case Pan_Kai:
        	// draw the region values
            //for(UniverseCell cell = gb.allCells; cell!=null; cell=cell.next)
            //{
            //    char thiscol = cell.col;
            //    int row = cell.row;
            //    int ypos = (brect.y + brect.height) - gb.cellToY(thiscol, row);
            //    int xpos = brect.x + gb.cellToX(thiscol, row);
            //    String msg = ""+cell.regionNumber;;
            //    if(cell.topChip()!=null) 
            //    	{ StockArt.SmallO.drawChip(gc,this,SQUARE,xpos,ypos,msg); }
            //    else { G.Text(gc,true,xpos-SQUARE/2,ypos-SQUARE/2,SQUARE,SQUARE,Color.white,null,msg);
            //   	}
           //} 
            break;
        
        }

        if(hitCell!=null)
        {	// draw a highlight rectangle here, but defer drawing an arrow until later, after the moving chip is drawn
      		high.arrow = moving 
      			? StockArt.DownArrow 
      			: StockArt.UpArrow;
            high.spriteColor = Color.red;
            high.awidth = SQUARE/2;
        }
       // {	for(UniverseCell c = gb.allCells; c!=null; c=c.next)
    	//{
    	//int ypos = (brect.y + brect.height) - gb.cellToY(c.col, c.row);
    	//int xpos = brect.x + gb.cellToX(c.col, c.row);
    	//if(c.diagonalResult==diagonalSweepResult.diagonal) 
       // 		{G.Text(gc, false,xpos,ypos,SQUARESIZE-SQUARESIZE/4,SQUARESIZE/4,Color.black,null,"D");
    //		}
    	//if(c.diagonalResult==diagonalSweepResult.adjacent) 
    	//	{G.Text(gc, false,xpos,ypos,SQUARESIZE-SQUARESIZE/4,SQUARESIZE/4,Color.black,null,"a"); }
    	//}
    	//}

    }
    
    PopupManager pattern = new PopupManager();
    public void patternMenu()
    {	pattern.newPopupMenu(this,deferredEvents);
    	  for(variation target : variation.values())
    	  {   if(target.isNudoku())
    			  {
    		  		pattern.addMenuItem(target.name,target);
    			  }
    	  }
    	pattern.show(G.Left(patternRect),G.Top(patternRect));
    }
    public void drawGivensRect(Graphics gc,HitPoint highlight)
    {
    	GC.frameRect(gc,Color.black,givensRect);
    	UniverseCell rack[] = b.givenRack;
    	Rectangle r = givensRect;
    	int sz = G.Width(r)/11;
    	int cx = G.Left(r)+sz/2;
    	int cy = G.centerY(r);
    	boolean canDrop = (b.pickedObject!=null) && (b.pickedObject.isGiven());
    	boolean canPick = b.pickedObject==null;
    	for(UniverseCell c : rack)
    	{	UniverseChip ch = c.getGiven();
    		if(c.drawChip(gc,this,ch,(canPick || canDrop ) ? highlight : null,sz,cx,cy,null))
    		{
    			highlight.arrow = canDrop ? StockArt.DownArrow : StockArt.UpArrow;
    			highlight.awidth = sz;
    		}
    		cx += sz;
    	}
    	
    }
    public void drawTakensRect(Graphics gc,HitPoint highlight)
    {
    	GC.frameRect(gc,Color.black,takensRect);
    	DrawCommonChipPool(gc, b, 0 ,b.takenRack,takensRect,null,b.whoseTurn,highlight); 
    }
    public void drawPatternRect(Graphics gc,HitPoint highlight)
    {	Rectangle r = patternRect;
    	String msg = b.rules.name;
    	GC.Text(gc,true,r,Color.black,null,msg);
    	if(G.pointInRect(highlight,r))
    	{	highlight.spriteRect = r;
    		highlight.spriteColor = Color.red;
    		highlight.hitCode = UniverseId.PatternLocation;
    	}
    	GC.frameRect(gc,Color.black,r);
    }
    public void drawValidRect(Graphics gc,HitPoint highlight)
    {	Rectangle r = validRect;
    	boolean valid = b.isValidSudoku();
    	String msg = valid ? "Ok" : "Invalid";
    	GC.Text(gc,true,r,valid?Color.black:Color.red,null,msg);
    	GC.frameRect(gc,Color.black,r);
    }
     public void drawAuxControls(Graphics gc,HitPoint highlight)
    {  if(b.rules.isNudoku())
    	{ drawPatternRect(gc,highlight); 
    	  drawValidRect(gc,highlight);
    	  drawGivensRect(gc,highlight);
    	  drawTakensRect(gc,highlight);
    	}
    }
    //
    // draw the board and things on it.  If gc!=null then actually 
    // draw, otherwise just notice if the highlight should be on
    //
    public void redrawBoard(Graphics gc, HitPoint highlight)
    {  UniverseBoard gb = disB(gc);
       int nPlayers = gb.nPlayers();
       boolean ourTurn = OurMove();
      boolean moving = hasMovingObject(highlight);
      HitPoint ot = ourTurn ? highlight : null;	// hit if our turn
      HitPoint select = moving?null:ot;	// hit if our turn and not dragging
      HitPoint ourSelect = (moving && !reviewMode()) ? null : highlight;	// hit if not dragging
       UniverseState vstate = gb.getState();
      gameLog.playerIcons = true;
       gameLog.redrawGameLog2(gc, ourSelect, logRect, Color.black,boardBackgroundColor,standardBoldFont(),standardPlainFont());
    
        drawBoardElements(gc, gb, boardRect, ot);
    	boolean localDone = allPlayersLocal()&&!reviewOnly&&(G.Height(doneRects[0])>0);
        for(int idx = 0;idx<nPlayers;idx++)
        {
        	commonPlayer player = getPlayerOrTemp(idx);
    		player.setRotatedContext(gc, highlight, false);
        	DrawCommonChipPool(gc, gb,idx,gb.rack[idx],playerRacks[idx],localDone?doneRects[idx]:null,gb.whoseTurn,ot);
    		player.setRotatedContext(gc, highlight, true);
    		 
         }
 
        GC.setFont(gc,standardBoldFont());
       if(vstate==UniverseState.PUZZLE_STATE)
        {
        switch(gb.rules)
        	{
        case Nudoku_9x9:
        case Nudoku_6x6:
        case Nudoku_12:
		case Nudoku_11:
		case Nudoku_10:
		case Nudoku_9:
		case Nudoku_8:
		case Nudoku_7:
		case Nudoku_6:
		case Nudoku_5:
		case Nudoku_4:
		case Nudoku_3:
		case Nudoku_2:
		case Nudoku_1_Box:
		case Nudoku_2_Box:
   		case Nudoku_3_Box:
		case Nudoku_4_Box:	// 3 x 2 in 2x2
   		case Nudoku_5_Box:
		case Nudoku_6_Box:
		case Nudoku_1:
		case Sevens_7:

        	if(GC.handleRoundButton(gc, generateRect, 
            		select, s.get("Generate Puzzle"),
                    HighlightColor, rackBackGroundColor))
                    {	// always display the done button, but only make it active in
                    	// the appropriate states
                    	select.hitCode = UniverseId.GeneratePuzzle;
                    }
	        	break;
			default: ;
        	}
        }
		commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
		double messageRotation = pl.messageRotation();
		
		if (vstate != UniverseState.PUZZLE_STATE)
        {
			if(reviewOnly || !plannedSeating() || (G.Height(doneRects[0])<=0))
				{
				handleDoneButton(gc,messageRotation,doneRect,(gb.DoneState() ? select : null), 
					HighlightColor, rackBackGroundColor);
				}
            if((gb.rules==variation.Diagonal_Blocks_Duo)||(gb.rules==variation.Blokus_Duo))
            {
            if((vstate==UniverseState.PLAY_OR_SWAP_STATE)||(vstate==UniverseState.CONFIRM_SWAP_STATE))
            {
            	if (GC.handleRoundButton(gc, messageRotation,resignRect, 
                		select, s.get(SWAP),
                        HighlightColor, (vstate==UniverseState.CONFIRM_SWAP_STATE)?HighlightColor:rackBackGroundColor))
                {	// always display the done button, but only make it active in
                	// the appropriate states
                    select.hitCode = GameId.HitSwapButton;
                }	
            }
            if(( (vstate==UniverseState.PASS_STATE) || (vstate==UniverseState.RESIGN_STATE))
            		&& gb.currentPlayerIsBehind())
            {
            	if (GC.handleRoundButton(gc, resignRect, 
                		(gb.DoneState()? select : null), s.get(RESIGN),
                        HighlightColor, (vstate==UniverseState.RESIGN_STATE)?HighlightColor:rackBackGroundColor))
                {	// always display the done button, but only make it active in
                	// the appropriate states
                    select.hitCode = GameId.HitResignButton;
                }
            }}
			handleEditButton(gc,messageRotation,editRect,select, highlight, HighlightColor, rackBackGroundColor);
                }

 		drawPlayerStuff(gc,(vstate==UniverseState.PUZZLE_STATE),ourSelect,HighlightColor,rackBackGroundColor);


        standardGameMessage(gc,
        		messageRotation,
        		vstate==UniverseState.GAMEOVER_STATE?gameOverMessage(gb):s.get(vstate.getDescription()),
        				vstate!=UniverseState.PUZZLE_STATE,
        				gb.whoseTurn,
        				stateRect);
    	UniverseChip ch = UniverseChip.getChip(gb.playerColor[gb.whoseTurn],0);
    	ch.drawChip(gc, this, iconRect, null);
        String msg = "";
        switch(gb.rules)
        {
        case Universe:
        case Pan_Kai:
        	msg = "make the last move";
        	break;
        case Phlip:
        	msg = "own the most blocks";
        	break;
   
        	default: ;
        }

        goalAndProgressMessage(gc,ourSelect,s.get(msg),progressRect, goalRect);

        drawAuxControls(gc,ourSelect);
        drawVcrGroup(ourSelect, gc);
        redrawChat(gc,highlight);
        if(gb.pickedObject!=null)
        {	// this is a little nonstandard  redrawBoard is called from mouse motion,
        	// so we need to pick up the mouse sensitivity here.
        	drawPickedTile(gc,highlight,rotatorRect,gb,gb.pickedObject);
        	if(plannedSeating() && G.Height(rotator2Rect)>0)
        	{
           	drawPickedTile(gc,highlight,rotator2Rect,gb,gb.pickedObject);
        	}
        }
    }

    /**
     * Execute a move by the other player, or as a result of local mouse activity,
     * or retrieved from the move history, or replayed form a stored game. 
     * @param mm the parameter is a commonMove so the superclass commonCanvas can
     * request execution of moves in a generic way.
     * @return true if all went well.  Normally G.Error would be called if anything went
     * seriously wrong.
     */
     public boolean Execute(commonMove mm,replayMode replay)
    {	 
 
        handleExecute(b,mm,replay);
        // use the standard definition
        startBoardAnimations(replay,b.animationStack,SQUARESIZE,MovementStyle.Simultaneous);
        if(replay.animate) { playSounds(mm); }
 
        return (true);
    }
     
/**
 * parse a move specifier on behalf of the current player.  This is called by the 
 * "game" object when it receives a move from the other player.  Note that it may
 * be called while we are in review mode, so the current undoInfo of the board should
 * not be considered.
 */
    public commonMove ParseNewMove(String st, int player)
    {
        return (new UniverseMovespec(st, player));
    }


    
private void playSounds(commonMove m)
{

    // add the sound effects
    switch(m.op)
    {
    case MOVE_RACK_BOARD:
      	 playASoundClip(light_drop,100);
       	 playASoundClip(heavy_drop,100);
   	break;
    case MOVE_PICK:
    	 playASoundClip(light_drop,100);
    	 break;
    case MOVE_PICKB:
    case MOVE_PICKGIVEN:
    	playASoundClip(light_drop,100);
    	break;
    case MOVE_DROP:
    	break;
    case MOVE_DROPB:
      	 playASoundClip(heavy_drop,100);
      	break;
    default: break;
    }
	
}

 
/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 */
    public void StartDragging(HitPoint hp)
    {
        if (hp.hitCode instanceof UniverseId)// not dragging anything yet, so maybe start
        {
        UniverseId hitObject = (UniverseId)hp.hitCode;
		UniverseCell cell = hitCell(hp);
		if(cell!=null)
		{
	    switch(hitObject)
	    {
         case GivenOnBoard:
        	PerformAndTransmit("Pickg " + cell.col+" "+cell.row);
         	break;
        case GivensRack:
        case ChipRack:
        case TakensRack:
         	PerformAndTransmit("Pick " + cell.col+ " "+cell.row);
        	break;
	    case BoardLocation:
	    	// note, in this implementation the board squares are themselves pieces on the board
	    	// if the board becomes a graphic, then this > should be >= to enable click-and-drag 
	    	// behavior as well as click-to-pick
	    	{
	    	UniverseChip chip = cell.topChip();
	    	if(cell.topChip()!=null)
	    		{
	    		PerformAndTransmit("Pickb "+cell.col+" "+cell.row+" "+chip.chipNumber());
	    		}}
	    	break;
		default:
			break;
        }
		}
        }
    }

    	private void generatePuzzle()
    	{	doInit(false);
        	PerformAndTransmit("Start p0");
        	CommonMoveStack solution = b.generatePuzzle();
        	switch(b.rules)
        	{
        	default:
        	{
        	doInit(false);
        	UniverseChip.clearSudokuValues();
        	while(solution.size()>0)
        	{
        		PerformAndTransmit(solution.pop().moveString());
        	}}}
    	}
	/** 
	 * this is called on "mouse up".  We may have been just clicking
	 * on something, or we may have just finished a click-drag-release.
	 * We're guaranteed just one mouse up, no bounces.
	 */
    public void StopDragging( HitPoint hp)
    {
        CellId id = hp.hitCode;
        if((id==GameId.HitDoneButton)
        		&& b.rules.passIsPermanent()
        		&& (b.getState()==UniverseState.PASS_STATE))
        {
        	PerformAndTransmit("AllDone");
        }
        else if(!(id instanceof UniverseId)) {   missedOneClick = performStandardActions(hp,missedOneClick);}
    	else {
    	missedOneClick = false;
     	UniverseId hitObject = (UniverseId)hp.hitCode;
        UniverseState state = b.getState();
        switch (hitObject)
        {
        default:
        	throw G.Error("Hit Unknown: %s", hitObject);
         case PatternLocation:
        	{	patternMenu();
        	}
         	break;
        case GeneratePuzzle:
        	generatePuzzle();
        	break;
	    case RotateCW:
		      	{	UniverseCell cell = b.pickedCell;
		      		PerformAndTransmit("RotateCW "+cell.col+" "+cell.row);
		      	}
	      	break;
	    case RotateCCW:
		      	{	UniverseCell cell = b.pickedCell;
		      		PerformAndTransmit("RotateCCW "+cell.col+" "+cell.row);
		      	}
		      	break;
	    case RotateNothing:
		      	break;
        	
        case FlipCell:
          	{	UniverseCell cell = b.pickedCell;
          		PerformAndTransmit("Flip "+cell.col+" "+cell.row);
          	}
          	break;
          case BoardLocation:	// we hit the board 
			switch(state)
			{
			default: throw G.Error("Not expecting drop on filled board in state %s",state);
			case CONFIRM_STATE:
			case PLAY_STATE:
			case PLAY_OR_SWAP_STATE:
			case PUZZLE_STATE:
			{
				UniverseCell cell = hitCell(hp);
				UniverseChip chip = (cell==null) ? null : cell.topChip();
				if(b.movingObjectIndex()>=0)
				{ if(cell!=null) { PerformAndTransmit("Dropb "+cell.col+" "+cell.row); }
				}
				else if(chip!=null)
				{
				PerformAndTransmit( "Pickb "+cell.col+" "+cell.row+" "+chip.chipNumber());
				}
				break;
			}}
			break;
        case ChipRack:
        case TakensRack:
        case GivensRack:
        	{
       		UniverseCell cell = hitCell(hp);
        	int mov = b.movingObjectIndex();
        	if(mov>=0) { PerformAndTransmit("Drop "+cell.col+" "+cell.row); }
        	}
           break;
        }}
     }

    public void setDisplayParameters(UniverseBoard gb,Rectangle r)
    {
	    gb.SetDisplayParameters(1.0,1.0,  0,0.0,  0);
	    gb.SetDisplayRectangle(r);
    }
   	
    public boolean drawPickedTiles(Graphics offGC,HitPoint hp)
    {
    	boolean draw1 = drawPickedTile(offGC,hp,rotatorRect,b,b.pickedObject);
    	boolean draw2 = plannedSeating() && (G.Height(rotator2Rect)>0) && drawPickedTile(offGC,hp,rotator2Rect,b,b.pickedObject);
    	return(!(draw1 || draw2));
    }

    /** these are drawn separately, directly on the canvas.  They
    might seem to flash on and off.
    */
    public void drawCanvasSprites(Graphics offGC, HitPoint hp)
    {
 		// rotator box for moving chip
        if((b.pickedObject==null)
        		|| drawPickedTiles(offGC,hp))
        {
        DrawTileSprite(offGC,hp); //draw the floating tile, if present
        }
        
        drawSprites(offGC);
    }
    /**
     * this is a token or tokens that initialize the variation and
     * set immutable parameters such as the number of players
     * and the random key for the game.  It can be more than one
     * token, which ought to be parseable by {@link #performHistoryInitialization}
     * @return return what will be the init type for the game
     */
    public String gameType() 
    { 
    	return(G.concat(b.gametype," ",b.randomKey," ",b.nPlayers()," ",b.revision)); 
   }
    public String sgfGameType() { return(Universe_SGF); }    
    
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
    public void performHistoryInitialization(StringTokenizer his)
    {	String token = his.nextToken();		// should be a universe init spec
    	int rk = G.IntToken(his);
    	int np = G.IntToken(his);
    	int rev = his.hasMoreTokens() ? G.IntToken(his) : 100;
    	// make the random key part of the standard initialization,
    	// even though games like universe probably don't use it.
        b.doInit(token,rk,np,rev);
        adjustPlayers(np);
    }

    /** handle action events
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {
    	if( pattern.selectMenuTarget(target))
    	{
    	variation newtarget = (variation)pattern.rawValue;
    	b.setRules(newtarget);
    	doInit(false);
	    resetBounds();
    	generalRefresh();
    	return(true);
    	}
    	else if (target==findGivensItem)
    	{
    		findBestGivens();
    		return(true);
    	}
    	else
    	return(super.handleDeferredEvent(target,command));
     }
/** handle the run loop, and any special actions we need to take.
 * The mouse handling and canvas painting will be called automatically
 *  */
    
    //   public void ViewerRun(int wait)
    //   {
    //       super.ViewerRun(wait);
    //   }

    public BoardProtocol getBoard()   {    return (b);   }
    
    public SimpleRobotProtocol newRobotPlayer()
    { return(new UniversePlay(this)); }


    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * universe summary:
		37 files visited 0 problems
	   diagonal block duo summary:
		1441 files visited 0 problems
	   pan kai summary: 5/27/2023
		1195 files visited 0 problems
	   philip summary: 5/27/2023
		1781 files visited 0 problems
     */
    public void ReplayMove(sgf_node no)
    {
        String comments = "";
        sgf_property prop = no.properties;

        while (prop != null)
        {
            String name = prop.getName();
            String value = (String) prop.getValue();

            if (setup_property.equals(name))
            {	StringTokenizer st = new StringTokenizer(value);
            	String typ = st.nextToken();
            	int ran = G.IntToken(st);
            	int np = G.IntToken(st);
            	int rev = st.hasMoreTokens() ? G.IntToken(st) : 100;
                b.doInit(typ,ran,np,rev);
                adjustPlayers(np);
             }
            else if (name.equals(comment_property))
            {
                comments += value;
            }
            else if (parseVersionCommand(name,value,2)) {}
            else if (parsePlayerCommand(name,value)) {}
            else 
            {
                replayStandardProps(name,value);
            }

            prop = prop.next;
        }

        if (!"".equals(comments))
        {
            setComment(comments);
        }
    }
}

