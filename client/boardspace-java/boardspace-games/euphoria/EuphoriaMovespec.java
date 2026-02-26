/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.
    
    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/. 
 */
package euphoria;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;
import online.game.*;

import java.awt.Font;

import lib.ExtendedHashtable;

public class EuphoriaMovespec extends commonMPMove implements EuphoriaConstants
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_PLACE_WORKER = 208;
    static final int MOVE_RETRIEVE_WORKER = 209;
    static final int MOVE_CHOOSE_RECRUIT = 210;
    static final int MOVE_ITEM_TO_BOARD = 211;
    static final int MOVE_ITEM_TO_PLAYER = 212;
    static final int FIGHT_THE_OPRESSOR = 213;
    static final int JOIN_THE_ESTABLISHMENT = 214;
    static final int USE_RECRUIT_OPTION = 215;
    static final int USE_DIE_ROLL = 216;
    static final int DONT_USE_DIE_ROLL = 217;
    static final int EPHEMERAL_PICK = 218;					// allow ephemeral moves at startup
    static final int EPHEMERAL_DROP = 219;
    static final int EPHEMERAL_CHOOSE_RECRUIT = 220;
    static final int NORMALSTART = 221;
    static final int CONFIRM_RECRUITS = 223;
    static final int EPHEMERAL_CONFIRM_RECRUITS = 224;
    static final int EPHEMERAL_CONFIRM_ONE_RECRUIT = 225;
    static final int MOVE_MOVE_WORKER = 226;			// move worker board-to-board
    static final int MOVE_ITEM = 227;					// move some item board-to-bopard
    static final int USE_SECOND_RECRUIT_OPTION = 228;	// only for julia the acolyte
    static final int USE_FIRST_RECRUIT_OPTION = 229;	// only for julia the acolyte
    static final int EPHEMERAL_CONFIRM_DISCARD = 230;	// confirm discarding recruits
    static final int CONFIRM_DISCARD = 231;				
    static final int MOVE_SACRIFICE = 232;	// sacrifice a worker
    static final int MOVE_RECRUIT = 233;	// add a random new recruit (for testing)
    static final int MOVE_LOSEMORALE = 234;	// lose 1 morale to allow placing a second worker
    static final int MOVE_MARKET = 235;
    static final int MOVE_PEEK = 236;
    static final int EPHEMERAL_CHOOSE_RECRUITS = 237;
    
    /* this is used by the move filter to select ephemeral moves */
    public boolean isEphemeral()
	{
		switch(op)
		{
		case EPHEMERAL_PICK:
		case EPHEMERAL_DROP:
		case EPHEMERAL_CHOOSE_RECRUIT:
		case EPHEMERAL_CONFIRM_RECRUITS:
		case EPHEMERAL_CONFIRM_ONE_RECRUIT:
		case EPHEMERAL_CONFIRM_DISCARD:
		case EPHEMERAL_CHOOSE_RECRUITS:
			return(true);
		default: return(false);
		}
	}
    /* this is used by the move filter to select ephemeral moves */
    public boolean isTransmitted()
	{
		switch(op)
		{
		case EPHEMERAL_PICK:
		case EPHEMERAL_CHOOSE_RECRUITS:
		case EPHEMERAL_DROP: return(false);
		default: return(true);
		}
	}
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,	// this adds "start" "done" "edit" and so on.
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"EPick",EPHEMERAL_PICK,
        	"EDrop",EPHEMERAL_DROP,
        	"EChoose",EPHEMERAL_CHOOSE_RECRUIT,
        	"Peek", MOVE_PEEK,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
        	"Place",MOVE_PLACE_WORKER,
        	"Retrieve",MOVE_RETRIEVE_WORKER,
        	"Choose",MOVE_CHOOSE_RECRUIT,
        	"ToBoard",MOVE_ITEM_TO_BOARD,
        	"ToPlayer",MOVE_ITEM_TO_PLAYER,
        	"Roll",USE_DIE_ROLL,
        	"NoRoll",DONT_USE_DIE_ROLL,
        	"NormalStart",NORMALSTART,
        	"Relocate",MOVE_MOVE_WORKER,
        	"MoveItem",MOVE_ITEM,
        	"Sacrifice",MOVE_SACRIFICE,
        	"newrecruit",MOVE_RECRUIT,
        	"newmarket",MOVE_MARKET,
        	"losemorale",MOVE_LOSEMORALE,
        	"echooseRecruits",EPHEMERAL_CHOOSE_RECRUITS,
           	EuphoriaId.ConfirmDiscard.name(),CONFIRM_DISCARD,
           	EuphoriaId.EConfirmDiscard.name(),EPHEMERAL_CONFIRM_DISCARD,
        	EuphoriaId.RecruitOption.name(),USE_RECRUIT_OPTION,
        	EuphoriaId.RecruitFirstJuliaOption.name(),USE_FIRST_RECRUIT_OPTION,
        	EuphoriaId.RecruitSecondJuliaOption.name(),USE_SECOND_RECRUIT_OPTION,
        	EuphoriaId.FightTheOpressor.name(),FIGHT_THE_OPRESSOR,
        	EuphoriaId.JoinTheEstablishment.name(),JOIN_THE_ESTABLISHMENT,
        	EuphoriaId.ConfirmRecruits.name(),CONFIRM_RECRUITS,
        	EuphoriaId.EConfirmRecruits.name(),EPHEMERAL_CONFIRM_RECRUITS,
        	EuphoriaId.EConfirmRecruits.name(),EPHEMERAL_CONFIRM_RECRUITS,
        	EuphoriaId.EConfirmOneRecruit.name(),EPHEMERAL_CONFIRM_ONE_RECRUIT);
          }

    //
    // variables to identify the move
    EuphoriaId source;
    EuphoriaId dest;
    Colors from_color;
    Colors to_color;
    int from_row;
    int to_row;
    EuphoriaChip chip;
    EuphoriaChip chipIn;
    boolean followedByDone = false;
    double montecarloWeight = 1.0;
    public String []gameEvents=null;
    public String[] gameEvents() { return(gameEvents); }

    public EuphoriaMovespec()
    {
    } // default constructor

    /* constructor */
    public EuphoriaMovespec(String str, int p)
    {
        parse(str, p);
    }
    public EuphoriaMovespec(int opc,EPlayer p)
    {
    	op = opc;
    	from_color = to_color = p.color;
     	player = p.boardIndex;
    }
    public EuphoriaMovespec(int opc,int p)
    {
    	op = opc;
     	player = p;
    }
    // constructor for selected die rolls.
    public EuphoriaMovespec(int opc,EuphoriaId id,EPlayer p)
    {
    	op = opc;
    	from_color = to_color = p.color;
    	player = p.boardIndex;
    	source = id;
    }
    // constructor for recruitoption
    public EuphoriaMovespec(int opc,EuphoriaChip ch,EPlayer p)
    {
    	op = opc;
    	from_color = to_color = p.color;
    	player = p.boardIndex;
    	chip = ch;
    }
    public EuphoriaMovespec(int opc,Colors c,int pl)
    {	op = opc;
    	from_color = c;
    	to_color = c;
    	player = pl;
     }
    /* contstructor for place workers */
    public EuphoriaMovespec(EPlayer p,EuphoriaCell src,int idx,EuphoriaCell d)
    {
    	op = MOVE_PLACE_WORKER;
    	source = src.rackLocation();
    	from_color = p.color;
    	from_row = idx;
    	dest = d.rackLocation();
    	to_row = d.row;
    	player = p.boardIndex;
    	
    }
    /* constructor for retrieve worker */
    public EuphoriaMovespec(EuphoriaCell s ,EuphoriaCell d,WorkerChip worker,int pl)
    {
    	op = MOVE_RETRIEVE_WORKER;
    	source = s.rackLocation();
    	from_row = s.row;
    	dest = d.rackLocation();
    	to_color = worker.color;
    	player = pl;
    }  
    
    /*  sacrifice a worker */
    public EuphoriaMovespec(int o,EPlayer p,EuphoriaCell s,int idx)
    {
    	op = o;
    	from_color = p.color;
    	to_color = p.color;
    	source = s.rackLocation();
    	from_row = idx;
    	player = p.boardIndex;
    }
    
    /*  move item to board and choose_recruit */
    public EuphoriaMovespec(int o,EPlayer p,EuphoriaCell s,EuphoriaCell d)
    {
    	op = o;
    	from_color = p.color;
    	to_color = p.color;
    	source = s.rackLocation();
    	from_row = s.row;
    	dest = d.rackLocation();
    	to_row = d.row;
    	player = p.boardIndex;
    }
    /*  move item to board */
    public EuphoriaMovespec(int o,EPlayer p,EuphoriaCell s,int idx,EuphoriaCell d)
    {
    	op = o;
    	from_color = p.color;
    	from_row = idx;
    	to_color = p.color;
    	source = s.rackLocation();
    	dest = d.rackLocation();
    	to_row = d.row;
    	player = p.boardIndex;
    }
    
    
    /*  move item to player */
    public EuphoriaMovespec(int o,EuphoriaCell s,int i,EPlayer p,EuphoriaCell d)
    {
    	op = o;
    	source = s.rackLocation();
    	from_row = i;
    	dest = d.rackLocation();
    	to_color = p.color;
    	player = p.boardIndex;
    }    
    
    /*  move item to player */
    public EuphoriaMovespec(int o,EuphoriaCell s,EPlayer p,EuphoriaCell d)
    {
    	op = o;
    	source = s.rackLocation();
    	from_row = s.row;
    	dest = d.rackLocation();
    	to_color = p.color;
    	player = p.boardIndex;
    }
    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        EuphoriaMovespec other = (EuphoriaMovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (dest == other.dest)
				&& (to_row == other.to_row) 
				&& (from_row == other.from_row)
				&& (to_color == other.to_color)
				&& (player == other.player));
    }

    public void Copy_Slots(EuphoriaMovespec to)
    {	super.Copy_Slots(to);
    	to.source = source;
    	to.dest = dest;
    	to.to_row = to_row;
    	to.followedByDone = followedByDone;
    	to.from_row = from_row;
    	to.from_color = from_color;
    	to.chip = chip;
    	to.to_color = to_color;
    	to.chipIn = chipIn;
        to.gameEvents = gameEvents;
       if( (op==USE_RECRUIT_OPTION) && (chip==null)) { G.Error("Missing recruit") ; }
    }

    public commonMove Copy(commonMove to)
    {
        EuphoriaMovespec yto = (to == null) ? new EuphoriaMovespec() : (EuphoriaMovespec) to;

        // we need yto to be a EuphoriaMovespec at compile time so it will trigger call to the 
        // local version of Copy_Slots
        Copy_Slots(yto);

        return (yto);
    }
    private void parseExtra(Tokenizer msg)
    {
   		if(msg.hasMoreElements())
		{
			String card = msg.nextElement();	
			chipIn = chip = ArtifactChip.find(card);
		}
    }

    /* parse a string into the state of this move.  Remember that we're just parsing, we can't
     * refer to the state of the board or the game.  This parser follows the recommended practice
     * of keeping it very simple.  A move spec is just a sequence of tokens parsed by calling
     * nextToken
     * @param msg a string tokenizer containing the move spec
     * @param the player index for whom the move will be.
     * */
    private void parse(String omsg, int p)
    {	Tokenizer msg = new Tokenizer(omsg);
    	String cmd = firstAfterIndex(msg);
    	player = p;
        // look for card annotations
        {
        	int i1 = omsg.indexOf('"');
        	int i2 = omsg.lastIndexOf('"');
         	if(i1>0 && i2>i1)
        		{
        		chip = EuphoriaChip.find(omsg.substring(i1+1,i2));
        		}
        }
        op = D.getInt(cmd, MOVE_UNKNOWN);
        switch (op)
        {
        case MOVE_MARKET:
        	dest = EuphoriaId.get(msg.nextElement());
        	to_row = msg.intToken();
        	parseExtra(msg);
        	break;
        case MOVE_UNKNOWN:
        	throw G.Error("Can't parse %s", cmd);
        case USE_DIE_ROLL:
        	source = EuphoriaId.get(msg.nextElement());
        	break;
        case DONT_USE_DIE_ROLL:
        	break;
        
        case MOVE_SACRIFICE:
       		from_color = Colors.get(msg.nextElement());
    		source = EuphoriaId.get(msg.nextElement());
    		from_row = msg.intToken();
           	break;
           	
        case MOVE_ITEM:
        case MOVE_MOVE_WORKER:
           	source = EuphoriaId.get(msg.nextElement());
           	from_row = msg.intToken();
           	dest = EuphoriaId.get(msg.nextElement());
           	to_row = msg.intToken();
           	break;
           	
        case MOVE_RETRIEVE_WORKER:
        	source = EuphoriaId.get(msg.nextElement());
    		from_row = msg.intToken();
    		to_color = Colors.get(msg.nextElement());
    		dest = EuphoriaId.get(msg.nextElement());
         	break;
        case USE_RECRUIT_OPTION:
        case USE_SECOND_RECRUIT_OPTION:
        	break;
        case MOVE_ITEM_TO_BOARD:
       		to_color = from_color = Colors.get(msg.nextElement());
    		source = EuphoriaId.get(msg.nextElement());
    		from_row = msg.intToken();
    		dest = EuphoriaId.get(msg.nextElement());
    		to_row = msg.intToken();
    		break;
    		
        case MOVE_ITEM_TO_PLAYER:
    		source = EuphoriaId.get(msg.nextElement());
    		from_row = msg.intToken();
    		to_color = Colors.get(msg.nextElement());
    		dest = EuphoriaId.get(msg.nextElement());
    		parseExtra(msg);
 
    		break;

        case EPHEMERAL_CHOOSE_RECRUIT:
        case MOVE_CHOOSE_RECRUIT:
        		to_color = from_color = Colors.get(msg.nextElement());
        		source = EuphoriaId.get(msg.nextElement());
        		dest = EuphoriaId.get(msg.nextElement());
        		player = p;
        		parseExtra(msg);
        		break;
        case MOVE_PLACE_WORKER:
        		from_color = Colors.get(msg.nextElement());
        		source = EuphoriaId.get(msg.nextElement());
        		from_row = msg.intToken();
        		dest = EuphoriaId.get(msg.nextElement());
        		to_row = msg.intToken();
        		break;
        case MOVE_DROPB:
				dest = EuphoriaId.get(msg.nextElement());	// B or W
	            to_row = msg.intToken();
	            break;

		case MOVE_PICKB:
			source = EuphoriaId.get(msg.nextElement());
            from_row = msg.intToken();

            break;
		case CONFIRM_DISCARD:
		case EPHEMERAL_CONFIRM_DISCARD:
          	from_color = Colors.get(msg.nextElement());		
			break;
		case EPHEMERAL_DROP:
        case MOVE_DROP:
          	to_color = Colors.get(msg.nextElement());
            dest = EuphoriaId.get(msg.nextElement());
            break;
            
        case EPHEMERAL_PICK:
        case MOVE_PICK:
        	from_color  = Colors.get(msg.nextElement());
            source  = EuphoriaId.get(msg.nextElement());
            from_row = msg.intToken();
            break;

        case MOVE_START:
            player = D.getInt(msg.nextElement());

            break;
        case CONFIRM_RECRUITS:
        case EPHEMERAL_CHOOSE_RECRUITS:
        case EPHEMERAL_CONFIRM_RECRUITS:
        case EPHEMERAL_CONFIRM_ONE_RECRUIT:
        	from_color = Colors.get(msg.nextElement());
        	player = p;
        	break;
        case MOVE_DONE:
        	parseExtra(msg);
        	break;
        
        case MOVE_RECRUIT:
        	{
        	to_row = msg.intToken();
        	parseExtra(msg);
        	}
        	break;
        default:
            break;
        }
    }
    Text censoredMoveText(EuphoriaViewer v,EuphoriaMovespec state[],EuphoriaBoard b,Font f)
    {	
    	// default treatment
    	return(shortMoveText(v,f));
    }

    static double diceScale[]= {1.0,1.4,0.1,-0.5};
    
    double [] scale(EuphoriaChip ch)
    {
    	if(ch==null)
    		{ return(diceScale); }
       	if(ch==EuphoriaChip.Energy) { return(new double[]{1.0,0.9,0,-0.3}); }
       	if(ch==EuphoriaChip.Gold) { return(new double[]{1.0,1.4,0.0,-0.2}); }
      	if(ch==EuphoriaChip.Stone) { return(new double[] { 1.0,1.6,0.4,-0.2} ); }
      	if(ch==EuphoriaChip.Food) { return(new double[] { 1.0,1.6,0.0,-0.4} ); }
      	if(ch==EuphoriaChip.Clay) { return(new double[] { 1.0,1.4,0.0,-0.2} ); }
      	if(ch==EuphoriaChip.Water) { return(new double[] { 1.0,1.4,-0.1,-0.4} ); }
    	if(ch.isArtifact())	{ return new double[]{1.0,0.45,0,-0.25}; }
    	return diceScale;
    	
    }
    private EuphoriaChip cardBack(EuphoriaChip chip)
    {	if(chip==null) { return(null); }
    	return(
    	chip.isRecruit()
    		?RecruitChip.CardBack
    		:chip.isArtifact()
    			?ArtifactChip.CardBack
    			:chip);
    }
    
   
    /** construct an abbreviated move string, mainly for use in the game log.  These
     * don't have to be parseable, they're intended only to help humans understand
     * the game record.
     * */
    public Text shortMoveText(commonCanvas v,Font f)
    {
        switch (op)
        {
        case MOVE_RECRUIT:
        	return TextChunk.create("New "+to_row+" "+((chip==null)?"":chip.getName()));
        	
        case MOVE_PICKB:
        	if(chip.isWorker())
        	{
        		return(TextChunk.join(
        				TextGlyph.create("xx",chip,v,scale(chip)),
        				TextChunk.create(" from "+source.prettyName)));
        	}
        	return (TextChunk.create(""));
		case MOVE_DROPB:
            return (dest.isWorkerCell
            		? TextChunk.create(dest.prettyName)
            		: TextChunk.join(
            				TextChunk.create(" -1 "),
            				TextGlyph.create("xx",chip,v,scale(chip))));

		case EPHEMERAL_DROP:
        case MOVE_DROP:
        	if(dest==EuphoriaId.PlayerNewWorker) { return(TextChunk.create("")); }
        	return(TextChunk.join(
        			TextChunk.create(" +1 "),
        			TextGlyph.create("xx",
        					cardBack(chip),v,scale(chip))));
        case EPHEMERAL_PICK:
        case MOVE_PICK:
        	if(chip!=null && chip.isWorker()) 
        		{ return(TextChunk.join(
        				TextGlyph.create("xx",chip,v,scale(chip)),
        				TextChunk.create("  to ")));
        		}
        	else { return (TextChunk.create("")); }

        case MOVE_PEEK:
        case USE_DIE_ROLL:
        case MOVE_DONE:
        	return (TextChunk.create(""));
            
        case MOVE_ITEM_TO_PLAYER:
        	return(TextChunk.join(
        			TextChunk.create(" +1 "),
        			TextGlyph.create("xx",cardBack(chip),v,scale(chip))));
        	
        case MOVE_ITEM_TO_BOARD:
        	return(TextChunk.join(
        			TextChunk.create(" -1 "),
        			TextGlyph.create("xx",chip,v,scale(chip))));
        case EPHEMERAL_CHOOSE_RECRUIT:
        case MOVE_CHOOSE_RECRUIT:
        	return(TextChunk.join(
        			TextChunk.create(" +1 "),
        			TextGlyph.create("xx",cardBack(chip),v,scale(chip))));
        default:
            return (TextChunk.create(D.findUniqueTrans(op)));
        case MOVE_PLACE_WORKER:
        	return(TextChunk.join(
        			TextGlyph.create("xx",chip,v,scale(chip)),
        			TextChunk.create(" to "+dest.prettyName)));
        case MOVE_RETRIEVE_WORKER:
        	return(TextChunk.join(
           			TextGlyph.create("xx",chip,v,scale(chip)),
         			TextChunk.create(" from "+ source.prettyName)));

        case MOVE_SACRIFICE:
        	return TextChunk.join(
           			TextGlyph.create("xx",chip,v,scale(chip)),
         			TextChunk.create(" from "+ source.prettyName));
        	
        case MOVE_MOVE_WORKER:
        case MOVE_ITEM:
        	return TextChunk.join(
           			TextGlyph.create("xx",chip,v,scale(chip)),
         			TextChunk.create(" from "+ source.prettyName),
         			TextGlyph.create("xx",chip,v,scale(chip)),
        			TextChunk.create(" to "+dest.prettyName));
        case MOVE_MARKET:
        	return(TextChunk.create(dest.name()+" "+to_row+" "+chip.name));
        	
        case USE_SECOND_RECRUIT_OPTION:
        case USE_RECRUIT_OPTION:
        	return(TextChunk.create("Use "+((chip==null)?"Recruit":((RecruitChip)chip).name)));
        			
        case FIGHT_THE_OPRESSOR:	return(TextChunk.create("Fight the Opressor"));
        case JOIN_THE_ESTABLISHMENT:	return(TextChunk.create("Join the Establishment"));
        case NORMALSTART:
        case CONFIRM_RECRUITS:
        case EPHEMERAL_CONFIRM_RECRUITS:
        case EPHEMERAL_CHOOSE_RECRUITS:
        case EPHEMERAL_CONFIRM_ONE_RECRUIT:
        case DONT_USE_DIE_ROLL:	return(TextChunk.create(""));

      	
        }
    }
    /** construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and only secondarily human readable */
    public String moveString()
    {
        String main = mainMoveString();
        String card = cardMoveString();
        return(main+card);
    }
    String cardMoveString()
    {
    	return((chip==null)?"":(" \""+chip.name+"\""));
    }
        // adding the move index as a prefix provides numbers
        // for the game record and also helps navigate in joint
        // review mode
        
        
        public String mainMoveString()
        {
        String ind = indexString();
        String opname = ind+D.findUnique(op)+" ";
        switch (op)
        {
        case CONFIRM_DISCARD:
        case EPHEMERAL_CONFIRM_DISCARD:
        	return G.concat(opname,from_color.name());
        	
        case MOVE_ITEM_TO_BOARD:
        	return G.concat(opname,from_color.name()," ",source.name()," ",from_row," ",dest.name()," ",to_row);
        case MOVE_ITEM_TO_PLAYER:
        	return(opname+source.name()+" "+from_row+" "+to_color.name()+ " "+dest.name());
         	
        case MOVE_CHOOSE_RECRUIT:
        case EPHEMERAL_CHOOSE_RECRUIT:
        	return(opname+from_color.name()+" "+source.name()+" "+dest.name());
        	
        case MOVE_PICKB:
	        return (opname + source.name() + " " + from_row);

		case MOVE_DROPB:
	        return (opname + dest.name()+" " + to_row);

        case MOVE_DROP:
        case EPHEMERAL_DROP:
        	return(opname +to_color.name() +" "+ dest.name());
        case MOVE_PICK:
        case EPHEMERAL_PICK:
            return (opname + from_color.name()+" "+source.name()+" "+from_row);
        case MOVE_MARKET:
        	return(G.concat(opname,dest.name()," ",to_row));
        	
        case MOVE_START:
            return ("Start P" + player);
        case MOVE_RETRIEVE_WORKER:
        	return(opname+source.name()+" "+from_row+" "+to_color.name()+" "+dest.name());
        case MOVE_PLACE_WORKER:
        	return(opname+from_color.name()+" "+source.name()+" "+from_row+" "+dest.name()+" "+to_row);
       
        case MOVE_SACRIFICE:
           	return G.concat(opname,from_color.name()," ",source.name()," ",from_row);
        	
        case MOVE_MOVE_WORKER:
        case MOVE_ITEM:
        	return G.concat(opname,source.name()," ",from_row," ",dest.name()," ",to_row);
        	
        case USE_DIE_ROLL:
        	return(opname+source.name());
        case CONFIRM_RECRUITS:
        case EPHEMERAL_CONFIRM_RECRUITS:
        case EPHEMERAL_CHOOSE_RECRUITS:
        case EPHEMERAL_CONFIRM_ONE_RECRUIT:
           	return(opname+from_color.name());
        case MOVE_RECRUIT:
        	return G.concat(opname,to_row);
        default:
             return (opname);
        }
    }

       public void changeToASynchronous()
       {	switch(op)
    	   	{
    	   	case MOVE_PICK:  op = EPHEMERAL_PICK; break;
    	   	case MOVE_DROP:  op = EPHEMERAL_DROP; break;
    	   	case MOVE_CHOOSE_RECRUIT: op = EPHEMERAL_CHOOSE_RECRUIT;
    	   		break;
    	   	default: break;
    	   	}
       }
       public void changeToSynchronous()
       {   switch(op)
    	   {
    	   case EPHEMERAL_DROP: op = MOVE_DROP; break;
    	   case EPHEMERAL_PICK: op = MOVE_PICK; break;
    	   case EPHEMERAL_CHOOSE_RECRUIT: op = MOVE_CHOOSE_RECRUIT; break;
    	   
    	   default: break;
    	   }
       }

}
