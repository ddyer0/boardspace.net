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
package truchet;

import online.game.*;

import lib.G;
import lib.Graphics;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;
import lib.exCanvas;
import lib.Drawable;
import lib.ExtendedHashtable;


class ArrowIcon implements Drawable
{	static int NominalSize = 100;
	String specs = "";
	boolean merge = false;
	static int offsets[] = { 0,2,-2};
	private int offc(int n) { return offsets[Math.min(offsets.length-1,n)]; }
	static ArrowIcon Left = new ArrowIcon(true,"W");
	static ArrowIcon Right = new ArrowIcon(true,"E");
	static ArrowIcon Down = new ArrowIcon(true,"S");
	static ArrowIcon Up = new ArrowIcon(true,"N");
	public ArrowIcon(boolean ismerge,String mergeSpecs)
	{
		specs = mergeSpecs;
		merge = ismerge;
	}
	public void drawChip(Graphics gc, exCanvas c, int size0, int posx, int posy, String msg) 
	{	int size = size0*9/10;
		int ncount = 0;
		int scount = 0;
		int wcount = 0;
		int ecount = 0;
		for(int sz=specs.length(),i=0; i<sz;i++)
		{
			char ch = specs.charAt(i);
			int from_x = posx;
			int from_y = posy;
			int to_x = posx;
			int to_y = posy;
			int longsize = (size/2)*(merge?-1:1);
			int shortsize = Math.max(2,size/10)*(merge?-1:1);
			int tick = Math.abs(shortsize);
			switch(ch)
			{
			default: break;
			case 'N': 
			{
				if(merge) { from_y = posy-longsize; to_y = posy-shortsize; } else
				{ to_y = posy-longsize; from_y = posy - shortsize; };
				int dx = offc(ncount);
				from_x += dx;
				to_x += dx;
				ncount++;
			}
				break;
			case 'S':
			{
				if(merge) { from_y = posy+longsize; to_y = posy+shortsize; } else 
				{ to_y = posy+longsize; from_y = posy - shortsize; }
				int dx = offc(scount);
				from_x += dx;
				to_x += dx;
				scount++;
			}
				break;
			case 'W':
			{
				if(merge) { from_x = posx-longsize; to_x = posx - shortsize; }	else 
				{ to_x = posx-longsize; from_x = posx - shortsize; };
				int dy = offc(wcount);
				from_y += dy;
				to_y += dy;
				wcount++;
			}
				break;
			case 'E':
			{
				if(merge) { from_x = posx+longsize; to_x = posx + shortsize;}	else 
				{ to_x = posx+longsize; from_x = posx + shortsize; }
				int dy = offc(ecount);
				from_y += dy;
				to_y += dy;
				ecount++;
			}
				break;
			}
			gc.drawArrow(from_x,from_y,to_x,to_y,tick,1); 
		}
	}

	public int getWidth() {
		return NominalSize;
	}

	public int getHeight() {
		return NominalSize;
	}
	
}

