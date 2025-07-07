package online.search;

import lib.OStack;
import online.game.BoardProtocol;

class BoardStack extends OStack<BoardProtocol>
{
	public BoardProtocol[] newComponentArray(int sz) {
		return new BoardProtocol[sz];
	}
	
}