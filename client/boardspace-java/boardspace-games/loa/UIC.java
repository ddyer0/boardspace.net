/* copyright notice */package loa;

import java.awt.Color;

import lib.CellId;
import online.game.BaseBoard.BoardState;
import online.game.Opcodes;

public interface UIC extends Opcodes
{	
	static String LoaGoal = "Connect all your pieces";
	static String SelectSourceMessage = "Select the #1 piece to move";
	static String SelectDestMessage = "Select the destination for the selected piece";
	
	enum LoaState implements BoardState
	{
		Puzzle,Play,GameOver;

		public boolean GameOver() {
			return(this==GameOver);
		}

		public boolean Puzzle() {
			return(this==Puzzle);
		}

		public boolean simultaneousTurnsAllowed() {
			
			return false;
		}
	}
	
	public enum LoaId implements CellId
	{
		HitBoard,
		;
		public String shortName() { return(name()); }
	}
	static String LoaStrings[] = {
	
		"LOAP",
		LoaGoal,
		SelectSourceMessage,
		SelectDestMessage,
	};
	static String LoaStringPairs[][] = 
		{
			{   "LOA_family","Lines of Action"},
			{ 	"LOARANDOM","Loa Random"},
			{	"LOA", "Lines of Action"},
			{	"LOA-12","Loa 12x12"},
			{	"LOA_variation","8x8 board"},
			{	"LOAP_variation","contest variation"},
			{	"LOA-12_variation","12x12 board"},
			{	"LOARANDOM_variation","Random symmetric start"}
		};
	
    public static String viewer_class = "loa.viewer.Game";
    public static String player_class = "loa.player.Loa_Board";
    public static final int BLACK_INDEX = 0;
    public static final int WHITE_INDEX = 1;
    public static final String[] PColors = { "B", "W" };
    public static final int M_Pass = 0;
    public static final int M_Resign = -1;
    public static final int M_Forfeit = -2;
    public static final int M_Vacate = -3;
    public static final int M_Select = -4;
    public static final int M_Start = MOVE_START;
    public static final int M_Edit = MOVE_EDIT;
    public static final int M_Undo = MOVE_UNDO;
    public static String LOA_SGF = "9";
    public static Color reviewModeBackground = new Color(243, 203, 100);
    public static Color table_color = new Color(223, 203, 155);
    public static Color board_color = new Color(223, 203, 155);
    public static Color black_square_color = new Color(218, 155, 71);
    public static Color loap_7_color = new Color(0.2f,0.6f,0.2f);
    public static Color loap_3_color = new Color(0.99f,0.8f,0.2f);
    public static Color white_square_color = new Color(230, 187, 130);
    public static Color grid_color = new Color(0, 0, 0);
    public static Color black_stone_color = new Color(30, 30, 30);
    public static Color white_stone_color = new Color(244, 244, 244);
    public static Color selected_arrow_color = new Color(255, 0, 0);
    public static Color lastmove_arrow_color = new Color(120, 120, 120);
    public static Color moving_stone_color = new Color(255, 0, 255);
    public static Color checking_to_square_color = new Color(0, 255, 255);
    public static Color blocked_square_color = new Color(100, 100, 100);
    public static int LOAPS_MAX_MOVES = 101;
    public static int LOAPS_CENTER_BONUS = 7;
    public static int LOAPS_CORNER_BONUS = 3;
    public static int LOAPS_CONNECTIVITY_BONUS = 12;
    
 
    public static final String rules = "Rules for the Lines of Action game\n\n" +
        "1. The object of the game is to get all your pieces into one connected group.  Diagonals are considered connected.\n" +
        "2. Moves are always in straignt lines.  Diagonals, Up, and Down are legal directions.\n" +
        "3. The legal distance you can move depends on the number of pieces on the line you want to move along.  You always move the same number of squares as there are pieces ANYWHERE ALONG THE LINE.\n" +
        "4. You can jump over your own pieces\n" +
        "5. You cannot jump over your opponents pieces, but you can capture by landing on a square he occupies.\n" +
        "6. If you have no legal moves (possible, but not likely) you must pass.\n";
    public static final int sgf_game_id = 9; // identifies LOA games in sgf format

    /* panel ids, powers of 2 please! */
    public static final int history_panel = 1;
    public static final int comment_panel = 2;
    public static final int setup_panel = 4;
    public static final int title_panel = 8;
    public static final int all_moves = 16;
    public static final int writeok = 32; //ie: not readonly
    public static final int invert_y_panel = 64;
    public static final int show_grid_panel = 128;
    public static final int player_mode = 256;

    /* sgf property names */
    public static final String addingstones_property = "AS";
    public static final String initialposition_property = "IP";
    public static final String select_property = "SE";
    public static final String player_black = "PB";
    public static final String player_white = "PW";
    public static final String invert_y_axis = "IY";

    /* strictly local sgf properties */
    public static final String local_move_property = "MV";
    public static final String historyposition_property = "HP";
    public static final String ImageDir = "/loa/images/";
    public static final int LIGHT_TILE_INDEX = 0;
    public static final int DARK_TILE_INDEX = 1;
    public static final int GREEN_TILE_INDEX = 2;
    public static final int YELLOW_TILE_INDEX = 3;
    public static final int BACKGROUND_TILE_INDEX = 4;
    public static final int BACKGROUND_REVIEW_TILE_INDEX = 5;
    public static final String TextureNames[] = 
    	{ "light-tile","dark-tile","green-tile","yellow-tile" ,
    	 "background-tile","background-review-tile",
    	 };
   public static final int ICON_INDEX = 0;
   public static final String IconNames[] = {     	 "loa-icon-nomask" };
    
   double TSCALES[][] = 
   	{ 	{ 0.5,0.5,1.2},
   		{ 0.5,0.5,1.2},
   		{ 0.5,0.5,1.2},
   		{ 0.5,0.5,1.2}};

   public static final String ImageNames[] = {"black-chip-np","white-chip-np"};
    public static final double SCALES[][] = 
	{ 	{0.53,0.470,1.5},
		{0.53,0.472,1.5}
		};
    
    public enum Setup_Type
    {	LOAP("LOAP",7),
    	Loa_12("Loa-12",12),
    	Loa("Loa",8),
    	Standard("Standard",8),
    	Parachute("Parachute",8),
    	Scrambled("Scrambled-Eggs",8),
    	Random("LoaRandom",8),
    	Gemma("Gemma",8),
    	Custom("Custom",8),
    	Reversed("Reversed",8),
    	Reversed_Scrambled("Reversed Scrambled",8),
    	Reversed_Parachute("Reversed Parachute",8);
    String name = this.toString();
    int boardSize = 8;
    Setup_Type(String n,int bs) { name = n; boardSize = bs; }
    static public Setup_Type find(String str) 
    	{
    	for(Setup_Type se : values())
    	{	if(str.equalsIgnoreCase(se.name)) { return(se); }
    	}
    	return(null);
    	}
    }
    enum Move_Reason
    {
    	Ok,From_Empty,Off_Board,Land_On_Friend,Bad_Direction,
    	Wrong_Count,Wrong_Color,Bad_Coordinates,Skip_Enemy,Blocked_Square
    }
    
}
