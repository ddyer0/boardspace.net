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

class NodeGeneStack extends OStack<NodeGene>
{
	public NodeGene[] newComponentArray(int sz) {
		return new NodeGene[sz];
	}
	
}
//
// genes act as the nodes between connections. 
//
public class NodeGene implements IoAble,CompareTo<NodeGene>,StackIterator<NodeGene>
{	
	public enum TYPE {
		INPUT,
		HIDDEN,
		OUTPUT,
		;
	}
	public String toString() { return "<node "+type+" #"+id+">"; }
	private ConnectionGene inputs = null;
	public ConnectionGene getInputs()
	{
		return inputs;
	}
	public void setInputs(ConnectionGene c)
	{   G.Assert(type!=TYPE.INPUT,"shouldn't be an input node");
		inputs = c;
	}
	public NodeGene() { }
	public ConnectionGene addConnection(ConnectionGene conn)
	{	conn.next = inputs;
		inputs = conn;
		return conn;
		
	}
	public NodeGene copy() 
	{
		NodeGene n = new NodeGene();
		n.type = type;
		n.id = id;
		n.value = value;
		n.generation = generation;
		n.name = name;
		return n;
	}
	private TYPE type;
	private int id;
	private double value = 0;
	int generation = 0;
	String name=null;
	
	
	public void setInputValue(double v)
	{
		G.Assert(type==TYPE.INPUT,"attempted to set value of a non-input node %s",this);
		value = v;
	}
	
	public void setComputedValue(double v,int gen)
	{	G.Assert(type!=TYPE.INPUT,"attempted to set computed value of a input node %s",this);
		value = v;
		generation = gen;
	}
	
	public static double sinh(double x) 
	{
		return (Math.exp(x) - Math.exp(-x))/2;
	}
	
	public static double cosh(double x)
	{
		return (Math.exp(x) + Math.exp(-x))/2;
	}
	
    public static double atanh(double x) {
        if (Math.abs(x) >= 1) {
            throw new IllegalArgumentException("Argument must be between -1 and 1 (exclusive)");
        }
        return 0.5 * Math.log((1 + x) / (1 - x));
    }
    // twice as fast and very small difference from tanh
    public static double approximateTanh(double x)
    {// possible substute a faster approximation
    	return 1-(2*(1/(1+Math.exp(x*2))));
    }
    public static double tanh(double x) {
    	return sinh(x) / cosh(x);
    }
    /**
     * 	Random r = new Random();
	double toterr=0;
	long start = G.Date();
	for(int i=0;i<1000000;i++)
	{	double d = r.nextDouble()*10-5;
		//double v0 = Math.tanh(d);
		double v0 = NodeGene.tanh(d);
		toterr += v0;
	}
	G.print("total time error ",G.Date()-start);

     * @param gen
     * @return
     */
	public double getValue(int gen)
	{	switch(type)
		{
		default: G.Error("Not expecting type %s",type);
		case INPUT:	generation =gen; return value;
		case OUTPUT:
		case HIDDEN:
			if(gen==generation) { return value; }
			else
			{
			throw G.Error("Node %s not evaluated yet",this);
			}
		}
	}
	public double getLastValue()
	{	return value; 
	}
	
	private static int nodeCounter = 0;
		
	public NodeGene(NodeGene gene) {
		int id = gene.id;
		nodeCounter = Math.max(nodeCounter,id);
		this.type = gene.type;
		this.id = id;
	}
	public NodeGene(TYPE ty)
	{
		id = ++nodeCounter;
		type = ty;
	}

	public TYPE getType() {
		return type;
	}

	public int getId() {
		return id;
	}
	// true if n2 is one of this node's immediate inputs
	public boolean hasInput(NodeGene n2) 
	{	ConnectionGene in = inputs;
		int target = n2.getId();
		while(in!=null) 
		{ int from = in.getInNode();
		  if(from==target) { return true; }
		  in = in.getNext();
		}
		return false;
	}
	public boolean save(PrintStream out) 
	{	if(name!=null) { out.print("N2 "+name+" "); }
		out.println("N1 "+id+" "+type);
		return true;
	}
	public boolean load(Tokenizer tok) {
		String ntype = tok.nextElement();
		if("N2".equals(ntype)) { name = tok.nextElement(); ntype = tok.nextElement(); }
		if("N1".equals(ntype))
		{
			id = tok.intToken();
			nodeCounter = Math.max(nodeCounter,id);
			type = TYPE.valueOf(tok.nextElement());
			return true;
		}
		return false;
	}
	public void setName(String name) {	}
	public int compareTo(NodeGene o) {
		return G.signum(id-o.id);
	}
	public int altCompareTo(NodeGene o) {
		return G.signum(o.id-id);
	}

	public StackIterator<NodeGene> push(NodeGene item) {
		StackIterator<NodeGene> stack = new NodeGeneStack();
		stack.push(this);
		stack.push(item);
		return stack;
	}

	public StackIterator<NodeGene> insertElementAt(NodeGene item, int at) {
		StackIterator<NodeGene> stack = new NodeGeneStack();
		stack.push(this);
		stack.insertElementAt(item,at);
		return stack;
	}
}
