/* copyright notice */package container;

import lib.G;
import lib.InternationalStrings;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;

public interface ContainerConstants
{	
	static final String AcceptMessage = "Accept $#1 from #2";
	static final String BuyMessage = "Buy the goods for $#1";
	static final String LoadDockMessage = "Load more goods from the Dock";
	static final String TakeFirst = "Take your first action";
	static final String TakeSecond = "Take your second action";
	static final String LoadGoods = "Load the goods you will produce";
	static final String LoadShip = "Load your ship from the Dock";
	static final String LoadWarehouse = "Load the your warehouse from the seller";
	static final String OfferPrice = "Offer a price for the goods on the ship";
	static final String RepriceFactory = "Reprice the goods in your factory storage";
	static final String RepriceWarehouse = "Reprice the goods in your warehouse storage";
	static final String TradeForGold = "trade a second container for a gold container";
	static final String FundLoan = "fund a loan from a player or the bank";
	static final String AcceptOrPay = "accept the bid, or pay the amount yourself";
	static final String IncreaseBid = "you may increase your bid";
	static final String LoadLuxury = "load a luxury container";
	static final String MustLoan = "You must take out a loan to pay interest";
	static final String PlaceBid = "Place your bid to fund the loan";
	static final String AcceptLoan = "Accept this loan financing or not";
	static final String FinishAuction = "click Done to finish this Auction";
	static final String LoanForMessage = "Loan for #1 from #2";
	static final String LoanFromBank = "Loan for #1 from the bank";
	static final String InterestPaid = "#1 is interest paid";
	static final String PaymentsReceived = "#1 is interest payments received";
	static final String IslandEffeciency = "#1 is the effeciency of island purchases";
	static final String LoansMessage = "Loans";
	static final String GoalMessage = "Maximize your profits";
	static final String AuctionMessage = "Auction";
	static final String WarehouseMessage = "Warehouse";
	static final String UnsoldWarehouse = "Unsold Warehouses";
	static final String ShipMessage = "#1 Ship";
	static final String MachineMessage = "#1 Machine";
	static final String ContainerCountMessage = "#1 #2 Containers";
	static final String LoanCard = "Loan Card";
	static final String DeclineLoan = "Let someone else make the loan";
	static final String MakeLoan = "I will make the loan";
	static final String SetBid = "set bid amount";
	static final String SetBidAmount = "Bid $ #1";
	static final String RequestingBids = "Requesting Bids";
	static final String WaitForPlayers = "Wait for the other players to decide";
	static final String RequestingLoan = "Requesting Loan";
	static final String IWillFund = "Yes, I will fund the loan";
	static final String BankWillFund = "No, let the bank fund it";
	static final String AcceptFromBank = "Accept the loan from the bank";
	static final String DeclineFromBank = "Decline the loan";
	static final String SpotPrice = "Spot Price #1";
	static final String CashScore = "Cash/Score";
	static final String MachinesMessage = "Machines";
	static final String WarehousesMessage = "Warehouses";
	static final String ShippingMessage = "Shipping";
	static final String IslandMessage = "Island";
	static final String CashSpent = "#1 is cash spent on auctions";
	static final String IslandValue = "#1 is the value of your goods on the island";
	static final String CashPerAction = "#1 is cash earned per shipping action";
	static final String CashForShipping = "#1 is cash spent on shipping";
	static final String CashFromAuctions = "#1 is cash reveived from auctions";
	static final String CashPerWarehouse = "#1 is cash earned per warehouse action";
	static final String CashForGoods = "#1 is cash spent on warehouses and goods";
	static final String CashForSelling = "#1 is cash earned selling goods";
	static final String CashPerProduction = "#1 is cash earned per production action";
	static final String CashForMachines = "#1 is cash spent on machines and production";
	static final String CashOnHand = "#1 is your cash on hand";
	static final String FinalScore = "#1 is your final score";
	static final String SeeCashMessage = "seeyourcash";
	static final String SeeingCashMessage = "seeingyourcash";
	static final String ServiceName = "Container Player info for #1";
	static final String LoanOffer = "loan #1 - #2";
	static final String MaxPrice = "up to #1";
	static final String RebidOffer = "$ #1 - #2";

