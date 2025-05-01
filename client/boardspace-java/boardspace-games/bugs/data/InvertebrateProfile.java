package bugs.data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

public class InvertebrateProfile {
	int uid;
	String directory = "";
	String name = "unknown";
	Rank rank = Rank.Unknown;
	String commonName = "unknown";
	String category = "unknown";
	Hashtable<Semantics,String> notes = new Hashtable<Semantics,String>();
	public enum Semantics implements Serializable {
	    uid("Unique identifier for this species"),
	    directory("Directory or file path associated with this entry"),
	    name("taxonomic name of this profile"),
	    rank("taxonomic rank of this profile"),
	    commonName("Widely used common name"),
	    category("Primary classification category"),
	    categories("Set of relevant invertebrate categories"),
	    metamorphosis_type("Type of metamorphosis during development"),
	    larvae_type("Form taken by larval stage"),
	    body_symmetry("Arrangement of body parts around a central axis"),
	    body_shape("Overall shape or configuration of the body"),
	    feeding_timing("Typical time of day when feeding occurs"),
	    exoskeleton_type("Composition of the exoskeleton"),
	    exoskeleton_color("Typical coloration of the exoskeleton"),
	    feeding_apparatus_type("Morphology of mouthparts or feeding structures"),
	    locomotion_mode("Primary mode of movement"),
	    trophic_level("Position in the food chain"),
	    reproductive_cycle("Timing and nature of reproductive cycles"),
	    genetic_diversity("Variation in the gene pool of the species"),
	    invasion_risk("Potential for invasive behavior in new environments"),
	    pesticide_resistance("Degree of resistance to chemical pesticides"),
	    toxic_to_humans("Degree of toxicity to humans"),
	    economic_impact("Impact on human economies (positive or negative)"),
	    exoskeleton_strength("Physical durability of the exoskeleton"),
	    warning_coloration("Use of bright colors to deter predators"),
	    toxin_secretion("Ability to secrete toxic substances"),
	    venomous("Capability of injecting venom"),
	    olfactory_capacity("Sensitivity and range of chemical smell detection"),
	    tactile_sensing("Ability to detect touch or pressure"),
	    light_sensitivity("Sensitivity to ambient light"),
	    sound_reception("Ability to perceive sound or vibration"),
	    uv_sensitivity("Sensitivity to ultraviolet light"),
	    infrared_detection("Ability to detect infrared wavelengths or heat"),
	    environmental_plasticity("Adaptability to environmental changes"),
	    salinity_tolerance("Range of water salinities tolerated"),
	    temperature_tolerance("Range of environmental temperatures tolerated"),
	    humidity_tolerance("Range of air humidities tolerated"),
	    ph_tolerance("Range of pH conditions tolerated"),
	    altitude_range("Range of elevations the species inhabits"),
	    body_length("Typical body length measurements"),
	    body_mass("Typical body mass measurements"),
	    chromosome_number("Number of chromosomes in the genome"),
	    depth_range("Depths within aquatic environments the species inhabits"),
	    development_time("Time required for development between stages"),
	    egg_size("Size of the eggs laid"),
	    embryonic_stage_duration("Duration of the embryonic stage"),
	    fecundity("Number of offspring produced per reproductive cycle"),
	    generation_time("Time between successive generations"),
	    genome_size("Total size of the genome in base pairs"),
	    life_span("Expected or observed duration of life"),
	    number_of_eggs("Range of eggs laid at one time"),
	    number_of_limbs("Number of functional limbs"),
	    number_of_segments("Number of distinct body segments"),
	    speed_range("Typical range of movement speeds");

	    public final String definition;

	    Semantics(String definition) {
	        this.definition = definition;
	    }
	}
	enum Rank { Unknown, Genus, Species, GenusAndSpecies, Family, Order,Class, Phylum, Kingdom, Domain};
	
