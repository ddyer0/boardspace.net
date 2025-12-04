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
package mutton;


import java.awt.*;
import javax.swing.JCheckBoxMenuItem;

import common.GameInfo;

import online.common.*;
import lib.Graphics;
import lib.Image;
import lib.CellId;
import lib.ChatInterface;
import lib.DefaultId;
import lib.ExtendedHashtable;
import lib.G;
import lib.GC;
import lib.HitPoint;
import lib.LFrameProtocol;
import lib.SoundManager;
import lib.TextButton;
import lib.Tokenizer;
import online.game.*;
import online.game.sgf.sgf_node;
import online.game.sgf.sgf_property;
import online.search.SimpleRobotProtocol;


/*
TO DO:
* = fixed, not verified
- = fix not working
+ = fix is verified.
	11) Treat farmer's bid process as assigning points to self?
	13)*When random robot player is farmer, and wolf must pass because
	    of no victims to eat (all wolves isolated), then human player
	    gets to play farmer's turn, too...
	14)*Fix random number usage
	16) "Graphics bug" with Avatar

   Other fixes:
*/
public class MuttonGameViewer extends commonCanvas implements MuttonConstants
{
	static final long serialVersionUID = 1000;

	// The Graphics helper that manages the images.
	private MuttonGraphicsHelper mg;

	// colors
	private Color HighlightColor = new Color(0.2f, 0.95f, 0.75f);
	private Color rackBackGroundColor = new Color(93, 200, 110);
	private Color boardBackgroundColor = new Color(93, 200, 110);
	

	// The colors used to draw the background of the history display, depending on
	// the state of the associated sheep.
	private Color [] historyColumnColors = new Color [3];

	// Images, shared among all instances of the class so loaded only once
	private static Image[] textures = null; // background textures

	// private state
	private MuttonGameBoard myBoard = null; //the board from which we are displaying
	private int CELLSIZE; //size of the layout cell
 
	// addRect is a service provided by commonCanvas, which supports a mode
	// to visualize the layout during development.  Look for "show rectangles"
	// in the options menu.
	private TextButton swapButton = addButton(SWAP,GameId.HitSwapButton,SwapDescription,
			HighlightColor, rackBackGroundColor);
	private Rectangle historyRect    = addRect("historyRect");
	private Rectangle wolfTargetRect = addRect("wolfTargetRect");
	private Rectangle currScoreRect  = addRect("currScoreRect"); 
	private Rectangle UpArrowRect    = addRect("UpArrowRect");
	private Rectangle DownArrowRect  = addRect("DownArrowRect");
	private Rectangle farmerFaceRect = addRect("FarmerRaceRect");

	// Custom menu items
	private JCheckBoxMenuItem historyDisplayOption = null;    // Menu option to hide the history display
	private boolean showHistory = true;                      // current state

	// These are "magic numbers" for the dimensions of the board & history panel.
	private final static int HEX_SCREEN_WIDTH = 64;
	private final static int HEX_SCREEN_HEIGHT = 48;
	private final static int HEX_SCREEN_VERTICAL_SPACING = 36;
	private final static int BOARD_INSET = 10;
	private final static int HISTORY_CELL_WIDTH = 20;
	private final static int HISTORY_CELL_HEIGHT = 18;
	private final static int HISTORY_HEADER_HEIGHT = 30;
//	private final static int HISTORY_PANEL_NUM_ROWS = 11; /// 5;

	private double boardScale = 1.0;
	private double historyPanelScale = 1.0;

	// The turn number of the first row that is displayed on the history panel
///	private final static int historyFirstRow = 1;

	// These are used animate the wolf on the screen for the farmer and
	// spectators while the wolf player is hiding & choosing meals.  The
	// index indicates which wolf view is drawn.  The timer is used to slow
	// down the animation update to only once every ANIMATED_WOLF_UPDATE_FREQUENCY
	// times the system calls getBoardCoords().
	private int animatedWolfDisplayIndex;
	private int animatedWolfDisplayTimer;
	private static final int ANIMATED_WOLF_UPDATE_FREQUENCY = 2;

	// A flag that controls whether the farmer play can select more than one animal
	// to shoot.  If disabled, then clicking on an animal to shoot will result in
	// the currently targeted one being deselected first.  This is to prevent
	// accidentaly rages, which end the game for the farmer.
	private boolean rageEnabled = false;

	// A cell that purposely doesn't exist on the board.
	private MuttonCell nonBoardCell = new MuttonCell (-1, -1, CELL_NOT_EXIST);

	// The keys used for the different text of the "Done" button
	private String [] DoneButtonTextKeys = {
		"Pass", "Shoot", "Rage",
		"Hide", "Eat", "Done"
	};
	private final static int DONE_BTN_PASS_KEY_INDEX  = 0;
	private final static int DONE_BTN_SHOOT_KEY_INDEX = 1;
	private final static int DONE_BTN_RAGE_KEY_INDEX  = 2;
	private final static int DONE_BTN_HIDE_KEY_INDEX  = 3;
	private final static int DONE_BTN_EAT_KEY_INDEX   = 4;
	private final static int DONE_BTN_DONE_KEY_INDEX  = 5;

	// Preload the images into memory for use by drawing routines.
	// Only the background textures are managed by this; other images are managed by
	// the MuttonGraphicsHelper object.
	public void preloadImages () {
		if (textures == null) {
			// images and textures are static variables, so they're shared by
			// the entire class and only get loaded once.  Special synchronization
			// tricks are used to make sure.
			textures = loader.load_images(ImageDir,TextureNames);
			SoundManager.preloadSounds(Sounds);
		}
		gameIcon = textures[ICON_INDEX];
	}

	/**
	 * This is the main initialization that is called only once by the
	 * boardspace system.
	 *
	 *@param info   This hashtable contains information from the environment.
	 **/
	public void init (ExtendedHashtable info,LFrameProtocol frame) {
		super.init(info,frame);
		// use_grid = reviewer;  // use this to turn the grid letters off by default

		// Load the graphics helper module.
		mg = MuttonGraphicsHelper.getInstance(this);

		// Initialize the custom menu options
		historyDisplayOption = myFrame.addOption("Display chart", true, deferredEvents);

		// Create the board
		myBoard = new MuttonGameBoard (info.getString(GameInfo.GAMETYPE, Mutton_INIT),
		                               sharedInfo.getInt(OnlineConstants. RANDOMSEED , -1));
        //useDirectDrawing(); // not tested yet
		doInit(false);

		// Initialize custom stuff
		historyColumnColors[SHEEP_STATUS_ALIVE]      = new Color (169, 204, 135);
		historyColumnColors[SHEEP_STATUS_DEAD_SHEEP] = new Color (233, 140, 135);
		historyColumnColors[SHEEP_STATUS_DEAD_WOLF]  = new Color (169, 142, 201);

		animatedWolfDisplayIndex = 0;
		animatedWolfDisplayTimer = 0;
	}

	/**
	 * Initialize the system back to the "Start" position.  This is called
	 * when about to load a game or when the "WayBack" button is pressed.
	 * The Player's list and even game subtype may change.
	 *
	 * @param preserve_history   Used to determine if the history should be
	 *                           preserved or not.
	 **/
	public void doInit (boolean preserve_history) {
		super.doInit(preserve_history);
		myBoard.doInit(myBoard.gametype, myBoard.randomKey);
		if(!preserve_history)
		{
       	 startFirstPlayer();
         PerformAndTransmit("Board_State " + myBoard.getBoardLayoutString(), false, replayMode.Live);
		}
	}
 

	/**
     * calculate a metric for one of three layouts, "normal" "wide" or "tall",
     * which should normally correspond to the area devoted to the actual board.
     * these don't have to be different, but devices with very rectangular
     * aspect ratios make "wide" and "tall" important.  
     * @param width
     * @param height
     * @param wideMode
     * @param tallMode
     * @return a metric corresponding to board size
     */
    public int setLocalBoundsSize(int width,int height,boolean wideMode,boolean tallMode)
    {	
        int chatHeight = selectChatHeight(height);
        int ncols = tallMode ? 42 : wideMode ? 66: 54; // more cells wide to allow for the aux displays
        int nrows = tallMode ? 40 : wideMode ? 27 : 35 ;  
        int cellw = width / ncols;
        int cellh = (height-chatHeight) / nrows;
        
        CELLSIZE = Math.max(2,Math.min(cellw, cellh)); //cell size appropriate for the aspect ratio of the canvas

        return(CELLSIZE);
    }

