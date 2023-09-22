/* copyright notice */package mutton;

import bridge.Config;
import lib.CellId;

import online.game.BaseBoard.BoardState;


public interface MuttonConstants 
{	
	static final String PlayFarmerMessage = "Play Farmer";
	
	static String MuttonStrings[] =
	{	"Mutton",
		"Farmer configuring board",
		PlayFarmerMessage,
		"Hide",
		"Wolf player hiding",
		"Wolf moving suspects",
		"Wolf player eating",
		"Farmer select",
		"Shoot",
		"Eat",
		"Farmer moving scared",
		
	};
	static String MuttonStringPairs[][] = 
	{
		{"Mutton_family","Mutton"},
		{"Mutton-shotgun","shotgun Mutton"},
        {"Mutton_variation","standard Mutton"},
        {"Mutton-shotgun_variation","+ shotgun"},
	};


    
	static final String HIDDEN = "H";
	enum MuttonId implements CellId
	{
	// They represent unique places for hitCode.
	HIT_BOARD_LOCATION,
	HIT_EMPTY_BOARD_LOCATION,
	HIT_HISTORY_COLUMN,
	HIT_WOLF_UP_ARROW,
	HIT_WOLF_DOWN_ARROW,
	HIT_FARMER_FACE,
	MuttonDoneButton,
	;
		public String shortName() { return(name()); }
	
	}
	// other things you can point at.  Negative numbers are fixed objects such as buttons
	// positive numbers are movable objects you pick up and drag.  There are also some
	// shared object such as HitNoWhere
	// -- None for Mutton --

	// init strings for variations of the game.
	static final String Mutton_INIT = "mutton"; // Init for standard game
	static final String Mutton_SHOTGUN_INIT = "mutton-shotgun"; // Init for shotgun game

    public enum MuttonState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	WOLF_HIDING_STATE("Wolf player hiding"),
    	WOLF_CHOOSING_MEAL_STATE("Wolf player eating"),
    	WOLF_MOVING_SHEEP_STATE("Wolf moving suspects"),
    	FARMER_CHOOSING_TARGETS_STATE("Farmer select"),
    	FARMER_MOVING_SHEEP_STATE("Farmer moving scared"),
    	WOLF_HAS_NO_VALID_MEAL_PSEUDO_STATE("Wolf no valid meals"),
    	FARMER_CONFIGURING_BOARD("Farmer configuring board")   	;
    	String description;
    	MuttonState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }



	// Move commands, actions encoded by movespecs.  Values chosen so these
	// integers won't look quite like all the other integers
	// These next must be unique integers in the MuttonMoveSpec dictionary.
	static final int MOVE_FINISHED_HIDING = 211;
	static final int MOVE_EAT = 212;
	static final int MOVE_SHOOT = 213;
	static final int MOVE_RELOCATE = 214;
	static final int MOVE_DONE_RELOCATING = 215;
	static final int MOVE_BOARD_STATE = 216;
	static final int MOVE_PICKUP = 218;
	static final int MOVE_DROP = 219;

	static final String Mutton_SGF = "mutton"; // sgf game id allocated for Mutton

	// The size of the game board.
	public static int BOARD_COLS = 7;
	public static int BOARD_ROWS = 7;

	// Values within the MuttonCell objects that indicate if a space is either
	// empty or doesn't exist on the board.
	static final int CELL_NOT_EXIST = -2;
	static final int CELL_EMPTY = -1;

	// The number of dead sheep needed for the wolf to win the game.
	static final int DEFAULT_DEAD_SHEEP_NEEDED_FOR_WOLF_WIN = 11;

	// Sheep status for the history panel
	static final int SHEEP_STATUS_ALIVE = 0;
	static final int SHEEP_STATUS_DEAD_SHEEP = 1;
	static final int SHEEP_STATUS_DEAD_WOLF = 2;

	// The types of highlighting that can be used when drawing a cell on the
	// board.
	static final int PRE_ANIMAL_HIGHLIGHT = 0;
	static final int POST_ANIMAL_HIGHLIGHT = 1;
	static final int HIGHLIGHT_NONE = -1;
	static final int HIGHLIGHT_AS_TARGETED = 0;
	static final int HIGHLIGHT_AS_LAST_VICTIM = 1;
	static final int HIGHLIGHT_AS_SUSPECT = 2;
	static final int HIGHLIGHT_AS_SCARED = 3;
	static final int HIGHLIGHT_STAR = 4;

	
	// File names for the images
	static final String ImageDir = "/mutton/images/";
	static final String [] ImageNames = {
		"alphabet_black.png",
		"alphabet_red.png",
		"sheep_array_6.png",
		"wolf_array_6.png",
		"board_tile.png",
		"history_dead_header_icons.png",
		"history_dead_icon.png",
		"history_suspect_icon.png",
		"board_tile_highlight.png",
		"highlights.png",
		"blood_highlight.png",
		"numbers.png",
		"up_down_arrows.png",
		"farmer_faces.png"
	};

	// Image Id's
	// Sheep & wolf images drawn by Reiner "Tiles" Prokein and were
	// downloaded from his website at http://reinerstileset.4players.de
	// They are specifically licensed as freeware for others to use.
	//
	// Yellow star where farmer chooses target score drawn by ensarija
	//   and is available from http://openclipart.org/ and is in the
	//   public domain.
	static final int IMG_ALPHABET_BLACK = 0;
	static final int IMG_ALPHABET_RED = 1;
	static final int IMG_SHEEP = 2;
	static final int IMG_WOLVES = 3;
	static final int IMG_BOARD_TILE = 4;
	static final int IMG_HISTORY_DEAD_HEADER = 5;
	static final int IMG_HISTORY_DEAD_SHEEP = 6;
	static final int IMG_HISTORY_SUSPECT_SHEEP = 7;
	static final int IMG_ACTIVE_HIGHLIGHT = 8;
	static final int IMG_CELL_HIGHLIGHTS = 9;
	static final int IMG_BLOOD_HIGHLIGHT = 10;
	static final int IMG_NUMBERS = 11;
	static final int IMG_ARROWS = 12;
	static final int IMG_FARMER_FACES = 13;

	// Background tile images
	static final int BACKGROUND_TILE_INDEX = 0;
	static final int BACKGROUND_REVIEW_INDEX = 1;
	static final int ICON_INDEX = 2;
	static final String TextureNames[] = 
		{ "background-tile" ,"background-review-tile",
			"mutton-icon-nomask"};

	// Sound file names
	// These sounds are created from sound files downloaded from the free sound
	// project at http://www.freesound.org
	// Sheep bleat by Erdie (Sheep.flac)
	// Wolf howl by Robinhood76 (00829 wolf howl one shot.wav)
	// Eating sound by gabemiller74 (WereWolfM2.aif)
	// Gun shot by fastson (RemingtonGunshot.wav)
	// Pickup sound by FreqMan (garbage bag (3).wav)
	static final String SND_PICKUP      = "mutton/sounds/Pickup" + Config.SoundFormat;
	static final String SND_EAT_SHEEP   = "mutton/sounds/EatSheep" + Config.SoundFormat;
	static final String SND_EAT_WOLF    = "mutton/sounds/EatWolf" + Config.SoundFormat;
	static final String SND_SHOOT_SHEEP = "mutton/sounds/ShootSheep" + Config.SoundFormat;
	static final String SND_SHOOT_WOLF  = "mutton/sounds/ShootWolf" + Config.SoundFormat;
	static final String Sounds[] = 
		{SND_PICKUP,SND_EAT_SHEEP,SND_EAT_WOLF,SND_SHOOT_SHEEP,SND_SHOOT_WOLF
		};
}