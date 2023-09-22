/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.
    
    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/. 
 */
package online.search.nntest;
import java.awt.*;
import java.awt.event.*;



public class Welcome2 extends Panel 
		implements ActionListener, MouseListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Panel b_panel;
	private Button[] b;
	private Frame[] windows;
	private String[] descriptions;
	private TextArea desc;

	private BackPropagation net = null;
	private double[] net_parameters;
	
	@SuppressWarnings("unused")
	private final int NODE_COUNTS[] = { 7, 3, 10 };

	private ConfigFrame w;
	private ErrorFrame e;
	private NetFrame n;
	private ClickFrame c;

	private final String Default_text = 
		"\n\n\n\n\n              " + 
		"NEURAL NETWORK DEMONSTRATION APPLET";

	private final String LED_text = 
		"\nTesting and Training window\n" +
		"\nThe purpose of this window is to have all training " +
		"and testing functionality available in one place. " +
		"Here you can enter the data with which to train the net.\n" +
		"\nDefault is network that learns numbers from 0 to 9\n" +
		"\nPlease see the 'Help' menu within " + 
		"the window for instructions on use";

	private final String Config_text =
		"\nConfiguration window\n" +
		"\nWhere the user can configure the network " + 
		"to their heart's content\n" + 
		"\nDefault configuration is for a network " + 
		"that learns numbers from 0 to 9\n" +
		"\nPlease see the 'Help' menu within " +
		"the window for instructions on use";

	private final String Error_text = 
		"\nError Plotting window\n" +
		"\nPlots the error from the network. This error is a " +
		"combination of the individual errors " +
		"from each output node.\n" +
		"\nPlease see the 'Help' menu within " +
		"the window for instructions on use";

	private final String Pic_text = 
		"\nNetwork Visualisation window\n" +
		"\nDraws a pictorial representation of the network " +
		"that is updated as the network trains\n" +
		"\nDefault if for a network of 7 input nodes, 3 hidden " +
		"nodes, and 10 output nodes\n" +
		"\nPlease see the 'Help' menu within the " +
		"window for instructions on use";

	private long delay = 0;
	private int current_sample = 0;
	
	public void init() {
		b = new Button[4];
		b[0] = new Button("LED");
		b[1] = new Button("Configuration");
		b[2] = new Button("Error Plot");
		b[3] = new Button("Visualisation");

		desc = new TextArea(Default_text, 11, 50,
			TextArea.SCROLLBARS_NONE);
		add(desc, BorderLayout.CENTER);
		desc.setEditable(false);

		b_panel = new Panel();
		b_panel.setLayout( new GridLayout(1, b.length) );

		for (int i=0; i < b.length; i++) {
			b_panel.add( b[i] );
			b[i].addActionListener(this);
			b[i].addMouseListener(this);
		} // for

		add( b_panel, BorderLayout.SOUTH );

		w = new ConfigFrame(this);
		w.setSize(500, 300);
		
		e = new ErrorFrame("Error Plot");

		n = new NetFrame();
		
		c = new ClickFrame(this);

		windows = new Frame[b.length];
		windows[0] = c;
		windows[1] = w;
		windows[2] = e;
		windows[3] = n;

		descriptions = new String[b.length];
		descriptions[0] = LED_text;
		descriptions[1] = Config_text;
		descriptions[2] = Error_text;
		descriptions[3] = Pic_text;

		// get the default parameters
		w.update_parameters();

		// in case the defaults change.
		int[] node_counts = new int[3];
		node_counts[0] = c.DEFAULT_INPUTS;
		node_counts[1] = (int) net_parameters[0];
		node_counts[2] = (int) net_parameters[1];
		
		setBackground(Color.white);

		// n.change_net(node_counts);

		// delay = 1000;
	} // init()

	// triggered by pressing one of the buttons.
	// finds out which button was pressed and shows/hides
	// the appropriate window.
	@SuppressWarnings("deprecation")
	public void actionPerformed( ActionEvent _e ) {
		int i=0;

		// System.out.println("Clicked!");

		for ( ; i < b.length; i++) {
			if (_e.getSource() == b[i]) { break; }
		} // for

		if (i == b.length) { 
			System.out.println("NO source!"); 
			System.exit(1); 
		} // if

		if ( !(windows[i].isShowing()) ) { 
			windows[i].show();
			windows[i].toFront();
		} // if
		else { 
			if ( windows[i].getState() == Frame.ICONIFIED ) {
				windows[i].setState(Frame.NORMAL);
			} // if
			else { windows[i].hide(); }
		} // else
	} // actionListener()

	// Mouse Listener routines follow ..
	public void mouseClicked(MouseEvent e) { }
	public void mousePressed(MouseEvent e) { }
	public void mouseReleased(MouseEvent e) { }

	public void mouseEntered(MouseEvent e) { 
		int i=0;
		
		for ( ; i < b.length; i++) {
			if (e.getSource() == b[i]) { break; }
		} // for

		desc.setText(descriptions[i]);
	} // mouseEntered()

	public void mouseExited(MouseEvent e) { 
		desc.setText(Default_text);
	} // mouseExited()

	// end of Mouse Listener routines ..

	// spreads the parameters around all the windows as needed
	public void create_net(double[] parameters) {
		System.out.print("\nCreate_net() ");

		if (net != null) {
			// telling the net to stop executing
			net.kill();

			// now set the net adrift, so that the 
			// garbage collector removes it
			net = null; 

			// execute the collector ..
			System.gc();
		} // if

		for (int i=0; i < parameters.length; i++) {
			System.out.print(parameters[i] + " ");
		} // for

		net_parameters = parameters;

		c.set_sample_count( (int) net_parameters[3], 
				(int) net_parameters[1]);

		// blank out the picture
		n.set_layers(null);
	} // create_net()

	// creates the net itself and starts training
	public void start_training(double[][] inputs, double[][] outputs) {
		int[] neurons = new int[3];

		neurons[0] = c.DEFAULT_INPUTS;
		neurons[1] = (int) net_parameters[0];
		neurons[2] = (int) net_parameters[1];

		// if the thread is still alive, 
		// then interrupt it and kill it!
		if ( net != null ) { 
			net.kill(); 
			net = null;
			System.gc();	

			try { Thread.sleep(delay); }
			catch(InterruptedException _e) { }
		} // if

		System.out.println("\nKilled old net");

		net = new BackPropagation(neurons, inputs, outputs, 
				net_parameters[4], net_parameters[5],
				net_parameters[6], (long) net_parameters[2]);

		// make new picture
		// n = new NetFrame(neurons);
		// n.set_layers( net.get_layers() );

		System.out.print("\nStrart_Training() ");
		for (int i=0; i < net_parameters.length; i++) {
			System.out.print(net_parameters[i] + " ");
		} // for
		
		e.set_epoch_limit( (int) 
			(net_parameters[2]) ); // *net_parameters[3]));
			// (int) net_parameters[2] );
		
		current_sample = 0;

		net.set_parent(this);
		net.set_delay(delay);
		net.start();

		System.out.print(" Training_done");
	} // start_training()

	// to be called by the net after each training sample
	public void draw() {
		// if (!e.isShowing()) { return; }
	
		// long time = System.currentTimeMillis();

		// don't remove, needed to draw properly
		// n.set_layers(net.get_layers());
		// n.redraw();

		// while ( (time + 10000) > System.currentTimeMillis() ) { }

		/*
		try { System.in.read(); }
		catch (IOException _e) { }
		*/

		if ((n.isShowing()) && (n.getState() == Frame.NORMAL)) {
			if (delay != n.get_delay()) {
				delay = n.get_delay();
				net.set_delay(delay);
				System.out.println(delay);
			} // if
			n.set_layers(net.get_layers());
		} // if
		else { 
			if (delay != 0) {
				delay = 0;

				// sleep for a second so that the net has time 
				// to finish it's current sample ..
				try { Thread.sleep(1000); }
				catch(InterruptedException _e) { }

				net.set_delay(delay);
				System.out.println("No delay");
				System.out.println(delay);
			} // if
		} // else

		// error plot update
		if ((current_sample % net_parameters[3]) == 0) {
			e.new_point( net.get_error() );

			System.out.println("Plot : " + 
				(current_sample / net_parameters[3]));
		} // if

		// System.out.println("" + System.currentTimeMillis());

		/*
		try { net.wait(1); }
		catch (InterruptedException _e) { }
		*/
		
		System.out.println("Sample : " + current_sample);

		current_sample++;
	} // draw()

	// returns the index of the highest firing output neuron.
	public int test(double[] input) {
		// if the user presses 'test' before training ..
		if (net == null) { return -1; }

		// update the picture too .. 
		n.set_layers(net.get_layers());

		return net.test(input);
	} // test()

	public void stop_training() { 
		if (net != null) { net.kill(); }
	} // stop_training()

	public void net_done() {
		c.finished_training();		
	} // net_done()
} // class Welcome2


