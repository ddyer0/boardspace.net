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
package euphoria;

import static euphoria.EuphoriaMovespec.*;

import java.awt.*;

/* below here should be the same for codename1 and standard java */
import online.common.*;

import java.util.*;

import bridge.Config;
import common.GameInfo;
import euphoria.EPlayer.PFlag;
import euphoria.EPlayer.PlayerView;
import euphoria.EPlayer.TFlag;
import euphoria.EuphoriaBoardConstructor.Decoration;
import lib.Graphics;
import lib.Image;
import lib.CellId;
import lib.ChatInterface;
import lib.DefaultId;
import lib.Drawable;
import lib.ExtendedHashtable;
import lib.FontManager;
import lib.G;
import lib.GC;
import lib.GameLayoutManager;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.StockArt;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;
import online.game.*;
import online.game.sgf.sgf_game;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;
import rpc.RpcService;
import vnc.VNCService;

/**
 * TODO: add "eye" to click on recruitcards in opening selection
 * 
 * Overall Architecture
 * 
 * The site provides the lobby, choice game and opponents, communication between the players, information 
 * for spectators,  rankings, and a host of other services.  Each game has to be concerned only with 
 * the game itself.   An individual game (say, Hex) is launched and each client independently initializes
 * itself to a common starting state.   Thereafter each player specifies messages to be broadcast to the
 * other participants, and receives messages being broadcast by the other participants, which keep everyone
 * informed about the state of the game.  There is no common "true" state of the game - all the participants
 * keep in step by virtue of seeing the same stream of messages.    Messages are mostly simple "pick up a stone"
 * "place a stone on space x" and so on.
 * 
 * The things a game must implement are specified by the class "ViewerProtocol", and a game could just
 * start there and be implemented completely from scratch, but in practice there is another huge pile
 * of things that every game has to do; dealing with graphics, mouse events, saving and restoring the
 * game state from static records, replaying and reviewing games and so on.   These are implemented in the 
 * class "commonCanvas" and by several board-like base classes for hexagonal and square geometry boards.   
 * All the existing games for boardspace use these classes to provide graphics and basic board representation.
 * 
 * For games with robot players, there is another huge pile of things that a robot has to do, generating
 * moves, evaluating and choosing the best, and implementing a lookahead several moves deep.   There's a
 * standard framework for this using the "RobotProtocol" class and the "SearchDriver" class. 
 */
/**
 * 
 * This is intended to be maintained as the reference example how to interface to boardspace.
 * <p>
 * The overall structure here is a collection of classes specific to Hex, which extend
 * or use supporting online.game.* classes shared with the rest of the site.  The top level 
 * class is a Canvas which implements ViewerProtocol, which is created by the game manager.  
 * The game manager has very limited communication with this viewer class, but manages
 * all the error handling, communication, scoring, and general chatter necessary to make
 * the game part of the site.
 * <p>
 * The main classes are:
 * <br>EuphoriaViewer - this class, a canvas for display and mouse handling
 * <br>EuphoriaBoard - board representation and implementation of the game logic
 * <br>EuphoriaMovespec - representation, parsing and printing of move specifiers
 * <br>EuphoriaPlay - a robot to play the game
 * <br>EuphoriaConstants - static constants shared by all of the above.  
 *  <p>
 *  The primary purpose of the EuphoriaViewer class is to do the actual
 *  drawing and to mediate the mouse gestures.  All the actual work is 
 *  done in an event loop, rather than in direct response to mouse or
 *  window events, so there is only one process involved.  With a single 
 *  process, there are no worries about synchronization among processes
 *  of lack of synchronization - both major causes of flakey user interfaces.
 *  <p>
 *  The actual mouse handling is done by the commonCanvas class, which simply 
 *  records the recent mouse activity, and triggers "MouseMotion" to be called
 *  while the main loop is executing.
 *  <p>
 *  Similarly, the actual "update" and "paint" methods for the canvas are handled
 *  by commonCanvas, which merely notes that a paint is needed and returns immediately.
 *  paintCanvas is called in the event loop.
 *  <p>
 *  The drawing methods here combine mouse handling and drawing in a slightly
 *  nonstandard way.  Most of the drawing routines also accept a "HitPoint" object
 *  which contains the coordinates of the mouse.   As objects are drawn, we notice
 *  if the current object contains the mouse point, and if so deposit a code for 
 *  the current object in the HitPoint.  the Graphics object for drawing can be null,
 *  in which case no drawing is actually done, but the mouse sensitivity is checked
 *  anyway.  This method of combining drawing with mouse sensitivity helps keep the
 *  mouse sensitivity accurate, because it is always in agreement with what is being
 *  drawn.
 *  <p>
 * *  
*/
public class EuphoriaViewer extends CCanvas<EuphoriaCell,EuphoriaBoard> implements EuphoriaConstants
{	
    // file names for jpeg images and masks
    static final String SoundDir = G.isCodename1() ? "/appdata/euphoria-other/data/" : "/euphoria/sounds/";
    static final String ImageDir = G.isCodename1() ? "/appdata/euphoria-other/images/" : "/euphoria/images/";
	// sound files
	static String CARD_PLACE = SoundDir + "Card place #2"+ Config.SoundFormat;
	static String DOOR_OPEN = SoundDir + "door"+ Config.SoundFormat;
	static String KACHING = SoundDir + "CashRegister"+ Config.SoundFormat;
	static String WONTGETFOOLED = SoundDir + "wontgetfooled"+ Config.SoundFormat;
	static String DIE_ROLL[] = { SoundDir + "Dice roll #1"+ Config.SoundFormat,
		SoundDir + "Dice roll #2"+ Config.SoundFormat,
		SoundDir + "Dice roll #3"+ Config.SoundFormat,
		SoundDir + "Dice roll #4"+ Config.SoundFormat};
	static final String Euphoria_SGF = "Euphoria"; // sgf game number allocated for nuphoria   

	static final int BACKGROUND_TILE_INDEX = 0;
	static final int BACKGROUND_REVIEW_INDEX = 1;
	static final int BOARD_INDEX = 2;
	static final String TextureNames[] = { "background-tile" ,"background-review-tile", "board-notext"};
    // some general constants that might not always be
    static final int MAX_WORKERS = 4;
    static final int STARTING_RECRUITS = 4;
    static final int STARTING_AUTHORITY_TOKENS = 10;
    static final int MAX_PLAYERS = 6;
    static final int ALLEGIANCE_TIER_3 = 8;
    static final int ALLEGIANCE_TIER_2 = 5;
    static final int ALLEGIANCE_TIER_1 = 2;
    static final int TUNNEL_REVEAL = 6;
    static final int MIN_KNOWLEDGE_TRACK = 1;
    static final int MIN_MORALE_TRACK = 1;
    static final int MAX_KNOWLEDGE_TRACK = 6;
    static final int MAX_MORALE_TRACK = 6;
    Cost TunnelAllegiance[] = {Cost.IsEuphorian,Cost.IsSubterran,Cost.IsWastelander,Cost.IsIcarite};
	static final Benefit[] TUNNEL_BENEFIT_CHASE_THE_MINER = 
		{
		Benefit.Goldx2,Benefit.Stonex2,Benefit.Clayx2
		};

	static final Benefit[] UPGRADED_BENEFIT = {Benefit.CardAndGold,Benefit.CardAndStone,Benefit.CardAndClay};
    static final Cost[] REROLL_PENALTIES = {null,Cost.CommodityOrResourcePenalty,Cost.CommodityOrResourcex2Penalty,Cost.CommodityOrResourcex3Penalty,Cost.CommodityOrResourcex4Penalty};
    // how many worker tokens must be in place to open a market
    static final int TOKENS_TO_OPEN_MARKET[] = {0,1,2,2,3,4,4};	

	boolean showMedallions=false;
	public int getAltChipset()
	{
		return(showMedallions?1:0);
	}
	 public boolean allowUndo() 
	  {	if(super.allowUndo())
		  {
		  commonMove m = getCurrentMove();
		  // this prevents a misclick at the opening from undoing the recruit selection
		  if(m.op==NORMALSTART) { return(false); }
		  return(true);
		  }
	  	return(false);
	  }
	 

    public boolean allowRobotsToRun(commonPlayer p)
    {
    	if(simultaneousTurnsAllowed() && bb.getPlayer(p.boardIndex).hasReducedRecruits()) 
    	{
    		return false;
    	}
    	return true;
    }

    public boolean startSynchronousPlay()
    {
 	   if(!reviewOnly 
 			   && !reviewMode() 
 			   && (bb.getState()==EuphoriaState.NormalStart)
 			   && bb.readyToStartNormal() 
 			   && allRobotsIdle()
 			   )
 	   {
		   PerformAndTransmit("NormalStart",false,replayMode.Live);
 		   canonicalizeHistory();
 		   return(true);
 		  //PerformAndTransmit("NormalStart");	// a second time for the record
	   }
 	   return(false);
    } 

    // this is non-standard
    // this allows simultaneous card moves to display properly
    public int getOurMovingObject(HitPoint highlight)
    {	if(bb.ephemeralRecruitMode())
    	{	
    		int rp = selectedRecruitPlayer(highlight,bb);
    		if(rp>=0)
    		{
    			EPlayer pl = bb.getPlayer(rp);
    			EuphoriaChip ch = (pl==null) ? null : pl.ephemeralPickedObject;
    			return((ch==null) ? NothingMoving : ch.chipNumber());
    		}
    		else 
    		{
    			return(NothingMoving);
    		}
    	}
    	if (OurMove())
    	{
    	// encode the source of the card as well as the card, so the 
    	// client will conceal the card identity if appropriate
        return(bb.movingObjectIndex());
    	}
    	return(NothingMoving);
    }
       //
    // convert the ephemeral moves into non-ephemeral equivalents, and
    // sort them into canonical order.  This is called at the end of
    // the placement phase to bake-in the players placements.
    //
    public void canonicalizeHistory()
    {
    	CommonMoveStack  h = History;
    	CommonMoveStack ephemera = new CommonMoveStack();
    	CommonMoveStack permanent = new CommonMoveStack();
    	int firstEphemeral = 0;
    	while(h.size()>0) 
    		{ commonMove m = h.pop();
    		  boolean ep = m.isEphemeral();
    		  if(ep) 
    		  { firstEphemeral = m.index();
    		    m.setEvaluation(m.player*1000+m.index());	// set evaluation for the sort
    		    ephemera.push(m); 
    		  }
    		  else
    		  { permanent.push(m); 
    		  }
    		}
    	ephemera.sort(false);		// sort by evaluation.  This puts all of each players ephemeral moves together
    	
    	EuphoriaMovespec prev = null;
    	while((permanent.size()>0)||(ephemera.size()>0))
    	{
    		EuphoriaMovespec top = (EuphoriaMovespec)permanent.top();
    		if((top==null) || (top.index()>firstEphemeral))
    		{
    			// copy the ephemeral
    			while(ephemera.size()>0)
    			{
    				EuphoriaMovespec m = (EuphoriaMovespec)ephemera.pop();
    				switch(m.op)
    				{
    				default: throw G.Error("Not expecting move %s",m);
       				case EPHEMERAL_CONFIRM_DISCARD:
       					m.op = CONFIRM_DISCARD;
       					m.setIndex(h.size());
       					m.next = null;
       					if(prev!=null) { prev.next = m; }
       					prev = m;
       					h.push(m);

       					break;
       				case EPHEMERAL_CHOOSE_RECRUITS:
       				case EPHEMERAL_PICK: 
    				case EPHEMERAL_DROP:
    						break;	// remove
     				case EPHEMERAL_CONFIRM_ONE_RECRUIT:
    				case EPHEMERAL_CONFIRM_RECRUITS:
    						m.op = CONFIRM_RECRUITS;
    						if(prev!=null && prev.op==m.op) {	// remove duplicate
    							G.print("Remove duplicate ",prev,m);
    						}
    						else
    						{
        					m.setIndex(h.size());
        					m.next = null;
        					if(prev!=null) { prev.next = m; }
        					prev = m;
        					h.push(m);
    						}
        					break;
    				case EPHEMERAL_CHOOSE_RECRUIT:
    					m.op = MOVE_CHOOSE_RECRUIT;	// convert to a non-ephemeral move
    					/*$FALL-THROUGH$*/
    				case MOVE_CHOOSE_RECRUIT:
    					if((prev!=null)
    							&& (prev.op==MOVE_CHOOSE_RECRUIT)
    							&& (prev.dest==m.source)
    							&& (prev.source==m.dest))
    						{
    						// remove a back and forth pair.  This is necessary because not all players
    						// will have seen these moves as sequential, so some will have removed them
    						// and others will not.
    						h.pop();
    						prev = (EuphoriaMovespec)h.top();
    						prev.next = null;
    						}
    						else 
    						{
    							m.setIndex(h.size());
    							m.next = null;
    							if(prev!=null) { prev.next = m; }
    							prev = m;
    							h.push(m);
    						}
    					
     				}
    			}
    		}
    		if(top!=null)
    			{ top.setIndex(h.size());
    			  top.next = null;
    			  permanent.pop();
    			  if(prev!=null) { prev.next = top; }
    			  if(top.op==NORMALSTART)
    			  {	  // the NORMALSTART move is performed separately by 
    				  // each of the clients, so they all will get a different 
    				  // elapsed time.  They must all be the same so the ongoing
    				  // game state remains identical for all clients.
    				  top.setElapsedTime(prev.elapsedTime());
    			  }
    			  h.push(top);
    			  prev = top;
    			}
    	}
    	//G.print("Canonicalize to "+formStoryString());
    	
    }
	static final long serialVersionUID = 1000;
     // colors
    private Color reviewModeBackground = new Color(220,165,200);
    private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
    private Color chatBackgroundColor = new Color(230,230,255);
    private Color rackBackGroundColor = new Color(167,183,231);
    private Color boardBackgroundColor = new Color(193,223,255);
    
private Color EuphoriaMouseColors[] = {
		Color.red,Color.green,Color.blue,
		Color.black,Color.white,new Color(1.0f,0.0f,1.0f)
};
private Color EuphoriaMouseDotColors[] = {
		Color.black,Color.black,Color.white,
		Color.white,Color.black,Color.white
};
private Color playerBackground[] = {
	new Color(208,184,202),
	new Color(178,199,202),
	new Color(178,199,220),
	new Color(141,151,175),
	new Color(178,184,202),
	new Color(208,184,220),
};
    // images are shared among all instances of the class so loaded only once
    private static Image[] textures = null;			// background textures
    int STANDARD_CELLSIZE = 1;
    private boolean magnifier = true;
    
    // private state
    private EuphoriaBoard bb = null; //the board from which we are displaying
  
    // addRect is a service provided by commonCanvas, which supports a mode
    // to visualize the layout during development.  Look for "show rectangles"
    // in the options menu.
    private Rectangle benefitRect = addRect("BenefitRect");
    private Rectangle playerRecruitRect = addRect(".playerRecruits");
    private Rectangle fullPlayerRecruitRect = addRect(".fullRecruits");
     //private Rectangle repRect = addRect("repRect");	// not needed for euphoria
    private Rectangle magnifierRect = addRect("magnifier");
    
    public boolean allowOpponentUndoNow()
    {
    	if(super.allowOpponentUndoNow())
    	{
    		if(!bb.revealedNewInformation() && !hasPeeked())
    		{
    			return(true);
    		}
    	}
    	return(false);
    }
    public void doShowSgf()
    {
        if (mutable_game_record || G.debug() )
        {
            super.doShowSgf();
        }
        else
        {//super.doShowSgf();
            theChat.postMessage(ChatInterface.GAMECHANNEL, ChatInterface.KEYWORD_CHAT,
                s.get(CensoredGameRecordString));	// not available during the game
            
        }
    }
    
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	
       	EuphoriaChip.preloadImages(loader,ImageDir);	// load the images used by stones
		WorkerChip.preloadImages(loader, ImageDir);	// worker dice
		MarketChip.preloadImages(loader, ImageDir);	// market cards
		DilemmaChip.preloadImages(loader, ImageDir);	// dilemma cards
		ArtifactChip.preloadImages(loader, ImageDir);	// dilemma cards
		
