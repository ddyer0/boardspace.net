package bugs.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bridge.Platform;
import lib.Bitset;
import lib.G;

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
	static int TAX_OFFSET = 0;
	static int SPEC_OFFSET = 100;
	public abstract int getUid();
	static String DefaultProfile = "Animalia";
    public enum Habitat {
        FRESHWATER, MARINE, SOIL, FOREST, GRASSLAND, DESERT,
        URBAN,  VARIED, UNKNOWN
        
    }

    public enum Flying {
        YES, NO, UNKNOWN
    }
  
    public enum Diet {
        HERBIVORE(false,true),
        FUNGIVORE(false,true),
        CARNIVORE(true,false),
        OMNIVORE(true,false),
        DETRITIVORE(false,true),
        SCAVENGER(false,true),
        NEGAVORE(false,true),	// eaten by carnivores
        PARASITE(false,false),
        UNKNOWN(false,false);
    	boolean isCarnivore = false;
    	boolean isHerbivore = false;
        Diet(boolean car,boolean veg)
        {
        	isCarnivore = car;
        	isHerbivore = veg;
        }
    }
    public enum Rank {
        DOMAIN, KINGDOM, PHYLUM, CLASS, ORDER, FAMILY, GENUS, GENUSANDSPECIES, UNKNOWN;
    	
    }

	public static String readFromFile(BufferedReader reader) throws IOException
	{
		StringBuilder combined = new StringBuilder();
        String line;
        boolean some = false;
        while ((line = reader.readLine()) != null) 
        {	if(!line.trim().isEmpty())
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
        return s == null ? "" : s.replace("\t", "\\t").replace("\n", "\\n");
    }

    public static String unescape(String s) {
        return s.replace("\\t", "\t").replace("\\n", "\n");
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
        InputStream res = Platform.class.getResourceAsStream(file);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(res)))
        {
            String line;
             while ((line = readFromFile(reader)) != null) {
                if (header==null) {
                    header = line;
                    nFields = line.split("\t",-1).length;
                }
                else
                {
                String[] fields = line.split("\t", -1);
                if (fields.length < nFields) 
                	{
                    G.Error("expected %s fields, got %s",nFields,fields.length);
                	}
                	T s = deserialize(fields);
                	list.put(s.getKey(),s);
                }
            }
        }
        catch (IOException f)
        {
        	G.Error("Loading ",file);
        }
        return (list);
    }

    public void writeToFile(Hashtable<String,T> list, File file, String header) throws IOException 
    {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            if (header!=null) {
                writer.write(header+"\n\n");
            }
            for (T m : list.values()) {
                writer.write(m.serialize());
                writer.newLine();
                writer.newLine();
            }
        }
    }
    
    public List<String> getImageResources(String path) 
    {
        URL resource = Platform.class.getResource(path);
        if (resource == null) {
            return new ArrayList<>();
        }
        try {
        List<String> imageFiles = new ArrayList<>();
        if (resource.getProtocol().equals("file")) {
            File dir = new File(resource.toURI());
            File[] files = dir.listFiles((dir1, name) -> name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg") || name.toLowerCase().endsWith(".gif"));
            if (files != null) {
                for (File file : files) {
                    imageFiles.add(file.getName());
                }
            }
        } else {
            try (Stream<Path> stream = Files.walk(Paths.get(resource.toURI()))) {
                imageFiles = stream.map(String::valueOf)
                        .filter(name -> name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg") || name.toLowerCase().endsWith(".gif"))
                        .collect(Collectors.toList());
            }
        }
        return imageFiles;
       }
        catch (Throwable err)
        {
        	G.Error("error geting image resources for "+path,err);
        }
        return null;
     }

}
