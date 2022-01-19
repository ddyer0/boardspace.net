package online.game.sgf;

import lib.OStack;

public class sgf_gamestack extends OStack<sgf_game> {
	public sgf_game[] newComponentArray(int n) { return(new sgf_game[n]); }
}
