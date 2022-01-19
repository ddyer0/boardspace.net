package yspahan;

public class YspahanPlayStratFactory 
{
	public static IYspahanPlayStrat getStrategy(int level, int player, int numPlayer, long randomKey)
	{
		IYspahanPlayStrat myStrat = null;
		switch (level)
		{
		case 0:
			myStrat = new YspahanPlayStratDumbot(player, numPlayer,randomKey+numPlayer+player );
			break;

		case 1:
			myStrat = new YspahanPlayStrat8(player, numPlayer,randomKey+numPlayer+player);			
			break;

		case 2:
			myStrat = new YspahanPlayStrat12(player, numPlayer,randomKey+numPlayer+player);			
			break;

		default:
			myStrat = new YspahanPlayStratDumbot(player, numPlayer,randomKey+numPlayer+player);
			break;
		}
		//if (myDebug)
		//{
		//	System.out.println(myStrat);
		//}
		return myStrat;
	}

}
