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

public class Log implements TransferFunction
{	private double bias = 0;
	private double scale = 1.0;
	public void setScale(double s) { scale = s; }
	public void setBias(double b) { bias = b; }
	    public double getOutput(double net) {
	        return (log(net)+bias)*scale;
	    }
	   
	public double getDerivative(double net) {
		return (1/((net/scale)-bias));
	    }       
	public boolean outOfRange(double net) { return(false); }

}