	public enum Semantics2 implements Serializable {
	    uid,
	    directory,
	    species,
	    commonName,
	    category,
	    categories,
	    metamorphosis_type,
	    larvae_type,
	    body_symmetry,
	    body_shape,
	    feeding_timing,
	    exoskeleton_type,
	    exoskeleton_color,
	    feeding_apparatus_type,
	    locomotion_mode,
	    trophic_level,
	    reproductive_cycle,
	    genetic_diversity,
	    invasion_risk,
	    pesticide_resistance,
	    toxic_to_humans,
	    economic_impact,
	    exoskeleton_strength,
	    warning_coloration,
	    toxin_secretion,
	    venomous,
	    olfactory_capacity,
	    tactile_sensing,
	    light_sensitivity,
	    sound_reception,
	    uv_sensitivity,
	    infrared_detection,
	    environmental_plasticity,
	    salinity_tolerance,
	    temperature_tolerance,
	    humidity_tolerance,
	    ph_tolerance,
	    altitude_range,
	    body_length,
	    body_mass,
	    chromosome_number,
	    depth_range,
	    development_time,
	    egg_size,
	    embryonic_stage_duration,
	    fecundity,
	    generation_time,
	    genome_size,
	    life_span,
	    number_of_eggs,
	    number_of_limbs,
	    number_of_segments,
	    speed_range
	}
	enum Units 
	{
		UNKNOWN, MILLIMETERS, CENTIMETERS, DEGREESC, PH, COUNT, METERS, GRAMS, DAYS, PERCENT, MILLIMETERSPERSECOND
	}
    enum XB {
        UNKNOWN,
        N_A,
        OTHER,
        NO,
        YES
    }

    enum ToxinSecretion {
    	UNKNOWN,
        N_A,
        NONE,
        MILD,
        STRONG
    }

    enum Venomous {
    	UNKNOWN,
        N_A,
        NON_VENOMOUS,
        WEAKLY_VENOMOUS,
        HIGHLY_VENOMOUS
    }

    enum OlfactoryCapacity {
    	UNKNOWN,
        N_A,
        WEAK,
        MODERATE,
        ACUTE
    }

    enum TactileSensing {
    	UNKNOWN,
        N_A,
        LOW,
        MEDIUM,
        HIGH
    }

    enum LightSensitivity {
    	UNKNOWN,
        N_A,
        LOW,
        MEDIUM,
        HIGH
    }

    enum SoundReception {
    	UNKNOWN,
        N_A,
        LOW,
        MEDIUM,
        HIGH
    }

    enum UvSensitivity {
    	UNKNOWN,
        N_A,
        NONE,
        MILD,
        STRONG
    }

    enum InfraredDetection {
    	UNKNOWN,
        N_A,
        ABSENT,
        LIMITED,
        PRECISE
    }

    enum SalinityTolerance {
    	UNKNOWN,
        N_A,
        FRESHWATER_ONLY,
        BRACKISH,
        MARINE
    }

    enum EnvironmentalPlasticity {
    	UNKNOWN,
        N_A,
        LOW,
        MODERATE,
        HIGH
    }

    static class AltitudeRange {
    	Units units = Units.METERS;
        public double min;
        public double max;
        public XB isNA = XB.UNKNOWN;

        public AltitudeRange(double min, double max) {
            this.min = min;
            this.max = max;
            isNA = XB.YES;
        }

        public AltitudeRange() {}
    }

    static class BodyLength {
    	Units units = Units.MILLIMETERS;
        public double min;
        public double max;
        public XB isNA = XB.UNKNOWN;

        public BodyLength(double min, double max) {
            this.min = min;
            this.max = max;
            isNA = XB.YES;
        }

        public BodyLength() {}
    }

    static class BodyMass {
    	Units units = Units.GRAMS;
        public double min;
        public double max;
        public XB isNA = XB.UNKNOWN;

        public BodyMass(double min, double max) {
            this.min = min;
            this.max = max;
            isNA = XB.YES;
        }

        public BodyMass() {}
    }

    static class ChromosomeNumber {
    	Units units = Units.COUNT;
        public int number;
        public XB isNA = XB.UNKNOWN;

        public ChromosomeNumber(int min) {
            this.number = min;
            isNA = XB.YES;
        }

        public ChromosomeNumber() {}
    }

    static class DepthRange {
    	Units units = Units.METERS;
        public double min;
        public double max;
        public XB isNA = XB.UNKNOWN;

