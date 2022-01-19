
package goban.shape.shape;

import net.sf.jazzlib.ZipEntry;
import net.sf.jazzlib.ZipInputStream;
import net.sf.jazzlib.ZipOutputStream;
import bridge.File;
import bridge.FileInputStream;
import bridge.FileOutputStream;
import bridge.ObjectInputStream;
import bridge.ObjectOutputStream;
import bridge.ThreadDeath;
import bridge.URL;

import com.codename1.io.Util;
import com.codename1.io.gzip.GZIPInputStream;


import java.io.IOException;
import java.io.InputStream;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import lib.G;
import lib.Http;

public class ShapeLibrary implements Runnable
{	Vector<OneShape> shapedata = null;		//the raw data
	Hashtable<Integer,ShapeNormalizer> keyed_shapes=new Hashtable<Integer,ShapeNormalizer>();
	
	/* constructor */
	public ShapeLibrary(Vector<OneShape> data)
	{ shapedata = data;
		if(data!=null) { Build_Hashdata(); }
	}
	@SuppressWarnings("unchecked")
	private void read_zipdata(InputStream in)
	{
		try {
			ZipInputStream zip = new ZipInputStream(in);
			zip.getNextEntry();
			Util.register("OneShape",goban.shape.shape.OneShape.class);
			Util.register("ResultCode",goban.shape.shape.ResultCode.class);
			Util.register("MultiResult",goban.shape.shape.MultiResult.class);
			Util.register("ResultProtocol",goban.shape.shape.ResultProtocol.class);
			Util.register("SingleResult",goban.shape.shape.SingleResult.class);
			Util.register("ResultCode",goban.shape.shape.ResultCode.class);
			Util.register("SimpleLocation",goban.shape.shape.SimpleLocation.class);
			ObjectInputStream s = new ObjectInputStream(zip);
			shapedata = (Vector<OneShape>)s.readObject();
			s.close();
		} 
		catch (IOException e) { G.print("IO Eror reading library" + e); }
		catch (ClassNotFoundException e) { G.print("Class not found" + e); }
		if(shapedata!=null) { Build_Hashdata(); }
		
	}
	private void read_lispdata(InputStream in)
	{
		try {
			shapedata = goban.shape.shape.lisp.reader.ReadStream(in);
			in.close();
			if(shapedata!=null) { Build_Hashdata(); }
			goban.shape.shape.lisp.reader.Add_Dropped_Shapes(this);
			Build_Vectordata();
		} 
		catch (IOException e) { G.print("IO Eror reading library" + e); }
		if(shapedata!=null) { Build_Hashdata(); }
		
	}
	private void read_gzipdata(InputStream in)
	{	
		try {
			shapedata = goban.shape.shape.lisp.reader.ReadStream(new GZIPInputStream(in));
			in.close();
			if(shapedata!=null) { Build_Hashdata(); }
			goban.shape.shape.lisp.reader.Add_Dropped_Shapes(this);
			Build_Vectordata();
		} 
		catch (IOException e) { G.print("IO Eror reading library" + e); }
		if(shapedata!=null) { Build_Hashdata(); }
		
	}
	private static ShapeLibrary library = null;
	private String libraryName = null;
	public static boolean loading = false;
	public void waitForLoad()
	{
		if(loading)
		{	try { synchronized(this)
			{
			while(loading)
			{
				this.wait();
			}}
		} catch(InterruptedException err) {};
		}
	}
	public void finishedLoading()
	{
		loading=false;
		synchronized(this) { notifyAll(); }
	}
	
	public void run()
	{
		shapedata=null;
		try {
			InputStream input = G.getResourceAsStream(libraryName);
			if(null==input) { input = new FileInputStream(libraryName); }
			if(libraryName.endsWith(".gzip"))
			{	// this branch is used only in codename1
				read_gzipdata(input);
				Save_ShapeLibrary("file:/shape-data.out","shape-data.zip");
				System.out.println("reloading shape-data.out");
				read_zipdata(input);
				System.out.println("ok");
			}
			else if(libraryName.endsWith(".lisp"))
			{
			read_lispdata(input);	
			}
			else {
			read_zipdata(input);
			}
		}
	    catch (IOException e) { G.print("IO Eror reading library" + e); }
    	catch (ThreadDeath err) { throw err;}
		catch (Throwable e) { Http.postError(this,"loading Go shape library",e); }
		finally { finishedLoading(); }
	}
	public ShapeLibrary(String filename)
	{	
		libraryName = filename;
		loading=true;
		new Thread(this).start();
	}
	
	/** load from a serialized java object */
	public ShapeLibrary(URL libname)
	{	
		libraryName = libname.toExternalForm();
		loading = true;
		new Thread(this).start();
	}
	
