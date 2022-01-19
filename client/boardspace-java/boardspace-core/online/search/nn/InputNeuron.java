package online.search.nn;

import lib.G;

public class InputNeuron extends SimpleNeuron implements Neuron 
{	
	public InputNeuron(String l, TransferFunction f) 
	{
		super(l, f);
	}
	public void setInput(double val) { value=val;}
	public void calculate() {}
	public void updateNeuronWeights(double learningRate) {};
	public double calculateHiddenNeuronError() { return(0); }
	public void connectFrom(Neuron[] neurons) {
		G.Error("Can't connect an input neuron");
	}

	public Connection[] getInputConnections() {	return null; }
	public Weight[] getWeights() { return(null); }
	public void mapConnections() { }
	public Weight getWeightFrom(Neuron from) { throw G.Error("Shouldn't be called"); }

}
