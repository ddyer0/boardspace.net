package bugs.data;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import bridge.Platform;
import bugs.BugCard;
import bugs.BugsConstants;
import lib.Bitset;
import lib.G;
import lib.Image;
import lib.Tokenizer;
import lib.Utf8Reader;

public class Profile extends DataHelper<Profile> implements KeyProvider,BugsConstants {
	
    public String name;
    public int getUid() { return -1; }
    public String toString() {
    	 return "<profile "+name+">";  
    }
	public static Profile get(String n) { return profiles.get(n); }

    public String getKey() { return name; }
    public Rank rank;
    public String lastChanged;
    public String profileGroup;
    public Bitset<Habitat> habitat = new Bitset<Habitat>();
    public Flying flying;
    public Diet diet;
    public Image illustrationImage = null;
    public int cardPoints=0;
    public Profile getProfile() { return this; }
    public Taxonomy getCategory()
    {	
    	Taxonomy group = Taxonomy.get(profileGroup);
    	if(group==null)
    	{
    		group = Taxonomy.get(DefaultProfile);
    	}
    	return group;
    }
    public DataHelper<?> getPrimary()
    {
    	return rank==Rank.GENUSANDSPECIES ? MasterSpecies.get(name) : Taxonomy.get(name);
    }
    public String getShortDescription()
    {
    	return getPrimary().getShortDescription();
    }
    public String getCommonName()
    {
    	return getPrimary().getCommonName();
    }
    public String getScientificName() { return getPrimary().getScientificName(); }
    
    public String getDescription()
    {
    	return getPrimary().getDescription();
    }
    public String serialize() {
        return String.join("\t",
            escape(name),
            escape(rank.name()),
            escape(lastChanged),
        	escape(profileGroup),
            habitat != null ? habitat.memberString(Habitat.values()).replace(' ',',') : "",
            flying != null ? flying.name() : "",
            diet != null ? diet.name() : ""
        );
    }
    public boolean isHerbivore()
    {
    	return diet.isHerbivore;
    }
    public boolean isCarnivore()
    {
    	return diet.isCarnivore;
    }
    public boolean isFlying()
    {
    	return flying==Flying.YES;
    }
    public void calculatePoints()
    {
        Taxonomy p = getCategory();
        double sz = Taxonomy.largestCategorySize/2+1;
        // revision - make card point values LESS for carnivore and flying, because they are
        // more valuable in game play
        cardPoints = Math.max(1,(int)(sz/p.split1Count+0.5
        					+((p==Taxonomy.getWildType()) ? WILDCARD_BONUS : 0)
        					+(isCarnivore()?CARNIVORE_BONUS:0) 
        					+(isFlying()?FLYING_BONUS : 0)));
    }
    
    public Profile deserialize(String fields[]) {
 
        Profile profile = new Profile();
        profile.name = unescape(fields[0]);
        profile.rank = Rank.valueOf(unescape(fields[1]).toUpperCase());
        profile.lastChanged = unescape(fields[2]);
        profile.profileGroup = unescape(fields[3]);
        parseHabitatSet(profile.habitat,fields[4]);
        String flying = fields[5];
        profile.flying = Flying.valueOf(flying.length()==0 ? "NO" : fields[5].toUpperCase());
        String diet = fields[6];
        profile.diet = Diet.valueOf(diet.length()==0 ? "UNKNOWN" : diet.toUpperCase());
        return profile;
    }
    public static Hashtable<String,Profile> profiles = null;
  
