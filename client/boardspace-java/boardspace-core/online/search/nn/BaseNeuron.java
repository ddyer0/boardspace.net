package online.search.nn;

public abstract class BaseNeuron implements Neuron
{
	public boolean isBiasNeuron() { return(false); }
	
	public void revertConnectionWeights() 
	{	revertError();
	        Weight weights[] = getWeights();
	        if(weights!=null)
	        {
	        for (Weight w : weights) {
	            // get the input from current connection
	            w.revertWeight();

	        }}
	}
	public void copyWeights(Neuron from)
	{
		Weight toConn[] = getWeights();
		Weight fromConn[] = from.getWeights();
		if(toConn!=null)
			{for(int cn = 0; cn<toConn.length; cn++)
				{	toConn[cn].setWeight(fromConn[cn].getWeight());
				}
			}
	}
}
