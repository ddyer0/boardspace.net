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
package octiles;

import online.game.*;

import lib.G;
import lib.HorizontalBar;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;
import bridge.Color;
import lib.ExtendedHashtable;
import lib.Font;


public class OctilesMovespec extends commonMPMove implements OctilesConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKB = 206; // pick from the board
    static final int MOVE_DROPB = 207; // drop on the board
    static final int DROP_AND_MOVE = 210;	// move board to board
    static final int MOVE_ROTATE = 211;	// rotate a chip

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
  			"Move",DROP_AND_MOVE,
  			"Rotate",MOVE_ROTATE);
  	}

    OctilesId source; // where from/to
	int object;	// object being picked/dropped
	char from_col; //for from-to moves, the source column
	int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    char drop_col;	// columb where tile is dropped
    int drop_row;	// row where tile is dropped
    int rotation;	// the rotation of the dest chip
    OctilesState state;	// the state of the move before state, for UNDO
    int saved_rotation;	// the rotation under the tile drop, for UNDO
    
    public OctilesMovespec()
    {
    } // default constructor

    /* constructor */
    public OctilesMovespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }
    /* constructor */
    public OctilesMovespec(int opc,int who)
    {
    	op = opc;
    	player = who;
    }

    public OctilesMovespec(char drop_c,int drop_r,int drop_obj,int rot,
    		char from_c,int from_r,char to_c,int to_r,int pl)
    {	player = pl;
    	op = DROP_AND_MOVE;
    	drop_col = drop_c;
    	drop_row = drop_r;
    	object = drop_obj;
    	rotation = rot;
    	from_col = from_c;
    	from_row = from_r;
    	to_col = to_c;
    	to_row = to_r;
    }
    public boolean Same_Move_P(commonMove oth)
    {
    	OctilesMovespec other = (OctilesMovespec) oth;

        return ((op == other.op) 
        		&& (rotation == other.rotation)
				&& (source == other.source)
				&& (object == other.object)
				&& (state == other.state)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (from_row == other.from_row)
				&& (from_col == other.from_col)
				&& (drop_col == other.drop_col)
				&& (drop_row == other.drop_row)
				&& (player == other.player));
    }

    public void Copy_Slots(OctilesMovespec to)
    {	super.Copy_Slots(to);
        to.player = player;
		to.object = object;
		to.rotation = rotation;
        to.to_col = to_col;
        to.to_row = to_row;
        to.from_col = from_col;
        to.from_row = from_row;
        to.drop_col = drop_col;
        to.drop_row = drop_row;
        to.state = state;
        to.source = source;
        to.saved_rotation = rotation;
    }

    public commonMove Copy(commonMove to)
    {
    	OctilesMovespec yto = (to == null) ? new OctilesMovespec() : (OctilesMovespec) to;

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
        
 	        
        case DROP_AND_MOVE:			// robot move from board to board
            source = OctilesId.TileLocation;	
            drop_col = msg.charToken();
            drop_row = msg.intToken();
            object = msg.intToken();       //dropped tile num
            rotation = msg.intToken();
            from_col = msg.charToken();	//from col,row
            from_row = msg.intToken();
 	        to_col = msg.charToken();		//to col row
	        to_row = msg.intToken();
	        break;
	        
        case MOVE_DROPB:
	       source = OctilesId.TileLocation;
	       to_col = msg.charToken();
	       to_row = msg.intToken();
	       object = msg.intToken();
	       rotation = msg.intToken();
	       break;

		case MOVE_PICKB:
            source = OctilesId.TileLocation;
            from_col = msg.charToken();
            from_row = msg.intToken();
            object = msg.intToken();

            break;
        case MOVE_ROTATE:
        	source = OctilesId.TileLocation;
        	to_col = msg.charToken();	
        	to_row = msg.intToken();
            object = msg.intToken();
            rotation = msg.intToken();
            break;

        case MOVE_PICK:
            source = OctilesId.TilePoolRect;
            object = msg.intToken();
            break;
            
        case MOVE_DROP:
            source = OctilesId.TilePoolRect;
            object = msg.intToken();
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

        default:

            break;
        }
    }
    
    private Text makeGraph(int dis,int player,commonCanvas canvas)
    {
    	int height = Font.getFontMetrics(canvas).getHeight()*2/3;
    	double distance = Math.min(7, Math.max(0,dis))/7.0;
    	Color color = canvas.getMouseColor(player);
    	HorizontalBar graph = new HorizontalBar(height*8,height,distance,color);
    	Text chunk = TextGlyph.create(",XXXXX",graph,canvas,new double[] {1,1.5,-0.5,0});
    	return(chunk);
    }
    private Text makeIcon(int chipn,int rotation,commonCanvas canvas)
    {
    	OctilesChip chip = OctilesChip.getChip(chipn);
    	return(TextGlyph.create("xx",chip,canvas,new double[] {1.3,1.5,-0.5,0}));
    }
    public Text censoredMoveText(commonCanvas canvas,OctilesMovespec next)
    {
    	Text def = shortMoveText(canvas);
    	switch(op)
    	{
    	default: break;
    	case MOVE_PICK:
    	case MOVE_DROP:
    	case MOVE_ROTATE:
    		return(TextChunk.create(""));
    	case MOVE_DROPB:
    		if(object<100) 
    			{ return(TextChunk.join(
    						makeIcon(object,rotation,canvas),
    						TextChunk.create(""+to_col+to_row+" ")
    						)); }
    		break;
    	case MOVE_PICKB:
    		if(next!=null && next.op==MOVE_DROPB && (object>=100))
    		{
        		int distance = (int)G.distance(from_col, from_row, next.to_col,next.to_row);
        		return(TextChunk.join(makeGraph(distance,player,canvas),TextChunk.create(""+from_col+from_row)));
    		}
    		break;
    	case DROP_AND_MOVE:
    		int distance = (int)G.distance(from_col, from_row, to_col,to_row);
    		return(TextChunk.join(
    					makeIcon(object,rotation,canvas),
    					TextChunk.create(""+drop_col+drop_row+" "),
    					makeGraph(distance,player,canvas),
    					TextChunk.create(""+from_col+from_row+"-"+to_col+to_row)));
    	}
    	return(def);
    }

    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String shortMoveString()
    {
        switch (op)
        {
        case MOVE_PICKB:
            return (object+" "+from_col + from_row);

		case MOVE_DROPB:
 		case MOVE_ROTATE:
 			if(to_col>='a') { return("("+rotation+")"); }
 			else { return("-"+to_col+to_row); }
        case MOVE_DROP:
            return (""+object+">>");
        case MOVE_PICK:
            return (((object<10)?" ":"")+object);
        case DROP_AND_MOVE:
        {	String ob = (object<10)?" "+object:""+object;
        	return(ob+"("+rotation+")"+drop_col+drop_row+" "+from_col + from_row+"-"+to_col + to_row);
        }
        case MOVE_DONE:
            return ("");

        default:
            return (D.findUniqueTrans(op));

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

        case MOVE_PICKB:
	        return (opname + from_col + " " + from_row+" "+object);

		case MOVE_DROPB:
		case MOVE_ROTATE:
	        return (opname + to_col + " " + to_row+" "+object+" "+rotation);

		case DROP_AND_MOVE:
			return(opname+ drop_col+" "+drop_row+" "+object+" "+rotation+" "
					+ from_col + " " + from_row+" "+to_col+" "+to_row);
        case MOVE_PICK:
        case MOVE_DROP:
             return (opname+object);

        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
