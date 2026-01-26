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
package online.game;

import java.awt.Font;
import java.awt.event.ActionListener;

import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.game.sgf.export.sgf_names;
import online.search.UCTNode;

import java.io.PrintStream;
import java.util.*;
import lib.*;

/**
 * this class is the basis for representing individual moves in the game.  A few standard
 * actions are expected, but most of the actual moves in the game should be named and parsed
 * as required for the particular game.  Standard practice is to parse strings using Tokenizer
 * for simplicity, but that is not required.
 * 
 * commonMoves accept and parse strings, which may be either generated locally or incoming from
 * the network, and generate an internal representation for use by the board.  They also can 
 * print themselves in several ways, as "short" non-parseable strings for the game log, and
 * as "long" parseable strings for transmission and creation of game records. 
 * 
 * commonMoves are also used as the currency of the robots, which use Vectors of moves
 * and produce evaluations and other statistics of each move.
 * 
 * for games with more than 2 players, you should use {@link commonMPMove}
 * 
 * @author ddyer
 *
 */

class RobotProperties
{
	UCTNode uctNode = null;
    double evaluation = Double.NaN; 		// the net evaluation, after search, of this move
    double local_evaluation = Double.NaN; 	// static eval of this move
    commonMove best_move = null; // the best successor move to this move, from search
    static final commonMove.EStatus DEFAULT_DEPTH_LIMITED = commonMove.EStatus.NOT_EVALUATED;
    commonMove.EStatus depth_limited = DEFAULT_DEPTH_LIMITED;	// if this move hit the depth limit for the search
    boolean gameover;		// true if this move ended the game
    
    void Copy_Slots(RobotProperties to)
    {
    	to.uctNode = uctNode;
    	to.evaluation = evaluation;
    	to.local_evaluation = local_evaluation;
    	to.best_move = best_move;
    	to.depth_limited = depth_limited;
    	to.gameover = gameover;
    }
}

class HistoryProperties
{
    int index = -1; 			// index in the history buffer
    String comment = null;
    String numString = null;
    boolean lineBreak = false;
    long elapsedTime = -1;
    StackIterator<commonMove> variations;
    Hashtable<String,Object>properties;
    StackIterator<MoveAnnotation> annotations;
    void Copy_Slots(HistoryProperties to)
    {
    	to.index = index;
    	to.comment = comment;
    	to.numString = numString;
    	to.lineBreak = lineBreak;
    	to.variations = variations;
    	to.properties = properties;
    	to.elapsedTime = elapsedTime;
    	to.annotations = annotations;
    }
}

/**
 * this is a default filter used to present only the ephemeral or only the final moves
 * at the end of the history for use by the game filter.  "Ephemeral" moves are 
 * those executed in parallel by the players for the final move in progress.
 * 
 * The standard methodology is to pair simultaneous and synchronous moves, and 
 * convert the moves in the history from the simul to syn modes when the moves
 * are committed and put in the standard order.
 * 
 * @author ddyer
 *
 */
class FinalFilter implements online.game.commonMove.MoveFilter
{ 	public FinalFilter(boolean fin) { includeFinal = fin; }
	boolean includeFinal;
	// renumbering must be off for the ephemeral pass, and normally will be on for the regular pass
	public boolean reNumber() { return(includeFinal); }
	public boolean included(commonMove m) 
	{	return(m.isTransmitted() && (includeFinal != m.isEphemeral()));
	}
}


