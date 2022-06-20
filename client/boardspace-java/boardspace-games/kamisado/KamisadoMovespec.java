package kamisado;

import online.game.*;
import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.ExtendedHashtable;


public class KamisadoMovespec extends commonMove implements KamisadoConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D);
        D.putInt("Pickb", MOVE_PICKB);
        D.putInt("Dropb", MOVE_DROPB);
 		D.putInt("Move",MOVE_BOARD_BOARD);
    }

	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    KamisadoChip chip;
    
    public KamisadoMovespec()
    {
    } // default constructor

    /* constructor for simple moves */
    public KamisadoMovespec(int opc,int pl)
    {
    	op = opc;
    	player = pl;
    }
    /* constructor for from-to moves */
    public KamisadoMovespec(char fc,int fr,char tc,int tr,int who)
    {	op = MOVE_BOARD_BOARD;
    	player = who;
    	from_col = fc;
    	to_col = tc;
    	from_row = fr;
    	to_row = tr;
    }
    /* constructor */
    public KamisadoMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public KamisadoMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    public boolean Same_Move_P(commonMove oth)
    {
    	KamisadoMovespec other = (KamisadoMovespec) oth;

        return ((op == other.op) 
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(KamisadoMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.chip = chip;
    }

    public commonMove Copy(commonMove to)
    {
    	KamisadoMovespec yto = (to == null) ? new KamisadoMovespec() : (KamisadoMovespec) to;

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

        op  = D.getInt(cmd, MOVE_UNKNOWN);

        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Can't parse %s", cmd);
        

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

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas v)
    {
        switch (op)
        {
        case MOVE_PICKB:
            return (TextChunk.join(
            		TextGlyph.create("xx",chip,v,new double[]{1,2,-0.2,-0.5}),
            		TextChunk.create(""+from_col + from_row)));

		case MOVE_DROPB:
            return (TextChunk.create(""+to_col + to_row));


        case MOVE_BOARD_BOARD:
        	return(TextChunk.join(
        			TextGlyph.create("xx",chip,v,new double[]{1,2,-0.2,-0.5}),
        			TextChunk.create(""+from_col + from_row+"-"+to_col + to_row)));
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
	        return (opname + to_col + " " + to_row);

		case MOVE_BOARD_BOARD:
			return(opname + from_col + " " + from_row
					+ " " + to_col + " " + to_row);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
