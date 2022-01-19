package qyshinsu;

import java.util.*;
import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.ExtendedHashtable;
import online.game.*;


public class QyshinsuMovespec extends commonMove implements QyshinsuConstants
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
		D.putInt("Remove",MOVE_REMOVE);
		D.putInt("Add",MOVE_RACK_BOARD);
   }

    QIds source; // where from/to
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    QyshinsuChip object;
    
    public QyshinsuMovespec()
    {
    } // default constructor

    /* constructor */
    public QyshinsuMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public QyshinsuMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    /* constructor */
    public QyshinsuMovespec(int opc,int pla,QIds src,char from_c,int from_r,char to_c,int to_r)
    {	op = opc;
    	source = src;
    	to_col = to_c;
    	from_col = from_c;
    	to_row = to_r;
    	from_row = from_r;
    	player = pla;
    }
    public boolean Same_Move_P(commonMove oth)
    {
    	QyshinsuMovespec other = (QyshinsuMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(QyshinsuMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.object = object;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
    	QyshinsuMovespec yto = (to == null) ? new QyshinsuMovespec() : (QyshinsuMovespec) to;

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
            source =  QIds.get(msg.nextToken());	// white rack or black rack
            from_col = '@';						// always
            from_row = G.IntToken(msg);			// index into the rack
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);			// cup size
 	        break;
	        
        case MOVE_REMOVE:			// robot move from board to board
            source = QIds.BoardLocation;		
            from_col = G.CharToken(msg);	//from col,row
            from_row = G.IntToken(msg);
            to_col = '@';
            to_row = G.IntToken(msg);       //cupsize
	        break;
	        
        case MOVE_DROPB:
	       source =  QIds.BoardLocation;
	       to_col = G.CharToken(msg);
	       to_row = G.IntToken(msg);
	       break;

		case MOVE_PICKB:
            source =  QIds.BoardLocation;
            from_col = G.CharToken(msg);
            from_row = G.IntToken(msg);

            break;

        case MOVE_PICK:
            source =  QIds.get(msg.nextToken());
            from_col = '@';
            from_row = G.IntToken(msg);
            break;
            
        case MOVE_DROP:
            source =  QIds.get(msg.nextToken());
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
    public Text shortMoveText(commonCanvas v)
    {
        switch (op)
        {
        case MOVE_PICKB:
        	if(object!=null) 
        	{ return TextChunk.join(TextGlyph.create("xxx",object,v,new double[] {1,1.25,0,-0.2}),
        			TextChunk.create(" "+from_row)
        			); 
        	}
            return TextChunk.create(""+from_col + from_row);

		case MOVE_DROPB:
			if(to_col=='B')
			{	return TextChunk.create(" >> ");
			}
            return TextChunk.create(" >> "+ to_row);

        case MOVE_DROP:
        	return TextChunk.create(" >>");
        case MOVE_PICK:
        	if(object!=null) { return TextGlyph.create("xxx",object,v,new double[] {1,1.25,0,-0.2}); }
            return TextChunk.create("("+from_row+")");
        case MOVE_RACK_BOARD:
        	if(object!=null) 
        	{	return TextChunk.join(TextGlyph.create("xxx",object,v,new double[] {1,1.25,0,-0.2}),
        			TextChunk.create(" >> "+to_row));
        	}
        	return TextChunk.create("("+from_row+") >> "+to_col + " " + to_row);
        case MOVE_REMOVE:
        	if(object!=null)
        	{
        		return(TextChunk.join(TextGlyph.create("xxx",object,v,new double[] {1,1.25,0,-0.2}),
        				TextChunk.create(" "+from_row+" >>")
        				));
        	}
        	return TextChunk.create(""+from_col + from_row+" >>");
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
        String ind = "";

        if (index() >= 0)
        {
            ind += (index() + " ");
        }
        // adding the move index as a prefix provides numnbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {

        case MOVE_PICKB:
	        return (ind+D.findUnique(op) + " " + from_col + " " + from_row);

		case MOVE_DROPB:
	        return (ind+D.findUnique(op) + " " + to_col + " " + to_row);

		case MOVE_RACK_BOARD:
			return(ind+D.findUnique(op) + " " +source.shortName+ " "+from_row+" " + to_col + " " + to_row);
		case MOVE_REMOVE:
			return(ind+D.findUnique(op) + " " + from_col + " " + from_row+" "+to_row);
			
        case MOVE_PICK:
            return (ind+D.findUnique(op) + " "+source.shortName+ " "+from_row);

        case MOVE_DROP:
             return (ind+D.findUnique(op) + " "+source.shortName+ " "+to_row);

        case MOVE_START:
            return (ind+"Start P" + player);

        default:
            return (ind+D.findUnique(op));
        }
    }

}