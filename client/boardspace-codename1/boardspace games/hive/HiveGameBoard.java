package hive;


import java.util.*;

import lib.*;
import lib.Random;
import online.game.*;


/**
 * HiveGameBoard knows all about the game of Hive, which is played
 * on a hexagonal board. It gets a lot of logistic support from 
 * common.hexBoard, which knows about the coordinate system.  
 * 
 * This class doesn't do any graphics or know about anything graphical, 
 * but it does know about states of the game that should be reflected 
 * in the graphics.
 * 
 *  The principle interface with the game viewer is the "Execute" method
 *  which processes moves.  Note that this
 *  
 *  In general, the state of the game is represented by the contents of the board,
 *  whose turn it is, and an explicit state variable.  All the transitions specified
 *  by moves are mediated by the state.  In general, my philosophy is to be extremely
 *  restrictive about what to allow in each state, and have a lot of tripwires to
 *  catch unexpected transitions.   We expect to be fed only legal moves, but mistakes
 *  will be made and it's good to have the maximum opportunity to catch the unexpected.
 *  
 * Note that none of this class shows through to the game controller.  It's purely
 * a private entity used by the viewer and the robot.
 * 
 * @author ddyer
 *
 */

class HiveGameBoard extends hexBoard<HiveCell> implements BoardProtocol,HiveConstants
{ 	
    // indexes into the balls array, usually called the rack
    static final HiveId[] chipPoolIndex = { HiveId.White_Bug_Pool, HiveId.Black_Bug_Pool };
    static final HiveId[] setupPoolIndex = { HiveId.White_Setup_Pool, HiveId.Black_Setup_Pool };
    // sequence numbers for the standard pieces.  Those with multiples get 1 2 3 etc.
	// those that are unique get no number indicated by -1
	
    /* the "external representation for the board is A1 B2 etc.  This internal representation is X,Y
       where adjacent X's are separated by 2.  This gives the board nice mathematical properties for
       calculating adjacency and connectivity. */
    static int[] HiveCols = { 25,24,23,22,21,20,19,18, 17, 16, 15, 14,13,12,11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 }; // these are indexes into the first ball in a column, ie B1 has index 2
    static int[] HiveNInCol =     { 26,26,26,26,26,
    								26,26,26,26,26,
    								26,26,26,26,26,
    								26,26,26,26,26,
    								26,26,26,26,26,26 }; // depth of columns, ie A has 4, B 5 etc.

    static final String[] HIVEGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

    // support for placement displays
    public int lastPlacement = 1;
    
	private HiveState unresign;
	private HiveState board_state;
	private HiveState undrawState;
	private int lastRealMoveNumber = 0;
	public HivePiece lastDroppedObject = null;
	public boolean robotCanOfferDraw = true;
	public boolean canOfferDraw()
	{ return((moveNumber-lastRealMoveNumber)<=1); 
	}
	private boolean setupAccepted = false;
	boolean swappedRacks = false;
	public HiveCell[] rackForPlayer(int n) { return(racks[swappedRacks?n^1:n]); }
	public HiveCell[] setupRackForPlayer(int n) { return(setupRacks[swappedRacks?n^1:n]); }
	public HivePiece playerQueen(int pl)
	{
		return((swappedRacks!=(pl==0)) ? HivePiece.WhiteQueen : HivePiece.BlackQueen);
	}
	public boolean colorsInverted() { return(swappedRacks!=(getColorMap()[0]!=0)); } 
	Hashtable<HivePiece,HiveCell>pieceLocation = new Hashtable<HivePiece,HiveCell>();
	CellStack occupiedCells = new CellStack();
	private boolean robotBoard = false;
	public HiveId playerColor(int p)
	{
		return((swappedRacks!=(p==0)) ? HiveId.White_Bug_Pool : HiveId.Black_Bug_Pool);
	}
	public int swappedPlayer(int p)
	{
		return(swappedRacks?nextPlayer[p]:p);
	}
	public boolean swappedWinForPlayer(int p)
	{
		return(WinForPlayerNow(p));
	}

	public HiveState getState() {return(board_state); }
	public void setState(HiveState st) 
	{ 	unresign = (st==HiveState.RESIGN_STATE)?board_state:null;
		board_state = st;
		if(!board_state.GameOver()) 
			{ AR.setValue(win,false); 	// make sure "win" is cleared
			}
	}
public CellStack animationStack = new CellStack();
public variation gamevariation = variation.hive;
	public Bitset<PieceType> pieceTypeIncluded; 
    public int numActivePieceTypes()
    {  return(pieceTypeIncluded.size());
    }
    //
    // private variables
    //
    private HiveCell setupRacks[][] = new HiveCell[2][HivePiece.NUMPIECETYPES];
    private HiveCell racks[][]=new HiveCell[2][HivePiece.NUMPIECETYPES];
     
    // intermediate states in the process of an unconfirmed move should
    // be represented explicitly, so unwinding is easy and reliable.
    public HivePiece pickedObject = null;
    public HiveCell pickedSource = null;
    public HiveCell droppedDest = null;
    private HiveCell stunned = null;	// piece stunned by being moved by an original pillbug, or moved at all with the new pillbug
    private HiveCell prestun = null;	// piece just moved, will be considered stunned next turn
    
    
    // temporary list of destination cells allocate as a resource for speed
    private HiveCell[][]tempDestResource = new HiveCell[6][];
    private int tempDestIndex=-1;
    public synchronized HiveCell []getTempDest() 
    	{ if(tempDestIndex>=0) { return(tempDestResource[tempDestIndex--]); }
    	  return(new HiveCell[20*6*2+10]);
    	}
    public synchronized void returnTempDest(HiveCell[]d) { tempDestResource[++tempDestIndex]=d; }
    

    // slither around the hive.  If nsteps>0, step n times before landing (spider)
    // call with 3 for spider, 1 for queen, 0 for ant
    // stops when count reaches 0, firstC is reached
    // avoids backtracking using prevC.
    // direction direction+1 is the first direction searched.  This is used to force
    // ants to take a "left hand walk" so they don't get confused by U shaped hives.
    private int slither(int nsteps,HiveCell firstC,HiveCell prevC,int direction,HiveCell source,HiveCell dests[],int startidx,int di,CellStack path,boolean onlyone)
    {	if(path!=null) { path.push(firstC); }
    	int val = slither_internal(source,true,nsteps,firstC,prevC,direction,source,dests,startidx,di,path,onlyone);
    	if(path!=null) { path.pop(); }
    	return(val);
    }
    private int slither_internal(
    		HiveCell origin,
    		boolean top,
    		int nsteps,
    		HiveCell firstC,
    		HiveCell prevC,
    		int direction,
    		HiveCell source,
    		HiveCell dests[],
    		int startidx,
    		int di,
    		CellStack path,
    		boolean onlyone)
    {	
		int len = source.geometry.n;
		boolean threestep = (nsteps>0);	// this is a spider (or queen) move
		nsteps--;
		//System.out.println("s "+source+direction);
		for(int i=1;i<=len;i++)
		{	int dir = (i+direction)%len;
			int prevdir = (i+direction-1+len)%len;
			int nextdir = (i+direction+1)%len;
			HiveCell c = source.exitTo(dir);
			HiveCell prevc = source.exitTo(prevdir);
			HiveCell nextc = source.exitTo(nextdir);
			if(top) { sweep_counter++; }
			if( (c!=firstC)			// not looped around
				&& (c!=prevC)		// not backtracking
				&& (c!=origin)		// not all the way back
				&& ((threestep || (c.sweep_counter!=sweep_counter))	// not already seen (for ant moves)
					)
				&& (c.height()==0) 	// not occupied
				&& ((prevc==origin)||(nextc==origin)||(prevc.height()==0)||(nextc.height()==0))	// not a closed gate
				&& adjacentCell(c,source,origin)) // still part of the hive
				{ c.sweep_counter=sweep_counter;
				  if(path!=null) { path.push(c); }
				  if(nsteps<=0) 
				  { di = addDest(dests,c,startidx,di,false,path); 
					if(onlyone) { return(di); }
				  }
				  if(nsteps!=0) 
				  { di=slither_internal(origin,false,nsteps,firstC,source,((dir+len/2)%len),c,dests,startidx,di,path,onlyone); 
					if(onlyone && (di>0)) { return(di); }
				  }
				  if(path!=null) { path.pop(); }
				}
		}
		return(di);
    }

    // 
    // any blank cell that is adjacent to any cell,
    // and also any cell
    //
    private int slitherAnywhere(HiveCell source,HiveCell dests[],int destidx,CellStack path,boolean onlyone)
    {	int n=0;
		int tilesOnBoard = 0;
		int sweep = ++sweep_counter;
		for(int i=0,lim=occupiedCells.size();i<lim;i++)
		{	HiveCell c = occupiedCells.elementAt(i);
			tilesOnBoard++;
			for(int dir=0;dir<CELL_FULL_TURN;dir++)
			{ HiveCell ca = c.exitTo(dir);
			  if(ca.sweep_counter!=sweep && (ca!=source) && ca.adjacentCell(ca,source))
			  	{ ca.sweep_counter=sweep; 
			  	  if(path!=null) 
			  	  	{ path.push(source);
			  	  	  path.push(ca);
			  	  	}
			  	  	n = addDest(dests,ca,0,n,false,path);
					if(onlyone) { return(n); }
			  	  	if(path!=null)
			  	  	{	path.pop();
			  	  		path.pop();
			  	  	}
			  	}
			}
		}
		if(tilesOnBoard==0) 
			{ //first move
			HiveCell cell =  getCell((char)('A'+(ncols/2)),ncols/2);
			if(path!=null)
			{
			path.push(source);
			path.push(cell);
			}
			n = addDest(dests,cell,0,n,false,path);
			if(onlyone) { return(n); }
			if(path!=null)
			{
			path.pop();
			path.pop();
			}
			}
		return(n);
    }
    // true if this cell is a legal place to drop a new piece
    // for the player whose turn it is.
    private boolean legalDropDest(HiveCell cell,HivePiece obj)
    {	// dropping in a new piece
    	if(obj==null) { return false; }
    	if(obj.type==PieceType.BLANK) { return(true); }
		if(cell.height()>0) { return(false); }
		if(cell.nOtherColorAdjacent(obj.color)>0) { return(occupiedCells.size()==1); }
		if(cell.nOwnColorAdjacent(obj.color)>0) { return(true); }
		if(occupiedCells.size()==0) { return(true); }
		return(false);
    }
    