class CloseWindow extends WindowAdapter {
	@SuppressWarnings("deprecation")
	public void windowClosing(WindowEvent e) {
		e.getWindow().hide();
	} // windowClosing()
} // class CloseWindow






























// ConfigFrame starts here

// To do :
//	- implement the creation of a new net.
class ConfigFrame extends MyFrame implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	final int INPUT_COUNT = 7; // total input count
	final int INT_INPUTS = 4; // number of inputs that are integers

	// defaults
	final int INT_DEFAULTS[] = 
		{ 3, 10, 40000, 10 };
	final double DOUBLE_DEFAULTS[] = 
		{ 0.01, 0.01, 0.15 };

	Label[] labels;
	TextField[] fields;
	double[] inputs;
	Button ok;
	Welcome2 parent;
	ErrorDialog error;
	
	final String help =
	"\nAn explanation of each field follows :\n" +
	"\n- Number of Hidden Neurons : means exactly what it says " +
	".. sets the number of hidden neurons in the net. Beware of " +
	"making this too high, since too many hidden neurons will " +
	"result in overfitting. Just the right number will result " +
	"in better performances.\n" +
	"\n- Number of output neurons: \n" +
	"Sets the number of ouput nodes. This is synnonimous with " +
	"the number of categories that the network can classify an " +
	"input into\n." +
	"\n-Epochs : the number of times the training set is shown to the " +	
	"neural net.\n" +
	"\n- Numbert of training example: : how many examples should be " +
	"in the set.\n" +
	"\n- Learning rate : used to set prevent an overshoot of the " +
	"minimum  error points.\n" +
	"\n- Momentum : the term used to keep the network going in " +
	"the same direction as previous (at least for a little while).\n" +
	"\n- Maximum Error : The maximum error acceptable. If the network " +	
	"achieves at least this error level (or lower), then training " +
	"will stop\n" +
	"\n- Build New Net Button : sets the parameters to the new " +
	"values, and then clears the training data held previously.";

	final String desc = 
	"\nThe number of input units was not made user-configurable " +
	"because this would interfere with the default demonstration. " +
	"Since there are seven clickable segments in the LED, there " +
	"are seven inputs nodes in the network (one for each), and " +
	"the input nodes recieve 1.0 if the corresponding segment " +
	"is on and 0.0 if the segment is off.\n" +
	"\nIt wouldn't make sense for the number of input nodes to be " +
	"variable in this demonstration, since that would then " +
	"invalidate the LED as an " +
	"input mechanism due to a different encoding system being " +
	"required.\n" +
	"\nA good recommendation is that both 'Sample Count' and " +
	"'Number of output neurons' be equal, since more than one " +
	"sample per category isn't necessary (due to error free " +
	"inputs) as long as each sample is correctly classified. " +
	"The number of " +
	"output nodes should be likewise restricted to be equal " +
	"to 'Sample Count' since each sample is from a distinct " +
	"category, and it's best to use one unit per category .. hence " +
	"both sample count and the number of output nodes should " +
	"be equal.\n" +
	"\nEven though it's possible to have multiple hidden layers, " +
	"and the network class itself allows for this, in this " +
	"demosntration applet the decision was made to limit the " +
	"number of layers to three (one hidden) since a three layer " +
	"network can learn any kind of decision function.";

	public ConfigFrame( Welcome2 a ) {
		super("Configuration");
		setSize(300, 200);
		
		addWindowListener( new CloseWindow() );
	
		fields = new TextField[INPUT_COUNT];
		labels = new Label[INPUT_COUNT];
		inputs = new double[INPUT_COUNT];
		ok = new Button("Build New Net");

		error = new ErrorDialog(this, 
			"Illegal entry : Restoring default for that field");

		parent = a;
	
		// integer valued inputs
		// labels[0] = new Label("Number of input nodes", 
		// 				Label.CENTER);
		labels[0] = new Label("Number of hidden nodes",
						Label.CENTER);
		labels[1] = new Label("Number of output nodes",
						Label.CENTER);		
		labels[2] = new Label("Epochs", Label.CENTER);
		labels[3] = new Label("Number of Training Samples",
						Label.CENTER);

		// double valued inputs
		labels[4] = new Label("Learning rate", Label.CENTER);
		labels[5] = new Label("Momentum", Label.CENTER);
		labels[6] = new Label("Maximum Error", Label.CENTER);		

		for (int i=0; i < INT_INPUTS; i++) {
			fields[i] = new TextField("" + INT_DEFAULTS[i], 5);
			inputs[i] = 0.0;
		} // for

		for (int i=INT_INPUTS; i < INPUT_COUNT; i++) {
			fields[i] = new TextField("" + 
				DOUBLE_DEFAULTS[i - INT_INPUTS], 5);
			inputs[i] = 0.0;
		} // for
		
		// add message about format
		addComponent( new Label(
			"Following parameters must in integer format",
			Label.CENTER), 0, 0, 4, 1);			

		for (int i=0; i < 2*INT_INPUTS; i += 2) {
			addComponent(labels[i/2], (i%4), (i/4) + 1, 1, 1);
			addComponent(fields[i/2], (i+1)%4, (i/4) + 1, 1, 1);
		} // for

		addComponent( new Label(
			"Following parameters must in double format",
			Label.CENTER), 0, 5, 4, 1);
		
		for (int i=2*INT_INPUTS; i < 2*INPUT_COUNT; i += 2) {
			addComponent(labels[i/2], 
					(i%4), (i/4) + 4, 1, 1);
			addComponent(fields[i/2], 
					(i+1)%4, (i/4) + 4, 1, 1);
		} // for
		
		ok.addActionListener(this);
		addComponent(ok, 2, 7, 2, 1);
		
		// error.setResizable(false);
		error.setSize(350, 125);
		// error.addWindowListener( new CloseWindow() );
		
		//setResizable(false);

		setHelpText(help);
		setDescText(desc);

		// pass the defaults to the applet
		// update_parameters();
	} // ConfigFrame()
	
	@SuppressWarnings("deprecation")
	public void actionPerformed( ActionEvent e ) {
		process(e);

		// System.out.print("\n");

		if (e.getSource() != ok) { return; }
	
		try {
			update_parameters();
		} // try
		catch(NumberFormatException _e) {
			error.show();
			
			// restore the defaults to the inputs that 
			// caused the exception
			for (int i=0; i < INT_INPUTS; i++) {
				if (inputs[i] == -1.0) {
					fields[i].setText("" + INT_DEFAULTS[i]);
				} // if
			} // for

			for (int i=INT_INPUTS; i < INPUT_COUNT; i++) {
				if (inputs[i] == -1.0) {
					fields[i].setText("" + 
						DOUBLE_DEFAULTS[i - INT_INPUTS]);
				} // if
			} // for

			return;
		} // NumberFormatException()
		
		String old_message = error.getText();
		error.setText("Net Created!, " +
			"now you should enter the training data");
		error.show();
		error.setText(old_message);

		// this.setVisible(false);
		
		// parent.train();
	} // actionPerformed()
	
	public void update_parameters() {
		System.out.print("\nUpdate_Parameters() ");
		for (int i=0; i < INT_INPUTS; i++) {
			inputs[i] = -1.0;
			inputs[i] = 
				(double) Integer.parseInt( (fields[i]).getText() );
		
			System.out.print(inputs[i] + " ");
		} // for

		for (int i=INT_INPUTS; i < INPUT_COUNT; i++) {
			inputs[i] = -1.0;
			inputs[i] = 
				Double.parseDouble( (fields[i]).getText() );
		
			System.out.print(inputs[i] + " ");
		} // for

		parent.create_net(inputs);
	} // update_parameters()

} // class ConfigFrame


