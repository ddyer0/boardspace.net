package chess;

import online.game.*;
import java.util.*;

import lib.ExtendedHashtable;
import lib.G;

import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;


public class ChessMovespec extends commonMove implements ChessConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; 	// pick a chip from a pool
    static final int MOVE_DROP = 205; 	// drop a chip
    static final int MOVE_PICKB = 206; 	// pick from the board
    static final int MOVE_DROPB = 207; 	// drop on the board
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
    static final int MOVE_SUICIDE = 211;
    static final int MOVE_STALEMATE = 212;

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
 			"Move",MOVE_BOARD_BOARD,
 			"Suicide",MOVE_SUICIDE,
 			"Stalemate",MOVE_STALEMATE);
   }

    ChessId source; // where from/to
    ChessId dest;	
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    ChessChip chip = null;
    public ChessMovespec() // default constructor
    {
    }
    public ChessMovespec(int opc, int pl)	// constructor for simple moves
    {
    	player = pl;
    	op = opc;
    }
    // constructor for most moves
    public ChessMovespec(ChessCell from,ChessCell to,int who)
    {	op = MOVE_BOARD_BOARD;
    	player = who;
    	source = from.rackLocation();
    	from_col = from.col;
    	from_row = from.row;
    	dest = to.rackLocation();
    	to_col = to.col;
    	to_row = to.row;
    }	
    /* constructor */
    public ChessMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public ChessMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    /* constructor for robot moves */
    public ChessMovespec(int opc,ChessCell from,ChessCell to,int who)
    {
    	player = who;
    	op = opc;
    	from_col = from.col;
    	from_row = from.row;
    	source = from.rackLocation();
    	to_col = to.col;
    	to_row = to.row;
    	dest = to.rackLocation();
    }

 
    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
    	ChessMovespec other = (ChessMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(ChessMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.source = source;
        to.dest = dest;
        to.chip = chip;
    }

    public commonMove Copy(commonMove to)
    {
    	ChessMovespec yto = (to == null) ? new ChessMovespec() : (ChessMovespec) to;

        // we need yto to be a ChessMovespec at compile time so it will trigger call to the 
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
        case MOVE_SUICIDE:
        	source = ChessId.BoardLocation;		
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        dest = ChessBoard.CaptureLocation[to_row];
	        break;
       	
        case MOVE_BOARD_BOARD:			// robot move from board to board
        	dest = source = ChessId.BoardLocation;		
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;
	        
		case MOVE_DROPB:
		case MOVE_PICKB:
            source = ChessId.BoardLocation;
            to_col = from_col = G.CharToken(msg);
            to_row = from_row = G.IntToken(msg);

            break;

        case MOVE_PICK:
            source = ChessId.get(msg.nextToken());
            break;
            
        case MOVE_DROP:
           source =ChessId.get(msg.nextToken());
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

       default:
            break;
        }
    }
	static double chipScale[] = {1.5,3.5,0,-0.40};
    private Text icon(commonCanvas v,Object... msg)
    {	
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
        	return(icon(v,from_col,from_row));
 
		case MOVE_DROPB:
            return (icon(v," - ",to_col,to_row));

        case MOVE_DROP:
        case MOVE_PICK:
            return (icon(v,source.shortName));
        case MOVE_SUICIDE:
        	return(icon(v,"x ",from_col,from_row));
        case MOVE_BOARD_BOARD:
        	return(icon(v,from_col,from_row,"-",to_col, to_row));
        case MOVE_DONE:
            return (icon(v,""));

        default:
            return (icon(v,D.findUnique(op)));
        }
    }

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String moveString()
    {
		String ind = (index() >= 0) ? (index() + " ") : "";
		String opname = D.findUnique(op)+" ";
        // adding the move index as a prefix provides numbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        case MOVE_PICKB:
	        return (ind+opname+ from_col + " " + from_row);

		case MOVE_DROPB:
	        return (ind+opname + to_col + " " + to_row);
		case MOVE_SUICIDE:
		case MOVE_BOARD_BOARD:
			return(ind+opname+ from_col + " " + from_row
					+ " " + to_col + " " + to_row);
        case MOVE_PICK:
            return (ind+opname+source.shortName+ " "+from_row);

        case MOVE_DROP:
             return (ind+opname+source.shortName+ " "+to_row);

        case MOVE_START:
            return (ind+"Start P" + player);

        default:
            return (ind+opname);
        }
    }

    /* standard java method, so we can read moves easily while debugging */
    //public String toString()
    //{
    //    return ("P" + player + "[" + moveString() + "]");
    //}
}
