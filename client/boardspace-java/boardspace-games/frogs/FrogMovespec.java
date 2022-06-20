package frogs;

import java.util.*;
import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import online.game.*;
import lib.ExtendedHashtable;
public class FrogMovespec extends commonMPMove implements FrogConstants
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
        	"Move",MOVE_MOVE,
        	"Onboard",MOVE_ONBOARD);
   }

    FrogId source; // where from/to
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    char from_col;
    int from_row;
    FrogPiece object;
    
    public FrogMovespec()
    {
    } // default constructor

    /* constructor */
    public FrogMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public FrogMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    
    // constructor for MOVE_ONBOARD
    public FrogMovespec(int who,int opcode,
    		FrogId froms,int fromr,
    		char destcol,int destrow)
    {	player = who;
    	op = opcode;
    	to_col = destcol;
    	to_row = destrow;
    	from_col = '@';
    	from_row = fromr;
        source = froms;
    }   
    // constructor for MOVE_MOVE
    public FrogMovespec(int who,int opcode,
    		char fromc,int fromr,
    		char destcol,int destrow)
    {	player = who;
    	op = opcode;
    	to_col = destcol;
    	to_row = destrow;
    	from_col = fromc;
    	from_row = fromr;
        source = FrogId.BoardLocation;
    }
    public FrogMovespec(int who,int opcode)
    {	player = who;
    	op = opcode;
    }
    public boolean Same_Move_P(commonMove oth)
    {
        FrogMovespec other = (FrogMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (from_col==other.from_col)
				&& (from_row==other.from_row)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (player == other.player));
    }

    public void Copy_Slots(FrogMovespec to)
    {	super.Copy_Slots(to);
		to.from_row = from_row;
		to.from_col = from_col;
        to.to_col = to_col;
        to.to_row = to_row;
        to.object = object;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
        FrogMovespec yto = (to == null) ? new FrogMovespec() : (FrogMovespec) to;

        // we need yto to be a FrogMovespec at compile time so it will trigger call to the 
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

        case MOVE_ONBOARD:
        	{
        	source = Frog_Hands[G.IntToken(msg)];
        	from_col = '@';
        	from_row = G.IntToken(msg);
        	to_col = G.CharToken(msg);
        	to_row = G.IntToken(msg);
        	}
        	break;
        case MOVE_MOVE:
        	{
        	source = FrogId.BoardLocation;
        	from_col = G.CharToken(msg);
        	from_row = G.IntToken(msg);
        	to_col = G.CharToken(msg);
        	to_row = G.IntToken(msg);
        	}
        	break;
        case MOVE_DROPB:
	            source = FrogId.BoardLocation;
	            to_col = G.CharToken(msg);
	            to_row = G.IntToken(msg);
	            break;

		case MOVE_PICKB:
            source = FrogId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
 
            break;

        case MOVE_DROP:
            source = Frog_Hands[G.IntToken(msg.nextToken())];
            to_col = '@';
            to_row = G.IntToken(msg);
            break;
            
        case MOVE_PICK:
            source = Frog_Hands[G.IntToken(msg.nextToken())];
            from_col = '@';
            from_row = G.IntToken(msg);
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
    {
        switch (op)
        {
        case MOVE_PICKB:
        	{Text txt = TextChunk.create(""+from_col+from_row);
        	if(object!=null) { txt  = TextChunk.join(
        			TextGlyph.create("xxx", object, v, new double[] {1,1.5,0,-0.25}),
        			txt);}
            return txt;
        	}     	
		case MOVE_DROPB:
            return TextChunk.create("-"+to_col+to_row);

        case MOVE_DROP:
            return TextChunk.create(D.findUnique(op) + " "+source.handNum()+" "+to_row);
            
        case MOVE_PICK:
        	if(object!=null) 
        		{return(TextGlyph.create("xxx",object,v,new double[] {1,1.5,0,-0.25}));
        		}
            return TextChunk.create(""+from_row);
        case MOVE_MOVE:
        	{
        	Text ch =  TextChunk.create(""+from_col+from_row+"-"+to_col+to_row);
    		ch = TextChunk.join(
    				TextGlyph.create("xxx", object, v, new double[] {1,1.5,0,-0.25}),
    				ch);
        	
        	return(ch);
        	}
        case MOVE_ONBOARD:
        	{
        	Text ch = TextChunk.create(""+from_row+"-"+to_col+to_row);
        	if(object!=null)
        	{
        		ch = TextChunk.join(
        				TextGlyph.create("xxx", object, v, new double[] {1,1.5,0,-0.25}),
        				ch);
        	}
        	return(ch);
        	}
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
        // adding the move index as a prefix provides numbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        case MOVE_PICKB:
	        return (opname+ from_col + " " + from_row);


        case MOVE_DROPB:
	        return (opname+ to_col + " " + to_row);

        case MOVE_DROP:
            return (opname+source.handNum()+" "+to_row);

        case MOVE_PICK:
            return (opname+source.handNum()+" "+from_row);

        case MOVE_START:
            return (indx+"Start P" + player);
        case MOVE_ONBOARD:
        	return(opname+source.handNum()+" "+from_row+" "+to_col+" "+to_row);
        case MOVE_MOVE:
        	return(opname+from_col+" "+from_row+" "+to_col+" " +to_row);
        default:
            return (opname);
        }
    }

}
