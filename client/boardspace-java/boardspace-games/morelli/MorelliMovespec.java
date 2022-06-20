package morelli;

import online.game.*;
import java.util.*;

import lib.G;
import lib.TextChunk;
import lib.Text;
import lib.ExtendedHashtable;

public class MorelliMovespec extends commonMove implements MorelliConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
    static final int MOVE_GAMEOVER = 211;		// dummy move for game is over
    static final int CHOOSE_SETUP = 212;
    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D);
        D.putInt("Pickb", MOVE_PICKB);
        D.putInt("Dropb", MOVE_DROPB);
 		D.putInt("Move",MOVE_BOARD_BOARD);
 		D.putInt("GameOver", MOVE_GAMEOVER);
 		D.putInt("Choose",CHOOSE_SETUP);
   }
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    
    public MorelliMovespec() // default constructor
    {
    }
    public MorelliMovespec(int opc, int pl)	// constructor for simple moves
    {
    	player = pl;
    	op = opc;
    }
    public MorelliMovespec(int opc,char fc,int fr,char tc,int tr,int pl)
    {	op = opc;
    	from_col = fc;
    	to_col = tc;
    	from_row = fr;
    	to_row = tr;
    	player = pl;
    }
    /* constructor */
    public MorelliMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public MorelliMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    
    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
    	MorelliMovespec other = (MorelliMovespec) oth;

        return ((op == other.op) 
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(MorelliMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
     }

    public commonMove Copy(commonMove to)
    {
    	MorelliMovespec yto = (to == null) ? new MorelliMovespec() : (MorelliMovespec) to;

        // we need yto to be a MorelliMovespec at compile time so it will trigger call to the 
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
        
        case CHOOSE_SETUP:
        	from_row = Setup.valueOf(msg.nextToken()).ordinal();
        	break;
         case MOVE_BOARD_BOARD:			// robot move from board to board
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;
	        
        case MOVE_DROPB:
	       to_col = G.CharToken(msg);
	       to_row = G.IntToken(msg);
	       break;

		case MOVE_PICKB:
             from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
            break;


        case MOVE_START:
            player = D.getInt(msg.nextToken());
            break;

        default:
 
            break;
        }
    }

    public Text shortMoveText(commonCanvas can)
    {	// upcast the canvas
    	return(shortMoveText((MorelliViewer)can));
    }
    
    private Text coloredCoordinate(MorelliViewer v,char col,int row)
    {
    	if(v!=null) { return(v.coloredCoordinate(col,row)); }
    	return(TextChunk.create(""+col+row+" "));
    }
    /* construct a move Text for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(MorelliViewer v)
    {
        switch (op)
        {
        case MOVE_PICKB:
            return (coloredCoordinate(v,from_col,from_row));

		case MOVE_DROPB:
            return (coloredCoordinate(v,to_col,to_row));

        case MOVE_BOARD_BOARD:
        	return(TextChunk.join(coloredCoordinate(v,from_col,from_row),coloredCoordinate(v,to_col,to_row)));
        case MOVE_DONE:
            return (TextChunk.create(""));

        default:
            return (TextChunk.create(D.findUniqueTrans(op)));
        case CHOOSE_SETUP:
        	return(TextChunk.create("Choose "+Setup.getSetup(from_row).name()));
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
	        return (opname + to_col + " " + to_row);

		case MOVE_BOARD_BOARD:
			return(opname + from_col + " " + from_row
					+ " " + to_col + " " + to_row);
	   
		case CHOOSE_SETUP:
	        	return(indx+"Choose "+Setup.getSetup(from_row).name());
	 
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
