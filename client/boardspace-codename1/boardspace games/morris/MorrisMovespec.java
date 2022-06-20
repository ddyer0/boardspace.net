package morris;

import online.game.*;
import java.util.*;

import lib.G;
import lib.ExtendedHashtable;


public class MorrisMovespec extends commonMove implements MorrisConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; 	// pick a chip from a pool
    static final int MOVE_DROP = 205; 	// drop a chip
    static final int MOVE_PICKB = 206; 	// pick from the board
    static final int MOVE_DROPB = 207; 	// drop on the board
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
	static final int MOVE_RACK_BOARD = 211;	// jump and capture
	static final int MOVE_CAPTURE = 212;	// jump and capture

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
 			"Move",MOVE_BOARD_BOARD,
			"Place",MOVE_RACK_BOARD,
			"Capture",MOVE_CAPTURE);
   }

    MorrisId source; // where from/to
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    public MorrisMovespec() // default constructor
    {
    }
    public MorrisMovespec(int opc, int pl)	// constructor for simple moves
    {
    	player = pl;
    	op = opc;
    }
    /* constructor */
    public MorrisMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public MorrisMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    /* constructor for robot moves */
    public MorrisMovespec(int opc,MorrisCell from,MorrisCell to,int who)
    {
    	player = who;
    	op = opc;
    	from_col = from.col;
    	from_row = from.row;
    	to_col = to.col;
    	to_row = to.row;
    }
    public MorrisMovespec(int opc,MorrisId from,MorrisCell to,int who)
    {
    	player = who;
    	op = opc;
    	source = from;
    	to_col = to.col;
    	to_row = to.row;
    }
    public MorrisMovespec(int opc,MorrisCell from,MorrisId to,int who)
    {
    	player = who;
    	op = opc;
    	from_col = from.col;
    	from_row = from.row;
    	source = to;
     }
    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
    	MorrisMovespec other = (MorrisMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(MorrisMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.source = source;
     }

    public commonMove Copy(commonMove to)
    {
    	MorrisMovespec yto = (to == null) ? new MorrisMovespec() : (MorrisMovespec) to;

        // we need yto to be a MorrisMovespec at compile time so it will trigger call to the 
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
        case MOVE_RACK_BOARD:
           	source =  MorrisId.get(msg.nextToken());
	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;

        case MOVE_CAPTURE:
           	source =  MorrisId.get(msg.nextToken());
 	        from_col = G.CharToken(msg);		//to col row
	        from_row = G.IntToken(msg);
	        break;

        case MOVE_BOARD_BOARD:			// robot move from board to board
        	source = MorrisId.BoardLocation;		
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;
	        

		case MOVE_DROPB:
		case MOVE_PICKB:
            source = MorrisId.BoardLocation;
            to_col = from_col = G.CharToken(msg);
            to_row = from_row = G.IntToken(msg);

            break;

        case MOVE_PICK:
            source = MorrisId.get(msg.nextToken());
            break;
            
        case MOVE_DROP:
           source =MorrisId.get(msg.nextToken());
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
        case MOVE_PICKB:
            return (""+from_col + from_row);
		case MOVE_DROPB:
            return (" - "+to_col + to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return (source.shortName);
        case MOVE_CAPTURE:
        	return("x "+from_col+ from_row);
        case MOVE_RACK_BOARD:
        	return(""+to_col+to_row);
        case MOVE_BOARD_BOARD:
        	return(""+from_col + from_row+"-"+to_col + to_row);
        case MOVE_DONE:
            return ("");

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
        // adding the move index as a prefix provides numbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        case MOVE_PICKB:
	        return (opname + from_col + " " + from_row);

		case MOVE_DROPB:
	        return (opname+ to_col + " " + to_row);
	        
		case MOVE_RACK_BOARD:
			return(opname + source.shortName+" " + to_col + " " + to_row);
		
		case MOVE_CAPTURE:
			return(opname + source.shortName+" " + from_col + " " + from_row);

		case MOVE_BOARD_BOARD:
			return(opname + from_col + " " + from_row
					+ " " + to_col + " " + to_row);
        case MOVE_PICK:
            return (opname+source.shortName);

        case MOVE_DROP:
             return (opname+source.shortName);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
  
            return (opname);
        }
    }

    /* standard java method, so we can read moves easily while debugging */
    //public String toString()
    //{
    //    return ("P" + player + "[" + moveString() + "]");
    //}
}
