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

//
// Specialized cell used for the game Mutton.
//
public class MuttonCell implements MuttonConstants
{
	// The location of the cell on the board.
	private int col;
	private int row;

	// The id of the sheep that is on this cell.
	private int sheepId = CELL_EMPTY;

	// The state of the sheep that is on this cell.
	private boolean isAlive;
	private boolean isWolf;

	// The rotation value used to display the contents of this cell.
	private int rotationValue = 1;

	// Flags to indicate if this cell is valid for various moves
	// The valid meal flag indicates that the animal on this space is adjacent
	// to a wolf and is therefore a valid target to be eaten.
	private boolean validMealFlag = false;

	// The types of highlighting that should be applied to this cell
	// The first element is highlighting drawn below the animal.
	// The second element is highlighting drawn above the animal.
	private int [] highlightCode = new int [2];

	/**
	 * Construct a new cell on the board.
	 * @param initialSheepId    The id of the sheep on this cell.
	 */
	public MuttonCell (int lcol, int lrow, int initialSheepId) {
		col = lcol;
		row = lrow;
		init(initialSheepId);
	};

	/**
	 * Initialize the cell back to an initial state.
	 * 
	 * @param initialSheepId    The id of the sheep on this cell.
	 */
	public void init (int initialSheepId) {
		this.sheepId = initialSheepId;
		this.isAlive = (initialSheepId >= 0);
		this.isWolf = false;
		this.rotationValue = 1;
		this.validMealFlag = false;
		this.highlightCode[0] = HIGHLIGHT_NONE;
		this.highlightCode[1] = HIGHLIGHT_NONE;
	}

	/**
	 * Return the row of this cell on the board.
	 */
	public int getRow () {
		return row;
	}

	/**
	 * Return the column of this cell on the board.
	 */
	public int getCol () {
		return col;
	}

	/**
	 * Return if this cell is on the given space or not.
	 */
	public boolean isSpace (int lcol, int lrow) {
		return ((col == lcol) && (row == lrow));
	}

	/**
	 * Return the id of the sheep that is on this cell.
	 */
	public int getSheepId () {
		return sheepId;
	}

	/**
	 * Set the id of the sheep that is on this cell.
	 */
	public void setSheepId (int newId) {
		sheepId = newId;
	}

	/**
	 * Return if the animal on this cell is alive or dead.
	 */
	public boolean isAlive () {
		return isAlive;
	}

	/**
	 * Set the alive/dead state of the animal on this cell.
	 */
	public void setAlive (boolean lisAlive) {
		isAlive = lisAlive;
	}

	/**
	 * Return if this sheep is really a wolf.
	 */
	public boolean isWolf () {
		return isWolf;
	}

	/**
	 * Set the wolf status of the cell.
	 */
	public void setWolf (boolean lisWolf) {
		isWolf = lisWolf;
	}

	/**
	 * Get the current validMealFlag for the cell.
	 */
	public boolean isValidMeal () {
		return validMealFlag;
	}

	/**
	 * Set the value of the validMealFlag for the cell.
	 */
	public void setValidMealFlag (boolean validFlag) {
		validMealFlag = validFlag;
	}

	/**
	 * Get the current highlight code for the cell.
	 */
	public int getHighlightCode (int whichCode) {
		return highlightCode[whichCode];
	}

	/**
	 * Set the value of the highlight code for the cell.
	 */
	public void setHighlightCode (int whichCode, int lhighlightCode) {
		highlightCode[whichCode] = lhighlightCode;
	}

	/**
	 * Return the display rotation value for this cell.
	 */
	public int getDisplayRotation () {
		return rotationValue;
	}

	/**
	 * Set the display rotation value for this cell.
	 */
	public void setDisplayRotation (int newRotation) {
		rotationValue = newRotation;
	}

	/**
	 * Remove anything on this space to make it empty.
	 */
	public void clear () {
		sheepId = CELL_EMPTY;
		highlightCode[0] = HIGHLIGHT_NONE;
		highlightCode[1] = HIGHLIGHT_NONE;
	}

	/**
	 * Return if this space is empty or not.
	 */
	public boolean isEmpty () {
		return (sheepId == CELL_EMPTY);
	}

	/**
	 * Return if this space is actually on the board or not.
	 */
	public boolean isOnBoard () {
		return (sheepId != CELL_NOT_EXIST);
	}

	/**
	 * Copy the contents of another cell into this one.
	 *
	 * @param from   The cell whose contents we are copying.
	 */
	public void copyFrom (MuttonCell from) {
		this.col = from.col;
		this.row = from.row;
		this.sheepId = from.sheepId;
		this.isAlive = from.isAlive;
		this.isWolf = from.isWolf;
		this.rotationValue = from.rotationValue;
		this.highlightCode[0] = from.highlightCode[0];
		this.highlightCode[1] = from.highlightCode[1];
		this.validMealFlag = from.validMealFlag;
	}

	/**
	 * Logically move an animal from another cell into this one.
	 *
	 * @param from   The cell the animal is moving from.
	 */
	public void moveAnimalFrom (MuttonCell from) {
		// Move the info over from the source cell.
		this.sheepId = from.sheepId;
		this.isAlive = from.isAlive;
		this.isWolf = from.isWolf;
		this.rotationValue = from.rotationValue;
		this.highlightCode[0] = from.highlightCode[0];
		this.highlightCode[1] = from.highlightCode[1];

		// Clear animal info from the source cell.
		from.sheepId = CELL_EMPTY;
		from.isAlive = false;
		from.isWolf = false;
		from.highlightCode[0] = HIGHLIGHT_NONE;
		from.highlightCode[1] = HIGHLIGHT_NONE;
	}

	/**
	 * Determine if this cell is the same (logical) cell as the given one.
	 */
	public boolean sameCell (MuttonCell otherCell) {
		if (sheepId == CELL_NOT_EXIST) {
			return (otherCell.sheepId == CELL_NOT_EXIST);
		}
		return ((sheepId == otherCell.sheepId) &&
		        (isAlive == otherCell.isAlive) &&
		        (isWolf  == otherCell.isWolf) &&
		        (row == otherCell.row) &&
		        (col == otherCell.col));
	}
}
