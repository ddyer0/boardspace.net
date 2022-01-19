package arimaa;

import online.game.*;
import java.util.*;

import lib.G;
import lib.StockArt;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.ExtendedHashtable;

public class ArimaaMovespec extends commonMove implements ArimaaConstants
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
    			"Move",MOVE_BOARD_BOARD,
    			"OnBoard",MOVE_RACK_BOARD,
    			"Pull",MOVE_PULL,
    			"Push",MOVE_PUSH,
    			"FinishPull", MOVE_FINISH_PULL,
    			"FinishPush", MOVE_FINISH_PUSH,
    			"PlaceRabbits", MOVE_PLACE_RABBITS);
   }
    
    ArimaaId source; // where from/to
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    int undoInfo;	// the state of the move before state, for UNDO
    int pushPullDirection;
    boolean captures = false;
    ArimaaChip picked = null;
    ArimaaChip victim = null;
    
    public ArimaaMovespec()
    {
    } // default constructor

    /* constructor */
    public ArimaaMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    public ArimaaMovespec(int opc,int pla)
    {
    	op = opc;
    	player = pla;
    }
    // construictor for board to board moves
    public ArimaaMovespec(int opc,char fc,int fr,char tc,int tr,int who)
    {
    	op = opc;
    	from_col = fc;
    	from_row = fr;
    	to_col = tc;
    	to_row = tr;
    	player = who;
    }
    // construictor for push/pull
    public ArimaaMovespec(int opc,char fc,int fr,char tc,int tr,int dir,int who)
    {
    	op = opc;
    	from_col = fc;
    	from_row = fr;
    	to_col = tc;
    	to_row = tr;
    	pushPullDirection = dir;
    	player = who;
    }
    // constructor for move rack to board
    public ArimaaMovespec(int opc,ArimaaId src,char fc,int fr,char tc,int tr,int who)
    {	op = opc;
    	source = src;
    	from_col = fc;
    	from_row = fr;
    	to_col = tc;
    	to_row = tr;
    	player = who;
    }
    /* constructor */
    public ArimaaMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    public boolean Same_Move_P(commonMove oth)
    {
    	ArimaaMovespec other = (ArimaaMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (pushPullDirection == other.pushPullDirection)
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(ArimaaMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.pushPullDirection = pushPullDirection;
        to.from_col = from_col;
        to.from_row = from_row;
        to.captures = captures;
        to.undoInfo = undoInfo;
        to.source = source;
        to.picked = picked;
    }

    public commonMove Copy(commonMove to)
    {
    	ArimaaMovespec yto = (to == null) ? new ArimaaMovespec() : (ArimaaMovespec) to;

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

        int opcode = D.getInt(cmd, MOVE_UNKNOWN);
        op = opcode;

        switch (opcode)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Can't parse %s", cmd);
        case MOVE_RACK_BOARD:	// a robot move from the rack to the board
            source = ArimaaId.valueOf(msg.nextToken());	// white rack or black rack
            from_col = '@';						// always
            from_row = G.IntToken(msg);			// index into the rack
 	        to_col = G.CharToken(msg);			// destination cell col
	        to_row = G.IntToken(msg);  			// destination cell row
	        break;
	        
        case MOVE_PUSH:
        case MOVE_PULL:
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        pushPullDirection = G.IntToken(msg);
	        break;
        case MOVE_FINISH_PULL:
        case MOVE_FINISH_PUSH:
        case MOVE_BOARD_BOARD:			// robot move from board to board
            source = ArimaaId.BoardLocation;		
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
 	        to_col = G.CharToken(msg);		//to col row
	        to_row = G.IntToken(msg);
	        break;
	        
        case MOVE_DROPB:
	       source = ArimaaId.BoardLocation;
	       to_col = G.CharToken(msg);
	       to_row = G.IntToken(msg);
	       break;

		case MOVE_PICKB:
            source = ArimaaId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);

            break;

        case MOVE_PICK:
        	source = ArimaaId.valueOf(msg.nextToken());
            from_col = '@';
            from_row = G.IntToken(msg);
            break;
            
        case MOVE_DROP:
            source = ArimaaId.valueOf(msg.nextToken());
            to_col = '@';
            to_row = G.IntToken(msg);
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

        default:
        	break;
        }
    }
    static final StockArt directions[] = 
    	{StockArt.SolidRightArrow,
    	StockArt.SolidUpArrow,
    	StockArt.SolidLeftArrow,
    	StockArt.SolidDownArrow};
    
    public Text shortMoveText(commonCanvas v)
    {	double discScale[] = new double[]{1.5,1.5,0,-0.15};
    	boolean reverse = ((ArimaaViewer)v).reversed();
    	Text ms = (picked==null) 
    		? TextChunk.create("")
    		: TextGlyph.create("xxxx",picked,v,discScale);
        switch (op)
        {
        case MOVE_PICKB:
        	{
        	Text chunk = TextChunk.join(ms,TextChunk.create(""+from_col + (9-from_row)+"-"));
            return (chunk);
        	}
		case MOVE_DROPB:
			{
			Text chunk = TextChunk.create(" "+to_col + (9-to_row));
			if(victim!=null)
			{	Text msg = TextGlyph.create("xxx",
					directions[pushPullDirection^(reverse?2:0)],
					v,
					new double[]{1,0.8,0,-0.3});
				chunk.append(msg);
				//if(picked!=null) { chunk.append(TextChunk.create(" Pushed by ")); }
			}
            return (chunk);
			}
		case MOVE_PLACE_RABBITS:
			return(TextChunk.create("Place Rabbits"));
        case MOVE_DROP:
        	return (TextChunk.create(source.name()+(9-to_row)));
        case MOVE_PICK:
        	return(ms);
        case MOVE_RACK_BOARD:
        	return(TextChunk.join(ms,TextChunk.create(""+to_col + (9-to_row))));
        case MOVE_BOARD_BOARD:
        case MOVE_FINISH_PULL:
        case MOVE_FINISH_PUSH:
        	return(TextChunk.join(ms,TextChunk.create(""+from_col + (9-from_row)+"-"+to_col  + (9 - to_row))));
        case MOVE_PUSH:
           	return(TextChunk.join(
           			ms,
           			TextChunk.create(""+from_col+(9-from_row)+"-"+to_col+(9-to_row)),
           			TextGlyph.create("xxxx",victim,v,discScale),
           			TextGlyph.create("xxx",directions[pushPullDirection^(reverse?2:0)],v,new double[]{1,0.8,0,-0.3})));    
        case MOVE_PULL:
        	return(TextChunk.join(
           			ms,
        			TextChunk.create(""+from_col+(9-from_row)+"-"+to_col+(9-to_row)),
           			TextGlyph.create("xxxx",victim,v,discScale),
           			TextGlyph.create("xxx",directions[pushPullDirection^(reverse?2:0)],v,new double[]{1,0.8,0,-0.3})));
        case MOVE_DONE:
        	return (TextChunk.create(""));

        default:
        	return (TextChunk.create(D.findUnique(op)));

        }
    }

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String moveString()
    {
		String ind = (index() >= 0) ? (index() + " ") : "";
		String opname = D.findUnique(op)+" ";
        // adding the move index as a prefix provides numnbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        case MOVE_PICKB:
	        return G.concat(ind,opname , from_col , " ", from_row);

		case MOVE_DROPB:
	        return G.concat(ind+opname  , to_col , " " , to_row);

		case MOVE_RACK_BOARD:
			return G.concat(ind,opname  ,source.name(), " ",from_row
					, " " , to_col , " " , to_row);
		case MOVE_PUSH:
		case MOVE_PULL:
			return G.concat(ind,opname, from_col , " " , from_row
					, " " , to_col , " " , to_row, " ",pushPullDirection);
		case MOVE_BOARD_BOARD:
		case MOVE_FINISH_PULL:
		case MOVE_FINISH_PUSH:
			return G.concat(ind,opname , from_col , " " , from_row
					, " " , to_col , " " , to_row);
        case MOVE_PICK:
            return G.concat(ind,opname ,source.name(), " ",from_row);

        case MOVE_DROP:
             return G.concat(ind,opname,source.name(), " ",to_row);

        case MOVE_START:
            return G.concat(ind,"Start P" , player);

        default:
        	return G.concat(ind,opname);
        }
    }

}
