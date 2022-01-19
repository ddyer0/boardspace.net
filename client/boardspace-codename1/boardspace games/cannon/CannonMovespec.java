package cannon;

import online.game.*;
import java.util.*;

import lib.G;
import lib.ExtendedHashtable;

public class CannonMovespec extends commonMove implements CannonConstants
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
 			"Move",MOVE_BOARD_BOARD,
 			"Capture",CAPTURE_BOARD_BOARD,
 			"Retreat",RETREAT_BOARD_BOARD,
 			"Slide",SLIDE_BOARD_BOARD,
 			"Shoot2",SHOOT2_BOARD_BOARD,
 			"Shoot3",SHOOT3_BOARD_BOARD,
			"OnBoard",MOVE_RACK_BOARD);
   }

    CannonId source; // where from/to
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    CannonState state;	// the state of the move before state, for UNDO
    CannonChip capture=null;
    
    public CannonMovespec()
    {
    } 
    // for robot rack moves
    public CannonMovespec(int opc,CannonId s,int who,int src,char to_c,int to_r)
    {	op= opc;
    	source = s;
    	player = who;
    	from_col='@';
    	from_row = src;
    	to_col = to_c;
    	to_row = to_r;
    }
    // for robot board moves
    public CannonMovespec(int opc,CannonId src,int who,char from_c,int from_r,char to_c,int to_r)
    {	op= opc;
    	source = src;
    	player = who;
     	from_col = from_c;
    	from_row = from_r;
    	to_col = to_c;
    	to_row = to_r;
    }
    
    /* default constructor */
    public CannonMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public CannonMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    public boolean Same_Move_P(commonMove oth)
    {
    	CannonMovespec other = (CannonMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(CannonMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.capture = capture;
        to.state = state;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
    	CannonMovespec yto = (to == null) ? new CannonMovespec() : (CannonMovespec) to;

        // we need yto to be a CannonMovespec at compile time so it will trigger call to the 
        // local version of Copy_Slots
        Copy_Slots(yto);

        return (yto);
    }

    /* parse a string into the state of this move.  Remember that we're just parsing, we can't
     * refer to the state of the board or the game.
     * 
     */
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
            source = CannonId.find(msg.nextToken());	// white rack or black rack
            from_col = '@';						// always
            from_row = G.IntToken(msg);			// index into the rack
 	        to_col = G.CharToken(msg);			// destination cell col
	        to_row = G.IntToken(msg);  			// destination cell row
	        break;
        case CAPTURE_BOARD_BOARD:
        case RETREAT_BOARD_BOARD:
        case SLIDE_BOARD_BOARD:
        case SHOOT2_BOARD_BOARD:
        case SHOOT3_BOARD_BOARD:
        case MOVE_BOARD_BOARD:			// robot move from board to board
            source = CannonId.BoardLocation;		
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;
	        
        case MOVE_DROPB:
	       source = CannonId.BoardLocation;
	       to_col = G.CharToken(msg);
	       to_row = G.IntToken(msg);
	       break;

		case MOVE_PICKB:
            source = CannonId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
 
            break;

        case MOVE_PICK:
            source = CannonId.find(msg.nextToken());
            from_col = '@';
            from_row = G.IntToken(msg);
            break;
            
        case MOVE_DROP:
            source = CannonId.find(msg.nextToken());
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

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String shortMoveString()
    {
        switch (op)
        {
        case MOVE_PICKB:
            return (""+from_col + from_row);

		case MOVE_DROPB:
			{
			String bridge = (capture==null)?"-":"x";
			if(source==CannonId.BoardLocation)
			{	int dis = Math.max(Math.abs(from_col-to_col),Math.abs(from_row-to_row));
				switch(dis)
				{
				case 2: bridge = "/"; break;
				case 3: bridge = ">"; break;
				case 4: bridge = ">>"; break;
				case 5: bridge = ">>>"; break;
				default:
					break;
				}
			}
            return (bridge+to_col + to_row);
			}
        case MOVE_DROP:
        case MOVE_PICK:
            return (source.shortName);
        case MOVE_RACK_BOARD:
        	return(source.shortName+from_row+"-"+to_col + to_row);
        case CAPTURE_BOARD_BOARD:
        	return(""+from_col + from_row+"x"+to_col + to_row);
        case RETREAT_BOARD_BOARD:
        	return(""+from_col + from_row+"/"+to_col + to_row);
        case MOVE_BOARD_BOARD:
        	return(""+from_col + from_row+"-"+to_col + to_row);
        case SLIDE_BOARD_BOARD:
        	return(""+from_col + from_row+">"+to_col + to_row);
        case SHOOT2_BOARD_BOARD:
        	return(""+from_col + from_row+">>"+to_col + to_row);
        case SHOOT3_BOARD_BOARD:
        	return(""+from_col + from_row+">>>"+to_col + to_row);

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
        case MOVE_PICKB:
	        return (ind+opname+ from_col + " " + from_row);

		case MOVE_DROPB:
	        return (ind+opname+ to_col + " " + to_row);

		case MOVE_RACK_BOARD:
			return(ind+opname+source.shortName+ " "+from_row
					+ " " + to_col + " " + to_row);
		case CAPTURE_BOARD_BOARD:
		case RETREAT_BOARD_BOARD:
		case SLIDE_BOARD_BOARD:
		case SHOOT2_BOARD_BOARD:
		case SHOOT3_BOARD_BOARD:
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

}
