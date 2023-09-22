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
