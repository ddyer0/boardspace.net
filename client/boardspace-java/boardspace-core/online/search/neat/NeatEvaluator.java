package online.search.neat;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
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

import lib.ErrorX;
import lib.G;
import lib.IntIntHashtable;

/**
 * General design details.  This works with a set of N variant Genomes, referred to as a Cohort
 * currently N=100, but other numbers of Genomes ought to work with similar effect.  It's an open
 * question what N is best for any partucular learning task.
 * 
 * The basic task is to "evaluate" each genome in the cohort, and rank them.  At the simplest
 * level this involves just comparing the performance of each genome to the ideal.  For game
 * play, each genome is compared to each other genome in the cohort, and the pairwise performances
 * are tallied.
 * 
 * The evaluated genomes in the cohort are grouped into species by a black box function,
 * based on their similarity, the number of species is desired to be about the square root
 * of the cohort size, and the parameters are tweaked at runtime to get approximately the
 * right number of species.  So with 100 genomes in the cohort, we'd ideally get 10 species
 * of 10 members each.  It's unknown if this is the ideal number of species.
 * 
 * Within each species, the two fittest members are passed into the next generation, and a
 * child is generated with these genomes as parents.  This is motivated by the idea of 
 * "improving the species" but it's unknown if that's 2+1 is the best solution. 
 * 
 * This yields 3xNumberOfSpecies genomes for the next generation.  The remainder of the new
 * cohort are generatedfrom pairs of genomes from the original set, weighted to selecting
 * from the fitter species.
 * 
 * Children are created with crossover from 2 parents and some set of random mutations of
 * various types; adding or deleting connections, adding or deleting nodes, disabling or
 * enabling connections and so on.  The probabilities of these operations are themselves
 * mutated and passed onto the new generation.  The idea being that mutation rates that
 * are successfull will be passed on, and will tend toward ideal rates.  It also seems
 * that these rates ought to change in the course of evolution - initially adding new
 * connections and nodes is obviously needed, whereas eventually only fine tuning of
 * parameter values is needed (if we're lucky!)
 * 
 * 
 * 
 */
public class NeatEvaluator {
	
	GenomeEvaluator evaluator;

	private FitnessGenomeComparator fitComp = new FitnessGenomeComparator();
		
	private Random random = new Random();
	
	/* Constants for tuning */
	private double C1 = 1.0;
	private double C2 = 1.0;
	private double C3 = 0.4;
	private double DT = 30.0;
	private int populationSize;
	
	private List<Genome> genomes;
	
	private List<Species> species;
	
	private Map<Genome, Species> mappedSpecies;
	private Map<Genome, Double> scoreMap;
	private double highestScore;
	private Genome fittestGenome;
	private int startingGeneration = 0;
	private String files = "output/";

	private String FITNESS_KEY = "Fitness";
	private String STARTING_FITNESS_KEY = "StartingFitness";
	private boolean saveCheckpoint = false;
	public NeatEvaluator(String project,int population,GenomeEvaluator e,String resumeFrom)
	{
		files =project;
		evaluator = e;
		populationSize = population;
		
		if(resumeFrom!=null &&resumeFrom.endsWith(".txt"))
		{	try {
			FileInputStream f = new FileInputStream(new File(resumeFrom));
			BufferedReader s = new BufferedReader(new InputStreamReader(f));
			resumeFrom = s.readLine();
			int idx = resumeFrom.lastIndexOf("-");
			if(idx>0) 
				{ 	String match = resumeFrom.substring(idx+1);
					int idx2 = match.lastIndexOf("/");
					if(idx2>0) { match = match.substring(0,idx2); }
					startingGeneration = G.IntToken(match)+1;				
				}
			f.close();
			}
			catch (IOException err)
			{
				G.print("can't read checkpoint ",resumeFrom," : ",err);
				resumeFrom = null;
			}
		}
	
		if(resumeFrom!=null)
		{

		reloadCohort(populationSize,resumeFrom);
		}
		else
		{
		createCohort(populationSize,e.createNetwork());
		}
	}
	public void sizeForPopulation(int populationSize)
	{
		this.populationSize = populationSize;
		genomes = new ArrayList<Genome>(populationSize);
		scoreMap = new HashMap<Genome, Double>();
		mappedSpecies = new HashMap<Genome, Species>();
		species = new ArrayList<Species>();

	}
	public void reloadCohort(int populationSize,String fromDirectory)
	{	sizeForPopulation(populationSize);
		File[] files = new File(fromDirectory).listFiles();
		for(File f : files)
		{	String name = f.getName();
			int ind = name.lastIndexOf('.');
			String type = ind>=0 ? name.substring(ind+1): "";
			if("genome".equals(type))
			{	try {
					Genome g = (Genome)NetIo.load(f);
					scoreMap.put(g,0.0);
					double fit = g.getParameter(FITNESS_KEY);
					g.setParameter(STARTING_FITNESS_KEY,fit);
					genomes.add(g);		
				}
				catch (ErrorX err)
				{
					G.print("damaged genome ",f," ",err);
				}
			}
		}
		saveCheckpoint = genomes.size()>populationSize;
		G.print(genomes.size()," genomes loaded");
	}
	
