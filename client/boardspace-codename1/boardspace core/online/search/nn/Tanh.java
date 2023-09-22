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
import static com.codename1.util.MathUtil.*;

public class Tanh implements TransferFunction
{	double slope = 1.0;
	double bias = 0;
	double scale = 1.0;
	public void setScale(double s) { scale = s; }
	public void setBias(double b) { bias = b; }
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
            return (-1.0+bias)*scale;
        }
        double E_x = exp(slope * net);                
        return (((E_x - 1d) / (E_x + 1d)+bias)*scale);
 	}

	public double getDerivative(double out) 
	{	double output = out/scale-bias;
		return (1d - output * output);	// plus .1 fixes the flat spot
	}

}
