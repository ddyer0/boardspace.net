/* copyright notice */package takojudo;

import online.game.*;
import java.util.*;

import lib.G;
import lib.ExtendedHashtable;


public class TakojudoMovespec extends commonMove implements TakojudoConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);

 
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
    static final int MOVE_DECLINE_DRAW = 211;
    static final int MOVE_OFFER_DRAW = 212;
    static final int MOVE_ACCEPT_DRAW = 213;

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
        	"Pickb", MOVE_PICKB,
        	"Dropb", MOVE_DROPB,
        	"Offer", MOVE_OFFER_DRAW,
        	"Decline", MOVE_DECLINE_DRAW,
        	"Accept", MOVE_ACCEPT_DRAW,
   			"Move",MOVE_BOARD_BOARD);
   }

	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    TakojudoState state;	// the state of the move before state, for UNDO
    
    public TakojudoMovespec() // default constructor
    {
    }
    public TakojudoMovespec(char from_c,int from_r,char to_c,int to_r,int who)
    {	player = who;
    	from_col = from_c;
    	from_row = from_r;
    	to_col = to_c;
    	to_row = to_r;
    	op = MOVE_BOARD_BOARD;
    }
    public TakojudoMovespec(int opc, int pl)	// constructor for simple moves
    {
    	player = pl;
    	op = opc;
    }
    /* constructor */
    public TakojudoMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public TakojudoMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    public boolean Same_Move_P(commonMove oth)
    {
    	TakojudoMovespec other = (TakojudoMovespec) oth;

        return ((op == other.op) 
				&& (state == other.state)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(TakojudoMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.state = state;
    }

    public commonMove Copy(commonMove to)
    {
    	TakojudoMovespec yto = (to == null) ? new TakojudoMovespec() : (TakojudoMovespec) to;

        // we need yto to be a TakojudoMovespec at compile time so it will trigger call to the 
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
	        
        case MOVE_DROPB:
	       to_col = G.CharToken(msg);
	       to_row = G.IntToken(msg);
	       break;

		case MOVE_PICKB:
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
 
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
            return (""+from_col + from_row+"-");

		case MOVE_DROPB:
            return (""+to_col +  to_row);

        case MOVE_BOARD_BOARD:
        	return(""+from_col + from_row+"-"+to_col +  to_row);
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

        case MOVE_PICKB:
	        return (opname + from_col + " " + from_row);

		case MOVE_DROPB:
	        return (opname+ to_col + " " + to_row);


		case MOVE_BOARD_BOARD:
			return(opname+ from_col + " " + from_row
					+ " " + to_col + " " + to_row);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