    private int legalDropDests(HiveCell dests[],HivePiece p,boolean onlyone)
    {	int n=0;
     	int sweep = ++sweep_counter;
     	int sz = occupiedCells.size();
     	switch(sz)
     	{
     	case 0:	// first move
     		dests[n++] = getCell((char)('A'+(ncols/2)),ncols/2); 
     		break;
     	case 1:	// second move
     		{
     		HiveCell center = occupiedCells.elementAt(0);
     		dests[n++] = center.exitTo(0);	// only need to try one spot
     		}
     		break;
     	default:
	     		
	   		for(int i=0,lim=occupiedCells.size();i<lim;i++)
			{	HiveCell c = occupiedCells.elementAt(i);
				for(int dir=0;dir<CELL_FULL_TURN;dir++)
					{ HiveCell ca = c.exitTo(dir);
					  if((ca.sweep_counter!=sweep) && legalDropDest(ca,p)) 
					  	{ ca.sweep_counter=sweep; dests[n++]=ca;
					  	  if(onlyone) { return(n); }
					  	}
					}
			}
	   		break;
     	}
     	return(n);
    }
    
    // return true if exit from source is a "gate", ie a barrier of taller pieces.
    private boolean isGate(HiveCell source,boolean picked,HiveCell dest,int dir)
    {	int srcH = source.height()+(picked?1:0);
    	HiveCell prev = source.exitTo(dir-1);
    	int prevH = prev.height();
    	if(prevH<=srcH) { return(false); }
    	HiveCell next = source.exitTo(dir+1);
    	int nextH = next.height();
    	if(nextH<=srcH) { return(false); }
    	int destH = dest.height();
    	if((destH>=prevH)||(destH>=nextH)) { return(false); }
    	return(true);
    }

    //
    // add a destination to the destination stack, but only if it is new.
    //
    Hashtable<HiveCell,HiveCell[]> pathHash = new Hashtable<HiveCell,HiveCell[]>();
    
    private int addDest(HiveCell dests[],HiveCell newdest,int startidx,int idx,boolean pill,CellStack path)
    {	
    	for(int si = startidx;(si<idx);si++)
		{ if(dests[si]==newdest) 
			{ 
			if(path!=null && (newdest.pillbug_dest == false))
			{
				HiveCell oldpath[] = pathHash.get(newdest);
				HiveCell newpath[] = path.toArray();
				if((newpath!=null) && ((oldpath==null) || newpath.length<oldpath.length))
				{
					pathHash.put(newdest,newpath);
				}
			}
			return(idx); 
			}
		}
    	dests[idx++] = newdest;
    	if(path!=null)
    	{
    		pathHash.put(newdest,path.toArray());
    	}
    	newdest.pillbug_dest = pill;
    	return(idx);
    }
    private int addPillbugFlips(HiveCell dests[],int startidx,int idx,HiveCell source,boolean picked,HiveCell pill,CellStack path,int srcDir,boolean onlyone)
    {
		// move to any empty space adjacent to the pillbug
    	if(isGate(source,picked,pill,srcDir) )
    	{ return(idx); 	// can't pull through the gate to pick up, either
    	}
		for(int dir=0; dir<CELL_FULL_TURN; dir++)
		{
			HiveCell adj = pill.exitTo(dir);
			if((adj!=source) 
				&& (adj.topChip()==null)
				&& !isGate(pill,false,adj,dir)	// can't fling through a gate
					)
			{	
				idx = addDest(dests,adj,startidx,idx,true,path);
				if(onlyone) { return(idx); }
			}
		}
		return(idx);
    }

    //
    // add pillbug flip moves for some random piece.
    //
    private int legalPillbugDests(HiveCell source,HiveId forColor,boolean picked,HivePiece moving,HiveCell dests[],int startidx,int idx,CellStack path,boolean onlyone)
    {   
    	if((pieceTypeIncluded.test(PieceType.PILLBUG)
    			|| pieceTypeIncluded.test(PieceType.ORIGINAL_PILLBUG))
    	        && ((source.height()+((pickedObject!=null)?1:0))==1)			// pieces on top can't be flipped
    			&& validHive(source))	// pieces that would break the hive can't be flipped
    	{
      		for(int dir = 0; dir<CELL_FULL_TURN; dir++)
        		{
        			HiveCell adj = source.exitTo(dir);
        			if(adj!=stunned)
        			{
        			HivePiece top = adj.topChip();
        			if((top!=null)
        				&& (top.color==forColor)
        				&& (adj!=stunned))
        			{	
        				if( top.isPillbug()	// new pillbug can't re-flip the piece that moved
       						|| ((top.type==PieceType.MOSQUITO)
       								&& (adj.height()==1)
        								// note that if we picked up a pillbug next to a mosquito
        								// the pillbug still gives its power to permit itself to be moved!
        								&& ( HivePiece.isPillbug(moving)
        										|| adj.isAdjacentToPillbug())))
        					{ 
        					if(!isGate(source,picked,adj,dir))
        					{	// can't flip through a gate
        					idx = addPillbugFlips(dests,startidx,idx,source,picked,adj,path,dir,onlyone);
        					if(onlyone && idx>0) { return(idx); }
        					}
        					}
        			}}
        		}
        	}
     	return(idx);
    }
    //
    // legal destinations for source as if it contained a piece of specified type.
    // the source cell will already be picked.  This is used for mosquito to add
    // moves for all the acquired movement types.  For pillbugs, this adds only
    // the normal pillbug moves, not the flip moves.
    //
    private int legalDestsForType(HiveCell source,boolean picked,PieceType type,HiveCell dests[],int startidx,int idx,CellStack path,boolean onlyone)
    {	
    	switch(type)
    	{
     	case ORIGINAL_PILLBUG:
    		// regular pillbug moves up 1, step 0, down 1
    		{
    		if(!validHive(source)) { return(idx); }	// can't violate the hive rule
			for(int dir1=0;dir1<CELL_FULL_TURN;dir1++)
			{	HiveCell up1 = source.exitTo(dir1);
				if((up1!=null)
						&&(up1.height()>0) // must hop up something
						&&(!isGate(source,picked,up1,dir1))
						)
						{	for(int dir3=0; dir3<CELL_FULL_TURN;dir3++)
							{	HiveCell down = up1.exitTo(dir3);
								if((down!=null) 
										&& (down.height()==0)
										&& (!isGate(up1,picked,down,dir3))
										&& (down!=source))
								{	idx = addDest(dests,down,startidx,idx,false,path);
									if(onlyone) { return(idx); }
								}
					}
				}
			}
			return(idx);
			}
    	case LADYBUG:
    		// up 1, step 1, down 1
    		{
    		if(!validHive(source)) { return(idx); }	// can't violate the hive rule
			for(int dir1=0;dir1<CELL_FULL_TURN;dir1++)
			{	HiveCell up1 = source.exitTo(dir1);
				if((up1!=null)
						&&(up1.height()>0) // must hop up something
						&&(!isGate(source,picked,up1,dir1))
						)
				{	for(int dir2=0; dir2<CELL_FULL_TURN;dir2++)
					{	HiveCell up2 = up1.exitTo(dir2);
						if((up2!=null) 
								&& (up2.height()>0)
								&& (up2!=source)
								&& (!isGate(up1,false,up2,dir2))
								) 	// still up?
						{	for(int dir3=0; dir3<CELL_FULL_TURN;dir3++)
							{	HiveCell down = up2.exitTo(dir3);
								if((down!=null) 
										&& (down.height()==0)
										&& (!isGate(up2,false,down,dir3))
										&& (down!=source))
								{	idx = addDest(dests,down,startidx,idx,false,path);
									if(onlyone) { return(idx); }
								}
							}
						}
					}
				}
			}
    		return(idx);
    		}
    	case PILLBUG:	// new model pillbug moves like a Q
		case QUEEN:
		{	// queen moves by sliding one cell in any direction to an empty space
			if(!validHive(source)) { return(idx); }
			return(slither(1,source,null,0,source,dests,startidx,idx,path,onlyone));
	 	}
		case BEETLE:
	   	{	// beetle moves by sliding one cell in any direction to
			// any kind of space, except it is limited by pincer blocks
			if(!validHive(source)) 
				{ //picking a beetle off the base level, still have to worry about the hive rule
				return(idx); 
				}
			int len = source.geometry.n;
			int di = idx;
			int myheight = source.height()-(picked?0:1);
			for(int i=0;i<len;i++)
			{	HiveCell c = source.exitTo(i);
				int ch = c.height();
				if((ch<myheight)||(ch>0)|| adjacentCell(c,source,null))
				{	// must be either an occupied cell or a cell you can slide into
					// either the previous or next cell must also be on the same level
					HiveCell nextC = source.exitTo(i+1);
					int nextch = nextC.height();
	 				if((nextch<=ch)||(nextch<=myheight)) 
	 					{ di = addDest(dests,c,startidx,di,false,path); 
	 					  if(onlyone) { return(di); }
	 					}
					else
						{
						HiveCell prevC = source.exitTo(i+len-1);
						int prevch = prevC.height();
						if((prevch<=ch)||(prevch<=myheight)) 
							{ di = addDest(dests,c,startidx,di,false,path);
							  if(onlyone) { return(di); }
							}
						}
				}
			}
			return(di);
		}
			
		case GRASSHOPPER:
		{	int di=idx;
			if(!validHive(source)) { return(idx); }	// can't violate the hive rule
			// grasshoppers can't be penned in, so don't check for a pinned cell
			for(int direction=0;direction<CELL_FULL_TURN;direction++)
			{	HiveCell c = source.exitTo(direction);
				if((c!=null)&&(c.height()>0)) // must hop over something
				{	while((c!=null)&&(c.height()>0)) { c=c.exitTo(direction); }
					if(c!=null) 
					{ di = addDest(dests,c,startidx,di,false,path);
						if(onlyone) { return(di); }
					}
				}
			}
			return(di);
		}
		
		case SPIDER:
		{
	    	if(!validHive(source)) { return(idx); }	// can't violate the hive rule
	   		return(slither(3,source,null,0,source,dests,startidx,idx,path,onlyone));
		}
	   	case BLANK:
			/* blanks can move anywhere on or adjacent to the hive, subject to the hive rule of course */
	    	if(!validHive(source)) { return(idx); }	// can't violate the hive rule
	    	return(slitherAnywhere(source,dests,startidx,path,onlyone));
		
		case ANT:
		{
	    	if(!validHive(source)) { return(idx); }	// can't violate the hive rule
	   		return(slither(0,source,null,0,source,dests,startidx,idx,path,onlyone));
	   	}
		case MOSQUITO:
			if((source.height()+(picked?1:0))>1)
			{	// mostquito acting as beetle
				return(legalDestsForType(source,picked,PieceType.BEETLE,dests,startidx,idx,path,onlyone));
			}
			{
			// otherwise, mosquito accumulates the moves of everything it touches
			for(int dir=0;dir<CELL_FULL_TURN;dir++)
			{	HiveCell c = source.exitTo(dir);
				HivePiece p = c.topChip();
				if(p!=null)
				{
				switch(p.type)
				{	case MOSQUITO:	// no moves picked up from other mosquitos	
							break;
					case ANT: 
					case SPIDER:
					case GRASSHOPPER: 
					case QUEEN: 
					case BEETLE: 
					case LADYBUG: 
					case PILLBUG:
					case BLANK:
					case ORIGINAL_PILLBUG: idx = legalDestsForType(source,picked,p.type,dests,startidx,idx,path,onlyone); break;
					default: throw G.Error("not expecting type %s",p.type);
				}
				}}
			}
			return(idx);
	   	default: throw  G.Error("Not expecting type %s",type);
	}
	
 }
    //
    // note that this has to work correctly both when "picked" has been picked
    // up, and when it is still sitting on top of "source"
    //
    int legalDests(HiveCell source,boolean picked,HivePiece top,HiveCell dests[],CellStack path,int who,boolean onlyone)
    {	if((source==stunned)&&(who==whoseTurn)) { return(0); }
    	if(top==null) { top = source.topChip(); }
    	G.Assert(source.onBoard,"cell on board");
    	HiveId targetColor = playerColor(who);
    	// pillbug dests take precedence, so do these first
    	int idx = legalPillbugDests(source,targetColor,picked,pickedObject,dests,0,0,path,onlyone);
    	int idx2 = ((top!=null)&&(top.color==targetColor))
    					?legalDestsForType(source,picked,top.type,dests,0,idx,path,onlyone) 
    					: idx;
    	return(idx2);
    }
	
