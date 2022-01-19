package online.game;

/**
 * this is a class used to insert messages into the raw move history
 * of a game.
 * @author ddyer
 *
 */
public class dummyMove extends commonMove {
	public String message;
	public dummyMove(String str)
	{	message = str;
	}
	public commonMove Copy(commonMove to) {
		if(to==null) { to=new dummyMove(""); }
		dummyMove meto = (dummyMove)to;
		meto.message = message;
		return(to);
	}

	public boolean Same_Move_P(commonMove other) {
		//  Auto-generated method stub
		dummyMove me = (dummyMove)other;
		return((message==null) ? (me.message==null) : message.equals(me.message));
	}

	public String moveString() {
		//  Auto-generated method stub
		return(message);
	}

	public String shortMoveString() {
		//  Auto-generated method stub
		return(message);
	}

}
