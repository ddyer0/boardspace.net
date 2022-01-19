package online.search.nn;

public class BiasNeuron extends FCNeuron implements Neuron
{
	public BiasNeuron(String name,TransferFunction transfer,double v) { super(name,transfer); value=v; }
	public boolean isBiasNeuron() { return(true); }
	public void calculate() { }	// no calculation
	public void connectFrom(Neuron neurons[]) {}	// no need for from/to either
	public void setInput(double v) { };
}
