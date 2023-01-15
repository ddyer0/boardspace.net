package goban.shape.shape.gui;
import goban.shape.beans.*;

import java.awt.*;

// ------------------
// Define main class.
// ------------------
public class ShapePanel extends Panel
{
	
    /**
	 * 
	 */
	static final long serialVersionUID = 1L;
	// --------------------------------
    // Define private member variables.
    // --------------------------------
    private Label lavaVar0 = null;
    private Label lavaVar1 = null;
    public Button findbutton = null;
    public GridBoard positionboard = null;
    public GridBoard shapeboard = null;
    public GridBoard bigboard = null;
    public Choice majormode = null;
    public Panel resultpanel = null;
    private Label lavaVar70 = null;
    private Label lavaVar71 = null;
    public TextField BlackResult = null;
    public TextField WhiteResult = null;
    public TextField BlackAuxText = null;
    public TextField WhiteAuxText = null;
    public TextArea message = null;
    public Checkbox ShowResults = null;
    public Checkbox ShowMoves = null;
    public Checkbox ChangeLibrary = null;
    public Checkbox ShowInfo = null;
    public TextField BlackMove = null;
    public TextField WhiteMove = null;
    public Panel controlpanel = null;
    public Checkbox ToggleBlack = null;
    public Checkbox ToggleWhite = null;
    public Button BackButton = null;
    public Button ForeButton = null;
    public Button make_white_move_button = null;
    public Button make_black_move_button = null;
    private CheckboxGroup checkGroup1 = new CheckboxGroup();


	
    // -----------------------------
    // Define constructor for class.
    // -----------------------------
    public ShapePanel()
    {
		
        // Set up container component.
        setBackground( new Color( 192, 192, 192 ) );
        setForeground( new Color( 0, 0, 0 ) );
		
        // Make the GUI.
        makeGUI();
		
    }
	
