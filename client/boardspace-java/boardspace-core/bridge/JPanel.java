package bridge;

import java.awt.Component;
import java.awt.Container;

public class JPanel extends javax.swing.JPanel 
{
	String title = "";
	private Container contentPane = new Container();
	public String getTitle() { return title; }
	public JPanel () { super(); }
	public JPanel(String p)
	{	super();
		title = p;
	}
	public void addC(Component p) {
		contentPane.add(p);
	}

	public void addC(String where, Component p) {
		contentPane.add(where,p);
	}

	public Container getContentPane() {
		return contentPane;
	}

	public void setContentPane(Container p) {
		contentPane = p;
	}
	public void dispose() {
	}
	
	public void addWindowListener(WindowListener who) {
		
	}
	


}
