package online.search;

import common.GameInfo;
import lib.G;
import online.game.BoardProtocol;
import online.game.commonMPMove;
import online.game.commonMove;

public abstract class commonMPRobot<BOARDTYPE extends BoardProtocol> extends commonRobot<BOARDTYPE>
{
	public abstract double valueOfWin();
	
    /**
     * this is intended to catch class configuration errors for new games being developed.
     */
   public void validateClasses()
    {
    	GameInfo go = sharedInfo.getGameInfo();
    	if(go!=null)
    	{
    		G.Assert(viewer.ParseNewMove("done",0) instanceof commonMPMove,"should be commonMPMove");
    	}
    }
	 //
	 // rescore the position for a different player.  The underlying
	 // assertion here is that the player component scores are accurate
	 // ie; the players don't score themselves differently if they are
	 // the player to move. 
	 //
	 public double reScorePosition(commonMPMove m,int forplayer)
	 {	return(m.reScorePosition(forplayer,valueOfWin()));
	 }
	 public double reScoreMPPosition(commonMPMove m,int forplayer)
	 {
		 return m.reScoreMPPosition(forplayer);
	 }
	 public commonMove getCurrentVariation()
	 {
		 return search_driver.getCurrentVariation();
	 }

}
