// ===========================================================================
// CONTENT  : CLASS BasicInspector
// AUTHOR   : Manfred Duchrow
// VERSION  : 1.2 - 12/04/2004
// HISTORY  :
//  16/11/1999  duma  CREATED
//	11/01/2000	duma	added		-> actCloseAll, actBasicInspect
//	13/02/2004	duma	changed	-> Re-design to support tabbed object inspection
//	09/04/2004	duma	changed	-> contentArea is now an ObjectValuePanel
//
// Copyright (c) 1999-2004, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.joi;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.PrintStream;
import java.lang.reflect.Modifier;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.pf.text.StringUtil;

/**
 * This is the default inspector, which can display all normal java objects.
 * Currently it opens a window on the object to inspect and displays all
 * of its fields that are <b>not</b> <i>static</i> and <b>not</b> <i>final</i>,
 * which means no class variables and no constants, but all instance variables
 * (including inherited attributes).<br>
 * For arrays it lists up all elements from 0 to n.<br>
 * For deeper inspection it is possible to open a new inspector on each
 * attribute.<br>
 * <br>
 * Here is an example how to use the inspector:<br>
 * <ul><code>
 * panel = new JPanel() ;<br>
 * BasicInspector.inspect( panel ) ;
 * </code></ul>
 *
 * @author Manfred Duchrow
 * @version 1.2
 * @since JDK 1.2
 */
