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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import lib.G;
import lib.IoAble;
import lib.Sort;
import lib.StackIterator;
import lib.Tokenizer;
import online.search.neat.NodeGene.TYPE;
/**
 * 
 * plan for learning the learning parameters.
 * normalize the parameters do the min is 0 and max is 1
 * add a "learning rate" master switch that is a multiplier
 * tweak each learning parameter by += random(1)*learningrate before mutation
 */
public class Genome implements IoAble
{	public static int genomesCreated = 0;
	public Genome parent = null;
	public void setParent(Genome f) { parent = f; generation = f.generation+1; }
	/**
	 * these are parameters used in the mutation process, which can be
	 * manipulated by the training algorithm
	 */
	enum MutationParameter
	{ LEARNING_RATE(0.1,true),			// 10% change in value, and actually mutatable true
	  MUTATION_RATE(0.5,true),			// probabilty of adding mutations this pass
	  PROBABILITY_RETAINING(0.9,true),	// probability of retaining a node unchanged in a mutation pass
	  PROBABILITY_PERTURBING(0.9,true), 	// probability of assigning new weight, of not retained
	  								// remaining probability is nuke and assign a new value
	  PERTURBATION_SCALE(0.1,true),		// 10% scaling of perturbed parameters (actually, +- 0.05)
	  RESET_WEIGHT_MAX(1,false),			// max value after connection reset
	  RESET_WEIGHT_MIN(-1,false),			// min value after connection reset
	  CROSSOVER_PROBABILITY(0.5,true),	// probability of swapping genes when they are concurrent
	  NODE_CHANGE_RATE(0.1,true),		// probability of adding any nodes	  
	  CONNECTION_CHANGE_RATE(0.2,true),	// probability of adding or removing any connections
	  NEW_CONNECTION_RATE(0.01,true), 	// probability of adding new connections (proportional to the network size)
	  CONNECTION_DECAY_RATE(0.001,true),	// normally less than the creation rate
	  CONNECTION_REMOVAL_RATE(0.2,true),	// percentage of inactive connections to remove
	  NODE_REMOVAL_RATE(1.0,false),		// percentage of unused nodes to remove
	  NEW_NODE_RATE(0.01,true),		// probability of adding new nodes (proportional to the network size)
	  MAX_NETWORK_SIZE(2000,false),		// limit the overall size of the network
	  MAX_CONNECTOME_SIZE(20000,false), 	// limit the overall number of connections in the network
	  
	  
		;

	  double defaultValue;
	  boolean mutatable;
	  MutationParameter(double v,boolean mut) 
	  	{ defaultValue = v; 
	  	  mutatable = mut;
	  	}
	}
	Object species = "";
	public void setSpecies(Object s) 
	{ species = s; 
	}
	
	// this is a parameter mechanism that allows known or unknown parameters to be saved and used
	public Map<String,Double>parameters = new HashMap<String,Double>();
	public Set<String> getParameters() { return parameters.keySet(); }
	private double getParameter(MutationParameter key) 
	{ 	double val = getParameter(key.name());
		return val;
	}	
	private void setParameter(MutationParameter key,double val)
	{
		setParameter(key.name(),val);
	}
	public double getParameter(String key) 
	{ 	double val = parameters.get(key);
		return val;
	}	
	public void setParameter(String key,double val)
	{
		parameters.put(key,val);
	}
	
		
	String fromFile = "";
	String fromParent = "";
	String fromSpecies = "";
	String comment = "";
	private Map<Integer, ConnectionGene> connections;
	private Map<Integer, NodeGene> nodes;
	private ArrayList<NodeGene> outputNodes;
	private ArrayList<NodeGene> inputNodes;
	public ArrayList<NodeGene> getInputs() { return inputNodes; }
	public ArrayList<NodeGene> getOutputs() { return outputNodes; }
	
	// this is an interface to allow the mutator to store whatever other data
	// it wants to in the network.  Presumably related to training the next
	// evalStep
	private Map<String,IoAble> extraData = null;
	public Map<String,IoAble> getExtraData() 
	{	if(extraData==null) { extraData=new HashMap<String,IoAble>(); }
		return extraData; 
	}
	
	public String toString() { return "<Genome "+generation+" "+fromFile+" "+nodes.size()+"+"+connections.size()
				/* " "+species+(parent==null?"":" from "+parent.fromFile)*/+">"; }
	
	private NodeGene getGene(int id)
	{
		return nodes.get(id);
	}
	private ConnectionGene getConnection(int id)
	{
		return connections.get(id);
	}
	
	/* note these are maintained as the nodes and connections are added.
	 * if they are ever removed, they'll need to be maintained there too.
	 */
	private int highestNodeInnovation = 0;
	private int highestConnectionInnovation = 0;
	private int lowestNodeInnovation = Integer.MAX_VALUE;
	private int lowestConnectionInnovation = Integer.MAX_VALUE;
	
