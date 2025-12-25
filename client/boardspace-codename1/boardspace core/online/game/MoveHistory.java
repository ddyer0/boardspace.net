package online.game;

import lib.G;

/**
 * this is a specialization of commonMoveStack to act as a move history for a game 
 */


import lib.PopupManager;
import lib.exCanvas;
import online.game.BaseBoard.BoardState;
import online.game.sgf.export.sgf_names;
public class MoveHistory extends CommonMoveStack  implements SequenceStack,sgf_names
{
	public int viewStep=-1;					// scrollback position
	public int viewStep() { return viewStep; }
	public int viewMoveNumber = -1;
	public int sliderPosition=-1;
	public commonPlayer viewTurn = null;	// player whose turn it was when we entered review mode
	public CommonMoveStack rememberedPositions = null;
	/** this is the index into the game history at the point
	 * we are currently examining.  -1 means we're not reviewing.
	 */
	public int viewMove = -1; 			// the maximum move number in an incomplete game
	public BoardState pre_review_state;	// board state before entering review mode
	
    public commonMove currentHistoryMove()
    {
        if (viewStep > 0)
        {
            return (elementAt(viewStep - 1));
        }
        else if(size()>0) 
        	{
        	  return(top()); 
        	}

        return (null);
    }

	public commonMove matchesCurrentMove(commonMove targetMove) {
		commonMove cm = currentHistoryMove();
		if(cm.Same_Move_P(targetMove)) { return cm; }
		return null;
	}
	public void clear()
	{	super.clear();
		rememberedPositions = null;
	}
	public CommonMoveStack rememberedPositions()
	{
		if(rememberedPositions==null) { rememberedPositions = new CommonMoveStack(); }
		return rememberedPositions;
	}
	
	private String canonicalName(commonMove m)
	{	int rememberedPositionIndex = 0;
		for(int i=0;i<rememberedPositions.size();i++)
		{
			String n = rememberedPositions.elementAt(i).getNodeName();
			if(n!=null && n.length()>0 && n.charAt(0)=='#')
			{
				int k = G.guardedIntToken(n.substring(1),0);
				rememberedPositionIndex = Math.max(rememberedPositionIndex,k);
			}
		}
		return("#"+(rememberedPositionIndex+1));
	}
	
	public void forgetThis()
	{	commonMove m = currentHistoryMove();
		forgetNamedPosition(m);
	}
	
	/**
	 * give this position a name, or forget it if it's already remembered
	 */
	public void rememberThis()
	{	commonMove m = currentHistoryMove();
		forgetNamedPosition(m);
		String s = canonicalName(m);
		rememberNamedPosition(m,s);
	}
	/** forget the position at m
	 * 
	 * @param m
	 */
	public void forgetNamedPosition(commonMove m)
	{	m.setNodeName(null);
		rememberedPositions().remove(m,true);
	}
	/**
	 * remember the position at m with a name
	 * @param m
	 * @param name
	 */
	public void rememberNamedPosition(commonMove m,String name)
	{
		if(name==null)
		{
		forgetNamedPosition(m);
		}
		else
		{
		m.setNodeName(name);
		CommonMoveStack rp = rememberedPositions();
		rp.pushNew(m);
		}
	}
	public void rememberNamedPosition(String name)
	{
		rememberNamedPosition(currentHistoryMove(),name);
	}
	/**
	 * get the number of remembered positions
	 * @return
	 */
	public int nRememberedPositions() 
	{	if(rememberedPositions==null) { return 0; }
		return rememberedPositions().size();
	}
	/** 
	 * get the nth remembered position
	 * @param n
	 * @return
	 */
	public commonMove getRememberedPosition(int n) { return rememberedPositions().elementAt(n); }
	
	private PopupManager rememberedPositionPopup = null;
	private PopupManager rememberedPositionPopup()
	{	if(rememberedPositionPopup==null) { rememberedPositionPopup = new PopupManager(); }
		return rememberedPositionPopup;
	}
	/**
	 * show a menu of all the remembered positions
	 * @param parent
	 * @param x
	 * @param y
	 */
	public void showRememberedPositionMenu(exCanvas parent,int x,int y)
	{
		PopupManager pop = rememberedPositionPopup();
		int size = nRememberedPositions();
		pop.newPopupMenu(parent,parent.deferredEvents);
		for(int i=0;i<size;i++)
	    	{
			commonMove m = getRememberedPosition(i);
			String s = m.getNodeName()+" @"+m.getSliderNumString();
	    	pop.addMenuItem(s,m);
	    	}
		pop.useSimpleMenu = false;
		pop.show(x,y);
	}
	/**
	 * handle hits on the position menu
	 * @param c
	 * @param target
	 * @return
	 */
	public boolean handleDeferredEvent(commonCanvas c,Object target) {
		PopupManager positions = rememberedPositionPopup;
		if(positions!=null && positions.selectMenuTarget(target))
		{
			commonMove m = (commonMove)positions.rawValue;
			String name = m.getNodeName();
			if(name!=null) { c.doScrollTo(findPath(name)); }
			return true;
		}
		return false;
	}
	/**
	 * find a path from root to a node with the specified name
	 * 
	 * @param root
	 * @param target
	 * @param path
	 * @return
	 */
	private CommonMoveStack findPath(commonMove root, String target,CommonMoveStack path)
	{
		while(root!=null)
		{
		if(target.equals(root.getNodeName())) 
			{ path.push(root);
			  return path;
			}
		int nv =root.nVariations();
		if(nv<=1)
		{
			root = root.firstVariation();
		}
		else
		{	path.push(root);
			for(int i=0;i<nv;i++)
			{	
				commonMove variation = root.getVariation(i);
				path.push(variation);
				CommonMoveStack v = findPath(variation,target,path);
				if(v!=null) 
					{ return v;}
				path.pop();
			}
			path.pop();
		}
		}
		return null;
	}
	/**
	 * find a path to the specified name
	 * 
	 * @param named
	 * @return
	 */
	public CommonMoveStack findPath(String named) {
		return findPath(elementAt(0),named,new CommonMoveStack());
	}
	/**
	 * switch to the nth of the variations of m
	 * @param m
	 * @param nth
	 */
	public void switchToVariation(commonMove m,int nth)
	{	
		setSize(m.index()+1);
		commonMove v = m.next = m.getVariation(nth);
		addToHistoryAndExtend(v);
	}

	public int addToHistoryAndExtend(commonMove m)
    {	
        addElement(m); // add the new move (or the new variation) to the history
        // add the principle variation of to the history
        return(extendHistory(m));
    }
    public int extendHistory(commonMove m)
        {
        	commonMove newmove = m;
        	int hsize = size();
        	newmove.setIndex(hsize - 1);

            if (newmove.next != null)
            {
                while (newmove.next != null)
                {
                    addElement(newmove.next);
                    newmove = newmove.next;
                }

                return (hsize);
            }

            return (-1);
     }
    public void rememberNamedPosition(int step,String n)
    {	if(step==-2) { step = size()-1; }	// compensate for setting at the end
    	if(step>=0 && step<size())
    	{
    		rememberNamedPosition(elementAt(step),n);
    	}
    }
	public String getNodeName(int step) {
		if(step<0) { step = size()-1; }
		if(step>=0 && step<size())
		{
			return elementAt(step).getNodeName();
		}
		return null;
	}
	public String getCurrentNodeName()
	{	return currentHistoryMove().getNodeName();
	}
	
}
