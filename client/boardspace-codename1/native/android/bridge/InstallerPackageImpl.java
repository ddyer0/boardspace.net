package bridge;

import android.app.Activity;
import com.codename1.impl.android.AndroidNativeUtil;
import android.content.pm.PackageManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.ActivityInfo;
import android.net.wifi.WifiManager;
import android.util.DisplayMetrics;
import android.os.Build;
import android.os.Build.VERSION;
import android.view.Display;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.math.BigInteger;
import android.os.Build;
import java.lang.reflect.Method;
import java.io.DataOutputStream;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;


public class InstallerPackageImpl {
	// cribbed from various "drawer" source code, decompiled from the lastgameboard chess app./
    public static final String ANALYTICS_SENDER_PACKAGE = "com.lastgameboard.gameboardservicetest";    
	public static String ACTION_CHANGE_VISIBILITY = "com.lastgameboard.gameboardservice.drawer.action_CHANGE_DRAWER_VISIBLITY";
	public static String EXTRA_CHANGE_VISIBILITY = "com.lastgameboard.gameboardservice.drawer.key.CHANGE_DRAWER_VISIBLITY_STATE";

	public static void setDrawerVisibility(Application application, boolean z) 
		{
	        setDrawerVisibility(application.getApplicationContext(), z);
	    }

	public static void setDrawerVisibility(Context context, boolean z) 
		{
	        Intent intent = new Intent();
	        intent.setAction(ACTION_CHANGE_VISIBILITY);
	        intent.putExtra(EXTRA_CHANGE_VISIBILITY, z ? 1 : 0);
	        intent.setComponent(new ComponentName(ANALYTICS_SENDER_PACKAGE, "com.lastgameboard.gameboardservice.drawer.DrawerVisibilityBroadcastReceiver"));
	        context.sendBroadcast(intent);
	    }

	public String eval(String command)
	{	
		try{
			Context c = AndroidNativeUtil.getContext();
			setDrawerVisibility(c,false);
			//this is the alternate that imitates the adb commands that turn the drawer off.
			//it requires that app have the INTERACT_ACROSS_USERS permission granted
			//Runtime.getRuntime().exec(command);
		}
		catch(Throwable e){
		    return e.toString();
		}
		return "ok";
	}
	
    public String getPackages() 
    {
    	Context c = AndroidNativeUtil.getContext();
		PackageManager pm = c.getPackageManager();
		java.util.List<PackageInfo> installedPackages = pm.getInstalledPackages(PackageManager.GET_ACTIVITIES);
		String res = "";
	    for (PackageInfo p : installedPackages) 
	    {	
	    	res += " "+p.packageName;
	    }
		return(res);
	}
    private double stableDensity(DisplayMetrics metrics)
    {	// very old versions of android didn't have this variable. Rather than try
    	// to guess, just punt if there is a problem
    	try {
    		return metrics.DENSITY_DEVICE_STABLE;
    	}
    	catch (Throwable err)
    	{
    		return(metrics.densityDpi);
    	}
    }
    public String getOSInfo()
    {	Context c = AndroidNativeUtil.getContext();
		DisplayMetrics metrics = c.getResources().getDisplayMetrics();
		//Display display = c.getDisplay();
		//DisplayMetrics metrics = new DisplayMetrics ();
		//display.getMetrics(metrics);

    	return("brand="+android.os.Build.BRAND +"\n"
    			+"board="+android.os.Build.BOARD +"\n"
    			+"manufacturer="+android.os.Build.MANUFACTURER +"\n"
    			+"product="+android.os.Build.PRODUCT +"\n"
    			+"model="+android.os.Build.MODEL +"\n"
    			+"hardware="+android.os.Build.HARDWARE +"\n"
    			+"device="+android.os.Build.DEVICE +"\n"
    			+"tags="+android.os.Build.TAGS +"\n"
    			+"display="+android.os.Build.DISPLAY +"\n"
     			+"DENSITY_DEVICE_STABLE="+stableDensity(metrics)+"\n"
       			+"density="+metrics.density+"\n"
       			+"densityDpi="+metrics.densityDpi+"\n"
       			+"scaledDensity="+metrics.scaledDensity+"\n"
       			+"widthPixels="+metrics.widthPixels+"\n"
       			+"xdpi="+metrics.xdpi+"\n"
       			+"heightPixels="+metrics.heightPixels+"\n"
       			+"ydpi="+metrics.ydpi+"\n"      			
       			
    			);
    }
    public String getInstaller(String pack) 
    {	// amazon to "com.amazon.venezia" as well to contrast with 
		// Google Play's "com.android.vending".
		Context c = AndroidNativeUtil.getContext();
		PackageManager pm = c.getPackageManager();
		return(pm.getInstallerPackageName(pack));
	}
    public int getOrientation() 
    {	
    	Activity act = AndroidNativeUtil.getActivity();
    	return(act.getRequestedOrientation());
    }
    public int setOrientation(boolean portrait,boolean reverse)
    {	int newo = portrait 
			?   (reverse
					? ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
	    			: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
			:  	(reverse
					? ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
					: ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    	setOrientation(newo);
    	return(newo);
    }
    
    public void setOrientation(int o)
    {
    	Activity act = AndroidNativeUtil.getActivity();
    	//act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
    	act.setRequestedOrientation(o);
    }
    public double getScreenDPI()
    {	
    	Context c = AndroidNativeUtil.getContext();
    	DisplayMetrics metrics = c.getResources().getDisplayMetrics();
    	//Display display = c.getDisplay();
    	//DisplayMetrics metrics = new DisplayMetrics ();
    	//display.getMetrics(metrics);
  	
    	
		long z = (long)metrics.densityDpi;
		long y =(long)stableDensity(metrics);
		long x = (long)metrics.xdpi;
		long v = z + (x<<11)+ (y<<22);
		return((double)v);
    }
    
    public String getLocalWifiIpAddress() {
    	Context context = AndroidNativeUtil.getContext();
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            ipAddressString = null;
        }

        return ipAddressString;
    }
    /**
     * Retrieves the net.hostname system property
     */
    public String getHostName() {
        try {
            Method getString = Build.class.getDeclaredMethod("getString", String.class);
            getString.setAccessible(true);
            return getString.invoke(null, "net.hostname").toString();
        } catch (Exception ex) {
            return null;
        }
    }
    
    public void hardExit() {
    	AndroidNativeUtil.getActivity().finish();
    	System.exit(0);
    }
    
    public boolean isSupported() {
        return true;
    }

}
