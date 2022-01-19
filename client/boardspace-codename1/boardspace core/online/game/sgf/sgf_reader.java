package online.game.sgf;

import bridge.BufferedReader;
import bridge.File;
import bridge.FileInputStream;
import bridge.FileOutputStream;
import bridge.Frame;
import bridge.URL;
import bridge.Utf8Printer;
import bridge.FileDialog;
import net.sf.jazzlib.ZipEntry;
import net.sf.jazzlib.ZipInputStream;
import online.game.sgf.export.sgf_names;

import com.codename1.util.regex.StringReader;


import java.io.*;
import java.util.Hashtable;

import lib.*;

/** this class contains methods to read sgf format files.  It's completely
generic (knows nothing about any particular game), so the result will have
to be augmented by game-specific knowledge.  But it is extremely useful to
be able to read and write the file into a manipulable internal format
<P>
The result of calling sgf_reader is an array of sgf_game object, each sgf_game
is a tree of nodes, each node is a list of properties.  This directly mirrors
the format of sgf files.
*/
public class sgf_reader implements sgf_names
{
    Reader stream;
    PrintStream errors = null;
    String zipname = null;
    String filename = null;
    String literalString = null;
    URL url;
    URL zipUrl;
    int line_number = 1;
    int game_seq = 1;
    String saved_token = null;
 
    sgf_reader(String file, String zip,
        PrintStream errs)
    {
        zipname = zip;
        SetFileName(file);
        this.url = null;

        if (errors != null)
        {
            errors = errs;
        }
    }

    sgf_reader(String file, PrintStream errs)
    {
        SetFileName(file);
        url = null;
        zipUrl = null;
        if (errs != null)
        {
            errors = errs;
        }
    }

    sgf_reader(URL surl, String zip, PrintStream errs)
    {
        zipUrl = surl;
        url = null;
        zipname = zip;
        filename = surl.getFile();
        literalString = null;

        if (errs != null)
        {
            errors = errs;
        }
    }

    sgf_reader(File file, PrintStream errs)
    {
        this(file.getAbsolutePath(), errs);
    }

    sgf_reader(URL surl, PrintStream errs)
    {	
        this.url = surl;
        this.zipUrl = null;
        this.filename = surl.getFile();
        this.literalString = null;

        if (errs != null)
        {
            this.errors = errs;
        }
    }
    public sgf_reader(InputStream str,PrintStream errs)
    {	stream = new Utf8Reader(str);
    }
    /* public interfaces */

    /** parse a file, print errors to printstream (null means System.out) and
    return an array of games read.*/
    public static sgf_game[] parse_sgf_file(File file,
        PrintStream errors)
    {
        sgf_reader reader = new sgf_reader(file, errors);

        return (reader.sgf_parse());
    }

    public static sgf_game[] parse_sgf_file(String zipname, String filename, PrintStream errors)
    {
        sgf_reader reader = new sgf_reader(filename, zipname, errors);

        return (reader.sgf_parse());
    }

    /** parse a file, print errors to printstream (null means System.out) and
            return an array of games read.*/
    public static sgf_game[] parse_sgf_file(String filename, PrintStream errors)
    {
        sgf_reader reader = new sgf_reader(filename, errors);

        return (reader.sgf_parse());
    }

