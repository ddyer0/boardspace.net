package online.search.nn;

public interface TransferFunction {
	public void setBias(double b);
	public void setScale(double s);
	public double getOutput(double net);
	public double getDerivative(double net);
	public boolean outOfRange(double net);
}