class ErrorDialog extends Dialog implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Button ok;
	Label mess;

	public ErrorDialog( Frame parent, String message ) {
		super(parent, "Note", true);
		// setSize(200, 100);
		setLayout( new BorderLayout() );
		
		ok = new Button("OK");
		ok.addActionListener(this);
		// ok.setBackground(Color.white);
		
		mess = new Label(message, Label.CENTER);
		
		add(mess, BorderLayout.CENTER);
		add(ok, BorderLayout.SOUTH);

		setSize(250, 125);
		addWindowListener( new CloseWindow() );

		setBackground(Color.white);
	} // ErrorDialog()
	
	@SuppressWarnings("deprecation")
	public void actionPerformed( ActionEvent e ) {
		hide();
	} // actionPerformed()

	public void setText(String new_text) {
		mess.setText(new_text);
	} // setText()

	public String getText() {
		return mess.getText();
	} // getText()
} // class ErrorDialog


class MyFrame extends Frame 
		implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	GridBagLayout gb;
	GridBagConstraints gc;
	MenuBar mb;
	Menu m1;
	MenuItem instructions, descriptions;
	TextWindow help, desc;

	public MyFrame(String name) {
		super(name);
		setSize(300, 200);
		setBackground(Color.white);

		gb = new GridBagLayout();
		gc = new GridBagConstraints();

		gc.fill = GridBagConstraints.NONE;
		gc.anchor = GridBagConstraints.CENTER;

		setLayout(gb);
		
		instructions = new MenuItem("Help");
		descriptions = new MenuItem("Details");

		instructions.addActionListener(this);
		descriptions.addActionListener(this);

		m1 = new Menu("Menu");
		m1.add(instructions);
		m1.add(descriptions);

		mb = new MenuBar();
		mb.add(m1);
		setMenuBar(mb);

		help = new TextWindow("Usage instructions");
		desc = new TextWindow("Further explanations");

		help.addWindowListener( new CloseWindow() );
		desc.addWindowListener( new CloseWindow() );

		addWindowListener( new CloseWindow() );
	} // MyFrame()

	// should set gc.fill before calling this
	public void addComponent(Component c, int column, 
			int row, int width, int height) {
		gc.gridx = column;
		gc.gridy = row;

		gc.gridwidth = width;
		gc.gridheight = height;

		gb.setConstraints(c, gc);
		add(c);

		// restore the defaults
		gc.fill = GridBagConstraints.NONE;
		gc.anchor = GridBagConstraints.CENTER;
	} // addComponent()

	// to be overridden
	public void actionPerformed( ActionEvent _e ) {
		process(_e);
	} // actionPerformed()

	// should be called by every child window in actionPerformed
	// so that the TextWindows are processed
	@SuppressWarnings("deprecation")
	public void process( ActionEvent _e ) {
		TextWindow window = null;

		if (_e.getSource() == instructions) {
			window = help;
		} // if

		if (_e.getSource() == descriptions) {
			window = desc;
		} // if

		if (window == null) { return; }

		if ( !(window.isShowing()) ) { 
			window.show();
			window.toFront();
		} // if
		else { 
			if ( window.getState() == Frame.ICONIFIED ) {
				window.setState(Frame.NORMAL);
			} // if
			else { window.hide(); }
		} // else
	} // process()

	public void setHelpText(String new_help) {
		help.setText(new_help);
	} // setHelpText()

	public void setDescText(String new_desc) {
		desc.setText(new_desc);
	} // setDescText()
} // class MyFrame


		

























// ErrorFrame starts here