		if (textures == null)
    	{ 	// note that for this to work correctly, the images and masks must be the same size.  
        	// Refer to http://www.andromeda.com/people/ddyer/java/imagedemo/transparent.html
    		
    		// images and textures are static variables, so they're shared by
    		// the entire class and only get loaded once.  Special synchronization
    		// tricks are used to make sure this is true.
    		
    	  // load the background textures as simple images
          textures = loader.load_images(ImageDir,TextureNames);
    	}
		gameIcon = textures[BOARD_INDEX];
		// do the recruits last so codename1 will swap out the main res file	
		RecruitChip.preloadImages(loader);	// recruit cards
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
    	// 
    	// for games that require some random initialization, the random key should be
    	// captured at this point and passed to the the board init too.
        // randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
    	//
        super.init(info,frame);
        EuphoriaState.putStrings();
        // use_grid=reviewer;// use this to turn the grid letters off by default
        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	EuphoriaState.putStrings();
        }
        MouseColors  = EuphoriaMouseColors;
        MouseDotColors = EuphoriaMouseDotColors;

       	int players_in_game = Math.max(2,info.getInt(OnlineConstants.PLAYERS_IN_GAME,3));
       	int randomKey = info.getInt(OnlineConstants.RANDOMSEED,-1);
       	// magic number that produces a 3p game with 2 factionless recruits -1118173504
       	//randomKey = -1118173504;
        bb = new EuphoriaBoard(info.getString(GameInfo.GAMETYPE, Variation.Euphoria.name()),randomKey,players_in_game,getStartingColorMap(),EuphoriaBoard.REVISION);
        useDirectDrawing(true);
        doInit(false);      	 
    }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        bb.doInit(bb.gametype);						// initialize the board
        if(!preserve_history) { adjustPlayers(bb.nPlayers()); }	// adjust players causes hidden connections to reset
        bb.activePlayer = getActivePlayer().boardIndex;
        if(reviewOnly  || isTurnBasedGame()) { setSimultaneousTurnsAllowed(false); }
        if(!preserve_history)
    	{ // must be start p0, we use the cycle of players to determine when all the players have chosen recruits.
        	PerformAndTransmit(reviewOnly?EDIT:"Start P0", false,replayMode.Replay);
    	}
    }
    boolean startedPlaying = false;
    /** this is called by the game controller when all players have connected
     * and the first player is about to be allowed to make his first move. This
     * may be a new game, or a game being restored, or a player rejoining a game.
     * You can override or encapsulate this method.
     */
    public void startPlaying()
    {	bb.setActivePlayer(getActivePlayer().boardIndex);
    	startedPlaying = true;
    	super.startPlaying();
    }
    

    /**
     * update the players clocks.  The normal thing is to tick the clocks
     * only for the player whose turn it is.  Games with a simtaneous action
     * phase need to do something more complicated.
     * @param inc the increment (in milliseconds) to add
     * @param p the current player, normally the player to update.
     */
    public void updatePlayerTime(long inc,commonPlayer p)
    {	if(simultaneousTurnsAllowed())
    	{
    		for(commonPlayer pl : players)
    		{
    			if((pl!=null) && !bb.hasReducedRecruits(pl.boardIndex))		// this player has not finished
    			{
    				super.updatePlayerTime(inc,pl); 	// keep on ticking
    			}
    		}
    	}
    	else if(p!=null)
    	{ super.updatePlayerTime(inc,p); 
    	}
    }

    public Rectangle createPlayerGroup(int player,int x,int y,double rot,int CEL)
    {	commonPlayer pl0 = getPlayerOrTemp(player);
    	Rectangle zone = pl0.playerBox;
        Rectangle p0time = pl0.timeRect;
        Rectangle p0aux = pl0.extraTimeRect;
        Rectangle p0anim = pl0.animRect;
        Rectangle name = pl0.nameRect;
        Rectangle pic = pl0.picRect;
        //first player name
        int nameW = CEL*4	;
        int picW = CEL*3;
        G.SetRect(name,x,y,nameW,CEL);
        // first player portrait
        G.SetRect(pic,x+CEL,y+CEL, picW, picW);
         
        // time display for first player
        G.SetRect(p0time, x+nameW,y,	3*CEL/2, 2*CEL/3);
        G.AlignLeft(p0aux,G.Bottom(p0time),p0time);
        // first player "i'm alive" animation ball
        G.SetRect(p0anim, G.Right(p0time),y,CEL,CEL);
        pl0.displayRotation = rot;
    	G.SetRect(zone,x, y, CEL*11, CEL*4);
        return(zone);
    }
    
    static final int boardrows = 26; 	// board columns
    static final int boardcols = 30;  	// board rows.  Preserve this aspect ratio to 
    double bCellSize;
    int CELLSIZE;
    
    public void setLocalBounds(int x, int y, int width, int height)
    {	G.SetRect(fullRect, x, y, width, height);
    	GameLayoutManager layout = selectedLayout;
    	int nPlayers = nPlayers();
       	int chatHeight = selectChatHeight(height);
       	// ground the size of chat and logs in the font, which is already selected
    	// to be appropriate to the window size
    	int fh = standardFontSize();
    	int minLogH = fh*10;	
        int minVcrW = fh*16;
        int margin = fh/2;
        double nrows = boardrows+0.5;  
        double ncols = boardcols;
       	// this does the layout of the player boxes, and leaves
    	// a central hole for the board.
    	layout.selectLayout(this, nPlayers, width, height,
    			margin,		// margin for boxes allocated
    			0.6 ,	// space allocated to the board
    			1.0,	// 1:1 aspect ratio for the board
    			fh*2.5,
    			fh*(3.5-0.1*nPlayers),	// maximum cell size
    			0.2		// preference for the designated layout, if any
    			);
      	int minChatW = fh*35;	 	
      	// if the chat is present, make the log the same width
      	// if not, make a narrower than the chat is required to be
      	// .. this subtlety in an attempt to optimize 3 player corner configurations on playtable
    	int minLogW = Math.min(chatHeight==0?fh*30:minChatW,width-minChatW-margin*4);
        int buttonW = fh*8;
    	// place the chat and log automatically, preferring to place
    	// them together and not encroaching on the main rectangle.
        
    	layout.placeTheChatAndLog(chatRect, minChatW, chatHeight,minChatW*2,3*chatHeight/2,
    							logRect,minLogW, minLogH, minLogW, minLogH*2);
     	
        layout.placeDoneEdit(buttonW, 3*buttonW/2, doneRect, null);
        
       	layout.placeTheVcr(this,minVcrW,minVcrW*3/2);

        Rectangle main = layout.getMainRectangle();
    	int mainX = G.Left(main);
    	int mainY = G.Top(main);
    	int mainW = G.Width(main);
    	int mainH = G.Height(main);
    	// calculate a suitable cell size for the board
    	double cs = Math.min((double)mainW/ncols,(double)(mainH/(nrows+1)));
    	bb.CELLSIZE = STANDARD_CELLSIZE = CELLSIZE = (int)cs;
    	// G.print("cell "+cs0+" "+cs+" "+bestPercent);
    	// center the board in the remaining space
    	int boardW = (int)(ncols*cs);
    	int boardH = (int)(nrows*cs);
        int stateH = fh*5/2;
    	int extraW = (mainW-boardW)/2;
    	int extraH = (mainH-boardH-stateH)/2;
    	int boardX = mainX+extraW;
    	int boardY = mainY+extraH+stateH;
    	int boardBottom = boardY+boardH;
        int stateY = boardY-stateH+stateH/4;
        layout.returnFromMain(extraW,extraH);
        placeStateRow(boardX,stateY,boardW,stateH,iconRect,stateRect,magnifierRect,annotationMenu,numberMenu,noChatRect);

        // make the recruit rect square and centered on the board, so everything
        // will be easy when displayed rotated.
    	G.SetRect(boardRect,boardX,boardY,boardW,boardH);
    	int dim = Math.min(boardW, boardH)-CELLSIZE*2;
    	G.SetRect(playerRecruitRect,boardX+(int)(boardW*0.25),boardY+(boardH-dim)/2,(int)(boardW*0.73),dim);
    	G.SetRect(fullPlayerRecruitRect,boardX+(boardW-dim)/2,boardY+(boardH-dim)/2,dim,dim);

        G.SetRect(benefitRect,G.Left(boardRect) + CELLSIZE*4,boardY + CELLSIZE*4,
        		boardW-CELLSIZE*8,boardH-CELLSIZE*8);
 
        G.SetRect(editRect,boardX+boardW-buttonW*3/4,boardY+(int)(boardH*0.7),buttonW*2/3,buttonW/2);
       	// goal and bottom ornaments, depending on the rendering can share
    	// the rectangle or can be offset downward.  Remember that the grid
    	// can intrude too.
    	placeRow(boardX, boardBottom-stateH,boardW,stateH,goalRect);      
    	
        setProgressRect(progressRect,goalRect);
        positionTheChat(chatRect,chatBackgroundColor,rackBackGroundColor);
    }


    /**
    * sprites are normally a game piece that is "in the air" being moved
    * around.  This is called when dragging your own pieces, and also when
    * presenting the motion of your opponent's pieces, and also during replay
    * when a piece is picked up and not yet placed.  While "obj" is nominally
    * a game piece, it is really whatever is associated with b.movingObject()
    
      */
    public void drawSprite(Graphics g,int obj,int xp,int yp)
    {
    	// draw an object being dragged
    	// use the board cell size rather than the window cell size
    	if(simultaneousTurnsAllowed() && (obj!=getMovingObject(null))) { return; }
    	EuphoriaBoard gb = disB(g);
    	EuphoriaCell src = gb.getSource();
    	int scale0 = src!=null ? src.defaultScale : 0;
    	int scale = scale0 <=0 ? gb.cellSize()*4 : scale0;
    	EuphoriaChip chip = EuphoriaChip.getChip(obj);
    	if(chip!=null) 
    		{ if(gb.getGuiState()!=EuphoriaState.Puzzle) { chip = chip.getSpriteProxy();}
    		  chip.drawChip(g,this,remoteViewer>=0 ? scale*2 : scale, xp, yp, null); 
    		}
    }
    // also related to sprites,
    // default position to display static sprites, typically the "moving object" in replay mode
    //public Point spriteDisplayPoint()
    //{	BoardProtocol b = getBoard();
    //	int celloff = b.cellSize();
    //	return(new Point(G.Right(boardRect)-celloff,G.Bottom(boardRect)-celloff));
    //}  


    Image scaled = null;
    
    /** draw the deep unchangable objects, including those that might be rather expensive
     * to draw.  This background layer is used as a backdrop to the rest of the activity.
     * in our cease, we draw the board and the chips on it. 
     * */
    private void drawFixedElements(Graphics gc, EuphoriaBoard gb,Rectangle brect)
    { // erase
    	boolean reviewBackground = reviewMode()&&!mutable_game_record;
      GC.setColor(gc,reviewBackground ? reviewModeBackground : boardBackgroundColor);
      //GC.fillRect(gc, fullRect);
     textures[BACKGROUND_TILE_INDEX].tileImage(gc, fullRect);   
      if(reviewBackground)
      {	 
       textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,boardRect);   
      }
      gb.SetDisplayRectangle(brect);
      // if the board is one large graphic, for which the visual target points
      // are carefully matched with the abstract grid
      if(remoteViewer<0)
      {
    	  scaled = textures[BOARD_INDEX].centerScaledImage(gc, brect,scaled);
    	  if(gc!=null) { drawDecorations(gc,gb,brect); }
      if(isIIB())
      {
      	int boardX = G.Left(brect);
      	int boardY = G.Top(brect);
      	int boardW = G.Width(brect);
  		int boardH = G.Height(brect);
    	double cardzone[] = cardArea;
    	int x0 = boardX+(int)(boardW*cardzone[0]/100);
    	int x1 = boardX+(int)(boardW*cardzone[2]/100);
    	int y0 = boardY+(int)(boardH*cardzone[1]/100);
    	int y1 = boardY+(int)(boardH*cardzone[3]/100);
    	EuphoriaChip.CardMarket.getImage(loader).centerImage(gc, x0,y0,x1-x0,y1-y0);
      }}
    }
    private void drawDecorations(Graphics gc, EuphoriaBoard gb,Rectangle brect)
   {	GC.setFont(gc,standardBoldFont());
   	   int left = G.Left(brect);
   	   int top = G.Top(brect);
       for(Decoration dec : gb.decorations())
       {	
    	int x = gb.positionToX(dec.left)/100;
       	int y = gb.positionToY(dec.top)/100;
       	int w = gb.positionToX(dec.right)/100-x;
       	int h = gb.positionToY(dec.bottom)/100-y;
       	GC.Text(gc,true,left+x,top+y,w,h,	Color.black,null,dec.name);
       	//G.frameRect(gc, Color.red, left+x,top+y,w,h);
       }   	
   }
    private void drawExtraChips(Graphics gc,EuphoriaBoard gb)
     {
         // draw the allegiance markers
         int CELLSIZE = gb.CELLSIZE;
     	for(Allegiance allegiance : Allegiance.values())
     	{
     	int ord = allegiance.ordinal();
     	int val = gb.getAllegianceValue(allegiance);
     	
     	if((allegiance!=Allegiance.Icarite) && (allegiance!=Allegiance.Factionless))
     	{	
     		int tunnelPos = gb.getTunnelPosition(allegiance);
     		if(tunnelPos<gb.euphorianTunnelSteps.length-1)
     		{
     		EuphoriaCell end = gb.tunnelEnds[ord];
     		int xp = gb.positionToX(end.center_x/100);
     		int yp = gb.positionToY(end.center_y/100);
     		EuphoriaChip.Hubcap.drawChip(gc,this,CELLSIZE*2,xp,yp,null);
     		}
     	}
     	if(val>=ALLEGIANCE_TIER_1)
     		{
     		double bounds[] = gb.commodityBounds[ord];
     		int xp = gb.positionToX(bounds[0]/100);
     		int yp = gb.positionToY(bounds[1]/100);
     		EuphoriaChip.Level1Markers[ord].drawChip(gc,this,CELLSIZE,xp,yp,null);
     		}
     	if(val>=ALLEGIANCE_TIER_2)
     		{
     		{
     		EuphoriaCell end = gb.tunnelBenefitMarker[ord];
     		int xp = gb.positionToX(end.center_x/100);
     		int yp = gb.positionToY(end.center_y/100);
     		EuphoriaChip.Level2Markers[ord].drawChip(gc,this,CELLSIZE,xp-CELLSIZE/2,yp+CELLSIZE,null);
     		}
     		// special case for icarites
     		if(allegiance==Allegiance.Icarite)
     			{
         		EuphoriaCell end = gb.icariteWindSalon;
         		int xp = gb.positionToX(end.center_x/100);
         		int yp = gb.positionToY(end.center_y/100);
         		EuphoriaChip.Level2Markers[ord].drawChip(gc,this,CELLSIZE,xp-CELLSIZE/2,yp+CELLSIZE,null);
      			}
     		}
       	}
  
     }
    private boolean drawCardProxy(Graphics gc,HitPoint highlight,int sz,EuphoriaCell cell,int xpos,int ypos,
    		EuphoriaChip pr,double xstep,double ystep,String msg)
    {	boolean toponly = "toponly".equals(msg);
    	if(toponly) { msg = null; }
    	EuphoriaCell tempCell = cell;
    	if(pr!=null)
    		{tempCell = new EuphoriaCell(); 
    		tempCell.copyAllFrom(cell);
    		tempCell.color = cell.color;
    		tempCell.reInit();
    		for(int i=0,ncards = Math.max(1,cell.height());
				i<ncards;
				i++) 
    			{ EuphoriaChip replacement = pr;
    			  if(toponly && (i+1)<ncards) { replacement = cell.chipAtIndex(i); }
    			 tempCell.addChip(replacement); }
    		}
		cell.defaultScale = sz;
		if(tempCell.drawStack(gc,this,highlight,sz,xpos,ypos,0,xstep,ystep,msg))
		{	highlight.hitObject = cell;
		return(true);
		}
		return(false);
    }
    // draw a stack, ? as message to draw backs, topOnly to hide only the top
    private boolean drawStack(Graphics gc,HitPoint highlight,int size,EuphoriaCell cell,int xpos,int ypos,double xstep,double ystep,String msg)
    {	
    	boolean empty = (cell.height()==0);
    	boolean proxy =  empty || "toponly".equals(msg);
    	if("?".equals(msg)) { proxy = true; msg = null; }
    	if(proxy)
    	{	EuphoriaChip ch = cell.contentType().isArtifact()
    					? empty ? ArtifactChip.CardBlank : ArtifactChip.CardBack
    					: empty ? RecruitChip.CardBlank  : RecruitChip.CardBack;
    		return drawCardProxy(gc,highlight,size,cell,xpos,ypos,ch,xstep,ystep,msg);
    	}
    	else
    	{
    		return (cell.drawStack(gc, this, highlight, size, xpos, ypos, 0, xstep, ystep,msg));
    	}
    }
    
    private void drawStackOnPlayer(Graphics gc,commonPlayer pl,EuphoriaBoard gb,HitPoint highlight,Rectangle r,
    		EuphoriaCell cell,int xpos,int ypos,HitPoint tip,boolean fromHiddenWindow)
    {	
    	boolean hit = false;
    	int CELLSIZE = gb.CELLSIZE;
    	EuphoriaId rack = cell.rackLocation();

    	numberMenu.saveSequenceNumber(cell,xpos,ypos);

    	switch(rack)
    	{	 case PlayerNewWorker:
    			cell.defaultScale = CELLSIZE; 
    			hit = cell.drawStack(gc,this,highlight,CELLSIZE,xpos,ypos,0,0.2,0.5,"?");
    			break;
    		 case PlayerWorker:
    			cell.defaultScale = CELLSIZE; 
    			hit = cell.drawStack(gc,this,highlight,CELLSIZE,xpos,ypos,0,0.9,null);
    			if(hit)
    				{
    				switch (gb.getGuiState())
    				{
    				case PlaceNew:
    				case Place:
    				case PlaceAnother:
    					if(gb.usingDoubles) { highlight.hit_index = cell.findChip(gb.doublesElgible); }
    					break;
    					default: break;
    				}
    				}
   			break;
    		case PlayerAuthority:
    			cell.defaultScale = CELLSIZE; 
    			hit = cell.drawStack(gc,this,highlight,10*CELLSIZE/5,xpos,ypos,0,0.04,0.0,""+cell.height());
    			break;
       		case PlayerActiveRecruits:
     			cell.defaultScale = CELLSIZE;
     			if(gb.hasReducedRecruits)
     			{
     			try { 
     			// add an allegiance medallion to the card view, because the card
     			// is too small to read, and the colors no distinctive enough.  This is
     			// piggybacked onto the altChipSet mechanism.
     			showMedallions = true;
    			hit = cell.drawStack(gc,this,highlight,3*CELLSIZE/2,xpos,ypos,0,0.2,0.2,null);
     			} 
     			finally { showMedallions = false; }
     			}else
     			{	// always hide the active recruits that haven't been selected for sure
     				hit=drawStack(gc,highlight,3*CELLSIZE/2,cell, xpos,ypos,0.15,0.15,"?");
     			}
     			break;
       		case PlayerArtifacts:
       			cell.defaultScale = CELLSIZE;
 
       			{
       			// if we're allowed to pick them up, draw face up.  Don't allow an uncommitted card to be seen.
       			boolean isDest = gb.isDest(cell);
       			// always hidden and unselectable in the un-enlarged display
       			hit = drawStack(gc,null,CELLSIZE/2,cell,xpos,ypos,1.25/(1+cell.height()),0.0,"?");
        		if(hit && !isDest )
       			{	if(highlight.down || !G.isTouchInterface() )
       				{
       				EuphoriaChip ch = cell.chipAtIndex(Math.max(0,Math.min(cell.height()-1,highlight.hit_index)));
       				if(ch!=null)
       				{
       					ch.drawChip(gc,this,CELLSIZE*2,xpos,ypos-CELLSIZE,null);
       				}}
       				else {
       					highlight.hitCode = EuphoriaId.ShowPlayerPeek;
       				}
       			}}
        			break;
       		case PlayerDilemma:
       			cell.defaultScale = CELLSIZE;
       			EPlayer p = gb.getPlayer(cell.color);
       			if(p.testPFlag(PFlag.HasResolvedDilemma))
       			{
       			 hit = cell.drawStack(gc,this,null,CELLSIZE, xpos,ypos,0,0,null);
       			}
       			else
       			{ hit = drawCardProxy(gc,highlight,CELLSIZE,cell, xpos,ypos,DilemmaChip.CardBack,0,0,null);
       			}
       			break;
       		case PlayerHiddenRecruits:
     			cell.defaultScale = CELLSIZE;
    			hit = drawCardProxy(gc,highlight,3*CELLSIZE/2,cell, xpos,ypos,(cell.height()==0)?RecruitChip.CardBlank:RecruitChip.CardBack,0.15,0.0,null);
    			break;
    		default:
    			{
     			cell.defaultScale = CELLSIZE;
     			double scale = 1.0-((cell.height()-5.0)/(cell.height()+1));
     			double yscale = 0.2*scale;
     			double xscale = 0.1*scale;
    			hit = cell.drawStack(gc,this,highlight,CELLSIZE,xpos,ypos,0,xscale,yscale,null);
    			}
    			break;
    			
    	}
    	if(!hit && (gb.getGuiState()!=EuphoriaState.Puzzle) && (gb.pickedObject!=null) && (highlight!=null) && (r!=null))
    	{
    		hit = G.pointInRect(highlight,r);	// just drop anywhere
    		if(hit)
    		{
    			highlight.hitCode = cell.rackLocation;
    			highlight.hitObject = cell;
    			highlight.hit_index = 0;
    			highlight.spriteRect = r;
    		}
    	}
    	if(hit)
    	{	highlight.awidth = cell.defaultScale;
    		highlight.arrow = (gb.movingObjectIndex()>=0) ? StockArt.DownArrow : StockArt.UpArrow;
    		highlight.spriteColor = Color.red;
    		
    	}    
  
    	String desc = rack.defaultDescription;
    	if((desc!=null) && !"".equals(desc))
    		{HitPoint.setHelpTextNear(tip, xpos,ypos, cell.defaultScale,cell.defaultScale,
   					s.get0or1(desc,cell.height()));
    		}

    }  
   
    // this is where recruits are drawn, most for presentation only, but the
    // two notable exceptions are Rowena the Mentor and Chaga the Gamer
    private boolean drawPlayerRecruits(Graphics gc,EuphoriaBoard gb,commonPlayer pl,EuphoriaCell c,Rectangle brect,HitPoint highlight,HitPoint tip)
    {	EPlayer p = gb.getPlayer(pl.boardIndex);
      	double newrotation = pl.displayRotation;
    	boolean activeRecruits = c.rackLocation==EuphoriaId.PlayerActiveRecruits;
    	int cx=0;
    	int cy=0;
    	// this is called in the context of drawing the player's stuff, so the player's
    	// rotation is in effect.  We want to rotate the recruits so the will be displayed
    	// to the current player.
     	if(newrotation!=0)
    	{
    	cx = G.centerX(brect);
    	cy = G.centerY(brect);
    	GC.setRotation(gc, newrotation, cx, cy);
    	HitPoint.setRotation(tip,newrotation, cx, cy);
    	}
     	if(gc!=null) 
     		{ Image im = EuphoriaChip.CloudBackground.getImage(loader);
     		im.drawImage(gc,G.Left(brect),G.Top(brect),G.Width(brect),G.Height(brect)); 
     		}
     	int CELLSIZE = (G.Height(brect)/3);
     	int xp = G.centerX(brect);
     	int yp = G.Top(brect)+CELLSIZE/2;
     	int np = c.height();
     	int sz = 3*CELLSIZE/2;
     	int step = (int)(np>3?(sz*0.4) : (sz*0.65));
     	String test = ((c.height()>0)&&((c.rackLocation==EuphoriaId.PlayerActiveRecruits)||(c.rackLocation==EuphoriaId.PlayerHiddenRecruits)))
    					? ((RecruitChip)(c.topChip())).tested 
    					: null;
    					
    	if(np<3)
    	{
    		yp += CELLSIZE/3;
    		sz = 2*CELLSIZE;
    	}
    	boolean somehit = false;
    	// 
    	// draw the stack one chip at a time, so we can inspect 
    	// and do special things with specific chips
    	for(int idx = 0; idx<c.height(); idx++)
    	{
    	EuphoriaChip recruit = c.chipAtIndex(idx);
    	EuphoriaId rack = c.rackLocation();
    	boolean hit = recruit.drawChip(gc,this,sz,xp,yp,null,rack,test);
    	somehit |= hit;
    	// draw the eye box
		  if(StockArt.Eye.drawChip(gc,this,sz/12,xp+(int)(sz*0.45),yp-(int)(sz*0.28),tip,EuphoriaId.ShowChip,null,1,1.33))
		  {
			  tip.hitObject = recruit;
			  tip.hit_index = pl.boardIndex;
		  }

		if(activeRecruits && (recruit==RecruitChip.RowenaTheMentor) && (highlight!=null))
    	{	if(p.canUseRowenaTheMentor())
    		{
    		Rectangle r = new Rectangle(xp-sz/2+sz/10,yp+sz/20,sz/2,sz/8);
    		if(GC.handleSquareButton(gc,r, 
            		highlight,s.get(UseRecruitAbility),
                    HighlightColor, rackBackGroundColor))
				{ hit = true;
				  highlight.hitCode = EuphoriaId.RecruitOption;  
				  highlight.hitObject = RecruitChip.RowenaTheMentor;
				}
    		}
    	}
    	if(activeRecruits && recruit==RecruitChip.ChagaTheGamer && (highlight!=null))
    	{	EuphoriaState state = gb.getGuiState();
    		if(((state==EuphoriaState.PlaceOrRetrieve) || (state==EuphoriaState.Retrieve))
    				&& p.canUseChagaTheGamer())
    		{
    			Rectangle r = new Rectangle(xp-sz/2+sz/10,yp+sz/20,sz/2,sz/8);
        		if(GC.handleSquareButton(gc,r, 
                		highlight,s.get(UseRecruitAbility),
                        HighlightColor, rackBackGroundColor))
    				{ hit = true;
    				  highlight.hitCode = EuphoriaId.RecruitOption;  
    				  highlight.hitObject = RecruitChip.ChagaTheGamer;
    				}
    		}
    	}
    	if(c.rackLocation()==EuphoriaId.PlayerDilemma)
    	{	
    		// offer the choice to resolve the dilemma
    		if(gb.getGuiState().isInitialWorkerState() 
    				&& (gb.pickedSourceStack.size()==0)
    				&& gb.canResolveDilemma(p)
    				&& (highlight!=null)
    				)	// we're in the beginning
    		{	
    			yp += 2*CELLSIZE/3;
    			xp -= CELLSIZE/2;
    			Rectangle fight = new Rectangle(xp,yp,CELLSIZE,CELLSIZE/4);
    			Rectangle join = new Rectangle(xp,yp+CELLSIZE/3,CELLSIZE,CELLSIZE/4);
    			if(GC.handleSquareButton(gc,fight, 
                		highlight,s.get(FightTheOpressor),
                        HighlightColor, rackBackGroundColor))
    				{ hit = true;
    				  highlight.hitCode = EuphoriaId.FightTheOpressor; 
    				  highlight.setHelpText(s.get(ExplainFightTheOpressor));
    				} 
    			
    			if(GC.handleSquareButton(gc,join, 
                		highlight,s.get(JoinTheEstablishment),
                        HighlightColor, rackBackGroundColor))
    				{ hit = true;
    				highlight.hitCode = EuphoriaId.JoinTheEstablishment; 
    				highlight.setHelpText(s.get(ExplainJoinTheEstablishment,p.color.name()));
    				}

    		}
    	}
    	yp += step;
    	}
    	if(G.pointInRect(tip,brect)
    			&& (tip.hitCode==DefaultId.HitNoWhere)
    			&& !somehit)
    	{
    		tip.hitCode = EuphoriaId.ShowPlayerView;
    		tip.hit_index = EPlayer.PlayerView.Normal.ordinal();
    		tip.hitObject = p.artifacts;
    		tip.awidth =CELLSIZE/4;
     		tip.spriteColor = Color.red;
    	}
    	if(newrotation!=0)
    	{
    	GC.setRotation(gc, -newrotation, cx, cy);
        HitPoint.setRotation(tip,-newrotation, cx, cy);
    	}
    	return(somehit);
    }

    private void framePlayer(Graphics gc,EPlayer p,Rectangle r)
    {
     	GC.fillRect(gc,playerBackground[p.color.ordinal()],r);
     	//EuphoriaChip.PlayMat.getImage(loader).centerImage(gc,r);
    	GC.frameRect(gc,Color.black,r);

    }
    private boolean censorHiddenInfo(EuphoriaBoard gb,int player,HitPoint highlight)
    {	
    	boolean visible =
    			allowed_to_edit		// review or game over
    			|| (allPlayersLocal())	// main table in offline mode
    			|| (player==getActivePlayer().boardIndex)	// our info
    			|| (remoteWindowIndex(highlight)>=0)
    			;
    	return (!visible);
    }
    //
    // in a few cases, a particular Artifact is required, but the default stack drawing
    // has no mechanism to filter the sensitivity.  This post-processes the stack 
    // drawing hit, and un-hits the missteps.
    //
    private void filterArtifactHits(HitPoint hp,EuphoriaBoard gb,EuphoriaCell cell)
    {
    	switch(gb.getGuiState())
    	{
    	case PayCost:
    		{
    		CommonMoveStack all = gb.GetListOfMoves();
    		int hit_index = hp.hit_index;
    		for(int lim=all.size()-1; lim>=0; lim--)
    			{
    			EuphoriaMovespec m = (EuphoriaMovespec)all.elementAt(lim);
    			if((m.op==MOVE_ITEM_TO_BOARD)
    					&& (m.source==EuphoriaId.PlayerArtifacts)
    					&& (m.from_row==hit_index)) { return; }
    			}	
    		hp.neutralize();
    		}
    		break;
    	default: break;
    	}
    }
    private int globalPlayer = 0;
    EuphoriaCell globalRecruits = null;
    private void drawGlobalRecruits(Graphics gc,EuphoriaBoard gb,HitPoint any)
    {	if(globalRecruits!=null)
    	{
			 drawPlayerRecruits(gc,gb,getPlayerOrTemp(globalPlayer), globalRecruits,playerRecruitRect,
					 globalPlayer==gb.whoseTurn?any:null,any); 
    	}
    }
    
    private EuphoriaChip bigChip = null;
    private int bigChipPlayer = 0;
    private void drawBigChip(Graphics gc,double rotation,Rectangle r,HitPoint hp)
    {	EuphoriaChip ch = bigChip;
    	if(ch!=null)
    	{
        	if(rotation!=0) { GC.setRotatedContext(gc,r,hp,rotation); }
        	if(ch.drawChip(gc,this,r,hp,EuphoriaId.ShowChip,(String)null)) { hp.hitObject = null;}
        	if(rotation!=0) { GC.unsetRotatedContext(gc,hp); }
    	}
    }
    public double[] iconTextScale = new double[] { 1,0.9,-0.1,-0.1};
    public Drawable getPlayerIcon(int n)
    {	EPlayer p = bb.getPlayer(n);
		Colors color = p.color;
		playerTextIconScale = iconTextScale;
    	return EuphoriaChip.getKnowledge(color);
    }
    //
    // if r is not null, the whole rectangle is the target instead of just the individual cell
    //
    private void drawPlayerStuff(Graphics gc,EuphoriaBoard gb,HitPoint highlight,Rectangle r,int player,
    		Hashtable<EuphoriaCell,EuphoriaMovespec> sources,Hashtable<EuphoriaCell,EuphoriaMovespec> dests,
    		HitPoint tip,boolean fromHiddenWindow)
    {	EPlayer p = gb.getPlayer(player);
    	Colors color = p.color;
     	Rectangle pr =  r;
     	EuphoriaState state = gb.getGuiState();
		// true to use the main screen in addition to the player area
     	boolean useFullScreen = !allPlayersLocal() || (!fromHiddenWindow && !reviewMode() && (player==gb.whoseTurn));
     	int topR = G.Top(r);
     	int leftR = G.Left(r);
     	int heightR = G.Height(r);
     	int widthR = G.Width(r);
     	int unitSize = widthR/12;
       	int xp = leftR+unitSize/2;
    	int yp = topR+heightR-unitSize/2;
    	int ypm = yp-unitSize-unitSize/2;
    	boolean anySelectionAllowed = !censorHiddenInfo(gb,player,highlight);
    	GC.setFont(gc,standardPlainFont());
    	PlayerView view = fromHiddenWindow ? p.hiddenView : p.view;
   
    	if(highlight!=null && autoCardMode && payingCards && (view == EPlayer.PlayerView.Normal)) 
    	{
    		view = p.hiddenView = p.view = EPlayer.PlayerView.AutoArtifacts;
    	}
    	else if(view==EPlayer.PlayerView.AutoArtifacts)
    	{	if(!payingCards)
    			{ view = p.hiddenView = p.view = EPlayer.PlayerView.Normal; }
    	}
    	GC.setFont(gc,labelFont);
    	EuphoriaChip.getKnowledge(color).drawChip(gc,this,unitSize, xp,G.Top(r)+unitSize+unitSize/2,""+p.knowledge);
    	EuphoriaChip.getMorale(color).drawChip(gc, this,unitSize, xp, G.Top(r)+(int)(unitSize*2.4),""+p.morale);
    	
    	commonPlayer pl = getPlayerOrTemp(player);
    	drawStackOnPlayer(gc,pl,gb,
    			gb.legalToHitPlayer(p.authority,sources,dests)?highlight:null,
    			pr,p.authority,
    			xp-unitSize/3,G.Top(r)+(int)(unitSize*4),tip,fromHiddenWindow);
    	boolean hit =false;
		boolean hide = allPlayersLocal() && !fromHiddenWindow && (view==PlayerView.AutoArtifacts);
    	switch(view)
    	{
       	default: throw G.Error("Not expeting view %s",view);
       	case HiddenRecruits:
       		{
       		EuphoriaCell c = p.hiddenRecruits;
       		int h = c.height();
       		int sz = unitSize*((h<=1) ? 8 : 4);
       		c.defaultScale = unitSize*6;
       		drawRecruitStack(p,gc,p.hiddenRecruits,tip,sz,xp+unitSize*4,yp-unitSize*2,(h<=2?1.0:0.55),0.0);
       		
       		if(useFullScreen)
       			{ globalPlayer = player;
       			  globalRecruits = c;
       			}
       		}
    		break;
    	case ActiveRecruits:
    		{
    		EuphoriaCell c = p.activeRecruits;
    		int h = c.height();
    		int sz = unitSize*((h<=1) ? 8 : 4);
    		c.defaultScale = unitSize*6;
    		
    		hit = drawRecruitStack(p,gc,c,tip,
    					sz,xp+unitSize*4,yp-unitSize*2,
    					(h<=2?1.0:h<=4 ? 0.55 : 0.4),0.1); 
    		if(useFullScreen) 
    			{ globalPlayer = player;
    			  globalRecruits = c;
    				//hit = drawPlayerRecruits(gc,gb,player,p, c,playerRecruitRect,highlight,tip); 
    			}
    		}
    		break;
    	case Dilemma:
    		p.dilemma.defaultScale = unitSize*9;
    		hit = p.dilemma.drawStack(gc,this,null,unitSize*6,xp+unitSize*5,yp-unitSize*2,0,1.5,0.0,null); 
    		if(useFullScreen) 
    			{ globalPlayer = player;
    			  globalRecruits = p.dilemma;
    				//hit |= drawPlayerRecruits(gc,gb,player,p, p.dilemma,playerRecruitRect,highlight,tip); 
    			} 
    		break;
    	case Artifacts:
    	case AutoArtifacts:
    		p.artifacts.defaultScale = unitSize;
    		if(gb.isDest(p.artifacts))
    		{
    		hit = drawCardProxy(gc,highlight,unitSize,p.artifacts, xp+2*unitSize/2,yp-unitSize,
       					ArtifactChip.CardBack,1.6,0.0,"toponly");
    			
    		}
    		else
    		{
    		HitPoint high = (gb.legalToHitPlayer(p.artifacts,sources,dests)?highlight:null);
    		hit = drawStack(gc,high,unitSize,p.artifacts, xp+2*unitSize/2,yp-unitSize,1.6,0.0,(hide ? "?" : null));
    		if(hit)
    			{
    			 filterArtifactHits(high,gb,p.artifacts);
    			}
    		}
     		break;
     	case Normal:
     		{
   		{	if(globalPlayer==player) { globalRecruits = null; }
     	   	// draw new workers over the player picture.
           	if(state!=EuphoriaState.Puzzle)
           		{drawStackOnPlayer(gc,pl,gb,gb.legalToHitPlayer(p.marketBasket,sources,dests)?highlight:null,pr,p.marketBasket,xp+unitSize*2,yp,tip,fromHiddenWindow);
           
           		}
           	drawStackOnPlayer(gc,pl,gb,gb.legalToHitPlayer(p.newWorkers,sources,dests)?highlight:null,null,p.newWorkers,xp+unitSize*2,yp,tip,fromHiddenWindow);  		

        	xp+= unitSize*4;
        	drawStackOnPlayer(gc,pl,gb,gb.legalToHitPlayer(p.workers,sources,dests)?highlight:null,pr,p.workers,xp,yp,tip,fromHiddenWindow);
        	StockArt.SmallO.drawChip(gc,this,unitSize*5/2,xp+unitSize/2,yp,""+p.totalWorkers);
        	
        	{
        	int hx = xp+unitSize*3;
        	int hy = G.Top(r)+unitSize/2;
        	drawStackOnPlayer(gc,pl,gb,gb.legalToHitPlayer(p.dilemma,sources,dests)?highlight:null,pr,p.dilemma,
        			hx,hy ,tip,fromHiddenWindow);
        	Rectangle ht = new Rectangle(xp+unitSize*2,G.Top(r),unitSize*2,unitSize);
        	if( (p.dilemma.height()>0)
        			&& G.pointInRect(tip,ht) 
       			&& (p.testPFlag(PFlag.HasResolvedDilemma) || anySelectionAllowed)
        			&& (tip.hitCode==DefaultId.HitNoWhere))
         	{	tip.hitCode = EuphoriaId.ShowPlayerView;
         		tip.hitObject = p.artifacts;
         		tip.spriteRect = ht;
         		tip.hit_index =  EPlayer.PlayerView.Dilemma.ordinal();
        		tip.spriteColor = Color.red;

          	}}

         	xp += unitSize;
        	drawStackOnPlayer(gc,pl,gb,gb.legalToHitPlayer(p.food,sources,dests)?highlight:null,pr,p.food,xp,yp,tip,fromHiddenWindow);
        	drawStackOnPlayer(gc,pl,gb,gb.legalToHitPlayer(p.water,sources,dests)?highlight:null,pr,p.water,xp,ypm,tip,fromHiddenWindow);
        	xp += unitSize;
        	drawStackOnPlayer(gc,pl,gb,gb.legalToHitPlayer(p.bliss,sources,dests)?highlight:null,pr,p.bliss,xp,yp,tip,fromHiddenWindow);
        	drawStackOnPlayer(gc,pl,gb,gb.legalToHitPlayer(p.energy,sources,dests)?highlight:null,pr,p.energy,xp,ypm,tip,fromHiddenWindow);
        	xp += unitSize;
        	drawStackOnPlayer(gc,pl,gb,gb.legalToHitPlayer(p.gold,sources,dests)?highlight:null,pr,p.gold,xp,yp,tip,fromHiddenWindow);
        	drawStackOnPlayer(gc,pl,gb,gb.legalToHitPlayer(p.stone,sources,dests)?highlight:null,pr,p.stone,xp,ypm,tip,fromHiddenWindow);
        	xp += unitSize;
           	drawStackOnPlayer(gc,pl,gb,gb.legalToHitPlayer(p.clay,sources,dests)?highlight:null,pr,p.clay,xp,yp,tip,fromHiddenWindow);

           	drawStackOnPlayer(gc,pl,gb,gb.legalToHitPlayer(p.artifacts,sources,dests)?highlight:null,pr,p.artifacts,xp,ypm,tip,
           			fromHiddenWindow);

        	// if we missed the artifacts but are nearby, put up the eye
           	{
           	Rectangle hr = new Rectangle(xp-unitSize,ypm-unitSize/2,unitSize*2,unitSize);
         	if( (p.artifacts.height()>0)
         			&& G.pointInRect(anySelectionAllowed
         					?tip
         					:highlight,	hr) && (tip.hitCode==DefaultId.HitNoWhere))
         	{	tip.hitCode = EuphoriaId.ShowPlayerPeek;
         		tip.hitObject = p.artifacts;
         		tip.hit_index =  EPlayer.PlayerView.Artifacts.ordinal();
         		tip.spriteRect = hr;
        		tip.spriteColor = Color.red;

          	}}

        	xp += 2*unitSize+unitSize/2;
        	// require accurate placement for recruit cards because there are 2 piles.
        	drawStackOnPlayer(gc,pl,gb,gb.legalToHitPlayer(p.activeRecruits,sources,dests)?highlight:null,null,p.activeRecruits,xp,yp,tip,fromHiddenWindow);
           	
        	{
        	drawStackOnPlayer(gc,pl,gb,null,null,p.allegianceStars,xp,yp,null,fromHiddenWindow);
        	Rectangle hr = new Rectangle(xp-unitSize,yp-unitSize/2,unitSize*2,unitSize);
         	if( (p.activeRecruits.height()>0)
         			&& G.pointInRect(tip,hr) 
         			&& p.hasReducedRecruits()
         			// added 10/2/2023, don't allow peeking at other players recruits when they
         			// have decided but we're still in the selection phase
         			&& ((p.boardIndex==getActivePlayer().boardIndex) || !simultaneousTurnsAllowed())
         			&& (tip.hitCode==DefaultId.HitNoWhere))
         	{	tip.hitCode = EuphoriaId.ShowPlayerView;
         		tip.hitObject = p.activeRecruits;
         		tip.hit_index = EPlayer.PlayerView.ActiveRecruits.ordinal();
         		tip.spriteRect = hr;
        		tip.spriteColor = Color.red;
          	}}
         	

        	// require accurate placement for recruit cards because there are 2 piles.
         	{
        	drawStackOnPlayer(gc,pl,gb,
        			gb.legalToHitPlayer(p.hiddenRecruits,sources,dests)?highlight:null,
        			null,p.hiddenRecruits,xp,ypm,tip,fromHiddenWindow);
         	Rectangle hr = new Rectangle(xp-unitSize,
         					ypm-unitSize/2,
         					unitSize*2,
         					unitSize);
         	if( (p.hiddenRecruits.height()>0) 
         		
         			&& G.pointInRect(anySelectionAllowed
         								? tip 
         								: highlight,
         					hr)
         			&& (tip.hitCode==DefaultId.HitNoWhere))
         	{	tip.hitCode = EuphoriaId.ShowPlayerView;
         		tip.hitObject = p.hiddenRecruits;
         		tip.spriteRect = hr;
        		tip.hit_index = EPlayer.PlayerView.HiddenRecruits.ordinal();
        		tip.spriteColor = Color.red;

          	}
        	}}
   		
 		if(allPlayersLocal() && bb.ephemeralRecruitMode() && !p.hasReducedRecruits())
 		{	Rectangle subr = new Rectangle(leftR+widthR/4,topR+heightR/4,widthR/2,heightR/2);
 			GC.setFont(gc,largeBoldFont());
 			if(GC.handleSquareButton(gc, subr,  tip,
 					s.get(ChooseRecruitState),HighlightColor, boardBackgroundColor))
 			{	tip.hitCode = EuphoriaId.ChooseRecruit;
 				tip.hit_index = player;
 			}
 		}
 
     		}
     		
     		break;
    	}
    	if(hit)
    	{
    		tip.awidth = unitSize;
    		tip.spriteColor = Color.red;
    		tip.arrow = (bb.pickedObject!=null) ? StockArt.DownArrow : StockArt.UpArrow;
    	}
    	if(G.pointInRect(tip,r) 
    			&& !hit
    			&& (tip.hitCode==DefaultId.HitNoWhere)
    			&& (p.view!=EPlayer.PlayerView.Normal))
		{
			tip.hitCode = EuphoriaId.ShowPlayerView;
			tip.hitObject = p.artifacts;
			tip.awidth =unitSize;
    		tip.spriteColor = Color.red;
			tip.hit_index =  (hide && (view==PlayerView.AutoArtifacts)) ? PlayerView.Artifacts.ordinal() : PlayerView.Normal.ordinal();
    		tip.arrow = StockArt.NoEye;
		}
    	
    	if(allPlayersLocal()
    			&& !fromHiddenWindow 
    			&& enableDone(gb)
    			&& plannedSeating()
    			&& (highlight!=null)
    			)
    	{	int dw = unitSize*3;
    		handleDone(gc,gb,highlight,new Rectangle(leftR+widthR-dw,topR,dw,dw/2));
    	}
     }
    boolean isIIB() { return bb.isIIB(); }
    private EuphoriaChip popupDisplay = null;
    private int popupDisplay_x = 0;
    private int popupDisplay_y = 0;
    private int popupDisplay_s = 0;
    private long animPhase = 0;
    private void drawStackOnBoard(Graphics gc,EuphoriaBoard gb,HitPoint highlight,EuphoriaCell cell,int xpos,int ypos,HitPoint tip)
    {	boolean doTips = true;
    	cell.rotateCurrentCenter(gc,xpos, ypos);
     	boolean hit = false;
    	int CELLSIZE = gb.CELLSIZE;
    	EuphoriaId rack = cell.rackLocation();
    	EuphoriaChip top = cell.topChip();
    	if((top!=null) && (highlight!=null) && top.isWorker() && (gb.pickedObject==null) )
    	{	animPhase = G.Date();
    		int mp = (animPhase%2000>1000)?12:11;
     		StockArt.SmallO.drawChip(gc,this,(CELLSIZE*mp)/2,xpos,ypos,null);   
    		repaint(1000);
    	}
    	switch(rack)
    	{	case MoraleTrack:
    		case KnowledgeTrack: 
    			{	double spacing = 0.2;
    				cell.defaultScale = CELLSIZE;
    				hit = cell.drawStack(gc,this,highlight,CELLSIZE,xpos,(int)(ypos+cell.height()*spacing*CELLSIZE/2),
    						0,spacing,
    						null);
    				break;
    			}
    			
       		case EnergyPool:
    		case AquiferPool:
    			cell.defaultScale = CELLSIZE;
				hit = cell.drawStack(gc,this,highlight,CELLSIZE,xpos,ypos,0,0.5,0.0,null);
				break;
    		case FarmPool:
     		case BlissPool:
    			cell.defaultScale = CELLSIZE;
				hit = cell.drawStack(gc,this,highlight,CELLSIZE,xpos,ypos,0,0.5,0.0,null);
				break;
    		case StoneQuarry:
    			cell.defaultScale = CELLSIZE;
    			hit = cell.drawStack(gc,this,highlight,CELLSIZE,xpos,ypos,0,-0.15,-0.25,null);
    			break;

    		case ClayPit:
    		case GoldMine:
				cell.defaultScale = CELLSIZE;
				hit = cell.drawStack(gc,this,highlight,CELLSIZE,xpos,ypos,0,-0.25,0.25,null);
    			break;
    		case ArtifactDiscards:
    			{
    			cell.defaultScale = CELLSIZE;
    			if(isIIB())
    				{ ypos+= CELLSIZE*2.4; 
    				  xpos -= CELLSIZE/3;
    				}
    			if(cell.height()==0)
    			{
    				hit = drawCardProxy(gc,highlight,CELLSIZE,cell,xpos,ypos,ArtifactChip.CardBlank,0.01,0.0,null);
    			}
    			else {
    				hit = cell.drawStack(gc,this,highlight,CELLSIZE,xpos,ypos,0,0.0,0.0,null);
    			}}
    			break;
    		case ArtifactDeck:
    			if(!isIIB())
    			{
				cell.defaultScale = CELLSIZE;
    			hit = drawCardProxy(gc,highlight,CELLSIZE,cell,xpos,ypos,ArtifactChip.CardBack,0.01,0.0,null);
    			}
    			break;
    		case MarketBasket:
				cell.defaultScale = CELLSIZE;
    			hit = cell.drawStack(gc,this,highlight,CELLSIZE,xpos,ypos,0,1.5,0.0,null);
    			break;
    		case UnusedMarkets:
    			cell.defaultScale = CELLSIZE/2;
    			hit = drawCardProxy(gc,highlight,CELLSIZE/2,cell,xpos,ypos,MarketChip.CardBack,0.00,0.15,null);   			
    			break;
    		case UnusedWorkers:
    			cell.defaultScale = CELLSIZE;
    			if((gb.getGuiState()==EuphoriaState.Puzzle)
    				&& (cell.height()>0)
    				&& (gb.pickedObject==null)
    				&& (gb.getPlayer(cell.row).totalWorkers>=MAX_WORKERS))
    			{	// special logic to prevent creating too many workers
    				highlight = null;
    			}
    				
    			hit = cell.drawStack(gc,this,highlight,CELLSIZE,xpos,ypos,0,-1.0,0.0,null);
    			break;
    		case UnusedDilemmas:
    			cell.defaultScale = CELLSIZE;
    			hit = cell.drawStack(gc,this,highlight,CELLSIZE*2,xpos,ypos,0,0.00,0.1,null);
    			
    			break;
  			
    		case UnusedRecruits:
    		case UsedRecruits:
    			cell.defaultScale = CELLSIZE;
     			hit = drawCardProxy(gc,highlight,CELLSIZE*2,cell,xpos,ypos,RecruitChip.CardBack,0.00,0.03,null);
    			break;
    			
    		case Market:	// the big market chips, sometimes with authority tokens
    			// pay for lionel is when lionel the cook is dropping food.  It doesn't drop here.
    			boolean lionel = gb.getGuiState()==EuphoriaState.PayForLionel;
     			cell.defaultScale = CELLSIZE;
    			if((cell.height()>0) && (cell.topChip()!=MarketChip.CardBack))
    				{
    				EuphoriaChip ch = cell.chipAtIndex(0);
    				//String tested = ch.tested;
       				hit = cell.drawStack(gc,this,highlight,CELLSIZE,xpos-3*CELLSIZE/2,ypos,0,0.3,-0.1,null);
       				
       				if(G.pointInRect(tip,xpos-2*CELLSIZE,ypos-2*CELLSIZE,2*CELLSIZE,2*CELLSIZE))
       					{	popupDisplay = ch;
       						popupDisplay_s = CELLSIZE*3;
       						popupDisplay_x = xpos+1*CELLSIZE;
       						popupDisplay_y = ypos-3*CELLSIZE;
       					}
    				}
    			else
    			{
    				// draw a simple stack (expected to be of 2 markets
    				hit = cell.drawStack(gc,this,highlight,CELLSIZE,xpos,ypos,0,0.0,0.0,null);
    			}
    			if(cell.ignoredForPlayer!=null) {
    				// lionel the cook
    				if(EuphoriaChip.Food.drawChip(gc,this,CELLSIZE,xpos,ypos,lionel ? highlight : null,cell.rackLocation(),null))
    				{
    					highlight.spriteColor = Color.red;
    					highlight.hitObject =cell;
    					highlight.awidth = CELLSIZE;
    				}
    			}
    			break;
    		case EuphorianAuthority:
    		case SubterranAuthority:
    		case IcariteAuthority:
    		case WastelanderAuthority:
    			cell.defaultScale = CELLSIZE;
    			hit = cell.drawStack(gc,this,highlight,cell.defaultScale,xpos,ypos,0,0.0,0.0,null);
    			break;
    		case Trash:
    			hit = cell.drawStack(gc,this,highlight,CELLSIZE,xpos,ypos,0,0.3,-0.3,null);
    			cell.defaultScale = CELLSIZE;
    			break;
    		case ArtifactBazaar:
    			{
    			cell.defaultScale = CELLSIZE;
    			hit = cell.drawStack(gc,this,highlight,cell.defaultScale,xpos,ypos,0,0.0,0.0,null);
    			break;
 			
    			}
    		default:
				cell.defaultScale = CELLSIZE;
				if(rack.isWorkerCell)
				{
				hit = cell.drawStack(gc,this,highlight,CELLSIZE,xpos,ypos,0,0.3,0.2,null);
				}
				else
				{
				hit = cell.drawStack(gc,this,highlight,CELLSIZE,xpos,ypos,0,0.0,0.0,null);
				}
    			break;
    			
    	}
    	if(hit)
    	{	highlight.awidth = cell.defaultScale;
    		highlight.spriteColor = Color.red;
    		highlight.arrow = (gb.movingObjectIndex()>=0) ? StockArt.DownArrow : StockArt.UpArrow;
    	}
    	if(cell.placementCost==Cost.MarketCost) 
			{ // this turns off the tooltips for closed market building squares
    		  // and not yet built markets.
    		doTips = false; 
			}
    	if(doTips)
    	{	String desc = rack.defaultDescription;
        	if(cell.marketPenalty!=null)
        	{ 
        		StockArt.SmallX.drawChip(gc,this,cell.lastSize(),cell.centerX(),cell.centerY(),null);
        		desc = cell.marketPenalty.getExplanation();
        	}
        	desc = s.get0or1(desc,cell.height());
        	
    		Cost cost = cell.placementCost;
    		if((cost!=null)&&(cost!=Cost.Closed))
    		{	String add = s.get(cost.description);
    			desc = (desc==null? "" : desc + "\n") + s.get(costAddon,add);
    		}
    	if((desc!=null) && !"".equals(desc))
    		{
    		HitPoint.setHelpTextNear(tip, xpos,ypos, cell.defaultScale,cell.defaultScale,TextChunk.colorize(desc,null,gameEventText));
    		}
    	}
    }
   /** draw the board and the chips on it. the gc will normally draw on a background
    * array which contains the slowly changing part of the board. 
    * */
    private void drawBoardElements(Graphics gc, EuphoriaBoard gb, Rectangle brect, HitPoint highlight,
    		Hashtable<EuphoriaCell,EuphoriaMovespec> sources,Hashtable<EuphoriaCell,EuphoriaMovespec> dests,HitPoint tip)
    {
        //
        // now draw the contents of the board and highlights or ornaments.  We're also
    	// called when not actually drawing, to determine if the mouse is pointing at
    	// something which might allow an action.  Either gc or highlight might be
    	// null, but not both.
        //
    	
        // using closestCell is preferable to G.PointInside(highlight, xpos, ypos, CELLRADIUS)
        // because there will be no gaps or overlaps between cells.
    	if(gc!=null) 
    		{ gb.setStatusDisplays(); 
    		  drawExtraChips(gc,gb);
    		}
    	popupDisplay = null;
    	boolean puzzle = gb.getGuiState()==EuphoriaState.Puzzle;
    	boolean moving = gb.pickedObject!=null;
        for(EuphoriaCell cell = gb.displayCells ;	// display cells continues into allCells
        			cell!=null;
        			cell=cell.next)
          {	EuphoriaId rack = cell.rackLocation();
            if(rack.puzzleOnly() && !puzzle) {}
            else
            {
         	int ypos = gb.cellToY(cell);
            int xpos = gb.cellToX(cell);
            boolean canhit = (highlight==null)
            					? false 
            					: moving 
            						? gb.legalToDropOnBoard(cell,dests) 
            						: gb.legalToHitBoard(cell,sources);
            drawStackOnBoard(gc,gb,canhit?highlight:null,cell,xpos,ypos,tip);
        	numberMenu.saveSequenceNumber(cell,xpos,ypos);

            }
          }
        if(tip!=null && tip.hitCode instanceof EuphoriaId)
        {	
        	EuphoriaId rack = (EuphoriaId)tip.hitCode;
        	switch(rack)
        	{
        	case UnusedRecruits:
        	case UsedRecruits:
        	case UnusedMarkets:
        	case UnusedDilemmas:
        		EuphoriaCell c = hitCell(tip);
        		if(c!=null)
        		{
        		int ind = Math.max(0,Math.min(c.height(),tip.hit_index));
        		int xp = tip.hit_x;
        		int yp = tip.hit_y;
        		EuphoriaChip ch = c.chipAtIndex(ind);
        		if(ch!=null) { ch.drawChip(gc,this,c.defaultScale*8,xp+gb.cellSize()*5,yp,null); }
        		}
        		break;
        	default: break;
        	}
        }
        if(popupDisplay!=null)
        {	popupDisplay.drawChip(gc,this,popupDisplay_s,popupDisplay_x,popupDisplay_y,null);
        }
    }
 
    // draw the board zoomed to bounds[]
    private void drawZoomedBoardElements(Graphics gc, EuphoriaBoard gb, Rectangle brect, HitPoint highlight,
    		Hashtable<EuphoriaCell,EuphoriaMovespec>sources,Hashtable<EuphoriaCell,EuphoriaMovespec>dests,double bounds[],HitPoint tip)
    {	//top= 0.0; bottom=1.0; left=0.5 ;right=1.0;
    	if(bounds==null) { drawBoardElements(gc,gb,brect,highlight,sources,dests,tip); }
    	else
    	{
    	double left = bounds[0]/100.0;
    	double top = bounds[1]/100.0;
    	double right = bounds[2]/100.0;
    	double bottom = bounds[3]/100.0;
    	int boardX = G.Left(boardRect);
    	int boardY = G.Top(boardRect);
    	int boardW = G.Width(boardRect);
    	int boardH = G.Height(boardRect);
		double boardRatio = (double)boardW/boardH;
		int viewW = G.Width(brect);
		int viewH = G.Height(brect);
		double viewRatio = (double)viewW/viewH;
		int viewL = G.Left(brect);
		int viewT = G.Top(brect);
		Rectangle dispR;
		double scale;
		// 
		// this is extra tricky because the board aspect ratio is sacrosanct.  All the objects
		// on the board are relative to the full board size, and the shape of the display rectangle
		// has to match the shape of the board image.  The zoom views are not constrained to this
		// so the first determination is if the zoom area will be limited by the height or the width
		if(viewRatio > boardRatio)
		{	// dominated by view height
			int vieww = (int)(viewH*boardRatio);
			dispR = new Rectangle(viewL+(viewW-vieww)/2 , viewT, vieww, viewH);
			scale = (viewH/(boardH*(bottom-top)));
		}
		else
		{	// dominated by view width
			int viewh = (int)(G.Width(brect)/boardRatio);
			dispR = new Rectangle(viewL, viewT+(viewH-viewh)/2 , viewW, viewh);
			scale = viewW/(boardW*(right-left));
		}
		double neww = (boardW*scale);
		double newh = (boardH*scale);
    	Rectangle newBR = new Rectangle(boardX-(int)(left*neww),boardY-(int)(top*newh),(int)neww,(int)newh);
    	
     	if(gc!=null)
    	{	Rectangle oldClip = GC.combinedClip(gc,brect);
    		int xdis = G.Left(dispR)-boardX;
    		int ydis = G.Top(dispR)-boardY;
     		GC.translate(gc,xdis,ydis);
    		drawFixedElements(gc,gb,newBR);
    		GC.translate(gc,-xdis,-ydis);
    		GC.setClip(gc,oldClip);
    	}
    	int hx = G.Left(tip);
    	int hy = G.Top(tip);
     	try {
    		gb.CELLSIZE = (int)(STANDARD_CELLSIZE*scale);
    		gb.SetDisplayRectangle(newBR);
        	double xpos = left+(double)(hx-G.Left(brect))/G.Width(newBR);
        	double ypos = top+(double)(hy-G.Top(brect))/G.Height(newBR);
        	
        	G.SetLeft(tip,gb.positionToX(xpos));		// adjust the mouse coordinates
    		G.SetTop(tip,gb.positionToY(ypos));
    		Rectangle oldClip = GC.combinedClip(gc,brect);
     	   	drawBoardElements(gc,gb,newBR,highlight,sources,dests,tip);
     	   	GC.setClip(gc,oldClip);
     	   	GC.frameRect(gc,Color.red,brect);
      	   	//G.frameRect(gc,Color.red,brect);
      	   	//G.frameRect(gc,Color.green,dispR);
      	}
    	finally 
    		{ gb.CELLSIZE = STANDARD_CELLSIZE;
    	  	  gb.SetDisplayRectangle(boardRect);
    	  	  G.SetLeft(tip,hx);
    	  	  G.SetTop(tip, hy);
       		}
    	}
    }
    
    private void drawStackOnRecruit(int pl,Graphics gc,boolean showHidden,EuphoriaBoard gb,HitPoint highlight,EuphoriaCell cell,int size,int xpos,int ypos,String msg)
    {	cell.rotateCurrentCenter(gc,xpos,ypos);
    	cell.defaultScale = size;
     	if(drawStack(gc,highlight,size,cell,xpos,ypos,0.0,0.6,showHidden ? msg : "?"))
    	{	
    	highlight.awidth = cell.defaultScale/3;
		highlight.spriteColor = Color.red;
    	highlight.arrow = (gb.movingObjectIndex()>=0) ? StockArt.DownArrow : StockArt.UpArrow;
    	}
     	if(cell.height()==1)
     	{
			  if(StockArt.Eye.drawChip(gc,this,size/8,xpos+(int)(size*0.43),ypos-(int)(size*0.26),highlight,EuphoriaId.ShowChip,null,1,1.33))
			  {
				  highlight.hitObject = cell.topChip();
				  highlight.awidth = size/10;
				  highlight.hit_index = pl;
			  }
		
     	}
    }
    public boolean drawRecruitStack(EPlayer p,Graphics gc,EuphoriaCell c,HitPoint hp,int sz,int cx,int cy,double xo,double yo)
    {	boolean hit = false;
    	boolean hitEye = false;
    	for(int i=0;i<c.height();i++)
    	{	int xpos = (int)(cx+i*sz*xo);
    		int ypos = (int)(cy+i*sz*yo);
    		EuphoriaChip ch = c.chipAtIndex(i);
    		if(ch.drawChip(gc,this,sz,xpos,ypos,hitEye?null:hp,c.rackLocation,null,1,1))
    		{	
    		hp.hit_index = PlayerView.Normal.ordinal(); 
			hp.hitCode = EuphoriaId.ShowPlayerView;
			hp.hitObject = c;
			hp.spriteRect = null;
			hp.spriteColor = null;
    		}
    		if(StockArt.Eye.drawChip(gc,this,sz/10,xpos+(int)(sz*0.42),ypos-(int)(sz*0.25),hp,EuphoriaId.ShowChip,null,1,1.33))
    		{	hitEye = true;
    			hp.hitObject = ch;
    			hp.hit_index = p.boardIndex;
    		}
    	}
    	return hit;
    }
    private boolean showHiddenUI = false;
    private int selectedRecruitPlayer(EuphoriaBoard gb)
    {
    	return ( (allPlayersLocal()||reviewOnly)
    				? (simultaneousTurnsAllowed() ? gb.recruitPlayer : gb.whoseTurn)
    				: getActivePlayer().boardIndex
    				);
    }
    private int selectedRecruitPlayer(HitPoint hp,EuphoriaBoard gb)
    {
    	int rp = remoteWindowIndex(hp);
    	return (rp>=0 ? rp : selectedRecruitPlayer(gb));
    }
    
    private void showClosedOverlay(Graphics gc,Rectangle br,HitPoint highlight)
    {	int w = G.Width(br);
    	int h = G.Height(br);
    	Rectangle r = new Rectangle(G.Left(br),G.Top(br),w/20,h/20);
    	GC.fillRect(gc,rackBackGroundColor,r); 
    	GC.frameRect(gc, Color.black, r);
    	StockArt.Checkmark.drawChip(gc, this, w/20,G.centerX(r),G.centerY(r),highlight,EuphoriaId.CloseBox,null);
    }
    
    boolean useEphemeralPick = false;
    EuphoriaChip ephemeralPick = null;
    
    private void drawRecruitElements(Graphics gc,EuphoriaBoard gb,int pl,Rectangle brect,HitPoint highlight,HitPoint anySelect,
    			Hashtable<EuphoriaCell,EuphoriaMovespec>sources,Hashtable<EuphoriaCell,EuphoriaMovespec>dests,boolean fromHidden)
    {	EuphoriaState state = gb.getGuiState();
    	EPlayer p = gb.getPlayer(pl);
    	commonPlayer cpl = getPlayerOrTemp(pl);
    	boolean showHidden = reviewOnly || fromHidden || !allPlayersLocal() || showHiddenUI;
    	double rotation = cpl.displayRotation;
    	EuphoriaChip epicked = p.ephemeralPickedObject;
    	if(useEphemeralPick) { ephemeralPick = epicked; }
    	int cx=0;
    	int cy=0;
    	
    	
    	if(!fromHidden && rotation!=0)
    	{
    	cx = G.centerX(brect);
    	cy = G.centerY(brect);
    	GC.setRotation(gc, rotation, cx, cy);
    	HitPoint.setRotation(highlight,rotation, cx, cy);
    	}
    	int w = G.Width(brect);
    	int h = G.Height(brect);
		int ystep = h/3;
		int xstep = w/2;
		int x = G.Left(brect);
		int y = G.Top(brect);
		int xp = x+xstep/2;
		int yp0=y+ystep/2;
		int yp = yp0;
   		int stepX = w/8;
   		int stepY = h/4;
   		int siz = Math.min(stepX*2, stepY*2);
   		boolean discard = false;
		int CELLSIZE = gb.CELLSIZE;
		int cardScale = siz+siz/2;
		int margin = 3;
		int smallBox = w/20;
		EuphoriaChip.CloudBackground.getImage(loader).drawImage(gc,x,y,w,h	); 
    	switch(state)
    	{
    	default: 
    			//G.Error("not expecting %s",state);
    		break;
     	case RecruitOption:
    	case DieSelectOption:
    	case ConfirmRecruitOption:
    		{ RecruitChip recruit = gb.activeRecruit();
 
    			{ int xx = xp+xstep/2;
    			  int yy = yp+ystep/2;
    			  int sz = xstep+xstep/2;
    			  recruit.drawChip(gc,this,sz,xx,yy,null); 
				  if(StockArt.Eye.drawChip(gc,this,sz/12,xx+(int)(sz*0.45),yy-(int)(sz*0.28),anySelect,EuphoriaId.ShowChip,null,1,1.33))
				  {
					  anySelect.hitObject = recruit;
					  anySelect.hit_index = pl;
				  }
 			
    			}
    		  
    		  if((recruit==RecruitChip.AmandaTheBroker)
    				  ||(recruit==RecruitChip.AmandaTheBroker_V2)
    				  ||(recruit==RecruitChip.YordyTheDemotivator_V2))
    		  {	// amanda lets you select your die roll
    			  int rx = xp;
    			  int ry = yp+3*ystep/2;
    			  for(int i=1;i<=6;i++)
    			  {
    				  WorkerChip ch = WorkerChip.getWorker(p.color,i);
    				  if(ch==gb.selectedDieRoll)
    				  {
    					  StockArt.SmallO.drawChip(gc,this,CELLSIZE*14,rx,ry,null);
    				  }
    				  ch.drawChip(gc,this,CELLSIZE*3,rx,ry,highlight,DieRolls[i-1],null);
    				  rx += CELLSIZE*3;
    			  }
    		  }
    		  else
    		  {
    		  int buttony = yp+3*ystep/2;
    		  int buttonh = ystep/4;
    		  xp -= xstep/6;
    		  xstep += xstep/3;
    		  Rectangle r = new Rectangle(xp,buttony,xstep,buttonh);
              Rectangle rl = new Rectangle(xp,buttony+buttonh*2,xstep,buttonh);
              GC.setFont(gc,largeBoldFont());
             if(recruit==RecruitChip.JuliaTheAcolyte)
             {	// options are keep the same or use bumper's value
            	 WorkerChip bumpee = gb.bumpedWorker;
            	 WorkerChip bumper = gb.bumpingWorker;
            	 int bumperK = bumper.knowledge();
            	 Text option1 = TextChunk.join(TextChunk.create(s.get(EuphoriaId.RecruitFirstJuliaOption.prettyName)),
            				TextGlyph.create("xxxx",bumpee,this,new double[] {1.0,1.0,0.25,-0.25}));
            	 G.SetTop(r,G.Top(r)-G.Height(r)*2/3);
            	 if(GC.handleSquareButton(gc,0,r, highlight,option1,	
            			 	Color.black,Color.white,
                           HighlightColor, rackBackGroundColor))
            	 {
            	 		highlight.hitCode = EuphoriaId.RecruitFirstJuliaOption;
            	 		highlight.hitObject = bumpee;          		 
            	 }
            	 if(bumpee.knowledge()!=bumperK)
            	 {	G.SetTop(r,G.Top(r)+G.Height(r)*4/3);
            	 	Text option2 = TextChunk.join(TextChunk.create(s.get(EuphoriaId.RecruitSecondJuliaOption.prettyName)),
         				TextGlyph.create("xxxx",bumper,this,new double[] {1.0,1.0,0.25,-0.25}));
            	 	if(GC.handleSquareButton(gc,0,r, highlight,option2,	
             			 	Color.black,Color.white,
                            HighlightColor, rackBackGroundColor))
            	 		{
            	 		highlight.hitCode = EuphoriaId.RecruitSecondJuliaOption;
            	 		highlight.hitObject = bumper;
            	 		}
            	 }
            	 
             }
             else if(GC.handleSquareButton(gc,r, 
              		highlight,s.get(UseRecruitAbility),
                      HighlightColor, rackBackGroundColor))
  				{ 
            	highlight.hitObject = recruit;
  				highlight.hitCode = EuphoriaId.RecruitOption; 
  				}
             
    		  if(GC.handleSquareButton(gc,rl, 
                		highlight,s.get(DontUseRecruitAbility),
                        HighlightColor, rackBackGroundColor))
    				{ 
    				highlight.hitCode = GameId.HitDoneButton;
    				}
    		  }
    		}
    		break;
    	case ChooseOneRecruit:
	   	case ConfirmOneRecruit:	    	
	   	case ConfirmActivateRecruit:
		   	{
		   	
		   	EuphoriaCell a1 = p.newRecruits[0];
		   	EuphoriaCell a2 = p.newRecruits[1];
		   	EuphoriaCell active = p.activeRecruits;
		   	EuphoriaCell hidden = p.hiddenRecruits;
		   	int bigCardScale = siz*2;
		   	if((a1.topChip()!=null) || (a2.topChip()!=null))
		   	{
		   	drawStackOnRecruit(pl,gc,showHidden,gb,gb.canHitRecruit(a1,p,sources,dests)?highlight:null,a1,bigCardScale,xp,yp,null);
		   	drawStackOnRecruit(pl,gc,showHidden,gb,gb.canHitRecruit(a2,p,sources,dests)?highlight:null,a2,bigCardScale,xp+bigCardScale,yp,null);
		   	}
		   	drawStackOnRecruit(pl,gc,true,gb,gb.canHitRecruit(active,p,sources,dests)?highlight:null,active,cardScale,xp,yp+ystep*2,active.label);
		   	drawStackOnRecruit(pl,gc,showHidden,gb,gb.canHitRecruit(hidden,p,sources,dests)?highlight:null,hidden,cardScale,xp+xstep,yp+ystep*2,hidden.label);
		   	}
		   	break;
    	case DiscardFactionless:
    	case EphemeralDiscardFactionless:
       	case EphemeralConfirmDiscardFactionless:
    	case ConfirmDiscardFactionless:
    		discard = true;
			//$FALL-THROUGH$
		case ChooseRecruits:
    	case EphemeralChooseRecruits:
    	case ConfirmRecruits:
    	case EphemeralConfirmRecruits:
    	case ActivateOneRecruit:

    		{
    		Font giant = FontManager.getFont(standardPlainFont(), FontManager.Style.Bold, 30);
    		GC.setFont(gc, giant);
       		GC.Text(gc, true,x,y,w,stepY/2 ,Color.yellow,null, s.get(RecruitsForPlayer,prettyName(pl)));
    		// draw the available recruits
       		if(state!=EuphoriaState.ActivateOneRecruit)
       		{
    		for(int i=0;i<p.newRecruits.length;i++)
    		{	EuphoriaCell c1 = p.newRecruits[i];
    			drawStackOnRecruit(pl,gc,true,gb,gb.canHitRecruit(c1,p,sources,dests)?highlight:null,
    					c1,siz,x+stepX*(2*i+1),y+stepY+stepY/4,c1.label);
    		}}
    		if(discard)	// draw the discarded recruits
    		{
    			yp += yp0+stepY*2;
    			EuphoriaCell c1 = p.discardedRecruits;
    			GC.setFont(gc,largeBoldFont());
    			GC.Text(gc, true,x, y+stepY*2,stepX*4 ,stepY/4, Color.yellow,null,c1.label);
    			int actY = y+stepY*3;
    			drawStackOnRecruit(pl,gc,true,gb,gb.canHitRecruit(c1,p,sources,dests)?highlight:null,c1,siz+siz/2,x+stepX*4,actY,c1.label);

       			if(c1.topChip()!=null)
       			{	int doneX = x+w/2+stepX*2;
       				int doneY = actY - stepX/2; 
       				Rectangle done = new Rectangle(doneX,doneY,stepX*2,stepX);
       				if(handleDoneButton(gc,done,highlight,HighlightColor,rackBackGroundColor))
       				{	boolean ephemeralRecruits = bb.ephemeralRecruitMode();
       					highlight.hitCode = ephemeralRecruits
       											? EuphoriaId.EConfirmDiscard
       											: EuphoriaId.ConfirmDiscard;
       				}
       			}   			
    		}
    		else // draw the currently chosen recruits
      		{	yp += yp0+stepY*2;
    			xp = x+stepX/2;
    			EuphoriaCell c1 = p.activeRecruits;
    			EuphoriaCell c2 = p.hiddenRecruits;
    			GC.setFont(gc,largeBoldFont());
    			GC.Text(gc, true,x, y+stepY*2,stepX*4 ,stepY/4, Color.yellow,null,c1.label);
    			GC.Text(gc, true,x+stepX*4,y+stepY*2, stepX*4,stepY/4, Color.yellow,null,c2.label);
    			int actY = y+stepY*3;
    			drawStackOnRecruit(pl,gc,true,gb,gb.canHitRecruit(c1,p,sources,dests)?highlight:null,c1,siz+siz/2,x+stepX*2,actY,c1.label);
       		
    			drawStackOnRecruit(pl,gc,true,gb,gb.canHitRecruit(c2,p,sources,dests)?highlight:null,c2,siz+siz/2,x+stepX*6,actY,c2.label);
    			EuphoriaChip ep = gb.movingObject(p);
    			if(ep instanceof RecruitChip && (c2.topChip()==null))
    			{
    				RecruitChip recruit = (RecruitChip)ep;
    				if(recruit.allegiance == Allegiance.Factionless)
    				{
    					StockArt.SmallX.drawChip(gc,this,siz,x+stepX*6,actY,null);
    					anySelect.setHelpText(FactionlessWarning);
    				}
    			}
       			if((state!=EuphoriaState.ActivateOneRecruit) && (c1.topChip()!=null)&&(c2.topChip()!=null))
       			{	int doneX = x+w/2-stepX;
       				int doneY = actY - stepX/2; 
       				Rectangle done = new Rectangle(doneX,doneY,stepX*2,stepX);
       				if(handleDoneButton(gc,done,highlight,HighlightColor,rackBackGroundColor))
       				{	boolean ephemeralRecruits = bb.ephemeralRecruitMode();
       					highlight.hitCode = ephemeralRecruits 
								? EuphoriaId.EConfirmOneRecruit
								: simultaneousTurnsAllowed() ? EuphoriaId.EConfirmRecruits : EuphoriaId.ConfirmRecruits;
       				}
       			}
      		}
    	   	    	   	
     		}
    		
    		break;
    
    	}
    	if((gc!=null) 
    			&& (highlight!=null) 
    			&& (highlight.hitCode!=DefaultId.HitNoWhere)
    			)
    	{
    		EuphoriaCell c = hitCell(highlight);
    		if(c!=null)
    		{
    		switch(c.rackLocation())
    		{
    		case PlayerHiddenRecruits:
    		case PlayerActiveRecruits:
    		case PlayerNewRecruits0:
    		case PlayerNewRecruits1:
    		case PlayerNewRecruits2:
    		case PlayerNewRecruits3:
    			EuphoriaChip top = c.topChip();
    			if(top!=null)
    			{
    			top.drawChip(gc,this,2*w/3,x+w/2,y+h/2,null);
    			}
    			break;
			default:
				break;
    		}}
    	}
    	if(!fromHidden && rotation!=0)
    	{
     	GC.setRotation(gc, -rotation, cx, cy);
    	HitPoint.setRotation(highlight,-rotation, cx, cy);
    	}
    	
    	// draw the close box
		{
		 Rectangle br = new Rectangle(x+w-smallBox-margin,y+margin,smallBox,smallBox);
		 GC.fillRect(gc,rackBackGroundColor,br);
		 StockArt.FancyCloseBox.drawChip(gc,this,br,anySelect,EuphoriaId.CloseBox);
		}
		

    }
     
    //
    // interpolate a zoom specification at some position from 0-1.0
    // set activeZoom is inside the bounds.  This has the side effect
    // of turning off image caching so the zoom is smoother.
    //
    private double []interpolateZoom(double step,double[]start)
    {	if((step==0.0) || (step>=1)) {  return(start); }
    	double val[] = new double[4];
    	val[0] = G.interpolateD(step,0,start[0]);
    	val[1] = G.interpolateD(step,0,start[1]);
    	val[2] = G.interpolateD(1.0-step,start[2],100);
    	val[3] = G.interpolateD(1.0-step,start[3],100);
    	repaint(1);
    	//G.startLog("zoom step");
    	return(val);
    }
    
    private long startZoom = 0;			// the starting time (in milliseconds) of the current zoom
    private boolean payingCards = false;
    private boolean autoCardMode = true;
	private double cardArea[] = { 25,15,47,33 };
	private double artifactsAndCommodities[] = { 25,10,84,30 };//
	private double commodityArea[] = { 57,10,85,20};
	private double commodityAndResourceArea[] = {41,10,84,22};//
	private double resourceOnlyArea[] = { 42,14,62,28};
	private double marketBasketZone[] = {55,50,70,60};
	private double resourceArea[] = {31,10,59,30 };
	private double resourceArea_IIB[] = {26,10,59,30 };



    // return a zoom zone based on the current state of the board, or null to use the whole board
    // the zoom is interpolated toward the target over a short time interval
    private double []zoomZone(EuphoriaBoard gb)
    {   double zoomTime = 1000;	// 1000 millseconds = 1 seconds
    	long now = G.Date();
    	boolean costIncludesArtifacts=false;
    	double zoom[] = null;
    	if(startZoom==0) { startZoom = now;}
    	double zoomStep = Math.min(1.0,(now-startZoom)/zoomTime);
      	switch(gb.getGuiState())
    	{
      	case FightTheOpressor:
      	case JoinTheEstablishment:
      		zoom = cardArea;
      		costIncludesArtifacts = true;
      		break;
      	case DiscardResources:
      		zoom = artifactsAndCommodities;
      		break;
    	case CollectBenefit:
    	case CollectOptionalBenefit:
    	case ConfirmBenefit:
    		Benefit bene = gb.pendingBenefit();
    		if(bene!=null) // if it is null, something is terribly wrong in the board engine
    		{
    		switch(bene)
    		{
    		case FreeArtifactOrResource:
    			zoom = new double[] {26,10,58,30};
    			break;
     		case ArtifactOrWaterX2:
       		case ArtifactOrBlissX2:
       		case ArtifactOrEnergyX2:
       		case ArtifactOrFoodX2:
       		case ArtifactOrEnergyX3:
       		case ArtifactOrFoodX3:
       		case ArtifactOrBlissX3:
       		case ArtifactOrWaterX3:
       		case ArtifactOrGoldOrEnergyX2:
       		case ArtifactOrGoldOrEnergyX3:
      		case ArtifactOrClayOrFoodX2:
       		case ArtifactOrClayOrFoodX3:
      		case ArtifactOrStoneOrWaterX2:
       		case ArtifactOrStoneOrWaterX3:
       			zoom = artifactsAndCommodities;
       			break;
    		case EuphorianAuthority2:
    		case WastelanderAuthority2:
    		case SubterranAuthority2:
    			zoom = gb.generalMarketZones[bene.placementZone().ordinal()];
    			break;
    		case CardOrGold:
    		case CardOrStone:
    		case CardOrClay:  
    		case CardAndStone:
    		case CardAndGold:
    		case CardAndClay:
    		case IcariteInfluenceAndResourcex2:
      		   	zoom = isIIB() ? resourceArea_IIB : resourceArea;
      		   	break;
       		case Resource:
       			return resourceOnlyArea; 
    		case Commodity:
    		case Commodityx2:
    		case Commodityx3:
    		case WaterOrEnergy:
    		case KnowledgeOrFood:
    		case MoraleOrEnergy:
    		case KnowledgeOrBliss:
    		case WaterOrMorale:
    			zoom = commodityArea;
    			break;
    		default: break;
    		case Artifactx2for1:
    		case MoraleOrKnowledge:
    		case Moralex2OrKnowledgex2:
    				zoom = marketBasketZone;
    				break;
    		case WaterOrStone:
    		case ClayOrFood:
    		case StoneOrWater:
    		case GoldOrEnergy:
    		case ResourceOrCommodity:
    		case ResourceAndCommodity:
      			 zoom = commodityAndResourceArea;
      			 break;
       		case IcariteInfluenceAndCardx2:	// in IIB, taking a card requires an interaction
       		case Artifact:
       		case FirstArtifact:
       		case FreeArtifact:
       			zoom = cardArea;
    		}}
    		break;
    	case PayForOptionalEffect:
    	case ConfirmPayForOptionalEffect:
    		zoom = commodityArea;
    		break;
    	case PayCost:
    	case ConfirmUseJackoOrContinue:
    	case ConfirmUseMwicheOrContinue:
    	case ConfirmUseJacko:
    	case ConfirmPayCost:
    		Cost pend = gb.pendingCost();
    		if(pend==null) { bb.p1("Pending cost shouldn't be null"); }
    		else
    		switch(pend)
    		{
    		case Artifactx3OrArtifactAndBlissx2:
    		case Artifactx3OrArtifactAndBlissx2AndCommodity:
    		case BlissOrFoodx4_Card:
    		case Commodity_Artifact:
    		case Commodity_Book:
    		case Commodity_Bifocals:
    		case Commodity_Balloons:
    		case Commodity_Box:
    		case Commodity_Bat:
    		case Commodity_Bear:
    			zoom = artifactsAndCommodities;
    			break;
    		// dilemma costs
    		case BatOrCardx2:
    		case BearOrCardx2:
     		case BoxOrCardx2:
    		case BalloonsOrCardx2:
     		case BifocalsOrCardx2:
      		case BookOrCardx2:
       		case Card:
       		case Artifact:
       		case CardForGeek:
       		case CardForGeekx2:
       		case Cardx2:
       		case Cardx3:
       		case Cardx4:
       		case Cardx5:
       		case Cardx6:
       		case ArtifactJackoTheArchivist_V2:
       		case Artifactx3:
       		case Artifactx2:
       		case Morale_Artifactx3_Brian:
       		case Book_Card:
       		case Box:
       		case Book:
       		case Bifocals:
       		case Bat:
       		case Balloons:
       		case Bear:
     			zoom = cardArea;
          		costIncludesArtifacts = true;

     			break;
    		case Bliss_Commodity:	// breeze bar and sky lounge
    			zoom = commodityArea;
 
    			break;
    		default: 
    			zoom = commodityAndResourceArea;
    			break;
    		}
    		break;
    	default: break;
    	}
      	if(zoom!=null)
      	{	payingCards = costIncludesArtifacts;
      		return(interpolateZoom(zoomStep,zoom));
      	}
      	else
      	{
      	startZoom = 0;
      	payingCards = false;
      	autoCardMode = true;
      	return(null);
      	}
    }
    
    public boolean zoomIsActive() { return(startZoom>0); }

    int gameLogScroll = 0;
    EuphoriaMovespec logState[] = new EuphoriaMovespec[2];
    
    public String[] combineEvents(String first[],String second[])
    {
    	if(first==null) { return(second); }
    	if(second==null) { return(first); }
    	int lfirst = first.length;
    	int lsecond = second.length;
    	String res[] = new String[lfirst+lsecond];
    	for(int i=0;i<lfirst;i++) { res[i]=first[i]; }
    	for(int j=0;j<lsecond; j++) { res[lfirst+j] = second[j]; }
    	return(res);
    }

    
    double chipScales[] = {1.2,1.4,0.-0.1,-0.1};
    double stoneScales[] = {1.0,1.4,0.5,-0.25};
    double cardScales[] = {1.0,0.55,0,-0.35};
    double[] escale = {1.0,1.0,0,-0.5};
       Text gameEventText[] = {
    		
       		TextGlyph.create("Euphorian","xx",EuphoriaChip.Euphorian,this,escale),
       		TextGlyph.create("Wastelander","xx",EuphoriaChip.Wastelander,this,escale),
       		TextGlyph.create("Subterran","xx",EuphoriaChip.Subterran,this,escale),
       		TextGlyph.create("Icarite","xx",EuphoriaChip.Icarite,this,escale),
       		
       		TextGlyph.create("Water","xx",EuphoriaChip.Water,this,chipScales),
       		TextGlyph.create("Miner","xx",EuphoriaChip.Miner,this,chipScales),
       		TextGlyph.create("Energy","xx",EuphoriaChip.Energy,this,chipScales),
       		TextGlyph.create("Food","xx",EuphoriaChip.Food,this,chipScales),
       		TextGlyph.create("Bliss","xx",EuphoriaChip.Bliss,this,new double[] {1.1,1.4,0.1,-0.2}),
       		
       		TextGlyph.create("Gold","xx",EuphoriaChip.Gold,this,chipScales),
       		TextGlyph.create("Stone","xx",EuphoriaChip.Stone,this,stoneScales),
       		TextGlyph.create("Clay","xx",EuphoriaChip.Clay,this,chipScales),

       		TextGlyph.create("Artifact","xx",ArtifactChip.CardBack,this,cardScales),
    		TextGlyph.create("Commodity","xx",EuphoriaChip.Commodity,this,new double[]{1.0,1.0,0,-0.5}),
    		TextGlyph.create("Resource","xx",EuphoriaChip.Resource,this,new double[]{1.0,1.0,0,-0.5}),
    		TextGlyph.create("Commodities","xx",EuphoriaChip.Commodity,this,new double[]{1.0,1.0,0,-0.5}),
    		TextGlyph.create("Resources","xx",EuphoriaChip.Resource,this,new double[]{1.0,1.0,0,-0.5}),
    		
    		TextGlyph.create("RedStar","xx",EuphoriaChip.AuthorityMarkers[Colors.Red.ordinal()],this,chipScales),
       		TextGlyph.create("GreenStar","xx",EuphoriaChip.AuthorityMarkers[Colors.Green.ordinal()],this,chipScales),
       		TextGlyph.create("BlueStar","xx",EuphoriaChip.AuthorityMarkers[Colors.Blue.ordinal()],this,chipScales),
       		TextGlyph.create("PurpleStar","xx",EuphoriaChip.AuthorityMarkers[Colors.Purple.ordinal()],this,chipScales),
       		TextGlyph.create("BlackStar","xx",EuphoriaChip.AuthorityMarkers[Colors.Black.ordinal()],this,chipScales),
       		TextGlyph.create("WhiteStar","xx",EuphoriaChip.AuthorityMarkers[Colors.White.ordinal()],this,chipScales),
       	    		
    		TextGlyph.create("RedKnowledge","xx",EuphoriaChip.KnowledgeMarkers[Colors.Red.ordinal()],this,chipScales),
    		TextGlyph.create("RedMorale","xx",EuphoriaChip.MoraleMarkers[Colors.Red.ordinal()],this,chipScales),
    		
    		TextGlyph.create("GreenKnowledge","xx",EuphoriaChip.KnowledgeMarkers[Colors.Green.ordinal()],this,chipScales),
    		TextGlyph.create("GreenMorale","xx",EuphoriaChip.MoraleMarkers[Colors.Green.ordinal()],this,chipScales),
    		
    		TextGlyph.create("BlueKnowledge","xx",EuphoriaChip.KnowledgeMarkers[Colors.Blue.ordinal()],this,chipScales),
    		TextGlyph.create("BlueMorale","xx",EuphoriaChip.MoraleMarkers[Colors.Blue.ordinal()],this,chipScales),
    		
    		TextGlyph.create("PurpleKnowledge","xx",EuphoriaChip.KnowledgeMarkers[Colors.Purple.ordinal()],this,chipScales),
    		TextGlyph.create("PurpleMorale","xx",EuphoriaChip.MoraleMarkers[Colors.Purple.ordinal()],this,chipScales),
    		
    		TextGlyph.create("BlackKnowledge","xx",EuphoriaChip.KnowledgeMarkers[Colors.Black.ordinal()],this,chipScales),
    		TextGlyph.create("BlackMorale","xx",EuphoriaChip.MoraleMarkers[Colors.Black.ordinal()],this,chipScales),
    		
    		TextGlyph.create("WhiteKnowledge","xx",EuphoriaChip.KnowledgeMarkers[Colors.White.ordinal()],this,chipScales),
    		TextGlyph.create("WhiteMorale","xx",EuphoriaChip.MoraleMarkers[Colors.White.ordinal()],this,chipScales),
  };
    Text gameMoveText[] =   
    {	TextGlyph.create("Generator","xx",EuphoriaChip.Energy,this,escale),
    	TextGlyph.create("Aquifer","xx",EuphoriaChip.Water,this,new double[]{1.0,1.2,-0.3,-0.40}), //{1.0,1.0,0,-0.5};
    	TextGlyph.create("Farm","xx",EuphoriaChip.Food,this,escale),
    	TextGlyph.create("Cloud Mine","xx",EuphoriaChip.Bliss,this,escale),
   		TextGlyph.create("Euphorian","xx",EuphoriaChip.Euphorian,this,escale),
   		TextGlyph.create("Wastelander","xx",EuphoriaChip.Wastelander,this,escale),
   		TextGlyph.create("Subterran","xx",EuphoriaChip.Subterran,this,escale),
   		TextGlyph.create("Icarite","xx",EuphoriaChip.Icarite,this,escale),
    };
    public Text censoredMoveText(SequenceElement m,int idx,Font f)
    {
    	Text str = ((EuphoriaMovespec)m).censoredMoveText(this,logState,bb,f);
    	str.colorize(s,gameMoveText);
    	return(str);
    }
    public Text colorize(String str)
    {
    	return TextChunk.colorize(str,null,gameEventText);
    }

    String getStateDescription(EuphoriaState state,EuphoriaBoard gb)
    {	String val = state.description(); 
    	switch(state)
    	{
    	case PlaceAnother:
    		{	EPlayer p = gb.getCurrentPlayer();
    			if((gb.revision>=124) && !p.testTFlag(TFlag.HasLostMorale))
    			{
    				return s.get(PlaceAnotherStateIIB,p.color.name());
    			}
    			break;
    		}
    	case PayCost:
    	case PayForOptionalEffect:
    		// depends on the item being paid
    		{
    		Cost cost = gb.pendingCost();
    		if(cost!=null)
    		{switch(cost)
    			{
    			case WaterOrKnowledge:
    			case EnergyOrKnowledge:
    			case FoodOrKnowledge:	return(s.get(cost.description));
    			default:
    				String msg = cost.description;
        			switch(cost)
        			{
        			case ArtifactJackoTheArchivist_V2:
        				if(gb.getDest()!=null) { msg = Cost.Artifactx3.description; }
        				return(s.get(val,s.get(msg)));
 
        			case BlissOrFoodRetrieval:
        				if((gb.getCurrentPlayer().totalWorkers==2) 
       						&& gb.recruitAppliesToCurrentPlayer(RecruitChip.YordyTheDemotivator))
        				{	val = YordyTheDemotivatorPayment;
        				}
						//$FALL-THROUGH$
					default:
        			return(s.get(val,s.get(msg)));
        			}}
    			}
    		}

    		break;
    	case CollectOptionalBenefit:
    		{
    			Benefit bene = gb.pendingBenefit();
    			String benefit = bene.description;
    			return(s.get(benefit));
    		}
     	case CollectBenefit:
    		{
    		Benefit bene = gb.pendingBenefit();
    		if(bene!=null)
    			{
    			return(s.get(val,s.get(bene.description)));
    			}
    		
    		}
    		break;
    	default: break;
    	
    	}
    	return(s.get(val));
    }
    private boolean useRecruitGui(EuphoriaState state,EuphoriaBoard gb)
    {
        boolean simultaneous = !isSpectator() && gb.simultaneousTurnsAllowed(state);
        int pl = selectedRecruitPlayer(gb);
        boolean ourMove = pl==gb.whoseTurn() || simultaneous;
 
    	return ((startedPlaying||reviewOnly) 
    			&& state.hasRecruitGui() 
    			&& (state==EuphoriaState.RecruitOption)
    			   || (state==EuphoriaState.ConfirmOneRecruit)
    			   || (state==EuphoriaState.ConfirmActivateRecruit)
    			   || (state==EuphoriaState.ActivateOneRecruit)
    			   || ((state!=EuphoriaState.Puzzle)
    				  && (pl>=0)
          			  && ourMove 
          			  &&  !gb.getPlayer(pl).hasReducedRecruits()
    				  ));
    }
    private boolean enableDone(EuphoriaBoard gb)
    {
    	return(gb.DoneState()
    			&&(!gb.ephemeralRecruitMode()||(selectedRecruitPlayer(gb)>=0)));
    }
    private void handleDone(Graphics gc,EuphoriaBoard gb,HitPoint select,Rectangle r)
    {
       if (GC.handleRoundButton(gc, r, 
        			enableDone(gb)
        				? select
        				: null, s.get(DoneAction),
                HighlightColor, rackBackGroundColor))
        {	// always display the done button, but only make it active in
        	// the appropriate states
        	
            select.hitCode = (gb.getGuiState()==EuphoriaState.EphemeralConfirmRecruits)
            							? gb.ephemeralRecruitMode() 
            									? EuphoriaId.EConfirmOneRecruit
            									: EuphoriaId.EConfirmRecruits
            							:GameId.HitDoneButton;
        }
    }
    
    boolean minimizeOverlay = false;
    
    /**
     * draw the main window and things on it.  
     * If gc!=null then actually draw, 
     * If selectPos is not null, then as you draw (or pretend to draw) notice if
     * you are drawing under the current position of the mouse, and if so if you could
     * click there to do something.  Care must be taken to consider if a click really
     * ought to be allowed, considering spectator status, use of the scroll controls,
     * if some board token is already actively moving, and if the game is active or over.
     * <p>
     * This dual purpose (draw, and notice mouse sensitive areas) tends to make the
     * code a little complicated, but it is the most reliable way to make sure the
     * mouse logic is in sync with the drawing logic.
     * <p>
    General GUI checklist
<p>
<li>vcr scroll section always tracks, scroll bar drags
<li>lift rect always works
<li>zoom rect always works
<li>drag board always works
<li>pieces can be picked or dragged
<li>moving pieces always track
<li>stray buttons are insensitive when dragging a piece
<li>stray buttons and pick/drop are inactive when not on turn
*/
    public void redrawBoard(Graphics gc, HitPoint selectPos)
    {  EuphoriaBoard gb = disB(gc);
       int nPlayers = gb.nPlayers();
       //drawFixedElements(gc);
       EuphoriaState state = gb.getGuiState();
       boolean moving = getMovingObject(selectPos)>=0;
    	if(gc!=null)
		{
			// note this gets called in the game loop as well as in the display loop
			// and is pretty expensive, so we shouldn't do it in the mouse-only case
			gb.SetDisplayRectangle(boardRect);
		}
       // 
       // if it is not our move, we can't click on the board or related supplies.
       // we accomplish this by suppressing the highlight pointer.
       //
       boolean simultaneous = !reviewMode() && !isSpectator() && gb.simultaneousTurnsAllowed(state);
       boolean ourMove = OurMove() || simultaneous;
       boolean recruitGui = useRecruitGui(gb.getState(),gb);
       boolean recruitOverlay = globalRecruits!=null;
       boolean bigChipOverlay = bigChip!=null; 
       boolean anyAuxGui = recruitGui|recruitOverlay|bigChipOverlay ;
       HitPoint ourTurnSelect = ourMove ? selectPos : null;
       
       numberMenu.clearSequenceNumbers();

       //
       // whoseMove refers to the actually state of the board, not who is allowed
       // to make mouse gestures to change the state.  The symptom if this is wrong
       // was that after a live game ends, stepping slowly back would show the wrong
       // player as "to move" in the prompts, and if the GUI cared the actual moves
       // display would become confused.
       //
       int whoseMove = (simultaneousTurnsAllowed())
				? getActivePlayer().boardIndex
				: gb.whoseTurn;
       //
       // even if we can normally select things, if we have already got a piece
       // moving, we don't want to hit some things, such as the vcr group
       //
       HitPoint buttonSelect = moving ? null : ourTurnSelect;
       // hit anytime nothing is being moved, even if not our turn or we are a spectator
       HitPoint nonDragSelect = (moving && !reviewMode()) ? null : selectPos;
       gameLog.playerIcons = true;
       gameLog.redrawGameLog2(gc, selectPos, logRect,
    		   Color.black,boardBackgroundColor,
    		   standardBoldFont(),standardBoldFont());

		// draw the hidden windows before the normal windows, so the animations
		// will use the normal windows coordinates
       drawHiddenWindows(gc, selectPos);

       double []currentZoom = zoomZone(gb);
       boolean drawnZoomed = false;
   	   if(currentZoom==null) { magnifier = true; }	// turn it back on for next time
   	   {
   		Hashtable<EuphoriaCell,EuphoriaMovespec>dests = moving ? gb.getDests(whoseMove) : null;
   		Hashtable<EuphoriaCell,EuphoriaMovespec>sources = moving ? null : gb.getSources(whoseMove);
   		double []actualZoom = (magnifier && ourMove && !animating)?currentZoom:null;
   		drawnZoomed = actualZoom!=null;
        drawZoomedBoardElements(gc, 
    		   gb, 
    		   boardRect, 
    		   anyAuxGui?null:ourTurnSelect,sources,dests,
    		   actualZoom,
    		   selectPos);
   	   }
       if(currentZoom!=null)
       {	EuphoriaChip ch = magnifier ? EuphoriaChip.UnMagnifier : EuphoriaChip.Magnifier;
       		EuphoriaId rack = magnifier ? EuphoriaId.UnMagnifier : EuphoriaId.Magnifier;
       		ch.drawChip(gc,this,magnifierRect,ourTurnSelect,rack,(String)null);
       }
	   int who = selectedRecruitPlayer(gb);
	   if(recruitGui)
       {    
    	   if(who>=0)
    	   {
    		
    		Hashtable<EuphoriaCell,EuphoriaMovespec>dests = moving ? gb.getDests(who) : null;
       		Hashtable<EuphoriaCell,EuphoriaMovespec>sources = moving ? null : gb.getSources(who);
       		Rectangle rect = playerRecruitRect;
 			  switch(state)
 			  {
 			  case DiscardFactionless:
 			  case EphemeralDiscardFactionless:
 			  case EphemeralChooseRecruits:
 			  case ChooseRecruits:
 			  case ConfirmRecruits:
 			  case ConfirmOneRecruit:
 			  case ConfirmActivateRecruit:
 			  case ActivateOneRecruit:
 			  case EphemeralConfirmDiscardFactionless:
 			  case EphemeralConfirmRecruits:
 			  case ConfirmDiscardFactionless:	
 			  case Resign:
 			  case Gameover:
 				  rect = fullPlayerRecruitRect;
 				  break;
 			  default: break;
 			  }
 			if(minimizeOverlay)
 			{
 			showClosedOverlay(gc,boardRect,selectPos);
 			}
 			else
 			{
       		drawRecruitElements(gc,gb,who,rect,ourMove ? selectPos : null,selectPos,sources,dests,false);
 			}
       	   }
       }
	   
	   if (recruitOverlay)
	   {
		   drawGlobalRecruits(gc,gb, selectPos);
	   }
       else if(!recruitGui)
       { showHiddenUI = false;	// make sure we will start hiding
         minimizeOverlay = false;
       }
	   if(bigChipOverlay)
	   {   commonPlayer p = getPlayerOrTemp(bigChipPlayer);
		   drawBigChip(gc,p.displayRotation,boardRect,selectPos);
	   }
      commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
      double messageRotation = pl.messageRotation();

       GC.setFont(gc,standardBoldFont());
       boolean face = plannedSeating();
       // draw the board control buttons 

		if (state != EuphoriaState.Puzzle)
        {	// if in any normal "playing" state, there should be a done button
			// we let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			if(!face) { handleDone(gc,gb,buttonSelect,doneRect); }
			
			handleEditButton(gc,messageRotation,editRect,buttonSelect, selectPos,HighlightColor, rackBackGroundColor);
 
        }

		{
		Hashtable<EuphoriaCell,EuphoriaMovespec>dests = moving ? gb.getDests(whoseMove) : gb.getSources(whoseMove);
        for(int i=0;i<nPlayers;i++)
        {  	EPlayer p = gb.getPlayer(i);
        	commonPlayer cp = getPlayerOrTemp(i);
        	boolean allowSelection = ((state==EuphoriaState.Puzzle)
        								||(state==EuphoriaState.ConfirmBump)
        								||(i==whoseMove));
        	// don't allow the player picture to be hit when expanding cards that will cover it.
        	// (or an auxiliary button)
        	boolean allowPlayerSelection = allowSelection 
        			&& !((p.view==EPlayer.PlayerView.Artifacts)
        				|| (p.view==EPlayer.PlayerView.AutoArtifacts)
        			    || (p.view==EPlayer.PlayerView.Dilemma)
        			    || gb.ephemeralRecruitMode()
        			    );
        	cp.setRotatedContext(gc, selectPos, false);
    		framePlayer(gc,p,cp.playerBox); 
    		cp.setRotatedContext(gc, selectPos, true);
    		// unrotate so the rotate won't be a duplicate, then rerotate
        	drawPlayerStuff(gc,cp,(state==EuphoriaState.Puzzle),allowPlayerSelection?nonDragSelect:null,HighlightColor,rackBackGroundColor);
    		cp.setRotatedContext(gc, selectPos, false);    		
    		
    		// draw in this order so the background, usual player stuff, and foreground are in that order
        	drawPlayerStuff(gc,gb,allowSelection ? ourTurnSelect : null,	cp.playerBox,i,dests,dests,selectPos,false);        	
        	cp.setRotatedContext(gc, selectPos, true);
        }}
 
       	// draw the avatars
        	String msg = state==EuphoriaState.Gameover?gameOverMessage(gb):getStateDescription(state,gb);
        	EPlayer cp = gb.getCurrentPlayer();
        	if(state==EuphoriaState.EphemeralChooseRecruits)
        		{
        		cp = gb.getPlayer(getActivePlayer().boardIndex);
        		if(!useRecruitGui(state,gb)) 
        			{msg = s.get(NormalStartState);
        			}
        		}
        	Text tmsg = colorize(msg);
            standardGameMessage(gc,
            		messageRotation,
            		Color.black,
            		tmsg,
          				state!=EuphoriaState.Puzzle && !gb.simultaneousTurnsAllowed(state),
          				whoseMove,
          				stateRect);
            EuphoriaChip.getAuthority(cp.color).drawChip(gc,this,iconRect,null);
            goalAndProgressMessage(gc,nonDragSelect,Color.white,s.get(EuphoriaVictoryCondition),progressRect, goalRect);
            //DrawRepRect(gc,gb.Digest(),repRect);	// Not needed for euphoria
        
        if(!drawnZoomed && !recruitGui && !recruitOverlay && !bigChipOverlay) 
        	{ numberMenu.drawSequenceNumbers(gc,CELLSIZE*2,labelFont,labelColor);        	
        	}
        // draw the vcr controls
        drawVcrGroup(nonDragSelect, gc);

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
    	 // record some state so the game log will look pretty
    	{   
    		bb.setActivePlayer(getActivePlayer().boardIndex);
            handleExecute(bb,mm,replay);
            numberMenu.recordSequenceNumber(bb.moveNumber()+(bb.doneLast?0:1));
        /**
         * animations are handled by a simple protocol between the board and viewer.
         * when stones are moved around on the board, it pushes the source and destination
         * cells onto the animationStck.  startBoardAnimations converts those points into
         * animation sprites.  drawBoardElements arranges for the destination stones, which
         * are already in place, to disappear until the animation finishes.  The actual drawing
         * is done by drawSprites at the end of redrawBoard
         */
        lastDropped = bb.lastDroppedObject;	// this is for the image adjustment logic
				if(replay.animate) 
					{ playSounds(mm); // animations after sounds, so animations
						// can add additional sounds.
					}
    	}
         startBoardAnimations(replay);
      return (true);
    }
     /**
      * This is a simple animation which moves everything at the same time, at a speed proportional to the distance
      * for euphoria, this is normally just one chip moving.  Note that the interface to drawStack arranges to make the
      * destination chip disappear until the animation is finished.
      * @param replay
      */
     void startBoardAnimations(replayMode replay)
     {	try {
        if(replay.animate)
     	{	int CELLSIZE = bb.CELLSIZE;
     		int dice_rolled = 0;
     		double full = G.distance(0,0,G.Width(boardRect),G.Height(boardRect));
        	while(bb.animationStack.size()>1)
     		{
     		EuphoriaCell dest = bb.animationStack.pop();
     		EuphoriaCell src = bb.animationStack.pop();
    		double dist = src.distanceTo(dest);
    		double endTime = masterAnimationSpeed*0.5*Math.sqrt(dist/full);
    		//
    		// in cases where multiple chips are flying, topChip() may not be the right thing.
    		//
    		EuphoriaChip chip = dest.topChip();
    		if(chip!=null)
    		{	if(chip.isArtifact())
    			{
    			EuphoriaId rack = dest.rackLocation();
    			if((rack==EuphoriaId.ArtifactDeck) || (dest.rackLocation().perPlayer)) 
    				{ chip = ArtifactChip.CardBack; }
        		playASoundClip(CARD_PLACE,200);
    			}
    		else if (chip.isResource())
    		{	playASoundClip(heavy_drop,200);
    		}
    		else if(chip.isCommodity())
    		{
    			playASoundClip(light_drop,200);
    		}
    		else if(chip.isAuthorityMarker())
    			{
    			playASoundClip(KACHING,200);
    			}
    		else if(chip.isWorker() && (dest.rackLocation==EuphoriaId.UnusedWorkers))
    		{	
    			playASoundClip(WONTGETFOOLED,1000);
    			endTime *= 5;
    		}
    		else if(chip.isWorker() && (src.rackLocation==EuphoriaId.PlayerNewWorker))
    		{
    			dice_rolled++;
    		}
    		}
     		startAnimation(src,dest,chip,CELLSIZE,0,endTime);
     		}
     	
        	if(dice_rolled>0)
        	{
        		playASoundClip(DIE_ROLL[Math.min(dice_rolled,DIE_ROLL.length)-1],500);
        	}
     	}}
     	finally {
        	bb.animationStack.clear();
     	}
     } 

 void playSounds(commonMove mm)
 {
	 EPlayer p = bb.getCurrentPlayer();
	 if(p.testTFlag(TFlag.SomeLostToAltruism) && !p.testTFlag(TFlag.PlayedLostSound))
	 {	// TODO: replace swish with a better sound
		 p.setTFlag(TFlag.PlayedLostSound);
		 playASoundClip(heavy_drop,300);
		 return;
	 }
	 
	 switch(mm.op)
	 {
	 case MOVE_DONE:
		 if(bb.openedAMarketLastTurn) 
		 	{ bb.openedAMarketLastTurn = false;
		 	  playASoundClip(DOOR_OPEN,200);
		 	}
		 else if(bb.shuffledADeckLastTurn) 
		 	{ bb.shuffledADeckLastTurn = false;
		 	  playASoundClip(CARD_SHUFFLE,200); }
		 else { playASoundClip(light_drop,100); }
		 break;
	 case MOVE_DROPB:
	 case MOVE_PICKB:
	 case MOVE_PICK:
	 case MOVE_DROP:
	 	{
		EuphoriaCell d = bb.getDest();
		EuphoriaChip top = d!=null ? d.topChip() : null;
		if((top!=null) && top.isAuthorityMarker())
			{
			playASoundClip(KACHING,200);
			}
			else
			{
				playASoundClip(light_drop,100);
			}
	 	}
		 break;
	 default: break;
	 }

	 
	 
 }
