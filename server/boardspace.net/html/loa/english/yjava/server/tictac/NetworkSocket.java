/* Class    :  NetworkSocket - used by GameServer 
 * Author   :  Andrew Miller (rhuk@cis.ufl.edu)
 * Version  :  7/1/96  JAVA SDK 1.02
 * Notice   :  Copyright (C) 1996 Andrew Miller
 *
 * This is the main class for the network communication	process used by GameServer.
 * It is quite simple and contains very low-level methods that basically handle the
 * creation, reading, writing, and status of the socket.
 *
 */

import java.net.*;
import java.io.*;
import java.lang.*;

// declaration of class NetworkScoket
class NetworkSocket {

  Socket socket;
  DataInputStream in;
  PrintStream out;
  String name = new String("Guest");  //default name for the user
  int id = 1;

// Constructor function that takes a socket as a parameter  
NetworkSocket(Socket client){
    //get the streams
    try{
      	in = new DataInputStream(new BufferedInputStream(client.getInputStream()));
      	out = new PrintStream (new BufferedOutputStream(client.getOutputStream(), 1024), false);
        socket = client;
   	} catch(IOException e){}
}

// Takes a string and puts it on the outgoing stream, then flushes the stream
public void write_stream (String s) { //write to the socket

  	out.println(s);  //write the string to the socket
  	out.flush();
}

// Takes an input stream, returns the string
public String read_stream() {  //returns the data to get 
  
  	try{
    	String inputStream = in.readLine(); //get a line
    	return inputStream;
  	}catch(IOException e){}
  	return "null";  //return the read in string
}

// returns whether or not the client can read from the socket
public int is_readable() {  //returns if can read from this socket
  
  	boolean test;

  	if (out.checkError()){  //return -1 if error to close connection
    	System.out.println("Error: Client unreachable");
    	return -1;
  	}
  	try{
  		test = (in.available()>1);  //return true if can read 
  	}catch(IOException e){ return -1; }//close the connection if can't test
  	if (test)   //1 if can read
    	return 1;
  	else        // 0 if can't
    	return 0;
}

// closes everything down 
public void kill() {  //kill everything

  	try{
     	if (out != null) out.close();
    	if (in != null) in.close();
    	if (socket != null) socket.close();
  	}catch (IOException e){}

}

}

