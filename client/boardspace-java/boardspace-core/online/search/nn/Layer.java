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

import lib.OStack;
import lib.Random;

class LayerStack extends OStack<Layer>
{
	public Layer[] newComponentArray(int sz) { return(new Layer[sz]);	}
}

public interface Layer 
{	/** calculate the values for this layer, using the
		current values of the inputs.  
	*/
	public void calculate();

	/** map the output connections in preparation for learning.  This builds strucures
	 * that are not needed when only using an existing network. */
	public void mapConnections();

	
	/** set the input values for this layer.  This is only
	 * valid for an input layer
	 * @param inputs
	 */
	public void setInputs(double... inputs);
	
	public void setTotalValues(double n);
	/* this is the total of the values returned by getValues() which we maintain
	 * so callers won't have to total them again
	 */
	public double getTotalValues();
	/** get an array of the values of this layer */
	public double[] getValues();
	
	/** get the neurons in this layer */
	public Neuron[] getNeurons();
	/** connect this layer to each of an array of neurons */
	public void connectFrom(Neuron prev[]);
	
	/** connect this layer to an array of previous layers */	
	public void connectFrom(Layer... prev);
	
	/** copy the weights of this layer from the weights of an equivalent layer */
	public void copyWeights(Layer from);
	
	
	/* initialize the weights in this layer to random values. For example, 
	 *  initializeWeights(new Random(),-0.5,2.0) would initialize weights
	 *  to random values in the range -1 to 1 */
	public void initializeWeights(Random r,double offset,double scale);
	
	public boolean calculateErrorAndUpdateHiddenNeurons(double learningRate);
	/**
	 * print the weights in a readable format
	 * @param s
	 */
	public void printNetworkWeights(PrintStream s);
	public void dumpWeights(boolean values);
	public String getName();
	public String getID();
	public void setID(String n);
	public boolean isOutputLayer();
	public boolean isInputLayer();
}
