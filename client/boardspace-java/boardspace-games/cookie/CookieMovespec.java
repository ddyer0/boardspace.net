package cookie;

import online.game.*;

import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.ExtendedHashtable;

public class CookieMovespec extends commonMove implements CookieConstants
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
        	"Slide", CRAWL_FROM_TO,
        	"Move",MOVE_FROM_TO,
        	"Add",MOVE_RACK_BOARD,

        	"Orange",CHIP_OFFSET + CookieChip.ORANGE_CHIP_INDEX,
        	"Blue",CHIP_OFFSET + CookieChip.BLUE_CHIP_INDEX,
        	"Sugar",CHIP_OFFSET + CookieChip.SUGAR_CHIP_INDEX,
        	"Ginger",CHIP_OFFSET + CookieChip.GINGER_CHIP_INDEX,
        	"Chocolate",CHIP_OFFSET + CookieChip.CHOCOLATE_CHIP_INDEX,
        	"Crawl",CHIP_OFFSET + CookieChip.CRAWL_INDEX);
    }

    CookieId source; // where from/to
    char from_col;
    int from_row;
    CookieId dest;
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    CookieChip target;
    public CookieMovespec()
    {
    } 
    // constructor for robot onboard moves
    public CookieMovespec(int opc,int row,char cc,int rr,int pl)
    {	op = opc;
    	source = CookieId.ChipPool;
    	from_row = row;
    	dest = CookieId.BoardLocation;
    	to_col = cc;
    	to_row = rr;
    	player = pl;
    }
    // constructor for robot board moves
    public CookieMovespec(int opc,char fc,int fr,char cc,int rr,int pl)
    {	op = opc;
    	source = CookieId.BoardLocation;
    	from_col = fc;
    	from_row = fr;
    	dest = CookieId.BoardLocation;
    	to_col = cc;
    	to_row = rr;
    	player = pl;
    }
    /* constructor */
    public CookieMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public CookieMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    public CookieMovespec(int opcode,int pl) { player=pl; op=opcode; }
    public boolean Same_Move_P(commonMove oth)
    {
        CookieMovespec other = (CookieMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (dest == other.dest)
				&& (from_col == other.from_col)
				&& (from_row == other.from_row)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (player == other.player));
    }

    public void Copy_Slots(CookieMovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.source = source;
        to.from_col = from_col;
        to.from_row = from_row;
        to.dest = dest;
        to.target = target;
    }

    public commonMove Copy(commonMove to)
    {
        CookieMovespec yto = (to == null) ? new CookieMovespec() : (CookieMovespec) to;

        // we need yto to be a Hexmovespec at compile time so it will trigger call to the 
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

        case MOVE_DROPB:
	            dest = CookieId.BoardLocation;
				to_col = G.CharToken(msg);
	            to_row = G.IntToken(msg);

	            break;

        case MOVE_FROM_TO:
        case CRAWL_FROM_TO:
        	source = CookieId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);
        	dest = CookieId.BoardLocation;
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);
            break;
            
        case MOVE_RACK_BOARD:
        	source = CookieId.ChipPool;
            from_row = D.getInt(msg.nextToken());
        	dest = CookieId.BoardLocation;
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);
            break;
          
		case MOVE_PICKB:
            source = CookieId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);

            break;

        case MOVE_DROP:
        	dest = CookieId.ChipPool;
        	to_col = '@';
        	to_row = D.getInt(msg.nextToken());
        	break;
        case MOVE_PICK:
            source = CookieId.ChipPool;
            from_col = '@';
            from_row = D.getInt(msg.nextToken());
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());
            break;

        default:
            break;
        }
    }
    public Text shortMoveText(commonCanvas v)
    {	Text base = TextChunk.create(shortMoveString());
    	if(target!=null)
    	{
    		Text pre = TextGlyph.create("xxx",target,v,new double[] {1.5,1,0,-0.2});
    		base = TextChunk.join(pre,base);
    	}
    	return(base);
    }
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String shortMoveString()
    {
        switch (op)
        {
        case MOVE_PICKB:
            return (""+ from_col + from_row);

		case MOVE_DROPB:
            return (" "+to_col + to_row);
        case MOVE_PICK:
            return (""+D.findUnique(from_row));

        case MOVE_DONE:
            return ("");

        default:
            return (D.findUnique(op));
            
        case MOVE_FROM_TO:
        case CRAWL_FROM_TO:
        	return(""+from_col+from_row+" "+to_col+to_row);
        	
        case MOVE_RACK_BOARD:
        	return(""+D.findUnique(from_row)+" "+to_col+to_row);
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
	        return (opname + to_col + " " + to_row);

         case MOVE_PICK:
            return (opname + D.findUnique(from_row));

        case MOVE_START:
            return (indx+"Start P" + player);

        case MOVE_DROP:
        	return (opname + D.findUnique(to_row));
        case MOVE_RACK_BOARD:
        	return (opname + D.findUnique(from_row)+" "+to_col+" "+to_row);
        case MOVE_FROM_TO:
        case CRAWL_FROM_TO:
        	return (opname + from_col+" "+from_row+" "+to_col+" "+to_row);
 
        default:
            return (opname);
        }
    }
  
 }