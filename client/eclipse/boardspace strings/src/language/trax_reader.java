package language;
/* import a trax format file as an sgf file.  Derived form the pbem importer for LOA
 */

import online.game.sgf.*;
import online.game.sgf.export.sgf_names;

import java.io.*;
import java.net.*;
import java.util.*;
import trax.TraxConstants;

public class trax_reader implements TraxConstants,sgf_names
{   BufferedReader stream;
    PrintStream console = System.out;
    URL url;
    String filename;
    String current_line;
    private static final int start = 0;
    private static final int running = 1;
    private static final int finished = 2;
    boolean rereadline=false;
    int line_index;
    int state = start;
    int current_move_number=1;
    String title=null;
    String setup=null;
    sgf_node current_node;
    sgf_node initial_node;
		boolean valid_game_parsed=false;

	  void reinit()
	  { state=start;
	    current_move_number=1;
	    setup=null;
	    title=null;
	    current_node=null;
	    initial_node=null;
	  	valid_game_parsed=false;
	  }
    /** constructor, given a file name and a stream to print error messages.  null
    is acceptable for the error stream */
    trax_reader(File fname, PrintStream errors)
        {this.filename = fname.getAbsolutePath();
         this.url = null;
         if(errors!=null) { this.console = errors; }
        }
    /** constructor, given a file name and a stream to print error messages.  null
    is acceptable for the error stream */
    trax_reader(String fname, PrintStream errors)
        {this.filename = fname;
         this.url = null;
         if(errors!=null) { this.console = errors; }
        }
    trax_reader(URL furl, PrintStream errors)
        {this.url = furl;
         this.filename = furl.getFile();
        if(errors!=null) { this.console = errors; }
        }

