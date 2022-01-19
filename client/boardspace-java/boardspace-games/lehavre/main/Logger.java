package lehavre.main;

import java.io.*;
import java.text.*;
import java.util.*;

/**
 *
 *	The <code>Logger</code> class writes any game events in a log file.
 *	This includes failures, error messages and the chat log.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/8
 */
 public final class Logger
 {

	/** The path to the log file. */
	private static final String LOG_PATH = "lehavre/logs/lehavre%s.log";

	/** The file name of the log file. */
	private String filename;

	/** The file handle for the log file. */
	private BufferedWriter log;

	/** The state of activation. */
	private boolean active = false;

	/**
	 *	Creates a new <code>Logger</code> instance.
	 *	Creates the log file and saves its file handle unless false provided.
	 *	@param active provide false to disable logging
	 */
	public Logger(boolean active) {
		this.active = active;
		if(!active) return;
		try {
			filename = String.format(LOG_PATH, new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
		} catch(Exception e) {
			e.printStackTrace();
			filename = null;
		}
	}

 	/**
	 *	Writes the given message into the log file.
	 *	@param message the message
	 *	@param format provide true to format the message
	 */
	public void write(String message, boolean format) {
		if(!active) return;
		if(message != null){
			message = String.format("%s\r\n", message.replaceAll("<[^>]+>", ""));
			String fm = new SimpleDateFormat().format(new Date());
			if(format) message = String.format("%s %s", fm, message);
		} else {
			message = "\r\n";
		}
		if(filename != null) {
			try {
				log = new BufferedWriter(new FileWriter(filename, true));
				log.write(message);
				log.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		System.out.print(message);
	}

	/**
	 *	Closes the log file.
	 */
	public void close() {
		active = false;
	}

	/**
	 *	Returns the string representation.
	 *	@return the string representation
	 */
	public String toString() {
		return String.format("%s[%s]", getClass().getSimpleName(), filename);
	}
}