package online.common;

import lib.G;

public class SeatingChart {

	// communication setting up the lobby or games
	public enum Seating
	{	
		Bottom_Left(0.2,0),
		Bottom_Center(0.5,0),
		Bottom_Right(0.8,0),
		Bottom_Pair_Left(0.33,0),
		Bottom_Pair_Right(0.66,0),
		Top_Left(0.2,1),
		Top_Center(0.5,1),
		Top_Right(0.8,1),
		Top_Pair_Left(0.33,1),
		Top_Pair_Right(0.66,1),
		
		Below_Left(0.2,-0.3),			// bubbles for stacked bubbles on the same side of the table
		Below_Center(0.5,-0.3),
		Below_Right(0.8,-0.3),
		Below_Pair_Left(0.33,-0.3),
		Below_Pair_Right(0.66,-0.3),

		Left_End(0,0.5),
		Right_End(1,0.5);
		double x_position;
		double y_position;
		Seating(double xpos,double ypos)
		{
			x_position = xpos;
			y_position = ypos;
		}
	}
	static Seating[] soloReview = {};
	static Seating[] soloPortrait = { Seating.Right_End };
	static Seating[] soloLandscape = {Seating.Bottom_Center};
	static Seating[] sideBySide = { Seating.Bottom_Pair_Right,Seating.Bottom_Pair_Left};
	static Seating[] faceToFaceLandscape = { Seating.Top_Center,Seating.Bottom_Center};
	static Seating[] faceToFacePortrait = { Seating.Left_End,Seating.Right_End };
	static Seating[] leftCorner = {Seating.Bottom_Center,  Seating.Left_End};
	static Seating[] rightCorner = { Seating.Bottom_Center, Seating.Right_End};
	static Seating[] leftL = { Seating.Bottom_Pair_Right,Seating.Bottom_Pair_Left,Seating.Left_End};
	static Seating[] rightL = { Seating.Bottom_Pair_Right, Seating.Bottom_Pair_Left,Seating.Right_End};
	static Seating[] threeAround = { Seating.Bottom_Center,Seating.Left_End,Seating.Top_Center};
	static Seating[] threeAcross = { Seating.Bottom_Pair_Right,Seating.Bottom_Pair_Left,Seating.Top_Center };
	static Seating[] threeWide = {  Seating.Bottom_Center,Seating.Left_End, Seating.Right_End };
	static Seating[] fourAround = { Seating.Bottom_Center,Seating.Left_End,Seating.Top_Center,Seating.Right_End };
	static Seating[] fourAroundU = { Seating.Bottom_Pair_Right,Seating.Bottom_Pair_Left,Seating.Left_End, Seating.Right_End};
	static Seating[] fourAcross = { Seating.Bottom_Pair_Right,Seating.Bottom_Pair_Left,Seating.Top_Pair_Left,Seating.Top_Pair_Right };
	static Seating[] fiveAround = { Seating.Bottom_Pair_Right,Seating.Bottom_Pair_Left,Seating.Left_End,Seating.Top_Center,Seating.Right_End};
	static Seating[] fiveAcross = {Seating.Bottom_Pair_Right,Seating.Bottom_Pair_Left,Seating.Top_Left,Seating.Top_Center,Seating.Top_Right};
	static Seating[] fiveAround1Edge = {Seating.Bottom_Pair_Right,Seating.Bottom_Pair_Left,Seating.Left_End,Seating.Top_Pair_Left,Seating.Top_Pair_Right};
	static Seating[] fiveAround1EdgeCenter = {Seating.Bottom_Pair_Right,Seating.Bottom_Pair_Left,Seating.Left_End,Seating.Top_Pair_Left,Seating.Top_Pair_Right};
	static Seating[] sixAround = {Seating.Bottom_Pair_Right,Seating.Bottom_Pair_Left,Seating.Left_End,Seating.Top_Pair_Left,Seating.Top_Pair_Right,Seating.Right_End};
	static Seating[] sixAcross = {Seating.Bottom_Right,Seating.Bottom_Center,Seating.Bottom_Left,Seating.Top_Left,Seating.Top_Center,Seating.Top_Right};