    /** parse a file, print errors to printstream (null means System.out) and
    return an array of games read. */
    public static sgf_game[] parse_sgf_file(File filename,PrintStream errors)
        { trax_reader reader = new trax_reader(filename,errors);
        return(reader.sgf_parse());
        }
    /** parse a file, print errors to printstream (null means System.out) and
    return an array of games read. */
    public static sgf_game[] parse_sgf_file(String filename,PrintStream errors)
        { trax_reader reader = new trax_reader(filename,errors);
        return(reader.sgf_parse());
        }
    /** parse a file specified by a URL, print errors to printstream
    (null means System.out) and return an array of games read. */
    public static sgf_game[] parse_sgf_file(URL url,PrintStream errors)
    {
    	trax_reader reader = new trax_reader(url,errors);
        return(reader.sgf_parse());
    }
    /** parse the file the sgf_reader was created to parse */
    public sgf_game[] sgf_parse()
        {
        try
        {try
            {
            stream = (url!=null)
                                ? new BufferedReader(new InputStreamReader(url.openStream()))
                                : new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
            return(sgf_parse_internal());
            }
        finally { if(stream!=null) {stream.close(); }}
        }
        catch (IOException err)
            {
         console.println("File " + filename + err.toString());
            }
        return(null);
        }
sgf_game[] sgf_parse_internal()
{   Vector<sgf_game> results=new Vector<sgf_game>();
    sgf_game glist[]=null;
    sgf_game g = null;
    int size = 0;
    do {
     g = parse_game();
    if(g!=null) { results.addElement(g); size++;}
    } while (g!=null);
    if(size>0)
    { glist=new sgf_game[size];
      for(int i=0;i<size;i++) { glist[i]=results.elementAt(i); }
    }else
    {  console.println("No trax game found in " + filename);
		}
    return(glist);
}
sgf_game parse_game()
{	reinit();
  while(parse_line()==false && !(state==finished) )
      {if(current_line==null) state=finished; };
    if(initial_node!=null || valid_game_parsed)
        { 
            sgf_property root_properties= new sgf_property(game_property,""+Trax_SGF);
            root_properties = new sgf_property(fileformat_property,sgf_file_format,root_properties);
            if(title!=null)
                {
                root_properties = new sgf_property(setup_property,setup,root_properties);
            	root_properties = new sgf_property(gamename_property,title,root_properties);
                root_properties = new sgf_property(gametitle_property,title,root_properties);
                }
             else { console.println("No game name found in " + filename);}
            sgf_node root_node = new sgf_node(root_properties);
            sgf_node start_node = new sgf_node(new sgf_property("P0","start p0"));
            start_node.addElement(initial_node);
			root_node.addElement(start_node);
			sgf_game game = new sgf_game(root_node);
            game.source_file = filename;
            return(game);
     }
        return(null);
}

boolean parse_line()
{ boolean done=false;
  line_index = 0;

    try{    if(rereadline) { rereadline=false; } else { current_line = stream.readLine(); }
            String token = next_line_token();
            if(token!=null) token=token.toLowerCase();

            if((token==null)|| (token.length()==0)) {}
            else if(token.charAt(0) == '#') 
            	{ if(state==running) { rereadline=true; state=finished; }
            	  else { parse_title_line(); } 
            	}
            else if(state==running) { line_index=0; parse_game_line(); }
    }
    catch(IOException err) { done=true; }
    return(done);
}

void parse_move_spec(String moven, String spec)
{
    if(moven!=null && !moven.toLowerCase().equals("(auto)"))
    { int m2 = Integer.parseInt(moven);
        if(m2==current_move_number)
                { current_move_number++;
                  add_move_node(spec);
                }
                else
                {   console.println("At line: " + current_line);
                    console.println("Move number is " + m2 + " but expected " + current_move_number);
                }
    }
}
String split_spec(String spec)
{	String ostring = "move ";
	int len = spec.length();
	if("resign".equals(spec.toLowerCase())) { return("resign"); }
	if(len>4) { console.println("At line: "+current_line);
				console.println("strange move spec: "+spec);
				}
	boolean prevdigit = false;
	for(int i=0;i<len;i++) 
	{	char ch = spec.charAt(i);
		boolean dig = Character.isDigit(ch);
		if(dig |= prevdigit) { ostring = ostring + " "; }
		prevdigit = dig;
		ostring = ostring + ch;
	}
	return(ostring);
}
void add_move_node(String spec)
{   String color = ((current_move_number&1)==0) /* even numbered moves are black */
                                 ? "P0" : "P1";
    sgf_property p = new sgf_property( color, split_spec(spec));
    sgf_node n = new sgf_node(p);
    if(current_node==null) { initial_node = n; }
    else { current_node.addElement(n); }
    current_node = n;
}
void parse_game_line()
{  	String tok = null;
	try
	{
	line_index = 0;	// start from the beginning of the line
	do 
	{ tok = next_line_token();
	if((tok!=null) && (tok.length()>0))
	{	char ch = tok.charAt(0);
		if(Character.isDigit(ch))
		{	// new move number
			parse_move_spec(tok,next_line_token());
		}
		else 
			{ String comment = current_line.substring((ch==';') ? line_index : 0);
			  String current = current_node.get_property(comment_property);
			  current = (current==null) ? comment : (current + "\n"+comment);
			  current_node.set_property(comment_property,current);
			  line_index = current_line.length();
			}
	}
	} while(tok!=null);
	}
    catch(Throwable err)
    { console.println("Error parsing game line: " + current_line);
      console.println("Error is: " + err.toString());
    }
}

void parse_title_line()  throws IOException
{ String ti = null;
  String tok = null;
  
  do {
	tok=next_line_token();
	if("#".equals(tok)) {}
	else if(":".equals(tok)) { tok=null; }
	else if(tok==null) {}
	else { ti = (ti==null) ? tok : (ti+" "+tok); }
  } while (tok!=null);
  title = ti;
  
  System.out.println("Title: "+title);
  do { current_line = stream.readLine(); } 
  while((current_line==null)||"".equals(current_line));
 
  line_index = 0;
  String su = "";
  while((tok=next_line_token())!=null) { su = su+tok.toLowerCase(); }
  setup = su;
  line_index = 0;
  state = running;
}
boolean IsBreak(char ch)
{	return((ch==';')||(ch==':')||(ch=='#')); 
}
String next_line_token()
{
    String value = null;

    if(current_line!=null)
    {
    int len = current_line.length();
    int first_index=line_index;

    while(line_index<len && Character.isWhitespace(current_line.charAt(line_index)))
     {line_index++;
     }
    first_index=line_index;
    boolean done=false;
    while(line_index<len && !Character.isWhitespace(current_line.charAt(line_index)) && !done)
    {line_index++;
     done = IsBreak(current_line.charAt(line_index-1));
     if(line_index<len) { done |= IsBreak(current_line.charAt(line_index)); }
    }
    if(line_index>first_index)
     {value = current_line.substring(first_index,line_index);
     }
}
 return(value);
     }

    public static void main(String args[])
    { int n=args.length;
      PrintStream console = System.out;
        if(n==0)
        { console.println("java loa.common.trax_reader <inputfile> {outputfile}");
        }
        else
        {
        sgf_game[] g = trax_reader.parse_sgf_file(args[0],console);
        if(g!=null)
        {
         if(n>1) { sgf_reader.sgf_save(args[1],g); }
         else
         {
         console.println(g.length + " Games: ");
       try
        {for(int i=0; i<g.length; i++)
        {   g[i].sgf_print(console);
        }}
        catch (IOException err)
        { console.println("Error writing file: " + err);
        }
      }}
    }
    console.close();
     }

}