	public void createCohort(int populationSize, Genome startingGenome) 
	{	sizeForPopulation(populationSize);
		Random r = new Random();
		for (int i = 0; i < populationSize; i++) 
			{
			// fill the initial pool with mutants
			Genome newgenome = i==0 ? startingGenome
					: makeNewGeneration(r.nextInt(),
								genomes.get(r.nextInt(genomes.size())),
								genomes.get(r.nextInt(genomes.size())));
			scoreMap.put(newgenome,0.0);
			genomes.add(newgenome);			
		}	
		fittestGenome = genomes.get(0);

	}
	
	public NeatEvaluator(int populationSize,GenomeEvaluator e) 
	{	evaluator = e;
		createCohort(populationSize,e.createNetwork());
	}
	
	/**
	 * Runs one generation
	 */
	public void evaluate(int generation) 
	{
		// Reset everything for next generation
		scoreMap.clear();
		mappedSpecies.clear();
		highestScore = -100;
		fittestGenome = null;

		speciate();
		List<FitnessGenome> totalFitness = new ArrayList<FitnessGenome>(genomes.size());
		
		// Evaluate genomes and assign score
		evaluator.startGeneration();
		
		for (Genome g : genomes) {
			if(g!=null)
			{
			Species s = mappedSpecies.get(g);		// Get species of the genome
			
			double score = evaluator.evaluate(g);
			g.setParameter(FITNESS_KEY,score);
			
			s.addFitness(score);	
			FitnessGenome fg = new FitnessGenome(g, score);
			totalFitness.add(fg);
			s.fitnessPop.add(fg);
			scoreMap.put(g, score);
			if (score > highestScore) {
				highestScore = score;
				fittestGenome = g;
			}}
		}
		
		evaluator.finishGeneration();
		//System.out.println("Fittest "+fittestGenome);
		
		if(saveCheckpoint || exitRequest || (generation % 50 == 0))
				{	saveCheckpoint = false;
					saveCheckpoint(generation,totalFitness);
				}
		
	}
	public void speciate()
	{
		int targetNumberOfSpecies = (int)Math.sqrt(genomes.size()+0.99);
		int loops = 0;
		do
		{
		for (Species s : species) {	s.reset(random); }
		// Place genomes into species
		for (Genome g : genomes) {
			if(g!=null)
			{
			boolean foundSpecies = false;
			for (Species s : species) {
				{
				double dis = Genome.compatibilityDistance(g, s.mascot, C1, C2, C3);
				if (dis < DT) { // compatibility distance is less than DT, so genome belongs to this species
					s.members.add(g);
					g.setSpecies(s);
					mappedSpecies.put(g, s);
					foundSpecies = true;
					break;
				}}
			}
			if (!foundSpecies) { // if there is no appropiate species for genome, make a new one
				Species newSpecies = new Species(g);
				species.add(newSpecies);
				g.setSpecies(newSpecies);
				mappedSpecies.put(g, newSpecies);
				//G.print("New species "+newSpecies+" "+g);
			}}
		}
		int ns = species.size();
		if(ns>targetNumberOfSpecies+1) { DT *= 1.5; }
		else if(ns<targetNumberOfSpecies+1) { DT *= 0.75; DT=Math.max(0.5,DT); }

		// Remove unused species
		Iterator<Species> iter = species.iterator();
		while(iter.hasNext()) {
			Species s = iter.next();
			if (s.members.isEmpty()) {
				iter.remove();
				//G.print("remove species "+s);
			}
		}
		} while(loops++<8
				&& DT>0.5
				&& ((species.size()>targetNumberOfSpecies+2) 
				|| (species.size()<targetNumberOfSpecies-2)));
	
	}
	//
	// this was a very slow experiment without result.  Making each new child qualify as fitter than it's parent
	// was exceptionally and inexplicably slow. About 70% of applicants were rejected
	//
	boolean qualifyNext = false;
	@SuppressWarnings("unused")
	public void makeNewGeneration()
	{
		List<Genome> nextGenGenomes = new ArrayList<Genome>(populationSize);
		IntIntHashtable generationCounts = new IntIntHashtable();
		@SuppressWarnings("unused")
		int accepted = 0;
		int rejected = 0;
		
		// put best genomes from each species into next generation
		for (Species s : species) {
			Collections.sort(s.fitnessPop, fitComp);
			int nextSize= nextGenGenomes.size();
			for(int i=0;i<Math.min(2,s.fitnessPop.size());i++)		// keep 2 from each species
			{
				FitnessGenome fittestInSpecies = s.fitnessPop.get(i);
				Genome genome = fittestInSpecies.genome;
				G.Assert(genome!=null,"fittest in species is null");
				nextGenGenomes.add(genome);
				int generation = genome.generation;
				generationCounts.put(generation,generationCounts.get(generation)+1);
				
				//System.out.println("keep "+fittestInSpecies.genome+" "+fittestInSpecies.fitness);
			}
			if(nextGenGenomes.size()-nextSize==2)
			{
				// add a child of those two winning parents
				Genome parent = nextGenGenomes.get(nextSize);
				Genome parent2 = nextGenGenomes.get(nextSize+1);
				Genome child = makeNewGeneration(random.nextInt(),
						parent,parent2
						);
				if(qualifyNext)
				{
				double parentfit = scoreMap.get(parent);
				double parent2fit = scoreMap.get(parent2);
				G.Assert(parentfit>=parent2fit,"should be fitter");

				G.Assert(child!=null,"child species is null");
				double childfit = evaluator.evaluate(child);
				if(childfit>parentfit)
				{
				accepted++;
				nextGenGenomes.add(child);
				}
				else 
				{
					rejected++;
				}
				}
				else
				{
					nextGenGenomes.add(child);
				}
			}
		}
		/*
		 * this prints a list of the generation map of the genomes surviving into the next round.
		 * it shows the desired/expected behavior, that several distinct lines coexist and develop
		 * in parallel.
		int keys[] = generationCounts.getKeys();
		Arrays.sort(keys);
		System.out.print("carry over ");
		for(int k :keys)
		{
			System.out.print("g"+k+":"+generationCounts.get(k)+" ");
		}
		System.out.println();
		*/
		// Breed the rest of the genomes
		while (nextGenGenomes.size() < populationSize) { // replace removed genomes by randomly breeding
			Species s = getRandomSpeciesBiasedAjdustedFitness(random);
			
			Genome p1 = getRandomGenomeBiasedAdjustedFitness(s, random);
			Genome p2 = getRandomGenomeBiasedAdjustedFitness(s, random);
			Genome child = makeNewGeneration(random.nextInt(),p1,p2);
			if(qualifyNext)
			{
			
			double p1fit = scoreMap.get(p1);
			double p2fit = scoreMap.get(p2);
			double parentfit = Math.max(p1fit,p2fit);
			double childfit = evaluator.evaluate(child);
			if(childfit>parentfit)
			{
			accepted++;
			nextGenGenomes.add(child);
			}
			else
			{
				rejected++;
			}
			}
			else if(child!=null)
			{
				nextGenGenomes.add(child);
			}
		}
		//G.print("rejected "+((double)rejected/(accepted+rejected)));
		genomes = nextGenGenomes;
	}
	