    public void setLocalBoundsWT(int x, int y, int width, int height,boolean wideMode,boolean tallMode)
    {   
        int chatHeight = selectChatHeight(height);
        boolean noChat = (chatHeight==0);
        int C2 = CELLSIZE/2;
 		// The whole canvas area to draw on.
		G.SetRect(fullRect,x,y, width, height);
		// The main board (with the sheep/wolves on it.)
		int boardW = CELLSIZE * 40;
		int stateH = CELLSIZE+C2;
		G.SetRect(boardRect, BOARD_INSET,chatHeight + stateH+C2,boardW, (boardW * 264) / 448);

		// The state display that indicates what actions should happen next.
		int stateY = chatHeight + BOARD_INSET / 2;
		int goalW = CELLSIZE*5;
		G.SetRect(noChatRect, G.Right(boardRect)-stateH, stateY, stateH,stateH);
		placeRow(G.Left(noChatRect)-goalW,stateY,goalW,stateH,goalRect);
		setProgressRect(progressRect,goalRect);
		G.SetRect(stateRect, BOARD_INSET,stateY ,	G.Left(goalRect)-BOARD_INSET	,stateH);


		boardScale = G.Width(boardRect) / 448.0;

 
		{
            commonPlayer pl0 =getPlayerOrTemp(0);
            commonPlayer pl1 = getPlayerOrTemp(1);
            Rectangle p0time = pl0.timeRect;
            Rectangle p1time = pl1.timeRect;
            Rectangle p0anim = pl0.animRect;
            Rectangle p1anim = pl1.animRect;
            Rectangle firstPlayerRect = pl0.nameRect;
            Rectangle secondPlayerRect = pl1.nameRect;
            Rectangle firstPlayerPicRect = pl0.picRect;
            Rectangle secondPlayerPicRect = pl1.picRect;
            Rectangle p0aux = pl0.extraTimeRect;
            Rectangle p1aux = pl1.extraTimeRect;
            int logHeight = Math.max(chatHeight,CELLSIZE*5);
			// first player name
			G.SetRect(firstPlayerRect, 
					tallMode ? G.Left(boardRect) : G.Right(boardRect) + CELLSIZE,
					tallMode ? G.Bottom(boardRect)+CELLSIZE 
							: !tallMode&noChat ? logHeight+C2 : G.Top(boardRect)-C2,
					 CELLSIZE * 6, (3 * CELLSIZE) / 2);
			// first player portrait
			G.SetRect(firstPlayerPicRect,
						G.Left( firstPlayerRect), G.Bottom(firstPlayerRect),
						CELLSIZE * 6, CELLSIZE * 6);
			// time display for first player
			G.SetRect(p0time, G.Right(firstPlayerRect) + (3 * CELLSIZE) / 2,G.Top(firstPlayerRect) - (C2),
					CELLSIZE * 4, 3*C2);
			G.AlignLeft(p0aux, G.Bottom(p0time), p0time);
			// first player "i'm alive" animation ball
			G.SetRect(p0anim, G.Left(p0aux),G.Bottom(p0aux), CELLSIZE, CELLSIZE);

			// second player name
			G.AlignXY(secondPlayerRect,
					tallMode|wideMode ? G.Right(p0time)+CELLSIZE : G.Left( firstPlayerRect),
					tallMode|wideMode ? G.Top(firstPlayerRect) : G.Bottom(boardRect) - G.Height(firstPlayerRect),
					firstPlayerRect);
			// second player portrait
			G.AlignXY(secondPlayerPicRect,G.Left( secondPlayerRect),
					tallMode|wideMode ? G.Bottom(secondPlayerRect) : G.Top(secondPlayerRect) - G.Height(firstPlayerPicRect),
					firstPlayerPicRect);
			

			// time display for second player
			G.AlignXY(p1time, 
					G.Right(secondPlayerRect) + CELLSIZE,
					G.Top( secondPlayerRect) - (wideMode|tallMode ? 0 : 3*CELLSIZE),
					p0time);
			// second player "i'm alive" animation ball
			G.AlignLeft(p1aux, G.Bottom(p1time), p1time);

			G.AlignXY(p1anim, G.Left(p1time),G.Bottom(p1aux),p0anim);

			// "done" rectangle, should always be visible, but only active
			// when a move is complete.
			int doneW =CELLSIZE*6-C2;
			int doneH = CELLSIZE*2;
			G.SetRect(doneRect,
					G.Right(boardRect)-doneW,
					G.Bottom(boardRect)-doneH ,
					doneW,
					doneH);
		
			int hLeft = wideMode ? G.Right(boardRect)+CELLSIZE : BOARD_INSET;
			int hTop = wideMode ? G.Bottom(firstPlayerPicRect)+C2 : G.Bottom( boardRect) + BOARD_INSET+(tallMode ? CELLSIZE*9 : 0);
			// The history panel that shows the sheep status.
			G.SetRect(historyRect, 
					hLeft,
					hTop,
					wideMode ? width-hLeft-C2 : G.Width(boardRect) + (tallMode ? 0 : CELLSIZE * 7),
					height-hTop-C2);

			historyPanelScale = ((double) G.Width(historyRect)) / (HISTORY_CELL_WIDTH * 26);
			int logY = tallMode&& noChat ? G.Bottom(doneRect)+C2 : y;
			G.SetRect(logRect, 
						(tallMode?G.Right(p1time) : G.Right(boardRect))+C2,
						logY ,
						13 * CELLSIZE-C2, 
						tallMode && noChat ? G.Top(historyRect)-logY-C2 : logHeight);

			G.SetRect(chatRect,x+C2,y,G.Width(boardRect), chatHeight);

			//this sets up the "vcr cluster" of forward and back controls.
			SetupVcrRects(
			    C2, G.Bottom(boardRect) - (4 * CELLSIZE),
			    CELLSIZE * 7, 3 * CELLSIZE);


		// The "swap" rectangle is only visible when the wolf player is hiding
		// the wolves.  It is only active when there are no wolves on the board.
		G.AlignLeft(swapButton,
				G.Top(doneRect)-doneH-C2,
				doneRect) ;
		}

		// The rectangle where the number of dead sheep required for a wolf
		// win is displayed.
		int wolfSize = CELLSIZE*3;
		G.SetRect(wolfTargetRect, G.Right(boardRect) - CELLSIZE * 5,
				G.Top( boardRect) + BOARD_INSET,
				 wolfSize, wolfSize);

		// Rectangles for the Up & Down arrows that the farmer uses to set
		// the goal for the wolf player.
		G.SetRect(UpArrowRect, G.Right(wolfTargetRect)+C2,G.Top(wolfTargetRect),
				 CELLSIZE,C2);

		G.AlignLeft(DownArrowRect, G.Bottom(UpArrowRect)+CELLSIZE,
				 UpArrowRect);

		// The display of the current number of dead sheep.
		G.AlignXY(currScoreRect,G.Left( wolfTargetRect)+ CELLSIZE,
				G.Top(doneRect) - wolfSize-C2,
				wolfTargetRect);

		// The farmer's face that is used to enable rage.
		G.AlignXY(farmerFaceRect, G.Right(boardRect) - (CELLSIZE * 5) / 2,
				G.Bottom(wolfTargetRect) + CELLSIZE*2,
				wolfTargetRect);

		positionTheChat(chatRect,Color.white,Color.white);
		generalRefresh();
	}

	//
	// sprites are normally a game piece that is "in the air" being moved
	// around.  This is called when dragging your own pieces, and also when
	// presenting the motion of your opponent's pieces, and also during replay
	// when a piece is picked up and not yet placed.  While "obj" is nominally
	// a game piece, it is really whatever is associated with b.movingObject()
	//
	public void drawSprite (Graphics gc, int obj, int xp, int yp) {
		// For Mutton, the sprites are the animals that are dragged during
		// relocation.
		// The value of obj is the same as that returned by myBoard.movingObjectIndex().
		// This is the id of the moving animal.
		// Other calls are made to get the wolf & rotation values to use to draw
		// the sprite correctly.
		if (obj >= 0) {
			// There really is a sheep being dragged.
			int sheepId = obj;
			int displayRotation = myBoard.getMovingAnimalDisplayRotation();
			boolean isAlive = true;  // The only animals that are dragged are alive.
			int screenX = xp;
			int screenY = yp;

			// The dragged picture is only a wolf if the game is over or the
			// current player is the wolf player
			boolean isWolf = myBoard.isMovingAnimalWolf() &&
			     (myBoard.isWolfPlayer(getActivePlayer().boardIndex) || mutable_game_record);

			// Paint the sheep/wolf image
			mg.paintImage( gc, fullRect, screenX, screenY,
			               isWolf ? IMG_WOLVES : IMG_SHEEP,
			               displayRotation,
			               isAlive ? 0 : 1,
			               boardScale);
			// Paint the letter
			mg.paintImage( gc, fullRect, screenX, screenY,
			               isAlive ? IMG_ALPHABET_BLACK : IMG_ALPHABET_RED,
			               sheepId, 0,
			               boardScale);
		}
	}