	// factory method
	public HiveCell newcell(char c,int r)
	{	return(new HiveCell(c,r));
	}
	private void makepiece(int pl,PieceType typ,int seq)
	{	// create a piece and place it in the rack.  Create cells for the rack.
		HiveCell c = racks[pl][typ.ordinal()];
		addChip(c,HivePiece.findPiece(chipPoolIndex[pl], typ, seq,false));
	}
	public HiveGameBoard(String init,int map[]) // default constructor
    {
        drawing_style = DrawingStyle.STYLE_CELL;//STYLE_NOTHING; // don't draw the cells.  STYLE_CELL to draw them
        Grid_Style = HIVEGRIDSTYLE;
        isTorus=true;
        Random r = new Random(232534345);
        setColorMap(map);
        // create the rack
        for(int i=0;i<racks.length;i++)
        { HiveCell row[]=racks[i];
          for(int j=0;j<row.length;j++) 
          	{ row[j] = new HiveCell(chipPoolIndex[i],(char)('a'+i),j,r.nextLong());
			}
        }
        // create the setup rack
        for(int pl=0;pl<setupRacks.length;pl++)
        { HiveCell row[]=setupRacks[pl];
          for(int j=0;j<row.length;j++) 
          	{ row[j] = new HiveCell(setupPoolIndex[pl],(char)('a'+pl),j,r.nextLong());
 			}
        }
        int[] firstcol = null;
    	int[] ncol = null;
    	firstcol = HiveCols; 
    	ncol = HiveNInCol;
        Random r2 = new Random(69942354);
        initBoard(firstcol, ncol, null); //this sets up a hexagonal board
        allCells.setDigestChain(r2);
        
        doInit(init); // do the initialization 
    }

    public HiveGameBoard cloneBoard() 
	{ HiveGameBoard dup = new HiveGameBoard(gametype,getColorMap()); 
	  dup.copyFrom(this);
	  return(dup); 
   	}
    public void copyFrom(BoardProtocol b) { copyFrom((HiveGameBoard)b); }
    public void SetDrawState() 
    	{ setState(HiveState.DRAW_STATE); }
    
    public void reInit(HiveCell[] c)
    {	for(HiveCell d : c) { if(d!=null) { d.reInit(); }}
    }
    public void reInit(HiveCell[][] cells)
    {	for(HiveCell c[] : cells) { reInit(c); }
    }
 
    // standared init for Hex.  Presumably there could be different
    // initializations for variation games.
    private void Init_Standard(String game)
    {	
    	pieceLocation.clear();
    	occupiedCells.clear();
    	variation var = variation.find_variation(game);
    	if(var!=null)
    		{ pieceTypeIncluded = var.included;
    		}
    	else { throw G.Error(WrongInitError,game); }
        gametype = game;
        gamevariation = var;
        Hivemovespec.shortenUniqueNames = (var!=variation.hive_u);
        setState(HiveState.PUZZLE_STATE);
        // clear the rack
        reInit(racks);
        reInit(setupRacks);
        for(HiveCell c = allCells; c!=null; c=c.next) { c.reInit(); }
        robotBoard = false;
        whoseTurn = FIRST_PLAYER_INDEX;
        droppedDest = null;
        pickedSource = null;
        pickedObject = null;
        lastPlacement = 1;
    }

    public void sameboard(BoardProtocol f) { sameboard((HiveGameBoard)f); }
    /**
     * Robots use this to verify a copy of a board.  If the copy method is
     * implemented correctly, there should never be a problem.  This is mainly
     * a bug trap to see if BOTH the copy and sameboard methods agree.
     * @param from_b
     */
    public void sameboard(HiveGameBoard from_b)
    {
        super.sameboard(from_b); 
        G.Assert(swappedRacks==from_b.swappedRacks,"swappedRacks mismatch");
        G.Assert(sameCells(racks,from_b.racks),"rack cells mismatch");
        G.Assert(sameCells(setupRacks,from_b.setupRacks),"setuprack cells mismatch");
        G.Assert(setupAccepted==from_b.setupAccepted,"setupAccepted mismatch");
        //G.Assert(sameCells(stunned,from_b.stunned),"stunned piece mismatch");
        G.Assert(pickedObject==from_b.pickedObject, "pickedObject matches");
        G.Assert(lastPlacement==from_b.lastPlacement,"lastPlacement mismatch");

    }

    /** this is used in fraud detection to see if the same game is being played
     * over and over. Each game in the database contains a digest of the final
     * state of the game.  Other site machinery looks for duplicate digests.
     * @return
     */
    public long Digest()
    { 
        Random r = new Random(64 * 1000); // init the random number generator
    	long v = 0;
        // the basic digestion technique is to xor a bunch of random numbers. The key
        // trick is to always generate exactly the same sequence of random numbers, and
        // xor some subset of them.  Note that if you tweak this, all the existing
        // digests are invalidated.
        //
        v ^= Digest(r,racks);
        v ^= Digest(r,setupRacks);
        for(int i=0,lim=occupiedCells.size();i<lim; i++)
		{	
			HiveCell c = occupiedCells.elementAt(i);
				{	v ^= c.Digest(r);
				}
			
		}
        
        {
        long d1 = r.nextLong();
        long d2 = r.nextLong();
        long d3 = r.nextLong();

        if(swappedRacks) { v ^=d3; }
        // note for hive cells, all the Digest is the same, but for pick/unpick we want the exact identity.
        v ^= (pickedSource!=null) ? d1+pickedSource.randomv :  d2; 
        }
		v ^= chip.Digest(r,pickedObject);
		//v ^= Digest(r,stunned);
		v ^= r.nextLong()*(board_state.ordinal()*10+whoseTurn);
      return (v);
    }
    

