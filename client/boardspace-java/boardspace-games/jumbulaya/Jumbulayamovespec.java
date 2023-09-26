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
package jumbulaya;

import java.util.*;

import jumbulaya.JumbulayaConstants.JumbulayaId;
import jumbulaya.JumbulayaConstants.Option;
import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import online.game.*;
import lib.ExtendedHashtable;

public class Jumbulayamovespec extends commonMPMove
{	// this is the dictionary of move names
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int MOVE_SETOPTION = 209;	// set option
    static final int MOVE_SHOW = 210;		// show tiles for a player
    static final int MOVE_PLAYWORD = 211;	// play a word from the rack
    static final int MOVE_SEE = 212;		// see tiles on the hidden rack
    static final int MOVE_LIFT = 213; 		// lift a tile in the user interface rack
    static final int MOVE_REPLACE = 214; 	// replace a tile in the user interface rack
    static final int MOVE_MOVETILE = 215;		// move a tile from a rack to the board or back
    static final int MOVE_DROPFROMBOARD = 216;	// drop a tile that was previously on the board
    static final int MOVE_JUMBULAYA = 217;		// play a jumbulaya
    static final int MOVE_STARTJUMBULAYA = 218;	// start playing a jumbulaya
    static final int MOVE_SELECT = 219;			// select a cell
    static final int MOVE_DISCARDRACK = 220;		// discard all tiles
    static final int MOVE_STOPJUMBULAYA = 221;		// give up on a jumbulaya
    static final int MOVE_REMOTELIFT = 222;			// lift of a remote tile
    static final int MOVE_REMOTEDROP = 223;			// drop of a remote tile
    static final int MOVE_CANCELLED = 224;			// was a droponboard move, now cancelled
    static final int MOVE_DROPONRACK = 225;
    static
    {	// load the dictionary
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
    		"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
        	"SetOption",MOVE_SETOPTION,
        	"Show", MOVE_SHOW,
        	"Play",MOVE_PLAYWORD,
        	"See", MOVE_SEE,
        	"Lift", MOVE_LIFT,
        	"Replace",MOVE_REPLACE,
        	"move",MOVE_MOVETILE,
        	"dropFromBoard", MOVE_DROPFROMBOARD,
        	"jumbulaya", MOVE_JUMBULAYA,
        	"startjumbulaya", MOVE_STARTJUMBULAYA,
        	"select",MOVE_SELECT,
        	"discardrack",MOVE_DISCARDRACK,
        	"stopjumbulaya", MOVE_STOPJUMBULAYA,
        	"Rlift", MOVE_REMOTELIFT,
        	"cancelled",MOVE_CANCELLED,
        	"droponrack",MOVE_DROPONRACK,
        	"remotedrop", MOVE_REMOTEDROP);
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
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    int mapped_row = -1;
    JumbulayaId dest;	
    JumbulayaId source; // only for move tile
    char from_col;	// only for move tile
    int from_row;	// only for move tile
    JumbulayaChip chip;
    String word = null;
    CellStack path = null;
    String gameEvents[] = null;
    public String[] gameEvents() { return(gameEvents); }
    
    public Jumbulayamovespec()
    {
    } // default constructor

