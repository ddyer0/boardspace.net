package volo;

import java.util.*;

import lib.G;
import online.game.*;
import lib.ExtendedHashtable;

public class VoloMovespec extends commonMove implements VoloConstants
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);

    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
        	"Slide",MOVE_SLIDE,
        	"Select",MOVE_SELECT);
     }
    //
    // variables to identify the move
    VoloId source; // where from/to
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    char from_col;	// starting point for move
    int from_row;
    VoloId direction;	// direction of the line of birds
    int nchips;		// number of birds in the line
    
    public VoloMovespec()// default constructor
    {
    } 
    // constructor for robot drop moves
    public VoloMovespec(int opc,char c,int r,int pl)
    {	from_col = to_col = c;
    	op = opc;
    	from_row = to_row = r;
    	player = pl;
    	source = VoloId.BoardLocation;
    }
    public VoloMovespec(int opc,int pl)
    {	player = pl;
    	op = opc;
    }
    // constructor for slide moves
    public VoloMovespec(int opc,char c,int r,VoloId dir,int n,char to_c,int to_r,int pla)
    {	op = opc;
    	player = pla;
    	to_col = to_c;
    	to_row = to_r;
    	from_col = c;
    	from_row = r;
    	direction = dir;
    	nchips = n;
    }
    /* constructor */
    public VoloMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public VoloMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    public boolean Same_Move_P(commonMove oth)
    {
        VoloMovespec other = (VoloMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_col == other.from_col)
				&& (from_row == other.from_row)
				&& (nchips == other.nchips)
				&& (direction == other.direction)
				&& (player == other.player));
    }

    public void Copy_Slots(VoloMovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.source = source;
        to.direction = direction;
        to.nchips = nchips;
        to.from_row = from_row;
        to.from_col = from_col;
    }

    public commonMove Copy(commonMove to)
    {
        VoloMovespec yto = (to == null) ? new VoloMovespec() : (VoloMovespec) to;

        // we need yto to be a VoloMovespec at compile time so it will trigger call to the 
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
        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Can't parse %s", cmd);
        case MOVE_DROPB:
	            source = VoloId.BoardLocation;
	            to_col = G.CharToken(msg);
	            to_row = G.IntToken(msg);

	            break;

		case MOVE_PICKB:
		case MOVE_SELECT:
            source = VoloId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);

            break;

        case MOVE_DROP:
        case MOVE_PICK:
            source = VoloId.get(msg.nextToken());

            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;
        case MOVE_SLIDE:
        	source = VoloId.BoardLocation;
        	from_col = G.CharToken(msg);
        	from_row = G.IntToken(msg);
        	direction = VoloId.get(msg.nextToken());
        	nchips = G.IntToken(msg);
        	to_col = G.CharToken(msg);
        	to_row = G.IntToken(msg);
        	break;
        default:
            break;
        }
    }

    /** construct an abbreviated move string, mainly for use in the game log.  These
     * don't have to be parseable, they're intended only to help humans understand
     * the game record.
     * */
    public String shortMoveString()
    {
        switch (op)
        {
        case MOVE_SELECT:
            return ("" + from_col + " " + from_row);

        case MOVE_PICKB:
		case MOVE_DROPB:
            return (""+to_col + " " + to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return (source.shortName);

        case MOVE_DONE:
            return ("");
            
        case MOVE_SLIDE:
        	return(D.findUnique(op)+" "+from_col+from_row+"-"+direction.shortName+"-"+nchips+" "+to_col+to_row);

        default:
            return (D.findUnique(op));

        }
    }

    /** construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and only secondarily human readable */
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
        case MOVE_SELECT:
	        return (opname+ from_col + " " + from_row);

		case MOVE_DROPB:
	        return (opname+to_col + " " + to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return (opname+source.shortName);

        case MOVE_START:
            return (indx+"Start P" + player);

        case MOVE_SLIDE:
        	return(opname+from_col+" "+from_row+" "+direction.shortName+" "+nchips+" "+to_col+" "+to_row);
        default:
            return (D.findUnique(op));
        }
    }

}
