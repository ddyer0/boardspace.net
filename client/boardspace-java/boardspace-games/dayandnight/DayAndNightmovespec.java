package dayandnight;

import java.util.*;

import dayandnight.DayAndNightConstants.DayAndNightId;
import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import online.game.*;
import lib.ExtendedHashtable;
public class DayAndNightmovespec 
		extends commonMove	// for a multiplayer game, this will be commonMPMove
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_FROM_TO = 208;	// move 
 
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
        	"move",MOVE_FROM_TO);
  }
   // public boolean visit()
   // {	
   // 	return (false);
   //
   //}
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
    DayAndNightId source; // where from/to
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    char from_col;
    int from_row;
    
    DayAndNightChip chip;
    
    // these provide an interface to log annotations that will be seen in the game log
    String gameEvents[] = null;
    public String[] gameEvents() { return(gameEvents); }

    public DayAndNightmovespec()
    {
    } // default constructor

    /* constructor */
    public DayAndNightmovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    /* constructor */
    public DayAndNightmovespec(int opc,int pl)
    {
    	op = opc;
    	player = pl;
    }
    /** constructor for robot moves.  Having this "binary" constor is dramatically faster
     * than the standard constructor which parses strings
     */
    public DayAndNightmovespec(int opc,char col,int row,int who)
    {
    	op = opc;
     	to_col = col;
    	to_row = row;
    	player = who;
    }
    /* constructor */
    public DayAndNightmovespec(int opc,char from_c,int from_r,char col,int row,int who)
    {
    	op = opc;
    	from_col = from_c;
    	from_row = from_r;
     	to_col = col;
    	to_row = row;
    	player = who;
    }
   
    /* constructor */
    public DayAndNightmovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        DayAndNightmovespec other = (DayAndNightmovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_col == other.from_col)
				&& (from_row == other.from_row)
				&& (player == other.player));
    }

    public void Copy_Slots(DayAndNightmovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.source = source;
        to.chip = chip;
    }

    public commonMove Copy(commonMove to)
    {
        DayAndNightmovespec yto = (to == null) ? new DayAndNightmovespec() : (DayAndNightmovespec) to;

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

        case MOVE_FROM_TO:
        	source = DayAndNightId.BoardLocation;
        	from_col = G.CharToken(msg);
        	from_row = G.IntToken(msg);
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);
            break;
            
        case MOVE_DROPB:
		case MOVE_PICKB:
            source = DayAndNightId.BoardLocation;
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);
            break;

        case MOVE_DROP:
        case MOVE_PICK:
            source = DayAndNightId.get(msg.nextToken());

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

    /** construct an abbreviated move string, mainly for use in the game log.  These
     * don't have to be parseable, they're intended only to help humans understand
     * the game record.  The alternative method {@link #shortMoveText} can be implemented
     * to provide colored text or mixed text and icons.
     * 
     * */
    public Text shortMoveText(commonCanvas v)
    {
        switch (op)
        {
        case MOVE_FROM_TO:
        	return(icon(v,from_col,from_row,"-",to_col,to_row));
        	
        case MOVE_PICKB:
            return icon(v,to_col , to_row);

		case MOVE_DROPB:
            return icon(v,to_col ,to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return icon(v,source.shortName());

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
        case MOVE_FROM_TO:
        	return G.concat(ind,opname ,  from_col," ",from_row," ", to_col , " " , to_row);
 
        case MOVE_PICKB:
		case MOVE_DROPB:
	        return G.concat(ind,opname , to_col , " " , to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return G.concat(ind,opname , source.shortName());

        case MOVE_START:
            return G.concat(ind,"Start P" , player);

        default:
            return G.concat(ind,opname);
        }
    }
 
}