public abstract class commonMove 
	implements lib.CompareTo<commonMove> , Opcodes, StackIterator<commonMove>, SequenceElement,sgf_names
{	
	public enum EStatus 
	{
		NOT_EVALUATED,					// not examined yet
		EVALUATED,						// static evaluated
		EVALUATED_CONTINUE,				// push another level
		EVALUATED_DRAWN,				// evaluated as a draw
		DEPTH_LIMITED_SEARCH,			// depth limited by search
		DEPTH_LIMITED_GAMEOVER,			// depth limited because the game is over
		DEPTH_LIMITED_TRANSPOSITION;	// depth limited by the transposition table
	}
	private HistoryProperties HProps=null;
	private RobotProperties RProps=null;
    public long digest = 0;			// digest after making this move through the ui
	public long digest() { return(digest); }
	public void setDigest(long v) { digest = v; }
	public static MoveFilter getFinalFilter() { return(new FinalFilter(true)); }
	public static MoveFilter getEphemeralFilter() { return(new FinalFilter(false)); }
	public MoveFilter finalFilter() { return(new FinalFilter(true)); }
	public MoveFilter ephemeralFilter() { return(new FinalFilter(false)); }

	public String getNodeName() { return (String)getProperty(nodename_property); }
	public void setNodeName(String name) { setProperty(nodename_property,name); }
	
	private HistoryProperties H()
	{
		if(HProps==null) 
			{ HProps = new HistoryProperties(); 
			}
		return(HProps);
	}
	private RobotProperties R()
	{
		if(RProps==null) { RProps = new RobotProperties(); }
		return(RProps);
	}
	
	// functions that access the extensions
	public StackIterator<MoveAnnotation> getAnnotations() { return HProps==null ? null : HProps.annotations; }
	public void setAnnotations(StackIterator<MoveAnnotation> an) {   if(an!=null || HProps!=null) { H().annotations = an; }}
	public void addAnnotation(MoveAnnotation an)
	{
		StackIterator<MoveAnnotation> old = getAnnotations();
		if(old==null) { old = an;} else { old = old.push(an); }
		setAnnotations(old);
	}
	public long elapsedTime() { return(H().elapsedTime); }
	public void setElapsedTime(long v)
    { HistoryProperties h = H();
   	  //G.print("E "+this+" "+h.elapsedTime+"->"+v);
      h.elapsedTime = v; }
    public boolean gameover() { return(RProps==null ? false : R().gameover); }
    public boolean isDrawn() { return(RProps==null ? false : R().depth_limited==EStatus.EVALUATED_DRAWN); }
    public void setGameover(boolean v) { if((RProps==null)&&(v==false)) {} else { R().gameover = v; }}  
    public EStatus depth_limited() { return(RProps==null ? RobotProperties.DEFAULT_DEPTH_LIMITED : R().depth_limited);}
    /**
     * return true if this node is marked for deeper search.  Normally this is status EVALUATED
     * but EVALUATED_CONTINUE marks nodes extended for quiescence search.
     * @return
     */
    public boolean searchDeeper()
    {
    	EStatus lim = depth_limited();
    	return lim==EStatus.EVALUATED || lim==EStatus.EVALUATED_CONTINUE || lim==EStatus.NOT_EVALUATED;
    }
    public void set_depth_limited(EStatus v) { R().depth_limited = v; }
    public double local_evaluation() { return(R().local_evaluation); }
    public double set_local_evaluation(double v) { R().local_evaluation = v; return(v); }
    public double evaluation() { return R().evaluation; }
    public double setEvaluation(double v) { R().evaluation = v; return(v); }
	public void setEvaluations(double v)
	{	RobotProperties R = R();
		R.local_evaluation = R.evaluation = v;
	}
    public commonMove best_move() { return(RProps==null ? null : R().best_move); };
    public void set_best_move(commonMove v) { if((v==null) && (RProps==null)) {} else { R().best_move = v; }}
    public UCTNode uctNode() { return(RProps==null ? null : R().uctNode); }
    public void setUctNode(UCTNode n) { R().uctNode=n; }
    public int index() { return(HProps==null ? -1 : H().index); }
    public void setIndex(int v) { H().index = v; }
	
	public interface MoveFilter {
		public boolean included(commonMove m);
		public boolean reNumber(); 
	}
	class TrueFilter implements MoveFilter
		{ public boolean included(commonMove m) { return(true); }
		  public boolean reNumber() { return(false); }
		}
	/**
	 * return true if this move is ephemeral - ie if will be
	 * executed by all the players simultaneously.  Normally
	 * any ephemeral moves are later transformed into non-ephemeral
	 * equivalents.
	 * @return
	 */
	public boolean isEphemeral() { return(false); }		// ephemeral moves that will be replaced
	public boolean isTransmitted() { return(true); }	// moves that will be transmitted to others
	/**
	 * this can be overridden to not care or care about numbers other than 2
	 * @param n
	 */
	public void checkNPlayers(int n)
		{ G.Assert(n<=2,"must use commonMPMoves rather than commonMove"); 
		}
	
	public String getComment() { return(HProps==null ? null : H().comment); }
	public void setComment(String str) 
		{ if(str==null && HProps==null) { }
		else { H().comment = str; } 
		}
	
	public boolean getLineBreak() { return(HProps==null ? false : H().lineBreak); }
	public void setLineBreak(boolean v) 
		{ if(!v && HProps==null) {} 
			else { H().lineBreak=v; } 
		}

	static String DEFAUlTNUMSTRING = "?";	// so the UI can recognize nondefault..
	public String getSliderNumString()
		{ 	String v = HProps==null ? null : H().numString;
			return(v==null ? DEFAUlTNUMSTRING : v);
		}
	private static Object getDefaultNumString()
	{
		return(DEFAUlTNUMSTRING);
	}
	public static boolean isDefaultNumString(Object st)
	{	// fool the bug checker into forgetting we're dealing with a string
		Object a = getDefaultNumString();
		return(st==a);
	}
	public void setSliderNumString(String str)
		{ boolean def = (str==null || isDefaultNumString(str));
		  if(def && HProps==null) {}
		  else { H().numString = str;	} 
		}

	public double reScorePosition(int forPlayer)
	{
		return((player == forPlayer)
    			? evaluation()			// player doesn't change 
    			: -evaluation());
	}
	public double reScorePosition(int forplayer,double valueOfWin)
	{
		throw G.Error("you shouldn't get here, type should be commonMPMove");
	}
	public double reScoreMPPosition(int forplayer)
	{
		throw G.Error("you shouldn't get here, type should be commonMPMove");
	}

   //
    // note that if you add variables, you probably should also add them
    // to the Copy_Slots and same_move_p methods
    //
    public int player=-1;			// board index of the player who made this move
    public int player() { return player; }
	public int op=-1;				// move opcode
    //
    // these are used by the search driver, but since that vastly dominates
    // the usage, we make everyone else drag them around too.
    //
	public commonMove next = null; // next move in sequence, for the game history
    
    public StackIterator<commonMove> getVariations() { return(HProps==null ? null : H().variations); }
    public void setVariations(StackIterator<commonMove> to) 
    { 	if(to==null && HProps==null) { }
    	else { H().variations= to; 
    		}
    }
    
    public Object getProperty(String propName)
    {
    	if(HProps == null) { return(null); }
    	Hashtable<String,Object> props = H().properties;
    	if(props!=null) { return(props.get(propName)); }
    	return(null);
    }
    public void copyPropertiesTo(sgf_node root)
    {
    	Hashtable<String,Object> props = H().properties;
    	if(props!=null)
    	{
    		for(Enumeration<String> keys = props.keys(); keys.hasMoreElements();)
    		{
    			String k = keys.nextElement();
    			Object v = props.get(k);
    			if(v!=null)
    			{
    				root.set_property(k,v.toString());
    			}
    		}
    	}
    }
    public void printProperties(PrintStream ps)
    {
    	Hashtable<String,Object> props = H().properties;
    	if(props!=null)
    	{
    		for(Enumeration<String> keys = props.keys(); keys.hasMoreElements();)
    		{
    			String k = keys.nextElement();
    			Object v = props.get(k);
    			if(v!=null)
    			{
    			ps.print(k);
    			ps.print("[");
    			ps.print(sgf_property.bracketedString(v.toString()));
    			ps.println("]");
    			}
    		}
    	}
    }
    public void setProperty(String propName,Object value)
    {	if(HProps==null && value==null) {}
    	HistoryProperties h = H();
    	Hashtable<String,Object>props = h.properties;
    	if(value==null && props==null) {}
    	else
    	{	if(props==null) { h.properties = props = new Hashtable<String,Object>(); }
    		if(value==null) 
    			{ props.remove(propName);
    			  if(props.size()==0) { h.properties=null; }
    			}
    		else { props.put(propName, value); }
    	}
    }
    
    //public int search_clock;	// point at which this move was encountered by the robot
	
     /** 
     * this is used to compare two moves and determine if they are the same.  This "sameness"
     * should reflect the movement and resource changes local to the move, but not the complete
     * state of the board.  It's used to find candidates for "killer" heuristics in the search,
     * and to detect new/not new branches in the game history.
     * @param other
     * @return true if these moves are the same move 
     */
    public abstract boolean Same_Move_P(commonMove other);

    /** used to tack a player id onto a move for game records */
    public String playerString() { return("P"+player); }
    /**
     * the inverse of playerString, with allowances for string case
     * @param st
     * @return a small integer
     */
    public static int playerNumberToken(String st)
	{
		if(st.equalsIgnoreCase("p-1")) { return(0); }
		if(st.length()==2)
		{
		int ch = st.charAt(0);
		int nn = st.charAt(1)-'0';
		if(((ch=='p')||(ch=='P')) && (nn>=0)) { return(nn); }
		}
	return(-1);
	}
    

    
    /** longMoveString is used for sgf format records and can contain other information
     * intended to be ignored in the normal course of play, for example human-readable
     * information
     */
    public String longMoveString() { return(moveString()); }
    /** create a copy of this move.  The default method calls Copy_Slots, which is probably
     * the method that should be overridden or wrapped.
     * @param to
     * @return a Copy of this move.  This copy should include any modifications
     * and/or annotations that were made when the move was executed.
     */
    public abstract commonMove Copy(commonMove to);
    
    /** wrap this method to augment the behavior of Copy */
    public void Copy_Slots(commonMove to)
    {	//to.setLineBreak(getLineBreak());
        //to.search_clock = search_clock;
        //to.setVariations(getVariations());
    	if(HProps!=null) { HProps.Copy_Slots(to.H()); }
    	if(RProps!=null) { RProps.Copy_Slots(to.R()); }
    	
        to.player = player;
        to.op = op;
        to.digest = digest;
    }

    // used in sorting move lists according to evaluation */
    public int compareTo(commonMove o)
    {	double v = o.evaluation()-evaluation();
    	return(v>0.0 ? 1 : v<0.0 ? -1 : 0);
    }
    
    
   
    // alternate sort predicate which sorts all terminal nodes before any nonterminal.
    // this is an optimization in the search, to consider all the moves that won't 
    // cause more search before any that may.
    //
    public int altCompareTo(commonMove o)
    {	
    	// sort all game ending moves first, all terminals second, then by evaluation
    	boolean go = gameover();
    	if(o.gameover()!=go)
    		{ return(go ? -1 : 1); 
    		}
    	boolean o_depth = o.depth_limited()!=EStatus.EVALUATED;
    	boolean depth = depth_limited()!=EStatus.EVALUATED;
    	if(o_depth!=depth)
    		{	return(depth ? -1 : 1); 
    		}
    	double v = o.evaluation()-evaluation();
    	return(v>0.0 ? 1 : v<0.0 ? -1 : 0);
    } 		 

    /* true of this other move is the same as this one */
    public int nVariations()
    {	
        if (next == null)
        {
            return (0);
        }
        StackIterator<commonMove> variations = getVariations();
        return(variations==null ? 0 : variations.size());

    }
    /**
     * get the N'th variation
     * @param n
     * @return a commonMove
     */
    public commonMove getVariation(int n)
    {	StackIterator<commonMove> variations = getVariations();
    	if(variations==null) { return(next); }
    	return(variations.elementAt(n));
    }

    public commonMove firstVariation()
    {
        return (next);
    }


    public commonMove findVariation(commonMove command)
    {
        if ((next != null) && command.equals(next))
        {
            return (next);
        }
        StackIterator<commonMove> variations = getVariations();
        if (variations != null)
        {
            for (int i = 0,lim = variations.size(); i < lim; i++)
            {
                commonMove m = variations.elementAt(i);

                if (command.equals(m))
                {
                    return (m);
                }
            }
        }

        return (null);
    }

    public void variationsMenu(PopupManager menu,MenuParentInterface window,ActionListener listen)
    {	menu.newPopupMenu(window,listen);
    	StackIterator<commonMove> variations = getVariations();
        if (variations != null)
        {
            for (int i = 0,lim = variations.size(); i < lim; i++)
            {
                commonMove m = variations.elementAt(i);
                String str = menuMoveString(m);
                menu.addMenuItem(str,m);
            }
        }
        else
        {  String str = (next == null) ? "" : menuMoveString(next);
           if("".equals(str)) { str = "->"; } 
            {menu.addMenuItem( str,next);
            }
        }
    }
    
    //
    // this is the normal entry point, where no extra filtering or renumbering occurs.
    //
    public commonMove formHistoryTree(PrintStream os,boolean includeTimes)
    {	return(formHistoryTree(os,new TrueFilter(),-1,includeTimes));
    }
    //
    // the filter options is so Raj can remove ephemeral moves from the tree.  This is
    // a bit of an ad-hoc solution for now, to avoid duplicating or messing with an important
    // core algorithm.  This method is used to convert the move history into a string that
    // is then used by the server to define the complete state of the game.
    //
    public commonMove formHistoryTree(PrintStream os,MoveFilter filter,int prev_number,boolean includeTimes)
    {	// if filter says reNumber()=true and prev_number>=0 then then renumber the
    	// moves starting from prev_number+1. Move numbers are not terribly important
    	// for live games, but in review games, with variations, move numbers refer up
    	// the tree and cause branches.
    	// the motivation for renumbering is that for reconstruction of live games, all players
    	// must have identical game description stings, and uncertainties caused by simultaneous play
    	// tend stick in extra numbers and different numbers of extra numbers.
    	// Removing the ephemerals and renumbering restores a deterministic sequence.
        commonMove rval = this;
        commonMove current = this;
        boolean renumber = filter.reNumber() && (prev_number>=0);
        while (current != null)
        {	if(!filter.included(current)) { current = current.next; }
        	else
        	{
            String ms = current.moveString();

            if (ms.length() > 0)
            {
            	if(renumber)
            	{	//
            		// format is "nn move xx", replace "nn" with our own number
            		//
            		prev_number++;
            		ms = "" + prev_number+ ms.substring(ms.indexOf(' '));
            	}
            	os.print(" , ");
            	if(includeTimes)
            	{
            	long time = current.elapsedTime();
            	String name = current.getNodeName();
            	if(name!=null)
            		{ // this is the preferred form, but not compatible with old clients.
            		  // this should be used after version 9.03 is extinct
            		  // os.print("+N "); os.print(G.quote(name)); os.print(" "); 
            		  os.print("+"); 
            		  os.print(name);
            		  os.print(' ');
            		}
            	if(time>=0) { os.print("+T "); os.print(time); os.print(" "); }
            	}
                os.print(ms);
            }
            StackIterator<commonMove> variations = current.getVariations();
            if (variations!=null)
            {
                commonMove lastmove = null;

                for (int i = 0,lim = variations.size(); i < lim; i++)
                {
                    rval = variations.elementAt(i);
                    lastmove = rval.formHistoryTree(os,filter,prev_number,includeTimes);
                }
                commonMove nx = current.next;
                if ((nx != lastmove) && (lastmove != null))
                {	String ms2 = nx.moveString();
                	if(renumber)
                	{	
            		prev_number++;
            		ms2 = "" + prev_number+ ms2.substring(ms.indexOf(' '));
                	}
                   	os.print(" , ");
                   	if(includeTimes)
                   	{
                	long time = nx.elapsedTime();
                	if(time>=0) { os.print("+T "); os.print(time);os.print(" "); }
                   	}
                    os.print(ms2);

                }

                current = null;
            }
            else
            {
                current = current.next;
            }}
        }

        return (rval);
    }

    public void removeVariation(commonMove v)
    {
        if (next == v)
        {
            next = null;
        }
        StackIterator<commonMove> variations = getVariations();
        if (variations != null)
        {	StackIterator<commonMove> newvar = variations.remove(v);
        	
            if (next == null)
            {
                next = variations.elementAt(0);
            }
            setVariations(newvar);
            if (variations.size() == 1)
            {
                setVariations(null);
            }
        }
    }

    public void recordVariation(commonMove m)
    {	StackIterator<commonMove> variations = getVariations();
        if (variations == null)
        {
        	setVariations(m);
        }
        else
        {
        variations.insertElementAt(m,0);
        }
    }

    public commonMove addVariation(commonMove v)
    {
        if (v != null)
        {
            if (next == null)
            {
                next = v;
            }
            else if ((next == v) || v.Same_Move_P(next))
            { //nothing
            	v=next;
            }
            else
            {	StackIterator<commonMove> variations = getVariations();
                if (variations == null)
                { 
                	StackIterator<commonMove> vv = next;
                	next = v;
                	vv = vv.push(v);
                	setVariations(vv);
                }
                else
                {
                    next = v;

                    // look among the current variations
                    for (int i = 0,lim = variations.size(); i < lim; i++)
                    {
                        commonMove newv = variations.elementAt(i);

                        if ((v == newv) || v.Same_Move_P(newv))
                        {
                            next = newv; // make sure next is the variation we selected

                            return (newv);
                        }
                    }
                    setVariations(variations.push(v));
                }
            }
        }

        return (v);
    }
    /** 
     * 
     * @return a short, nonparseable representation of this move for use in the game log.
     */
    public String shortMoveString() { throw G.Error("This method should be overridden");  }
    
    /**
     * this gets the short description of a move, primarily for use in the game log
     *  
     * @return the short move string as a colorized Text object
     */
    public Text shortMoveText(commonCanvas v, Font font) { return(TextChunk.create(shortMoveString())); }
 
    /**
     * produce a move string for use in a variation selection menu
     * 
     * @param var
     * @return a string suitable for a variation choice menu
     */
    public String menuMoveString(commonMove var)
    {	String bas = "??";
    	// this used to be shortMoveString, but in the modern era where shortMoveSring can be replaced
    	// by shortMoveText, with mixed text and icons, we revert to using moveSting
    	String str = var.moveString();
    	if((str.length()>2) && (Character.isDigit(str.charAt(0))))
    	{
    		int idx = str.indexOf(' ');
    		if(idx>0) { str = str.substring(idx); }
    	}
    	StackIterator<commonMove>variations = getVariations();
    	if(variations!=null)
    	{	for(int i=0;i<variations.size();i++)
    		{
    		commonMove m = variations.elementAt(i);
    		if(m==var) { bas =""+(i+1)+">"; break; }
    		}
    	} else if (var==next) { bas = "1>"; }
       	if(var==next) { bas += ">"; }
        return(bas+" "+str);
    }
    /** produce a parseable move string.  this string must be parseable
     * to create a duplicate of the current move.
     * @return a parseable string representing the move
     */
    public abstract String moveString();
    
    

    /** add the standard moves that most games will need.  These mostly
     * correspond to the standard buttons on the user interface.
     * 
     * the current standard moves are "Done" "Edit" "Start" "Resign" "Undo" and
     * indexes 0 and 1 for P0 and P1
     * 
     * Note that there is no standard semantics or implementation for these moves;
     * the only effect here is to put them in the dictionary.
     * 
     * @param D
     */
    static public void add2PStandardMoves(ExtendedHashtable D,Object... more)
    {	D.addStringPairs(
    		DONE, MOVE_DONE,
        	EDIT, MOVE_EDIT,
        	START, MOVE_START,
        	RESIGN,MOVE_RESIGN,
        	NULLMOVE,MOVE_NULL,
 			P0,FIRST_PLAYER_INDEX,
			P1,SECOND_PLAYER_INDEX,
        	PASS,MOVE_PASS,
			SWAP,MOVE_SWAP,
			OFFERDRAW,MOVE_OFFER_DRAW,
			ACCEPTDRAW,MOVE_ACCEPT_DRAW,
			DECLINEDRAW,MOVE_DECLINE_DRAW,
			RESET, MOVE_RESET,
			UNDO, MOVE_UNDO,
			UNDO_REQUEST, MOVE_PLEASEUNDO,
			UNDO_DECLINE, MOVE_DONTUNDO,
			UNDO_ALLOW, MOVE_ALLOWUNDO,
			GAMEOVERONTIME, MOVE_GAMEOVERONTIME);
    	D.addStringPairs(more);
    }
    static public void addStandardMoves(ExtendedHashtable D,Object... pairs)
    {
    	add2PStandardMoves(D,pairs);
    }
    
    /**
     * this is the default method used to show "principle variations" from search
     * results. 
     * which can be used to trigger breakpoints.
     * @param msg
     */
    public void showPV(String msg)
    {
        commonMove pm = this;
        G.print(msg + pm.evaluation());

        while (pm != null)
        {
            G.print(pm 
            		//+ " #" + pm.search_clock 
            		+ " " +
                pm.local_evaluation());
            pm = pm.best_move();
        }

        G.print();
    }
    /* standard java method, so we can read moves easily while debugging */
    public String toString()
    {	UCTNode u = uctNode();
        return ("P" + player + "[" + moveString() + "]" 
        		    + (gameover()?"T":"")
        			+ ((u==null)?"":u.toString()));
    }
	public String[] gameEvents() {
		return null;
	}

	public StackIterator<commonMove>push(commonMove m)
	{	StackIterator<commonMove> news = new CommonMoveStack();
		news.push(this);
		news.push(m);
		return(news);
	}
	public StackIterator<commonMove>insertElementAt(commonMove m,int at)
	{
		StackIterator<commonMove> news = new CommonMoveStack();
		news.push(this);
		return news.insertElementAt(m,at);
	}

	// this is available for debugging, return true if this is a move "of interest"
	public boolean visit() { return(false); };
	public String indexString() { int ind = index();  return (ind<0 ? "" : ind+" "); }
	
	// this is used to keep some moves out of the game log
	public boolean ignoredInLogs()
	{	return op==MOVE_START;
	}
    public Text censoredMoveText(commonCanvas v,BoardProtocol b, Font font)
    {	// default treatment
    	return(shortMoveText(v,font));
    }
    
    // this is the traditional version used for 2p games
    public boolean isEvaluated() 
    {
    	return !Double.isNaN(evaluation());
    }
    // this is the modern version used for multiplayer games
    public boolean isUctScored() {
		return !Double.isNaN(evaluation());
	}
    /**
     * return set the index from the first token if it is a number
     * and return the next (ie; non index) token
     * @param msg
     * @return 
     */
    public String firstAfterIndex(Tokenizer msg)
    {
    	String cmd = msg.nextToken();
 
        if (Character.isDigit(cmd.charAt(0)))
        { // if the move starts with a digit, assume it is a sequence number
            setIndex(G.IntToken(cmd));
            cmd = msg.nextToken();
        }
        return cmd;
    }
}
