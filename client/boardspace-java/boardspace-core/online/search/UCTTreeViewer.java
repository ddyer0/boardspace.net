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
package online.search;

import java.awt.Color;
import lib.Graphics;
import java.awt.Rectangle;

import javax.swing.JCheckBoxMenuItem;

import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.InspectorInterface;
import lib.LFrameProtocol;
import lib.ShellProtocol;
import lib.Sort;
import lib.StockArt;
import lib.CellId;
import online.game.CommonMoveStack;
import online.game.commonMove;


public class UCTTreeViewer extends TreeViewer
{
	/**
	 * 
	 */
	static final long serialVersionUID = 1L;
	public UCTMoveSearcher search = null;
	private NodeStack path = new NodeStack();
	private CommonMoveStack kids = new CommonMoveStack();
	private JCheckBoxMenuItem sortUCT = null;
	
	public void startInspector()
	{
		InspectorInterface inspect = (InspectorInterface)G.MakeInstance("lib.InspectorBridge");
		inspect.view(search);
	}

    public void startShell()
    {	
    	ShellProtocol shell = (ShellProtocol)G.MakeInstance("lib.ShellBridge");
    	shell.startShell("viewer",this,"searcher",search,"path",path,"out",System.out);
    	G.setPrinter(shell);
    }
    
	public void init(ExtendedHashtable info,LFrameProtocol frame)
	{	super.init(info,frame);
     	sortUCT = myFrame.addOption("sort by UCT", false,deferredEvents);
	}
    public boolean handleDeferredEvent(Object target, String command)
    {	
    	boolean handled = super.handleDeferredEvent(target,command);
    	return(handled);
    }
	public void setTree(TreeProviderProtocol s)
	{	if(s instanceof UCTMoveSearcher)
		{
		search = (UCTMoveSearcher)s;
		path.clear();
		kids.clear();
		if(s!=null)
		{
		path.push(search.root);
		kids.push(null);
		}
		repaint();
		}
	}
	public void ViewerRun(int wait)
	{
		super.ViewerRun(wait);
		UCTMoveSearcher ss = search;
		if((ss!=null) && ss.active) 
			{ repaint(100); 
			}
	}
	enum UCTID implements CellId { hitRoot, hitNode, hitChild ;
		public String shortName() { return(name()); }
	} ;
	int nameW = 430;
	int lineW = 200;
	int step = 15;
	
	private void dispNode(Graphics gc, HitPoint pt,UCTID code,int x, int y, Object child,String extra)
	{
		GC.Text(gc,false,x,y,nameW,step,Color.black,null,child.toString()+((extra==null)?"":extra));
		if(G.pointInRect(pt,x,y,nameW,step))
		{
			pt.hitCode = code;
			pt.hitObject = child;
			GC.frameRect(gc,Color.blue,x,y,nameW,step);
		}
	}
	public void drawCanvas(Graphics gc, boolean complete,HitPoint pt)
	{	Rectangle rect = getBounds();
		Color bk1 = new Color(230,230,230);
		Color bk2 = new Color(230,230,250);
		GC.setColor(gc,getBackground());
		GC.fillRect(gc,rect);
			
			if(search!=null)
				{
				UCTNode root = search.root;
				if(root!=null)
				{
					int totalVisits = 0;
					
					int y = G.Top(rect);
					int x = G.Left(rect);
					int w = G.Width(rect);
					int row = 0;
					GC.fillRect(gc,((row&1)==0)?bk1:bk2,x,y,w,step);
					GC.Text(gc,false,x,y,x+nameW,y+step,Color.black,null,"Root: Time, TreeSize");
					GC.fillRect(gc,Color.black,x+nameW,y,(int)(search.partDone*lineW),2);
					GC.fillRect(gc,Color.blue,x+nameW,y+2,(int)(((double)search.stored_children/search.stored_child_limit)*lineW),2);
					y+= step;
					row++;
					if(G.pointInRect(pt,x,y,nameW,step))
					{
						pt.hitCode = UCTID.hitRoot;
						pt.hitObject = root;
						GC.frameRect(gc,Color.blue,x,y,nameW,step);
					}
	
					for(int i=1;i<path.size();i++)
					{	root = path.elementAt(i);
						commonMove kid = kids.elementAt(i);
						String bs = root.nodesBelowString();
						GC.fillRect(gc,((row&1)==0)?bk1:bk2,x,y,w,step);
						dispNode(gc,pt,UCTID.hitNode,x,y,root,
								"("+bs+")"
										+(kid==null?null:kid.playerString()+"["+kid.moveString()+"]"
										+(kid.gameover()?"T":"")));
						y += step;
						row++;
					}
					commonMove []children = root.cloneChildren();
					if(children!=null)
					{
					int nChildren = children.length;
					boolean uct = sortUCT.getState();
					for(int i=0;i<nChildren;i++)
					{
						commonMove kid = children[i];
						UCTNode n = kid.uctNode();
						if(n!=null) { kid.setEvaluation( uct 
										? n.getUct()
										: Math.abs(n.getVisits())); 
								}
						else { kid.setEvaluation(0); }
					}
					Sort.sort(children,false);
					
					for(int i=0;i<nChildren;i++)
					{
						commonMove child = children[i];
						if(child!=null)
						{
							UCTNode node = child.uctNode();
							if(node!=null) { totalVisits += Math.abs(node.getVisits()); }
						}
					}
					for(int i=0;i<nChildren;i++,y+=step,row++)
						{
						commonMove child = children[i];
						
						if(child!=null)
						{
						UCTNode n = child.uctNode();
						String bs = (n==null ) ? "0" : n.nodesBelowString();
						GC.fillRect(gc,((row&1)==0)?bk1:bk2,x,y,w,step);
						dispNode(gc,pt,UCTID.hitChild,x,y,child,(child.gameover()?"T":"")+"("+bs+")");
						
						UCTNode node = child.uctNode(); 
						if(node!=null)
						{
							int win1 = (int)(node.getDisplayWinRate()*lineW)+lineW/2;
							int win2 = lineW/2;
							int vis = (int)((double)Math.abs(node.getVisits())/totalVisits*lineW);
							if(win1<win2) { GC.fillRect(gc,Color.red,nameW+win1,y+4,win2-win1,2); }
							else { GC.fillRect(gc,Color.blue,nameW+win2,y+4,win1-win2,2); }
							GC.fillRect(gc,Color.black,nameW,y+6,vis,2);
						}
						}}
					}
				}
				}
			
			StockArt.SmallX.drawChip(gc,this,30,G.Left(pt),G.Top(pt),null);
	}

	public void StopDragging(HitPoint hp) 
	{
		if(hp.hitCode instanceof UCTID)
		{
		UCTID hitCode =(UCTID)hp.hitCode;
		switch(hitCode)
		{
		default: G.Error("Hit unknown %s",hitCode);
		case hitRoot:
			path.clear();
			kids.clear();
			if(search.root!=null) { path.push(search.root); kids.push(null); }
			break;
		case hitNode:
			UCTNode node = (UCTNode)hp.hitObject;
			UCTNode vic = null;
			while(path.size()>1 && (vic!=node))
			{
				vic = path.pop();
				kids.pop();
				
			}
			break;
		case hitChild:
			{
			commonMove m = (commonMove)hp.hitObject;
			UCTNode n = m.uctNode();
			if(n!=null)
				{
				G.print("selected node ",n);
				path.push(n);
				kids.push(m);
				}
			break;
			}
		}
		
	}
	}

}
