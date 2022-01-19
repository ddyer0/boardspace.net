package online.search.nn;

public abstract class GenericLayer extends BaseLayer implements Layer
{
	Neuron neurons[];
	TransferFunction transferFunction;
	String layerName;			// true name for interconnection
	String id=null ;			// active use ID for external reference
	private double totalValues = 0;
	public double getTotalValues() { return(totalValues); }
	public void setTotalValues(double v) { totalValues = v; }
	public String getID() { return(id==null ? layerName : id); }
	public void setID(String n) { id = n; }
	public String getName() { return(layerName); }
	@SuppressWarnings("deprecation")
	public String toString() { return("<"+getClass().getName()+" "+layerName+">"); }
	public Neuron[] getNeurons() { return(neurons);}
	public Neuron makeNeuron(int seq) { return(new FCNeuron(layerName+"-"+seq,transferFunction)); }
	public GenericLayer(String name,TransferFunction f)
	{
		transferFunction = f;
		layerName = name;
	}
	public GenericLayer(String name,int size,TransferFunction f)
	{	this(name,f);
		setSize(size);
	}
	public void setSize(int size)
	{
		neurons = new Neuron[size];
		for(int idx=size-1; idx>=0; idx--)
		{	
			neurons[idx] = makeNeuron(idx);
		}
	}
	public boolean calculateErrorAndUpdateHiddenNeurons(double learningRate) 
	{	boolean rangeError = false;
		for(Neuron neuron : getNeurons()) {	
                            // calculate the neuron's error (delta)
			double neuronError = neuron.calculateHiddenNeuronError(); 
			rangeError |= neuron.setError(neuronError);
			neuron.updateNeuronWeights(learningRate);
		} // for
		return(rangeError);
	}
}
