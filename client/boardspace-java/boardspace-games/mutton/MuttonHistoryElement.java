package mutton;

public class MuttonHistoryElement {

	// The id number of the sheep that was killed this turn.
	private int killedSheepId;

	// Indicates if the sheep killed this turn was a wolf or not.
	private boolean wasWolf;

	// The id numbers of the neighboring sheep that are now suspects.
	private int [] suspectSheepIds = new int [6];

	/**
	 * Constructor for a new history element
	 *
	 * @param lkilledSheepId    The id number of the sheep that was eaten this turn.
	 * @param lwasWolf          Indicates if the sheep killed this turn was a wolf or not.
	 * @param lsuspectSheepIds   The id numbers of the neighboring sheep that are now suspects.
	 */
	public MuttonHistoryElement (int lkilledSheepId, boolean lwasWolf, int [] lsuspectSheepIds) {
		killedSheepId = lkilledSheepId;
		wasWolf = lwasWolf;
		suspectSheepIds = lsuspectSheepIds;
	}

	/**
	 * Return the id of the sheep killed this turn.
	 */
	public int getKilledSheepId () { return killedSheepId; }

	/**
	 * Return whether the sheep killed this turn was a wolf or not.
	 */
	public boolean killedSheepWasWolf () { return wasWolf; }

	/**
	 * Return an array of suspect sheep.
	 */
	public int [] getSuspectSheep () { return suspectSheepIds; }

	/**
	 * Return a digest value for this history element.
	 * See the method digest() in MuttonGameBoard for a description of what the
	 * digest is used for.
	 * @return a digest value that represents this history element.
	 */
	public int getDigestValue () {
		// See the value with the constants.
		int val = killedSheepId * 17 + (wasWolf ? 1 : 0);

		// Modify the value based on the suspect sheep id's.
		for (int i=0; i<suspectSheepIds.length; i++) {
			val = (val * 17) + suspectSheepIds[i];
		}

		return val;
	}

	/**
	 * Determine if the given history element is the same as this one.
	 */
	public boolean sameHistory (MuttonHistoryElement otherHistory) {
		if (killedSheepId != otherHistory.killedSheepId)
			return false;

		if (wasWolf != otherHistory.wasWolf)
			return false;

		if (suspectSheepIds.length != otherHistory.suspectSheepIds.length)
			return false;

		for (int i=0; i<suspectSheepIds.length; i++) {
			if (suspectSheepIds[i] != otherHistory.suspectSheepIds[i])
				return false;
		}

		return true;
	}
}