/**
 * parse a move specifier on behalf of the current player.  This is called by the 
 * "game" object when it receives a move from the other player.  Note that it may
 * be called while we are in review mode, so the current state of the board should
 * not be considered.
 */
    public commonMove ParseNewMove(String st, int player)
    {
        return (new EuphoriaMovespec(st, player));
    }
    
/**
 * prepare to add nmove to the history list, but also edit the history
 * to remove redundant elements, so that indecisiveness by the user doesn't
 * result in a messy game log.  
 * 
 * For all ordinary cases, this is now handled by the standard implementation
 * in commonCanvas, which uses the board's Digest() method to distinguish new
 * states and reversions to past states.
 * 
 * For reference, the commented out method below does the same thing for "Hex". 
 * You could resort to similar techniques to replace or augment what super.EditHistory
 * does, but your efforts would probably be better spent improving your Digest() method
 * so the commonCanvas method gives the desired result.
 * 
 * Note that it should always be correct to simply return nmove and accept the messy
 * game record.
 * 
 * This may require that move be merged with an existing history move
 * and discarded.  Return null if nothing should be added to the history
 * One should be very cautious about this, only to remove real pairs that
 * result in a null move.  It is vital that the operations performed on
 * the history are identical in effect to the manipulations of the board
 * state performed by "nmove".  This is checked by verifyGameRecord().
 * 
 * in commonEditHistory()
 * 
 */
      public commonMove EditHistory(commonMove nmove)
      {	
    	  boolean oknone = ((nmove.op==NORMALSTART) 
    			  				|| (nmove.op==EPHEMERAL_CONFIRM_ONE_RECRUIT)
    			  				|| (nmove.op==EPHEMERAL_CONFIRM_RECRUITS));
    	  if(nmove.op==MOVE_PEEK)
			{	commonMove prev =  History.top();
				if((prev.op==MOVE_PEEK) && (prev.player==nmove.player))
				{	// remove duplicate peeks
					return null;
				}
				return nmove;
			}
    	  else
       	  if(nmove.op==EPHEMERAL_DROP)
    	  {	// special problem if ephemeral_drop and ephemeral_pick get separated by an intervening
    		// ephemeral_choose from another player.
    		int idx = History.size()-1;
    		EuphoriaMovespec target = (EuphoriaMovespec)nmove;
    		while(idx>0)
    		{
    			EuphoriaMovespec m = (EuphoriaMovespec)History.elementAt(idx);
   				
    			if((m.op==EPHEMERAL_PICK) && (m.dest==target.source) && (m.player==target.player))
    			{
    				removeHistoryElement(idx);
    				nmove = null;
    				idx = -1;
    			}
    			else { idx--; }
    		}
          	return(null);	
    	  }

  
    	commonMove rval = EditHistory(nmove, oknone);    

    	return(rval);
      }

    
    /** 
     * this method is called from deep inside PerformAndTransmit, at the point
     * where the move has been executed and the history has been edited.  It's
     * purpose is to verify that the history accurately represents the current
     * state of the game, and that the fundamental game machinery is in a consistent
     * and reproducible state.  Basically, it works by creating a duplicate board
     * resetting it and feeding the duplicate the entire history, and then verifying 
     * that the duplicate is the same as the original board.  It's perfectly ok, during
     * debugging and development, to temporarily change this method into a no-op, but
     * be warned if you do this because it is throwing an error, there are other problems
     * that need to be fixed eventually.
     */
 // public void verifyGameRecord()
 // {	//DISABLE_VERIFY = true;
 // 	super.verifyGameRecord();
 // }
    
    private void doDrop(EuphoriaCell target,replayMode replay)
    {
 		EuphoriaId rack = target.rackLocation();
		EuphoriaState state = bb.getGuiState();
		boolean simultaneous = bb.simultaneousTurnsAllowed(state);
        if(bb.pickedObject!=null)
        {
 		PerformAndTransmit((rack.perPlayer
						? (simultaneous ? "EDrop " : "Drop ")+target.color+" "+rack.name() 
						: "Dropb "+rack.name()+" "+target.row),
						true,replay);
        }
    }
    
