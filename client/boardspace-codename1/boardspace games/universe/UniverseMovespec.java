package universe;

import com.codename1.ui.geom.Rectangle;

import java.util.*;
import lib.G;
import lib.IStack;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;

import online.game.*;
import lib.ExtendedHashtable;

public class UniverseMovespec extends commonMPMove implements UniverseConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D);
        D.putInt("Pick", MOVE_PICK);
        D.putInt("Pickb", MOVE_PICKB);
        D.putInt("PickG", MOVE_PICKGIVEN);
        D.putInt("Drop", MOVE_DROP);
        D.putInt("Dropb", MOVE_DROPB);
		D.putInt("OnBoard",MOVE_RACK_BOARD);
		D.putInt("RotateCW", MOVE_ROTATE_CW);
		D.putInt("RotateCCW", MOVE_ROTATE_CCW);
		D.putInt("Flip", MOVE_FLIP);
		D.putInt("Unpick", MOVE_UNPICK);
		D.putInt("Assign",MOVE_ASSIGN);
		D.putInt("Link",MOVE_LINK);
		D.putInt("Gameover",MOVE_GAMEOVER);
		D.putInt("AllDone",MOVE_ALLDONE);
   }
   //
   // removed these, 3/2015.  They messed up the presentation of moves in game logs.
   // I think they were obsoleted by the "liteMove" implementation in UniversePlay
   //
   // public int hashCode()
   // {
   // 	return((to_row<<12)+(to_col<<18)+(player<<24)+(op<<25)+(from_col<<4)+(from_row+rotation<<14));
   // }
   // public boolean equals(Object a)
   // {
   // 	return( (a instanceof commonMove) && Same_Move_P((commonMove)a)); 
   // }
    
	char from_col='A'; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col='A'; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    int rotation;	// rotation and flip for onboard moves
    int assignedValues[];
    UniverseChip chip = null;
    
    public UniverseMovespec()
    {
    } // default constructor

    /* constructor */
    public UniverseMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public UniverseMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    /* constructor for onboard moves */
    public UniverseMovespec(int opc,char from_c,int from_r,int rot,boolean flipped,char to_c,int to_r,int who)
    {	op = opc;
    	player = who;
    	to_col = to_c;
    	to_row = to_r;
    	from_col = from_c;
    	from_row = from_r;
    	rotation = rot + (flipped?4:0);
    }
    /* constructor for link moves */
    public UniverseMovespec(int opc,char from_c,int from_r,char to_c,int to_r,int who)
    {	op = opc;
    	player = who;
    	to_col = to_c;
    	to_row = to_r;
    	from_col = from_c;
    	from_row = from_r;
    }
    public UniverseMovespec(int opc,char from_c,int from_r,int[]values)
    {	op = opc;
    	from_col = from_c;
    	from_row = from_r;
    	assignedValues = values;
    }
    public UniverseMovespec(int opc,int who)
    {	op = opc;
    	player = who;
    }

    public boolean Same_Move_P(commonMove oth)
    {
    	UniverseMovespec other = (UniverseMovespec) oth;

        return ((op == other.op) 
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (rotation == other.rotation)
				&& (player == other.player));
    }

    public void Copy_Slots(UniverseMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.rotation = rotation;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.chip = chip;
        to.assignedValues = assignedValues;
        
    }

    public commonMove Copy(commonMove to)
    {
    	UniverseMovespec yto = (to == null) ? new UniverseMovespec() : (UniverseMovespec) to;

        // we need yto to be a UniverseMovespec at compile time so it will trigger call to the 
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
        
        case MOVE_RACK_BOARD:	// a robot move from the rack to the board
            from_col = G.CharToken(msg);		// player index
            from_row = G.IntToken(msg);			// index into the rack
            rotation = G.IntToken(msg);			// rotation and flip
  	        to_col = G.CharToken(msg);			// destination cell col
	        to_row = G.IntToken(msg);  			// destination cell row
	        break;
	        
	        
        case MOVE_DROPB:
	       to_col = G.CharToken(msg);
	       to_row = G.IntToken(msg);
	       break;

        case MOVE_PICKGIVEN:
		case MOVE_PICKB:
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);

            break;

	     case MOVE_DROP:
	           to_col = G.CharToken(msg);
	           to_row = G.IntToken(msg);
	           break;
	     case MOVE_ROTATE_CW:
	     case MOVE_ROTATE_CCW:
	     case MOVE_FLIP:
	     case MOVE_PICK:
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
            break;

        case MOVE_START:
             player = D.getInt(msg.nextToken());

            break;
        case MOVE_LINK:
        	from_col = G.CharToken(msg);
        	from_row = G.IntToken(msg);
        	to_col = G.CharToken(msg);
        	to_row = G.IntToken(msg);
        	break;
        case MOVE_ASSIGN:
        	{
        	IStack vals = new IStack();
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
            while(msg.hasMoreTokens())
            	{
            	vals.push(G.IntToken(msg));
            	}
            assignedValues = vals.toArray();
        	}
        	break;
        default:

            break;
        }
    }
    Text chipGlyph(commonCanvas v,String alt)
    {
    	if(chip==null) { return(TextChunk.create(alt));}
    	Rectangle r = chip.boundingSquare(100,00,00);
    	double w = -G.Right(r)/(G.Width(r)*2.0);
    	double h = -G.Bottom(r)/(G.Height(r)*2.0);
    	double scl = 0.3;
    	int ma = Math.max(G.Width(r),G.Height(r));
    	if(ma>=300) { scl = 0.25; } 
    	if(ma>=500) { scl = 0.2; }
    	//G.print("R "+r.x+ " "+r.y + " "+r.width+" " +r.height+" "+h);
    	return(TextGlyph.create("xxxxx",chip,v,new double[]{2.0,scl,w,h}));
    }
    boolean superCeded()
    {
    	if(next!=null)
    	{
    		switch(next.op)
    		{
    		default: break;
    		case MOVE_FLIP:
    		case MOVE_ROTATE_CW:
    		case MOVE_ROTATE_CCW: return(true);
    		}
    	}
    	return(false);
    }
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas v)
    {
        switch (op)
        {
        case MOVE_PICKB:
        case MOVE_PICKGIVEN:
            return (chipGlyph(v," "+from_col + from_row));

		case MOVE_DROPB:
            return (TextChunk.create(" " + to_col + to_row));

        case MOVE_DROP:
        	return(chipGlyph(v,""+to_col+to_row));
        case MOVE_FLIP:
        	if(superCeded()) { return(TextChunk.create("")); }
            return (chipGlyph(v,"Flip "));

        case MOVE_PICK:
        	if(superCeded()) { return(TextChunk.create("")); }
        	return (chipGlyph(v,"#"+from_row));
        case MOVE_ROTATE_CW:
        	if(superCeded()) { return(TextChunk.create("")); }
        	return (chipGlyph(v,"R-CW "));
        case MOVE_ROTATE_CCW:
        	if(superCeded()) { return(TextChunk.create("")); }
        	return (chipGlyph(v,"R-CCW "));
        case MOVE_LINK:
        case MOVE_RACK_BOARD:
        	return(TextChunk.join(chipGlyph(v,"#"+from_row),TextChunk.create("-"+to_col+to_row)));
        case MOVE_DONE:
            return (TextChunk.create(""));
        case MOVE_ASSIGN:
        	{
            	String msg = "";
            	if(assignedValues!=null)
            	{
            		for(int lim=assignedValues.length,i=0;  i<lim; i++)
            		{
            			msg += " "+assignedValues[i];
            		}
            	}
            	return (TextChunk.join(chipGlyph(v,"#"+from_col),TextChunk.create(""+from_row+msg)));
            	}
       default:
            return (TextChunk.create(D.findUnique(op)));

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

        case MOVE_PICKGIVEN:
        case MOVE_PICKB:
	        return (opname+ from_col + " " + from_row);

		case MOVE_DROPB:
	        return (opname + to_col + " " + to_row);

		case MOVE_LINK:
			return(opname+ from_col+ " "+from_row
					+ " " + to_col + " " + to_row);
		case MOVE_RACK_BOARD:
			return(opname + from_col+ " "+from_row
					+ " " +rotation+ " " + to_col + " " + to_row);
        case MOVE_PICK:
        case MOVE_FLIP:
        case MOVE_ROTATE_CCW:
        case MOVE_ROTATE_CW:
            return (opname+from_col+" "+from_row);

        case MOVE_DROP:
             return (opname+to_col+" "+to_row);

        case MOVE_START:
            return (indx+"Start P" + player);

        case MOVE_ASSIGN:
        	{
        	String msg = "";
        	if(assignedValues!=null)
        	{
        		for(int lim=assignedValues.length,i=0;  i<lim; i++)
        		{
        			msg += " "+assignedValues[i];
        		}
        	}
        	return (opname+from_col+" "+from_row+msg);
        	}
        default:
            return (opname);
        }
    }

}
