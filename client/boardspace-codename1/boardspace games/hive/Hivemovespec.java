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
package hive;

import java.util.*;

import lib.Drawable;
import lib.G;
import lib.MultiGlyph;
import lib.Text;
import lib.TextChunk;
import lib.TextGlyph;
import online.game.*;
import lib.ExtendedHashtable;
public class Hivemovespec extends commonMove implements HiveConstants
{
    static final String preAttachNames[] = {"/", "-", "\\", "","",""};
    static final String postAttachNames[] = {"", "", "", "/", "-", "\\"};
    
    static ExtendedHashtable D = new ExtendedHashtable(true);
    static boolean shortenUniqueNames = false;
    static
    {
        // these int values must be unique in the dictionary
    	addStandardMoves(D,
        	"Pick", MOVE_PICK,
        	"Pickb", MOVE_PICKB,
        	"Drop", MOVE_DROP,
        	"Dropb", MOVE_DROPB,
        	"Pdropb",MOVE_PDROPB,
        	"Move",MOVE_MOVE,
        	"PMove",MOVE_PMOVE,
        	"MoveDone",MOVE_MOVE_DONE,
        	"PMoveDone",MOVE_PMOVE_DONE,
        	"PassDone",MOVE_PASS_DONE,
        	"PlayWhite",MOVE_PLAYWHITE,
        	"PlayBlack",MOVE_PLAYBLACK);
   }

    HiveId source; // where from/to
	HivePiece object;	// object being picked/dropped
    char to_col; // for from-to moves, the destination column
    int to_row; // for from-to moves, the destination row
    char from_col;
    int from_row;
    HiveState state;	// the state of the move before state, for UNDO
    HiveCell location; // source location for UNDO
    HiveCell stun; 		// for undo
    String attachment = "";
    HivePiece attachObject = null;
    int attachDirection = 0;
    
    public Hivemovespec()
    {
    } // default constructor

    /* constructor */
    public Hivemovespec(String str, int p)
    {
        parse(new StringTokenizer(str), p);
    }

    /* constructor */
    public Hivemovespec(StringTokenizer ss, int p)
    {
        parse(ss, p);
    }

    public Hivemovespec(int who,int opcode,HivePiece bug,HiveCell dest,HiveCell src)
    {
    	player = who;
    	op = opcode;
    	object = bug;
    	to_col = dest.col;
    	to_row = dest.row;
        source = HiveId.BoardLocation;
        setAttachment(dest,bug,src);
    }
    public Hivemovespec(int who,int opcode)
    {	player = who;
    	op = opcode;
    }
    public boolean Same_Move_P(commonMove oth)
    {
        Hivemovespec other = (Hivemovespec) oth;

        return ((op == other.op) 
				&& (source == other.source)
				&& (object == other.object)
				&& (from_col==other.from_col)
				&& (from_row==other.from_row)
				&& (to_row == other.to_row) 
				&& (to_col == other.to_col)
				&& (player == other.player));
    }

    public void Copy_Slots(Hivemovespec to)
    {	super.Copy_Slots(to);
		to.object = object;
		to.from_row = from_row;
		to.from_col = from_col;
        to.to_col = to_col;
        to.to_row = to_row;
        to.state = state;
        to.location = location;
        to.source = source;
        to.attachment = attachment;
        to.attachDirection = attachDirection;
        to.attachObject = attachObject;
    }

