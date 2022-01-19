package goban;

import online.game.*;
import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.ExtendedHashtable;

public class GoMovespec extends commonMove implements GoConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_RACK_BOARD = 209;	// move from rack to board
	static final int MOVE_DROP_BLACK = 211;
	static final int MOVE_DROP_WHITE = 212;
	static final int MOVE_ADD_BLACK = 213;
	static final int MOVE_ADD_WHITE = 214;
	static final int MOVE_DEAD = 215;
	static final int MOVE_UNDEAD = 216;
	static final int MOVE_NEXTPLAYER = 217;
	static final int MOVE_RESUMEPLAY = 218;
	static final int MOVE_UNDOREQUEST = 219;
	static final int MOVE_OKUNDO = 220;
	static final int MOVE_DENY_UNDO = 221;
	static final int MOVE_ANNOTATE = 222;
	static final int MOVE_HANDICAP = 223;
	static final int MOVE_RESUMESCORING = 224;
	static final int MOVE_DONESCORING = 225;
	static final int MOVE_KOMI = 226;
	static final int MOVE_SAFE = 227;
	static final int PlainMove[] = { MOVE_DROP_BLACK, MOVE_DROP_WHITE };
    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D);
        D.putInt("Pick", MOVE_PICK);
        D.putInt("Pickb", MOVE_PICKB);
        D.putInt("Drop", MOVE_DROP);
        D.putInt("Dropb", MOVE_DROPB);
		D.putInt("OnBoard",MOVE_RACK_BOARD);
		D.putInt("B",MOVE_DROP_BLACK);
		D.putInt("W",MOVE_DROP_WHITE);
		D.putInt("AB",MOVE_ADD_BLACK);
		D.putInt("AW",MOVE_ADD_WHITE);
		D.putInt("KM",MOVE_KOMI);
		D.putInt("Dead",MOVE_DEAD);
		D.putInt("Undead",MOVE_UNDEAD);
		D.putInt("NextPlayer",MOVE_NEXTPLAYER);
		D.putInt("Resume",MOVE_RESUMEPLAY);
		D.putInt("ResumeScoring",MOVE_RESUMESCORING);
		D.putInt("DoneScoring",MOVE_DONESCORING);
		D.putInt("OkUndo", MOVE_OKUNDO);
		D.putInt("DenyUndo",MOVE_DENY_UNDO);
		D.putInt("UndoRequest",MOVE_UNDOREQUEST);
		D.putInt("Annotate",MOVE_ANNOTATE);
		D.putInt("Handicap",MOVE_HANDICAP);
		D.putInt("Safe",MOVE_SAFE);
   }

    GoId source; // where from/to
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    CellStack viewerInfo = null;
    public GoMovespec() // default constructor
    {
    }
    public GoMovespec(int opc, int pl)	// constructor for simple moves
    {
    	player = pl;
    	op = opc;
    }
    public GoMovespec(int opc,char col,int row,int pl)
    {
    	op = opc;
    	to_col = col;
    	to_row = row;
    	player = pl;
    }
    /* constructor */
    public GoMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public GoMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    
    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
    	GoMovespec other = (GoMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(GoMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.source = source;
        to.viewerInfo = viewerInfo;
    }

    public commonMove Copy(commonMove to)
    {
    	GoMovespec yto = (to == null) ? new GoMovespec() : (GoMovespec) to;

        // we need yto to be a GoMovespec at compile time so it will trigger call to the 
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
        case MOVE_HANDICAP:
        	to_row = G.IntToken(msg);
        	break;
        case MOVE_RACK_BOARD:	// a robot move from the rack to the board
        	source = GoId.get(msg.nextToken());	// white rack or black rack
            from_col = '@';						// always
            from_row = G.IntToken(msg);			// index into the rack
 	        to_col = G.CharToken(msg);			// destination cell col
	        to_row = G.IntToken(msg);  			// destination cell row
	        break;
        case MOVE_ADD_BLACK:
        case MOVE_DROP_BLACK:
        	{
        	player = 0;
        	String place = msg.hasMoreTokens() ? msg.nextToken().toUpperCase() : "TT";
        	to_col = place.charAt(0);
        	to_row = -(place.charAt(1)-'A'+1);
        	}
        	break;
        case MOVE_ADD_WHITE:
        case MOVE_DROP_WHITE:
	    	{
	    	player = 1;
        	String place = msg.hasMoreTokens() ? msg.nextToken().toUpperCase() : "TT";
	    	to_col = place.charAt(0);
	    	to_row = -(place.charAt(1)-'A'+1);
	    	}
	    	break;
      	case MOVE_ANNOTATE:
        case MOVE_DROPB:
        case MOVE_DEAD:
        case MOVE_SAFE:
        case MOVE_UNDEAD:
		case MOVE_PICKB:
            source = GoId.BoardLocation;
            to_col = from_col = G.CharToken(msg);
            to_row = from_row = G.IntToken(msg);

            break;

        case MOVE_PICK:
            source = GoId.get(msg.nextToken());
            from_col = '@';
            switch(source)
            {
            case AnnotationViewButton:
            	{
            	String tok = msg.nextToken();
            	from_row = Annotation.find(tok).ordinal();
            	}
            	break;
           	default:
           		from_row = G.IntToken(msg);
           		break;
            }
            
            break;
        case MOVE_KOMI:
        	double km = G.DoubleToken(msg);
        	to_row = (int)(km*2);
        	break;
        case MOVE_DROP:
            source =GoId.get(msg.nextToken());
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

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String shortMoveString(commonCanvas c)
    {	GoBoard b = (GoBoard)((GoViewer)c).getBoard();
        switch (op)
        {
        case MOVE_HANDICAP:
        	return("Handicap "+to_row);
        case MOVE_UNDEAD:
        	return("Undead "+from_col+" "+from_row);
        	
        case MOVE_DEAD:
        	return("Dead "+from_col+" "+from_row);
        case MOVE_SAFE:
        	return("Safe "+from_col+" "+from_row);
        case MOVE_PICKB:
            return ("@"+from_col + " "+from_row);

		case MOVE_DROPB:
            return (to_col + " " + to_row);
            
		case MOVE_ANNOTATE:
        case MOVE_DROP:
        case MOVE_PICK:
           	switch(source)
        	{
        	case AnnotationViewButton:
        		return (Annotation.values()[from_row].name());
        	default:
        		return (source.shortName);
        	}
            
        case MOVE_ADD_BLACK:
        case MOVE_DROP_BLACK:
        	return(""+to_col+" "+(b.boardRows+to_row+1));
        case MOVE_DROP_WHITE:
        case MOVE_ADD_WHITE:
        	return(""+to_col+" "+(b.boardRows+to_row+1));
        case MOVE_RACK_BOARD:
        	return(source.shortName+"@ "+to_col + " " + to_row);
        case MOVE_DONE:
        case MOVE_NEXTPLAYER:
            return ("");
        case MOVE_DONESCORING:
        	return("Done Scoring");
        case MOVE_RESUMESCORING:
        	return("Resume Scoring");
        case MOVE_KOMI:
        	return("KM "+(to_row/2.0));
        default:
            return (D.findUnique(op));
        }
    }
    public Text shortMoveText(commonCanvas c)
    {	String str = shortMoveString(c);
    	return(TextChunk.create(str));
    }
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String moveString()
    {
        String ind = "";

        if (index() >= 0)
        {
            ind += (index() + " ");
        }
        // adding the move index as a prefix provides numbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        case MOVE_HANDICAP:
        	return(ind+"Handicap "+to_row);
        case MOVE_UNDEAD:
        	return(ind+"Undead "+from_col+" "+from_row);
        	
        case MOVE_DEAD:
        	return(ind+"Dead "+from_col+" "+from_row);

        case MOVE_SAFE:
        	return(ind+"Safe "+from_col+" "+from_row);

        case MOVE_PICKB:
	        return (ind+D.findUnique(op) + " " + from_col + " " + from_row);
	        
        case MOVE_ANNOTATE:
		case MOVE_DROPB:
	        return (ind+D.findUnique(op) + " " + to_col + " " + to_row);

		case MOVE_RACK_BOARD:
			return(ind+D.findUnique(op) + " " +source.shortName+ " "+from_row
					+ " " + to_col + " " + to_row);
        case MOVE_PICK:
        	switch(source)
        	{
        	case AnnotationViewButton:
        		return (ind+D.findUnique(op) + " "+source.shortName+ " "+Annotation.values()[from_row].name());
        	default:
        		return (ind+D.findUnique(op) + " "+source.shortName+ " "+from_row);
        	}

        case MOVE_DROP:
             return (ind+D.findUnique(op) + " "+source.shortName+ " "+to_row);

        case MOVE_START:
            return (ind+"Start P" + player);

        case MOVE_DROP_BLACK:
        	return(ind+"B "+to_col+(char)('A'-to_row-1));
        	
        case MOVE_DROP_WHITE:
        	return(ind+"W "+to_col+(char)('A'-to_row-1));
 
        case MOVE_ADD_BLACK:
        	return(ind+"AB "+to_col+(char)('A'-to_row-1));
        	
        case MOVE_ADD_WHITE:
        	return(ind+"AW "+to_col+(char)('A'-to_row-1));
 
        case MOVE_KOMI:
        	return(ind+"KM "+(to_row/2.0));
        default:
            return (ind+D.findUnique(op));
        }
    }

    /* standard java method, so we can read moves easily while debugging */
    //public String toString()
    //{
    //    return ("P" + player + "[" + moveString() + "]");
    //}
}
