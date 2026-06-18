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
package graphicstest;

import java.awt.Rectangle;
import lib.Graphics;
import lib.CellId;
import lib.ExtendedHashtable;
import lib.G;
import lib.HitPoint;
import lib.Tokenizer;
import lib.LFrameProtocol;
import online.game.*;
import online.game.sgf.sgf_node;

import online.search.SimpleRobotProtocol;

import bridge.XJMenu;


public class GraphicsViewer extends CCanvas<GraphicsCell,GraphicsBoard> implements GraphicsConstants
{		
    static final String Prototype_SGF = "graphicstest"; // sgf game name

    // file names for jpeg images and masks
    static final String ImageDir = "/graphicstest/images/";
     
    // private state
    private GraphicsBoard bb = null; //the board from which we are displaying
 
     
/**
 * this is called during initialization to load all the images. Conventionally,
 * these are loading into a static variable so they can be shared by all.
 */
    public synchronized void preloadImages()
    {	GraphicsChip.preloadImages(loader,ImageDir);	// load the images used by stones
		gameIcon = GraphicsChip.Icon.image;
    }

    /**
     * this is the hook for substituting alternate tile sets.  This is called at a low level
     * from drawChip, and the result is passed to the chip's getAltChip method to substitute
     * a different chip.
     */
    public int getAltChipset() { return(0); }
    
    public XJMenu testMenu = null;
    
    interface TestAble
    {
    	public void runTest(Graphics gc,int x,int y,int w,int h);
    }
    
    
    class Test
    {	Test(String sn,String ln,TestAble r)
    	{
    		run = r;
    		shortName = sn;
    		longName = ln;
    	}
    	TestAble run;
    	String shortName;
    	String longName;
    	Object menuItem;
    }

	/**
	 * 
	 * this is the real instance intialization, performed only once.
	 * info contains all the goodies from the environment.
	 * */
    public void init(ExtendedHashtable info,LFrameProtocol frame)
    {	
    	// for games with more than two players, the default players list should be 
    	// adjusted to the actual number, adjusted by the min and max
        super.init(info,frame);
        // use_grid=reviewer;// use this to turn the grid letters off by default
        
        
        if(G.debug())
        {	// initialize the translations when debugging, so there
        	// will be console chatter about strings not in the list yet.
        	GraphicsConstants.putStrings();
        }
        bb = new GraphicsBoard();
        //
        // this gets the best results on android, but requires some extra care in
        // the user interface and in the board's copyBoard operation.
        // in the user interface.
        useDirectDrawing(true);
        
        testMenu = new XJMenu("Test Actions",true);
        myFrame.addToMenuBar(testMenu);
        for(Test t : tests)
        {
        	t.menuItem = myFrame.addAction(testMenu,t.shortName,deferredEvents);
        }
        testMenu.setVisible(true);
        doInit(false);
    }

    /** 
     *  used when starting up or replaying and also when loading a new game 
     *  */
    public void doInit(boolean preserve_history)
    {
        //System.out.println(myplayer.trueName + " doinit");
        super.doInit(preserve_history);				// let commonViewer do it's things
        bb.doInit(bb.gametype);						// initialize the board
        if(!preserve_history)
    	{ 
        	// the color determines the first player
        	startFirstPlayer();
        	//
        	// alternative where the first player is just chosen
        	//startFirstPlayer();
 	 		//PerformAndTransmit(reviewOnly?"Edit":"Start P"+first, false,replayMode.Replay);

    	}
    }

    public void setLocalBounds(int x, int y, int width, int height)
    {	G.SetRect(fullRect,x,y,width,height);
    }

    public void drawSprite(Graphics g,int obj,int xp,int yp)
    {
    }

	
    static boolean toggle = false;
    static int step = 0;

    public void redrawBoard(Graphics gc, HitPoint selectPos)
    {
    	if(gc!=null)
    	{
    		selectedTest.run.runTest(gc,0,0,G.Width(fullRect),G.Height(fullRect)); 	
    	}

    }

