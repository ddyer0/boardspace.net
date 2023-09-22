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
