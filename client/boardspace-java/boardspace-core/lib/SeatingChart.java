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
package lib;
/**
 * 
 * these are the templates for player seating which are used by gamelayoutmanager and seatingviewer.
 * 
 */
public class SeatingChart {
	//
	// these are possible seat positions, along the sides of the board
	//
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
		public double x_position;
		public double y_position;
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
	static Seating[] faceToFacePortraitSide = { Seating.Bottom_Left,Seating.Bottom_Right };
	static Seating[] leftCorner = {Seating.Bottom_Center,  Seating.Left_End};
	static Seating[] rightCorner = { Seating.Bottom_Center, Seating.Right_End};
	static Seating[] leftL = { Seating.Bottom_Pair_Right,Seating.Bottom_Pair_Left,Seating.Left_End};
	static Seating[] rightL = { Seating.Bottom_Pair_Right, Seating.Bottom_Pair_Left,Seating.Right_End};
	static Seating[] threeAroundL = { Seating.Bottom_Center,Seating.Left_End,Seating.Top_Center};
	static Seating[] threeAroundR = { Seating.Bottom_Center,Seating.Right_End,Seating.Top_Center};
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
	public String explanation = null;
	public SeatingChart(DefinedSeating idd,Seating[]specs,String expl)
	{	id=idd;
		seats = specs==null ? new Seating[0] : specs;
		explanation = expl;
	}

	public boolean seatingPortrait() { return(id.seatingPortrait());}
	public boolean seatingFaceToFace() { return(id.seatingFaceToFace()); }
	public boolean seatingAround() { return(id.seatingAround); }
	
	public boolean plannedSeating() 
	{	return (getNSeats()>=2) && (seatingFaceToFace()||seatingAround());
	}
	
	public Seating[]getSeats() { return(seats); }
	public int getNSeats() { return(seats.length); }
	static String OnePlayerExplanation = "oneplayerexplanation";
	static String BlankExplanation = "blankseatingexplanation";
	static String TwoPlayerExplanation = "twoplayerexplanation";
	static String ThreePlayerExplanation = "threeplayerexplanation";
	static String FourPlayerExplanation = "fourplayerexplanation";
	static String FivePlayerExplanation = "fiveplayerexplanation";
	static String SixPlayerExplanation = "sixplayerexplanation";
	
	public static SeatingChart blank = new SeatingChart(DefinedSeating.Undefined,null,BlankExplanation);
	private static SeatingChart defaultReview = new SeatingChart(DefinedSeating.Undefined,soloReview,BlankExplanation);
	public static SeatingChart defaultPassAndPlay = new SeatingChart(DefinedSeating.SideBySide,sideBySide,TwoPlayerExplanation);
	private static SeatingChart defaultPortrait = new SeatingChart(DefinedSeating.Undefined,soloPortrait,OnePlayerExplanation);
	private static SeatingChart defaultLandscape = new SeatingChart(DefinedSeating.Undefined,soloLandscape,OnePlayerExplanation);
	private static SeatingChart default3P = new SeatingChart(DefinedSeating.ThreeAcross,threeAcross,ThreePlayerExplanation);
	private static SeatingChart default4P = new SeatingChart(DefinedSeating.FourAcross,fourAcross,FourPlayerExplanation);
	private static SeatingChart default5P = new SeatingChart(DefinedSeating.FiveAcross,fiveAcross,FivePlayerExplanation);
	private static SeatingChart default6P = new SeatingChart(DefinedSeating.SixAcross,sixAcross,SixPlayerExplanation);
	public static SeatingChart facePortrait = new SeatingChart(DefinedSeating.FaceToFacePortrait,faceToFacePortrait,TwoPlayerExplanation);
	private static SeatingChart leftCorner2P = new SeatingChart(DefinedSeating.LeftCorner,leftCorner,TwoPlayerExplanation);
	private static SeatingChart rightCorner2P = new SeatingChart(DefinedSeating.RightCorner,rightCorner,TwoPlayerExplanation);
	private static SeatingChart leftCorner3P = new SeatingChart(DefinedSeating.ThreeLeftL,leftL,ThreePlayerExplanation);
	private static SeatingChart rightCorner3P = new SeatingChart(DefinedSeating.ThreeRightL,rightL,ThreePlayerExplanation);
	