    // also related to sprites,
    // default position to display static sprites, typically the "moving object" in replay mode
    //public Point spriteDisplayPoint()
    //{	BoardProtocol b = getBoard();
    //	int celloff = b.cellSize();
    //	return(new Point(G.Right(boardRect)-celloff,G.Bottom(boardRect)-celloff));
    //}  

	/**
	 * Overriding this method so that we can hide the user's mouse movements
	 * when the wolf player is choosing which of the 4 sheep will be wolves.
	 * And we can do "animation" of the wolf while this is happening so that
	 * the farmer player sees that the wolf player is actually doing something.
	 */
	public String encodeScreenZone(int x, int y,Point p) {
		if (((myBoard.getState() == MuttonState.WOLF_HIDING_STATE) ||
		     (myBoard.getState() == MuttonState.WOLF_CHOOSING_MEAL_STATE)) &&
		    !mutable_game_record &&
		    myBoard.isWolfPlayer(getActivePlayer().boardIndex) &&
		    (boardRect.contains(x, y) || historyRect.contains(x, y))) {
			// The game is in wolf hiding state and is not over and the
			// mouse is inside either the boardRect or the historyRect,
			// so hide the cursor

			animatedWolfDisplayTimer += 1;
			if (animatedWolfDisplayTimer == ANIMATED_WOLF_UPDATE_FREQUENCY) {
				animatedWolfDisplayTimer = 0;
				animatedWolfDisplayIndex = (animatedWolfDisplayIndex + 1) % 6;
			}
			G.SetLeft(p, 0);
			G.SetTop(p, animatedWolfDisplayIndex);
			return(HIDDEN);
		}
		return super.encodeScreenZone(x, y,p);
	}

	/**
	 * Overriding this method so that we can "animate" a wolf while the
	 * wolf player is hiding & choosing a meal.
	 */
	public Point decodeScreenZone(String z,int x, int y) {
		if (HIDDEN.equals(z)) {
			// This is indication that the wolf player is moving within
			// the area with a hidden wolf, so use the Y coordinate as
			// the animation updated value.
			animatedWolfDisplayIndex = y;
			return new Point (0, 0);
		}

		return super.decodeScreenZone(z,x, y);
	}

	/*
	 * Compute the screen x,y coordinates for the center of a box of
	 * the mutton history panel.  The given point will be relocated to point to
	 * the correct location.
	 *
	 * Only the "contents" rectangles are included in this calculations.  The
	 * headers along the top with the sheep letters and the turn number on the
	 * left are *not* included.  So, for example, calling this with coordinate
	 * (0, 0) will return the center of the cell for the status of sheep A
	 * during turn 1.
	 *
	 * @param screenPt   The point to be relocated.
	 * @param col        The logical column number within the history panel.
	 * @param row        The logical row number within the history panel.
	 * @param hCenter    If true, the point is centered in the given column.
	 *                   Otherwise, the point is the left edge of the given column.
	 * @return the screenPt given.
	 */
	private Point setHistoryPanelPoint (Point screenPt, int col, int row, boolean center) {
		int xPos = ((col * 2 + (center ? 1 : 0)) * G.Width(historyRect)) / 52;
		int yPos = HISTORY_HEADER_HEIGHT + (row * HISTORY_CELL_HEIGHT) + (HISTORY_CELL_HEIGHT / 2);

		G.SetLeft(screenPt,xPos);
		G.SetTop(screenPt,yPos);
		return screenPt;
	}

	/*
	 * Given a point on the screen, find the column of the history panel that
	 * contains that point and return it.  If the point is not within the
	 * history panel, then this will return -1. 
	 */
	private int getContainingColumn (HitPoint highlight, int rowsInHistory) {
		if (highlight != null) {
			// Determine if the point is within the history rectangle vertically.
			int yoff = G.Top(highlight) -G.Top( historyRect);
			int rowsToDraw = Math.max(rowsInHistory, 1);
			int historyHeight = HISTORY_HEADER_HEIGHT + (rowsToDraw * HISTORY_CELL_HEIGHT);
			if ((yoff >= 0) && (yoff <= historyHeight)) {
				// Determine if the point is within the history rectangle horizontally.
				int xoff = G.Left(highlight) - G.Left(historyRect);
				if (xoff > 0) {
					return (xoff * 26) / G.Width(historyRect);
				}
			}
		}

		return -1;
	}

	/*
	 * Given a point on the screen, find the cell on the board that contains that
	 * point and return it.  If the point is not within the board, then this will
	 * return a cell that doesn't exist on the board.
	 */
	private MuttonCell getContainingCell (HitPoint highlight) {
		if (highlight == null) {
			// If there is no highlight point, then we're not on the board.
			return nonBoardCell;
		}

		// Compute the offset within the board of the point.
		int xoff = G.Left(highlight) - G.Left(boardRect);
		int yoff = G.Top(highlight) - G.Top(boardRect);

		// Scale the point from screen scale to "real" scale
		xoff = (int) ( xoff / boardScale);
		yoff = (int) ( yoff / boardScale);

		// Calculate the initial row that the point is in.
		int row = (yoff / HEX_SCREEN_VERTICAL_SPACING);

		// Even rows are slid over half a hex.
		if ((row & 0x01) == 0) {
			xoff -= (HEX_SCREEN_WIDTH / 2);
		}

		// Calculate the initial column that the point is in.
		int col = (xoff / HEX_SCREEN_WIDTH);

		// Determine if the point is within the upper portion of the triangle
		// at the top of a hexagon that may make the point in the row above.
		// (Note: This calculation uses magic numbers that work for the current
		//        image size of 64x48 pixels.  I'm too lazy to make this
		//        generic for now.)
		int interSpaceY = (yoff % HEX_SCREEN_VERTICAL_SPACING);
		if (interSpaceY < 12) {
			int interSpaceX = (xoff % HEX_SCREEN_WIDTH);
			if (8 * interSpaceY + 3 * interSpaceX < 96) {
				// Point is in the upper left corner and needs to move up a row
				// and over a hex.
				row -= 1;
				if ((row & 0x01) == 0) {
					col -= 1;
				}
			} else if (8 * interSpaceY - 3 * interSpaceX < -96) {
				// Point is in the upper right corner and needs to mover up a
				// row and over a hex.
				row -= 1;
				if ((row & 0x01) != 0) {
					col += 1;
				}
			}
		}

		// Return the cell that the calculated location
		return myBoard.getCell(col, row);
	}

	/*
	 * Calculate the x-coordinate on the screen for the center of the cell at
	 * (col, row) on the board.
	 */
	private int calcScreenX (int col, int row) {
		// Start in the center of the hexagon that is in the right column.
		int x = (HEX_SCREEN_WIDTH / 2) + (col * HEX_SCREEN_WIDTH);

		// Even rows are shifted over half a hexagon to the right.
		if ((row & 0x01) == 0) {
			x += (HEX_SCREEN_WIDTH / 2);
		}

		// Scale to screen scale
		x = (int) ( x * boardScale);

		return x;
	}

	/*
	 * Calculate the x-coordinate on the screen for the center of the cell at
	 * (col, row) on the board.
	 */
	private int calcScreenY (int col, int row) {
		int y = (HEX_SCREEN_HEIGHT / 2) + (row * HEX_SCREEN_VERTICAL_SPACING);

		// Scale to screen scale
		y = (int) ( y * boardScale);

		return y;
	}

