package online.search.nn;


import java.io.PrintStream;

import lib.G;
import lib.Random;

/** pool layer. One set of weights applies to all neurons, which 
 * are structured to do a 4:1 reduction.  So the size of this layer
 * is specified by the input layer/4 rather than the nominal size
 * given in the network initialization
 * 
 * This implementation of a pool layer with weights and a transfer
 * function is a little different from the conventional, where the
 * combination phase would be simple MAX 
*/
public class PoolLayer extends FilterLayer
{
	public Neuron[] getNeurons() { return(neurons); }
	
	// constructor
	public PoolLayer(String name,TransferFunction f,CoordinateMap map) 
	{	super(name,f,map);
	
	}
	
	public void createWeights()
	{
		createWeights(4);
	}

	/** connect this layer to an array of previous layers */	
	public void connectFrom(Layer... prev)
	{	
	  	int nrows = coordinateMap.getNRows();
	  	int ncols = coordinateMap.getNCols();
	  	int nneurons = ((nrows+1)/2)*((ncols+1)/2);
	  	setSize(nneurons*prev.length);
	  	int idx = 0;
	  	
		for(Layer l : prev)
		{ Neuron [] fromNeurons = l.getNeurons();
		  Neuron toNeurons[] = getNeurons();
		  for(int colnum = 0; colnum<ncols; colnum+=2)
		  { char col = (char)('A'+colnum);
		    for(int row = 1;row<=nrows; row+=2)
		  {	
		    	int center = coordinateMap.getIndex(col,row);
		    	int left = coordinateMap.getIndex(col,row+1);
		    	int top = coordinateMap.getIndex((char)(col+1),row);
		    	int diag = coordinateMap.getIndex((char)(col+1),row+1);
		    	toNeurons[idx].connectFrom(new Neuron[]
		    				{ center>=0 ? fromNeurons[center] : null,
		    				  left>=0 ? fromNeurons[left] : null,
		    				  top>=0 ? fromNeurons[top] : null,
		    				  diag>=0 ? fromNeurons[diag] : null});
		    	idx++;
		  }}}
	}

	public void connectFrom(Neuron fromNeurons[])
	{	G.Error("shouldn't be called");
	}
	

	public void initializeWeights(Random r,double offset,double scale)
	{	for(int i=0;i<neighborWeights.length;i++) 
		{ neighborWeights[i].setWeight((r.nextDouble()+offset)*scale); 
		}
	}

	public void printNetworkWeights(PrintStream out)
	{
		for(FilterWeight w : neighborWeights)
		{
			out.print(w.getId());
			out.print(" ");
			out.print(w.getWeight());
			out.print(" ");
		}	
		out.println();
		out.println();
	}
	
	public void copyWeights(Layer fromlayer)
	{	Weight to[] = neighborWeights;
		Weight from[] = ((PoolLayer)fromlayer).neighborWeights;
		for(int i=0;i<to.length; i++) { to[i].setWeight(from[i].getWeight()); }
	}
	public void dumpWeights(boolean values)
	{			
			String val = "";
			for(Weight w : neighborWeights)
			{
				val += " "+(int)(w.getWeight()*100);
			}
			System.out.println(val);
	}	

}
