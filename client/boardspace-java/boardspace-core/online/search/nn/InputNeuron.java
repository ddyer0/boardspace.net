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
