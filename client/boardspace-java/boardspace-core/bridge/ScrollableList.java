package bridge;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.JList;

@SuppressWarnings("rawtypes")
class ScrollableListModel<E> extends AbstractListModel
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Vector<E> data = new Vector<E>();

	public int getSize() {
		return(data.size());
	}

	public synchronized E getElementAt(int index) {
		return( (index<0 || index>=data.size()) ? null : data.elementAt(index));
	}
	public synchronized void add(E item) 
		{ data.addElement(item); 
		  fireContentsChanged(this,data.size(),data.size());
		}
	public void removeAll() { data.setSize(0); }
}
//
//this class is an attempt to integrate scrolling
//with selection.  If you click you get a selection
//if you click and drag you get scrolling, and no selection
//
@SuppressWarnings("rawtypes")
public class ScrollableList<E> extends JList implements MouseListener
{	/**
	 * 
	 */
	static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	public ScrollableList(int i, boolean b) 
	{
		super(new ScrollableListModel<E>());
		addMouseListener(this);
	}
	// with the standard codename1 api, it's impossible to revert to a "nothing selected"
	// state.  This is a workaround, to allow something to be selected only when the button
	// is pressed.
	boolean nothingSelected = true;


	@SuppressWarnings("unchecked")
	public void removeAll()
	{	nothingSelected = true;
		clearSelection();
		((ScrollableListModel<E>)getModel()).removeAll();
		super.removeAll();
		invalidate();
	}
	public E getSelectedItem()
	{
		if(nothingSelected) { return(null); }
		int index = getSelectedIndex();
		return(getItem(index));
	}
	@SuppressWarnings("unchecked")
	public E getItem(int index)
	{
		return (E)(getModel().getElementAt(index));
	}
	public void add(E item)
	{	
		@SuppressWarnings("unchecked")
		ScrollableListModel<E> model = (ScrollableListModel<E>)getModel();;
		model.add(item);
		invalidate();
		
	}
	public int getItemCount() { return(getModel().getSize());}
	public int getSelectedIndex()
	{
		if(nothingSelected) { return(-1); }
		else { return(super.getSelectedIndex());}
	}

	public void mouseClicked(MouseEvent e) {
		nothingSelected = false;
		
	}
	public void mousePressed(MouseEvent e) {
		nothingSelected = false;
	}
	public void mouseReleased(MouseEvent e) {
		
	}
	public void mouseEntered(MouseEvent e) {
		
		
	}
	public void mouseExited(MouseEvent e) {
		
	}
}