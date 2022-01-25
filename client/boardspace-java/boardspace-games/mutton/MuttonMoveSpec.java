package mutton;

import java.util.*;

import lib.G;
import online.game.*;
import lib.ExtendedHashtable;
public class MuttonMoveSpec extends commonMove implements MuttonConstants
{
	static ExtendedHashtable D = new ExtendedHashtable(true);

	static {
		// these int values must be unique in the dictionary
		addStandardMoves(D);
		D.putInt("Wolves_Hidden", MOVE_FINISHED_HIDING);
		D.putInt("Eat", MOVE_EAT);
		D.putInt("Shoot", MOVE_SHOOT);
		D.putInt("Relocate", MOVE_RELOCATE);
		D.putInt("Done_Relocating", MOVE_DONE_RELOCATING);
		D.putInt("Board_State", MOVE_BOARD_STATE);
		D.putInt("Pickup", MOVE_PICKUP);
		D.putInt("Drop", MOVE_DROP);
	}

	// The list of sheep that are targeted.
	// This is used for the Eat, Shoot & Hidden moves.
	// For the relocate move, this is the sheep that is moving.
	public int [] sheepIds = new int [0];

	// The destination point that the sheep is moving to.
	// This is used for relocation moves
	public int [] destination = new int [0];

	// The string that indicates the locations of all the sheep on the board.
	// This is used for the MOVE_BOARD_STATE move during the initial setup
	// where the farmer player can reposition the sheep anyway he wants before
	// the wolf player decides which are the wolves.
	public String boardStateString;

	// Indication of whether or not this move is concealed.
	// Currently, this is only used to determine if the MOVE_FINISHED_HIDING
	// short form should be: "Hide" (if the game is in progress) or
	// "Hide : a b c d" if the game is over.
	public boolean concealed = true;

	// In the case that the shotgun variant is being used, then we want
	// to display the shoot move differently in the history panel.
	// Without the shotgun, it should look like "Shoot X".
	// With the shotgun, it should look like "Shoot X - a b c", where
	// a, b, c are the scared sheep that are moved by the farmer on
	// later turns.  This determines if the shoot command should have
	// the - appended to the short version of the move.
	public boolean moveToFollow = false;

	/**
	 * Default constructor for an empty move spec.
	 */
	public MuttonMoveSpec () {
	}

	/**
	 * Constructor for a move spec given a string and player.
	 * @param str   The string of the move
	 * @param p     The player making the move.
	 */
	public MuttonMoveSpec (String str, int p) {
		parse(new StringTokenizer(str), p);
	}

	/**
	 * Constructor for a move spec given a string tokenizer and player.
	 * @param ss    The tokenizer that given the move string.
	 * @param p     The player making the move.
	 */
	public MuttonMoveSpec (StringTokenizer ss, int p) {
		parse(ss, p);
	}

	/**
	 * Determine if the given move is the same as this one.
	 */
	public boolean Same_Move_P (commonMove oth) {
		MuttonMoveSpec other = (MuttonMoveSpec) oth;

		return (   (op == other.op)
		        && (player == other.player)
		        && intArrayEquals(sheepIds, other.sheepIds)
		        && intArrayEquals(destination, other.destination)
		        && stringEquals(boardStateString, other.boardStateString));
	}

	/**
	 * Copy this move into the given moveSpec.
	 * @param to   The moveSpec to make a copy of this one.
	 */
	public void Copy_Slots (MuttonMoveSpec to) {
		super.Copy_Slots(to);

		to.sheepIds = sheepIds;
		to.destination = destination;
		to.concealed = concealed;
		to.boardStateString = boardStateString;
	}

	/**
	 * Copy this move into the given move.
	 * @param to    The move to make a copy of this one.
	 *              If null, then a new move is created.
	 * @return the copied move.
	 */
	public commonMove Copy (commonMove to) {
		MuttonMoveSpec moveTo = (to == null) ? new MuttonMoveSpec() : (MuttonMoveSpec) to;

		// we need moveTo to be a MuttonMoveSpec at compile time so it will trigger call to the 
		// local version of Copy_Slots
		Copy_Slots(moveTo);

		return moveTo;
	}

