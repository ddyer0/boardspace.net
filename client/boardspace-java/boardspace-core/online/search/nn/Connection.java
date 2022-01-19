package online.search.nn;

public class Connection implements Weight
{
	Neuron fromNeuron;
	double weight = 1;
	double previousWeight;
	String id;
	public String getId() { return(id); }
	
	public double getWeight() { return(weight); }
	public void revertWeight() { weight = previousWeight; }
	public void setWeight(double w) { previousWeight=weight; weight = w; }
	public Neuron getFromNeuron() { return(fromNeuron); }
	public Connection(Neuron from,Neuron to) 
	{ fromNeuron = from;
	  id = fromNeuron.getName()+" "+to.getName();
	}
	public String toString() { return("<conn "+getId()+" "+weight+">");}
	public void copyWeights(Connection from)
	{
		setWeight(from.getWeight());
	}

}