	private int getHighestNodeInnovation() { return highestNodeInnovation; }
	private int getHighestConnectionInnovation() { return highestConnectionInnovation; }
	private int getLowestNodeInnovation() { return lowestNodeInnovation; }
	private int getLowestConnectionInnovation() { return lowestConnectionInnovation; }
	
	public Genome() {
		genomesCreated++;
		fromFile = "#"+genomesCreated;
		for(MutationParameter p : MutationParameter.values())
			{	// preload all the standard parameter values
				setParameter(p,p.defaultValue);
			}
		nodes = new HashMap<Integer, NodeGene>();
		inputNodes = new ArrayList<NodeGene>();
		outputNodes = new ArrayList<NodeGene>();
		connections = new HashMap<Integer, ConnectionGene>();
	}
	
	public Genome(Genome toBeCopied) 
	{
		this();
		for (NodeGene gene : toBeCopied.getNodeGenes().values()) {
			// this creates copies of the connections
			addNodeGeneCopy(gene);
		}
		
		for (ConnectionGene connection : toBeCopied.getConnectionGenes().values()) {
			// this creates copies of the connections
			addConnectionGeneCopy(connection);
		}
		for(String k : toBeCopied.getParameters())
		{
		setParameter(k,toBeCopied.getParameter(k));
		}
		generation = toBeCopied.generation+1;
	}
	
	public NodeGene addNodeGene(TYPE t)
	{
		return addNodeGene(new NodeGene(t),false);
	}
	public NodeGene addNodeGene(TYPE t,String nam)
	{
		NodeGene n = addNodeGene(t);
		n.name = nam;
		return n;
	}

	public NodeGene addNodeGeneCopy(NodeGene gene) 
	{	return addNodeGene(gene,true);
	}
	
	private NodeGene allNodes[] = null;
	
	public NodeGene[] getAllNodes()
	{	if(allNodes==null)
		{
		allNodes = nodes.values().toArray(new NodeGene[nodes.size()]);
		//This might help with reproducability if there's a bug
		Sort.sort(allNodes);
		}
		return allNodes;
	}
	
	private NodeGene addNodeGene(NodeGene gene,boolean copy)
	{	allNodes = null;
		int id = gene.getId();
		G.Assert(getGene(id)==null,"gene number %s already present",id);
		highestNodeInnovation = Math.max(id,highestNodeInnovation);
		lowestNodeInnovation = Math.min(id,lowestNodeInnovation);
		NodeGene n = copy ? gene.copy() : gene;
		nodes.put(id, n);
		switch(n.getType())
		{
		default: throw G.Error("Not expecting type %s",n.getType());
		case INPUT:
			inputNodes.add(n);
			break;
		case OUTPUT:
			outputNodes.add(n);
			break;
		case HIDDEN: 
			break;	
		}
		return n;
	}
	int nodesRemoved = 0;
	StackIterator<NodeGene> nodesRemovedList = null;
	private void removeNodeGene(NodeGene gene)
	{	allNodes = null;
		nodesRemoved++;
		nodesRemovedList = (nodesRemovedList==null) ? gene : nodesRemovedList.push(gene);
		NodeGene g = nodes.remove(gene.getId());
		G.Assert(g==gene,"already removed ? %s",g);
	}
	
	// add a connection that we know is safe (or else!) 
	public ConnectionGene addConnectionGene(NodeGene input1,NodeGene output,double weight,boolean active)
	{
		return addConnectionGene(new ConnectionGene(input1,output,weight,active),false);
	}
	
	public ConnectionGene[] allConnections = null;
	// add a copy of the gene
	public ConnectionGene addConnectionGeneCopy(ConnectionGene gene)
	{	return addConnectionGene(gene,true);
	}
	int connectionsRemoved=0;
	StackIterator<ConnectionGene> connectionsRemovedList = null;
	private void removeConnectionGene(ConnectionGene c)
	{
		allConnections = null;
		// this is "only" to assist debugging
		connectionsRemoved++;
		connectionsRemovedList= (connectionsRemovedList==null) ? c : connectionsRemovedList.push(c);
		ConnectionGene x = connections.remove(c.getInnovation());
		G.Assert(x==c,"gene %s wasn't removed, already removed?",c);
	}
	private ConnectionGene[] getAllConnections()
	{
		if(allConnections==null)
		{
			allConnections = connections.values().toArray( new ConnectionGene[connections.size()]);
			//This might help with reproducability if there's a bug
			//Sort.sort(allConnections);
		}
		return allConnections;
	}
	private ConnectionGene addConnectionGene(ConnectionGene newConnection,boolean copy)
	{	
		int n = newConnection.getInnovation();
		G.Assert(getConnection(n)==null,"connection %s already exists",n);

		int from = newConnection.getInNode();
		NodeGene fromGene = getGene(from);
		G.Assert(fromGene!=null,"from node %s doesn't exist",from);
		
		
		int to = newConnection.getOutNode();
		NodeGene toGene = getGene(to);
		G.Assert(toGene!=null,"to node %s doesn't exist",to);
		
		highestConnectionInnovation = Math.max(n,highestConnectionInnovation);
		lowestConnectionInnovation = Math.min(n,lowestConnectionInnovation);
		ConnectionGene g = copy ? new ConnectionGene(newConnection) : newConnection;
		toGene.addConnection(g);
		allConnections = null;
		connections.put(n, g);
		return g;
	}
	