/**
 * the preferred mouse gesture style is to let the user "pick up" objects
 * by simply clicking on them, but we also allow him to click and drag. 
 * StartDragging is called when he has done this.
 * <p>
 * Note on debugging: If you get here mysteriously with hitOjbect and hitCode
 * set to default values, instead of the values you expect, you're probably
 * not setting the values when the gc is null.
 */
    public void StartDragging(HitPoint hp)
    {	//G.print("Start "+hp);
        if (hp.hitCode instanceof EuphoriaId) // not dragging anything yet, so maybe start
        {
       	EuphoriaId hitObject =  (EuphoriaId)hp.hitCode;
       	int mov = getMovingObject(hp);
        if(hitObject.isSpecialCommand) {}
        else if(mov<0)
        {  EuphoriaCell hitCell = bb.getCell(hitCell(hp));
           EuphoriaState state = bb.getGuiState();
           boolean simultaneous = simultaneousTurnsAllowed();
           boolean isDest = bb.isDest(hitCell);
           if(hitCell!=null && hitCell.topChip()!=null)
           {
           if(simultaneous)
           {	// ephemeral picks are not transmitted
        	   PerformAndTransmit("EPick "+hitCell.color+" "+hitObject.name()+" "+((hitCell.row>=0)?hitCell.row:hp.hit_index),false,replayMode.Live);
        	   HiddenGameWindow hidden = findHiddenWindow(hp);
        	   if(hidden==null) { useEphemeralPick = true; }
           }
           else if(hitObject.perPlayer)
        	{ 
        	  if( (bb.getGuiState()==EuphoriaState.PlaceAnother)
        			&& bb.loseMoraleLosesArtifact())
        			
        		{
        		  PerformAndTransmit("LoseMorale");
        		}
        	  	else
        	  	{
        	  		PerformAndTransmit("Pick "+hitCell.color+" "+hitObject.name()+" "+((hitCell.row>=0)?hitCell.row:hp.hit_index));
        	  	}}
        	else 
        	{	PerformAndTransmit("Pickb "+hitObject.name()+" "+ ((hitCell.row>=0)?hitCell.row:hp.hit_index));
        	}}
        // fling it back
        if(isDest)
        {	switch(state)
        	{
        	default: 
        		EuphoriaCell src = bb.getSource();
        		if(src!=null)
        		{	doDrop(src,replayMode.Single);
        			hp.hitCode = EuphoriaId.NoAction;
        			hp.dragging=true;
        		}
        		break;
        	case EphemeralChooseRecruits:
        	case ChooseRecruits:
        	case ConfirmRecruits: break;
        	}}
        }
        if (bb.movingObjectIndex() >= 0)
	        {	// if we got something started, inform the mouse handler
        		EuphoriaState state = bb.getGuiState();
        		hp.dragging = true;
        		switch(state)
        		{
        		case EphemeralChooseRecruits:
            	case ChooseRecruits:
            	case ConfirmRecruits: 
            		break;
        		default:
        		{
        	        boolean simultaneous = bb.simultaneousTurnsAllowed(state);
        	        Hashtable<EuphoriaCell,EuphoriaMovespec>dests =  bb.getDests(simultaneous 
        	        			? getActivePlayer().boardIndex : bb.whoseTurn);
		            if(dests.size()==1)
		             {	for(Enumeration<EuphoriaCell> en = dests.keys(); en.hasMoreElements(); )
		            	 { EuphoriaCell target = en.nextElement();
		             	   doDrop(target,replayMode.Single);
		             	   hp.hitCode = EuphoriaId.NoAction;
		            	 break;
		             	}
		             }
		         }}
	        } 
         //G.print("Start "+hp.dragging);
        }
     }
    private boolean activeReset = false;
    public void performReset()
    {	if(simultaneousTurnsAllowed()) 
    		{ 
        	activeReset = true;
        	if(reviewMode()) { scrollFarForward(); }
    		}
    	else 
    	{	super.performReset();
    	}
    }
    
    public boolean hasPeeked()
    {
    	return(bb.hasPeeked(getActivePlayer().boardIndex));
    }
    public void setHasPeeked(boolean v)
    {
    	bb.setHasPeeked(getActivePlayer().boardIndex,v);
    }

	/** 
	 * this is called on "mouse up".  We may have been just clicking
	 * on something, or we may have just finished a click-drag-release.
	 * We're guaranteed just one mouse up, no bounces.
 * <p>
 * Note on debugging: If you get here mysteriously with hitOjbect and hitCode
 * set to default values, instead of the values you expect, you're probably
 * not setting the values when the gc is null.
	 */
    public void StopDragging(HitPoint hp)
    {	//G.print("Stop "+hp);
        CellId id = hp.hitCode;
        activeReset = false;
        useEphemeralPick = false;
        if(!(id instanceof EuphoriaId)) 
        { 	missedOneClick = performStandardActions(hp,missedOneClick); 
        	if(missedOneClick) {  bigChip = null; }
        	if(activeReset)
        	{	
        		int player = selectedRecruitPlayer(hp,bb);
        		if(player>=0)
        		{
                EPlayer pl = bb.getPlayer(player);
                pl.hiddenView = PlayerView.Normal;
                EuphoriaCell src = pl.ephemeralPickedSource;
                if(src!=null) { 
                	PerformAndTransmit("eDrop "+src.color+" "+src.rackLocation().name(),false,replayMode.Live);
                }}
                return;
         	}
        	if(!missedOneClick) { bb.normalizePlayerViews(); }
        }
        else 
        {
        missedOneClick = false;
		EuphoriaId rack = (EuphoriaId)id;
		if(rack.isSpecialCommand)
			{switch(rack)
				{
				case CloseBox:	
					minimizeOverlay = !minimizeOverlay;
					break;
				case ShowChip:
					bigChip = (EuphoriaChip)hp.hitObject;
					bigChipPlayer = hp.hit_index;
					break;
				case ChooseRecruit:
					{	
					HiddenGameWindow hidden = findHiddenWindow(hp);
					if(hidden!=null) { hiddenChooseRecruit[hp.hit_index] = true; }
					else 
						{
						EPlayer p = bb.getPlayer(hp.hit_index);
						PerformAndTransmit("EChooseRecruits "+p.color.name());					
						}
					}
					break;
				case Magnifier: magnifier = true; break;
				case UnMagnifier: magnifier = false; break;
				case SelectDie1:
				case SelectDie2:
				case SelectDie3:
				case SelectDie4:
				case SelectDie5:
				case SelectDie6:
					if(bb.selectedDieRoll==hp.hitObject) 
						{ 
						  PerformAndTransmit("NoRoll");
						}
					else { 
						   PerformAndTransmit("Roll "+rack.name());
						}
					break;
				case ConfirmDiscard:
				case EConfirmDiscard:
				case EConfirmOneRecruit:
				case EConfirmRecruits:
				case ConfirmRecruits:
					{
					int pl = selectedRecruitPlayer(hp,bb);
					PerformAndTransmit(rack.name()+" "+ bb.getPlayer(reviewOnly ? bb.whoseTurn : pl).color);
					}
					break;
				case NoAction: break;
				case ShowPlayerRecruits:
					// toggle showing the recruits in the main UI.  This is needed
					// to initially conceal the recruit card choices in the offline
					// playing user interface.
					showHiddenUI = !showHiddenUI;
					break;
				case ShowPlayerView:
				case ShowPlayerPeek:
					{
					// show or hide the player's artifact cards, separately for the
					// on-screen view and the hidden screen view
				    EuphoriaCell hitObject = hitCell(hp);
					EPlayer p = bb.getPlayer(hitObject.color);
					HiddenGameWindow hidden = findHiddenWindow(hp);
					EPlayer.PlayerView pend = EPlayer.PlayerView.values()[hp.hit_index];
					if((hidden!=null)||(remoteViewer>=0)) { p.hiddenView = pend; }
					else { p.view = pend; }
					autoCardMode = false;
					bigChip = null;
					if((rack==EuphoriaId.ShowPlayerPeek)
							&& !reviewMode()
							&& !simultaneousTurnsAllowed()
							&& ourActiveMove()
							) 
						{
						// 2/10/2023 added additional conditions, so now peeks are only sent when on
						// the active player's turn.  Peeks are intended to disallow undo, as when you
						// peek at the cards you've been dealt. Anything you're allowed to do NOT on
						// your turn can't be revealing hidden information.
						PerformAndTransmit(new EuphoriaMovespec(MOVE_PEEK,getActivePlayer().boardIndex),true,replayMode.Live); 
						}
					}
					break;
				case RecruitOption:
					{
					RecruitChip chip = (hp.hitObject instanceof RecruitChip) ? (RecruitChip)hp.hitObject : null;
					G.Assert(chip!=null,"recruit chip missing");
					PerformAndTransmit(rack.name() +" \""+chip.name+"\"");
					EPlayer p = bb.getCurrentPlayer();
					p.view = EPlayer.PlayerView.Normal;
					autoCardMode = false;
					}
					break;
				case FightTheOpressor:
				case JoinTheEstablishment:
					{
					EPlayer p = bb.getCurrentPlayer();
					p.view = EPlayer.PlayerView.Normal;
					globalRecruits = null;
					bigChip = null;
					PerformAndTransmit(rack.name());
					}
					break;
				case RecruitFirstJuliaOption:
				case RecruitSecondJuliaOption:
					{
						PerformAndTransmit(rack.name() +" \""+bb.activeRecruit().name+"\"");
						EPlayer p = bb.getCurrentPlayer();
						p.view = EPlayer.PlayerView.Normal;
						autoCardMode = false;
					}
					break;
					
				default: throw G.Error("not expecting special command %s",rack);
				}
			}
			else if(getMovingObject(hp)>=0)
			{
		        boolean simultaneous = simultaneousTurnsAllowed();
		        EuphoriaCell hitObject = bb.getCell(hitCell(hp));
		        if(simultaneous)
		        {
		        int player = selectedRecruitPlayer(hp,bb);
		        EPlayer pl = bb.getPlayer(player);
		        EuphoriaCell src = pl.ephemeralPickedSource;
		        // unpick it
		        PerformAndTransmit("Edrop "+src.color+" "+src.rackLocation().name(),false,replayMode.Live);
		        // move it atomically, 
		        if(src!=hitObject)
		        	{PerformAndTransmit("EChoose "+src.color+" "+src.rackLocation
		        			    + " "+rack.name()); }			        
		        }
		        else
		        {
		        //G.print("Drop");
				PerformAndTransmit(rack.perPlayer
								? (simultaneous ? "EDrop " : "Drop ")+hitObject.color+" "+rack.name() 
								: "Dropb "+rack.name()+" "+hitObject.row);
		        }
			}}

    }


    public boolean gameHasEphemeralMoves() { return(true); }
    


    // draw the fixed elements, using the saved background if it is available
    // and believed to be valid.
    public void drawFixedElements(Graphics offGC)
    {	drawFixedElements(offGC, disB(offGC),boardRect);	
    }

    /**
     * this is a token or tokens that initialize the variation and
     * set immutable parameters such as the number of players
     * and the random key for the game.  It can be more than one
     * token, which ought to be parseable by {@link online.game.commonCanvas#performHistoryInitialization}
     * @return return what will be the init type for the game
     */
     public String gameType() 
    	{
    	   // in games which have a randomized start, this method would return
    	   // return(bb.gametype+" "+bb.randomKey); 
    	return(bb.gameType()); 
    	}	
     
    // this is the subgame "setup" within the master type.
    public String sgfGameType() { return(Euphoria_SGF); }	// this is the official SGF number assigned to the game

    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
     public void performHistoryInitialization(Tokenizer his)
    {   //the initialization sequence
    	String token = his.nextToken();
    	//
    	// in games which have a randomized start, this is the point where
    	// the randomization is inserted
        int rev = his.intToken();
        int np =  rev<100 ? rev : his.intToken();
        long rk = his.longToken();
    	bb.doInit(token,rk,np,rev);
        adjustPlayers(np);
    }


