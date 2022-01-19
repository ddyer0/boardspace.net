package online.search.nn;

import java.io.PrintStream;

public interface Network
{	public Network duplicateInstance();
	public String getName();
	public String getInfo();
	public void setFromFile(String file);
	public Neuron[] getOutputNeurons();
	public Neuron[] getInputNeurons();
	public Layer[] getLayers();
	public void calculate();
	public double[] getValues();
	public double[] getValues(String layer);
	public double[] getValues(Layer l);
	public double getTotalValues();
	public void learn(double expected[]);
	public void learn(Layer l,double expected[]);
	public CoordinateMap getCoordinateMap();
	/** calculate the network values for an input vector */
	public double[] calculateValues(int layer,double...in);
	
	public double[] calculateValues(Layer layer,double...in);
	
	public double[] calculateValues(int layer,double in[],int layer2,double[] in2);
	
	public double[] calculateValues(Layer layer,double in[],Layer layer2,double[] in2);
	
	public double[] calculateValues(Layer layer,double in[],Layer layer2,double[] in2,Layer layer3,double in3[]);

	public double[] calculateValues(Layer layer,double in[],Layer layer2,double[] in2,Layer layer3,double in3[],Layer Layer4,double value[]);

	public Layer getLayer(String id);

	public void saveNetwork(PrintStream stream,String comment);
	public int inputNetworkSize();
	
	public void saveNetwork(String file,String comment);
	
	public void dumpWeights(boolean values);

	public void copyWeights(Network from);
	
	public Network duplicate();
}
