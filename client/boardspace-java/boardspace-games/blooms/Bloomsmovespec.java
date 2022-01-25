package blooms;

import java.util.*;

import blooms.BloomsConstants.BloomsId;
import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import online.game.*;
import lib.ExtendedHashtable;
public class Bloomsmovespec extends commonMove
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
 
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB);
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
    BloomsId source; // where from/to
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    BloomsChip target = null;
    
    public Bloomsmovespec()
    {
    } // default constructor

    /* constructor */
    public Bloomsmovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    /** constructor for robot moves.  Having this "binary" constor is dramatically faster
     * than the standard constructor which parses strings
     */
    public Bloomsmovespec(int opc,char col,int row,BloomsId what,int who)
    {
    	op = opc;
    	source = what;
    	to_col = col;
    	to_row = row;
    	player = who;
    }
    /* constructor */
    public Bloomsmovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    /* constructor */
    public Bloomsmovespec(int opc, int p)
    {	op = opc;
    	player = p;
    }

    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        Bloomsmovespec other = (Bloomsmovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (player == other.player));
    }

    public void Copy_Slots(Bloomsmovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.source = source;
        to.target = target;
    }

    public commonMove Copy(commonMove to)
    {
        Bloomsmovespec yto = (to == null) ? new Bloomsmovespec() : (Bloomsmovespec) to;

        // we need to to be a Bloomsmovespec at compile time so it will trigger call to the 
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

        int opcode = D.getInt(cmd, MOVE_UNKNOWN);
        op = opcode;
        switch (opcode)
        {
        case MOVE_DROPB:
				source = BloomsId.find(msg.nextToken());	// B or W
	            to_col = G.CharToken(msg);
	            to_row = G.IntToken(msg);

	            break;

		case MOVE_PICKB:
            source = BloomsId.BoardLocation;
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);

            break;

        case MOVE_DROP:
        case MOVE_PICK:
            source = BloomsId.find(msg.nextToken());

            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

        case MOVE_UNKNOWN:
        	throw G.Error("Cant parse %s", cmd);
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
        case MOVE_PICKB:
            return (""+ to_col + " " + to_row+">");

		case MOVE_DROPB:
            return (to_col + " " + to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return (""+source.shortName);

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
    {  	switch (op)
    {
        case MOVE_PICKB:
            return TextChunk.create(""+ to_col + to_row+">");

		case MOVE_DROPB:
		{	Text msg = TextChunk.create(" "+to_col + to_row);
			if(target!=null)
			{ 	msg = TextChunk.join(TextGlyph.create("xxx",target,v,new double[] {1,1.25,0,-0.2}),
					msg);
			}
            return msg;
		}
        case MOVE_DROP:
        	{
        	Text msg = TextChunk.create(""+source.shortName);
        	if(target!=null)
        	{
        	msg = TextChunk.join(TextGlyph.create("xxx",target,v,new double[] {1,1.25,0,-0.2}),
					msg);
        	}
            return msg;
        }
        case MOVE_PICK:
        	return TextChunk.create(" ");
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
	        return (opname+ to_col + " " + to_row);

		case MOVE_DROPB:
	        return (opname+source.shortName+" " + to_col + " " + to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return (opname+source.shortName);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
