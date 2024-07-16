/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.
    
    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/. 
 */
package lib;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import bridge.JPanel;
import bridge.XTextField;
import bridge.Config;
import bridge.FullscreenPanel;
import bridge.ScrollableList;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Label;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import jzlib.ZipEntry;
import jzlib.ZipInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;


/* below here should be the same for codename1 and standard java */

import java.io.*;
import java.util.*;

class ProxyString {
	String value;
	String displayValue;
	ProxyString(String v)
	{
		value = displayValue = v;
	}
	ProxyString(String v,String dv)
	{
		value = v;
		displayValue = dv;
	}
	public String toString() { return(displayValue); }
}
/**
 * this class serves as a universal file dialog.
 * 
 * @author Ddyer
 *
 */
public class FileSelector extends FullscreenPanel 
	implements Runnable,DeferredEventHandler,WindowListener,Config
{	static final long serialVersionUID = 1L;

	// result codes passed to the observer, normally a game viewer
	public static final String SERVERSAVE = "serversave";
	public static final String SERVERFILE = "serverfile";
	public static final String SELECTEDGAME = "selectedgame";
	public void wake() { G.wake(this); }
	public class FileSelectorResult
	{
		public String opcode;	// load or save
		public URL file;	// file URL
		public URL zip;		// containing zip file, if any
		FileSelectorResult(String o,URL fil,URL zi)
		{	opcode = o;	
			file = fil;
			zip = zi;
		}
	}

	// some display components
    private XTextField moveField; //the activity message, "reading directory .." and so on
    private XTextField dirField; //the current directory message
    private Label dirLabel ;
    private XTextField filterField; //the current filter words
    private ScrollableList<ProxyString> gameList; //the list of individual files
    private String gamefile; //the currently selected file
    private ScrollableList<ProxyString> dirList; //the list of directories
    
    private JPanel sourcePanel = null;		// with multiple sources, this panel is shown
    private Vector<FileSource> sources = new Vector<FileSource>();
    private Label currentRoot;
    
    private JPanel filterPanel;
    private JPanel dirListPanel;
    private JPanel gameListPanel;
    private JPanel dirPanel;
    private JButton cancelButton = null;
    private JButton okButton = null;
    private JButton saveButton = null;
    private JButton mkdirButton = null;
    private JButton deleteButton = null;
    private JPanel exitPanel = null;
    private Vector<JButton>sourceButtons = new Vector<JButton>();
    
    private FileSource currentSource = new FileSource();
    public URL selectedUrl = null;
    
    public DeferredEventManager deferredEvents = new DeferredEventManager(this);
    InternationalStrings s;
    public SimpleObservable observer = new SimpleObservable(this); 	//list of observers
    private boolean inited = false;		//this is used to prevent game updates from firing during initialization
   
 
    //
    // layout with a mostly vertical orientation, with main
    // panes for subdirectories and individual files.
    //
    public void setLocalBounds(int x,int cy,int w,int h)
    {	
    	Dimension dirPref = dirPanel.getPreferredSize();
    	int dirH = G.Height(dirPref);
    	
    	dirLabel.setText(s.get(!currentSource.readOnly ? CurrentFileMessage : DirectoryMessage));
    	saveButton.setVisible(!currentSource.readOnly);
    	
    	Dimension movePref = moveField.getPreferredSize();
    	int moveH = G.Height(movePref);

    	Dimension filterPref = filterPanel.getPreferredSize();
    	int filterH = G.Height(filterPref);
       	boolean multisource = sources.size()>0;
    	int sourceH = 0;
    	int y = cy;
    	boolean seeExit = currentSource.modal || !currentSource.readOnly;

    	cancelButton.setVisible(currentSource.modal);
    	
    	if(multisource)
    	{
    		Dimension sourcePref = sourcePanel.getPreferredSize();
    		sourceH = G.Height(sourcePref);
    		sourcePanel.setFrameBounds(0,y,w,sourceH);
    		y += sourceH;
    	}

    	int exitH = 0;
    	if(seeExit)
    	{
        	Dimension exitPref = exitPanel.getPreferredSize();
        	exitH = G.Height(exitPref);
    	}
 
    	int flexH = h-(filterH+moveH+dirH+sourceH+exitH);
    	
    	int dirLH = flexH/2;
    	int gameH = flexH-dirLH;
   	
    	
     	sourcePanel.setVisible(multisource);


    	dirPanel.setFrameBounds(x,y,w,dirH);
    	y += dirH;   	
  	
    	
    	exitPanel.setVisible(seeExit);
    	
    	if(seeExit)
    	{
        	exitPanel.setFrameBounds(x,y,w,exitH);
        	y+= exitH;
    	}
    	
    	if(G.isCodename1()) { dirList.setBounds(x,y,w,dirLH); }
    	else { dirListPanel.setFrameBounds(x,y,w,dirLH); }
    	y += dirLH;
    	filterPanel.setFrameBounds(x,y,w,filterH);
    	y += filterH;
    	if(G.isCodename1())
    	{gameList.setBounds(x,y,w,gameH);
    	}
    	else 
    	{
    	gameListPanel.setFrameBounds(x,y,w,gameH);
    	}
    	y += gameH;
    	
     	moveField.setBounds(x,y,w,moveH);
    }
    
    // all possible items are constructed, not all will
    // be visible in most configurations.
    private void construct()
    {
        setLayout(new NullLayout(this));
        sourcePanel = new JPanel();
        sourcePanel.setLayout(new BoxLayout(sourcePanel,BoxLayout.X_AXIS));
        currentRoot = new Label("");
    	for(int i=0;i<sources.size();i++)
    	{	FileSource fs = sources.elementAt(i);
    		if(fs!=null)
    		{
    		JButton b = new JButton(fs.name);
    		sourcePanel.addC(b);
    		b.addActionListener(deferredEvents);
    		b.setVisible(true);
    		sourceButtons.add(b);
    		}
    	}
    	sourcePanel.addC(currentRoot);
    	currentRoot.setVisible(true);
    	
    	addC(sourcePanel);
    
        dirField = new XTextField(52);
        dirField.setBackground(Config.FrameBackgroundColor);
        dirField.setActionCommand("ok");
        dirPanel = new JPanel();
        dirPanel.setLayout(new BoxLayout(dirPanel,BoxLayout.X_AXIS));
        dirLabel = new Label("");
        dirLabel.setBackground(Config.FrameBackgroundColor);
        dirPanel.addC(dirLabel);
        dirPanel.setBackground(Config.FrameBackgroundColor);
        dirPanel.setOpaque(true);
        dirPanel.addC(dirField);
        dirPanel.setVisible(true);

        exitPanel = new JPanel();
        exitPanel.setLayout(new BoxLayout(exitPanel,BoxLayout.X_AXIS));
        cancelButton = new JButton(s.get(CancelMessage));
        okButton = new JButton(s.get(LoadGame));
        okButton.setBackground(Config.FrameBackgroundColor);
        saveButton = new JButton(s.get(SaveGame));
        saveButton.setBackground(Config.FrameBackgroundColor);
        cancelButton.setBackground(Config.FrameBackgroundColor);
        exitPanel.addC(cancelButton);
        mkdirButton = new JButton(s.get(MakeDirectory));
        deleteButton = new JButton(s.get(Delete));
        exitPanel.addC(mkdirButton);
        exitPanel.addC(okButton);
        exitPanel.addC(saveButton);
        exitPanel.addC(deleteButton);
        exitPanel.setOpaque(true);
        gameList = new ScrollableList<ProxyString>(12, false);       
        dirList = new ScrollableList<ProxyString>(8, false);
        
        moveField = new XTextField(40);
        moveField.setEditable(false);
        moveField.setText("");
        moveField.setBackground(Config.FrameBackgroundColor);
        moveField.setFont(G.getFont(s.get("fontfamily"), G.Style.Bold, G.standardizeFontSize(G.defaultFontSize)));

        filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel,BoxLayout.X_AXIS));
        JLabel matchLabel = new JLabel(s.get(MatchWords));
        matchLabel.setBackground(Config.FrameBackgroundColor);
        filterPanel.addC(matchLabel);
        filterField = new XTextField(40);
        filterField.setEditable(true);
        filterField.setActionCommand("ok");
        filterField.setText("");
        filterField.setFont(G.getFont(s.get("fontfamily"), G.Style.Bold, 12));
        //filterField.setMaximumSize(new Dimension(1000,30));
        filterPanel.setBackground(Config.FrameBackgroundColor);
        filterPanel.setOpaque(true);
        filterPanel.addC(filterField);
  
        addC(dirPanel);	// directory name
        if(exitPanel!=null) { addC(exitPanel); }
        if(G.isCodename1())
        {
        	addC(dirList);
        }
        else {
        dirListPanel = new JPanel();
        dirListPanel.setLayout(new BoxLayout(dirListPanel,BoxLayout.X_AXIS));
        add(dirListPanel);	// directory list
        dirListPanel.add(new JScrollPane(dirList));
        }
        addC(filterPanel);	// name filter     
        if(G.isCodename1())
        {
        	addC(gameList);        	
        }
        else {
       	gameListPanel = new JPanel();
        gameListPanel.add(new JScrollPane(gameList));
       	gameListPanel.setLayout(new BoxLayout(gameListPanel,BoxLayout.X_AXIS));
        addC(gameListPanel);
        }
        addC(moveField);

        filterField.addActionListener(deferredEvents);
        gameList.addListSelectionListener(deferredEvents);
        dirList.addListSelectionListener(deferredEvents);
        dirField.addActionListener(deferredEvents);
        cancelButton.addActionListener(deferredEvents);  
       	okButton.addActionListener(deferredEvents);
       	saveButton.addActionListener(deferredEvents);
    	mkdirButton.addActionListener(deferredEvents);
    	deleteButton.addActionListener(deferredEvents);
    }
    // constructor for multiple sources of files
    public FileSelector(FileSource... source)
    {
        s = G.getTranslations();
    	for(int i=0;i<source.length;i++) { if(source[i]!=null) { sources.add(source[i]); }}
    	construct();
    	selectSource(source[0]);
    	new Thread(this,"file selector").start();
    	inited = true;
    }
    // constructor for a single source of files
    public FileSelector(URL base, String dir,
            boolean locked, boolean zips)
        {
            this(base, dir, locked, zips,true,false);
        }
    // constructor for a single source of files
    public FileSelector(URL dbase, String dir,
        boolean locked, boolean zips,boolean beModal,boolean save)
    {	
        s = G.getTranslations();
        construct();

    	currentSource.modal = beModal;
    	currentSource.readOnly = !save;  
        currentSource.openZips = zips;         
        if(dbase!=null && dir!=null) { setDirectory(G.getUrl(dbase,dir)); }
        currentSource.dirlistlocked = locked;
       
        new Thread(this,"file selector").start();
        inited = true;
    }
    
    public void setPrefixDir(URL ss)
    {	currentSource.setPrefixDir(ss);
    }
    private void selectSource(FileSource sr)
    {	if(sr!=null)
    	{
    	currentSource = sr;
    	currentRoot.setText(currentSource.rootDir().toExternalForm());
    	setDirectory(currentSource.currentDir());
    	for(int i=0;i<sources.size();i++)
    	{	FileSource ps = sources.elementAt(i);
    		JButton p = sourceButtons.elementAt(i);
    		p.setForeground(sr==ps ? Color.black : Color.gray);
    	}}
    	setLocalBounds(0,0,getWidth(),getHeight());
    	repaint();
    }
    public void setDirectory(URL ss)
    {
    	if(ss!=null) 
    		{ 
    		if(currentSource.rootDir()==null) { currentSource.setRootDir(ss);}
    		setPrefixDir(ss); 
    		}
        if(!currentSource.readOnly && "file".equals(ss.getProtocol())) 
        	{ new File(ss.getFile()).mkdir();
        	}
        dirList.removeAll();
        gameList.removeAll();
        G.print("Switch to "+ss);
        initDir(ss);
    }
    private String currentSubDir(URL dir)
    {
        return (currentSource.selectSubDir(dir));
    }

    public SimpleObservable addObserver(SimpleObserver o)
    {  	
    	observer.addObserver(o);
    	return(observer);
    }

    public void deleteObserver(SimpleObserver o)
    {  	observer.removeObserver(o);
    }
    
    // read a SGF file, parse it and build a game tree
    // sounds simple, eh...
    private void initGame(URL gameurl)
    {
        initGame(gameurl.toExternalForm());
    }

    private void initGame(String game)
    {       
        if(dirField!=null) 
        	{ 
        	  if(currentSource.currentZip!=null)
        		{ selectedUrl = G.getUrl("file:" + game);
        		  dirField.setText(game);
        		}
        	else
        	{ selectedUrl = G.getUrl(game);
        	  dirField.setText(currentSubDir(selectedUrl)); 
        	}}
        if (currentSource.readOnly)
        {	sendLoad();
        }
    }
    
    // notify observers of a load file request
    private void sendLoad()
    {
    	if(inited && (selectedUrl!=null))
    	{	
            observer.setChanged(
            		new FileSelectorResult(
            				SERVERFILE,
            				selectedUrl,
            				currentSource.currentZip));
    	}
    }
    // notify observers of a save file request
    private void sendSave()
    {
    	if(inited && (selectedUrl!=null))
    	{
            observer.setChanged(
            		new FileSelectorResult(
            				SERVERSAVE,
            				selectedUrl,
            				currentSource.currentZip));
    	}
    }
    // read a directory, well actually read a html file
    // and take interesting stuff from it
    public void initDir(URL dir)
    {
        Vector<String> match = new Vector<String>();
        { //get the list of match words

            StringTokenizer t = new StringTokenizer(filterField.getText());

            while (t.hasMoreTokens())
            {
                match.addElement(t.nextToken().toLowerCase());
            }
        }

        String ds = dir.toExternalForm();
        gameList.setVisible(false);
        gameList.removeAll();
        gamefile = "";

        try
        {
            if (currentSource.openZips && ds.endsWith(".zip"))
            {
                if(dirList.getItemCount()==0)
                {
                	initDir(makeURL(ds.substring(0,ds.lastIndexOf('/'))));
                    gameList.removeAll();
                }
                initZipDir(dir, ds, match);
            }
            else
            {
                initFileDir(dir, ds, match);
            }
        }
    	catch (ThreadDeath err) { throw err;}
        catch (Throwable e)
        {
            G.print("Error: " + e.toString());
            G.printStackTrace(e);
        }
        finally
        {
        	gameList.setVisible(true);        
        	gameList.repaint();
        	dirList.repaint();
        }
    }

    private boolean nameMatches(String name, Vector<String> match)
    {
        boolean ok = true;
        String lname = name.toLowerCase();
        for (Enumeration<String> e = match.elements(); ok && e.hasMoreElements();)
        {
            ok &= (lname.indexOf(e.nextElement()) >= 0);
        }

        return (ok);
    }

    private boolean readAndParseDirectory(URL dir,Vector<String> match,
    		Vector<String>directories,Vector<String>nondirectories) throws IOException
            {
    	boolean isFile = "file".equals(dir.getProtocol());
        InputStream file = null;
        Utf8Reader dis = null;
        SortedStringStack sortList = new SortedStringStack();
        boolean dirReadError = false;
        try
        {
            boolean dirisnext = false;
            file = dir.openStream();
            if(file!=null)
            {
            dis = new Utf8Reader(file);
            String name = "";
            String line;
            int linen = 1;
            while ((line = dis.readLine()) != null)
            {	try {
            	//G.print("R: "+line);
            	String lcline = line.toLowerCase();
                int hindex = lcline.indexOf("<a href", 0);
                int iindex = lcline.indexOf("<img ", 0);
                if (isFile && (hindex < 0) && (iindex < 0))
                { //simple listing, not html.  Strange things happen with unicode file names.
                  // on windows, we can receive raw accent characters in the 0x80 to 0xff range
                  // but harder unicode such as chinese and korean delivers actual ? characters.
                	File newfile = new File(dir.getFile()+line);
                    if (newfile.isDirectory() || lcline.endsWith(".zip"))
                    {
                            directories.addElement(line);
                    }
                    else if (nameMatches(line, match))
                    {
                        sortList.push(new SortedString(line));
                    }
                }
                else
                {	// html directory listings are more complex; we look for <a href="xxx">
                	// where the xxx is utf8 encoded as a url, so unicode "c3e9" is encoded
                	// as % c 3 % e 9
                	// in other parts of the same line, raw utf8 is found.
                    while ((hindex >= 0) || (iindex >= 0))
                    {
                        boolean quote = false;
                        int index = iindex;
                        boolean ishref = false;

                        if ((iindex < 0) ||
                                ((hindex >= 0) && (hindex < iindex)))
                        {
                            ishref = true;
                            index = hindex;
                        }

                        //	System.out.println(lcline);
                        for (; index < lcline.length(); index++)
                        {
                            char ch = line.charAt(index);

                            if (quote)
                            {
                                if (ch == '\"')
                                {
                                    //System.out.println("name " + name + " " + dirisnext+" " + ishref);
                                    if (!ishref) //not href, must be img
                                    { //nt systems use an image named "directory.gif"
                                        dirisnext = ((name.toLowerCase()
                                                          .indexOf("directory")) >= 0);
                                    }
                                    else
                                    {
                                        if (name.startsWith("/") ||
                                                name.startsWith("?"))
                                        {
                                             //on unix systems, these are .. . etc.
                                        }
                                        else if (name.endsWith("/") ||
                                                dirisnext)
                                        { //unix systems identify a dir with a trailing /
                                                directories.addElement(name);
                                        }
                                        else
                                        {  
                                            if (nameMatches(name, match))
                                            {   // these names are typically URI-encoded with %xx%yy for funny or unicode characters
                                            	// we pass them unchanged so that if they're used to create new urls used to open files,
                                            	// the origial (whatever it was) will be used to create the new file url.
                                            	// this avoids the problems if some file servers deliver raw unicode and others
                                            	// deliver utf8-encoded unicode.
                                                if (currentSource.openZips &&
                                                        name.endsWith("zip"))
                                                {
                                                    directories.addElement(name);
                                                }
                                                else
                                                {
                                                    sortList.push(new SortedString(name));
                                                }
                                            }
                                        }
                                    }

                                    quote = false;

                                    break;
                                }

                                name = name + ch;
                            }
                            else if (ch == '\"')
                            {
                                quote = true; //found the start of a name
                                name = "";
                            }
                        }

                        hindex = lcline.indexOf("<a href", index);
                        iindex = lcline.indexOf("<img ", index);
                    }
                }
            }
            catch (Throwable err)
            	{
            	G.print("\nError parsing line ",linen," ",name,"\n",err,"\n",line);
            	}
            linen++;
        }}}
        catch (FileNotFoundException ee) { dirReadError = true; }
        finally
        {
            if (dis != null)
            {
                dis.close();
            }

            if (file != null)
            {
                file.close();
            }
        }

        sortList.sort();
        for(int i=0;i<sortList.size();i++) { nondirectories.add(sortList.elementAt(i).str); }
        return(dirReadError);
    }

    /* setup based on a directory listing provided by the browset */
    private void initFileDir(URL dir, String ds, Vector<String> match)
        throws IOException
    {
        Vector<String> directories = new Vector<String>();
        Vector<String> nondirectories = new Vector<String>();
        if (!ds.endsWith("/"))
        {
            try
            {
                dir = makeURL(ds = ds + "/");
            }
            catch (MalformedURLException err)
            { //pro forma catch
            }
        }

        //System.out.println("read directory "+dir);
        currentSource.currentZip = null;
        currentSource.filePaneDir = dir;
        if (currentSource.rootDir() == null)
        {
        	currentSource.setRootDir(dir);
        }

        moveField.setText(s.get(ReadingDir));
        selectedUrl = dir;
        dirField.setText(currentSubDir(dir));
        
        boolean dirReadError = readAndParseDirectory(dir,match,directories,nondirectories);
        
        for(int i=0,lim=nondirectories.size(); i<lim;i++)
        {	String item = nondirectories.elementAt(i);
        	String eitem = URLDecoder.decode(item,"UTF-8");		// decode %xx%yy into unicode strings for display
        	gameList.add(new ProxyString(item,eitem));
        }
        
        if (!dirReadError)
        {
            moveField.setText("    ");
        }
        else
        {
            moveField.setText(s.get(ErrorReadingDir,dir.toString()));
        }
        
        //if we got any subdirectories, descend a level
        {
            int ndirs = directories.size();
            int currentDirCount = dirList.getItemCount();
            currentSource.filePaneDir = dir;

            if (ndirs > 0 || (currentDirCount==0))
            {
                dirList.setVisible( false);
            	dirList.removeAll();
                dirList.add(new ProxyString("../"));
                
                if(ndirs==0)
                {	String ext = dir.toExternalForm();
                	if(!ext.equals(currentSource.baseDir()))
                	{
                	int idx = ext.lastIndexOf('/',ext.length()-2);
                	URL parent = makeURL(ext.substring(0,idx));
                    readAndParseDirectory(parent,match,directories,new Vector<String>());
                    ndirs = directories.size();
                	}
                }
                
                currentSource.dirPaneDir = dir;
                SortedStringStack sorted = new SortedStringStack();
                
                for (int i = 0; i < ndirs; i++)
                {
                    sorted.push(new SortedString(directories.elementAt(i)));
                }
                
                sorted.sort();
                for(int i=0;i<sorted.size();i++)
                { String el = sorted.elementAt(i).str;
                  String del = URLDecoder.decode(el,"UTF-8");	// decode %xx%yy into unicode strings for display
                  dirList.add(new ProxyString(el,del)); 
                }
                
                dirList.setVisible(true);
                dirList.clearSelection();
                setLocalBounds(0,0,getWidth(),getHeight());	// standard java needs this
            }
        }
    }


    public URL[] getURLs(ScrollableList<ProxyString> l)
    {
        int nitems = l.getItemCount();
        URL[] results = new URL[nitems];

        for (int i = 0; i < nitems; i++)
        {
            try
            {	String item = (l.getItem(i).value);
                results[i] = makeURL(currentSource.filePaneDir, item);
            }
            catch (MalformedURLException e)
            { // pro forma catch
            }
        }

        return (results);
    }

    public URL[] getDirectories()
    {
        return (getURLs(dirList));
    }

    public URL[] getFiles()
    {
        return (getURLs(gameList));
    }

    // select a game from a list
    private void selectGame(ScrollableList<ProxyString> listbox)
    {
        URL newgameurl;
        URL oldgamedir = currentSource.filePaneDir;
        String oldgamefile = gamefile;
        boolean newdir = false;
        ProxyString newgamefileProxy = listbox.getSelectedItem();
        String newgamefile = newgamefileProxy==null ? null : newgamefileProxy.value;
        if ((newgamefile != null) && (!newgamefile.equals(oldgamefile)))
        {
            boolean isdir = (listbox == dirList) || newgamefile.endsWith("/");

            try
            {
                if (newgamefile.startsWith("http:")||newgamefile.startsWith("https:"))
                {
                    newgameurl = makeURL(newgamefile);
                }
                else if (isdir)
                { //lock us to the root   				
                	if(newgamefile.equals("../"))
                	{
                	if(currentSource.currentZip!=null) 
                	{ currentSource.currentZip=null;
                	  newdir = true;
                	  newgameurl = currentSource.dirPaneDir; 
                	}
                	else
                		{
                		newgameurl = currentSource.filePaneDir;	// set a default
                		String path = currentSource.filePaneDir.toExternalForm();
                		int length = path.length();
            
                		if(length>8)
                			{
                			int idx = path.lastIndexOf("/",length-2);
                			if(idx>=7) { path = path.substring(0,idx+1); }
                			String root = currentSource.rootdir.toExternalForm();
                			// if locked, don't ascend above the root
                			if(!(currentSource.dirlistlocked 
                					&& !root.equals(path)
                					&& root.startsWith(path)))	// can't go below the root
                			{
                			newgameurl = makeURL(path);
                			}
                			}
                		}
                	}
                	else
                	{
                    newgameurl = makeURL(currentSource.dirPaneDir, newgamefile);
                	}
                }
                else
                {	gamefile = newgamefile;
                    newgameurl = makeURL(currentSource.filePaneDir, newgamefile);
                }

                if (isdir)
                {	if(!newgameurl.equals(oldgamedir) || newdir)
                	{
                    initDir(newgameurl);
                	}
                }
                else if (currentSource.currentZip != null)
                {
                    initGame(gamefile);
                }
                else
                {
                    initGame(newgameurl);
                }
            }
        	catch (ThreadDeath err) { throw err;}
            catch (Throwable e)
            {
                Http.postError(this, "Error reading file", e);
                moveField.setText(s.get(ErrorReadingFile,e.toString()));
            }
        }
        else
        {
            gamefile = oldgamefile;
        }
    }

    // get a new directory
    private void getDir()
        {	String text = dirField.getText();
    		initDir("".equals(text) 
    				? currentSource.rootDir() 
    				: currentSource.selectedUrl(text));
    }

    /* setup based on reading a zip directory */
    private void initZipDir(URL dir, String ds, Vector<String> match)
        throws IOException
    {
        moveField.setText(s.get(ReadingFile,dir.getFile()));
        selectedUrl = G.getUrl(ds);
        dirField.setText(currentSubDir(selectedUrl));

        currentSource.currentZip = dir;

        InputStream file = null;
        ZipInputStream zip = null;

        try
        {
            String name = null;
            file = dir.openStream();

            ZipEntry z;
            zip = new ZipInputStream(file);
            SortedStringStack sorted = new SortedStringStack();
            
            while (((z = zip.getNextEntry()) != null) &&
                    ((name = z.getName()) != null))
            {
                if (!z.isDirectory())
                {
                    if (nameMatches(name, match))
                    {  //G.print("add "+name);
                        sorted.push(new SortedString(name));
                    }
                }
            }
            sorted.sort();
            for(int i=0;i<sorted.size();i++) 
            { String el = sorted.elementAt(i).str;
              String del = URLDecoder.decode(el,"UTF-8");	// decode %xx%yy into unicode strings for display
              gameList.add(new ProxyString(el,del)); 
            }
            moveField.setText("    ");
        }
    	catch (ThreadDeath err) { throw err;}
        catch (Throwable err) { moveField.setText(err.toString()); }
        finally
        { //if(zip!=null) { zip.close(); }

            if (file != null)
            {
                file.close();
            }
            if(zip!=null) { zip.close(); }
        }
    }

    @SuppressWarnings("unchecked")
	public void itemStateChanged(Object evt)
    {	//List sel = (List) evt.getItemSelectable();
    	if(evt instanceof ScrollableList<?>)//if(sel!=null)
    	{
    	selectGame((ScrollableList<ProxyString>)evt);
    	}
    }

    private boolean DoActionEvent(Object target,String command)
    {
    	for(int i=0;i<sourceButtons.size();i++)
    	{
    		if(target==sourceButtons.elementAt(i))
    		{
    			selectSource(sources.elementAt(i));
    			return(true);
    		}
    	}
        if (target == filterField)
        {	if("ok".equals(command))
        {
            initDir((currentSource.currentZip != null) ? currentSource.currentZip : currentSource.filePaneDir);
        }
        }
        else if (target == dirField)
        {
        	if("ok".equals(command))
        	{
        	String text = dirField.getText();
        	if("".equals(text) || (text.indexOf(':')>=0) || text.endsWith("/"))
        		{dirList.removeAll();
            getDir();
        		}}
        }
        else if (target == cancelButton)
        {
        	selectedUrl = null;
        	if(currentSource.modal) { exit = true; }
        }
        else if(target == okButton)
        {	selectedUrl = currentSource.selectedUrl(dirField.getText());
        	if(selectedUrl!=null)
        		{ if(currentSource.modal) { exit = true; }
        		  sendLoad();
        		}
        }
        else if(target == saveButton)
        {	selectedUrl = currentSource.selectedUrl(dirField.getText());
        	if(selectedUrl!=null)
        		{ if(currentSource.modal) { exit = true; }
        		  sendSave();
        		}
        }
        else if(target==deleteButton)
        {
        	URL u = currentSource.selectedUrl(dirField.getText());
        	File f = new File(u.getFile());
        	f.delete();
        	setDirectory(currentSource.currentDir());	
        }
        else if(target == mkdirButton)
        {	URL u = currentSource.selectedUrl(dirField.getText());
        	File f = new File(u.getFile());
        	f.mkdir();
        	getDir();
        }
        setFocused(this);
        return (true);
    }

    public boolean exit = false;


    

	public void run() {

		while(!exit)
			{
			// handle slow events such as reading zip files over the web in a separate thread
			try
			{
			deferredEvents.handleDeferredEvent(this);
			G.doDelay(100);
			} 
			catch (Throwable er)
				{ G.print("Error in file handler "+er);}			
				}
		}

	public LFrameProtocol startDirectory(String frameName,boolean closeable)
	{
		XFrame f = new XFrame(s.get(frameName));
		f.setContentPane(this);
		f.addWindowListener(this);
		f.setCloseable(closeable);	// keep it open, will be closed with the main frame.
		if(!G.isCodename1()) 
		{
		 double scale = G.getDisplayScale();
		 f.setBounds( 0, 0, (int)(scale*450), (int)(scale*430)); 
		}
    	f.setIconAsImage(StockArt.GameSelectorIcon.image);

		f.setVisible(true);
		return(f);
	}
	public void update(SimpleObservable observable, Object eventType, Object data) 
	{
		// this gets hit when the save action is completed
		if(data instanceof String)
		{
		if(data.equals("filesaved")) { setDirectory(currentSource.currentDir()); }
		}
	}

	public boolean handleDeferredEvent(Object e, String command) 
	{
		DoActionEvent(e,command);
		itemStateChanged(e);
		return(true);
	}


	public void windowClosing(WindowEvent e) {
		selectedUrl = null;
		exit = true;
		
	}

    private static final String DirectoryMessage = "Current Directory:";
    private static final String CurrentFileMessage = "Selected File: ";
    private static final String LoadGame = "Load Game";
    private static final String MakeDirectory = "Make Directory";
    private static final String Delete = "Delete";
    private static final String MatchWords = "Match words:";
    private static final String CancelMessage = "Cancel";
    private static final String SaveGame = "Save Game";
    private static final String ReadingDir = "reading directory...";
    private static final String ErrorReadingDir = "Error reading directory #1";
    private static final String ErrorReadingFile = "Error reading file #1";
    private static final String ReadingFile = "reading #1";
    public static final String[]FileSelectorStrings = {
    		DirectoryMessage,
    		CurrentFileMessage,
    		LoadGame,
    		Delete,
    		MakeDirectory,
       		MatchWords,
    		CancelMessage,
    		SaveGame,
    		ReadingDir,
    		ErrorReadingDir,
    		ErrorReadingFile,
    		ReadingFile,
     	};

    private URL makeURL(URL dirname,String filename) throws MalformedURLException
    {	return(new URL(dirname,filename));
    }
    private URL makeURL(String filename) throws MalformedURLException
    {	return(new URL(filename));
    }
    

} // class FileSelector