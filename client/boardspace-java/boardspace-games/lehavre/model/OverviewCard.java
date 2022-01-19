package lehavre.model;

/**
 *
 *	The <code>OverviewCard</code> class represents the overview card.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.10 2010/11/26
 */
public final class OverviewCard
{
	/** The type of game. */
	private final GameType gameType;

	/** The number of players. */
	private final int playerCount;

	/**
	 *	Creates a new <code>OverviewCard</code> instance
	 *	for the given type of game and number of players.
	 *	@param type the type of game
	 *	@param count the number of players
	 */
	public OverviewCard(GameType type, int count) {
		gameType = type;
		playerCount = count;
	}

	/**
	 *	Returns the type of game.
	 *	@return the type of game
	 */
	public GameType getGameType() {
		return gameType;
	}

	/**
	 *	Returns the number of players.
	 *	@return the number of players
	 */
	public int getPlayerCount() {
		return playerCount;
	}
}