        public DepthRange(double min, double max) {
            this.min = min;
            this.max = max;
            isNA = XB.YES;
        }

        public DepthRange() {}
    }

    static class DevelopmentTime {
    	Units units = Units.DAYS;
        public double min;
        public double max;
        public XB isNA = XB.UNKNOWN;

        public DevelopmentTime(double min, double max) {
            this.min = min;
            this.max = max;
            isNA = XB.YES;
        }

        public DevelopmentTime() {}
    }

    static class EggSize {
    	Units units = Units.MILLIMETERS;
        public double min;
        public double max;
        public XB isNA = XB.UNKNOWN;

        public EggSize(double min, double max) {
            this.min = min;
            this.max = max;
            isNA = XB.YES;
        }

        public EggSize() {}
    }

    static class EmbryonicStageDuration {
    	Units units = Units.DAYS;
        public double min;
        public double max;
        public XB isNA = XB.UNKNOWN;

        public EmbryonicStageDuration(double min, double max) {
            this.min = min;
            this.max = max;
            isNA = XB.YES;
        }

        public EmbryonicStageDuration() {}
    }

    static class Fecundity {
    	// units are offspring per generation
    	Units units = Units.COUNT;
        public double min;
        public double max;
        public XB isNA = XB.UNKNOWN;

        public Fecundity(double min, double max) {
            this.min = min;
            this.max = max;
            isNA = XB.YES;
        }

        public Fecundity() {}
    }

    static class GenerationTime {
    	Units units = Units.DAYS;
        public double min;
        public double max;
        public XB isNA = XB.UNKNOWN;

        public GenerationTime(double min, double max) {
            this.min = min;
            this.max = max;
            isNA = XB.YES;
        }

        public GenerationTime() {}
    }

    static class GenomeSize {
    	Units units = Units.COUNT;
        public double min;
        public double max;
        public XB isNA = XB.UNKNOWN;

        public GenomeSize(double min, double max) {
            this.min = min;
            this.max = max;
            isNA = XB.YES;
        }

        public GenomeSize() {}
    }

    static class HumidityTolerance {
    	Units units = Units.PERCENT;
        public double min;
        public double max;
        public XB isNA = XB.UNKNOWN;

        public HumidityTolerance(double min, double max) {
            this.min = min;
            this.max = max;
            isNA = XB.YES;
        }

        public HumidityTolerance() {}
    }

    static class LifeSpan {
    	Units units = Units.DAYS;
        public double min;
        public double max;
        public XB isNA = XB.UNKNOWN;

        public LifeSpan(double min, double max) {
            this.min = min;
            this.max = max;
            isNA = XB.YES;
        }

        public LifeSpan() {}
    }

    static class NumberOfEggs {
    	Units units = Units.COUNT;
        public int min;
        public int max;
        public XB isNA = XB.UNKNOWN;

        public NumberOfEggs(int min, int max) {
            this.min = min;
            this.max = max;
            isNA = XB.YES;
        }

        public NumberOfEggs() {}
    }

    static class NumberOfLimbs {
    	Units units = Units.COUNT;
        public int min;
        public int max;
        public XB isNA = XB.UNKNOWN;

        public NumberOfLimbs(int min, int max) {
            this.min = min;
            this.max = max;
            isNA = XB.YES;
        }

        public NumberOfLimbs() {}
    }

    static class NumberOfSegments {
    	Units units = Units.COUNT;
        public int min;
        public int max;
        public XB isNA = XB.UNKNOWN;

        public NumberOfSegments(int min, int max) {
            this.min = min;
            this.max = max;
            isNA = XB.YES;
        }

        public NumberOfSegments() {}
    }

    static class PhTolerance {
    	Units units = Units.PH;
        public double min;
        public double max;
        public XB isNA = XB.UNKNOWN;

        public PhTolerance(double min, double max) {
            this.min = min;
            this.max = max;
            isNA = XB.YES;
        }

        public PhTolerance() {}
    }

    static class SpeedRange {
    	Units units = Units.MILLIMETERSPERSECOND;
        public double min;
        public double max;
        public XB isNA = XB.UNKNOWN;

        public SpeedRange(double min, double max) {
            this.min = min;
            this.max = max;
            isNA = XB.YES;
        }

