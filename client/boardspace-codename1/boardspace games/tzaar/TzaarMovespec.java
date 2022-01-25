package tzaar;

import online.game.*;

import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.ExtendedHashtable;


public class TzaarMovespec extends commonMove implements TzaarConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
        	"Dropcap",MOVE_DROPCAP,
			"Move",MOVE_BOARD_BOARD,
			"Capture",CAPTURE_BOARD_BOARD,
			"OnBoard",MOVE_RACK_BOARD);
 
   }
    static String sourceName(int n)
    { 	return(D.findUnique(n));
    }
    TzaarId source; // where from/to
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    int undoInfo;	// from and to height before the operations
    TzaarState state;	// the state of the move before state, for UNDO
    TzaarChip top;
    TzaarChip bottom;
    public TzaarMovespec()
    {
    } // default constructor

    /* constructor */
    public TzaarMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    public TzaarMovespec(int opcode,int pl)
    {
    	op=opcode;
    	player=pl;
    }
    // constructor for robot drop moves
    public TzaarMovespec(int opcode,char col,int row,int pl)
    {
    	op = opcode;
    	source = TzaarId.BoardLocation;
    	player = pl;
    	to_col = col;
    	to_row = row;
    }
    public TzaarMovespec(int opcode,TzaarCell from,TzaarCell to,int pl)
    {
       op = opcode;
       source = from.rackLocation();
       player = pl;
       from_col = from.col;
       from_row = from.row;
       to_col = to.col;
       to_row = to.row;
     }
    public TzaarMovespec(int opcode,char fromc,int fromr,char toc,int tor,int pl)
    {
       op = opcode;
       source = TzaarId.BoardLocation;	
       player = pl;
       from_col = fromc;
       from_row = fromr;
       to_col = toc;
       to_row = tor;
     }
    /* constructor */
    public TzaarMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    public boolean Same_Move_P(commonMove oth)
    {
    	TzaarMovespec other = (TzaarMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (state == other.state)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(TzaarMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.undoInfo = undoInfo;
        to.state = state;
        to.source = source;
        to.top = top;
        to.bottom = bottom;
    }

    public commonMove Copy(commonMove to)
    {
    	TzaarMovespec yto = (to == null) ? new TzaarMovespec() : (TzaarMovespec) to;

        // we need yto to be a DipoleMovespec at compile time so it will trigger call to the 
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
        case CAPTURE_BOARD_BOARD:
        case MOVE_BOARD_BOARD:			// robot move from board to board
            source = TzaarId.BoardLocation;		
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;
        case MOVE_DROPCAP:
        case MOVE_DROPB:
	       source = TzaarId.BoardLocation;
	       to_col = G.CharToken(msg);
	       to_row = G.IntToken(msg);
	       break;

		case MOVE_PICKB:
             source = TzaarId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
            break;
            
		case MOVE_RACK_BOARD:
			source = TzaarId.get(msg.nextToken());
			from_col = '@';
			from_row = G.IntToken(msg);
	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
			break;
        case MOVE_PICK:
            source = TzaarId.get(msg.nextToken());
            from_col = '@';
            from_row = G.IntToken(msg);
            break;
            
        case MOVE_DROP:
        	source = TzaarId.get(msg.nextToken());
            to_col = '@';
            to_row = G.IntToken(msg);
            break;

        case MOVE_START:
        	player = D.getInt(msg.nextToken());

            break;
        default:
            break;
        }
    }
    private Text getIcon(TzaarChip chip,String text,commonCanvas v)
    {	Text message = TextChunk.create(text+" ");
    	if(chip != null)
    	{	message = TextChunk.join( TextGlyph.create("     ",chip,v,new double[]{1.2,1.4,-0.3,-0.2}),
    				message);
    	}
    	return(message);
    }

    public Text shortMoveText(commonCanvas v)
    {
        switch (op)
        {
        case MOVE_PICKB:
            return (getIcon(top,""+from_col + from_row,v));

		case MOVE_DROPB:
		case MOVE_DROPCAP:
            return (getIcon(bottom,""+to_col +  to_row+"  ",v));
	    case MOVE_DROP:
            return (TextChunk.create(source.shortName));
	    case MOVE_RACK_BOARD:
	    	return(getIcon(top,"@"+to_col+to_row,v));
	    case MOVE_BOARD_BOARD:
	    case CAPTURE_BOARD_BOARD:
	       	return( TextChunk.join(getIcon(top,""+from_col + from_row,v),
	       							getIcon(bottom,""+to_col + to_row+"  ",v)));
 	    case MOVE_PICK:
        case MOVE_DONE:
            return (TextChunk.create(""));
        default:
        		return(TextChunk.create(D.findUnique(op)));
       }
    }

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String moveString()
    {
		String indx = indexString();
		String opname = indx+D.findUnique(op)+" ";
        // adding the move index as a prefix provides numbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        case MOVE_PICKB:
	        return (opname + from_col + " " + from_row);

        case MOVE_DROPCAP:
		case MOVE_DROPB:
	        return (opname + to_col + " " + to_row);
		case CAPTURE_BOARD_BOARD:
		case MOVE_BOARD_BOARD:
			return(opname+ from_col + " " + from_row+" " + to_col + " " + to_row);
        case MOVE_PICK:
            return (opname+source.shortName+ " "+from_row);
        case MOVE_RACK_BOARD:
            return (opname+source.shortName+ " "+from_row+" "+to_col+" "+to_row);

        case MOVE_DROP:
             return (opname+source.shortName+ " "+to_row);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