    /** parse a file specified by a URL, print errors to printstream
    (null means System.out) and return an array of games read. */
    public static sgf_game[] parse_sgf_file(URL url,
        PrintStream errors)
    {	Plog.log.appendNewLog("new sgf reader ");
    	Plog.log.appendLog(url);
    	
        sgf_reader reader = new sgf_reader(url, errors);
        sgf_game res[] = reader.sgf_parse();
        return (res);

    }
    // input for reading from a zip file we already have open
    public static sgf_game[] parse_sgf_file(String z,String fil,InputStream s,PrintStream errs)
    {	
    	sgf_reader reader = new sgf_reader(s,errs);
    	reader.zipname = z;
    	reader.filename = fil;
    	sgf_game g = reader.parse_sgf_game();
    	return(g==null ? null : new sgf_game[]{g});
    }
    static URL zipNameCache = null;
    static Hashtable<String,sgf_game> zipGameCache = null;
    static int BatchSize = 100;
    private static sgf_game[] gameFromCache(String filename)
    {
    	sgf_game game = zipGameCache.get(filename);
    	if(game!=null)
    	{
    		sgf_game[] val = new sgf_game[1];
    		val[0]=game;
    		zipGameCache.remove(filename);
    		return(val);
    	}
    	return(null);
    }
    public static sgf_game[] parse_sgf_file(URL zipurl,  String filename,PrintStream errors)
    {
        sgf_reader reader = new sgf_reader(zipurl, filename, errors);
        if(filename==null)
        {
        	return(reader.sgf_parse(9999999));
        }
        else
        {
        if(zipurl == zipNameCache)
        {
        	sgf_game g[] = gameFromCache(filename);
        	if(g!=null) { return(g); }
        }
        
        sgf_game games[] = reader.sgf_parse(BatchSize*10);
        if(games!=null && games.length>0)
        {
        	if(zipGameCache==null) { zipGameCache = new Hashtable<String,sgf_game>(); }
        	if(zipNameCache!=zipurl) 
        		{ zipNameCache = zipurl; 
        		  zipGameCache.clear(); }
        	for(int lim=games.length-1; lim>=0; lim--)
        	{
        		sgf_game g = games[lim];
        		zipGameCache.put(g.source_file,g);
        	}
        	sgf_game g[] = gameFromCache(filename);
        	if(g!=null) { return(g); }
        }
        return (games);
        }
    }

    /** set the file name.  As a special kludge, if the string starts with a left
            parenthesis, treat it as a literal in-line file instead of as a filename,
            to make it possible to embed the entire game as an argument to an applet.
            Due to bugs (or features, if you prefer) in various browser's implementations of
            the <I>APPLET</I> tag, this only works for fairly small and simple strings.
            */
    private void SetFileName(String file)
    {
        if ((file != null) && file.trim().startsWith("("))
        {
            this.literalString = file;
            this.filename = null;
        }
        else
        {
            this.filename = file;
            this.literalString = null;
        }
    }

    /** parse the file the sgf_reader was created to parse */
    public sgf_game[] sgf_parse()
    {
    	return(sgf_parse(1));
    }
    private sgf_game[] sgf_parse_internal() throws IOException
    {	InputStream s =null;
    	if(url!=null) { s = url.openStream(); }
        if(s==null) { s = new FileInputStream(filename); }

        if (s != null) //if the stream is supposed to be a zip, 
        { //make a reader from the raw string
            stream = new Utf8Reader(s);
        }
        return(sgf_parse_internal_stream());
    }
    public sgf_game[] sgf_parse(int n)
   {
    	InputStream s =null;
    	sgf_game rval[] = null;
        try
        {
            try
            {
                if (literalString != null)
                { //input from literal strings, doesn't support zip
                    stream = new BufferedReader(new StringReader(literalString));
                }
                else if(zipUrl!=null)
                {	// supposed to be a zip
                	sgf_gamestack games = new sgf_gamestack();
                	s = zipUrl.openStream();
                	if(s!=null)
                	{
                    ZipInputStream zip = new ZipInputStream(s);
                    ZipEntry z;
                    String name;
                    boolean takeAll = false;
                	while (((z = zip.getNextEntry()) != null)
                			&& ((name = z.getName()) != null)
                			&& (n>0)
                			)
                    {	if(name.endsWith("/")) {}
                    	else if(zipname==null)
                    		{
                    		sgf_game newgame = new sgf_game(zipUrl,name);
                    		games.push(newgame);
                    		n--;
                    		}
                    	else if (name.equals(zipname) || takeAll)
                        {	
                        	sgf_game newgame = null;
                        	MarkableInputStream mzip = new MarkableInputStream(zip);
                        	mzip.mark(10000);
                        	try {
                        		stream = new Utf8Reader(mzip);
                        		newgame = parse_sgf_game();

                            zipname = filename = name;
                            if(newgame!=null) { games.push(newgame); }
                            }
                            catch (ErrorX e)
                            {	// if we get a parse error in the "all" phase,
                            	// we're reading a file not asked for, so don't
                            	// cause any complaint
                            	if(takeAll) { return(games.toArray());}
                            	throw e;
                            }
                            takeAll = true;
                            if(n>0) { n--;} 	// get the next n too
                        }
                    }}
                	rval = games.toArray();
                }
                else // single file
                {	rval =  sgf_parse_internal();
                }

             }
            finally
            { //close the fake stream first

                try
                {
                    if (stream != null)
                    { //System.out.println("Closing stream "+stream);
                        stream.close();
                        //System.out.println("Stream closed");
                        stream = null;
                    }
                }
                catch (Throwable err)
                {
                }

                //close the real stream
                try
                {
                    if (s != null)
                    { //System.out.println("Closing s "+s);
                        s.close();

                        //System.out.println("S closed");
                    }
                }
                catch (Throwable err)
                {
                }

 
                //	System.out.println("both closed");
            }
        }
        catch (IOException err)
        {
            errors.println("File " + filename +
                ((zipname == null) ? "" : (":" + zipname)) + err.toString());
        }

        return (rval);
    }

