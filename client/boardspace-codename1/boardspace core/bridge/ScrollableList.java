package bridge;

//
//this class is an attempt to integrate scrolling
//with selection.  If you click you get a selection
//if you click and drag you get scrolling, and no selection
//
public class ScrollableList<E> extends List<E> implements MouseListener
{	/**
	 * 
	 */
	static final long serialVersionUID = 1L;

	public ScrollableList(int i, boolean b) 
	{
		super(i, b);
		addMouseListener(this);
	}
	// with the standard codename1 api, it's impossible to revert to a "nothing selected"
	// state.  This is a workaround, to allow something to be selected only when the button
	// is pressed.
	boolean nothingSelected = true;

	public void removeAll()
	{
		super.removeAll();
		nothingSelected = true;
	}
	public E getSelectedItem()
	{
		if(nothingSelected) { return(null); }
		return(super.getSelectedItem());
	}
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
		nothingSelected = false;
		
	}
	public void mouseEntered(MouseEvent e) {
		
		
	}
	public void mouseExited(MouseEvent e) {
		
	}
}