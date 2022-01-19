package bridge;


public interface InstallerPackage extends com.codename1.system.NativeInterface 
{
    public String getInstaller(String name);
    public String getPackages();
    public int getOrientation();
    public int setOrientation(boolean portrait,boolean rev);
    public String getLocalWifiIpAddress();
    public String getHostName();
    public double getScreenDPI();
    public String getOSInfo();
    public void hardExit();
}

