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

import static java.lang.Math.*;
/**
 * this is a mostly simple implementation of {@link SpriteProtocol}, which should be adequate
 * for any simple requirement to drag a sprite across the screen. 
 * @author ddyer
 *
 */
public class SimpleSprite implements SpriteProtocol
{	
	/**
	 * various acceleration profiles for sprites.
	 * @author ddyer
	 *
	 */
	public enum Movement 
	{
		Linear,		// @value uniform speed
		SlowIn,		// @value slow at first
		SlowOut,	// @value slowing at the end
		SlowInOut,	// @value slow start and slow finish
		Accelerating	// @value faster and faster
	};
	public String toString()
	{
		return(""+start_x+","+start_y+"-"+end_x+","+end_y+" "+(endTime-startTime)+" "+chip);
	}
	public Movement movement = Movement.SlowInOut;
	double specifiedStart;			// starting time from activation in seconds
	public double specifiedDuration;		// duration in seconds
	public double getStart() { return(specifiedStart); }
	public double getDuration() { return(specifiedDuration); }
	private long initialNow;		// absolute time in milliseconds
	private long startTime;			// absolute time in milliseconds
	private long endTime;			// absolute time in milliseconds
	int start_x;					// pixel coordinates on screen
	int start_y;					// 
	int end_x;						//
	int end_y;						//
	public boolean overlapped = false;
	public double start_rotation = 0;
	public double end_rotation = 0;
	public boolean isAlwaysActive = false;
	public boolean isAlwaysActive() { return(isAlwaysActive); }
	public int wander_x=-1;
	public int wander_y=-1;
	Drawable chip = null;
	public int size;
	public int finalsize;
	
	public void setFromEqualTo() 
	{	start_x = end_x;
		start_y = end_y;
		wander_x = start_x;
		wander_y = start_y;
	}
	// activate sets the clock running based on a specific time.  This fixes
	// the problem that a group of animations were scheduled to dovetail perfectly,
	// but didn't because they couldn't all be created instantaneously
	private void activate(long now)
	{
		if(initialNow==0)
		{
			initialNow = now;
			startTime = now+(long)(specifiedStart*1000);
			endTime = now + (long)ceil((specifiedStart+specifiedDuration)*1000);
		}
	}
	/**
	 * if true, this animation deliberately overlaps the contents of the
	 * destination cell.  If false, the content of the animation cell 
	 * are suppressed until the animation finishes
	 */
	public boolean isOverlapped() { return(overlapped); }
	/**
	 * set the sprite to wander slightly.  A secondary point inside the start-finish rectangle
	 * is chosen at random. This point interpolates toward the endpoint and the starting point
	 * interpolates toward the random point instead of the eventual endpoint.
	 */
	public void setRandomWander()
	{	Random r = new Random();
		// randomize a secondary target within the convex hull of the start and end points
		wander_x = G.interpolate(r.nextDouble(),start_x,end_x);
		wander_y = G.interpolate(r.nextDouble(), start_y,end_y);
		long duration = endTime-startTime;
		startTime += (int)(r.nextDouble()*0.1*duration);
		endTime += (int)(r.nextDouble()*0.1*duration-0.05*duration);
	}
	/**
	 * constructor 
	 * @param randomize if true, randomize the animation path
	 * @param ch a stockart chip to be drawn
	 * @param width the chip's display size
	 * @param start the number of seconds to delay before starting
	 * @param duration the number of seconds to the end of the animation
	 * @param from_x staring x coordinate
	 * @param from_y starting y coordinate
	 * @param to_x ending x coordinate
	 * @param to_y ending y coordinate
	 */
	public SimpleSprite(boolean randomize,Drawable ch,int width,double start,double duration,int from_x,int from_y,int to_x,int to_y,double rot)
	{	this(randomize,ch,width,start,duration,from_x,from_y,to_x,to_y,rot,rot);
	}
	/**
	 * 
	 * constructor 
	 * @param randomize if true, randomize the animation path
	 * @param ch a stockart chip to be drawn
	 * @param width the chip's display size
	 * @param start the number of seconds to delay before starting
	 * @param duration the number of seconds to the end of the animation
	 * @param from_x staring x coordinate
	 * @param from_y starting y coordinate
	 * @param to_x ending x coordinate
	 * @param to_y ending y coordinate
	 * @param from_rot starting rotation
	 * @param end_rot ending rotation
	 */
	public SimpleSprite(boolean randomize,Drawable ch,int width,double start,double duration,int from_x,int from_y,int to_x,int to_y,double from_rot,double end_rot)
	{	specifiedStart =start;
		specifiedDuration = duration;
		size = width;
		end_rotation = end_rot;
		start_rotation = from_rot;
		finalsize = size;
		start_x = from_x;
		start_y = from_y;
		end_x = to_x;
		end_y = to_y;
		chip = ch;
		if(randomize) { setRandomWander(); }
	}

