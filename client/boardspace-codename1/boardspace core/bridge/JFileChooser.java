package bridge;

import lib.FileSelector;
import lib.G;
import lib.SimpleObservable;
import lib.SimpleObserver;
import lib.XFrame;

public class JFileChooser  implements SimpleObserver
{	public static String FILES = "files";
	public static String FILES_AND_DIRECTORIES = "dirs";
	public static String FORSAVE = "save";	

	FileSelector selector = null;
	String dir = "";
	String selectedFile = null;
	String caption = null;
	boolean forSave = false;
	boolean allowDirs = false;
	XFrame selectorFrame = null;

	// constructor
	public JFileChooser(String string)
	{
		caption = string;
		selector = new FileSelector(null,"",false,true,true,forSave);
		String root = G.documentBaseDir();
		selector.setPrefixDir(G.getUrl("file:"+root));
		selector.setDirectory(G.getUrl("file:"+root));
		selector.addObserver(this);
	}

	public void setDialogTitle(String string) {
		caption = string;
	}
	public void showOpenDialog(Object object) {
		selectorFrame = selector.startDirectory("file selector",true);
		setVisible(true);
	}

	public File getSelectedFile() {
		return(selectedFile==null ? null : new File(selectedFile));
	}

	public void setFileSelectionMode(String filesAndDirectories) {
		forSave = FORSAVE.equals(filesAndDirectories);
		allowDirs = FILES_AND_DIRECTORIES.equals(filesAndDirectories);
	}

	public void setVisible(boolean val)
	{
		selectorFrame.setVisible(val);
		if(val)
		{
		while(!selector.exit) { G.waitAWhile(this, 200); }
		URL fu = selector.selectedUrl;
		if(fu!=null)
			{
			dir = fu.getDirectory();
			selectedFile = fu.getFile(); 
			}
		else { dir = null; selectedFile=null; }
		G.print("file selected : "+dir+" + "+selectedFile);
		selectorFrame.dispose(); 
		}
	}

	public String getDirectory() 
	{  return dir;
	}
	public void setDirectory(String baseDir)
	{
		String root = G.documentBaseDir();
		String base = root + baseDir;	
		new File(base).mkdir();
		selector.setPrefixDir(G.getUrl("file:"+root));
		selector.setDirectory(G.getUrl("file:"+base));
	}


	public String getFile() {
		return selectedFile;
	}

	public void update(SimpleObservable observable, Object from,Object data) {

	}

}
