package pushfight;

import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import online.game.*;
import lib.ExtendedHashtable;
public class Pushfightmovespec extends commonMove implements PushfightConstants
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_FROM_TO = 208;	// move on the board
    static final int MOVE_PUSH = 209;		// push move
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
        	"Pickb", MOVE_PICKB,
        	"Dropb", MOVE_DROPB,
        	"Move", MOVE_FROM_TO,
        	"push", MOVE_PUSH);
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
    char from_col;
    int from_row;
    PushfightChip from;
    PushfightChip to;
    public static int instanceCount = 0;
    public Pushfightmovespec()
    {	instanceCount++;
    } // default constructor

    /* constructor */
    public Pushfightmovespec(String str, int p)
    {	instanceCount++;
        parse(new StringTokenizer(str), p);
    }
    /* constructor for simple moves */
    public Pushfightmovespec(int opc,int who)
    {	instanceCount++;
    	op = opc;
    	player = who;
    }
    /* constructor for robot moves */
    public Pushfightmovespec(int opc,PushfightCell from,PushfightCell to,int who)
    {	instanceCount++;
    	from_col = from.col;
    	from_row = from.row;
    	to_col = to.col;
    	to_row = to.row;
    	player = who;
    	op = opc;
    }
    private static CommonMoveStack reserve = new CommonMoveStack();
    static synchronized public void recycle(commonMove m) 
    {	if(reserve.size()<100) { reserve.push(m); }}
    static synchronized public Pushfightmovespec create(int opc,int who)
    {
       	Pushfightmovespec m = reserve.size()==0 ? new Pushfightmovespec() : (Pushfightmovespec)reserve.pop();
        m.op = opc;
        m.player = who;
        return(m);
    }
    static synchronized public Pushfightmovespec create(int opc,PushfightCell from,PushfightCell to,int who)
    {
    	Pushfightmovespec m = reserve.size()==0 ? new Pushfightmovespec() : (Pushfightmovespec)reserve.pop();
    	m.from_col = from.col;
    	m.from_row = from.row;
    	m.to_col = to.col;
    	m.to_row = to.row;
    	m.player = who;
    	m.op = opc;
    	return(m);
    }

    /* constructor */
    public Pushfightmovespec(StringTokenizer ss, int p)
    {	instanceCount++;
        parse(ss, p);
    }

    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        Pushfightmovespec other = (Pushfightmovespec) oth;

        return ((op == other.op) 
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row) 
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(Pushfightmovespec tomove)
    {	super.Copy_Slots(tomove);
        tomove.to_col = to_col;
        tomove.to_row = to_row;
        tomove.from_col = from_col;
        tomove.from_row = from_row;
        tomove.to = to;
        tomove.from = from;
    }

    public commonMove Copy(commonMove to)
    {
        Pushfightmovespec yto = (to == null) ? new Pushfightmovespec() : (Pushfightmovespec) to;

        // we need yto to be a Pushfightmovespec at compile time so it will trigger call to the 
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
        	throw G.Error("Cant parse " + cmd);

        case MOVE_DROPB:
	            to_col = (char)(G.CharToken(msg)+1);
	            to_row = G.IntToken(msg);

	            break;

		case MOVE_PICKB:
            from_col = (char)(G.CharToken(msg)+1);
            from_row = G.IntToken(msg);

            break;

		case MOVE_FROM_TO:
		case MOVE_PUSH:
            from_col = (char)(G.CharToken(msg)+1);
            from_row = G.IntToken(msg);
            to_col = (char)(G.CharToken(msg)+1);
            to_row = G.IntToken(msg);
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
        throw G.Error("Overridden");
    }
    

    public Text fromChunk(commonCanvas v)
    {
       	Text msg = TextChunk.create(""+(char)(from_col-1) + from_row);
        	if(from!=null)
        		{ msg = TextChunk.join(
        				TextGlyph.create("xx", from, v, new double[] {1,1,0,0}),
        				msg
        				);
        		}
        	return(msg);
     }
    public Text toChunk(commonCanvas v)
    {    
		Text msg = TextChunk.create(" - "+(char)(to_col-1) + to_row);
		if(to!=null)
		{
			msg = TextChunk.join(
					msg,
    				TextGlyph.create("xx", to, v, new double[] {1,1,0,0})
    				);
		}
        return (msg);
    }
    /**
     * shortMoveText lets you return colorized text or mixed text and graphics.
     * @see lib.Text
     * @see lib.TextGlyph 
     * @see lib.TextChunk
     * @param v
     * @return a Text object
     */
    public Text shortMoveText(commonCanvas v)
    {
        switch (op)
        {
        case MOVE_PICKB:
        	return fromChunk(v);
 		case MOVE_DROPB:
 			return toChunk(v);
 			
 		case MOVE_PUSH:
		case MOVE_FROM_TO:
			return TextChunk.join(
					fromChunk(v),
					toChunk(v));
			
        case MOVE_DONE:
            return TextChunk.create("");

        default:
            return TextChunk.create(D.findUnique(op));

        }
    }
    
    /** construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and only secondarily human readable */
    public String moveString()
    {
		String ind = (index() >= 0) ? (index() + " ") : "";
		String opname = D.findUnique(op)+" ";
        // adding the move index as a prefix provides numnbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        case MOVE_PICKB:
	        return (ind+opname + (char)(from_col-1) + " " + from_row);

		case MOVE_DROPB:
	        return (ind+opname + (char)(to_col-1) + " " + to_row);
	       
		case MOVE_PUSH:
		case MOVE_FROM_TO:
			return(ind+opname+(char)(from_col-1) + " " + from_row+" "+(char)(to_col-1) + " " + to_row);


        case MOVE_START:
            return (ind+"Start P" + player);

        default:
            return (ind+opname);
        }
    }

}