    // ---------------------------
    // Define constructor for GUI.
    // ---------------------------
    private void makeGUI()
    {
		
        // Create the GUI system.
        LavaLayout mainLayout = new LavaLayout( 25, 15 );
        setLayout( mainLayout );
        lavaVar0 = new Label( "Select Position", Label.CENTER );
        mainLayout.setConstraints( lavaVar0, makeLavaCons( 0, 13, 4, 2 ) );
        add( lavaVar0 );
        lavaVar0.setBackground( new Color( 192, 192, 192 ) );
        lavaVar0.setForeground( new Color( 0, 0, 0 ) );
        lavaVar0.setEnabled( true );
        lavaVar0.setVisible( true );
        lavaVar1 = new Label( "Starting Shape", Label.CENTER );
        mainLayout.setConstraints( lavaVar1, makeLavaCons( 5, 13, 5, 2 ) );
        add( lavaVar1 );
        lavaVar1.setBackground( new Color( 192, 192, 192 ) );
        lavaVar1.setForeground( new Color( 0, 0, 0 ) );
        lavaVar1.setEnabled( true );
        lavaVar1.setVisible( true );
        findbutton = new Button( "Find" );
        mainLayout.setConstraints( findbutton, makeLavaCons( 10, 9, 4, 2 ) );
        add( findbutton );
        findbutton.setBackground( new Color( 192, 192, 192 ) );
        findbutton.setForeground( new Color( 0, 0, 0 ) );
        findbutton.setEnabled( true );
        findbutton.setVisible( true );
        positionboard = new GridBoard();
        mainLayout.setConstraints( positionboard, makeLavaCons( 0, 9, 4, 4 ) );
        add( positionboard );
        positionboard.setBackground( new Color( 192, 192, 192 ) );
        positionboard.setForeground( new Color( 0, 0, 0 ) );
        positionboard.setEnabled( true );
        positionboard.setVisible( true );
        shapeboard = new GridBoard();
        mainLayout.setConstraints( shapeboard, makeLavaCons( 5, 9, 5, 4 ) );
        add( shapeboard );
        shapeboard.setBackground( new Color( 192, 192, 192 ) );
        shapeboard.setForeground( new Color( 0, 0, 0 ) );
        shapeboard.setEnabled( true );
        shapeboard.setVisible( true );
        bigboard = new GridBoard();
        mainLayout.setConstraints( bigboard, makeLavaCons( 15, 0, 10, 11 ) );
        add( bigboard );
        bigboard.setBackground( new Color( 192, 192, 192 ) );
        bigboard.setForeground( new Color( 0, 0, 0 ) );
        bigboard.setEnabled( true );
        bigboard.setVisible( true );
        majormode = new Choice();
        mainLayout.setConstraints( majormode, makeLavaCons( 10, 11, 4, 2 ) );
        add( majormode );
        majormode.setBackground( new Color( 192, 192, 192 ) );
        majormode.setForeground( new Color( 0, 0, 0 ) );
        majormode.setEnabled( true );
        majormode.setVisible( true );
        resultpanel = new Panel();
        mainLayout.setConstraints( resultpanel, makeLavaCons( 0, 0, 15, 9 ) );
        add( resultpanel );
        resultpanel.setBackground( new Color( 192, 192, 192 ) );
        resultpanel.setForeground( new Color( 0, 0, 0 ) );
        resultpanel.setEnabled( true );
        resultpanel.setVisible( true );
        LavaLayout resultpanelLayout = new LavaLayout( 30, 15 );
        resultpanel.setLayout( resultpanelLayout );
        lavaVar70 = new Label( "i f BLACK moves", Label.RIGHT );
        resultpanelLayout.setConstraints( lavaVar70, makeLavaCons( 0, 3, 7, 2 ) );
        resultpanel.add( lavaVar70 );
        lavaVar70.setBackground( new Color( 192, 192, 192 ) );
        lavaVar70.setForeground( new Color( 0, 0, 0 ) );
        lavaVar70.setEnabled( true );
        lavaVar70.setVisible( true );
        lavaVar71 = new Label( "if WHITE moves", Label.RIGHT );
        resultpanelLayout.setConstraints( lavaVar71, makeLavaCons( 0, 5, 7, 2 ) );
        resultpanel.add( lavaVar71 );
        lavaVar71.setBackground( new Color( 192, 192, 192 ) );
        lavaVar71.setForeground( new Color( 0, 0, 0 ) );
        lavaVar71.setEnabled( true );
        lavaVar71.setVisible( true );
        BlackResult = new TextField( "", 5 );
        resultpanelLayout.setConstraints( BlackResult, makeLavaCons( 7, 3, 6, 2 ) );
        resultpanel.add( BlackResult );
        BlackResult.setBackground( new Color( 192, 192, 192 ) );
        BlackResult.setForeground( new Color( 0, 0, 0 ) );
        BlackResult.setEnabled( true );
        BlackResult.setVisible( true );
        WhiteResult = new TextField( "", 5 );
        resultpanelLayout.setConstraints( WhiteResult, makeLavaCons( 7, 5, 6, 2 ) );
        resultpanel.add( WhiteResult );
        WhiteResult.setBackground( new Color( 192, 192, 192 ) );
        WhiteResult.setForeground( new Color( 0, 0, 0 ) );
        WhiteResult.setEnabled( true );
        WhiteResult.setVisible( true );
        BlackAuxText = new TextField( "", 5 );
        resultpanelLayout.setConstraints( BlackAuxText, makeLavaCons( 21, 3, 9, 2 ) );
        resultpanel.add( BlackAuxText );
        BlackAuxText.setBackground( new Color( 192, 192, 192 ) );
        BlackAuxText.setForeground( new Color( 0, 0, 0 ) );
        BlackAuxText.setEnabled( true );
        BlackAuxText.setVisible( true );
        WhiteAuxText = new TextField( "", 5 );
        resultpanelLayout.setConstraints( WhiteAuxText, makeLavaCons( 21, 5, 9, 2 ) );
        resultpanel.add( WhiteAuxText );
        WhiteAuxText.setBackground( new Color( 192, 192, 192 ) );
        WhiteAuxText.setForeground( new Color( 0, 0, 0 ) );
        WhiteAuxText.setEnabled( true );
        WhiteAuxText.setVisible( true );
        message = new TextArea( "", 5, 10 );
        resultpanelLayout.setConstraints( message, makeLavaCons( 0, 7, 30, 8 ) );
        resultpanel.add( message );
        message.setBackground( new Color( 192, 192, 192 ) );
        message.setForeground( new Color( 0, 0, 0 ) );
        message.setEnabled( true );
        message.setVisible( true );
        ShowResults = new Checkbox( "Expected Result", true );
        resultpanelLayout.setConstraints( ShowResults, makeLavaCons( 7, 0, 8, 3 ) );
        resultpanel.add( ShowResults );
        ShowResults.setBackground( new Color( 192, 192, 192 ) );
        ShowResults.setForeground( new Color( 0, 0, 0 ) );
        ShowResults.setEnabled( true );
        ShowResults.setVisible( true );
        ShowMoves = new Checkbox( "move at", true );
        resultpanelLayout.setConstraints( ShowMoves, makeLavaCons( 15, 0, 7, 3 ) );
        resultpanel.add( ShowMoves );
        ShowMoves.setBackground( new Color( 192, 192, 192 ) );
        ShowMoves.setForeground( new Color( 0, 0, 0 ) );
        ShowMoves.setEnabled( true );
        ShowMoves.setVisible( true );
        ChangeLibrary = new Checkbox( "Full Library", false );
        resultpanelLayout.setConstraints( ChangeLibrary, makeLavaCons( 0, 0, 7, 3 ) );
        resultpanel.add( ChangeLibrary );
        ChangeLibrary.setBackground( new Color( 192, 192, 192 ) );
        ChangeLibrary.setForeground( new Color( 0, 0, 0 ) );
        ChangeLibrary.setEnabled( true );
        ChangeLibrary.setVisible( true );
        ShowInfo = new Checkbox( "info", true );
        resultpanelLayout.setConstraints( ShowInfo, makeLavaCons( 22, 0, 8, 3 ) );
        resultpanel.add( ShowInfo );
        ShowInfo.setBackground( new Color( 192, 192, 192 ) );
        ShowInfo.setForeground( new Color( 0, 0, 0 ) );
        ShowInfo.setEnabled( true );
        ShowInfo.setVisible( true );
        BlackMove = new TextField( "", 5 );
        resultpanelLayout.setConstraints( BlackMove, makeLavaCons( 13, 3, 8, 2 ) );
        resultpanel.add( BlackMove );
        BlackMove.setBackground( new Color( 192, 192, 192 ) );
        BlackMove.setForeground( new Color( 0, 0, 0 ) );
        BlackMove.setEnabled( true );
        BlackMove.setVisible( true );
        WhiteMove = new TextField( "", 5 );
        resultpanelLayout.setConstraints( WhiteMove, makeLavaCons( 13, 5, 8, 2 ) );
        resultpanel.add( WhiteMove );
        WhiteMove.setBackground( new Color( 192, 192, 192 ) );
        WhiteMove.setForeground( new Color( 0, 0, 0 ) );
        WhiteMove.setEnabled( true );
        WhiteMove.setVisible( true );
        controlpanel = new Panel();
        mainLayout.setConstraints( controlpanel, makeLavaCons( 14, 11, 11, 4 ) );
        add( controlpanel );
        controlpanel.setBackground( new Color( 192, 192, 192 ) );
        controlpanel.setForeground( new Color( 0, 0, 0 ) );
        controlpanel.setEnabled( true );
        controlpanel.setVisible( true );
        LavaLayout controlpanelLayout = new LavaLayout( 15, 10 );
        controlpanel.setLayout( controlpanelLayout );
        ToggleBlack = new Checkbox( "toggle Black stones", true );
        ToggleBlack.setCheckboxGroup( checkGroup1 );
        controlpanelLayout.setConstraints( ToggleBlack, makeLavaCons( 0, 0, 8, 3 ) );
        controlpanel.add( ToggleBlack );
        ToggleBlack.setBackground( new Color( 192, 192, 192 ) );
        ToggleBlack.setForeground( new Color( 0, 0, 0 ) );
        ToggleBlack.setEnabled( true );
        ToggleBlack.setVisible( true );
        ToggleWhite = new Checkbox( "toggle White stones", false );
        ToggleWhite.setCheckboxGroup( checkGroup1 );
        controlpanelLayout.setConstraints( ToggleWhite, makeLavaCons( 0, 3, 8, 3 ) );
        controlpanel.add( ToggleWhite );
        ToggleWhite.setBackground( new Color( 192, 192, 192 ) );
        ToggleWhite.setForeground( new Color( 0, 0, 0 ) );
        ToggleWhite.setEnabled( true );
        ToggleWhite.setVisible( true );
        BackButton = new Button( "<" );
        controlpanelLayout.setConstraints( BackButton, makeLavaCons( 9, 2, 3, 3 ) );
        controlpanel.add( BackButton );
        BackButton.setBackground( new Color( 192, 192, 192 ) );
        BackButton.setForeground( new Color( 0, 0, 0 ) );
        BackButton.setEnabled( true );
        BackButton.setVisible( true );
        ForeButton = new Button( ">" );
        controlpanelLayout.setConstraints( ForeButton, makeLavaCons( 12, 2, 3, 3 ) );
        controlpanel.add( ForeButton );
        ForeButton.setBackground( new Color( 192, 192, 192 ) );
        ForeButton.setForeground( new Color( 0, 0, 0 ) );
        ForeButton.setEnabled( true );
        ForeButton.setVisible( true );
        make_white_move_button = new Button( "Make White Move" );
        controlpanelLayout.setConstraints( make_white_move_button, makeLavaCons( 1, 6, 6, 3 ) );
        controlpanel.add( make_white_move_button );
        make_white_move_button.setBackground( new Color( 192, 192, 192 ) );
        make_white_move_button.setForeground( new Color( 0, 0, 0 ) );
        make_white_move_button.setEnabled( true );
        make_white_move_button.setVisible( true );
        make_black_move_button = new Button( "Make Black Move" );
        controlpanelLayout.setConstraints( make_black_move_button, makeLavaCons( 8, 6, 6, 3 ) );
        controlpanel.add( make_black_move_button );
        make_black_move_button.setBackground( new Color( 192, 192, 192 ) );
        make_black_move_button.setForeground( new Color( 0, 0, 0 ) );
        make_black_move_button.setEnabled( true );
        make_black_move_button.setVisible( true );
		
    }
	
