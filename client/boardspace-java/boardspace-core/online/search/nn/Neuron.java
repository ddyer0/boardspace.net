package online.search.nn;

import lib.OStack;

class NeuronStack extends OStack<Neuron>
{
	public Neuron[] newComponentArray(int sz) { return(new Neuron[sz]);	}
}
public interface Neuron 
{	/** get the unique name of this neuron */
	public String getName();
	/** get the current value of this neuron */
	public double getValue();
	/** set the current value of this neuron (only allowed for input neurons) */
	public void setValue(double v);
	/** map over all incoming connections, adding this node as an output connection.
	 * this is a preliminary step to applying learning to a neuron
	 */
	public void mapConnections(); 
	/**
	 * add a single output connection to this node.
	 * @param to
	 */
	public void addOutputConnection(Neuron to);
	/**
	 * 
	 * @return get the current error of this node
	 */
	public double getError();
	/**
	 * set the current error of this node
	 * @param i
	 * @return the error
	 */
	public boolean setError(double i);
	/**
	 * revert the error to its previous value
	 */
	public void revertError();
	/**
	 * run the forward calculation for this node
	 */
	public void calculate() ;
	/**
	 * note that the input value of this node exceeds the valid range
	 * @param val
	 */
	public void setRangeError(boolean val);
	/**
	 * 
	 * @return true if this is a bias neuron
	 */
	public boolean isBiasNeuron();
	/** set the value of this neuron.  This will only be valid
	 * for input neurons.
	 * @param v
	 */
	public void setInput(double v);
	/** connect the list of neurons as the inputs */
	public void connectFrom(Neuron neurons[]);
	/**
	 * This method is used
	 * for fully connected layers, but not for filter layers
	 * @return the input connections for this neuron
	 */
	public Connection[] getInputConnections();
	/**
	 * This is used when saving a network
	 * @return get the weights for this neuron.
	 */
	public Weight[] getWeights();
	/**
	 * get a list of the neurons for which this neuron is an input.  This is 
	 * needed only when learning.
	 * @return an array of weights
	 */
	public Neuron[] getOutputConnections();
	/**
	 * get the transfer function for this neuron (shared with all neurons in a layer)
	 * @return the transfer function
	 */
	public TransferFunction getTransferFunction();
	
	public void updateNeuronWeights(double learningRate);
	public double calculateHiddenNeuronError() ;
	public Weight getWeightFrom(Neuron n);
	
	/** revert the weights associated with this neuron to their previous value.
	 * This is used to back out of a failed learning step
	 */
	public void revertConnectionWeights(); 
	
	/**
	 * copy the weights from a comparable neuron
	 * @param from
	 */
	public void copyWeights(Neuron from);

	
}
