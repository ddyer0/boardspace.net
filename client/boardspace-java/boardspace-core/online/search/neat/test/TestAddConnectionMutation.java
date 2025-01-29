package online.search.neat.test;

import java.util.Random;

import online.search.neat.*;
import online.search.neat.NodeGene.TYPE;

// ok 1/23/2025
public class TestAddConnectionMutation {
	
	public static void main(String[] args) {

		Random r = new Random();
		
		Genome genome = new Genome();
				
		genome.addNodeGene(TYPE.INPUT);
		genome.addNodeGene(TYPE.INPUT);
		genome.addNodeGene(TYPE.HIDDEN);
		genome.addNodeGene(TYPE.HIDDEN);
		genome.addNodeGene(TYPE.OUTPUT);
		
		genome.addConnectionMutation(r);
	}

}
