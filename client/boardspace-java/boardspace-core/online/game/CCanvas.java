package online.game;

import lib.Graphics;
import lib.HitPoint;

@SuppressWarnings("serial")
public abstract class CCanvas<CELLTYPE extends cell<?>,BOARDTYPE extends BoardProtocol> extends commonCanvas
{

	@SuppressWarnings("unchecked")
	public CELLTYPE hitCell(HitPoint hp) { return((hp.hitObject instanceof cell<?>) ? (CELLTYPE)hp.hitObject: null);}
	@SuppressWarnings("unchecked")
	public BOARDTYPE disB(Graphics gc) { return((BOARDTYPE)super.disB(gc)); }
	@SuppressWarnings("unchecked")
	public BOARDTYPE disB() { return((BOARDTYPE)super.disB()); }
	
}