	public Map<Integer, ConnectionGene> getConnectionGenes() {
		return connections;
	}
	
	public Map<Integer, NodeGene> getNodeGenes() {
		return nodes;
	}
	
	//
	// optionally perform a mutation pass on the weights, keeping some, changing some, resetting some.
	@SuppressWarnings("unused")
	public void mutation(Random r) {
		if(r.nextDouble()<getParameter(MutationParameter.MUTATION_RATE))
		{
		double retainp = getParameter(MutationParameter.PROBABILITY_RETAINING);
		double perturb = getParameter(MutationParameter.PROBABILITY_PERTURBING);
		double scale = getParameter(MutationParameter.PERTURBATION_SCALE);
		double max = getParameter(MutationParameter.RESET_WEIGHT_MAX);
		double min = getParameter(MutationParameter.RESET_WEIGHT_MIN);
		int retain = 0;
		int modify = 0;
		int reset = 0;
		// use the natural iterator rather than creating a list
		for(ConnectionGene con : connections.values())
		{
			if(r.nextDouble()>retainp)
			{
			if (r.nextDouble() < perturb) 
			{ 			// uniformly perturbing weights
				double w = con.getWeight();
				modify++;
				double neww = w+scale*(r.nextDouble()-0.5);
				con.setWeight(neww);		// perturb by += 1/2
			} else { // assigning new weight +=2
				reset++;
				con.setWeight(r.nextDouble()*(max-min)+min);
			}}
			else { retain++; }
		}
		//G.print("retain ",retain," modify ",modify," reset ",reset);
		}
	}
	
	// transitively recurse to determine if node1 is below us
	private boolean dependsOn(NodeGene node1,NodeGene target,int generation)
	{
		if(node1.getType()==TYPE.INPUT) { return false; }
		if(node1.generation==generation) { return false; }
		if(node1==target) { return true; }
		node1.generation = generation;
		ConnectionGene connections = node1.getInputs();
		while(connections!=null)
		{
			NodeGene from = getGene(connections.getInNode());
			if(dependsOn(from,target,generation)) { return true; }
			connections = connections.next;
		}
		
		return false;
	}
	
	// pass over the connections and permanently remove some of them that are inactive
	public void removeUnusedConnections(Random r)
	{
		evalStep++;
		double rate = getParameter(MutationParameter.CONNECTION_REMOVAL_RATE);
		for(NodeGene g : outputNodes)
		{
			removeUnusedConnections(r,g,evalStep,rate);
		}
	}
	private void removeUnusedConnections(Random r,NodeGene g,int generation,double rate)
	{
		if(g.generation==generation) { return; }
		g.generation = generation;	// node was reached
		ConnectionGene inputs = g.getInputs();
		ConnectionGene prev = null;
		//audit();
		while(inputs!=null)
		{	ConnectionGene current = inputs;
			inputs = inputs.next;
			if(!current.isExpressed() && r.nextDouble()<rate)
			{
				removeConnectionGene(current);
				if(prev!=null) { prev.next = inputs; current=prev; }
				else { g.setInputs(inputs); current = null; }
			}
			else
			{
				removeUnusedConnections(r,getGene(current.getInNode()),generation,rate);
			}
			prev = current;
		}
		//audit();
		
	}
	// recursively descend and mark the nodes we encounter
	private int sweep(NodeGene g,int generation)
	{
		int n = 0;
		if(g.generation==generation) { return 0; }
		g.generation = generation;
		n++;
		ConnectionGene inputs = g.getInputs();
		while(inputs!=null)
		{
			int nodeNum = inputs.getInNode();
			NodeGene next = getGene(nodeNum);
			n += sweep(next,generation);
			inputs = inputs.next;
		}
		return n;
		
	}
	/** perform integrity checks after mutation
	 * 
	 */
	public void audit()
	{
		// get these up front so debugging will have them available
		NodeGene all[] = getAllNodes();
		ConnectionGene connections[] = getAllConnections();
		
		// sweep all the reachable nodes and mark them with a evalStep number
		evalStep++;
		for(NodeGene g : outputNodes)
		{
			sweep(g,evalStep);
		}
		
		// check that all the input connections, which are hard linked
		// correspond to the contents of the connections map
		for(NodeGene g : all)
		{
			ConnectionGene inputs = g.getInputs();
			while(inputs!=null)
			{
				ConnectionGene d = getConnection(inputs.getInnovation());
				G.Assert(inputs==d,"mismatched connection %s %s for %s",inputs,d,g);
				inputs = inputs.next;
			}
		}
		
		// check that each connection in the connections map refers to
		// a valid input and output node that was reached in the marking pass
		// and that each connection is hard linked to the node it expects
		for(int i=0;i<connections.length;i++)
		{
			ConnectionGene c = connections[i];
			int innode = c.getInNode();
			int outnode = c.getOutNode();
			NodeGene in = getGene(innode);
			NodeGene out = getGene(outnode);
			G.Assert(in!=null,"missing input node %s for %s",in,c);
			G.Assert(out!=null,"missing output node %s for %s",out,c);
			G.Assert(out.hasInput(in),"missing dependency for %s on %s",out,in);
			//G.Advise(in.generation==evalStep,"unreached input %s for %s in %s",in,c,this);
			//G.Advise(out.generation==evalStep,"unreached output %s for %s in %s",out,c,this);
		}
	}
	
