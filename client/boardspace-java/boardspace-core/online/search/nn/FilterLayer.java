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

import lib.G;
import lib.Random;


/** filter layer, one set of weights applies to all neurons.  The normal mode
 * is to connect to a corresponding neuron and all adjacent neurons.  this version
 * allows adjacent to be defined locally, as perhaps hexagonal or orthogonal.  we also
 * allow single connections without neighbors to additional layers.
*/
public class FilterLayer extends GenericLayer
{

class FilterNeuron extends SimpleNeuron
{	Neuron fromNeurons[];
	// constructor 
	public FilterNeuron(String l,TransferFunction f,double fill)
	{	super(l,f);
		fillValue = fill;
	}
	public void mapConnections()
	{
		for(Neuron n : fromNeurons) { if(n!=null) { n.addOutputConnection(this); }}
	}
	
	public Weight getWeightFrom(Neuron n)
	{
		for(int lim = fromNeurons.length-1;  lim>=0; lim--)
		{
			Neuron from = fromNeurons[lim];
			if(from==n) 
			{
				return(neighborWeights[lim]);
			}
		}
		throw G.Error("No connection from %s", n);
	}
	
	public TransferFunction getTransferFunction() 
	{
		return(transferFunction);
	}
	
	public void connectFrom(Neuron neurons[])
	{	
		fromNeurons = neurons;
	}
	
	public void calculate() 
	{
		double nextVal = 0;
		for(int lim=neighborWeights.length-1;lim>=0;lim--)
		{	Neuron from = fromNeurons[lim];
			double weight = neighborWeights[lim].getWeight();
			if(from!=null) { nextVal += weight*from.getValue(); }
			else { nextVal += fillValue*weight; }
		}
		setValue(getTransferFunction().getOutput(nextVal));		
	}
	public void updateNeuronWeights(double learningRate)
	{
       // iterate through all neuron's input connections
		double totalContributions = getValue();
		double totalError = getError();
		
		for(int lim = fromNeurons.length-1; lim>=0; lim--)
		{
			Neuron inputNeuron = fromNeurons[lim];
			if(inputNeuron!=null)
			{
			Weight inputWeight = neighborWeights[lim];
			double input = inputNeuron.getValue();
            double weight = inputWeight.getWeight();
            double rawContrib = Math.abs(input*weight)/totalContributions;
            double contribution = Math.max(0.001,Math.min(0.1,rawContrib));
        	double neuronError = totalError*contribution;
            
            // calculate the weight change
            double weightChange = learningRate * neuronError * input;
            double newWeight = weight + weightChange;
            // set the new connection weight
            inputWeight.setWeight(newWeight);
        	}}
        }

	public Connection[] getInputConnections() {
		throw G.Error("Shouldn't be called");
	}

	public Weight[] getWeights() {
		return neighborWeights;
	}

}
	FilterWeight neighborWeights[];
	CoordinateMap coordinateMap;
	double fillValue = 0;
	
	// singlelayers are additional layers where only the center of the neighborhood
	// is sampled
	Layer singleLayers[] = null;
	
	public Neuron makeNeuron(int seq) 
	{ return(new FilterNeuron(layerName+"-"+seq,transferFunction,fillValue)); 
	}

	public Neuron[] getNeurons() { return(neurons); }
	
	// constructor
	public FilterLayer(String name,int size,TransferFunction f,CoordinateMap map,Layer[]single) 
	{	this(name,f,map);
		singleLayers = single;
		setSize(size);
	}
	// constructor
	public FilterLayer(String name,TransferFunction f,CoordinateMap map) 
	{	super(name,f);
		coordinateMap = map;		
	}
	
	public void setSize(int n)
	{
		super.setSize(n);
		createWeights();
	}
	public void createWeights()
	{
		int nweights = coordinateMap.getNNeighbors();
		// the number of weights is the number of neighbors, 
		// plus the center neuron, plus the center neuron for each 
		// of the additional layers.
		createWeights(nweights+1+(singleLayers==null?0:singleLayers.length));
	}
	public void createWeights(int nweights)
	{
		neighborWeights = new FilterWeight[nweights];
		String nname = getNeurons()[0].getName();
		for(int i=0;i<nweights; i++)
		{	neighborWeights[i] = new FilterWeight(""+i+" "+nname);
		}
	}
	public void connectFrom(Neuron fromNeurons[])
	{	
		Neuron [] toNeurons = getNeurons();
		G.Assert(fromNeurons.length == toNeurons.length,"must have the same size");
		for(int lim = toNeurons.length-1; lim>=0; lim--)
			{
			Neuron to = toNeurons[lim];
			if(!to.isBiasNeuron())
			{
			int nWeights = coordinateMap.getNNeighbors();
			int toInd = 0;
			Neuron fromConn[] = new Neuron[nWeights+1+(singleLayers==null?0:singleLayers.length)];
			fromConn[toInd++] = fromNeurons[lim];
			if(singleLayers!=null)
			{
				for(Layer l : singleLayers)
				{	fromConn[toInd++] = l.getNeurons()[lim];	
				}
				
			}
			for(int i=0;i<nWeights;i++)
			{	int dest = coordinateMap.getNeighborIndex(lim,i);
				if(dest>=0) { fromConn[toInd++] = fromNeurons[dest]; }
				else { toInd++; }	// keep the neurons in positional sync with the weights
			}
			to.connectFrom(fromConn);
			}
			
			}
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
		Weight from[] = ((FilterLayer)fromlayer).neighborWeights;
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
