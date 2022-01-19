/* Class    :  NCWindow - Noughts & Crosses
 * Author   :  Andrew Miller (rhuk@cis.ufl.edu)
 * Version  :  7/1/96  JAVA SDK 1.02
 * Notice   :  Copyright (C) 1996 Andrew Miller
 *
 * This is the main part of the Noughts and Crosses game.  A frame is created and the GUI 
 * is drawn on it.  
 * 
 * This client is designed around the GameServer and GameClient classes.  This means that
 * the server is very stupid, it only supports a few functions.  What this means to the 
 * programmer is that the clients take care of pretty much everything.  This allows them
 * to be very flexible and even allows the possiblity of playing multiple types of games
 * with the same GameServer.
 * 
 * The class takes care of all mouse events (except those on the GameBoard.
 * All the processing of input and output is also handled here.  Access to network features
 * has a layer of abstraction, namely the GameClient class.  This provides the ability to
 * to create an arbitrary game and still use the same network functionality.
 * 
 * note: the board is created as an array of nine elements that tells which player is 
 * occupying that position.
 *
 *  |-----|-----|-----|
 *  |     |     |     |
 *  |  0  |	 1  |  2  |
 *  |	  |	    |	  |
 *  |-----|-----|-----|
 *  |     |     |     |
 *  |  3  |	 4  |  5  |	 =  moves[0,1,2,3,4,5,6,7,8]
 *  |	  |	    |	  | 
 *  |-----|-----|-----|
 *  |     |     |     |
 *  |  6  |	 7  |  8  |
 *  |	  |	    |	  |
 *  |-----|-----|-----|
 */


import java.awt.*;
import java.applet.Applet;
import java.net.*;
import java.io.*;
import java.util.Vector;

// class decleration of NCWindow
class NCWindow extends Frame implements Runnable{ //the working window

	GameClient gclient = new GameClient();
  	String text, opponent_temp;
  	Vector id_vector = new Vector(1,1);  	// dynamic vector to store users id's
  	boolean game_on = false;				// flag for game state
  	boolean piece_placed = false;			// flag for move state
  	boolean finish = false;					// flag for game over
  	boolean challenged = false;				// flag for challenge
  	boolean turn = true;					// flag for turn
  	static final int PLAYER1 = 1;			
  	static final int PLAYER2 = 2;
  	int player = PLAYER1;					// who is player 1
  	int other_player = PLAYER2;				// who is player 2
  	int opponent_id;						// store id of opponent
  	int personal_id;						// store your id
  	int nameIndex;
  	int moves[] =  new int[9];				// store all moves made

   	URL base;								// host to connect to
	int port; 								// the port to connect to
  
  	Thread thread;							// create a thread to run in

	graphicPanel panel2;					// title panel
	graphicPanel panel3;					// filler panel
	graphicPanel top;
	graphicPanel bottom;
	CPictButton connect_button;
	CPictButton accept_button;
	CPictButton abort_button;
	CPictButton quit_button;

    GameBoard panel1;						// create a gameboard
    Label name_box;							// create a name label
    TextField name;							// create a name box
    Label opponent_box;						// create an opponent label
    TextField opponent;						// create an opponent box
    Button connect;							// create a connect button
    Button close;							// create a close button
    Button challenge;						// create an accept button
    Button clear;							// create a clear button
    List user_list;							// create a list of users
    TextArea textArea;						// create an area to display messages
    Label users_names;						// create a label
    TextField textField;					// create an input textfield

