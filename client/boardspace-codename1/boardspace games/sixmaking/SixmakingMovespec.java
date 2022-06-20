package sixmaking;

import online.game.*;
import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.ExtendedHashtable;


public class SixmakingMovespec extends commonMove implements SixmakingConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; 	// pick a chip from a pool
    static final int MOVE_DROP = 205; 	// drop a chip
    static final int MOVE_PICKB = 206; 	// pick from the board
    static final int MOVE_DROPB = 207; 	// drop on the board
    static final int MOVE_BOARD_BOARD = 210;	// move board to board
    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
   			"Move",MOVE_BOARD_BOARD);
   }

    SixmakingId source; // where from/to
    SixmakingId dest; 	// were to
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    int height;	// height of the pick
    int from_full_height = 0;
    SixmakingChip from_chip=null;
    int to_full_height = 0;
    
    public SixmakingMovespec() // default constructor
    {
    }
    public SixmakingMovespec(int opc, int pl)	// constructor for simple moves
    {
    	player = pl;
    	op = opc;
    }

    /* constructor for robot moves */
    public SixmakingMovespec(SixmakingCell from,SixmakingCell to, int h,int who)
    {	op = MOVE_BOARD_BOARD;
    	G.Assert(h>0,"bad height");
    	source = from.rackLocation();
    	dest = to.rackLocation();
    	from_col = from.col;
    	from_row = from.row;
    	to_col = to.col;
    	to_row = to.row;
    	height = h;
    	player = who;
    }
    /* constructor */
    public SixmakingMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public SixmakingMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    /* constructor for robot moves */
    public SixmakingMovespec(int opc,SixmakingCell from,SixmakingCell to,int who)
    {
    	player = who;
    	op = opc;
    	height = 1;
    	from_col = from.col;
    	from_row = from.row;
    	to_col = to.col;
    	to_row = to.row;
    }
    /* constructor for robot moves */
    public SixmakingMovespec(int opc,SixmakingCell from,SixmakingCell to,int h,int who)
    {   G.Assert(h>0,"bad height");
    	player = who;
    	op = opc;
    	from_col = from.col;
    	from_row = from.row;
    	height = h;
    	to_col = to.col;
    	to_row = to.row;
    }  
    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
    	SixmakingMovespec other = (SixmakingMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (dest == other.dest)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(SixmakingMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.source = source;
        to.dest = dest;
        to.height = height;
        to.from_full_height = from_full_height;
        to.to_full_height = to_full_height;
        to.from_chip = from_chip;
     }

    public commonMove Copy(commonMove to)
    {
    	SixmakingMovespec yto = (to == null) ? new SixmakingMovespec() : (SixmakingMovespec) to;

        // we need yto to be a SixmakingMovespec at compile time so it will trigger call to the 
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

        case MOVE_BOARD_BOARD:			// robot move from board to board
        	source = SixmakingId.get(msg.nextToken());
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
        	height = G.IntToken(msg);
        	dest = SixmakingId.get(msg.nextToken());
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	       	G.Assert(height>0,"bad height");
	        break;

		case MOVE_DROPB:
            dest = SixmakingId.BoardLocation;
            to_col = from_col = G.CharToken(msg);
            to_row = from_row = G.IntToken(msg);
            break;
            
		case MOVE_PICKB:
            source = SixmakingId.BoardLocation;
            to_col = from_col = G.CharToken(msg);
            to_row = from_row = G.IntToken(msg);
            height = G.IntToken(msg);
           	G.Assert(height>0,"bad height");

            break;

        case MOVE_PICK:
            source = SixmakingId.get(msg.nextToken());
            from_col = '@';
            height = G.IntToken(msg);
            break;
            
        case MOVE_DROP:
           source =SixmakingId.get(msg.nextToken());
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

        default:
            break;
        }
    }

    private Text moveIcon(commonCanvas v,SixmakingChip chip,int height,String text,boolean before)
    {	Text str = TextChunk.create(text+(before?"":"    "));
    	if((height>0)&&(player>=0))
    	{	int h = Math.min(5,height-1);
    		SixmakingChip top = SixmakingChip.caps[chip.playerIndex()][h];
    		TextChunk chess = TextGlyph.create("   ",top,v,new double[]{1.5,5.0,-1.2,-0.75});
    		TextChunk disc = TextGlyph.create("   ",chip,v,new double[]{1.50,4.0,-0.2,-0.5});
    		Text glyph = TextChunk.join(disc,chess);
    		str = before ? TextChunk.join(glyph,str) : TextChunk.join(str,glyph);
    	}
    	return(str);
    }
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas v)
    {
        switch (op)
        {
        case MOVE_PICKB:
            return (moveIcon(v,from_chip,from_full_height,""+from_col + from_row,true));
		case MOVE_DROPB:
            return (moveIcon(v,from_chip,to_full_height,"-"+to_col + to_row,false));

        case MOVE_DROP:
        case MOVE_PICK:
            return (TextChunk.create(""));
        case MOVE_BOARD_BOARD:
        	if(source!=SixmakingId.BoardLocation) { return(moveIcon(v,from_chip,to_full_height,"-"+to_col+to_row,false)); }
        	return(TextChunk.join(
        			moveIcon(v,from_chip,from_full_height,""+from_col + from_row,true),
        			moveIcon(v,from_chip,to_full_height," ("+height+") "+to_col + to_row,false)));
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
        // adding the move index as a prefix provides numbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        case MOVE_PICKB:
	        return (opname + from_col + " " + from_row+" "+height);

		case MOVE_DROPB:
	        return (opname + to_col + " " + to_row);

		case MOVE_BOARD_BOARD:
			return(opname+source.shortName+" " + from_col + " "	+ from_row
					+ " "+height+" "+dest.shortName+ " " + to_col + " " + to_row);
        case MOVE_PICK:
            return (opname+source.shortName+ " "+height);

        case MOVE_DROP:
             return (opname+source.shortName+ " "+to_row);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
  
            return (opname);
        }
    }

    /* standard java method, so we can read moves easily while debugging */
    //public String toString()
    //{
    //    return ("P" + player + "[" + moveString() + "]");
    //}
}
