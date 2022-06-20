package oneday;

import online.game.*;
import java.util.*;

import lib.G;
import oneday.OnedayBoard.OnedayLocation;
import lib.ExtendedHashtable;


public class OnedayMovespec extends commonMPMove implements OnedayConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_DRAW = 206;	// reveal the draw card
    static final int MOVE_TO_DISCARD = 208;	// move directly to discard
    static final int MOVE_TO_RACK = 209;	// move from rack to board
    static final int MOVE_TO_RACK_AND_DISCARD = 210;	// move board to board
	static final int MOVE_SHUFFLE = 211;		// shuffle the deck (robot only)
	static final int EPHEMERAL_PICK = 212;
	static final int EPHEMERAL_DROP = 213;
	static final int EPHEMERAL_TO_RACK = 214;
	static final int NORMALSTART = 215;
	static final int MOVE_WALK = 216;		// walk between stations
	static final int MOVE_EXIT = 217;		// exit the train at the next stop
	static final int MOVE_BOARD = 218;		// board the next train
	static final int MOVE_RUN = 219;		// run the simulator to time n
	
	// designate moves that are ephemeral and will be replaced
	public boolean isEphemeral()
	{
		switch(op)
		{
		case EPHEMERAL_PICK:
		case EPHEMERAL_DROP:
		case EPHEMERAL_TO_RACK:
			return(true);
		default: return(false);
		}
	}
	// designatge moves that are never transmitted to others
	public boolean isTransmitted()
	{
		switch(op)
		{
		case EPHEMERAL_DROP: 
		case EPHEMERAL_PICK: return(false);
		default: return(true);
		}
	}
    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
        	"Pick", MOVE_PICK,
        	"EPick",EPHEMERAL_PICK,
        	"Edrop",EPHEMERAL_DROP,
        	"ERack",EPHEMERAL_TO_RACK,
        	"Drop", MOVE_DROP,
			"Shuffle",MOVE_SHUFFLE,
			"Draw",MOVE_DRAW,
 			"ToRack",MOVE_TO_RACK,
			"ToRackDis",MOVE_TO_RACK_AND_DISCARD,
			"NormalStart",NORMALSTART,
			"ToDis",MOVE_TO_DISCARD,
			"Walk",MOVE_WALK,
			"Board", MOVE_BOARD,
			"Exit", MOVE_EXIT,
			"Run",MOVE_RUN);

   }

    OneDayId source; // where from/to
	char from_col; 	//for from-to moves, the source column
	int from_row; 	// for from-to moves, the source row
    char to_col; 	// for from-to moves, the destination column
    int to_row; 	// for from-to moves, the destination row
    int discard;	// row to discard to
    long timeStep;
    boolean exposed = false;	// exposed card was replaced
    OnedayLocation location = null;

    public OnedayMovespec() // default constructor
    {
    }
    public OnedayMovespec(int opc, int pl)	// constructor for simple moves
    {	
    	player = pl;
    	op = opc;
    }
    /* constructor */
    public OnedayMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public OnedayMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    // constructor for MOVE_TO_RACK
    public OnedayMovespec(int opc,char col,int row,int pl)
    {
    	op = opc;
    	to_col = col;
    	to_row = row;
    	player = pl;
    }
    
    // constructor for MOVE_TO_RACK_AND_DISCARD
    public OnedayMovespec(int opc,OneDayId src,char col,int row,char tc,int tr,int dis,int pl)
    {
    	op = opc;
    	source = src;
    	from_col = col;
    	from_row = row;
    	to_col = tc;
    	to_row = tr;
    	discard = dis;
    	player = pl;
    }
    
    // constructor for MOVE_TO_DISCARD
    public OnedayMovespec(int opc,OneDayId src,char col,int row,int dis,int pl)
    {
    	op = opc;
    	source = src;
    	from_col = col;
    	from_row = row;
     	discard = dis;
    	player = pl;
    }    
    
    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
    	OnedayMovespec other = (OnedayMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (exposed==other.exposed)
				&& (discard == other.discard)
				&& (location == other.location)
				&& (timeStep == other.timeStep)
				&& (player == other.player));
    }

    public void Copy_Slots(OnedayMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.discard = discard;
        to.location = location;
        to.source = source;
        to.exposed = exposed;
        to.timeStep = timeStep;
    }

    public commonMove Copy(commonMove to)
    {
    	OnedayMovespec yto = (to == null) ? new OnedayMovespec() : (OnedayMovespec) to;

        // we need yto to be a OnedayMovespec at compile time so it will trigger call to the 
        // local version of Copy_Slots
        Copy_Slots(yto);

        return (yto);
    }

    /* parse a string into the state of this move.  Remember that we're just parsing, we can't
     * refer to the state of the board or the game.
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
        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Can't parse %s", cmd);
        	
        case MOVE_RUN:
        	{
        		timeStep = Long.parseLong(msg.nextToken());
        	}
        	break;
        case MOVE_BOARD:
        case MOVE_EXIT:
        	timeStep = Long.parseLong(msg.nextToken());
        	break;
        case MOVE_WALK:
        	{
        	timeStep = Long.parseLong(msg.nextToken());
        	Station sta = Station.getStation(msg.nextToken());
        	Line line = Line.getLine(msg.nextToken());
        	Platform pl = sta.getPlatform(line,msg.nextToken());
        	location = pl;
        	}
        	break;
        case EPHEMERAL_TO_RACK: 
        case MOVE_TO_RACK:	// a robot move from the rack to the board
   	        to_col = G.CharToken(msg);			// destination cell col
	        to_row = G.IntToken(msg);  			// destination cell row
	        break;
	        
        case MOVE_TO_DISCARD:
            source = OneDayId.get(msg.nextToken());	
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
 	        discard = G.IntToken(msg);   	
        	break;
        case MOVE_TO_RACK_AND_DISCARD:			// robot move from board to board
            source = OneDayId.get(msg.nextToken());	
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        discard = G.IntToken(msg);
	        break;
	        
        case MOVE_PICK:
        case EPHEMERAL_PICK:
            source = OneDayId.get(msg.nextToken());
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
            break;
            
        case MOVE_DROP:
        case EPHEMERAL_DROP:
        	source = OneDayId.get(msg.nextToken());
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;
        default:

            break;
        }
    }

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String shortMoveString()
    {
        switch (op)
        {
        case MOVE_RUN: return("");
        case MOVE_WALK:
        	return(D.findUnique(op)
        		+" "+location.getStation().getUid()
        		+" "+location.getLine().getUid()
        		+" "+location.getPlatform().getUid());

         case MOVE_DROP:
         case EPHEMERAL_DROP:
            return (source.shortName+" "+(to_row+1));
        case MOVE_PICK:
        case EPHEMERAL_PICK:
        	switch(source)
        	{
        	case DrawPile:	return("Draw > ");
        	case StartingPile: return("Draw > ");
        	default:
        		return (source.shortName+" "+from_row+" > ");
        	}
            
        case EPHEMERAL_TO_RACK:
        case MOVE_TO_RACK:
        	return("@ "+to_col + " " + to_row);
        case MOVE_DRAW:
        	return("Draw");
        case MOVE_TO_RACK_AND_DISCARD:
        	return( ((source==OneDayId.DrawPile) ? "" : "S"+(from_row+1))
        			+" > "+ to_row+" > S"+(discard+1));
        case MOVE_TO_DISCARD:
        	return( ((source==OneDayId.DrawPile) ? "" : "S"+(from_row+1))+" > S"+(discard+1));
       case MOVE_DONE:
            return ("");
        case NORMALSTART:
        	return("Start regular play");
        default:
            return (D.findUniqueTrans(op));

        }
    }

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String moveString()
    {
		String indx = indexString();
		String opname = indx+D.findUnique(op)+" ";
        // adding the move index as a prefix provides numnbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        case MOVE_RUN:
        	return(opname+timeStep);
        	
        case MOVE_BOARD:
        case MOVE_EXIT:
        	return(opname+timeStep);
        	
        case MOVE_WALK:
        	
        	return(opname
        				+ timeStep
        				+" "+location.getStation().getUid()
        				+" "+location.getLine().getUid()
        				+" "+location.getPlatform().getUid());
        	
        case EPHEMERAL_PICK:
        case MOVE_PICK:
  	        return (opname + source.shortName+ " " + from_col + " " + from_row);
		case MOVE_DROP:
		case EPHEMERAL_DROP:
	        return (opname + source.shortName+" " + to_col + " " + to_row);
		case EPHEMERAL_TO_RACK:
		case MOVE_TO_RACK:
			return(opname + to_col + " " + to_row);
		case MOVE_TO_RACK_AND_DISCARD:
			return(opname + source.shortName+" " + from_col + " " + from_row
					+ " " + to_col + " " + to_row+ " "+discard);
		case MOVE_TO_DISCARD:
			return(opname+ source.shortName+" " + from_col + " " + from_row
					+" "+discard);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:

            return (opname);
        }
    }

 
}
