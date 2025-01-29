package online.search.neat.test;

import java.util.Random;

import online.search.neat.*;
import online.search.neat.NodeGene.TYPE;

// ok 1/23/2025
public class TestAddNodeMutation {
	
	public static void main(String[] args) {
		try {
		Random r = new Random();
		
		Genome genome = new Genome();
				
		NodeGene input1 = genome.addNodeGene(TYPE.INPUT);
		NodeGene input2 = genome.addNodeGene(TYPE.INPUT);
		NodeGene output = genome.addNodeGene(TYPE.OUTPUT);
		
		genome.addConnectionGene(input1,output,0.5f,true);
		genome.addConnectionGene(input2,output,1f,true);
		
		for(int i=0;i<10;i++)
		{
		GenomePrinter.printGenome(genome, "output/nodemutation/test"+i+".png");
		
		genome.addNodeMutation(r);
		
		}
		GenomePrinter.printGenome(genome, "output/nodemutation/add_nod_mut_test_after.png");
		}
		catch (Throwable e)
		{	System.out.println("error in TestsNodeAdd"+e);
			e.printStackTrace();
		}
	}

}
