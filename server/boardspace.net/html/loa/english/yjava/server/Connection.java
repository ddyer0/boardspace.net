/* Class    :  Connection - used by GameServer 
 * Author   :  Andrew Miller (rhuk@cis.ufl.edu)
 * Version  :  7/1/96  JAVA SDK 1.02
 * Notice   :  Copyright (C) 1996 Andrew Miller
 *
 * Each connection is contained in its own thread.  This class creates a thread then
 * initializes a socket for network communications.  
 *
 */


import java.net.*;
import java.io.*;
import java.lang.*;

// class decleration of Connection
class Connection extends Thread {  

  Socket socket = null;  //the actual socket Connection
  public boolean is_connected = false;  //the conenction 
  ServerSocket server = null;  // the socket of the GameServer
  
//constructor method for class Connection
Connection (ServerSocket server_sock) {

    server = server_sock;
}

// returns whether or not the connection is connected to the server
boolean connected() {

    return is_connected;
}

// returns the opened socket 
Socket get_socket() { 
    is_connected = false;
    return socket;  //return the opened socket
}

// part that actually runs.  Attempts to obtain a new socket
public void run() {

  	while (true){
    	is_connected = false;
    	System.out.println("In run Method.");
    	if (!is_connected) { //don't get another socket until this one taken
    		try{
          		socket = server.accept();
        	} catch(IOException e) {}
        	is_connected = true;  //set that is connected
        	this.suspend();
      	}
  	}  
}

}   