	//
	// pass over the reachable nodes and remove some that are unused
	//
	private void removeUnusedNodes(Random r)
	{
		evalStep++;
		double rate = getParameter(MutationParameter.NODE_REMOVAL_RATE);
		for(NodeGene g : outputNodes)
		{
			removeUnusedNodes(r,g,evalStep,rate);
		}
		// remove any nodes that are now completely unconnected
		StackIterator<NodeGene> toBeRemoved = null;
		// use the natural iterator rather than forming and explicit list
		// but that requires we remove after finishing the traversal
		for(NodeGene g : nodes.values())
		{
			if((g.generation!=evalStep) && (g.getType()==TYPE.HIDDEN))
			{	toBeRemoved = (toBeRemoved==null) ? g : toBeRemoved.push(g); 
			}
		}
		while(toBeRemoved!=null)
		{		
			NodeGene g = (NodeGene)toBeRemoved.top();
			toBeRemoved = toBeRemoved.discardTop();		
			ConnectionGene inputs = g.getInputs();
			g.setInputs(null);
			while(inputs!=null)
			{	ConnectionGene ne = inputs;
				inputs = inputs.next;
				removeConnectionGene(ne);					
			}
				
			removeNodeGene(g);
		}
	}
	//
	// traverse the network and if we find nodes receiving input from nodes
	// that have no inputs, remove the connection.  This allows the node with
	// no input to be actually removed.
	//
	private void removeUnusedNodes(Random r,NodeGene g,int generation,double rate)
	{
		if(g.generation==generation) { return; }
		g.generation = generation;	// node was reached
		if(g.getType()!=TYPE.INPUT) 
		{
		ConnectionGene inputs = g.getInputs();
		ConnectionGene prev =null;
		while(inputs!=null)
		{
			ConnectionGene current = inputs;
			inputs = inputs.next;
			NodeGene below = getGene(current.getInNode());
			if(below.getType()==TYPE.HIDDEN)
			{
			ConnectionGene belowIn = below.getInputs();
			if(belowIn==null && r.nextDouble()<rate)
			{
				removeConnectionGene(current);
				if(prev==null) { g.setInputs(inputs); current = null;}
				else { prev.next = inputs; current=prev; }
			}
			else 
			{		
				removeUnusedNodes(r,below,generation,rate);
			}}
			prev = current;
		}}
		
	}
	//
	// this is the main entry point to mutate the network by removing things.
	//
	public void killConnectionMutation(Random r)
	{	if(r.nextDouble()<getParameter(MutationParameter.CONNECTION_CHANGE_RATE))
		{
		nodesRemoved = 0;
		nodesRemovedList = null;
		connectionsRemoved = 0;
		connectionsRemovedList = null;
		removeUnusedConnections(r);
		//audit();
		removeUnusedNodes(r);
		//audit();
		//if(nodesRemoved>0 || connectionsRemoved>0) { G.print("Removed ",connectionsRemoved," connections ",nodesRemoved," nodes"); }
		int nToKill = (int)(connections.size()*getParameter(MutationParameter.CONNECTION_DECAY_RATE));
		if(nToKill>0)
		{
		ConnectionGene allConnections[] = getAllConnections();
		int len = allConnections.length;
		if(len>0)
		{
		for(int i=0;i<nToKill;i++)
		{
			ConnectionGene c =allConnections[r.nextInt(len)];
			if(c.isExpressed()) { c.disable(); }
			else { c.enable(); }
		}}}}
	}
	//
	// add a new random connection between nodes, but always in a "downhill" direction
	// so the graph remains directed, with no recurrency.
	//
	public void addConnectionMutation(Random r) 
	{	
		if(r.nextDouble()<getParameter(MutationParameter.CONNECTION_CHANGE_RATE));
		{
		double rate = getParameter(MutationParameter.NEW_CONNECTION_RATE);
		int maxAttempts = (int)(rate*Math.sqrt(connections.size()));
		if(maxAttempts==0) { maxAttempts++; }
		double maxConnections = getParameter(MutationParameter.MAX_CONNECTOME_SIZE);
		NodeGene[] allNodes = getAllNodes();
		int len = allNodes.length;
		if(len>0)
		{
		int tries = 0;
		double maxv = getParameter(MutationParameter.RESET_WEIGHT_MAX);
		double minv = getParameter(MutationParameter.RESET_WEIGHT_MIN);
		while (tries < maxAttempts && (connections.size()<maxConnections))
			{
			tries++;
			NodeGene fromNode = allNodes[r.nextInt(len)];	// the "from" node
			NodeGene toNode = allNodes[r.nextInt(len)];
			
			addNodeConnection(fromNode,toNode,r.nextDouble()*(maxv-minv)+minv);
			}
		}}
	}
	