// To do :
//	- implement the error plot function
class ErrorFrame extends MyFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ErrorPlot c;
	Label error_label, epoch_label;
	TextField error, epoch;
	ScrollPane scroller;
	int epoch_count = 0, epoch_limit = 1, epoch_gap = 0;
	
	// NetPic n;
	// int nodes[] = { 7, 3, 10 };

	final String help =
	"\nThere is nothing for the user to manipulate here, not " +
	"even the text boxes ..\n\nThe user should watch and see " +
	"the trends in the error.\n" +
	"\nNote that the window is not resizable since this would " +
	"interfere with the plotting of the error.";

	final String desc = 
	"\nThe error plotted is the batch error, ie. the combined error " +
	"of each iteration of all the samples. This approach was taken " +
	"with the view that this shows a better trend, since most " +
	"of the results in an epoch will tend in the same direction " +
	"of descent, with the odd stray result that changes the trend. " +
	"In the batch error that we use, such stray results have a " +
	"reduced influence due to the greater combined influences " +
	"of the remaining results.\n" +
	"\nAn assumption was made that the trend was more important " +
	"than the actual numerical values, so no actual values are " +
	"shown in the plot (though the error value of the last point " +
	"is shown for reference).\n" +
	"\nTo better show the trends, a decision was made to scale " +
	"the plot to have a maximum of (1.2 * the greatest " +
	"error encountered so far), meaning that the peak value of " +
	"the plot (usually the first point) will be near the top of " +
	"the plot, with the trends then being most visible to the " +
	"user. Normally the greatest error is encountered first, and all " +
	"the subsequent errors will decrease.";

	public ErrorFrame(String name) {
		super(name);
		setSize(504, 300);
		
		
		c = new ErrorPlot();
		// c.setSize(50000, 200);
		
		
		// n = new NetPic(nodes);

		error_label = new Label("Error level", Label.CENTER);
		epoch_label = new Label("Epoch number", Label.CENTER);
		
		// scroller = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);

		// scroller.add(c);
		// scroller.add(c);
		// scroller.setSize(300, 200);
		
		error = new TextField("0.0", 5);
		epoch = new TextField("0", 5);

		error.setEditable(false);
		epoch.setEditable(false);
		
		gc.fill = GridBagConstraints.BOTH;
		gc.weightx = 1.0;
		gc.weighty = 1.0;
		// addComponent(scroller, 0, 0, 4, 3);
		addComponent(c, 0, 0, 4, 3);
				
		addComponent(epoch_label, 0, 3, 1, 1);
		addComponent(epoch, 1, 3, 1, 1);
		addComponent(error_label, 2, 3, 1, 1);
		addComponent(error, 3, 3, 1, 1);
		
		setResizable(false);

		setHelpText(help);
		setDescText(desc);
	} // ErrorFrame()

	public void new_point( double point ) {
		if (epoch_count > epoch_limit) { 
			System.out.println("\nError : exceeded epoch count!");
		} // if

		System.out.println("Epoch : " + epoch_count);

		if ( (epoch_count % epoch_gap) != 0 ) {
			epoch_count++;
			return;
		} // if

		c.plot(epoch_count/epoch_gap, point);
		
		error.setText("" + point);
		epoch.setText("" + epoch_count);
		
		epoch_count++;

		// scroller.setScrollPosition(epoch_count - 200, 0);
	} // new_point()

	// call before plotting points!
	public void set_epoch_limit(int max_epoch) {
		epoch_limit = max_epoch;
		epoch_gap = epoch_limit/c.getWidth();

		if ( (double) epoch_gap < 
			(double)epoch_limit/(double)c.getWidth() ) {
			epoch_gap++;
		} // if

		epoch_count = 0;
		c.clear();

		System.out.println("\nEpoch_gap : " + epoch_gap);
	} // set_epoch_limit()
} // class ErrorFrame


class ErrorPlot extends Canvas {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int x, y;
	double max_error = 0.0;
	int[] past_errors = null;

	public ErrorPlot() { 
		setSize(500, 200);

		setBackground(Color.cyan);

		// n = new NodePic(0, 0, 100, 200, 20);
		// l = new LayerPic(5, 50, 50);
		
		// setIgnoreRepaint(true) ;
		
		// this.setIgnoreRepaint(true);

		clear();
	} // ErrorPlot()

	public void paint( Graphics g ) {
		/*
		for (int i=0; i < getWidth(); i++) {
			// g.drawOval(i, (int) (Math.exp(-i/10) * getHeight()), 1, 1);

			// sample plot
			// g.drawOval(i, (int) (Math.random() * getHeight()), 1, 1);
		} // for
		*/
		// n.draw(g);
		// l.draw(g);

		// g.fillOval(x, y, 3, 3);

		for (int i=0; i < past_errors.length; i++) {
			g.fillOval(i, past_errors[i], 3, 3);
		} // for
	} // paint()

	// override the standard update()
	public void update( Graphics g ) {
		paint(g);
	} // update()

	public void plot(int epoch, double error) {
		if (error > max_error) { max_error = 1.1*error; }
	
		if (past_errors == null) { 
			clear();
		} // if

		x = epoch;
		// y = getHeight() - ((int) (error/max_error))*getHeight();
		
		y = getHeight() - ((int) ((error/max_error) * getHeight()));

		// getGraphics().fillOval(x, y, 3, 3);

		past_errors[x] = y;
		repaint();
	} // plot()
	
	public void clear() {
		past_errors = new int[getWidth() + 1];
		System.out.println("Array : " + past_errors.length);

		for (int i=1; i < past_errors.length; i++) {
			past_errors[i] = -10;
		} // for

		past_errors[0] = 0;
		max_error = 0.0;

		Graphics g = getGraphics();

		// g will be null if the window isn't showing
		if (g != null) {
			Color old = g.getColor();

			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(old);
		} // if

		// repaint();
	} // clear()

} // class ErrorPlot























// NetFrame starts here

