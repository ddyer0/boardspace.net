package tamsk;

import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import online.game.*;
import tamsk.TamskConstants.TamskId;
import lib.ExtendedHashtable;
public class Tamskmovespec 
		extends commonMove	// for a multiplayer game, this will be commonMPMove
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_PICKRING = 208;	//pick a ring from a player
    static final int MOVE_DROPRING = 209;
    static final int MOVE_DROPRINGB = 210;
    static final int MOVE_PICKRINGB = 211;
    static final int MOVE_FROM_TO = 212;
    static final int MOVE_FIFTEEN = 213;
    static final int MOVE_STOPTIME = 214;
    static final int MOVE_STARTTIME = 215;
    static final int MOVE_TIMEEXPIRED = 216;
    static final int MOVE_GAMEOVER = 217;
    static final int MOVE_DELAY = 218;
    
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
        	"PickRing",MOVE_PICKRING,
        	"DropRing",MOVE_DROPRING,
        	"PickRingB",MOVE_PICKRINGB,
        	"DropRingB",MOVE_DROPRINGB,
        	"Move",MOVE_FROM_TO,
        	"Fifteen",MOVE_FIFTEEN,
        	"Stop",MOVE_STOPTIME,
        	"Restart",MOVE_STARTTIME,
        	"TimeExpired",MOVE_TIMEEXPIRED,
        	"GameOver",MOVE_GAMEOVER,
        	"Delay",MOVE_DELAY
    			);
  }
    //
    // adding these makes the move specs use Same_Move_P instead of == in hash tables
    //needed when doing chi square testing of random move generation, but possibly
    //hazardous to keep in generally.
    //public int hashCode()
    //{
    //	return(to_row<<12+to_col<<18+player<<24+op<<25);
    //}
    //public boolean equals(Object a)
    //{
    //	return( (a instanceof commonMove) && Same_Move_P((commonMove)a)); 
    //}
    //
    // variables to identify the move
    TamskId source; // where from/to
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    TamskChip chip;
    long flipTime = -1;
    char from_col;
    int from_row;
    long gameTime = -1;
    boolean rejected = false;	// any move might be rejected due to time violations
    
    // these provide an interface to log annotations that will be seen in the game log
    String gameEvents[] = null;
    public String[] gameEvents() { return(gameEvents); }

    public Tamskmovespec()
    {
    } // default constructor

    /* constructor */
    public Tamskmovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    public Tamskmovespec(int opc , int p)
    {
    	op = opc;
    	player = p;
    }
    /** constructor for robot moves.  Having this "binary" constructor is dramatically faster
     * than the standard constructor which parses strings
     */
    public Tamskmovespec(int opc,char col,int row,int who)
    {
    	op = opc;
     	to_col = col;
    	to_row = row;
    	from_col = col;
    	from_row = row;
    	player = who;
    }
    /* constructor */
    public Tamskmovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    public Tamskmovespec(int moveFromTo, TamskCell c, TamskCell d2, int who) {
		op = moveFromTo;
		from_col = c.col;
		from_row = c.row;
		to_col = d2.col;
		to_row = d2.row;
		player = who;
		source = TamskId.BoardLocation;
	}

	/**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        Tamskmovespec other = (Tamskmovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (player == other.player));
    }

    public void Copy_Slots(Tamskmovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.source = source;
        to.chip = chip;
        to.flipTime = flipTime;
        to.from_col = from_col;
        to.from_row = from_row;
        to.rejected = rejected;
        to.gameTime = gameTime;
    }

    public commonMove Copy(commonMove to)
    {
        Tamskmovespec yto = (to == null) ? new Tamskmovespec() : (Tamskmovespec) to;

        // we need yto to be a Pushfightmovespec at compile time so it will trigger call to the 
        // local version of Copy_Slots
        Copy_Slots(yto);

        return (yto);
    }

    /* parse a string into the state of this move.  Remember that we're just parsing, we can't
     * refer to the state of the board or the game.  This parser follows the recommended practice
     * of keeping it very simple.  A move spec is just a sequence of tokens parsed by calling
     * nextToken
     * @param msg a string tokenizer containing the move spec
     * @param the player index for whom the move will be.
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
        	throw G.Error("Cant parse " + cmd);
        case MOVE_DELAY:
        	from_row = to_row = G.IntToken(msg);
        	break;
        case MOVE_FROM_TO:
            source = TamskId.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);    
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);
        	break;
        case MOVE_PICKRINGB:
        case MOVE_DROPRINGB:
            source = TamskId.BoardRing;
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);
            break;
       	
        case MOVE_DROPB:
		case MOVE_PICKB:
            source = TamskId.BoardLocation;
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);

            break;

		case MOVE_PICKRING:
		case MOVE_DROPRING:
        case MOVE_DROP:
        case MOVE_PICK:
            source = TamskId.valueOf(msg.nextToken());
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

        default:

            break;
        }
        if(msg.hasMoreTokens()) { gameTime = G.LongToken(msg);}
    }

    private Text icon(commonCanvas v,Object... msg)
    {	double chipScale[] = {1.3,0.8	,-0.2,-0.2};
    	Text m = TextChunk.create(G.concat(msg));
    	if(chip==TamskChip.Ring)
    	{
    		m = TextChunk.join(TextGlyph.create("xxx", chip, v,chipScale),
    					m);
    	}
    	else if (chip!=null)
    	{	String tstr = "";
    		if(flipTime>=0)
    			{long seconds = flipTime/1000;
    			long minutes = seconds/60;
    			tstr = G.format("%d:%02d",minutes,seconds%60);
    			}
    		m = TextChunk.join( TextGlyph.create("xxx","xxx",tstr,chip,v,chipScale),
    				m);
    	}
    	return(m);
    }

    /** construct an abbreviated move string, mainly for use in the game log.  These
     * don't have to be parseable, they're intended only to help humans understand
     * the game record.  The alternative method {@link #shortMoveText} can be implemented
     * to provide colored text or mixed text and icons.
     * 
     * */
    public Text shortMoveText(commonCanvas v)
    {
        switch (op)
        {
        case MOVE_PICKRINGB:
        case MOVE_PICKB:
            return icon(v,to_col , to_row);
            
        case MOVE_DROPRINGB:
		case MOVE_DROPB:
            return icon(v,"-",to_col ,to_row);

		case MOVE_FROM_TO:
			return icon(v,from_col,from_row,"-",to_col,to_row);
			
		case MOVE_PICKRING:
		case MOVE_DROPRING:
        case MOVE_DROP:
        case MOVE_PICK:
            return icon(v,source.shortName());

        case MOVE_DONE:
            return TextChunk.create("");
            
        case MOVE_DELAY:
        	return TextChunk.create("Delay "+to_row);
        case MOVE_FIFTEEN:
        	if(chip!=null) { return icon(v); } 
			//$FALL-THROUGH$
		case MOVE_STARTTIME:
        case MOVE_STOPTIME:
        	setLineBreak(true);     		
			//$FALL-THROUGH$
		default:
            return TextChunk.create(D.findUniqueTrans(op));

        }
    }

    /** construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and only secondarily human readable */
    public String moveString()
    {
		String indx = indexString();
		String opname = indx+D.findUnique(op)+" ";
        // adding the move index as a prefix provides numnbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        case MOVE_PICKRINGB:
        case MOVE_DROPRINGB:
        case MOVE_PICKB:
		case MOVE_DROPB:
	        return G.concat(opname , to_col , " " , to_row," ",gameTime);

		case MOVE_FROM_TO:
			return G.concat(opname,from_col," ",from_row," ",to_col," ",to_row," ",gameTime);
			
		case MOVE_PICKRING:
		case MOVE_DROPRING:
        case MOVE_DROP:
        case MOVE_PICK:
            return G.concat(opname , source.shortName()," ",gameTime);

        case MOVE_START:
            return G.concat(indx,"Start P" , player," ",gameTime);

        case MOVE_DELAY:
        	return G.concat(opname,to_row," ",gameTime);
        	
        default:
            return G.concat(opname," ",gameTime);
            
        }
    }
    /**
     *  longMoveString is used for sgf format records and can contain other information
     * intended to be ignored in the normal course of play, for example human-readable
     * information
     */
    /*
    public String longMoveString()
    {	String str = moveString();
    	return(str);
    }
    */
    /** standard java method, so we can read moves easily while debugging */
    /*
    public String toString()
    {	return super.toString();
        //return ("P" + player + "[" + moveString() + "]");
    }
    */
    /*
    public void setGameover(boolean v)
    {	//if(visit())
    	//	{
    	//	G.Error("makeover");
    	//	}
    	super.setGameover(v);
    }
    */
   /* 
    public boolean visit()
    {	
    	//if( (from_col=='F' && from_row==10 && to_col=='E' && to_row==9))
    	//{
    	//	UCTNode uct = uctNode();
    	//	if(uct!=null) { UCTNode.marked = uct; }
    	//	return(true);
    	//}
    	return(false);
    	//		||(from_col=='C' && from_row==5 && to_col=='A' && to_row==7);
    	//return(false);
    }
    */
}
