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