class NetFrame extends MyFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	NetCanvas net;
	// final int nodes[] = { 7, 3, 10 }; // defaults
	ScrollPane scroll;
	
	TextField time = null;
	Label l = null;

	long delay = 5000;

	final String help = 
	"\nDisplays the visualization of the network.\n" +
	"\nThe user may enter a desired delay into the text box. This " +
	"delay will then be used to pause the network after each " +
	"sample is trained, ie. the network will pause for the " +
	"nominated number of " +
	"seconds before training the next sample. The time specified " +
	"may be of double format only .. otherwise no change occurs, " +
	"and the current delay is unchanged in the box. The current " +
	"unchanged delay will also be shown in the box as well.\n" +
	"\nIt's recommended that a high enough delay be set so " +
	"that the network drawn does NOT redraw itself immediately " +
	"upon finishing the previous iteration. This may be occurring " +
	"due to the time taken to finish the drawing (which can " +
	"be considerable, depending on the processor), so the network " +
	"must be slowed down enough to ensure that each sample is " +
	"shown on the screen.\n\nTesting in the LED window will also lead " +
	"to a graphical representation in this window being drawn.\n" +
	"\nUpon opening the window, the network will slow down " +
	"(if training), and will speed up automatically when the " +
	"window is hidden. It takes at least as long as the 'time' " +
	"specified, so if the user sets very high delay times, " +
	"then they should remember that it'll take one time step to " +
	"go to no delay processing, eg. if the user specifies 5 " +
	"seconds/sample, then it'll take 5 seconds after closing " +
	"the window before the network will be training with no delay. " +
	"Unfortunately this is unavoidable due to the fact the " +
	"network is paused for a certain time, and may not be " +
	"unpaused before the nominated time passes. Just be a little patient.";

	final String desc = 
	"\nThe purpose of this window is to enable the user to see " +
	"the relationships amongst the nodes in the network, as well " +
	"as to see the weights as they change with time.\n" +
	"\nThis will allow the user to learn much faster from " +
	"these examples than from just reading books.\n" +
	"\nIt's especially worthwhile to look at the weights between " +
	"the nodes. Dark Blue represents inhibitory connections " +
	"with weights of -1.0 or less, significantly reducing the " +
	"input level of the recieving node. Dark Red represents " +
	"1.0 and greater weights, which are significant " +
	"contributors to the input level of the recieving neuron." +
	"Although in theory the weights may range to any value, for " +
	"the graphical representation it was decided to keep the visible " +
	"range between 1.0 and -1.0, so that the smaller weights are " +
	"still visible. Even so, if a weight reaches 1.0 and the firing " + 
	"node is at full strength (ie. 1.0 activation level), then the " +
	"firing level of the recieving node (if assuming it only recieves " +	
	"inputs from this connection) is at 0.73 already " +
	"( 1/(1+exp(-1)) = 0.73 ). That by itself is enough to judge a " +
	"node to be firing.\n" +
	"\nNode output levels are indicated by the darkness of the " +
	"node, with black being 1.0 and white being 0.0. With this " +
	"system weakly firing nodes are less noticeable, which " +
	"helps to declutter the view.\n" +
	"\nThe purpose of this is to show the user the relationships " + 
	"between the nodes and how they influence each other. The " +
	"inputs and outputs could have been shown, but it's much " +
	"easier to do it through the LED (testing mode), which will " +
	"also show the internal workings of the network for the " +
	"input supplied.";

	public NetFrame() {
		super("Graphical Representation");
		setSize(600, 500);

		net = new NetCanvas();
		net.setSize(550, 450);
		
		scroll = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		scroll.setSize(1, 1);
		scroll.add(net);
		scroll.setSize(500, 450);
		
		
		gc.fill = GridBagConstraints.BOTH;
		gc.weightx = 1.0;
		gc.weighty = 1.0;
		addComponent(scroll, 0, 0, 5, 5);

		l = new Label("Seconds per update");
		addComponent(l, 0, 6, 1, 1);

		time = new TextField("5", 5);
		addComponent(time, 3, 6, 1, 1);

		setHelpText(help);
		setDescText(desc);
		
		// setReziable(false);
		// setSize(net.getWidth(), net.getHeight() + l.getHeight());
	} // NetFrame

	// let l be null if wanting to change the network 
	// architecture, then call again with a non-null
	// LAYER[]
	public void set_layers(LAYER[] l) {
		net.make_net(l);
	} // set_layers()

	/*
	// MUST call set_layers() again after this function
	public void change_net(int[] node_counts) {
		net = new NetPic(node_counts);
	} // change_net()
	*/

	public void redraw() { net.repaint(); }

	public long get_delay() {
		long t;

		try {
			t = (long) (1000 * Double.parseDouble( time.getText() ));
		} // try
		
		// if the text entered isn't a double number,
		// then restore the old time and show it in the box.
		catch(NumberFormatException _e) {
			time.setText("" + (double) (delay/1000) );
			return delay; 
		} // catch()

		if (t != delay) { delay = t; }

		return delay; 
	} // get_delay()
} // class NetFrame

class NetCanvas extends Canvas {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	NetPic pic = null;
	// Pic pic = null;

	public NetCanvas() { 
		// pic = new Pic();
		setBackground(Color.white);
	} // NetCanvas()

	public void paint( Graphics g ) {
		g.translate(0,30);

		if (pic != null) {
			g.drawString("Inputs", (int) (pic.max_width()/2.2), 
				LayerPic.box_h/4);

			pic.draw(g);
	
			g.setColor(Color.black);
			g.drawString("Outputs", (int) (pic.max_width()/2.2),
				pic.max_height() - 10);

			g.drawString("Legend", pic.max_width() + 30, -10);
			g.drawString("Weights", pic.max_width() + 5, 20);
			g.drawString("Outputs", pic.max_width() + 65, 20);

			draw_weight_legend(pic.max_width() + 10, 40, g);
			draw_node_legend(pic.max_width() + 70, 40, g);
			g.setColor(Color.black);
		} // if
	} // paint()

	public void make_net(LAYER[] l) {
		if (l == null) { 
			pic = null; 
			repaint(); 
			return;
		} // if

		if (pic == null) {
			pic = new NetPic(l);
			setSize(pic.max_width() + 120, pic.max_height() + 50);
		} // if
		else { pic.set_layers(l); }

		repaint();
	} // make_net()

	private void draw_weight_legend(int x, int y, Graphics g) {
		g.drawString("+1.0", x + 10, y + 5);

		for (int i=0; i < 256; i += 2) {
			g.setColor( new Color(255-i, 0, 0, 255-i) );
			g.drawLine(x, y, x+5, y);
			y++;
		} // for

		g.setColor(Color.black);
		g.drawString("0.0", x + 10, y + 5);

		for (int i=0; i < 256; i += 2) {
			g.setColor( new Color(0, 0, i, i) );
			g.drawLine(x, y, x+5, y);
			y++;
		} // for
		
		g.drawString("-1.0", x + 10, y + 5);
	} // draw_weight_legend()

	private void draw_node_legend(int x, int y, Graphics g) {
		g.setColor(Color.black);
		g.drawString("1.0", x + 10, y + 5);

		for (int i=0; i < 256; i++) {
			g.setColor( new Color(i, i, i) );
			g.drawLine(x, y, x+5, y);
			y++;
		} // for

		g.setColor(Color.black);
		g.drawString("0.0", x + 10, y + 5);
	} // draw_node_legend()

	/*
	public void remove_net() {
		pic = null

		repaint();
	} // make_new_net()
	*/
} // class NetCanvas


class NetPic {
	LayerPic[] layers;
	LAYER[] node_layers;

