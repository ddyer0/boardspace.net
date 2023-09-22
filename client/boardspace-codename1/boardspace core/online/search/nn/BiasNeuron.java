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

public class BiasNeuron extends FCNeuron implements Neuron
{
	public BiasNeuron(String name,TransferFunction transfer,double v) { super(name,transfer); value=v; }
	public boolean isBiasNeuron() { return(true); }
	public void calculate() { }	// no calculation
	public void connectFrom(Neuron neurons[]) {}	// no need for from/to either
	public void setInput(double v) { };
}
