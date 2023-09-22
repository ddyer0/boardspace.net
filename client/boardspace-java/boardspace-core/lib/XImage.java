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
package lib;



//helper class for double buffering.  This keeps track of
//what frames have been prepared and have been seen.
public class XImage 
{	public Image theImage;				// the underlying image
	public double imageSize(ImageStack im)	{ return(theImage==null) ? 0 : theImage.imageSize(im); }
	public int sequence = 0;
	static int screenCount = 1;
	private boolean readyToSee = false;	// true if prepared
	private boolean written = false;	// true when written but not yet aged
	private boolean waitForRewrite = false;
	@SuppressWarnings("unused")
	// lastGraphics remembers the last result of getGraphics.  It has no function
	// other than preventing the underlying structures from being garbage collected
	// current theory is that this is the underlying cause of the long running 
	// "incomplete background" bug.
	void clear() 
	{ written = readyToSee = false; 
	  waitForRewrite = true;
	}

	boolean written() { return(written); }
	boolean unseen() { return(written && endReadTime==0); }
	
	int timeUntilReadyToWrite(long now)
	{
		if(!waitForRewrite) { return(0); }
		long delay = Math.max(0, endReadTime-now);
		if(delay<=0) { waitForRewrite=false; }
		//G.addLog("Must wait "+this+" "+delay);
		
		return((int)(delay));
	}
	
	boolean readyToDiscard(long now)
	{	int wait = timeUntilReadyToWrite(now);
		return(wait<=0);
	}
	
	int timeUntilReadyToSee(long now)
	{	if(readyToSee) { return(0); }
		if(written)
		{
			long delay = Math.max(0, endWriteTime-now);
			return((int)delay);
		}
		return(0);
	}
	boolean readyToSee(long now) 
	{	if(readyToSee) { return(true); }
		if(written)
		{
			long delay = (endWriteTime-now);
			if(delay<=0) { setReadyToSee(); return(true); }
		}
		return(false);
	}
	
	public void setReadyToSee() { readyToSee=true;	}
	boolean invalid=false;
	int writtenState = 0;
	void setWritten(long n) 
		{ writtenState++;
		  invalid = false; 
		  endWriteTime = n;
		  usedForPinch = false;
		  endReadTime = 0;
		  seenCount = 0;
		  written=true; 
		  }
	// time at which we finished reading the buffer.  Possibly the buffer
	// should be left undisturbed for another repaintStrategy.frameTime milliseconds
	private long endReadTime;
	public long seenCount = 0;
	// time at which we finished writing a frame, plus repaintstrategy.releaseTime milliseconds.
	public void setReadyToRewrite(long when)
	{	seenCount++;
		endReadTime = when;
	}
	public long whenReady() { return(endReadTime); }
	
	private long endWriteTime;

	boolean mustBeComplete = true;
	boolean usedForPinch = false;
	public Graphics getGraphics()	// get the graphics in prep for making a new frame
	{ clear();		// not prepared written or seen yet
	  return(theImage.getGraphics()); 
     }
	// constructor
	XImage(Image im) { theImage = im; sequence=screenCount++; }
	public String toString() { return(""+theImage+"#"+sequence); }
	public Image getImage() { return(theImage); }
	
 }