	public static SeatingChart faceLandscape = new SeatingChart(DefinedSeating.FaceToFaceLandscapeSide,faceToFaceLandscape,TwoPlayerExplanation);
	
	private static SeatingChart portrait = new SeatingChart(DefinedSeating.RightEnd,soloPortrait,OnePlayerExplanation);

	private static SeatingChart default3POffline = new SeatingChart(DefinedSeating.Undefined,threeBelow,ThreePlayerExplanation);
	private static SeatingChart default4POffline = new SeatingChart(DefinedSeating.Undefined,fourBelow,FourPlayerExplanation);
	private static SeatingChart default5POffline = new SeatingChart(DefinedSeating.Undefined,fiveBelow,FivePlayerExplanation);
	private static SeatingChart default6POffline = new SeatingChart(DefinedSeating.Undefined,sixBelow,SixPlayerExplanation);

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
		  portrait,
		  defaultLandscape,
		  defaultPassAndPlay,
		  faceLandscape,
		  facePortrait,
		  leftCorner2P,
		  rightCorner2P,
		  leftCorner3P,
		  rightCorner3P,
		  new SeatingChart(DefinedSeating.ThreeAroundL,threeAroundL,ThreePlayerExplanation),
		  new SeatingChart(DefinedSeating.ThreeWide,threeWide,ThreePlayerExplanation),
		  new SeatingChart(DefinedSeating.ThreeAroundR,threeAroundR,ThreePlayerExplanation),
		  default3P,
		  new SeatingChart(DefinedSeating.FourAround,fourAround,FourPlayerExplanation),
		  new SeatingChart(DefinedSeating.FourAroundU,fourAroundU,FourPlayerExplanation),
		  default4P,
		  new SeatingChart(DefinedSeating.FiveAround,fiveAround,FivePlayerExplanation),
		  new SeatingChart(DefinedSeating.FiveAround1Edge,fiveAround1Edge,FivePlayerExplanation),
		  default5P,
		  new SeatingChart(DefinedSeating.SixAround,sixAround,SixPlayerExplanation),
		  default6P,
		};
	public static enum DefinedSeating 
	{	Undefined(false,false,false,null,false,false),
		Portrait(true,false,false,null,false,false),		// down, with spare rects at the right
		SideBySide(false,false,false,Portrait,false,false),				// two player game with side by side seating
		FaceToFaceLandscapeTop(false,false,true,null,true,false),		// two player,  across the top and bottom
		FaceToFaceLandscapeSide(false,false,true,FaceToFaceLandscapeTop,false,false),			// two player, over and under on the side

		LeftCornerTall(false,true,true,null,true,false),
		LeftCornerWide(false,true,true,LeftCornerTall,false,true),
		LeftCorner(false,true,true,LeftCornerWide,false,false),
		
		RightCornerTall(false,true,true,null,true,false),
		RightCornerWide(false,true,true,RightCornerTall,false,true),
		RightCorner(false,true,true,RightCornerWide,false,false),
		
		FaceToFacePortraitSide(true,false,true,null,false,false),
		FaceToFacePortrait(true,false,true,FaceToFacePortraitSide,false,false),
		RightEnd(true,true,false,null,false,false),
		ThreeLeftLW(false,true,true,null,false,true),
		ThreeLeftL(false,true,true,ThreeLeftLW,false,true),
		ThreeRightLW(false,true,true,null,false,true),
		ThreeRightL(false,true,true,ThreeRightLW,false,true),
		ThreeAroundLeft(false,true,true,null,false,false),
		ThreeAroundRight(false,true,true,null,false,false),
		ThreeAroundLH(false,true,true,ThreeAroundLeft,false,false),
		ThreeAroundL(false,true,true,ThreeAroundLH,false,false),
		ThreeAroundRH(false,true,true,ThreeAroundRight,false,false),
		ThreeAroundR(false,true,true,ThreeAroundRH,false,false),
		ThreeWideLeft(false,true,true,null,false,true),
		ThreeWide(false,true,true,ThreeWideLeft,false,true),
		ThreeAcrossLeftCenter(false,false,true,null,false,true),
		ThreeAcrossLeft(false,false,true,ThreeAcrossLeftCenter,false,true),
		ThreeAcross(false,false,true,ThreeAcrossLeft,false,true),
		FourAroundEdgeRect(false,true,true,null,false,false),
		FourAroundW(false,true,true,FourAroundEdgeRect,false,false),
		FourAround(false,true,true,FourAroundW,false,false),
		FourAroundUW(false,true,true,null,false,true),
		FourAroundU(false,true,true,FourAroundUW,false,true),
		FourAcrossEdge(false,false,true,null,false,true),
		FourAcross(false,false,true,FourAcrossEdge,false,true),
		FiveAroundEdge(false,true,true,null,false,true),
		FiveAroundEdgeFirst(false,true,true,FiveAroundEdge,false,true),
		FiveAround(false,true,true,FiveAroundEdgeFirst,false,true),
		FiveAround1EdgeCenter(false,true,true,null,false,true),
		FiveAround1Edge(false,true,true,FiveAround1EdgeCenter,false,true),
		FiveAcrossEdge(false,false,true,null,false,true),
		FiveAcross(false,false,true,FiveAcrossEdge,false,true),
		SixAroundEdge(false,true,true,null,false,true),
		SixAround(false,true,true,SixAroundEdge,false,true),
		SixAcross(false,false,true,null,false,true),
		// seating charts not represented in the seating viewer
		
		Landscape(false,false,false,null,false,false),		// down, with spare rects below
		Across(false,false,false,null,false,false),			// across, with spare rects at the right
		Portrait2X(true,false,false,null,false,false),		// two column portrait, spare rects at right
		Landscape2X(false,false,false,null,false,false),	// two column landscape, spare rects below
		Portrait3X(true,false,false,null,false,false),		// three column portrait, spare rects at right
		Landscape3X(false,false,false,null,false,false), 
		;
		DefinedSeating(boolean port,boolean ar,boolean ftf,DefinedSeating alter,boolean ponly,boolean lonly)
		{	portrait = port;
			seatingFaceToFace = ftf;
			seatingAround = ar;
			alternate=alter;
			portraitOnly = ponly;
			landscapeOnly = lonly;
		}
		boolean portrait;
		boolean seatingAround;
		boolean seatingFaceToFace;
		public boolean portraitOnly;
		public boolean landscapeOnly;
		public DefinedSeating alternate;
		public boolean seatingPortrait() { return(portrait); }
		public boolean seatingAround() { return(seatingAround); }
		public boolean seatingFaceToFace() { return(seatingFaceToFace); }
		public boolean seatingAssigned() { return(seatingFaceToFace||seatingAround); }
		
	}
	static String SeatingChartStrings [] = {
			
	};
	static String SeatingChartStringPairs[][] =
{
		{BlankExplanation,"prepare a reviewer for games which\n were played online at Boardspace.net\nor locally on this device"
		},
		{OnePlayerExplanation,"Set up to play a solo game\n on this device"},
		{TwoPlayerExplanation,"Set up to play a 2 player game\n sharing this device"},
		{ThreePlayerExplanation,"Set up to play a 3 player game\n sharing this device"},
		{FourPlayerExplanation,"Set up to play a 4 player game\n sharing this device"},
		{FivePlayerExplanation,"Set up to play a 5 player game\n sharing this device"},
		{SixPlayerExplanation,"Set up to play a 6 player game\n sharing this device"},
};
	public static void putStrings()
	{	InternationalStrings.put(SeatingChartStrings);
		InternationalStrings.put(SeatingChartStringPairs);
	}
	
}