	private static int errorCount = 1;
	private Genome makeNewGeneration(int seed,Genome p1,Genome p2)
	{	Random r = new Random(seed);
		//p1.audit();
		//p2.audit();
		Genome child = mateWithCrossover(r,p1,p2);
		try {
		child.audit();
		child.killConnectionMutation(random);	// disable some random connections
		child.audit();
		child.mutation(random);					// random mutations at the child's mutation rate
		//child.audit();
		child.addNodeMutation(random);			// random new nodes proportional to the child's count
		//child.audit();
		child.addConnectionMutation(random);	// random new connections proportional to the child's node count
		child.audit();
		return child;
		}
		catch (Throwable err)
		{	G.print("error creating next generation from ",p1,p2," : ",err);
			NetIo.save(child,"g:/temp/nn/train/error-makenewgeneration"+errorCount++,
					"error in makenewgeneration "+err+"\n"+err.getStackTrace());
		}
		return null;
	
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
            completeWeight += s.averageFitness();
		}
        double r = Math.random() * completeWeight;
        double countWeight = 0.0;
        for (Species s : species) {
            countWeight += s.averageFitness();
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
		public double totalFitness = 0;
		public static int speciesCount = 0;
		int speciesNumber = 0;
		public String toString() { return "<species #"+speciesNumber+" :"+members.size()+">";}
		public Species(Genome mascot) {
			this.mascot = mascot;
			speciesCount++;
			speciesNumber = speciesCount;
			this.members = new LinkedList<Genome>(); 
			this.members.add(mascot);
			this.fitnessPop = new ArrayList<FitnessGenome>(); 
		}
		
		public double averageFitness() {
			int size = members.size();
			return size==0 ? 0 : totalFitness/size;
		}

		public void addFitness(double fitness) {
			this.totalFitness += fitness;
		}
		
		/*
		 *	 Selects new random mascot + clear members + set totaladjustedfitness to 0f
		 */
		public void reset(Random r) {
			if(members.size()>0)
			{
			int newMascotIndex = r.nextInt(members.size());
			this.mascot = members.get(newMascotIndex);
			members.clear();
			fitnessPop.clear();
			totalFitness = 0f;
			}
		}
	}

