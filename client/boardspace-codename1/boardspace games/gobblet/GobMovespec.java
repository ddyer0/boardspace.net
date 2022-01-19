package gobblet;

import online.game.*;

import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.ExtendedHashtable;

public class GobMovespec extends commonMove implements GobConstants
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
        D.putInt("W", GobbletId.White_Chip_Pool.IID());
        D.putInt("B", GobbletId.Black_Chip_Pool.IID());
		D.putInt("Move",MOVE_BOARD_BOARD);
		D.putInt("OnBoard",MOVE_RACK_BOARD);
   }

    GobbletId source; // where from/to
	int object;	// object being picked/dropped
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    GobbletState state;	// the state of the move before state, for UNDO
    GobCup moved=null;
    GobCup covered=null;
    public GobMovespec()
    {
    } // default constructor

    /* constructor */
    public GobMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public GobMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    public boolean Same_Move_P(commonMove oth)
    {
    	GobMovespec other = (GobMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (object == other.object)
				&& (state == other.state)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(GobMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
		to.object = object;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.state = state;
        to.source = source;
        to.moved = moved;
        to.covered = covered;
    }

    public commonMove Copy(commonMove to)
    {
    	GobMovespec yto = (to == null) ? new GobMovespec() : (GobMovespec) to;

        // we need yto to be a GobMovespec at compile time so it will trigger call to the 
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
        
        case MOVE_RACK_BOARD:	// a robot move from the rack to the board
            source = GobbletId.get(D.getInt(msg.nextToken()));	// white rack or black rack
            from_col = '@';						// always
            from_row = G.IntToken(msg);			// index into the rack
            object = G.IntToken(msg);			// cup size
 	        to_col = G.CharToken(msg);			// destination cell col
	        to_row = G.IntToken(msg);  			// destination cell row
	        break;
	        
        case MOVE_BOARD_BOARD:			// robot move from board to board
            source = GobbletId.BoardLocation;		
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
            object = G.IntToken(msg);       //cupsize
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;
	        
        case MOVE_DROPB:
	       source = GobbletId.BoardLocation;
	       to_col = G.CharToken(msg);
	       to_row = G.IntToken(msg);
	       break;

		case MOVE_PICKB:
            source = GobbletId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
            object = G.IntToken(msg);

            break;

        case MOVE_PICK:
            source = GobbletId.get(D.getInt(msg.nextToken()));
            from_col = '@';
            from_row = G.IntToken(msg);
            object = G.IntToken(msg);
            break;
            
        case MOVE_DROP:
        	source = GobbletId.get(D.getInt(msg.nextToken()));
            to_col = '@';
            to_row = G.IntToken(msg);
            object = G.IntToken(msg);
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

        default:
            break;
        }
    }
    private Text mov(commonCanvas v,String mv)
    {
    	Text base = TextChunk.create(mv);
    	if(covered!=null)
    	{
    		base = TextChunk.join(base,TextGlyph.create("xxx", covered, v, new double[] {1.5,1.5,0,0}));
    	}
    	if(moved!=null)
    	{
    		base = TextChunk.join(TextGlyph.create("xxx", moved, v, new double[] {1.5,1.5,0,0}),
    				base);
    	}
    	return(base);
    }
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas v)
    {
        switch (op)
        {
        case MOVE_PICKB:
            return mov(v,object+" "+from_col + from_row);

		case MOVE_DROPB:
            return mov(v,""+to_col + to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return mov(v,""+object+"-");
        case MOVE_RACK_BOARD:
        	return mov(v,""+object+"-"+to_col + to_row);
        case MOVE_BOARD_BOARD:
        	return mov(v,object+" "+from_col + from_row+"-"+to_col + to_row);
        case MOVE_DONE:
            return (TextChunk.create(""));

        default:
            return (TextChunk.create(D.findUnique(op)));

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
	        return (ind+D.findUnique(op) + " " + from_col + " " + from_row+" "+object);

		case MOVE_DROPB:
	        return (ind+D.findUnique(op) + " " + to_col + " " + to_row+" "+object);

		case MOVE_RACK_BOARD:
			return(ind+D.findUnique(op) + " " +D.findUnique(source.IID())+ " "+from_row+" "+object
					+ " " + to_col + " " + to_row);
		case MOVE_BOARD_BOARD:
			return(ind+D.findUnique(op) + " " + from_col + " " + from_row+" "+object
					+ " " + to_col + " " + to_row);
        case MOVE_PICK:
            return (ind+D.findUnique(op) + " "+D.findUnique(source.IID())+ " "+from_row+" "+object);

        case MOVE_DROP:
             return (ind+D.findUnique(op) + " "+D.findUnique(source.IID())+ " "+to_row+" "+object);

        case MOVE_START:
            return (ind+"Start P" + player);

        default:
            return (ind+D.findUnique(op));
        }
    }

}
