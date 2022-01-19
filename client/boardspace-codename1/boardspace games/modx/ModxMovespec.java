package modx;

import online.game.*;
import java.util.*;

import lib.G;
import lib.ExtendedHashtable;

public class ModxMovespec extends commonMove implements ModxConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; 	// pick a chip from a pool
    static final int MOVE_DROP = 205; 	// drop a chip
    static final int MOVE_PICKB = 206; 	// pick from the board
    static final int MOVE_DROPB = 207; 	// drop on the board
    static final int MOVE_RACK_BOARD = 210;	// move board to board

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D);
        D.putInt("Pick", MOVE_PICK);
        D.putInt("Pickb", MOVE_PICKB);
        D.putInt("Drop", MOVE_DROP);
        D.putInt("Dropb", MOVE_DROPB);
 		D.putInt("Move",MOVE_RACK_BOARD);
   }

    ModxId source; // where from/to
    char col; // for from-to moves, the destination column
    int row; // for from-to moves, the destination row

    public ModxMovespec() // default constructor
    {
    }
    public ModxMovespec(int opc, int pl)	// constructor for simple moves
    {
    	player = pl;
    	op = opc;
    }
    /* constructor */
    public ModxMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public ModxMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    /* constructor for robot moves */
    public ModxMovespec(int opc,ModxId from,ModxCell to,int who)
    {
    	player = who;
    	op = opc;
    	source = from;
    	col = to.col;
    	row = to.row;
    }
    /* constructor for robot moves */
    public ModxMovespec(int opc,ModxCell from,ModxCell target,ModxCell to,int who)
    {
    	player = who;
    	op = opc;
     	col = to.col;
    	row = to.row;
    }  
    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
    	ModxMovespec other = (ModxMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (row == other.row) 
				&& (col == other.col)
				&& (player == other.player));
    }

    public void Copy_Slots(ModxMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.col = col;
        to.row = row;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
    	ModxMovespec yto = (to == null) ? new ModxMovespec() : (ModxMovespec) to;

        // we need yto to be a ModxMovespec at compile time so it will trigger call to the 
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

        case MOVE_RACK_BOARD:			// robot move from board to board
            source = ModxId.get(msg.nextToken());	// from rack
 	        col = G.CharToken(msg);		//to col row
	        row = G.IntToken(msg);
	        break;
	        
		case MOVE_DROPB:
		case MOVE_PICKB:
            source = ModxId.BoardLocation;
            col = G.CharToken(msg);
            row = G.IntToken(msg);

            break;

       case MOVE_DROP:
       case MOVE_PICK:
            source = ModxId.get(msg.nextToken());
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
    public String shortMoveString()
    {
        switch (op)
        {
        case MOVE_PICKB:
 		case MOVE_DROPB:
            return (" - "+col + row);

        case MOVE_DROP:
        case MOVE_PICK:
            return (source.shortName);
            
        case MOVE_RACK_BOARD:
        	return(""+source.shortName+"-"+col + row);
 
        case MOVE_DONE:
            return ("");

        default:  
            return (D.findUnique(op));

        }
    }

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String moveString()
    {
        String ind = "";

        if (index() >= 0)
        {
            ind += (index() + " ");
        }
        // adding the move index as a prefix provides numbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        case MOVE_PICKB:
	        return (ind+D.findUnique(op) + " " + col + " " + row);

		case MOVE_DROPB:
	        return (ind+D.findUnique(op) + " " + col + " " + row);

		case MOVE_RACK_BOARD:
			return(ind+D.findUnique(op) + " "+source.shortName
					+ " " + col + " " + row);
        case MOVE_PICK:
            return (ind+D.findUnique(op) + " "+source.shortName);

        case MOVE_DROP:
             return (ind+D.findUnique(op) + " "+source.shortName);

        case MOVE_START:
            return (ind+"Start P" + player);

        default:
            return (ind+D.findUnique(op));
        }
    }

    /* standard java method, so we can read moves easily while debugging */
    //public String toString()
    //{
    //    return ("P" + player + "[" + moveString() + "]");
    //}
}