	static final String Container_INIT = "container";	//init for standard game
	static final String Container_First_Init = "container-first";
	static final String Container_Second_Init = "container-second";
	enum ContainerId implements CellId
	{
		AtSeaLocation,	// cells containing ships
		AtDockLocation,	// player may cells containing chips
		ShipGoodsLocation,	// cells containing goods
		IslandGoodsLocation,	// goods on an island space
		FactoryGoodsLocation,	// goods on a player mat
		WarehouseGoodsLocation,	// warehouse on a player mat
		MachineLocation,		// cells containing machines
		WarehouseLocation,	// cells containing warehouses
    	ContainerLocation,	// cells containing unsold containers
    	AuctionLocation,				// active auction slot
    	AtIslandParkingLocation,		// parking lot at the island
    	LoanLocation,			// loan card
    	CashLocation,		// cashbox
    	HitDeclineLoan,			// won't fund a loan
    	HitFundLoan,			// will fund a loan
    	HitPendingAuctionDone,		// hit done
    	HitBidLocation,		// place bid
    	HitAcceptLocation,	// accept a bid
    	HitBuyLocation,		// buy a lot yourself instead of accepting a bid
    	HitAcceptLoanLocation,	// accept a loan
    	HitDeclineLoanLocation,	// declined a loan
    	HitPendingLoanDone,		// pending loan 
    	HitSelectPerspective,	// for debugging the robot
    	Zoom_Slider,			// animation speed
    	SeeCash,				// see the cash balance
    	HideInfo,				// hide the hidden information
    	;
    
	public String shortName() { return(name()); }
	
	static public ContainerId find(int l)
	{	for(ContainerId v : values())  { if (v.IID()==l) { return(v); }}
		return(null);
	}
	static public ContainerId get(int l)
	{	ContainerId v = find(l);
		G.Assert(v!=null,"Uid %d not found",l);
		return(v);
	}
	public int IID() { return(0x73758+ordinal()); }

	}
	class StateStack extends OStack<ContainerState>
	{
		public ContainerState[] newComponentArray(int n) { return(new ContainerState[n]); }
	}
	public enum ContainerState implements BoardState
    {	PUZZLE_STATE(PuzzleStateDescription),
    	RESIGN_STATE(ResignStateDescription),
    	GAMEOVER_STATE(GameOverStateDescription),
    	CONFIRM_STATE(ConfirmStateDescription),
    	PLAY1_STATE(TakeFirst),		// 
        PLAY2_STATE(TakeSecond),		// 
        LOAD_SHIP_STATE(LoadDockMessage),	// loading second and subsequent goods from a warehouse to a ship
        LOAD_FACTORY_GOODS_STATE(LoadGoods),	// load machine goods from storage
        LOAD_WAREHOUSE_GOODS_STATE(LoadWarehouse),	// buyng warehouse goods from a factory
        AUCTION_STATE(OfferPrice),					// auctioning goods at the island
        REPRICE_FACTORY_STATE(RepriceFactory),		// repricing goods in factory storage
        REPRICE_WAREHOUSE_STATE(RepriceWarehouse),		// repricing goods in warehouse storage
        TRADE_CONTAINER_STATE(TradeForGold),		// trade 2 for 1 gold
        FUND_LOAN_STATE(FundLoan),				// other players decide to fund the loan or not
        ACCEPT_BID_STATE(AcceptOrPay),				// after an auction, accept the high bid or buy it.
        REBID_STATE(IncreaseBid),					// second round of bidding for goods
        LOAD_LUXURY_STATE(LoadLuxury),			// load a luxury container
        LOAD_SHIP_1_STATE(LoadShip),			// loading the first container onto a ship
        TAKE_LOAN_STATE(MustLoan),				// must take out a loan to pay interest.. what a loser
        FINANCEER_STATE(PlaceBid),				// bidding on funding for a loan
        ACCEPT_LOAN_STATE(AcceptLoan),			// final accepting of a loan
        CONFIRM_AUCTION_STATE(FinishAuction);		// final acceptance of an auction or a loan
     
    	String description;
    	ContainerState(String des) { description = des; }
    	public String getDescription() { return(description); }
    	public boolean GameOver() { return(this==GAMEOVER_STATE); }
    	public boolean Puzzle() { return(this==PUZZLE_STATE); } public boolean simultaneousTurnsAllowed() { return(false); }
    }
    
     /* states of the board/game.  Because several gestures are needed to complete a move, and
    there are several types of move, we use a state machine to determine what is legal */
    //static final int PUZZLE_STATE = 0; // no game, just plopping balls and removing rings at will.
    //static final int RESIGN_STATE = 1; // pending resignation, ready to confirm
    //static final int GAMEOVER_STATE = 2; // game is over (someone won or resigned)
    static final int CONFIRM_STATE = 3; 	// move and remove completed, ready to commit to it.
	
