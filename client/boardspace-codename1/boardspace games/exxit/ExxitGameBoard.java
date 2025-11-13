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
package exxit;

import online.game.*;

import java.util.*;

import lib.*;
import lib.Random;


/**
 * ExxitGameBoard knows all about the game of Exxit, which is played
 * on a hexagonal board which is constructed as you play
 * common.hexBoard, which knows about the coordinate system.  
 * 
 * Exxit was cloned from Hive, so the board representation is initially
 * very similar.  In particular, the edgeless board with a diameter of about
 * 26 is a shared feature.
 * 
 * This class doesn't do any graphics or know about anything graphical, 
 * but it does know about states of the game that should be reflected 
 * in the graphics.
 * 
 *  The principle interface with the game viewer is the "Execute" method
 *  which processes moves.  Note that this
 *  
 *  In general, the state of the game is represented by the contents of the board,
 *  whose turn it is, and an explicit state variable.  All the transitions specified
 *  by moves are mediated by the state.  In general, my philosophy is to be extremely
 *  restrictive about what to allow in each state, and have a lot of tripwires to
 *  catch unexpected transitions.   We expect to be fed only legal moves, but mistakes
 *  will be made and it's good to have the maximum opportunity to catch the unexpected.
 *  
 * Note that none of this class shows through to the game controller.  It's purely
 * a private entity used by the viewer and the robot.
 * 
 * @author ddyer
 *
 */

public class ExxitGameBoard extends infiniteHexBoard<ExxitCell> implements BoardProtocol,ExxitConstants
{ 	static final int REVISION = 101;		// 101 switches to an infinite board
	public int getMaxRevisionLevel() { return(REVISION); }
	static final int NUMPIECES = 8;
    static final int NUMTILES = 39;
    static final int MAX_STACK_HEIGHT = NUMPIECES*3+1;	// we stack all the pieces on one tile
    static final int NODIST = 0;
    static final int DISTRIBUTE_ONBOARD = 1;
    static final int DISTRIBUTE_OFFBOARD = 2;
    static final ExxitId[] chipPoolIndex = { ExxitId.White_Chip_Pool, ExxitId.Black_Chip_Pool };
    static final ExxitId[] tilePoolIndex = { ExxitId.White_Tile_Pool, ExxitId.Black_Tile_Pool };
  