    public commonMove Copy(commonMove to)
    {
        Hivemovespec yto = (to == null) ? new Hivemovespec() : (Hivemovespec) to;

        // we need yto to be a Hivemovespec at compile time so it will trigger call to the 
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

        case MOVE_PMOVE:
        case MOVE_MOVE:
        case MOVE_PMOVE_DONE:
        case MOVE_MOVE_DONE:
        		{
        		HiveId pool = HiveId.get(msg.nextToken());
        		//
        		// this is a little unnatural and convoluted becase we want to maintain
        		// compatibilility with game records from before the pillbug.  for MOVE_MOVE
        		// the second token is B or W followed by the short bug id which doesn't include color
        		//
        		switch(pool)
	        		{
	        		default: throw G.Error("not expecting %s",pool);
	        		case Black_Bug_Pool: break;
	        		case White_Bug_Pool: break;
	        		}
	            source = HiveId.BoardLocation;
				HivePiece bug = HivePiece.getBug(pool.shortName,msg.nextToken());
				object = bug;
	            to_col = G.parseCol(msg);
	            to_row = G.IntToken(msg);
	            attachment = msg.nextToken();
        		}
	            break;
        case MOVE_DROPB:
            	{
            	String tok = msg.nextToken();
            	source = HiveId.BoardLocation;
            	// old game records are not specific as modern ones, so we
            	// have to use the player information.
            	object = player>=0
            				? HivePiece.getBug(player==0 ? "W":"B",tok) 
            				: HivePiece.getBug(tok);	// object to object number
            	to_col = G.parseCol(msg);
            	to_row = G.IntToken(msg);
            	attachment = msg.nextToken();
            	}
            	break;
        case MOVE_PDROPB:
	            source = HiveId.BoardLocation;
				object = HivePiece.getBug(msg.nextToken());	// object to object number
	            to_col = G.parseCol(msg);
	            to_row = G.IntToken(msg);
	            attachment = msg.nextToken();
	            break;

		case MOVE_PICKB:
			{
            source = HiveId.BoardLocation;
            from_col = G.parseCol(msg);
            from_row = G.IntToken(msg);
            String tok = msg.nextToken();
        	// old game records are not specific as modern ones, so we
        	// have to use the player information.
           object = (player>=0) 
            		? HivePiece.getBug(player==0?"W":"B",tok)  
            		: HivePiece.getBug(tok);
			}
            break;

        case MOVE_DROP:
             source = HiveId.get(msg.nextToken());
            to_col = '@';
            to_row = G.IntToken(msg);
            object = HivePiece.getBug(msg.nextToken());
            attachment = msg.nextToken();
            break;
            
        case MOVE_PICK:
            source = HiveId.get(msg.nextToken());
            from_col = '@';
            from_row = G.IntToken(msg);
            object = HivePiece.getBug(source.shortName,msg.nextToken());
            break;

        case MOVE_START:
            player = D.getInt(msg.nextToken());

            break;

        default:
            break;
        }
    }
    public static String bugName(HiveId pl,HivePiece bug)
    {
    	boolean plmatch = (bug.color==pl);
    	String name = plmatch?bug.standardBugName():bug.exactBugName();
    	if(shortenUniqueNames && bug.potentiallyUnique)
    		{ int last = name.length()-1;
    		  if(name.charAt(last)=='1') { name=name.substring(0,last); }
    		}
    	return(name);
    }
    public static String exactBugName(HivePiece bug)
    {	return(bugName(null,bug));
    }
    
    // return "attachment descriptors" which are to help humans parse moves, but are not
    // parsed (or parseable) themselves.
    public static String attachment(HiveCell c,HiveCell ignoreCell)
    {	HivePiece bug = c.topChip();
    	if(bug!=null) { return(exactBugName(bug)); }
    	for(int lim=c.geometry.n-1;lim>=0;lim--) 
    	{	HiveCell adjc = c.exitTo(lim);
    		if(adjc!=ignoreCell)
    		{
    		HivePiece ab = adjc.topChip();
    		if(ab!=null) 
    			{ return(postAttachNames[lim]+exactBugName(ab)+preAttachNames[lim]); }
    		}
       	}
    	return(".");
    }
    int scoreAsAttachment(HivePiece from,HivePiece to)
    {	int sc = 0;
    	if(from.color!=to.color) { sc += 100; }
    	if(to.isQueen())
    		{ sc += 50; }
    	if(to.potentiallyUnique) { sc++; }
    	return(sc);
    }
    // return "attachment descriptors" which are to help humans parse moves, but are not
    // parsed (or parseable) themselves.
    public void setAttachment(HiveCell c,HivePiece pickedObject,HiveCell ignoreCell)
    {	HivePiece bug = c.topChip();
    	if(bug!=null) 
    		{ attachment = exactBugName(bug);
    		  attachObject = bug; 
    		  attachDirection = -1; 
    		}
    	else if(pickedObject!=null)
    	{
    	int bestScore = -1;
    	// ignorecell is the place the pickedobject came from, we don't normally want to use
    	// that as the reference point...  the exception is when dismounting a beetle
    	///if((ignoreCell!=null) && ((ignoreCell.height()>=2) || (ignoreCell.topChip()!=pickedObject))) { ignoreCell = null; }
    	
    	for(int i=0;i<c.nAdjacentCells();i++) 
    	{	HiveCell adjc = c.exitTo(i);
    		if(adjc!=null && adjc!=ignoreCell)
    		{
    		HivePiece ab = adjc.topChip();
    		if(ab!=null) 
    			{ 
    			int score = scoreAsAttachment(pickedObject,ab);	// like opposite color, queens, and unique bugs in that order
    			if(score>=bestScore)
    				{
    				attachment = postAttachNames[i]+exactBugName(ab)+preAttachNames[i];
    				attachObject = ab;
    				attachDirection = i;
    				bestScore = score;
    				}
    			}
    		}
    		}
    	  if(bestScore>=0) { return; }
    	}
    	
    	attachment = ".";
    	attachObject = c.topChip();
    	attachDirection = -1;
    } 
    
    private String attachmentText()
    {
    	switch(attachDirection)
    	{
    	default: return(" ?? ");
    	case -1: return(" on ");
    	case 4: return(" left ");
    	case 5: return(" above-left ");
    	case 0: return("above-right ");
    	case 1: return(" right ");
    	case 2: return(" below-right ");
    	case 3: return(" below-left ");
    	}
    }
    private double[]attachScale(boolean reverse)
    {	double scl = 2.4;
    	double val[] = null;
    	switch(attachDirection)
    	{
    	default: 
    	case -1: val = new double[]{3.6,-0.3,0.0}; break;
    	case 4:  val = new double[]{scl,  0.5, 0.0}; break;
    	case 5:	val = new double[]{scl,  0.3, 0.45}; break;
    	case 0: val = new double[]{scl, -0.3, 0.45}; break;
    	case 1: val = new double[]{scl, -0.5, 0.0}; break;
    	case 2: val = new double[]{scl, -0.3,-0.45}; break;
    	case 3: val = new double[]{scl,  0.3, -0.45}; break;
    	}
    	if(reverse) { val[1]=-val[1]; val[2]=-val[2]; }
    	return(val);
    }
    private double attachYShift(boolean reverse)
    {
    	double scl[] = attachScale(reverse);
    	return(-0.25 -scl[2]/2);
    }
    private double glyphScale = 0.75;
    private double lineScale = 2.0;
    private double attachScale = 2.8;
    private Text attachmentGlyph(HiveGameViewer v,Drawable mainGlyph)
    {	boolean reverse = v.reverse_y();
    	if(attachObject==null)
    	{	if((op==MOVE_MOVE) && (mainGlyph!=null))
    		{ 
    		  return(TextGlyph.create("xxxx",mainGlyph,v,new double[]{lineScale,4,0,0}));
    		}
    		return(TextChunk.create(attachment));
    	}
    	else {
    		Text attach = TextGlyph.create("xxxx",attachObject,v,bugScale);
    		if(mainGlyph!=null)
    		{
    		 MultiGlyph glyph = MultiGlyph.create();	
    		 glyph.append(mainGlyph,new double[]{attachScale,0,0});
    		 glyph.prepend(attachObject,attachScale(reverse));
    		 return(TextGlyph.create("xxxx",glyph,v,new double[]{lineScale,glyphScale*2.2,1.0,attachYShift(reverse)}));
    		}
    		else
    		{
    		Text attext = TextChunk.create(attachmentText());
    		return(TextChunk.join(attext,attach));
    		}
    	}
     }
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public String shortMoveString()
    {
        switch (op)
        {
        case MOVE_PICKB:
        	{
        	HivePiece bug = object;
            return (bug.exactBugName());
        	}
        case MOVE_MOVE:
        case MOVE_PMOVE_DONE:
        case MOVE_MOVE_DONE:
        case MOVE_PMOVE:
        	{
        	HivePiece bug = object;
        	return(bug.exactBugName()+" "+attachment);
        	}
		case MOVE_DROPB:
		case MOVE_PDROPB:
            return (" "+attachment);

        case MOVE_DROP:
            return (D.findUnique(op) + " "+source.name()+ object.getName());
            
        case MOVE_PICK:
            return (object.exactBugName());
        case MOVE_DONE:
        case MOVE_RESET:
            return ("");

        case MOVE_EDIT:
        case MOVE_START:
        case MOVE_NULL:
        case MOVE_RESIGN:
        case MOVE_PASS:
            return (D.findUniqueTrans(op));

        default:
        	return D.findUnique(op);
      
        }
    }    

    
    /* construct a move string for this move.  These are the inverse of what are accepted
    by the constructors, and are also human readable */
    public Text shortMoveText(commonCanvas v)
    {	return(shortMoveText((HiveGameViewer)v));
    }
    double bugScale[] = { 2.0,glyphScale*5.0,0,-0.4 };
    public Text shortMoveText(HiveGameViewer v)
    {	if(v.useTextNotation) { return(TextChunk.create(shortMoveString())); }
        switch (op)
        {
        case MOVE_PLAYBLACK:
        	return(TextChunk.create("Play Black"));
        case MOVE_PLAYWHITE:
        	return(TextChunk.create("Play White"));
        case MOVE_PICKB:
        	{
        	if((next!=null) && ((next.op==MOVE_PDROPB)||(next.op == MOVE_DROPB)) && (((Hivemovespec)next).attachObject!=null)) 
        		{ return(TextChunk.create("")); }
        	return(TextGlyph.create("xxxx",object,v,bugScale));
        	}
        case MOVE_MOVE:
        case MOVE_PMOVE:
        case MOVE_PMOVE_DONE:
        case MOVE_MOVE_DONE:
        	{
         	return(attachmentGlyph(v,object));
        	}
		case MOVE_DROPB:
		case MOVE_PDROPB:
            return (attachmentGlyph(v,object));

        case MOVE_DROP:
            return (TextChunk.join(TextChunk.create(D.findUnique(op) + " "+source.shortName),
            					   TextGlyph.create("xxxx",object,v,bugScale)));
            
        case MOVE_PICK:
        	if((next!=null) && ((next.op == MOVE_DROPB)||(next.op==MOVE_PDROPB)) && (((Hivemovespec)next).attachObject!=null)) 
    			{ return(TextChunk.create("")); }
        	return (TextGlyph.create("xxxx",object,v,bugScale));
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
        	
        case MOVE_PICKB:
	        return (opname+ G.printCol(from_col) + " " + from_row+" "+object.exactBugName());

        case MOVE_MOVE:
        case MOVE_PMOVE:
        case MOVE_PMOVE_DONE:
        case MOVE_MOVE_DONE:
        	{
        	HivePiece bug = object;
	        return (opname +bug.color.shortName()+" "+bug.exactBugName()+" " + G.printCol(to_col) + " " + to_row+" "+attachment);
        	}
        case MOVE_DROPB:
        case MOVE_PDROPB:
	        return (opname +object.exactBugName()+" " + G.printCol(to_col) + " " + to_row+" "+attachment);

        case MOVE_DROP:
            return (opname +source.shortName+" "+to_row+" "+object.exactBugName()+" "+attachment);

        case MOVE_PICK:
        	{
       		HivePiece bug =object;
            return (opname +source.shortName+" "+from_row+" "+bug.exactBugName());
        	}
        case MOVE_START:
            return (indx+"Start P" + player);

        default:
            return (opname);
        }
    }

}
