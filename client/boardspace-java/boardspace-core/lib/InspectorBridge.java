package lib;


import org.pf.joi.Inspector;
/**
 * this class is only instantiated by Class.forName, so it's dependency on the
 * inspector package remains latent unless actually invoked.  As implemented
 * here, use  G.inspect(something) to actually inspect.
 * @author ddyer
 *
 */
public class InspectorBridge  extends Inspector implements InspectorInterface 
{		
	public void view(Object o) {
		inspect(o);
	}

	public void viewWait(Object o) {
		inspectWait(o);
	}
/*
 * Caused by: java.security.AccessControlException: access denied ("java.util.PropertyPermission" "org.pf.util.os.name" "read")
	at java.security.AccessControlContext.checkPermission(Unknown Source)
	at java.security.AccessController.checkPermission(Unknown Source)
	at java.lang.SecurityManager.checkPermission(Unknown Source)
	at sun.plugin2.applet.AWTAppletSecurityManager.checkPermission(Unknown Source)
	at java.lang.SecurityManager.checkPropertyAccess(Unknown Source)
	at java.lang.System.getProperty(Unknown Source)
	at org.pf.util.SysUtil.determineOsFamilyName(SysUtil.java:102)
	at org.pf.util.SysUtil.<clinit>(SysUtil.java:66)
	... 13 more

 * java.lang.ExceptionInInitializerError
	at org.pf.file.PropertyFileLoader.loadProperties(PropertyFileLoader.java:75)
	at org.pf.file.PropertyFileLoader.loadProperties(PropertyFileLoader.java:59)
	at org.pf.joi.Preferences.initialize(Preferences.java:255)
	at org.pf.joi.Preferences.<init>(Preferences.java:128)
	at org.pf.joi.Preferences.<clinit>(Preferences.java:75)
	at org.pf.joi.AbstractObjectSpy.<init>(AbstractObjectSpy.java:69)
	at org.pf.joi.ObjectSpy.<init>(ObjectSpy.java:51)
	at org.pf.joi.Inspector.objectSpyFor(Inspector.java:705)
	at org.pf.joi.Inspector.objectSpyFor(Inspector.java:722)
	at org.pf.joi.Inspector.inspectObject(Inspector.java:682)
	at org.pf.joi.BasicInspector.inspectObject(BasicInspector.java:285)
	at org.pf.joi.Inspector.launchInspectorOn(Inspector.java:459)
	at org.pf.joi.Inspector.inspect(Inspector.java:174)
	at org.pf.joi.Inspector.inspect(Inspector.java:160)
	at lib.InspectorBridge.view(InspectorBridge.java:15)
	at lib.G.inspect(G.java:1849)

 */
}
