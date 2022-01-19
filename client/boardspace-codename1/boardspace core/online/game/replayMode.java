package online.game;

/**
 * replayMode serves as advice to control saving animation information or not
 * 
 * @author ddyer
 *
 */
public enum replayMode
{	/** live indicates the user is manipulating the UI directly, but the robot is using accelerated move specifiers, 
 	so normally the usert's moves are not animated, but the robot's move are. */
	Live,
	/** Replay means a high speed replay or a robot move search is in progress, so no animations should be done */
	Replay, 
   	/** Single means a single step replay is in progress, so both the user and robot's moves should be animated */
	Single
}