package bugs.data;

import java.util.Hashtable;

public class MasterSpecies extends DataHelper<MasterSpecies> implements KeyProvider
{
    public int uid;
    public int getUid() { return uid+SPEC_OFFSET; }
    public String name;
    public String getScientificName() { return name; }
    public String getKey() { return name; }
    public String description;
    public String getDescription() { return description; }
    public String shortDescription;
    public String getShortDescription() { return shortDescription; }
    public Profile getProfile() { return Profile.get(name); }
    public String commonName;
    public String getCommonName() { return commonName==null ? "(no common name)" : commonName;}
    public String lastUpdated;
    public String family;
    public String serialize() {
        return String.join("\t",
            String.valueOf(uid),
            escape(name),
            escape(description),
            escape(commonName),
            escape(lastUpdated),
            escape(family),
            escape(shortDescription)
        );
    }

    public MasterSpecies deserialize(String fields[]) {
        MasterSpecies m = new MasterSpecies();
        m.uid = Integer.parseInt(fields[0]);
        m.name = unescape(fields[1]);
        // ignore q
        m.description = unescape(fields[3]);
        m.commonName = unescape(fields[4]);
        m.lastUpdated = unescape(fields[5]);
        m.family = unescape(fields[6]);
        m.shortDescription = unescape(fields[7]);
        return m;
    }

    public String toString() {
        return "<species "+name+">";  
    }
    public static Hashtable<String,MasterSpecies> species = null;
    public static void load(String file)
    {
    	if(species==null)
    	{	species = new MasterSpecies().readFromFile(file);
    	}
    }
    public static MasterSpecies get(String n) { return species.get(n); }
}