        public SpeedRange() {}
    }

    static class TemperatureTolerance {
    	Units units = Units.DEGREESC;
        public double min;
        public double max;
        public XB isNA = XB.UNKNOWN;

        public TemperatureTolerance(double min, double max) {
            this.min = min;
            this.max = max;
            isNA = XB.YES;
        }

        public TemperatureTolerance() {}
    }

	enum MetamorphosisType {
		UNKNOWN,
	    N_A,
	    NONE,
	    INCOMPLETE,
	    COMPLETE
	}

	enum LarvaeType {
		UNKNOWN,
	    N_A,
	    GRUB,
	    MAGGOT,
	    CATERPILLAR,
	    NYMPH,
	    NAIAD,
	    OTHER
	}

	enum BodySymmetry {
		UNKNOWN,
	    N_A,
	    RADIAL,
	    BILATERAL,
	    ASYMMETRICAL
	}

	enum BodyShape {
		UNKNOWN,
	    N_A,
	    ELONGATED,
	    FLATTENED,
	    SPHERICAL,
	    SEGMENTED,
	    IRREGULAR
	}

	enum FeedingTiming {
		UNKNOWN,
	    N_A,
	    DIURNAL,
	    NOCTURNAL,
	    CREPUSCULAR,
	    VARIABLE
	}

	enum ExoskeletonType {
		UNKNOWN,
	    N_A,
	    CHITINOUS,
	    CALCAREOUS,
	    MIXED
	}

	enum ExoskeletonColor {
		UNKNOWN,
	    N_A,
	    DARK,
	    LIGHT,
	    TRANSLUCENT,
	    MULTICOLORED,
	    MIMETIC, BROWN
	}

	enum FeedingApparatusType {
		UNKNOWN,
	    N_A,
	    MANDIBLES,
	    PROBOSCIS,
	    SIPHON,
	    PIERCING_SUCTORIAL,
	    FILTERING,
	    OTHER
	}

	enum LocomotionMode {
		UNKNOWN,
	    N_A,
	    CRAWLING,
	    SWIMMING,
	    FLYING,
	    BURROWING,
	    JET_PROPULSION,
	    SESSILE,
	    OTHER
	}

	enum TrophicLevel {
		UNKNOWN,
	    N_A,
	    PRIMARY_CONSUMER,
	    SECONDARY_CONSUMER,
	    TERTIARY_CONSUMER,
	    OMNIVORE,
	    SCAVENGER,
	    FILTER_FEEDER
	}

	enum ReproductiveCycle {
		UNKNOWN,
	    N_A,
	    ANNUAL,
	    SEASONAL,
	    CONTINUOUS,
	    SEMELPAROUS,
	    ITEROPAROUS
	}

	enum GeneticDiversity {
		UNKNOWN,
	    N_A,
	    LOW,
	    MEDIUM,
	    HIGH
	}

	enum ExoskeletonStrength {
		UNKNOWN,
	    N_A,
	    SOFT,
	    FLEXIBLE,
	    HARDENED,
	    ARMORED
	}

	enum WarningColoration {
		UNKNOWN,
	    N_A,
	    ABSENT,
	    PRESENT,
	    SUBTLE,
	    INTENSE
	}

	enum InvasionRisk {
		UNKNOWN,
	    N_A,
	    LOW,
	    MEDIUM,
	    HIGH
	}

	enum PesticideResistance {
		UNKNOWN,
	    N_A,
	    NONE,
	    MODERATE,
	    HIGH, LOW
	}

	enum ToxicToHumans {
		UNKNOWN,
	    N_A,
	    NON_TOXIC,
	    MILDLY_TOXIC,
	    HIGHLY_TOXIC
	}