	/*
	 * Draw the Mutton specific board contents.
	 * We're also called when not actually drawing, to determine if the mouse
	 * is pointing at something which might allow an action.  Either gc or
	 * highlight might be null, but not both.
	 */
	private void drawMuttonElements (Graphics gc, MuttonGameBoard gb, HitPoint highlight) {
		// Determine if the mouse pointer is within the board and/or the history panel.

		// Find the cell that contains the highlight point on the board.
		MuttonCell closestCell = getContainingCell(highlight);
		if (!gb.isValidMouseTarget(closestCell)) {
			closestCell = nonBoardCell;
		}
		int historyColumn = closestCell.getSheepId();

		if ((highlight != null) && (closestCell.isOnBoard())) {
			// Note what we hit on the board: row, col, and cell
			boolean empty = closestCell.isEmpty();
			highlight.hitCode = empty ? MuttonId.HIT_EMPTY_BOARD_LOCATION :  MuttonId.HIT_BOARD_LOCATION;
			highlight.hitObject = closestCell;
			highlight.col = (char) closestCell.getCol();
			highlight.row = closestCell.getRow();
		}

		// Find the column of the history panel that contains the highlight point.
		// For game purposes, selecting an animal via the history column is
		// treated identically to the mouse being on the hexagon that contains
		// the animal of the same id.  However, if playing with the "shotgun"
		// variant, some animals are removed from the board during play.  So
		// there are columns of the history panel that may be moused over that
		// don't correspond to a cell on the board.  Therefore, we can't treat
		// the history panel truly identically to the board.
		if (historyColumn < 0) {
			historyColumn = getContainingColumn(highlight, gb.getHistorySize());
			if ((highlight != null) && (historyColumn >= 0)) {
				// Note what we hit in the history table
				closestCell = gb.findCellWithSheep(historyColumn);
				highlight.hitCode = closestCell.isOnBoard() ?  MuttonId.HIT_BOARD_LOCATION :  MuttonId.HIT_HISTORY_COLUMN;
				highlight.hitObject = closestCell;
				highlight.col = 0;
				highlight.row = historyColumn;
			}
		}

		// Next check if we're in other misc. rects on the board.
		int hitArrow = -1;
		boolean hitFarmer = false;
		if ((historyColumn < 0) && (highlight != null)) {

			// Check if we're in the up or down arrow rects for the target
			// number of dead sheep for the wolf player to win.
			if (gb.getState() == MuttonState.FARMER_CONFIGURING_BOARD) {
				if (G.pointInRect(highlight,UpArrowRect) && (gb.getWolfWinTarget() < 22)) {
					highlight.hitCode =  MuttonId.HIT_WOLF_UP_ARROW;
					highlight.hitObject = null;
					highlight.col = 0;
					highlight.row = 0;
					hitArrow = 1;
				} else if (G.pointInRect(highlight,DownArrowRect) && (gb.getWolfWinTarget() > 1)) {
					highlight.hitCode =  MuttonId.HIT_WOLF_DOWN_ARROW;
					highlight.hitObject = null;
					highlight.col = 0;
					highlight.row = 0;
					hitArrow = 2;
				}
			}

			// Check if we're in the farmer face rect for enabling rage
			if (G.pointInRect(highlight,farmerFaceRect) && (gb.getFarmerId() == getActivePlayer().boardIndex)) {
				highlight.hitCode =  MuttonId.HIT_FARMER_FACE;
				highlight.hitObject = null;
				highlight.col = 0;
				highlight.row = 0;
				hitFarmer = true;
			}
		}


		if (gc != null) {
			drawMuttonBoard(gc, gb, closestCell);
			drawMuttonHistoryPanel(gc, gb, historyColumn);
			drawFarmerFace(gc, gb, hitFarmer);
			if (gb.getFarmerId() == 0) {
				// If the farmer is player 0, then draw the target above and
				// current score below.
				drawWolfTarget(gc, gb, hitArrow, wolfTargetRect);
				drawCurrentScore(gc, gb, currScoreRect);
			} else {
				// If the farmer is player 1, then draw the target below and
				// current score above.
				drawWolfTarget(gc, gb, hitArrow, currScoreRect);
				drawCurrentScore(gc, gb, wolfTargetRect);
			}
		}
	}

	/*
	 * This draws the farmer's face to indicate if rage is enabled or not.
	 */
	private void drawFarmerFace (Graphics gc, MuttonGameBoard gb, boolean highlight) {
		if (gb.getFarmerId() == getActivePlayer().boardIndex) {
			// Draw the correct base image (rage or not) depending on the rage setting
			// in the game board.
			mg.paintImage(gc, boardRect,
			              G.Left(farmerFaceRect) - G.Left(boardRect),G.Top( farmerFaceRect) -G.Top( boardRect),
			              IMG_FARMER_FACES, rageEnabled ? 2 : 1, 0, boardScale);

			// If highlighted, then draw the outline
			if (highlight) {
				mg.paintImage(gc, boardRect,
						G.Left(farmerFaceRect) -G.Left( boardRect), G.Top(farmerFaceRect) - G.Top(boardRect),
				              IMG_FARMER_FACES, 0, 0, boardScale);
			}
		}
	}

	/*
	 * This draws the rectangle that indicates the number of dead sheep that
	 * wolf need to achieve to win.
	 */
	private void drawWolfTarget (Graphics gc, MuttonGameBoard gb, int hitArrow, Rectangle baseRect) {
		int rectCenterX = (G.Width(baseRect) / 2);
		int rectCenterY = (G.Height(baseRect) / 2);

		// Draw the star image.
		mg.paintImage(gc, baseRect, rectCenterX, rectCenterY,
		              IMG_CELL_HIGHLIGHTS, HIGHLIGHT_STAR, 0, boardScale);
		
		// Draw the target number
		mg.paintNumber(gc, baseRect, rectCenterX, rectCenterY, gb.getWolfWinTarget(), boardScale);

		// Draw the up & down arrows, using the highlighted or unhighlighted
		// image depending on if the mouse is in one of them.
		if (gb.getState() == MuttonState.FARMER_CONFIGURING_BOARD) {
			int tgtVal = gb.getWolfWinTarget();
			if (tgtVal < 22) {
				mg.paintImage(gc, UpArrowRect, 0, 0, IMG_ARROWS,
				              (hitArrow == 1) ? 1 : 0, 0, boardScale);
			}
			if (tgtVal > 1) {
				mg.paintImage(gc, DownArrowRect, 0, 0, IMG_ARROWS,
				              (hitArrow == 2) ? 1 : 0, 1, boardScale);
			}
		}

		// Draw the ! symbol, if shotgun mode is enabled
		if (gb.isShotgunEnabled()) {
			mg.paintImage(gc, baseRect, rectCenterX + 5, rectCenterY + 10,
			              IMG_CELL_HIGHLIGHTS, HIGHLIGHT_AS_SCARED, 0, boardScale);
		}
	}

	/*
	 * This draws the rectangle that indicates the current number of dead
	 * sheep on the board.
	 */
	private void drawCurrentScore (Graphics gc, MuttonGameBoard gb, Rectangle baseRect) {
		int deadSheepCount = gb.getDeadSheepCount();

		if (deadSheepCount > 0) {
			int rectCenterX = (G.Width(baseRect) / 2);
			int rectCenterY = (G.Height(baseRect) / 2);

			// Draw the dead sheep image.
			mg.paintImage(gc, baseRect, rectCenterX, rectCenterY,
			              IMG_SHEEP, 4, 1, boardScale);
	
			// Draw the score number
			mg.paintNumber(gc, baseRect, rectCenterX, rectCenterY,
					deadSheepCount, boardScale);
		}
	}