	// add a new node along an existing path
	public void addNodeMutation(Random r) 
	{	if(r.nextDouble()<getParameter(MutationParameter.NODE_CHANGE_RATE));
		{
		double newrate = getParameter(MutationParameter.NEW_NODE_RATE);
		int nnew = (int)(newrate*Math.sqrt(nodes.size()));
		if(nnew==0) { nnew++; }
		double max_nodes = getParameter(MutationParameter.MAX_NETWORK_SIZE);
		double maxval = getParameter(MutationParameter.RESET_WEIGHT_MAX);
		double minval = getParameter(MutationParameter.RESET_WEIGHT_MIN);
		ConnectionGene all[] = getAllConnections();
		int len = all.length;
		if(len>0)
		{
		for(int i=0;i<nnew && nodes.size()<max_nodes;i++)
		{
		ConnectionGene con = all[r.nextInt(len)];	// random connection
		
		NodeGene inNode = nodes.get(con.getInNode());
		NodeGene outNode = nodes.get(con.getOutNode());
				
		NodeGene newNode = addNodeGene(TYPE.HIDDEN);
		addConnectionGene(inNode, newNode, r.nextDouble()*(maxval-minval)+minval, true);
		addConnectionGene(newNode, outNode,  r.nextDouble()*(maxval-minval)+minval, true);
		G.Assert(newNode.getInputs()!=null,"has a connection");
		}
		}}
	}
	public void mutateLearningValues(Random r)
	{
		double learn = getParameter(MutationParameter.LEARNING_RATE);
		if(learn>=0)
		{
		for(MutationParameter p : MutationParameter.values())
		{
			if(p.mutatable)
			{
				double oldval = getParameter(p);
				double newval = oldval + (r.nextDouble()-0.5)*learn*(oldval+0.01);
				double clippedNewval = Math.max(0,Math.min(1,newval));
				//G.print(p," ",clippedNewval);
				setParameter(p,clippedNewval);
				if(p==MutationParameter.LEARNING_RATE)
				{
					learn = clippedNewval;
				}
			}
		}
		}
	}
	/**
	 * @param parent1	More fit parent
	 * @param parent2	Less fit parent
	 */
	public Genome crossover(Genome parent2, Random r) {
		Genome child = new Genome();
		child.setParent(this);
		//G.print("Parent "+parent2);
		//audit();
		//parent2.audit();
		for(String k : getParameters())
		{
			child.setParameter(k,getParameter(k));
		}
		child.mutateLearningValues(r);
		
		for (NodeGene parent1Node : nodes.values()) {
			child.addNodeGeneCopy(parent1Node);
		}
		
		for (ConnectionGene add : connections.values())
		{	int outNode = add.getOutNode();
			ConnectionGene p2Connection = parent2.getConnection(add.getInnovation());
			if ((p2Connection!=null) 
					&& (r.nextDouble()<getParameter(MutationParameter.CROSSOVER_PROBABILITY)))
				{ 
				  G.Assert(p2Connection.getInNode()==add.getInNode(),"mismatched input nodes");
				  G.Assert(p2Connection.getOutNode()==outNode,"mismatched output nodes");
				  add = p2Connection; 	// crossover shared gene
				}
			child.addConnectionGeneCopy(add);
		}
		child.audit();
		return child;
	}
	
