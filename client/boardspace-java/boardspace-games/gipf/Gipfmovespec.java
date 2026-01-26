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
package gipf;

import online.game.*;

import lib.G;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import lib.Tokenizer;

import java.awt.Font;

import lib.ExtendedHashtable;

public class Gipfmovespec extends commonMove implements GipfConstants
{
    static ExtendedHashtable D = new ExtendedHashtable(true);

    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
        	"Remove",MOVE_REMOVE,
        	"Slide",MOVE_SLIDE,
        	"Preserve",MOVE_PRESERVE,
        	"Standard",MOVE_STANDARD,
        	"SlideFrom", MOVE_SLIDEFROM,
        	"Zertz",MOVE_ZERTZ,
        	"Yinsh",MOVE_YINSH,
        	"Tamsk",MOVE_TAMSK,
        	"Punct",MOVE_PUNCT,
        	"Dvonn",MOVE_DVONN,
        	"Pslide",MOVE_PSLIDE,
        	"Pdrop",MOVE_PDROPB
        	);
   }

    GipfId source;
    Potential from_potential = Potential.None;
    char from_col; // for from-to moves, the source column
    int from_row; // for from-to moves, the source row
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    int undoinfo;
    
    /* constructor */
    public Gipfmovespec(){};
    
    // constructor for robot moves
    public Gipfmovespec(int opcode,char fc,int fr,int pl)
    {
    	player = pl;
    	op = opcode;
    	from_col = fc;
    	from_row = fr;
    	to_col = fc;
    	to_row = fr;
    }
    // constructor for robot moves
    public Gipfmovespec(int opcode,Potential p,char fc,int fr,int pl)
    {
    	player = pl;
    	from_potential = p;
    	op = opcode;
    	from_col = fc;
    	from_row = fr;
    	to_col = fc;
    	to_row = fr;
    }
    // constructor for robot moves
    public Gipfmovespec(int opcode,GipfCell c,GipfCell d,int pl)
    {
    	player = pl;
    	op = opcode;
    	from_col = c.col;
    	from_row = c.row;
    	to_col = d.col;
    	to_row = d.row;
    }
    public Gipfmovespec(Potential p,GipfCell from,GipfCell to,int pl)
    {	op = MOVE_PSLIDE;
    	from_potential = p;
    	from_col = from.col;
    	from_row = from.row;
    	to_col = to.col;
    	to_row = to.row;
    	player = pl;
    }
    
    public Gipfmovespec(String str, int p)
    {
        parse(new Tokenizer(str), p);
    }

    /* constructor */
    public Gipfmovespec(int scmd, int playerindex)
    {
        op = scmd;
        player = playerindex;
    }

    public boolean Same_Move_P(commonMove oth)
    {
        Gipfmovespec other = (Gipfmovespec) oth;

        return ((op == other.op)
        		&& (from_col == other.from_col)
        		&& (from_row == other.from_row) 
        		&& (from_potential==other.from_potential)
        		&& (to_row == other.to_row) 
        		&& (player == other.player));
    }
    public void Copy_Slots(Gipfmovespec to)
    {
    	super.Copy_Slots(to);
    	to.from_col = from_col;
    	to.from_row = from_row;
    	to.to_col = to_col;
    	to.to_row = to_row;
    	to.source = source;
    	to.from_potential = from_potential;
    	to.undoinfo = undoinfo;
    }
    public commonMove Copy(commonMove o)
    {	Gipfmovespec oo = (o==null) ? new Gipfmovespec() : (Gipfmovespec)o;
    	Copy_Slots(oo);
        return (oo);
    }

    /* parse a string into the state of this move */
    private void parse(Tokenizer msg, int p)
    {
        String cmd = firstAfterIndex(msg);
        player = p;
        op = D.getInt(cmd, MOVE_UNKNOWN);
        switch(op)
        {
        case MOVE_UNKNOWN:   throw G.Error("Can't parse %s", cmd);

        case MOVE_PSLIDE:
        	from_potential = Potential.valueOf(msg.nextToken());
			//$FALL-THROUGH$
		case MOVE_SLIDEFROM:
		case MOVE_TAMSK:
        case MOVE_SLIDE:
        case MOVE_ZERTZ:
        case MOVE_YINSH:
        case MOVE_DVONN:
        case MOVE_PUNCT:
        	from_col = msg.charToken();
        	from_row = msg.intToken();
        	to_col = msg.charToken();
        	to_row = msg.intToken();
        	break;
        case MOVE_PDROPB:
        	from_potential = Potential.valueOf(msg.nextToken());
			//$FALL-THROUGH$
		case MOVE_DROPB:
        	from_col = to_col = msg.charToken();
        	from_row = to_row = msg.intToken();
        	break;
        case MOVE_REMOVE:
        case MOVE_PICKB:
        case MOVE_PRESERVE:
        	from_col = msg.charToken();
        	from_row = msg.intToken();
        	break;
        case MOVE_DROP:
        case MOVE_PICK:
        	source = GipfId.get(msg.nextToken());
        	if(msg.hasMoreTokens()) { from_row=to_row=msg.intToken(); }
        	break;
        case MOVE_START:
        	player = D.getInt(msg.nextToken());
        	break;
        default:
        	break;
        }
     }
    private Text icon(commonCanvas v,Object... msg)
    {	double chipScale[] = {1,1.0,-0.1,-0.1};
    	Text m = TextChunk.create(G.concat(msg));
    	if(from_potential!=null)
    	{	GipfChip ch = GipfChip.getChip(player,from_potential);
    		m = TextChunk.join(TextGlyph.create("xxxx", ch, v,chipScale),
    					m);
    	}
    	return(m);
    }
    
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas v,Font f)
    {
        switch (op)
        {
        default: return TextChunk.create(shortMoveString());
        case MOVE_PICKB:
        	return icon(v,from_col,from_row);
        case MOVE_PDROPB:
        	return icon(v,to_col,to_row);
        case MOVE_DROPB:
        	return icon(v,to_col,to_row);
        case MOVE_ZERTZ:
        case MOVE_YINSH:
        case MOVE_TAMSK:
        case MOVE_PUNCT:
        case MOVE_DVONN:
        	return icon(v,from_col,from_row,"-",to_col,to_row);
       }
    }

    
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String shortMoveString()
    {
        switch (op)
        {
        case MOVE_PICK: return("");
        case MOVE_DROP:
        	return(source.shortName);
        case MOVE_REMOVE:
        	return G.concat("x ",from_col,from_row);
        case MOVE_PRESERVE:
        	return G.concat("++ ",from_col,from_row);
        case MOVE_PICKB:
        	return G.concat(from_col," ",from_row);
        case MOVE_PDROPB:
        	return G.concat(from_potential," ",to_col,to_row);
        case MOVE_DROPB:
        	return G.concat(to_col,to_row);
        case MOVE_START:
            return G.concat("Start P",player);
        case MOVE_PSLIDE:
        	return G.concat(from_potential," ",from_col,from_row,"-",to_col,to_row);
        case MOVE_SLIDE:
        case MOVE_ZERTZ:
        case MOVE_YINSH:
        case MOVE_TAMSK:
        case MOVE_PUNCT:
        case MOVE_DVONN:
        	return G.concat(from_col,from_row,"-",to_col,to_row);
        case MOVE_SLIDEFROM:
        	return G.concat(",",to_col,to_row);
        case MOVE_DONE:
        	 return("");
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

        switch (op)
        {
        case MOVE_DROP:
        	return G.concat(opname,source.shortName," ",to_row);
        case MOVE_PICK:
        	return G.concat(opname,source.shortName," ",from_row);
        case MOVE_PICKB:
        case MOVE_REMOVE:
        case MOVE_PRESERVE:
        	return G.concat(opname,from_col," ",from_row);
        case MOVE_PDROPB:
        	return G.concat(opname,from_potential," ",to_col," ",to_row);
        case MOVE_DROPB:
           	return G.concat(opname,to_col," ",to_row);
        case MOVE_START:
            return G.concat(indx,"Start P",player);
        case MOVE_SLIDEFROM:
        case MOVE_YINSH:
        case MOVE_ZERTZ:
        case MOVE_DVONN:
        case MOVE_PUNCT:
        case MOVE_SLIDE:
        case MOVE_TAMSK:
        	return G.concat(opname,from_col," ",from_row," ",to_col," ",to_row);
        case MOVE_PSLIDE:
        	return G.concat(opname,from_potential," ",from_col," ",from_row," ",to_col," ",to_row);
        default:
            return (opname);
            
        }
    }
}
