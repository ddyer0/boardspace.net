package crossfire;

import java.util.*;

import lib.G;
import online.game.*;
import lib.ExtendedHashtable;
public class CrossfireMovespec extends commonMove implements CrossfireConstants
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);

    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D);	// this adds "start" "done" "edit" and so on.
        D.putInt("Pick", MOVE_PICK);
        D.putInt("Pickb", MOVE_PICKB);
        D.putInt("Drop", MOVE_DROP);
        D.putInt("Dropb", MOVE_DROPB);
        D.putInt("Move", MOVE_FROM_TO);
        D.putInt("Reserve", MOVE_FROM_RESERVE);
        D.putInt("RobotResign",MOVE_ROBOT_RESIGN);
  }
    //
    // variables to identify the move
    CrossId source; // where from/to
	String object;	// object being picked/dropped
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    char from_col;
    int from_row;
    int height=0;
    int undoInfo=0;
    //
    // variables for use by the robot
    CrossfireState state;	// the state of the move before state, for robot UNDO
    
    public CrossfireMovespec()
    {
    } // default constructor

    /* constructor */
    public CrossfireMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    /* constructor */
    public CrossfireMovespec(int opc,int p)
    {
    	op = opc;
    	player = p;
    }
    /* constructor for move from reserve */
    public CrossfireMovespec(int opc,CrossId src,char col,int row,int pl)
    {
    	op = opc;
    	source = src;
    	to_col = col;
    	to_row = row;
    	player = pl;
    }
    /* constrctor for move-from-to */
    public CrossfireMovespec(int opc,char fc,int fr,char tc,int tr,int dist,int who)
    {
    	op = opc;
    	player = who;
    	from_col = fc;
    	from_row = fr;
    	to_col = tc;
    	to_row = tr;
    	height = dist;
    }
    /* constructor */
    public CrossfireMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    public boolean Same_Move_P(commonMove oth)
    {
        CrossfireMovespec other = (CrossfireMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (object == other.object)
				&& (state == other.state)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (height==other.height)
				&& (player == other.player));
    }

    public void Copy_Slots(CrossfireMovespec to)
    {	super.Copy_Slots(to);
 		to.object = object;
        to.to_col = to_col;
        to.to_row = to_row;
        to.state = state;
        to.height = height;
        to.source = source;
        to.from_col = from_col;
        to.from_row = from_row;
        to.undoInfo = undoInfo;
    }

    public commonMove Copy(commonMove to)
    {
        CrossfireMovespec yto = (to == null) ? new CrossfireMovespec() : (CrossfireMovespec) to;

        // we need yto to be a CrossfireMovespec at compile time so it will trigger call to the 
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
        	throw G.Error("Can't parse %s", cmd);
        case MOVE_FROM_TO:
        	source = CrossId.BoardLocation;
        	from_col = G.CharToken(msg);
        	from_row = G.IntToken(msg);
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);
            height = G.IntToken(msg);
            break;
        case MOVE_FROM_RESERVE:
        	source = CrossId.get(msg.nextToken());
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);
            break;
        case MOVE_DROPB:
	            source = CrossId.EmptyBoard;
				object = msg.nextToken();	// B or W
	            to_col = G.CharToken(msg);
	            to_row = G.IntToken(msg);
	            height = G.IntToken(msg);
	            break;

		case MOVE_PICKB:
            source = CrossId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);

            break;

        case MOVE_DROP:
        case MOVE_PICK:
            source = CrossId.get(msg.nextToken());

            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

        default:

            break;
        }
    }

    /** construct an abbreviated move string, mainly for use in the game log.  These
     * don't have to be parseable, they're intended only to help humans understand
     * the game record.
     * */
    public String shortMoveString()
    {
        switch (op)
        {
        case MOVE_PICKB:
            return (""+from_col + from_row+"-");

		case MOVE_DROPB:
            return (""+to_col + to_row);

        case MOVE_DROP:
        	return("-R");
        case MOVE_PICK:
            return ("R-");

        case MOVE_DONE:
            return ("");

        case MOVE_ROBOT_RESIGN:
        	return(RESIGN);
        default:
            return (D.findUniqueTrans(op));

        case MOVE_FROM_TO:
        	return(""+from_col+from_row+"-"+to_col+to_row);
        case MOVE_FROM_RESERVE:
        	return("R-"+to_col+to_row);
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
	        return (opname  + from_col + " " + from_row);

		case MOVE_DROPB:
	        return (opname+object+" " + to_col + " " + to_row+" "+height);

        case MOVE_DROP:
        case MOVE_PICK:
            return (opname+source.shortName);

        case MOVE_START:
            return (indx+"Start P" + player);
        case MOVE_FROM_TO:
        	return(opname+from_col+" "+from_row+" "+to_col+" "+to_row+" "+height);
        case MOVE_FROM_RESERVE:
        	return(opname+source.shortName+" "+to_col+" "+to_row);
        default:
            return (opname);
        }
    }

}