	public static double compatibilityDistance(Genome genome1, Genome genome2,
			double excessGeneWeight, double disjointGeneWeight, double weightDifferenceWeight) 
	{
		int excessGenes = countExcessGenes(genome1, genome2);
		int disjointGenes = countDisjointGenes(genome1, genome2);
		double avgWeightDiff = averageWeightDiff(genome1, genome2);
		
		return excessGenes * excessGeneWeight + disjointGenes * disjointGeneWeight + avgWeightDiff * weightDifferenceWeight;
	}
	//
	// add a new connection between node1 and node2, being careful to maintain
	// the network integrity and keeping it a DAG from.
	//
	public boolean addNodeConnection(NodeGene fromNode,NodeGene toNode,double strength)
	{
		if(fromNode==toNode) { return false; }
		
		TYPE fromNodeType = fromNode.getType();
		TYPE toNodeType = toNode.getType();
		boolean reversed = false;
		switch(fromNodeType)
		{
		case INPUT:
			switch(toNodeType)
			{
			case INPUT:
				return false;		// just say we did
			case OUTPUT: 
			case HIDDEN: break;
			default: throw G.Error("Not expecting %s",toNodeType);
			}
			break;
		case OUTPUT:
			switch(toNodeType)
			{
			case OUTPUT: 
				return false;
			case INPUT:
			case HIDDEN:
				reversed = true;
				break;

			default: throw G.Error("Not expecting %s",toNodeType);
			}
			break;
		case HIDDEN:
			switch(toNodeType)
			{
			case INPUT:
				reversed = true;
				break;
			case OUTPUT: 
				break;
			case HIDDEN:
				// maintain the connections are a directed graph, no recurrency
				boolean toDependsOnFrom = dependsOn(fromNode,toNode,++evalStep);
				// redundancy, remove eventually
				// boolean fromDependsOnTo = dependsOn(toNode,fromNode,++evalStep);
				// G.Assert(!(toDependsOnFrom&&fromDependsOnTo),"shouldn't be mutual");
				
				reversed = toDependsOnFrom;
				//if(reversed) { System.out.println("swapping "+fromNode+toNode);}
				break;
			default: throw G.Error("Not expecting %s",toNodeType);
			}
			break;
		default: throw G.Error("Not expecting %s",fromNodeType);
		}		

		NodeGene n1 = reversed ? toNode : fromNode;
		NodeGene n2= reversed ? fromNode : toNode;
		
		if(n1.hasInput(n2))	{ return false; }	// already an immediate child

		addConnectionGene(n1, n2, strength, true);
		//System.out.println("Added connection "+n1+" to "+n2 );
		return true;
	}
	
	
	public static int countMatchingGenes(Genome genome1,Genome genome2)
	{
		int count = 0;
		{
		Map<Integer,NodeGene> g1 = genome1.getNodeGenes();
		Map<Integer,NodeGene> g2 = genome2.getNodeGenes();
		for(Integer g1n : g1.keySet())
		{
			if(g2.containsKey(g1n)){ count++; }
		}}
		{
		Map<Integer,ConnectionGene> c1 = genome1.getConnectionGenes();
		Map<Integer,ConnectionGene> c2 = genome2.getConnectionGenes();
		for(Integer c1n : c1.keySet())
		{
			if(c2.containsKey(c1n)){ count++; }
		}}

		return count;
	}

	public static int countDisjointGenes(Genome genome1, Genome genome2) 
	{
		int disjointGenes = 0;
		{
		Map<Integer,NodeGene> n1 = genome1.getNodeGenes();
		Map<Integer,NodeGene> n2 = genome2.getNodeGenes();
		// count genes in range but not included in both
		{
		int n2High = genome2.getHighestNodeInnovation();
		int n2Low = genome2.getLowestNodeInnovation();
		for(Integer g1 : n1.keySet())
		{	// if inside the possible range
			if(g1>=n2Low && g1<=n2High && !n2.containsKey(g1))
				{ disjointGenes++; }
		}}
		
		{
		int n1High = genome1.getHighestNodeInnovation();
		int n1Low = genome1.getLowestNodeInnovation();
		for(Integer g2 : n2.keySet())
		{	// if inside the possible range in n1
			if(g2>=n1Low && g2<=n1High && !n1.containsKey(g2)) 
				{ disjointGenes++; }
		}}}

		{
		Map<Integer,ConnectionGene> c1 = genome1.getConnectionGenes();
		Map<Integer,ConnectionGene> c2 = genome2.getConnectionGenes();
		int c2High = genome2.getHighestConnectionInnovation();
		int c2Low = genome2.getLowestConnectionInnovation();
		for(Integer g1 : c1.keySet())
		{	// if inside the possible range
			if(g1>=c2Low && g1<=c2High && !c2.containsKey(g1)) 
				{ disjointGenes++; }
		}
		int c1High = genome1.getHighestConnectionInnovation();
		int c1Low = genome1.getLowestConnectionInnovation();
		for(Integer g2 : c2.keySet())
		{	// if inside the possible range
			if(g2>=c1Low && g2<=c1High && !c1.containsKey(g2))
				{ disjointGenes++; }
		}}

		return disjointGenes;
	}

