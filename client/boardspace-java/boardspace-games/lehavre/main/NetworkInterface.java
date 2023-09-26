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

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.JFrame;

import lehavre.model.GameState;

public interface NetworkInterface {
	public boolean isStandaloneGame();
	public AddressInterface getCreator();
	public AddressInterface getSelf();
	public boolean isConnected();
	public boolean isOpen();
	public void quit();
	public void send(AddressInterface address, Order order);
	public void login(String cluster, String address, String name, GameState state) ;
	
	// support for reading text files and images.  Applets must be careful to filter
	// through the root applet.
	public boolean fileExists(String file);
	public InputStreamReader getReader(String file) throws IOException;
	public InputStream getStream(String file) throws IOException;
	public Image getImage(String file);
	public Image getScaledImage(Image im,double scale);
	public JFrame getFrame();
}
