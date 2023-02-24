package bridge;

import com.codename1.ui.Font;
import com.codename1.ui.list.DefaultListCellRenderer;
import com.codename1.ui.list.ListModel;
import lib.AwtComponent;
import lib.G;

//
// the overall behavior of codename1 List is altered so you can
// always drag without selecting an item, but press-move or long press
//
public class List<T> extends com.codename1.ui.List<T> implements ActionProvider,AwtComponent
{
	MouseAdapter mouse = new MouseAdapter(this);
	boolean down = false;
	boolean dragged = false;
	boolean longPress = false;
	int pressx = 0;
	int pressy = 0;
	public Color getBackground() { return(new Color(getStyle().getBgColor())); }
	public Color getForeground() { return(new Color(getStyle().getFgColor())); }

	public List(int i, boolean b)
	{	super();
		setFireOnClick(false);
		setScrollToSelected(false);
		DefaultListCellRenderer<?> render = ((DefaultListCellRenderer<?>)getRenderer());
		render.setAlwaysRenderSelection(true); 
		G.Assert(!b,"mult mode not supported");		
	}
	public List(ListModel<T> m) { super(m); }
	
	public void invalidate() {}
	public Font getFont() { return(G.getFont(getStyle())); }

	public void clearSelection() { }

	public void repaint() 
	{ 	if(MasterForm.isCompletelyVisible(this))
		{
		  super.repaint();
		} 
	}
	public void addMouseListener(MouseListener m)
	{
		mouse.addMouseListener(m);
	}
	public void addItemListener(ItemListener fileSelector) 
	{ mouse.addItemListener(fileSelector); 
	}
	public void addListSelectionListener(ListSelectionListener l)
	{
		mouse.addListSelectionListener(l);
	}
	
	public void setMultipleMode(boolean b) {
		G.Assert(!b,"mult mode not supported");		
	}

	public void removeAll() { while(size()>0) { getModel().removeItem(0); }}

	public void add(T elementAt)
	{ addItem(elementAt); 
	}
	
	public int getItemCount() {	 return(size()); }

	public T getItem(int i) { return(getModel().getItemAt(i)); }

	public void setBounds(int x,int y,int w, int h)
	{	setX(x);
		setY(y);
		setWidth(w);
		setHeight(h);
	}

	// this keeps the selection highlight from showing up at the previous
	// selection location.
	public int getCurrentSelected()
	{
		if(down) 
			{ return(-1); // this should be    pointerSelect(pressx,pressy)

			}
		return getSelectedIndex();
	}
	
	
	// suppress the pointer down so we don't select on finger press
	public void pointerPressed(int x,int y)
	{	// override and suppress pointerpressed, so just clicking 
		// does nothing.  If you move before release, it will be a
		// drag, if not it will be a release.
		dragged = false;
		longPress = false;
		pressx = x;
		pressy = y;
		down = true;
	}

	
	// activate on pointer released if there is no drag
	public void pointerReleased(int x,int y)
	{	down = false;
		// this is carefully balanced, the "pressed" if we didn't drag does
		// the selection.  The "released" in either case cancels the drag so
		// the scroll sticks.
		if(!(dragged || longPress))
		{ super.pointerPressed(pressx,pressy); 
		}
		super.pointerReleased(x,y); 
	}

	// drag with no selection
	public void pointerDragged(int x,int y)
	{	down = true;
		dragged = true;
		super.pointerDragged(x,y);
	}
	// long press also suppressed
	public void longPointerPress(int x,int y)
	{	// this is carefully balanced, the "pressed" if we didn't drag does
		// the selection.  The "released" in either case cancels the drag so
		// the scroll sticks.
		dragged = false;
		longPress = true;
		pressx = x;
		pressy = y;
		down = true;
	}
	public FontMetrics getFontMetrics(Font f) {
		return G.getFontMetrics(f);
	}
}
