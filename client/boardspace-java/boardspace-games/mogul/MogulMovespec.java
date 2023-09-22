/* copyright notice */package mogul;

import online.game.*;
import java.util.*;

import lib.G;
import lib.InternationalStrings;
import lib.ExtendedHashtable;

public class MogulMovespec extends commonMPMove implements MogulConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_SHUFFLE = 206;	// only for the robot to use
    static public InternationalStrings s = null;
    static void setTranslations(InternationalStrings st) { s = st; }
    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
        	"Pick", MOVE_PICK,
        	"Drop", MOVE_DROP,
	    	"Shuffle",MOVE_SHUFFLE);
	    
   }

    MogulId source; // where from/to
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    public MogulMovespec() // default constructor
    {
    }
    public MogulMovespec(int opc, int pl)	// constructor for simple moves
    {
    	player = pl;
    	op = opc;
    }
    /* constructor */
    public MogulMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public MogulMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    public boolean Same_Move_P(commonMove oth)
    {
    	MogulMovespec other = (MogulMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(MogulMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.from_col = from_col;
        to.from_row = from_row;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
    	MogulMovespec yto = (to == null) ? new MogulMovespec() : (MogulMovespec) to;

        // we need yto to be a MogulMovespec at compile time so it will trigger call to the 
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

        
        int opcode = D.getInt(cmd, MOVE_UNKNOWN);
        op = opcode;

        if(op==MOVE_UNKNOWN)
        {
        	MogulId n = MogulId.find(cmd);
        	if(n!=null) { op = n.opcode();  return;} 
        }
        switch (opcode)
        {
        
        case MOVE_UNKNOWN:
        	
        	throw G.Error("Can't parse %s", cmd);
        
        case MOVE_PICK:
            source = MogulId.get(msg.nextToken());
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
            break;
            
        case MOVE_DROP:
            source = MogulId.get(msg.nextToken());
            from_col =  G.CharToken(msg);
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
        case MOVE_DROP:
        case MOVE_PICK:
            return (source.shortName+""+ from_col + from_row);
          
        case MOVE_DONE:
            return ("");
            
         default:
        	 String st = MogulId.opName(op);
        	 if(st!=null) { return(s.get(st)); }
        	 throw G.Error("shortMoveString Not implemented: %s", op);
        case MOVE_EDIT:
        case MOVE_START:
        case MOVE_RESIGN:
        case MOVE_SHUFFLE:
            return (D.findUniqueTrans(op));

        }
    }

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String moveString()
    {
		String ind = indexString();
        // adding the move index as a prefix provides numnbers
        // for the game record and also helps navigate in joint
        // review mode
		switch (op)
		{
		default:
		{
			String n = MogulId.opName(op);
			if(n!=null) { return(ind+n); }
			return (ind+D.findUnique(op));
		}
		case MOVE_DROP:
		case MOVE_PICK:
			return (ind+D.findUnique(op) + " "+source.shortName+ " "+from_col+" " +from_row);

		case MOVE_START:
			return (ind+"Start P" + player);
	            
		}
    }

    /* standard java method, so we can read moves easily while debugging */
    //public String toString()
    //{
    //    return ("P" + player + "[" + moveString() + "]");
    //}
}
