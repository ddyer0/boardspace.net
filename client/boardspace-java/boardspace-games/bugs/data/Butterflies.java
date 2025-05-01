package bugs.data;

import java.util.*;

public class Butterflies {

    public static class ButterflySpecies {
        public final String commonName;
        public final String scientificName;
        public final String description;
        public final InvertebrateProfile profile;

        public ButterflySpecies(String commonName, String scientificName, String description, InvertebrateProfile profile) {
            this.commonName = commonName;
            this.scientificName = scientificName;
            this.description = description;
            this.profile = profile;
        }
    }

    public static final List<ButterflySpecies> ALL_BUTTERFLIES = new ArrayList<>();

    public static final ButterflySpecies WHITE_PEACOCK;
    public static final ButterflySpecies PURPLE_EMPEROR;
    public static final ButterflySpecies MAP_BUTTERFLY;
    public static final ButterflySpecies SILVER_WASHED_FRITILLARY;
    public static final ButterflySpecies ATLAS_MOTH;
    public static final ButterflySpecies BLUE_MORPHO;
    public static final ButterflySpecies OWL_BUTTERFLY;
    public static final ButterflySpecies LEOPARD_LACEWING;
    public static final ButterflySpecies CLOUDED_YELLOW;
    public static final ButterflySpecies COMMON_GRASS_YELLOW;
    public static final ButterflySpecies GLASSWING;
    public static final ButterflySpecies GREAT_EGGFLY;
    public static final ButterflySpecies GREEN_HAIRSTREAK;
    public static final ButterflySpecies PAPER_KITE;
    public static final ButterflySpecies INDIAN_MOON_MOTH;
    public static final ButterflySpecies JULIA_BUTTERFLY;
    public static final ButterflySpecies COMMON_BUCKEYE;
    public static final ButterflySpecies SMALL_COPPER;
    public static final ButterflySpecies MALACHITE;
    public static final ButterflySpecies MONARCH;
    public static final ButterflySpecies MOUNTAIN_APOLLO;
    public static final ButterflySpecies ORANGE_OAKLEAF;
    public static final ButterflySpecies PAINTED_LADY;
    public static final ButterflySpecies PEACOCK;
    public static final ButterflySpecies LARGE_WHITE;
    public static final ButterflySpecies POSTMAN;
    public static final ButterflySpecies RED_ADMIRAL;
    public static final ButterflySpecies SMALL_TORTOISESHELL;
    public static final ButterflySpecies SWALLOWTAIL;
    public static final ButterflySpecies ULYSSES;
    public static final ButterflySpecies ZEBRA_LONGWING;
    public static final ButterflySpecies SOUTHERN_FESTOON;

