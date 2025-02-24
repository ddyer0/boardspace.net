package online.search.neat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

import lib.G;
import lib.IoAble;
import lib.StreamTokenizer;

public class NetIo {
	static String Schema = "1";
	static String IdString = "SAVENETWORK";
	static String CommentString = "COMMENT";
	static String BeginString = "BEGIN";
	static String EndString = "END";
	
	
	public static IoAble load(String file) 
	{	return load(new File(file));
	}
	public static IoAble load(File file)
	{
		InputStream s=null;
		IoAble net = null;
		try {
			s = new FileInputStream(file);
			if(s!=null)
				{BufferedReader ps = new BufferedReader(new InputStreamReader(s));
				net = loadNetwork(ps);
				net.setName(file.getName());
				s.close();
				}
		} catch (IOException e) {
			G.Error("Load network error %s",e);
		}
		
		return(net);
	}
	
	public static void save(IoAble target,String file,String comment) 
	{
		OutputStream s=null;
		File f = new File(file);
		try {
			f.getParentFile().mkdirs();
			s = new FileOutputStream(f);
			if(s!=null)
				{PrintStream ps = new PrintStream(s);
				saveNetwork(target,ps,comment);
				ps.close();
				s.close();
				}
		} catch (IOException e) {
			G.Error("Save network error %s",e);
		}
	}
	
	@SuppressWarnings("deprecation")
	public static void saveNetwork(IoAble target,PrintStream out,String comment)
	{	String name = target.getClass().getName().toString();
		out.println(IdString+" "+Schema);
		out.println(BeginString+" "+name);
		out.println(CommentString+" "+G.quote(comment));
		target.save(out);
		out.println(EndString+" "+name);
	}
	
	@SuppressWarnings("unused")
	public static IoAble loadNetwork(BufferedReader s)
	{	
		StreamTokenizer tok = new StreamTokenizer(s);
		
		if(tok.nextElement().equals(IdString)
				&& tok.nextElement().equals(Schema)
				&& tok.nextElement().equals(BeginString))
		{
			String className = tok.nextElement();
			if(CommentString.equals(tok.nextElement()))
			{
				String comment = tok.nextElement();

				IoAble target = (IoAble)G.MakeInstance(className);
			
				target.load(tok);
				if((tok.nextElement().equals(EndString)
						&& tok.nextElement().equals(className)))
					{
					return target;
					}
			}
			G.Error("loadNetwork finished abnormally");
			return null;
		}
		G.Error("loadNetwork failed at the start");
		return null;
	}

}
