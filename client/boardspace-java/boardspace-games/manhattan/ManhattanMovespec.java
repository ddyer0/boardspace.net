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
package manhattan;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;
import manhattan.ManhattanConstants.MColor;
import manhattan.ManhattanConstants.ManhattanId;
import manhattan.ManhattanConstants.Type;
import online.game.*;

import java.awt.Font;

import lib.ExtendedHashtable;
public class ManhattanMovespec 
		extends commonMPMove
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_SELECT = 208;	// select an item
    static final int MOVE_RETRIEVE = 209;	// retrieve workers
    static final int MOVE_FROM_TO = 210;	// place workers
    static final int MOVE_ATTACK = 211;	// place workers
    static final int MOVE_REPAIR = 212;	// repair buildings
    static final int MOVE_CONTRIBUTE = 213;
    static final int MOVE_APPROVE = 214;
    static final int EPHEMERAL_CONTRIBUTE = 313;
    static final int EPHEMERAL_APPROVE = 314;
    static final int MOVE_SKIPMYTURN = 315;	// fake move for the robot to use
    
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
        	"Pick", MOVE_PICK,
        	"Drop", MOVE_DROP,
        	"Skip",MOVE_SKIPMYTURN,
        	"Select",MOVE_SELECT,
        	"Retrieve",MOVE_RETRIEVE,
        	"Move",MOVE_FROM_TO,
        	"Attack",MOVE_ATTACK,
        	"Repair",MOVE_REPAIR,
        	"Contribute",MOVE_CONTRIBUTE,
        	"EContribute",EPHEMERAL_CONTRIBUTE,
        	"Approve",MOVE_APPROVE,
        	"EApprove",EPHEMERAL_APPROVE
    			);
  }
    /* this is used by the move filter to select ephemeral moves */
    public boolean isEphemeral()
	{
    	return op>300;
	}

    //
    // variables to identify the move
    ManhattanId source; // where from/to
    ManhattanId dest;
    int from_row;
    MColor from_color = MColor.Board;
    MColor to_color = MColor.Board;
    int to_row; // for from-to moves, the destination row
    int from_index = 0;
    int to_index = 0;
    ManhattanChip chip;
    ManhattanCell cell;
    //
    // this is a hack to track invalid game records
    // public long startingDigest = 0;
    //
    // these provide an interface to log annotations that will be seen in the game log
    String gameEvents[] = null;
    public String[] gameEvents() { return(gameEvents); }

    public ManhattanMovespec()
    {
    } // default constructor

    /* constructor */
    public ManhattanMovespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }
    public ManhattanMovespec(int opc , int p)
    {
    	op = opc;
    	player = p;
    }
    
    public ManhattanMovespec(int opc,ManhattanCell c,int index,int who)
    {
    	op = opc;
    	source=dest=c.rackLocation();
    	from_color = to_color = c.color;
    	from_row = to_row = c.row;
    	from_index = to_index = index;
    	player = who;
    }
    /** constructor for robot moves.  Having this "binary" constor is dramatically faster
     * than the standard constructor which parses strings
     */
    public ManhattanMovespec(int opc,ManhattanId d,int row,int who)
    {
    	op = opc;
     	dest = source = d;
    	to_row = from_row = row;
    	player = who;
    }


    // for retrieve
    public ManhattanMovespec(int opc, MColor color, int pla)
    {
		op = opc;
		from_color = to_color = color;
		player = pla;
	}
    // for contribute
    // for retrieve
    public ManhattanMovespec(int opc, MColor color, int am,int pla)
    {
		op = opc;
		from_color = to_color = color;
		from_index = to_index = am;
		player = pla;
	}

    // move workers somewhere
    public ManhattanMovespec(int opc,ManhattanCell from,int index,ManhattanCell to,int pl)
    {
    	op = opc;
    	G.Assert(to.color!=null,"should be colored");
    	G.Assert(from.color!=null,"should be colored");
    	source = from.rackLocation();
    	from_color = from.color;
    	from_row = from.row;
    	from_index = index;
    	dest = to.rackLocation();
    	to_color = to.color;
    	
    	to_row = to.row;
    	to_index = -1;
    	player = pl;
    }
	/**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        ManhattanMovespec other = (ManhattanMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (dest == other.dest)
				&& (to_row == other.to_row) 
				&& (from_row == other.from_row)
				&& (player == other.player))
        		&& (from_index == other.from_index)
        		&& (to_index == other.to_index)
        		&& (from_color == other.from_color)
        		&& (to_color == other.to_color)
        		;
    }

    public void Copy_Slots(ManhattanMovespec to)
    {	super.Copy_Slots(to);
        to.dest = dest;
        to.to_row = to_row;
        to.source = source;
        to.from_row = from_row;
        to.chip = chip;
        to.to_index = to_index;
        to.from_index = from_index;
        to.to_color = to_color;
        to.from_color = from_color;
    }

    public commonMove Copy(commonMove to)
    {
        ManhattanMovespec yto = (to == null) ? new ManhattanMovespec() : (ManhattanMovespec) to;

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
    private void parse(Tokenizer msg, int p)
    {
    	parse1(msg,p);
    	//if(msg.hasMoreTokens()) { startingDigest = G.LongToken(msg); }
    }
    private void parse1(Tokenizer msg,int p)
    {
        String cmd = firstAfterIndex(msg);
        player = p;
        op = D.getInt(cmd, MOVE_UNKNOWN);
        
        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Cant parse " + cmd);
        	
        case MOVE_SELECT:
        case MOVE_REPAIR:
        case MOVE_DROP:
        	dest = ManhattanId.valueOf(msg.nextToken());
        	to_color = MColor.valueOf(msg.nextToken());
            to_row = msg.intToken();
            to_index = msg.intToken();
            break;
            

        case MOVE_PICK:
            source = ManhattanId.valueOf(msg.nextToken());
            from_color = MColor.valueOf(msg.nextToken());
            from_row = msg.intToken();
            from_index = msg.intToken();
            break;
      	
        case MOVE_ATTACK:
        case MOVE_FROM_TO:
            source = ManhattanId.valueOf(msg.nextToken());
            from_color = MColor.valueOf(msg.nextToken());
            from_row = msg.intToken();
            from_index = msg.intToken();
            dest = ManhattanId.valueOf(msg.nextToken());
            to_color = MColor.valueOf(msg.nextToken());
            to_row = msg.intToken();
            to_index = -1;
            break;
            
        case MOVE_CONTRIBUTE:
        case EPHEMERAL_CONTRIBUTE:
        	from_color = to_color =  MColor.valueOf(msg.nextToken());
        	from_index = to_index = msg.intToken();
        	break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;
            
        case EPHEMERAL_APPROVE:
        case MOVE_APPROVE:
        case MOVE_RETRIEVE:
        	from_color = to_color = MColor.valueOf(msg.nextToken());
        	break;
        default:
        case MOVE_SKIPMYTURN:

            break;
        }
    }

    private Text icon(commonCanvas v,Object... msg)
    {	double chipScale[] = {1,1.5,-0.0,-0.5};
    	Text m = TextChunk.create(G.concat(msg));
    	if(chip!=null)
    	{
    		m = TextChunk.join(TextGlyph.create("xx", chip, v,chipScale),
    					m);
    	}
    	return(m);
    }
    private Text postIcon(commonCanvas v,ManhattanChip ch,Object... msg)
    {	double chipScale[] = {1.5,1.3,-0.0,-0.2};
    	Text m = TextChunk.create(G.concat(msg));
    	if(chip!=null)
    	{
    		m = TextChunk.join(m,TextGlyph.create("","xx", ch, v,chipScale));
    	}
    	return(m);
    }
    /** construct an abbreviated move string, mainly for use in the game log.  These
     * don't have to be parseable, they're intended only to help humans understand
     * the game record.  The alternative method {@link #shortMoveText} can be implemented
     * to provide colored text or mixed text and icons.
     * 
     * */
    public Text shortMoveText(commonCanvas v,Font f)
    {	//if(gameEvents!=null) { setLineBreak(true); }
        switch (op)
        {
        case MOVE_DONE:
        	setLineBreak(true);
            return TextChunk.create("");
        case MOVE_FROM_TO:
        	{
        	Text ic = icon(v," ");
        	Text dc = dropText(v);
        	return TextChunk.join(ic,dc);
        	}
        case MOVE_ATTACK:
        	Text from =  icon(v," attack ");
        	Text to = dropText(v);
        	return TextChunk.join(from,to);
        	
        case MOVE_SELECT:
        	if(cell!=null)
        	{
        		switch(cell.type)
        		{
        		case BuildingMarket:
        			return postIcon(v,chip,"");
        		default: break;
        		}
        	}
			return TextChunk.create("");

        case MOVE_REPAIR:
        case MOVE_DROP:
        	return dropText(v);
        	
        case MOVE_PICK:
            return icon(v," ");
        case MOVE_CONTRIBUTE:
        case EPHEMERAL_CONTRIBUTE:
        	return icon(v,from_color.name()+" "+from_index);

        default:
        case MOVE_SKIPMYTURN:
        case EPHEMERAL_APPROVE:
        case MOVE_APPROVE:
        case MOVE_RETRIEVE:
            return TextChunk.create(from_color.name()+" "+D.findUniqueTrans(op));

        }
    }
    public Text dropText(commonCanvas v)
    {
       	commonMove n = next;
    	if(n!=null && n.op==MOVE_PICK) { return TextChunk.create(""); }
    	ManhattanChip ch = null;
    	if(cell!=null)
    	{
    	switch(cell.type)
    	{     
    	case Fighter:
    		ch = ManhattanChip.getFighter(to_color);
    		return postIcon(v,ch,"");
    	case Bomber:
    		ch = ManhattanChip.getBomber(to_color);
    		return postIcon(v,ch,"");
    	case Bomb:
    		boolean done = next!=null && next.op==MOVE_DONE;
    		if(done)
    			{ setLineBreak(true); 
    			  ch=cell.chipAtIndex(0); 
    			  return postIcon(v,ch,"bomb");
    			}
    		return TextChunk.create("");
    	case BuildingMarket:
    		ch = cell.chipAtIndex(0);
    		return postIcon(v,ch," buy  ");
    	case Building:
    		ch = cell.chipAtIndex(0);
    		return postIcon(v,ch," use  ");
    	case Worker:
    		switch(cell.rackLocation())
    		{
          	case BuyWithWorker:
          	case BuyWithEngineer:
          		ch = cell.chipAtIndex(0);
          		return postIcon(v,ch," buy ");
    		case MakeUranium:
    			ch = ManhattanChip.Uranium;
    			return postIcon(v,ch," refine ");
    			
    		case MakePlutonium:
    			ch = ManhattanChip.Plutonium;
    			return postIcon(v,ch," refine ");
    		case Mine:
    			ch = ManhattanChip.Yellowcake;
    			return postIcon(v,ch," mine ");
    		case DesignBomb:
    			return postIcon(v,ch," design bombs ");
    		case Espionage:
    			return postIcon(v,ch," espionage ");
    		case MakeMoney:
    			return postIcon(v,ch," get money ");
    		case MakeFighter:
    			return postIcon(v,ch," buy fighters ");
    		case MakeBomber:
    			return postIcon(v,ch," buy bombers ");
    		case Repair:
     			return postIcon(v,ch," repair ");
    		case AirStrike:
    			return postIcon(v,ch," airstrike ");
    		case University:
    			return postIcon(v,ch,"university");
    		default: break;
    		}
    		break;
    	default:
    		break;
    	}}
    	return icon(v,to_color.name()+" "+dest.name()+" "+to_row);

    }
    /** construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and only secondarily human readable */
    public String moveString()
    {
    	return moveString1() /* +" "+startingDigest */;
    }
    private String buildingInfo()
    {
    	ManhattanCell c = cell;
		if(c!=null
				&& (c.type==Type.Building)
				&& (dest==ManhattanId.Building))
		{	ManhattanChip ch = c.chipAtIndex(0);
			if(ch!=null)
				{
				 return " \""+ch.benefit+"\"";
				}	
		}
		return "";
    }
    
    public String moveString1()
    {
		String indx = indexString();
		String opname = indx+D.findUnique(op)+" ";
        // adding the move index as a prefix provides numnbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        case MOVE_REPAIR:
        case MOVE_SELECT:
        case MOVE_DROP:
        	{
        	return G.concat(opname , dest," ",to_color," ",to_row," ",to_index,buildingInfo());
        	}
        case MOVE_PICK:
            return G.concat(opname , source," ",from_color," ",from_row," ",from_index);
        case MOVE_ATTACK:
        case MOVE_FROM_TO:
        	{
        		
        	return G.concat(opname , source, " ",from_color," ",from_row," ",from_index," ",
        			dest," ",to_color," ",to_row,buildingInfo());
        	}
        case MOVE_START:
            return G.concat(indx,"Start P" , player);

        case MOVE_CONTRIBUTE:
        case EPHEMERAL_CONTRIBUTE:
        	return G.concat(opname,from_color," ",from_index);

        case EPHEMERAL_APPROVE:
        case MOVE_APPROVE:
        case MOVE_RETRIEVE:
        case MOVE_SKIPMYTURN:
        	return G.concat(opname , " ",from_color);
        	
        default:
            return G.concat(opname);
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
