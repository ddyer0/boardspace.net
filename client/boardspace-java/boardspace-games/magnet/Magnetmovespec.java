package magnet;

import java.util.*;

import lib.G;
import magnet.MagnetConstants.MagnetId;
import online.game.*;
import lib.ExtendedHashtable;
public class Magnetmovespec extends commonMove 
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_FROM_TO = 208;  // move from rack to board
    static final int MOVE_PROMOTE = 209;	// promote the piece
    static final int MOVE_DEMOTE = 210;	// demote the piece
    static final int MOVE_SELECT = 211;	// select first mover
    static final int MOVE_RANDOM = 212;
	static final int EPHEMERAL_PICK = 412;
	static final int EPHEMERAL_MOVE = 414;
	static final int EPHEMERAL_PICKB = 415;
	static final int NORMALSTART = 416;
	static final int EPHEMERAL_DONE = 417;

	public boolean isEphemeral() { return(op>=400); }
	
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Select", MOVE_SELECT,
        	"Dropb", MOVE_DROPB,
        	"Promote", MOVE_PROMOTE,
        	"Demote", MOVE_DEMOTE,
        	"Move", MOVE_FROM_TO,
        	"RandomMove", MOVE_RANDOM,
        	"ePick", EPHEMERAL_PICK,
        	"ePickb", EPHEMERAL_PICKB,
        	"eMove", EPHEMERAL_MOVE,
        	"NormalStart", NORMALSTART,
        	"eDone", EPHEMERAL_DONE);
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
    MagnetId source; // where from/to
    char from_col;
    int from_row;
    MagnetId dest;
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    String movements = "";
    public Magnetmovespec()
    {
    } // default constructor

    /* constructor */
    public Magnetmovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    /** constructor for robot moves.  Having this "binary" constructor is dramatically faster
     * than the standard constructor which parses strings
     */
    public Magnetmovespec(int opc,char col,int row,MagnetId what,int who)
    {
    	op = opc;
    	source = what;
    	to_col = col;
    	to_row = row;
    	player = who;
    }
    /* constructor */
    public Magnetmovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    /* constructor for robot placement moves */
    public Magnetmovespec(int opc,MagnetCell from,MagnetCell to,int who)
    {	op = opc;
    	player = who;
    	source = from.rackLocation();
    	from_col = from.col;
    	from_row = from.row;
    	dest = to.rackLocation();
    	to_col = to.col;
    	to_row = to.row;
    }
    /* constructor for robot select moves */
    public Magnetmovespec(int opc,MagnetCell from,int who)
    {	op = opc;
    	player = who;
    	dest = source = from.rackLocation();
    	to_col = from_col = from.col;
    	to_row = from_row = from.row;
    }
    /* constructor for done, resign etc. */
    public Magnetmovespec(int opc,int who)
    {
    	op = opc;
    	player = who;
    }
    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        Magnetmovespec other = (Magnetmovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (dest == other.dest)
				&& (from_col == other.from_col)
				&& (from_row == other.from_row)
				&& (player == other.player));
    }

    public void Copy_Slots(Magnetmovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.source = source;
        to.dest = dest;
        to.from_col = from_col;
        to.from_row = from_row;
        to.movements = movements;
    }

    public commonMove Copy(commonMove to)
    {
        Magnetmovespec yto = (to == null) ? new Magnetmovespec() : (Magnetmovespec) to;

        // we need yto to be a Magnetmovespec at compile time so it will trigger call to the 
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
        case MOVE_FROM_TO:
        case MOVE_RANDOM:
        case EPHEMERAL_MOVE:
        	source = MagnetId.get(msg.nextToken());
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);

        	dest = MagnetId.get(msg.nextToken());
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);
            break;
            
        case MOVE_PROMOTE:
        case MOVE_DEMOTE:
        case MOVE_DROPB:
        		dest = MagnetId.BoardLocation;
	            to_col = G.CharToken(msg);
	            to_row = G.IntToken(msg);
	            break;

        case MOVE_SELECT:
		case MOVE_PICKB:
		case EPHEMERAL_PICKB:
            source = MagnetId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);

            break;

        case MOVE_DROP:
        	dest = MagnetId.get(msg.nextToken());
            to_row = G.IntToken(msg);
            break;

        case MOVE_PICK:
        case EPHEMERAL_PICK:
            source = MagnetId.get(msg.nextToken());
            from_row = G.IntToken(msg);
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

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
        case MOVE_SELECT:
        case MOVE_PICKB:
        case EPHEMERAL_PICKB:
            return ("");

        case MOVE_FROM_TO:
        case MOVE_RANDOM:
        case EPHEMERAL_MOVE:
        	return (((dest==MagnetId.BoardLocation)?"":dest.shortName)+ " " + to_col +  to_row
        	+ movements);

		case MOVE_DROPB:
		case MOVE_DEMOTE:
		case MOVE_PROMOTE:
            return (" +"+to_col + to_row+movements);

        case MOVE_DROP:
            return (D.findUnique(op) + " "+dest.shortName+to_row+movements);

        case EPHEMERAL_PICK:
        case MOVE_PICK:
            return ("");

        case MOVE_DONE:
            return ("");

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
        case EPHEMERAL_PICKB:
        case MOVE_SELECT:
 	        return (opname  + from_col + " " + from_row);

        case MOVE_FROM_TO:
        case MOVE_RANDOM:
        case EPHEMERAL_MOVE:
        	return (opname 
        			+ source.shortName+" " + from_col + " " + from_row
        			+" "+dest.shortName+ " " + to_col + " " + to_row);
        	
        case MOVE_PROMOTE:
        case MOVE_DEMOTE:
		case MOVE_DROPB:
	        return (opname + to_col + " " + to_row);

        case MOVE_DROP:
            return (opname +dest.shortName+" "+to_row);
        case EPHEMERAL_PICK:
        case MOVE_PICK:
            return (opname +source.shortName+" "+from_row);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }
  
}