	/*
	 * This method draws the alive and dead sheep & wolves on the board in the
	 * correct locations.
	 *
	 * @param gc           The graphics context to draw on.
	 * @param gb           The game board to retrieve the state from.
	 * @param activeCell   The cell on the board to highlight as the mouse location.
	 */
	private void drawMuttonBoard (Graphics gc, MuttonGameBoard gb, MuttonCell activeCell) {
		// There are four "planes" of drawing used to draw the board.
		// These planes are:
		//   1) A "pre-sheep" highlight used to indicate the last moves
		//   2) The highlight to indicate which cell the mouse is in.
		//   3) The sheep & it's letter.
		//   4) A "post-sheep" highlight used to draw icons that indicate
		//      various statuses.
		// These planes are drawn in that order, placing some items "behind"
		// others on the screen.

		int screenX, screenY;

		// Draw the pre-sheep highlights on the board (including the blood
		// highlight if this is a dead animal.)
		for (int r = 0; r < BOARD_ROWS; r++) {
			for (int c = 0; c < BOARD_COLS; c++) {
				MuttonCell cell = gb.getCell(c, r);
				if (!cell.isEmpty() && !cell.isAlive() && cell.isOnBoard()) {
					screenX = calcScreenX(c, r);
					screenY = calcScreenY(c, r);
					mg.paintImage(gc, boardRect, screenX, screenY, IMG_BLOOD_HIGHLIGHT,
					              0, 0, boardScale);
				}

				int highlightCode = cell.getHighlightCode(PRE_ANIMAL_HIGHLIGHT);
				if (highlightCode != HIGHLIGHT_NONE) {
					screenX = calcScreenX(c, r);
					screenY = calcScreenY(c, r);
					mg.paintImage(gc, boardRect, screenX, screenY, IMG_CELL_HIGHLIGHTS,
					              highlightCode, 0, boardScale);
				}
			}
		}

		// If the active cell is on the board, then draw the active highlight image
		if (activeCell.isOnBoard()) {
			int c = activeCell.getCol();
			int r = activeCell.getRow();
			screenX = calcScreenX(c, r);
			screenY = calcScreenY(c, r);
			mg.paintImage(gc, boardRect, screenX, screenY, IMG_ACTIVE_HIGHLIGHT, 0, 0, boardScale);
		}

		// Go through the board, drawing the sheep & wolves that are on the board.
		// We only draw the real wolves when either
		//    a) This client is the wolf player
		// or b) The game is over.
		// Otherwise, we draw all animals as sheep.
		int pictureIdForWolves = 
		     (gb.isWolfPlayer(getActivePlayer().boardIndex) || mutable_game_record) ?
		        IMG_WOLVES : IMG_SHEEP;
		for (int r = 0; r < BOARD_ROWS; r++) {
			for (int c = 0; c < BOARD_COLS; c++) {
				MuttonCell cell = gb.getCell(c, r);
				int sheepId = cell.getSheepId();
				if (sheepId >= 0) {
					screenX = calcScreenX(c, r);
					screenY = calcScreenY(c, r);
					boolean isAlive = cell.isAlive();
					int animalImageId = cell.isWolf() ?
						     (isAlive ? pictureIdForWolves : IMG_WOLVES) :
						     IMG_SHEEP;

					// Paint the sheep/wolf image
					mg.paintImage(gc, boardRect, screenX, screenY,
					              animalImageId,
					              cell.getDisplayRotation(),
					              isAlive ? 0 : 1,
					              boardScale);
					// Paint the letter
					mg.paintImage(gc, boardRect, screenX, screenY,
					              isAlive ? IMG_ALPHABET_BLACK : IMG_ALPHABET_RED,
					              sheepId, 0,
					              boardScale);
				}
			}
		}

		// Draw the post-sheep highlights on the board
		for (int r = 0; r < BOARD_ROWS; r++) {
			for (int c = 0; c < BOARD_COLS; c++) {
				MuttonCell cell = gb.getCell(c, r);
				int highlightCode = cell.getHighlightCode(POST_ANIMAL_HIGHLIGHT);
				if (highlightCode != HIGHLIGHT_NONE) {
					screenX = calcScreenX(c, r);
					screenY = calcScreenY(c, r);
					mg.paintImage(gc, boardRect, screenX, screenY, IMG_CELL_HIGHLIGHTS, highlightCode, 0, boardScale);
				}
			}
		}

		// Draw the animated wolf if we're not the wolf player and the wolf
		// player is doing something that is kept hidden.
		if (!gb.isWolfPlayer(getActivePlayer().boardIndex) &&
		   ((gb.getState() ==MuttonState.WOLF_HIDING_STATE) ||
		    (gb.getState() == MuttonState.WOLF_CHOOSING_MEAL_STATE))
		) {
			screenX = calcScreenX(0, 0) - CELLSIZE * 2;
			screenY = calcScreenY(0, 0) + CELLSIZE;
			mg.paintImage(gc, boardRect, screenX, screenY, IMG_WOLVES, animatedWolfDisplayIndex, 0, boardScale);
		}
	}

	/*
	 * This method draws the history panel that indicates which sheep are still alive,
	 * which are dead sheep and which are dead wolves.
	 * 
	 * @param activeSheep  The column to highlight.
	 */
	private void drawMuttonHistoryPanel (Graphics gc, MuttonGameBoard gb, int activeSheep) {
		// Go through the sheep history and draw the codes for sheep that were
		// eaten & moved.
		Point screenCenterPoint = new Point (0, 0);
		Point screenLeftPoint = new Point (0, 0);
		Point screenRightPoint = new Point (0, 0);
		Point screenPt = new Point (0, 0);

		// Determine the number of rows to draw.
		//    If the history panel is to be drawn, then we draw a minimum of 1 row.
		//    If the history panel is not to be drawn, then we draw 0 rows.
		int rowsToDraw = showHistory ? Math.max(gb.getHistorySize(), 1) : 0;
		int historyHeight = HISTORY_HEADER_HEIGHT + (rowsToDraw * HISTORY_CELL_HEIGHT);

		// First, color the columns by sheep status and label the columns.
		for (int i = 0; i < 26; i++) {
			// Compute the bounds of this column of the history panels
			screenLeftPoint   = setHistoryPanelPoint(screenLeftPoint, i, 0, false);
			screenRightPoint  = setHistoryPanelPoint(screenRightPoint, i+1, 0, false);
			screenCenterPoint = setHistoryPanelPoint(screenCenterPoint, i, 0, true);
			int ulX = G.Left(historyRect) + G.Left(screenLeftPoint);
			int ulY = G.Top(historyRect) + G.Top(screenLeftPoint) - (HISTORY_CELL_HEIGHT / 2) - HISTORY_HEADER_HEIGHT;
			int historyCellWidth = G.Left(screenRightPoint) - G.Left(screenLeftPoint);

			// Paint the column according to the current status of this sheep.
			int status = gb.getSheepStatus(i);
			GC.fillRect(gc,historyColumnColors[status],ulX, ulY, historyCellWidth, historyHeight);

			// Outline the column in black.
			GC.frameRect(gc,Color.black,ulX, ulY, historyCellWidth, historyHeight);

			// If this is the active sheep, then highlight the column
			if (i == activeSheep) {
				GC.frameRect(gc,Color.yellow,ulX+1, ulY+1, historyCellWidth - 2, historyHeight - 2);
				GC.frameRect(gc,Color.yellow,ulX+2, ulY+2, historyCellWidth - 4, historyHeight - 4);
				GC.setColor (gc,Color.black);
			}

			// Draw the sheep name at the top of the column.
			int nameX = G.Left(historyRect) + G.Left(screenCenterPoint) + 1;
			int nameY = ulY + (HISTORY_HEADER_HEIGHT / 2);
			mg.paintImage(gc, fullRect, nameX, nameY, IMG_ALPHABET_BLACK, i, 0, historyPanelScale);

			// Draw the icon over the name that that indicates if it is a dead wolf or dead sheep.
			// (If it's still alive, then this will draw a blank.)
			mg.paintImage(gc, fullRect, nameX, nameY, IMG_HISTORY_DEAD_HEADER, status, 0, historyPanelScale);
		}

		// We only draw more stuff if we have rows to draw.
		if (rowsToDraw > 0) {
			// Paint the horizontal lines across the history panel.
			for (int r = 0; r < rowsToDraw; r++) {
				screenPt = setHistoryPanelPoint(screenPt, 0, r, true);
				int lineY = G.Top(historyRect) + G.Top(screenPt) - (HISTORY_CELL_HEIGHT / 2);
				GC.drawLine(gc,G.Left(historyRect), lineY,G.Right( historyRect), lineY);
			}
	
			// Next, draw each row of the sheep history panel.
			// (We use the actual history size because we can't fetch history
			//  elements that don't exist).
			rowsToDraw = gb.getHistorySize();
			for (int r = 0; r < rowsToDraw; r++) {
				// Draw the history icons for the sheep that was killed this turn.
				MuttonHistoryElement historyEl = gb.getHistoryForTurn(r);
				int eatenSheepId = historyEl.getKilledSheepId();
				screenPt = setHistoryPanelPoint(screenPt, eatenSheepId, r, true);
				G.SetLeft(screenPt,G.Left(screenPt)+G.Left(historyRect)+1);
				G.SetTop(screenPt,G.Top(screenPt)+ G.Top(historyRect)+1);
				mg.paintImage(gc, fullRect, G.Left(screenPt), G.Top(screenPt), IMG_HISTORY_DEAD_SHEEP, 0, 0, historyPanelScale);
	
				// Draw the history icons for the sheep that are suspect this turn.
				int [] suspectSheep = historyEl.getSuspectSheep();
				for (int i=0; i<suspectSheep.length; i++) {
					screenPt = setHistoryPanelPoint(screenPt, suspectSheep[i], r, true);
					G.SetLeft(screenPt,G.Left(screenPt)+G.Left(historyRect)+1);
					G.SetTop(screenPt, G.Top(screenPt)+G.Top(historyRect)+1);
					mg.paintImage(gc, fullRect, G.Left(screenPt), G.Top(screenPt), IMG_HISTORY_SUSPECT_SHEEP, 0, 0, historyPanelScale);
				}
			}
		}
	}


