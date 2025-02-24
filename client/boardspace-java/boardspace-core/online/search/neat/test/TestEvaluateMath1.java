package online.search.neat.test;

import java.util.ArrayList;
import java.util.Random;

import lib.G;
import online.search.neat.*;

/**
 * Tests learning of a math function of 1 variable.
 */
public class TestEvaluateMath1 implements GenomeEvaluator
{
	NeatEvaluator evaluator = null;
	
	public double theFunction(double x)
	{
		return Math.sin(x);	// sin of x 
	}

	// this version evaluates the genome alone, and compares it to the know/desired value
	public double evaluate(Genome g)
	{	return evaluate(g,false);
	}
	public double evaluate(Genome g,boolean print)
	{
		//GenomePrinter.printGenome(this, "output/currenteval.png");
		double totalE = 0;
		double n = 1000;
		Random r = new Random();
		ArrayList<NodeGene> inputNodes = g.getInputs();
		ArrayList<NodeGene> outputNodes = g.getOutputs();
		for(int target=0;target<n;target++)
		{	double in = r.nextDouble()+target;
			double iv = in%(2*Math.PI);
			inputNodes.get(0).setInputValue(iv);	
			g.evaluate();
			double val = outputNodes.get(0).getLastValue();
			double targetv = theFunction(in);
			double error = Math.abs(targetv-val);
			if(print)
			{
			G.print(target," ",iv," ",val," ",error);
			}
			totalE += error*error;
		}
		double avee = totalE/n;
		double fit = avee<1 ? 1-avee/2 : 1/(avee*2);
		if(print) { G.print("\nfitness "+fit); }
		return fit;
		
	}
	
	public double test(String file)
	{
		Genome genome = (Genome)NetIo.load(file);
		return evaluate(genome,true);
	}
	public Genome createPrototypeNetwork()
	{
		return Genome.createBlankNetwork(1,1);
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
		TestEvaluateMath1 me = new TestEvaluateMath1();
		if(args.length>0) 
		{
		if("test".equals(args[0])) { me.test("output/Generation 3500.genome"); }
		}
		else 
		{ me.doIt();
		}
	
	}

}
