package exxit;

import online.game.*;
import java.util.*;

import lib.G;
import lib.ExtendedHashtable;

public class Exxitmovespec extends commonMove implements ExxitConstants
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
        	"Exchange",MOVE_EXCHANGE,
        	"Move",MOVE_MOVE);
    }

    ExxitId source; // where from/to
    ExxitId object;	// object being picked/dropped
    char from_col;
    int from_row;
    int direction;
    int undoDistributionInfo;		// info for robot undo
    ExxitPiece []undoExchangeInfo;	//  exhange undo
    String shortMoveString="";
    ExxitState state;	// the state of the move before state, for UNDO
    int undoInfo;
    
    public Exxitmovespec()
    {
    } // default constructor

    /* constructor */
    public Exxitmovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    
    // constructor for distribution moves, for use by the robot
    public Exxitmovespec(char col,int row,int dir,ExxitId id,int who)
    {	op = MOVE_MOVE;
    	source = ExxitId.BoardLocation;
    	object = id;
    	from_col = col;
    	from_row = row;
    	direction = dir;
    	player = who;
    }
    // constructor for dropb type moves, fo ruse by the robot
    public Exxitmovespec(int opcode,ExxitId src,char col,int row,int who)
    {	op = opcode;
   		source = ExxitId.BoardLocation;
       	object = src;
    	from_col = col;
    	from_row = row;
    	player = who;
    }
    /* constructor */
    public Exxitmovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    public Exxitmovespec(int who,int opcode)
    {	player = who;
    	op = opcode;
    }
    public boolean Same_Move_P(commonMove oth)
    {
        Exxitmovespec other = (Exxitmovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (object == other.object)
				&& (state == other.state)
				&& (undoInfo == other.undoInfo)
				&& (from_col==other.from_col)
				&& (from_row==other.from_row)
				&& (direction == other.direction)
				&& (undoDistributionInfo == other.undoDistributionInfo)
				&& (undoExchangeInfo == other.undoExchangeInfo)
				&& (player == other.player));
    }

    public void Copy_Slots(Exxitmovespec to)
    {	super.Copy_Slots(to);
		to.object = object;
		to.from_row = from_row;
		to.from_col = from_col;
		to.direction = direction;
        to.state = state;
        to.undoInfo = undoInfo;
        to.undoDistributionInfo = undoDistributionInfo;
        to.undoExchangeInfo = undoExchangeInfo;
        to.source = source;
        to.shortMoveString = shortMoveString;
    }

    public commonMove Copy(commonMove to)
    {
        Exxitmovespec yto = (to == null) ? new Exxitmovespec() : (Exxitmovespec) to;

        // we need yto to be a Exxitmovespec at compile time so it will trigger call to the 
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
        		{
        		object = ExxitId.get(msg.nextToken());
        		from_col = G.CharToken(msg);
        		from_row = G.IntToken(msg);
        		direction = G.IntToken(msg);
         		}
        		break;
        case MOVE_DROPB:
	            source = ExxitId.BoardLocation;
				object = ExxitId.get(msg.nextToken());	// object to object number
	            from_col = G.CharToken(msg);
	            from_row = G.IntToken(msg);
	            break;

        case MOVE_EXCHANGE:
		case MOVE_PICKB:
            source = ExxitId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
            object = ExxitId.get(msg.nextToken());

            break;

        case MOVE_DROP:
            source = ExxitId.get(msg.nextToken());
            break;
            
        case MOVE_PICK:		// "pick { B W } 
            source = ExxitId.get(msg.nextToken());
            from_col = '@';
            from_row = 0;
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
        	return("");
        case MOVE_EXCHANGE:
        case MOVE_MOVE:
 		case MOVE_DROPB:
            return (shortMoveString);

        case MOVE_DROP:
            return (D.findUnique(op) + " "+source.shortName);
            
        case MOVE_PICK:
            return (source.shortName);

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
		String ind = (index() >= 0) ? (index() + " ") : "";
		String opname = D.findUnique(op)+" ";
        // adding the move index as a prefix provides numnbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        case MOVE_EXCHANGE:
        case MOVE_PICKB:
	        return (ind+opname+ from_col + " " + from_row+" "+object.shortName);

        case MOVE_MOVE:
	        return (ind+opname+object.shortName+" " + from_col+" "+from_row+" "+direction);

        case MOVE_DROPB:
	        return (ind+opname+object.shortName+" " + from_col + " " + from_row);

        case MOVE_DROP:
            return (ind+opname+source.shortName);

        case MOVE_PICK:
            return (ind+opname+source.shortName);

        case MOVE_START:
            return (ind+"Start P" + player);

        default:
            return (ind+opname);
        }
    }
    public String longMoveString()
    {	String str = moveString();
    	if(!"".equals(shortMoveString)) { str += " @"+shortMoveString; }
    	return(str);
    }

}
