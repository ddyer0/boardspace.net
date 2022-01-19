package online.search.nn;

public class Linear implements TransferFunction
{
	private double scale = 1.0;
	private double bias = 0;
	public void setScale(double s) { scale = s; }
	public void setBias(double b) { bias = b; }
	public double getOutput(double net) 
	{	return((net+bias)*scale);
	}

	public double getDerivative(double output) 
	{ 	
    return(scale);
	}
	public boolean outOfRange(double net) { return(false); }
}