    public static void load(String file,String... imagePath)
    {
    	if(profiles==null)
    	{	Profile np = new Profile();
    		profiles = np.readFromFile(file);
    		for(String path : imagePath)
    		{
    			np.loadImages(path);
    		}
    	}
    }
    public List<String> getImageResourceList(String path,String name)
    {
       	List<String>lis = new ArrayList<String>();
       	InputStream in = Platform.class.getResourceAsStream(path+name);
    	if(in!=null)
    	{
    	Utf8Reader ins = new Utf8Reader(in);
    	String line = null;
    	try {
    	while( (line = ins.readLine())!=null)
    	{
    		lis.add(line);
    	}
    	ins.close();
    	}
    	catch (IOException e) {}
    	}
    	return lis;
    }
    public static String uncomposite = "g:/share/projects/boardspace-java/boardspace-games/";
    public void loadImages(String imagePath)
    {   
		List<String> images = getImageResources(imagePath);
		boolean empty = images.isEmpty();
		FileWriter out = null;
		try {
		if(empty)
			{ 
			// when running from jars, the list isn't available
			images = getImageResourceList(imagePath,"index.txt"); 
			}
		else if(!G.isCodename1())
			{// generate the list for use in the delivered version
			out = new FileWriter(uncomposite+imagePath+"index.txt");
			}
		
		for(String s : images)
		{	Tokenizer tok = new Tokenizer(s," ,_-.");
			String genus = tok.nextElement();
			String species = tok.nextElement();
			String key = genus+" "+species;
			Profile p = profiles.get(key);
			if(p==null) { p = profiles.get(species); if(p!=null) { key = species; }}
			if(p==null) { G.print("No profile match for ",key); }
			else if(!s.contains("mask.jpg"))
			{	Image im;
    			String mask = null;
    			if(s.endsWith(".jpg"))
    			{
    				mask = s.substring(0,s.length()-4)+"-mask.jpg";
    				im = new Image(imagePath+s,imagePath+mask);
    				if(out!=null)
    				{
    					out.write(s+"\n");
    					out.write(s+"-mask.jpg\n");
    				}
    			}
    			else
    			{
    				im = new Image(imagePath+s);
    				if(uncomposite!=null && s.endsWith(".png"))
    				{	String basename = s.substring(0,s.length()-4);
    					String maskName = uncomposite+imagePath+basename+"-mask.jpg";
    					String backName = uncomposite+imagePath+basename+".jpg";
    					Image newmask = new Image(maskName);
    					Image newback = new Image(backName);
    					im.unComposite(newback,newmask);
    					newmask.SaveImage(maskName);
    					newback.SaveImage(backName);
    				}
    			}
    			p.illustrationImage = im;
				DataHelper<?> source = MasterSpecies.species.get(key);
				if(source==null) { source = Taxonomy.taxonomies.get(key); }
				else if(BugCard.getBugCard(key)==null)
				{	p.calculatePoints();
					new BugCard(key,p,source);
				}
			}
		}
		if(out!=null) { out.close(); }
    	}
		catch(IOException e)
		{ G.print("error "+e); 
		}
    }
    /*
     * map the varios classification for habitat into just 4 choices.
     */
	private static Bitset<Habitat> waterHabitat = new Bitset<Habitat>(Habitat.MARINE,Habitat.FRESHWATER,Habitat.AQUATIC);
	private static Bitset<Habitat> groundHabitat = new Bitset<Habitat>(Habitat.DESERT,Habitat.SOIL,Habitat.VARIED);
	private static Bitset<Habitat> grassHabitat = new Bitset<Habitat>(Habitat.GRASSLAND,Habitat.URBAN,Habitat.VARIED);
	private static Bitset<Habitat> forestHabitat = new Bitset<Habitat>(Habitat.FOREST,Habitat.VARIED);
	private static Bitset<Habitat> nonWaterHabitat = new Bitset<Habitat>(Habitat.values()).difference(waterHabitat);
	private static Bitset<Habitat> nonGroundHabitat = new Bitset<Habitat>(Habitat.values()).difference(groundHabitat);
	private static Bitset<Habitat> nonGrassHabitat = new Bitset<Habitat>(Habitat.values()).difference(grassHabitat);
	private static Bitset<Habitat> nonForestHabitat = new Bitset<Habitat>(Habitat.values()).difference(forestHabitat);
	
	// true if this bug can live in the water
	public boolean hasWaterHabitat() {
		return habitat.test(waterHabitat);
	}
	// true if this bug can live on the ground
	public boolean hasGroundHabitat() {
		return habitat.test(groundHabitat);
	}
	// true if this bug can live in the grass
	public boolean hasGrassHabitat() {
		return habitat.test(grassHabitat);
	}
	// true if this bug can live in the forest
	public boolean hasForestHabitat() {
		return habitat.test(forestHabitat);
	}
	// true if this bug lives only in the water
	public boolean hasOnlyWater()
	{
		return !habitat.test(nonWaterHabitat);
	}
	// true if this bug lives only in the forest
	public boolean hasOnlyForest()
	{
		return !habitat.test(nonForestHabitat);
	}
	// true if this bug lives only in the ground
	public boolean hasOnlyGround()
	{
		return !habitat.test(nonGroundHabitat);
	}
	// true if this bug lives only in the grass
	public boolean hasOnlyGrass()
	{
		return !habitat.test(nonGrassHabitat);
	}

}