	/*
	 * Draw the main window and the things on it.  This is also used to
	 * determine what the current mouse pointer is pointing at.
	 *
	 * If gc != null, then actually draw on it.
	 * If selectPos != null, then as you draw (or pretend to draw) notice if
	 * something can happen given the current position of the mouse.  Care must
	 * be taken to consider if a click really ought to be allowed, considering
	 * spectator status, use of the scroll controls, if some board token is
	 * already actively moving, and if the game is active or over.
	 *
	 * This dual purpose (draw, and notice mouse sensitive areas) tends to make
	 * the code a little complicated, but it is the most reliable way to make
	 * sure the mouse logic is in sync with the drawing logic.
	 *
	General GUI checklist

	vcr scroll section always tracks, scroll bar drags
	lift rect always works
	zoom rect always works
	drag board always works
	pieces can be picked or dragged
	moving pieces always track
	stray buttons are insensitive when dragging a piece
	stray buttons and pick/drop are inactive when not on turn
	 */
	public void redrawBoard(Graphics gc, HitPoint selectPos) {
		MuttonGameBoard gb = (MuttonGameBoard) disB(gc);
		MuttonState state = gb.getState();
		boolean moving = hasMovingObject(selectPos);

		// If it is not our move, we can't click on the board or related supplies.
		// we accomplish this by supressing the highlight pointer.
		HitPoint ourTurnSelect = OurMove() ? selectPos : null;

		// Even if we can normally select things, if we have already got a piece
		// moving, we don't want to hit some things, such as the vcr group
		HitPoint buttonSelect = moving ? null : ourTurnSelect;

		// Hit anytime nothing is being moved, even if not our turn or we are a spectator
		HitPoint nonDragSelect = (moving && !reviewMode()) ? null : selectPos;

		gameLog.redrawGameLog(gc, nonDragSelect, logRect, boardBackgroundColor);
		drawMuttonElements(gc, gb, ourTurnSelect);

		GC.setFont(gc,standardBoldFont());

		// Draw the swap button if we're in the wolf hiding phase and
		// player 0 is the farmer.  (If player 0 is the wolf, then
		// player 1 must have already swapped places and so we don't
		// allow the farmer to re-swap back.)
		if ((state == MuttonState.WOLF_HIDING_STATE) && (gb.getFarmerId() == 0)) {
			swapButton.show(gc, ((gb.getWolfCount() == 0) ? buttonSelect : null));
		}

		if (state != MuttonState.PUZZLE_STATE) {
			// When in any normal "playing" state, there should be a done button.
			// We let the board be the ultimate arbiter of if the "done" button
			// is currently active.
			int doneButtonTextIndex = getDoneButtonTextIndex(gb);
			if (GC.handleRoundButton(gc, doneRect,
			       (gb.DoneState() ? buttonSelect : null),
			       s.get(DoneButtonTextKeys[doneButtonTextIndex]),
			       HighlightColor, rackBackGroundColor)) {
				// always display the done button, but only make it active in
				// the appropriate states
				buttonSelect.hitCode = MuttonId.MuttonDoneButton;
				buttonSelect.row = doneButtonTextIndex;
			}
		}

		// Highlight the current player's button in yellow
		if ((gc != null) && (players != null)) {
			int highlightedPlayerId = 0;
			if (gb.getState() == MuttonState.GAMEOVER_STATE) {
				// If the game is over, then highlight the winner in blue
				GC.setColor(gc,Color.blue);
				highlightedPlayerId = gb.win[0] ? 0 : 1;
			} else {
				// The game is not over, so highlight the current player in yellow.
				GC.setColor(gc,Color.yellow);
				highlightedPlayerId = gb.whoseTurn;
			}

			if (players[highlightedPlayerId] != null) {
				Rectangle currPlayerRect = players[highlightedPlayerId].nameRect;
				gc.fillRoundRect(G.Left(currPlayerRect)-5, G.Top(currPlayerRect)-5,
						G.Width(currPlayerRect)+10, G.Height(currPlayerRect)+10,
						10, 10);
			}
			GC.setColor(gc,Color.black);
		}

		drawPlayerStuff(gc, (state == MuttonState.PUZZLE_STATE),nonDragSelect,
		                HighlightColor, rackBackGroundColor);

		// Display standard messages

		// If it's the wolf's turn to eat and he doesn't have a valid
		// meal, then we want to set the game message to "wolf must pass"
		// rather than "wolf must eat."  However, this state isn't a
		// real state in the game.
		MuttonState displayState = state;
		if ((state == MuttonState.WOLF_CHOOSING_MEAL_STATE) &&
		    (!gb.wolfHasValidMeal())) {
			displayState = MuttonState.WOLF_HAS_NO_VALID_MEAL_PSEUDO_STATE;
		}
		commonPlayer pl = getPlayerOrTemp(gb.whoseTurn);
		double messageRotation = pl.messageRotation();

		standardGameMessage(gc,messageRotation,
            		state==MuttonState.GAMEOVER_STATE?gameOverMessage(gb):s.get(displayState.getDescription()),
            				state!=MuttonState.PUZZLE_STATE,
            				gb.whoseTurn,
            				stateRect);
		goalAndProgressMessage(gc, nonDragSelect, "", progressRect, goalRect);


		// Draw the vcr controls
		drawVcrGroup(nonDragSelect, gc);
	}

	/*
	 * Return the index for the text key that should be used for the text of
	 * the "Done" button given the current game board configuration.
	 */
	private int getDoneButtonTextIndex (MuttonGameBoard gb) {
		switch (gb.getState()) {
			case WOLF_HIDING_STATE :
				return DONE_BTN_HIDE_KEY_INDEX;
			case WOLF_CHOOSING_MEAL_STATE :
				if (gb.wolfHasValidMeal())
					return DONE_BTN_EAT_KEY_INDEX;
				else
					return DONE_BTN_PASS_KEY_INDEX;
			case FARMER_CHOOSING_TARGETS_STATE :
				switch (gb.getNumberOfSheepTargeted()) {
					case 0  : return DONE_BTN_PASS_KEY_INDEX;
					case 1  : return DONE_BTN_SHOOT_KEY_INDEX;
					default : return DONE_BTN_RAGE_KEY_INDEX;
				}
		default:
			break;
		}
		return DONE_BTN_DONE_KEY_INDEX;
	}

	/**
	 * Execute a move by the other player, or as a result of local mouse activity,
	 * or retrieved from the move history, or replayed from a stored game. 
	 * @param mm the parameter is a commonMove so the superclass commonCanvas can
	 * request execution of moves in a generic way.
	 * @return true if all went well.  Normally G.Error would be called if anything went
	 * seriously wrong.
	 */
	public boolean Execute (commonMove mm, replayMode replay) {

		handleExecute(myBoard, mm,replay);

		if (replay.animate) {
			playSounds((MuttonMoveSpec)mm);
		}

		if (myBoard.getState() == MuttonState.GAMEOVER_STATE) {
			// If the game is over, then we can un-conceal all of the moves
			// in the history list.
			for (int i=0; i<History.size(); i++) {
				MuttonMoveSpec histMove = (MuttonMoveSpec) History.elementAt(i);
				histMove.concealed = false;
			}
		}
		return (true);
	}

	void playSounds(MuttonMoveSpec mm) {
		switch(mm.op) {
			case MOVE_RELOCATE:
			case MOVE_BOARD_STATE:
			case MOVE_DROP:
				playASoundClip(light_drop,100);
				break;
			case MOVE_EAT:
				switch(wolfOrSheepVictim(mm)) {
					case VICTIM_WOLF :
						playASoundClip(SND_EAT_WOLF, 100);
						break;
					case VICTIM_SHEEP :
						playASoundClip(SND_EAT_SHEEP, 100);
						break;
				default:
					break;
				}
				break;
			case MOVE_SHOOT:
				switch(wolfOrSheepVictim(mm)) {
					case VICTIM_WOLF :
						playASoundClip(SND_SHOOT_WOLF, 100);
						break;
					case VICTIM_SHEEP :
						playASoundClip(SND_SHOOT_SHEEP, 100);
						break;
				default:
					break;
				}
				break;
			case MOVE_PICKUP:
				playASoundClip(SND_PICKUP, 100);
				break;
			default: break;
		}
	}

