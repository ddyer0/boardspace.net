package rithmomachy;

import lib.Random;
import lib.OStack;

class CaptureStack extends OStack<CaptureDescription>
{
	public CaptureDescription[] newComponentArray(int n) { return(new CaptureDescription[n]); }
}
public class CaptureDescription implements RithmomachyConstants
{
	CaptureType type;				// the type of capture performed
	RithmomachyCell victim;			// the piece removed
	RithmomachyCell attacker;		// the primary attacker, usually the piece moved
	RithmomachyCell also_attacker;	// second capturer for capture by eruption
	RithmomachyChip victimChip;		// the chip captured.
	RithmomachyChip attackerChip;
	RithmomachyChip also_attackerChip;
	long Digest(Random r)
	{	long v = RithmomachyCell.Digest(r,victim);
		v ^= RithmomachyCell.Digest(r,attacker);
		v ^= RithmomachyCell.Digest(r,also_attacker);
		v ^= RithmomachyChip.Digest(r,victimChip);
		v ^= RithmomachyChip.Digest(r,attackerChip);
		v ^= RithmomachyChip.Digest(r,also_attackerChip);
		return(v);
		
	}
	// main constructor
	public CaptureDescription(CaptureType tt,RithmomachyCell v,RithmomachyChip vi,
				RithmomachyCell a1,RithmomachyChip a1i,
				RithmomachyCell a2, RithmomachyChip a2i)
	{
		type = tt;
		attacker = a1;
		also_attacker = a2;
		victim = v;
		victimChip = vi;
		attackerChip = a1i;
		also_attackerChip = a2i;
		
	}
	CaptureDescription copy() 
	{ 	return(new CaptureDescription(type,victim,victimChip,attacker,attackerChip,also_attacker,also_attackerChip));
	}	
	public int attackerValue() { return((attackerChip==null) ? attacker.stackValue() : attackerChip.value); }
	public int also_attackerValue() { return((also_attackerChip==null) ? also_attacker.stackValue() : also_attackerChip.value); }

}