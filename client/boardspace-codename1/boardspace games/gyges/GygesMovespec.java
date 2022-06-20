package gyges;

import online.game.*;
import java.util.*;

import lib.*;


public class GygesMovespec extends commonMove implements GygesConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D);
        D.putInt("Pick", MOVE_PICK);
        D.putInt("Pickb", MOVE_PICKB);
        D.putInt("Drop", MOVE_DROP);
        D.putInt("Dropb", MOVE_DROPB);
 		D.putInt("Move",MOVE_BOARD_BOARD);
		D.putInt("OnBoard",MOVE_RACK_BOARD);
		D.putInt("Dropb_R",MOVE_DROPB_R);
   }

    GygesId source; // where from/to
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    GygesChip chip;
    CellStack bounces = null;	// the path of bounces, or the path of individual steps
    CellStack steps = null;	// individual steps in the path
    public GygesMovespec() // default constructor
    {
    }
    public GygesMovespec(int opc, int pl)	// constructor for simple moves
    {
    	player = pl;
    	op = opc;
    }
    /* constructor */
    public GygesMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    /* constructor for move_rack_board*/
    public GygesMovespec(int opc,GygesId src,int row,char dest_c,int dest_r,int pl)
    {	op = opc;
    	source = src;
    	from_row = row;
    	to_row = dest_r;
    	to_col = dest_c;
    	player = pl;
    }
    // constructor for move_dropb by robot
    public GygesMovespec(int opc,char col, int row, int pl)
    {
    	op = opc;
    	to_col = col;
    	to_row = row;
    	source = GygesId.BoardLocation;
    	player = pl;
    }
    public GygesMovespec(int opc,char col,int row,char dest_c,int dest_r,int pl)
    {	op = opc;
    	source = GygesId.BoardLocation;
    	from_row = row;
    	from_col = col;
    	to_row = dest_r;
    	to_col = dest_c;
    	player = pl;
    }
    /* constructor */
    public GygesMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    public boolean Same_Move_P(commonMove oth)
    {
    	GygesMovespec other = (GygesMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(GygesMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.chip = chip;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
    	GygesMovespec yto = (to == null) ? new GygesMovespec() : (GygesMovespec) to;

        // we need yto to be a GygesMovespec at compile time so it will trigger call to the 
        // local version of Copy_Slots
        Copy_Slots(yto);

        return (yto);
    }

    /* parse a string into the undoInfo of this move.  Remember that we're just parsing, we can't
     * refer to the undoInfo of the board or the game.
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
            source = GygesId.get(msg.nextToken());	// white rack or black rack
            from_row = G.IntToken(msg);			// index into the rack
 	        to_col = G.CharToken(msg);			// destination cell col
	        to_row = G.IntToken(msg);  			// destination cell row
	        break;
	        
        case MOVE_BOARD_BOARD:			// robot move from board to board
            source = GygesId.BoardLocation;		
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;
	        
        case MOVE_DROPB:
        case MOVE_DROPB_R:
	       source = GygesId.BoardLocation;
	       to_col = G.CharToken(msg);
	       to_row = G.IntToken(msg);
	       break;

		case MOVE_PICKB:
            source = GygesId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
 
            break;

        case MOVE_PICK:
            source = GygesId.get(msg.nextToken());
            from_row = G.IntToken(msg);
            break;
            
        case MOVE_DROP:
            source = GygesId.get(msg.nextToken());
            to_row = G.IntToken(msg);
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

        default:
            break;
        }
    }
    public Text chipIcon(commonCanvas v,String msg)
    {
    	Text t = TextChunk.create(msg);
    	if(chip!=null)
    	{
    		t = TextChunk.join(
    				TextGlyph.create("xxx",chip,v,new double[]{1,2,0.2,-0.0}),
    				t);
    	}
    	return(t);
    }
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas v)
    {
        switch (op)
        {
        case MOVE_PICKB:
            return (chipIcon(v,""+from_col + from_row+"-"));

		case MOVE_DROPB:
		case MOVE_DROPB_R:
            return (chipIcon(v," "+to_col + to_row));

        case MOVE_PICK:
            return (chipIcon(v,((source==GygesId.First_Player_Pool)||(source==GygesId.Second_Player_Pool))?"":source.shortName));
        case MOVE_DROP:
            return (TextChunk.create(source.shortName));

        case MOVE_RACK_BOARD:
        	return(chipIcon(v,""+to_col + to_row));
        case MOVE_BOARD_BOARD:
        	return(chipIcon(v,""+from_col + from_row+"-"+to_col + to_row));
        case MOVE_DONE:
            return (TextChunk.create(""));

        default:
            return (TextChunk.create(D.findUniqueTrans(op)));

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
		case MOVE_DROPB_R:
	        return (opname + to_col + " " + to_row);

		case MOVE_RACK_BOARD:
			return(opname +source.shortName+ " "+from_row+ " " + to_col + " " + to_row);
		case MOVE_BOARD_BOARD:
			return(opname + from_col + " " + from_row + " " + to_col + " " + to_row);
        case MOVE_PICK:
            return (opname+source.shortName+ " "+from_row);

        case MOVE_DROP:
             return (opname+source.shortName+ " "+to_row);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
