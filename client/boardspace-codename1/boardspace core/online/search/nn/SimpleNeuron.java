/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.

    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/.
 */
package online.search.nn;

abstract public class SimpleNeuron extends BaseNeuron implements Neuron
{
	TransferFunction transferFunction;
	String nameLoc="";
	protected double value;
	protected double error;
	protected double previousError;
	protected int outOfRange = 0;
	NeuronStack outputConnections = null;
	public Neuron[] getOutputConnections() 
	{	if(outputConnections!=null) { return(outputConnections.toArray()) ; }
		return(null);
	}
	public void addOutputConnection(Neuron n)
	{
		if(n!=null)
		{
			if(outputConnections==null) { outputConnections = new NeuronStack(); }
			outputConnections.push(n);
		}
	}
	public SimpleNeuron(String l,TransferFunction f)
	{	nameLoc = l;
		transferFunction = f;
	}
	public String toString() 
	{ return("<n("
			+ nameLoc+" "
			+value+">");
	}
	public String getName() { return(nameLoc); }
	public TransferFunction getTransferFunction() 
	{
		return(transferFunction);
	}
	public void revertError() { error = previousError; }
	public boolean setError(double d) 
	{ 
	  previousError = error;
	  error = d; 
	  if(getTransferFunction().outOfRange(getValue()+d*2)) 
	  	{ setRangeError(true); return(true); 
	  	}
	  return(false);
	}
	public void setRangeError(boolean val)
	{	//G.print("Out of range");
		outOfRange++;
	}
	public double getValue() { return(value); }
	public void setValue(double v) { value = v; }
	public double getError() { return(error); }
	public void accumulateError(double e) { error+= e; }

	public void setInput(double v) 
	{
		throw new Error("Can't set the value of a non-input neuron");
	}
	/*
	 * Calculates and returns the neuron's error (neuron's delta) for the given neuron param
	 * 
	 * @param neuron
	 *            neuron to calculate error for
	 * @return neuron error (delta) for the specified neuron
	 */
	public double calculateHiddenNeuronError() {		
		double deltaSum = 0d;

		Neuron toNeurons[] = getOutputConnections();
		
		if(toNeurons!=null)
		{
			for(Neuron connection : toNeurons) 
			{	
			double delta = connection.getError()
							* connection.getWeightFrom(this).getWeight();
			deltaSum += delta; // weighted delta sum from the next layer
			} // for
		}

		TransferFunction transferFunction = getTransferFunction();
		double value = getValue(); // should we use input of this or other neuron?
		double f1 = transferFunction.getDerivative(value);
		double neuronError = f1 * deltaSum;
		return neuronError;
	}
}
