package stac;

import online.game.*;
import java.util.*;

import lib.G;
import lib.MultiGlyph;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.ExtendedHashtable;


public class StacMovespec extends commonMove implements StacConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
	static final int MOVE_CARRY = 208;
	static final int MOVE_PICKC = 209;	// carry pick in the UI
    static final int MOVE_BOARD_BOARD = 210;	// move board to board

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
 			"Move",MOVE_BOARD_BOARD,
			"Carry",MOVE_CARRY,
			"Pickc",MOVE_PICKC);
    }

    StacId source; // where from/to
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    
    public StacMovespec() // default constructor
    {
    }
    // constructor for move and carry
    public StacMovespec(int opc,StacCell origin,StacCell dest,int who)
    {	from_col = origin.col;
    	from_row = origin.row;
    	to_col = dest.col;
    	to_row = dest.row;
    	op = opc;
    	player = who;
    }
    public StacMovespec(int opc, int pl)	// constructor for simple moves
    {
    	player = pl;
    	op = opc;
    }
    /* constructor */
    public StacMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public StacMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    
    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
    	StacMovespec other = (StacMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(StacMovespec to)
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
    	StacMovespec yto = (to == null) ? new StacMovespec() : (StacMovespec) to;

        // we need yto to be a StacMovespec at compile time so it will trigger call to the 
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
        // these are needed by a few fossile game records 
        if("accept".equals(cmd)) { op = MOVE_ACCEPT_DRAW; }
        else if("decline".equals(cmd)) { op = MOVE_DECLINE_DRAW; }
        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Can't parse %s", cmd);
        case MOVE_CARRY:	// move with a piece in tow
        case MOVE_BOARD_BOARD:			// robot move from board to board
        	source = StacId.BoardLocation;		
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;
	        
        case MOVE_DROPB:
		case MOVE_PICKB:
		case MOVE_PICKC:
            source = StacId.BoardLocation;
            to_col = from_col = G.CharToken(msg);
            to_row = from_row = G.IntToken(msg);

            break;

        case MOVE_PICK:
            source = StacId.get(msg.nextToken());
            from_col = '@';
            from_row = 0;
            break;
            
        case MOVE_DROP:
           source =StacId.get(msg.nextToken());
            to_col = '@';
            to_row = 0;
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

        default:
 
            break;
        }
    }

    private MultiGlyph dualIcon(StacChip a,StacChip b)
    {
    	MultiGlyph glyph = MultiGlyph.create();	
    	
    	glyph.append(a,new double[]{1.0,0,0});
		glyph.append(b,new double[]{1.0,0,0});
		return(glyph);
	}
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas viewer)
    {	double scl[] = new double[]{1.2,1.4,0.0,-0.4};
        switch (op)
        {
        case MOVE_PICKC:
        	return (TextChunk.join(
        			TextGlyph.create("xxxx",dualIcon(StacChip.black,StacChip.boss[player]),viewer,scl),
        			TextChunk.create(""+from_col + from_row+"-")));
        case MOVE_PICKB:
            return (TextChunk.join(
        			TextGlyph.create("xxxx",StacChip.boss[player],viewer,scl),
        			TextChunk.create(""+from_col + from_row+"-")));

		case MOVE_DROPB:
            return (TextChunk.create(""+to_col + to_row));

        case MOVE_DROP:
        case MOVE_PICK:
            return (TextChunk.create(source.shortName));
        case MOVE_CARRY:
        	return(TextChunk.join(
        			TextGlyph.create("xxxx",dualIcon(StacChip.black,StacChip.boss[player]),viewer,scl),
        			TextChunk.create(""+from_col + from_row+"-"+to_col+to_row)));
        case MOVE_BOARD_BOARD:
        	return(TextChunk.join(
        			TextGlyph.create("xxxx",StacChip.boss[player],viewer,scl),
        			TextChunk.create(""+from_col + from_row+"-"+to_col+to_row)));
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
        // adding the move index as a prefix provides numbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        case MOVE_PICKC:
        case MOVE_PICKB:
	        return (opname+ from_col + " " + from_row);

		case MOVE_DROPB:
	        return (opname+ to_col + " " + to_row);

		case MOVE_CARRY:
		case MOVE_BOARD_BOARD:
			return(opname + from_col + " " + from_row	+ " " + to_col + " " + to_row);
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

    /* standard java method, so we can read moves easily while debugging */
    //public String toString()
    //{
    //    return ("P" + player + "[" + moveString() + "]");
    //}
}
