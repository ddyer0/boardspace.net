package bridge;


public interface InstallerPackage extends NativeInterface
{
    public String getInstaller(String name);
    public String getPackages();
    public int getOrientation();
    public int setOrientation(boolean portrait,boolean rev);
    public String getLocalWifiIpAddress();
    public String getHostName();
}
