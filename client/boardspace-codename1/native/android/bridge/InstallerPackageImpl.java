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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.math.BigInteger;
import android.os.Build;
import java.lang.reflect.Method;

public class InstallerPackageImpl {
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
    public String getOSInfo()
    {
    	return("brand="+android.os.Build.BRAND +"\n"
    			+"board="+android.os.Build.BOARD +"\n"
    			+"manufacturer="+android.os.Build.MANUFACTURER +"\n"
    			+"product="+android.os.Build.PRODUCT +"\n"
    			+"model="+android.os.Build.MODEL +"\n"
    			+"hardware="+android.os.Build.HARDWARE +"\n"
    			+"device="+android.os.Build.DEVICE +"\n"
    			+"tags="+android.os.Build.TAGS +"\n"
    			+"display="+android.os.Build.DISPLAY +"\n"
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
    	Activity act = AndroidNativeUtil.getActivity();
    	act.setRequestedOrientation(
    			newo
     					);
    	return(newo);
    }
    
    public double getScreenDPI()
    {
		Context c = AndroidNativeUtil.getContext();
		DisplayMetrics metrics = c.getResources().getDisplayMetrics();
		return(metrics.densityDpi);
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
