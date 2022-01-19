/* Class    :  GameBoard - Noughts & Crosses
 * Author   :  Andrew Miller (rhuk@cis.ufl.edu)
 * Version  :  7/1/96  JAVA SDK 1.02
 * Notice   :  Copyright (C) 1996 Andrew Miller
 *
 * This class extends the panel class and basically draws the noughts and crosses grid.
 * The it determines which pieces have been placed and draws those accordingly.
 * it handels mouse down events (clicks) on the board and responds accordingly.  Because
 * this class must access elements from NCWindow, you must pass the NCWindow object to the
 * GameBoard class.
 */

import java.awt.*;
import java.io.*;


// class decleration of GameBoard
class GameBoard extends Panel {	

	Toolkit toolkit = Toolkit.getDefaultToolkit();
 
	static final int PLAYER1 = 1;
  	static final int PLAYER2 = 2;
    NCWindow target;
	Image gameboard, cross, nought;
	
	int xoffset, yoffset, locationx, locationy, box_number;

// constructor for GameBoard
public GameBoard(NCWindow target, Image board, Image x, Image o) { 
	this.target = target;				// set target equal to NCWindow object
	gameboard = board;
	cross = x;
	nought = o;

}

// paint updates the grid and draws the respective moves
public void paint(Graphics g) {
	
	
	int yoff = 53;
	int xoff = 53;
    	yoffset = yoff;
	xoffset = xoff;

	g.drawImage(gameboard, 0, 0, this);

	for (int i=0; i<9; i++) {
	  	if (target.moves[i] == target.PLAYER1)
	    	g.drawImage(cross, (xcoord(i)*xoff)+16, (ycoord(i)*yoff)+8, this);
	  	if (target.moves[i] == target.PLAYER2)
	    	g.drawImage(nought, (xcoord(i)*xoff)+16, (ycoord(i)*yoff)+8, this); 	

	}
}


// locate xcoordinate offset depending on which square you are in
public int xcoord(int i) {
	if(i==0 || i==3 || i==6) return 0;
	if(i==1 || i==4 || i==7) return 1;
	if(i==2 || i==5 || i==8) return 2;
	return 9;
}

// locate ycoordinate offset depending on which square you are in
public int ycoord(int i) {
	if(i==0 || i==1 || i==2) return 0;
	if(i==3 || i==4 || i==5) return 1;
	if(i==6 || i==7 || i==8) return 2;
	return 9;
}


// find which sqaure you are in depending on your x and y coordinates
public int findBox(int x, int y) {
	int yindex;
	int xindex;

	yindex = y - 8;
	xindex = x - 16;

	if((yindex <= 1*yoffset) && (yindex > 0*yoffset)){
	  	if((xindex <= 3*xoffset) && xindex > (2*xoffset)) return 2;
	  	if((xindex <= 2*xoffset) && xindex > (1*xoffset)) return 1;
	  	if((xindex <= 1*xoffset) && xindex > (0*xoffset)) return 0;
	}
	if((yindex <= 2*yoffset) && (yindex > 1*yoffset)){
	  	if((xindex <= 3*xoffset) && xindex > (2*xoffset)) return 5;
	  	if((xindex <= 2*xoffset) && xindex > (1*xoffset)) return 4;
	  	if((xindex <= 1*xoffset) && xindex > (0*xoffset)) return 3;
	}
	if((yindex <= 3*yoffset) && (yindex > 2*yoffset)){
	  	if((xindex <= 3*xoffset) && xindex > (2*xoffset)) return 8;
	  	if((xindex <= 2*xoffset) && xindex > (1*xoffset)) return 7;
	  	if((xindex <= 1*xoffset) && xindex > (0*xoffset)) return 6;
	}
	return 0;
}

// handle mouse click events on the GameBoard
public boolean mouseDown(Event e, int x, int y) {

	int index;
	Integer element;

	if (target.game_on) {
	  	element = new Integer(target.opponent_id);
	  	if (target.id_vector.contains(element))	
            index = target.id_vector.indexOf(element);
	  	else {
	    	index = target.id_vector.indexOf(new Integer(target.personal_id));
	    	target.gclient.send_stream("!!directive!!mssge,surrender|"+Integer.toString(index+1)+"|");
	    }
	  	if(target.game_on) {
	    	if(target.turn) {
	      		box_number = findBox(x,y);
	      		if(target.moves[box_number] == 0){
	      	  		target.piece_placed = true;
		  			target.moves[box_number]=target.player;
		  			target.gclient.send_stream("!!directive!!pmove,"+Integer.toString(box_number)+'|'+Integer.toString(index+1)+'|');
	    	  		target.turn = false;
	      		}
	      		if(target.hasWon(target.player)) {
		  			repaint();
		  			target.display("You WIN!!!!! you will go second this time\n");
		  			target.gclient.send_stream("!!directive!!mssge,ulose|"+Integer.toString(index+1)+'|');
		  			target.finish = true;
		  			target.turn = false;
	 	    	}
            	if(target.staleMate()  && !target.hasWon(target.player)) {
		  			repaint();
		  			target.display("Stalemate, try again...\n");
		  			target.gclient.send_stream("!!directive!!mssge,stale|"+Integer.toString(index+1)+'|');
		  			target.turn = false;
		  			target.finish = true;
	      		}
	    	}
	  	repaint();
	  	}	  
          
  	}
   	return true; // we handled this event  	
}

public void update(Graphics g) {
	paint (g);
}

}
