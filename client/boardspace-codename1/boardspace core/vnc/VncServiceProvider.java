package vnc;

public interface VncServiceProvider 
{
	public VncScreenInterface provideScreen();
	public  VncEventInterface provideEventHandler();
	public VncServiceProvider newInstance();
	public void stopService(String reason);
	public String getName();
	public void setName(String n);
	public boolean isActive();
}