    /** parse some stream */
    public sgf_game[] parse_sgf_stream(BufferedReader bstream, PrintStream errs)
    {
        stream = bstream;

        if (errs != null)
        {
            this.errors = errs;
        }

        return (sgf_parse_internal_stream());
    }

    private sgf_game[] sgf_parse_internal_stream()
    {
        int nitems = 0;
        sgf_gamestack s = new sgf_gamestack();
        Plog.log.appendNewLog("Parse ");
        Plog.log.appendLog(filename);
        Plog.log.finishEvent();
        sgf_game newgame = null;
        
        while ((newgame = parse_sgf_game()) != null)
        {
            s.push(newgame);

            //errors.println("game: " + newgame);
            nitems++;
        }
    
        {
            sgf_game[] games = new sgf_game[nitems];

            while (--nitems >= 0)
            {
                games[nitems] = s.pop();
            }

            //errors.println("Returning " + games);
            return (games);
        }
    }

    /** parse one game, as delimited by matching parentheses */
    private sgf_game parse_sgf_game()
    {
        String token = parse_token();

        if (token == null || (token.length()==0))
        {
            return (null);
        }
        else if (token.equals("("))
        {
            sgf_node root = parse_sgf_sequence();
            token = parse_token();

           //errors.println("eog token is " + token);
            if ((token != null) && token.equals(")"))
            {
                sgf_game game = new sgf_game(root);
                game.source_file = (zipname == null) ? filename : zipname;
                game.sequence = game_seq++;
                if(root.get_property(gamename_property)==null)
                	{root.set_property(gamename_property,game.source_file);
                	}
                return (game);
            }

            token_error("parsing game", "trailing \"\"", token);

            return (null);
        }

        token_error("parse_game", "initial \"(\"", token);

        return (null);
    }

    /** parse a sequence of nodes, as delimited by a semicolon */
    /** parse a sequence of nodes, as delimited by a semicolon */
    private sgf_node parse_sgf_sequence()
    {
        String token = null;
        sgf_node first_node = null;
        sgf_node last_node = null;

        while (((token = parse_token()) != null) &&
                (token.equals(";") || token.equals("(")))
        {
            if (token.equals(";"))
            {
                sgf_node latest_node = parse_node();

                if (first_node == null)
                {
                    first_node = latest_node;
                }
                else
                {
                    last_node.addElement(latest_node);
                }

                last_node = latest_node;

                //errors.print("Node:  " ); if(latest_node!=null) latest_node.sgf_print(errors);errors.println();
            }
            else if (token.equals("("))
            {
                parse_sgf_variation(last_node);
            }
        }

        reread_token(token);

        return (first_node);
    }

    private void parse_sgf_variation(sgf_node root)
    {
        String token = null;
        sgf_node last_node = root;
        int count = 0;

        while (((token = parse_token()) != null) &&
                ((token.equals(";")) || (token.equals("(")) ||
                (token.equals(")"))))
        {
            if (token.equals(";"))
            {
                sgf_node latest_node = parse_node();
                last_node.addElement(latest_node);
                last_node = latest_node;

                //errors.print("Node:  " ); if(latest_node!=null) latest_node.sgf_print(errors);errors.println();
            }

            if (token.equals(")"))
            {
                count--;

                if (count < 0)
                {
                    return;
                }

                last_node = root;
            }

            if (token.equals("("))
            {
                count++;
                parse_sgf_variation(last_node);
                count--;
            }
        }
    }