    /* constructor */
    public Jumbulayamovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }
    
    // constructor for step moves
    public Jumbulayamovespec(int opc,JumbulayaId f,char fc,int fr,
    		JumbulayaId t,char tc,int tr,int who)
    {
    	op = opc;
    	source = f;
    	from_col = fc;
    	from_row = fr;
    	dest = t;
    	to_col = tc;
    	to_row = tr;
    	mapped_row = -1;
    	player = who;
    }
    
    // constructor for robot moves
    public Jumbulayamovespec(Word w,int who)
    {
    	word = w.name;
    	player = who;
    	if(w.isJumbulaya())
    	{
    		op = MOVE_JUMBULAYA;
    		path = w.getPath();  		
    	}
    	else {
        	op = MOVE_PLAYWORD;
        	to_col = w.seed.col;
        	to_row = w.seed.row;   	
        	mapped_row = -1;
    	}
    	
    }
    
    
    /** constructor for simple robot moves - pass and done 
     */
    public Jumbulayamovespec(int opc,int who)
    {
    	op = opc;
    	player = who;
    }

    
    /** constructor for robot moves.  Having this "binary" constor is dramatically faster
     * than the standard constructor which parses strings
     */
    public Jumbulayamovespec(int opc,char col,int row,JumbulayaId what,int who)
    {
    	op = opc;
    	dest = what;
    	to_col = col;
    	to_row = row;
    	mapped_row = -1;
    	player = who;
    }
    /* constructor */
    public Jumbulayamovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    /**
     * This is used to check for equivalent moves "as specified" not "as executed", so
     * it should only compare those elements that are specified when the move is created. 
     */
    public boolean Same_Move_P(commonMove oth)
    {
        Jumbulayamovespec other = (Jumbulayamovespec) oth;

        return ((op == other.op) 
				&& (dest == other.dest)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (player == other.player)
        		&& (source == other.source)
        		&& (from_col == other.from_col)
        		&& (from_row == other.from_row)
        		&& (mapped_row == other.mapped_row)
        		&& (path == other.path)
        		&& (word==null ? other.word==null : word.equals(other.word)));
    }

    public void Copy_Slots(Jumbulayamovespec to)
    {	super.Copy_Slots(to);
        to.to_col = to_col;
        to.to_row = to_row;
        to.dest = dest;
        to.chip = chip;
        to.word = word;
        to.source = source;
        to.to_col = to_col;
        to.to_row = to_row;
        to.mapped_row = mapped_row;
        to.path = path;
    }

    public commonMove Copy(commonMove to)
    {
        Jumbulayamovespec yto = (to == null) ? new Jumbulayamovespec() : (Jumbulayamovespec) to;

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
        case MOVE_PLAYWORD:
           	to_col = G.CharToken(msg);
        	to_row = G.IntToken(msg);
        	word = msg.nextToken();
        	break;
        case MOVE_JUMBULAYA:
        	{
        		word = msg.nextToken();
        		CellStack pp = new CellStack();
        		while(msg.hasMoreTokens())
        		{
        			to_col = G.CharToken(msg);
        			to_row = G.IntToken(msg);
        			pp.push(new JumbulayaCell(to_col,to_row));       			
        		}
        		path = pp;
        	}
        	break;
        case MOVE_STARTJUMBULAYA:
        	if(msg.hasMoreTokens())
        	{
        		String color = msg.nextToken();
        		word = color;
        	}
        	break;
        case MOVE_SELECT:
        case MOVE_DROPB:
        	dest = JumbulayaId.BoardLocation;
        	to_col = G.CharToken(msg);
        	to_row = G.IntToken(msg);
         	break;
        case MOVE_MOVETILE:
        	source = JumbulayaId.valueOf(msg.nextToken());
        	from_col = G.CharToken(msg);
        	from_row = G.IntToken(msg);
        	dest = JumbulayaId.valueOf(msg.nextToken());
        	to_col = G.CharToken(msg);
        	to_row = G.IntToken(msg);
        	break;
        
      
		case MOVE_PICKB:
            dest = JumbulayaId.BoardLocation;
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);
            mapped_row = -1;
            break;
			
		case MOVE_LIFT:
		case MOVE_REMOTELIFT:
        case MOVE_PICK:
            dest = JumbulayaId.valueOf(msg.nextToken());
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);        
            mapped_row = (msg.hasMoreTokens()) ? G.IntToken(msg) : -1;
			break;
			
		case MOVE_REPLACE:
		case MOVE_REMOTEDROP:
        case MOVE_DROP:
            dest = JumbulayaId.valueOf(msg.nextToken());
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);        
            mapped_row = -1;
            break;
        case MOVE_DROPONRACK:
        case MOVE_DROPFROMBOARD:
            to_col = G.CharToken(msg);
            to_row = G.IntToken(msg);
            from_row = G.IntToken(msg);
            break;
        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;
        case MOVE_SHOW:
        case MOVE_SEE:
        	{
        	char pl = G.CharToken(msg);
        	boolean v = G.BoolToken(msg);
        	to_col = pl;
        	to_row = v ? 1 : 0;
        	}
        	break;
        case MOVE_SETOPTION:
        	{
        	Option o = Option.valueOf(msg.nextToken());
        	boolean v = G.BoolToken(msg);
        	to_row = o.ordinal()*2|(v?1:0);
        	}
        	break;
        case MOVE_CANCELLED:
        default:

            break;
        }
    }

    private Text icon(commonCanvas v,Object... msg)
    {	double chipScale[] = {1.2,1.2,-0.2,-0.5};
    	Text m = TextChunk.create(G.concat(msg));
    	if(chip!=null)
    	{
    		m = TextChunk.join(TextGlyph.create("xx", chip, v,chipScale),
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
        case MOVE_PLAYWORD:
        	return TextChunk.create("");
        case MOVE_PICKB:
            return icon(v,to_col , to_row);

        case MOVE_MOVETILE:
        	if(dest==JumbulayaId.DrawPile) 
        	{
        		return(icon(v,"^ "));
        	}
			//$FALL-THROUGH$
		case MOVE_SELECT:
        case MOVE_DROPB:
            return icon(v,to_col ,to_row);
        case MOVE_JUMBULAYA:
        	return TextChunk.create("Jumbulaya "+word);      			
        case MOVE_STARTJUMBULAYA:
        	return TextChunk.create("Start Jumbulaya");
        case MOVE_STOPJUMBULAYA:
        	return TextChunk.create("Stop Jumbulaya");
        case MOVE_LIFT:
        case MOVE_REMOTELIFT:
		case MOVE_REPLACE:
        case MOVE_DROP:
        case MOVE_DROPFROMBOARD:
        case MOVE_REMOTEDROP:
        case MOVE_SHOW:
        case MOVE_SEE:
        case MOVE_PICK:
        case MOVE_CANCELLED:
        case MOVE_DONE:
        case MOVE_DROPONRACK:
            return TextChunk.create("");
        case MOVE_DISCARDRACK:
        	return TextChunk.create("Discard Rack");
        	
        case MOVE_SETOPTION:
        	return TextChunk.create("Option "+Option.getOrd(to_row/2)+(((to_row&1)==0)?" false" : " true"));
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
        case MOVE_PICKB:
		case MOVE_DROPB:
		case MOVE_SELECT:
	        return G.concat(opname, to_col," ",to_row);
		case MOVE_MOVETILE:
	        return (G.concat(opname,
	        		source.name()," ", from_col," ", from_row,
	        		" ",dest.name()," ", to_col," ", to_row));

		case MOVE_JUMBULAYA:
			{
	      	StringBuilder b = new StringBuilder();
	      	b.append(indx);
	      	b.append("Jumbulaya ");
	      	b.append(word);
	      	b.append(" ");
           	for(int i=0;i<path.size();i++)
        	{
        		JumbulayaCell c = path.elementAt(i);
        		b.append(c.col);
        		b.append(" ");
        		b.append(c.row);
        		b.append(" ");
        	}
          	return(b.toString());
			}
		case MOVE_DROPONRACK:
		case MOVE_DROPFROMBOARD:
	           return G.concat(opname,to_col," ",to_row," ",from_row);
	           
		case MOVE_LIFT:
		case MOVE_REMOTELIFT:
        case MOVE_PICK:
        	 return G.concat(opname,dest.name()," ",to_col," ",to_row," ",mapped_row);
        	 
		case MOVE_REPLACE:
        case MOVE_DROP:
        case MOVE_REMOTEDROP:
            return G.concat(opname,dest.name()," ",to_col," ",to_row);
            
        case MOVE_PLAYWORD:
        	return G.concat(opname ,to_col," ",to_row," ",word);
        case MOVE_START:
            return G.concat(indx,"Start P",player);
        case MOVE_SEE:
        case MOVE_SHOW:
        	return G.concat(opname,to_col,((to_row==0)?" false" : " true"));
        	
        case MOVE_SETOPTION:
        	return G.concat(opname,Option.getOrd(to_row/2),(((to_row&1)==0)?" false" : " true"));
        	
        case MOVE_STARTJUMBULAYA:
        	return(G.concat(opname,word==null?"":" "+word));
        	
        case MOVE_STOPJUMBULAYA:
        case MOVE_CANCELLED:
        default:
            return G.concat(opname);
        }
    }

}
