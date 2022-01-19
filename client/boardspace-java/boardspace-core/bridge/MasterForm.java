package bridge;

import java.awt.Component;
import java.awt.Container;
import lib.Graphics;
import lib.G;
import lib.GC;
import lib.Http;
import lib.SizeProvider;

@SuppressWarnings("serial")
public class MasterForm extends Container {

	/**
	 * 
	 */
	private static MasterForm masterForm = null;
	private static MasterPanel masterPanel = null;
	public MasterForm(String name) {}
	public static MasterForm getMasterForm()
	{
		if(masterForm==null) { masterForm=new MasterForm(Config.APPNAME); }
		return(masterForm);
	}
	private void addMasterPanel()
	{	try {
	  if(masterPanel==null)
		  {MasterPanel mp = masterPanel = new MasterPanel(this);
		  add(mp);
		  mp.setVisible(true);
		  masterPanel = mp;
		  }
		}
		catch (Throwable err) { Http.postError(this,"adding master panel",err); }
	}
	public static MasterPanel getMasterPanel()
	{	final MasterForm form = getMasterForm();
		if(masterPanel==null)
			{
			G.runInEdt(new Runnable() { public void run() { form.addMasterPanel(); }});
			}
		  return(masterPanel);
	}


	private int globalRotation=0;
	public static int getGlobalRotation() { return(getMasterForm().globalRotation); }
	public static void setGlobalRotation(int n) {
		getMasterForm().globalRotation = n&3;
    }


    /* support for rotating windows */
	public static int translateX(SizeProvider client,int x) 
	{ return( ((getGlobalRotation())&2)!=0
				? client.getWidth()-x
				: x);
	}
	public static int translateY(SizeProvider client,int y) 
	{ return( ((getGlobalRotation())&2)!=0
				? client.getHeight()-y
				: y);
	}

	public static boolean rotateCanvas(Component client,Graphics g)
	{	boolean v = rotateCanvas(g,client.getWidth()/2,client.getHeight()/2);;
		//if(v) { System.out.println("ro "+client+" "+g); }
		return v;
	}
	static Graphics rotated = null;
	public static boolean rotateCanvas(Graphics g,int cx,int cy)
	{	
		if((g!=null ) &&(g!=rotated))
		{
		int rot = getGlobalRotation();
		if(rot!=0)
		{ 	rotated = g;
			GC.resetAffine(g);
			GC.setRotation(g,(rot*Math.PI)/2,cx,cy); 
 			return(true);
		}}
		return(false);
	}
	
	public static void unrotateCanvas(Component client,Graphics g)
	{	//System.out.println("un "+client+" "+g);
		unrotateCanvas(g,client.getWidth()/2,client.getHeight()/2);
	}

	public static void unrotateCanvas(Graphics g,int cx,int cy)
	{	
 			int rot = getGlobalRotation();
 			if(rot!=0)
 			{
 			if(rotated==null)
 			{
 				System.out.println("not rotated");
 			}
 			rotated = null;
 			GC.setRotation(g,-(rot*Math.PI)/2,cx,cy); 
 			}
 	    }
	public static boolean canRepaintLocally(Graphics g) { return(true); }	


}
