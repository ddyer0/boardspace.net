package online.search.neat.test;

import online.search.neat.*;
import online.search.neat.NodeGene.TYPE;

public class TestGeneCounting {
	

	public static void main(String[] args) {

		Genome genome1 = new Genome();
		Genome genome2 = new Genome();
		
		NodeGene[] sharedNodes = new NodeGene[10];
		for (int i = 0; i < sharedNodes.length; i++) {
			sharedNodes[i] = genome2.addNodeGeneCopy(genome1.addNodeGene(TYPE.HIDDEN));
		}
		
		ConnectionGene[] sharedConnections = new ConnectionGene[5];
		for (int i = 0; i < sharedConnections.length; i++) {
			sharedConnections[i] = genome2.addConnectionGeneCopy(genome1.addConnectionGene(sharedNodes[i], sharedNodes[i*2],1f, true));
		}
				
		System.out.println("Number of matching genes = "+Genome.countMatchingGenes(genome1, genome2)+"\t Correct answer = "+(sharedNodes.length+sharedConnections.length));
		System.out.println("Number of disjoint genes = "+Genome.countDisjointGenes(genome1, genome2)+"\t Correct answer = 0");
		System.out.println("Number of excess genes = "+Genome.countExcessGenes(genome1, genome2)+"\t Correct answer = 0");
		System.out.println("\n");
		
		{
		// add some unique genes to genome1
		NodeGene x1 =genome1.addNodeGene(TYPE.HIDDEN);
		NodeGene x2 =genome1.addNodeGene(TYPE.HIDDEN);
		NodeGene x3 =genome1.addNodeGene(TYPE.HIDDEN);
		genome1.addConnectionGene(x1,x2,1,true);
		genome1.addConnectionGene(x2,x3,1,true);
		genome1.addConnectionGene(sharedNodes[4],x1,1,true);
		
		System.out.println("Number of matching genes = "+Genome.countMatchingGenes(genome1, genome2)+"\t Correct answer = "+(sharedNodes.length+sharedConnections.length));
		System.out.println("Number of disjoint genes = "+Genome.countDisjointGenes(genome1, genome2)+"\t Correct answer = 0");
		System.out.println("Number of excess genes = "+Genome.countExcessGenes(genome1, genome2)+"\t Correct answer = 6");
		System.out.println("\n");
		}
		
		{
		// add some unique genes to genome2
		NodeGene x1 = genome2.addNodeGene(TYPE.HIDDEN);
		NodeGene x2 = genome2.addNodeGene(TYPE.HIDDEN);
		NodeGene x3 = genome2.addNodeGene(TYPE.HIDDEN);
		genome2.addConnectionGene(x1,x2,1,true);
		genome2.addConnectionGene(x2,x3,1,true);
		genome2.addConnectionGene(sharedNodes[6],x1,1,true);
		
		System.out.println("Number of matching genes = "+Genome.countMatchingGenes(genome1, genome2)+"\t Correct answer = "+(sharedNodes.length+sharedConnections.length));
		System.out.println("Number of disjoint genes = "+Genome.countDisjointGenes(genome1, genome2)+"\t Correct answer = 6");
		System.out.println("Number of excess genes = "+Genome.countExcessGenes(genome1, genome2)+"\t Correct answer = 6");
		System.out.println("\n");
		}

		
		System.out.println("Counting genes between same genomes, but with opposite parameters:");
		System.out.println("Number of matching genes = "+Genome.countMatchingGenes(genome2, genome1)+"\t Correct answer = "+(sharedNodes.length+sharedConnections.length));
		System.out.println("Number of disjoint genes = "+Genome.countDisjointGenes(genome2, genome1)+"\t Correct answer = 6");
		System.out.println("Number of excess genes = "+Genome.countExcessGenes(genome2, genome1)+"\t Correct answer = 6");
		System.out.println("\n");
		
		genome2.addConnectionGeneCopy(genome1.addConnectionGene(sharedNodes[5],sharedNodes[7],1,true));
		genome2.addNodeGeneCopy(genome1.addNodeGene(TYPE.HIDDEN));

		
		System.out.println("Number of matching genes = "+Genome.countMatchingGenes(genome1, genome2)+"\t Correct answer = "+(sharedNodes.length+sharedConnections.length+2));
		System.out.println("Number of disjoint genes = "+Genome.countDisjointGenes(genome1, genome2)+"\t Correct answer = 12");
		System.out.println("Number of excess genes = "+Genome.countExcessGenes(genome1, genome2)+"\t Correct answer = 0");
	}

}
