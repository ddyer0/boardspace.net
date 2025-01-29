package online.search.neat.test;

import java.util.ArrayList;
import java.util.Random;

import lib.G;
import online.search.io.NetIo;
import online.search.neat.*;

/**
 * Tests a simple evaluator that runs for 100 generations, and scores fitness based on the amount of connections in the network.
 * 
 * @author hydrozoa
 */
public class TestEvaluateMath2 implements GenomeEvaluator
{
	NeatEvaluator evaluator = null;
	
	public double theFunction(double x,double y)
	{
		return Math.sqrt(x*x+y*y);	// sin of x 
	}
	// this version evaluates the genome alone, and compares it to the know/desired value
	public double evaluate(Genome g)
	{	return evaluate(g,false);
	}
	public double evaluate(Genome g,boolean print)
	{
		//GenomePrinter.printGenome(this, "output/currenteval.png");
		double totalE = 0;
		double n = 10;
		Random r = new Random();
		ArrayList<NodeGene> inputNodes = g.getInputs();
		ArrayList<NodeGene> outputNodes = g.getOutputs();
		for(int target=0;target<n;target++)
		{	
			for(int mod = 1; mod<10; mod++)
			{
			double in = r.nextDouble()+target;
			double in2 = r.nextDouble()+mod;
			inputNodes.get(0).setInputValue(in);	
			inputNodes.get(1).setInputValue(in2);
			g.evaluate();
			double val = outputNodes.get(0).getLastValue();
			double targetv = theFunction(in,in2);
			double error = Math.abs(targetv-val);
			if(print)
			{
			G.print(target," ",in," ",in2," ",val," ",targetv," ",error);
			}
			totalE += error;
			}
		}
		double avee = totalE/n;
		double fit = avee<1 ? 1-avee/2 : 1/(avee*2);
		if(print) { G.print("\naverage error ",avee+" fitness "+fit); }
		return fit;
		
	}
	
	public double test(String file)
	{
		Genome genome = (Genome)NetIo.load(file);
		return evaluate(genome,true);
	}
	public Genome createPrototypeNetwork()
	{
		return Genome.createBlankNetwork(2,1);
	}
	public void doIt()
	{		
		evaluator = new NeatEvaluator(100, this);

		evaluator.runEvaluation(10000);

	}
	public static Genome bestSoFar = null;
	public void setBest(Genome best)
	{
		bestSoFar = best;
	}
	public static void main(String[] args) {
		TestEvaluateMath2 me = new TestEvaluateMath2();
		if(args.length>0) 
		{
		if("test".equals(args[0])) { me.test("output/Generation 1500.genome"); }
		}
		else 
		{ me.doIt();
		}
	
	}

}