	static Seating[] sixBelow = {Seating.Bottom_Right,Seating.Bottom_Center,Seating.Bottom_Left,Seating.Below_Left,Seating.Below_Center,Seating.Below_Right};
	static Seating[] fiveBelow = {Seating.Bottom_Pair_Right,Seating.Bottom_Pair_Left,Seating.Below_Left,Seating.Below_Center,Seating.Below_Right};
	static Seating[] fourBelow = { Seating.Bottom_Pair_Right,Seating.Bottom_Pair_Left,Seating.Below_Pair_Left,Seating.Below_Pair_Right };
	static Seating[] threeBelow = { Seating.Bottom_Pair_Right,Seating.Bottom_Pair_Left,Seating.Below_Center };

	public DefinedSeating id;
	Seating seats[] = null;
	public SeatingChart(DefinedSeating idd,Seating[]specs)
	{	id=idd;
		seats = specs==null ? new Seating[0] : specs;
	}
	public boolean seatingPortrait() { return(id.seatingPortrait());}
	public boolean seatingFaceToFace() { return(id.seatingFaceToFace()); }
	public boolean seatingAround() { return(id.seatingAround); }
	
	public boolean plannedSeating() 
	{	return (getNSeats()>=2) && (seatingFaceToFace()||seatingAround());
	}
	
	public Seating[]getSeats() { return(seats); }
	public int getNSeats() { return(seats.length); }
	public static SeatingChart blank = new SeatingChart(DefinedSeating.Undefined,null);
	private static SeatingChart defaultReview = new SeatingChart(DefinedSeating.Undefined,soloReview);
	private static SeatingChart defaultPassAndPlay = new SeatingChart(DefinedSeating.SideBySide,sideBySide);
	private static SeatingChart defaultPortrait = new SeatingChart(DefinedSeating.Undefined,soloPortrait);
	private static SeatingChart defaultLandscape = new SeatingChart(DefinedSeating.Undefined,soloLandscape);
	private static SeatingChart default3P = new SeatingChart(DefinedSeating.ThreeAcross,threeAcross);
	private static SeatingChart default4P = new SeatingChart(DefinedSeating.FourAcross,fourAcross);
	private static SeatingChart default5P = new SeatingChart(DefinedSeating.FiveAcross,fiveAcross);
	private static SeatingChart default6P = new SeatingChart(DefinedSeating.SixAcross,sixAcross);
	private static SeatingChart facePortrait = new SeatingChart(DefinedSeating.FaceToFacePortrait,faceToFacePortrait);
	private static SeatingChart leftCorner2P = new SeatingChart(DefinedSeating.LeftCorner,leftCorner);
	private static SeatingChart rightCorner2P = new SeatingChart(DefinedSeating.RightCorner,rightCorner);
	private static SeatingChart leftCorner3P = new SeatingChart(DefinedSeating.ThreeLeftL,leftL);
	private static SeatingChart rightCorner3P = new SeatingChart(DefinedSeating.ThreeRightL,rightL);
	
	private static SeatingChart faceLandscape = new SeatingChart(DefinedSeating.FaceToFaceLandscapeSide,faceToFaceLandscape);
	

	private static SeatingChart default3POffline = new SeatingChart(DefinedSeating.Undefined,threeBelow);
	private static SeatingChart default4POffline = new SeatingChart(DefinedSeating.Undefined,fourBelow);
	private static SeatingChart default5POffline = new SeatingChart(DefinedSeating.Undefined,fiveBelow);
	private static SeatingChart default6POffline = new SeatingChart(DefinedSeating.Undefined,sixBelow);

