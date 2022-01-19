package online.search.nn;

import static com.codename1.util.MathUtil.*;

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
