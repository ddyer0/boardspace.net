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
