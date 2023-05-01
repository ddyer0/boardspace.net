package palago;

import online.game.*;

import java.util.*;

import lib.G;
import lib.ExtendedHashtable;


public class Palagomovespec extends commonMove implements PalagoConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB);
    }

    PalagoId source; // where from/to
    int object;
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    PalagoState state;	// the state of the move before state, for UNDO
    int undoInfo;
    PalagoCell dstack = null;
    public Palagomovespec()
    {
    } // default constructor
    public Palagomovespec(int pl,int opc,char co,int ro,int cel)
    {	player = pl;
    	op = opc;
    	to_row = ro;
    	to_col = co;
    	object = cel;
    }
    /* constructor */
    public Palagomovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public Palagomovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    public Palagomovespec(int pl,int opcode) { player=pl; op=opcode; }
    public boolean Same_Move_P(commonMove oth)
    {
        Palagomovespec other = (Palagomovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (object == other.object)
				&& (state == other.state)
				&& (undoInfo==other.undoInfo)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (player == other.player));
    }

    public void Copy_Slots(Palagomovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.object = object;
        to.state = state;
        to.undoInfo = undoInfo;
        to.dstack = dstack;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
        Palagomovespec yto = (to == null) ? new Palagomovespec() : (Palagomovespec) to;

        // we need to to be a Movespec at compile time so it will trigger call to the 
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

        case MOVE_DROPB:
	            source = PalagoId.EmptyBoard;
				to_col = G.parseCol(msg);
	            to_row = G.IntToken(msg);
	            object = G.IntToken(msg);

	            break;

		case MOVE_PICKB:
            source = PalagoId.BoardLocation;
            to_col = G.parseCol(msg);;
            to_row = G.IntToken(msg);

            break;

        case MOVE_DROP:
        	source = PalagoId.ChipPool;
        	break;
        case MOVE_PICK:
            source = PalagoId.ChipPool;
            to_row = G.IntToken(msg);
            to_col = '@';
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
            return (D.findUnique(op) +" " + G.printCol(to_col) + to_row);

		case MOVE_DROPB:
            return (""+G.printCol(to_col) + to_row+" "+object);
        case MOVE_PICK:
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
	        return (opname+ G.printCol(to_col) + " " + to_row);

		case MOVE_DROPB:
	        return (opname+ G.printCol(to_col) + " " + to_row+" "+object);

         case MOVE_PICK:
            return (opname+to_row);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }
 
 }
