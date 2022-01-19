package syzygy;

import online.game.*;

import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.ExtendedHashtable;


public class SyzygyMovespec extends commonMove implements SyzygyConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
        	"Pickb", MOVE_PICKB,
        	"Dropb", MOVE_DROPB,
        	"Move",MOVE_FROM_TO,

        	"Planet4",SyzId.CHIP_OFFSET.ordinal() + SyzygyChip.PLANET4_INDEX,
        	"Planet1",SyzId.CHIP_OFFSET.ordinal()  + SyzygyChip.PLANET1_INDEX,
        	"Planet2",SyzId.CHIP_OFFSET.ordinal()  + SyzygyChip.PLANET2_INDEX,
        	"Planet3",SyzId.CHIP_OFFSET.ordinal()  + SyzygyChip.PLANET3_INDEX);
    }

    char from_col;
    int from_row;
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    int undoinfo;	// the state of the move before state, for UNDO
    SyzygyChip chip;
    public SyzygyMovespec()
    {
    } 
    // constructor for robot board moves
    public SyzygyMovespec(int opc,char fc,int fr,char cc,int rr,int pl)
    {	op = opc;
    	from_col = fc;
    	from_row = fr;
    	to_col = cc;
    	to_row = rr;
    	player = pl;
    }
    /* constructor */
    public SyzygyMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public SyzygyMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    public SyzygyMovespec(int opcode,int pl) { player=pl; op=opcode; }
    public boolean Same_Move_P(commonMove oth)
    {
        SyzygyMovespec other = (SyzygyMovespec) oth;

        return ((op == other.op) 
				&& (undoinfo == other.undoinfo)
				&& (from_col == other.from_col)
				&& (from_row == other.from_row)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (player == other.player));
    }

    public void Copy_Slots(SyzygyMovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.undoinfo = undoinfo;
        to.from_col = from_col;
        to.from_row = from_row;
    }

    public commonMove Copy(commonMove to)
    {
        SyzygyMovespec yto = (to == null) ? new SyzygyMovespec() : (SyzygyMovespec) to;

        // we need yto to be a Hexmovespec at compile time so it will trigger call to the 
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
				to_col = G.CharToken(msg);
	            to_row = G.IntToken(msg);

	            break;

        case MOVE_FROM_TO:
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
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
    private Text icon(commonCanvas v,Object... msg)
    {	double chipScale[] = {1,1.5,-0.2,-0.5};
    	Text m = TextChunk.create(G.concat(msg));
    	if(chip!=null)
    	{
    		m = TextChunk.join(TextGlyph.create("xx", chip, v,chipScale),
    					m);
    	}
    	return(m);
    }

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas v)
    {
        switch (op)
        {
        case MOVE_PICKB:
            return icon(v,from_col ,from_row);

		case MOVE_DROPB:
            return icon(v,to_col, to_row);

        case MOVE_DONE:
            return TextChunk.create("");

        default:
            return TextChunk.create(D.findUnique(op));
            
        case MOVE_FROM_TO:
        	return icon(v,from_col,from_row,",",to_col,to_row);
        	
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
	        return (ind+opname+ to_col + " " + to_row);


        case MOVE_START:
            return (ind+"Start P" + player);

        case MOVE_FROM_TO:
        	return (ind+opname+from_col+" "+from_row+" "+to_col+" "+to_row);
 
        default:
            return (ind+opname);
        }
    }

 }
