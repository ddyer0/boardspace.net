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
public interface LoaderConfig {
	static String protocol = "https";
	
	/**
	 * the web host to contact. Preferably by https, but fallback to http if that fails.
	 */
	static final String serverName = "boardspace.net";		// host to run from
	/**
	 * this is the root class expected to be present in the list of jars.  It should
	 * contain the standard jar manifest that lists all the other classes.
	 */
	static final String cacheRoot = "OnlineLobby.jar";		// jar file at the root of the app
	/**
	 * this is the runnable class that is actually run from the cache
	 */
	static final String runClass = "util.JWSApplication";		// class to run
	 	
	/**
	 * this url will fetch the list of jars to be cached.  For a simple enough application, 
	 * it could be just a text file to be fetched, but the intended url would be a simple
	 * perl program.
	 */
	static final String webUrl = "/cgi-bin/applettag.cgi?tagname=appjarinfo";	// url to fetch the list of jars
	/**
	 * this url is a logging application, specifically to log problems the end user
	 * has with this application.  there's no other way you'll ever know!
	 */
    static final String errorURL = "/cgi-bin/error.cgi";	
    
	static final String MiniloaderVersion = "1.7";
				// version 1.4 adds a retry in copyurl
				// version 1.5 adds a -setup arg that sends the rest of args to the app
				// version 1.6 removed a "synchronized" that caused deadlocks
				// version 1.7 adds limits of errors associated with storage full
	
    static final String MiniloaderId = "miniloader=version-"+MiniloaderVersion;

	/**
	 * these args are passed to the runnable class as system properties.
	 */
	static final String[] runArgs = 
		{ "servername",serverName,
		  "miniloader",MiniloaderVersion,
		 // "reviewonly","true",
		 // "gamename","tintas",
		 // "reviewerdir0","/tintas/tintasgames/",
		  };	// args to set as properties for the run

}
