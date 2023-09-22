/* copyright notice */package micropul;

import online.game.*;
import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.ExtendedHashtable;

public class Micropulmovespec extends commonMove implements MicropulConstants
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
        	"Rotate", MOVE_ROTATE,
        	"Move",MOVE_MOVE,
        	"RRack",MOVE_RRACK);
    }

    MicroId source; // where from
    MicroId dest;	// where to
    char from_col;
    int from_row;
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    int rotation;	// rotation of the tile
    int tile;
    MicropulChip chip;		// for the game log
    MicropulState state;	// the state of the move before state, for UNDO
    int undoInfo;
    public Micropulmovespec()
    {
    } // default constructor

    /* constructor */
    public Micropulmovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    public Micropulmovespec(int pl,MicroId from,char fc, int fr, MicroId to,char tc, int tr,int rot)
    {
    	op = MOVE_MOVE;
    	player = pl;
    	source = from;
    	from_col = fc;
    	from_row = fr;
    	dest = to;
    	to_col = tc;
    	to_row = tr;
    	rotation = rot;
    }
    /* constructor */
    public Micropulmovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    public boolean Same_Move_P(commonMove oth)
    {
        Micropulmovespec other = (Micropulmovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (dest == other.dest)
				&& (state == other.state)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (tile == other.tile)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (rotation == other.rotation)
				&& (undoInfo == other.undoInfo)
				&& (player == other.player));
    }

    public void Copy_Slots(Micropulmovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.tile = tile;
        to.state = state;
        to.undoInfo = undoInfo;
        to.rotation = rotation;
        to.source = source;
        to.dest = dest;
        to.chip = chip;
    }

    public commonMove Copy(commonMove to)
    {
        Micropulmovespec yto = (to == null) ? new Micropulmovespec() : (Micropulmovespec) to;

        // we need yto to be a Movespec at compile time so it will trigger call to the 
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
        	source = MicroId.get(msg.nextToken());
        	from_col = G.CharToken(msg);
        	from_row = G.IntToken(msg);
        	dest = MicroId.get(msg.nextToken());
        	to_col = G.CharToken(msg);
        	to_row = G.IntToken(msg);
        	rotation = G.IntToken(msg);
        	break;
        case MOVE_RRACK:
        	dest = source = MicroId.Rack;
        	player = D.getInt(msg.nextToken());
        	to_row = G.IntToken(msg);
        	rotation = G.IntToken(msg);
        	break;
        case MOVE_ROTATE:
        		dest = source = MicroId.BoardLocation;
        		to_col = G.CharToken(msg);
        		to_row = G.IntToken(msg);
        		rotation = G.IntToken(msg);
        		break;
        case MOVE_DROPB:
	            dest = MicroId.EmptyBoard;
				to_col = G.CharToken(msg);
	            to_row = G.IntToken(msg);
	            rotation = G.IntToken(msg);
	            break;

		case MOVE_PICKB:
            source = MicroId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
            break;

        case MOVE_DROP:
        	dest = MicroId.get(msg.nextToken());
        	switch(dest)
        	{
        	case Core: break;
        	case Supply:
        	case Jewels: player = D.getInt(msg.nextToken());
        		break;
        	case Rack:
        		player = D.getInt(msg.nextToken());
        		to_col = '@';
        		to_row = G.IntToken(msg);
				break;
			default:
				break;
        	}
        	break;
        	
        case MOVE_PICK:
            source = MicroId.get(msg.nextToken());
           	switch(source)
        	{
        	case Core: break;
        	case Supply:
        	case Jewels: player = D.getInt(msg.nextToken());
        		break;
        	case Rack:
        		player = D.getInt(msg.nextToken());
        		from_col = '@';
        		from_row = G.IntToken(msg);
        		break;
			default:
				break;
        	}
            tile = G.IntToken(msg);
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
    {	double scale[] = new double[]{1,0.9,0,-0.3};
        switch (op)
        {
        case MOVE_MOVE:
        	if(source==MicroId.Jewels) { scale[1]=2.0; }
        	return(TextChunk.join(
        			TextGlyph.create("xxx",chip,v,scale),
        			TextChunk.create((dest==MicroId.BoardLocation)?" "+to_col+to_row:"")));
        	
        case MOVE_PICKB:
            return (TextChunk.create(""+ from_col + " " + from_row));
        case MOVE_RRACK:
        	return(TextChunk.create("RR "+D.findUnique(player)+" "+ to_row+ " "+rotation));
        case MOVE_ROTATE:
        	return(TextChunk.create("R "+to_col + to_row));
 
		case MOVE_DROPB:
            return (TextChunk.create(" "+to_col + to_row));
        case MOVE_PICK:
        	{
        	String msg = "";
        	switch(source)
        	{
        	case Jewels:
        			scale[1]=2.0;     			
				//$FALL-THROUGH$
			case Supply: 
         	case Core: 
        	case Rack:
			default:
				break;
        	}
            return (TextChunk.join(
            		TextGlyph.create("xxx",chip,v,scale),
            		TextChunk.create(msg)));
        	}
        case MOVE_DROP:
            return (TextChunk.create(""));
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

        case MOVE_MOVE:
        	return(opname+source.shortName
    			+ " " + from_col+" "+from_row
    			+ " " + dest.shortName
    			+ " " + to_col+" "+to_row
    			+ " " + rotation);

        case MOVE_PICKB:
	        return (opname+ from_col + " " + from_row);
        case MOVE_RRACK:
	        return (opname + D.findUnique(player) + " " + to_row+" "+rotation);
        case MOVE_ROTATE:
 		case MOVE_DROPB:
	        return (opname + to_col + " " + to_row+" "+rotation);

        case MOVE_PICK:
    	{
    	String msg = opname+source.shortName+" ";
    	switch(source)
	    	{
	    	case Jewels:
	    	case Supply: msg += " "+D.findUnique(player);
	    		break;
	    	case Core: break;
	    	case Rack:
	    		msg += " "+D.findUnique(player) + " "+from_row;
	    		break;
		default:
			break;
	    	}
    	msg += " "+tile;
        return (msg);
    	}
    case MOVE_DROP:
   		{
    	String msg = opname+dest.shortName+" ";
    	switch(dest)
	    	{
	    	case Jewels:
	    	case Supply: msg += " "+D.findUnique(player);
	    		break;
	    	case Core: break;
	    	case Rack:
	    		msg += " "+D.findUnique(player) + " "+to_row;
	    		break;
	    	default:
	    		break;
	    	}
	        return (msg);
    	}
     case MOVE_START:
            return (indx+"Start P" + player);

     default:
    	 return (opname);
        }
    }

}
