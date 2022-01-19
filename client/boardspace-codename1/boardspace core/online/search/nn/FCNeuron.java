package online.search.nn;

import lib.G;

/**
 * a fully connected neuron, all connections are explicit and fixed
 * @author Ddyer
 *
 */
public class FCNeuron extends SimpleNeuron
{	Connection inputConnections[] = null;
	public Connection[] getInputConnections() { return(inputConnections); }
	public Weight[] getWeights() { 	return(inputConnections);	}
	
	public FCNeuron(String l,TransferFunction f) { super(l,f); }	


	public void connectFrom(Neuron neurons[]) {
		
		int siz = neurons.length;
		inputConnections = new Connection[siz];
		for(int lim=siz-1; lim>=0; lim--)
		{
			inputConnections[lim] = new Connection(neurons[lim],this);
		}
	}
	/**
	 * calculate the current value of this neuron
	 */
	public void calculate() 
	{
		double nextVal = 0;
		Connection connections[] = getInputConnections();
		if(connections!=null)
		{
		for(Connection conn : connections)
		{	
			nextVal += conn.getFromNeuron().getValue()*conn.getWeight();
		}}
		setValue(getTransferFunction().getOutput(nextVal));		
	}

	public void updateNeuronWeights(double learningRate)
	{
        // get the error(delta) for specified neuron,

        // tanh can be used to minimise the impact of big error values, which can cause network instability
        // suggested at https://sourceforge.net/tracker/?func=detail&atid=1107579&aid=3130561&group_id=238532
        // double neuronError = Math.tanh(neuron.getError());
        
        // iterate through all neuron's input connections
        Connection conns[] = getInputConnections();
        if(conns!=null)
        {
        double totalContributions = getValue();
        double totalError = getError();
	    
        for (Connection connection : conns) {
            // get the input from current connection
            double input = connection.getFromNeuron().getValue();
            double weight = connection.getWeight();
            double rawContrib = Math.abs(input*weight)/totalContributions;
            double contribution = Math.max(0.001,Math.min(0.1,rawContrib));
        	double neuronError = totalError*contribution;
            
            // calculate the weight change
            double weightChange = learningRate * neuronError * input;
            double newWeight = weight + weightChange;
            // set the new connection weight
            connection.setWeight(newWeight);
        	}
        }
    }
	public Weight getWeightFrom(Neuron n)
	{
		Connection connections[] = getInputConnections();
		if(connections!=null)
		{
			for(Connection conn : connections)
			{
				if(conn.getFromNeuron()==n) { return(conn); }
			}
		}
		throw G.Error("Connection from %s not found",n);
	}

	
	public void mapConnections() 
	{
		Connection connections[] = getInputConnections();
		if(connections!=null)
		{
			for(Connection c : connections) 
				{ c.getFromNeuron().addOutputConnection(this); 
				}
		}
	}

}
