/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.
    
    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/. 
 */
package takojudo;

import lib.CellId;

import online.game.BaseBoard.BoardState;

public interface TakojudoConstants 
{	static String VictoryCondition = "Immobilize your opponent";
	static String TakojudoPlayState = "Move one of your pieces";
	static String EndGameAsLoss = "Click on Done to end the game as a Loss";
	
    static String TakojudoStrings[]=
    {
       "Tako Judo",
       EndGameAsLoss,
       VictoryCondition,
       TakojudoPlayState,   	
    };
    static String TakojudoStringPairs[][]=
    {
    	{"TakoJudo_family","Tako Judo"},
    	{"TakoJudo","Tako Judo"},
    	{"TakoJudo_variation","standard Tako Judo"},
    	
    };

    
	static final int DEFAULT_COLUMNS = 8;	// 8x6 board
	static final int DEFAULT_ROWS = 8;
	static final String Tacojudo_INIT = "takojudo";	//init for standard game

    enum TacoId implements CellId
    {
    	BoardLocation,
    	ReverseViewButton,
    	DeclineDraw,
        OfferDraw,
        AcceptDraw, ToggleEye

    	;

   }
    public enum TakojudoState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	DRAW_STATE(DrawStateDescription),	// game is a draw, click to confirm
    	PLAY_STATE(TakojudoPlayState), 	// place a marker on the board 
    	OFFER_DRAW_STATE(OfferDrawStateDescription),
    	QUERY_DRAW_STATE(OfferedDrawStateDescription),
    	ACCEPT_DRAW_STATE(AcceptDrawStateDescription),
    	DECLINE_DRAW_STATE(DeclineDrawStateDescription),
    	RESIGN_DRAW_STATE(EndGameAsLoss);
    	String description;
    	TakojudoState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }
	
    static final String Takojudo_SGF = "Tacojudo"; // sgf game name
    static final String[] TACOJUDOGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

 
    // file names for jpeg images and masks
    static final String ImageDir = "/takojudo/images/";
	// sounds
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int BOARD_NP_INDEX = 2;
    static final int BOARD_INDEX = 0;
    static final int LOGO_INDEX = 1;
    static final String ImageNames[] = {  "board","logo","board-np" };
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	 };

}