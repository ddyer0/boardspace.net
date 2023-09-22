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

public interface Network
{	public Network duplicateInstance();
	public String getName();
	public String getInfo();
	public void setFromFile(String file);
	public Neuron[] getOutputNeurons();
	public Neuron[] getInputNeurons();
	public Layer[] getLayers();
	public void calculate();
	public double[] getValues();
	public double[] getValues(String layer);
	public double[] getValues(Layer l);
	public double getTotalValues();
	public void learn(double expected[]);
	public void learn(Layer l,double expected[]);
	public CoordinateMap getCoordinateMap();
	/** calculate the network values for an input vector */
	public double[] calculateValues(int layer,double...in);
	
	public double[] calculateValues(Layer layer,double...in);
	
	public double[] calculateValues(int layer,double in[],int layer2,double[] in2);
	
	public double[] calculateValues(Layer layer,double in[],Layer layer2,double[] in2);
	
	public double[] calculateValues(Layer layer,double in[],Layer layer2,double[] in2,Layer layer3,double in3[]);

	public double[] calculateValues(Layer layer,double in[],Layer layer2,double[] in2,Layer layer3,double in3[],Layer Layer4,double value[]);

	public Layer getLayer(String id);

	public void saveNetwork(PrintStream stream,String comment);
	public int inputNetworkSize();
	
	public void saveNetwork(String file,String comment);
	
	public void dumpWeights(boolean values);

	public void copyWeights(Network from);
	
	public Network duplicate();
}
