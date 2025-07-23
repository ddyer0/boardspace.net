package bugs.data;

import java.util.Hashtable;

import com.codename1.ui.Font;

import bridge.SystemFont;
import bugs.Goal;
import bugs.BugCard;
import bugs.BugsConstants;
import lib.G;
import lib.Graphics;
import lib.Image;
import lib.TextContainer;

public class Taxonomy extends DataHelper<Taxonomy> implements KeyProvider,Goal,BugsConstants
{	public int uid;
	public int getUid() { return uid+TAX_OFFSET;}
    public String name;
    public String getKey() { return name; }
    public Rank rank = Rank.UNKNOWN;
    public String parent;
    public String description;
    public String getDescription() { return description; }
    public String shortDescription;
    public String getShortDescription() { return shortDescription; }

    public String commonName;
    public String getCommonName() { return commonName==null ? "(no common name)" : commonName;}
    public String getScientificName() { return rank.name().toLowerCase() + " : "+name; }
    public String lastModified;
    public int speciesCount;
    public int split1;
    public int split1Count = 0;
    public static int largestCategorySize = 0;
    public static Taxonomy largestCategory = null;
    public Profile getProfile() { return Profile.get(name); }
    public String serialize() {
        return G.concat("\t",
        	escape(""+uid),
        	escape(name),
            escape(rank.name()),
            escape(parent),
            escape(description),
            escape(commonName),
            String.valueOf(speciesCount),
            String.valueOf(split1),
            escape(shortDescription)
        );
    }

    public Taxonomy deserialize(String fields[]) {
 
    	Taxonomy entry = new Taxonomy();
        
        //name	rank	parent	description	last_modified	common_name	species_count	split1
        entry.uid = Integer.parseInt(fields[0]);
        entry.name = unescape(fields[1]);
        entry.rank = Rank.valueOf(unescape(fields[2].toUpperCase()));
        entry.parent = unescape(fields[3]);
        entry.description = unescape(fields[4]);
        entry.lastModified = unescape(fields[5]);
        entry.commonName = unescape(fields[6]);
        entry.speciesCount = Integer.parseInt(fields[7]);
        entry.split1 = Integer.parseInt(fields[8]);
        entry.split1Count = entry.speciesCount;
        entry.shortDescription = unescape(fields[9]);
        return entry;
    }
    
    // constuct an artificial node for animalia to be "everything else"
    public static void makeDefaultProfile()
    {
       	Taxonomy animalia = get(DefaultProfile);
    	int sum = 0;
    	int max = 0;
    	Taxonomy large = null;
    	for(Taxonomy a : taxonomies.values())
    	{
    		if(a != animalia)
    		{
    			if(a.split1==1)
    			{	if(a.split1Count>max)
    				{ max = a.split1Count;
    				  large = a;
    				}
    			sum += a.split1Count;
    			//G.print(a,a.speciesCount);
    			}
    		}
    	}
    	largestCategorySize = max;
    	largestCategory = large;
    	animalia.split1Count = animalia.speciesCount-sum;
    	animalia.split1 = 1;
    	//G.print(animalia,animalia.split1Count);
    }
    
    public static Hashtable<String,Taxonomy>  taxonomies = null;
    public static void load(String f)
    {	if(taxonomies==null)
    	{
    	taxonomies = new Taxonomy().readFromFile(f);
    	
    	makeDefaultProfile();
    	
    	}
    }
    public String toString() {
        return "<taxo "+name+">";  
    }
    static private Taxonomy animals = null;
    public static Taxonomy getWildType()
    {	if(animals==null) { animals = Taxonomy.get("Animalia");}
    	return animals;
    }
    
    public static Taxonomy get(String n) { return taxonomies.get(n); }

	public void drawExtendedChip(Graphics gc,Font baseFont,int xp,int yp,int w,int h,boolean x2)
	{
		Profile profile = getProfile();
		String sd = profile.getDescription();
		int margin = h/20;
		TextContainer textContainer = new TextContainer(BugsId.Description);
		if(sd!=null)
		{	
			textContainer.setBounds(xp+margin,yp+margin,w-margin*2,h-margin*2);
			textContainer.setText(sd);
			textContainer.setFont(SystemFont.getFont(baseFont,w/5));
			textContainer.selectFontSize();
			textContainer.flagExtensionLines = false;
			textContainer.frameAndFill=false;
			textContainer.setVisible(true);
			textContainer.redrawBoard(gc,null);
		}
	}
	public String getHelpText()
	{
		return getCommonName()+"\n"+getScientificName();
	}
	public Image getIllustrationImage()
	{
		Profile prof1 = getProfile();
		return prof1.illustrationImage;
	}
	public double pointValue(BugCard card) 
	{
		return card.pointValue()*BONUS_MULTIPLIER;
	}
	// below the image, code what it scores
	public String legend(boolean twice)
	{
		return "" + (int)(BONUS_MULTIPLIER*(twice ? 2 : 1))+"x"	;
	}
	public boolean matches(BugCard bug,boolean wild)
	{	Profile bugProf = bug.getProfile();
		Taxonomy bugGroup = bugProf.getCategory();
		return (this==bugGroup) || (wild && bugGroup==Taxonomy.getWildType());
	}
}
