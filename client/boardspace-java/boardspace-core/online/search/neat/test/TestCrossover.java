package online.search.neat.test;

import java.util.Random;

import online.search.neat.*;
import online.search.neat.NodeGene.TYPE;

// ok 2/23/2025
public class TestCrossover {
	
	public static void main(String[] args) 
	{
		Genome parent1 = new Genome();
		NodeGene p1genes[] = new NodeGene[6];
		for (int i = 1; i < 4; i++) 
			{
			p1genes[i] = parent1.addNodeGene(TYPE.INPUT);
			}
		p1genes[4] = parent1.addNodeGene(TYPE.OUTPUT);
		p1genes[5] = parent1.addNodeGene(TYPE.HIDDEN);
		
		ConnectionGene[] p1connections = new ConnectionGene[6];

		p1connections[0] = parent1.addConnectionGene(p1genes[1], p1genes[4], 0.1f, true);
		p1connections[1] = parent1.addConnectionGene(p1genes[2], p1genes[4], 0.2f, false);
		p1connections[2] = parent1.addConnectionGene(p1genes[3], p1genes[4], 0.3f, true);
		p1connections[3] = parent1.addConnectionGene(p1genes[2], p1genes[5], 0.4f, true);
		p1connections[4] = parent1.addConnectionGene(p1genes[5], p1genes[4], 0.5f, true);
		p1connections[5] = parent1.addConnectionGene(p1genes[1], p1genes[5], 0.6f, true);
		
		Genome parent2 = new Genome(parent1);


		
		System.out.println("Mutate parents");
		for(int i=0;i<6;i++) { p1connections[i].setWeight(0.05f+p1connections[i].getWeight()); }
		GenomePrinter.printGenome(parent1, "output/parent1.png");
		GenomePrinter.printGenome(parent2, "output/parent2.png");
		
		Genome child = parent2.crossover(parent1, new Random());
		GenomePrinter.printGenome(child, "output/child.png");
	}
}