    // --------------------------------------------
    // Define a 'make grid bag constraints' method.
    // --------------------------------------------
    public GridBagConstraints makeGridCons( int x, int y, int dx, int dy, int wx, int wy, int fill, int anch, int it, int il, int ib, int ir, int px, int py )
    {
		
        // Define local variables.
        GridBagConstraints cons = new GridBagConstraints();
		
        // Set the constraints.
        cons.weightx    = wx;
        cons.weighty    = wy;
        cons.gridx      = x;
        cons.gridy      = y;
        cons.gridwidth  = dx;
        cons.gridheight = dy;
        cons.fill       = fill;
        cons.anchor     = anch;
        cons.insets     = new Insets( it, il, ib, ir );
        cons.ipadx      = px;
        cons.ipady      = py;
		
        // Return the constraints to the caller.
        return cons;
		
    }
	
    // ----------------------------------------------
    // Define a 'make LavaLayout constraints' method.
    // ----------------------------------------------
    public LavaLayoutConstraints makeLavaCons( int x, int y, int dx, int dy )
    {
		
        // Define local variables.
        LavaLayoutConstraints cons = new LavaLayoutConstraints();
		
        // Set the constraints.
        cons.gridX      = x;
        cons.gridY      = y;
        cons.gridWidth  = dx;
        cons.gridHeight = dy;
		
        // Return the constraints to the caller.
        return cons;
		
    }
	
}