	enum EconomicImpact {
		UNKNOWN,
	    N_A,
	    NEGATIVE,
	    NEUTRAL,
	    POSITIVE
	}
	enum InvertebrateCategory {
	    LARVAL_STAGE_PRESENCE("Indicates if the invertebrate has a distinct larval stage before adulthood."),
	    PUPAL_STAGE_PRESENCE("True if the species enters a pupal stage during metamorphosis."),
	    ASEXUAL_REPRODUCTION("Capable of reproducing without a mate, often via budding, fragmentation, or parthenogenesis."),
	    SEXUAL_REPRODUCTION("Reproduces through the combination of genetic material from two different sexes."),
	    HERMAPHRODITISM("Possesses both male and female reproductive organs, potentially capable of self-fertilization."),
	    PARTHENOGENESIS("A form of asexual reproduction where offspring develop from unfertilized eggs."),
	    EGG_LAYING("Reproductive strategy where embryos develop outside the mother’s body in laid eggs."),
	    LIVE_BIRTH("Gives birth to live offspring rather than laying eggs."),
	    BROOD_CARE("Exhibits behaviors to protect and nurture offspring post-egg-laying or birth."),
	    SEASONAL_REPRODUCTION("Reproduction that occurs in specific seasons or climatic conditions."),
	    TERRESTRIAL("Lives primarily on land, including soil, forests, deserts, and urban environments."),
	    FRESHWATER("Inhabits non-saline water bodies such as lakes, rivers, and ponds."),
	    MARINE("Lives in saltwater environments such as oceans, seas, and tidal zones."),
	    INTERTIDAL("Inhabits coastal areas between high and low tide marks."),
	    ARBOREAL("Lives in or among trees and foliage above ground level."),
	    SUBTERRANEAN("Dwells underground or within the soil layer."),
	    ENDOPARASITIC("Lives inside the body of a host organism and derives nutrients at the host’s expense."),
	    ECTOPARASITIC("Lives on the external surface of a host organism."),
	    HOST_SPECIFICITY("Indicates how selective a parasite is in choosing its host species."),
	    HABITAT_RANGE("Breadth of habitat types that the organism can occupy."),
	    SOIL_TYPE_PREFERENCE("Favors particular types of soil (sandy, clay, loamy, etc.)."),
	    NESTING_SUBSTRATE("Preferred material or environment for nest construction."),
	    MIGRATORY_HABITAT("Habitats used during seasonal or life-stage-related migrations."),
	    HERBIVORE("Feeds primarily on plant matter."),
	    CARNIVORE("Feeds on other animals."),
	    OMNIVORE("Consumes both plant and animal matter."),
	    DETRITIVORE("Feeds on decomposing organic matter."),
	    FILTER_FEEDER("Strains food particles from water using specialized feeding structures."),
	    PARASITE("Lives on or in another organism, harming the host to benefit itself."),
	    SCAVENGER("Consumes dead organisms or organic refuse."),
	    BLOOD_FEEDER("Feeds on the blood of other animals (hematophagy)."),
	    FUNGIVORE("Consumes fungi, including molds and mushrooms."),
	    NECTAR_FEEDER("Feeds on floral nectar, often aiding in pollination."),
	    PREDATORY_BEHAVIOR("Actively hunts, captures, and consumes live prey."),
	    AMBUSH_PREDATOR("Lies in wait to surprise and capture prey."),
	    GRAZING_BEHAVIOR("Feeds on low-lying, stationary food sources, often vegetation."),
	    INTERNAL_DIGESTION("Digests food internally after ingestion."),
	    EXTERNAL_DIGESTION("Secretes enzymes outside the body to liquefy food before ingestion."),
	    SYMBIOTIC_FEEDING("Relies on symbiotic relationships to obtain nutrients."),
	    ALGAL_FEEDER("Consumes algae as a primary food source."),
	    FOOD_STORAGE("Ability to store food internally or externally for future use."),
	    MICROSCOPIC("Indicates presence or absence of microscopic."),
	    MINUTE("Indicates presence or absence of minute."),
	    SMALL("Indicates presence or absence of small."),
	    MEDIUM("Indicates presence or absence of medium."),
	    LARGE("Indicates presence or absence of large."),
	    SEGMENTED_BODY("Indicates presence or absence of segmented body."),
	    APPENDAGE_TYPE("Indicates presence or absence of appendage type."),
	    SHELL_PRESENCE("Indicates presence or absence of shell presence."),
	    MOUTH_PARTS_TYPE("Indicates presence or absence of mouth parts type."),
	    LIMB_SPECIALIZATION("Indicates presence or absence of limb specialization."),
	    BODY_REGION_DIFFERENTIATION("Indicates presence or absence of body region differentiation."),
	    SIZE_VARIABILITY("Indicates presence or absence of size variability."),
	    COMPOUND_EYES("Indicates presence or absence of compound eyes."),
	    SIMPLE_EYES("Indicates presence or absence of simple eyes."),
	    ANTENNAE("Indicates presence or absence of antennae."),
	    CHEMORECEPTORS("Indicates presence or absence of chemoreceptors."),
	    ECHOLOCATION("Indicates presence or absence of echolocation."),
	    BIOLUMINESCENCE("Indicates presence or absence of bioluminescence."),
	    CENTRALIZED_BRAIN("Indicates presence or absence of centralized brain."),
	    NERVE_NET("Indicates presence or absence of nerve net."),
	    VIBRATION_SENSING("Indicates presence or absence of vibration sensing."),
	    HEAT_SENSING("Indicates presence or absence of heat sensing."),
	    MECHANORECEPTORS("Indicates presence or absence of mechanoreceptors."),
	    BALANCE_ORGANS("Indicates presence or absence of balance organs."),
	    MAGNETORECEPTION("Indicates presence or absence of magnetoreception."),
	    SENSORY_APPENDAGES("Indicates presence or absence of sensory appendages."),
	    SESSILE("Indicates presence or absence of sessile."),
	    SWIMMING("Indicates presence or absence of swimming."),
	    BURROWING("Indicates presence or absence of burrowing."),
	    FLYING("Indicates presence or absence of flying."),
	    GLIDING("Indicates presence or absence of gliding."),
	    CRAWLING("Indicates presence or absence of crawling."),
	    JET_PROPULSION("Indicates presence or absence of jet propulsion."),
	    CIRCADIAN_RHYTHMS("Indicates presence or absence of circadian rhythms."),
	    HIBERNATION("Indicates presence or absence of hibernation."),
	    MIGRATION("Indicates presence or absence of migration."),
	    SEASONAL_ACTIVITY("Indicates presence or absence of seasonal activity."),
	    CLIMBING_ABILITY("Indicates presence or absence of climbing ability."),
	    TUNNELING_BEHAVIOR("Indicates presence or absence of tunneling behavior."),
	    HOST_SEEKING_BEHAVIOR("Indicates presence or absence of host seeking behavior."),
	    AGGREGATION_BEHAVIOR("Indicates presence or absence of aggregation behavior."),
	    TERRITORIALITY("Indicates presence or absence of territoriality."),
	    SOCIAL_STRUCTURE("Indicates presence or absence of social structure."),
	    FORAGING_STRATEGY("Indicates presence or absence of foraging strategy."),
	    CAMOUFLAGE("Indicates presence or absence of camouflage."),
	    MIMICRY("Indicates presence or absence of mimicry."),
	    SHELLS("Indicates presence or absence of shells."),
	    STINGERS("Indicates presence or absence of stingers."),
	    INK_RELEASE("Indicates presence or absence of ink release."),
	    AUTOTOMY("Indicates presence or absence of autotomy."),
	    BURROW_ESCAPE("Indicates presence or absence of burrow escape."),
	    NOXIOUS_CHEMICALS("Indicates presence or absence of noxious chemicals."),
	    THREAT_POSTURE("Indicates presence or absence of threat posture."),
	    ARMOR_PLATES("Indicates presence or absence of armor plates."),
	    SPEED_ESCAPE("Indicates presence or absence of speed escape."),
	    DEATH_FEIGNING("Indicates presence or absence of death feigning."),
	    LIGHT_FLASHING("Indicates presence or absence of light flashing."),
	    COLONY_DEFENSE("Indicates presence or absence of colony defense."),
	    MECHANICAL_DEFENSES("Indicates presence or absence of mechanical defenses."),
	    DISTRACTION_DISPLAYS("Indicates presence or absence of distraction displays."),
	    POLLINATORS("Indicates presence or absence of pollinators."),
	    DECOMPOSERS("Indicates presence or absence of decomposers."),
	    SOIL_AERATORS("Indicates presence or absence of soil aerators."),
	    PREY_BASE("Indicates presence or absence of prey base."),
	    PEST_SPECIES("Indicates presence or absence of pest species."),
	    MUTUALISTIC("Indicates presence or absence of mutualistic."),
	    COMMENSAL("Indicates presence or absence of commensal."),
	    INVASIVE_SPECIES("Indicates presence or absence of invasive species."),
	    INDICATOR_SPECIES("Indicates presence or absence of indicator species."),
	    KEYSTONE_SPECIES("Indicates presence or absence of keystone species."),
	    NUTRIENT_RECYCLER("Indicates presence or absence of nutrient recycler."),
	    SYMBIOTIC_HOST("Indicates presence or absence of symbiotic host."),
	    BIOTURBATOR("Indicates presence or absence of bioturbator."),
	    HABITAT_ENGINEER("Indicates presence or absence of habitat engineer."),
	    PREDATOR_CONTROL("Indicates presence or absence of predator control."),
	    PRIMARY_CONSUMER("Indicates presence or absence of primary consumer."),
	    SECONDARY_CONSUMER("Indicates presence or absence of secondary consumer."),
	    POLLINATION_SYMBIOSIS("Indicates presence or absence of pollination symbiosis."),
	    CROP_DAMAGE_AGENT("Indicates presence or absence of crop damage agent."),
	    PHYLUM("Indicates presence or absence of phylum."),
	    CLASSIFICATION_CLASS("Indicates presence or absence of classification class."),
	    ORDER("Indicates presence or absence of order."),
	    SYMBIOTIC_CATEGORY("Indicates presence or absence of symbiotic category."),
	    CLADE_MEMBERSHIP("Indicates presence or absence of clade membership."),
	    EVOLUTIONARY_ADAPTATIONS("Indicates presence or absence of evolutionary adaptations."),
	    FOSSIL_RECORD("Indicates presence or absence of fossil record."),
	    MITOCHONDRIAL_DNA_TYPE("Indicates presence or absence of mitochondrial dna type."),
	    GENE_EXPRESSION("Indicates presence or absence of gene expression."),
	    CYTOCHROME_C_VARIATION("Indicates presence or absence of cytochrome c variation."),
	    PHYLOGENETIC_DISTANCE("Indicates presence or absence of phylogenetic distance."),
	    BARCODING_DNA("Indicates presence or absence of barcoding dna."),
	    ANCESTRAL_TRAITS("Indicates presence or absence of ancestral traits."),
	    TAXONOMIC_RANK("Indicates presence or absence of taxonomic rank."),
	    LINEAGE_SPLITS("Indicates presence or absence of lineage splits."),
	    DEVELOPMENTAL_PATHWAYS("Indicates presence or absence of developmental pathways."),
	    DISEASE_VECTOR("Indicates presence or absence of disease vector."),
	    CROP_PEST("Indicates presence or absence of crop pest."),
	    POLLINATION_PROVIDER("Indicates presence or absence of pollination provider."),
	    BIOCONTROL_AGENT("Indicates presence or absence of biocontrol agent."),
	    MEDICINAL_USE("Indicates presence or absence of medicinal use."),
	    FOOD_SOURCE("Indicates presence or absence of food source."),
	    ORNAMENTAL_USE("Indicates presence or absence of ornamental use."),
	    CULTURAL_SIGNIFICANCE("Indicates presence or absence of cultural significance."),
	    ENVIRONMENTAL_INDICATOR("Indicates presence or absence of environmental indicator."),
	    MODEL_ORGANISM("Indicates presence or absence of model organism."),
	    INVOLVED_IN_ALLERGIES("Indicates presence or absence of involved in allergies."),
	    INDUSTRIAL_USE("Indicates presence or absence of industrial use."),
	    PHARMACEUTICAL_RESOURCE("Indicates presence or absence of pharmaceutical resource."),
	    SCIENTIFIC_RESEARCH_USE("Indicates presence or absence of scientific research use."),
	    EDUCATIONAL_USE("Indicates presence or absence of educational use."),
	    AQUACULTURE_RELEVANCE("Indicates presence or absence of aquaculture relevance."),

