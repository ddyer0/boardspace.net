/* copyright notice */package mutton;

import java.awt.Rectangle;

import online.game.*;
import online.game.export.ViewerProtocol;
import online.search.*;
import java.util.*;
import lib.*;
import lib.Random;

/**
 * The Robot player only has to implement the basic methods to generate and evaluate moves.
 * the actual search is handled by the search driver framework.
 * 
 * In general, the Robot has it's own thread and operates on a copy of the board, so the
 * main UI can continue unaffected by the processing of the robot.
 * 
 * This robot player is version 2, and plays a better game of Mutton than the random bot.
 * 
 * For the standard Mutton game, there are 14,950 ways that the wolves can be chosen.
 *   26 animals taken 4 at a time = (26!) / ((4!) * (26-4)!) = 14,950.
 * The core functionality of this robot player is that all 14,950 combinations are
 * evaluated against the current history of eaten events.  If a combination is not
 * consistent with history, then it is thrown out.  The valid configs are then totaled
 * up to determine the number of times that each living animal appears in the valid
 * configs.  The higher this number, then the more likely that animal is of being a
 * wolf.
 * 
 * As the wolf:
 *    When eating:
 *       We pretend to eat each potential meal and evaluate what the farmer would know
 *       if that new meal were added to the history.  We then choose the meal that
 *       keeps the average probability of all animals being a wolf to a minimum.
 *
 *    When moving:
 *       We look at the result of moving all suspects into all valid empty spaces
 *       and then look at all of the meals that we would have (assuming the farmer
 *       doesn't shoot anyone) and evaluate what the farmer would know if that new
 *       meal were added to the history.  Of all of these moves, we pick the move
 *       that keeps the average probability of all animals being a wolf to a minimum.
 *       Note: Actually trying all suspects into all empty spaces takes a very long
 *             time, computationally.  (For 6 suspects, it took over 4 minutes to
 *             move on a 2.5Ghz PC).  To avoid this, the number of simultaneous
 *             movers is limited to 3.  If there are more than 3 suspects to be
 *             moved, then 3 are picked at random and used to evaluate the positions.
 *             Since only 1 animal is moved at a time, the robot player will be
 *             called again to move the remaining ones.  Until the number of suspects
 *             to be moved drops below 3, then we're really making a local best-effort
 *             move rather than a global best-effort move.
 * 
 * As the farmer:
 *    When shooting:
 *       We evaluate the probability of all animals being a wolf.  If the animal
 *       with the highest probability is above a threshold, then we will shoot it.
 *       (If there are more than 1 animal with the same highest probability, then
 *       we pick one of them randomly.)  The shooting threshold starts at 60% and
 *       is reduced by 5% for every dead sheep.
 *       If we have determined exactly who the wolves are (ie: all living wolves
 *       have a probability of 100%), then we will rampage and shoot all of them
 *       on our turn.
 *
 *    When moving:
 *       We look at the result of moving all scared sheep into all vacant spaces
 *       and look at all the meals that the wolf might do on his next turn.  For
 *       each possible next meal, we determine how many wolf configs would remain
 *       and we pick the move that makes the remaining configs the smallest.
 * 
 * 
 * Note:
 *    This mutton robot does some look-ahead, but does not use any of the typical
 *    min/max type of board evaluation.
 *    So, this robot really wants to just implement the SimpleRobotProtocol.
 *    However, the sample code provided (Hex) uses the full min/max search engine
 *    and only exposes those interfaces used by that engine and (presumably) hides 
 *    other important work that would need to be done for a SimpleRobotProtocol
 *    (run(), for example...).
 * 
 *    So, this code pretends to do min/max, but doesn't really.
 *    It implements DoFullMove() to return a move, but the rest of the min/max
 *    functions are stubbed out and never called.
 * 
 * @author rwalter
 *
 */
