/* copyright notice */package lehavre.view;

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import lehavre.main.*;

/**
 *
 *	The <code>ChatWindow</code> class is used for the communication between
 *	the players. It also serves as the game console that documents each action.
 *
 *	@author Grzegorz Kobiela
 *	@version 1.00 2009/1/17
 */
public final class ChatWindow
extends ControlledWindow
{
	static final long serialVersionUID =1L;
	/** The entire editor format. */
	private static final String CHAT_FORMAT = "<html><body style=\"font-family:monospace; font-size:%dpt;\">%s</body></html>";

	/** The entry format. */
	private static final String ENTRY_FORMAT = "<div style=\"color:#%s;\">%s %s</div>\n";

	/** The available message colors. */
	public static final String DEFAULT_COLOR = "000000";
	public static final String SELF_COLOR = "ff0000";
	public static final String SYSTEM_COLOR = "0000ff";

	/** The text area that displays the chat messages. */
	private final JEditorPane area;

	/** The message input box. */
	private final JTextField input;

	/** The messages displayed in the window. */
	private final ArrayList<String> messages;

	/* The maximum amount of displayed messages. */
	private final int messageLimit;

	/** The font size of the messages. */
	private final int fontSize;

	/**
	 *	Creates a new <code>ChatWindow</code> instance.
	 *	@param control the control object
	 */
	public ChatWindow(LeHavre control) {
		super(control, "chat");
		messages = new ArrayList<String>();
		messageLimit = gui.getInt("MessageLimit");
		fontSize = gui.getInt("FontHeight");

		/* Create the chat area */
		area = new JEditorPane();
		area.setEditable(false);
		area.setContentType("text/html");
		area.setBackground(gui.getColor("EditorPane"));
		area.setOpaque(true);
		JScrollPane scroll = new JScrollPane(area);
		scroll.getVerticalScrollBar().setUnitIncrement(gui.getInt("ScrollUnit"));
		getContentPane().add(scroll, BorderLayout.CENTER);

		/* Create the text input field */
		input = new JTextField();
		input.setBorder(BorderFactory.createLoweredBevelBorder());
		input.setBackground(gui.getColor("TextField"));
		input.setOpaque(true);
		input.addKeyListener(
			new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					if(e.getKeyCode() != KeyEvent.VK_ENTER) return;
					ChatWindow.this.control.sendMessage(filter(input.getText()));
					input.setText("");
				}
			}
		);
		getContentPane().add(input, BorderLayout.SOUTH);
		setBounds(gui.getBounds("Window"));
	}

	/**
	 *	Filters the given text for HTML special characters
	 *	and replaces them by their HTML entities.
	 *	@param text the text to filter
	 */
	public String filter(String text) {
		return text.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
	}

	/**
	 *	Displays the given message in the given color.
	 *	Use a class constant to address the color.
	 *	@param message the message
	 *	@param color the color
	 */
	public void write(String message, String color) {
		if(!isVisible()) control.openChat();
		messages.add(String.format(ENTRY_FORMAT, color, new SimpleDateFormat().format(new Date()), message));
		if(messages.size() > messageLimit) messages.remove(0);
		StringBuilder content = new StringBuilder();
		for(String msg: messages) content.append(msg);
		final String format = String.format(CHAT_FORMAT, fontSize, content);
		if(EventQueue.isDispatchThread()) area.setText(String.format(CHAT_FORMAT, fontSize, content));
		else {
			
			try {
				SwingUtilities.invokeLater(
					new Runnable() {
						public void run() {
							area.setText(format);
						}
					}
				);
			} catch(Exception e) {
				System.out.println("S: "+format);
				e.printStackTrace();
			}
		}
	}

	/**
	 *	Sets the chat window invisible.
	 */
	public void quit() {
		setVisible(false);
		control.chatClosed();
	}
}