package ponte;

import online.game.*;
import ponte.PonteConstants.Bridge;
import ponte.PonteConstants.PonteId;

import java.util.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.ExtendedHashtable;


public class PonteMovespec extends commonMove
{
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
 	static final int MOVE_PLACE_BRIDGE = 211;	// place a bridge
 	static final int MOVE_PLACE_TILE = 212;		// place a tile
 	static final int MOVE_GAMEOVER = 213;		// end the game
 	static final int MOVE_BRIDGE_END = 214;		// set bridge angle
    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D);
        D.putInt("Pick", MOVE_PICK);
        D.putInt("Pickb", MOVE_PICKB);
        D.putInt("Drop", MOVE_DROP);
        D.putInt("Dropb", MOVE_DROPB);
		D.putInt("Bridge",MOVE_PLACE_BRIDGE);
		D.putInt("GameOver",MOVE_GAMEOVER);
		D.putInt("Tile",MOVE_PLACE_TILE);
		D.putInt("BridgeEnd",MOVE_BRIDGE_END);
		for(PonteId val : PonteId.values())
			{ String n = val.shortName; 
			  if((n!=null)&&(D.get(n)==null)) { D.putInt(n, val.ordinal()+100); }
			}
   }
    
    PonteId source; // where from/to
	char col; //for from-to moves, the source column
	int row; // for from-to moves, the source row
    String bridgeEnd=null;
    PonteId bridgeId = null;
    public PonteMovespec() // default constructor
    {
    }
    
    public PonteMovespec(int opc, int pl)	// constructor for simple moves
    {
    	player = pl;
    	op = opc;
    }
    
    // constructor for bridge placement moves
    public PonteMovespec(PonteCell c,Bridge b,int who)
    {	op = MOVE_PLACE_BRIDGE;
    	col = c.col;
    	row = c.row;
    	source = b.id;
    	player = who;
    }
    
    
    // constructor for tile placement moves
    public PonteMovespec(PonteCell c,PonteCell b,int who)
    {	op = MOVE_PLACE_TILE;
    	col = c.col;
    	row = c.row;
    	player = who;
    	source = b.rackLocation();
    }
    
    /* constructor */
    public PonteMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public PonteMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    public boolean Same_Move_P(commonMove oth)
    {
    	PonteMovespec other = (PonteMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (row == other.row)
				&& (col == other.col)
				&& (player == other.player));
    }

    public void Copy_Slots(PonteMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
        to.bridgeEnd = bridgeEnd;
        to.bridgeId = bridgeId;
        to.col = col;
        to.row = row;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
    	PonteMovespec yto = (to == null) ? new PonteMovespec() : (PonteMovespec) to;

        // we need yto to be a PonteMovespec at compile time so it will trigger call to the 
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
      
        case MOVE_PLACE_TILE:
        	source = PonteId.find(msg.nextToken());
 	       	col = G.CharToken(msg);
 	       	row = G.IntToken(msg);
 	       	break;
	        
        case MOVE_PLACE_BRIDGE:
	       	source = PonteId.find(msg.nextToken());
	       	col = G.CharToken(msg);
 	       	row = G.IntToken(msg);
 	       	break;
        case MOVE_BRIDGE_END:
        case MOVE_DROPB:
	       source = PonteId.BoardLocation;
	       col = G.CharToken(msg);
	       row = G.IntToken(msg);
	       break;

		case MOVE_PICKB:
            source = PonteId.BoardLocation;
            col = G.CharToken(msg);
            row = G.IntToken(msg);
 
            break;

        case MOVE_PICK:
             source = PonteId.find(D.getInt(msg.nextToken())-100);
            col = '@';
             break;
            
        case MOVE_DROP:
            source = PonteId.find(D.getInt(msg.nextToken())-100);
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

        default:
            break;
        }
    }

   
    private Text getChipGlyph(PonteId src,commonCanvas viewer)
    {	double scl[] = src.chip.logScales;
    	//G.print("s "+src+" "+scl[0]+" "+scl[1]+" "+scl[2]+" "+scl[3]);
    	//scl = new double[] {1.0 ,0.8 ,-1.2, -1.0};
    	return(TextGlyph.create("xx",src.chip,viewer,
				scl));
    }
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas viewer)
    {
        switch (op)
        {
        case MOVE_PLACE_TILE:
        	return(TextChunk.join(
        			getChipGlyph(source,viewer),
        			TextChunk.create("-"+col+row+" ")));
        case MOVE_PLACE_BRIDGE:
        	return(TextChunk.join(
        			getChipGlyph(source,viewer),
        			TextChunk.create("-"+col+row+" ")));
        case MOVE_PICKB:
            return (TextChunk.create("@"+col + row+((bridgeEnd==null)?"":"-"+bridgeEnd)));
		case MOVE_DROPB:
				// a little ad-hoc editing.  If the bridge was placed in an ambiguous place
				// and the user switched it, we have the initial drop plus the switch. So
				// we edit out the initial drop.
			if((next!=null) && (next.op==MOVE_BRIDGE_END)) { return(TextChunk.create("")); }
			//$FALL-THROUGH$
		case MOVE_BRIDGE_END:
			return (TextChunk.create(""+ col+ row));
        case MOVE_DROP:
        	return(TextChunk.create(""+col+row));
        case MOVE_PICK:
        	if(next!=null) 
        	{	PonteMovespec n = (PonteMovespec)next;
        		if((n.next!=null) && (n.op==MOVE_DROPB) ) 
        			{ PonteMovespec nn = (PonteMovespec)n.next;
        			  if((nn.op==MOVE_BRIDGE_END) && (nn.bridgeId!=null)) { n = nn; }
        			}
        			else { n = this; }
        		// look ahead to the drop, which records what angle we used
        		if(n.bridgeId!=null)
        		{
        		return(TextChunk.join(getChipGlyph(n.bridgeId,viewer),TextChunk.create("-")));
        		}
        	}
            return (TextChunk.join(getChipGlyph(source,viewer),TextChunk.create("-")));
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
		String indx = indexString();
		String opname = indx+D.findUnique(op)+" ";
        // adding the move index as a prefix provides numnbers
        // for the game record and also helps navigate in joint
        // review mode
        switch (op)
        {
        case MOVE_PLACE_TILE:
        	return(opname+source.name()+" "+col+" "+row);

        case MOVE_PLACE_BRIDGE:
        	return(opname+source.name()+" "+col+" "+row);

        case MOVE_PICKB:
        case MOVE_BRIDGE_END:
		case MOVE_DROPB:
	        return (opname+ col + " " + row);

		case MOVE_PICK:
            return (opname+D.findUnique(source.ordinal()+100));

        case MOVE_DROP:
             return (opname+D.findUnique(source.ordinal()+100));

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
