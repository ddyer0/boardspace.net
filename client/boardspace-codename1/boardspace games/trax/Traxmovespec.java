package trax;

import online.game.*;

import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.ExtendedHashtable;


public class Traxmovespec extends commonMove implements TraxConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D);
        D.putInt("Pick", MOVE_PICK);
        D.putInt("Pickb", MOVE_PICKB);
        D.putInt("Drop", MOVE_DROP);
        D.putInt("Dropb", MOVE_DROPB);
        D.putInt("Rotateb",MOVE_ROTATEB);
        D.putInt("Move",MOVE_MOVE);

   }

    TraxId source; // where from/to
    public char to_col; // for from-to moves, the destination column
    public int to_row; // for from-to moves, the destination row
    public int to_row_after() { return((to_row==0) ? 1 : to_row); }
    public char to_col_after() { return((to_col=='@') ? 'A' : to_col); }
    public Traxmovespec()
    {
    } // default constructor

    /* constructor */
    public Traxmovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public Traxmovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    public Traxmovespec(int opcode,int mat,char col,int row,int who)
    {	// constructor for the robot
     	op = opcode;
    	source = MATCHTILES[mat];
    	to_col = col;
    	to_row = row;
    	player = who;
    }
    public boolean Same_Move_P(commonMove oth)
    {
        Traxmovespec other = (Traxmovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (player == other.player));
    }

    public void Copy_Slots(Traxmovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.source = source;
     }

    public commonMove Copy(commonMove to)
    {
        Traxmovespec yto = (to == null) ? new Traxmovespec() : (Traxmovespec) to;

        // we need yto to be a Traxmovespec at compile time so it will trigger call to the 
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
        case MOVE_MOVE:	// used by "trax" format games
        	to_col = G.CharToken(msg);
        	to_row = G.IntToken(msg);
        	source = TraxId.get(msg.nextToken());
        	break;
        case MOVE_ROTATEB:
        case MOVE_DROPB:
				source =TraxId.get(msg.nextToken());	// 0-5
	            to_col = G.CharToken(msg);
	            to_row = G.IntToken(msg);

	            break;

		case MOVE_PICKB:
            source = TraxId.BoardLocation;
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);

            break;

        case MOVE_DROP:
        case MOVE_PICK:
            source = TraxId.get(msg.nextToken());

            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

        default:

            break;
        }
    }

    private double chipScale[] = {1.0,0.75,0.3,-0.3};
    
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas v)
    {
    	return(shortMoveText((TraxGameViewer)v));
    }
    public Text shortMoveText(TraxGameViewer v)
    {
        switch (op)
        {
        case MOVE_PICKB:
            return (TextChunk.create(D.findUnique(op) +" " + to_col + " " + to_row));
            
        case MOVE_MOVE:
		case MOVE_DROPB:
		case MOVE_ROTATEB:
	       	{
			Text lead = TextChunk.create(""+to_col + " " + to_row);
			Text post = sourceGlyph(v);
			return(TextChunk.join(lead,post));
	       	}
 
        case MOVE_DROP:
        case MOVE_PICK:
        	{	Text lead = TextChunk.create(D.findUniqueTrans(op));
        		Text post = sourceGlyph(v);
        		return TextChunk.join(lead,post);
        	}
            
        case MOVE_DONE:
            return (TextChunk.create(""));

        default:
             return (TextChunk.create(D.findUniqueTrans(op)));
        }
    }
       
    private Text sourceGlyph(TraxGameViewer v)
    {
			Text post = null;
			TraxChip chips[] = v.imageGroup(2);
        	switch(source)
        	{
        	// note that / and \ are troublesome characters because of the potential that
        	// they are treated as escape characters.  The superfluous space after is a
        	// little extra protection.
        	default: throw G.Error("oops");
        	case hitTile0: 
        		post = TextGlyph.create("xxx",chips[0],v,chipScale);
        		break;
        	case hitTile1:
        		post = TextGlyph.create("xxx",chips[1],v,chipScale);
        		break;
        	case hitTile2: 
        		post = TextGlyph.create("xxx",chips[2],v,chipScale);
        		break;
        	case hitTile3:
        		post = TextGlyph.create("xxx",chips[3],v,chipScale);
        		break;
        	case hitTile4: 
        		post = TextGlyph.create("xxx",chips[4],v,chipScale);
        		break;
        	case hitTile5: 
        		post = TextGlyph.create("xxx",chips[5],v,chipScale);
        		break;
        	case hitPlus:
        		post = TextChunk.create( " + ");
        		break;
        	case hitSlash:
        		post = TextChunk.create(" / ");
        		break;
        	case hitBack:
        		post = TextChunk.create( " \\ ");
        		break;
        	}
        	if(post==null) { post = TextChunk.create(""); }
    	return(post);
            
    }
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String moveString()
    {
        String objstr = "";
		String indx = indexString();
		String opname = indx+D.findUnique(op)+" ";

		// adding the move index as a prefix provides numnbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
 
        case MOVE_PICKB:
	        return (opname+ to_col + " " + to_row);
 		case MOVE_DROPB:
 		case MOVE_ROTATEB:
 			objstr = source.shortName+" ";
 			//$FALL-THROUGH$
		case MOVE_MOVE:
		
			{
			String str = null;
	       	switch(source)
        	{
        	default: throw G.Error("oops");
        	case hitPlus:
        	case hitTile0: case hitTile1: str = "+"; break;
        	case hitSlash:
        	case hitTile2: case hitTile4: str = "/"; break;
        	case hitBack:
        	case hitTile3: case hitTile5: str = "\\"; break;
        	}
 
	        return (opname+ objstr + to_col + " " + to_row + " " + str);
			}
        case MOVE_DROP:
        case MOVE_PICK:
            return (opname+source.shortName);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