	private static final int VICTIM_WOLF = 1;
	private static final int VICTIM_SHEEP = 2;
	private static final int VICTIM_NONE = 3;
	/*
	 * Determine if the victim of the given move was a sheep, wolf or
	 * neither.
	 * @return VICTIM_WOLF, VICTIM_SHEEP, or VICTIM_NONE.
	 */
	private int wolfOrSheepVictim(MuttonMoveSpec mm) {
		if (mm.sheepIds.length > 0) {
			MuttonCell sheepCell = myBoard.findCellWithSheep(mm.sheepIds[0]);
			return sheepCell.isWolf() ? VICTIM_WOLF : VICTIM_SHEEP;
		}
		return VICTIM_NONE;
	}	

	/**
	 * parse a move specifier on behalf of the current player.  This is called by the 
	 * "game" object when it receives a move from the other player.  Note that it may
	 * be called while we are in review mode, so the current state of the board should
	 * not be considered.
	 */
	public commonMove ParseNewMove(String st, int player) {
		return (new MuttonMoveSpec(st, player));
	}

	/*
	 * This will look through the end of the move history and see if there is
	 * a relocate move for the given sheep in it for the current turn.  If
	 * there is, then that move will be edited to change the destination to the
	 * one in the given move.  This will keep the move history clean if the
	 * player moves the same animal around multiple times in the same turn by
	 * only keeping the last one.
	 * @return the currMove if it is to be added to the history, or null
	 *          if a previous move was edited.
	 */
	private MuttonMoveSpec editHistoryRelocateForPiece (MuttonMoveSpec currMove) {
		int movingAnimalId = currMove.sheepIds[0];
		int histSize = History.size() - 1;
		int idx = histSize;
		while (idx > 0) {
			MuttonMoveSpec histMove = (MuttonMoveSpec) History.elementAt(idx);
			if (histMove.op == MOVE_RELOCATE) {
				if (histMove.sheepIds[0] == movingAnimalId) {
					// Found it!, so edit it.
					histMove.destination = currMove.destination;
					return null;
				} else {
					// Go back a step and look at that one...
					idx -= 1;
				}
			} else {
				// The move in history was not MOVE_RELOCATE, so we stop
				// looking back and return the current move so that it will be
				// added to the history.
				return currMove;
			}
		}

		// Should never get here (since a MOVE_RELOCATE should never be the first
		// move of a game), but we'll cover the case anyway...
		return currMove;
	}

	/*
	 * Remove the prior move from the history queue if it is of the given type.
	 */
	private boolean removePriorMoveIf (int moveType) {
		int historySize = History.size();
		if (historySize != 0) {
			MuttonMoveSpec priorMove =
			     (MuttonMoveSpec) History.elementAt(historySize - 1);
			if (priorMove.op == moveType) {
				// Remove the prior move
				popHistoryElement();
				return true;
			}
		}
		return false;
	}

	/**
	 * Prepare to add nmove to the history list, but also edit the history
	 * to remove redundant elements, so that indecisiveness by the user doesn't
	 * result in a messy game log.
	 * This may require that move be merged with an existing history move
	 * and discarded.  Return null if nothing should be added to the history
	 * One should be very cautious about this, only to remove real pairs that
	 * result in a null move.
	 */
	public commonMove EditHistory (commonMove nmove) {
		MuttonMoveSpec newMove = (MuttonMoveSpec) nmove;
		MuttonMoveSpec rval = newMove;    // default returned value

		switch (newMove.op) {
			case MOVE_BOARD_STATE :
				removePriorMoveIf(MOVE_PICKUP);
				removePriorMoveIf(MOVE_BOARD_STATE);
				// If the current board state is not the farmer configuring
				// then don't put this into the game record.
				if (myBoard.getState() != MuttonState.FARMER_CONFIGURING_BOARD) {
					rval = null;
				}
				break;
			case MOVE_PICKUP :
				break;
			case MOVE_DROP :
				removePriorMoveIf(MOVE_PICKUP);
				rval = null;
				break;
			case MOVE_FINISHED_HIDING :
				break;
			case MOVE_EAT :
				break;
			case MOVE_SHOOT :
				newMove.moveToFollow = myBoard.isShotgunEnabled();
				break;
			case MOVE_RELOCATE :
				// If this relocation is moving an animal that was already moved
				// during this turn, then we want to edit the previous relocate
				// move to indicate the new destination.
				removePriorMoveIf(MOVE_PICKUP);
				rval = editHistoryRelocateForPiece(newMove);
				break;
			case MOVE_DONE_RELOCATING :
				break;
			case MOVE_DONE :
				break;
			case MOVE_RESIGN :
				break;
			case MOVE_START :
				break;
			default :
				break;
		}

		return (rval);
	}

	/**
	 * The preferred mouse gesture style is to let the user "pick up" objects
	 * by simply clicking on them, but we also allow him to click and drag. 
	 * StartDragging is called when he has done this.
	 */
	public void StartDragging (HitPoint hp) {
		if (hp.hitCode instanceof MuttonId) 
		{	
			// Not currently dragging a sheep, so check to see if we will start
			switch ((MuttonId)hp.hitCode) {
				default:
					break;
				case HIT_BOARD_LOCATION:
					MuttonCell sourceCell = (MuttonCell) hp.hitObject;
					if (myBoard.canMoveAnimalFrom(sourceCell)) {
						PerformAndTransmit("Pickup " + sourceCell.getSheepId());
					}
					break;
			}
		}
	}

	/** 
	 * This is called on "mouse up".  We may have been just clicking
	 * on something, or we may have just finished a click-drag-release.
	 * We're guaranteed just one mouse up, no bounces.
	 */
	public void StopDragging(HitPoint hp) {
		
		CellId id = hp.hitCode;
		if (!(id instanceof MuttonId) && (performStandardActions(hp,missedOneClick))) {}
		else if (id==DefaultId.HitNoWhere) {leaveLockedReviewMode();}
		else if (!(id instanceof MuttonId)) { }
		else
		{
		int movingSheepId = myBoard.getMovingSheepId();
		MuttonState state = myBoard.getState();
		MuttonCell hitCell = (MuttonCell) hp.hitObject;
		MuttonId hitCode = (MuttonId)id;

		if (OurMove() && (movingSheepId >= 0)) {
			// An animal was being dragged, so we need to check if it landed on
			// a valid destination cell.  If so, then we put it there.  If not,
			// then it gets put back on its original space.  This test must be
			// done before any other checks because if the mouse is let up in
			// any part of the window that isn't the board, then we want to drop
			// the animal back on its spot and not think that there was a click
			// in that object.

			// Now, see if this is a valid move.
			if ((hitCode == MuttonId.HIT_EMPTY_BOARD_LOCATION) &&
				myBoard.isValidMoveDestination(hitCell)) {
				if ((state == MuttonState.WOLF_MOVING_SHEEP_STATE) ||
				    (state == MuttonState.FARMER_MOVING_SHEEP_STATE)) {
					PerformAndTransmit("Relocate " + movingSheepId + " " + hitCell.getCol() + " " + hitCell.getRow());
				} else if (state == MuttonState.FARMER_CONFIGURING_BOARD) {
					myBoard.returnMovingSheep();
					PerformAndTransmit("Board_State " + myBoard.getNewBoardLayoutString(movingSheepId, hitCell));
				}
			} else {
				// The animal was dropped somewhere other than a valid, empty
				// board space, so just return it to its original space.
				PerformAndTransmit("Drop");
			}

		}
		else {
		switch (hitCode) {
			default:
				throw G.Error("Hit Unknown: %s" , hitCode);
			case HIT_BOARD_LOCATION:
				// Player clicked on an occupied part of the board.
				switch (state) {
					case WOLF_HIDING_STATE :
						myBoard.doSheepWolfMutate(hitCell.getSheepId());
						break;
					case WOLF_CHOOSING_MEAL_STATE :
						if (hitCell.isValidMeal()) {
							myBoard.toggleCellTargeted(hitCell, false);
						}
						break;
					case FARMER_CHOOSING_TARGETS_STATE :
						if (hitCell.isAlive()) {
							myBoard.toggleCellTargeted(hitCell, rageEnabled);
						}
						break;
					default :
						break;
				}
				break;
			case HIT_HISTORY_COLUMN :
				// Player clicked on one of the columns of the history panel
				// for which the corresponding animal isn't on the board.
				break;
			case MuttonDoneButton : // We hit in the "Done" button
				if(state==MuttonState.RESIGN_STATE) { PerformAndTransmit("Done"); }
				else
				{
				switch (hp.row) {
					case DONE_BTN_PASS_KEY_INDEX :
					case DONE_BTN_SHOOT_KEY_INDEX :
					case DONE_BTN_RAGE_KEY_INDEX :
					case DONE_BTN_EAT_KEY_INDEX :
						String cmdString =
						    (myBoard.getState() == MuttonState.WOLF_CHOOSING_MEAL_STATE) ?
						        "Eat " : "Shoot ";
						String victims = myBoard.getTargetedSheepIds();
						PerformAndTransmit(cmdString + victims);
						break;
					case DONE_BTN_HIDE_KEY_INDEX :
						PerformAndTransmit("Wolves_Hidden " + myBoard.getWolfIdsString());
						break;
					case DONE_BTN_DONE_KEY_INDEX :
						PerformAndTransmit("Done_Relocating");
						break;
				default:
					break;
				}}
				break;
			case HIT_EMPTY_BOARD_LOCATION :
				break;
			case HIT_WOLF_UP_ARROW :
				PerformAndTransmit("Board_State " + myBoard.getNewBoardLayoutString(1));
				break;
			case HIT_WOLF_DOWN_ARROW :
				PerformAndTransmit("Board_State " + myBoard.getNewBoardLayoutString(-1));
				break;
			case HIT_FARMER_FACE :
				// Toggle enabling rage.
				rageEnabled = !rageEnabled;

				// Turn off all currently targeted animals but the last one.
				// (If rage is now being turned on, then there was at most 1
				//  animal targeted, so this won't do anything.  If rage is
				//  now being turned off, this will leave us with only the last
				//  one targeted.)
				myBoard.untargetAllBut(1);
				break;
		}
		}
		}
	}

