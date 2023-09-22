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
