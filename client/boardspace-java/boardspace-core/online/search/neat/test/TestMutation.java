package online.search.neat.test;

import java.util.Random;

import online.search.neat.*;
import online.search.neat.NodeGene.TYPE;

public class TestMutation {
	
	public static void main(String[] args) {
		Random r = new Random();
		
		Genome genome = new Genome();
				
		NodeGene input1 = genome.addNodeGene(TYPE.INPUT);
		NodeGene input2 = genome.addNodeGene(TYPE.INPUT);
		NodeGene output = genome.addNodeGene(TYPE.OUTPUT);
				
		genome.addConnectionGene(input1,output,0.5f,true);
		genome.addConnectionGene(input2,output,1f,true);
		
		GenomePrinter.printGenome(genome, "output/mut_test_before.png");
		
		genome.mutation(r);
		
		GenomePrinter.printGenome(genome, "output/mut_test_after.png");
	}

}
