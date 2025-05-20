package bugs.data;

import java.util.Hashtable;
import java.util.List;

import bugs.BugCard;
import lib.Bitset;
import lib.G;
import lib.Image;
import lib.Tokenizer;

public class Profile extends DataHelper<Profile> implements KeyProvider {
	
    public String name;
    
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
    public void calculatePoints()
    {
        Taxonomy p = getCategory();
        double sz = Taxonomy.largestCategorySize/2+1;
        cardPoints = (int)(sz/p.split1Count+0.5);
        
        
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
    public static String uncomposite = "g:/share/projects/boardspace-java/boardspace-games/";
    public void loadImages(String imagePath)
    {
		List<String> images = getImageResources(imagePath);
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
				else if(BugCard.get(key)==null)
				{	p.calculatePoints();
					new BugCard(key,p,source);
				}
			}
			
		}
    }
    /*
     * map the varios classification for habitat into just 4 choices.
     */
	private Bitset<Habitat> waterHabitat = new Bitset<Habitat>(Habitat.MARINE,Habitat.FRESHWATER);
	private Bitset<Habitat> groundHabitat = new Bitset<Habitat>(Habitat.DESERT,Habitat.SOIL,Habitat.VARIED);
	private Bitset<Habitat> grassHabitat = new Bitset<Habitat>(Habitat.GRASSLAND,Habitat.URBAN,Habitat.VARIED);
	private Bitset<Habitat> forestHabitat = new Bitset<Habitat>(Habitat.FOREST,Habitat.VARIED);
	
	public boolean hasWaterHabitat() {
		return habitat.test(waterHabitat);
	}
	public boolean hasGroundHabitat() {
		return habitat.test(groundHabitat);
	}
	public boolean hasGrassHabitat() {
		return habitat.test(grassHabitat);
	}
	public boolean hasForestHabitat() {
		return habitat.test(forestHabitat);
	}
}
