/* Class    :  GameServer 
 * Author   :  Andrew Miller (rhuk@cis.ufl.edu)
 * Version  :  7/1/96  JAVA SDK 1.02
 * Notice   :  Copyright (C) 1996 Andrew Miller
 *
 * The central idea when designing this server was to make it as stupid as possible and yet
 * it must still be able to perform simple network duties.  The reason for this is that 
 * allowing the client have control and making it capable of determining game play makes 
 * the server more flexible.  You can be playing multiple games with multiple amounts of
 * people.  The server just doesnt care.  
 * 
 * It is responsible for:
 * 1:looking for connections 
 * 2:adding those connections to its global list (along with a unique ID number)
 * 3:broadcasting to all the connections the current list of users (and their unique IDs)
 * 4:support for name changing
 * 5:letting the other users know when a client has disconnected (so they can update their
 *   own user lists).
 * 6:looking for "directives" and distributing them to the correct clients.
 *
 * Any message received by the server is parsed to determine whether or not it is a special
 * instruction.  Directives are the most useful function that the server provides.  The 
 * server doesnt care what is contained in the server (usually game information), but it 
 * looks to the end of the directive for a list of clients to redistribute the directive to.
 * This allows for specific interaction among a selected group of clients.
 *
 *
 * Code and ideas for these classes comes in part from the following sources:
 *
 * - 'Java in a Nutshell' by David Flanagan
 * - 'Java Unleashed' by Sams Net
 * - 'Writing Java Applets' by John Rodley
 * - 'Simple Server' by Kyle Palmer
 * - 'Multiserver' by Jeff Breidenbach
 *  
 */

import java.net.*;
import java.io.*;
import java.lang.*;


//declaration of class GameServer
class GameServer {

  	static int users = 0;   	//the number of active users
  	static int unique_id = 1;  	//setup unique id number for users
  	static int DEFAULT_PORT = 8432;
  	static NetworkSocket clients[] = new NetworkSocket[29];
  
public static void main(String args[]) {
	
	ServerSocket serverSocket = null;
  	int i, port = 0, connectTest;
	boolean chatFlag = true;

  	if (args.length == 1) {
		try { port = Integer.parseInt (args[0]);}
		catch (NumberFormatException e) { port = 0;}
	}	
    if (port == 0) port = DEFAULT_PORT;

	try {
    	serverSocket = new ServerSocket(port);
  	} 
	catch (IOException e) {
    	System.out.println("Could not listen on port: " + e);
     	System.exit(1);
	}

	Socket clientSocket = null;
	Connection user = new Connection(serverSocket);

	if (users == 0){  //if no users, wait for one
		try {
    		clientSocket = serverSocket.accept();
    	} 
		catch (IOException e) {
      		System.out.println("Accept failed: " + e);
      		System.exit(1);
		}
		add_client(new NetworkSocket(clientSocket)); //add the client
	}
        
	String inputStream;		//string for Input IO
	String outputStream;	//string for Output IO
        
//get the vector the write to
        

	user.start();  //start polling for new users
	try {
    	inputStream = "Connected! Double-click user to challenge";
     	i = users;
  		while (users >= 0) {
        	if (users>0 && i<=users ){
             	connectTest = clients[i].is_readable();
                if (connectTest == -1){ 	//if Connection stale
                  	remove_client(i);  	//remove the client
                }
                else if (connectTest == 1){ 	//if there is a line to get
                  	inputStream = clients[i].read_stream();  	//get the string
         			chatFlag = process_line(inputStream,i); //process the line
                  	if (chatFlag){ 	//echo line if is plain text
                    	if (clients[i] != null)
							System.out.println(clients[i].name+": "+inputStream);
                      		write_all((clients[i].name).toUpperCase()+": "+inputStream);  //echo it
                  	}
               	}
         	}
        	try{
             	user.join(2); //give Connection testing some time
            } catch(InterruptedException e) {}
        	if (user.connected() == true){
                add_client(new NetworkSocket(user.get_socket()));
                user.resume();
			}
         	i++;
			if (i>users && users>=1) i=1;
     		if (i>users && users<1)	i=0;
		}
		serverSocket.close();
	}catch(IOException e){}
}

// sends all the names and IDs in the servers list to all the clients
static void send_names(){ 

    String names = new String("!!names!!"); //start with indicator
    int i;

    if (users < 1) return; // do nothing for a no users
    for (i=1;i<=users;i++)  //add all users
    	if (clients[i] != null) //if everthing is in order
        	names = names + Integer.toString(clients[i].id) +"~" + clients[i].name + "|";  //append names and delimiters
    write_all(names);  //echo the names to everyone
}

// process the incoming data stream and return a status flag
static boolean process_line (String line,int i){

    int next, usernum;
    String temp ;
    String directive = new String("!!directive!!");   //new indicator
    
    //false if don't echo line

    if (line.startsWith("!!directive!!")) {      //instruction to send directive to users
		line = line.substring(13);
		next = line.indexOf('|');  // get position of next delimeters
		directive = directive + line.substring(0, next)+","+clients[i].name;
 		line = line.substring(next+1);
		while (line.length() > 0) {    // send directive to players in list
			next = line.indexOf('|');
			if (next == -1) break;  // end of list
			temp = line.substring(0, next);  // check for range
			if ((Integer.parseInt(temp)) > users) break;
			clients[Integer.parseInt(temp)].write_stream(directive);
			line = line.substring(next+1);
    	}
    	return false;
    }
 	
    else if (line.startsWith("!!name!!")){  //instruction to change name
      	clients[i].name = line.substring(8);  //name is the rest of line
      	clients[i].write_stream("Name changed to "+line.substring(8)); 
		System.out.println (line.substring(8)+" enters");
      	send_names(); //send an updated names list
      	return false;  //don't echo this string
    }
    
	else if (line.equals("!!quit!!")){	  //instruction to quit 
      	write_all(clients[i].name+" is logging out."); //tell everyone
		System.out.println(clients[i].name+" logged out");
      	remove_client(i);
      	return false;
    }
    return true;

}

// writes the string to all the users contained in the server's list
static void write_all (String text) { //write to all the clients
    
	int i;
    
    for (i=1;i<=users;i++)
      	clients[i].write_stream(text);
}

// adds a client and assigns it a unique id, then updates client name lists
static  void add_client(NetworkSocket client) {
	synchronized(clients) {

		if (users >= 25){
			client.write_stream("Sorry, there already 25 users.");
			client.kill(); //don't allow too many
			return;
		}

		clients[++users] = client;  //add the client to the list
		clients[users].id = unique_id++;
		clients[users].write_stream("!!directive!!mssge,uid,"+Integer.toString(clients[users].id));
		clients[users].write_stream("Connected! Double-click user to challenge");
		send_names();  //update everyones names list
	}
}

// removes a client from the list and updates the client name lists
static  void remove_client(int client){  
	synchronized(clients) {

		clients[client].kill();  //close everything 
		clients[client]=clients[users];
		clients[users]=null;
		users--;
		if (users <= 0) unique_id = 1;
		send_names();  //update the names list
	}
}
  
}
