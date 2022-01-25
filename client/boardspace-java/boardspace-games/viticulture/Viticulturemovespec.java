package viticulture;

import java.util.*;

import lib.G;
import lib.StackIterator;
import lib.Text;
import lib.TextChunk;
import online.game.*;
import lib.ExtendedHashtable;
public class Viticulturemovespec extends commonMPMove implements ViticultureConstants
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_SELECT = 208;	// select a cell
    static final int MOVE_SELECTWAKEUP = 209;
    static final int MOVE_PLACE_WORKER = 210;
    static final int MOVE_PLACE_STAR = 211;
    static final int MOVE_BUILD = 212;
    static final int MOVE_DISCARD = 213;
    static final int MOVE_PLANT = 214;
    static final int MOVE_SKIP = 215;
    static final int MOVE_NEWWAKEUP = 216;
    static final int MOVE_TRADE = 217;
    static final int MOVE_MAKEWINE = 218;
    static final int MOVE_SELLWINE = 219;
    static final int MOVE_FILLWINE = 220;
    static final int MOVE_UPROOT = 221;
    static final int MOVE_STAR = 222;
    static final int MOVE_RETRIEVE = 223;
    static final int MOVE_SWITCH = 225;
    static final int MOVE_TAKEACTION = 226;
    static final int MOVE_AGEONE = 227;	// age 1 wine
    static final int MOVE_NEXTSEASON = 228;
    static final int MOVE_BUILDCARD = 229;
    static final int MOVE_TRAIN = 230;	// train a worker
    static final int MOVE_BONUS = 231;	// place a bonus action
    static final int MOVE_UNSELECT = 232;	// undo selections
    
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D);	// this adds "start" "done" "edit" and so on.
        D.putInt("Pick", MOVE_PICK);
        D.putInt("Pickb", MOVE_PICKB);
        D.putInt("Drop", MOVE_DROP);
        D.putInt("Dropb", MOVE_DROPB);
        D.putInt("Select",MOVE_SELECT);
        D.putInt("SelectWakeup",MOVE_SELECTWAKEUP);
        D.putInt("PlaceStar", MOVE_PLACE_STAR);
        D.putInt("MoveStar", MOVE_STAR);
        D.putInt("Place", MOVE_PLACE_WORKER);
        D.putInt("Build",MOVE_BUILD);
        D.putInt("Discard", MOVE_DISCARD);
        D.putInt("Plant", MOVE_PLANT);
        D.putInt("SkipSecondAction",MOVE_SKIP);
        D.putInt("NewWakeup",MOVE_NEWWAKEUP);
        D.putInt("Trade",MOVE_TRADE);
        D.putInt("MakeWine",MOVE_MAKEWINE);
        D.putInt("SellWine",MOVE_SELLWINE);
        D.putInt("FillWine",MOVE_FILLWINE);
        D.putInt("Uproot",MOVE_UPROOT);
        D.putInt("Retrieve",MOVE_RETRIEVE);
        D.putInt("Switch", MOVE_SWITCH);
        D.putInt("TakeAction", MOVE_TAKEACTION);
        D.putInt("AgeOne", MOVE_AGEONE);
        D.putInt("nextSeason", MOVE_NEXTSEASON);
        D.putInt("BuildCard", MOVE_BUILDCARD);
        D.putInt("Train",MOVE_TRAIN);
        D.putInt("PlaceBonus",MOVE_BONUS);
        D.putInt("Unselect", MOVE_UNSELECT);
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
    ViticultureId source; // where from/to
    ViticultureId dest;
    char from_col;
    int from_row;
    int from_index;		// used for second red in champagne
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    StackIterator<ViticultureChip> cards=null;				// cards accumulated as we move
    StackIterator<ViticultureChip> replayCards=null;		// read for replay
       
    public String []gameEvents=null;
    public String[] gameEvents() { return(gameEvents); }
    public ViticultureChip currentWorker = null;
	public double montecarloWeight;
    
    public Viticulturemovespec()
    {
    } // default constructor

    /* constructor */
    public Viticulturemovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    // for makewine moves
    public Viticulturemovespec(int opc,ViticultureId des,ViticultureCell sou,int grape2,int grape3,int pl)
    {
    	op = opc;
    	player = pl;
    	dest = des;
    	source = sou.rackLocation();
    	to_col = from_col = sou.col;
    	from_row = sou.row;
    	from_index = grape2;
    	to_row = grape3;
 
    } 
    
    // for switch field moves
    public Viticulturemovespec(int opc,ViticultureCell from,int fidx,ViticultureCell to,int toidx,int pl)
    {
    	op = opc;
    	player = pl;
    	dest = to.rackLocation();
    	to_col = to.col;
    	to_row = to.row;
    	source = from.rackLocation();
    	from_col = from.col;
    	from_row = from.row;
    	from_index = fidx*100+toidx;
     }
    
    // for move type operations
    public Viticulturemovespec(int opc,ViticultureCell from,int index,ViticultureCell to,int whoseTurn)
    {
    	player = whoseTurn;
    	op = opc;
    	source = from.rackLocation();
    	from_col = from.col;
    	from_row = from.row;
    	dest = to.rackLocation();
    	to_col = to.col;
    	to_row = to.row;
    	from_index = index;
    }
    // for trade moves
    public Viticulturemovespec(int opc,ViticultureId sou,char col,int row,int indx,ViticultureId de,int pl)
    {
    	op = opc;
    	player = pl;
    	source = sou;
    	dest = de;
    	from_col = to_col = col;
    	from_row = row;
    	from_index = indx;
    	to_row = 0;
    }
    // for place worker moves, idx is the index in the cell, not the index OF the cell (which is zero)
    public Viticulturemovespec(int opc,ViticultureId sou,int idx,ViticultureCell d,int pl)
    {
    	op = opc;
    	player = pl;
    	source = sou;
    	from_col = (char)('A'+player);
    	from_row = 0;
    	from_index = idx;
    	dest = d.rackLocation();
    	to_col = d.col;
    	to_row = d.row;
    }
    /* constructor */
    public Viticulturemovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }
    public Viticulturemovespec(int opc,ViticultureId s,int who)
    {	op = opc;
    	dest = source = s;
    	to_col = from_col = '@';
    	to_row = from_row = 0;
    	player = who;
    }

    // for plant
    public Viticulturemovespec(int opc,PlayerBoard pb,int from,int to,int whoseTurn)
    {
    	op = opc;
    	source = ViticultureId.Cards;
    	dest = ViticultureId.Field;
    	from_col = to_col = pb.colCode;
    	from_row = 0;
    	from_index = from;
    	to_row = to;
    	player = whoseTurn;
    }
    
    public Viticulturemovespec(int opc,int who)
    {
    	op = opc;
    	player = who;
    }

    // for select and sell wine
    public Viticulturemovespec(int opc,ViticultureCell c,int who)
    {	op = opc;
    	dest = source = c.rackLocation();
    	from_row = to_row = c.row;
    	to_col = from_col = c.col;
    	from_index = 0;
    	player = who;
    }
    // for select and sell cards
    public Viticulturemovespec(int opc,ViticultureCell c,int fidx,int who)
    {	op = opc;
    	dest = source = c.rackLocation();
    	from_row = to_row = c.row;
    	to_col = from_col = c.col;
    	from_index = fidx;
    	player = who;
    }
    
    // for wine making
    public Viticulturemovespec(int opc, ViticultureId from,char fc,int fr,int fr2,int fr3,ViticultureId to,int pl)
    {
    	op = opc;
    	source = from;
    	from_col = to_col = fc;
    	from_row = fr;
    	from_index = fr2;
    	to_row = fr3; 
    	dest = to;
    	player = pl;
    }
    
    // training worker
    public Viticulturemovespec(int opc,ViticultureId to,char col,ChipType kind,int pl)
    {
    	op = opc;
    	player = pl;
    	source = dest = to;
    	from_col = to_col = col;
    	from_row = to_row = 0;
    	from_index = kind.ordinal();
    }
    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        Viticulturemovespec other = (Viticulturemovespec) oth;

        return (other!=null 
        		&& (op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (from_index == other.from_index)
				&& (dest == other.dest)
				&& (player == other.player));
    }
    
    public boolean sameWine(Viticulturemovespec other,boolean exact)
    {
        // exact is a flag that should always be true, but is false
    	// for games before rev 115.  This bug allowed under-value champagne to be made
        return ((op == other.op) 
				&& (source == other.source)
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (!exact || (to_row == other.to_row))
            	// from_index==-1 is a cheap champagne made with the charmat and only 1 red + 1 white
				&& ((other.from_index==-1) || (from_index == other.from_index))
				&& (dest == other.dest)
				&& (player == other.player));
    }

    public void Copy_Slots(Viticulturemovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.source = source;
        to.from_col = from_col;
        to.from_row = from_row;
        to.from_index = from_index;
        to.dest = dest;
        to.cards = cards;
        to.replayCards = replayCards;
        to.currentWorker = currentWorker;
        to.gameEvents = gameEvents;
    }
    public String currentWorkerName()
    {
    	if(currentWorker!=null) 
    		{ ViticultureColor color = currentWorker.color;
    		  ChipType type = currentWorker.type;
    		  return(((color==null) ? "" : color.name())+"-"+((type==null)? "" : type.name())); }
    	return(null);
    }
    public commonMove Copy(commonMove to)
    {
        Viticulturemovespec yto = (to == null) ? new Viticulturemovespec() : (Viticulturemovespec) to;

        // we need yto to be a xxxmovespec at compile time so it will trigger call to the 
        // local version of Copy_Slots
        Copy_Slots(yto);

        return (yto);
    }
    
    private void parseExtraChips(StringTokenizer msg)
    {	
    	if(msg.hasMoreTokens())
    	{
    		String tok = msg.nextToken();
    		if(tok.equals("("))
    		{	
    			while(msg.hasMoreTokens())
    			{String type = msg.nextToken();
    			String rest = "";
    			String space = "";
    			if(")".equals(tok)) { break; }
    			
    			while(msg.hasMoreTokens())
    			{
    				tok = msg.nextToken();
    				rest = rest + space + tok;
    				space = " ";
    				if(tok.endsWith("\"")) 
					{ ViticultureChip next = ViticultureChip.getChip(ChipType.find(type),rest);
					  replayCards = (replayCards == null) ?  next : replayCards.push(next);
					  rest = "";
					  break;
					}
    			}}
    		}
    	}
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
        	throw G.Error("Cant parse %s", cmd);
        case MOVE_PASS:
        	op = MOVE_NEXTSEASON;
        	break;
         	
        case MOVE_RETRIEVE:
        	source = ViticultureId.get(msg.nextToken());
        	from_col = G.CharToken(msg);
        	from_row = G.IntToken(msg);
        	from_index = G.IntToken(msg);
        	dest = ViticultureId.get(msg.nextToken());
        	to_col = G.CharToken(msg);
        	break;
        	       	
        case MOVE_TRAIN:
        	source = dest = ViticultureId.get(msg.nextToken());
        	from_col = to_col = G.CharToken(msg);
        	from_row = to_row = 0;
        	from_index = ChipType.find(msg.nextToken()).ordinal();
        	break;
        	
        case MOVE_MAKEWINE:
        	dest = ViticultureId.get(msg.nextToken());
        	to_col = from_col = G.CharToken(msg);
        	from_row = G.IntToken(msg);
        	source =  (dest==ViticultureId.WhiteWine) ? ViticultureId.WhiteGrape : ViticultureId.RedGrape;
        	// second bottle in from_index
        	// third bottle in to_row
         	switch(dest)
        	{
        	case Champaign:	
        		from_index = G.IntToken(msg);
        		to_row = G.IntToken(msg);
        		break;
			case RoseWine: 
				from_index = G.IntToken(msg);
				break;
			default: ;
        	}
        	break;
        	
        case MOVE_PLACE_WORKER:       	
        	from_col = G.CharToken(msg);
        	from_row = 0;
        	from_index = G.IntToken(msg);
        	source = ViticultureId.Workers;
        	dest =  ViticultureId.get(msg.nextToken());
        	to_col = G.CharToken(msg);
        	to_row = G.IntToken(msg);
        	break;
        	
        case MOVE_TRADE:
        	source = ViticultureId.get(msg.nextToken());      	
        	from_col = G.CharToken(msg);
        	from_row = G.IntToken(msg);
        	from_index = G.IntToken(msg);

        	dest =  ViticultureId.get(msg.nextToken());
        	to_col = from_col;
        	to_row = 0;
         	break;
        	       	
        case MOVE_DROPB:
           	dest = ViticultureId.get(msg.nextToken());
           	to_col = '@';
        	to_row = G.IntToken(msg);
        	break;
      	
        case MOVE_DROP:
        	dest = ViticultureId.get(msg.nextToken());
        	to_col = G.CharToken(msg);
        	to_row = G.IntToken(msg);
        	break;
        	
        case MOVE_PICKB:
            source = ViticultureId.get(msg.nextToken());
            from_col = '@';
            from_row = G.IntToken(msg);
            from_index = msg.hasMoreTokens() ? G.IntToken(msg) : 0;
            break;
            
        case MOVE_UPROOT:
        case MOVE_PICK:
            source = ViticultureId.get(msg.nextToken());
            dest = ViticultureId.Field;
            from_col = to_col = G.CharToken(msg);
            from_row = to_row = G.IntToken(msg);
            from_index = G.IntToken(msg);
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());
            break;
          
        case MOVE_DISCARD:
        case MOVE_FILLWINE:
        case MOVE_SELLWINE:
        case MOVE_AGEONE:
        case MOVE_SELECT:
        	dest = source = ViticultureId.get(msg.nextToken());
        	to_col = from_col = G.CharToken(msg);
        	to_row = from_row = G.IntToken(msg);
        	from_index = G.IntToken(msg);
        	break;
        case MOVE_STAR:
        	source = dest = ViticultureId.StarTrack;
        	from_col = to_col = '@';
        	from_row = G.IntToken(msg);
        	from_index = G.IntToken(msg);
        	to_row = G.IntToken(msg);
        	break;
        	
        case MOVE_TAKEACTION:
        	source = dest = ViticultureId.get(msg.nextToken());
        	from_col = to_col = '@';
        	from_row = to_row = 3;
        	from_index = 0;
        	break;
        	
        case MOVE_BONUS:
        	source = dest = ViticultureId.get(msg.nextToken());
        	from_col = to_col = '@';
        	from_row = to_row = G.IntToken(msg);
        	break;
        	
        case MOVE_PLACE_STAR:
        	source = dest = ViticultureId.StarTrack;
        	from_col = to_col = '@';
        	from_row = to_row = G.IntToken(msg);
        	break;
        case MOVE_PLANT:
        	source = ViticultureId.Cards;
        	from_col = G.CharToken(msg);
        	from_row = 0;
        	from_index = G.IntToken(msg);
        	dest = ViticultureId.Field;
        	to_col = G.CharToken(msg);
        	to_row = G.IntToken(msg);
        	break;
        case MOVE_SWITCH:
        	source = ViticultureId.Vine;
        	from_col = G.CharToken(msg);
        	from_row = G.IntToken(msg);
        	from_index = G.IntToken(msg);
        	dest = ViticultureId.Vine;
        	to_col = G.CharToken(msg);
        	to_row = G.IntToken(msg);
        	from_index = 100*from_index+G.IntToken(msg);
         	break;
        case MOVE_BUILDCARD:
        	source = ViticultureId.Cards;
        	from_index = G.IntToken(msg);
           	dest = ViticultureId.get(msg.nextToken());
        	from_col = to_col = G.CharToken(msg);
        	from_row = to_row = 0;
        	break;
        case MOVE_BUILD:
        	dest = ViticultureId.get(msg.nextToken());
        	source = dest.getUnbuilt();
        	from_col = to_col = G.CharToken(msg);
        	from_row = to_row = 0;
        	break;
        case MOVE_NEWWAKEUP:
        	dest = ViticultureId.RoosterTrack;
        	to_col = G.CharToken(msg);
        	to_row = G.IntToken(msg);
        	break;

        case MOVE_SELECTWAKEUP:
        	dest = ViticultureId.RoosterTrack;
        	to_col = 'A';
        	to_row = G.IntToken(msg);
        	break;
        default:

            break;
        }
        parseExtraChips(msg);
    }
    
    Text censoredMoveText(ViticultureViewer v,ViticultureBoard b)
    {	
    	// default treatment
    	return(shortMoveText(v));
    }

    /** construct an abbreviated move string, mainly for use in the game log.  These
     * don't have to be parseable, they're intended only to help humans understand
     * the game record.  The alternative method {@link #shortMoveText} can be implemented
     * to provide colored text or mixed text and icons.
     * 
     * */
    public String shortMoveString()
    {
        switch (op)
        {
        case MOVE_PICKB:
        	String name = currentWorkerName();
        	return(name==null ? source.shortName : "Retrieve "+name);

		case MOVE_DROPB:
			return (dest.shortName);
		case MOVE_TAKEACTION:
			return("takeaction "+source.shortName);
			
		case MOVE_SWITCH:
			return("switch "+from_col+" "+from_row+" "+(from_index/100)+" "+to_col+" "+to_row+" "+(from_index%100));
			
		case MOVE_MAKEWINE:
			return("make "+dest.shortName+" ");
			
		case MOVE_PLACE_WORKER:
			{
			String worker = currentWorkerName();
			if(worker==null) { worker = source.shortName; }
			return(worker+" "+dest.shortName);
			}
		case MOVE_TRADE:
			return(source.shortName+" "+from_col+" "+from_row+" "+from_index+" "+dest.shortName);
		
        case MOVE_UPROOT:
        	return ("Uproot "+from_row+" "+from_index);
        	
        case MOVE_DROP:
        case MOVE_DISCARD:
        case MOVE_SELECT:
        case MOVE_PICK:
        case MOVE_UNSELECT:
        case MOVE_DONE:
            return ("");
        	
        case MOVE_PLANT:
        	return("plant "+from_index+" "+to_row);
       
        case MOVE_BUILDCARD:
        	return(" StructureCards");
        	
        case MOVE_BUILD:
        	{
        	String color = currentWorker!=null ? currentWorker.color.name()+"-" : "";
        	return("build "+color+dest.shortName);
        	}        	

        case MOVE_RETRIEVE:
        	return("retrieve from "+source.shortName+" "+from_index);
        	
        case MOVE_TRAIN:
        	{
        	String color = currentWorker!=null ? currentWorker.color.name()+"-" : "";
			return("train "+color+ChipType.values()[from_index]);
        	}
        case MOVE_SELLWINE:
        	return("sell "+source.name());
        case MOVE_FILLWINE:
        case MOVE_AGEONE:
           	return(D.findUnique(op)+" "+source.name());
        case MOVE_BONUS:
           	return("+bonus");
        case MOVE_PLACE_STAR:
        	{
        	String color = currentWorker!=null ? currentWorker.color.name()+"-" : "";
        	return(" "+color+"Star "+RegionNames[to_row]);
        	}
        case MOVE_STAR:
        	return("move "+RegionNames[from_row]+" "+RegionNames[to_row]);
        case MOVE_NEWWAKEUP:
           	return("new wakeup "+to_col+" "+(to_row+1));

        case MOVE_SELECTWAKEUP:
        	return("wakeup "+(to_row+1));
        	
        default:
            return (D.findUnique(op));

        }
    }
    
   
    /**
     * shortMoveText lets you return colorized text or mixed text and graphics.
     * @see lib.Text
     * @see lib.TextGlyph 
     * @see lib.TextChunk
     * @param v
     * @return a Text object
     */
    private static commonMove prev;
    public Text shortMoveText(commonCanvas v)
    {	String m = shortMoveString();
    	if((prev!=null) && (prev.op==MOVE_MAKEWINE) && (op == MOVE_MAKEWINE))
    	{
    		m = m.substring(5);
    	}
    	prev = this;
    	return(TextChunk.create(m));
    }
    // print the extra cards accumulated while playing this move
    private String extraChips()
    {	if(cards!=null)
    	{
    	StringBuilder val = new StringBuilder();
    	val.append(" ( ");
    	for(int i=0;i<cards.size(); i++)
    	{
    		ViticultureChip card = cards.elementAt(i);
    		val.append(card.type);
    		val.append(" ");
    		val.append('"');
    		val.append(card.cardName);
    		val.append('"');
    		val.append(" ");
    	}
    	val.append(" ) ");
    	return(val.toString());
    	}
    	return("");
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
        case MOVE_PICKB:
        	String index = from_index==0 ? "" : " "+from_index;
	        return (opname + source.name()+ " " + from_row+index);

 		case MOVE_DROPB:
	        return (opname + dest.name()+" " + to_row + extraChips());

        case MOVE_DROP:
            return (opname + dest.name()+" "+to_col+" "+to_row + extraChips());
       	
        case MOVE_UPROOT:
        case MOVE_PICK:
            return (opname+source.name()+" "+from_col+" "+from_row+" "+from_index + extraChips());
          

        case MOVE_RETRIEVE:
        	return(opname+source.name()+" "+from_col+" "+from_row+" "+from_index+" "+dest.name()+" "+to_col + extraChips());

        case MOVE_TRADE:
        	return(opname+source.name()+" "+from_col+" "+from_row+" "+from_index+" "+dest.name() + extraChips());

 		case MOVE_MAKEWINE:
			String rest = " "+from_row;
			switch(dest)
			{
			case Champaign: rest += " "+from_index+" "+to_row;
				break;
			case RoseWine: rest += " "+from_index;
				break;
			default: ;
			}
			return(opname+dest.name()+" "+to_col+rest + extraChips());

		case MOVE_PLACE_WORKER:
			return(opname+from_col+" "+from_index+" "+dest.name()+" "+to_col+" "+to_row);

		case MOVE_TRAIN:
			return(opname+dest.name()+" "+to_col+" "+ChipType.values()[from_index]);
			
        case MOVE_START:
            return (indx+"Start P" + player + extraChips());

        case MOVE_SELLWINE:
        case MOVE_FILLWINE:
        case MOVE_DISCARD:
        case MOVE_AGEONE:
        case MOVE_SELECT:
        	return (opname+ source.name()+" "+from_col+" "+from_row+" "+from_index + extraChips());
        	
        case MOVE_PLANT:
        	 return(opname+from_col+" "+from_index+" "+to_col+" "+to_row + extraChips());
       
        case MOVE_BUILDCARD:
        	return(opname+from_index+" "+dest.name()+" "+to_col +" "+to_row+ extraChips());
        	
        case MOVE_BUILD:
        	return(opname+dest.name()+" "+to_col + extraChips());
        case MOVE_NEWWAKEUP:
        	return(opname+to_col+" "+to_row + extraChips());
        	
        case MOVE_STAR:
        	return(opname+from_row+" "+from_index+" "+to_row + extraChips());
        	
        case MOVE_TAKEACTION:
        	return(opname+dest.name() + extraChips());
        	
        case MOVE_SWITCH:
			return(opname+from_col+" "+from_row+" "+(from_index/100)+" "+to_col+" "+to_row+" "+(from_index%100) + extraChips());
 
        case MOVE_BONUS:
        	return(opname+source.name()+" "+to_row + extraChips());
        	
        case MOVE_PLACE_STAR:
        case MOVE_SELECTWAKEUP:
        	return(opname+to_row + extraChips());
        default:
        case MOVE_UNSELECT:
            return (opname + extraChips());
        }
    }
    /**
     *  longMoveString is used for sgf format records and can contain other information
     * intended to be ignored in the normal course of play, for example human-readable
     * information
     */
    public String longMoveString()
    {	String str = moveString();
		if(gameEvents!=null)
		{	
		// Save the game events to the log
		StringBuilder s = new StringBuilder();
		for(int i=0;i<gameEvents.length;i++) { s.append(gameEvents[i]); s.append('\n'); }
		setComment(s.toString());
		}
    	return(str);
    }

    public static Viticulturemovespec copyFrom(Viticulturemovespec from)
    {	return ((from==null) ? null : (Viticulturemovespec)from.Copy(null));
    }
    
}
