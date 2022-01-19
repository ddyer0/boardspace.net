package lehavre.view;

import java.awt.*;
import java.io.*;
import java.util.*;

import lehavre.main.NetworkInterface;

/**
 *
 *	The <code>GUIHelper</code> class is contains the GUI settings
 *	for the windows used in the game. The window is specified
 *	during cnstruction.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/17
 */
public final class GUIHelper
{
	/** The path to the bold font file. */
	private static final String BOLD_FONT_PATH = "lehavre/fonts/BERNHC.TTF";

	/** The path to the card font file. */
	private static final String CARD_FONT_PATH = "lehavre/fonts/Oz-Handicraft-Win95BT.ttf";

	/** The path to the file containing the configuration. */
	private static final String CONFIG_PATH = "lehavre/config/gui.txt";

	/** The hashtable saving the data read from the settings file. */
	private static Hashtable<String, Object> settings=null;

	static Font boldFont = null;
	public static void setBoldFont(Font f) { boldFont = f; }
	public static Font getBoldFont()
	{	return(boldFont);
	}
	static Font plainFont = null;
	public static Font getPlainFont()
	{	return(plainFont);
	}
	public synchronized static void getGuiData(NetworkInterface network)
	{
		// move the creation of the base fonts to this, which is called statically
		// when the client is initialized
		if(boldFont==null)
		{
			try {
				plainFont = Font.createFont(Font.TRUETYPE_FONT, network.getStream(BOLD_FONT_PATH));
				//GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
			} catch(Exception e) {
				e.printStackTrace();
			}
		
		}
		if(plainFont==null)
		{
			try {
				plainFont = Font.createFont(Font.TRUETYPE_FONT, network.getStream(CARD_FONT_PATH));
				//GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
			} catch(Exception e) {
				e.printStackTrace();
			}
		
		}
		if(settings==null)
		{
		settings = new Hashtable<String, Object>();
		try {
			BufferedReader reader = new BufferedReader(network.getReader(CONFIG_PATH));
			String line;
			while((line = reader.readLine()) != null) {
				int k = line.indexOf("#");
				if(k >= 0) line = line.substring(0, k);
				line = line.trim();
				if(line.length() == 0) continue;
				String[] pairs = line.split("\\s+");
				String key = pairs[0];
				Object[] values = new Object[pairs.length - 1];
				for(int i = 0; i < values.length; i++) values[i] = getNumber(pairs[i + 1]);

				/* Single value read */
				if(values.length == 1) {
					settings.put(key, values[0]);
					continue;

				/* Point read */
				} else if(key.endsWith("Offset")) {
					settings.put(key, new Point((Integer)values[0], (Integer)values[1]));
					continue;

				/* Dimension read */
				} else if(key.endsWith("Size")) {
					settings.put(key, new Dimension((Integer)values[0], (Integer)values[1]));
					continue;

				/* Color read */
				} else if(key.endsWith("Color")) {
					settings.put(key, new Color((Integer)values[0], (Integer)values[1], (Integer)values[2]));
					continue;

				/* Rectangle read */
				} else if(key.endsWith("Bounds")) {
					settings.put(key, new Rectangle((Integer)values[0], (Integer)values[1], (Integer)values[2], (Integer)values[3]));
					continue;

				/* Insets read */
				} else if(key.endsWith("Padding")) {
					settings.put(key, new Insets((Integer)values[0], (Integer)values[1], (Integer)values[2], (Integer)values[3]));
					continue;

				/* Multiple values read */
				} else {
					boolean integers = true;
					for(Object value: values) {
						if(value instanceof Double) {
							integers = false;
							break;
						}
					}
					int i = 0;
					if(integers) {
						int[] array = new int[values.length];
						for(Object value: values) array[i++] = (Integer)value;
						settings.put(key, array);
					} else {
						double[] array = new double[values.length];
						for(Object value: values) array[i++] = (Double)value;
						settings.put(key, array);
					}
					continue;
				}
			}
			reader.close();
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		}
	}

	/**
	 *	Returns the given string parsed to a number.
	 *	@param string the string
	 */
	private static Object getNumber(String string) {
		if(string == null) return 0;
		if(string.contains(".")) return Double.parseDouble(string);
		return Integer.parseInt(string);
	}

	//================================================================================================= MAIN PART

	/** The prefix representing the window. */
	private final String prefix;

	/**
	 *	Creates a new <code>GUIHelper</code> instance
	 *	for the window represented by the given prefix.
	 *	@param prefix the prefix
	 */
	public GUIHelper(String prefix) {
		this.prefix = prefix;
	}

	/**
	 *	Returns the value of the entry with the given key.
	 *	@param key the key
	 *	@return the value
	 */
	private Object get(String key) {
		return settings.get(prefix + key);
	}

	/**
	 *	Returns the integer value of the entry with the given key.
	 *	@param key the key
	 *	@return the value
	 */
	public int getInt(String key) {
		Object value = get(key);
		if(value instanceof Integer) return (Integer)value;
		return 0;
	}

	/**
	 *	Returns the double value of the entry with the given key.
	 *	@param key the key
	 *	@return the value
	 */
	public double getDouble(String key) {
		Object value = get(key);
		if(value instanceof Double) return (Double)value;
		return 0;
	}

	/**
	 *	Returns the point saved under the given key.
	 *	@param key the key
	 *	@return the point
	 */
	public Point getOffset(String key) {
		Object value = get(key + "Offset");
		if(value instanceof Point) return new Point((Point)value);
		return null;
	}

	/**
	 *	Returns the dimension saved under the given key.
	 *	@param key the key
	 *	@return the dimension
	 */
	public Dimension getSize(String key) {
		Object value = get(key + "Size");
		if(value instanceof Dimension) return new Dimension((Dimension)value);
		return null;
	}

	/**
	 *	Returns the color saved under the given key.
	 *	@param key the key
	 *	@return the color
	 */
	public Color getColor(String key) {
		Object value = get(key + "Color");
		if(value instanceof Color) {
			Color color = (Color)value;
			return new Color(color.getRed(), color.getGreen(), color.getBlue());
		}
		return null;
	}

	/**
	 *	Returns the rectangle saved under the given key.
	 *	@param key the key
	 *	@return the color
	 */
	public Rectangle getBounds(String key) {
		Object value = get(key + "Bounds");
		if(value instanceof Rectangle) return new Rectangle((Rectangle)value);
		return null;
	}

	/**
	 *	Returns the insets saved under the given key.
	 *	@param key the key
	 *	@return the color
	 */
	public Insets getPadding(String key) {
		Object value = get(key + "Padding");
		if(value instanceof Insets) {
			Insets insets = (Insets)value;
			return new Insets(insets.top, insets.left, insets.bottom, insets.right);
		}
		return null;
	}

	/**
	 *	Returns the int array saved under the given key.
	 *	@param key the key
	 *	@return the int array
	 */
	public int[] getIntArray(String key) {
		Object value = settings.get(prefix + key);
		if(value instanceof int[]) return (int[])value;
		return null;
	}

	/**
	 *	Returns the double array saved under the given key.
	 *	@param key the key
	 *	@return the double array
	 */
	public double[] getDoubleArray(String key) {
		Object value = settings.get(prefix + key);
		if(value instanceof double[]) return (double[])value;
		return null;
	}
}