	Image title_image;
	Image game_board_image;
	Image filler_image;
	Image connect_up;
	Image connect_dn;
	Image accept_up;
	Image accept_dn;
	Image abort_up;
	Image abort_dn;
	Image quit_up;
	Image quit_dn;
	Image cross;
	Image nought;
	Image top_image;
	Image bottom_image;
	Color bk_color;
	Color fg_color;
	Color bx_color;


  
// constructor for NCWindow
NCWindow(URL host,int p,Image[] images) {
 
  	base = host;   							//save the parameters
  	port = p;
	title_image = images[9];
	game_board_image = images[10];
	filler_image = images[8];
	connect_up = images[0];
	connect_dn = images[1];
	accept_up = images[2];
	accept_dn = images[3];
	abort_up = images[4];
	abort_dn = images[5];
	quit_up = images[6];
	quit_dn = images[7];
	cross = images[12];
	nought = images[11];
	top_image = images[13];
	bottom_image = images[14];

 
 	addNotify();
  	resize(365,455);   						// Default Window Size
//    setResizable(false);
  	setTitle("by rhuk@cis.ufl.edu");			// set title

//set layout for GUI
											// use a placement layout scheme
 	setLayout(null);

	bk_color = new Color(90,90,90);
	fg_color = new Color(255,206,140);
	bx_color = new Color(2,29,15);

	setBackground(bk_color);
	setForeground(fg_color);
    panel1=new GameBoard(this,game_board_image, cross, nought);
    panel1.setLayout(null);
    add(panel1);
    panel1.reshape(0,116,180,180);

	top = new graphicPanel(bottom_image,36,24,10,1);
	top.setLayout(null);
	add(top);
	top.reshape(0,25,360,24);

	bottom = new graphicPanel(bottom_image,36,24,10,1);
	bottom.setLayout(null);
	add(bottom);
	bottom.reshape(0,432,360,24);

	panel2 = new graphicPanel(title_image,0,0,0,0);
	panel2.setLayout(null);
	add(panel2);
	panel2.reshape(0,50,258,66);

	panel3 = new graphicPanel(filler_image,0,0,0,0);
	panel3.setLayout(null);
	add(panel3);
	panel3.reshape(180,271,78,25);
    
    name=new TextField(11);
    name.setFont(new Font("Helvetica",Font.PLAIN,12));
	name.setBackground(Color.black);
	name.setForeground(fg_color);
    add(name);
 	name.setText("Guest");
    name.reshape(258,52,99,30);

	opponent=new TextField(11);
    opponent.setFont(new Font("Helvetica",Font.PLAIN,12));
	opponent.setBackground(Color.black);
	opponent.setForeground(fg_color);
	opponent.setEditable(false);
    add(opponent);
    opponent.reshape(258,84,99,30);


	add(connect_button = new CPictButton(connect_up, connect_dn, null));
    connect_button.reshape(180,116,78,38);
    add(accept_button = new CPictButton(accept_up, accept_dn, null));
	accept_button.reshape(180,154,78,39);
	add(abort_button = new CPictButton(abort_up, abort_dn, null));
	abort_button.reshape(180,193,78,39);
	add(quit_button = new CPictButton(quit_up, quit_dn, null));
	quit_button.reshape(180,232,78,39);

    user_list=new List();
	user_list.setBackground(Color.black);
	user_list.setForeground(fg_color);
	user_list.reshape(258,116,99,176);
    add(user_list);
    user_list.reshape(258,116,99,176);
    
    
    textArea=new TextArea(41,8);
    textArea.setFont(new Font("Helvetica",Font.PLAIN,11));
	textArea.setBackground(Color.black);
	textArea.setForeground(fg_color);
	textArea.setEditable(false);
    add(textArea);
    textArea.reshape(7,296,350,100);
    
    textField=new TextField(41);
    textField.setFont(new Font("Helvetica",Font.PLAIN,12));
	textField.setBackground(Color.black);
	textField.setForeground(fg_color);
    add(textField);
    textField.reshape(7,401,350,30);
     
  	validate();

}


// gets the thread going
public void start() {

  	thread = new Thread(this,"main"); 		//create a thread for this window
  	thread.setPriority(Thread.MIN_PRIORITY); 	//low priority so components work
  	thread.start();
}

// what to do while thread is running
public void run() {
  
    display("Hit the connect button to start.\n");  
    while (true) {
      	try{thread.sleep(50);} catch(InterruptedException e){}
      	if (gclient.running()) 
			continue;	
      	if (gclient.input_stream_available()) {
			text = gclient.get_input();
  			if (gclient.is_nameslist(text))
   	  			write_list(text.substring(9)); //update the list
			if (gclient.is_directive(text)) 
	  			processDirective(text.substring(13)); //process directive
   			if (!gclient.is_directive(text) && !gclient.is_nameslist(text))
   	  			display(text);  //otherwise display the text
      	}
 	}
}


// If directive, deal with it according to the message that is attached
public void processDirective(String text) {

	String temp;
	int next, temp_id, opp_id;

	temp = text;	
	text = text.substring(6);

// if the directive is a challenge...

	if (temp.startsWith("chlng")) {
	  	next = text.indexOf(',');
	    temp = text.substring(0,next);
	    if (!game_on) {
	      	opponent_id = Integer.parseInt(temp);
	      	text = text.substring(next+1);
	      	temp = text.substring(0);
	      	opponent_temp = temp.trim();
	      	player = PLAYER2;
	      	other_player = PLAYER1;
	      	turn = false;
	      	challenged = true;
	      	display("You have been challenged by "+temp);
	      	display("hit the Accept button to accept the challenge\n");
  	    }
        else {
	      	opp_id = Integer.parseInt(temp);
	      	temp_id = id_vector.indexOf(new Integer(opp_id));
   	      	gclient.send_stream("!!directive!!mssge,busy|"+Integer.toString(temp_id+1)+"|");
	    }
	    
	}

// if the directive is a message...

	if(temp.startsWith("mssge")) {
	  	if (text.startsWith("uid")) {					  	// User ID update message
	    	temp = text.substring(4);
	    	personal_id = Integer.parseInt(temp.trim());
	  	}
	  	if (text.startsWith("busy")) 						// user busy message
	    	display ("That player is currently playing a game...\n");
	  	if (text.startsWith("ulose")) {						// you lose game message
	    	display ("You lost, you go first this time\n");
	    	turn = true;
	    	finish = true;
	    	game_on = false;
	    	temp_id = id_vector.indexOf(new Integer(opponent_id));
   	    	gclient.send_stream("!!directive!!mssge,reset|"+Integer.toString(temp_id+1)+"|");
	  	}
	  	if (text.startsWith("stale")) {						// stalemate message
	    	display("Stalemate, try again...\n");
	    	turn = true;
	    	finish = true;
	    	game_on = false;
	    	temp_id = id_vector.indexOf(new Integer(opponent_id));
   	    	gclient.send_stream("!!directive!!mssge,reset|"+Integer.toString(temp_id+1)+"|");
	  	}  
	  	if (text.startsWith("reset")) {						// reset board message
	    	temp_id = id_vector.indexOf(new Integer(opponent_id));
   	    	gclient.send_stream("!!directive!!mssge,ack_reset|"+Integer.toString(temp_id+1)+"|");
	    	reset_board();
	  	}
	  	if (text.startsWith("ack_reset")) 					// acknowledged reset message
	    	reset_board();
	  	if (text.startsWith("surrender")){					// surrender message
	    	display("Your opponent has surrendered\n");
	    	reset_board();
   	    	game_on = false;
   	    	turn = true;
   	    	opponent.setText("");
	  	}
	}

// if directive is an accept challenge...

	if(temp.startsWith("accpt") && !challenged) {
	  	next = text.indexOf(',');
	  	temp = text.substring(0,next);
	  	temp_id = Integer.parseInt(temp.trim());
	  	if(temp_id == opponent_id) {					// if its the same person you challeged
	    	text = text.substring(next+1);
	    	opponent.setText(text.trim());
	    	game_on = true;
	    	turn = true;
	    	player = PLAYER1;
	    	other_player = PLAYER2;
	    	challenged = false;
	    	display("GAME ON!! you go first\n");
	  	}	
	}

// if directive is a move

	if(temp.startsWith("pmove")) {
	  	next = text.indexOf(',');
	  	temp = text.substring(0,next);
	  	moves[Integer.parseInt(temp.trim())] = other_player;	// add move to list
	  	panel1.repaint();
	  	turn = true;
	}	  
}

// check all possible winning combinations to see if player has won
public boolean hasWon(int playerx) {
	if((moves[0]==playerx && moves[1]==playerx && moves[2]==playerx) ||
	   	(moves[3]==playerx && moves[4]==playerx && moves[5]==playerx) ||
	   	(moves[6]==playerx && moves[7]==playerx && moves[8]==playerx) ||
	   	(moves[0]==playerx && moves[3]==playerx && moves[6]==playerx) ||
	   	(moves[1]==playerx && moves[4]==playerx && moves[7]==playerx) ||
	   	(moves[2]==playerx && moves[5]==playerx && moves[8]==playerx) ||
	   	(moves[0]==playerx && moves[4]==playerx && moves[8]==playerx) ||
	   	(moves[2]==playerx && moves[4]==playerx && moves[6]==playerx)) return true;
	else return false;
}

// check to see if all moves have been made and no open squares remain
public boolean staleMate() {
	for (int i=0; i<9; i++) {
	  	if (moves[i] == 0) return false;
	}
	return true;
}

// reset all the moves and repaint the gameboard
public void reset_board() {
	for (int i=0; i<9; i++) moves[i] = 0;   // clear board vector;
	panel1.repaint();
	finish = false;
	game_on = true;
}


// put the unique id's into the id_vector and the names into the user_list
public void write_list(String list) { 
  	int i, next, n;
  	int temp_id;

  	user_list.delItems( 0, user_list.countItems() -1 );  // workaround for netscape bug
  	user_list.clear();  //erase the current list
  	id_vector.removeAllElements();						 // clear vector

  	for (i=0;i<list.length();i++) { //go through the list
    	  n = list.indexOf('~',i);     //get position of next id
    	  next = list.indexOf('|',i);  //get position of next delimiter
    	  if (next >= list.length() || next == -1)
      		break;  //done on last element
    	  temp_id = Integer.parseInt(list.substring(i,n));
    	  id_vector.addElement(new Integer(temp_id));

    	  user_list.addItem(list.substring(n+1,next)); //write this user
    	  i = next;  //look for a new delimiter
  	}
	user_list.reshape(258,116,99,176);
} 

// display the text to the output area of the screen
public void display(String s){ //write to the text area
 
  	try{thread.sleep(100);}catch(InterruptedException e){}
  	textArea.appendText(s);
}

// event handler for GUI components
public boolean handleEvent(Event evt) {
  
  	boolean statusflag = false;
  	int index, storage;

// when you double-click on a user name in the user_list
  	if (evt.id == Event.ACTION_EVENT && evt.target == user_list && !game_on) {
    	index = user_list.getSelectedIndex();
     	opponent_id = ((Integer)id_vector.elementAt(index)).intValue();
     	if (opponent_id != personal_id){
       		display("Challenging "+ (String)evt.arg + " to Noughts & Crosses\n");
       		gclient.send_stream("!!directive!!chlng,"+Integer.toString(personal_id)+"|"+Integer.toString(index+1)+"|");
     	}
  	}

// when you need to connect first
  	if (!gclient.is_connected() && evt.id == Event.ACTION_EVENT)
    	textArea.appendText("Hit the connect button to connect.\n");





// when you perform an action while you are connected
  	if (evt.id == Event.ACTION_EVENT && gclient.is_connected()) {
    	if (gclient.stream_down()){
      		display("A socket error has occured, you are not connected.\n");
      		return super.handleEvent(evt);
    	}

    	if (evt.target == textField){ 					//if want to send the text
      		String textstream = textField.getText();
      		textField.setText("");
      		gclient.send_stream(textstream);
    	}			
    
    	if (evt.target == name && gclient.is_connected()){ 	//if want to send the text
      		String textstream = name.getText();
      		gclient.send_stream("!!name!!" +textstream);    // send updated name  
    	}

  	} 
  	return super.handleEvent(evt);		// allow for other events to be handled
}

public boolean mouseUp (Event evt, int x, int y) {
    boolean statusflag;
	int index;

// when you hit the connect button and are not already connected	
    if (evt.target == (Component)connect_button && !gclient.is_connected()) {
      String host=base.getHost();
      display("Trying to connect to " + host + " on port " + port + "\n");
      statusflag = gclient.connect(host,port);
      if (!statusflag) {
     	display("Unable to to connect to " + host + " on port " + port + "\n");
		return true;
		}
      else
	display("\nYour are 'Guest'. You can change this in the Player box above.\n");
    return true;
    }

// when you hit the close button
  	if (evt.target == (Component)quit_button) {  //if request to close the window
    	display("Preparing to log off.\n");  //log out first
    	if (game_on) {
      		index = id_vector.indexOf(new Integer(opponent_id));
      	gclient.send_stream("!!directive!!mssge,surrender|"+Integer.toString(index+1)+"|");
    	}	
    	gclient.disconnect();
    	dispose();  //close the window
  	return true;
	}

// when you hit the clear button
  	if (evt.target == (Component)abort_button && game_on) {  //if request to close the window
   		display("Surrendering...");  //let user know his/her fate
   		index = id_vector.indexOf(new Integer(opponent_id));
   		gclient.send_stream("!!directive!!mssge,surrender|"+Integer.toString(index+1)+"|");
   		reset_board();
   		game_on = false;
   		turn = true;
   		opponent.setText("");
		return true;
  	}


// when you hit the challenge button
  	if (evt.target == (Component)accept_button && challenged) {  //if accepting challengee
   		display("Accepted! You will move second.\n");  //explain situation
   		index = id_vector.indexOf(new Integer(opponent_id));
   		gclient.send_stream("!!directive!!accpt,"+Integer.toString(personal_id)+"|"+Integer.toString(index+1)+"|");
   		challenged = false;
   		game_on = true;
   		opponent.setText(opponent_temp);
		return true;
  	}


    return false;
}

// tells the application how to clean itself up properly
public void stop(){  //on program termination, clean up

  	int index;
  	display("Preparing to log off.\n");
  	if (game_on) {
    	index = id_vector.indexOf(new Integer(opponent_id));
    	gclient.send_stream("!!directive!!mssge,surrender|"+Integer.toString(index+1)+"|");
  	}
  	gclient.cleanup();
}


}

// simple class used to extend Panel to display custom bitmaps
// it repeats the image in x & y directions multiple times
// depending on the values passed

class graphicPanel extends Panel {
	Image image;
	int x_multiple;
	int y_multiple;
	int x_size;
	int y_size;

public graphicPanel(Image i, int xsize, int ysize, int xmult, int ymult) {
	
	image = i;
	x_multiple = xmult;
	y_multiple = ymult;
	x_size = xsize;
	y_size = ysize;
}

public void paint(Graphics g) {
	if (x_size == 0 && y_size == 0)
    	g.drawImage(image,0,0,this);
	else {
		for(int a=0; a < (x_size * x_multiple); a=a+x_size) {
			for(int b=0; b < (y_size * y_multiple); b=b+y_size) {
				g.drawImage(image,a,b,this);
			}
		}

	}
}

public void update(Graphics g) {
	paint (g);
}

}