	public class FitnessGenomeComparator implements Comparator<FitnessGenome> {

		public int compare(FitnessGenome one, FitnessGenome two) {
			if (one.fitness < two.fitness) {
				return 1;
			} else if (one.fitness > two.fitness) {
				return -1;
			}
			return 0;
		}		
	}
	/**
	 * get the full cohort of genomes currently under test
	 * @return
	 */
	public Genome[] getCohort() {
		return (genomes.toArray(new Genome[genomes.size()]));
	}
	public boolean exitRequest = false;
	public void runEvaluation(int generations) {
		
		for (int i = startingGeneration; i <= generations && !exitRequest; i++) {
			evaluate(i);
			//if(i%100==0)
			report(i);
			makeNewGeneration();
		}
	}
	public void saveCheckpoint(int generation,List<FitnessGenome>fitnessList)
	{		
		Genome fittest = getFittestGenome();
		double fitness = getHighestFitness();
		Collections.sort(fitnessList,fitComp);
		
		NetIo.save(fittest,files+"Generation "+generation+".genome","fitness "+fitness);	

		// this reports on the species distribution to see if its working reasonably
		// last time it ran the sizes and fitness of the species were plausible
		
		for (Species sspec : species) {
				System.out.println("s: "+sspec+" "+sspec.averageFitness());
		}
	
		int item = 0;
		String cohort = files+"cohort-generation-"+generation+"/";
		for(FitnessGenome fg : fitnessList)
		{
		NetIo.save(fg.genome,
					 cohort+"item-"+generation+"-"+item+".genome",
					"generaton "+generation);	
		item++;
		}
		PrintStream f;
		try {
			f = new PrintStream(new FileOutputStream(files+"checkpoint.txt"));
			f.println(cohort);
			f.close(); 
		}
		catch (IOException err)
		{
		}
	}
	public void report(int generation)
	{
		Genome fittest = getFittestGenome();
		double fitness = getHighestFitness();
		System.out.print("Generation: "+generation);
		System.out.print("\tFitness: "+fitness);
		System.out.print("\tSpecies: "+getSpeciesAmount());
		System.out.print("\tbest: "+fittest);
		Species s = mappedSpecies.get(fittest);
		System.out.println(" "+s+" avg "+s.averageFitness());
		/*
		float weightSum = 0;
		for (ConnectionGene cg :fittest.getConnectionGenes().values()) {
			if (cg.isExpressed()) {
				weightSum += Math.abs(cg.getWeight());
			}
		}
		System.out.print("\tWeight sum: "+weightSum);
		System.out.print("\n");
		*/
		
	}
	public void setExitRequest(boolean b) {
		exitRequest = b;
	}

}