	private ExxitState unresign;
	private ExxitState board_state;
	public CellStack animationStack = new CellStack();
	public ExxitState getState() {return(board_state); }
	public void setState(ExxitState st) 
	{ 	unresign = (st==ExxitState.RESIGN_STATE)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
     //
    // private variables
    //
	private ExxitCell whiteChips = null;
	private ExxitCell blackChips = null;
    public ExxitCell rack[]=new ExxitCell[2];						// pieces in reserve, one cell per player with a stack of chips
    public ExxitCell tiles = null;									// tiles in reserve, one cell with a stack of hexes
    private ExxitCell transportCell = null;							// spare cell used in distribution moves
    private ExxitCell undoCell = null;								// stack of pieces in the last move
    private ExxitPiece allPieces[] = new ExxitPiece[2*NUMPIECES];	// all the pieces all the chips regardless of color or location
    private ExxitPiece allTiles[] = new ExxitPiece[NUMTILES];		// all the tiles regargless of location
    private ExxitPiece everyThing[]=new ExxitPiece[2*NUMPIECES+NUMTILES];	// all chips and all tiles
    private int numberOfThings=0;
    private int blob_index[] = new int[2];							// how many blobs per player
    private int blob_size[][] = new int[2][NUMTILES];				// size of each blob per player
    private boolean blobs_valid = false;								// true when the a sweep is known to be unnecessary
    private int sweep_counter=0;									// incremented when a sweep of cells is needed
    private int tilesInGame = NUMTILES;								// number of tiles actually in the game, some may be discarded for short games
	private int tilesOnBoard = 0;									// count of tiles played
    private boolean prosetup = false;								// true if playing the pro game 
	private boolean pass_ends_game = false; // after one pass
	// these are used only for labeling
	private int prisonerCount = 0;					// number of prisoner groups that have been created
	private int droppedTileCount = 0;				// number of pregame tiles
	public int robotDepth = 0;
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public ExxitPiece pickedObject = null;		// the object in the air being moved
    public ExxitCell pickedSource = null;		// the source of the object
    public ExxitCell droppedDest = null;		// the place it was dropped
    // temporary list of destination cells allocate as a resource for speed
    private ExxitCell[][]tempDestResource = new ExxitCell[6][];
    private int tempDestIndex=-1;
    public synchronized ExxitCell []getTempDest() 
    	{ if(tempDestIndex>=0) { return(tempDestResource[tempDestIndex--]); }
    	  return(new ExxitCell[NUMTILES+6]);
    	}
    public synchronized void returnTempDest(ExxitCell[]d) { tempDestResource[++tempDestIndex]=d; }
    

    public final ExxitCell GetExxitCell(char c,int row) 
    { return(getCell(c,row)); 
    }
     
    // construct an array of all the places the moving tile or chip can be dropped
    private int legalDropDests(ExxitCell dests[])
    {	int n=0;
    	boolean droptile = (pickedObject!=null) && (pickedObject.typecode==TILE_TYPE);
    	boolean puzzle = (board_state==ExxitState.PUZZLE_STATE);
    	sweep_counter++;
   		for(int i=0;i<tilesInGame;i++)
		{	ExxitCell c = allTiles[i].location;
			if((c!=null)&&c.onBoard)
			{	if(droptile)
				{	G.Assert(c.height()>0,"cell isn't empty");
					// tiles can be dropped adjacent to existing tiles
					{ for(int dir=0;dir<6;dir++) 
						{ ExxitCell dc = c.exitTo(dir);
						  if((dc.sweep_counter!=sweep_counter) && (dc.height()==0)) 
						  	{ dc.sweep_counter=sweep_counter; 
						  	  dests[n++]=dc; 
						    }
						}
					}
				}
				else if(puzzle || c.isEmptyTile()) 
				{ // chips can be dropped on empty tiles
				  dests[n++] = c; 
				}
			}
		}
   		return(n);
    }
    //
    // note that this has to work correctly both when "picked" has been picked
    // up, and when it is still sitting on top of "source"
    // this has to be synchronized because it makes "temporary" unpick/pick contrary
    // to the usual customs of boardspace games.
    private synchronized int legalDests(ExxitCell source,ExxitPiece picked,ExxitCell dests[])
    {	
       	int nadded = 0;
       	switch(board_state)
    	{
     	case DISTRIBUTE_STATE:
    		if(source!=null) 
    			{	// source already picked
    			G.Assert(pickedObject!=null,"should be");
    			G.Assert(picked==pickedObject,"should be 2");
    			unPickObject();				// temporarily undo the pick
    			dests[nadded++] = source;
    			CommonMoveStack  all = GetListOfMoves();
    			for(int i=0;i<all.size();i++)
    			{	Exxitmovespec sp = (Exxitmovespec)all.elementAt(i);
    					if((sp.from_col == source.col) && (sp.from_row==source.row))
	    				{	
	    					int dir = sp.direction;
	    					int height = source.height()-1;
	    					ExxitCell tc = source.exitTo(dir);
	    					while((height>0)) 
	    					{	dests[nadded++]=tc;
	    						height--;
	       						if(tc.height()==0) break;
	       					 	tc = tc.exitTo(dir);
	    					}
	    				}
    				}
    			pickObject(source);
    			}
    			else 
    			{
    			CommonMoveStack  all = GetListOfMoves();
    			for(int i=0;i<all.size();i++)
    				{
    				Exxitmovespec sp = (Exxitmovespec)all.elementAt(i);
    				dests[nadded++] = GetExxitCell(sp.from_col,sp.from_row);
    				}
    			}
    			return(nadded);
     		// otherwise fall into the general drop logic
    		
      	case DROP_STATE:
    	case DROP_OR_EXCHANGE_STATE:
    	case EXCHANGE_STATE:
    	case DROPTILE_STATE:
    		if(source!=null) { unPickObject(); }
    		CommonMoveStack all =  new CommonMoveStack();
     	 	if(board_state==ExxitState.DROPTILE_STATE) { GetListOfDroptileMoves(all,whoseTurn); }
    	 	else { GetListOfDropMoves(all,whoseTurn); }
     		if(source==null) {  GetListOfExchangeMoves(all,whoseTurn); }

     		for(int i=0;i<all.size();i++)
    		{	Exxitmovespec sp = (Exxitmovespec)all.elementAt(i);
    			dests[nadded++] = GetExxitCell(sp.from_col,sp.from_row);
    		}
     		if(source!=null) { pickObject(source); }
     		return(nadded);
    	default:
    		throw G.Error("Not expecting state %s",board_state);
       	case PUZZLE_STATE:
    		return(legalDropDests(dests));
    	}
    }
	
	// factory method for cells on the board
	public ExxitCell newcell(char c,int r)
	{	return(new ExxitCell(c,r));
	}
	// make a new chip
	private void makepiece(int pl,int pidx,int typ,int seq,long dig)
	{	// create a piece and place it in the rack.  Create cells for the rack.
		ExxitPiece p = allPieces[pidx]=new ExxitPiece(typ*2+pl,typ,pl,seq,dig);
		everyThing[numberOfThings++] = p;
	}
	public ExxitPiece playerChip[] = new ExxitPiece[2];
	
	// make a new tile
	private void maketile(int player,int typ,int seq,long dig)
	{	ExxitPiece p = new ExxitPiece(typ*2+player,typ,player,seq,dig);
		if(tiles==null) 
			{ tiles = new ExxitCell('t',typ,NUMTILES);
			  tiles.onBoard=false;
			  tiles.rackLocation=ExxitId.White_Tile_Pool;  
			 }
		tiles.addPiece(p);
		allTiles[seq]=p;
		everyThing[numberOfThings++]=p;
		p.home_location=tiles;
	}

    public ExxitGameBoard(String init,int map[]) // default constructor
    {	Random r = new Random(124656);
    	setColorMap(map, 2);
        drawing_style = DrawingStyle.STYLE_CELL;//STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = EXXITGRIDSTYLE;
        revision = REVISION;
        transportCell = new ExxitCell('t',0); 
        transportCell.onBoard=false;
        undoCell = new ExxitCell('u',0);
        undoCell.onBoard=false;
        // create the set of pieces used in the game.
        int idx=0;
		whiteChips = new ExxitCell((char)('a'+0),CHIP_TYPE);
		blackChips = new ExxitCell((char)('a'+1),CHIP_TYPE);
		blackChips.onBoard = false;
		whiteChips.onBoard = false;
		whiteChips.rackLocation = ExxitId.White_Chip_Pool;
		blackChips.rackLocation = ExxitId.Black_Chip_Pool;

        for(int pl=FIRST_PLAYER_INDEX; pl<=SECOND_PLAYER_INDEX;pl++)
        { 
          for(int j=0;j<NUMPIECES;j++) { makepiece(pl,idx,CHIP_TYPE,idx,r.nextLong()); idx++;}	// 8 chips
          playerChip[map[pl]] = allPieces[idx-1];
        }
        // make the tiles second so they're on the end of the "everyThing" list
        for(int j=0;j<NUMTILES;j++) { maketile(j&1,TILE_TYPE,j,r.nextLong()); }
        doInit(init); // do the initialization 
    }


    // not a draw state for us, but an end of game state
    public void SetDrawState()
    	{ setState(ExxitState.DRAW_STATE); }
    

    // standared init for Hex.  Presumably there could be different
    // initializations for variation games.
    private void Init_Standard(String game)
    {	
    	boolean blitz = Exxit_BLITZ.equalsIgnoreCase(game);
    	boolean beginner = Exxit_Beginner.equalsIgnoreCase(game);
 		

    	int tilecount = blitz ? 29 : (beginner ? 19 : NUMTILES);
    	prosetup = Exxit_PRO.equalsIgnoreCase(game);
        gametype = game;
        setState(ExxitState.PUZZLE_STATE);
        reInitBoard(26,26); //this sets up a hexagonal board
        whoseTurn = FIRST_PLAYER_INDEX;
        numberOfThings = everyThing.length;
        
        while(tiles.height()>tilecount) 
        	{ ExxitPiece p = tiles.removeTop();
        	  int h = tiles.height();
           	  --numberOfThings;
           	  G.Assert(allTiles[h]==p,"same piece for allTiles");
        	  G.Assert(everyThing[numberOfThings]==p,"same piece");
        	  //allTiles[h]=null;
        	  //everyThing[numberOfThings]=null;
        	  tilesInGame=h;
        	}
  
        pickObject(tiles);
        pickedObject.setColor(SECOND_PLAYER_INDEX);
        dropObject(getCell(ExxitId.BoardLocation,'M',14));
        labelCell('M',14);
        acceptPlacement();
        
        pickObject(tiles);
        pickedObject.setColor(FIRST_PLAYER_INDEX);
        dropObject(getCell(ExxitId.BoardLocation,'N',14));
        labelCell('N',14);
        acceptPlacement();
 
        if(!prosetup)
        {          
        pickObject(tiles);
        pickedObject.setColor(FIRST_PLAYER_INDEX);
        dropObject(getCell(ExxitId.BoardLocation,'M',13));
        labelCell('M',13);
        acceptPlacement();
        
        pickObject(tiles);
        pickedObject.setColor(SECOND_PLAYER_INDEX);
        dropObject(getCell(ExxitId.BoardLocation,'N',13));
        labelCell('N',13);
        acceptPlacement(); 
        }
        
        if(Exxit_INIT.equalsIgnoreCase(game))
        {
        }
        else if(blitz || beginner)
        {	
        }
        else if(prosetup)
        { 
        }
        else { throw G.Error(WrongInitError,game); }
        droppedDest = null;
        pickedSource = null;
        pickedObject = null;
    }
    public void sameboard(BoardProtocol f) { sameboard((ExxitGameBoard)f); }

    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(ExxitGameBoard from_b)
    {
        super.sameboard(from_b); // hexboard compares the boards
        sameCells(rack,from_b.rack);
        G.Assert(ExxitCell.sameCell(pickedSource,from_b.pickedSource),"pickedSource matches");
        G.Assert(ExxitCell.sameCell(droppedDest,from_b.droppedDest), "droppedDest matches");
        G.Assert(pickedObject==from_b.pickedObject, "pickedObject matches");
        // here, check any other state of the board to see if
        G.Assert((pass_ends_game == from_b.pass_ends_game),"pass ends matches");
        G.Assert((tilesOnBoard == from_b.tilesOnBoard),"tilesOnBoard matches");
        G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");
    }

    /** this is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * state of the game.  Other site machinery looks for duplicate digests.
     * @return an integer representing the digested state of the game.
     */
    public long Digest()
    {	long v = 0;
        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
        Random r = new Random(64 * 1000); // init the random number generator
        long pos[] = new long[6];
        v = tilesOnBoard*r.nextLong();
        for(int i=0;i<6;i++) { pos[i]=r.nextLong(); }
		for(int i=0;i<numberOfThings; i++)
		{	long rv = 0;
			ExxitPiece p = everyThing[i];
			ExxitCell c = p.location;
			if((c!=null) && c.onBoard && (p==c.topPiece())) 
				{	
					rv = c.Digest(r);
					for(int j=0;j<c.nAdjacentCells();j++)
					{	ExxitCell ac = c.exitTo(j);
						if(ac!=null) { rv += (pos[j]^ac.Digest(r)); }
					}
					v += rv;
				}
		}
	   v ^= ExxitPiece.Digest(r,pickedObject);
	   v ^= Digest(r,pickedSource);
       v ^= (board_state.ordinal()*10+whoseTurn)*r.nextLong();
       return (v);
    }
    public String gameType()
    { 
      if(revision<101) { return gametype; }
      // the lower case is a secret clue that we're in 4 token mode
      // instead of 1 token      else
      return(G.concat(gametype.toLowerCase()," ",players_in_game," ",randomKey," ",revision));
    }
    // this visitor method implements copying the contents of a cell on the board
    public void copyFrom(ExxitCell c,ExxitCell from)
    {	c.copyFrom(from,allPieces,allTiles);
    }
    public ExxitGameBoard cloneBoard() 
	{ ExxitGameBoard dup = new ExxitGameBoard(gametype,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((ExxitGameBoard)b); }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(ExxitGameBoard from_b)
    {	super.copyFrom(from_b);
    	robotDepth = from_b.robotDepth;
        pass_ends_game = from_b.pass_ends_game;
        prisonerCount = from_b.prisonerCount;
        droppedTileCount = from_b.droppedTileCount;
        tilesOnBoard = from_b.tilesOnBoard;
        pickedObject = from_b.pickedObject;
        pickedSource = getCell(from_b.pickedSource);
        droppedDest = getCell(from_b.droppedDest);
        copyFrom(rack,from_b.rack);
		copyFrom(tiles,from_b.tiles);
        board_state = from_b.board_state;
        unresign = from_b.unresign;

        if(G.debug()) { sameboard(from_b); }
    }
    public void reInit(String d)
    {
    	revision = 0;
    	doInit(d);
    }
    public void doInitOld(String gtype)
    {
    	doInit(gtype,0,0,0);
    }
    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {	
   	   StringTokenizer tok = new StringTokenizer(gtype);
   	   String typ = tok.nextToken();
   	   int np = tok.hasMoreTokens() ? G.IntToken(tok) : players_in_game;
   	   long ran = tok.hasMoreTokens() ? G.IntToken(tok) : key;
   	   int rev = tok.hasMoreTokens() ? G.IntToken(tok) : revision;
   	   doInit(typ,np,ran,rev);
    }
    public void doInit(String typ,int np,long ran,int rev)
    {
    	adjustRevision(rev);
    	randomKey =ran;
    	G.Assert(np==2,"players can only be 2");
    	setIsTorus(rev<101);
    	
    	robotDepth = 0;
    	whiteChips.doInit(); 
    	blackChips.doInit();
       undoCell.doInit();
       transportCell.doInit();
       tiles.doInit();
       blob_index[0]=blob_index[1]=0;
       tilesInGame = NUMTILES;
       	animationStack.clear();
      	int map[] = getColorMap();
   		rack[map[0]] = whiteChips;
   		rack[map[1]] = blackChips;
   		whiteChips.reInit();
   		whiteChips.reInit();
   		for(int pl=FIRST_PLAYER_INDEX,idx=0; pl<=SECOND_PLAYER_INDEX;pl++)
   	        { 
   	          for(int j=0;j<NUMPIECES;j++) 
   	          	{ ExxitPiece p = allPieces[idx];
   	          	  ExxitCell c = rack[map[pl]];
   	          	  c.addPiece(p);
   	          	  p.home_location=c;		// note the home position for each piece
   	          	  idx++;
   	          	}
       }
       for(int i=0;i<tilesInGame;i++)
       {	ExxitPiece p = allTiles[i];
       		p.location=null;
       		tiles.addPiece(p);
       }
       prisonerCount = 0;
       droppedTileCount = 0;
       tilesOnBoard = 0;
       blobs_valid = false;
       droppedDest=null;
       pickedSource=null;
       pickedObject=null;
       pass_ends_game = false;
       moveNumber = 1;
       board_state=ExxitState.PUZZLE_STATE;

       Init_Standard(typ);
       
        // note that firstPlayer is NOT initialized here
    }


    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer(replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Move not complete, can't change the current player, state %s",board_state);
        case PUZZLE_STATE:
            break;
        case DROP_STATE:
        case GAMEOVER_STATE:
        case DROPTILE_STATE:
        	if(replay==replayMode.Live)
        		{// allow some early damaged games to proceed
        		throw G.Error("Move not complete, can't change the current player, state %s",board_state);
        		}
 	
			//$FALL-THROUGH$
        case CONFIRM_DISTRIBUTE_STATE:
        case CONFIRM_EXCHANGE_STATE:
        case CONFIRM_STATE:
        case PASS_STATE:
        case DRAW_STATE:
        case RESIGN_STATE:
            moveNumber++; //the move is complete in these states
            setWhoseTurn(nextPlayer[whoseTurn]);
            return;
        }
    }

    /** this is used to determine if the "Done" button in the UI is live
     *
     * @return true if the "done" button should be active
     */
    public boolean DoneState()
    {	
        switch (board_state)
        {
        case RESIGN_STATE:
        case CONFIRM_STATE:
        case CONFIRM_DISTRIBUTE_STATE:
        case CONFIRM_EXCHANGE_STATE:
        case PASS_STATE:
        case DRAW_STATE:
            return (true);

        default:
            return (false);
        }
    }
    public boolean DigestState()
    {	
    	return(DoneState());
    }
    public void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=winNext=false; }	// simultaneous win is a draw
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(ExxitState.GAMEOVER_STATE);
    }
    int playerIndex(ExxitPiece ch) { return(getColorMap()[ch.colorIndex]); }
    // count the number of cells whose top color matches "pl"
    // this is used as a factor in some evaluators
    int countTopCells(int pl)
    {  	int n = 0;
    	for(int i=0;i<tilesInGame;i++)
    	{	ExxitPiece p = allTiles[i];
    		if(p!=null)
    		{	ExxitCell c = p.location;
    			if((c!=null) && c.onBoard)
    			{	ExxitPiece p1 = c.topPiece();
    				if((p1.typecode==CHIP_TYPE)&&(playerIndex(p1)==pl)) { n++; }
    			}
    		}
    	}
    	return(n);
    }
    // sweep from a particular cell, counting the size of the blobs
    void sweep_from(int player,int blob,ExxitCell cell)
    {	cell.sweep_counter = sweep_counter;
    	// using +1 instead of ++ avoids a code generator bug on IOS
    	// as of 7/2017 issue https://github.com/codenameone/CodenameOne/issues/2168
    	blob_size[player][blob] = blob_size[player][blob]+1;
    	cell.blobNumber = blob; 
    	
     	for(int i=0;i<6;i++) 
    	{	ExxitCell c = cell.exitTo(i);
    		if((c!=null)	// there is a cell 
    			&&(c.sweep_counter!=sweep_counter)	// and we haven't counted it
     			&& (c.height()>0))					// and it's not empty
    		{	ExxitPiece p = c.pieceAtIndex(0);	// top piece
    			if((p.typecode==TILE_TYPE)		// is contains a tile
    				&&(playerIndex(p)==player))		// and it's our color
    			{	sweep_from(player,blob,c);
    			}
    		}
    	}
    }
    // sweep the board and count the blobs
    void sweep()
    {	sweep_counter++;
    	blob_index[FIRST_PLAYER_INDEX]=blob_index[SECOND_PLAYER_INDEX]=0;
    	for(int i=0;i<tilesInGame;i++)
    	{	ExxitPiece tile = allTiles[i];
    		ExxitCell loc = tile.location;
    		if((loc!=null) && loc.onBoard && (loc.sweep_counter!=sweep_counter))
    		{	int thisPlayer = playerIndex(tile);
    			int thisBlob = blob_index[thisPlayer]++;
    			blob_size[thisPlayer][thisBlob]=0;
    			sweep_from(thisPlayer,thisBlob,loc);
    		}
    	}
    	blobs_valid=true;
    }
    int scoreForPlayer(int pl,boolean sweep)
    {	if(!blobs_valid) { sweep(); }
    	int total=0;
    	int big = 0;
    	for(int i=0;i<blob_index[pl];i++)
    	{	int sz = blob_size[pl][i];
    		total += sz;
    		if(sz>big) { big=sz; }
    	}
    	return(total+big);
    }
    



    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public void acceptPlacement()
    {
        pickedObject = null;
        droppedDest = null;
        pickedSource = null;

     }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
        ExxitCell dr = droppedDest;
        if(dr!=null)
        	{
        	droppedDest = null;
        	switch(pickedObject.typecode)
        	{
        	case TILE_TYPE:	
        		if(dr.onBoard) { tilesOnBoard--; }
        		//System.out.println("tiles "+tilesOnBoard);
        		blobs_valid=false;
        		break;
        	default: break;
        	}
        	dr.removeTop(pickedObject);
        	}

    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	ExxitPiece po = pickedObject;
    	if(po!=null)
    	{
    	ExxitCell ps = pickedSource;
    	if((po.typecode==TILE_TYPE) && pickedSource.onBoard) { tilesOnBoard++; }
    	pickedObject = null;
    	ps.addPiece(po);
      	}
      	pickedSource=null;
     }
    // 
    // drop the floating object.
    //
    private void dropObject(ExxitCell dest)
    {
       G.Assert((pickedObject!=null)&&(droppedDest==null),"ready to drop");
       switch (dest.rackLocation())
        {
        default: throw G.Error("Not expecting dest %s",dest);
        case BoardLocation:
           	droppedDest = dest;
        	droppedDest.addPiece(pickedObject);
           	switch(pickedObject.typecode)
        	{
        	case TILE_TYPE:	
        		tilesOnBoard++; 
        		//System.out.println("tiles "+tilesOnBoard);
        		blobs_valid = false;
        		break;
        	default: break;
        	}

        	break;
        case Black_Tile_Pool:
        case White_Tile_Pool:
        case Black_Chip_Pool:		// back in the pool
        case White_Chip_Pool:		// back in the pool
        	droppedDest = dest;
        	droppedDest.addPiece(pickedObject);
            break;
        }
    }   
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(ExxitCell cell)
    {	return(droppedDest==cell);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    {	ExxitPiece ch = pickedObject;
    	if((ch!=null)&&(droppedDest==null))
    		{ return(ch.imageIndex(false));
    		}
        return (NothingMoving);
    }

    public String movingObjectName()
    {
    	if((pickedObject!=null)&&(droppedDest==null))
    		{ return(pickedObject.prettyName);
    		}
    	return (null);
    }
    public int movingObjectType()
    {	return((pickedObject!=null)?pickedObject.typecode:-1);
    }
    // This is a service routine for the displayer.  Return
    // a hash array of cells where the currently moving insect
    // could land.
    //
    public Hashtable<ExxitCell,ExxitCell> movingObjectDests()
    {	Hashtable<ExxitCell,ExxitCell> dd = new Hashtable<ExxitCell,ExxitCell>();
    	if(movingObjectIndex()>=0)
    	{ExxitCell tempDests[]=getTempDest();
    		if(pickedSource.onBoard)
    		{ 
    		  int dests = legalDests(pickedSource,pickedObject,tempDests);
    		  for(int i=0;i<dests;i++) { dd.put(tempDests[i],tempDests[i]); }
    		}
    		else
    		{ 
    	    int nn = legalDropDests(tempDests);
    	    for(int i=0;i<nn;i++) { dd.put(tempDests[i],tempDests[i]); }
    		}
    	    returnTempDest(tempDests);
    	}
    return(dd);
    }
    

    private final ExxitPiece pickObject(ExxitCell c)
    {  	G.Assert((pickedObject==null)&&(pickedSource==null),"not ready to pick");
    	G.Assert(getCell(c)==c,"not my cell");
    	pickedSource = c;
    	pickedObject = c.removeTop();
    	return(pickedObject);
    }

	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private ExxitCell getCell(ExxitId source, char col, int row)
    {	
    
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);
        case BoardLocation:
        	return(GetExxitCell(col,row));
        case White_Chip_Pool:
        	return(whiteChips);
        case Black_Chip_Pool:
        	return(blackChips);
        case Black_Tile_Pool:
        case White_Tile_Pool:
        	return(tiles);
        }
   }
    public ExxitCell getCell(ExxitCell c) { return((c==null)?null:getCell(c.rackLocation(),c.col,c.row)); }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private ExxitPiece pickObject(ExxitId source, char col, int row)
    {	
    
        switch (source)
        {
        default:
        	throw G.Error("Not expecting source %s", source);

        case BoardLocation:
         	{
        	ExxitCell c = GetExxitCell(col,row);
        	ExxitPiece pp = pickObject(c);
        	if(pp.typecode==TILE_TYPE) { tilesOnBoard--; }
         	break;
         	}
        case White_Chip_Pool:
        	{
        	ExxitCell c = whiteChips;
        	pickObject(c);
        	break;
        	}
        case Black_Chip_Pool:
        	{
        	ExxitCell c = blackChips;
        	pickObject(c);
         	break;
        	}
        case Black_Tile_Pool:
        case White_Tile_Pool:
        	pickObject(tiles);
        	pickedObject.setColor((source==ExxitId.White_Tile_Pool)?FIRST_PLAYER_INDEX:SECOND_PLAYER_INDEX);
        	break;
        }
        return(pickedObject);
   }
    //	
    //true if col,row is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(ExxitCell c)
    {
        return (pickedSource==c);
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting drop in state %s", board_state);
        case DROP_STATE:
        case DROPTILE_STATE:
        case DROP_OR_EXCHANGE_STATE:
        	setState(ExxitState.CONFIRM_STATE);
        	break;
        case PUZZLE_STATE:
        	acceptPlacement();
            break;
        }
    }
    private void setNextStateAfterDone(replayMode replay)
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting state %s",board_state);
    	case DRAW_STATE:
    		setWin();
    		break;
    	case GAMEOVER_STATE:
    		if(replay!=replayMode.Live) { break; }
			//$FALL-THROUGH$
    	case DROP_STATE:
    	case DROPTILE_STATE:
    		if(replay==replayMode.Live)
    		{// allow some early damaged games to proceed
    		throw G.Error("Move not complete, can't change the current player, state %s",board_state);
    		}

			//$FALL-THROUGH$
    	case CONFIRM_DISTRIBUTE_STATE:
    	case CONFIRM_EXCHANGE_STATE:
    		confirmExchange();
    		// fall through
			//$FALL-THROUGH$
		case CONFIRM_STATE:
     	case PASS_STATE:
    		setState(nextPlayState(whoseTurn));
    		break;
    	case PUZZLE_STATE:
    		break;
    	}

    }    
    private void setWin()
    {	int score1 = scoreForPlayer(whoseTurn,true);
    	int score2 = scoreForPlayer(nextPlayer[whoseTurn],false);
    	setGameOver(score1>score2,score2>score1);
    }
    private void doDone(replayMode replay)
    {	boolean gameo = false;
        acceptPlacement();
        if(board_state==ExxitState.CONFIRM_EXCHANGE_STATE)
        {	if(tiles.height()==0) 
        	{ 	setWin();
        		confirmExchange();
        		gameo = true;
        	}
        }
        if((board_state==ExxitState.PASS_STATE)||(board_state==ExxitState.DRAW_STATE))
        {	if(pass_ends_game) { setWin(); gameo = true; }
        	else { pass_ends_game=true; }
        }
        else 
        {
        pass_ends_game = false;
        }
        
        if (board_state==ExxitState.RESIGN_STATE)
        {	setGameOver(false,true);
        	confirmExchange();
        	gameo = true;
         }
        if(!gameo)
        {	setNextPlayer(replay);
        	setNextStateAfterDone(replay); 
        }
    }
  
    private ExxitState nextPlayState(int who)
    {	if(prosetup && (tilesOnBoard<6))
    	{ return(ExxitState.DROPTILE_STATE);
    	}
    	{int nDist = nDistributionMoves(who);
    	if(nDist==0)
    	{	int nExchange = nExchangeMoves(who);
    		int nDrop = nDropMoves(who);
    		if((nDrop>0)&&(nExchange>0)) { return(ExxitState.DROP_OR_EXCHANGE_STATE); }
    		if(nDrop>0) { return(ExxitState.DROP_STATE); }
    		if(nExchange>0) { return(ExxitState.EXCHANGE_STATE); }
    		return(ExxitState.PASS_STATE);
    	}
    	return(ExxitState.DISTRIBUTE_STATE);
    	}
    }
    
    // used to load and unload the special "transportCell" as a stack
    private void loadCell(ExxitCell from,ExxitCell to)
    {	G.Assert(from!=to,"different cells");
    	while (from.height()>0)
    	{ ExxitPiece p = from.topPiece();
    	  if((p==null) || (p.typecode==TILE_TYPE)) { return; }
    	  from.removeTop();
    	  to.addPiece(p);
    	} 
    }
    // do a distribution move from some center in some direction
    private void doDistribution(ExxitCell c,int dir,replayMode replay)
    {	ExxitCell nx = c.exitTo(dir);
    	G.Assert(transportCell.height()==0,"transport is free");
    	G.Assert(undoCell.height()==0,"undo cell is free");
    	loadCell(c,transportCell);
    	pickedSource = c;
    	while(transportCell.height()>0)
    	{	ExxitPiece p = transportCell.topPiece();
    		if(replay.animate) {
    		animationStack.push(pickedSource);
    		animationStack.push(nx);
    		}
    	   	if(nx.height()==0) // off the board
    			{ loadCell(transportCell,nx);
    			  undoCell.push(p);
    			}
    	    else  { transportCell.removeTop(); nx.addPiece(p); undoCell.push(p); nx = nx.exitTo(dir); }
    	}
    }
    
    // construct the label for printing the game record
    public String distributionLabel()
    {	String val = pickedSource.cellName + "x";
    	boolean targetSeen=false;
    	int hgt = undoCell.height()-1;
    	for(int i=0;i<=hgt;i++)
    	{	ExxitPiece p = undoCell.pieceAtIndex(i);
    		ExxitCell loc = p.location;
    		if(!targetSeen && (loc.height()>2)) 
    		{ // target cell of the distribution is the opposite colored stack
    		  // which will be the first thing taller than 2 now
    			val += loc.cellName; targetSeen=true; 
    		}
    		else if(i==hgt)
    		{	// last cell, maybe a drop
    			ExxitPiece p1 = loc.pieceAtIndex(0);
    			if(p1.typecode==CHIP_TYPE) { val += "=" + loc.cellName; }
    		}
    	}
    	return(val);
    }
    private void undoDistribution()
    {	
    	while(undoCell.height()>0)
    	{	ExxitPiece p = undoCell.pop();
    		ExxitCell location = p.location;
    		if((location.pieceAtIndex(0).typecode==CHIP_TYPE))
    				{	// a stack dropped off the board
    				loadCell(location,transportCell);
    				}
    		else
    		{	transportCell.addPiece(location.removeTop());
    		}
    	}
    	loadCell(transportCell,pickedSource);
    }
    // do undos a little differently for the robot, because we want to 
    // minimuze the baggage rather than keep it as simple as possible
    // the Execute process records the initial height, we retrace the
    // path and load the undo stack, the carry on with the usual undo
    private void robotUndoDistribution(ExxitCell cell,int dir,int idepth)
    {	int depth = idepth;
    	G.Assert(undoCell.height()==0,"Undo cell is available");
    	G.Assert(transportCell.height()==0,"Transport cell is available");
    	ExxitCell nx = cell;
    	while(depth > 0)
    	{	nx = nx.exitTo(dir);		// step to next cell
    		if(nx.pieceAtIndex(0).typecode==TILE_TYPE)
    		{	// this an onboard spot
    			depth--;
    			ExxitPiece tp = nx.topPiece();
    			G.Assert(tp.location!=transportCell,"not on transport");
    			undoCell.push(tp);
    		}
    		else	// an off board stack
    		{	G.Assert(nx.height()==depth,"got the rest");
    			ExxitPiece tp = nx.pieceAtIndex(0); 
    			G.Assert(tp.location!=transportCell,"not on transport");
    			undoCell.push(tp); 
    			depth=0;
    		}
    	}
    	pickedSource = cell;
    	undoDistribution();
    }
    private void doExchange(ExxitCell c,replayMode replay)
    {	// return the pieces to the reserve
    	createExitCells(c);
    	while(c.height()>0)
    	{	ExxitPiece p = c.removeTop();
    		undoCell.push(p);
    		switch(p.typecode)
    		{
    		default: throw G.Error("not expecting piece type %s",p.typecode);
    		case CHIP_TYPE:
    			ExxitCell dest = rack[playerIndex(p)];
    			dest.addPiece(p);
    			if(replay.animate)
    			{
    				animationStack.push(c);
    				animationStack.push(dest);
    			}
    		}
    	}
    	// add a new hex
    	ExxitPiece p = tiles.removeTop();
    	p.setColor(getColorMap()[whoseTurn]);
    	undoCell.push(p);
    	c.addPiece(p);
    	if(replay.animate) {
    		animationStack.push(tiles);
    		animationStack.push(c);
    	}
    	tilesOnBoard++;
    	blobs_valid = false;
    	for(int dir=0;dir<geometry.n;dir++)
    		{	ExxitCell cx =c.exitTo(dir);
    			if(cx!=null) 
    				{ createExitCells(cx);
    				if((tiles.height()>0) && cx.canExchange()) { doExchange(cx,replay); }
    				}
    		}
     }
    
    // generate a label for an exchange move
    public String exchangeLabel()
    {	String val = "";
    	ExxitCell ploc = null;
     	int hgt = undoCell.height()-1;
    	for(int i=0;i<=hgt;i++)
    	{	ExxitPiece p = undoCell.pieceAtIndex(i);
    		if(p.typecode==TILE_TYPE)
    		{
    			ExxitCell loc = p.location;
    			if(loc!=ploc)
    			{
    			 val += "+" + loc.cellName;
    			 ploc=loc;
    			}
    		}
     	}
    	return(val);
    }
    private void undoExchange()
    {	ExxitCell location = null;		// the cell we're restoring
    	while(undoCell.height()>0)
    	{	ExxitPiece p = undoCell.pop();
    		switch(p.typecode)
    		{	
    		case TILE_TYPE:	// this is a tile that was placed.  Put it back in the reserve
    			location = p.location;
    			location.removeTop();
    			tilesOnBoard--;
    			blobs_valid = false;
    			tiles.addPiece(p);
    			break;
    		case CHIP_TYPE:
    			// this is a chip that was restored to the reserve, put it back on the board
    			ExxitPiece newp = p.location.removeTop();
    			G.Assert(newp==p,"same top piece");
    			location.addPiece(p);
    			break;
			default:
				break;
    		}
    	}
    }

    private void confirmExchange()
    {	undoCell.doInit();
    }
    
    public boolean Execute(commonMove mm,replayMode replay)
    {	Exxitmovespec m = (Exxitmovespec)mm;
 
        //System.out.println("E "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {
        case MOVE_PASS:
        	setState(ExxitState.PASS_STATE);
        	break;
        case MOVE_DONE:
         	doDone(replay);
            break;
        case MOVE_EXCHANGE:
        	if(pickedObject!=null) { unPickObject(); }
        	G.Assert(undoCell.height()==0,"undo cell available");
        	int cells = tilesOnBoard;
        	doExchange(GetExxitCell(m.from_col,m.from_row),replay);
        	m.undoDistributionInfo=tilesOnBoard-cells;	// used by playSounds;
        	setState(ExxitState.CONFIRM_EXCHANGE_STATE);
        	break;
        case MOVE_MOVE:
        	if(pickedObject!=null) { unPickObject(); }
        	ExxitCell cell = GetExxitCell(m.from_col,m.from_row);
        	m.undoDistributionInfo = cell.height()-1;		// initial height for robot undo
        		// also used for playSounds
        	doDistribution(cell,m.direction,replay);
        	setState(ExxitState.CONFIRM_DISTRIBUTE_STATE);
        	break;
        	// fall into dropb
        case MOVE_DROPB:
            { ExxitCell c = GetExxitCell(m.from_col,m.from_row);
              if(isSource(c)) { unPickObject(); }
              else
              {
              switch(board_state)
              {
              case DISTRIBUTE_STATE:
              default: throw G.Error("Not expecting drop in state %s",board_state);
              case DROP_OR_EXCHANGE_STATE:
              case DROP_STATE:
              case DROPTILE_STATE:
             	  { 
             		  if(pickedObject==null) 
             	  		{ ExxitCell pool = getCell(m.object,m.from_col,m.from_row);
             	  		  pickObject(pool);
             	  		  if(replay.animate)
             	  		  {
             	  			  animationStack.push(pool);
             	  			  animationStack.push(c);
             	  		  }
             	  		}
            	   dropObject(c);
            	   setNextStateAfterDrop();
            	  }
            	  break;
           	  case PUZZLE_STATE:
           		  if(pickedObject==null) { pickObject(m.object,m.from_col,m.from_row); }
           		  dropObject(c);
           		  setNextStateAfterDrop();
           		  break;
            }
              createExitCells(c);
              }
         		

           
            }
            break;

        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	switch(board_state)
        	{
            default: throw G.Error("Not expecting pickb in state %s",board_state);
            case DISTRIBUTE_STATE:
            case DRAW_STATE:
            case CONFIRM_STATE:
            case DROPTILE_STATE:
        		if(isDest(GetExxitCell(m.from_col,m.from_row)))
        		{ unDropObject(); 
        		  ExxitCell c = pickedSource;
        		  if(c!=null) { unPickObject(); }
        		  setState(nextPlayState(whoseTurn));
        		  if(c!=null) { pickObject(c); }
        		}
        		else 
        		{ // pickup for a distribution move
        		  pickObject(ExxitId.BoardLocation,m.from_col,m.from_row);  
        		}
        		break;
        	case PUZZLE_STATE:
        		pickObject(ExxitId.BoardLocation, m.from_col, m.from_row);
        		break;
        	};
        	break;
        	
        case MOVE_DROP: // drop on chip pool;
            dropObject(getCell(m.source, m.from_col, m.from_row));
            setNextStateAfterDrop();

            break;

        case MOVE_PICK:
            unDropObject();
            unPickObject();
            pickObject(m.source, m.from_col, m.from_row);
 
            break;


        case MOVE_START:
            setWhoseTurn(m.player);
            unDropObject();
            unPickObject();
            undoCell.doInit();
            transportCell.doInit();
            setState(ExxitState.PUZZLE_STATE);
            setState(nextPlayState(whoseTurn)); 
            break;

        case MOVE_RESIGN:
            setState(unresign==null?ExxitState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
            setState(ExxitState.PUZZLE_STATE);
            undoCell.doInit();
            transportCell.doInit();
            break;
        case MOVE_GAMEOVERONTIME:
        	setGameOver(true,false);
        	break;
        default:
        	cantExecute(m);
        }

        //System.out.println("Ex "+m+" for "+whoseTurn+" "+board_state);
        //System.out.println("Digest "+Digest());
        return (true);
    }

    // service routine for the viewer
    public boolean LegalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);

        case CONFIRM_EXCHANGE_STATE:
        case DISTRIBUTE_STATE:
        case EXCHANGE_STATE:
        case PASS_STATE:
        case CONFIRM_STATE:
        case CONFIRM_DISTRIBUTE_STATE:
        case DRAW_STATE:
		case GAMEOVER_STATE:
		case DROPTILE_STATE:
		case RESIGN_STATE:
			return(false);
        case DROP_STATE:
        case DROP_OR_EXCHANGE_STATE:
        	return(player==whoseTurn);
        case PUZZLE_STATE:
        	return(true);
        }
    }
    // service routine for the viewer
    public boolean LegalToHitTiles()
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);
        case CONFIRM_EXCHANGE_STATE:
        case DROP_STATE:
        case EXCHANGE_STATE:
        case DROP_OR_EXCHANGE_STATE:
        case DISTRIBUTE_STATE:
		case GAMEOVER_STATE:
		case CONFIRM_DISTRIBUTE_STATE:
		case CONFIRM_STATE:
		case PASS_STATE:
		case DRAW_STATE:
		case RESIGN_STATE:
			return(false);
        case PUZZLE_STATE:
        	return((pickedObject==null)||(pickedObject.typecode==TILE_TYPE));
        case DROPTILE_STATE: 
            return (true);
        }
    }
    // service routine for the viewer
    public boolean LegalToHitBoard(ExxitCell cell)
    {
        switch (board_state)
        {
		case DISTRIBUTE_STATE:
		case EXCHANGE_STATE:
		case DROP_OR_EXCHANGE_STATE:
		case DROP_STATE:
		case DROPTILE_STATE:
			ExxitCell tempDests[]=getTempDest();
			boolean val = AR.arrayContains(tempDests,cell,legalDests(pickedSource,pickedObject,tempDests));
			returnTempDest(tempDests);
			return(val);

		case CONFIRM_EXCHANGE_STATE:
			for(int i=0;i<undoCell.height();i++)
			{	ExxitPiece p = undoCell.pieceAtIndex(i);
				if(p.typecode==TILE_TYPE)
				{	ExxitCell c = p.location;
					if(c==cell) { return(true); }
				}
			}
			return(false);
			
		case CONFIRM_DISTRIBUTE_STATE:
			if(cell==pickedSource) { return(true); }
			for(int i=0;i<undoCell.height();i++)
			{	ExxitPiece p = undoCell.pieceAtIndex(i);
				ExxitCell c = p.location;
				if(c==cell) { return(true); }
			}
			return(false);
		case DRAW_STATE:
		case CONFIRM_STATE:
			return(cell==droppedDest);
		case PASS_STATE:
		case GAMEOVER_STATE:
		case RESIGN_STATE:
			return(false);


        default:
        	throw G.Error("Not expecting state %s", board_state);

        case PUZZLE_STATE:
        	return((pickedObject==null) 
        			? ((cell!=null)&&(cell.height()>0)) 	// something available to pick up
        			: ( (pickedObject.typecode==CHIP_TYPE)	// dropping a beetle?
        					?true								// beetles go anywhere
        					:(cell.height()==0)));				// others only on level 0
        }
    }
    

 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Exxitmovespec m)
    {	robotDepth++;
        m.state = board_state;
        m.undoInfo = (pass_ends_game?1:0); //record the starting state. The most reliable
        // to undo state transistions is to simple put the original state back.
 
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");

        if (Execute(m,replayMode.Replay))
        {	if(m.op==MOVE_EXCHANGE)
        	{	// save a copy of the undo stack
        		int ncells = undoCell.height();
        		ExxitPiece pc[]=new ExxitPiece[ncells];
         		for(int i=0;i<ncells;i++) { pc[i]=undoCell.pieceAtIndex(i); }
        		m.undoExchangeInfo = pc;
        		setState(ExxitState.CONFIRM_EXCHANGE_STATE);
        		doDone(replayMode.Replay);
        	} 
        	else
            if ((m.op == MOVE_DONE)||(m.op==MOVE_PASS))
            {
            }
            else if (DoneState())
            {
                doDone(replayMode.Replay);
            }
            else
            {
            	throw G.Error("Robot move should be in a done state");
            }
        }
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(Exxitmovespec m)
    {	robotDepth--;
        //System.out.println("U "+m+" for "+whoseTurn);
        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;
   	    case MOVE_EXCHANGE:
   	    	{
   	    	ExxitPiece pc[] = m.undoExchangeInfo;
   	    	G.Assert(undoCell.height() == 0,"undo cell is available");
   	    	G.Assert(pc!=null,"undo info is available");
   	    	for(int i=0;i<pc.length;i++) { undoCell.push(pc[i]); }
   	    	undoExchange();
   	    	break;
   	    	}
        case MOVE_DROPB:
        	{
        	ExxitCell c = GetExxitCell(m.from_col,m.from_row);
        	ExxitPiece p = c.removeTop();
        	switch(p.typecode)
        	{ 
        	default: throw G.Error("not expecting %s",p);
        	case CHIP_TYPE: 
        		rack[playerIndex(p)].addPiece(p);
        		break;
        	case TILE_TYPE: 
        		tiles.addPiece(p);
        		tilesOnBoard--;
        		break;
        	}
        	break;
        	}
   	    case MOVE_MOVE:
   	    	{
   	    	ExxitCell c = GetExxitCell(m.from_col,m.from_row);
   	    	robotUndoDistribution(c,m.direction,m.undoDistributionInfo);
   	    	break;
   	    	}
   	    case MOVE_PASS:
   	    	break;
        case MOVE_DONE:
            break;
        case MOVE_RESIGN:
            break;
        }
    	droppedDest=null;
  	    pickedSource=null;
  	   	pickedObject=null;
        pass_ends_game=((m.undoInfo&1)!=0);
	    setState(m.state);
	    if(whoseTurn!=m.player)
	    {	moveNumber--;
	    	setWhoseTurn(m.player);
	    }
 }

