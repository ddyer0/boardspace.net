package mogul;

import lib.G;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;

public interface MogulConstants 
{	static String ChipHelpText = "#1{##no chips, chip, chips}";
	static String CardHelpText = "#1{##no cards, card, cards}";
	static String OutOfAuction = "Out of the auction";
	static String StillInAuction = "Still in the Auction";
	static String NormalPlayDescription = "Pay a chip, or take the pot";
	static String WonAuctionDescription = "Take the card, or sell cards";
	static String SellCardActionDescription = "Sell Cards";
	static String SellOneCardActionDescription = "Sell cards one at a time";
	static String SellNoneString = "Sell no cards";
	static String DoneSellingString = "Done selling cards";
	static String SellSomeCardsString = "Sell some #1 cards";
	static String SellAnotherCardString = "Sell another #1 card";
	static String SellAllCardsString = "Sell All #1 Cards";
	static String TakeCardString = "Take The #1 Card";
	static String VictoryCondition = "Score the most points buying and selling cards";
	static String PointCount = "#1{ Points, Point, Points}";
	static String DiscardDescriptionString = "#1{##No Discards, Discard, Discards}";
	static String NoChipsString = "Take no chips";
	static String BankDescriptionString = "#1{##bank is empty,bank has # chip,bank has # chips}";
	static String AuctionDescription = "The auction is for a #1 card, or the right to sell #2 cards";
	static String DeckDescriptionStringKey = "DeckDescriptionStringKey";
	static String DeckDescriptionString = 
		"deck contains 8 brown,7 pink, 6 blue, 5 green, 5 yellow cards\n"
	   +"with borders 5 brown, 7 pink, 7 blue, 6 green, 6 yellow";
	static String SellShortMoveString = "Sell";
	static String SellallShortMoveString = "Sell All";
	static String TakeChipsShortMoveString = "Take Chips";
	static String BuyChipsShortMoveString = "Buy Chips";
	static String PayShortMoveString = "Pay";
	static String TakeCardShortMoveString = "Take Card";
	static final String ServiceName = "Information for #1";
	static final String PayChip = "PayChip";
	public static String MogulStrings[] = 
	{	"Mogul",
		ServiceName,
		SellShortMoveString,
		TakeChipsShortMoveString,
		BuyChipsShortMoveString,
		PayShortMoveString,
		AuctionDescription,
		TakeCardShortMoveString,
		SellallShortMoveString,
		ChipHelpText,
		PointCount,
		BankDescriptionString,
		NoChipsString,
		DiscardDescriptionString,
		VictoryCondition,
		SellNoneString,
		DoneSellingString,
		SellAllCardsString,
		TakeCardString,
		SellSomeCardsString,
		SellAnotherCardString,
		CardHelpText,
		OutOfAuction,
		StillInAuction,
		SellCardActionDescription,
		NormalPlayDescription,
		WonAuctionDescription,
    	"Cards",
    	"BuyChips",
    	"Deck",
    	"TakePot",
    	"TakeCard",
    	"Discards",
    	"EndSale",
    	"SellAll",
    	"SellCard"
	};
	public static final String MogulStringPairs[][] = 
	{	{"Mogul_family","Mogul"},
        {"Mogul_variation","standard Mogul"},
        {PayChip,"Pay Chip"},
		{DeckDescriptionStringKey,DeckDescriptionString}
	};
	
	static final int TRACK_ACROSS = 10;
	static final int TRACK_DOWN = 6;
	static final int TRACK_AROUND = (TRACK_ACROSS+TRACK_DOWN)*2;
	static final int N_CHIPS = 45;
	static final int CHIPS_PER_PLAYER = 6;
	static final String Mogul_INIT = "mogul";	//init for standard game

    //	these next must be unique integers in the dictionary
    enum MogulId implements CellId
    {	SeeChips("See"),
    	BoardLocation("B"),
    	HitChips("PayChip"),
    	HitCards("Cards"),
    	HitBank("BuyChips"),
    	HitDeck("Deck"),
    	HitPot("TakePot"),
    	HitAuction("TakeCard"),
    	HitDiscards("Discards"),
    	HitNoSale("EndSale"),
    	HitSellAll("SellAll"),
    	HitSellCard("SellCard"),
    	PlayerEye("PlayerEye");
	String shortName = name();
	public String shortName() { return(shortName); }
	public int opcode() { return(300+ordinal()); }
	static public MogulId findOp(int i) 
		{ for(MogulId v : values()) { if(v.opcode()==i) { return(v); }} return(null); }
	static public String opName(int i) 
		{ MogulId v = findOp(i);
		  return(v==null ? null : v.shortName); 
		} 
	MogulId(String sn) { if(sn!=null) { shortName = sn; }}
	static public MogulId find(String s)
	{	
		for(MogulId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
		return(null);
	}
	static public MogulId get(String s)
	{	MogulId v = find(s);
		G.Assert(v!=null,IdNotFoundError,s);
		return(v);
	}
    }

	
    public class StateStack extends OStack<MogulState>
	{
		public MogulState[] newComponentArray(int n) { return(new MogulState[n]); }
	}
    
    public enum MogulState implements BoardState
    {	Puzzle(PuzzleStateDescription),
    	Gameover(GameOverStateDescription),
    	Resign(ResignStateDescription),
    	Confirm(ConfirmStateDescription),
    	Play(NormalPlayDescription),
    	WonAuction(WonAuctionDescription),
    	SellCard(SellCardActionDescription),
    	SellOneCard(SellOneCardActionDescription);
    	String description;
    	MogulState(String des) { description = des; }
    	public String getDescription() { return(description); }
    		public boolean Puzzle() { return(this==Puzzle); } public boolean simultaneousTurnsAllowed() { return(false); }
    	public boolean GameOver() { return(this==Gameover); }
    };
	
    static final String Mogul_SGF = "Mogul"; // sgf game name
    static final String[] MOGULGRIDSTYLE = { "1", null, "A" }; // left and bottom numbers

 
    // file names for jpeg images and masks
    static final String ImageDir = "/mogul/images/";
	// sounds
    static final int BACKGROUND_TILE_INDEX = 0;
    static final int BACKGROUND_REVIEW_INDEX = 1;
    static final int ICON_INDEX = 2;
    static final String TextureNames[] = 
    	{ "background-tile" ,
    	  "background-review-tile",
    	  "mogul-icon-nomask",
    	  };

    static final int BOARD_INDEX = 0;
    static final int PATCH_INDEX = 1;
    static final String ImageNames[] = { "board","patch" };


}