	public NetPic(LAYER[] l) {
		node_layers = l;

		int[] node_counts;

		int max_nodes = 0;
		int max_width = 0;
		int layer_height = 0;

		node_counts = new int[node_layers.length];
		for (int i=0; i < node_counts.length; i++) {
			node_counts[i] = node_layers[i].get_nodes().length;
		} // for

		max_nodes = find_max(node_counts);
		max_width = LayerPic.box_w * max_nodes;
		layer_height = LayerPic.box_h;
		layers = new LayerPic[ node_counts.length ];

		// setSize( max_width, (layers.length * LayerPic.box_h) );

		for (int i=0; i < layers.length; i++) {
			int width = 0;
			int x_pos = 0, y_pos = 0;

			width = node_counts[i] * LayerPic.box_w;
			x_pos = (max_width - width)/2;
			y_pos = i * layer_height;

			layers[i] = new LayerPic(node_counts[i], x_pos, y_pos);
		} // for

		// System.out.println("\n" + this.getWidth() + " " + this.getHeight());

	} // NetPic()

	public void draw( Graphics g ) {
		NodePic[] nodes_prev;
		NodePic[] nodes_curr;

		if (node_layers == null) { return; }
		
		for (int i=0; i < layers.length; i++) {
			NODE[] nodes = node_layers[i].get_nodes();
			double[] outputs = new double[nodes.length];

			for (int j=0; j < outputs.length; j++) {
				outputs[j] = nodes[j].get_output();
			} // for

			layers[i].draw(g, outputs);
		} // for		
		
		// should be "i < node_layers.length"
		for (int i=1; i < node_layers.length; i++) {
			nodes_prev = layers[i - 1].get_nodes();
			nodes_curr = layers[i].get_nodes();

			for (int j=0; j < nodes_curr.length; j++) {
				double[] weights = ((node_layers[i]).get_nodes())[j].get_weights();

				for (int k=0; k < nodes_prev.length; k++) {
					g.setColor( get_colour(weights[k]) );
					g.drawLine(	nodes_prev[k].get_output_x(),
							nodes_prev[k].get_output_y(),
							nodes_curr[j].get_input_x(),
							nodes_curr[j].get_input_y() );
				} // for
			} // for
		} // for
		
	} // paint()

	private int find_max(int[] array) {
		int max = 0;

		for (int i=0; i < array.length; i++) {
			if (array[i] > max) { max = array[i]; }
		} // for

		return max;
	} // find_max()

	
	// used for a new net of equal dimensions as
	// the previous one
	public void set_layers( LAYER[] l ) {
		node_layers = l;
	} // set_net()
	
	// dummy so far .. 
	public Color get_colour(double in) {
		// if (Math.abs(in) < 0.001) { return Color.white; }

		if (in < 0.0000) {
			if (in < -1.0) {
				return new Color(0, 0, 255, 255);
			} // if
	
			return new Color(0, 0, 
				(int) (255*Math.abs(in)), 
				(int) (255*Math.abs(in)) );
		} // if

		if (in > 0.0000) {
			if (in > 1.0) { 
				return new Color(255, 0, 0, 255); 
			} // if

			return new Color( (int) (255*in), 0, 0, 
				(int) (255*in) );
		} // if

		// should never reach here ..
		return Color.white;
	} // get_colour()

	public int max_width() {
		int max_count;

		NodePic[] n = layers[0].get_nodes();
		max_count = n.length;

		for (int i=1; i < layers.length; i++) {
			n = layers[i].get_nodes();
			if (n.length > max_count) { max_count = n.length; }
		} // for

		return (max_count * LayerPic.box_w);
	} // max_width()

	public int max_height() {
		return (LayerPic.box_h * layers.length);
	} // max_height()
} // class NetPic


class NodePic {
	int box_x = 0, box_y = 0;
	int node_x = 0, node_y = 0;
	int input_x = 0, input_y = 0;
	int output_x = 0, output_y = 0;
	int node_dim = 0;

	public NodePic(int border_x, int border_y, 
			int border_h, int border_w,
			int node_dimensions) {
		box_x = border_x;
		box_y = border_y;
		
		node_dim = node_dimensions;
		node_x = box_x + ( (border_h - node_dim)/2 );
		node_y = box_y + ( (border_w - node_dim)/2 );

		input_x = node_x + ( node_dim/2 );
		input_y = node_y;

		output_x = input_x;
		output_y = node_y + node_dim;
	} // NodePic()

	public void draw( Graphics g ) {
		@SuppressWarnings("unused")
		int box2_x, box2_y;

		g.fillOval(node_x, node_y, node_dim, node_dim);
		
		// g.drawLine(output_x, output_y, 1000, 1000);
	} // draw()

	public int get_input_x() { return input_x; }

	public int get_input_y() { return input_y; }

	public int get_output_x() { return output_x; }
	
	public int get_output_y() { return output_y; }

} // class NodePic


class LayerPic {
	final static int node_hw = 20;
	public static final int box_w = 40;
	public static final int box_h = 100;

	NodePic[] nodes = null;

	public LayerPic(int node_count, int x, int y) {
		int current_x = 0, current_y = 0;

		current_x = x;
		current_y = y;

		nodes = new NodePic[node_count];
		
		for (int i=0; i < node_count; i++) {
			nodes[i] = new NodePic( current_x, current_y,
							box_w, box_h, node_hw );

			current_x += box_w;
		} // for
	} // LayerPic()

	public int get_width() { return (nodes.length * box_w); }
	public int get_height() { return box_h; }

	// public int get_node_w() { return box_w; }
	// public int get_node_h() { return box_h; }

	public void draw( Graphics g, double[] outputs ) {
		for (int i=0; i < nodes.length; i++) {
			g.setColor( get_colour(outputs[i]) );
			(nodes[i]).draw(g);
		} // for
	} // draw()
	
	public Color get_colour(double output) {
		if (output > 1.0) { return Color.black; }
		if (output < 0.0) { return Color.white; }

		return new Color( (float) (1.0 - output), 
			(float) (1.0 - output), 
			(float) (1.0 - output) );
	} // get_colour()

	public NodePic[] get_nodes() { return nodes; }

} // class LayerPic



















// ClickFrame starts here


class Segment { 
	int x=0, y=0, w=0, h=0;
	boolean clicked = false;

	public Segment(int start_x, int start_y, int width, int height) {
		x = start_x;
		y = start_y;
		w = width;
		h = height;
		
		// addMouseListener(this);
	} // Segment()

	public void within(int point_x, int point_y) {
		boolean vertical = false, horizontal = false, within = false;

		// if the segment is already clicked, then change nothing
		// if (clicked == true) { return; }

		horizontal = (point_x >= x) && (point_x <= x+w);
		vertical = (point_y >= y) && (point_y <= y+h);

		within = (horizontal && vertical);

		if (!within) { return; }

		clicked = (!clicked);

		// System.out.println("\n" + clicked);

		// return clicked;
	} // within()

