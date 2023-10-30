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

import java.net.InetAddress;
import java.lang.Throwable;

public class InstallerPackageImpl implements InstallerPackage
{
    public String getPackages() {
        return null;
    }

	public void setDrawers(boolean vis)
	{	
	}
	public String eval(String command)
	{	
		
		return "ok";
	}
	

    public String getInstaller(String param) {
        return null;
    }
    public String getHostName() 
    { 	try {
    		return(InetAddress.getLocalHost().getHostName());
		} catch (Throwable e) {};
		return(null);
	} 
    
    public boolean isSupported() {
        return true;
    }
    public double getScreenDPI() { return(96.0); }
    public String getOSInfo() { return("simulator"); }
    public int getOrientation() { return(0); }
    public int setOrientation(boolean portrait,boolean rev) { return(0); }
    public String getLocalWifiIpAddress() { return("localhost"); }
    public void hardExit() {
    	System.exit(0);
    }
}
