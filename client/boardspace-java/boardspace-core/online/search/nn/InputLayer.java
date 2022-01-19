package online.search.nn;

import java.io.PrintStream;

import lib.G;
import lib.Random;

/**
 * specialization of a layer that generates input neurons 
 * @author Ddyer
 *
 */
public class InputLayer extends GenericLayer
{	public boolean isInputLayer() { return(true); }
	public Neuron makeNeuron(int seq) 
	{ return(new InputNeuron((layerName+"-"+seq),null));
	} 
	public InputLayer(String name,int size)
	{	super(name,size,null);		
	}

	public void connectFrom(Neuron prev[]) { G.Error("Can't connect an input layer"); }
	public void mapConnections() {  }

	public void initializeWeights(Random r,double offset,double scale) {};
	
	public boolean calculateErrorAndUpdateHiddenNeurons(double learningRate) 
	{	// doesn't apply to input layers
		return false;
	}
	public void printNetworkWeights(PrintStream s) { }

	public void copyWeights(Layer from) {	}
	public void dumpWeights(boolean values) {}
}
