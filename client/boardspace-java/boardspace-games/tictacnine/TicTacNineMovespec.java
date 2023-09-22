/* copyright notice */package tictacnine;

import online.game.*;
import java.util.*;

import lib.G;
import lib.ExtendedHashtable;


public class TicTacNineMovespec extends commonMove implements TicTacNineConstants
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
 		D.putInt("Move",MOVE_BOARD_BOARD);
   }

    TicId source; // where from/to
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    TictacnineState state;	// the state of the move before state, for UNDO
    
    public TicTacNineMovespec()
    {
    } // default constructor

    /* constructor */
    public TicTacNineMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public TicTacNineMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    public boolean Same_Move_P(commonMove oth)
    {
    	TicTacNineMovespec other = (TicTacNineMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (state == other.state)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(TicTacNineMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.state = state;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
    	TicTacNineMovespec yto = (to == null) ? new TicTacNineMovespec() : (TicTacNineMovespec) to;

        // we need yto to be a TicTacNineMovespec at compile time so it will trigger call to the 
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
            source = TicId.BoardLocation;		
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;
	        
        case MOVE_DROPB:
	       to_col = G.CharToken(msg);
	       to_row = G.IntToken(msg);
	       break;

		case MOVE_PICKB:
            source = TicId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);

            break;

        case MOVE_PICK:
            source = TicId.Chip_Rack;
            from_row = G.IntToken(msg);
            break;
            
        case MOVE_DROP:
            source = TicId.Chip_Rack;
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
        case MOVE_PICKB:
            return ("@"+from_col + from_row);

		case MOVE_DROPB:
            return (to_col + " " + to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return (""+to_row);
        case MOVE_BOARD_BOARD:
        	return("@"+from_col + from_row+" "+to_col + " " + to_row);
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
        String ind = "";

        if (index() >= 0)
        {
            ind += (index() + " ");
        }
        // adding the move index as a prefix provides numnbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        case MOVE_PICKB:
	        return (ind+D.findUnique(op) + " " + from_col + " " + from_row);

		case MOVE_DROPB:
	        return (ind+D.findUnique(op) + " " + to_col + " " + to_row);

		case MOVE_BOARD_BOARD:
			return(ind+D.findUnique(op) + " " + from_col + " " + from_row
					+ " " + to_col + " " + to_row);
        case MOVE_PICK:
            return (ind+D.findUnique(op) + " "+from_row);

        case MOVE_DROP:
             return (ind+D.findUnique(op) +" "+to_row);

        case MOVE_START:
            return (ind+"Start P" + player);

        default:
            return (ind+D.findUnique(op));
        }
    }

    /* standard java method, so we can read moves easily while debugging */
    public String toString()
    {
        return ("P" + player + "[" + moveString() + "]");
    }
}
