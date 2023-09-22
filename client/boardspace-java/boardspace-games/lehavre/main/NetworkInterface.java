/* copyright notice */package lehavre.main;

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