    public HiveCell getCell(HiveCell c)
    {
    	if(c==null) { return(null); }
    	return(getCell(c.rackLocation(),c.col,c.row));
    }
    public HiveCell getCell(HiveId dest, char col, int row)
    {
    	switch(dest)
    	{
          default: throw G.Error("Not expecting dest %s",dest);
          case BoardLocation: // an already filled board slot.
          	return(getCell(col,row));
          case Black_Bug_Pool:		// back in the pool
          	return(racks[SECOND_PLAYER_INDEX][row]);
          case White_Bug_Pool:		// back in the pool
          	return(racks[FIRST_PLAYER_INDEX][row]);
          case Black_Setup_Pool:
           	return(setupRacks[SECOND_PLAYER_INDEX][row]);      	  
          case White_Setup_Pool:
           	return(setupRacks[FIRST_PLAYER_INDEX][row]);
        	  
    	}
    }

    /* make a copy of a board.  This is used by the robot to get a copy
     * of the board for it to manupulate and analyze without affecting 
     * the board that is being displayed.
     *  */
    public void copyFrom(HiveGameBoard from_b)
    {
        super.copyFrom(from_b);
        getCell(occupiedCells,from_b.occupiedCells);
        pieceLocation.clear();
        for(Enumeration<HivePiece> e = from_b.pieceLocation.keys(); e.hasMoreElements(); )
        {	HivePiece key = e.nextElement();
        	HiveCell loc = from_b.pieceLocation.get(key);
        	pieceLocation.put(key, getCell(loc));
        }
        pickedObject = from_b.pickedObject;
        pickedSource = getCell(from_b.pickedSource);
        droppedDest = getCell(from_b.droppedDest);
        setupAccepted = from_b.setupAccepted;
        swappedRacks = from_b.swappedRacks;
        robotBoard = from_b.robotBoard;
        lastRealMoveNumber = from_b.lastRealMoveNumber;
        stunned = getCell(from_b.stunned);
        prestun = getCell(from_b.prestun);
        copyFrom(racks,from_b.racks);
        copyFrom(setupRacks,from_b.setupRacks);
        board_state = from_b.board_state;
        unresign = from_b.unresign;
        undrawState = from_b.undrawState;
        pickedObject = from_b.pickedObject;
        lastPlacement = from_b.lastPlacement;
        sameboard(from_b); 
    }

    HivePiece nextFreeChip(HiveId pl, PieceType p)
    {	for(int i=1; i<=6;i++)
    	{	HivePiece ch = HivePiece.findPiece(pl,p,i,true);
    		if((pickedObject!=ch) && (pieceLocation.get(ch)==null)) { return(ch); }
    	}
    	return(null);
    }

    /* initialize a board back to initial empty state */
    public void doInit(String gtype,long key)
    {  randomKey = key;
       Init_Standard(gtype);
       for(int pl=FIRST_PLAYER_INDEX; pl<=SECOND_PLAYER_INDEX;pl++)
       { for(int i=HivePiece.StartingPieceTypes.length-2; i>=0; i--)	// deliberately skip the blank
       		{ makepiece(pl,HivePiece.StartingPieceTypes[i],HivePiece.StartingPieceSeq[i]);
        	}
        
       	HiveId targetColor = chipPoolIndex[pl];
       	if(gamevariation==variation.hive_u)
       		{ //populate the setup rack
       		  HiveCell row[] = setupRacks[pl];
       		  for(PieceType pt : HivePiece.SetupPieceTypes)
       		  {
       		  HivePiece p = HivePiece.getCanonicalChip(targetColor,pt);
	      	  if(p!=null)
	      	  {
	      		  HivePiece ch = nextFreeChip(targetColor,p.type);
	      		  if(ch!=null) { addChip(row[pt.ordinal()],ch); }	// not all chiptypes exist
	      	  }}
              setupAccepted = false;
       		}
       	else { // in other variations, the setup is fixed.
       		setupAccepted = true; 
       }
       
       }
       animationStack.clear();
       droppedDest=null;
       pickedSource=null;
       stunned = null;
       prestun = null;
       pickedObject=null;
       swappedRacks = getColorMap()[0]!=0;
       moveNumber = 1;
       lastRealMoveNumber = 1;
       setState(HiveState.PUZZLE_STATE);
        // note that firstPlayer is NOT initialized here
    }



