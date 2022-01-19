package colorito;

import online.game.*;
import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.ExtendedHashtable;

public class ColoritoMovespec extends commonMove 
{
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
 
    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
        	"Pickb", MOVE_PICKB,
        	"Dropb", MOVE_DROPB,
 			"Move",MOVE_BOARD_BOARD);
   }

	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    ColoritoChip chip;
    CellStack path = null;
    int depth = 0;
    public ColoritoMovespec() // default constructor
    {
    }
    public ColoritoMovespec(int opc, int pl)	// constructor for simple moves
    {
    	player = pl;
    	op = opc;
    }
    /* constructor */
    public ColoritoMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public ColoritoMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    public ColoritoMovespec(int opc,char fc,int fr,char tc,int tr,int who)
    {	op = opc;
    	from_col = fc;
    	from_row = fr;
    	to_col = tc;
    	to_row = tr;
    	player = who;
    }
    
    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
    	ColoritoMovespec other = (ColoritoMovespec) oth;

        return ((op == other.op) 
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(ColoritoMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.depth = depth;
        to.from_col = from_col;
        to.from_row = from_row;
        to.path = path;
        to.chip = chip;
    }

    public commonMove Copy(commonMove to)
    {
    	ColoritoMovespec yto = (to == null) ? new ColoritoMovespec() : (ColoritoMovespec) to;

        // we need yto to be a ColoritoMovespec at compile time so it will trigger call to the 
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
    static double chipScale[] = {1,2,-0.2,-0.50};
    private Text icon(commonCanvas v,Object... m)
    {	
    	Text msg = TextChunk.create(G.concat(m));
    	if(chip!=null)
    		{ msg = TextChunk.join(TextGlyph.create("xxx",chip,v,chipScale),msg);
        	}
    	return(msg);
    }
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas v)
    {	
        switch (op)
        {
        case MOVE_PICKB:
            return icon(v,"",from_col , from_row,"-");
		case MOVE_DROPB:
            return icon(v,"",to_col, to_row);
        case MOVE_BOARD_BOARD:
         	return icon(v,"",from_col , from_row,"-",to_col ,to_row);
        case MOVE_DONE:
            return TextChunk.create("");
        default:
            return TextChunk.create(D.findUnique(op));

        }
    }

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
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
	        return (ind+opname + from_col + " " + from_row);

		case MOVE_DROPB:
	        return (ind+opname + to_col + " " + to_row);

		case MOVE_BOARD_BOARD:
			return(ind+opname+ from_col + " " + from_row
					+ " " + to_col + " " + to_row);
 
        case MOVE_START:
            return (ind+"Start P" + player);

       default:
            return (ind+opname);
        }
    }

    /* standard java method, so we can read moves easily while debugging */
    //public String toString()
    //{
    //    return ("P" + player + "[" + moveString() + "]");
    //}
}
