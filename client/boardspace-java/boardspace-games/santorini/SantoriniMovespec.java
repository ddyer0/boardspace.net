package santorini;

import java.util.*;

import lib.G;
import online.game.*;
import lib.ExtendedHashtable;

public class SantoriniMovespec extends commonMove implements SantoriniConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
        	"Dome", MOVE_DOME,
        	"Swapwith", MOVE_SWAPWITH,
   			"Move",MOVE_BOARD_BOARD,
   			"select",MOVE_SELECT,
   			"push",MOVE_PUSH,
   			"dropswap", MOVE_DROP_SWAP,
   			"droppush", MOVE_DROP_PUSH);
   }

    SantorId source; // where from/to
	int object;	// object being picked/dropped
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    
    public SantoriniMovespec()
    {
    } // default constructor
    public SantoriniMovespec(int pl,int opc,char co,int ro)
    {	player = pl;
    	source = SantorId.BoardLocation;
    	op = opc;
    	to_col = co;
    	to_row = ro;	
    	from_col = co;
    	from_row = ro;
    }
    // done and simple moves
    public SantoriniMovespec(int pl,int opc)
    {
    	op = opc;
    	player = pl;
    }    
    // select gods
    public SantoriniMovespec(int pl,int opc,int ro)
    {
    	op = opc;
    	player = pl;
    	from_row = ro;
    	source = SantorId.GodsId;
    }
    public SantoriniMovespec(int pl,int opc,char fc,int fr,char co,int ro)
    {	player = pl;
    	source = SantorId.BoardLocation;
    	op = opc;
    	from_row = fr;
    	from_col = fc;
    	to_col = co;
    	to_row = ro;	
    }
    /* constructor */
    public SantoriniMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public SantoriniMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    public boolean Same_Move_P(commonMove oth)
    {
    	SantoriniMovespec other = (SantoriniMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (object == other.object)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(SantoriniMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
		to.object = object;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
    	SantoriniMovespec yto = (to == null) ? new SantoriniMovespec() : (SantoriniMovespec) to;

        // we need yto to be a CarnacMovespec at compile time so it will trigger call to the 
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
        
        case MOVE_SELECT:
        	source = SantorId.GodsId;
        	from_row = SantoriniChip.findGodIndex(msg.nextToken());
        	break;
        case MOVE_SWAPWITH:
        case MOVE_PUSH:
        case MOVE_BOARD_BOARD:			// robot move from board to board
            source = SantorId.BoardLocation;		
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
            object = G.IntToken(msg);       //cupsize
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;
        case MOVE_DOME:        	
        case MOVE_DROPB:
        case MOVE_DROP_SWAP:
        case MOVE_DROP_PUSH:
	       source = SantorId.BoardLocation;
	       to_col = G.CharToken(msg);
	       to_row = G.IntToken(msg);
	       break;

		case MOVE_PICKB:
            source = SantorId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
            object = G.IntToken(msg);

            break;

        case MOVE_PICK:
            source = SantorId.get(msg.nextToken());
            from_col = '@';
            from_row = G.IntToken(msg);
            break;
            
        case MOVE_DROP:
            source = SantorId.get(msg.nextToken());
            to_col = '@';
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
        case MOVE_SELECT:
        	return("select "+SantoriniChip.findGodName(from_row));
        	
        case MOVE_PICKB:
            return (""+from_col + from_row+"-");
        case MOVE_DOME:
		case MOVE_DROPB:
            return (""+to_col + to_row);
		case MOVE_DROP_PUSH:
			return ("push "+source.shortName+object);			
		case MOVE_DROP_SWAP:
			return ("swap "+source.shortName+object);
        case MOVE_DROP:
        case MOVE_PICK:
            return (source.shortName+object);
        case MOVE_PUSH:
        	return("swap "+from_col + from_row+"-"+to_col + to_row);
        case MOVE_SWAPWITH:
        	return("swap "+from_col + from_row+"-"+to_col + to_row);
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
        // adding the move index as a prefix provides numnbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        	
        case MOVE_SELECT:
        	return(opname+ SantoriniChip.findGodName(from_row));

        case MOVE_PICKB:
	        return (opname+ from_col + " " + from_row+" "+object);
        case MOVE_DOME:
		case MOVE_DROPB:
		case MOVE_DROP_SWAP:
		case MOVE_DROP_PUSH:
	        return (opname + to_col + " " + to_row+" "+object);

		case MOVE_SWAPWITH:
		case MOVE_PUSH:
		case MOVE_BOARD_BOARD:
			return(opname+ from_col + " " + from_row+" "+object
					+ " " + to_col + " " + to_row);
        case MOVE_PICK:
            return (opname+source.shortName+ " "+from_row);

        case MOVE_DROP:
             return (opname+source.shortName+ " "+to_row);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
