/*
*
* reader
*
*/
package goban.shape.shape.lisp; 

import java.io.*;
import java.net.URL;
import java.util.*;

import goban.shape.shape.*;
import lib.G;

/** this reader reads the go shape database in it's original "Lispm" form
and produces the java compatible internal form based on the
ShapeLibrary class */
public class reader {
	
	Reader stream;
	int line_number=1;
	String saved_token=null;
	static Vector<Dropped_Shape> All_Dropped_Shapes=new Vector<Dropped_Shape>();

	private char readch() throws IOException
	{
		int c = stream.read();
		if(c==-1) { throw new IOException();}
		if(c=='\n')line_number++;
		return((char)c);
	}

	private boolean is_singleton_char(char ch)
	{	switch(ch)
		{ case '(': case ')': case '|': case '\"': case '#':
			return(true);
		default: break;
		}
		return(false);
	}
	private void reread_token(String str) { saved_token = str; }
	
	private String parse_token()
	{	if(saved_token==null)
		{
			StringBuffer s = new StringBuffer();
			int nchars = 0;
			boolean done=false;
			
			try
			{do 
				{ char ch = readch();
					if (Character.isWhitespace(ch)) 
					{
						if(nchars>0) done=true; 
					}
					else if (is_singleton_char(ch))
					{ reread_token(""+ch);
						done=true;
					}
					else { s.append(ch); nchars++; }
				} while(!done);
			} 
			catch (IOException err) {};
			
			if(nchars>0) 
			{ String val = new String(s); 
				//errors.println("Token: " + val);
				return(val);
			}
		}
		
		{String v =saved_token;
			//errors.println("Token: " + v);
			saved_token=null;
			return(v);
		}}
	
	/* constructor */
	reader(Reader r) 
	{ stream=r;
	}
	
	
	Vector<Object> Make_Vector(LList l2)
	{ Vector<Object> v = new Vector<Object>();
		while(l2!=null)
        { v.addElement(l2.Contents());
			l2 = l2.Next();
        }
		return(v);
	}
	
	Object readlist()
	{	LList listsofar=null;
		LList listhead =null;
		Object token=null;
		do {
			token = readtoken();
			boolean isstring = (token instanceof String);
			
			if(isstring && token.equals("."))
			{ /* make dotted lists into normal lists */
				token = readtoken();
				isstring = (token instanceof String);
			}
			
			if(isstring && token.equals(")"))
			{ return(listhead);
			}else 
			if(token!=null)
			{
				LList more = new LList(token,null);
				if(listsofar==null)
				{ listsofar = more;
					listhead = more;
				}else
				{ listsofar.Set_Next(more);
					listsofar = more;
				}
				
			};
		} while(token!=null);
		return(listhead);
	}
	
	String parse_quoted_string(char terminal)
	{ 
		boolean quote=false;
		StringBuffer s = new StringBuffer();
		try
		{boolean done=false;
			do {char ch = readch();
				if(ch=='\\') { quote=true; } 
				else
				{
					/* cr+lf logic */
					if(ch=='\r')
					{ch=readch();
						if(ch=='\n') 
						{//cr + lf, emit lf only
						}else
						{s.append('\n');
						}
					}			
					if(!quote && ch==terminal)
					{ done=true; 
					} else 
					{
						
						s.append(ch);
					}
					quote=false;
				}} while (!done);
			return(new String(s));
		}
		catch (IOException err) {};
		return(null);
	} 
	
	Object readtoken()
	{ String val= parse_token();
		Object rval = val;
		if(val==null) {}
		else if(val.equals("\""))
		{ rval=parse_quoted_string('\"');
			
		}
		else if (val.equals("|"))
		{ rval=parse_quoted_string('|');		
		}
		else if (val.equals("("))
		{
			rval=readlist();
		}
		else if (val.equals("#"))
		{rval=Make_Vector((LList)readtoken());
		}

		//	System.out.println("Line " + line_number + " token " + rval);
		return(rval);
	}
	/** called from OneShape constructor. */
	static public void Make_Dropped_Shapes(ShapeProtocol par,LList c)
	{	int len = LList.LList_Length(c);
		for(int i=0;i<len;i++) 
		{ LList dshape = (LList)c.Contents();
			c=c.Next();
			int rv=Integer.parseInt((String)(dshape.Contents()));
			dshape=dshape.Next();
			int rp=Integer.parseInt((String)(dshape.Contents()));
			dshape=dshape.Next();
			{OneShape dropped_shape = Make_Lisp_DB_Shape(new LList("MODIFY-SHAPE",(LList)dshape.Contents()));
				/* important point here, the introns for the dropped shapes in this 
				database are stored with respect to the parent shape, not the actual
				shape.  So to make an viable, independant shape, we have to drop
				that bit out of the results */
				//System.out.println("Cleanup for " + name);
				for(int idx=0;idx<dropped_shape.results.length;idx++)
				{ dropped_shape.results[idx].RemoveBit(rv);
				}
				All_Dropped_Shapes.addElement(new Dropped_Shape(
					dropped_shape.points,dropped_shape.results,rv,rp,par));
				
				//System.out.println(" + " + len + " dropped shapes" + s);
			}}
	}
	