    static {
        WHITE_PEACOCK = createSpecies("White Peacock", "Anartia jatrophae", "Recognizable by white wings with orange and brown markings.");
        PURPLE_EMPEROR = createSpecies("Purple Emperor", "Apatura iris", "A striking woodland butterfly with iridescent purple wings.");
        MAP_BUTTERFLY = createSpecies("Map Butterfly", "Araschnia levana", "Named for its wing patterns resembling a map.");
        SILVER_WASHED_FRITILLARY = createSpecies("Silver-washed Fritillary", "Argynnis paphia", "Large fritillary with silver streaks under the wings.");
        ATLAS_MOTH = createSpecies("Atlas Moth", "Attacus atlas", "One of the world’s largest moths with huge, patterned wings.");
        BLUE_MORPHO = createSpecies("Blue Morpho Butterfly", "Morpho menelaus", "Famous for its iridescent blue wings and rainforest habitat.");
        OWL_BUTTERFLY = createSpecies("Owl Butterfly", "Caligo memnon", "Large butterfly with eye spots that mimic owl eyes.");
        LEOPARD_LACEWING = createSpecies("Leopard Lacewing", "Cethosia cyane", "Brightly colored with a leopard-like wing pattern.");
        CLOUDED_YELLOW = createSpecies("Clouded Yellow Butterfly", "Colias croceus", "Common migratory butterfly with bright yellow wings.");
        COMMON_GRASS_YELLOW = createSpecies("Common Grass Yellow", "Eurema hecabe", "Widespread yellow butterfly seen fluttering near grasses.");
        GLASSWING = createSpecies("Glasswing Butterfly", "Greta oto", "Notable for its transparent wings that help it evade predators.");
        GREAT_EGGFLY = createSpecies("Great Eggfly", "Hypolimnas bolina", "A fast-flying butterfly with prominent white wing spots.");
        GREEN_HAIRSTREAK = createSpecies("Green Hairstreak", "Callophrys rubi", "Bright green underwings help it camouflage in foliage.");
        PAPER_KITE = createSpecies("Paper Kite", "Idea leuconoe", "Graceful flyer with black-and-white wings often seen in butterfly houses.");
        INDIAN_MOON_MOTH = createSpecies("Indian Moon Moth", "Actias selene", "A green moth with long tails and pale moon-like markings.");
        JULIA_BUTTERFLY = createSpecies("Julia Butterfly", "Dryas iulia", "Orange butterfly with elongated wings common in gardens.");
        COMMON_BUCKEYE = createSpecies("Common Buckeye", "Junonia coenia", "Features eye spots that deter predators.");
        SMALL_COPPER = createSpecies("Small Copper", "Lycaena phlaeas", "Tiny butterfly with vibrant copper-colored wings.");
        MALACHITE = createSpecies("Malachite Butterfly", "Siproeta stelenes", "Green and black butterfly found in tropical forests.");
        MONARCH = createSpecies("Monarch Butterfly", "Danaus plexippus", "Known for its incredible long-distance migration and orange and black wings.");
        MOUNTAIN_APOLLO = createSpecies("Mountain Apollo", "Parnassius apollo", "Lives in mountainous regions and has white wings with red spots.");
        ORANGE_OAKLEAF = createSpecies("Orange Oakleaf", "Kallima inachus", "Camouflages as a dead leaf when wings are closed.");
        PAINTED_LADY = createSpecies("Painted Lady Butterfly", "Vanessa cardui", "A cosmopolitan butterfly known for its migratory behavior.");
        PEACOCK = createSpecies("Peacock Butterfly", "Aglais io", "Large eye spots on wings resemble a peacock’s tail.");
        LARGE_WHITE = createSpecies("Large White", "Pieris brassicae", "Also called cabbage white; a common garden visitor.");
        POSTMAN = createSpecies("Postman Butterfly", "Heliconius melpomene", "Feeds on passionflower and has bright red bands.");
        RED_ADMIRAL = createSpecies("Red Admiral", "Vanessa atalanta", "Dark wings with red bands; known for territorial behavior.");
        SMALL_TORTOISESHELL = createSpecies("Small Tortoiseshell", "Aglais urticae", "Common in Europe; bright orange with black spots.");
        SWALLOWTAIL = createSpecies("Swallowtail Butterfly", "Papilio machaon", "Recognized by its tail-like extensions and global distribution.");
        ULYSSES = createSpecies("Ulysses Butterfly", "Papilio ulysses", "Brilliant blue butterfly native to northeastern Australia.");
        ZEBRA_LONGWING = createSpecies("Zebra Longwing Butterfly", "Heliconius charithonia", "Long black wings with white stripes resembling a zebra.");
        SOUTHERN_FESTOON = createSpecies("Southern Festoon", "Zerynthia polyxena", "Beautiful butterfly with ornate, festooned wings.");

        Collections.addAll(ALL_BUTTERFLIES,
            WHITE_PEACOCK, PURPLE_EMPEROR, MAP_BUTTERFLY, SILVER_WASHED_FRITILLARY,
            ATLAS_MOTH, BLUE_MORPHO, OWL_BUTTERFLY, LEOPARD_LACEWING,
            CLOUDED_YELLOW, COMMON_GRASS_YELLOW, GLASSWING, GREAT_EGGFLY,
            GREEN_HAIRSTREAK, PAPER_KITE, INDIAN_MOON_MOTH, JULIA_BUTTERFLY,
            COMMON_BUCKEYE, SMALL_COPPER, MALACHITE, MONARCH, MOUNTAIN_APOLLO,
            ORANGE_OAKLEAF, PAINTED_LADY, PEACOCK, LARGE_WHITE, POSTMAN, RED_ADMIRAL,
            SMALL_TORTOISESHELL, SWALLOWTAIL, ULYSSES, ZEBRA_LONGWING, SOUTHERN_FESTOON
        );
    }

    private static ButterflySpecies createSpecies(String commonName, String scientificName, String description) {
        InvertebrateProfile p = new InvertebrateProfile();
        p.categories.add(InvertebrateProfile.InvertebrateCategory.SOCIAL_STRUCTURE);
        p.metamorphosis_type = InvertebrateProfile.MetamorphosisType.COMPLETE;
        p.larvae_type = InvertebrateProfile.LarvaeType.CATERPILLAR;
        p.body_symmetry = InvertebrateProfile.BodySymmetry.BILATERAL;
        p.body_shape = InvertebrateProfile.BodyShape.SEGMENTED;
        p.exoskeleton_type = InvertebrateProfile.ExoskeletonType.CHITINOUS;
        p.feeding_apparatus_type = InvertebrateProfile.FeedingApparatusType.PROBOSCIS;
        p.locomotion_mode = InvertebrateProfile.LocomotionMode.FLYING;
        p.trophic_level = InvertebrateProfile.TrophicLevel.PRIMARY_CONSUMER;
        p.reproductive_cycle = InvertebrateProfile.ReproductiveCycle.SEASONAL;
        p.genetic_diversity = InvertebrateProfile.GeneticDiversity.MEDIUM;
        p.warning_coloration = InvertebrateProfile.WarningColoration.PRESENT;
        p.toxic_to_humans = InvertebrateProfile.ToxicToHumans.NON_TOXIC;
        p.exoskeleton_strength = InvertebrateProfile.ExoskeletonStrength.HARDENED;
        p.olfactory_capacity = InvertebrateProfile.OlfactoryCapacity.MODERATE;
        p.light_sensitivity = InvertebrateProfile.LightSensitivity.HIGH;
        p.sound_reception = InvertebrateProfile.SoundReception.LOW;
        p.uv_sensitivity = InvertebrateProfile.UvSensitivity.STRONG;
        p.environmental_plasticity = InvertebrateProfile.EnvironmentalPlasticity.MODERATE;
        return new ButterflySpecies(commonName, scientificName, description, p);
    }
}
