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

public class Connection implements Weight
{
	Neuron fromNeuron;
	double weight = 1;
	double previousWeight;
	String id;
	public String getId() { return(id); }
	
	public double getWeight() { return(weight); }
	public void revertWeight() { weight = previousWeight; }
	public void setWeight(double w) { previousWeight=weight; weight = w; }
	public Neuron getFromNeuron() { return(fromNeuron); }
	public Connection(Neuron from,Neuron to) 
	{ fromNeuron = from;
	  id = fromNeuron.getName()+" "+to.getName();
	}
	public String toString() { return("<conn "+getId()+" "+weight+">");}
	public void copyWeights(Connection from)
	{
		setWeight(from.getWeight());
	}

}