	public void Save_ShapeLibrary(String zipname,String filename)
	{
		try {
			FileOutputStream out = new FileOutputStream(new File(zipname));
			ZipOutputStream zip = new ZipOutputStream(out);
			zip.putNextEntry(new ZipEntry(filename));
			ObjectOutputStream s = new ObjectOutputStream(zip);
			s.writeObject(shapedata);
			s.flush();	
			s.close();
			zip.close();
			out.close();
		} 
		catch (IOException e) { G.print("IO Eror writing library" + e); }
	}
	void RemoveOneShape(OneShape s)
	{
		ShapeNormalizer norm[] = s.ExtendToIsomers();
		for(int i=0;i<norm.length;i++)
		{ ShapeNormalizer n = norm[i];
			Integer code = n.hashCode();
			//System.out.println("Remove " + code.intValue()  + " for " + n);
			keyed_shapes.remove(code);
		}
		
	}
	boolean Merge_Shapes(ShapeNormalizer existing,ShapeNormalizer newshape)
	{
		/* attempt to merge two shapes.  This is possible if their position coverages
		do not overlap.  This problem arises from drop-in shapes that arise more than
		one way. */
		OneShape s1 = existing.reference_shape;
		OneShape s2 = newshape.reference_shape;
		
		RemoveOneShape(s1);
		RemoveOneShape(s2);	//first clean up the old stuff
		
		OneShape s3 = s1.Combine_With(s2);
		if(s3!=null) { BuildOneShape(s3); return(true); }
		else { 	return(false); }
	}
	
	/* put all the elements in the hashtable for fast lookup */
	public void BuildOneShape(OneShape s)
	{
		ShapeNormalizer norm[] = s.ExtendToIsomers();
		//System.out.println("shape s " + s + " extends to " + norm );
		for(int i=0;i<norm.length;i++)
		{ShapeNormalizer n=norm[i];
			Integer newkey=n.hashCode();
			ShapeNormalizer newval = (ShapeNormalizer)keyed_shapes.get(newkey);
			if(newval!=null)
			{ //System.out.println("Conflict with " + newkey.intValue() + " for " + n);
				//System.out.println(".. is " + newval + newval.hashCode());
				if(!Merge_Shapes(newval,n))
				{G.print("hash keys must be unique; " );
				G.print( newval + newval.Ascii_Picture() );
				G.print( " conflicts with " + n + n.Ascii_Picture());
				}  
				break;//skip the rest, the merge took care of it if it's possible to do so */
			}else
			{	 keyed_shapes.put(newkey,n);
			}
		}
	}
	public void Build_Hashdata()
	{	keyed_shapes.clear();
		/* build hash data for the primary elements */
		for(Enumeration<OneShape> e = shapedata.elements();
			e.hasMoreElements(); )
		{
			BuildOneShape((OneShape)e.nextElement());
		}	
	}
	public void Build_Vectordata()
	{	Vector<OneShape> v = new Vector<OneShape>();
		G.print("Old vector has " + shapedata.size() + " elements");
		for(Enumeration<ShapeNormalizer> e=keyed_shapes.elements(); e.hasMoreElements(); )
		{ ShapeNormalizer n = e.nextElement();
			OneShape s = n.reference_shape;
			if(!v.contains(s)) { v.addElement(s); }
		}
		G.print("New Vector has " + v.size() + " elements ");
		shapedata = v;
		
	}
	
	/**
	 * locate a shape in the database, or null. The shape will be recognized
	 * independent of its position and orientation.
	 * @param v is an ElementProvider
	 * @return
	 */
	public ShapeProtocol Find_Shape(ElementProvider v)
	{	waitForLoad();
		LocationProvider p = zhash.vectormin(v);
		if(p!=null)
		{
			int hashcode = zhash.hash_black(v,p.getX(),p.getY());
			ShapeProtocol s = (keyed_shapes.get(hashcode));
			return( ((s!=null) && s.equal(v,p.getX(),p.getY()))
				? s
				: null);
		}
		else
		{return(null);
		}
	}
	
	/**
	 * locate a shape in the database, or null.  The shape will be recognized
	 * independent of its position and orientation.
	 * @param v an array of LocationProvider
	 * @return
	 */
	public ShapeProtocol Find_Shape(LocationProvider v[])
	{
		LocationProvider p = zhash.vectormin(v);
		if(p!=null)
		{
			int hashcode = zhash.hash_black(v,p.getX(),p.getY());
			ShapeProtocol s = (keyed_shapes.get(hashcode));
			return( ((s!=null) && s.equal(v,p.getX(),p.getY()))
				? s 
				: null);
		}
		else
		{return(null);
		}
	}
	

	public static boolean load(String name)
	{	library = null;
		library = new ShapeLibrary(name);
		return(library!=null);
	}
	public static ShapeProtocol find(LocationProvider v[])
	{	return(library.Find_Shape(v));
	}
	public static ShapeProtocol find(ElementProvider v)
	{	return(library.Find_Shape(v));
	}
	

}