public class BasicInspector extends Inspector 
													implements ActionListener, TreeSelectionListener
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
	protected static final int MIN_TREE_WIDTH						= 100 ;
	protected static final int MIN_TREE_HEIGHT					= 2000 ;
	protected static final int MIN_INFO_WIDTH						= 250 ;

	protected static final PrintStream errorDevice			= System.out ;
	
	protected static final String actSaveModifiedValue	= "save.modified.value" ;
	protected static final String actResetModifiedValue	= "reset.modified.value" ;
	protected static final String actSetValueToNull			= "set.value.to.null" ;
	
  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
	private InspectionWindowController controller = null ;
	protected InspectionWindowController getController() { return controller ; }
	protected void setController( InspectionWindowController newValue ) { controller = newValue ; }

	private InspectionView associatedView = null ;
	protected InspectionView getAssociatedView() { return associatedView ; }
	protected void setAssociatedView( InspectionView newValue ) { associatedView = newValue ; }
		
  private ObjectValuePanel contentArea = null ;
  protected ObjectValuePanel getContentArea() { return contentArea ; }
  protected void setContentArea( ObjectValuePanel newValue ) { contentArea = newValue ; }	

  private JTextField declaredTypeField = null ;
  protected JTextField getDeclaredTypeField() { return declaredTypeField ; }
  protected void setDeclaredTypeField( JTextField aValue ) { declaredTypeField = aValue ; }

  private JTextField actualTypeField = null ;
  protected JTextField getActualTypeField() { return actualTypeField ; }
  protected void setActualTypeField( JTextField aValue ) { actualTypeField = aValue ; }

  private Spy currentElement = null ;
  protected Spy getCurrentElement() { return currentElement ; }
  protected void setCurrentElement( Spy aValue ) { currentElement = aValue ; }

  private JTree elementTree	= null ;

  protected JTree getElementTree() { return elementTree ; }
  protected void setElementTree( JTree aValue ) { elementTree = aValue ; }

  // =========================================================================
  // CLASS METHODS
  // =========================================================================

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with default values.
   */
  public BasicInspector()
  {
  	super() ;
  } // BasicInspector() 

  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================

  /**
   * Is called whenever the selection in the element tree changed.    <br>
   * It updates the information in the right section of the inspector window.
   *
   * @see javax.swing.event.TreeSelectionListener
   */
	public void valueChanged( TreeSelectionEvent event )
	{
		TreePath treePath 		= null ;
		SpyTreeNode treeNode	= null ;
		Spy spy								= null ;

		treePath = event.getPath() ;
		treeNode = (SpyTreeNode)treePath.getLastPathComponent() ;
		spy = treeNode.getModel() ;
		this.setCurrentElement( spy ) ;
		this.updateInformation( spy ) ;
	} // valueChanged() 

  // -------------------------------------------------------------------------

	/**
	 * Is called whenever a user action event occured.    <br>
	 * This method is actually performing all actions, triggered by buttons,
	 * keystrokes or menu items.
	 *
	 * @param e The action event holding further information on what happened.
	 */
	public void actionPerformed(ActionEvent e)
	{
		if ( e.getActionCommand().equals( InspectionWindowController.actBasicInspectWindow ) )
			this.basicInspectCurrentElement( InspectionWindowController.INSPECT_IN_NEW_WINDOW ) ;
		else if ( e.getActionCommand().equals( InspectionWindowController.actBasicInspectTab ) )
			this.basicInspectCurrentElement( InspectionWindowController.INSPECT_IN_NEW_TAB ) ;
		else if ( e.getActionCommand().equals( InspectionWindowController.actBasicInspectHere ) )
			this.basicInspectCurrentElement( InspectionWindowController.INSPECT_IN_CURRENT_PLACE ) ;
		else if ( e.getActionCommand().equals( InspectionWindowController.actInspectHere ) )
			this.inspectCurrentElement( InspectionWindowController.INSPECT_IN_CURRENT_PLACE ) ;
		else if ( e.getActionCommand().equals( InspectionWindowController.actInspectWindow ) )
			this.inspectCurrentElement( InspectionWindowController.INSPECT_IN_NEW_WINDOW ) ;
		else if ( e.getActionCommand().equals( InspectionWindowController.actInspectTab ) )
			this.inspectCurrentElement( InspectionWindowController.INSPECT_IN_NEW_TAB ) ;
		else if ( e.getActionCommand().equals( actSaveModifiedValue ) )
			this.saveModifiedValue() ;
		else if ( e.getActionCommand().equals( actResetModifiedValue ) )
			this.resetModifiedValue() ;
		else if ( e.getActionCommand().equals( actSetValueToNull ) )
			this.setCurrentValueToNull() ;
	} // actionPerformed() 

	// -------------------------------------------------------------------------

	/**
	 * This method is called for every mouse click in the tree component.  <br>
	 * It brings up the popup menu for context specific actions.
	 */
	public void mouseClicked(MouseEvent e)
	{
		JTree tree						= null ;
		TreePath treePath			= null ;

		tree = (JTree)e.getSource() ;
		treePath = tree.getPathForLocation( e.getX(), e.getY() ) ;
		tree.setSelectionPath( treePath ) ;
		if ( ( ( e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK ) > 0 ) &&
				 ( e.getClickCount() == 2 ) )		// Double-click
		{
			this.inspectCurrentElement( prefs().getDoubleClickMode() ) ;
		}
		else if ( ( e.getModifiersEx() & MouseEvent.BUTTON2_DOWN_MASK ) > 0 )
		{
			this.inspectCurrentElement( prefs().getMiddleButtonMode() ) ;
		}
		else
		{
			// if ( e.isPopupTrigger() )   !!!!! DOES NOT WORK !!!!
			if ( ( e.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK ) > 0 )
			{
				this.createElementPopupMenu().show( tree, e.getX(), e.getY() ) ;
			}
		}
	} // mouseClicked() 

	// -------------------------------------------------------------------------
	
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================

	protected void inspectCurrentElement( int where )
	{
		if ( this.getCurrentElement() != null )
		{
			try
			{
				if ( where == InspectionWindowController.INSPECT_IN_NEW_WINDOW )
				{
					Inspector.inspect( this.getCurrentElement().getValue() ) ;
				}
				else
				{
					Inspector.inspectIn(	this.getController(), where,  
																this.getCurrentElement().getName(), 
															 	this.getCurrentElement().getValue() ) ;
				}
			}
			catch ( Exception ex )
			{
				ex.printStackTrace( errorDevice ) ;
			}
		}
	} // inspectCurrentElement() 

	// -------------------------------------------------------------------------

	protected void basicInspectCurrentElement( int where )
	{
		if ( this.getCurrentElement() != null )
		{
			try
			{
				if ( where == InspectionWindowController.INSPECT_IN_NEW_WINDOW )
				{
					Inspector.basicInspect( this.getCurrentElement().getValue() ) ;
				}
				else
				{
					Inspector.basicInspectIn( this.getController(), where, 
																		this.getCurrentElement().getName(), 
																 		this.getCurrentElement().getValue() ) ;
				}
			}
			catch ( Exception ex )
			{
				ex.printStackTrace( errorDevice ) ;
			}
		}
	} // basicInspectCurrentElement() 

	// -------------------------------------------------------------------------

	/**
	 * Inspect the given object.   <br>
	 * That means to display the internal state of the given object's attributes.
	 *
	 * @param obj The object to look inside
	 */
	protected void inspectObject( String name, Object obj )
	{
		super.inspectObject( name, obj ) ;
		if ( this.prefs().isAutoSortOn() )
		{
			this.getInspectedObject().sortElements() ;
		}
	} // inspectObject() 

  // -------------------------------------------------------------------------
	
	protected String getObjectDisplayName()
	{
		return this.getInspectedObject().getName() ;	
	} // getObjectDisplayName() 

	// -------------------------------------------------------------------------	

	protected InspectionView getInspectionView()
	{
		if ( this.getAssociatedView() == null )
		{
			this.setAssociatedView( this.buildInspectionView() ) ;
		}
		return this.getAssociatedView() ;
	} // getInspectionView() 

	// -------------------------------------------------------------------------

	protected InspectionView buildInspectionView()
	{
		InspectionView panel ;
		JSplitPane splitPane		= null ;
		JTextField dclField			= null ;
		JTextField actField			= null ;
		JPanel columnPanel			= null ;
		JPanel typePanel				= null ;
		JPanel infoPanel				= null ;

		this.setContentArea( new ObjectValuePanel(this) ) ;

		dclField = new JTextField() ;
		dclField.setEditable( false ) ;
		this.setDeclaredTypeField( dclField ) ;

		actField = new JTextField() ;
		actField.setEditable( false ) ;
		this.setActualTypeField( actField ) ;

		typePanel = new JPanel( new BorderLayout() ) ;

		columnPanel = new JPanel( new GridLayout( 2, 0 ) ) ;
		columnPanel.add( new JLabel( "DECLARED : " ) ) ;
		columnPanel.add( new JLabel( "ACTUAL : " ) ) ;
		typePanel.add( columnPanel, BorderLayout.WEST ) ;

		columnPanel = new JPanel( new GridLayout( 2, 0 ) ) ;
		columnPanel.add( dclField ) ;
		columnPanel.add( actField ) ;
		typePanel.add( columnPanel, BorderLayout.CENTER ) ;

		infoPanel = new JPanel( new BorderLayout() ) ;
		infoPanel.setMinimumSize( new Dimension( MIN_INFO_WIDTH, 200 ) ) ;
		infoPanel.add( typePanel, BorderLayout.NORTH ) ;
		infoPanel.add( this.getContentArea(), BorderLayout.CENTER ) ;

		splitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, true ) ;
		splitPane.setDividerLocation( prefs().getTreeWidth() );
		splitPane.setDividerSize(3);
		splitPane.setLeftComponent( this.createTreePanel() );
		splitPane.setRightComponent( infoPanel );
		panel = new InspectionView( this ) ;
		panel.add( splitPane ) ;
		return panel ;		
	} // buildInspectionView() 

	// -------------------------------------------------------------------------

	protected JMenuItem createMenuItem( String text, String command )
	{
		JMenuItem menuItem	= null ;

		menuItem = new JMenuItem( text ) ;
		menuItem.setActionCommand( command ) ;
		menuItem.addActionListener( this ) ;

		return menuItem ;
	} // createMenuItem() 

  // -------------------------------------------------------------------------

	protected JMenu createMenu( String text )
	{
		return ( new JMenu( text ) ) ;
	} // createMenu() 

  // -------------------------------------------------------------------------

	protected JPopupMenu createElementPopupMenu()
	{
		JMenuItem menuItem	= null ;
		JPopupMenu popup		= null ;

		popup = new JPopupMenu() ;
		menuItem = this.createMenuItem(	InspectionWindowController.TXT_INSPECT_HERE, 
																		InspectionWindowController.actInspectHere  ) ;
		popup.add( menuItem ) ;
		menuItem = this.createMenuItem(	InspectionWindowController.TXT_INSPECT_IN_NEW_TAB, 
																		InspectionWindowController.actInspectTab  ) ;
		popup.add( menuItem ) ;
		menuItem = this.createMenuItem(	InspectionWindowController.TXT_INSPECT_IN_NEW_WINDOW, 
																		InspectionWindowController.actBasicInspectWindow ) ;
		popup.add( menuItem ) ;
		
		popup.addSeparator() ;
		
		menuItem = this.createMenuItem(	InspectionWindowController.TXT_BASIC_INSPECT_HERE, 
																		InspectionWindowController.actBasicInspectHere ) ;
		popup.add( menuItem ) ;
		menuItem = this.createMenuItem(	InspectionWindowController.TXT_BASIC_INSPECT_IN_NEW_TAB, 
																		InspectionWindowController.actBasicInspectTab ) ;
		popup.add( menuItem ) ;
		menuItem = this.createMenuItem(	InspectionWindowController.TXT_BASIC_INSPECT_IN_NEW_WINDOW, 
																		InspectionWindowController.actBasicInspectWindow ) ;
		popup.add( menuItem ) ;

		return popup ;
	} // createElementPopupMenu() 

  // -------------------------------------------------------------------------

	protected JComponent createTreePanel()
	{
		JScrollPane scrollPane 						= null ;
		JTree tree												= null ;
		ObjectSpyTreeNode rootNode				= null ;
		DefaultTreeCellRenderer renderer	= null ;
		int rowHeight											= 0 ;
		int treeHeight										= MIN_TREE_HEIGHT ;

		rootNode = this.createTreeNodes() ;
		tree = new JTree( rootNode ) ;
		this.setElementTree(tree) ;
		renderer = new InspectionRenderer() ;
		tree.setCellRenderer( renderer ) ;
		rowHeight = renderer.getLeafIcon().getIconHeight() + 1 ;
		tree.setRowHeight( rowHeight ) ;
		treeHeight = ( rowHeight * (this.getInspectedObject().getFullElementCount() + 1) ) + 10 ;
		tree.setPreferredSize( new Dimension( prefs().getTreeWidth(), treeHeight ) ) ;
		tree.addTreeSelectionListener( this ) ;
		tree.addMouseListener( this ) ;
		tree.setSelectionRow( 0 ) ;
		scrollPane = new JScrollPane(tree);
		scrollPane.setMinimumSize( new Dimension( MIN_TREE_WIDTH, treeHeight ) ) ;

		return scrollPane ;
	} // createTreePanel() 

  // -------------------------------------------------------------------------

	protected String getTypeStringOf( Spy spy )
	{
		String typeString 		= "" ;

		typeString = spy.getTypeString() ;
		typeString = Modifier.toString( spy.getModifiers() ) + " " + typeString ;
		return typeString.trim() ;
	} // getTypeStringOf() 

  // -------------------------------------------------------------------------

	protected String getValueTypeStringOf( Spy spy )
	{
		String typeString	= null ;

		try
		{
			typeString = spy.getValueTypeString() ;
		}
		catch ( Exception ex )
		{
			ex.printStackTrace( errorDevice ) ;
		}
		return typeString ;
	} // getValueTypeStringOf() 

  // -------------------------------------------------------------------------

	protected String getValueStringOf( Spy spy )
	{
		String valueString 		= "" ;

		try
		{
			valueString = spy.getValueString() ;
		}
		catch ( Exception ex )
		{
			ex.printStackTrace( errorDevice ) ;
			valueString = ex.toString() ;
		}
		return valueString ;
	} // getValueStringOf() 

  // -------------------------------------------------------------------------

	protected Component getValueComponentOf( Spy spy )
	{
		try
		{
			return spy.getValueComponent() ;
		}
		catch ( Exception ex )
		{
			ex.printStackTrace( errorDevice ) ;
		}
		return null ;
	} // getValueComponentOf() 

  // -------------------------------------------------------------------------
		
	protected Object getValueOf( Spy spy )
	{
		Object value ;

		try
		{
			value = spy.getValue() ;
		}
		catch ( Exception ex )
		{
			ex.printStackTrace( errorDevice ) ;
			value = ex.toString() ;
		}
		return value ;
	} // getValueOf() 

  // -------------------------------------------------------------------------

  /**
   * Is called whenever the selection in the element tree changed.
   * @see javax.swing.event.TreeSelectionListener
   */
	protected void updateInformation( Spy spy )
	{
		Object value ;
		
		this.getDeclaredTypeField().setText( this.getTypeStringOf( spy ) ) ;
		this.getActualTypeField().setText( this.getValueTypeStringOf( spy ) ) ;
		
		value = this.getValueComponentOf( spy ) ;
		if ( this.isVisualComponent( value ) )
		{
			this.getContentArea().activateComponent( (Component)value );
		}
		else
		{
			this.getContentArea().activateText( this.getValueStringOf( spy ), 
											this.isEditableElement( spy ), spy.isPrimitive() ) ;
		}
	} // updateInformation() 

  // -------------------------------------------------------------------------

	protected boolean isEditableElement( Spy spy ) 
	{
		if ( ! this.prefs().isEditingSupported() )
			return false ;
		
		if ( spy == this.getInspectedObject() )
			return false ;
		
		return spy.isEditable() ;
	} // isEditableElement() 

	// -------------------------------------------------------------------------
	
	protected boolean isVisualComponent( Object obj ) 
	{
		return (	( obj instanceof Component )
		 		&&	!	( obj instanceof Window ) ) ;	
	} // isVisualComponent() 

	// -------------------------------------------------------------------------
	
	/**
	 * Set the filter flags specified by the given filter in the inspected 
	 * object
	 */
	protected void setFilter( ElementFilter filter )
	{
		this.getInspectedObject().setElementFilter( filter ) ;
		this.updateDisplay() ;	
	} // setFilter() 

	// -------------------------------------------------------------------------

	protected void updateDisplay()
	{
		this.getElementTree().clearSelection() ;
		this.getElementTree().setModel( new DefaultTreeModel( this.createTreeNodes() ) ) ;
	} // updateDisplay() 

  // -------------------------------------------------------------------------

	/**
	 * Display an error that occured during an attempt to modify an element's
	 * value.
	 */
	protected void displayValueModificationError( Exception ex )
	{
		JOptionPane.showMessageDialog( this.getDialogOwner(),
																	ex.toString(),
																	"Value Modification Error",
																	JOptionPane.ERROR_MESSAGE ) ;
		// ex.printStackTrace() ;
	} // displayValueModificationError() 

	// -------------------------------------------------------------------------

	protected void displayCurrentEditableValue() 
	{
		Spy spy ;
		
		spy = this.getCurrentElement() ;
		this.getActualTypeField().setText( this.getValueTypeStringOf( spy ) ) ;
		this.getContentArea().setEditableText( this.getValueStringOf( spy ) ) ;
	} // displayCurrentEditableValue() 

	// -------------------------------------------------------------------------
	
	protected void resetModifiedValue() 
	{
		this.displayCurrentEditableValue() ;
	} // resetModifiedValue() 

	// -------------------------------------------------------------------------
	
	protected void setCurrentValueToNull() 
	{
		try
		{
			this.getCurrentElement().setValue( null );
			this.displayCurrentEditableValue() ;
		}
		catch ( Exception e )
		{
			this.displayValueModificationError( e ) ;
		}
	} // setCurrentValueToNull() 

	// -------------------------------------------------------------------------
	
	protected void saveModifiedValue() 
	{
		Object newValue ;
		String text ;
		ElementSpy elementSpy ;

		if ( ! this.getCurrentElement().isElementSpy() )
			return ; // Should not happen
		
		elementSpy = (ElementSpy)this.getCurrentElement() ;
		
		text = this.getContentArea().getEditedText() ; 
		newValue = text ;
		try
		{
			if ( elementSpy.is_Integer_or_int() )
			{
				newValue = this.parseToInteger( text ) ;
			}
			else if ( elementSpy.is_Boolean_or_boolean() )
			{
				newValue = this.parseToBoolean( text ) ;
			}
			else if ( elementSpy.is_Character_or_char() )
			{
				newValue = this.parseToCharacter( text ) ;
			}
			else if ( elementSpy.is_Long_or_long() )
			{
				newValue = this.parseToLong( text ) ;
			}
			else if ( elementSpy.is_Short_or_short() )
			{
				newValue = this.parseToShort( text ) ;
			}
			else if ( elementSpy.is_Byte_or_byte() )
			{
				newValue = this.parseToByte( text ) ;
			}
			else if ( elementSpy.is_Float_or_float() )
			{
				newValue = this.parseToFloat( text ) ;
			}
			else if ( elementSpy.is_Double_or_double() )
			{
				newValue = this.parseToDouble( text ) ;
			}
			elementSpy.setValue( newValue );
			this.displayCurrentEditableValue() ;
		}
		catch ( Exception e )
		{
			this.displayValueModificationError( e ) ;
		}
	} // saveModifiedValue() 

	// -------------------------------------------------------------------------
	
	protected Integer parseToInteger( String text )
		throws Exception
	{
		return Integer.parseInt( text.trim() )  ;
	} // parseToInteger() 

	// -------------------------------------------------------------------------

	protected Boolean parseToBoolean( String str ) 
		throws Exception
	{
		String text ;
		
		text = str.trim() ;
		if ( "true".equals( text ) )
			return Boolean.TRUE ;
		
		if ( "false".equals( text ) )
			return Boolean.FALSE ;
	
		throw new Exception( "Invalid value '" + text + "' for boolean type.") ;
	} // parseToBoolean() 

	// -------------------------------------------------------------------------

	protected Character parseToCharacter( String str )
		throws Exception
	{
		String text ;
		
		text = str.trim() ;
		if ( text.length() == 1 )
			return text.charAt(0) ;

		throw new Exception( "Invalid value '" + text + "' for char type.") ;		
	} // parseToCharacter() 

	// -------------------------------------------------------------------------
	
	protected Long parseToLong( String text )
		throws Exception
	{
		return  Long.parseLong( text.trim() ) ;
	} // parseToLong() 
	
	// -------------------------------------------------------------------------
	
	protected Short parseToShort( String text )
		throws Exception
	{
		return Short.parseShort( text.trim() ) ;
	} // parseToShort() 
	
	// -------------------------------------------------------------------------
	
	protected Byte parseToByte( String text )
		throws Exception
	{
		return Byte.parseByte( text.trim() ) ;
	} // parseToByte() 
	
	// -------------------------------------------------------------------------
	
	protected Double parseToDouble( String text )
		throws Exception
	{
		return Double.parseDouble( text.trim()) ;
	} // parseToDouble() 
	
	// -------------------------------------------------------------------------
	
	protected Float parseToFloat( String text )
		throws Exception
	{
		return Float.parseFloat( text.trim() ) ;
	} // parseToFloat() 
	
	// -------------------------------------------------------------------------
	
	protected ObjectSpyTreeNode createTreeNodes()
	{
		ObjectSpyTreeNode rootNode		= null ;
		ElementSpyTreeNode treeNode 	= null ;
		java.util.Iterator iter				= null ;

		rootNode = new ObjectSpyTreeNode( this.getInspectedObject() ) ;
		iter = this.getInspectedObject().getElements().iterator() ;
		while ( iter.hasNext() )
		{
			treeNode = new ElementSpyTreeNode( (ElementSpy)iter.next() ) ;
			rootNode.add( treeNode ) ;
		} // while
		return rootNode ;
	} // createTreeNodes() 

  // -------------------------------------------------------------------------

	protected void sortElements()
	{
		if ( this.getInspectedObject().sortElements() )
		{
			this.updateDisplay() ;
		}
	} // sortElements() 

  // -------------------------------------------------------------------------

	protected ElementFilter elementFilter()
	{
		return this.getInspectedObject().getElementFilter() ;
	} // elementFilter() 

	// -------------------------------------------------------------------------

	protected Container getDialogOwner() 
	{
		return this.getController().getMainFrame().getContentPane() ;
	} // getDialogOwner() 

	// -------------------------------------------------------------------------
	
	protected String getInspectorId()
	{
		return "BasicInspector" ;
	} // getInspectorId() 

	// -------------------------------------------------------------------------

	protected Preferences prefs()
	{
		return Preferences.instance() ;
	} // prefs() 

	// -------------------------------------------------------------------------

	protected StringUtil str()
	{
		return StringUtil.current() ;
	} // str() 

	// -------------------------------------------------------------------------
	
} // class BasicInspector 