    //
    // change whose turn it is, increment the current move number
    //
    public void setNextPlayer(replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Move not complete, can't change the current player");
        case PUZZLE_STATE:
            break;
       case Swap:
        	moveNumber++;
        	setWhoseTurn((playerColor(whoseTurn)==HiveId.White_Bug_Pool) ? whoseTurn : nextPlayer[whoseTurn]);
        	break;
        case GAMEOVER_STATE:
        	// this shouldn't happen, but in some old archives it does.
        	if(replay==replayMode.Live) { G.Error("Move not complete, can't change the current player"); }
			//$FALL-THROUGH$
        case Setup:
        case CONFIRM_STATE:
        case PASS_STATE:
        case DRAW_STATE:
        case RESIGN_STATE:
        case DeclinePending:
        case DrawPending:
            moveNumber++; //the move is complete in these states
            setWhoseTurn(nextPlayer[whoseTurn]);
            return;
        }
    }

    /** this is used to determine if the "Done" button in the UI is live
     *
     * @return
     */
    public boolean DoneState()
    {	
        switch (board_state)
        {
        case RESIGN_STATE:
        case CONFIRM_STATE:
        case PASS_STATE:
        case DRAW_STATE:
        case Setup:
        case Swap:
        case DrawPending:
        case AcceptPending:
        case DeclinePending:
            return (true);

        default:
            return (false);
        }
    }
    public boolean DigestState()
    {	//if(board_state==HiveState.PASS_STATE) { return(false); }
    	return(DoneState());
    }
 /**
  * In our implementation, the letter side(a-k) is black
  * and the number side (1-11) is white.  Either player can be playing either color.
  * @param ind
  * @return
  */ 

    public void setGameOver(boolean winCurrent,boolean winNext)
    {	if(winCurrent && winNext) { winCurrent=winNext=false; }	// simultaneous win is a draw
    	win[whoseTurn]=winCurrent;
    	win[nextPlayer[whoseTurn]]=winNext;
    	setState(HiveState.GAMEOVER_STATE);
    }
     // this method is also called by the robot to get the blobs as a side effect
    public boolean WinForPlayerNow(int player)
    {	if(board_state==HiveState.GAMEOVER_STATE) { return(win[player]); }
    	HiveCell location = pieceLocation.get(playerQueen(nextPlayer[player]));
    	if(location!=null && location.onBoard)
    	{	return(location.isSurrounded());
    	}
    	return(false);
   	
    }



    //
    // return true if balls[rack][ball] should be selectable, meaning
    // we can pick up a ball or drop a ball there.  movingBallColor is 
    // the ball we would drop, or -1 if we want to pick up
    //
    public void acceptPlacement()
    {	if(droppedDest!=null)
    		{
        	pickedObject = null;
        	droppedDest = null;
        	pickedSource = null;
    		}
    	switch(board_state)
    	{
    	case DrawPending:
    	case DeclinePending:	// the accept/decline draw shouldn't affect the stun status
    		break;
    	default:
    	stunned = prestun;
    	prestun = null;
    		break;
    	}
    	undrawState = null;
    }
    //
    // undo the drop, restore the moving object to moving status.
    //
    private void unDropObject()
    {
        HiveCell dr = droppedDest;
        if(dr!=null)
        	{
        	droppedDest = null;
        	prestun = null;
         	pickedObject = unaddChip(dr);
         	if(pickedSource!=null)
        	{	HiveCell ps = pickedSource;
        		unPickObject();
        		pickObject(ps);
        	}
        	}

    }
    // 
    // undo the pick, getting back to base state for the move
    //
    private void unPickObject()
    {	HivePiece po = pickedObject;
    	if(po!=null)
    	{
    	HiveCell ps = pickedSource;
    	pickedSource=null;
    	pickedObject = null;
    	unremoveChip(ps,po);
    	}
     }
    
    private int previousLastFilled = 0;
    private int previousLastEmptied = 0;
    private int previousLastMover = 0;
    void addChip(HiveCell c,HivePiece p)
    {	if(c.onBoard)
    {	
    		if(c.topChip()==null) { occupiedCells.push(c);  }
    		c.lastContents = p;
    		previousLastFilled = c.lastFilled;
     		c.lastFilled = lastPlacement;
       		lastPlacement++;
    	}
    	else { addChipOffboard(c); }
    	
        c.addChip(p);
    	pieceLocation.put(p, c);
    }
    void addChipOffboard(HiveCell c)
    {
        switch(c.rackLocation())
        {
        case Black_Setup_Pool:
        case White_Setup_Pool:
     	   HivePiece oldPiece = c.topChip();
     	   if(oldPiece!=null)
     	   {	removeChip(c);
     	   }
     	   break;
        default:
     	  break;
        }  
    }
    void unremoveChip(HiveCell c,HivePiece p)
    	{	G.Assert(p!=null,"something placed");
    	if(c.onBoard)
    	{	
    		if(c.topChip()==null) { occupiedCells.push(c);  }
    		c.lastContents = p;
    		c.lastEmptied = previousLastEmptied;
    		lastPlacement++;
    	}
    	else { addChipOffboard(c); }
    
        c.addChip(p);
    	pieceLocation.put(p, c);
    }
    HivePiece removeChip(HiveCell c)
    {	HivePiece p = c.removeTop();
    	if(c.onBoard)
    	{
    	if(c.topChip()==null) { occupiedCells.remove(c, false); }
    	previousLastEmptied = c.lastEmptied;
    	c.lastEmptied = lastPlacement;
    	c.lastMover = whoseTurn;
    	}
    	pieceLocation.remove(p);
    	return(p);
    }
    HivePiece unaddChip(HiveCell c)
    {	HivePiece p = c.removeTop();
    	if(c.onBoard)
    	{
    	G.Assert(p!=null,"something removed");
    	if(c.topChip()==null) { occupiedCells.remove(c, false); }
    	c.lastFilled = previousLastFilled;
    	c.lastMover = previousLastMover;
    	lastPlacement--;
    	}
    	pieceLocation.remove(p);
    	return(p);
    }
    // 
    // drop the floating object.
    //
    private void dropObject(HiveCell dest)
    {  HivePiece po = pickedObject;
       G.Assert((po!=null)&&(droppedDest==null),"ready to drop");
       droppedDest = dest;
       pickedObject = null;
       lastDroppedObject = po;
       addChip(dest,po);
       
    }   
    //
    // true if col,row is the place where something was dropped and not yet confirmed.
    // this is used to mark the one square where you can pick up a marker.
    //
    public boolean isDest(HiveCell cell)
    {	return(droppedDest==cell);
    }
    
	//get the index in the image array corresponding to movingObjectChar 
    // or HitNoWhere if no moving object.  This is used to determine what
    // to draw when tracking the mouse.
    public int movingObjectIndex()
    {	HivePiece ch = pickedObject;
    	if((ch!=null)&&(droppedDest==null))
    		{ return(1);
    		}
        return (NothingMoving);
    }
    public PieceType movingObjectType()
    {	return((pickedObject!=null)?pickedObject.type:null);
    }
    // This is a service routine for the displayer.  Return
    // a hash array of cells where the currently moving insect
    // could land.
    //
    public Hashtable<HiveCell,HiveCell> movingObjectDests()
    {	Hashtable<HiveCell,HiveCell> dd = new Hashtable<HiveCell,HiveCell>();
    	if(movingObjectIndex()>=0)
    	{HiveCell tempDests[]=getTempDest();
    		if(pickedSource.onBoard)
    		{ 
    		  int dests = legalDests(pickedSource,true,pickedObject,tempDests,null,whoseTurn,false);
    		  for(int i=0;i<dests;i++) 
    		  { dd.put(tempDests[i],tempDests[i]); 
    		  }
    		}
    		else
    		{ 
    	    int nn = (pickedObject.type==PieceType.BLANK) 
    	    		? slitherAnywhere(null,tempDests,0,null,false) 
    	    		: legalDropDests(tempDests,pickedObject,false);
    	    for(int i=0;i<nn;i++) 
    	    { dd.put(tempDests[i],tempDests[i]); 
    	    }
    		}
    	    returnTempDest(tempDests);
    	}
    return(dd);
    }
    
    private commonMove currentMove = null;
    private final void pickObject(HiveCell c)
    {  	G.Assert((pickedObject==null)&&(pickedSource==null),"not ready to pick");
    	G.Assert((board_state==HiveState.PUZZLE_STATE) 
    				|| permissiveReplay	// special flag that we're in a replayed game
    				|| (board_state==HiveState.Setup) 
    				|| (c!=stunned),"picking stunned piece %s move %s",c,currentMove);
    	pickedSource = c;
    	switch(c.rackLocation())
    	{
    	case Black_Setup_Pool:
    	case White_Setup_Pool:
    		HivePiece oldpiece = pickedObject = removeChip(c);
    		HivePiece newpiece = nextFreeChip(oldpiece.color,oldpiece.type);
    		if(newpiece!=null) { addChip(c,newpiece); }
    		break;
    	default:
    		pickedObject = removeChip(c);
    		break;
    	}
    	
    }
	// pick something up.  Note that when the something is the board,
    // the board location really becomes empty, and we depend on unPickObject
    // to replace the original contents if the pick is cancelled.
    private void pickObject(HiveId source, char col, int row)
    {	
    	HiveCell c = getCell(source,col,row);
    	pickObject(c);
    }
    //	
    //true if col,row is the place where something was picked up.  This is used
    // by the board display to provide a visual marker where the floating chip came from.
    //
    public boolean isSource(HiveCell c)
    {
        return (pickedSource==c);
    }
    //
    // in the actual game, picks are optional; allowed but redundant.
    //

    private void setNextStateAfterDrop(replayMode replay)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting next after drop in state %s", board_state);
        case GAMEOVER_STATE:
        case PASS_STATE:
        	// this shouldn't happen, but in some old archives it does.
        	if(replay==replayMode.Live) { G.Error("Not expecting next after drop in state %s",board_state); }
        	break;
        case DRAW_STATE:
        case FIRST_PLAY_STATE:
        case PLAY_STATE:
        case QUEEN_PLAY_STATE:
        	setState(HiveState.CONFIRM_STATE);
        	break;
        case Setup:
        case PUZZLE_STATE:
        	acceptPlacement();
            break;
        }
    }
    private void setNextStateAfterDone(replayMode replay)
    {
       	switch(board_state)
    	{
    	default: throw G.Error("Not expecting Done in state %s",board_state);
    	case DRAW_STATE:
    		setGameOver(false,false);
    		break;
    	case Setup:
    		setState(HiveState.Swap);
    		break;
    	case Swap:
    		setupAccepted = true;
 			//$FALL-THROUGH$
    	case PASS_STATE:
		case CONFIRM_STATE:
    		lastRealMoveNumber = moveNumber;
 			//$FALL-THROUGH$
		case DeclinePending:
    		HiveState nextstate = nextPlayState(whoseTurn);
    		setState(nextstate);
    		if((nextstate==HiveState.PLAY_STATE)
    			&& !hasLegalMoves()) 
    			{ setState(HiveState.PASS_STATE); 
    			}
    	    
    		break;
		case DrawPending:
			setState(HiveState.AcceptOrDecline);
			break;
    	case GAMEOVER_STATE:
    		// this shouldn't happen, but in some old archives it does.
           	if(replay==replayMode.Live) { G.Error("Not expecting Done in state %s",board_state); }
            
			//$FALL-THROUGH$
    	case PUZZLE_STATE:
    		break;
    	}

    }
    private void doDone(replayMode replay)
    {
        acceptPlacement();
        if(board_state==HiveState.AcceptPending)
        {
        	setGameOver(false,false);
        }
        else if (board_state==HiveState.RESIGN_STATE)
        {	setGameOver(false,true);
         }
        else
        {	boolean win1 = WinForPlayerNow(whoseTurn);
        	boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
        	if((board_state==HiveState.DRAW_STATE) || win1 || win2) { setGameOver(win1,win2); }
        	else 
        	{ setNextPlayer(replay);
        	  setNextStateAfterDone(replay); 
        	}
        }
    }
    
    // choose the appropriate play state based on the number and type
    // of pieces on the board.  If the queen is on, simple play state.
    // if this is the fourth play and the queen is not on, queen_play_state
    // otherwise first_play_state.
    //
    public HiveState nextPlayState(int player)
    {	
    	HiveCell loc = pieceLocation.get(playerQueen(player));
    	int n=0;
    	if((loc!=null)&&loc.onBoard)
    		{ // queen has been played
    		return(HiveState.PLAY_STATE); 
    		}
    	HiveId targetColor = playerColor(player);
    	// queen not on board, see if it has to be next
    	for(int i=0,lim=occupiedCells.size(); i<lim; i++)
    	{	HiveCell c = occupiedCells.elementAt(i);
    		for(int lvl=c.height()-1; lvl>=0; lvl--)
    		{	HivePiece p = c.chipAtIndex(lvl);
    			if(p.color==targetColor)
    			{	n++;
    				if(n>=3) { return(HiveState.QUEEN_PLAY_STATE); }
    			}
    		}
    	}

    	if(gamevariation==variation.hive_u && !setupAccepted) { return(HiveState.Setup); }
    	return(HiveState.FIRST_PLAY_STATE);
    	}
    
    public boolean canPlayQueen(int pl)
    {
    	if(NO_Q1 && (board_state==HiveState.FIRST_PLAY_STATE))
    	{	HiveId targetColor = playerColor(pl);
    		for(int i=0,lim=occupiedCells.size(); i<lim; i++)
    		{
    		HiveCell c = occupiedCells.elementAt(i);
    		for(int lvl = c.height()-1; lvl>=0; lvl--)
    		{
    		 HivePiece p = c.chipAtIndex(lvl);
    		 if(p.color==targetColor) { return(true); }	// we've played something so we can play the Q if we want to
    		}}
    		return(false);
     	}
    	return(true);
    }

    public boolean hasPlayedQueen(int pl)
    {
    	HiveCell q = rackForPlayer(pl)[0];
    	return(q.topChip()==null);
    }
    // sweep the adjavent non-empty cells from center by simple recursive descent
    int sweep_counter=0;
    private void sweepBoard(HiveCell center,HiveCell ignored)
    {	if((center.sweep_counter!=sweep_counter)
    		&& (center.height()>0)
    		&& (center!=ignored))
    	{	center.sweep_counter = sweep_counter;
    		for(int i=0,len=center.geometry.n; i<len; i++)
    		{	sweepBoard(center.exitTo(i),ignored);
    		}
    	}
    }
    // sweep occupied cells ignoring two cells as empty
    private void sweepBoard(HiveCell center,HiveCell ignored,HiveCell ignored2)
    {	if((center.sweep_counter!=sweep_counter)
    		&& (center.height()>0)
    		&& (center!=ignored)
    		&& (center!=ignored2))
    	{	center.sweep_counter = sweep_counter;
    		for(int i=0,len=center.geometry.n; i<len; i++)
    		{	sweepBoard(center.exitTo(i),ignored,ignored2);
    		}
    	}
    }
    void sweepAndCountBoard(HiveCell center,int distance)
    {	if( ((center.sweep_counter!=sweep_counter)
    		||(center.overland_gradient>(sweep_counter+distance)))
    		&& (center.height()>0)
    		)
    	{	center.sweep_counter = sweep_counter;
 			center.overland_gradient = sweep_counter+distance; 
 	   		for(int i=0,len=center.geometry.n;i<len; i++)
  	    		{	sweepAndCountBoard(center.exitTo(i),distance+1);
  	    		}
      	}
    } 
    // slither around the board staring at cell firstC, and count the
    // keep track of the minimum distance
    int slitherAndCountBoard(HiveCell origin,HiveCell firstC,HiveCell prevC,int direction,int distance)
    {	
		int nVisited = 0;
    	if((firstC.sweep_counter!=sweep_counter)
    		||(firstC.slither_gradient>(sweep_counter+distance)))
    	{
    	firstC.sweep_counter = sweep_counter;
    	firstC.slither_gradient = sweep_counter+distance;
		int len = firstC.geometry.n;
		for(int i=1;i<=len;i++)
		{	int dir = (i+direction)%len;
			int prevdir = (i+direction-1+len)%len;
			int nextdir = (i+direction+1)%len;
			HiveCell c = firstC.exitTo(dir);
			HiveCell prevc = firstC.exitTo(prevdir);
			HiveCell nextc = firstC.exitTo(nextdir);
			if( (c!=prevC)		// not backtracking
				&& ((prevC==null)||(prevc.height()==0)||(nextc.height()==0))	// not a gate
				&& adjacentCell(c,firstC,null)) // still part of the hive
				{ if(c.height()==0) 	// not occupied
					{int nv = slitherAndCountBoard(origin,c,firstC,((dir+len/2)%len),distance+1);
					 if(distance>0) { nVisited++; }
					 else if(nv==0) { nVisited++; }
					}
					else if((c.sweep_counter!=sweep_counter)
				    		||(c.slither_gradient>(sweep_counter+distance)))
					{ //mark the adjacent filled cells
					  c.sweep_counter = sweep_counter;
					  c.slither_gradient = sweep_counter+distance;
					}
				}
		}}
    	return nVisited;
    }


    // return TRUE is this cell position adjacent to one of the cells that s is adjacent to
    public boolean adjacentCell(HiveCell which,HiveCell s,HiveCell e)
    {	if(which!=null) { return(which.adjacentCell(s,e)); }
    	return(false);
    }

    // return true if this is a connected hive, ignoring "ignored"
    public boolean validHive(HiveCell ignored)
    {	   	
    	if(pickedObject!=null) { return(true); }	// hive broken test already passed
    	if((ignored!=null) && (ignored.height()>1)) { return(true); }	// still valid if a stack
    	sweep_counter++;
    	boolean swept=false;
    	for(int i=0,lim=occupiedCells.size();i<lim;i++)
    	{	// make one sweep of the occupied cells.  When we first encounter
    		// a cell that's not empty and not ignored, sweep the board from there
    		// subsequently, if any cell was not swept, the hive is broken.
    		HiveCell l = occupiedCells.elementAt(i);
    		if(l!=null && l!=ignored)
    		{	if(swept)
    				{	if(l.sweep_counter!=sweep_counter) 
    					{ //this piece wasn't found in the sweep
    					return(false); 
    					}
    				}
    				else
    				{ swept=true;
    				  sweepBoard(l,ignored); 
    				}
    			}
    	}
    	return(true);
    }
    // return true if this is a connected hive, ignoring "ignored"
    // empty cell and stacked cell has already been considered for both ignored
    public boolean validHive2(HiveCell ignored,HiveCell ignored2)
    {	   	
    	sweep_counter++;
    	boolean swept=false;
    	for(int i=0,lim=occupiedCells.size();i<lim;i++)
    	{	// make one sweep of the occupied cells.  When we first encounter
    		// a cell that's not empty and not ignored, sweep the board from there
    		// subsequently, if any cell was not swept, the hive is broken.
    		HiveCell l = occupiedCells.elementAt(i);
    		if((l!=null) && (l!=ignored) && (l!=ignored2))
    		{	if(swept)
    				{	if(l.sweep_counter!=sweep_counter) 
    					{ //this piece wasn't found in the sweep
    					return(false); 
    					}
    				}
    				else
    				{ swept=true;
    				  sweepBoard(l,ignored,ignored2); 
    				}
    			}
    	}
    	return(true);
    }
    // return true if this cells mobility is only enabled
    // by the presence of a sibling which is also mobile
    public boolean sibMobile(HiveCell loc)
    {	if(loc.height()==1)
    	{
    	HiveId myColor = loc.topChip().color;
    	for(int dir = geometry.n-1; dir>=0; dir--)
    	{
    		HiveCell sib = loc.exitTo(dir) ;
    		if((sib.height()==1) && (sib.topChip().color==myColor))
    		{	if(validHive(sib) && !validHive2(loc,sib)) { return(true); }
    		}
    		}
    	}
    	return(false);
    }
    private void addAnimationPath(HiveCell src,HivePiece po,HiveCell dest)
    {	HiveCell dests[] = getTempDest();
    	CellStack path = new CellStack();
       	pathHash.clear();
    	legalDests(pickedSource,false,po,dests,path,whoseTurn,false);
    	HiveCell p[] = pathHash.get(dest);
    	if(p!=null)
    	{	if(p.length<=1) { animationStack.push(src); animationStack.push(dest); }
    		for(int idx = 1;idx<p.length;idx++)
    		{	animationStack.push(p[idx-1]);
    			animationStack.push(p[idx]);
    		}
    		
    	}
     	pathHash.clear();
    }
    public boolean Execute(commonMove mm,replayMode replay)
    {	Hivemovespec m = (Hivemovespec)mm;
        boolean pmove = false;
        boolean fall = false;
        boolean animate = false;
        //G.print("M "+mm+" "+board_state);
        currentMove = m;	// for debugging
        switch (m.op)
        {
        case MOVE_PLAYWHITE:
        	swappedRacks = getColorMap()[0]==0;
        	break;
        case MOVE_PLAYBLACK:
        	swappedRacks = getColorMap()[0]!=0;
        	break;
        case MOVE_PASS_DONE:
        case MOVE_PASS:
        	// note: this "pass" was auto-generated from the viewer when the user clicked on "done".
        	// it's important to change the state after the "pass" so the count
        	// of repetitions doesn't increase.  The "done" is already following.
        	if(board_state!=HiveState.GAMEOVER_STATE)	// this test shouldn't be necessary
        			// but some damaged game records allow another "pass" after gameover
        		{ setState(HiveState.CONFIRM_STATE);
        		}
        	prestun = null;
        	stunned = null;
        	if(m.op==MOVE_PASS_DONE) { doDone(replay); }
        	break;
        case MOVE_NULL:
        	setState(HiveState.CONFIRM_STATE);
        	// and fall into done
			//$FALL-THROUGH$
		case MOVE_DONE:
         	doDone(replay);
            break;
        case MOVE_PMOVE:
        case MOVE_PMOVE_DONE:
        	pmove = true;
			//$FALL-THROUGH$
		case MOVE_MOVE:
		case MOVE_MOVE_DONE:
        	HivePiece piece = m.object;
        	HiveCell loc = pieceLocation.get(piece);
        	pickObject(loc);
        	m.location = pickedSource;
        	G.Assert(pickedObject==piece,"picked the right thing");
        	animate = replay!=replayMode.Replay;
        	// fall into dropb
        	fall = true;
			//$FALL-THROUGH$
		case MOVE_PDROPB:
        	if(!fall) { pmove = true; }
			//$FALL-THROUGH$
		case MOVE_DROPB:
            { HiveCell c = getCell(m.to_col,m.to_row);
              HiveCell src = pickedSource;
              animate |= (replay==replayMode.Single);
              if(!robotBoard)
              {
            	  m.setAttachment(c,pickedObject,src);
              }
              switch(board_state)
              {
              default: throw G.Error("Not expecting drop in state %s",board_state);
              case PASS_STATE:
              case DRAW_STATE:
              case GAMEOVER_STATE:
            	  // this shouldn't happen, but in some old archives it does.
                 	if(replay==replayMode.Live) { G.Error("Not expecting drop in state %s",board_state); }
				//$FALL-THROUGH$
              case PLAY_STATE: 
              case FIRST_PLAY_STATE:
              case QUEEN_PLAY_STATE:
            	  if(c==src) 
            	  	{ unPickObject(); 
            	  	}
            	  else { HiveCell dest = getCell(m.source, m.to_col, m.to_row);
            	  		 HivePiece po = pickedObject;
             	  		 if(animate)
            	  		 {
            	  		 if(!src.onBoard)
            	  		 	{	animationStack.push(src);
            	  		 		animationStack.push(dest);
            	  		 	}
            	  		 	else
            	  		 	{ 
            	  		 		addAnimationPath(src,po,dest);
            	  		 	}
            	  		 }
             	  		 // do while the object is still picked
       	  		 		 dropObject(dest);
	 
            	  		 prestun = (pieceTypeIncluded.test(PieceType.PILLBUG)||pmove) 
            	  		 				? dest 	// new pillbug, all moved pieces are stunned
            	  		 				: null;	// stunned only if moved by pillbug
            	  		 setNextStateAfterDrop(replay);
            	  }
            	  break;
           	  case PUZZLE_STATE:
           	  case Setup:
           		  dropObject(getCell(m.source, m.to_col, m.to_row));
           		  setNextStateAfterDrop(replay);
           		  break;
            }}
            if((m.op==MOVE_PMOVE_DONE)||(m.op==MOVE_MOVE_DONE)) { doDone(replay); }
            break;
        case MOVE_OFFER_DRAW:
        	if(board_state==HiveState.DrawPending) { setState(undrawState); }
        	else {  undrawState = board_state;
        			setState(HiveState.DrawPending);
        		}
        	break;
        case MOVE_ACCEPT_DRAW:
           	switch(board_state)
        	{	
        	case AcceptPending: 	// cancel accept and revert to neutral
        		setState(HiveState.AcceptOrDecline); 
        		break;
           	case AcceptOrDecline:
           	case DeclinePending:	// accept pending
           		setState(HiveState.AcceptPending); 
           		break;
        	default: throw G.Error("Not expecting %s",board_state);
        	}
           	break;
        case MOVE_DECLINE_DRAW:
        	switch(board_state)
        	{	
        	case DeclinePending:	// cancel decline and revert to neutral
        		setState(HiveState.AcceptOrDecline); 
        		break;
        	case AcceptOrDecline:
        	case AcceptPending: setState(HiveState.DeclinePending); break;
        	default: throw G.Error("Not expecting %s",board_state);
        	}
        	break;
        case MOVE_PICKB:
        	// come here only where there's something to pick, which must
        	switch(board_state)
        	{
            default: throw G.Error("Not expecting pickb in state %s",board_state);
            case DRAW_STATE:
            case CONFIRM_STATE:
        		if(isDest(getCell(m.from_col,m.from_row)))
        		{ unDropObject(); 
        		  setState(nextPlayState(whoseTurn));
        		}
        		else {throw G.Error("Can't pick something else"); }
        		break;
           	case GAMEOVER_STATE:
        	case PASS_STATE:		// this shouldn't happen, but in some old archives it does.
        		if(replay==replayMode.Live) { G.Error("Not expecting pickb in state %s",board_state); }
				//$FALL-THROUGH$
        	case FIRST_PLAY_STATE:
        	case QUEEN_PLAY_STATE:
        	case PLAY_STATE:
        	case PUZZLE_STATE:
        	case Setup:
         		if((replay!=replayMode.Live )&&(pickedObject!=null)&&(pickedSource!=null))
         		{	// this shouldn't happen but it is a workaround for a few
         			// old game records which were damaged at birth.
         			G.print("Repairing damaged game, double pick");
         			unPickObject();
         		}
        		pickObject(HiveId.BoardLocation, m.from_col, m.from_row);
        		m.object = pickedObject;
        		break;
        	};
        	break;
        	
        case MOVE_DROP: // drop on chip pool;
        	{
       		HiveCell dest = getCell(m.source, m.to_col, m.to_row);
            if(dest==pickedSource) {
            	unPickObject();
            }
            else {
            dropObject(dest);      
            stunned = null;
            setNextStateAfterDrop(replay);
        	}}
            break;

        case MOVE_PICK:
            unDropObject();
            unPickObject();
            HiveCell p = pieceLocation.get(m.object);
            G.Assert(p!=null,"No piece location for %s, %s placed %s",p,m,pieceLocation.size());
            pickObject(p);
 
            break;

  
        case MOVE_START:
            setWhoseTurn(m.player);
            acceptPlacement();
            unPickObject();
            setState(HiveState.PUZZLE_STATE);
            { boolean win1 = WinForPlayerNow(whoseTurn);
              boolean win2 = WinForPlayerNow(nextPlayer[whoseTurn]);
              if(win1 || win2) { setGameOver(win1,win2); }
              else { setState(nextPlayState(whoseTurn));  }
            }
            break;

       case MOVE_RESIGN:
            setState(unresign==null?HiveState.RESIGN_STATE:unresign);
            break;
        case MOVE_EDIT:
        	acceptPlacement();
            setWhoseTurn(FIRST_PLAYER_INDEX);
            setState(HiveState.PUZZLE_STATE);

            break;
		case MOVE_GAMEOVERONTIME:
			setGameOver(true,false);
			break;
        default:
        	cantExecute(m);
        }
        //G.print("X "+mm+" "+board_state);
        //System.out.println("Ex "+m+" for "+whoseTurn+" "+board_state);
        //System.out.println("Digest "+Digest());
        return (true);
    }

    public boolean LegalToHitChips(int player)
    {
        switch (board_state)
        {
        default:
        	throw G.Error("Not expecting state %s", board_state);


        case PLAY_STATE:
        case QUEEN_PLAY_STATE:
        case FIRST_PLAY_STATE:
        	return((player==whoseTurn) && ((pickedSource==null)|| !pickedSource.onBoard));
        case PASS_STATE:
        case CONFIRM_STATE:
        case DRAW_STATE:
        case RESIGN_STATE:
		case GAMEOVER_STATE:
		case Swap:
		case DrawPending:
		case AcceptOrDecline:
		case DeclinePending:
		case AcceptPending:
			return(false);
        case PUZZLE_STATE:
        case Setup:
            return (true);
        }
    }

    public boolean LegalToHitBoard(HiveCell cell)
    {	HiveCell ps = pickedSource;
    	HivePiece po = pickedObject;
    	switch (board_state)
        {
		case PLAY_STATE:
		case FIRST_PLAY_STATE:
		case QUEEN_PLAY_STATE:
			if(ps!=null)
			{
			// dropping something
			if(cell==ps) { return(true); }
			if(ps.onBoard)
				{	// pick a landing zone for the piece
				HiveCell tempDests[]=getTempDest();
				boolean val = G.arrayContains(tempDests,cell,legalDests(ps,true,pickedObject,tempDests,null,whoseTurn,false));
				returnTempDest(tempDests);
				return(val);
				}
			return(legalDropDest(cell,po));
			}

			{
			// trying to pickup something
			HivePiece topBug = cell.topChip();
		    if((topBug!=null) && (cell!=stunned))
		    {
		    HiveId targetColor = playerColor(whoseTurn);
		    switch(board_state)
		    {	default: break;
			    case FIRST_PLAY_STATE:
			    case QUEEN_PLAY_STATE:
			    		return(false);		// can't pick from the board
			    case PLAY_STATE:
			    	if(!(validHive((cell.height()>1) ? null : cell)))
			    	{ // not our piece, or breaks the hive
			    	  return(false); 
			    	}
			    	if(topBug.color!=targetColor)
			    	{	// can still be flipped by a pillbug
			    		HiveCell tempDests[] = getTempDest();
			    		int n = legalPillbugDests(cell,targetColor,true,pickedObject,tempDests,0,0,null,false);
			    		returnTempDest(tempDests);
			    		return(n>0);
			    	}
			    	else if(stunned!=cell)	// can never move a stunned piece
			    	{

			    		// spiders have a rare condition with an interior cavity of size 3
			    		HiveCell tempDests[]=getTempDest();
			    		int val = legalDests(cell,false,topBug,tempDests,null,whoseTurn,false);
			    		returnTempDest(tempDests);
			    		return(val>0);
			    	}
		    }
		    }
			}
			return(false);
		case DRAW_STATE:
		case CONFIRM_STATE:
			return(cell==droppedDest);
		case PASS_STATE:
		case GAMEOVER_STATE:
		case RESIGN_STATE:
		case Swap:
		case DrawPending:
		case AcceptOrDecline:
		case AcceptPending:
		case DeclinePending:
			return(false);

        default:
        	throw G.Error("Not expecting state %s", board_state);

        case PUZZLE_STATE:
        case Setup:
        	return((pickedObject==null) 
        			? ((cell!=null)&&(cell.height()>0)) 	// something available to pick up
        			: ( ((pickedObject.type==PieceType.MOSQUITO)
        					||(pickedObject.type==PieceType.BLANK)
        					||(pickedObject.type==PieceType.BEETLE))	// dropping a beetle or a mosquito?
        					?true								// beetles go anywhere
        					:(cell.height()==0)));				// others only on level 0
        }
    }
    

 /** assistance for the robot.  In addition to executing a move, the robot
    requires that you be able to undo the execution.  The simplest way
    to do this is to record whatever other information is needed before
    you execute the move.  It's also convenient to automatically supply
    the "done" confirmation for any moves that are not completely self
    executing.
    */
    public void RobotExecute(Hivemovespec m)
    {
        m.state = board_state; //record the starting state. The most reliable
       	m.stun = stunned;
       // to undo state transistions is to simple put the original state back.
        
        G.Assert(m.player == whoseTurn, "whoseturn doesn't agree");
        //G.print("R "+m);
        if (Execute(m,replayMode.Replay))
        {	
            
        }
    }
 

   //
    // un-execute a move.  The move should only be unexecuted
    // in proper sequence.  This only needs to handle the moves
    // that the robot might actually make.
    //
    public void UnExecute(Hivemovespec m)
    {
        //G.print("U "+m+" for "+whoseTurn);

        switch (m.op)
        {
   	    default:
   	    	cantUnExecute(m);
        	break;
   	    case MOVE_MOVE:
   	    case MOVE_PMOVE:
   	    case MOVE_PMOVE_DONE:
   	    case MOVE_MOVE_DONE:
   	    	HiveCell c = getCell(m.to_col,m.to_row);
   	    	HivePiece p = removeChip(c);
   	    	addChip(m.location,p);
   	    	break;
   	    case MOVE_PASS:
        case MOVE_NULL:
        case MOVE_PASS_DONE:
        case MOVE_ACCEPT_DRAW:
        case MOVE_DECLINE_DRAW:
        case MOVE_OFFER_DRAW:
   	    	break;
        case MOVE_DONE:
            break;
        case MOVE_RESIGN:
            break;
        }
    	droppedDest=null;
  	    pickedSource=null;
  	   	pickedObject=null;
  	   	stunned = m.stun;
	    setState(m.state);
	    if(whoseTurn!=m.player)
	    {	moveNumber--;
	    	setWhoseTurn(m.player);
	    }
 }

