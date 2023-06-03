package blackdeath;

import java.util.*;

import lib.G;
import lib.Text;
import online.game.*;
import lib.ExtendedHashtable;
public class BlackDeathMovespec extends commonMPMove implements BlackDeathConstants
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_MORTALITY = 208;
    static final int MOVE_VIRULENCE = 209;
    static final int MOVE_INFECT = 210;				// unconditional infect
    static final int MOVE_INFECTION_ATTEMPT = 211;	// roll the dice and try
    static final int MOVE_ROLL = 212;			// roll 1 die
    static final int MOVE_ROLL2 = 213;			// roll 2 dice
    static final int MOVE_FROM_TO = 214;		// move a unit
    static final int MOVE_KILL = 215;			// kill a unit
    static final int MOVE_CURE = 216;			// kill a unit
    static final int MOVE_SELECT = 217;		// select die 1
    static final int MOVE_REINFECT = 218;	// reinfect from off the board
    static final int MOVE_PERFECTROLL = 219;	// perfect roll, guaranteed win
    static final int MOVE_USE_PERFECTROLL = 220;	// perfect roll, guaranteed win
    static final int MOVE_PLAYCARD = 221;
    static final int MOVE_KILLANDREMOVE = 222;	// kill a unit and the space
    static final int MOVE_QUARANTINE = 223;
    static final int MOVE_TEMPORARY_CHANGE_BOTH = 224;
    static final int MOVE_TEMPORARY_CHANGE_VIRULENCE = 225;
    static final int MOVE_TEMPORARY_CHANGE_MORTALITY = 226;
    static final int MOVE_TEMPORARY_SWAP_VIRULENCE = 227;
    static final int MOVE_TEMPORARY_CLOSE = 228;
    static final int MOVE_POGROM = 229;
    static final int MOVE_ESCAPE = 230;
    static
    {	// load the dictionary  
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
        "Pick", MOVE_PICK,
        "Pickb", MOVE_PICKB,
        "Drop", MOVE_DROP,
        "Dropb", MOVE_DROPB,
        "SetMortality",MOVE_MORTALITY,
        "SetVirulence", MOVE_VIRULENCE,
        "Infect",MOVE_INFECT,
        "InfectionAttempt",MOVE_INFECTION_ATTEMPT,
        "Movement", MOVE_FROM_TO,
        "Roll",MOVE_ROLL,
        "Roll2",MOVE_ROLL2,   
        "Kill",MOVE_KILL,
        "Cure",MOVE_CURE,
        "Select1",MOVE_SELECT,
        "Reinfect", MOVE_REINFECT,
        "PerfectRoll",MOVE_PERFECTROLL,
        "PerfectNext", MOVE_USE_PERFECTROLL,
         PlayCard, MOVE_PLAYCARD,
        "killAndRemove",MOVE_KILLANDREMOVE,
        "quarantine", MOVE_QUARANTINE,
        "pogrom",MOVE_POGROM,
        "tempBoth", MOVE_TEMPORARY_CHANGE_BOTH,
        "tempVirulence", MOVE_TEMPORARY_CHANGE_VIRULENCE,
        "tempMortality", MOVE_TEMPORARY_CHANGE_MORTALITY,
        "tempSwap",MOVE_TEMPORARY_SWAP_VIRULENCE,
        "closelink", MOVE_TEMPORARY_CLOSE,
         Escape, MOVE_ESCAPE);
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
    BlackDeathId source; 	// where from/to
    BlackDeathId dest;
    int to_row = 0;
    String from_name="";
    String to_name="";
    BlackDeathColor color;	// color for
    int cost;	// cost of movement
    BlackDeathChip selection;		// output feedback
    public String []gameEvents=null;
    
    public BlackDeathMovespec()// default constructor
    {
    }
    // constructor for virulence, mortality, roll, roll2
    public BlackDeathMovespec(int opc,BlackDeathColor col,int v,int pl)
    {
    	op = opc;
    	color = col;
    	to_row = v;
    	player = pl;
    }
    // constructor for mortality
    public BlackDeathMovespec(int opc,int pl)
    {
    	op = opc;
    	player = pl;
    }
    
    // constructor for set mortality/virulence
    public BlackDeathMovespec(BlackDeathColor c,int value)
    {
    	color = c;
    	to_row = value;
    }
    /* constructor */
    public BlackDeathMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    /** constructor for robot moves.  Having this "binary" constor is dramatically faster
     * than the standard constructor which parses strings
     */
    public BlackDeathMovespec(int opc,BlackDeathCell c,int who)
    {	dest = BlackDeathId.BoardLocation;
    	op = opc;
    	to_name = c.name;
    	player = who;
    }
    // constructor for play cards
    public BlackDeathMovespec(int opc,BlackDeathCell from,BlackDeathColor c,int ind,CardEffect e,BlackDeathCell to,int who)
    {
    	op = opc;
    	source = from.rackLocation();
    	dest = to.rackLocation();
    	color = c;
    	from_name = e.name();
    	to_row = ind;
    	player = who;
    }
    
    public BlackDeathMovespec(int opc,BlackDeathCell from,BlackDeathColor c,int ind,BlackDeathCell to,int who)
    {
    	op = opc;
    	source = from.rackLocation();
    	dest = to.rackLocation();
    	color = c;
    	to_row = ind;
    	player = who;
    }
    /* constructor for infection attempts */
    public BlackDeathMovespec(int opc,BlackDeathCell from,BlackDeathCell to,int who)
    {
    	op = opc;
    	player = who;
    	to_name = to.name;
    	from_name = from.name;
    	source = dest = BlackDeathId.BoardLocation;
    }
    /* constructor for move */
    public BlackDeathMovespec(int opc,BlackDeathCell from,BlackDeathCell to,int c,int who)
    {
    	op = opc;
    	player = who;
    	to_name = to.name;
    	from_name = from.name;
    	cost = c;
    	source = dest = BlackDeathId.BoardLocation;
    }
    /* constructor */
    public BlackDeathMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        BlackDeathMovespec other = (BlackDeathMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (dest == other.dest)
				&& (from_name.equals(other.from_name))
				&& (to_name.equals(other.to_name))
				&& (color == other.color)
				&& (cost == other.cost)
				&& (player == other.player));
    }

    public void Copy_Slots(BlackDeathMovespec to)
    {	super.Copy_Slots(to);
        to.to_row = to_row;
        to.source = source;
        to.color = color;
        to.to_name = to_name;
        to.from_name = from_name;
        to.cost = cost;
        to.gameEvents = gameEvents;
        to.selection = selection;
    }

    public commonMove Copy(commonMove to)
    {
        BlackDeathMovespec yto = (to == null) ? new BlackDeathMovespec() : (BlackDeathMovespec) to;

        // we need yto to be a BlackDeathMovespec at compile time so it will trigger call to the 
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
        case MOVE_MORTALITY:
        	{
        	color = BlackDeathColor.get(msg.nextToken());
        	to_row = G.IntToken(msg);
        	}
        	break;
        case MOVE_ROLL2:
        	
    		{
    		color = BlackDeathColor.get(msg.nextToken());
    		int r1 = G.IntToken(msg);
	   		to_row = r1*10 + G.IntToken(msg);
	   		}
    		break;
        case MOVE_PERFECTROLL:
        	color = BlackDeathColor.get(msg.nextToken());
        	to_row = 1;	//always success
        	break;
        case MOVE_ROLL:
        case MOVE_VIRULENCE:
        case MOVE_TEMPORARY_CHANGE_BOTH:
        case MOVE_TEMPORARY_CHANGE_VIRULENCE:
        case MOVE_TEMPORARY_CHANGE_MORTALITY:
        case MOVE_TEMPORARY_SWAP_VIRULENCE:
        	{
        	color = BlackDeathColor.get(msg.nextToken());
		   	to_row = G.IntToken(msg);
		   	}
		   	break;
        case MOVE_FROM_TO:
        	source = dest = BlackDeathId.BoardLocation;
         	from_name = msg.nextToken();
        	to_name = msg.nextToken();
        	cost = G.IntToken(msg);
        	break;
        case MOVE_TEMPORARY_CLOSE:
        case MOVE_INFECTION_ATTEMPT:
        	source = dest = BlackDeathId.BoardLocation;
         	from_name = msg.nextToken();
        	to_name = msg.nextToken();
        	break;
        case MOVE_SELECT:
        case MOVE_CURE:
        case MOVE_KILL:
        case MOVE_KILLANDREMOVE:
        case MOVE_QUARANTINE:
        case MOVE_POGROM:
		case MOVE_INFECT:
		case MOVE_REINFECT:
		case MOVE_DROPB:
			dest = BlackDeathId.BoardLocation;
			to_name = msg.nextToken();
			break;

		case MOVE_PICKB:
            source = BlackDeathId.BoardLocation;
            from_name = msg.nextToken();
            break;
        case MOVE_DROP:
        	color = BlackDeathColor.get(msg.nextToken());
            source = BlackDeathId.find(msg.nextToken());
            break;
 
        case MOVE_PLAYCARD:
         	color = BlackDeathColor.get(msg.nextToken());
            source = BlackDeathId.find(msg.nextToken());
            to_row = G.IntToken(msg);
            from_name = msg.nextToken();
            break;
         	
        case MOVE_PICK:
        	color = BlackDeathColor.get(msg.nextToken());
            source = BlackDeathId.find(msg.nextToken());
            to_row = G.IntToken(msg);
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;
        case MOVE_ESCAPE:
        case MOVE_USE_PERFECTROLL: break;
        default:

            break;
        }
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
        case MOVE_USE_PERFECTROLL: return("");
        case MOVE_VIRULENCE:
        	return G.concat("Virulence ",color.name()," = ",to_row);
        case MOVE_MORTALITY:
        	return G.concat("M ",color.name()," ",to_row);
        	
        case MOVE_TEMPORARY_CHANGE_BOTH:
        	return G.concat("tB ",color.name()," ",to_row);
        case MOVE_TEMPORARY_CHANGE_VIRULENCE:
        	return G.concat("tV ",color.name()," ",to_row);
        case MOVE_TEMPORARY_CHANGE_MORTALITY:
        	return G.concat("tM ",color.name()," ",to_row);
        case MOVE_TEMPORARY_SWAP_VIRULENCE:
        	return G.concat("tS ",color.name()," ",to_row);
        	
        case MOVE_ROLL:
        	String msg = ((selection==BlackDeathChip.SkullIcon)
					? "Mortality "
					: "");
        	return G.concat(msg,color.name()," Roll-",to_row);
        	
        case MOVE_PERFECTROLL:
        	return G.concat(color.name()," ","Perfect Roll-1");
        case MOVE_ROLL2:
        	{
        	int r1 = to_row/10;
        	int r2 = to_row%10;
        	return G.concat(color.name()," Roll-",r1," Roll-",r2);
        	}
        case MOVE_PICKB:
            return ("");

		case MOVE_SELECT:
			String name = to_name;
			if(selection!=null && selection.isDie()) 
				{ name = "Roll-"+selection.dieValue(); } 
			return G.concat("Select ",name," infection attempts");
			
		case MOVE_DROPB:
            return G.concat( " " ,to_name);

		case MOVE_PLAYCARD:
            return G.concat(D.findUniqueTrans(op) , " ",color," ",source.shortName," ",to_row," ",from_name);
			
			
        case MOVE_DROP:
        case MOVE_PICK:
            return G.concat(D.findUniqueTrans(op) ," ",color," ",source.shortName);

        case MOVE_DONE:
            return ("");
            
        case MOVE_KILLANDREMOVE:
        	return G.concat(" x ",to_name);
        	
        case MOVE_QUARANTINE:
        	return G.concat(" Q ",to_name);
        case MOVE_POGROM:
        	return G.concat(" P ",to_name);
      	
        case MOVE_KILL:
        case MOVE_CURE:
        	return G.concat("kill ",to_name);
        	
        case MOVE_REINFECT:
        case MOVE_INFECT:
        	return G.concat("Infect ",to_name);
        case MOVE_FROM_TO:
        	return G.concat("Move ",from_name," to ",to_name," cost ",(-cost));
        case MOVE_TEMPORARY_CLOSE:
        	return G.concat("x ",from_name," to ",to_name);
        case MOVE_INFECTION_ATTEMPT:
        	return G.concat("Infect ",from_name," to ",to_name);
        	
        case MOVE_ESCAPE:
        default:
            return (D.findUniqueTrans(op));

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
        case MOVE_PERFECTROLL:
        	return(G.concat(opname,color.name()));
        	
        case MOVE_ROLL2:
        	{
        	int r1 = to_row/10;
        	int r2 = to_row%10;
        	return(G.concat(opname,color.name()," ",r1," ",r2));
        	}
        case MOVE_ROLL:
        case MOVE_VIRULENCE:
        case MOVE_TEMPORARY_CHANGE_BOTH:
        case MOVE_TEMPORARY_CHANGE_VIRULENCE:
        case MOVE_TEMPORARY_CHANGE_MORTALITY:
        case MOVE_MORTALITY:
        case MOVE_TEMPORARY_SWAP_VIRULENCE:
        	return G.concat(opname,color.name()," "+to_row);

        case MOVE_PICKB:
	        return G.concat(opname, from_name);
        case MOVE_KILL:
        case MOVE_KILLANDREMOVE:
        case MOVE_CURE:
        case MOVE_SELECT:
        case MOVE_QUARANTINE:
        case MOVE_POGROM:
		case MOVE_DROPB:
	        return G.concat(opname,to_name);
		case MOVE_FROM_TO:
			return G.concat(opname,from_name," ",to_name," "+cost);			
		case MOVE_INFECTION_ATTEMPT:
		case MOVE_TEMPORARY_CLOSE:
			return G.concat(opname,from_name," ",to_name);
		case MOVE_REINFECT:
		case MOVE_INFECT:
			return G.concat(opname,to_name);
		case MOVE_DROP:
            return G.concat(opname, color.name()," ",source.shortName);

		case MOVE_PLAYCARD:
	           return G.concat(opname,color.name()," ",source.shortName," "+to_row," ",from_name);
		
		case MOVE_PICK:
            return G.concat(opname, color.name()," ",source.shortName," "+to_row);

        case MOVE_START:
            return (G.concat(indx,"Start P" ,player));
        case MOVE_USE_PERFECTROLL:
        case MOVE_ESCAPE:
        default:
            return (G.concat(opname));
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

    public String[] gameEvents()
    {	return(gameEvents);
    }
    
    public Text censoredMoveText(BlackDeathViewer v,BlackDeathBoard b)
    {	// default treatment
    	return(shortMoveText(v));
    }
    
}
