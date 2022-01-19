package goban.shape.shape.gui;

import java.io.PrintStream;

import javax.swing.JFrame;
import lib.Image;
import goban.shape.shape.lisp.reader;
/** shape library asa main */
public class Shape {
	public static void printhelp(PrintStream console)
	{ console.println("java Shape.gui.Shape {-sample filename} { -library filename } {-background filename} { -debug } { -showenv } { -lisp }");
	}
	// note that this main must not be visible to the ios application
	public static void main(String args[])
	{	PrintStream console = System.out;
		runner myrunner=null;
		Thread mythread=null;
		boolean lispformat = false;
		String lisp_library =        "g:/share/projects/boardspace-java/boardspace-games/goban/shape/data/shape-data.lisp";
		String lisp_sample_library = "g:/share/projects/boardspace-java/boardspace-games/goban/shape/data/sample.lisp";
		String library =             "g:/share/projects/boardspace-java/boardspace-games/goban/shape/data/shape-data.zip";
		String sample_library =      "g:/share/projects/boardspace-java/boardspace-games/goban/shape/data/sample-shape-data.zip";
		String background = "board.jpg";
		
		for(int i=0;i<args.length;)
		{String thisarg = args[i++];
			if(thisarg.equals("-library")) { library = args[i++]; }
			else if(thisarg.equals("-sample")) { sample_library = args[i++]; }
			else if(thisarg.equals("-lisp")) { lispformat = true; }
			else if(thisarg.equals("-h")) { printhelp(console); }
			else if(thisarg.equals("-background")) { background = args[i++]; }
			else { console.println("Argument " + thisarg + " not understood"); 
				printhelp(console);
			}
		}
		console.println("Process sample data");
		reader.Read_ShapeLibrary(sample_library,lisp_sample_library,console);
		console.println("Process full data");
		reader.Read_ShapeLibrary(library,lisp_library,console);
		
		ShapePanel p = new ShapePanel();
		JFrame f= new JFrame();
		f.setSize(800,400);
		f.add(p);
		Image im = new Image("background");
		im.setImage(p.getToolkit().getImage(background));
		myrunner = new runner(p,library,sample_library,lispformat,console,im);
		mythread = new Thread(myrunner);
		f.addWindowListener(myrunner);
		f.setVisible(true);
		mythread.start();
		mythread.setPriority(mythread.getPriority()-1);
	}
}