boolean isAdjacentToQueen(HiveCell c,HiveCell q1,HiveCell q2)
{
	for(int dir=0;dir<CELL_FULL_TURN;dir++)
	{
		HiveCell adj = c.exitTo(dir);
		if((adj==q1) || (adj==q2)) { return(true); }
	}
	return(false);
}


// 
// add pillbug flips of enemy pieces.  These are not included in the
// normal scan, because we normally don't move enemy pieces.
//
boolean addPillbugEnemyFlips(CommonMoveStack all,HiveCell c,HivePiece bug)
{	
	// debugging, verify that legalDests works the same
	if((c!=stunned) &&		// stunned pieces can't flip anything
		(HivePiece.isPillbug(bug)											// flip by our own pillbug
			||((bug.type==PieceType.MOSQUITO) && (c.height()==1) && c.isAdjacentToPillbug())))	// flip by our mosquito with the power
	{
		for(int dir=0; dir<CELL_FULL_TURN; dir++)
		{
			HiveCell adj = c.exitTo(dir);
			if(adj.height()==1)	// only pieces at base level are flippable.
			{
			HivePiece top = adj.topChip();
			if((top!=null)
					&& validHive(adj)
					&& !isGate(adj,false,c,dir+3)
					&& (top.color!=bug.color)		// this allows only opponent pieces to be moved
					// the original pillbug only ignored "stunned" pieces, 
					// the new pillbug ignores whatever piece was moved last.
					&& (adj!=stunned))		// and stunned pieces can't be moved.
			{
				for(int destdir=0; destdir<CELL_FULL_TURN; destdir++)
				{
					HiveCell dest = c.exitTo(destdir);
					if((dest.topChip()==null)
							&& !isGate(c,false,dest,destdir))
					{	// move from one to another cell adjacent to the pillbug
						// use exactBugId so the piece will be flagged with w or b
						if(all==null) { return(true); }
						all.addElement(new Hivemovespec(whoseTurn,MOVE_PMOVE_DONE,top,dest,c));
					}
				}
			}}
		}
	}
	return(false); 
}

