package tintas;

import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import online.game.*;
import lib.ExtendedHashtable;
public class Tintasmovespec extends commonMove implements TintasConstants
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_PAWN = 208;
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
        	"Pawn", MOVE_PAWN);
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
    TintasId dest; // where from/to
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    TintasChip target=null;
    
    public Tintasmovespec()
    {
    } // default constructor
    /* constructor */
    public Tintasmovespec(int opc,int p)
    {
    	player = p;
    	op = opc;
    }
    /* constructor */
    public Tintasmovespec(int opc,TintasCell c,int p)
    {
    	player = p;
    	op = opc;
    	dest = c.rackLocation();
    	to_col = c.col;
    	to_row = c.row;
    }
    /* constructor */
    public Tintasmovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    /** constructor for robot moves.  Having this "binary" constructor is dramatically faster
     * than the standard constructor which parses strings
     */
    public Tintasmovespec(int opc,char col,int row,TintasId what,int who)
    {
    	op = opc;
    	dest = what;
    	to_col = col;
    	to_row = row;
    	player = who;
    }
    /* constructor */
    public Tintasmovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        Tintasmovespec other = (Tintasmovespec) oth;

        return ((op == other.op) 
				&& (dest == other.dest)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (player == other.player));
    }

    public void Copy_Slots(Tintasmovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.dest = dest;
        to.target = target;
    }

    public commonMove Copy(commonMove to)
    {
        Tintasmovespec yto = (to == null) ? new Tintasmovespec() : (Tintasmovespec) to;

        // we need yto to be a Tintasmovespec at compile time so it will trigger call to the 
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
        case MOVE_PAWN:
        		dest = TintasId.BoardLocation;
        
	            to_col = G.CharToken(msg);
	            to_row = G.IntToken(msg);
	            break;

		case MOVE_PICKB:
            dest = TintasId.BoardLocation;
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);

            break;

        case MOVE_DROP:
        case MOVE_PICK:
            dest = TintasId.get(msg.nextToken());
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

        default:

            break;
        }
    }
    public Text shortMoveText(commonCanvas v)
    {
    	Text str = TextChunk.create(shortMoveString());
    	if(target!=null)
    	{
    		str = TextChunk.join(
    				TextGlyph.create("xxx",target,v,new double[]{1.0,1.0,0,-0.1}),
    				str);
    	}
    	return(str);
    	
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
        case MOVE_PICK:
        case MOVE_PICKB:
            return ("");

		case MOVE_DROPB:
		case MOVE_PAWN:
            return (""+to_col + to_row);

        case MOVE_DROP:
            return (D.findUnique(op) + " "+dest.shortName+to_col+to_row);

        case MOVE_DONE:
            return ("");

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
	        return (opname  + to_col + " " + to_row);

		case MOVE_DROPB:
		case MOVE_PAWN:
	        return (opname  + to_col + " " + to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return (opname+dest.shortName+" "+to_col+" "+to_row);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