static double TOP_WEIGHT = -0.1;
static double RESERVE_WEIGHT = 0.5;
// evaluator for "smartbot"
double nextEvaluation(int pl,boolean print)
{ 	// simple evaluation based on piece mobility and importance
	//double val = 0.0;
	//String msg = "";
	double score = scoreForPlayer(pl,true);
	int hh = rack[pl].height();
	int tc = countTopCells(pl);
	double aux_weight = ((tilesOnBoard+10)<tilesInGame)?1.0:((tilesInGame-tilesOnBoard)/10.0);
	double val = score+aux_weight*TOP_WEIGHT*tc+RESERVE_WEIGHT*hh;
	if(print)
		{ System.out.println(""+val+"="+score+" r="+hh*RESERVE_WEIGHT*aux_weight+" t="+tc*TOP_WEIGHT*aux_weight); 
		}
	return(val);
}
// evaluator for "dumbot"
double simpleEvaluation(int pl,boolean print)
{ 	// simple evaluation based on piece mobility and importance
 	// simple evaluation based on piece mobility and importance
	//double val = 0.0;
	//String msg = "";
	return(scoreForPlayer(pl,true));
	//if(print) { System.out.println(msg); }
	//return(val);
}
// evaluator for "bestbot"
double maxEvaluation(int pl,boolean print)
{  	// simple evaluation based on piece mobility and importance
	// simple evaluation based on piece mobility and importance
	//double val = 0.0;
	//String msg = "";
	double score = scoreForPlayer(pl,true);
	int hh = rack[pl].height();
	int tc = countTopCells(pl);
	double aux_weight = ((tilesOnBoard+10)<tilesInGame)?1.0:((tilesInGame-tilesOnBoard)/10.0);
	double val = score+aux_weight*TOP_WEIGHT*tc+RESERVE_WEIGHT*hh;
	if(print)
		{ System.out.println(""+val+"="+score+" r="+hh*RESERVE_WEIGHT*aux_weight+" t="+tc*TOP_WEIGHT*aux_weight); 
		}
	return(val);
}

