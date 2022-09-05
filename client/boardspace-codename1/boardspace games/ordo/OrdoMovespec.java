package ordo;

import online.game.*;
import ordo.OrdoConstants.OrdoId;

import java.util.*;

import lib.G;
import lib.ExtendedHashtable;

public class OrdoMovespec
	extends commonMove 		// for a multiplayer game, this will be commonMPMove 
{
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; 	// pick a chip from a pool
    static final int MOVE_DROP = 205; 	// drop a chip
    static final int MOVE_PICKB = 206; 	// pick from the board
    static final int MOVE_DROPB = 207; 	// drop on the board
    static final int MOVE_RETAIN = 208;	// retain a group
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
	static final int MOVE_CAPTURE = 212;	// direct capture from-to for ordo
	static final int MOVE_ORDO = 214;		// slide an ordo block
	static final int MOVE_SELECT = 215;		// select a chip to be moved

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
    			"Pick", MOVE_PICK,
       			"Pickb", MOVE_PICKB,
       			"Drop", MOVE_DROP,
       			"Dropb", MOVE_DROPB,
       			"Move",MOVE_BOARD_BOARD,
       			"Capture",MOVE_CAPTURE,
       			"Ordo",MOVE_ORDO,
       			"Retain",MOVE_RETAIN,
       			"Select",MOVE_SELECT);
   }

    OrdoId source; // where from/to
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    char target_col;	// for captures, the cell that is captured
    int target_row;	// for captures, the cell that is captured
    public OrdoMovespec() // default constructor
    {
    }
    public OrdoMovespec(int opc, int pl)	// constructor for simple moves
    {
    	player = pl;
    	op = opc;
    }
    /* constructor */
    public OrdoMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public OrdoMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    /* constructor for robot moves */
    public OrdoMovespec(int opc,OrdoCell from,OrdoCell to,int who)
    {
    	player = who;
    	op = opc;
    	from_col = from.col;
    	from_row = from.row;
    	to_col = to.col;
    	to_row = to.row;
    }
    /* constructor for robot moves */
    public OrdoMovespec(int opc,OrdoCell from,OrdoCell fromend,OrdoCell to,int who)
    {
    	player = who;
    	op = opc;
    	from_col = from.col;
    	from_row = from.row;
    	target_col = fromend.col;
    	target_row = fromend.row;
    	to_col = to.col;
    	to_row = to.row;
    }  
    public OrdoMovespec(int moveOrdo, OrdoCell from, int size, OrdoCell to, OrdoCell target,int who)
    {
    	op = moveOrdo;
    	player = who;
    	source = OrdoId.BoardLocation;	
    	from_col = from.col;
    	from_row = from.row;
    	to_col = to.col;
    	to_row = to.row;
    	target_col = target.col;
    	target_row = target.row;
    }
	/**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
    	OrdoMovespec other = (OrdoMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(OrdoMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.source = source;
        to.target_col = target_col;
        to.target_row = target_row;
    }

    public commonMove Copy(commonMove to)
    {
    	OrdoMovespec yto = (to == null) ? new OrdoMovespec() : (OrdoMovespec) to;

        // we need yto to be a OrdoMovespec at compile time so it will trigger call to the 
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
        case MOVE_ORDO:
           	source = OrdoId.BoardLocation;		
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
            target_col = G.CharToken(msg);	//from col,row
            target_row = G.IntToken(msg);
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;

        case MOVE_CAPTURE:
        case MOVE_BOARD_BOARD:			// robot move from board to board
        	source = OrdoId.BoardLocation;		
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;
	        
 		case MOVE_DROPB:
		case MOVE_PICKB:
		case MOVE_RETAIN:
		case MOVE_SELECT:
            source = OrdoId.BoardLocation;
            to_col = from_col = G.CharToken(msg);
            to_row = from_row = G.IntToken(msg);

            break;

        case MOVE_PICK:
            source = OrdoId.find(msg.nextToken());
            from_col = '@';
            from_row = G.IntToken(msg);
            break;
            
        case MOVE_DROP:
           source =OrdoId.find(msg.nextToken());
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
        case MOVE_SELECT:
        case MOVE_RETAIN:
            return G.concat("",from_col , from_row);
		case MOVE_DROPB:
            return G.concat(" - ",to_col , to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return (source.shortName);
        case MOVE_CAPTURE:
        	return G.concat("",from_col , from_row,"x",to_col , to_row);
        case MOVE_BOARD_BOARD:
        	return G.concat("",from_col , from_row,"-",to_col , to_row);
        case MOVE_ORDO:
        	return G.concat("",from_col , from_row," x ",to_col , to_row);
        case MOVE_DONE:
            return ("");

        default:
            return (D.findUniqueTrans(op));
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
        case MOVE_PICKB:
        case MOVE_SELECT:
        case MOVE_RETAIN:
	        return (G.concat(opname,  from_col , " " , from_row));

 		case MOVE_DROPB:
	        return (G.concat(opname , to_col , " " , to_row));

		case MOVE_ORDO:
			return G.concat(opname,  from_col , " " , from_row
					, " " , target_col," ",target_row
					, " " , to_col , " " , to_row);
		case MOVE_BOARD_BOARD:
		case MOVE_CAPTURE:
			return G.concat(opname ,from_col , " " , from_row
					, " " , to_col , " " , to_row);
        case MOVE_PICK:
            return G.concat(opname , source.shortName, " ",from_row);

        case MOVE_DROP:
             return G.concat(opname, source.shortName, " ",to_row);

        case MOVE_START:
            return G.concat(indx,"Start P" , player);

        default:
            return G.concat(D.findUnique(op));
        }
    }
    /*
    public void setGameover(boolean v)
    {	//if(visit())
    	//	{
    	//	G.Error("makeover");
    	//	}
    	super.setGameover(v);
    }
    */
   /* 
    public boolean visit()
    {	
    	//if( (from_col=='F' && from_row==10 && to_col=='E' && to_row==9))
    	//{
    	//	UCTNode uct = uctNode();
    	//	if(uct!=null) { UCTNode.marked = uct; }
    	//	return(true);
    	//}
    	return(false);
    	//		||(from_col=='C' && from_row==5 && to_col=='A' && to_row==7);
    	//return(false);
    }
    */
    /* standard java method, so we can read moves easily while debugging */
    //public String toString()
    //{
    //    return ("P" + player + "[" + moveString() + "]");
    //}
}