	/* Parse a string into the state of this move.
	 * Remember that we're just parsing, we can't refer to the state of the
	 * board or the game.
	 **/
	private void parse (StringTokenizer msg, int p) {
		String cmd = msg.nextToken();
		player = p;

		if (Character.isDigit(cmd.charAt(0)))
		{ // if the move starts with a digit, assume it is a sequence number
			setIndex(G.IntToken(cmd));
			cmd = msg.nextToken();
		}

		op = D.getInt(cmd, MOVE_UNKNOWN);

		switch (op) {
			default:
				throw G.Error("Can't parse %s", cmd);
			case MOVE_EDIT:
			case MOVE_DONE:
				break;
			case MOVE_FINISHED_HIDING :
			case MOVE_EAT :
			case MOVE_SHOOT :
			case MOVE_PICKUP :
				int len = msg.countTokens();
				sheepIds = new int [len];
				for (int i=0; i<len; i++) {
					sheepIds[i] = G.IntToken(msg);
				}
				break;

			case MOVE_RELOCATE :
				if (msg.countTokens() == 3) {
					sheepIds = new int [1];
					destination = new int [2];
					sheepIds[0] = G.IntToken(msg);
					destination[0] = G.IntToken(msg);
					destination[1] = G.IntToken(msg);
				}
				break;

			case MOVE_BOARD_STATE :
				if (msg.countTokens() > 0) {
					boardStateString = msg.nextToken();
				}
				break;

			case MOVE_DONE_RELOCATING :
			case MOVE_RESIGN :
			case MOVE_SWAP :
			case MOVE_DROP :
				break;

			case MOVE_START :
				player = D.getInt(msg.nextToken());
				break;
		}
	}

	/* Construct a move string for this move.
	 * These are the inverse of what are accepted by the constructors, and are
	 * also human readable.
	 * 
	 * These are used to display in the "move history" panel.
	 **/
	public String shortMoveString () {
		switch (op) {
			case MOVE_FINISHED_HIDING :
				if (concealed) {
					return "Hide";
				} else {
					return ("Hide " + intArrayToCharString(sheepIds));
				}

			case MOVE_EAT :
				if (sheepIds.length == 0) {
					return "Pass";
				} else {
					return ("Eat " + (char) (sheepIds[0] + 'A') + " -");
				}

			case MOVE_SHOOT :
				if (sheepIds.length == 0) {
					return "Pass";
				} else {
					String trailer = (moveToFollow ? " -" : "");
					return ("Shoot" + intArrayToCharString(sheepIds) + trailer);
				}

			case MOVE_RELOCATE :
				return (" " + (char) (sheepIds[0] + 'A'));

			case MOVE_BOARD_STATE :
				return ("Board Setup");

			case MOVE_SWAP :
				return (SWAP);

			case MOVE_DONE_RELOCATING :
			case MOVE_DONE:
			case MOVE_PICKUP:
			case MOVE_EDIT:
			case MOVE_DROP:
				return ("");

			default:
				return (D.findUnique(op));

		}
	}

	/* Construct a move string for this move.
	 * These are the inverse of what are accepted by the constructors, and are
	 * also human readable.
	 * 
	 * These are used to recreate the message that was parsed.
	 **/
	public String moveString () {
		String indx = indexString();
		String opname = indx+D.findUnique(op)+" ";

		// adding the move index as a prefix provides numbers
		// for the game record and also helps navigate in joint
		// review mode.
		switch (op) {
			case MOVE_FINISHED_HIDING :
				return (opname+ intArrayToString(sheepIds));

			case MOVE_EAT :
				if (sheepIds.length > 0) {
					return (opname + sheepIds[0]);
				} else {
					return (opname);
				}

			case MOVE_SHOOT :
				return (opname + intArrayToString(sheepIds));

			case MOVE_PICKUP :
				return (opname + sheepIds[0]);

			case MOVE_RELOCATE :
				return (opname + sheepIds[0] + " " + destination[0] + " " + destination[1]);


			case MOVE_BOARD_STATE :
				return (opname + boardStateString);


			case MOVE_START:
				return (indx + "Start P" + player);

			case MOVE_DONE_RELOCATING :
			case MOVE_SWAP :
			default:
				return (opname);
		}
	}


	/*
	 * Determine if two arrays of integers are equal.
	 */
	private boolean intArrayEquals (int [] a, int [] b) {
		if (a.length != b.length) {
			return false;
		}

		for (int i=0; i<a.length; i++) {
			if (a[i] != b[i]) {
				return false;
			}
		}
		return true;
	}

	/*
	 * Convert an array of integers into a space separated string.
	 * The resulting string will have a leading space.
	 * ie: The array {1, 2, 3} will result in "1 2 3 "
	 */
	private String intArrayToString (int [] a) {
		String result = "";

		for (int i=0; i < a.length; i++) {
			result += (a[i]+" ");
		}

		return result;
	}

	/*
	 * Convert an array of integers into a space separated string
	 * that is made up of alphabetic characters that correspond to the numbers
	 * in the array.
	 * The resulting string will have a leading space.
	 * ie: The array {1, 2, 12} will result in " B C M".
	 */
	private String intArrayToCharString (int [] a) {
		String s = "";
		for (int i=0; i<a.length; i++) {
			s += (" " + (char) (a[i] +  'A'));
		}
		return s;
	}

	/*
	 * Determine if the two given strings are equal.  This works even if
	 * one or both are null.
	 */
	private boolean stringEquals (String a, String b) {
		if (a == null) {
			return (b == null);
		}
		return a.equals(b);
	}
}