	/*
	 * Draw the deep unchangeable objects, including those that might be rather expensive
	 * to draw.  This background layer is used as a backdrop to the rest of the activity.
	 * For mutton, we draw the empty board. 
	 *
	 * @param gc     The graphics context to draw on.
	 * @param gb     The game board to draw.
	 */
	public void drawFixedElements(Graphics gc) 
	{ boolean review = reviewMode() && !mutable_game_record;
		// Fill the screen with the background tile.
	 textures[BACKGROUND_TILE_INDEX].tileImage(gc,fullRect);

		// If we're in review mode, then tile the game board with the review tile.
		if (review) {
			textures[BACKGROUND_REVIEW_INDEX].tileImage(gc,
					G.Left(boardRect)-CELLSIZE, G.Top(boardRect)-CELLSIZE,
					G.Width(boardRect)+2*CELLSIZE, G.Height(boardRect)+2*CELLSIZE);
		}

		// Draw the blank game board.
		for (int r = 0; r < BOARD_ROWS; r++) {
			for (int c = 0; c < BOARD_COLS; c++) {
				MuttonCell cell = myBoard.getCell(c, r);
				if (cell.isOnBoard()) {
					int screenX = calcScreenX(c, r);
					int screenY = calcScreenY(c, r);
					// Paint the empty cell image
					mg.paintImage(gc, boardRect, screenX, screenY, IMG_BOARD_TILE, 0, 0, boardScale);
				}
			}
		}
	}

	/**
	 * Return the what was used as the initial string to gameType when the
	 * game was started.
	 */
	public String gameType() {
		// In games which have a randomized start, this method would return
		// return(bb.gametype+" "+bb.randomKey);
		return(myBoard.gametype + " " + myBoard.randomKey);
	}

	/**
	 * Return the sgfGameType for this game.
	 */
	public String sgfGameType() {
		// This is the official SGF type assigned to the game
		return (Mutton_SGF);
	}

	/**
	 * We need to keep the "show sgf" command turned off while the game is in
	 * progress so that the farmer player can't see where the wolf player has
	 * hidden the wolves.
	 */
	public void doShowSgf() {
		if (mutable_game_record || G.debug()) {
			super.doShowSgf();
		} else {
			theChat.postMessage( ChatInterface.GAMECHANNEL , ChatInterface.KEYWORD_CHAT,
			    s.get( CensoredGameRecordString));
		}
	}

   
    
    // interact with the board to initialize a game
	public void performHistoryInitialization(Tokenizer his) {
		//the initialization sequence
		String token = his.nextToken();
		//
		// in games which have a randomized start, this is the point where
		// the randomization is inserted
		long randomKey = his.longToken();

		myBoard.doInit(token, randomKey);
	}


	/**
	 * Handle action events from menus.  Don't do any real work, just note
	 * state changes and if necessary set flags for the run loop to pick up.
	 * 
	 */
	public boolean handleDeferredEvent (Object target, String command) {
		boolean handled = super.handleDeferredEvent(target, command);

		if (target == historyDisplayOption) {
			handled = true;
			showHistory = historyDisplayOption.getState();
			repaint(20);
		}

		return (handled);
	}

/**
 * Handle the run loop, and any special actions we need to take.
 * The mouse handling and canvas painting will be called automatically.
 * 
 * This is a good place to make notes about threads.  Threads in Java are
 * very dangerous and tend to lead to all kinds of undesirable and/or flakey
 * behavior.  The fundamental problem is that there are three or four sources
 * of events from different system-provided threads, and unless you are very
 * careful, these threads will all try to use and modify the same data
 * structures at the same time.   Java "synchronized" declarations are
 * hard to get right, resulting in synchronization locks, or lack of
 * synchronization where it is really needed.
 * 
 * This toolkit addresses this problem by adopting the "one thread" model,
 * and this is where it is.  Any other threads should do as little as possible,
 * mainly leave breadcrumbs that will be picked up by this thread.
 * 
 * In particular:
 * GUI events do not respond in the native thread.  Mouse movement and button
 * events are noted for later.  Requests to repaint the canvas are recorded but
 * not acted upon.
 * Network I/O events, merely queue the data for delivery later.
 */
	 //   public void ViewerRun(int wait)
	 //   {
	 //       super.ViewerRun(wait);
	 //   }

	/**
	 * This is used by the stock parts of the canvas machinery to get
	 * access to the default board object.
	 */
	public BoardProtocol getBoard() {
		return (myBoard);
	}


	/** factory method to create a robot */
	public SimpleRobotProtocol newRobotPlayer () {
		return (new MuttonPlayV2Bot());
	}

    /** replay a move specified in SGF format.  
     * this is mostly standard stuff, but the key is to recognize
     * the elements that we generated in sgf_save
     * summary: 5/27/2023
     * 4174 files visited 0 problems
     */
    public void ReplayMove(sgf_node no)
    {
        String comments = "";
        sgf_property prop = no.properties;

        while (prop != null)
        {
            String name = prop.getName();
            String value = (String) prop.getValue();
            
            if (setup_property.equals(name))
            {   Tokenizer tok = new Tokenizer(value);
                String gametype = tok.nextToken();
                long randomKey;
                // some damaged games are missing the space between
                // mutton and the random key
                if("mutton".equalsIgnoreCase(gametype)||"mutton-shotgun".equalsIgnoreCase(gametype)) 
                { randomKey =  tok.longToken();
                }
                else 
                { int spl = gametype.lastIndexOf("n");
                  randomKey = Long.parseLong(gametype.substring(spl+1));
                  gametype = gametype.substring(0, spl+1);
                }
                myBoard.doInit(gametype, randomKey);
             }
            else if (name.equals(comment_property))
            {
                comments += value;
            }
           else if (parseVersionCommand(name,value,2)) {}
           else if (parsePlayerCommand(name,value)) {}
            else
            {	// handle standard game properties, and also publish any
            	// unexpected names in the chat area
            	replayStandardProps(name,value);
            }

            prop = prop.next;
        }

        if (!"".equals(comments))
        {
            setComment(comments);
        }
    }

	public Rectangle createPlayerGroup(int player, int x, int y, double rotation, int unit) {
		throw G.Error("Not needed with manual layout");
	}

}