    public boolean Execute(commonMove mm,replayMode replay)
    {	mm.player = 0;
        handleExecute(bb,mm,replay);
        
         return (true);
    }

    public commonMove ParseNewMove(String st,int pl)
    {
        return (new GraphicsMovespec());
    }


    public void StartDragging(HitPoint hp)
    {
    }


    public void StopDragging(HitPoint hp)
    {
        step++; 
        toggle=!toggle;
        CellId id = hp.hitCode;
       	if(!(id instanceof PrototypeId))  {   missedOneClick = performStandardActions(hp,missedOneClick);   }
        else {
        missedOneClick = false;
        PrototypeId hitCode = (PrototypeId)id;
        // if direct drawing, hp.hitObject is a cell from a copy of the board
        GraphicsCell hitObject = bb.getCell(hitCell(hp));
        switch (hitCode)
        {
        default:
        	if (performStandardButtons(hitCode, hp)) {}
        	else if (performVcrButton(hitCode, hp)) {}	// handle anything in the vcr group
            else
            {
            	throw G.Error("Hit Unknown object " + hitObject);
            }
        	break;
        }
        }
    }

     public String gameType() 
    	{
    	return("undefined"); 
    	}	
     
    // this is the subgame "setup" within the master type.
    public String sgfGameType() { return(Prototype_SGF); }	// this is the official SGF number assigned to the game

   
    /**
     * parse and perform the initialization sequence for the game, which
     * was produced by {@link online.game.commonCanvas#gameType}
     */
     public void performHistoryInitialization(Tokenizer his)
    {   //the initialization sequence

    }


    /** handle action events from menus.  Don't do any real work, just note
     * state changes and if necessary set flags for the run loop to pick up.
     * 
     */
    public boolean handleDeferredEvent(Object target, String command)
    {
        boolean handled = super.handleDeferredEvent(target, command);
        if(!handled )
        {
        	for(Test t : tests)
        	{
        		if(t.menuItem.equals(target))
        		{
        			selectedTest = t;
        			selected = t.run;
        			handled = true;
        		}
        	}
        }
        return (handled);
    }

    public BoardProtocol getBoard()   {    return (bb);   }

    public SimpleRobotProtocol newRobotPlayer() 
    {  throw G.Error("No robots");
    }

    public void ReplayMove(sgf_node no)
    {
    }

    public String gameProgressString()
    {	// this is what the standard method does
    	 return("");
    }

    public Rectangle createPlayerGroup(int player, int x, int y, double rotation, int unit) {
		return null;
	}
    
    public commonMove EditHistory(commonMove m)
    {	return(EditHistory(m,true));
    }
	
    public void ViewerRun(int n)
    {
    	super.ViewerRun(2000);
    }

    Test[] tests = {  	
    		//new Test("issue 4914","glitchy animation",new Test_4914()),   		// seems cured 6/1/2026
    		new Test("issue 3302","scaling and translation",new Test_3302()),
    		new Test("boardspace graphics","complex clipping and rotation",new test_boardspace_graphics()),
    		new Test("boardspace scale","simple scaling",new test_scale()),
    		// no longer considered a problem
    		// new Test("issue 3921","complex clipping and rotation",new test_3921()),
        	new Test("issue 3037","simpler rotation test",new Test_3037()),
        	//new Test("sound clips","test all sound clips",new soundclips()),
        	
        	new Test("issue 5171","clipping outside",new test_5171()),
    		new Test("issue 5058","ios crash overpush",new Test_5058()), 
    		new Test("issue 5058a","ios crash overpop",new Test_5058a()), 
        	//new Test("ios bad code #3108","gets nullpointerexception",new Dtest_3108()),
        	//new Test("issue 3136 gc","death by gc",new dtest_3136()),
        };
        Test selectedTest = tests[0];
        TestAble selected = selectedTest.run;
     
        
}

