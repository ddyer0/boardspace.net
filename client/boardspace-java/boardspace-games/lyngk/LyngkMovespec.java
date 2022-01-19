package lyngk;

import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lyngk.LyngkConstants.LyngkId;
import online.game.*;
import lib.ExtendedHashtable;
public class LyngkMovespec extends commonMove
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_FROM_TO = 208;	// move to claim a color
    static final int MOVE_BOARD_BOARD = 209;// move to stack on the board
 
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
    		"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
        	"Claim", MOVE_FROM_TO,
        	"Move", MOVE_BOARD_BOARD);
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
    LyngkId source; // where from/to
    char to_col; 	// for from column
    int to_row; 	// for from row
    LyngkId dest;
    char from_col;
    int from_row;
    LyngkCell target=null;
    LyngkCell target2=null;
    public LyngkMovespec()
    {
    } // default constructor

    /* constructor for the viewer */
    public LyngkMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    // constructor for robot claim color moves
    public LyngkMovespec(LyngkId from,LyngkId to,int pl)
    {	player = pl;
    	op = MOVE_FROM_TO;
    	source = from;
    	dest = to;
    }
    // constructor for robot done/pass/resign
    public LyngkMovespec(int opc,int pl)
    {
    	op = opc;
    	player = pl;
    }
    // constructor for robot stacking moves
    public LyngkMovespec(LyngkCell from, LyngkCell to,int pl)
    {	player = pl;
    	source = dest = LyngkId.BoardLocation;
    	op = MOVE_BOARD_BOARD;
    	from_col = from.col;
    	from_row = from.row;
    	to_col = to.col;
    	to_row = to.row;
     }

    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        LyngkMovespec other = (LyngkMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_col == other.from_col)
				&& (from_row == other.from_col)
				&& (dest == other.dest)
				&& (player == other.player));
    }

    public void Copy_Slots(LyngkMovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_row = from_row;
        to.from_col = from_col;
        to.source = source;
        to.dest = dest;
        to.target = target;
        to.target2 = target2;
    }

    public commonMove Copy(commonMove to)
    {
        LyngkMovespec yto = (to == null) ? new LyngkMovespec() : (LyngkMovespec) to;

        // we need yto to be a LyngkMovespec at compile time so it will trigger call to the 
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
        	source = LyngkId.find(msg.nextToken());
        	dest = LyngkId.find(msg.nextToken());
         	break;
   
        case MOVE_BOARD_BOARD:
        	source = LyngkId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
            dest = LyngkId.BoardLocation;	
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);
            break;
        case MOVE_DROPB:
        	dest = LyngkId.BoardLocation;	
        	to_col = G.CharToken(msg);
        	to_row = G.IntToken(msg);
        	break;

		case MOVE_PICKB:
            source = LyngkId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
            to_row = G.IntToken(msg); 	// picked index in puzzle mode
            break;

        case MOVE_DROP:
        	dest = LyngkId.get(msg.nextToken());
        	break;
        	
        case MOVE_PICK:
            source = LyngkId.get(msg.nextToken());

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
        case MOVE_FROM_TO:
 
        	return("");
 
        case MOVE_BOARD_BOARD:
        	return(""+from_col+from_row+"-"+to_col+to_row);
        	
        case MOVE_PICKB:
            return (""+from_col + from_row+"-");

		case MOVE_DROPB:
            return (""+to_col + to_row);

        case MOVE_DROP:
            return ("");
        case MOVE_PICK:
            return ("");

        case MOVE_DONE:
            return ("");

        default:
            return (D.findUnique(op));

        }
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
    	String msg = shortMoveString();
    	int ind = msg.indexOf('-');
    	Text str = null;
    	if(target2!=null && (ind>=0))
    	{		
    		String str2 = "";
    		if(ind>=0) { str2 = msg.substring(ind+1); msg = msg.substring(0,ind+1); }
    		Text icon = TextGlyph.create("xxx",target2,v,new double[] {1,1,0,-0.2});
    		str = TextGlyph.join(TextChunk.create(msg),icon,TextChunk.create(str2));
    	}
    	else { str = TextChunk.create(msg); }
    	
    	if(target!=null)
    	{
		Text icon = TextGlyph.create("xxx",target,v,new double[] {1,1,0,-0.2});
		str = TextGlyph.join(icon,str);
    	}
    	return(str);
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
        case MOVE_FROM_TO:
        	return(ind+opname+source.shortName+" "+dest.shortName);
        	
        case MOVE_BOARD_BOARD:
        	return(ind+opname+ from_col+" "+from_row+" "+to_col+" "+to_row);
        	
        case MOVE_PICKB:
	        return (ind+opname+ from_col + " " + from_row+" "+to_row);

		case MOVE_DROPB:
	        return (ind+opname + to_col + " " + to_row);

        case MOVE_DROP:
            return (ind+opname +dest.shortName);
        case MOVE_PICK:
            return (ind+opname +source.shortName);

        case MOVE_START:
            return (ind+"Start P" + player);

        default:
            return (ind+opname);
        }
    }

}