	public void draw( Graphics g ) {
		if (clicked) {
			g.fillRect(x, y, w, h);
		} // if
		else {
			g.drawRect(x, y, w, h);
		} // else
	} // draw()
	
	public void reset() { clicked = false; }
	
	/*
	public void mouseClicked( MouseEvent e ) {
		System.out.println("\n" + e.getX() + " " + e.getY());
	
		// for (int i=0; i < s.length; i++) {
			within(e.getX(), e.getY());
		// } // for
		
		repaint();
	} // mouseClicked()

	// needed to implement MouseListener
	public void mouseEntered( MouseEvent e ) {}
	public void mouseExited( MouseEvent e ) {}
	public void mousePressed( MouseEvent e ) {}
	public void mouseReleased( MouseEvent e ) {}
	*/

	public boolean isClicked() {
		return clicked;
	} // isClicked()
} // class Segment


class ClickCanvas extends Canvas implements MouseListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	final int breadth = 5;
	final int length = 30;
	final int x = 20;
	final int y = 20;

	Segment[] s = null;

	public ClickCanvas() {
		setBackground(Color.white);

		s = new Segment[7];

		s[0] = new Segment(x + breadth, y, length, breadth);
		s[1] = new Segment(x, y + breadth, breadth, length);
		s[2] = new Segment(x + breadth + length, y + breadth, 
						breadth, length);
		s[3] = new Segment(x + breadth, y + length + breadth, 
						length, breadth);
		s[4] = new Segment(x, y + 2*breadth + length, breadth, length);
		s[5] = new Segment(x + breadth + length, 
				y + 2*breadth + length, breadth, length);
		s[6] = new Segment(x + breadth, y + 2*length + 2*breadth,
						 length, breadth);
		
		/*
		for (int i=0; i < s.length; i++) {
			(s[i]).addMouseListener(this);
		} // for
		*/
		
		addMouseListener(this);
		
		setSize( (2*x + 2*breadth + length), (2*y + 2*length + 3*breadth) );
	} // ClickCanvas()

	public void paint( Graphics g ) {
		for (int i=0; i < s.length; i++) {
			s[i].draw(g);
		} // for
	} // draw()
	
	public void reset_LED() {
		for (int i=0; i < s.length; i++) {
			s[i].reset();
		} // for
	} // reset_LED()
	
	public void mouseClicked( MouseEvent e ) {
		// System.out.println("\n" + e.getX() + " " + e.getY());
	
		for (int i=0; i < s.length; i++) {
			s[i].within(e.getX(), e.getY());
		} // for
		
		repaint();
	} // mouseClicked()

	// needed to implement MouseListener
	public void mouseEntered( MouseEvent e ) {}
	public void mouseExited( MouseEvent e ) {}
	public void mousePressed( MouseEvent e ) {}
	public void mouseReleased( MouseEvent e ) {}
	
	public double[] get_inputs() {
		double[] inputs = new double[s.length];

		for (int i=0; i < s.length; i++) {
			if (s[i].isClicked()) { inputs[i] = 1.0; }
			else { inputs[i] = 0.0; }
		} // for
	
		return inputs;
	} // get_inputs()

} // class ClickCanvas