	    ;

	    private final String description;

	    InvertebrateCategory(String description) {
	        this.description = description;
	    }

	    public String getDescription() {
	        return description;
	    }

	    @Override
	    public String toString() {
	        String name = name().toLowerCase().replace("_", " ");
	        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	    }
	}

	public Set<InvertebrateCategory> categories = new HashSet<InvertebrateCategory>();
	    
     // Enumerated characteristics
    public MetamorphosisType metamorphosis_type = MetamorphosisType.UNKNOWN;
    public LarvaeType larvae_type = LarvaeType.UNKNOWN;
    public BodySymmetry body_symmetry = BodySymmetry.UNKNOWN;
    public BodyShape body_shape = BodyShape.UNKNOWN;
    public FeedingTiming feeding_timing = FeedingTiming.UNKNOWN;
    public ExoskeletonType exoskeleton_type  = ExoskeletonType.UNKNOWN;
    public ExoskeletonColor exoskeleton_color = ExoskeletonColor.UNKNOWN;
    public FeedingApparatusType feeding_apparatus_type  = FeedingApparatusType.UNKNOWN;
    public LocomotionMode locomotion_mode = LocomotionMode.UNKNOWN;
    public TrophicLevel trophic_level = TrophicLevel.UNKNOWN;
    public ReproductiveCycle reproductive_cycle;
    public GeneticDiversity genetic_diversity = GeneticDiversity.UNKNOWN;
    public InvasionRisk invasion_risk = InvasionRisk.UNKNOWN;
    public PesticideResistance pesticide_resistance = PesticideResistance.UNKNOWN;
    public ToxicToHumans toxic_to_humans = ToxicToHumans.UNKNOWN;
    public EconomicImpact economic_impact = EconomicImpact.UNKNOWN;
    public ExoskeletonStrength exoskeleton_strength = ExoskeletonStrength.UNKNOWN;
    public WarningColoration warning_coloration = WarningColoration.UNKNOWN;
    public ToxinSecretion toxin_secretion = ToxinSecretion.UNKNOWN;
    public Venomous venomous = Venomous.UNKNOWN;
    public OlfactoryCapacity olfactory_capacity = OlfactoryCapacity.UNKNOWN;
    public TactileSensing tactile_sensing = TactileSensing.UNKNOWN;
    public LightSensitivity light_sensitivity = LightSensitivity.UNKNOWN;
    public SoundReception sound_reception = SoundReception.UNKNOWN;
    public UvSensitivity uv_sensitivity = UvSensitivity.UNKNOWN;
    public InfraredDetection infrared_detection = InfraredDetection.UNKNOWN;
    public EnvironmentalPlasticity environmental_plasticity = EnvironmentalPlasticity.UNKNOWN;
    public SalinityTolerance salinity_tolerance = SalinityTolerance.UNKNOWN;