	public static int countExcessGenes(Genome genome1, Genome genome2) 
	{
		int excessGenes = 0;
		{
		Map<Integer,NodeGene> n1 = genome1.getNodeGenes();
		Map<Integer,NodeGene> n2 = genome2.getNodeGenes();
		// count genes in range but not included in both
		{
		int n2High = genome2.getHighestNodeInnovation();
		int n2Low = genome2.getLowestNodeInnovation();
		for(Integer g1 : n1.keySet())
		{	// if outside the possible range
			if(!(g1>=n2Low && g1<=n2High))
				{ excessGenes++; }
		}}
		
		{
		int n1High = genome1.getHighestNodeInnovation();
		int n1Low = genome1.getLowestNodeInnovation();
		for(Integer g2 : n2.keySet())
		{	// if outside the possible range in n1
			if(!(g2>=n1Low && g2<=n1High))
				{ excessGenes++; }
		}}}

		{
		Map<Integer,ConnectionGene> c1 = genome1.getConnectionGenes();
		Map<Integer,ConnectionGene> c2 = genome2.getConnectionGenes();
		int c2High = genome2.getHighestConnectionInnovation();
		int c2Low = genome2.getLowestConnectionInnovation();
		for(Integer g1 : c1.keySet())
		{	// if outside the possible range
			if(!(g1>=c2Low && g1<=c2High)) 
				{ excessGenes++; }
		}
		int c1High = genome1.getHighestConnectionInnovation();
		int c1Low = genome1.getLowestConnectionInnovation();
		for(Integer g2 : c2.keySet())
		{	// if outside the possible range
			if(!(g2>=c1Low && g2<=c1High ))
				{ excessGenes++; }
		}}

		return excessGenes;
	}

	
	public static float averageWeightDiff(Genome genome1, Genome genome2)
	{
		int matchingGenes = 0;
		float weightDifference = 0;
		
		Map<Integer,ConnectionGene> g1 = genome1.getConnectionGenes();
		Map<Integer,ConnectionGene> g2 = genome2.getConnectionGenes();
		
		for(Integer g1Id : g1.keySet())
		{
			ConnectionGene c1 = g1.get(g1Id);
			ConnectionGene c2 = g2.get(g1Id);
			if(c2!=null)
			{
				matchingGenes++;
				weightDifference += Math.abs(c1.getWeight()-c2.getWeight());
			}
		}			
		return matchingGenes>0 ? weightDifference/matchingGenes : 0;
	}
	public int generation = 1;		// mutation generation
	private int evalStep = 1;		// incremented each evaluation and some other operations
	private static int errorCount=1;
	
	/** this implements a "pull" evaluation where output nodes recursively pull values
	 * from their inputs, until they get back to an input node.  Recurrent connections
	 * are not permitted!
	 */
	public void evaluate()
	{
		evalStep++;
		try {
		for(int i=0;i<outputNodes.size();i++)
		{	NodeGene out = outputNodes.get(i);
			evaluate(out,evalStep);
		}
		}
		catch (Throwable err)
		{
			NetIo.save(this,"g:/temp/nn/train/error-"+errorCount++,"error in evaluation"+err);
		}
	}
	//
	// this recursively evaluates one output node
	// the network is required to be non-recurrent
	// evalStep is an arbitrary (but unique) number that
	// should reflect new inputs
	//
	private double evaluate(NodeGene c,int generation)
	{	c.setComputedValue(-1,-generation);	// flag that we're in progress, guard against recurrent networks
		ConnectionGene inputs = c.getInputs();
		double v = 0;
		while(inputs!=null)
		{
			if(inputs.isExpressed())
			{
			//G.Assert(in.getOutNode()==c.getId(),"should be us");
			int inn = inputs.getInNode();
			NodeGene inGene = getGene(inn);
			
			if(inGene.generation!=generation)
			{	
				if(inGene.getType()==TYPE.INPUT)
				{
				v += inGene.getValue(generation);	
				}
				else
				{
				G.Assert(inGene.generation!=-generation,"recursion lock on %s",inGene);
				evaluate(inGene,generation);
				}
			}
			v += inputs.getWeight()*inGene.getValue(generation);			
			}
			inputs = inputs.next;
		}
		
		if(c.getType()==TYPE.OUTPUT) { c.setComputedValue(v,generation); }
		else 
			{ if(v!=0.0) { v = NodeGene.approximateTanh(v); }
			  c.setComputedValue(v,generation ); 
			}

		return v;
	}

	//
	// save and reload genomes
	//
	public static String IDKEY = "GENOME";
	public static String VERSION = "1";
		// version 2 adds extra data
	public static String NODEKEY = "NODES";
	public static String CONNKEY = "CONNECTIONS";
	public static String DATAKEY = "DATA";
	public static String ENDKEY = "END";
	public static String PARAMETERSKEY = "PARAMETERS";
	public static String NAMEKEY = "NAME";
	public static String GENERATIONKEY = "GENERATION";
	public static String SPECIESKEY = "SPECIES";
	public static String PARENTKEY = "PARENT";
			