	/**
	 * return the height of this animation element.  This is used to reduce the
	 * height of the destination stack when animating a move.  In most cases this
	 * will be "1", but when moving a stack as a single element, it's the height
	 * of the stack.
	 */
	public int animationHeight() { return(chip.animationHeight()); }
	/**
	 * constructor 
	 * @param randomize if true, randomize the animation path
	 * @param ch a stock art chip to be drawn
	 * @param width the chip's display size
	 * @param seconds the number of seconds to the end of the animation
	 * @param from_x staring x coordinate
	 * @param from_y starting y coordinate
	 * @param to_x ending x coordinate
	 * @param to_y ending y coordinate
	 * @param rot the piece rotation
	 */
	public SimpleSprite(boolean randomize,Drawable ch,int width,double seconds,int from_x,int from_y,int to_x,int to_y,double rot)
	{	this(randomize,ch,width,0.0,seconds, from_x, from_y, to_x, to_y,rot);
	}

	/**
	 * constructor 
	 * @param randomize if true, randomize the animation path
	 * @param ch a stock art chip to be drawn
	 * @param width the chip's display size
	 * @param seconds the number of seconds to the end of the animation
	 * @param from_x staring x coordinate
	 * @param from_y starting y coordinate
	 * @param to_x ending x coordinate
	 * @param to_y ending y coordinate
	 * @param from_rot the starting piece rotation
	 * @param end_rot the final piece rotation
	 */
	public SimpleSprite(boolean randomize,Drawable ch,int width,double seconds,int from_x,int from_y,int to_x,int to_y,double from_rot,double end_rot)
	{	this(randomize,ch,width,0.0,seconds, from_x, from_y, to_x, to_y,from_rot,end_rot);
	}

	private double interpolate(long now)
	{	// calculate the linear range
		activate(now);
		double duration = endTime-startTime;
		double frac = duration<=0 ? 1 : (now-startTime)/(double)(duration);
		// expand or compress the range
		switch(movement)
		{
		default: throw G.Error("Not expecting movement style %s",movement);
		case Linear: return(frac);
		case SlowIn: return(1.0-cos(frac*PI/2));
		case SlowOut: return(sin(frac*PI/2));
		case SlowInOut: 
			{
			double ss = cos(frac*PI);
			double ss2 = frac>0.5 ? (0.5-ss/2) : (0.5-ss/2);
			return(ss2);
			}
		case Accelerating:
			{
			double ss = (exp(frac)-1.0)/(E-1);
			return(ss);
			}
		}
	}
	/**
	 * draw the sprite at the specified position along it's path
	 * 
	 * @param gc the graphics to draw
	 * @param c the canvas being drawn on
	 * @param now the current time
	 * */
	public void draw(Graphics gc,exCanvas c,long now)
	{	G.Assert(startTime<=endTime,"positive time");
		activate(now);
		G.Assert(startTime<=endTime,"positive time2");
		if((chip!=null) && (size>0) && (now>=startTime)&&(now<=endTime))
		{
		double frac = interpolate(now);
		int target_x = end_x;
		int target_y = end_y;
		if(wander_x>=0) { target_x = G.interpolate(frac,wander_x,target_x); }
		if(wander_y>=0) { target_y = G.interpolate(frac,wander_y,target_y); }
		int thissize = G.interpolate(frac,size,finalsize);
		int posx = G.interpolate(frac,start_x,target_x);
		int posy = G.interpolate(frac,start_y,target_y);
		double er = G.interpolateD(frac,start_rotation,end_rotation);
		//StockArt.SmallO.drawChip(gc,c,size,end_x,end_y,null);
		//StockArt.SmallX.drawChip(gc,c,size,wander_x,wander_y,null);
		GC.setRotation(gc,er, posx,posy);
		int sz = c.activeAnimationSize(chip,thissize);
		if(G.Advise(sz>2,"animation too small %s",this))
			{ chip.draw(gc,c,sz, posx,posy,null); 
			}
		GC.setRotation(gc,-er, posx,posy);
		}
	}
/**
 * return true if the sprite is expired
 */
	public boolean isExpired(long now) 
	{
		activate(now);
		return((now>endTime) || (chip==null) || (size<=0));
	}
	/** return true if this sprite has started */
	public boolean isStarted(long now)
	{
		return(now>=startTime);
	}
/**
 * request the sprite to expire and return resources.
 */
	public void cancel() {
		endTime = startTime-1;
	}

}
