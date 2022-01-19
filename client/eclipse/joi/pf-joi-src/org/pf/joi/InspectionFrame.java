// ===========================================================================
// CONTENT  : CLASS InspectionFrame
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.0 - 13/03/2004
// HISTORY  :
//  13/03/2004  mdu  CREATED
//
// Copyright (c) 2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi ;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.pf.swing.SwingUtil;
import org.pf.swing.TakeAllLayoutManager;

// ===========================================================================
// IMPORTS
// ===========================================================================

/**
 * Instances of this class can create the window frame for an object inspection. 
 *
 * @author Manfred Duchrow
 * @version 1.0
 */
public class InspectionFrame extends JFrame implements ChangeListener, MouseListener
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private transient InspectionWindowController controller = null ;
	protected InspectionWindowController getController() { return controller ; }
	protected void setController( InspectionWindowController newValue ) { controller = newValue ; }
	
	private transient JPanel contentPanel = null ;
	protected JPanel getContentPanel() { return contentPanel ; }
	protected void setContentPanel( JPanel newValue ) { contentPanel = newValue ; }
		
	private transient int elementCount = 0 ;
	protected int getElementCount() { return elementCount ; }
	protected void setElementCount( int newValue ) { elementCount = newValue ; }
  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public InspectionFrame( InspectionWindowController controller )
  {
    super() ;
    this.setController( controller ) ;
    this.initialize() ;
  } // InspectionFrame() 

	// -------------------------------------------------------------------------

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
	/**
	 * This method is called for every mouse click in the tree component.  <br>
	 * It brings up the popup menu for context specific actions.
	 */
	public void mouseClicked( MouseEvent e )
	{
		JTabbedPane tabbedPane ;
		
		if ( e.getSource() instanceof JTabbedPane ) 
		{
			tabbedPane = (JTabbedPane)e.getSource() ;
			if ( ( e.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK ) > 0 )
			{
				this.createTabPopupMenu().show( tabbedPane, e.getX(), e.getY() ) ;
			}
		}
	} // mouseClicked() 

	// -------------------------------------------------------------------------
	
	public void addInspection( InspectionView inspectView )
	{
		InspectionView firstView ;
		JTabbedPane tabbedPane ;
		
		if ( this.getElementCount() == 0 )
		{
			this.getContentPanel().add( inspectView ) ;	
		}
		else
		{
			if ( this.getElementCount() == 1 )
			{
				firstView = (InspectionView)this.getContentPanel().getComponent(0) ;
				this.getContentPanel().removeAll() ;
				tabbedPane = this.createTabbedPane( firstView ) ;
				this.getContentPanel().add( tabbedPane ) ;	
			}
			else
			{
				tabbedPane = this.getTabbedPane() ;
			}
			this.addToTabbedPane( tabbedPane, inspectView ) ;
			tabbedPane.setSelectedComponent( inspectView ) ;
		}
		elementCount++ ;	
	} // addInspection() 

	// -------------------------------------------------------------------------
	
	public void replaceInspection( InspectionView inspectView )
	{
		JTabbedPane tabbedPane ;
		int currentIndex ;
		
		if ( this.getElementCount() > 0 )
		{
			if ( this.getElementCount() == 1 )
			{
				this.getContentPanel().removeAll() ;
				this.getContentPanel().add( inspectView ) ;
				this.getContentPanel().revalidate() ;	
			}
			else
			{
				tabbedPane = this.getTabbedPane() ;
				currentIndex = tabbedPane.getSelectedIndex() ;
				if ( currentIndex >= 0 )
				{
					tabbedPane.setComponentAt( currentIndex, inspectView ) ;
					tabbedPane.setTitleAt( currentIndex, inspectView.getDisplayName() ) ;
				}
			}
			this.getContentPanel().repaint() ;
			this.viewChanged( inspectView ) ;
		}		
	} // replaceInspection()

	// -------------------------------------------------------------------------
	
	public void removeInspection( InspectionView inspectView )
	{
		JTabbedPane tabbedPane ;
		InspectionView lastView ;
		int index ;

		if ( this.getElementCount() > 1 )
		{
			tabbedPane = this.getTabbedPane() ;
			index = tabbedPane.indexOfComponent( inspectView ) ;
			if ( index >= 0 )
			{
				tabbedPane.remove( index ) ;
				elementCount-- ;
			}
			if ( this.getElementCount() == 1 )
			{
				lastView = (InspectionView)tabbedPane.getComponent(0) ;
				this.releaseTabbedPane( tabbedPane ) ;
				this.getContentPanel().removeAll() ;
				this.getContentPanel().add( lastView ) ;
			}
		}
	} // removeInspection() 

	// -------------------------------------------------------------------------
	
	/* (non-Javadoc)
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	public void stateChanged(ChangeEvent e)
	{
		JTabbedPane tabbedPane ;
		InspectionView selectedView ;
		
		tabbedPane = (JTabbedPane)e.getSource() ;
		selectedView = (InspectionView)tabbedPane.getSelectedComponent() ;
		this.viewChanged( selectedView ) ; 
	} // stateChanged() 

	// -------------------------------------------------------------------------

	// =========================================================================
	// MouseListener interface
	// =========================================================================
	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent e)
	{
		this.mouseClicked(e) ;
	} // mousePressed()
	
	// -------------------------------------------------------------------------

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered(MouseEvent e)
	{
		// ignore
	} // mouseEntered()
	
	// -------------------------------------------------------------------------

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited(MouseEvent e)
	{
		// ignore
	} // mouseExited()
	
	// -------------------------------------------------------------------------

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent e)
	{
		// ignore
	} // mouseReleased()

	// -------------------------------------------------------------------------
		
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
	protected void initialize()
	{
		JPanel panel ;
		ImageIcon logo ;

		SwingUtil.current().centerFrame( this, 
							this.prefs().getWindowWidth(), this.prefs().getWindowHeight()  ) ;
		this.setTitle( this.composeTitle() ) ;
		logo = ImageProvider.instance().getLogoIcon() ;
		if ( logo != null )
			this.setIconImage( logo.getImage() ) ;

		this.addWindowListener( this.getController() ) ;
		
		panel = new JPanel( new TakeAllLayoutManager( this.prefs().getMainMargin() ) ) ;
		this.setContentPanel( panel ) ; 

		this.getContentPane().add( panel ) ;
	} // initialize() 

	// -------------------------------------------------------------------------

	protected JTabbedPane createTabbedPane( InspectionView firstComp )
	{
		JTabbedPane tabbedPane ;
		
		tabbedPane = new JTabbedPane() ;
		tabbedPane.addChangeListener( this ) ;
		tabbedPane.addMouseListener( this ) ;
		this.addToTabbedPane( tabbedPane, firstComp ) ;
		return tabbedPane ;
	} // createTabbedPane() 

	// -------------------------------------------------------------------------

	protected void releaseTabbedPane( JTabbedPane tabbedPane )
	{
		tabbedPane.removeChangeListener( this ) ;
	} // releaseTabbedPane() 

	// -------------------------------------------------------------------------

	protected void addToTabbedPane( JTabbedPane tabbedPane, InspectionView comp )
	{
		tabbedPane.addTab( comp.getDisplayName(), comp ) ;
	} // addToTabbedPane() 

	// -------------------------------------------------------------------------

	protected JTabbedPane getTabbedPane()
	{
		return (JTabbedPane)this.getContentPanel().getComponent(0) ;
	} // getTabbedPane() 

	// -------------------------------------------------------------------------

	protected void viewChanged( InspectionView view )
	{
		this.getController().viewSelected( view ) ; 
		this.setTitle( this.composeTitle() ) ;		
	} // viewChanged() 

	// -------------------------------------------------------------------------

	protected InspectionView getSelectedView()
	{
		JTabbedPane tabbedPane ;
		InspectionView view = null ;
		
		if ( this.getContentPanel().getComponent(0) instanceof JTabbedPane )
		{
			tabbedPane = (JTabbedPane)this.getContentPanel().getComponent(0) ;
			view = (InspectionView)tabbedPane.getSelectedComponent() ;
		}
		return view ;
	} // getSelectedView()

	// -------------------------------------------------------------------------

	protected String composeTitle()
	{
		String title		= null ;

		title = Inspector.getProgSignature() + " : " 
					+ this.getController().getInspectorId() + " (" +
						this.getController().getInspectedObjectTypeString() + ")" ;
		return title ;
	} // composeTitle() 

	// -------------------------------------------------------------------------

	protected JPopupMenu createTabPopupMenu()
	{
		JMenuItem menuItem	= null ;
		JPopupMenu popup		= null ;

		popup = new JPopupMenu() ;
		menuItem = this.createMenuItem(	"Close tab", InspectionWindowController.actCloseTab ) ;
		popup.add( menuItem ) ;

		return popup ;
	} // createTabPopupMenu() 

	// -------------------------------------------------------------------------

	protected JMenuItem createMenuItem( String text, String command )
	{
		JMenuItem menuItem	= null ;

		menuItem = new JMenuItem( text ) ;
		menuItem.setActionCommand( command ) ;
		menuItem.addActionListener( this.getController() ) ;

		return menuItem ;
	} // createMenuItem() 

	// -------------------------------------------------------------------------

	protected Preferences prefs()
	{
		return Preferences.instance() ;
	} // prefs() 

	// -------------------------------------------------------------------------

} // class InspectionFrame 
