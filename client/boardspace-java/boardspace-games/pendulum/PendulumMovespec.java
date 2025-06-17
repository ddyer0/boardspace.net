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
package pendulum;

import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import online.game.*;
import pendulum.PendulumConstants.PendulumId;
import lib.ExtendedHashtable;
public class PendulumMovespec 
		extends commonMPMove	// for a multiplayer game, this will be commonMPMove
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; 	// pick a chip from a pool
    static final int MOVE_DROP = 205; 	// drop a chip
    static final int MOVE_FROM_TO = 206;// move a chip from a to b
    static final int MOVE_SETACTIVE = 207;	// set the active player
    static final int MOVE_FLIP = 208;		// flip a timer
    static final int MOVE_STANDARD_ACHIEVEMENT = 209;	// take the standard benefit instead of the legendary
    static final int MOVE_REST = 210;	// rest until the other players are ready too
    static final int MOVE_STARTCOUNCIL = 211;	// enter council mode
    static final int MOVE_SELECT = 212;		// select but do not pick up something
    static final int MOVE_READY = 213;
    static final int MOVE_STARTPLAY = 214;
    static final int MOVE_AUTOFLIP = 215;
    static final int MOVE_LEGENDARY_ACHIEVEMENT = 216;
    static final int MOVE_SWAPVOTES = 217;
    static final int MOVE_PAUSE_COMMUNICATION = 218;	// for privilege testing
    static final int MOVE_RESUME_COMMUNICATION = 219;	
    static final int MOVE_WAIT = 220;
    static final int MOVE_REFILL = 221;
    static final int MOVE_PAUSE = 222;	// pause timers
    static final int MOVE_RESUME = 223;	// resume timers
    
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
        	"Pick", MOVE_PICK,
        	"Drop", MOVE_DROP,
        	"Move", MOVE_FROM_TO,
        	"Flip",MOVE_FLIP,
        	"Rest",MOVE_REST,
        	"Ready",MOVE_READY,
        	"Play",MOVE_STARTPLAY,
        	"AutoFlip",MOVE_AUTOFLIP,
        	"Select",MOVE_SELECT,
        	"SwapVotes",MOVE_SWAPVOTES,
        	"pausecommunication",MOVE_PAUSE_COMMUNICATION,
        	"resumecommunication",MOVE_RESUME_COMMUNICATION,
        	"standardAchievement",MOVE_STANDARD_ACHIEVEMENT,
        	"legendaryAchievement",MOVE_LEGENDARY_ACHIEVEMENT,
        	"pause",MOVE_PAUSE,
        	"resume",MOVE_RESUME,
        	"Council",MOVE_STARTCOUNCIL,
        	"Wait",MOVE_WAIT,
        	"PlayerRefill",MOVE_REFILL,
        	"SetActive",MOVE_SETACTIVE);
    }
    //
    // adding these makes the move specs use Same_Move_P instead of == in hash tables
    //needed when doing chi square testing of random move generation, but possibly
    //hazardous to keep in generally.
    //public int hashCode()
    //{
    //	return(to_row<<12+to_col<<18+player<<24+op<<25);
    //}
    //public boolean equals(Object a)
    //{
    //	return( (a instanceof commonMove) && Same_Move_P((commonMove)a)); 
    //}
    //
    // variables to identify the move
    PendulumId source; // where from/to
    PendulumId dest;
    
    int forPlayer;		// the player making this move.  This is explicit separate from the regular "player" slot.
    					// games with simultaneous moves can't use the game state to guess the player
    char to_col;
    char from_col;
    int to_row; // for from-to moves, the destination row
    int from_row;
    PendulumChip chip;
    int blackTimer = 0;
    int greenTimer = 0;
    int purpleTimer = 0;
    long realTime = 0;
    // these provide an interface to log annotations that will be seen in the game log
    String gameEvents[] = null;
    public String[] gameEvents() { return(gameEvents); }

    public PendulumMovespec()
    {
    } // default constructor

    /* constructor */
    public PendulumMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    public PendulumMovespec(int opc , int p)
    {
    	op = opc;
    	forPlayer = player = p;
    }
  
    /** constructor for robot moves.  Having this "binary" constor is dramatically faster
     * than the standard constructor which parses strings
     */
    public PendulumMovespec(int opc,PendulumCell c,PendulumChip ch,int who)
    {
    	op = opc;
    	source = dest = c.rackLocation();
    	from_row = to_row = c.row;
      	chip = ch;
     	to_col = from_col = c.col;
    	forPlayer = player = who;
    	G.Assert(chip!=null,"should be a chip");
    }
    /* constructor */
    public PendulumMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    public PendulumMovespec(int moveFromTo, PendulumCell from, PendulumChip ch, PendulumCell to, int who) 
    {
		op = moveFromTo;
		source = from.rackLocation();
		from_row = from.row;
		from_col = from.col;
		chip = ch;
		dest = to.rackLocation();
		to_row = to.row;
		to_col = to.col;
		forPlayer = player = who;
		G.Assert(chip!=null,"should be a chip");
	}

	/**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        PendulumMovespec other = (PendulumMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (dest == other.dest)
				&& (from_row == other.from_row)
				&& (to_row == other.to_row) 
				&& (chip == other.chip)
				&& (from_col==other.from_col)
				&& (to_col==other.to_col)
				&& (forPlayer==other.forPlayer)
				&& (player == other.player));
    }

    public void Copy_Slots(PendulumMovespec to)
    {	super.Copy_Slots(to);
    	to.forPlayer = forPlayer;
        to.from_row = from_row;
        to.to_row = to_row;
        to.source = source;
        to.from_col = from_col;
        to.to_col = to_col;
        to.dest = dest;
        to.chip = chip;
        to.blackTimer = blackTimer;
        to.greenTimer = greenTimer;
        to.purpleTimer = purpleTimer;
        to.realTime = realTime;
    }

    public commonMove Copy(commonMove to)
    {
        PendulumMovespec yto = (to == null) ? new PendulumMovespec() : (PendulumMovespec) to;

        // we need yto to be a Pushfightmovespec at compile time so it will trigger call to the 
        // local version of Copy_Slots
        Copy_Slots(yto);

        return (yto);
    }

    /* parse a string into the state of this move.  Remember that we're just parsing, we can't
     * refer to the state of the board or the game.  This parser follows the recommended practice
     * of keeping it very simple.  A move spec is just a sequence of tokens parsed by calling
     * nextToken
     * @param msg a string tokenizer containing the move spec
     * @param the player index for whom the move will be.
     * */
    private void parse(StringTokenizer msg, int p)
    {
        String cmd = msg.nextToken();
        player = p;

        if (Character.isDigit(cmd.charAt(0)))
        { // if the move starts with a digit, assume it is a sequence number
            setIndex(G.IntToken(cmd));
            cmd = msg.nextToken();
        }

        op = D.getInt(cmd, MOVE_UNKNOWN);
        forPlayer = msg.hasMoreTokens() ? D.getInt(msg.nextToken()) : p;
        
        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Cant parse " + cmd);
        case MOVE_SWAPVOTES:	
        case MOVE_FROM_TO:
        	source = PendulumId.valueOf(msg.nextToken());
        	from_col = G.CharToken(msg);
        	from_row = G.IntToken(msg);
        	chip = PendulumChip.find(msg.nextToken());
        	dest = PendulumId.valueOf(msg.nextToken());
        	to_col = G.CharToken(msg);
        	to_row = G.IntToken(msg);
        	break;
        case MOVE_FLIP:
        case MOVE_DROP:
            source = dest = PendulumId.valueOf(msg.nextToken());
            from_col = to_col = G.CharToken(msg);
            from_row = to_row = G.IntToken(msg);
            break;
        case MOVE_WAIT:
        	from_row = to_row = G.IntToken(msg);
        	break;
        case MOVE_SELECT:
        case MOVE_PICK:
            source = dest = PendulumId.valueOf(msg.nextToken());
            from_col = to_col = G.CharToken(msg);
            from_row = to_row = G.IntToken(msg);
            if(msg.hasMoreTokens()) { chip = PendulumChip.find(msg.nextToken()); }
            break;
        case MOVE_REST:
        case MOVE_READY:
        case MOVE_LEGENDARY_ACHIEVEMENT:
        case MOVE_STANDARD_ACHIEVEMENT:
        case MOVE_SETACTIVE:
        case MOVE_REFILL:
        case MOVE_START:
            break;
        case MOVE_AUTOFLIP:    
        case MOVE_STARTCOUNCIL:
        case MOVE_PAUSE_COMMUNICATION:
        case MOVE_RESUME_COMMUNICATION:
        case MOVE_PAUSE:
        case MOVE_RESUME:
        case MOVE_STARTPLAY:
        default:

            break;
        }
        if(msg.hasMoreElements())
        {	String tok = msg.nextToken();
         	if("t".equals(tok))
         	{
         		purpleTimer = G.IntToken(msg);
         		greenTimer = G.IntToken(msg);
         		blackTimer = G.IntToken(msg);
         		if(msg.hasMoreElements()) { realTime = G.LongToken(msg); }
         	}
         	else { G.Error("unexpected token "+tok); }
        }
    }

    private Text icon(commonCanvas v,Object... msg)
    {	double chipScale[] = {1,1.5,-0.2,-0.5};
    	Text m = TextChunk.create(G.concat(msg));
    	if(chip!=null)
    	{
    		m = TextChunk.join(TextGlyph.create("xx", chip, v,chipScale),
    					m);
    	}
    	return(m);
    }

    /** construct an abbreviated move string, mainly for use in the game log.  These
     * don't have to be parseable, they're intended only to help humans understand
     * the game record.  The alternative method {@link #shortMoveText} can be implemented
     * to provide colored text or mixed text and icons.
     * 
     * */
    public Text shortMoveText(commonCanvas v)
    {
        switch (op)
        {
       	case MOVE_REFILL:
    		return TextChunk.create("Retrieve Cards");
        case MOVE_SWAPVOTES:
        case MOVE_DROP:
        case MOVE_FROM_TO:
        	switch(dest)
        	{
         	case PurpleActionA:
        	case PurpleActionB:
        		setLineBreak(true);
        		return icon(v,"Purple action");
        	case GreenActionA:
        	case GreenActionB:
        		setLineBreak(true);
        		return icon(v,"Green action");
        	case BlackActionA:
        	case BlackActionB:
        		setLineBreak(true);
       		return icon(v,"Black action");
        	case PurpleMeepleA:
        	case PurpleMeepleB:
        		setLineBreak(true);
       		return icon(v,"to Purple");
        	case BlackMeepleA:
        	case BlackMeepleB:
        		setLineBreak(true);
        		return icon(v,"to Black");
        	case GreenMeepleA:
        	case GreenMeepleB:
        		setLineBreak(true);
        		return icon(v,"to Green");
        	case PlayerCash:
        	case PlayerMilitary:
        	case PlayerCulture:
        		return icon(v,"Gained");
        		
        	case PlayerYellowBenefits:
        		return icon(v,"Yellow");
        	case PlayerBlueBenefits:
        		return icon(v,"Blue");
        	case PlayerRedBenefits:
        		return icon(v,"Red");
        	case PlayerBrownBenefits:
        		return icon(v,"Brown");
        	case PlayerCashReserves:
        	case PlayerMilitaryReserves:
        	case PlayerCultureReserves:
        		return icon(v,"Paid");
        	case PlayerPlayedStratCard:
        		setLineBreak(true);
        		return icon(v,"Card Played");
        	default:
        		return icon(v,dest.name());
        	}
        	
        case MOVE_READY:
        	return TextChunk.create("");
        case MOVE_REST:
        	return TextChunk.create("");
        case MOVE_LEGENDARY_ACHIEVEMENT:
        	chip = PendulumChip.legendary;
        	return icon(v,"Achievement");
        	
        case MOVE_STANDARD_ACHIEVEMENT:
        	return TextChunk.create("Standard Achievement");
        case MOVE_FLIP:
        	switch(source)
        	{
        	case BlackTimer:
        		chip = PendulumChip.blackTimer;
        		return icon(v,"Flip");
        	case GreenTimer:
        		chip = PendulumChip.greenTimer;
        		return icon(v,"Flip");
        	case PurpleTimer:
        		chip = PendulumChip.purpleTimer;
        		return icon(v,"Flip");
        	default: 
        		return TextChunk.create("flip");
        	}
        
        case MOVE_SELECT:
        	switch(source)
        	{
        	case RewardCard:
        		return icon(v,"Council Card");
        	default: break;
        	}
			//$FALL-THROUGH$
		case MOVE_PICK:
        	return icon(v,"");
        case MOVE_SETACTIVE:
        case MOVE_DONE:
            return TextChunk.create("");
        case MOVE_AUTOFLIP:
        case MOVE_STARTCOUNCIL:
        case MOVE_STARTPLAY:
        case MOVE_PAUSE_COMMUNICATION:
        case MOVE_RESUME_COMMUNICATION:
        case MOVE_WAIT:
        default:
            return TextChunk.create(D.findUniqueTrans(op));

        }
    }

    /** construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and only secondarily human readable */
    public String moveString()
    {
		String indx = indexString();
		String opname = indx+D.findUnique(op)+" P"+forPlayer+" ";
        // adding the move index as a prefix provides numnbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        case MOVE_SWAPVOTES:
        case MOVE_FROM_TO:
        	return G.concat(opname,source.name()," ",from_col," ",from_row," ",chip.idString()," ",dest.name()," ",to_col," ",to_row,
        			" t ",purpleTimer," ",greenTimer," ",blackTimer," ",realTime);
        case MOVE_FLIP:	
        case MOVE_DROP:
        	return G.concat(opname, dest.name()," ",to_col," ",to_row,
        			" t ",purpleTimer," ",greenTimer," ",blackTimer," ",realTime);
        	
        case MOVE_SELECT:
        case MOVE_PICK:
            return G.concat(opname , source.name()," ",from_col," ",from_row," ",chip.idString(),
            		" t ",purpleTimer," ",greenTimer," ",blackTimer," ",realTime);

        case MOVE_WAIT:
        	return G.concat(opname ,from_row);
        	
        case MOVE_STANDARD_ACHIEVEMENT:
        case MOVE_LEGENDARY_ACHIEVEMENT:
        case MOVE_REST:
        case MOVE_READY:
        case MOVE_REFILL:
        case MOVE_SETACTIVE:
            return opname;
        case MOVE_EDIT:
        	return "Edit";
        case MOVE_START:
        case MOVE_AUTOFLIP:
        case MOVE_STARTCOUNCIL:
        case MOVE_PAUSE_COMMUNICATION:
        case MOVE_RESUME_COMMUNICATION:
        case MOVE_PAUSE:
        case MOVE_RESUME:
        case MOVE_STARTPLAY:
        default:
            return opname;
        }
    }
    /**
     *  longMoveString is used for sgf format records and can contain other information
     * intended to be ignored in the normal course of play, for example human-readable
     * information
     */
    /*
    public String longMoveString()
    {	String str = moveString();
    	return(str);
    }
    */
    /** standard java method, so we can read moves easily while debugging */
    /*
    public String toString()
    {	return super.toString();
        //return ("P" + player + "[" + moveString() + "]");
    }
    */
    /*
    public void setGameover(boolean v)
    {	//if(visit())
    	//	{
    	//	G.Error("makeover");
    	//	}
    	super.setGameover(v);
    }
    */
   /* 
    public boolean visit()
    {	
    	//if( (from_col=='F' && from_row==10 && to_col=='E' && to_row==9))
    	//{
    	//	UCTNode uct = uctNode();
    	//	if(uct!=null) { UCTNode.marked = uct; }
    	//	return(true);
    	//}
    	return(false);
    	//		||(from_col=='C' && from_row==5 && to_col=='A' && to_row==7);
    	//return(false);
    }
    */
}
