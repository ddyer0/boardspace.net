package online.game;

/*
 * a collection of constants appropriate for games with more than 2 players. Usually
 * a multiplayer game's "constants" interface will extend this class.
 * 
 */
public interface Play6Constants extends PlayConstants
{
    // indexes into the balls array, usually called the rack
	static final int THIRD_PLAYER_INDEX = 2;
	static final int FOURTH_PLAYER_INDEX = 3;
	static final int FIFTH_PLAYER_INDEX = 4;
	static final int SIXTH_PLAYER_INDEX = 5;
	static final int MAX_PLAYER_INDEX = 5;
	static final String P0 = "P0";
	static final String P1 = "P1";
	static final String P2 = "P2";
	static final String P3 = "P3";
	static final String P4 = "P4";
	static final String P5 = "P5";
}
