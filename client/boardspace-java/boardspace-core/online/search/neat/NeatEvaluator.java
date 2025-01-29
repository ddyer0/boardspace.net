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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import online.search.io.NetIo;

public class NeatEvaluator {
	
	GenomeEvaluator evaluator;

	private FitnessGenomeComparator fitComp = new FitnessGenomeComparator();
		
	private Random random = new Random();
	
	/* Constants for tuning */
	private double C1 = 1.0;
	private double C2 = 1.0;
	private double C3 = 0.4;
	private double DT = 30.0;
	private double targetNumberOfSpecies = 10;
	private int populationSize;
	
	private List<Genome> genomes;
	private List<Genome> nextGenGenomes;
	
	private List<Species> species;
	
	private Map<Genome, Species> mappedSpecies;
	private Map<Genome, Double> scoreMap;
	private double highestScore;
	private Genome fittestGenome;
	
	private String files = "output/";

	public NeatEvaluator(String project,GenomeEvaluator e)
	{
		files =project;
		evaluator = e;
	}
	
	public void createCohort(int populationSize, Genome startingGenome) 
	{	this.populationSize = populationSize;
		targetNumberOfSpecies = Math.sqrt(populationSize);
		genomes = new ArrayList<Genome>(populationSize);
		for (int i = 0; i < populationSize; i++) {
			genomes.add(new Genome(startingGenome));
		}	
		
		nextGenGenomes = new ArrayList<Genome>(populationSize);
		mappedSpecies = new HashMap<Genome, Species>();
		scoreMap = new HashMap<Genome, Double>();
		species = new ArrayList<Species>();

	}
	
	public NeatEvaluator(int populationSize,GenomeEvaluator e) 
	{	evaluator = e;
		createCohort(populationSize,e.createPrototypeNetwork());
	}
	
	/**
	 * Runs one generation
	 */
	public void evaluate() {
		// Reset everything for next generation
		for (Species s : species) {
			s.reset(random);
		}
		scoreMap.clear();
		mappedSpecies.clear();
		nextGenGenomes.clear();
		highestScore = -100;
		fittestGenome = null;
		
		// Place genomes into species
		for (Genome g : genomes) {
			boolean foundSpecies = false;
			for (Species s : species) {
				if (Genome.compatibilityDistance(g, s.mascot, C1, C2, C3) < DT) { // compatibility distance is less than DT, so genome belongs to this species
					s.members.add(g);
					mappedSpecies.put(g, s);
					foundSpecies = true;
					break;
				}
			}
			if (!foundSpecies) { // if there is no appropiate species for genome, make a new one
				Species newSpecies = new Species(g);
				species.add(newSpecies);
				mappedSpecies.put(g, newSpecies);
			}
		}
		int ns = species.size();
		if(ns>targetNumberOfSpecies+1) { DT *= 1.1; }
		else if(ns<targetNumberOfSpecies+1) { DT *= 0.9; DT=Math.max(1,DT); }

		// Remove unused species
		Iterator<Species> iter = species.iterator();
		while(iter.hasNext()) {
			Species s = iter.next();
			if (s.members.isEmpty()) {
				iter.remove();
			}
		}
		
		// Evaluate genomes and assign score
		for (Genome g : genomes) {
			Species s = mappedSpecies.get(g);		// Get species of the genome
			
			double score = evaluator.evaluate(g);
			g.setParameter("Fitness",score);
			double adjustedScore = score / mappedSpecies.get(g).members.size();
			
			s.addAdjustedFitness(adjustedScore);	
			s.fitnessPop.add(new FitnessGenome(g, score));
			scoreMap.put(g, adjustedScore);
			if (score > highestScore) {
				highestScore = score;
				fittestGenome = g;
			}
		}
		
		// put best genomes from each species into next generation
		for (Species s : species) {
			Collections.sort(s.fitnessPop, fitComp);
			Collections.reverse(s.fitnessPop);
			for(int i=0;i<Math.min(2,s.fitnessPop.size());i++)		// keep 2 from each species
			{
				FitnessGenome fittestInSpecies = s.fitnessPop.get(i);
				nextGenGenomes.add(fittestInSpecies.genome);
				//System.out.println("keep "+fittestInSpecies.genome+" "+fittestInSpecies.fitness);
			}
		}
		
		// Breed the rest of the genomes
		while (nextGenGenomes.size() < populationSize) { // replace removed genomes by randomly breeding
			Species s = getRandomSpeciesBiasedAjdustedFitness(random);
			
			Genome p1 = getRandomGenomeBiasedAdjustedFitness(s, random);
			Genome p2 = getRandomGenomeBiasedAdjustedFitness(s, random);
			
			
			Genome child = makeNewGeneration(random.nextInt(),p1,p2);
			

			nextGenGenomes.add(child);
		}
		genomes = nextGenGenomes;
		nextGenGenomes = new ArrayList<Genome>();
		//System.out.println("Fittest "+fittestGenome);
	}
	private Genome makeNewGeneration(int seed,Genome p1,Genome p2)
	{	Random r = new Random(seed);
	p1.audit();
	p2.audit();
		Genome child = mateWithCrossover(r,p1,p2);
		//child.audit();
		child.mutation(random);					// random mutations at the child's mutation rate
		//child.audit();
		child.addNodeMutation(random);			// random new nodes proportional to the child's count
		//child.audit();
		child.addConnectionMutation(random);	// random new connections proportional to the child's node count
		//child.audit();
		child.killConnectionMutation(random);	// disable some random connections
		child.audit();
		return child;
	}
	private Genome mateWithCrossover(Random r,Genome p1,Genome p2)
	{	
		Genome child = null;
		if (scoreMap.get(p1) >= scoreMap.get(p2)) {
			child = p1.crossover(p2, r);
		} else {
			child = p2.crossover(p1, r);
		}
		return child;
	}
	/**
	 * Selects a random species from the species list, where species with a higher total adjusted fitness have a higher chance of being selected
	 */
	private Species getRandomSpeciesBiasedAjdustedFitness(Random random) {
		double completeWeight = 0.0;	// sum of probablities of selecting each species - selection is more probable for species with higher fitness
		for (Species s : species) {
            completeWeight += s.totalAdjustedFitness;
		}
        double r = Math.random() * completeWeight;
        double countWeight = 0.0;
        for (Species s : species) {
            countWeight += s.totalAdjustedFitness;
            if (countWeight >= r) {
            	 return s;
            }
        }
        throw new RuntimeException("Couldn't find a species... Number is species in total is "+species.size()+", and the total adjusted fitness is "+completeWeight);
	}
	
