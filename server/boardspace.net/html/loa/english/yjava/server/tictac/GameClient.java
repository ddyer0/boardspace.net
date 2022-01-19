/* Class    :  GameClient - Noughts & Crosses
 * Author   :  Andrew Miller (rhuk@cis.ufl.edu)
 * Version  :  7/1/96  JAVA SDK 1.02
 * Notice   :  Copyright (C) 1996 Andrew Miller
 *
 * This class interfaces the network classes of JAVA and also supports some functionality
 * of the GameServer.  Most members are quite self explanatory
 */


import java.net.*;
import java.io.*;

// clase decleration of GameClient
class GameClient {

	private boolean connect_state = false;		 // some variables
	private Socket to_server;
	private DataInputStream in = null;			 // input stream
	private PrintStream out = null;				 // output stream

// return whether or not there is a connection
public boolean is_connected() {

	return connect_state;
}

// return whether or not the connection is running
public boolean running() {
      	
	if (!connect_state || in == null || out == null)
 	  	return true;
	else return false;
}

// return whether or not the string is a names message from the server
public boolean is_nameslist(String text) {

	if (text.startsWith("!!names!!")) return true;
	else return false;
}

// return whether or not the string is a directive message from the server
public boolean is_directive(String text) {

	if (text.startsWith("!!directive!!")) return true;
	else return false;
	
}

// return whether or not there is an input stream available
public boolean input_stream_available() {

	try {
	  	if (in.available() > 0) return true;
	} catch (IOException e) {System.out.println("IO Exception " + e);}
	return false;
}

// return whether or not any of the IO streams are not functioning
public boolean stream_down() {
	if (out == null || in == null) return true;
	else return false;
}

// disconnect the socket
public void disconnect() {

   if (out != null && in != null){
   	  System.out.println("Disconnecting");
     	out.println("!!quit!!");	  // send quit message to server
      	out.flush();
      	try{
   			out.close();
   			in.close();
 			if (to_server != null)
   			to_server.close();
        }catch(IOException e){ System.out.println("IO Exception " + e); }
        connect_state = false;  //no longer connected
    }
   
}

// sends the string through socket to server
public void send_stream(String text) {
  System.out.println("Sending " + text);
	out.println(text);
	out.flush();
}

// receives a string through the socket and returns it
public String get_input() {
   try {
   	  String str = in.readLine() + '\n';
   	  System.out.println("Got " + str);
     	return (str); //return the input stream
   } catch (IOException e) {System.out.println("IO exception: " + e); }
   return null;
}

// try to connect to a server and return whether or not it was successful
public boolean connect(String host, int port) {

    try{
      	try{
  			to_server = new Socket(host,port);
      	}catch(UnknownHostException e){return false;}

 		in = new DataInputStream(new BufferedInputStream(to_server.getInputStream()));
 		out = new PrintStream (new
 		BufferedOutputStream(to_server.getOutputStream(),1024), false);
    } catch(IOException e){System.out.println("IO Exception " + e); }
    connect_state = true;  //make a connection
 	return true;
}

// cleanup routine that shuts things down 
public void cleanup() {

  	if (out != null){
    	out.println("!!quit!!");
    	out.flush();
  	}
  	try{
    	if (out != null)
      		out.close();
    	if (in != null)
      		in.close();
    	if (to_server != null)
      		to_server.close();
  	}catch(IOException e){System.out.println("IO Exception " + e); }
  	connect_state = false;  //no longer connected
}


}