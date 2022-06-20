package gipf;

import online.game.*;

import java.util.*;

import lib.G;
import lib.ExtendedHashtable;

public class Gipfmovespec extends commonMove implements GipfConstants
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
        	"Remove",MOVE_REMOVE,
        	"Slide",MOVE_SLIDE,
        	"Preserve",MOVE_PRESERVE,
        	"Standard",MOVE_STANDARD,
        	"SlideFrom", MOVE_SLIDEFROM);
   }

    GipfId source;
    char from_col; // for from-to moves, the source column
    int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    GipfState undostate;
    int undoinfo;
    
    /* constructor */
    public Gipfmovespec(){};
    
    // constructor for robot moves
    public Gipfmovespec(int pl,int opcode,char fc,int fr)
    {
    	player = pl;
    	op = opcode;
    	from_col = fc;
    	from_row = fr;
    	to_col = fc;
    	to_row = fr;
    }
    // constructor for robot moves
    public Gipfmovespec(int pl,int opcode,char fc,int fr,char tc,int tr)
    {
    	player = pl;
    	op = opcode;
    	from_col = fc;
    	from_row = fr;
    	to_col = tc;
    	to_row = tr;
    }
    public Gipfmovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public Gipfmovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    /* constructor */
    public Gipfmovespec(int playerindex, int scmd)
    {
        op = scmd;
        player = playerindex;
    }

    public boolean Same_Move_P(commonMove oth)
    {
        Gipfmovespec other = (Gipfmovespec) oth;

        return ((op == other.op)
        		&& (from_col == other.from_col)
        		&& (from_row == other.from_row) 
        		&& (to_row == other.to_row) 
        		&& (player == other.player));
    }
    public void Copy_Slots(Gipfmovespec to)
    {
    	super.Copy_Slots(to);
    	to.from_col = from_col;
    	to.from_row = from_row;
    	to.to_col = to_col;
    	to.to_row = to_row;
    	to.source = source;
    	to.undostate = undostate;
    	to.undoinfo = undoinfo;
    }
    public commonMove Copy(commonMove o)
    {	Gipfmovespec oo = (o==null) ? new Gipfmovespec() : (Gipfmovespec)o;
    	Copy_Slots(oo);
        return (oo);
    }

    /* parse a string into the state of this move */
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
        switch(op)
        {
        case MOVE_UNKNOWN:   throw G.Error("Can't parse %s", cmd);
        case MOVE_SLIDEFROM:
        case MOVE_SLIDE:
        	from_col = G.CharToken(msg);
        	from_row = G.IntToken(msg);
        	to_col = G.CharToken(msg);
        	to_row = G.IntToken(msg);
        	break;
        case MOVE_DROPB:
        	from_col = to_col = G.CharToken(msg);
        	from_row = to_row = G.IntToken(msg);
        	break;
        case MOVE_REMOVE:
        case MOVE_PICKB:
        case MOVE_PRESERVE:
        	from_col = G.CharToken(msg);
        	from_row = G.IntToken(msg);
        	break;
        case MOVE_DROP:
        case MOVE_PICK:
        	source = GipfId.get(msg.nextToken());
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
        case MOVE_PICK: return("");
        case MOVE_DROP:
        	return(source.shortName);
        case MOVE_REMOVE:
        	return("x "+from_col+" "+from_row);
        case MOVE_PRESERVE:
        	return("++ "+from_col+" "+from_row);
        case MOVE_PICKB:
        	return(""+from_col+" "+from_row);
        case MOVE_DROPB:
        	return(""+to_col+" "+to_row);
        case MOVE_START:
            return ("Start P"+player);
        case MOVE_SLIDE:
        	return(""+from_col+" "+from_row+"-"+to_col+" "+to_row);
        case MOVE_SLIDEFROM:
        	return(","+to_col+" "+to_row);
        case MOVE_DONE:
        	 return("");
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

        switch (op)
        {
        case MOVE_PICK:
        case MOVE_DROP:
        	return(opname+source.shortName);
        case MOVE_PICKB:
        case MOVE_REMOVE:
        case MOVE_PRESERVE:
        	return(opname+from_col+" "+from_row);
        case MOVE_DROPB:
           	return(opname+to_col+" "+to_row);
        case MOVE_START:
            return (indx+"Start P"+player);
        case MOVE_SLIDEFROM:
        case MOVE_SLIDE:
        	return(opname+from_col+" "+from_row+" "+to_col+" "+to_row);

        default:
            return (opname);
            
        }
    }
}
