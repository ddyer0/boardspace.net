/* copyright notice */package medina;

import online.game.*;
import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.ExtendedHashtable;

public class MedinaMovespec extends commonMPMove implements MedinaConstants
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
		D.putInt("OnBoard",MOVE_RACK_BOARD);

   }

    MedinaId source; // where from/to
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    MedinaChip chip = null;
    public MedinaMovespec()
    {
    } // default constructor

    /* constructor */
    public MedinaMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public MedinaMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    // for the robot
    public MedinaMovespec(MedinaId sour,char fc,int fr,char tc,int tr,int pla)
    {	op = MOVE_RACK_BOARD;
    	source = sour;
    	player = pla;
    	from_col = fc;
    	to_col = tc;
    	from_row = fr;
    	to_row = tr;
    	//if((source==PalaceLocation) && (from_col=='A') && (from_row==0) && (to_col=='B') && (to_row==12))
    	//{G.print("here "+this);
    	//}
    }
    public boolean Same_Move_P(commonMove oth)
    {
    	MedinaMovespec other = (MedinaMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(MedinaMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.chip= chip;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
    	MedinaMovespec yto = (to == null) ? new MedinaMovespec() : (MedinaMovespec) to;

        // we need yto to be a CarnacMovespec at compile time so it will trigger call to the 
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
        
        case MOVE_RACK_BOARD:	// a robot move from the rack to the board
            source = MedinaId.get(msg.nextToken());
            from_col = G.CharToken(msg);						// always
            from_row = G.IntToken(msg);			// index into the rack
 	        to_col = G.CharToken(msg);			// destination cell col
	        to_row = G.IntToken(msg);  			// destination cell row
	        break;
	        
	        
        case MOVE_DROPB:
	       source = MedinaId.BoardLocation;
	       to_col = G.CharToken(msg);
	       to_row = G.IntToken(msg);
	       break;

		case MOVE_PICKB:
            source = MedinaId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);

            break;

	    case MOVE_DROP:
            source = MedinaId.get(msg.nextToken());
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);
            break;

        case MOVE_PICK:
            source = MedinaId.get(msg.nextToken());
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
            break;
            
        case MOVE_START:
            player = D.getInt(msg.nextToken());
            break;

        default:

            break;
        }
    }


    double []getScale(MedinaChip ch)
    {
    	if(ch.isMeeple()) { return(new double[]{1.0,2.0,0,-0.2}); }
    	if(ch.isDome()) { return(new double[]{1.0,2.0,0.2,-0.5}); }
    	if(ch.isWall()) { return(new double[]{1.0,1.6,0.2,-0.2}); }
    	if(ch.isTea()) { return(new double[]{1.0,1.5,0.4,-0.2});}
    	return(new double[]{1.0,1.5,0,-0.2});
    }
    Text getChipGlyph(commonCanvas v)
    {
    	return((chip==null)
    			? TextChunk.create(source.shortName) 
    			: TextGlyph.create("xx",chip,v,getScale(chip)));
    }
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas v)
    {
        switch (op)
        {
        case MOVE_PICKB:
            return (TextChunk.create(""+from_col + from_row));

		case MOVE_DROPB:
            return (TextChunk.create(""+to_col + to_row));

        case MOVE_DROP:
        case MOVE_PICK:
            return (TextChunk.join(getChipGlyph(v),TextChunk.create(" ")));
        case MOVE_RACK_BOARD:
        	return(TextChunk.join(getChipGlyph(v),TextChunk.create(" "+to_col + to_row)));
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
 
        case MOVE_PICKB:
	        return (opname + from_col + " " + from_row);

		case MOVE_DROPB:
	        return (opname+ to_col + " " + to_row);

		case MOVE_RACK_BOARD:
			return(opname +source.shortName
					+ " " + from_col+" "+from_row
					+ " " + to_col + " " + to_row);

	    case MOVE_DROP:
            return (opname+source.shortName+ " " + to_col+" "+to_row);

	    case MOVE_PICK:
            return (opname+source.shortName+ " " + from_col+" "+from_row);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
