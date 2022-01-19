package qe;

import lib.G;
import lib.OStack;
import lib.CellId;
import online.game.BaseBoard.BoardState;


public interface QEConstants 
{	static String QEVictoryCondition = "Score the most while not using the most money";
	static String SealedBidStateDescription = "Make a sealed bid";
	static String WitnessStateDescription = "Consider the bids, hit Done to continue";
	static String RebidStateDescription = "Tied bids - some players must bid again";
	static String OpenBidStateDescription = "Make an open bid";
	static String EphemeralWaitDescription = "Wait for all other bids";
	static String NoQEMessage = "No QE";
	static String VPMessage = "#1 victory Points";
	static String MoneyMessage = "money created";
	static String TileValueMessage = "for value of tiles";
	static String NationalMessage = "for #1 tiles from #2";
	static String MonopolyValueMessage = "for monopy of #1 on #2";
	static String DiversityMessage = "for #1 different industries";
	static String ThriftMessage = "for creating the least money";
	static String MostMoneyMessage = "for creating the most money";
	static String BuildingName = "Building";
	static String AutosName = "Autos";
	static String PlanesName = "Planes";
	static String TrainsName = "Trains";
   	static String GRName = "Germany";
   	static String UKName = "United Kingdom";
   	static String FRName = "France";
   	static String USName = "United States"; 	   	
   	static String ForbiddenAmount = "Not #1";
	static String ServiceName = "Q.E. Player info for #1";
	static String HighBidMessage = "High Bid";
	static String QEStrings[] = 
	{  	ServiceName,
		HighBidMessage,
		ForbiddenAmount,
		EphemeralWaitDescription,
		WitnessStateDescription,
		BuildingName,AutosName,PlanesName,TrainsName,
		GRName,UKName,FRName,USName,
		NoQEMessage,
		VPMessage,
		MoneyMessage,
		NationalMessage,
		DiversityMessage,
		MostMoneyMessage,
		MonopolyValueMessage,
		ThriftMessage,
		TileValueMessage,
		SealedBidStateDescription,
		RebidStateDescription,
		OpenBidStateDescription,
		QEVictoryCondition,
	};
	static String QEStringPairs[][] = 
	{   {"QE","Q.E."},
		{"QE_family","Q.E."},
		{"QE_variation","Standard Q.E."}
	};

	class StateStack extends OStack<QEState>
	{
		public QEState[] newComponentArray(int n) { return(new QEState[n]); }
	}
	//
    // states of the game
    //
	public enum QEState implements BoardState
	{
	Puzzle(PuzzleStateDescription,false,false,false),
	SealedBid(SealedBidStateDescription,false,false,false),
	Rebid(RebidStateDescription,false,false,false),

	// UI only states
	EphemeralSealedBid(SealedBidStateDescription,false,false,true),
	EphemeralRebid(RebidStateDescription,false,false,true),
	EphemeralConfirm(ConfirmStateDescription,true,true,true),
	EphemeralWait(EphemeralWaitDescription,false,false,true),
	Witness(WitnessStateDescription,true,true,false),
	OpenBid(OpenBidStateDescription,false,false,false),
	Gameover(GameOverStateDescription,false,false,false),
	Confirm(ConfirmStateDescription,true,true,false);
	QEState(String des,boolean done,boolean digest,boolean sim)
	{	simultaneous = sim;
		description = des;
		digestState = digest;
		doneState = done;
	}
	boolean simultaneous;
	boolean doneState;
	boolean digestState;
	String description;
	public boolean GameOver() { return(this==Gameover); }
	public String description() { return(description); }
	public boolean doneState() { return(doneState); }
	public boolean digestState() { return(digestState); }
	public boolean Puzzle() { return(this==Puzzle); } 
	public boolean simultaneousTurnsAllowed() 
		{ 
		switch(this)
			{
			default:
				return(false);
			case SealedBid:
			case Rebid:
				return(true);
			}
		}
	};
	
    //	these next must be unique integers in the QEmovespec dictionary
	//  they represent places you can click to pick up or drop a stone
	enum QEId implements CellId
	{
    	
    	// cards
    	Back(null),
    	NoQE("NoQE"),
    	GR(GRName),
    	UK(UKName),
    	FR(FRName),
    	US(USName), 	   	
    	Build(BuildingName),
    	Autos(AutosName),
    	Planes(PlanesName),
    	Trains(TrainsName),
    	    	
    	Gov_1_US("Build-1-US"),
    	Gov_2_GR("Build-2-GR"),
    	Gov_3_FR("Build-3-FR"),
    	Gov_4_UK("Build-4-UK"),
    	
    	Cars_1_UK("Cars-1-UK"),
    	Cars_2_FR("Cars-2-FR"),
    	Cars_3_US("Cars-3-US"),
    	Cars_4_GR("Cars-4-GR"),
    	
    	Planes_1_FR("Planes-1-FR"),
    	Planes_2_UK("Planes-2-UK"),
    	Planes_3_GR("Planes-3-GR"),
    	Planes_4_US("Planes-4-US"),
    	
    	Trains_1_GR("Trains-1-GR"),
    	Trains_2_US("Trains-2-US"),
    	Trains_3_UK("Trains-3-UK"),
    	Trains_4_FR("Trains-4-FR"),
    	
    	HitTilesWon(null),		// player tiles won
    	HitIndustry(null),		// player industry card
    	HitWhiteBoard(null),	// player whiteboard card
    	HitNoQE(null),			// player noQE chip
    	HitAuction(null),
    	HitPending(null),
    	HitCurrent(null),
    	HitWaste(null),
    	HitEcommitButton(null),
    	BoardLocation(null),
    	ShowHidden(null),
    	;
    	String shortName = name();
    	public String shortName() { return(shortName); }
    	QEId(String sn) { if(sn!=null) { shortName = sn; }}
    	static public QEId find(String s)
    	{	
    		for(QEId v : values()) { if(s.equalsIgnoreCase(v.shortName)) { return(v); }}
    		return(null);
    	}
    	static public QEId get(String s)
    	{	QEId v = find(s);
    		G.Assert(v!=null,IdNotFoundError,s);
    		return(v);
    	}

	}
    
 enum QEVariation
    {
    	qe("qe");
    	String name ;
    	// constructor
    	QEVariation(String n) 
    	{ name = n; 
    	}
    	// match the variation from an input string
    	static QEVariation findVariation(String n)
    	{
    		for(QEVariation s : values()) { if(s.name.equalsIgnoreCase(n)) { return(s); }}
     		return(null);
    	}
     	
    }
 
 	// victory point values for multiple elements of the same type
 	int nationTable[] = {0,1,3,6,10};
 	int monopolyTable[] = {0,0,3,6,10,10};
 	int diversityTable[] =  {0,0,0,6,10};

	// move commands, actions encoded by movespecs.  Values chosen so these
    // integers won't look quite like all the other integers
 	
    static final String QE_SGF = "qe"; // sgf game number allocated for qe

    // file names for jpeg images and masks
    static final String ImageDir = "/qe/images/";

}