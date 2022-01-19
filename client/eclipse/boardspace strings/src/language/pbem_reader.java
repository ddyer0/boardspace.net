package language;

/* import a pbem format file as an sgf file.  This code worked for loadjava, but is untested
 * in the boardspace environment.
 */


import online.game.sgf.*;
import online.game.sgf.export.sgf_names;

import java.io.*;
import java.net.*;
import java.util.*;

import bridge.Utf8Printer;
import loa.UIC;

public class pbem_reader implements UIC,sgf_names
{   BufferedReader stream;
    PrintStream console = System.out;
    URL url;
    String filename;
    String current_line;
    private static final int start = 0;
    private static final int running = 1;
    private static final int finished = 2;
    int line_index;
    int state = start;
    int current_move_number=1;
    String black_player;
    String white_player;
    String title;
    sgf_node current_node;
    sgf_node initial_node;
		boolean valid_game_parsed=false;

	  void reinit()
	  { state=start;
	    current_move_number=1;
	    black_player=null;
	    white_player=null;
	    title=null;
	    current_node=null;
	    initial_node=null;
	  	valid_game_parsed=false;
	  }
    /** constructor, given a file name and a stream to print error messages.  null
    is acceptable for the error stream */
    pbem_reader(File fname, PrintStream errors)
        {this.filename = fname.getAbsolutePath();
         this.url = null;
         if(errors!=null) { this.console = errors; }
        }
    /** constructor, given a file name and a stream to print error messages.  null
    is acceptable for the error stream */
    pbem_reader(String fname, PrintStream errors)
        {this.filename = fname;
         this.url = null;
         if(errors!=null) { this.console = errors; }
        }
    pbem_reader(URL furl, PrintStream errors)
        {this.url = furl;
         this.filename = furl.getFile();
        if(errors!=null) { this.console = errors; }
        }

    /** parse a file, print errors to printstream (null means System.out) and
    return an array of games read. */
    public static sgf_game[] parse_sgf_file(File filename,PrintStream errors)
        { pbem_reader reader = new pbem_reader(filename,errors);
        return(reader.sgf_parse());
        }
    /** parse a file, print errors to printstream (null means System.out) and
    return an array of games read. */
    public static sgf_game[] parse_sgf_file(String filename,PrintStream errors)
        { pbem_reader reader = new pbem_reader(filename,errors);
        return(reader.sgf_parse());
        }
    /** parse a file specified by a URL, print errors to printstream
    (null means System.out) and return an array of games read. */
    public static sgf_game[] parse_sgf_file(URL url,PrintStream errors)
    {
        pbem_reader reader = new pbem_reader(url,errors);
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
    {  console.println("No pbem LoA game found in " + filename);
		}
    return(glist);
}
sgf_game parse_game()
{	reinit();
  while(parse_line()==false && !(state==finished) )
      {if(current_line==null) state=finished; };
    if(initial_node!=null || valid_game_parsed)
        { 
            sgf_property root_properties= new sgf_property(game_property,""+sgf_game_id);
            root_properties = new sgf_property(invert_y_axis,"true",root_properties);
            root_properties = new sgf_property(fileformat_property,sgf_file_format,root_properties);
            if(black_player!=null)
             { root_properties = new sgf_property(player_black,black_player,root_properties); }
             else { console.println("No black player name found in " + filename);}
            if(white_player!=null)
                { root_properties = new sgf_property(player_white,white_player,root_properties); }
             else { console.println("No white player name found in " + filename);}
            if(title!=null)
                { root_properties = new sgf_property(gamename_property,title,root_properties);
                  root_properties = new sgf_property(gametitle_property,title,root_properties);
                }
             else { console.println("No game name found in " + filename);}
            sgf_node root_node = new sgf_node(root_properties);
			root_node.addElement(initial_node);
            sgf_game game = new sgf_game(root_node);
            game.source_file = filename;
            return(game);
     }
    return(null);
}
boolean parse_line()
{ boolean done=false;
  line_index = 0;
    current_line = null;

    try{    current_line = stream.readLine();
            String token = next_line_token();
            if(token!=null) token=token.toLowerCase();

            if(token==null) {}
            else if(token.equals("summary") || token.equals("notification")) { parse_summary_line();    }
            else if(token.equals("ohs")) { parse_title_line(); }
            else if(token.equals("ohs:")) { parse_black();}
            else if(token.equals("eks:")) { parse_white();}
            else if((state==running) && token.equals("a")) { parse_final_line(); }
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
void add_move_node(String spec)
{   String color = ((current_move_number&1)==0) /* even numbered moves are black */
                                 ? "B" : "W";
    sgf_property p = new sgf_property( color, spec);
    sgf_node n = new sgf_node(p);
    if(current_node==null) { initial_node = n; }
    else { current_node.addElement(n); }
    current_node = n;
}
void parse_game_line()
{   String moven1 = next_line_token();
    String move1 = next_line_token();
    String moven2 = next_line_token();
    String move2 = next_line_token();

    try {
        parse_move_spec(moven1,move1);
        parse_move_spec(moven2,move2);
        }
    catch(Throwable err)
    { console.println("Error parsing game line: " + current_line);
      console.println("Error is: " + err.toString());
    }
}
void parse_white()
{
    white_player = next_line_token();
}
void parse_black()
{
    black_player = next_line_token();
}
void parse_title_line()  throws IOException
{/* look for Ohs Eks", next line is players */
 String token=next_line_token();
 if("(o)".equals(token)) { token=next_line_token(); }
  if(token!=null && token.toLowerCase().equals("eks"))
  { current_line = stream.readLine();
    line_index = 0;
    String black = next_line_token();
    String white = next_line_token();
    if(black_player==null) {black_player=black; }
    if(white_player==null) {white_player=white; }
    state = running;
  }
}
void parse_summary_line()
{/* look for "summary of Loa Board nnn", title becomes "Loa Board nnn" */
 String token=next_line_token();
  if(token!=null && token.toLowerCase().equals("of"))
    { title = current_line.substring(line_index);
    }
}
void parse_final_line()
{   /* look for "A B C D E F G H", beginning of the board picture */
    String token=next_line_token();
  if(token!=null && token.toLowerCase().equals("b")) 
  { state=finished; 
  	valid_game_parsed=true;
  }
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

    while(line_index<len && !Character.isWhitespace(current_line.charAt(line_index)))
    {line_index++;
    }
    if(line_index>first_index)
     {value = current_line.substring(first_index,line_index);
     }
}
 return(value);
     }

    public static void main(String args[])
    { int n=args.length;
      PrintStream console = new Utf8Printer(System.out);
        if(n==0)
        { console.println("java loa.common.pbem_reader <inputfile> {outputfile}");
        }
        else
        {
        sgf_game[] g = pbem_reader.parse_sgf_file(args[0],console);
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
}}
