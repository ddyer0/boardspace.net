
package goban.shape.shape;

import com.codename1.io.Externalizable;

/** ResultProtocol is an interface type implemented by two subclasses,
which correspond to exact results and results exact except for the number
of outside liberties */
public interface ResultProtocol extends Externalizable
{
	public int N_Adjacent_Groups();
	SingleResult Fate_for_N_Liberties(int n);
	int Min_Liberties();
	int Max_Liberties(); 	
	public ResultProtocol ReOrder(int order[]);
	public ResultProtocol LowerOrdinals(int val);
}
