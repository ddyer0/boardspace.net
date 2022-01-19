package online.search;

import java.util.StringTokenizer;

import lib.DStack;
import lib.G;

public abstract class DefaultEvaluator implements Evaluator
{
	public void setWeights(String str)
	{
		DStack d = new DStack();
		StringTokenizer tok = new StringTokenizer(str);
		while(tok.hasMoreTokens())
		{
			d.push(G.DoubleToken(tok.nextToken()));
		}
		setWeights(d.toArray());
	}
}