public void verifyMoves(CommonMoveStack mv)
{	CellStack occupied = new CellStack();
	CommonMoveStack moves = new CommonMoveStack();
	moves.copyFrom(mv);
	occupied.copyFrom(occupiedCells);
	for(int midx = moves.size()-1; midx>=0; midx--)
	{
		Hivemovespec m = (Hivemovespec)moves.elementAt(midx);
		HiveCell loc = pieceLocation.get(m.object);
		if(!loc.onBoard) { moves.remove(m,false); }
	}
	for(int idx=occupied.size()-1; idx>=0; idx--)
	{
		HiveCell c = occupied.elementAt(idx);
		HivePiece top = c.topChip();
		boolean legal = LegalToHitBoard(c);
		if(legal)
		{	pickObject(c);
			HiveCell tempDests[]=getTempDest();
			int ndests = legalDests(pickedSource,true,pickedObject,tempDests,null,whoseTurn,false);
			for(int i=0;i<ndests;i++)
			{
				HiveCell d = tempDests[i];
				boolean found = false;
					for(int midx=moves.size()-1; !found &&  midx>=0; midx--)
					{
					Hivemovespec m = (Hivemovespec)moves.elementAt(midx);
					found = (m.object==top)
							&& (m.to_col==d.col)
							&& (m.to_row==d.row);
					if(found)
					{	int mi = moves.size()-1;
						while(mi>=0)
						{	Hivemovespec it = (Hivemovespec)moves.elementAt(mi);
							if((it.object == m.object)
									&& (it.to_col==m.to_col)
									&& (it.to_row==m.to_row))
							{
							if(mi+1 == moves.size()) { moves.pop(); }	// remove all similar moves
							else { moves.remove(mi,false); }
							}
							mi--;
						}

					}
					}
					if(!found) { throw G.Error("Move "+c+" to "+d+" for "+top+" not found"); }
				}
				returnTempDest(tempDests);
			unPickObject();
		}
		else
		{	for(int midx=moves.size()-1; midx>=0; midx--)
			{
			Hivemovespec m = (Hivemovespec)moves.elementAt(midx);
			if(m.object==top)
				{
				throw G.Error("Move "+m+" shouldn't exist for "+top);
				}
			}
		}
	}
	if(moves.size()>0) { throw G.Error("Moves not accounted for %s",moves); }
}

