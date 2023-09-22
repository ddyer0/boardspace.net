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
/* a minimal weight, shared by each neuron in a layer */
public class FilterWeight implements Weight
{
	String id;
	double value; 
	double previousValue;
	public FilterWeight(String name) { id=name; }
	public String getId() {
		return id;
	}
	public double getWeight() { return(value); }

	public void setWeight(double v) 
	{ 	previousValue = value;
		value = v; 
	}
	public void revertWeight() {
		value = previousValue;		
	}
}