/* copyright notice */package gounki;

import online.game.*;
import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.ExtendedHashtable;

public class GounkiMovespec extends commonMove implements GounkiConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
    			"Pickb", MOVE_PICKB,
        		"Dropb", MOVE_DROPB,
        		"Move",MOVE_BOARD_BOARD,
        		"Deploy",MOVE_DEPLOY,
        		"Dstep",MOVE_DEPLOYSTEP);
   }

    GounkiId source; // where from/to
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
	GounkiId dest;	// rack location for the dest
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    char to_col2;	// second space for deployment
    int to_row2;
    char to_col3;	// third space for deployment
    int to_row3;
    int undoInfo;	// the undoInfo of the move before undoInfo, for UNDO
    GounkiState state;
    GounkiChip chip;
    public GounkiMovespec()
    {
    } // default constructor
    public GounkiMovespec(char fc,int fr,char tc,int tr,int p)
    {	op = MOVE_BOARD_BOARD;
    	source = dest = GounkiId.BoardLocation;
    	from_col = fc;
    	to_col = tc;
    	from_row = fr;
    	to_row = tr;
    	player = p;
    }
 // deploy constructor
    public GounkiMovespec(GounkiCell c,GounkiCell d1,GounkiCell d2,GounkiCell d3,int p)
    {	op = MOVE_DEPLOY;
    	source = dest = GounkiId.BoardLocation;
    	player = p;
    	
    	from_col = c.col;
    	from_row = c.row;
    	
    	to_col = d1.col;
    	to_row = d1.row;
    	
    	to_col2 = d2.col;
    	to_row2 = d2.row;
    	// deployments must be at least 2 spaces, but third is optional.
    	if(d3!=null) 
    	{ 
    	to_row3 = d3.row;
    	to_col3 = d3.col;
    	}
    }
    /* constructor */
    public GounkiMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public GounkiMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    public boolean Same_Move_P(commonMove oth)
    {
    	GounkiMovespec other = (GounkiMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (undoInfo == other.undoInfo)
				&& (state == other.state)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (dest == other.dest)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (to_col2 == other.to_col2)
				&& (to_row2 == other.to_row2)
				&& (to_col3 == other.to_col3)
				&& (to_row3 == other.to_row3)
				&& (player == other.player));
    }

    public void Copy_Slots(GounkiMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.dest = dest;
        to.undoInfo = undoInfo;
        to.state = state;
        to.to_col3 = to_col3;
        to.to_col2 = to_col2;
        to.to_row3 = to_row3;
        to.to_row2 = to_row2;
        to.source = source;
        to.chip = chip;
    }

    public commonMove Copy(commonMove to)
    {
    	GounkiMovespec yto = (to == null) ? new GounkiMovespec() : (GounkiMovespec) to;

        // we need yto to be a GounkiMovespec at compile time so it will trigger call to the 
        // local version of Copy_Slots
        Copy_Slots(yto);

        return (yto);
    }

    /* parse a string into the undoInfo of this move.  Remember that we're just parsing, we can't
     * refer to the undoInfo of the board or the game.
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

        case MOVE_DEPLOY:
            source = GounkiId.BoardLocation;	
            dest = GounkiId.BoardLocation;
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg)+2;
  	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg)+2;
  	        to_col2 = G.CharToken(msg);		//to col row
	        to_row2 = G.IntToken(msg)+2;
	        if(msg.hasMoreTokens())
	        {
	  	        to_col3 = G.CharToken(msg);		//to col row
		        to_row3 = G.IntToken(msg)+2;
	        }
	        break;
        case MOVE_BOARD_BOARD:			// robot move from board to board
            source = GounkiId.BoardLocation;	
            dest = GounkiId.BoardLocation;
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg)+2;
  	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg)+2;
	        break;
        case MOVE_DEPLOYSTEP:   
        case MOVE_DROPB:
	       source = GounkiId.BoardLocation;
	       to_col = G.CharToken(msg);
	       to_row = G.IntToken(msg)+2;
	       break;

		case MOVE_PICKB:
            source = GounkiId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg)+2;
 
            break;

 
        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

        default:
 
            break;
        }
    }

    static double chipScale[] = {1,2,-0.2,-0.5};
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
            return icon(v,from_col,(from_row-2),"-");

        case MOVE_DEPLOYSTEP:
		case MOVE_DROPB:
            return icon(v,to_col , (to_row-2));

        case MOVE_DEPLOY:
        	String third = (to_col3>='A') ? "-"+to_col3+(to_row3-2) : "";
        	return icon(v,from_col, from_row," ",to_col,(to_row-2),"-",to_col2,(to_row2-2),third);
        case MOVE_BOARD_BOARD:
        	return icon(v,from_col ,from_row,"-",to_col, (to_row-2));
        case MOVE_DONE:
            return TextChunk.create("");

        default:
            return TextChunk.create(D.findUniqueTrans(op));

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
	        return (opname  + from_col + " " + (from_row-2));


		case MOVE_DEPLOY:
        	String third = (to_col3>='A') ? " "+to_col3+" "+(to_row3-2) : "";
        	return(opname+from_col+" "+ (from_row-2)+" "+to_col +" "+ (to_row-2)+" "+to_col2+" "+(to_row2-2)+third);
		case MOVE_BOARD_BOARD:
			return(opname  + from_col + " " + (from_row-2)+ " " + to_col + " " + (to_row-2));
 
		case MOVE_DEPLOYSTEP:
		case MOVE_DROPB:
	        return (opname  + to_col + " " + (to_row-2));

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
