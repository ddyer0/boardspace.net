package online.search.neat;
/*
Copyright 2006-2025 by Dave Dyer

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
import java.io.PrintStream;

import lib.CompareTo;
import lib.G;
import lib.IoAble;
import lib.OStack;
import lib.StackIterator;
import lib.Tokenizer;

class ConnectionGeneStack extends OStack<ConnectionGene>
{
	public ConnectionGene[] newComponentArray(int sz) {
		return new ConnectionGene[sz];
	}
	
}
public class ConnectionGene implements IoAble,CompareTo<ConnectionGene>,StackIterator<ConnectionGene>
{
	
	private int inNode;
	private int outNode;
	
	// store weights as integers so they read/write compactly, and also to avoid
	// the butterfly effect of saving/reloading producing slightly different behavior
	private int actualWeight;
	private static double weightScale = 0.0001;
	
	private boolean expressed;
	private int innovation;		// innovation number for this connection
	private static int highestInnovationNumber = 0;
	
	public ConnectionGene next;
	
	public double getWeight() { return actualWeight*weightScale; }
	public void setWeight(double v) 
		{ actualWeight = (int)(v/weightScale); 
		}
	/**
	 * innovation numbers have to be globally unique, When deliberately copying a connection
	 * into a new genome with the same number, that means this is the same gene with the same
	 * endpoints, but potentially a different weight.
	 * @return
	 */
	private int newConnectionNumber()
	{	highestInnovationNumber++;
		return highestInnovationNumber;
	}
	public String toString()
	{
		return "<conn#"+innovation+" "+inNode+"-"+outNode+">";
	}
	public ConnectionGene(NodeGene inNode, NodeGene outNode, double weight, boolean expressed) {
		this.inNode = inNode.getId();
		this.outNode = outNode.getId();
		G.Assert(inNode!=outNode,"can't connect to self");
		this.actualWeight = (int)(weight/weightScale);
		this.expressed = expressed;
		this.innovation = newConnectionNumber();
	}
	
	public ConnectionGene(NodeGene inNode, NodeGene outNode, double weight, boolean expressed, int innovation) {
		highestInnovationNumber = Math.max(highestInnovationNumber,innovation);
		this.inNode = inNode.getId();
		this.outNode = outNode.getId();
		G.Assert(inNode!=outNode,"can't connect to self");
		this.actualWeight = (int)(weight/weightScale);
		this.expressed = expressed;
		this.innovation = innovation;
	}
	private ConnectionGene(int inNode, int outNode, int actualWeight, boolean expressed, int innovation) 
	{	highestInnovationNumber = Math.max(highestInnovationNumber,innovation);
		this.inNode = inNode;
		this.outNode = outNode;
		G.Assert(inNode!=outNode,"can't connect to self");
		this.actualWeight = actualWeight;
		this.expressed = expressed;
		this.innovation = innovation;
	}

	public ConnectionGene(ConnectionGene con) {
		highestInnovationNumber = Math.max(highestInnovationNumber,con.innovation);
		this.inNode = con.inNode;
		this.outNode = con.outNode;
		this.actualWeight = con.actualWeight;
		this.expressed = con.expressed;
		this.innovation = con.innovation;
	}

	public int getInNode() {
		return inNode;
	}

	public int getOutNode() {
		return outNode;
	}


	public boolean isExpressed() {
		return expressed && actualWeight!=0;
	}
	
	public void disable() {
		expressed = false;
	}
	public void enable() 
	{
		expressed = true;
	}

	public int getInnovation() {
		return innovation;
	}
	public ConnectionGene() {}
	public ConnectionGene copy() {
		return new ConnectionGene(inNode, outNode, actualWeight, expressed, innovation);
	}

	public boolean save(PrintStream out) {
		out.println("C1 "+inNode+" "+outNode+" "+actualWeight+" "+expressed+" "+innovation);
		return true;
	}
	public boolean load(Tokenizer tok) {
		if("C1".equals(tok.nextElement()))
		{
			inNode = tok.intToken();
			outNode = tok.intToken();
			actualWeight = tok.intToken();
			expressed = tok.boolToken();
			innovation = tok.intToken();
			highestInnovationNumber = Math.max(highestInnovationNumber,innovation);
			return true;
		}
		return false;
	}
	public void setName(String name) {	}
	
	public int compareTo(ConnectionGene o) {
		return G.signum(innovation-o.innovation);
	}
	public int altCompareTo(ConnectionGene o) {
		return G.signum(o.innovation-innovation);
	}
	public StackIterator<ConnectionGene> push(ConnectionGene item) {
		return new ConnectionGeneStack().push(this);
	}
	public StackIterator<ConnectionGene> insertElementAt(ConnectionGene item, int at) {
		return new ConnectionGeneStack().push(this).insertElementAt(item,at);
	}

	
}
