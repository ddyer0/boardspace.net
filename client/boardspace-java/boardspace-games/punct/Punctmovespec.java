package punct;

import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import online.game.*;
import lib.ExtendedHashtable;

public class Punctmovespec extends commonMove implements PunctConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Rotate",MOVE_ROTATE,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
        	"Move",MOVE_MOVE);
    }

    PunctId source; // where from/to
	int object;	// object being picked/dropped
	int rotation; // rotation of the piece
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    PunctState state;	// the state of the move before state, for UNDO
 
    // undo info for robot
	int from_row;
	char from_col;
	int from_rot;
	int from_level;
	public boolean evaluated;
	PunctPiece chip;
	
    public Punctmovespec()
    {
    } // default constructor

    /* constructor */
    public Punctmovespec(String str, int p)
    {	//System.out.println("Parse "+str);
        parse(new StringTokenizer(str), p);
    }
    // a move type move spec, for the robot
    public Punctmovespec(char fcol,int frow,char tcol,int trow,int rot,int whose)
    {	op = MOVE_MOVE;
    	source = PunctId.BoardLocation;
    	from_col = fcol;
    	from_row = frow;
    	to_col = tcol;
    	to_row = trow;
    	rotation = rot;
    	player=whose;
    }
    // a dropb type move, for the robot
    public Punctmovespec(int me,char col,int row ,int rr,int whoseTurn)
    {	op = MOVE_DROPB;
    	source = PunctId.EmptyBoard;
    	to_col = col;
    	to_row = row;
    	rotation = rr;
    	player = whoseTurn;
      }
    /* constructor */
    public Punctmovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    public boolean Same_Move_P(commonMove oth)
    {
        Punctmovespec other = (Punctmovespec) oth;

        return ((op == other.op) 
        		&& (from_row == other.from_row)
        		&& (from_col == other.from_col)
        		&& (from_rot == other.from_rot)
        		&& (from_level == other.from_level)
				&& (source == other.source)
				&& (object == other.object)
				&& (state == other.state)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (rotation == other.rotation)
				&& (player == other.player));
    }

    public void Copy_Slots(Punctmovespec to)
    {	super.Copy_Slots(to);
		to.object = object;
        to.to_col = to_col;
        to.to_row = to_row;
        to.state = state;
        to.source = source;
        to.rotation = rotation;
        to.from_row = from_row;
        to.from_col = from_col;
        to.from_rot = from_rot;
        to.from_level = from_level;
        to.evaluated = evaluated;
        to.chip = chip;
    }

    public commonMove Copy(commonMove to)
    {
        Punctmovespec yto = (to == null) ? new Punctmovespec() : (Punctmovespec) to;

        // we need yto to be a Punctmovespec at compile time so it will trigger call to the 
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
        
        case MOVE_MOVE:
        	source = PunctId.BoardLocation;
        	from_col = G.CharToken(msg);
        	from_row = G.IntToken(msg);
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);
            rotation = G.IntToken(msg);
           break;
            
        case MOVE_DROPB:
	            source = PunctId.EmptyBoard;
				object = G.IntToken(msg);	// piece index
	            to_col = G.CharToken(msg);
	            to_row = G.IntToken(msg);
	            rotation = G.IntToken(msg);

	            break;

		case MOVE_PICKB:
            source = PunctId.BoardLocation;
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);
 
            break;

        case MOVE_DROP:
        case MOVE_PICK:
             source = PunctId.get(msg.nextToken());
            object = G.IntToken(msg);
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

        default:

            break;
        }
    }
    
    private Text icon(commonCanvas v,Object... msg)
    {	
    	Text m = TextChunk.create(G.concat(msg));
    	if(chip!=null)
    	{	if(chip.rotation!=rotation)
    		{
    		chip = chip.copy();
    		chip.rotation = rotation;
    		}
    		m = TextChunk.join(TextGlyph.create("xx", chip, v,chip.chipScale()),
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
        	{
        	Punctmovespec n = (Punctmovespec)next;
        	if(n!=null && (n.chip==chip)) { n.chip = null; }
            return icon(v," ", to_col, to_row);
        	}
        case MOVE_MOVE:
        	return icon(v,from_col,from_row,"-",to_col ,to_row);
		case MOVE_DROPB:
            return icon(v,"-",to_col , to_row);

        case MOVE_PICK:
        	{
        	Punctmovespec n = (Punctmovespec)next;
        	if(n!=null && (n.chip==chip)) { n.chip = null; }
        	}
			//$FALL-THROUGH$
		case MOVE_DROP:
            return icon(v, " ");

        case MOVE_ROTATE:
        case MOVE_DONE:
            return TextChunk.create("");

        default:
            return TextChunk.create(D.findUniqueTrans(op));

       }
    }

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String moveString()
    {
		String indx = indexString();
		String opname = indx+D.findUnique(op)+" ";
        // adding the move index as a prefix provides numnbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {

        case MOVE_MOVE:
           	return(opname+from_col+" "+from_row+" "+to_col + " "+to_row+" "+rotation);
       	
        case MOVE_PICKB:
	        return (opname + to_col + " " + to_row);

		case MOVE_DROPB:
	        return (opname+object+" " + to_col + " " + to_row+" "+rotation);

        case MOVE_DROP:
        case MOVE_PICK:
            return (opname+source.shortName+" "+object);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
