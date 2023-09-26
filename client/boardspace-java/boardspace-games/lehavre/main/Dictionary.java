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
package lehavre.main;

import java.io.*;
import java.util.*;

import lib.G;

/**
 *	
 *
 *	The <code>Dictionary</code> class manages the game phrases.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/8
 */
public final class Dictionary
{
	/** The path to the file containing the language. */
	private static final String CONFIG_PATH = "lehavre/config/lang.txt";

	/** The path to the language directory. */
	public static final String LANG_PATH = "lehavre/lang";

	/** The path to the file containing the translated phrases. */
	private static final String DICT_PATH = "%s/%s.txt";

	/** The current language. */
	private String language;

	/** The hashtable containing the entries. */
	private final Hashtable<String, String> dictionary = new Hashtable<String, String>();

	/**
	 *	Creates a new <code>Dictionary</code> instance.
	 *	Fills the dictionary with entries for the current language.
	 *	@throws IOException if fails to read the language
	 */
	public Dictionary(NetworkInterface network,String lang) throws IOException {
		if(language==null)
			{
			if(lang==null)
				{
				
				if(network.fileExists(CONFIG_PATH))
				{
					BufferedReader reader = new BufferedReader(network.getReader(CONFIG_PATH));
					String rl = reader.readLine();
					if(rl!=null)
						{
						lang = rl.trim();
						}
					reader.close();
				}}
			if(lang==null)
				{
				Locale loc = Locale.getDefault();
				lang = loc.getLanguage();
				}
			load(lang,network);
			}
		
	}

	/**
	 *	Writes the given language to the language config file.
	 *	Will create the file if it doesn't exist.
	 *	@param language the language
	 *	@throws IOException if fails to write the language
	 */
	private void setLanguage(String language) throws IOException {
		G.Error("config path isn't set");
		BufferedWriter writer = new BufferedWriter(new FileWriter(CONFIG_PATH));
		writer.write("en");
		writer.close();
	}

	/**
	 *	Fills the dictionary with entries for the given language.
	 *	@param language the language
	 *	@return true if a new language was provided
	 *	@throws IOException if fails to read the language data
	 */
	public boolean load(String language,NetworkInterface network) throws IOException {
		if(language.equals(this.language)) return false;
		if(this.language != null) setLanguage(language);
		this.language = language;
		BufferedReader reader = new BufferedReader(network.getReader(String.format(DICT_PATH, LANG_PATH, language)));
		String current = null, line;
		while((line = reader.readLine()) != null) {
			if(line.startsWith("#") || line.trim().length() == 0) continue;
			String[] pair = line.split("\\t+");
			if(pair[0].length() > 0) {
				dictionary.put(current = pair[0], pair.length > 1 ? pair[1] : "");
			} else {
				dictionary.put(current, dictionary.remove(current) + " " + pair[1]);
			}
		}
		reader.close();
		return true;
	}

	/**
	 *	Returns the value of the entry with the given key.
	 *	@param key the key
	 *	@return	the value
	 */
	public String get(String key) {
		String it = dictionary.get(key);
		if(it==null)
		{
			System.out.println("Didn't find key: " + key);
			it = key;
		}
		return it.trim();
	}

	/**
	 *	Returns the language.
	 *	@return the language
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 *	Returns the string representation.
	 *	@return the string representation
	 */
	public String toString() {
		return String.format("%s[%s]", getClass().getSimpleName(), language);
	}
}