	/**
	 * Selects a random genome from the species chosen, where genomes with a higher adjusted fitness have a higher chance of being selected
	 */
	private Genome getRandomGenomeBiasedAdjustedFitness(Species selectFrom, Random random) {
		double completeWeight = 0.0;	// sum of probablities of selecting each genome - selection is more probable for genomes with higher fitness
		for (FitnessGenome fg : selectFrom.fitnessPop) {
			completeWeight += fg.fitness;
		}
        double r = Math.random() * completeWeight;
        double countWeight = 0.0;
        for (FitnessGenome fg : selectFrom.fitnessPop) {
            countWeight += fg.fitness;
            if (countWeight >= r) {
            	 return fg.genome;
            }
        }
        throw new RuntimeException("Couldn't find a genome... Number is genomes in selæected species is "+selectFrom.fitnessPop.size()+", and the total adjusted fitness is "+completeWeight);
	}
	
	public int getSpeciesAmount() {
		return species.size();
	}
	
	public double getHighestFitness() {
		return highestScore;
	}
	
	public Genome getFittestGenome() {
		return fittestGenome;
	}
	
	public class FitnessGenome {
		
		double fitness;
		Genome genome;
		
		public FitnessGenome(Genome genome, double fitness) {
			this.genome = genome;
			this.fitness = fitness;
		}
	}
	
	public class Species {
		
		public Genome mascot;
		public List<Genome> members;
		public List<FitnessGenome> fitnessPop;
		public double totalAdjustedFitness = 0f;
		
		public Species(Genome mascot) {
			this.mascot = mascot;
			this.members = new LinkedList<Genome>(); 
			this.members.add(mascot);
			this.fitnessPop = new ArrayList<FitnessGenome>(); 
		}
		
		public void addAdjustedFitness(double adjustedFitness) {
			this.totalAdjustedFitness += adjustedFitness;
		}
		
		/*
		 *	 Selects new random mascot + clear members + set totaladjustedfitness to 0f
		 */
		public void reset(Random r) {
			int newMascotIndex = r.nextInt(members.size());
			this.mascot = members.get(newMascotIndex);
			members.clear();
			fitnessPop.clear();
			totalAdjustedFitness = 0f;
		}
	}
	
	public class FitnessGenomeComparator implements Comparator<FitnessGenome> {

		@Override
		public int compare(FitnessGenome one, FitnessGenome two) {
			if (one.fitness > two.fitness) {
				return 1;
			} else if (one.fitness < two.fitness) {
				return -1;
			}
			return 0;
		}
		
	}
	
	public void runEvaluation(int generations) {
		
		for (int i = 0; i <= generations; i++) {
			evaluate();
			if(i%100==0)
			{
				report(i);
			}	// TODO Auto-generated method stub
		}
	}
	public void report(int generation)
	{
		Genome fittest = getFittestGenome();
		double fitness = getHighestFitness();
		System.out.print("Generation: "+generation);
		System.out.print("\tHighest fitness: "+fitness);
		System.out.print("\tAmount of species: "+getSpeciesAmount());
		System.out.print("\tbest performer: "+fittest);
		float weightSum = 0;
		for (ConnectionGene cg : getFittestGenome().getConnectionGenes().values()) {
			if (cg.isExpressed()) {
				weightSum += Math.abs(cg.getWeight());
			}
		}
		System.out.print("\tWeight sum: "+weightSum);
		System.out.print("\n");
		
		for (Species s : species) {
			Collections.sort(s.fitnessPop, fitComp);
			{
				System.out.println("s: "+s.members.size()+" "+s.totalAdjustedFitness);
			}
		}
		//GenomePrinter.printGenome(fittest, "output/connection_sum_100/"+i+".png");
		NetIo.save(fittest,files+"Generation "+generation+".genome","Square root: fitness "+fitness);	
	}
	
}
