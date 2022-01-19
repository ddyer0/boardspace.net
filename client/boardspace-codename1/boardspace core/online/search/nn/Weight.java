package online.search.nn;

public interface Weight 
{
	public String getId();
	public double getWeight();
	public void setWeight(double v);
	public void revertWeight();
}
