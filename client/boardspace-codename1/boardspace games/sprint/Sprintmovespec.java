package sprint;

import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import online.game.*;
import lib.ExtendedHashtable;
public class Sprintmovespec extends commonMPMove implements SprintConstants
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_SELECT = 208;	// select value for a blank
    static final int MOVE_PLAYWORD = 211;	// play a word from the rack
    static final int MOVE_SEE = 212;		// see tiles on the hidden rack
    static final int MOVE_LIFT = 213; 		// lift a tile in the user interface rack
    static final int MOVE_REPLACE = 214; 	// replace a tile in the user interface rack
    static final int MOVE_MOVETILE = 215;		// move a tile from a rack to the board or back
    static final int MOVE_PULL = 226;			// pull more tiles
    static final int MOVE_ENDGAME = 227;		// end the game now
    static final int MOVE_SWITCH = 228;			// switch point of view
    static final int MOVE_ENDEDGAME = 229;		// ack endgame
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
        	"SetBlank", MOVE_SELECT,
        	"Play",MOVE_PLAYWORD,
        	"See", MOVE_SEE,
        	"Lift", MOVE_LIFT,
        	"Replace",MOVE_REPLACE,
        	"move",MOVE_MOVETILE,
        	"pull",MOVE_PULL,
        	"endgame",MOVE_ENDGAME,
        	"ended",MOVE_ENDEDGAME,
        	"switch",MOVE_SWITCH
    			);
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
    int mapped_row = -1;
    SprintId dest;	
    SprintId source; // only for move tile
    char from_col;	// only for move tile
    int from_row;	// only for move tile
    SprintChip chip;
    String word = null;
    int direction;
    String gameEvents[] = null;
    public String[] gameEvents() { return(gameEvents); }
    
    public Sprintmovespec()
    {
    } // default constructor

    /* constructor */
    public Sprintmovespec(String str)
    {
        parse(new StringTokenizer(str));
    }
    public Sprintmovespec(Word w,int who)
    {
    	op = MOVE_PLAYWORD;
    	to_col = w.seed.col;
    	to_row = w.seed.row;
    	direction = w.direction;
    	word = w.name;
    	player = who;
    	mapped_row = -1;
    }
    /** constructor for simple robot moves - pass and done 
     */
    public Sprintmovespec(int opc,int who)
    {
    	op = opc;
    	player = who;
    }

    
    /** constructor for robot moves.  Having this "binary" constor is dramatically faster
     * than the standard constructor which parses strings
     */
    public Sprintmovespec(int opc,char col,int row,SprintId what,int who)
    {
    	op = opc;
    	dest = what;
    	to_col = col;
    	to_row = row;
    	player = who;
    }
    /* constructor */
    public Sprintmovespec(StringTokenizer ss)
    {
        parse(ss);
    }

    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        Sprintmovespec other = (Sprintmovespec) oth;

        return ((op == other.op) 
				&& (dest == other.dest)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (player == other.player)
        		&& (source == other.source)
        		&& (from_col == other.from_col)
        		&& (from_row == other.from_row)
        		&& (mapped_row == other.mapped_row)
        		&& (word==null ? other.word==null : word.equals(other.word))
        		&& (direction == other.direction));
    }

    public void Copy_Slots(Sprintmovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.dest = dest;
        to.chip = chip;
        to.word = word;
        to.source = source;
        to.to_col = to_col;
        to.to_row = to_row;
        to.direction = direction;
        to.mapped_row = mapped_row;
    }

    public commonMove Copy(commonMove to)
    {
        Sprintmovespec yto = (to == null) ? new Sprintmovespec() : (Sprintmovespec) to;

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
    private void parse(StringTokenizer msg)
    {
        String cmd = msg.nextToken();
 
        if (Character.isDigit(cmd.charAt(0)))
        { // if the move starts with a digit, assume it is a sequence number
            setIndex(G.IntToken(cmd));
            cmd = msg.nextToken();
        }
        //
        // for sprint, effectively all the moves are ephemeral, which means
        // they have to contain the player doing the moving.  Rather than address
        // this piecemiel, we make the player number uniformly the next token.
        //
        op = D.getInt(cmd, MOVE_UNKNOWN);
        if(op==MOVE_START) {}
        else if(msg.hasMoreTokens())  { player = G.IntToken(msg); }
        else {}
        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Cant parse " + cmd);
        case MOVE_PULL:	// pull new tiles from the pool
        	to_row = G.IntToken(msg);
        	break;
        case MOVE_PLAYWORD:
           	to_col = G.parseCol(msg);
        	to_row = G.IntToken(msg);
        	direction = G.IntToken(msg);
        	word = msg.nextToken();
        	break;
        case MOVE_DROPB:
        	dest = SprintId.BoardLocation;
        	to_col = G.parseCol(msg);
        	to_row = G.IntToken(msg);
        	break;
        case MOVE_MOVETILE:
        	source = SprintId.valueOf(msg.nextToken());
        	from_col = G.parseCol(msg);
        	from_row = G.IntToken(msg);
        	dest = SprintId.valueOf(msg.nextToken());
        	to_col = G.parseCol(msg);
        	to_row = G.IntToken(msg);
        	break;
        
      
		case MOVE_PICKB:
            dest = SprintId.BoardLocation;
            to_col = G.parseCol(msg);
            to_row = G.IntToken(msg);

            break;
		case MOVE_SELECT:
			dest = SprintId.BoardLocation;
			to_col = G.parseCol(msg);
			break;
			
		case MOVE_LIFT:
        case MOVE_PICK:
            dest = SprintId.valueOf(msg.nextToken());
            to_col = G.parseCol(msg);
            to_row = G.IntToken(msg);        
            mapped_row = (msg.hasMoreTokens()) ? G.IntToken(msg) : -1;
			break;

 		case MOVE_REPLACE:
		case MOVE_DROP:
            dest = SprintId.valueOf(msg.nextToken());
            to_col = G.parseCol(msg);
            to_row = G.IntToken(msg);
            mapped_row = msg.hasMoreTokens() ? G.IntToken(msg) : -1;
            break;

        case MOVE_START:
        	player = D.getInt(msg.nextToken());
            break;
         case MOVE_SEE:
        	{
        	char pl = G.parseCol(msg);
        	boolean v = G.BoolToken(msg);
        	to_col = pl;
        	to_row = v ? 1 : 0;
        	}
        	break;
        default:
        case MOVE_SWITCH:

            break;
        }
    }

    private Text icon(commonCanvas v,Object... msg)
    {	double chipScale[] = {1.2,1.2,-0.2,-0.5};
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
        case MOVE_PLAYWORD:
        	return TextChunk.create("Play "+G.printCol(to_col)+to_row);
        case MOVE_PICKB:
            return icon(v,G.printCol(to_col) , to_row);
        case MOVE_PULL:
        	return icon(v," ",to_row);
        case MOVE_MOVETILE:
        case MOVE_DROPB:
            return icon(v,G.printCol(to_col) ,to_row);

		case MOVE_SELECT:
        	return TextChunk.create("= "+G.printCol(to_col));
		case MOVE_LIFT:
		case MOVE_REPLACE:
        case MOVE_DROP:
        case MOVE_SEE:
        case MOVE_PICK:
        case MOVE_SWITCH:
        case MOVE_DONE:
        case MOVE_ENDGAME:
        case MOVE_ENDEDGAME:
            return TextChunk.create("");
        default:
            return TextChunk.create(D.findUniqueTrans(op));

        }
    }
  

    /** construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and only secondarily human readable */
    public String moveString()
    {
        String ind = indexString();
        String opname = ind+D.findUnique(op)+" "+player+" ";
        // adding the move index as a prefix provides numnbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        case MOVE_PULL:
        	return G.concat(opname," ",to_row);
        	
        case MOVE_PICKB:
		case MOVE_DROPB:
	        return G.concat(opname, G.printCol(to_col)," ",to_row);
		case MOVE_MOVETILE:
	        return (G.concat(opname,
	        		source.name()," ", from_col," ", from_row,
	        		" ",dest.name()," ", G.printCol(to_col)," ", to_row));

		case MOVE_LIFT:
        case MOVE_DROP:
        case MOVE_PICK:
        	 return G.concat(opname,dest.name()," ",G.printCol(to_col)," ",to_row," ",mapped_row);
        	 

		case MOVE_REPLACE:
             return G.concat(opname,dest.name()," ",G.printCol(to_col)," ",to_row);
        case MOVE_PLAYWORD:
        	return G.concat(opname,G.printCol(to_col)," ",to_row," ",direction," ",word);
        case MOVE_START:
            return G.concat(ind,"Start P",player);
        case MOVE_SELECT:
        	return G.concat(opname,G.printCol(to_col));
        case MOVE_SEE:
        	return G.concat(opname,G.printCol(to_col),((to_row==0)?" false" : " true"));
        default:
        case MOVE_ENDGAME:
        case MOVE_ENDEDGAME:
        case MOVE_SWITCH:
            return G.concat(opname);
        }
    }

}
