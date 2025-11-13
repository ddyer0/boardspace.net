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
package crosswordle;
 
 import static crosswordle.CrosswordleMovespec.*;


 import java.util.*;
 import lib.*;
 import lib.Random;
 import online.game.*;
 import online.game.cell.Geometry;
 import dictionary.Dictionary;
 
 /**
  * Initial work September 2020
  *  
  * This shares a lot with Jumbulaya and Wyps
  * if any features or bug fixes occur, evaluate them too.
  * 
  * @author ddyer
  *
  */
 

 class CrosswordleBoard extends squareBoard<CrosswordleCell> implements BoardProtocol
 {	static int REVISION = 101;			// 100 represents the initial version of the game
 										// 101 adds the interpretation of the low bit of randomkey as "hard"
 	CrosswordleVariation variation = CrosswordleVariation.Crosswordle_55;
 	static final String[] CrosswordsGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers
 	public int getMaxRevisionLevel() { return(REVISION); }
 	private CrosswordleState board_state = CrosswordleState.Puzzle;	
 	private CrosswordleState unresign = null;	// remembers the orignal state when "resign" is hit
 	public CrosswordleState getState() { return(board_state); }
 	InternationalStrings s = G.getTranslations();
	public StringStack guesses = new StringStack();
	public String usedLetters()
	{	StringBuilder b = new StringBuilder();
		for(int i=0;i<guesses.size();i++) { b.append(guesses.elementAt(i)); }
		return b.toString();
	}
 
   public int scoreForPlayer(int i)
   {
   	return(score[i]);
   }
	 /**
	  * this is the preferred method when using the modern "enum" style of game state
	  * @param st
	  */
	 public void setState(CrosswordleState st) 
	 { 	unresign = (st==CrosswordleState.Resign)?board_state:null;
		 board_state = st;
		 if(!board_state.GameOver()) 
			 { AR.setValue(win,false); 	// make sure "win" is cleared
			 }
	 }

	 int score[] = null;
	 Dictionary dictionary ;				// the current dictionary
	 
 // this is required even if it is meaningless for this game, but possibly important
 // in other games.  When a draw by repetition is detected, this function is called.
 // the game should have a "draw pending" state and enter it now, pending confirmation
 // by the user clicking on done.   If this mechanism is triggered unexpectedly, it
 // is probably because the move editing in "editHistory" is not removing last-move
 // dithering by the user, or the "Digest()" method is not returning unique results
 // other parts of this mechanism: the Viewer ought to have a "repRect" and call
 // DrawRepRect to warn the user that repetitions have been seen.
	 public void SetDrawState() {throw G.Error("not expected"); };	
 
	 private CrosswordleState resetState = CrosswordleState.Puzzle; 
	 public CrosswordleChip lastDroppedObject = null;	// for image adjustment logic
 
	 // factory method to generate a board cell
	 public CrosswordleCell newcell(char c,int r)
	 {	return(new CrosswordleCell(CrosswordleId.BoardLocation,c,r,Geometry.Square));
	 }
	 
	 // constructor 
	 public CrosswordleBoard(String init,int players,long key,int map[],Dictionary di,int rev) // default constructor
	 {
		 drawing_style = DrawingStyle.STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
		 Grid_Style = CrosswordsGRIDSTYLE;
		 setColorMap(map, players);
		 if(rev>=101)
		 {
			key &= ~1;	// start with easy puzzles
		 }
		 doInit(init,key,players,rev); // do the initialization 
 
 
		 dictionary = di;
 
	 }
	
	 public String gameType() { return(gametype+" "+players_in_game+" "+randomKey+" "+revision); }
	 
	 public void setRandomKey(long v,boolean hard) 
	 { randomKey = v;
	   if(hard) { randomKey = randomKey|1; } else { randomKey = randomKey & ~1; }
	   solutionKey = v;
	   solution = null;
	 }
	 
	 private void construct(int n,int k)
	 {	
		 reInitBoard(n,k);		 //this sets up the primary board and cross links
 
	 }
	 public void doInit(String gtype,long key)
	 {
		 StringTokenizer tok = new StringTokenizer(gtype);
		 String typ = tok.nextToken();
		 int np = tok.hasMoreTokens() ? G.IntToken(tok) : players_in_game;
		 long ran = tok.hasMoreTokens() ? G.LongToken(tok) : key;
		 int rev = tok.hasMoreTokens() ? G.IntToken(tok) : revision;
		 doInit(typ,ran,np,rev);
	 }
	 
	 private String solution = null;
	 private long solutionKey = 0;
	 
	 String getSolution(long key)
	 {	
		 if((solution==null)||(solutionKey!=key))
		 {	Builder b = Builder.getInstance();
		 	Random r = new Random(key);
		 	solution = b.getDaysPuzzle(key,ncols,nrows,revision>=101? (randomKey&1)!=0 :false);
		 	while(solution == null)
			 {	
		 		if(ncols>=6 && nrows>=6) { b.vocabulary = 9999999; }	//solution space is sparse
		 		solution = b.generate1Crossword(r.nextLong(),ncols,nrows);
			 }
			 solutionKey = key;
		 }
		 return solution;
	 }
	 
	 // load a solution encoded as a readable grid into the cells
	 private void loadSolution()
	 {	int index = 0;
		for(int row=nrows; row>0; row--)
		 {
			 for(int col = 0; col<ncols; col++)
			 {	CrosswordleCell c = getCell((char)('A'+col),row);
			 	char ch = 0;
			 	while( (ch=(solution.charAt(index++)))<=' ') {};
			 	CrosswordleChip chip = CrosswordleChip.getLetter(ch);
			 	c.reInit();
			 	c.addChip(chip);
			 }
		 }
	 }
 
	 /* initialize a board back to initial empty state */
	 public void doInit(String gtype,long key,int players,int rev)
	 {	randomKey = key;
	 	adjustRevision(rev);
		 players_in_game = players;
		 win = new boolean[players];
		 variation = CrosswordleVariation.findVariation(gtype);
		 G.Assert(variation!=null,WrongInitError,gtype);
		 score = new int[players];
		 gametype = gtype;
		 guesses.setSize(0);
	 switch(variation)
		 {
		 default: throw G.Error("Not expecting variation %s",variation);
		 case Crosswordle_55:
		 case Crosswordle_65:
		 case Crosswordle_66:
			 break;
		 }
	 
	 construct(variation.boardsizeX,variation.boardsizeY);
	 
	 //don't do this as part of init so copy boards are created fast
	 //getSolition(randomKey);
	 //loadSolution();
			 
		 Random r = new Random(randomKey+100);
		 r.nextLong();//  use one random, for compatibility
			 
		 setState(CrosswordleState.Puzzle);
				 
		 whoseTurn = FIRST_PLAYER_INDEX;
		 
		 acceptPlacement();
 
		 resetState = CrosswordleState.Puzzle;
		 lastDroppedObject = null;
		 moveNumber = 1;
 
		 // note that firstPlayer is NOT initialized here
	 }
 
	 /** create a copy of this board */
	 public CrosswordleBoard cloneBoard() 
	 { 
 
	   CrosswordleBoard dup = new CrosswordleBoard(gametype,players_in_game,randomKey,getColorMap(),dictionary,revision); 
	   dup.copyFrom(this);
	   
	   return(dup); 
	   }
	 public void copyFrom(BoardProtocol b) { copyFrom((CrosswordleBoard)b); }
 
	 /* make a copy of a board.  This is used by the robot to get a copy
	  * of the board for it to manipulate and analyze without affecting 
	  * the board that is being displayed.
	  *  */
	 public void copyFrom(CrosswordleBoard from_b)
	 {
		 super.copyFrom(from_b);
		 unresign = from_b.unresign;
		 board_state = from_b.board_state;
		 resetState = from_b.resetState;
		 AR.copy(score,from_b.score);
		 guesses.copyFrom(from_b.guesses);
		 solution = from_b.solution;
		 solutionKey = from_b.solutionKey;
		 if(G.debug()) { sameboard(from_b); }
	 }
 
	 
 
	 public void sameboard(BoardProtocol f) { sameboard((CrosswordleBoard)f); }
 
	 /**
	  * Robots use this to verify a copy of a board.  If the copy method is
	  * implemented correctly, there should never be a problem.  This is mainly
	  * a bug trap to see if BOTH the copy and sameboard methods agree.
	  * @param from_b
	  */
	 public void sameboard(CrosswordleBoard from_b)
	 {
		 super.sameboard(from_b); // // calls sameCell for each cell, also for inherited class variables.
		 G.Assert(unresign==from_b.unresign,"unresign mismatch");
		 G.Assert(variation==from_b.variation,"variation matches");
		 G.Assert(AR.sameArrayContents(score,from_b.score),"score mismatch");
		 G.Assert(guesses.eqContents(from_b.guesses),"guesses mismatch");
		 
		 // this is a good overall check that all the copy/check/digest methods
		 // are in sync, although if this does fail you'll no doubt be at a loss
		 // to explain why.
		 G.Assert(Digest()==from_b.Digest(),"Sameboard ok, Digest mismatch");
 
	 }
 
	 /** 
	  * Digest produces a 64 bit hash of the game state.  This is used in many different
	  * ways to identify "same" board states.  Some are relevant to the ordinary operation
	  * of the game, others are for system record keeping use; so it is important that the
	  * game Digest be consistent both within a game and between games over a long period
	  * of time which have the same moves. 
	  * (1) Digest is used by the default implementation of EditHistory to remove moves
	  * that have returned the game to a previous state; ie when you undo a move or
	  * hit the reset button.  
	  * (2) Digest is used after EditHistory to verify that replaying the history results
	  * in the same game as the user is looking at.  This catches errors in implementing
	  * undo, reset, and EditHistory
	  * (3) Digest is used by standard robot search to verify that move/unmove 
	  * returns to the same board state, also that move/move/unmove/unmove etc.
	  * (4) Digests are also used as the game is played to look for draw by repetition.  The state
	  * after most moves is recorded in a hashtable, and duplicates/triplicates are noted.
	  * (5) games where repetition is forbidden (like xiangqi/arimaa) can also use this
	  * information to detect forbidden loops.
	  * (6) Digest is used in fraud detection to see if the same game is being played
	  * over and over. Each game in the database contains a digest of the final
	  * state of the game, and a midpoint state of the game. Other site machinery
	  * looks for duplicate digests.  
	  * (7) digests are also used in live play to detect "parroting" by running two games
	  * simultaneously and playing one against the other.
	  */
	 public long Digest()
	 { 
		 // the basic digestion technique is to xor a bunch of random numbers. 
		 // many object have an associated unique random number, including "chip" and "cell"
		 // derivatives.  If the same object is digested more than once (ie; once as a chip
		 // in play, and once as the chip currently "picked up", then it must be given a
		 // different identity for the second use.
		 //
		 Random r = new Random(64 * 1000); // init the random number generator
		 long v = super.Digest(r);
		 // many games will want to digest pickedSource too
		 // v ^= cell.Digest(r,pickedSource);
		 v ^= Digest(r,revision);
		 v ^= Digest(r,score);
		 v ^= guesses.Digest(r);
		 v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
		 return (v);
	 }
 
	 public boolean hasBeenGuessed(String word)
	 {
		 return guesses.contains(word);
	 }
	 public boolean canBeGuessed(String word)
	 {	
		 int len = word.length();
		 return((len==ncols || len==nrows)
				 && !hasBeenGuessed(word)
				 && (dictionary.get(word)!=null));
	 }
	 public boolean isReady()
	 {
		 return dictionary.isLoaded();
	 }

	 /** this is used to determine if the "Done" button in the UI is live
	  *
	  * @return
	  */
	 public boolean DoneState()
	 {	return(board_state.doneState());
	 }
	 // this is the default, so we don't need it explicitly here.
	 // but games with complex "rearrange" states might want to be
	 // more selective.  This determines if the current board digest is added
	 // to the repetition detection machinery.
	 public boolean DigestState()
	 {	
		 return(board_state.digestState());
	 }
 
 
 
	 public boolean gameOverNow() { return(board_state.GameOver()); }
	 public boolean winForPlayerNow(int player)
	 {	if(win[player]) { return(true); }
		 boolean win = false;
		 return(win);
	 }

	 //
	 // accept the current placements as permanent
	 //
	 public void acceptPlacement()
	 {	
	  }
	 
	 // collect a word if all it's letters are exposed
	 public String collectWord(CrosswordleCell from,int direction)
	 {
		 StringBuilder b = new StringBuilder();
		 CrosswordleCell c = from;
		 while(c!=null) 
		 { 
			 if((c.color==LetterColor.Green)||(c.color==LetterColor.NewGreen))
			 {
			 char ch = c.topChip().lcChar;
			 b.append(ch);
			 c = c.exitTo(direction);
			 }
			 else { return null; }
		 }
		 return b.toString();
	 }
	 
	 //get the index in the image array corresponding to movingObjectChar 
	 // or HitNoWhere if no moving object.  This is used to determine what
	 // to draw when tracking the mouse.
	 // caution! this method is called in the mouse event process
	 public int movingObjectIndex()
	 { 
			 return (NothingMoving);
	 }
	/**
	  * get the cell represented by a source code, and col,row
	  * @param source
	  * @param col
	  * @param row
	  * @return
	  */
	 private CrosswordleCell getCell(CrosswordleId source, char col, int row)
	 {
		 switch (source)
		 {
		 default:
			 throw G.Error("Not expecting source " + source);
		 case BoardLocation:
		 case EmptyBoard:
			 return(getCell(col,row));
		 } 	
	 }
	 /**
	  * this is called when copying boards, to get the cell on the new (ie current) board
	  * that corresponds to a cell on the old board.
	  */
	 public CrosswordleCell getCell(CrosswordleCell c)
	 {
		 return((c==null)?null:getCell(c.rackLocation(),c.col,c.row));
	 }


	 //
	 // in the actual game, picks are optional; allowed but redundant.
	 //
	 private boolean gameOver()
	 {
		 for(CrosswordleCell c = allCells; c!=null; c=c.next)
		 {
			 switch(c.color) {
			 case Green:
			 case NewGreen: break;
			 default: 
				 score[0]=0;
				 return false;
			 }
		 }
		 score[0] = guesses.size();
		 return true;
	 }
	 private void setNextStateAfterDrop(CrosswordleCell c,CrosswordleChip ch,replayMode replay)
	 {
		 switch (board_state)
		 {
		 default:
			 throw G.Error("Not expecting drop in state " + board_state);
		 case Play:
			 if(gameOver()) { setState(CrosswordleState.Gameover); }
			 else {	 setState(CrosswordleState.Play);} 
			 break;
		 case Puzzle:
			 acceptPlacement();
			 break;
		 }
	 }
 
	 private void setNextStateAfterDone(replayMode replay)
	 {	
			   switch(board_state)
		 {
		 default: throw G.Error("Not expecting after Done state "+board_state);
		 case Gameover: break;
		 case Puzzle:
		 case Play:

    			setState( CrosswordleState.Play);
    		
    		break;
    	}
       	resetState = board_state;
    }
    public int nextPlayer()
    {
    	return((whoseTurn+1)%players_in_game);
    }


	public void scoreWord(CrosswordleMovespec m,String w)
	{	CrosswordleCell col = getCell('A',nrows);
		CrosswordleCell row = col;
		int len = w.length();
		for(CrosswordleCell c = allCells; c!=null; c=c.next)
		{	// age new yellow and new green to yellow and green
			switch(c.color) {
			case Blank:
			case Yellow:
			case Green:
				break;
			case NewYellow:	
				c.color = LetterColor.Yellow;
				break;
			case NewGreen:
				c.color = LetterColor.Green;
				break;
			default: G.Error("Not expected");			
			}
		}
		
		guesses.push(w);
		m.greens = 0;
		m.yellows = 0;
		m.index = guesses.size();
		if(len==nrows) { while(col!=null) { scoreWord(m,w,col,CELL_DOWN); col =col.exitTo(CELL_RIGHT); }}
		if(len==ncols) { while(row!=null) { scoreWord(m,w,row,CELL_RIGHT); row = row.exitTo(CELL_DOWN); }}
	}
	public void scoreWord(CrosswordleMovespec m,String word,CrosswordleCell from,int direction)
	{	CrosswordleCell c = from;
		for(int i=0,lim=word.length(); i<lim; i++)
		{
			CrosswordleChip wchar = CrosswordleChip.getLetter(word.charAt(i));
			CrosswordleChip top = c.topChip();
			if(wchar==top) 
				{ 
				if(c.color!=LetterColor.NewGreen)
				{
				if(c.color==LetterColor.NewYellow) { m.yellows--; }
				c.color = LetterColor.NewGreen;
				m.greens++; 
				}}
			else if(c.color==LetterColor.Blank || c.color==LetterColor.Yellow)
			{ 
				for(int j=0;j<word.length();j++)
				{
					CrosswordleChip target = CrosswordleChip.getLetter(word.charAt(j));
					if(target==top) 
						{ c.color = LetterColor.NewYellow; m.yellows++; break; 
						}
				}				
			}
			c = c.exitTo(direction);
		}
	}
    public boolean Execute(commonMove mm,replayMode replay)
    {	CrosswordleMovespec m = (CrosswordleMovespec)mm;
        //G.print("E "+m+" for "+whoseTurn+" "+board_state);
        switch (m.op)
        {
        case MOVE_PLAYWORD:
        	{
        	String w = m.word;
        	if(canBeGuessed(w))
        		{
        		scoreWord(m,w);
        		moveNumber++;
        		}
        	setNextStateAfterDrop(null,null,replay);
        	}
        	break;
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            // standardize the gameover state.  Particularly importing if the
            // sequence in a game is resign/start
            setState(CrosswordleState.Puzzle);	// standardize the current state
       	 	getSolution(randomKey);
       	 	loadSolution();

            setNextStateAfterDone(replay);

            break;

       case MOVE_RESIGN:
    	   	setState(unresign==null?CrosswordleState.Resign:unresign);
            break;
       case MOVE_EDIT:
        	acceptPlacement();
            setState(CrosswordleState.Puzzle);
 
            break;

       case MOVE_GAMEOVERONTIME:
    	   win[whoseTurn] = true;
    	   setState(CrosswordleState.Gameover);
    	   break;
       case MOVE_DONE:
    	   // resigning 
    	   setState(CrosswordleState.Gameover);
    	   break;
       default:
        	cantExecute(m);
        }
       //System.out.println("Ex "+m+" for "+whoseTurn+" "+state);
        return (true);
    }



}