public int CanDistributeToward(ExxitCell c,int dir,int who)
{	// c is known to be a cell suitable to distribute
	int oheight = c.height();
	int height = oheight;
	ExxitCell cell = c.exitTo(dir);
	boolean found = false;
	boolean foundsome = false;
	boolean offboard = false;
	while((height>1) && (cell!=null))
	{	ExxitPiece p = cell.topPiece();
		if(p==null) 
			{ offboard|=found; 
			  break; 
			}	// fell off the board
		if(cell.pieceAtIndex(0).typecode==CHIP_TYPE) { found=false; break; }	// found a chip off the board, no distribution
		if(!foundsome && (p.typecode==CHIP_TYPE))
		{	// first cell in this direction that contains a piece
			foundsome = true;
			if((playerIndex(p)==nextPlayer[who])&&(cell.height()<=oheight)) { found=true; } else { break; }
		}
		height--;
		cell = cell.exitTo(dir);
	}
	if(!found) { return(NODIST); }
	if(offboard) 
		{ return(DISTRIBUTE_OFFBOARD); 
		}
	return(DISTRIBUTE_ONBOARD);
}
public int GetListOfDistributionMoves(CommonMoveStack  all,int who)
{	int nadded = 0;
	boolean offBoard=false;
	int initialSize = (all!=null) ? all.size() : 0;
	for(int pidx=0;pidx<allPieces.length;pidx++)
	{	ExxitPiece p = allPieces[pidx];
		if(playerIndex(p)==who)
		{	ExxitCell c = p.location;
			if((c!=null) && c.onBoard && (p==c.topPiece()) && (c.pieceAtIndex(0).typecode==TILE_TYPE))
			{
				for(int dir=0;dir<6;dir++)
				{	// look in all directions
					int dist=CanDistributeToward(c,dir,who);
					switch(dist)
					{
					case NODIST:	break;
					case DISTRIBUTE_ONBOARD: 
						if(!offBoard)
							{if(all!=null) 
							{ // add a move type move
							 all.addElement(new Exxitmovespec(c.col,c.row,dir,chipPoolIndex[getColorMap()[who]],who)); }
							 nadded++;
							}
						break;
					case DISTRIBUTE_OFFBOARD: 
						if(!offBoard) 
						{	// first offboard distribution mode
							if(nadded>0)
							{
							// throw away any onboard distribution moves we've found
							if(all!=null) { all.setSize(initialSize); }
							nadded=0;
							}
							offBoard = true;
						}
						if(all!=null) 
						{ all.addElement(new Exxitmovespec(c.col,+c.row,dir,chipPoolIndex[getColorMap()[who]],who)); 
						}
						nadded++;
						break;
					default:
						break;
					}
				}
			}
			
		}
	}
	return(nadded);
}
// list of drop moves, given that there are no distribution moves
public int GetListOfDropMoves(CommonMoveStack  all,int who)
{	int nfound=0;
	if(rack[who].height()>0)
	{	for(int i=0;i<tilesInGame;i++)
		{	ExxitPiece tile = allTiles[i];
			ExxitCell c = tile.location;
			if(c.onBoard)
			{	if((c.height()==1) && (c.pieceAtIndex(0).typecode==TILE_TYPE))
				{	if(all!=null) 
					{ all.addElement(new Exxitmovespec(MOVE_DROPB,chipPoolIndex[getColorMap()[who]],c.col,c.row,who)); }
					nfound++;
				}
			}
		}
	}
	return(nfound);
}
//list of droptile moves, only used during expert setup phase
public int GetListOfDroptileMoves(CommonMoveStack  all,int who)
{	int nfound=0;
	if(board_state==ExxitState.DROPTILE_STATE)
	{
	sweep_counter++;
	for(int i=0;i<allTiles.length;i++)
	{	ExxitPiece p = allTiles[i];
		ExxitCell c = (p!=null) ? p.location : null;
		if(c!=null && c.onBoard)
		{	// a piece on top of a stack on the board
			//if(c.pieceAt(0).typecode==CHIP_TYPE)
			for(int dir=0;dir<6;dir++)
			{	ExxitCell nx =c.exitTo(dir);
				if((nx.sweep_counter!=sweep_counter) && (nx.height()==0))
				{	nx.sweep_counter=sweep_counter;
					if(all!=null) 
					{ all.addElement(new Exxitmovespec(MOVE_DROPB,tilePoolIndex[getColorMap()[who]],nx.col,nx.row,who)); 
					}
				nfound++;
				}
			}
		}
	}}
	return(nfound);
}