    /** parse one node, delimeted by matching parentheses */
    private sgf_node parse_node()
    {
        sgf_property first_property = null;
        sgf_property last_property = null;
        String token = null;
        String token_name = null;

        while ((!is_singleton(token = parse_token())) ||
                ((token_name != null) && ("[".equals(token))))
        {
            if ("[".equals(token))
            { //read more of the same token, multiple sets of brackets
                reread_token(token);
            }
            else
            { //we read a token name
                token_name = token;
            }
            String value = parse_bracketed_string();
            //System.out.println("token :" + token_name+":"+value);
            sgf_property prop = new sgf_property(token_name, value);

            if (first_property == null)
            {
                first_property = prop;
            }
            else
            {
                last_property.next = prop;
            }

            last_property = prop;

            //System.out.println("Volgende token");
        }

        reread_token(token);

        {
            sgf_node v = new sgf_node(first_property);

            //v.sgf_print(errors);
            return (v);
        }
    }

    /** singletons are characters which constitute a complete token, for example [ and ]
    */
    private boolean is_singleton(String str)
    { //System.out.println("in singelton" + str);

        return ((str != null) && (str.length() == 1) &&
        (is_singleton_char(str.charAt(0))));
    }

    private boolean is_singleton_char(char ch)
    { //System.out.println("is singelton_char" + (new Character(ch)).toString());

        switch (ch)
        {
        case '(':
        case ')':
        case '[':
        case ']':
        case ';':
            return (true);
		default:
			break;
        }

        return (false);
    }

    private char readch() throws IOException
    {
        int c = stream.read();
        if (c == -1)
        {
            throw new IOException();
        }

        if (c == '\n')
        {
            line_number++;
        }

        return ((char)c);
    }
	/**
     * @param ss
     * @return true if character is whitespace, including carraige return
	 */
    private boolean isWhitespace(char ch)
	{
		return(" \t\r\n".indexOf(ch)>=0);
	}
    /** parse a token from the input stream */
    private String parse_token()
    {
        if (saved_token == null)
        {
            StringBuffer s = new StringBuffer();
            int nchars = 0;
            boolean done = false;

            try
            {
                do
                {
                    char ch = readch();
                    if (isWhitespace(ch) || (ch == '\r'))
                    {
                        /* this should be included in isspace, but isn't in microsoft java IE 4.5 */
                        if (nchars > 0)
                        {
                            done = true;
                        }
                    }
                    else if (is_singleton_char(ch))
                    {
                        //System.out.println("reread" + (new Character(ch).toString()));
                        reread_token(String.valueOf(ch));
                        done = true;
                    }
                    else
                    {
                        s.append(ch);
                        nchars++;

                        //System.out.println("Hier kom ik niet"); 
                    }
                }
                while (!done);
            }
            catch (IOException err)
            {
            }

            if (nchars > 0)
            {
                String val = s.toString();

                //errors.println("Token: " + val);
                return (val);
            }
        }

        {
            String v = saved_token;

            //System.out.println("saved token: " + v);
            //errors.println("Token: " + v);
            saved_token = null;

            return (v);
        }
    }

    /** read everything inside a pair of brackets, respecting brackets escaped by \\
    */
    private String parse_bracketed_string()
    {
        //System.out.println("parse bracketed start");
        String token = parse_token();

        //System.out.println("parse bracketed token" + token);
        if ((token != null) && token.equals("["))
        {
            boolean quote = false;
            StringBuffer s = new StringBuffer();

            try
            {
                boolean done = false;

                do
                {
                    char ch = readch();
                     if (ch == '\\')
                    {
                        quote = true;
                        ch = readch();
                     }

                    {
                        /* cr+lf logic */
                        if (ch == '\r')
                        {
                            ch = readch();

                            if (ch == '\n')
                            {
                                 //cr + lf, emit lf only
                            }
                            else
                            {
                                
                                s.append('\n');
                            }
                        }

                        if (!quote && (ch == ']'))
                        {
                            done = true;
                        }
                        else
                        {
                            
                            s.append(ch);
                        }

                        quote = false;
                    }
                }
                while (!done);

                //System.out.println("Between brackets : " + (new String(s)));
                String str = s.toString();
                return (str);
            }
            catch (IOException err)
            {
            }
        }
        else
        {
            token_error("parse_bracketed_string", "[", token);
        }

        return (null);
    }

