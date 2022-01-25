package plateau.common;

import java.util.*;

import lib.G;
import online.game.*;
import lib.ExtendedHashtable;

public class plateaumove extends commonMove implements PlateauConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true); // dictionary 

    static
    {	addStandardMoves(D);
        D.put("move", MOVE_FROMTO);
        D.put("onboard", MOVE_ONBOARD);
        D.put("flip", MOVE_FLIP);

        D.put("pick", MOVE_PICK);
        D.put("drop", MOVE_DROP);

    }

    int pick; 				// piece number that was picked up or dropped off
    private int drop; 		// stack number that was dropped on, or -1 for an off-board drop
    int level;				// level in the stack were it was dropped
    String locus = ""; 		//X,Y etc
    String pubColors = ""; 	//visible colors
    String pieces = ""; 	//pieces in the stack
    String realColors = ""; //real colors
    boolean flip = false; 	// for move record
    String tolocus = ""; 	// for move record
    PlateauState undostate; // state for undo of express execute
    int undoStackIndex = 0;
    PlateauState state_after_execute=PlateauState.PUZZLE_STATE;	// hint for editHistory
    public plateaumove()
    {
    }

    /* constructor */
    public plateaumove(int opc,int pl)
    {
        player = pl;
        op = opc;
    }

    /* constructor */
    public plateaumove(String str,int pl)
    { //System.out.println("Mv: "+str);
        parse(new StringTokenizer(str),pl);
    }

    /* constructor */
    public plateaumove(StringTokenizer ss,int pl)
    {
        parse(ss,pl);
    }

    /* true of this other move is the same as this one */
    public boolean Same_Move_P(commonMove o)
    {
        plateaumove other = (plateaumove) o;

        return ((op == other.op)
        		&& (level == other.level) 
        		&& (pick == other.pick)
        		&& (player == other.player));
    }

    public void Copy_Slots(plateaumove to)
    {
        super.Copy_Slots(to);
        to.pick = pick;
        to.drop = drop;
        to.level = level;
        to.locus = locus;
        to.tolocus = tolocus;
        to.pubColors = pubColors;
        to.pieces = pieces;
        to.realColors = realColors;
        to.undostate = undostate;
        to.flip = flip;
        to.state_after_execute=state_after_execute;
        to.undoStackIndex = undoStackIndex;
    }

    public commonMove Copy(commonMove to)
    {
        plateaumove other = (to == null) ? new plateaumove() : (plateaumove) to;
        Copy_Slots(other);

        return (other);
    }

    /* parse a string into the state of this move */
    private void parse(StringTokenizer msg,int pl)
    {
        String cmd = msg.nextToken();

        if (Character.isDigit(cmd.charAt(0)))
        {
            setIndex(G.IntToken(cmd));
            cmd = msg.nextToken();
        }

        player = pl;

        op = D.getInt(cmd, MOVE_UNKNOWN);

        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Can't parse %s", cmd);
        case MOVE_FROMTO:
        {
            locus = msg.nextToken();
            level = G.IntToken(msg);
            realColors = msg.nextToken();
            tolocus = msg.nextToken();

            if ("F".equals(locus.substring(0, 1).toUpperCase()))
            {
                flip = true;
                locus = locus.substring(1);
            }

            pubColors = realColors.substring(0, 1);
        }

        break;

        case MOVE_ONBOARD:
        {
            locus = msg.nextToken();
            level = G.IntToken(msg);
            realColors = msg.nextToken();
            pubColors = realColors.substring(0, 1);
            pieces = msg.nextToken();
        }

        break;

        case MOVE_FLIP:
        {
            pick = G.IntToken(msg);

            if (msg.hasMoreTokens())
            {
                locus = msg.nextToken();
            }

            if (msg.hasMoreTokens())
            {
                pubColors = msg.nextToken();
            }
        }

        break;

        case MOVE_PICK:
        {
            pick = G.IntToken(msg);

            if (msg.hasMoreTokens())
            {
                locus = msg.nextToken();
                level = msg.hasMoreTokens()?G.IntToken(msg): 0;
            }
        }

        break;

        case MOVE_DROP:
        {
            drop = G.IntToken(msg);
            level = G.IntToken(msg);

            if (drop == -1)
            {
                level = 99;
            }

            if (msg.hasMoreTokens())
            {
                locus = msg.nextToken();
            }
        }

        break;

        case MOVE_START: // start playing (not a game move)
        	{
            player = D.getInt(msg.nextToken());
        	}
        	break;

        default:
            break;
        }
    }

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String moveString()
    {	String indx = indexString();
		String opname = indx+D.findUnique(op)+" ";

        switch (op)
        {
        case MOVE_ONBOARD:
            return (opname + locus + " " + level + " " + realColors + " " +
            pieces);

        case MOVE_FROMTO:
            return (opname + (flip ? "F" : "") + locus + " " + level + " " +
            realColors + " " + tolocus);

        case MOVE_FLIP:
            return (opname + pick + " " + locus + " " + pubColors);

        case MOVE_PICK:
        	if("".equals(locus)) { return(opname+pick); }
            return (opname + pick + " " + locus + " " + level);

        case MOVE_DROP:
            return (opname + drop + " " + level + " " + locus);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
        	return(opname);

        }
    }

    private String levelString()
    {
        return ((level == 100) ? "" : ("(" + level + ")"));
    }

    public String shortMoveString()
    {
        switch (op)
        {
        case MOVE_ONBOARD:
            return ("+ " + pubColors + "@" + locus + levelString());

        case MOVE_FROMTO:
            return ("M " + (flip ? "F" : "") + locus + " " + level + " " +
            pubColors + " " + tolocus);

        case MOVE_FLIP:

            if ("R".equals(locus))
            {
                return ("");
            }

            return ("F" + locus + "=" + pubColors);

        case MOVE_PICK:

            if ("R".equals(locus))
            {
                return ("");
            }

            if ("T".equals(locus) || "P".equals(locus))
            { // shuffle prisoners to and from exchange

                return (pieceTypeStr[pick]);
            }

            return (locus + ((level == 0) ? "" : levelString()));

        case MOVE_DROP:

            if ("R".equals(locus))
            {
                return ("");
            }

            return ("-" + locus + levelString());

        default:
             return(D.findUnique(op));

        case MOVE_DONE:
            return ("");
        }
    }

    public int drop()
    {
        return (drop);
    }

    public void setDrop(int n)
    {
        if ((op == MOVE_PICK) && (n == -1))
        {
        	throw G.Error("drop -1");
        }

        drop = n;
    }

    /* standard java method, so we can read moves easily while debugging */
    public String toString()
    {
        return ("P" + player + "[" + moveString() + "]");
    }
}