// list of excange moves, given that there are no distribution moves
public int GetListOfExchangeMoves(CommonMoveStack  all,int who)
{	int nfound=0;
	if(tiles.height()>0)
	{
	for(int i=0;i<allPieces.length;i++)
	{	ExxitPiece p = allPieces[i];
		ExxitCell c = p.location;
		
		if((c!=null) && c.onBoard && (c.topPiece()==p) && c.canExchange())
		{	// a piece on top of a stack on the board
			//if(c.pieceAt(0).typecode==CHIP_TYPE)
			if(all!=null) 
			{ all.addElement(new Exxitmovespec(MOVE_EXCHANGE,chipPoolIndex[getColorMap()[who]],c.col,c.row,who)); 
			}
			nfound++;
		}
	}}
	return(nfound);
}
public int nDistributionMoves(int who)
{	return(GetListOfDistributionMoves(null,who));
}
public int nExchangeMoves(int who)
{	return(GetListOfExchangeMoves(null,who));
}
public int nDropMoves(int who)
{	return(GetListOfDropMoves(null,who));
}
 public CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack all = new CommonMoveStack();
 	if(board_state==ExxitState.DROPTILE_STATE)
 		{
 		GetListOfDroptileMoves(all,whoseTurn);
  		}
 	else
 	{
 	int n = GetListOfDistributionMoves(all,whoseTurn);
 	if(n==0)
 	{	// if no distribution moves
 		n += GetListOfDropMoves(all,whoseTurn);
 		n += GetListOfExchangeMoves(all,whoseTurn);
 	}
	if(all.size()==0) { all.addElement(new Exxitmovespec(whoseTurn,MOVE_PASS)); }
 	}
	return(all);
  }
 public int nLegalMoves()
 {	CommonMoveStack  v = GetListOfMoves();
 	return(v.size());
 }
 
 
 //
 // cell labeling is a service we provide for the viewer, to help
 // it maintain the game record.  We do this work only in the viewer,
 // so the robot speed is not affected, and overhead is pretty unimportant.
 //
 // adjust the labels on the cell based on the current contents.  Except for the
 // transition from unoccupied to occupied, labels are static.  We also depend
 // on the fact that cells change one at a time.
 // 
 public void labelCells()
 {	for(ExxitCell c = allCells; c!=null; c=c.next)
 	{	labelCell(c);
  	}
 }
 // label once cell known to have changed
 private void labelCell(char col,int row) { labelCell(GetExxitCell(col,row)); }
 
 // set the label for one cell
 private void labelCell(ExxitCell c)
 {	if(c.height()>=1)
 	{
 	if("".equals(c.cellName))
 			{ int typecode = c.pieceAtIndex(0).typecode;
 			  switch(typecode)
				{
				default: throw G.Error("not expecting piece type %s",typecode);
				case TILE_TYPE:
					int cn = 'A';
					c.cellName = ""+(char)(cn + droppedTileCount);
					droppedTileCount++;
					break;
				case CHIP_TYPE:
					prisonerCount++;
					c.cellName = ""+ prisonerCount;
					break;
				}
 			}
 	}
 	else if(!"".equals(c.cellName))
 	{	// needs a labelectomy
 		String name =c.cellName;
 		char ct = name.charAt(0);
 		c.cellName = "";
 		switch(Character.isDigit(ct)?CHIP_TYPE:TILE_TYPE)
	    {
		  default: throw G.Error("Not expecting typecode %s",pickedObject.typecode);
		  case CHIP_TYPE:	
		  	{ int label = G.IntToken(name);
			  while(label<prisonerCount) { label++; changeCellLabel(""+label,""+(label-1)); }
			  prisonerCount--;
		  	}
		  	break;
		  case TILE_TYPE:
			  {	int label = ct-'A';
		  	  	while(label<droppedTileCount) 
		  	  		{ label++; changeCellLabel(""+(char)('A'+label),""+(char)('A'+label-1)); 
		  	  		}
				droppedTileCount--;
			  }
			break;
		  }

 	}
 }
 // this is used to effect a pulldown of labels
 private void changeCellLabel(String label,String newlabel)
 {	for(ExxitCell c = allCells; c!=null; c=c.next)
 	{	if(label.equals(c.cellName)) { c.cellName = newlabel; }
 	}
 }

}