// To do :
// - add two buttons, reset and clear ..
//	clear : clear up the segments and the text field
//	reset : remove all the training samples so far and start new net.
class ClickFrame extends MyFrame implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// one for each number from 0-9
	final int DEFAULT_OUTPUTS = 10;

	// one example per number
	final int DEFAULT_SAMPLES = 10;

	// one input for each segment of the LED
	final int DEFAULT_INPUTS = 7;

	// final double[DEFAULT_SAMPLES][DEFAULT_INPUTS] default_input

	ScrollPane scroll;
	ClickCanvas c;
	Label l;
	TextField t;
	Button b;
	Button mode_change;
	boolean train_mode = true, training_done = true;
	
	ErrorDialog error;

	int max_samples = DEFAULT_SAMPLES;
	int current_sample = 0;
	int outputs_count = DEFAULT_OUTPUTS;

	double[][] inputs = new double[DEFAULT_SAMPLES][DEFAULT_INPUTS];
	String[] outputs = new String[DEFAULT_SAMPLES];

	Welcome2 parent;

	final String help = 
	"\nThis window is used for entering data into the neural net, " +
	"and for testing it once the training is done\n" +
	"\nAs such it has two modes, training and testing mode " + 
	"(shown on the title bar of the window)\n" +
	"\nInitially the mode is set to training, and the mode may " +
	"not be changed until all the training examples have been " +
	"entered. The default number of samples is ten, but that may " +
	"be changed in the Configuration window.\n" +
	"\nNote : The button initially labelled as 'Add to training data' " +
	"will be refered to as the middle button\n" +
	"\nTraining data is addded " +
	"to the training set by setting a pattern into the clickable " +	
	"segments (the LED) and then " +
	"entering the correct output into the text box below 'Correct " + 
	"Output' (the output box). " +
	"Once that is done, press the middle button " +
	"to finish adding the example. \n\nWhen the " +
	"example has been added to the set, the output box " +
	"will clear itself. If the " +
	"text box is empty when the middle button is pressed, " +
	"nothing will be added to the training data, but the user needs " +
	"to beware since anything typed into the output box is regarded " +
	"as correct input .. so " +
	"it's best to set the LED and then write the correct output " +
	"into the text box, then immediately click on the button to " +
	"update the training data.\n" +
	"\nOnce all the training data has been entered, the middle button " +
	"will automatically change it's label to 'Train'. Now pressing " +
	"the middle button will begin training the network. It should " +
	"be noted that each time the 'Train' button is pressed a " +
	"completely new network will be created  (in accordance to " +
	"the parameters set in the Configuration window) and set " +
	"to training using the data entered earlier. Should it be " +
	"desired to clear the training data, then the 'Build New " +
	"Net' button in the Configuration window should be pressed. " +
	"This will completely clear out the past training data.\n" +
	"\nNote that the output box can only be edited by the user " +
	"when the training data is being added, and at all other " +
	"times it's not user-editable.\n" +
	"\nWhen training has begun in the network, the mode " +
	"will automatically change to 'Testing Mode', whereupon " +
	"the user may test the network during the training to get " +
	"a feel of how well it's is going.\n" +
	"\nTesting is carried out by setting a pattern in the " +
	"LED and then pressing the middle button when it's " +
	"labelled 'Test' (a mode change may be needed). This will " +
	"prompt the network to print it's guess into the output box.\n" +
	"\nWhile the training is still going on, the middle button " +
	"will be labelled as 'Stop training' when in training mode. " +
	"In such occasions, the user may press it at any time to " +
	"stop training, though once stopped, no further training may " +
	"be done. Pressing 'Train' will only cause a new net to " +
	"be created.\n" +
	"\nWhen training stops, then the window will be " +
	"automatically set to test mode. Then the user is able to " +
	"test to their heart's content, and may train a new net " +
	"(with the same data) by pressing 'Train'.\n" +
	"\nThere is a problem with the middle button resizing once " +
	"the window is hidden. This is caused by the java vm " +
	"automatically adjusting the size of components, and can " +
	"be solved by hiding and reshowing the window while the " +
	"button label is longer than the button itself, eg. 'Stop " +
	"Training' doesn't show properly when the button is just " +
	"wide enough to have 'Test' on it, so hiding and reshowing " +
	"the window with 'Stop training' as the button label forces " +
	"the java VM to increase the width of the button. No other " +
	"solution has been found.";

	final String desc = 
	"\nDefault configuration is for a network which learns to " +
	"associate patterns in the LED with numbers typed into the " +
	"output box.\n" +
	"\nThe fact that a LED is used for entering the numbers " +
	"means that the inputs are discrete, and hence error free " +
	"(ie. if a pattern is entered twice, exactly the same " +
	"inputs will be recieved in each occasion).\n" +
	"\nDue to the error-free nature of the inputs, only one " +
	"sample per category needs to be provided. For example, " +
	"only 10 patterns are needed for the default setting but " +
	"each should represent a different number. Repeated " +
	"patterns will speed up training a little, but aren't " +
	"really necessary.";

	public ClickFrame(Welcome2 a) {
		super("Train Mode");
		setSize(250, 200);

		parent = a;

		c = new ClickCanvas();
		/*
		scroll = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
		scroll.setSize(100, 50);
		scroll.add(c);
		*/
		// gc.fill = gc.BOTH;
		// gc.weightx = 1.0;
		// gc.weighty = 1.0;
		
		// addComponent(scroll, 0, 0, 2, 3);
		addComponent(c, 0, 0, 2, 5);
		
		l = new Label("Correct Output", Label.CENTER);
		addComponent(l, 2, 0, 2, 1);
		
		t = new TextField("", 10);
		addComponent(t, 2, 1, 2, 1);
		
		b = new Button("Add to training data");
		addComponent(b, 2, 2, 2, 1);
		b.addActionListener(this);
		b.setSize(b.getWidth(), b.getHeight());
		
		mode_change = new Button("Mode Change");
		addComponent(mode_change, 2, 3, 2, 1);
		mode_change.addActionListener(this);
		
		//setResizable(false);

		error = new ErrorDialog(this, 
			"Please enter All training data");
		// error.setSize(

		setHelpText(help);
		setDescText(desc);
	} // ClickFrame()

	@SuppressWarnings("deprecation")
	public void actionPerformed( ActionEvent e ) {
		process(e);

		if (( e.getSource() == mode_change ) && 
			( train_mode == true )) {

			if (current_sample != max_samples) { 
				String old_text = error.getText();

				error.setText("Enter all Training data first");
				error.show();
				error.setText(old_text);

				return;
			} // if

			set_test_mode();
			return;
		} // if
		
		if (( e.getSource() == mode_change ) && 
				( train_mode == false )) {
			set_train_mode();
			
			return;
		} // if
		
		if (( e.getSource() == b ) && ( train_mode == true )) {
			if (current_sample != max_samples) { 
				outputs[current_sample] = t.getText();

				// ignore blank inputs on the textfield
				if ( outputs[current_sample].equals("") ) 
					{ return; }

				inputs[current_sample] = c.get_inputs();

				System.out.println("\n" + outputs[current_sample]);

				for (int i=0; 
					i < (inputs[current_sample]).length; i++) {
					System.out.print(
						inputs[current_sample][i] + " ");
				} // for

				current_sample++;

				t.setText("");

				if (current_sample == max_samples) { 
					b.setLabel("Train");
					t.setText("");
					t.setEditable(false);
				} // if
			} // if
			else {
				if (!training_done) {
					parent.stop_training();
					b.setLabel("Train");
					return;
				} // if
			
				double[][] encoded_outputs =
					new double[max_samples][outputs_count];

				for (int i=0; i < max_samples; i++) {
					for (int j=0; j < outputs_count; j++) {
						if (i == j) {
							encoded_outputs[i][j] = 1.0;
						} // if
						else {
							encoded_outputs[i][j] = 0.0;
						} // else
					} // for
				} // for

				training_done = false;
				parent.start_training(inputs, encoded_outputs);

				set_test_mode();
			} // else

			System.out.println("Clicked");
			return;
		} // if
		
		if (( e.getSource() == b ) && ( train_mode == false )) {
			t.setText("");

			System.out.println("");
			for (int i=0; i < outputs.length; i++) {
				System.out.println(outputs[i]);
			} // for

			// get the response and check that it's not an error.
			int net_response = parent.test(c.get_inputs());
			if (net_response < 0) { return; }

			t.setText( outputs[net_response] );
			
			return;
		} // if
	} // actionPerformed()
	
	public void set_train_mode() {
		setTitle("Train Mode");
			
		l.setText("Correct Output");
			
		t.setText("");
		t.setEditable(true);
		
		if (current_sample == max_samples) {
			b.setLabel("Train");
			t.setEditable(false);

			if (!training_done) { 
				b.setLabel("Stop Training"); 
			} // if
		} // if
		else {
			b.setLabel("Add to training data");
		} // else

		train_mode = true;
			
		repaint();
	} // set_train_mode()

	public void set_test_mode() {
		setTitle("Test Mode");
			
		l.setText("Output of Net");
			
		t.setText("");
		t.setEditable(false);
			
		b.setLabel("Test");
			
		train_mode = false;
			
		repaint();
	} // set_test_mode()

	public void set_sample_count(int sample_count, 
			int output_node_count) {
		max_samples = sample_count;
		outputs_count = output_node_count;

		inputs = new double[sample_count][];
		outputs = new String[sample_count];

		current_sample = 0;

		set_train_mode();
	} // set_sample_count()

	public void finished_training() {
		training_done = true;

		set_test_mode();
	} // finished_training();

} // class ClickFrame








class TextWindow extends Frame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	TextArea a;

	public TextWindow(String title) {
		super(title);
		setSize(350, 250);
		setBackground(Color.white);

		a = new TextArea("", 50, 40,
			TextArea.SCROLLBARS_VERTICAL_ONLY);
		a.setEditable(false);

		add(a, BorderLayout.CENTER);

		//setResizable(false);
	} // TextWindow()

	public void setText(String new_text) {
		a.setText(new_text);
	} // setText()
} // class TextWindow




