public class MuttonPlayV2Bot
       extends commonRobot<MuttonGameBoard>
       implements Runnable, MuttonConstants, RobotProtocol
{
	

	// Random number generator used by the robot player.
	Random randGen = new Random();

	// The threshold probability for the farmer to shoot.  If all animals have
	// probabilities below this, then the farmer will pass.
	private final static double SHOOT_THRESHOLD_DERATE = 0.05;
	private final static double SHOOT_THRESHOLD_BASE = 0.60 + SHOOT_THRESHOLD_DERATE;

	// The number of animals & wolves in the game.
	private static final int NUM_ANIMALS = 26;
	private static final int NUM_WOLVES = 4;

	// These next items are used to calculate the probabilities that each animal
	// is a wolf given the current history.
	// The number of ways that the wolf player could have chosen the wolves
	// that is consistent with history.
	private int totalPossibilities;

	// Counts for each animal that indicates how many valid wolf configs include
	// that animal as a wolf.
	private int [] animalCounts = new int [NUM_ANIMALS];

	// The list of valid wolf configs.
	// It is computed each move by exhaustively checking all combinations of
	// NUM_ANIMALS taken NUM_WOLVES at a time (14,950 for the default config
	// of 26 taken 4 at a time) against the current history to determine if it
	// is possible.  Each entry of this vector is an array of NUM_WOLVES integers
	// which indicates who the wolves are in that configuration.
	private Vector<int[]> validWolfConfigs = new Vector<int[]> ();

	// These are used while determining the best meal to eat
	private double minAvgWolfProbability;
	private int bestVictimId = -1;
	private Vector<MuttonCell> neighbors = new Vector<MuttonCell> ();

	// This array is used when the farmer relocates scared animals after shooting
	// them when shotgun is enabled.
	@SuppressWarnings("unchecked")
	Vector<MuttonCell> [] validMealNeighborArray = new Vector [NUM_ANIMALS];

	// Comparing the outcome of all possible ways to move the animals into
	// all possible spaces can take an extraordinary amount of time if there
	// are too many sheep to move.  Experimentation has shown that considering
	// a maximum of 3 makes this run in a reasonable time.
	private final static int WOLF_RELOCATE_MAX_SUSPECTS = 3;
	private final static int FARMER_RELOCATE_MAX_SUSPECTS = 3;

	/*
	 * This class holds an element that describes a relocation event.  It includes
	 * the index of the animal that is moving and the index of the space that the
	 * animal is moving to.  This is used by the wolf & sheep relocation routines
	 * to keep track of best moves seen so far during the move scan.
	 */
	private class RelocationPair {
		public int mover;
		public int destination;
		public RelocationPair (int moverList, int destinationList) {
			this.mover = moverList;
			this.destination = destinationList;
		}
	}

	// Debug flag that will cause the robot to be very loquacious when enabled.
	boolean bverbose = false;

	/**
	 * Constructor for a new random robot player.
	 */
	public MuttonPlayV2Bot () {
		// Create the vectors of the validMealNeighborArray.
		for (int i=0; i < validMealNeighborArray.length; i++) {
			validMealNeighborArray[i] = new Vector<MuttonCell>();
		}
	}


	/**
	 * Prepare the robot, but don't start making moves.  G is the game object, gboard
	 * is the real game board.  The real board shouldn't be changed.  Evaluator and Strategy
	 * are parameters from the applet that can be interpreted as desired.  The debugging 
	 * menu items "set robotlevel(n)" set the value of "strategy".  Evaluator is not
	 * really used at this point, but was intended to be the class name of a plugin
	 * evaluator class.
	 *
	 * For Mutton, the strategy level is used by the newRobotPlayer() in MuttonGameViewer
	 * to create the correct robot player.  So, it is unused here.  This may change in the
	 * future depending on how robot player development goes...
	 */
	public void InitRobot (ViewerProtocol newParam, ExtendedHashtable info, BoardProtocol gboard,
	                       String evaluator, int strategy) {
		InitRobot(newParam, info, strategy);
		GameBoard = (MuttonGameBoard) gboard;
		board = new MuttonGameBoard(GameBoard.gametype, GameBoard.randomKey);
		if (bverbose) System.out.println("V2_Robot: InitRobot() Original Board = " + gboard + ", my clone = " + board);
		MONTEBOT = (strategy==MONTEBOT_LEVEL);
	}

	/** PrepareToMove is called in the thread of the main game run loop at 
	 * a point where it is appropriate to start a move.  We must capture the
	 * board state at this point, so that when the robot runs it will not
	 * be affected by any subsequent changes in the real game board state.
	 * The canonical error here was the user using the < key before the robot
	 * had a chance to capture the board state.
	 */
	public void PrepareToMove (int playerIndex) {
		if (bverbose) System.out.println("V2_Robot: Entering PrepareToMove()");

		// use this for a friendly robot that shares the board class
		board.copyFrom(GameBoard);

		// check that we got a good copy.  Not expensive to do this once per move
		board.sameboard(GameBoard);
	}

	public commonMove DoAlphaBetaFullMove() {
		if (bverbose) System.out.println("V2_Robot: Entering DoFullMove()");

		if (bverbose) {
			if (board.whoseTurn == board.getFarmerId()) {
				System.out.println("V2_Robot: I'm the farmer");
			} else {
				System.out.println("V2_Robot: I'm the wolf");
			}
		}

		MuttonMoveSpec theMove;

		switch (board.getState()) {
		case GAMEOVER_STATE :
			theMove = new MuttonMoveSpec("Done", board.whoseTurn);
			break;
		case FARMER_CONFIGURING_BOARD :
			theMove = new MuttonMoveSpec("Done_Relocating", board.whoseTurn);
			break;
		case WOLF_HIDING_STATE :
			if (board.getWolfWinTarget() >= 15) {
				// If the other player has set the wolf win target too high
				// (made it harder for the wolf player to win), then I want
				// to play as the farmer, not the wolf.
				theMove = new MuttonMoveSpec(SWAP, board.whoseTurn);
			} else {
				theMove = doV2WolfHiding();
			}
			break;
		case WOLF_CHOOSING_MEAL_STATE :
			theMove = doV2WolfEat();
			break;
		case WOLF_MOVING_SHEEP_STATE :
			theMove = doV2WolfRelocateMove();
			break;
		case FARMER_MOVING_SHEEP_STATE :
			theMove = doV2FarmerRelocateMove();
			break;
		case FARMER_CHOOSING_TARGETS_STATE :
			theMove = doV2FarmerShoot();
			break;
		default :
			System.out.println("V2_Robot: Unexpected board state: " + board.getState());
			theMove = null;
			break;
		}

		if (bverbose) {
			if (theMove != null) {
				System.out.println("V2_Robot Move: " + theMove.moveString());
			} else {
				System.out.println("V2_Robot Move: null");
			}
		}

		return theMove;
	}

	/*
	 * Pick 4 random sheep to hide the wolves in.
	 */
	private MuttonMoveSpec doV2WolfHiding () {
		// Create a vector of the 26 sheep.
		Vector<Integer> v = new Vector<Integer> ();
		for (int i = 0; i < 26; i++) {
			v.addElement(Integer.valueOf(i));
		}

		// Shuffle it.
		randGen.shuffle(v);

		// Create the move string 
		String moveString = "Wolves_Hidden";
		for (int i = 0; i < 4; i++) {
			moveString += (" " + v.elementAt(i).toString());
		}

		// Return the move to make
		return new MuttonMoveSpec (moveString, board.whoseTurn);
	}

	/*
	 * For each potential victim, we calculate the new probabilities for the
	 * remaining living wolves.  We then choose the victim that minimizes the
	 * average probability of the wolves.
	 */
	private MuttonMoveSpec doV2WolfEat () {
		// Generate the list of valid wolf configs (which is used by computeBestMeal)
		genValidWolfConfigs (board.getHistory(), board.getSheepStatus());

		// Compute the best meal to eat this turn.
		boolean hasMeal = computeBestMeal();

		if (hasMeal) {
			// Eat the best victim.
			return new MuttonMoveSpec ("Eat " + bestVictimId, board.whoseTurn);
		} else {
			// If no meals, then must pass.
			return new MuttonMoveSpec ("Eat", board.whoseTurn);
		}
	}

	/*
	 * Given a vector of potential meals, compute the "best" one to eat.
	 * The best is defined as the one that, if eaten, results in the lowest
	 * average wolf probabilities for the remaining living wolves.
	 *
	 * This sets:
	 *    bestVictim to the id of the best victim
	 *    minAvgWolfProbability to the average probability for the living
	 *       wolves that results from eating bestVictim.
	 *
	 * @param v  The vector of meals.
	 * @return true => There is at least 1 valid meal.
	 *         false => There are no valid meals.
	 */
	private boolean computeBestMeal () {
		// Try to eat a sheep...
		Vector<MuttonCell> v = board.getValidMeals(false);
		if (v.size() == 0) {
			// ... but if there aren't any, then eat a wolf.
			v = board.getValidMeals(true);
		}

		if (v.size() == 0) {
			// If no meals, then return false.
			return false;
		}

		// Create our "fake" environment that we will use to start looking
		// at the result of eating each potential victim in v.
		minAvgWolfProbability = 1.5;     // Something greater than 1.
		bestVictimId = -1;

		// For each potential meal, calculate the new probability distribution
		// that would result if we were to eat that one.
		for (int m = 0; m < v.size(); m++) {
			// Get the next meal.
			MuttonCell potentialMeal = v.elementAt(m);
			boolean isWolf = potentialMeal.isWolf();
			int mealId = potentialMeal.getSheepId();

			// Calculate the new probabilities for this configuration.
			board.getAliveNeighbors(potentialMeal, neighbors);
			filterPossibilitiesWithAdditionalDeadAnimal(mealId, board.createListOf(neighbors), isWolf);

			if (isWolf) {
				// enumeratePossibilities zeros out the count for known wolves.
				// If we leave the count as 0, then averageProbability will use
				// that 0 weight as meaning that the farmer has no idea that this
				// is a wolf, where we really want to make it 100%, since the
				// farmer will know that this is a wolf.  So, we have to adjust
				// the count back to the max.  This adjustment makes eating a
				// wolf very bad, rather than very good.
				animalCounts[mealId] = totalPossibilities;
			}

			// If the average probability for living wolves of this victim is
			// less than the minimum seen so far, then we have a new best victim.
			double averageProbability = calculateMaxProbability();
			if (averageProbability < minAvgWolfProbability) {
				bestVictimId = mealId;
				minAvgWolfProbability = averageProbability;
			}
		}

		// If the best victim is still -1, then something went wrong, so just
		// eat the first valid victim.
		if (bestVictimId == -1) {
			bestVictimId = ( v.elementAt(0)).getSheepId();

			if (bverbose) {
				System.out.println("V2_Robot computeBestMeal: No best victim!");
			}
		}

		return true;
	}

	/*
	 * Calculate the maximum probability relative to the total options in the
	 * animalCounts array.  This is used by the wolf to keep the farmer at the
	 * best disadvantage in that the probabilities of any animal being a wolf
	 * is minimized.
	 */
	private double calculateMaxProbability () {
		int max = 0;
		int counter = 0;
		for (int i=0; i < animalCounts.length; i++) {
			max = Math.max(max, animalCounts[i]);
			counter += animalCounts[i];
		}
		return ((double) max / (double) counter);
	}

	/*
	 * Move a suspect away from the most recent victim to a random valid spot
	 * on the board.
	 * If all suspects have been moved, then send a done move.
	 */
	private MuttonMoveSpec doV2WolfRelocateMove () {
		// Get the suspects that need to move.
		Vector<MuttonCell> suspectsToMove = board.getSuspectsToMove();
		if (suspectsToMove.size() == 0) {
			// No more suspects to move, so issue a "Done" message
			return new MuttonMoveSpec ("Done_Relocating", board.whoseTurn);
		}

		// Get the list of empty board spaces that we can move into
		Vector<MuttonCell> emptyBoardSpaces = board.getValidEmptyCells();

		// Trim the list of suspects if it is either (a) greater than the number of
		// spaces, or (b) greater than the max to consider in one relocation call.
		trimVector(suspectsToMove, Math.min(WOLF_RELOCATE_MAX_SUSPECTS, emptyBoardSpaces.size()));
		int suspectCount = suspectsToMove.size();

		// Generate the list of valid wolf configs (which is used by computeBestMeal)
		genValidWolfConfigs (board.getHistory(), board.getSheepStatus());

		double relocateBestAvg = 1.5;  // Something large.
		Vector<RelocationPair> bestRelocationPairs = new Vector<RelocationPair>();

		// Create generators for permuting the suspects and for combinations of
		// board spaces that they can be moved to.
		PermutationGenerator suspectOrderGen = new PermutationGenerator(suspectCount);
		CombinationGenerator spaceGen = new CombinationGenerator(emptyBoardSpaces.size(), suspectCount);

		// Go through all combinations of empty spaces that we could move to.
		while (spaceGen.hasMore()) {
			int [] nextSpaceCombination = spaceGen.next();

			// Go through all permutations of moving suspects into the current spaces.
			suspectOrderGen.reset();
			while (suspectOrderGen.hasMore()) {
				int [] suspectOrder = suspectOrderGen.next();

				// Move the suspects to the spaces as defined by suspectOrder &
				// nextSpaceCombination.
				for (int i=0; i<suspectCount; i++) {
					MuttonCell toCell = emptyBoardSpaces.elementAt(nextSpaceCombination[i]);
					MuttonCell fromCell = suspectsToMove.elementAt(suspectOrder[i]);
					toCell.moveAnimalFrom(fromCell);
				}

				// Compute the best meal for this config. 
				board.rescanValidMeals();
				if (computeBestMeal()) {
					if (minAvgWolfProbability < relocateBestAvg) {
						bestRelocationPairs.setSize(0);
						relocateBestAvg = minAvgWolfProbability;
					}
					if (relocateBestAvg == minAvgWolfProbability) {
						bestRelocationPairs.addElement(new RelocationPair (suspectOrder[0], nextSpaceCombination[0]));
					}
				}

				// Unmove suspects as defined by suspectOrder & nextSpaceCombination
				for (int i=0; i<suspectCount; i++) {
					MuttonCell fromCell = emptyBoardSpaces.elementAt(nextSpaceCombination[i]);
					MuttonCell toCell = suspectsToMove.elementAt(suspectOrder[i]);
					toCell.moveAnimalFrom(fromCell);
				}
			}
		}

		// Because we don't move all suspects at once (which would take too long
		// to compute), it may occur that moving the subset of suspects that
		// were randomly chosen leaves the wolf suspect with no victims.  In this
		// case, all the moves would come back as invalid and we'd be left with no
		// moves to chose from.  In this case, just make a random move.
		if (bestRelocationPairs.size() == 0) {
			bestRelocationPairs.addElement(new RelocationPair (
			                                   Random.nextInt(randGen, suspectCount),
			                                   Random.nextInt(randGen, emptyBoardSpaces.size())));
		}

		// Choose a move from the saved best ones.
		if (bverbose) {
			System.out.println("V2_Robot doV2WolfRelocateMove:");
			System.out.println("         Number wolves to move = " + suspectsToMove.size());
			System.out.println("         best avg = " + relocateBestAvg);
			System.out.println("         Number of potential relocations = " + bestRelocationPairs.size());
		}

		RelocationPair selectedPair = bestRelocationPairs.elementAt(Random.nextInt(randGen, bestRelocationPairs.size()));
		MuttonCell moverCell = suspectsToMove.elementAt(selectedPair.mover);
		MuttonCell destinationCell = emptyBoardSpaces.elementAt(selectedPair.destination);

		if (bverbose) {
			System.out.println("    Selected Pair = " + selectedPair.mover + ", " + selectedPair.destination);
		}

		return new MuttonMoveSpec ("Relocate " + moverCell.getSheepId() + " " + destinationCell.getCol() + " " + destinationCell.getRow(), board.whoseTurn);
	}

	/*
	 * Remove random elements from the given vector until its length is less than
	 * or equal to the given maxLength parameter.
	 */
	private void trimVector (Vector<MuttonCell> v, int maxLength) {
		while (v.size() > maxLength) {
			int removeItemId = Random.nextInt(randGen, v.size());
			v.removeElementAt(removeItemId);
		}
	}

	/*
	 * Move scared sheep away from the most recent victim of the farmer's shot.
	 */
	private MuttonMoveSpec doV2FarmerRelocateMove () {
		// Get the suspects that need to move.
		Vector<MuttonCell> suspectsToMove = board.getSuspectsToMove();
		if (suspectsToMove.size() == 0) {
			// No more suspects to move, so issue a "Done" message
			return new MuttonMoveSpec ("Done_Relocating", board.whoseTurn);
		}

		// Compute farmer knowledge of the current board position
		prepareFarmerInfo();

		// Get the list of empty board spaces that we can move into
		Vector<MuttonCell> emptyBoardSpaces = board.getValidEmptyCells();
		Vector<MuttonCell> relocatedBoardSpaces = new Vector<MuttonCell>();

		// Trim the list of suspects if it is either (a) greater than the number of
		// spaces, or (b) greater than the max to consider in one relocation call.
		trimVector(suspectsToMove, Math.min(FARMER_RELOCATE_MAX_SUSPECTS, emptyBoardSpaces.size()));
		int suspectCount = suspectsToMove.size();

		// A list of relocations that are currently the "best"
		int bestScore = 100000;  // Something large
		Vector<RelocationPair> bestRelocationPairs = new Vector<RelocationPair>();

		// Create generators for permuting the suspects and for combinations of
		// board spaces that they can be moved to.
		PermutationGenerator suspectOrderGen = new PermutationGenerator(suspectCount);
		CombinationGenerator spaceGen = new CombinationGenerator(emptyBoardSpaces.size(), suspectCount);

		// Go through all combinations of empty spaces that we could move to.
		while (spaceGen.hasMore()) {
			int [] nextSpaceCombination = spaceGen.next();

			// Go through all permutations of moving suspects into the current spaces.
			suspectOrderGen.reset();
			while (suspectOrderGen.hasMore()) {
				int [] suspectOrder = suspectOrderGen.next();

				// Move the suspects to the spaces as defined by suspectOrder &
				// nextSpaceCombination.
				relocatedBoardSpaces.setSize(0);
				for (int i=0; i<suspectCount; i++) {
					MuttonCell toCell = emptyBoardSpaces.elementAt(nextSpaceCombination[i]);
					MuttonCell fromCell = suspectsToMove.elementAt(suspectOrder[i]);
					toCell.moveAnimalFrom(fromCell);
					relocatedBoardSpaces.addElement(toCell);
				}

				// We need to verify that all of the suspects that are moving are
				// next to a live neighbor before bothering to evaluate the position.
				if (verifyAllHaveLiveNeighbor(relocatedBoardSpaces)) {
					// Determine which animals are valid meals.
					// For the farmer, who doesn't know who the wolves are, a valid meal is a live
					// animal who is next to a live animal who has a non-zero chance of being a wolf.
	
					// Clear the validMealNeighborArray vectors to all empty.
					for (int i=0; i < validMealNeighborArray.length; i++) {
						validMealNeighborArray[i].setSize(0);
					}

					// Scan the board, looking for live animals.  For each one found, determine if
					// it's a valid meal.  If it is, then populate the validMealNeighborArray with
					// the live neighbors.
					for (int c = 0; c < BOARD_COLS; c++) {
						for (int r = 0; r < BOARD_ROWS; r++) {
							MuttonCell cell = board.getCell(c,r);
							if (cell.isAlive() && !cell.isEmpty()) {
								// Get live neighbors from the board.
								int id = cell.getSheepId();
								board.getAliveNeighbors(cell, validMealNeighborArray[id]);
	
								// If all of the live neighbors have a 0% chance of being a wolf,
								// then this animal isn't really a valid meal.
								if (!mayHaveWolf(validMealNeighborArray[id])) {
									validMealNeighborArray[id].setSize(0);
								}
							}
						}
					}
	
					// For each valid meal, determine how many wolf combinations would be left if
					// that animal were the wolf's next victim.  Keep the maximum count as the "score"
					// for this relocation.
					int thisScore = -1;
					for (int i=0; i < validMealNeighborArray.length; i++) {
						if (validMealNeighborArray[i].size() > 0) {
							int currCount = countPossibilitiesWithAdditionalDeadAnimal(i, board.createListOf(validMealNeighborArray[i]));
							thisScore = Math.max(thisScore, currCount);
						}
					}
	
					// If this relocation's score is less than the best one found so far,
					// then toss the old ones and save this one.
					if ((thisScore > 0) && (thisScore < bestScore)) {
						bestRelocationPairs.setSize(0);
						bestScore = thisScore;
					}
					// If this score equals the best score, then add it to the list of best ones.
					if (thisScore == bestScore) {
						bestRelocationPairs.addElement(new RelocationPair (suspectOrder[0], nextSpaceCombination[0]));
					}
				}

				// Unmove suspects as defined by suspectOrder & nextSpaceCombination
				for (int i=0; i<suspectCount; i++) {
					MuttonCell fromCell = emptyBoardSpaces.elementAt(nextSpaceCombination[i]);
					MuttonCell toCell = suspectsToMove.elementAt(suspectOrder[i]);
					toCell.moveAnimalFrom(fromCell);
				}
			}
		}

		// Choose a move from the saved best ones.
		if (bverbose) {
			System.out.println("V2_Robot doV2FarmerRelocateMove:");
			System.out.println("         Number animals to move = " + suspectsToMove.size());
			System.out.println("         best score = " + bestScore);
			System.out.println("         Number of potential relocations = " + bestRelocationPairs.size());
		}

		RelocationPair selectedPair = bestRelocationPairs.elementAt(Random.nextInt(randGen, bestRelocationPairs.size()));
		MuttonCell moverCell = suspectsToMove.elementAt(selectedPair.mover);
		MuttonCell destinationCell = emptyBoardSpaces.elementAt(selectedPair.destination);

		if (bverbose) {
			System.out.println("    Selected Pair = " + selectedPair.mover + ", " + selectedPair.destination);
		}

		return new MuttonMoveSpec ("Relocate " + moverCell.getSheepId() + " " + destinationCell.getCol() + " " + destinationCell.getRow(), board.whoseTurn);
	}

	/*
	 * Determine if all of the cells in the given vector have at least one live
	 * neighbor.
	 */
	private boolean verifyAllHaveLiveNeighbor (Vector<MuttonCell> animals) {
		for (int i=0; i < animals.size(); i++) {
			MuttonCell cell = animals.elementAt(i);
			if (!board.hasLivingNeighbor(cell)) {
				return false;
			}
		}
		return true;
	}

	/*
	 * Determine if the given vector of animals contains at least one animal
	 * that the farmer has determined may be a wolf.
	 */
	private boolean mayHaveWolf (Vector<MuttonCell> animals) {
		for (int i=0; i < animals.size(); i++) {
			MuttonCell n = animals.elementAt(i);
			if (animalCounts[n.getSheepId()] > 0) {
				return true;
			}
		}
		return false;
	}

	/*
	 * Use the history to compute the probability of each animal being a wolf
	 * and choose one from the set with the highest probability, as long as
	 * it exceeds a given threshold.
	 */
	private MuttonMoveSpec doV2FarmerShoot () {
		// Compute farmer knowledge.
		prepareFarmerInfo();

		if (totalPossibilities == 1) {
			// If there is only possible configuration of wolves, then we should
			// shoot all of the living ones left.
			String moveString = "Shoot";
			for (int i=0; i < NUM_ANIMALS; i++) {
				if (animalCounts[i] != 0) {
					moveString += " " + i;
				}
			}
			return new MuttonMoveSpec (moveString, board.whoseTurn);
		}

		if (board.getWolfWinTarget() == (board.getDeadSheepCount() + 1)) {
			// If the wolf only needs to eat one more sheep to win, then we might
			// as well shoot *someone*.  If we don't, then we'll lose on the wolf's
			// next turn anyway.  If we happen to guess correctly, then we may win.
			// So, set totalPossibilities to 1 so that whichever remaining animal count
			// is the highest, it will be guaranteed to be above the SHOOT_THRESHOLD
			totalPossibilities = 1;
		}

		if (board.getWolfHasPassed()) {
			// If the wolf has passed, then the farmer must shoot something.
			// Remove all possible animals that have neighbors, since they
			// can't be wolves (otherwise the wolf player would have not passed.)
			for (int i=0; i < NUM_ANIMALS; i++) {
				if ((animalCounts[i] != 0) && board.hasLivingNeighbor(i)) {
					animalCounts[i] = 0;
				}
			}

			// Set totalPossibilities to 1 so that whichever remaining animal count
			// is the highest, it will be guaranteed to be above the SHOOT_THRESHOLD
			totalPossibilities = 1;
		}

		// We need to treat the case where there is only 1 wolf to find differently
		// from the other cases.  This is because if there is only 1 wolf and there
		// are n possible animals that could be a wolf, then each animal will compute
		// out at a 1/n probability, which will be less than the threshold, and
		// the farmer will never shoot.  The wolf player, therefore, simply has to
		// keep two suspects next to each other for the rest of the game and can
		// pick off sheep until he wins.
		if (board.getWolfCount() == 1) {
			// Find the living animals and determine which one, if chosen to be
			// shot and turns out to be a sheep, fractures the remaining live
			// animals into the most distinguishing groups.
			replaceAnimalCountWithNeighborCount();

			// Set totalPossibilities to 1 so that whichever neighbor count
			// is the highest, it will be guaranteed to be above the SHOOT_THRESHOLD
			totalPossibilities = 1;
		}

		// Find the maximum value in animalCounts
		Vector<Integer> v = new Vector<Integer> ();
		int max = 1;
		for (int i=0; i < NUM_ANIMALS; i++) {
			if (animalCounts[i] == max) {
				v.addElement(Integer.valueOf(i));
			} else if (animalCounts[i] > max) {
				v.setSize(0);
				v.addElement(Integer.valueOf(i));
				max = animalCounts[i];
			}
		}

		if (bverbose) {
			System.out.println("  totalPossibilities = " + totalPossibilities);
			System.out.println("  max = " + max);
			System.out.println("  max / totalPossibilities = " + ((double) max / (double) totalPossibilities));
		}

		// Our shooting threshold drops as more sheep are killed.
		double shootThreshold = SHOOT_THRESHOLD_BASE - (SHOOT_THRESHOLD_DERATE * board.getDeadSheepCount());

		// If we're above our threshold, then shoot one of them.
		if (((double) max / (double) totalPossibilities) > shootThreshold) {
			int victim = v.elementAt(Random.nextInt(randGen, v.size())).intValue();
			return new MuttonMoveSpec ("Shoot " + victim, board.whoseTurn);
		} else {
			return new MuttonMoveSpec ("Shoot", board.whoseTurn);
		}
	}


	/*
	 * Helper function that computes farmer knowledge given the current state of the
	 * game.  This is used by the farmer player for both deciding which animal to
	 * shoot and how to relocate animals after shooting, if the shotgun variant
	 * is used.
	 */
	private void prepareFarmerInfo () {
		int [] currAnimalStatus = board.getSheepStatus();

		// Generate the list of valid wolf configs for the current history
		genValidWolfConfigs(board.getHistory(), currAnimalStatus);

		// Create the animalCounts array that holds the number of possible wolf
		// configs that are consistent with the current history and that include
		// the given animal as a wolf.
		enumeratePossibilities(currAnimalStatus);

		// Remove the known wolves (since they are already dead)
		for (int i=0; i < currAnimalStatus.length; i++) {
			if (currAnimalStatus[i] == SHEEP_STATUS_DEAD_WOLF) {
				animalCounts[i] = 0;
			}
		}
	}
	
	

	/*
	 * This method is used when there is only 1 wolf left and the farmer is shooting.
	 * In this case, if there are n animals that are potential wolves, then each will
	 * have a 1/n probability of being a wolf, and that probability won't change by
	 * wolf actions (assuming the wolf player isn't an idiot.)
	 * So, we want to sort the potential wolves not by their probability of being a
	 * wolf, but by the likelihood of dividing the group of potential wolves into
	 * mutually independent groups to make elimination of potential suspects more
	 * fruitful.
	 * This will replace the values of the animalCounts[] array with a measure of
	 * this so that the rest of the farmer shooting method that chooses the max
	 * value will now choose the max of this new function, rather than probability.
	 */
	private void replaceAnimalCountWithNeighborCount () {
		int [] isNeighborCount = new int [NUM_ANIMALS];
		Vector<MuttonCell> n = new Vector<MuttonCell>();
		Vector<MuttonCell> possibleWolves = new Vector<MuttonCell>();

		// Scan the board, looking for possible wolves.  For each one found,
		// increment the counter for it's neighbors.
		for (int c = 0; c < BOARD_COLS; c++) {
			for (int r = 0; r < BOARD_ROWS; r++) {
				MuttonCell cell = board.getCell(c,r);
				int id = cell.getSheepId();
				if ((id >= 0) && (animalCounts[id] != 0)) {
					// Add this to the possibleWolvesVector
					possibleWolves.addElement(cell);

					// Get live neighbors from the board.
					board.getAliveNeighbors(cell, n);

					// Increment the neighbor count for each one.
					for (int i=0; i < n.size(); i++) {
						isNeighborCount[n.elementAt(i).getSheepId()] += 1;
					}
				}
			}
		}

		// Clear the animalCounts
		for (int i=0; i < animalCounts.length; i++) {
			animalCounts[i] = 0;
		}

		// Go through the possible wolves and set the animal counts to the maximum
		// count of all of it's neighbors.
		for (int i=0; i < possibleWolves.size(); i++) {
			MuttonCell wolf = possibleWolves.elementAt(i);
			int wolfId = wolf.getSheepId();

			board.getAliveNeighbors(wolf, n);
			for (int j=0; j < n.size(); j++) {
				int neighborId = n.elementAt(j).getSheepId();
				animalCounts[wolfId] = Math.max(animalCounts[wolfId], isNeighborCount[neighborId]);
			}
		}
	}

	/*
	 * For all of the valid wolf configurations, count the number of times each
	 * animal shows up as a wolf.
	 * 
	 * @param animalStatus  An array of the status of all of the animals.
	 */
	private void enumeratePossibilities (int [] animalStatus) {
		// Clear out the animal counts;
		animalCounts = new int [NUM_ANIMALS];
		totalPossibilities = validWolfConfigs.size();

		// Go through all valid wolf configs, incrementing animalCounts
		for (int c = 0; c < validWolfConfigs.size(); c++) {
			int [] currConfig = validWolfConfigs.elementAt(c);
			// Increment the animalCounts for the current config
			for (int i=0; i<currConfig.length; i++) {
				animalCounts[currConfig[i]] += 1;
			}
		};
	}

	/*
	 * Update the animalCounts array a totalPossibilities by taking a new dead
	 * animal into account.
	 *
	 * @param newDeadAnimal    The id of the new dead animal
	 * @param suspects         The list of suspect animals for the new dead animal.
	 * @param newDeadisWolf    Indicates if the new dead animal is a wolf or sheep.
	 */
	private void filterPossibilitiesWithAdditionalDeadAnimal (int newDeadAnimal, int [] suspects, boolean newDeadIsWolf) {
		for (int i=0; i<animalCounts.length; i++) {
			animalCounts[i] = 0;
		}
		totalPossibilities = 0;

		// Go through all valid wolf configs, incrementing animalCounts
		for (int c = 0; c < validWolfConfigs.size(); c++) {
			int [] currConfig = validWolfConfigs.elementAt(c);
			// In order to remain valid, the config must:
			//      - Correctly support or deny that the new dead animal is/is not a wolf
			//  and - Suspects must include at least one wolf
			if ((arrayContains(newDeadAnimal, currConfig) == newDeadIsWolf) &&
				containsAtLeastOne(currConfig, suspects)) {
				// Increment the animalCounts for the current config
				for (int i=0; i<currConfig.length; i++) {
					animalCounts[currConfig[i]] += 1;
				}
				totalPossibilities += 1;
			}
		}
	}

	/*
	 * Count the number of valid wolf configs that would result by taking a new
	 * dead animal into account.
	 * This does *not* update the animalCounts array or totalPossibilities.
	 *
	 * @param newDeadAnimal    The id of the new dead animal
	 * @param suspects         The list of suspect animals for the new dead animal.
	 * @return the number of possibilities that remain consistent with the new info.
	 */
	private int countPossibilitiesWithAdditionalDeadAnimal (int newDeadAnimal, int [] suspects) {
		int remainingPossibilities = 0;

		// Go through all valid wolf configs, counting valid configs
		for (int c = 0; c < validWolfConfigs.size(); c++) {
			int [] currConfig = validWolfConfigs.elementAt(c);
			// In order to remain valid, the config must:
			//      - Suspects must include at least one wolf
			if (containsAtLeastOne(currConfig, suspects)) {
				remainingPossibilities += 1;
			}
		}

		return remainingPossibilities;
	}
	/*
	 * Generate all valid wolf configs and put them into the vector validWolfConfigs
	 */
	private void genValidWolfConfigs (Vector<MuttonHistoryElement> history, int [] animalStatus) {
		// Populate the knownWolves array with known wolves.
		int [] knownWolves = new int [NUM_WOLVES];
		for (int i=0; i < knownWolves.length; i++) {
			knownWolves[i] = -1;
		}
		int idx = 0;
		for (int i=0; i < NUM_ANIMALS; i++) {
			if (animalStatus[i] == SHEEP_STATUS_DEAD_WOLF) {
				knownWolves[idx] = i;
				idx += 1;
			}
		}

		// Create a new combination generator to generate all possible
		// combinations of wolves.
		CombinationGenerator gen = new CombinationGenerator (NUM_ANIMALS, NUM_WOLVES);

		// Go through all wolf configs, checking to see if it is consistent with
		// knowledge.
		validWolfConfigs.setSize(0);
		while (gen.hasMore()) {
			int [] currConfig = gen.next();
			if (configConsistentWithHistory(currConfig, history, animalStatus, knownWolves)) {
				validWolfConfigs.addElement(AR.copy(currConfig));
			}
		};
	}

	/*
	 * Determine if the given configuration of wolves is consistent with our
	 * knowledge of history.
	 *
	 * @param currConfig   An array of suspected wolves.
	 * @param history      A vector of history elements indicating what has occurred.
	 * @param sheepStatus  An array of the status of all of the animals.
	 * @param knownWolves  An array of known (dead) wolves.
	 * @return true  => The given config is consistent with history
	 *         false => The given config is not consistent with history
	 */
	private boolean configConsistentWithHistory (int [] currConfig, Vector<MuttonHistoryElement> history, int [] animalStatus, int [] knownWolves) {
		// If a potential wolf is known to be a dead sheep, then this config
		// is not valid
		for (int i = 0; i < currConfig.length; i++) {
			if (animalStatus[currConfig[i]] == SHEEP_STATUS_DEAD_SHEEP) {
				return false;
			}
		}

		// If there are any dead wolves, and they aren't included in the potential
		// wolves, then this config is not valid.
		for (int i = 0; (i < knownWolves.length) && (knownWolves[i] >= 0); i++) {
			if (!arrayContains(knownWolves[i], currConfig)) {
				return false;
			}
		}

		// Check each line of history
		for (int i = 0; i < history.size(); i++) {
			MuttonHistoryElement el = history.elementAt(i);
			// At least one of the potential wolves must be in the list of suspects
			if (!containsAtLeastOne(currConfig, el.getSuspectSheep())) {
				return false;
			}
		}

		// This config has passed all tests, so it's clean.
		return true;
	}

	/*
	 * Determine if there is at least one element that is in both the sourceList
	 * and the targetList.
	 * 
	 * Because sourceList & targetList are small (sourceList is known to be
	 * NUM_WOLVES in size, and targetList is known to be 1..6 in size), this
	 * exhaustive comparison isn't too bad.  If they get to be larger in length,
	 * there are better algorithms for determining if there is overlap between
	 * the lists.  (Also, sourceList is known to be sorted from low to high, so
	 * that could help also.)
	*/
	private boolean containsAtLeastOne (int [] sourceList, int [] targetList) {
		for (int s=0; s<sourceList.length; s++) {
			for (int t=0; t<targetList.length; t++) {
				if (sourceList[s] == targetList[t]) {
					return true;
				}
			}
		}
		return false;
	}

	/*
	 * Determine if the given number is an entry in the given array.
	 */
	private boolean arrayContains(int tgt, int [] a) {
		for (int i = 0; i < a.length; i++) {
			if (a[i] == tgt) {
				return true;
			}
		}
		return false;
	}

	
	
	
	
	
	
	
/***************************************************************************
    These methods are needed for commonRobot, but aren't actually used by
    the Mutton robot.
*/
	
	public boolean Depth_Limit (int current, int max) {
		if (bverbose) System.out.println("V2_Robot Entering: Depth_Limit()");
	    //  Auto-generated method stub
		return true;
	}



    public CommonMoveStack  List_Of_Legal_Moves() {
		if (bverbose) System.out.println("V2_Robot Entering: List_Of_Legal_Moves()");
	    //  Auto-generated method stub
	    return null;
    }

    public void Make_Move(commonMove arg0) {
		if (bverbose) System.out.println("V2_Robot Entering: Make_Move()");
	    //  Auto-generated method stub
	    
    }

    public void StaticEval() {
		if (bverbose) System.out.println("V2_Robot Entering: StaticEval()");
	    //  Auto-generated method stub
	    
    }

    public double Static_Evaluate_Position(commonMove arg0) {
		if (bverbose) System.out.println("V2_Robot Entering: Static_Evaluate_Position()");
	    //  Auto-generated method stub
	    return 0;
    }

    public void Unmake_Move(commonMove arg0) {
		if (bverbose) System.out.println("V2_Robot Entering: Unmake_Move()");
	    //  Auto-generated method stub
	    
    }

	public boolean RobotPlayerEvent(int x, int y, Rectangle startrect) {
		if (bverbose) System.out.println("V2_Robot Entering: RobotPlayerEvent()");
		return false;
	}
	// this is the monte carlo robot, which for some games is much better then the alpha-beta robot
	// for the monte carlo bot, blazing speed of playouts is all that matters, as there is no
	// evaluator other than winning a game.
	public commonMove DoMonteCarloFullMove()
	{	commonMove move = null;
		try {
	       {
	       // it's important that the robot randomize the first few moves a little bit.
	       double randomn = 0.0;
	       UCTMoveSearcher monte_search_state = new UCTMoveSearcher(this);
	       monte_search_state.save_top_digest = true;	// always on as a background check
	       monte_search_state.save_digest=false;	// debugging only
	       monte_search_state.win_randomization = randomn;		// a little bit of jitter because the values tend to be very close
	       monte_search_state.timePerMove = 20;		// 10 seconds per move
	       monte_search_state.verbose = verbose;
	       monte_search_state.alpha = 0.5;
	       monte_search_state.final_depth = 100;			// tree tends to eb very deep, so restrain it.
	       monte_search_state.simulationsPerNode = 10;
	       monte_search_state.terminalNodeOptimization = false;
	       move = monte_search_state.getBestMonteMove();
	       }
			}
	     finally { ; }
	     if(move==null) { continuous = false; }
	    return(move);
	}
	/**
	 * for UCT search, return the normalized value of the game, with a penalty
	 * for longer games so we try to win in as few moves as possible.  Values
	 * must be normalized to -1.0 to 1.0
	 */
	 public double NormalizedScore(commonMove lastMove)
	 {	
	 	//boolean win = localBoard.WinForPlayerNow(player);
		//if(win) { return(0.8+0.2/boardSearchLevel); }
		//boolean win2 = board.WinForPlayerNow(nextPlayer[player]);
		//if(win2) { return(- (0.8+0.2/boardSearchLevel)); }
		return(0);
	}


}
