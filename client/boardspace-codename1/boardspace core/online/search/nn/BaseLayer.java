package online.search.nn;

public abstract class BaseLayer implements Layer
{	public boolean isOutputLayer() { return(false); }
	public boolean isInputLayer() { return(false); }
	public void calculate()
	{
		for(Neuron n : getNeurons()) { n.calculate(); }
	}
	public void mapConnections() 
	{
		for(Neuron n : getNeurons()) { n.mapConnections(); }
	}
	public void setInputs(double... inputs) {
		Neuron neurons[] = getNeurons();
		for(int idx = inputs.length-1; idx>=0; idx--)
		{
			neurons[idx].setInput(inputs[idx]);
		}
	}	
	public double[] getValues() 
	{
		Neuron n[] = getNeurons();
		int len = n.length;
		double v[] = new double[len];
		double tot = 0;
		for(int i=0;i<len;i++) 
			{ double v0 = n[i].getValue();
			  tot += v0;
			  v[i] = v0;
			}
		setTotalValues(tot);
		return(v);
	}
	public void connectFrom(Layer... prev)
	{	NeuronStack nn = new NeuronStack();
		for(Layer l : prev) { for(Neuron n : l.getNeurons()) { nn.push(n); }}
		connectFrom(nn.toArray());
	}
}