/** handle the run loop, and any special actions we need to take.
 * The mouse handling and canvas painting will be called automatically.
 * <p>
 * This is a good place to make notes about threads.  Threads in Java are
 * very dangerous and tend to lead to all kinds of undesirable and/or flakey
 * behavior.  The fundamental problem is that there are three or four sources
 * of events from different system-provided threads, and unless you are very
 * careful, these threads will all try to use and modify the same data
 * structures at the same time.   Java "synchronized" declarations are
 * hard to get right, resulting in synchronization locks, or lack of
 * synchronization where it is really needed.
 * <p>
 * This toolkit addresses this problem by adopting the "one thread" model,
 * and this is where it is.  Any other threads should do as little as possible,
 * mainly leave breadcrumbs that will be picked up by this thread.
 * <p>
 * In particular:
 * GUI events do not respond in the native thread.  Mouse movement and button
 * events are noted for later.  Requests to repaint the canvas are recorded but
 * not acted upon.
 * Network I/O events, merely queue the data for delivery later.
 *  */
    
    public void ViewerRun(int wait)
    {
        super.ViewerRun(wait);
        if(simultaneousTurnsAllowed())
        { 	startSynchronousPlay();
        }
        
    }
    /**
     * returns true if the game is over "right now", but also maintains 
     * the gameOverSeen instance variable and turns on the reviewer variable
     * for non-spectators.
     */
    //public boolean GameOver()
    //{	// the standard method calls b.GameOver() and maintains
    	// two variables.  
    	// "reviewer=true" means we were a player and the end of game has been reached.
    	// "gameOverSeen=true" means we have seen a game over state 
    //	return(super.GameOver());
    //}
    
    /** this is used by the stock parts of the canvas machinery to get 
     * access to the default board object.
     */
    public BoardProtocol getBoard()   {    return (bb);   }

    //** this is used by the game controller to supply entertainment strings to the lobby */
    // public String gameProgressString()
    // {	// this is what the standard method does
    // 	// return ((mutable_game_record ? Reviewing : ("" + viewMove)));
    // 	return(super.gameProgressString());
    // }


    public int PrettyScoreForPlayer(BoardProtocol gb, commonPlayer p)
    {	return(((EuphoriaBoard)gb).PrettyScoreForPlayer(p.boardIndex));
    }

    /** factory method to create a robot */
    public SimpleRobotProtocol newRobotPlayer() 
    {  return(new EuphoriaPlay());
    }
    private boolean collectingStats = false;
    
    public void doReplayCollection()
    {
    	collectingStats = true;
    	EuphoriaStats.allStats.clear();
    	try {
    		super.doReplayCollection();
    	}
    	finally 
    		{ collectingStats = false; 
    		  EuphoriaStats.saveStats();
    		}
    }
    public void ReplayGame(sgf_game ga)
    {
    	super.ReplayGame(ga);
    	if(collectingStats)
    	{
    		EuphoriaStats.collectStats(ga,bb,players);
    	}
    }

    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the contract is to recognize
     * the elements that we generated in sgf_save
     * 5/25/2023 no problems
     */
    public void ReplayMove(sgf_node no)
    {
        String comments = "";
        sgf_property prop = no.properties;

        while (prop != null)
        {
            String name = prop.getName();
            String value = (String) prop.getValue();
            if(name.equals(gamename_property))
            {	// special rule to fix one game that was played broken
            	bb.GAME_EU_Dumbot_Brius_2014_12_20_2045 = "EU-Dumbot-Brius-2014-12-20-2045".equals(value);
            }
            if (setup_property.equals(name))
            {	Tokenizer val = new Tokenizer(value);
            	// shoehorn the revision number in before the number of players
            	String variation = val.nextToken();
                int rev = val.intToken();
               	int np = rev<100 ? rev : val.intToken();
            	long rk = val.longToken();
                adjustPlayers(np);
                bb.doInit(variation,rk,np,rev);
              }
            else if (name.equals(comment_property))
            {
                comments += value;
            }
            else if (name.equals(game_property))
            {
                G.Assert(Variation.find(value)!=null,WrongInitError,value);
            }
           else if (parseVersionCommand(name,value,2)) {}
           else if (parsePlayerCommand(name,value)) {}
            else
            {	// handle standard game properties, and also publish any
            	// unexpected names in the chat area
            	replayStandardProps(name,value);
            }

            prop = prop.next;
        }

        if (!"".equals(comments))
        {
            setComment(comments);
        }
    }
    
    
    // create the appropriate set of hidden windows for the players when
    // the number of players changes.
    
 
    public void adjustPlayers(int np)
    {
        int HiddenViewWidth = 600;
        int HiddenViewHeight = 300;
    	super.adjustPlayers(np);
        if(RpcService.isRpcServer() || VNCService.isVNCServer() || G.debug())
        {
        createHiddenWindows(np,HiddenViewWidth,HiddenViewHeight);
        }
    }
    
    public String nameHiddenWindow()
    {	return(ServiceName);
    }


    public HiddenGameWindow makeHiddenWindow(String name,int index,int width,int height)
	   	{
	   		return new HiddenGameWindow(name,index,this,width,height);
	   	}
    
    private boolean hiddenChooseRecruit[] = new boolean[MAX_PLAYERS];
    
    public void drawHiddenWindow(Graphics gc,HitPoint hp,int index,Rectangle r)
    {	
    	EPlayer pl = bb.getPlayer(index);
    	if(pl!=null)
    	{
     	String name = prettyName(index);
     	int h = G.Height(r);
    	int w = G.Width(r);
    	int l = G.Left(r);
    	int t = G.Top(r);
    	int topPart = h/10;
    	int topSpace = topPart*2;
    	int margin = topPart/4;
    	int textW = w-topSpace*2;
    	int textX = l+topSpace;
    	bb.CELLSIZE = CELLSIZE = w/10;
    	Rectangle stateRect = new Rectangle(textX,t+topPart,textW,topPart);
    	Rectangle infoRect = new Rectangle(textX,t,textW,topPart);
    	Rectangle rackRect = new Rectangle(l+topPart,t+topSpace,w-topSpace,h-topSpace-margin);
    	Rectangle alertRect = new Rectangle(textX, t+topPart*2,textW/2,topPart);
     	boolean moving = pl.ephemeralPickedObject!=null;
     	EuphoriaState state = bb.getGuiState();
     	Font efont = FontManager.getFont(largeBoldFont(),topPart*2/3);
     	labelFont = efont;
   	   	GC.setFont(gc,efont);
    	if (remoteViewer<0)
    	{ GC.fillRect(gc,rackBackGroundColor,r);
    	}
    	
    	GC.Text(gc,true,infoRect,Color.black,null,s.get(ServiceName,name));
    	boolean recruitGui = bb.ephemeralRecruitMode() ? !pl.hasReducedRecruits() : state.hasRecruitGui();
     	if(recruitGui)
    		{
        	Hashtable<EuphoriaCell,EuphoriaMovespec>dests = moving ? bb.getDests(index) : null;
       		Hashtable<EuphoriaCell,EuphoriaMovespec>sources = moving ? null : bb.getSources(index);
    		drawRecruitElements(gc,bb,index,rackRect,hp,hp,sources,dests,true);
    		}
    		else
    		{
    		Hashtable<EuphoriaCell,EuphoriaMovespec>dests = moving ? bb.getDests(index) : null;
           	Hashtable<EuphoriaCell,EuphoriaMovespec>sources = moving ? null : bb.getSources(index);
           	drawPlayerStuff(gc,bb,index==bb.whoseTurn?hp:null,rackRect,index,sources,dests,hp,true);  		
    		}
    	
       	// draw the avatars
     	boolean doneSelect = false;
    	String msg = (state==EuphoriaState.Gameover)
    					?gameOverMessage(bb)
    					:getStateDescription(state,bb);
    	if((state==EuphoriaState.EphemeralConfirmRecruits)
    			&& pl.discardedRecruits.height()>0)
    	{
    		msg = s.get(NormalStartState);
    		doneSelect = true;
    	}
    	int who = bb.whoseTurn;
    	
    	GC.setFont(gc,efont);
    	Text tmsg = colorize(msg);
		boolean simul = bb.simultaneousTurnsAllowed(state);
		standardGameMessage(gc,0,Color.black,
        		tmsg,
      				state!=EuphoriaState.Puzzle && !simul,
      					who,
        				stateRect);
		if(!doneSelect && (who==index || simul))
    	{	switch(state)
    		{
    		default:
    		GC.setFont(gc, efont);
    		GC.Text(gc, false, alertRect,
    				Color.red,null,s.get(YourTurnMessage));
    		}
    	}
     	if(bigChip!=null)
     	{
     		drawBigChip(gc,0,r,hp);
     	}

    }}
    
    public CommonMoveStack censoredHistory(CommonMoveStack hist) 
    { 	
    	CommonMoveStack newHist = hist;
     	boolean some = false;
    	int sz = hist.size()-1;
    	while(sz>=0)
    	{
    		commonMove m = newHist.elementAt(sz);
    		if(m.op==MOVE_PEEK)
    		{	if(!some) 
    				{ some = true;
    				  // implements copy on write.  This ought to be unnecessary
    				  // since it's the very last thing we do, but we're being conservative here.
    				  newHist = new CommonMoveStack();
    				  newHist.copyFrom(hist);
    				}
    			newHist.remove(sz,true);
    		}
    		sz--;
    	}
    	if(some)
    	{	// renumber
    		for(int i=newHist.size()-1; i>=0; i--)
    		{
    			commonMove old = newHist.elementAt(i);
    			commonMove n = old.Copy(null);
    			n.setIndex(i);
    			newHist.setElementAt(n,i);
    		}
    	}
    	return(newHist);
    }
    public int getLastPlacement() {
		return (bb.moveNumber+(bb.doneLast?0:1));
	}
}
