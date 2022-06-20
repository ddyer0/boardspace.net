package mbrane;

import java.util.*;

import lib.G;
import mbrane.MbraneConstants.MbraneId;
import online.game.*;
import lib.ExtendedHashtable;
public class Mbranemovespec extends commonMove
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_PLACEBLACK = 208;
    static final int MOVE_PLACERED = 209;
    static final int MOVE_MOVE = 210;
    static final int MOVE_SCORESTEP = 211;
    static final int MOVE_STARTRESOLUTION = 212;
    static final int MOVE_OKRESOLUTION = 213;
    static final int MOVE_NORESOLUTION = 214;
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
        	"PlaceRed", MOVE_PLACERED,
        	"PlaceBlack",MOVE_PLACEBLACK,
        	"Move",MOVE_MOVE,
        	"ScoreStep",MOVE_SCORESTEP,
        	MbraneId.StartResolution.name(),MOVE_STARTRESOLUTION,
        	MbraneId.OkResolution.name(),MOVE_OKRESOLUTION,
        	MbraneId.NoResolution.name(),MOVE_NORESOLUTION);
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
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    int from_row;
    public Mbranemovespec()
    {
    } // default constructor

    /* constructor */
    public Mbranemovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    public Mbranemovespec(int opc,int who)
    {
    	op = opc;
    	player = who;
    }
    /** constructor for robot moves.  Having this "binary" constor is dramatically faster
     * than the standard constructor which parses strings
     */
    public Mbranemovespec(int opc,MbraneCell from,MbraneCell to,int who)
    {
    	op = opc;
    	from_row = from.row;
    	to_col = to.col;
    	to_row = to.row;
    	player = who;
    }
    /* constructor for scoring */
    public Mbranemovespec(int opc,int torow,int who)
    {
    	op = opc;
    	to_row = torow;
    	player = who;
    }
    /* constructor */
    public Mbranemovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        Mbranemovespec other = (Mbranemovespec) oth;

        return ((op == other.op) 
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row==other.from_row)
				&& (player == other.player));
    }

    public void Copy_Slots(Mbranemovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_row = from_row;
    }

    public commonMove Copy(commonMove to)
    {
        Mbranemovespec yto = (to == null) ? new Mbranemovespec() : (Mbranemovespec) to;

        // we need yto to be a Mbranemovespec at compile time so it will trigger call to the 
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
        	throw G.Error("Cant parse %s", cmd);
        case MOVE_MOVE:
        		from_row = G.IntToken(msg);
        		to_col = G.CharToken(msg);
        		to_row = G.IntToken(msg);
        		break;
        		
        case MOVE_DROPB:
	            to_col = G.CharToken(msg);
	            to_row = G.IntToken(msg);

	            break;

		case MOVE_PICKB:
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);

            break;

        case MOVE_DROP:
        case MOVE_PICK:
            to_row = G.IntToken(msg);
            to_col = '@';
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;
        case MOVE_SCORESTEP:
        	to_row = G.IntToken(msg);
        	break;
        default:
            break;
        }
    }

    /** construct an abbreviated move string, mainly for use in the game log.  These
     * don't have to be parseable, they're intended only to help humans understand
     * the game record.  The alternative method {@link #shortMoveText} can be implemented
     * to provide colored text or mixed text and icons.
     * 
     * */
    public String shortMoveString()
    {
        switch (op)
        {
        case MOVE_PICKB:
            return (D.findUnique(op) +" " + to_col + " " + to_row);

		case MOVE_DROPB:
            return (""+to_col + to_row);

        case MOVE_DROP:
        case MOVE_PICK:
        	return(""+to_row+"-");
        	
        case MOVE_DONE:
            return ("");
        case MOVE_MOVE:
        	return(""+from_row+"-"+to_col+to_row);
        case MOVE_SCORESTEP:
        	return("ScoreStep");
        default:
            return (D.findUniqueTrans(op));
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
	        return (opname + to_col + " " + to_row);

		case MOVE_DROPB:
	        return (opname + to_col + " " + to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return (opname +to_row);

        case MOVE_MOVE:
            return (opname+from_row+" "+to_col+" "+to_row);

        case MOVE_START:
            return (indx+"Start P" + player);
        case MOVE_SCORESTEP:
        	return(opname+to_row);
        default:
            return (opname);
        }
    }

}
