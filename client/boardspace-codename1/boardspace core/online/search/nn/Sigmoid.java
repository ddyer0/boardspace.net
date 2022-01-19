package online.search.nn;

import static com.codename1.util.MathUtil.*;

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
