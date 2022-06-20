package truchet;

import online.game.*;

import java.util.*;

import lib.G;
import lib.ExtendedHashtable;



public class TruMovespec extends commonMove implements TruConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D);
        D.putInt("Pick", MOVE_PICK);
        D.putInt("Pickb", MOVE_PICKB);
        D.putInt("Drop", MOVE_DROP);
        D.putInt("Dropb", MOVE_DROPB);
        D.putInt("Flip",MOVE_FLIP);
        D.putInt("Split",MOVE_SPLIT);
        D.putInt("Merge",MOVE_MERGE);
 		D.putInt("Move",MOVE_BOARD_BOARD);
		D.putInt("MoveSplit",MOVE_AND_SPLIT);
		D.putInt("MoveMerge",MOVE_AND_MERGE);
   }

    TruId object;	// what was picked
    int chip =0;
	String splitInfo;	// specifier for split/join
	int undoInfo = 0;
	TruCell undoFocus = null;
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    TruchetState state;	// the state of the move before state, for UNDO
    
    public TruMovespec()
    {
    } // default constructor

    /* constructor */
    public TruMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public TruMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    /* constructor for pass and resign moves */
    public TruMovespec(int opc,int p)
    {
    	op = opc;
    	player = p;
     }
    /* constructor for "flip" moves */
    public TruMovespec(char col,int row,int p)
    {
    	op = MOVE_FLIP;
    	player = p;
    	from_row = to_row = row;
    	from_col = to_col = col;
    }
    /* constructor for "move" moves */
    public TruMovespec(char col,int row,char tocol,int torow,int p)
    {
    	op = MOVE_BOARD_BOARD;
    	player = p;
    	from_row =  row;
    	from_col =  col;
    	to_col = tocol;
    	to_row = torow;
    }
    /* constructor for split and merge moves */
    public TruMovespec(int opcode,char col,int row,String info,int p)
    {
    	op = opcode;
    	player = p;
    	from_row = to_row = row;
    	from_col = to_col = col;
    	splitInfo = info;
   }  
    public boolean Same_Move_P(commonMove oth)
    {
    	TruMovespec other = (TruMovespec) oth;

        return ((op == other.op) 
				&& (object == other.object)
				&& ((splitInfo==null)?(other.splitInfo==null):splitInfo.equals(other.splitInfo))
				&& (undoInfo == other.undoInfo)
				&& (undoFocus==other.undoFocus)
				&& (chip==other.chip)
				&& (state == other.state)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(TruMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
		to.object = object;
		to.splitInfo = splitInfo;
		to.undoInfo = undoInfo;
		to.undoFocus= undoFocus;
		to.chip = chip;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.state = state;
    }

    public commonMove Copy(commonMove to)
    {
    	TruMovespec yto = (to == null) ? new TruMovespec() : (TruMovespec) to;

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
        
        case MOVE_BOARD_BOARD:			// robot move from board to board
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;
	        
        case MOVE_AND_SPLIT:
        case MOVE_AND_MERGE:
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	       	splitInfo = msg.nextToken();
	        break;

        case MOVE_SPLIT:
        case MOVE_MERGE:
        	to_col = from_col = G.CharToken(msg);
        	to_row = from_row = G.IntToken(msg);
        	splitInfo = msg.nextToken();
        	break;
        case MOVE_DROPB:
	       to_col = G.CharToken(msg);
	       to_row = G.IntToken(msg);
	       break;
        case MOVE_FLIP:
             from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
            break;
		case MOVE_PICKB:
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
            chip = G.IntToken(msg);

            break;

        case MOVE_PICK:
            object = TruId.get(msg.nextToken());
            break;
            
        case MOVE_DROP:
            object = TruId.get(msg.nextToken());
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
        	{
            return(""+from_col+from_row);
        	}
        case MOVE_FLIP:
        { char pcol = (char)(from_col-1);
          int prow = from_row-1;
           return("^"+pcol+prow+from_col+from_row);
         }

		case MOVE_DROPB:
            return ("-"+to_col + to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return (object.shortName);
        case MOVE_AND_SPLIT:
        case MOVE_AND_MERGE:
        	return(""+from_col + from_row+" "+to_col + to_row+((op==MOVE_SPLIT)?">":"<")+splitInfo);
        	
        case MOVE_SPLIT:
        case MOVE_MERGE:
        	return(""+to_col +  to_row+((op==MOVE_SPLIT)?">":"<")+splitInfo);
        	
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
 
        case MOVE_AND_SPLIT:
        case MOVE_AND_MERGE:
			return(opname+ from_col + " " + from_row+" "+ to_col + " " + to_row+" "+splitInfo);
        case MOVE_SPLIT:
        case MOVE_MERGE:
        	return(opname + to_col + " "+to_row + " " + splitInfo);
        case MOVE_FLIP:
        	 return (opname + from_col + " " + from_row);
        	 
        case MOVE_PICKB:
	        return (opname + from_col + " " + from_row+" "+chip);

		case MOVE_DROPB:
	        return (opname+ to_col + " " + to_row+" "+chip);

		case MOVE_BOARD_BOARD:
			return(opname+ from_col + " " + from_row+" "+ to_col + " " + to_row);
        case MOVE_PICK:
            return (opname+object.shortName);

        case MOVE_DROP:
             return (opname+object.shortName);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
