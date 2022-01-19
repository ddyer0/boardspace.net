package tammany;

import java.util.*;

import lib.G;
import online.game.*;
import lib.ExtendedHashtable;

public class TammanyMovespec extends commonMPMove implements TammanyConstants
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_FROM_TO = 210; // move from place to place on the board
    static final int MOVE_VOTE = 211;	// transmit vote totals
    static final int MOVE_CUBE = 212;	
    static final int MOVE_PICK_CUBE = 213;
    static final int MOVE_SLANDER = 214;
    static final int MOVE_ELECT = 215;	// stard election in a ward
    
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D);	// this adds "start" "done" "edit" and so on.
        D.putInt("Pick", MOVE_PICK);
        D.putInt("Pickb", MOVE_PICKB);
        D.putInt("Drop", MOVE_DROP);
        D.putInt("Dropb", MOVE_DROPB);
        D.putInt("Move",MOVE_FROM_TO);
        D.putInt("Vote",MOVE_VOTE);
        D.putInt("MoveC",MOVE_CUBE);
        D.putInt("PickC",MOVE_PICK_CUBE);
        D.putInt("Slander",MOVE_SLANDER);
        D.putInt("Elect",MOVE_ELECT);
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
    TammanyId source; // where from/to
    TammanyId dest;	
    char from_col;	// player index 
    char to_col;
    int to_row; 	// for from-to moves, the destination row
    int from_row;	
    int from_cube;	// the particular cube index
    double montecarloWeight = 1.0;
    TammanyChip object = null;
    String gameEvents[] = null;
    public String[] gameEvents() { return(gameEvents); }

    public TammanyMovespec()
    {
    } // default constructor

    /* constructor */
    public TammanyMovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    // constructor for simple moves
    public TammanyMovespec(int opc,int who)
    {
    	op = opc;
    	player = who;
    }
    // constructor for slander
    public TammanyMovespec(int opc,double weight,TammanyCell d,int who)
    {
    	op = opc;
       	montecarloWeight = weight;
       	from_col = to_col = d.col;
    	from_row = to_row = d.row;
    	source = dest = d.rackLocation();
    	player = who;
    	
    }
    // constructor for start election
    public TammanyMovespec(int opc,int ward,int who)
    {
    	op= opc;
    	to_row = ward;
    	player = who;
    }
    // constructor for move_from_to
    public TammanyMovespec(int opc,double weight,TammanyCell s,TammanyCell d,int who)
    {
    	op = opc;
    	player = who;
       	montecarloWeight = weight;
       	source = s.rackLocation();
    	from_col = s.col;
    	from_row = s.row;
    	dest = d.rackLocation();
    	to_col = d.col;
    	to_row = d.row;
    	from_cube = -1;
    }
    // constructor for move_cube
    public TammanyMovespec(int opc,double weight,TammanyCell s,int ind,TammanyCell d,int who)
    {	this(opc,weight,s,d,who);
    	from_cube = ind;
    }
    static int encodeVotes(int irish,int english,int german,int italian)
    {
    	return(irish|(english<<8)|(german<<16)|(italian<<24));
    }
    static int[] decodeVotes(int v)
    {	int[] vals = new int[4];
    	vals[0]=v&0xff;
    	vals[1]=(v>>8)&0xff;
    	vals[2]=(v>>16)&0xff;
    	vals[3]=(v>>24)&0xff;
    	return(vals);
    }
    static int encodeBoss(int ward, int votes)
    {
    	return((ward<<8) | votes);
    }
    static int[] decodeBoss(int v)
    {	int [] vals = new int[2];
    	vals[0] = (v>>8) & 0xff;
    	vals[1] = v & 0xff;
    	return(vals);
    }
    public TammanyMovespec(int opc,double weight,int ward,int boss,int votes_0,int votes_1, int votes_2,int votes_3,int pl)
    {	op = opc;
    	from_row = encodeBoss(ward,boss);
    	to_row = encodeVotes(votes_0,votes_1,votes_2,votes_3);
    	player = pl;
    	montecarloWeight = weight;
     }

    /* constructor */
    public TammanyMovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        TammanyMovespec other = (TammanyMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (to_row == other.to_row) 
				&& (from_row==other.from_row)
				&& (dest == other.dest)
				&& (from_col==other.from_col)
				&& (from_cube==other.from_cube)
				&& (player == other.player));
    }

    public void Copy_Slots(TammanyMovespec to)
    {	super.Copy_Slots(to);
        to.dest = dest;
        to.to_row = to_row;
        to.from_row = from_row;
        to.from_cube = from_cube;
        to.from_col = from_col;
        to.to_col = to_col;
        to.object = object;
        to.source = source;
    }

    public commonMove Copy(commonMove to)
    {
        TammanyMovespec yto = (to == null) ? new TammanyMovespec() : (TammanyMovespec) to;

        // we need yto to be a TammanyMovespec at compile time so it will trigger call to the 
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
        	throw G.Error("Can't parse %s", cmd);
       case MOVE_ELECT:
        	to_row = G.IntToken(msg);
        	break;
        case MOVE_CUBE:
        	source = TammanyId.get(msg.nextToken());
        	from_row =  G.IntToken(msg);
        	from_cube = G.IntToken(msg);
        	dest = TammanyId.get(msg.nextToken());
         	to_row =  G.IntToken(msg);
        	break;
        	
        case MOVE_SLANDER:
          	dest = source = TammanyId.get(msg.nextToken());
        	from_col = to_col = '@';
        	from_row = to_row = G.IntToken(msg);
        	break;
        	
        case MOVE_PICK_CUBE:
           	dest = source = TammanyId.get(msg.nextToken());
        	from_col = to_col = '@';
        	from_row = to_row = G.IntToken(msg);
        	from_cube = G.IntToken(msg);
        	break;
        	
        case MOVE_VOTE:
        	{ int ward = G.IntToken(msg);
        	  int irish = G.IntToken(msg);
        	  int english = G.IntToken(msg);
        	  int german = G.IntToken(msg);
        	  int italian = G.IntToken(msg);
        	  from_row = ward;
        	  to_row = encodeVotes(irish,english,german,italian);
        	}
        	break;
        case MOVE_FROM_TO:
        	source = TammanyId.get(msg.nextToken());
        	from_col = G.CharToken(msg);
        	from_row =  G.IntToken(msg);
        	dest = TammanyId.get(msg.nextToken());
        	to_col = G.CharToken(msg);
        	to_row =  G.IntToken(msg);
        	break;
        	
        case MOVE_DROPB:
        case MOVE_PICKB:
        	dest = source = TammanyId.get(msg.nextToken());
        	to_col = from_col = '@';
            to_row = from_row = G.IntToken(msg);
            from_cube = -1;
            break;

        case MOVE_DROP:
        case MOVE_PICK:
            dest = source = TammanyId.get(msg.nextToken());
            to_col = from_col = G.CharToken(msg);
            to_row = from_row = G.IntToken(msg);
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

        default:
 
            break;
        }
    }

    /** construct an abbreviated move string, mainly for use in the game log.  These
     * don't have to be parseable, they're intended only to help humans understand
     * the game record.
     * */
    public String shortMoveString()
    {
        switch (op)
        {
        case MOVE_VOTE:
        	{
        	int vals[] = decodeVotes(to_row);
        	int boss[] = decodeBoss(from_row);
        	return(D.findUnique(op) +" ward "+boss[0]+" "+boss[1]+" "+vals[0]+" "+vals[1]+" "+vals[2]+" "+vals[3]);
        	}
        case MOVE_ELECT:
        	return("Start election "+to_row);
         case MOVE_SLANDER:
         	return (D.findUnique(op) +" ward "+ to_row);
         	 
          	
         case MOVE_CUBE:
        	 if((source==TammanyId.WardCube)&&(dest==TammanyId.WardCube))
        	 {	setLineBreak(true);
        		 return("Move "
        				+ (object.isCube()?object.myCube.name():object.toString())
        				+ " "+from_row+" to "+to_row); 
        	 }
        	 else if((source==TammanyId.WardCube) && (dest==TammanyId.Bag))
        	 {
        		 setLineBreak(true);
        		 return("Remove "+(object.isCube()?object.myCube.name():object.toString())
        				 + " ward "+ from_row);
        	 }

			//$FALL-THROUGH$
		case MOVE_FROM_TO:
        	 switch(dest)
        	 {
        	 case Bag:
        	 case Trash:
        		 {String desc = object==null ? "" 
        				 		: (object.isCube() ? object.myCube.toString() 
        				 			: (object.isBoss() ? object.myBoss.toString()+" "+from_row
        				 				: (object.isInfluence() ? object.myInfluence.toString() : object.toString())));
        		 return( desc);
        		 }
        	 case PlayerInfluence:
        		 if(object!=null)
        		 {	setLineBreak(true);
         			 return("take "+object.myInfluence.toString()+" influence");
        		 }
        		 break;
        	 case ChiefOfPolice:
        	 case DeputyMayor:
        	 case PrecinctChairman:
        	 case CouncilPresident:
        		 setLineBreak(true);
        		 return((object!=null && object.isBoss()) ? object.myBoss.toString()+" is the new "+dest.prettyName(): "boss-"+dest);
        	 case WardBoss:
         		 return((object!=null && object.isBoss()) ? object.myBoss.toString()+" ward "+to_row : "boss-"+ to_row);
        	 case WardCube:
        		 setLineBreak(true);
        		 if(object==TammanyChip.freezer)
        		 {	
        		 	return("Freeze ward "+to_row);
        		 }
        		 else
        	 	{String ob = (object!=null) ? object.isCube()?object.myCube.name():object.toString() : "Cube";
        		 return(ob+" ward "+to_row);
        	 	}
         		 
        	 default: break;
        	 }
        	return (D.findUnique(op) +" " + source.shortName + " " + from_row+" "+dest.shortName + " " + to_row);
        	
         case MOVE_PICK_CUBE:
         case MOVE_PICKB:
        	switch(source)
        	{
        	case SlanderPlacement:
        		return("Slander ");
          	case WardBoss:
          		 if((object!=null)&&object.isBoss())
          		 {
          			 return(object.myBoss.toString()+" ");
          		 }
          		 break;
        	case WardCube:
          	case CastleGardens:
        		if((object!=null) && object.isCube())
        			{
        			return(object.myCube.name()+" ");
        			}
        		break;
        	case InfluencePlacement:
        		return(TammanyChip.ethnics[from_row].myCube.toString()+" ");
        	case BossPlacement:	
        		{	  
        		return( ((object!=null)&&object.isBoss()) ? object.myBoss.toString()+" ":"Boss "+from_row);
        		}
			default:
				break;
        	}
            return (source.shortName + " " + from_row);

		case MOVE_DROPB:
			switch(dest)
			{
			case Bag:
				setLineBreak(true);
				return("bag");
			case DeputyMayor:
			case CouncilPresident:
			case PrecinctChairman:
			case ChiefOfPolice:
        		setLineBreak(true);
				return(" is the new "+dest.prettyName() );
			case Trash:
				setLineBreak(true);
				return("trash");
			case WardCube:
				setLineBreak(true);
				//$FALL-THROUGH$
			case WardBoss:
				return("ward "+to_row);
			default:
				break;
			}
            return (dest.shortName + " " + to_row);

        case MOVE_DROP:
        case MOVE_PICK:
            return ( " "+source.shortName);

        case MOVE_DONE:
            return ("");

        default:
            return (D.findUnique(op));

        }
    }

    /** construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and only secondarily human readable */
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
        case MOVE_ELECT:
        	return(ind+D.findUnique(op)+" "+to_row);
        case MOVE_CUBE:
          	return (ind+D.findUnique(op) +" " + source.shortName + " " + from_row+" "+from_cube+" "+dest.shortName + " " + to_row);
 
        case MOVE_SLANDER:
        	return (ind+D.findUnique(op) +" " + dest.shortName + " " + to_row);
       	
        case MOVE_PICK_CUBE:
        	return (ind+D.findUnique(op) +" " + source.shortName + " " + from_row+" "+from_cube);
 
         case MOVE_VOTE:
           	{
            	int vals[] = decodeVotes(to_row);
            	return(ind+D.findUnique(op) +" "+from_row+" "+vals[0]+" "+vals[1]+" "+vals[2]+" "+vals[3]);
            	}

        case MOVE_FROM_TO:
        	return (ind+D.findUnique(op) +" " + source.shortName + " "+from_col+" " + from_row+" "+dest.shortName +" "+ to_col+" " + to_row);

        case MOVE_PICKB:
	        return (ind+D.findUnique(op) +" " + source.shortName + " " + from_row);

		case MOVE_DROPB:
	        return (ind+D.findUnique(op) + " "+dest.shortName + " " + to_row);

        case MOVE_DROP:
            return (ind+D.findUnique(op) + " "+dest.shortName +" "+to_col+" "+to_row);
        case MOVE_PICK:
            return (ind+D.findUnique(op) + " "+source.shortName+" "+from_col+" "+from_row);

        case MOVE_START:
            return (ind+"Start P" + player);

        default:
            return (ind+D.findUnique(op));
        }
    }

    /** standard java method, so we can read moves easily while debugging */
    public String toString()
    {	//return super.toString();
        return ("P" + player + "[" + moveString() + "]" + montecarloWeight +" "+uctNode());
    }
    
    boolean hasFutureNonVote(CommonMoveStack history, int idx)
    {
    	int siz = history.size();
    	while(++idx<siz)
    	{
    		commonMove m = history.elementAt(idx);
    		if(m.op!=MOVE_VOTE) {  return(true); }
    	}
    	return(false);
    }
    String censoredMoveString( CommonMoveStack history, int idx,TammanyBoard b)
    {	if((op == MOVE_VOTE)&& !hasFutureNonVote(history,idx))
    	{
    	return("");
    	}
    	else
    	{
    	// default treatment
    	return(shortMoveString());
    	}
    
    }
    
}