    public TemperatureTolerance temperature_tolerance = new TemperatureTolerance();
    public HumidityTolerance humidity_tolerance = new HumidityTolerance();
    public PhTolerance ph_tolerance = new PhTolerance();
    public AltitudeRange altitude_range = new AltitudeRange();
    public BodyLength body_length = new BodyLength();
    public BodyMass body_mass = new BodyMass();
    public ChromosomeNumber chromosome_number = new ChromosomeNumber();
    public DepthRange depth_range = new DepthRange();
    public DevelopmentTime development_time = new DevelopmentTime();
    public EggSize egg_size = new EggSize();
    public EmbryonicStageDuration embryonic_stage_duration = new EmbryonicStageDuration();
    public Fecundity fecundity = new Fecundity();
    public GenerationTime generation_time = new GenerationTime();
    public GenomeSize genome_size = new GenomeSize();
    public LifeSpan life_span = new LifeSpan();
    public NumberOfEggs number_of_eggs = new NumberOfEggs();
    public NumberOfLimbs number_of_limbs = new NumberOfLimbs();
    public NumberOfSegments number_of_segments = new NumberOfSegments();
    public SpeedRange speed_range = new SpeedRange();
 
    public InvertebrateProfile() {
    }

    public static InvertebrateProfile create() {
        return new InvertebrateProfile();
    }

}