	public static SeatingChart defaultSeatingChart(int nplayers)
	{
		if(!G.isTable()) { nplayers = 1; }
		{
		switch(nplayers)
		{
		default:
		case 1:	return( G.getScreenWidth()<G.getScreenHeight() ? defaultPortrait : defaultLandscape);
		case 2: return(defaultPassAndPlay);
		case 3: return(default3P);
		case 4: return(default4P);
		case 5: return(default5P);
		case 6: return(default6P);
		}}
	}
	public static SeatingChart NonServerCharts[] = 
		{
				defaultReview,	
				defaultPortrait,
				defaultLandscape,
				defaultPassAndPlay,
				faceLandscape,
				facePortrait,
				default3POffline,
				default4POffline,
				default5POffline,
				default6POffline,
				
		};
	public static SeatingChart ServerCharts[] = 
		{ defaultReview,
		  defaultPortrait,
		  defaultLandscape,
		  defaultPassAndPlay,
		  faceLandscape,
		  facePortrait,
		  leftCorner2P,
		  rightCorner2P,
		  leftCorner3P,
		  rightCorner3P,
		  new SeatingChart(DefinedSeating.ThreeAround,threeAround),
		  new SeatingChart(DefinedSeating.ThreeWide,threeWide),
		  default3P,
		  new SeatingChart(DefinedSeating.FourAround,fourAround),
		  new SeatingChart(DefinedSeating.FourAroundU,fourAroundU),
		  default4P,
		  new SeatingChart(DefinedSeating.FiveAround,fiveAround),
		  new SeatingChart(DefinedSeating.FiveAround1Edge,fiveAround1Edge),
		  default5P,
		  new SeatingChart(DefinedSeating.SixAround,sixAround),
		  default6P,
		};
	public static enum DefinedSeating 
	{	Undefined(false,false,false,null),
		Portrait(true,false,false,null),		// down, with spare rects at the right
		SideBySide(false,false,false,Portrait),				// two player game with side by side seating
		FaceToFaceLandscapeTop(false,false,true,null),		// two player,  across the top and bottom
		FaceToFaceLandscapeSide(false,false,true,FaceToFaceLandscapeTop),			// two player, over and under on the side
		LeftCornerWide(false,true,true,null),
		RightCornerWide(false,true,true,null),
		LeftCorner(false,true,true,LeftCornerWide),
		RightCorner(false,true,true,RightCornerWide),
		FaceToFacePortrait(true,false,true,null),
		ThreeLeftLW(false,true,true,null),
		ThreeLeftL(false,true,true,ThreeLeftLW),
		ThreeRightLW(false,true,true,null),
		ThreeRightL(false,true,true,ThreeRightLW),
		ThreeAroundLeft(false,true,true,null),
		ThreeAround(false,true,true,ThreeAroundLeft),
		ThreeWideLeft(false,true,true,null),
		ThreeWide(false,true,true,ThreeWideLeft),
		ThreeAcrossLeftCenter(false,false,true,null),
		ThreeAcrossLeft(false,false,true,ThreeAcrossLeftCenter),
		ThreeAcross(false,false,true,ThreeAcrossLeft),
		FourAroundEdgeRect(false,true,true,null),
		FourAroundEdge(false,true,true,FourAroundEdgeRect),
		FourAround(false,true,true,FourAroundEdge),
		FourAroundUW(false,true,true,null),
		FourAroundU(false,true,true,FourAroundUW),
		FourAcrossEdge(false,false,true,null),
		FourAcross(false,false,true,FourAcrossEdge),
		FiveAroundEdge(false,true,true,null),
		FiveAround(false,true,true,FiveAroundEdge),
		FiveAround1EdgeCenter(false,true,true,null),
		FiveAround1Edge(false,true,true,FiveAround1EdgeCenter),
		FiveAcrossEdge(false,false,true,null),
		FiveAcross(false,false,true,FiveAcrossEdge),
		SixAroundEdge(false,true,true,null),
		SixAround(false,true,true,SixAroundEdge),
		SixAcross(false,false,true,null),
		// seating charts not represented in the seating viewer
		
		Landscape(false,false,false,null),		// down, with spare rects below
		Across(false,false,false,null),			// across, with spare rects at the right
		Portrait2X(true,false,false,null),		// two column portrait, spare rects at right
		Landscape2X(false,false,false,null),	// two column landscape, spare rects below
		Portrait3X(true,false,false,null),		// three column portrait, spare rects at right
		Landscape3X(false,false,false,null), 
		;
		DefinedSeating(boolean port,boolean ar,boolean ftf,DefinedSeating alter)
		{	portrait = port;
			seatingFaceToFace = ftf;
			seatingAround = ar;
			alternate=alter;
		}
		boolean portrait;
		boolean seatingAround;
		boolean seatingFaceToFace;
		public DefinedSeating alternate;
		public boolean seatingPortrait() { return(portrait); }
		public boolean seatingAround() { return(seatingAround); }
		public boolean seatingFaceToFace() { return(seatingFaceToFace); }
		public boolean seatingAssigned() { return(seatingFaceToFace||seatingAround); }
		
	}

}