    static final int MOVE_PICK = 204; // pick a chip from a pool
    static final int MOVE_DROP = 205; // drop a chip
    static final int MOVE_PICKC = 206;	// pick a container (color)
    static final int MOVEC_FROM_TO = 207;	// move a container
    static final int MOVE_FROM_TO = 209;	// move from rack to board
    static final int MOVE_FUND = 211;		// agree to fund a loan
    static final int MOVE_BID = 212;		// bid on a loan or shipment
    static final int MOVE_DECLINE = 213;	// decline to fund
    
    static final int MOVE_EPHEMERAL_AUCTION_BID = 312;
    static final int MOVE_EPHEMERAL_FUND = 311;
    static final int MOVE_EPHEMERAL_DECLINE = 313;
    static final int MOVE_EPHEMERAL_LOAN_BID = 314;
    
    static final int MOVE_ACCEPT = 214;		// accept a player's bid
    static final int MOVE_BUY = 215;		// buy a lot instead of selling it.
    static final int MOVE_ACCEPT_LOAN = 216;
    static final int MOVE_DECLINE_LOAN = 217;
	
    static final int CONTAINER_OFFSET = 300;
    static final int CONTAINER_BLACK = 300;
    static final int CONTAINER_WHITE = 301;
    static final int CONTAINER_TAN = 302;
    static final int CONTAINER_BROWN = 303;
    static final int CONTAINER_ORANGE = 304;
    static final int CONTAINER_GOLD = 305;
    
	static final int STANDARD_LOAN_AMOUNT = 10;	
	static final int STANDARD_CONTAINER_VALUE = 10;
	static final int MACHINE_COLORS = 5;	// number of types of machine
	static final int MAX_SHIP_GOODS = 5;	// number of goods per ship
	static final int MAX_PLAYERS = 5;		// game is for 3-4
	static final int CONTAINER_COLORS = 6;	// number of container colors (including gold)
	static final int DEFAULT_COLUMNS = 18;	// standard container board (for layout only)
	static final int DEFAULT_ROWS = 13;		// standard container board (for layout only)
    

    
    static void putStrings()
    {
    	
    		final String[][] containerStringPairs =
    		{ {SeeCashMessage,"Enlarge Info for #1"},
    		  {SeeingCashMessage,"Info for #1"},	
    		  {"Container-First","Container - Original"},
    		  {"Container","Container"},
    		  {"Container-First_variation","original rules"},
    		  {"Container_family","Container"},
    		  {"Container_variation","second shipment rules"},  

    		};
    	
    		final String[] containerStrings = 
    		{	LoanOffer,
    			MaxPrice,
    			RebidOffer,
    	       AuctionMessage,
    	       ServiceName,
    	        GoalMessage,
    	        LoanForMessage,
    	        TakeFirst,
    	        RepriceFactory,
    	        TakeSecond,
    	        WarehouseMessage,
    	        UnsoldWarehouse,
    	        ShipMessage,
    	        MachineMessage,
    	        ContainerCountMessage,
    	        LoanCard,
    	        DeclineLoan,
    	        MakeLoan,
    	        FundLoan,
    	        LoadGoods,
    	        SetBid,
    	        OfferPrice,
    	        LoadWarehouse,
    	        RepriceWarehouse,
    	        LoadShip,
    	        LoadDockMessage,
    	        BuyMessage,
    	        AcceptMessage,
    	        SetBidAmount,
    	        RequestingBids,
    	        IncreaseBid,
    	        WaitForPlayers,
    	        AcceptOrPay,
    	        TradeForGold,
    	        MustLoan,
    	        RequestingLoan,
    	        BankWillFund,
    	        AcceptFromBank,
    	        IWillFund,
    	        PlaceBid,
    	        DeclineFromBank,
    	        AcceptLoan,
    	        LoanFromBank,
    	        SpotPrice,
    	        FinishAuction,
    	        LoadLuxury,       
    	        // for stats
    	        CashScore,
    	        MachinesMessage,
    	        WarehousesMessage,
    	        ShippingMessage,
    	        IslandMessage,
    	        LoansMessage,
    	        InterestPaid,
    	        PaymentsReceived,
    	        IslandEffeciency,
    	        CashSpent,
    	        IslandValue,
    	        CashPerAction,
    	        CashForShipping,
    	        CashFromAuctions,
    	        CashPerWarehouse,
    	        CashForGoods,
    	        CashForSelling,
    	        CashPerProduction,
    	        CashForMachines,
    	        CashOnHand,
    	        FinalScore,
    		};
    	InternationalStrings.put(containerStrings);
    	InternationalStrings.put(containerStringPairs);
    }
}