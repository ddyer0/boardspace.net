package bridge;

import java.net.InetAddress;
import java.lang.Throwable;

public class InstallerPackageImpl implements InstallerPackage
{
    public String getPackages() {
        return null;
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