public class TruMovespec extends commonMove implements TruConstants
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
        D.putInt("Flip",MOVE_FLIP);
        D.putInt("Split",MOVE_SPLIT);
        D.putInt("Merge",MOVE_MERGE);
 		D.putInt("Move",MOVE_BOARD_BOARD);
		D.putInt("MoveSplit",MOVE_AND_SPLIT);
		D.putInt("MoveMerge",MOVE_AND_MERGE);
   }

    TruId object;	// what was picked
    int chip =0;
	String splitInfo;	// specifier for split/join
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    Drawable obj = null;
    public TruMovespec()
    {
    } // default constructor

    /* constructor */
    public TruMovespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }

    /* constructor for pass and resign moves */
    public TruMovespec(int opc,int p)
    {
    	op = opc;
    	player = p;
     }
    /* constructor for "flip" moves */
    public TruMovespec(char col,int row,int p)
    {
    	op = MOVE_FLIP;
    	player = p;
    	from_row = to_row = row;
    	from_col = to_col = col;
    }
    /* constructor for "move" moves */
    public TruMovespec(char col,int row,char tocol,int torow,int p)
    {
    	op = MOVE_BOARD_BOARD;
    	player = p;
    	from_row =  row;
    	from_col =  col;
    	to_col = tocol;
    	to_row = torow;
    }
    /* constructor for split and merge moves */
    public TruMovespec(int opcode,char col,int row,String info,int p)
    {
    	op = opcode;
    	player = p;
    	from_row = to_row = row;
    	from_col = to_col = col;
    	splitInfo = info;
   }  
    public boolean Same_Move_P(commonMove oth)
    {
    	TruMovespec other = (TruMovespec) oth;

        return ((op == other.op) 
				&& (object == other.object)
				&& ((splitInfo==null)?(other.splitInfo==null):splitInfo.equals(other.splitInfo))
				&& (chip==other.chip)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (player == other.player));
    }

    public void Copy_Slots(TruMovespec to)
    {	super.Copy_Slots(to);
    	to.obj = obj;
        to.player = player;
		to.object = object;
		to.splitInfo = splitInfo;
		to.chip = chip;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
    }

    public commonMove Copy(commonMove to)
    {
    	TruMovespec yto = (to == null) ? new TruMovespec() : (TruMovespec) to;

        // we need yto to be a CarnacMovespec at compile time so it will trigger call to the 
        // local version of Copy_Slots
        Copy_Slots(yto);

        return (yto);
    }

    /* parse a string into the state of this move.  Remember that we're just parsing, we can't
     * refer to the state of the board or the game.
     * */
    private void parse(Tokenizer msg, int p)
    {
        String cmd = firstAfterIndex(msg);
        player = p;
        op = D.getInt(cmd, MOVE_UNKNOWN);        
        switch (op)
        {
        case MOVE_UNKNOWN:
        	throw G.Error("Can't parse %s", cmd);
        
        case MOVE_BOARD_BOARD:			// robot move from board to board
            from_col = msg.charToken();	//from col,row
            from_row = msg.intToken();
 	        to_col = msg.charToken();		//to col row
	        to_row = msg.intToken();
	        break;
	        
        case MOVE_AND_SPLIT:
        case MOVE_AND_MERGE:
            from_col = msg.charToken();	//from col,row
            from_row = msg.intToken();
 	        to_col = msg.charToken();		//to col row
	        to_row = msg.intToken();
	       	splitInfo = msg.nextToken();
	        break;

        case MOVE_SPLIT:
        case MOVE_MERGE:
        	to_col = from_col = msg.charToken();
        	to_row = from_row = msg.intToken();
        	splitInfo = msg.nextToken();
        	break;
        case MOVE_DROPB:
	       to_col = msg.charToken();
	       to_row = msg.intToken();
	       break;
        case MOVE_FLIP:
             from_col = msg.charToken();
            from_row = msg.intToken();
            break;
		case MOVE_PICKB:
            from_col = msg.charToken();
            from_row = msg.intToken();
            chip = msg.intToken();

            break;

        case MOVE_PICK:
            object = TruId.get(msg.nextToken());
            break;
            
        case MOVE_DROP:
            object = TruId.get(msg.nextToken());
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());
            break;

        default:
            break;
        }
    }
    

    private Text icon(commonCanvas v,Object... msg)
    {	double chipScale[] = {1,1.5,-0.2,-0.5};
    	Text m = TextChunk.create(G.concat(msg));
    	if(obj!=null)
    	{
    		m = TextChunk.join(TextGlyph.create("xx", obj, v,chipScale),
    					m);
    	}
    	return(m);
    }
    private Text splitIcon(commonCanvas v,boolean merge,String spl,String pre)
    {	double chipScale[] = {1,1.5,0.2,-0.5};
   
    	ArrowIcon icon = new ArrowIcon(merge,spl);
    	Text m = TextGlyph.create("xx", icon, v,chipScale);
    	if(pre!=null)
    	{
    		m = TextChunk.join(TextChunk.create(pre),m);
    	}
    	return m;
    }
    // the current move is a pickb
    // return null for a non-split move
    // return "" for a split move that should be specified by a previous move
    // return a combined split spec for a split/join that starts with this move
    private String extendedSplitInfo()
    {	
    	if("".equals(splitInfo)) { return ""; }
    	TruMovespec n = this;
    	String extended = "";
    	while((n = (TruMovespec)n.next)!=null && n.splitInfo!=null)
		{	
			extended = extended + n.splitInfo;
		}
    	if("".equals(extended)) { return null; }	// no split, must  be a move
    	return extended;
    }

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas v)
    {	
        switch (op)
        {
        case MOVE_PICKB:
        	{
        	//
        	// picks from the board can be a move of a stack, a split, or a merge.  
        	// if the pick is followed by a drop on the same color, the drop will 
        	// have no split info.  If the drop is to a different color, the drop
        	// will have split info.
        	String extended = extendedSplitInfo();
        	if(next==null) { return(icon(v,""+from_col+from_row)); }
        	else if(null == extended)
        		{ 
        		  return TextChunk.create(""+from_col+from_row+"-");
        		}
        	else if("".equals(extended)) { return TextChunk.create(""); }
        	else
        	{
        	// encoded as NSEW and if any of the directions are lower case, 
        	// this is a merge rather than a split
        	String upper = extended.toUpperCase();
        	boolean isMerge = !extended.equals(upper);
        	if(isMerge)
        		{
        			TruMovespec m = (TruMovespec)next;
        			return splitIcon(v,true,upper,""+m.to_col+m.to_row);
        		}
        		return splitIcon(v,false,upper,""+from_col+from_row);
        	}
        	
        	}
        	
        case MOVE_FLIP:
	        { char pcol = (char)(from_col-1);
	          int prow = from_row-1;
	           return(icon(v,""+pcol+prow));
	         }

		case MOVE_DROPB:
			if(splitInfo==null)
			{
				return icon(v,""+to_col+to_row);
			}
			else
			{
            return TextChunk.create("");
			}

        case MOVE_DROP:
        case MOVE_PICK:
            return icon(v,object.shortName);
        case MOVE_AND_SPLIT:
        	return splitIcon(v,false,splitInfo,""+from_col + from_row+"-"+to_col + to_row);
        case MOVE_AND_MERGE:
        	return splitIcon(v,true,splitInfo,""+from_col + from_row+"-"+to_col + to_row);
        	
        case MOVE_SPLIT:
        	return splitIcon(v,false,splitInfo,null);
        	
        case MOVE_MERGE:
        	return splitIcon(v,true,splitInfo,null);
        	
        case MOVE_BOARD_BOARD:
        	return(icon(v,""+from_col + from_row+"-"+to_col + to_row));
        case MOVE_DONE:
            return (TextChunk.create(""));

        default:
            return (TextChunk.create(D.findUniqueTrans(op)));

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
 
        case MOVE_AND_SPLIT:
        case MOVE_AND_MERGE:
			return(opname+ from_col + " " + from_row+" "+ to_col + " " + to_row+" "+splitInfo);
        case MOVE_SPLIT:
        case MOVE_MERGE:
        	return(opname + to_col + " "+to_row + " " + splitInfo);
        case MOVE_FLIP:
        	 return (opname + from_col + " " + from_row);
        	 
        case MOVE_PICKB:
	        return (opname + from_col + " " + from_row+" "+chip);

		case MOVE_DROPB:
	        return (opname+ to_col + " " + to_row+" "+chip);

		case MOVE_BOARD_BOARD:
			return(opname+ from_col + " " + from_row+" "+ to_col + " " + to_row);
        case MOVE_PICK:
            return (opname+object.shortName);

        case MOVE_DROP:
             return (opname+object.shortName);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
