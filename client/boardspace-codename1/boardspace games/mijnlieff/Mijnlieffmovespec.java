package mijnlieff;

import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import mijnlieff.MijnlieffConstants.MijnlieffId;
import online.game.*;
import lib.ExtendedHashtable;
public class Mijnlieffmovespec 
		extends commonMove	// for a multiplayer game, this will be commonMPMove
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_FROM_TO = 208; // full move
 
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
    MijnlieffId source; // where from/to
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    MijnlieffChip chip;
    
    // these provide an interface to log annotations that will be seen in the game log
    String gameEvents[] = null;
    public String[] gameEvents() { return(gameEvents); }

    public Mijnlieffmovespec()
    {
    } // default constructor

    /* constructor */
    public Mijnlieffmovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    /** constructor for robot moves.  Having this "binary" constor is dramatically faster
     * than the standard constructor which parses strings
     */
    public Mijnlieffmovespec(int opc,char col,int row,int who)
    {
    	op = opc;
     	to_col = col;
    	to_row = row;
    	player = who;
    }
    /* constructor */
    public Mijnlieffmovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    public Mijnlieffmovespec(int moveFromTo, MijnlieffChip top, MijnlieffCell to, int who) {
		op = MOVE_FROM_TO;
		source = top.id;
		to_col = to.col;
		to_row = to.row;
		player = who;
	}

	public Mijnlieffmovespec(int movePass, int whoseTurn) {
		op = movePass;
		player = whoseTurn;
		
	}

	/**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        Mijnlieffmovespec other = (Mijnlieffmovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (player == other.player));
    }

    public void Copy_Slots(Mijnlieffmovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.source = source;
        to.chip = chip;
    }

    public commonMove Copy(commonMove to)
    {
        Mijnlieffmovespec yto = (to == null) ? new Mijnlieffmovespec() : (Mijnlieffmovespec) to;

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
		case MOVE_PICKB:
            source = MijnlieffId.BoardLocation;
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);

            break;

        case MOVE_DROP:
        case MOVE_PICK:
            source = MijnlieffId.get(msg.nextToken());

            break;
        case MOVE_FROM_TO:
        	source = MijnlieffId.get(msg.nextToken());
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

    private Text icon(commonCanvas v,Object... msg)
    {	double chipScale[] = {1.5,1.5,-0.2,-0.5};
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
  
		case MOVE_DROPB:
            return icon(v,to_col ,to_row);

        case MOVE_DROP:
            return icon(v,source.shortName());

        case MOVE_FROM_TO:
        	return icon(v,to_col,to_row);
        	
        case MOVE_PICKB:
        case MOVE_PICK:
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
		String indx = indexString();
		String opname = indx+D.findUnique(op)+" ";
        // adding the move index as a prefix provides numnbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        case MOVE_PICKB:
		case MOVE_DROPB:
	        return G.concat(opname, to_col , " " , to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return G.concat(opname,source.shortName());

        case MOVE_START:
            return G.concat(indx,"Start P" , player);

        case MOVE_FROM_TO:
        	return G.concat(opname,source.shortName()," ",to_col," ",to_row);
        	
        default:
            return G.concat(opname);
        }
    }

}