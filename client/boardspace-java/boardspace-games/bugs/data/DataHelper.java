package bugs.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Hashtable;
import lib.Bitset;
import lib.G;
import lib.Image;
import lib.Utf8Reader;

interface KeyProvider
{ public String getKey(); 
  public String serialize();
}

public abstract class DataHelper<T extends KeyProvider> {
	public abstract String serialize();
	public abstract T deserialize(String line[]);
	public abstract String getKey();
	public abstract String getDescription();
	public abstract String getShortDescription();
	public abstract String getCommonName();
	public abstract String getScientificName();
	public abstract Profile getProfile();
	static int TAX_OFFSET = 100;
	static int SPEC_OFFSET = 200;
	public abstract int getUid();
	static String DefaultProfile = "Animalia";
    public enum Habitat {
        FRESHWATER, MARINE,AQUATIC, SOIL, FOREST, GRASSLAND, DESERT,
        URBAN,  VARIED, UNKNOWN
        
    }

    public enum Flying {
        YES, NO, UNKNOWN
    }
  
    public enum Diet {
        HERBIVORE(false,true,false),
        FUNGIVORE(false,true,false),
        CARNIVORE(true,false,false),
        OMNIVORE(true,false,false),
        DETRITIVORE(false,true,true),
        SCAVENGER(false,true,true),
        NEGAVORE(false,true,false),	// eaten by carnivores
        PARASITE(false,false,false),
        UNKNOWN(false,false,false);
    	boolean isPredator = false;
    	boolean isPrey = false;
    	boolean isScavenger = false;
         Diet(boolean car,boolean veg,boolean scav)
        {
        	isPredator = car;
        	isPrey = veg;
        	isScavenger = scav;
        }
        public boolean isPredator() { return isPredator; }
        public boolean isPrey() { return isPrey; }
        public boolean isScavenger() { return isScavenger; }
        public boolean isParasite() { return this==PARASITE; }
        public boolean isNegavore() { return this==NEGAVORE; }
        public boolean isHerbivore() { return this==HERBIVORE||this==FUNGIVORE;}
    }
    public enum Rank {
        DOMAIN, KINGDOM, PHYLUM, CLASS, ORDER, FAMILY, GENUS, GENUSANDSPECIES, UNKNOWN;
    	
    }

	public static String readFromFile(Utf8Reader reader) throws IOException
	{
		StringBuilder combined = new StringBuilder();
        String line;
        boolean some = false;
        while ((line = reader.readLine()) != null) 
        {	
        	if(!line.trim().isEmpty())
        	{
        	if(some) { combined.append("\n"); }
        	some = true;
        	combined.append(line);
        	}
        	else if(some) 
        		{ return combined.toString(); 
        		}
        }
        return line;
	}
    public static String escape(String s) {
    	if(s==null) { return null; }
    	String r1 = G.replaceAll(s,"\t", "\\t");
    	String r2 = G.replaceAll(r1,"\n", "\\n");
        return r2;
    }

    public static String unescape(String s) {
    	String r1 = G.replaceAll(s,"\\t", "\t");
    	String r2=  G.replaceAll(r1,"\\n", "\n");
    	return r2;
    }
    
    public void parseHabitatSet(Bitset<Habitat>habitat,String names)
    {
    	String s[] = G.split(names,',');
    	for(String m : s)
    	{	if(m.length()>0)
    		{
    		Habitat n = Habitat.valueOf(m.trim().toUpperCase());
    		habitat.set(n);
    		}
    	}
    }
    public String header = null;
    public int nFields = 0;
    public Hashtable<String,T> readFromFile(String file) 
    {
        Hashtable <String,T> list = new Hashtable<String,T>();
        try(	InputStream res = G.getResourceAsStream(file);
        		//BufferedReader reader = new BufferedReader(new InputStreamReader(res))) 
        		Utf8Reader reader = new Utf8Reader(res))
        	{
        	String line;
             while ((line = readFromFile(reader)) != null) 
             {	
                if (header==null) {
                    header = line;
                    nFields = G.split(line,'\t').length;
                }
                else
                {
                String[] fields = G.split(line,'\t');
                if (fields.length < nFields) 
                	{
                     G.Error("expected %s fields, got %s",nFields,fields.length);
                	}
                	T s = deserialize(fields);
                	list.put(s.getKey(),s);
                }
            }
           res.close();
        }
        catch (IOException f)
        {
        	G.Error("Loading ",file);
        }
        //G.print("read file: ",file," ",list.size());
        return (list);
    }

    public void writeToFile(Hashtable<String,T> list, File file, String header) throws IOException 
    {	// this code isn't tested
        PrintStream writer = new PrintStream(new FileOutputStream(file));
            if (header!=null) {
                writer.print(header+"\n\n");
            }
            for (T m : list.values()) {
                writer.print(m.serialize());
                writer.println();
                writer.println();
            }
            writer.close();
    }

    
    public String[] getImageResources(String path) 
    {	URL resource = G.getResourceUrl(path,false);
    	return Image.getImageList(resource);
    /*
    	StringStack imageFiles= new StringStack();
    	
        if (resource != null) 
        {
        try {
        if (resource.getProtocol().equals("file")) {
            File dir = new File(resource.toURI());
            File[] files = dir.listFiles((dir1, name) -> name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg") || name.toLowerCase().endsWith(".gif"));
            if (files != null) {
                for (File file : files) {
                    imageFiles.push(file.getName());
                }
            }
        }
        }
        catch(URISyntaxException e)
        {
        	throw G.Error("get resoruces %s",e);
        }
        }
        return imageFiles.toArray();
        */
     }


}