    private void reread_token(String str)
    {
        saved_token = str;
    }
    private String explode(String s)
    {	if(s!=null)
    	{
    	StringBuilder b = new StringBuilder();
    	b.append("(");
    	for(int i=0;i<s.length();i++) { int ii = s.charAt(i); b.append(ii); } 
    	b.append(" )");
    	return(b.toString());
    	}
    	else { return(s); }
    }
    /** print a helpful complaint about the format of the input file */
    private void token_error(String where, String expected, String got)
    {
        G.Error("Syntax error from " + where + " in " + filename +
            ((zipname == null) ? "" : (":" + zipname)) + " at line " +
            line_number + " expected " + expected + " but found " + got+" "+explode(got));
    }

    /** print an array of sgf_games as a readable sgf text file
    to a supplied stream */
    public static boolean sgf_save(PrintStream out, sgf_game... games)
    {
        return (sgf_save(out, games, false));
    }

    public static boolean sgf_save(PrintStream out,  sgf_game[] games, boolean strip)
    {
        try
        {
            for (int i = 0; i < games.length; i++)
            {
                games[i].sgf_print(out, strip);
            }
        }
        catch (IOException e)
        {
        }

        return (true);
    }

    /** print an array of sgf_games as a readable sgf text file */
    public static boolean sgf_save(String file,   sgf_game... games)
    {
        return (sgf_save(new File(file), games, false));
    }

    public static boolean sgf_save(String file,  sgf_game[] games, boolean strip)
    {
        return (sgf_save(new File(file), games, strip));
    }

    public static boolean sgf_save(File file, sgf_game... games)
    {
        return (sgf_save(file, games, false));
    }

    public static boolean sgf_save(URL file, sgf_game...games)
    {
    	return sgf_save(file,games,false);
    }
    public static boolean sgf_save(URL file, sgf_game[]games,boolean strip)
    {
    	if("file".equals(file.getProtocol()))
    	{
    		return(sgf_save(new File(file.getFile()),games,strip));
    	}
    	return(false);
    }
    public static boolean sgf_save(File file,  sgf_game[] games, boolean strip)
    {
        try
        {
            OutputStream fs = null;
            PrintStream out = null;
            try
            {
                fs = new FileOutputStream(file);
                out = Utf8Printer.getPrinter(fs);
                sgf_save(out,games,strip);
                return (true);
            }
            finally
            {
                if (out != null)
                {
                    out.flush();
                }
                if(fs!=null) { fs.close(); }
            }
        }
        catch (IOException err)
        {
            G.print("Can't open output " + file + " " +
                err.toString());
        }

        return (true);
    }


    static public FileDialog do_sgf_dialog_only(int key, String baseDir,String filter)
    {
    	// launch the file dialog
        FileDialog fd = new FileDialog((Frame)null,"Select a sgf format file",key);
        fd.setDirectory(baseDir);
        fd.setFile(filter);
        fd.setVisible(true);

        // get the selected filename, if any
        String directory = fd.getDirectory();

        if (directory == null)
        {
            return (null); //probably means aborted dialog
        }

        return (fd);
    }

    static public String do_sgf_dialog(int key, String baseDir,String filter)
    {
        FileDialog fd = do_sgf_dialog_only(key,baseDir, filter);

        if (fd != null)
        {
            String filename = fd.getFile();
            String directory = fd.getDirectory();
            String file_separ = G.getFileSeparator();

            if (filename.endsWith(".*.*"))
            {
                filename = filename.substring(0, filename.length() - 4);
            }

            if ((file_separ != null) && !directory.endsWith(file_separ))
            { // then add it
                directory += file_separ;
            }

            return (directory + filename);
        }

        return (null);
    }
}
