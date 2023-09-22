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
package bridge;

/**

The general scheme for boardspace to coexist as mobile and desktop is the the mobile/codename1 branch
has a "compatibility" package to make it look like AWT generation java, and all the classes to implement
this compatibility are in this, the "bridge" package.

Eventually, I decided to meet closer to the middle, so the bridge package in the desktop branch
has grown.  In effect, the two bridge packages constitute a "boardspace compatible" support 
layer.

The big picture for the bridge package is that none of the code is common between the
desktop and mobile versions, but all of the apis are common.

*/