	public boolean save(PrintStream out) 
	{	boolean hasData = extraData!=null && extraData.size()==0 ;
		String data = hasData
						? DATAKEY+" "+extraData.size()+" "
						: "";
		out.println(IDKEY+" "+VERSION+" "+NODEKEY+" "+nodes.size()
			+" "+CONNKEY+" "+connections.size()
			+" "+PARAMETERSKEY+" "+parameters.size()
			+" "+GENERATIONKEY+" "+generation
			+" "+NAMEKEY+" "+G.quote(fromFile)
			+" "+SPECIESKEY+" "+G.quote(""+species)
			+" "+"PARENT"+" "+G.quote(""+parent)
			+ " "+data+ENDKEY);
		for(NodeGene node : nodes.values())
		{
			node.save(out);
		}
		for(ConnectionGene conn : connections.values())
		{
			conn.save(out);
		}
		for(String key : parameters.keySet())
		{
			out.print(key);
			out.print(" ");
			out.println(parameters.get(key));
		}
		if(hasData)
		{
			for(String key : extraData.keySet())
			{
				out.print(G.quote(key));
				out.print(" ");
				IoAble.saveWithId(out,extraData.get(key));			
			}
		}
		return true;
	}

	public boolean load(Tokenizer tok) {
		String id = tok.nextElement();
		String vers = tok.nextElement();
		
		if(IDKEY.equals(id)
				&& VERSION.equals(vers))
		{	
			boolean end = false;
			boolean ok = true;
			int nodeCount = 0;
			int connCount = 0;
			int dataCount = 0;
			int parameterCount = 0;
			while(!end)
			{
			String next = tok.nextElement();
			if(NODEKEY.equals(next)) { nodeCount = tok.intToken(); }
			else if(CONNKEY.equals(next)) { connCount = tok.intToken(); }
			else if(PARAMETERSKEY.equals(next) ) { parameterCount = tok.intToken(); }
			else if(DATAKEY.equals(next)) { dataCount = tok.intToken(); }
			else if(NAMEKEY.equals(next)) { fromFile = tok.nextElement(); }
			else if(GENERATIONKEY.equals(next)) { generation =tok.intToken(); }
			else if(SPECIESKEY.equals(next)) { fromSpecies = tok.nextElement(); }
			else if(PARENTKEY.equals(next)) { fromParent = tok.nextElement(); }
			else if(ENDKEY.equals(next)) { end = true; }
			else { G.Error("unexpected key %s",next); }
			}
			while(nodeCount-- > 0 && ok)
			{
				NodeGene n = new NodeGene();
				ok = n.load(tok);
				if(ok) { addNodeGene(n,false); }
			}
			while(connCount-- > 0 && ok)
			{
				ConnectionGene c = new ConnectionGene();
				ok = c.load(tok);
				if(ok) { addConnectionGene(c,false); }
			}
			while(parameterCount-- > 0)
			{
				String key = tok.nextElement();
				double val = tok.doubleToken();
				setParameter(key,val);
			}
			while(dataCount-- > 0)
			{
				String name = tok.nextElement();
				IoAble val = IoAble.loadWithId(tok);
				getExtraData().put(name,val);
			}
			audit();
		}
		else
		{
			G.Error("Unexpected identifiers %s %s - expected %s %s",id,vers,IDKEY,VERSION);
		}
		return false;
	}
	public void setName(String name) {
		fromFile = name;
	}
	public String getName() { return fromFile; }
	
	public static Genome createBlankNetwork(int in,int out)
	{
		Genome g = new Genome();
		g.createBlankNetworkSized(in,out);
		return g;
	}
	public Genome createBlankNetworkSized(int in,int out)
	{
		for(int i=0;i<in;i++) { addNodeGene(TYPE.INPUT,"In"+i); }
		for(int i=0;i<out;i++) { addNodeGene(TYPE.OUTPUT,"Out"+i); }
		double maxv = getParameter(MutationParameter.RESET_WEIGHT_MAX);
		double minv = getParameter(MutationParameter.RESET_WEIGHT_MIN);
		
		// create one hidden node per input node, and fully connect them
		for(int h=0;h<in;h++)
		{
		NodeGene hidden = addNodeGene(TYPE.HIDDEN);
		ArrayList<NodeGene> ins = getInputs();
		ArrayList<NodeGene> outs = getOutputs();
		Random r = new Random();
		for(int i=0;i<ins.size();i++) { addConnectionGene(ins.get(i),hidden,r.nextDouble()*(maxv-minv)+minv,true); }
		for(int i=0;i<outs.size();i++) { addConnectionGene(hidden,outs.get(i),r.nextDouble()*(maxv-minv)+minv,true); }
		}
		audit();
		return this;
	}
}
