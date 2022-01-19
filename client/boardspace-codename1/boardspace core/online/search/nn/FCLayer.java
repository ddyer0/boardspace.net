package online.search.nn;

import java.io.PrintStream;

import lib.Random;
/** fully connected layer */
public class FCLayer extends GenericLayer 
{
	public FCLayer(String name,int size,TransferFunction f)
	{
		super(name,size,f);
	}

	public void connectFrom(Neuron fromNeurons[])
	{
		for(Neuron n : getNeurons()) { n.connectFrom(fromNeurons); }
	}

	public void initializeWeights(Random r,double offset,double value)
	{	for(Neuron nns : getNeurons())
			{ Weight conns[] = nns.getWeights();
			  if(conns!=null) 
			  	{ for(Weight c : conns) 
			  		{
			  			c.setWeight((r.nextDouble()+offset)*value); 
			  		}
			  	}
			}
		  
	}
	public void copyWeights(Layer from)
	{
		Neuron[] toNe = getNeurons();
		Neuron[] fromNe = from.getNeurons();
		for(int nn = 0;nn<toNe.length;nn++)
		{	toNe[nn].copyWeights(fromNe[nn]);
		}
	}
	
	public void printNetworkWeights(PrintStream out)
	{
			Neuron neurons[] = getNeurons();
			for(Neuron neuron : neurons)
			{
			
				Weight conns[] = neuron.getWeights();
				if(conns!=null)
				{
					for(Weight conn : conns)
					{	if(conn!=null)
						{
						out.print(conn.getId());
						out.print(" ");
						out.print(conn.getWeight());	
						out.print(" ");
						}
					}
					out.println();
				}
				
			}
			out.println();
		}
	public void dumpWeights(boolean values)
	{			
			String val = "";
			for(Neuron n : getNeurons())
			{	
			if(!values)
				{Connection [] conns = n.getInputConnections();
			if(conns!=null)
			{
				for(Connection c : conns) 
				{
					val += " "+(int)(c.getWeight()*100);
				}
			}}
			val += "="+n.getValue()+"("+n.getError()+") ";
			}
			System.out.println(val);
		}
}