CommonMoveStack  GetListOfMoves0()
 {	
	CommonMoveStack all = new CommonMoveStack();
	GetListOfMoves1(all);
	return(all);
 }
boolean GetListOfMoves1(CommonMoveStack all)
{	// first go after the drop moves
	switch(board_state)
	{
	case CONFIRM_STATE:
	case AcceptPending:
	case DeclinePending:
		all.addElement(new Hivemovespec(whoseTurn,MOVE_DONE));
		break;
	case AcceptOrDecline:
		all.addElement(new Hivemovespec(whoseTurn,MOVE_ACCEPT_DRAW));
		all.addElement(new Hivemovespec(whoseTurn,MOVE_DECLINE_DRAW));
		break;
	default:
	{
 	HiveCell tempDests[] = getTempDest();
 	HiveCell tempBlankDests[] = null;
		int nDrops = legalDropDests(tempDests,playerQueen(whoseTurn),all==null);	// not necessarily the q, just any piece of color
	int nBlanks = -1;
	 	HiveCell[] cells = rackForPlayer(whoseTurn);
 	boolean include_queen = canPlayQueen(whoseTurn);
 	boolean require_queen = board_state==HiveState.QUEEN_PLAY_STATE;
 	// onboarding pieces
 	for(PieceType pc : PieceType.values())
 		{
 		int pcidx = pc.ordinal();
 		HiveCell c = cells[pcidx];
 		HivePiece bug = c.topChip();

 		if((bug!=null) 
 				&& (include_queen || (bug.type!=PieceType.QUEEN))
 				&& (!require_queen || (bug.type==PieceType.QUEEN))
 				&& pieceTypeIncluded.test(bug.type))
 		 {	if(bug.type==PieceType.BLANK)
 		 		{	if(tempBlankDests==null)
 		 				{tempBlankDests = getTempDest();
 		 				 nBlanks = slitherAnywhere(null,tempBlankDests,0,null,all==null);
 		 				}
		 			for(int moven=0;moven<nBlanks;moven++) 
		 			{ HiveCell target = tempBlankDests[moven];
		 			if(all==null)
		 				{if(tempBlankDests!=null) { returnTempDest(tempBlankDests); }
		 				 returnTempDest(tempDests); 
		 				 return(true); 
		 				}
		 			all.addElement(new Hivemovespec(whoseTurn,MOVE_MOVE_DONE,bug,target,c));
		 			}}
 		 		else {
 		 			for(int moven=0;moven<nDrops;moven++) 
 		 			{ HiveCell target = tempDests[moven];
 		 			  Hivemovespec m = new Hivemovespec(whoseTurn,MOVE_MOVE_DONE,bug,target,c);
 		 			  if(all==null) 
 		 			  {
 		 			  if(tempBlankDests!=null) { returnTempDest(tempBlankDests); }
 		 			  returnTempDest(tempDests); 
 		 			  return(true); 
 		 			  }
 		 			  all.addElement(m);
 		 			}}
 		 }
 		
 		
 	if(tempBlankDests!=null) { returnTempDest(tempBlankDests); }
 	}
 	returnTempDest(tempDests);
 	}
	}
	
	boolean hasDropMoves = all==null ? true : all.size()>0;
 	if(board_state==HiveState.PLAY_STATE)
 	{
 	HiveId targetColor = playerColor(whoseTurn);
 	// now add the moves of pieces already in play
 	for(int idx=0,lim=occupiedCells.size(); idx<lim; idx++) 
 	{	
 		HiveCell c =  occupiedCells.elementAt(idx);
 		HivePiece bug = c.topChip();
 		if(bug.color==targetColor)	
 		{
 			
 			if(addPillbugEnemyFlips(all,c,bug))
 				{ if (all==null) { return(true); };	// add enemy flip moves by pillbugs and mosquitos
 				}
 			HiveCell tempDests[] = getTempDest();
 	 		int ndests = legalDests(c,false,bug,tempDests,null,whoseTurn,all==null);
 			for(int i=0;i<ndests;i++)
 			{	HiveCell dest = tempDests[i];
 				if(all==null) { returnTempDest(tempDests); return(true); }
 				if(dest.pillbug_dest)
 				{	
 	 				all.addElement(new Hivemovespec(whoseTurn,MOVE_PMOVE_DONE,bug,dest,c));
 				}
 				else
 				{
 				all.addElement(new Hivemovespec(whoseTurn,	MOVE_MOVE_DONE,bug,dest,c));
 				}
 			}
 			returnTempDest(tempDests);
 			}
 			}
 	if(G.debug() && (all!=null)) 
 		{ verifyMoves(all); }
 	}
 	if((all!=null) && !hasDropMoves && robotCanOfferDraw && (all.size()>1)) 
		{ all.addElement(new Hivemovespec(whoseTurn,MOVE_OFFER_DRAW)); 
		}

 	return(false);
 }

 public CommonMoveStack  GetListOfMoves()
 {	CommonMoveStack  all = GetListOfMoves0();
	if(all.size()==0) { all.addElement(new Hivemovespec(whoseTurn,MOVE_PASS_DONE)); }
	return(all);
  }
 public int nLegalMoves()
 {	CommonMoveStack  v = GetListOfMoves0();
 	return(v.size());
 }
 public boolean hasLegalMoves()
 {	
 	boolean has = GetListOfMoves1(null);
 	//int n = nLegalMoves();
 	//G.Assert(has==(n>0),"move match");
 	return(has);
 }
}