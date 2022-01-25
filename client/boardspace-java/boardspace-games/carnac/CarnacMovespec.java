package carnac;

import online.game.*;
import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.ExtendedHashtable;

public class CarnacMovespec extends commonMove implements CarnacConstants
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
 			"Tip",MOVE_TIP,
			"OnBoard",MOVE_RACK_BOARD,
			"Untip", MOVE_UNTIP);
   }

    CarnacId source; // where from/to
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    
    public CarnacMovespec() // default constructor
    {
    }
    public CarnacMovespec(int opc, int pl)	// constructor for simple moves
    {
    	player = pl;
    	op = opc;
    }
    public CarnacMovespec(int opc, char fromc,int fromr, char toc,int tor, int who)
    {
    	op = opc;
    	from_col = fromc;
    	from_row = fromr;
    	to_col = toc;
    	to_row = tor;
    	player = who;
    }
    /* constructor */
    public CarnacMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public CarnacMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    public boolean Same_Move_P(commonMove oth)
    {
    	CarnacMovespec other = (CarnacMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(CarnacMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
    	CarnacMovespec yto = (to == null) ? new CarnacMovespec() : (CarnacMovespec) to;

        // we need yto to be a CarnacMovespec at compile time so it will trigger call to the 
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
            from_col = '@';						// always
            from_row = G.IntToken(msg);			// index into the rack
  	        to_col = G.CharToken(msg);			// destination cell col
	        to_row = G.IntToken(msg);  			// destination cell row
	        break;
	        
        case MOVE_TIP:			// robot move from board to board
            source = CarnacId.BoardLocation;		
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;
	        
        case MOVE_DROPB:
	       source = CarnacId.BoardLocation;
	       to_col = G.CharToken(msg);
	       to_row = G.IntToken(msg);
	       break;

		case MOVE_PICKB:
            source = CarnacId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
 
            break;

        case MOVE_PICK:
            source = CarnacId.Chip_Pool;
            from_col = '@';
            from_row = G.IntToken(msg);
            break;
            
        case MOVE_DROP:
            source = CarnacId.Chip_Pool;
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

    static double glyphScale[] = {1.8,1.0,0.0,0.1 };
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas canvas)
    {
        switch (op)
        {
        case MOVE_PICKB:
            return (TextChunk.create(""+from_col + from_row));

		case MOVE_DROPB:
            return (TextChunk.create(""+to_col + to_row));

        case MOVE_DROP:
        	return (TextChunk.create("Drop"));
        case MOVE_PICK:
        	{
        	CarnacChip chip = CarnacChip.getChip(from_row);
            return (TextChunk.join(
            			TextGlyph.create(chip.shortName(),chip,canvas,glyphScale),
            			TextChunk.create(" - ")
            			));

        	}
        case MOVE_RACK_BOARD:
        	{
        	CarnacChip chip = CarnacChip.getChip(from_row);
        	return( TextChunk.join(
        				TextGlyph.create(chip.shortName(),chip,canvas,glyphScale),
        			    TextChunk.create(" - "+to_col + to_row)));
        	}
        case MOVE_TIP:
        	return(TextChunk.create("tip "+from_col + from_row+"-"+to_col + to_row));
        case MOVE_DONE:
        	return(TextChunk.create(" "));

        default:
            return (TextChunk.create(D.findUnique(op)));
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
        case MOVE_PICKB:
	        return (opname + from_col + " " + from_row);

		case MOVE_DROPB:
	        return (opname+ to_col + " " + to_row);

		case MOVE_RACK_BOARD:
			return(opname+from_row	+ " " + to_col + " " + to_row);
		case MOVE_TIP:
			return(opname + from_col + " " + from_row+" "+ to_col + " " + to_row);
        case MOVE_PICK:
            return (opname+from_row);

        case MOVE_DROP:
             return (opname+to_row);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