	static public void Add_Dropped_Shapes(ShapeLibrary lib)
	{
		/* build the dropped-shape elements cautiously, because they're not all
		in one place, initially.  for example the "knights move" shape arises
		in two ways, derived from bent for and also from crooked four"
		*/  
		G.print("Adding " + All_Dropped_Shapes.size() + " dropped shapes");  
		for(Enumeration<Dropped_Shape> e = All_Dropped_Shapes.elements();
			e.hasMoreElements(); )
		{Dropped_Shape s=(Dropped_Shape)e.nextElement();
	   	 lib.BuildOneShape(s);
		}
	}
	/** call readstream to read from an opened stream of some sort */
	public static Vector<OneShape> ReadStream(InputStream is)
	{ Vector<OneShape> result=null;
		reader s = new reader(new InputStreamReader(is));
	    Object v = null;
	    try {
	    do { v = s.readtoken();
			if(result==null) { result=new Vector<OneShape>(); }
			if(v!=null)
			{OneShape sh = Make_Lisp_DB_Shape((LList)v);
				System.out.println("read: " + sh);
				result.addElement(sh); 
			}
		} while (v!=null);
	    }
	    catch(NullPointerException e)
	    {
	    	G.print("Null pointer from "+v.getClass().getName()+" "+v);
	    }
	    try {
	    is.close();
	    } catch (IOException e) {}
		return(result);
		
	}
	
	static Vector<OneShape> ReadURL(URL f,PrintStream w)
	{	Vector<OneShape> result=null;
		try 
		{result=ReadStream(f.openStream());
		}
		catch (IOException e)
		{ w.println("IO exception for " + f + " " + e); 
		}
		return(result);
	}
	
	/** call ReadFile to absorb the file and return a vector of OneShape objects */
	static Vector<OneShape> ReadFile(String f)
	{Vector<OneShape> result=null;
		try 
		{
			result=ReadStream(new FileInputStream(f));
			
		}
		catch(IOException e) 
		{ G.print("IO Exception " + f + e); 
		}
		return(result);
	}
	
	/* constructor for URLs */
	static public ShapeLibrary Read_ShapeLibrary(URL filename,PrintStream console)
	{	 return(ProcessShapeData(null,ReadURL(filename,console)));	
	}
	/* constructor for files */
	static public ShapeLibrary Read_ShapeLibrary(String outfile,String filename,PrintStream console)
	{	 return(ProcessShapeData(outfile,ReadFile(filename)));
	}
	static ShapeLibrary ProcessShapeData(String outfile,Vector<OneShape> shapedata)
	{
		if(shapedata!=null) 
		{ ShapeLibrary lib = new ShapeLibrary(shapedata);
			Add_Dropped_Shapes(lib);
			lib.Build_Vectordata();
			if(outfile!=null) { lib.Save_ShapeLibrary(outfile,"shapelib.job"); }
			return(lib);
		}
		return(null);
	}

	static private SingleResult[] getMultiResults(LList x)
	{
		
		int len = LList.LList_Length(x);
		SingleResult []value=new SingleResult[len];
		for(int i=0;i<len;i++)
		{
			value[i] = SingleResult.Make_SingleResult((String)(x.Contents()));
			x=x.Next();
		}
		return(value);
	}

	static private ResultProtocol[] getResultsFromVector(Vector<Object>rspec)
	{	int size = rspec.size();
		ResultProtocol results[]=new ResultProtocol[size];
		for(int i=0;i<size;i++)
		{ Object o = rspec.elementAt(i); 
			if(o instanceof String) { results[i]=SingleResult.Make_SingleResult((String)o); }
			else if(o instanceof LList) {results[i]=MultiResult.Make_MultiResult(getMultiResults((LList)o)); }
			else {G.Assert(false,"Result " + o + " unexpected type");}
		}
		return(results);
	}
	@SuppressWarnings("unchecked")
	public static OneShape Make_Lisp_DB_Shape(LList rspec)
	{
		/* this ugliness creates a OneShape structure from the linked list/vector
		structure derived from the old lisp database 
		*/
		OneShape sh = new OneShape();	
		while(rspec!=null)
		{ String key = (String)rspec.Contents();
			rspec=rspec.Next();
			Object contents = rspec.Contents();
			rspec = rspec.Next();
			if(key.equals("MODIFY-SHAPE"))
			{ sh.picture = (String)contents;
				sh.points = zhash.Make_Point_Array(sh.picture);
				//System.out.println(picture + " points " + points + " code " + hashcode);
			}
			else if(key.equals(":NAME"))
			{ sh.name = (String)(contents);
			}
			else if(key.equals(":KEY-POINTS"))
	    	{	LList kpList = (LList)contents;
	    		String str = kpList.getString();
	    		//System.out.println("A "+str);
	    		sh.key_points = str;
	    	}
			else if(key.equals(":FATE-COMMENT"))
	    	{sh.fate_comment = (String)contents;
	    	}
			else if(key.equals(":DROPPED-SHAPES"))
	    	{ LList c = (LList)contents;
			  Make_Dropped_Shapes(sh,c);	//for side effect only, dropped shapes are remembered elsewhere
	    	}
			else if(key.equals(":FATE"))
			{ LList res = (LList)contents;
				int len = LList.LList_Length(res);
				ResultCode r[] = new ResultCode[len];	 
				sh.results=r;
				for(int i=0; i<len; i++) 
				{ LList oneres = (LList)res.Contents();			  
					res=res.Next();
					int poscode = Integer.parseInt((String)(oneres.Contents()));
					oneres = oneres.Next();
					ResultProtocol []results = getResultsFromVector((Vector<Object>)oneres.Contents());
					r[i] = new ResultCode(poscode,results);
				}
			}
			else
			{/* unknown key */
				System.out.println("Key " + key + " not handled in shape "+sh);
			}
			
		}
		return(sh);
	}
	public static void xmain(String args[])
	{
		if(args.length>=1) 
		{Vector<OneShape> v = ReadFile(args[0]);
			System.out.println("Read " + v);
		}
	}
	
}
