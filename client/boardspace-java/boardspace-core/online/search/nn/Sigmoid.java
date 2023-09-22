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

import static java.lang.Math.*;

// cribbed from org.neuroph.core.transfer.Sigmoid;

public class Sigmoid implements TransferFunction
{
	private double slope = 1d;
	private double scale = 1.0;
	private double bias = 0;
	public void setBias(double b) { bias = b; }
	public void setScale(double s) { scale = s; }
	public boolean outOfRange(double net)
	{
		return(net>100 || net<-100);
	}
	public double getOutput(double net) 
	{
        // conditional logic helps to avoid NaN
        if (net > 100) {
           return (1.0+bias)*scale;
        }else if (net < -100) {
           return (0.0+bias)*scale;
        }

        double den = 1d + exp(-this.slope * net);
        return((1d / den + bias)*scale);
	}

public double getDerivative(double out) 
	{ 	
    // +0.1 is fix for flat spot see http://www.heatonresearch.com/wiki/Flat_Spot
	double output = (out/scale)-bias;
	double derivative = slope * output * (1d - output) + 0.1;
	return derivative;
	}

}
