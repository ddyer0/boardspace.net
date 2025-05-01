package bugs.data;
public class Ants_withProfiles {

    public static class AntSpecies {
        public final String commonName;
        public final String scientificName;
        public final String description;

        public AntSpecies(String commonName, String scientificName, String description) {
            this.commonName = commonName;
            this.scientificName = scientificName;
            this.description = description;
        }
    }

    public static final AntSpecies ARGENTINE_ANT = new AntSpecies(
        "Argentine Ant",
        "Linepithema humile",
        "Native to South America, these ants are known for forming supercolonies and displacing native ant species."
    );

    public static final AntSpecies ARMY_ANT = new AntSpecies(
        "Army Ant",
        "Eciton burchellii",
        "Noted for their aggressive foraging behavior and nomadic lifestyle, forming large raiding swarms."
    );

    public static final AntSpecies BIG_HEADED_ANT = new AntSpecies(
        "Big-headed Ant",
        "Pheidole megacephala",
        "Characterized by major workers with disproportionately large heads; invasive in many regions."
    );

    public static final AntSpecies BLACK_GARDEN_ANT = new AntSpecies(
        "Black Garden Ant",
        "Lasius niger",
        "Common in Europe; known for building nests under pavements and in gardens."
    );

    public static final AntSpecies BULLET_ANT = new AntSpecies(
        "Bullet Ant",
        "Paraponera clavata",
        "Known for having one of the most painful insect stings; native to Central and South America."
    );

    public static final AntSpecies CARPENTER_ANT = new AntSpecies(
        "Carpenter Ant",
        "Camponotus pennsylvanicus",
        "Large ants that excavate wood to build nests, potentially causing structural damage."
    );

    public static final AntSpecies FIRE_ANT = new AntSpecies(
        "Fire Ant",
        "Solenopsis invicta",
        "Aggressive ants with a painful sting; form large mounds and are invasive in many areas."
    );

    public static final AntSpecies GHOST_ANT = new AntSpecies(
        "Ghost Ant",
        "Tapinoma melanocephalum",
        "Tiny ants with pale legs and gaster, making them appear translucent; common indoor pests."
    );

    public static final AntSpecies GREEN_TREE_ANT = new AntSpecies(
        "Green Tree Ant",
        "Oecophylla smaragdina",
        "Arboreal ants that build nests by weaving leaves together using larval silk."
    );

    public static final AntSpecies HARVESTER_ANT = new AntSpecies(
        "Harvester Ant",
        "Pogonomyrmex spp.",
        "Known for collecting seeds and having potent stings; nests often have cleared vegetation areas around them."
    );

    public static final AntSpecies HONEYPOT_ANT = new AntSpecies(
        "Honeypot Ant",
        "Myrmecocystus spp.",
        "Possess specialized workers that store nectar in their abdomens, serving as living food storage."
    );

    public static final AntSpecies LEAFCUTTER_ANT = new AntSpecies(
        "Leafcutter Ant",
        "Atta cephalotes",
        "Cultivate fungus by cutting leaves and using them as a substrate; exhibit complex social structures."
    );

    public static final AntSpecies LITTLE_BLACK_ANT = new AntSpecies(
        "Little Black Ant",
        "Monomorium minimum",
        "Small, dark-colored ants commonly found in urban areas; nests in soil and under objects."
    );

    public static final AntSpecies ODOROUS_HOUSE_ANT = new AntSpecies(
        "Odorous House Ant",
        "Tapinoma sessile",
        "Emits a rotten coconut or blue cheese odor when crushed; common household pest."
    );

    public static final AntSpecies PAVEMENT_ANT = new AntSpecies(
        "Pavement Ant",
        "Tetramorium immigrans",
        "Often found nesting in cracks in pavement; known for creating small soil mounds."
    );

    public static final AntSpecies PHARAOH_ANT = new AntSpecies(
        "Pharaoh Ant",
        "Monomorium pharaonis",
        "Small, yellow ants that infest buildings; difficult to control due to multiple queens."
    );

    public static final AntSpecies RED_WOOD_ANT = new AntSpecies(
        "Red Wood Ant",
        "Formica rufa",
        "Builds large mound nests in forests; important predators of forest pests."
    );

    public static final AntSpecies TRAPJAW_ANT = new AntSpecies(
        "Trap-jaw Ant",
        "Odontomachus spp.",
        "Possess powerful mandibles that snap shut to catch prey or defend against threats."
    );

    public static final AntSpecies WEAVER_ANT = new AntSpecies(
        "Weaver Ant",
        "Oecophylla longinoda",
        "Similar to green tree ants; build leaf nests and exhibit complex colony structures."
    );

    public static final AntSpecies YELLOW_CRAZY_ANT = new AntSpecies(
        "Yellow Crazy Ant",
        "Anoplolepis gracilipes",
        "Named for their erratic movements; invasive species that can form supercolonies."
    );

    public static final AntSpecies[] ALL_ANTS = {
        ARGENTINE_ANT, ARMY_ANT, BIG_HEADED_ANT, BLACK_GARDEN_ANT, BULLET_ANT,
        CARPENTER_ANT, FIRE_ANT, GHOST_ANT, GREEN_TREE_ANT, HARVESTER_ANT,
        HONEYPOT_ANT, LEAFCUTTER_ANT, LITTLE_BLACK_ANT, ODOROUS_HOUSE_ANT, PAVEMENT_ANT,
        PHARAOH_ANT, RED_WOOD_ANT, TRAPJAW_ANT, WEAVER_ANT, YELLOW_CRAZY_ANT
    };
        public static final InvertebrateProfile ARGENTINE_ANT_PROFILE = new InvertebrateProfile();
        static {
            ARGENTINE_ANT_PROFILE.categories.add(InvertebrateProfile.InvertebrateCategory.SOCIAL_STRUCTURE);
            ARGENTINE_ANT_PROFILE.metamorphosis_type = InvertebrateProfile.MetamorphosisType.COMPLETE;
            ARGENTINE_ANT_PROFILE.larvae_type = InvertebrateProfile.LarvaeType.GRUB;
            ARGENTINE_ANT_PROFILE.body_symmetry = InvertebrateProfile.BodySymmetry.BILATERAL;
            ARGENTINE_ANT_PROFILE.body_shape = InvertebrateProfile.BodyShape.SEGMENTED;
            ARGENTINE_ANT_PROFILE.exoskeleton_type = InvertebrateProfile.ExoskeletonType.CHITINOUS;
            ARGENTINE_ANT_PROFILE.feeding_apparatus_type = InvertebrateProfile.FeedingApparatusType.MANDIBLES;
            ARGENTINE_ANT_PROFILE.locomotion_mode = InvertebrateProfile.LocomotionMode.CRAWLING;
            ARGENTINE_ANT_PROFILE.trophic_level = InvertebrateProfile.TrophicLevel.OMNIVORE;
            // Binary and unknown traits are left as default false or N_A
        }

        public static final InvertebrateProfile ARMY_ANT_PROFILE = new InvertebrateProfile();
        static {
            ARMY_ANT_PROFILE.categories.add(InvertebrateProfile.InvertebrateCategory.SOCIAL_STRUCTURE);
            ARMY_ANT_PROFILE.metamorphosis_type = InvertebrateProfile.MetamorphosisType.COMPLETE;
            ARMY_ANT_PROFILE.larvae_type = InvertebrateProfile.LarvaeType.GRUB;
            ARMY_ANT_PROFILE.body_symmetry = InvertebrateProfile.BodySymmetry.BILATERAL;
            ARMY_ANT_PROFILE.body_shape = InvertebrateProfile.BodyShape.SEGMENTED;
            ARMY_ANT_PROFILE.exoskeleton_type = InvertebrateProfile.ExoskeletonType.CHITINOUS;
            ARMY_ANT_PROFILE.feeding_apparatus_type = InvertebrateProfile.FeedingApparatusType.MANDIBLES;
            ARMY_ANT_PROFILE.locomotion_mode = InvertebrateProfile.LocomotionMode.CRAWLING;
            ARMY_ANT_PROFILE.trophic_level = InvertebrateProfile.TrophicLevel.OMNIVORE;
            // Binary and unknown traits are left as default false or N_A
        }

        public static final InvertebrateProfile BIG_HEADED_ANT_PROFILE = new InvertebrateProfile();
        static {
            BIG_HEADED_ANT_PROFILE.categories.add(InvertebrateProfile.InvertebrateCategory.SOCIAL_STRUCTURE);
            BIG_HEADED_ANT_PROFILE.metamorphosis_type = InvertebrateProfile.MetamorphosisType.COMPLETE;
            BIG_HEADED_ANT_PROFILE.larvae_type = InvertebrateProfile.LarvaeType.GRUB;
            BIG_HEADED_ANT_PROFILE.body_symmetry = InvertebrateProfile.BodySymmetry.BILATERAL;
            BIG_HEADED_ANT_PROFILE.body_shape = InvertebrateProfile.BodyShape.SEGMENTED;
            BIG_HEADED_ANT_PROFILE.exoskeleton_type = InvertebrateProfile.ExoskeletonType.CHITINOUS;
            BIG_HEADED_ANT_PROFILE.feeding_apparatus_type = InvertebrateProfile.FeedingApparatusType.MANDIBLES;
            BIG_HEADED_ANT_PROFILE.locomotion_mode = InvertebrateProfile.LocomotionMode.CRAWLING;
            BIG_HEADED_ANT_PROFILE.trophic_level = InvertebrateProfile.TrophicLevel.OMNIVORE;
            // Binary and unknown traits are left as default false or N_A
        }

        public static final InvertebrateProfile BLACK_GARDEN_ANT_PROFILE = new InvertebrateProfile();
        static {
            BLACK_GARDEN_ANT_PROFILE.categories.add(InvertebrateProfile.InvertebrateCategory.SOCIAL_STRUCTURE);
            BLACK_GARDEN_ANT_PROFILE.metamorphosis_type = InvertebrateProfile.MetamorphosisType.COMPLETE;
            BLACK_GARDEN_ANT_PROFILE.larvae_type = InvertebrateProfile.LarvaeType.GRUB;
            BLACK_GARDEN_ANT_PROFILE.body_symmetry = InvertebrateProfile.BodySymmetry.BILATERAL;
            BLACK_GARDEN_ANT_PROFILE.body_shape = InvertebrateProfile.BodyShape.SEGMENTED;
            BLACK_GARDEN_ANT_PROFILE.exoskeleton_type = InvertebrateProfile.ExoskeletonType.CHITINOUS;
            BLACK_GARDEN_ANT_PROFILE.feeding_apparatus_type = InvertebrateProfile.FeedingApparatusType.MANDIBLES;
            BLACK_GARDEN_ANT_PROFILE.locomotion_mode = InvertebrateProfile.LocomotionMode.CRAWLING;
            BLACK_GARDEN_ANT_PROFILE.trophic_level = InvertebrateProfile.TrophicLevel.OMNIVORE;
            // Binary and unknown traits are left as default false or N_A
        }

        public static final InvertebrateProfile BULLET_ANT_PROFILE = new InvertebrateProfile();
        static {
            BULLET_ANT_PROFILE.categories.add(InvertebrateProfile.InvertebrateCategory.SOCIAL_STRUCTURE);
            BULLET_ANT_PROFILE.metamorphosis_type = InvertebrateProfile.MetamorphosisType.COMPLETE;
            BULLET_ANT_PROFILE.larvae_type = InvertebrateProfile.LarvaeType.GRUB;
            BULLET_ANT_PROFILE.body_symmetry = InvertebrateProfile.BodySymmetry.BILATERAL;
            BULLET_ANT_PROFILE.body_shape = InvertebrateProfile.BodyShape.SEGMENTED;
            BULLET_ANT_PROFILE.exoskeleton_type = InvertebrateProfile.ExoskeletonType.CHITINOUS;
            BULLET_ANT_PROFILE.feeding_apparatus_type = InvertebrateProfile.FeedingApparatusType.MANDIBLES;
            BULLET_ANT_PROFILE.locomotion_mode = InvertebrateProfile.LocomotionMode.CRAWLING;
            BULLET_ANT_PROFILE.trophic_level = InvertebrateProfile.TrophicLevel.OMNIVORE;
            // Binary and unknown traits are left as default false or N_A
        }

        public static final InvertebrateProfile CARPENTER_ANT_PROFILE = new InvertebrateProfile();
        static {
            CARPENTER_ANT_PROFILE.categories.add(InvertebrateProfile.InvertebrateCategory.SOCIAL_STRUCTURE);
            CARPENTER_ANT_PROFILE.metamorphosis_type = InvertebrateProfile.MetamorphosisType.COMPLETE;
            CARPENTER_ANT_PROFILE.larvae_type = InvertebrateProfile.LarvaeType.GRUB;
            CARPENTER_ANT_PROFILE.body_symmetry = InvertebrateProfile.BodySymmetry.BILATERAL;
            CARPENTER_ANT_PROFILE.body_shape = InvertebrateProfile.BodyShape.SEGMENTED;
            CARPENTER_ANT_PROFILE.exoskeleton_type = InvertebrateProfile.ExoskeletonType.CHITINOUS;
            CARPENTER_ANT_PROFILE.feeding_apparatus_type = InvertebrateProfile.FeedingApparatusType.MANDIBLES;
            CARPENTER_ANT_PROFILE.locomotion_mode = InvertebrateProfile.LocomotionMode.CRAWLING;
            CARPENTER_ANT_PROFILE.trophic_level = InvertebrateProfile.TrophicLevel.OMNIVORE;
            // Binary and unknown traits are left as default false or N_A
        }

        public static final InvertebrateProfile FIRE_ANT_PROFILE = new InvertebrateProfile();
        static {
            FIRE_ANT_PROFILE.categories.add(InvertebrateProfile.InvertebrateCategory.SOCIAL_STRUCTURE);
            FIRE_ANT_PROFILE.metamorphosis_type = InvertebrateProfile.MetamorphosisType.COMPLETE;
            FIRE_ANT_PROFILE.larvae_type = InvertebrateProfile.LarvaeType.GRUB;
            FIRE_ANT_PROFILE.body_symmetry = InvertebrateProfile.BodySymmetry.BILATERAL;
            FIRE_ANT_PROFILE.body_shape = InvertebrateProfile.BodyShape.SEGMENTED;
            FIRE_ANT_PROFILE.exoskeleton_type = InvertebrateProfile.ExoskeletonType.CHITINOUS;
            FIRE_ANT_PROFILE.feeding_apparatus_type = InvertebrateProfile.FeedingApparatusType.MANDIBLES;
            FIRE_ANT_PROFILE.locomotion_mode = InvertebrateProfile.LocomotionMode.CRAWLING;
            FIRE_ANT_PROFILE.trophic_level = InvertebrateProfile.TrophicLevel.OMNIVORE;
            // Binary and unknown traits are left as default false or N_A
        }

        public static final InvertebrateProfile GHOST_ANT_PROFILE = new InvertebrateProfile();
        static {
            GHOST_ANT_PROFILE.categories.add(InvertebrateProfile.InvertebrateCategory.SOCIAL_STRUCTURE);
            GHOST_ANT_PROFILE.metamorphosis_type = InvertebrateProfile.MetamorphosisType.COMPLETE;
            GHOST_ANT_PROFILE.larvae_type = InvertebrateProfile.LarvaeType.GRUB;
            GHOST_ANT_PROFILE.body_symmetry = InvertebrateProfile.BodySymmetry.BILATERAL;
            GHOST_ANT_PROFILE.body_shape = InvertebrateProfile.BodyShape.SEGMENTED;
            GHOST_ANT_PROFILE.exoskeleton_type = InvertebrateProfile.ExoskeletonType.CHITINOUS;
            GHOST_ANT_PROFILE.feeding_apparatus_type = InvertebrateProfile.FeedingApparatusType.MANDIBLES;
            GHOST_ANT_PROFILE.locomotion_mode = InvertebrateProfile.LocomotionMode.CRAWLING;
            GHOST_ANT_PROFILE.trophic_level = InvertebrateProfile.TrophicLevel.OMNIVORE;
            // Binary and unknown traits are left as default false or N_A
        }

        public static final InvertebrateProfile GREEN_TREE_ANT_PROFILE = new InvertebrateProfile();
        static {
            GREEN_TREE_ANT_PROFILE.categories.add(InvertebrateProfile.InvertebrateCategory.SOCIAL_STRUCTURE);
            GREEN_TREE_ANT_PROFILE.metamorphosis_type = InvertebrateProfile.MetamorphosisType.COMPLETE;
            GREEN_TREE_ANT_PROFILE.larvae_type = InvertebrateProfile.LarvaeType.GRUB;
            GREEN_TREE_ANT_PROFILE.body_symmetry = InvertebrateProfile.BodySymmetry.BILATERAL;
            GREEN_TREE_ANT_PROFILE.body_shape = InvertebrateProfile.BodyShape.SEGMENTED;
            GREEN_TREE_ANT_PROFILE.exoskeleton_type = InvertebrateProfile.ExoskeletonType.CHITINOUS;
            GREEN_TREE_ANT_PROFILE.feeding_apparatus_type = InvertebrateProfile.FeedingApparatusType.MANDIBLES;
            GREEN_TREE_ANT_PROFILE.locomotion_mode = InvertebrateProfile.LocomotionMode.CRAWLING;
            GREEN_TREE_ANT_PROFILE.trophic_level = InvertebrateProfile.TrophicLevel.OMNIVORE;
            // Binary and unknown traits are left as default false or N_A
        }

        public static final InvertebrateProfile HARVESTER_ANT_PROFILE = new InvertebrateProfile();
        static {
            HARVESTER_ANT_PROFILE.categories.add(InvertebrateProfile.InvertebrateCategory.SOCIAL_STRUCTURE);
            HARVESTER_ANT_PROFILE.metamorphosis_type = InvertebrateProfile.MetamorphosisType.COMPLETE;
            HARVESTER_ANT_PROFILE.larvae_type = InvertebrateProfile.LarvaeType.GRUB;
            HARVESTER_ANT_PROFILE.body_symmetry = InvertebrateProfile.BodySymmetry.BILATERAL;
            HARVESTER_ANT_PROFILE.body_shape = InvertebrateProfile.BodyShape.SEGMENTED;
            HARVESTER_ANT_PROFILE.exoskeleton_type = InvertebrateProfile.ExoskeletonType.CHITINOUS;
            HARVESTER_ANT_PROFILE.feeding_apparatus_type = InvertebrateProfile.FeedingApparatusType.MANDIBLES;
            HARVESTER_ANT_PROFILE.locomotion_mode = InvertebrateProfile.LocomotionMode.CRAWLING;
            HARVESTER_ANT_PROFILE.trophic_level = InvertebrateProfile.TrophicLevel.OMNIVORE;
            // Binary and unknown traits are left as default false or N_A
        }

        public static final InvertebrateProfile HONEYPOT_ANT_PROFILE = new InvertebrateProfile();
        static {
            HONEYPOT_ANT_PROFILE.categories.add(InvertebrateProfile.InvertebrateCategory.SOCIAL_STRUCTURE);
            HONEYPOT_ANT_PROFILE.metamorphosis_type = InvertebrateProfile.MetamorphosisType.COMPLETE;
            HONEYPOT_ANT_PROFILE.larvae_type = InvertebrateProfile.LarvaeType.GRUB;
            HONEYPOT_ANT_PROFILE.body_symmetry = InvertebrateProfile.BodySymmetry.BILATERAL;
            HONEYPOT_ANT_PROFILE.body_shape = InvertebrateProfile.BodyShape.SEGMENTED;
            HONEYPOT_ANT_PROFILE.exoskeleton_type = InvertebrateProfile.ExoskeletonType.CHITINOUS;
            HONEYPOT_ANT_PROFILE.feeding_apparatus_type = InvertebrateProfile.FeedingApparatusType.MANDIBLES;
            HONEYPOT_ANT_PROFILE.locomotion_mode = InvertebrateProfile.LocomotionMode.CRAWLING;
            HONEYPOT_ANT_PROFILE.trophic_level = InvertebrateProfile.TrophicLevel.OMNIVORE;
            // Binary and unknown traits are left as default false or N_A
        }

        public static final InvertebrateProfile LEAFCUTTER_ANT_PROFILE = new InvertebrateProfile();
        static {
            LEAFCUTTER_ANT_PROFILE.categories.add(InvertebrateProfile.InvertebrateCategory.SOCIAL_STRUCTURE);
            LEAFCUTTER_ANT_PROFILE.metamorphosis_type = InvertebrateProfile.MetamorphosisType.COMPLETE;
            LEAFCUTTER_ANT_PROFILE.larvae_type = InvertebrateProfile.LarvaeType.GRUB;
            LEAFCUTTER_ANT_PROFILE.body_symmetry = InvertebrateProfile.BodySymmetry.BILATERAL;
            LEAFCUTTER_ANT_PROFILE.body_shape = InvertebrateProfile.BodyShape.SEGMENTED;
            LEAFCUTTER_ANT_PROFILE.exoskeleton_type = InvertebrateProfile.ExoskeletonType.CHITINOUS;
            LEAFCUTTER_ANT_PROFILE.feeding_apparatus_type = InvertebrateProfile.FeedingApparatusType.MANDIBLES;
            LEAFCUTTER_ANT_PROFILE.locomotion_mode = InvertebrateProfile.LocomotionMode.CRAWLING;
            LEAFCUTTER_ANT_PROFILE.trophic_level = InvertebrateProfile.TrophicLevel.OMNIVORE;
            // Binary and unknown traits are left as default false or N_A
        }

        public static final InvertebrateProfile LITTLE_BLACK_ANT_PROFILE = new InvertebrateProfile();
        static {
            LITTLE_BLACK_ANT_PROFILE.categories.add(InvertebrateProfile.InvertebrateCategory.SOCIAL_STRUCTURE);
            LITTLE_BLACK_ANT_PROFILE.metamorphosis_type = InvertebrateProfile.MetamorphosisType.COMPLETE;
            LITTLE_BLACK_ANT_PROFILE.larvae_type = InvertebrateProfile.LarvaeType.GRUB;
            LITTLE_BLACK_ANT_PROFILE.body_symmetry = InvertebrateProfile.BodySymmetry.BILATERAL;
            LITTLE_BLACK_ANT_PROFILE.body_shape = InvertebrateProfile.BodyShape.SEGMENTED;
            LITTLE_BLACK_ANT_PROFILE.exoskeleton_type = InvertebrateProfile.ExoskeletonType.CHITINOUS;
            LITTLE_BLACK_ANT_PROFILE.feeding_apparatus_type = InvertebrateProfile.FeedingApparatusType.MANDIBLES;
            LITTLE_BLACK_ANT_PROFILE.locomotion_mode = InvertebrateProfile.LocomotionMode.CRAWLING;
            LITTLE_BLACK_ANT_PROFILE.trophic_level = InvertebrateProfile.TrophicLevel.OMNIVORE;
            // Binary and unknown traits are left as default false or N_A
        }

        public static final InvertebrateProfile ODOROUS_HOUSE_ANT_PROFILE = new InvertebrateProfile();
        static {
            ODOROUS_HOUSE_ANT_PROFILE.categories.add(InvertebrateProfile.InvertebrateCategory.SOCIAL_STRUCTURE);
            ODOROUS_HOUSE_ANT_PROFILE.metamorphosis_type = InvertebrateProfile.MetamorphosisType.COMPLETE;
            ODOROUS_HOUSE_ANT_PROFILE.larvae_type = InvertebrateProfile.LarvaeType.GRUB;
            ODOROUS_HOUSE_ANT_PROFILE.body_symmetry = InvertebrateProfile.BodySymmetry.BILATERAL;
            ODOROUS_HOUSE_ANT_PROFILE.body_shape = InvertebrateProfile.BodyShape.SEGMENTED;
            ODOROUS_HOUSE_ANT_PROFILE.exoskeleton_type = InvertebrateProfile.ExoskeletonType.CHITINOUS;
            ODOROUS_HOUSE_ANT_PROFILE.feeding_apparatus_type = InvertebrateProfile.FeedingApparatusType.MANDIBLES;
            ODOROUS_HOUSE_ANT_PROFILE.locomotion_mode = InvertebrateProfile.LocomotionMode.CRAWLING;
            ODOROUS_HOUSE_ANT_PROFILE.trophic_level = InvertebrateProfile.TrophicLevel.OMNIVORE;
            // Binary and unknown traits are left as default false or N_A
        }

        public static final InvertebrateProfile PAVEMENT_ANT_PROFILE = new InvertebrateProfile();
        static {
            PAVEMENT_ANT_PROFILE.categories.add(InvertebrateProfile.InvertebrateCategory.SOCIAL_STRUCTURE);
            PAVEMENT_ANT_PROFILE.metamorphosis_type = InvertebrateProfile.MetamorphosisType.COMPLETE;
            PAVEMENT_ANT_PROFILE.larvae_type = InvertebrateProfile.LarvaeType.GRUB;
            PAVEMENT_ANT_PROFILE.body_symmetry = InvertebrateProfile.BodySymmetry.BILATERAL;
            PAVEMENT_ANT_PROFILE.body_shape = InvertebrateProfile.BodyShape.SEGMENTED;
            PAVEMENT_ANT_PROFILE.exoskeleton_type = InvertebrateProfile.ExoskeletonType.CHITINOUS;
            PAVEMENT_ANT_PROFILE.feeding_apparatus_type = InvertebrateProfile.FeedingApparatusType.MANDIBLES;
            PAVEMENT_ANT_PROFILE.locomotion_mode = InvertebrateProfile.LocomotionMode.CRAWLING;
            PAVEMENT_ANT_PROFILE.trophic_level = InvertebrateProfile.TrophicLevel.OMNIVORE;
            // Binary and unknown traits are left as default false or N_A
        }

        public static final InvertebrateProfile PHARAOH_ANT_PROFILE = new InvertebrateProfile();
        static {
            PHARAOH_ANT_PROFILE.categories.add(InvertebrateProfile.InvertebrateCategory.SOCIAL_STRUCTURE);
            PHARAOH_ANT_PROFILE.metamorphosis_type = InvertebrateProfile.MetamorphosisType.COMPLETE;
            PHARAOH_ANT_PROFILE.larvae_type = InvertebrateProfile.LarvaeType.GRUB;
            PHARAOH_ANT_PROFILE.body_symmetry = InvertebrateProfile.BodySymmetry.BILATERAL;
            PHARAOH_ANT_PROFILE.body_shape = InvertebrateProfile.BodyShape.SEGMENTED;
            PHARAOH_ANT_PROFILE.exoskeleton_type = InvertebrateProfile.ExoskeletonType.CHITINOUS;
            PHARAOH_ANT_PROFILE.feeding_apparatus_type = InvertebrateProfile.FeedingApparatusType.MANDIBLES;
            PHARAOH_ANT_PROFILE.locomotion_mode = InvertebrateProfile.LocomotionMode.CRAWLING;
            PHARAOH_ANT_PROFILE.trophic_level = InvertebrateProfile.TrophicLevel.OMNIVORE;
            // Binary and unknown traits are left as default false or N_A
        }

        public static final InvertebrateProfile RED_WOOD_ANT_PROFILE = new InvertebrateProfile();
        static {
            RED_WOOD_ANT_PROFILE.categories.add(InvertebrateProfile.InvertebrateCategory.SOCIAL_STRUCTURE);
            RED_WOOD_ANT_PROFILE.metamorphosis_type = InvertebrateProfile.MetamorphosisType.COMPLETE;
            RED_WOOD_ANT_PROFILE.larvae_type = InvertebrateProfile.LarvaeType.GRUB;
            RED_WOOD_ANT_PROFILE.body_symmetry = InvertebrateProfile.BodySymmetry.BILATERAL;
            RED_WOOD_ANT_PROFILE.body_shape = InvertebrateProfile.BodyShape.SEGMENTED;
            RED_WOOD_ANT_PROFILE.exoskeleton_type = InvertebrateProfile.ExoskeletonType.CHITINOUS;
            RED_WOOD_ANT_PROFILE.feeding_apparatus_type = InvertebrateProfile.FeedingApparatusType.MANDIBLES;
            RED_WOOD_ANT_PROFILE.locomotion_mode = InvertebrateProfile.LocomotionMode.CRAWLING;
            RED_WOOD_ANT_PROFILE.trophic_level = InvertebrateProfile.TrophicLevel.OMNIVORE;
            // Binary and unknown traits are left as default false or N_A
        }

        public static final InvertebrateProfile TRAPJAW_ANT_PROFILE = new InvertebrateProfile();
        static {
            TRAPJAW_ANT_PROFILE.categories.add(InvertebrateProfile.InvertebrateCategory.SOCIAL_STRUCTURE);
            TRAPJAW_ANT_PROFILE.metamorphosis_type = InvertebrateProfile.MetamorphosisType.COMPLETE;
            TRAPJAW_ANT_PROFILE.larvae_type = InvertebrateProfile.LarvaeType.GRUB;
            TRAPJAW_ANT_PROFILE.body_symmetry = InvertebrateProfile.BodySymmetry.BILATERAL;
            TRAPJAW_ANT_PROFILE.body_shape = InvertebrateProfile.BodyShape.SEGMENTED;
            TRAPJAW_ANT_PROFILE.exoskeleton_type = InvertebrateProfile.ExoskeletonType.CHITINOUS;
            TRAPJAW_ANT_PROFILE.feeding_apparatus_type = InvertebrateProfile.FeedingApparatusType.MANDIBLES;
            TRAPJAW_ANT_PROFILE.locomotion_mode = InvertebrateProfile.LocomotionMode.CRAWLING;
            TRAPJAW_ANT_PROFILE.trophic_level = InvertebrateProfile.TrophicLevel.OMNIVORE;
            // Binary and unknown traits are left as default false or N_A
        }

        public static final InvertebrateProfile WEAVER_ANT_PROFILE = new InvertebrateProfile();
        static {
            WEAVER_ANT_PROFILE.categories.add(InvertebrateProfile.InvertebrateCategory.SOCIAL_STRUCTURE);
            WEAVER_ANT_PROFILE.metamorphosis_type = InvertebrateProfile.MetamorphosisType.COMPLETE;
            WEAVER_ANT_PROFILE.larvae_type = InvertebrateProfile.LarvaeType.GRUB;
            WEAVER_ANT_PROFILE.body_symmetry = InvertebrateProfile.BodySymmetry.BILATERAL;
            WEAVER_ANT_PROFILE.body_shape = InvertebrateProfile.BodyShape.SEGMENTED;
            WEAVER_ANT_PROFILE.exoskeleton_type = InvertebrateProfile.ExoskeletonType.CHITINOUS;
            WEAVER_ANT_PROFILE.feeding_apparatus_type = InvertebrateProfile.FeedingApparatusType.MANDIBLES;
            WEAVER_ANT_PROFILE.locomotion_mode = InvertebrateProfile.LocomotionMode.CRAWLING;
            WEAVER_ANT_PROFILE.trophic_level = InvertebrateProfile.TrophicLevel.OMNIVORE;
            // Binary and unknown traits are left as default false or N_A
        }

        public static final InvertebrateProfile YELLOW_CRAZY_ANT_PROFILE = new InvertebrateProfile();
        static {
            YELLOW_CRAZY_ANT_PROFILE.categories.add(InvertebrateProfile.InvertebrateCategory.SOCIAL_STRUCTURE);
            YELLOW_CRAZY_ANT_PROFILE.metamorphosis_type = InvertebrateProfile.MetamorphosisType.COMPLETE;
            YELLOW_CRAZY_ANT_PROFILE.larvae_type = InvertebrateProfile.LarvaeType.GRUB;
            YELLOW_CRAZY_ANT_PROFILE.body_symmetry = InvertebrateProfile.BodySymmetry.BILATERAL;
            YELLOW_CRAZY_ANT_PROFILE.body_shape = InvertebrateProfile.BodyShape.SEGMENTED;
            YELLOW_CRAZY_ANT_PROFILE.exoskeleton_type = InvertebrateProfile.ExoskeletonType.CHITINOUS;
            YELLOW_CRAZY_ANT_PROFILE.feeding_apparatus_type = InvertebrateProfile.FeedingApparatusType.MANDIBLES;
            YELLOW_CRAZY_ANT_PROFILE.locomotion_mode = InvertebrateProfile.LocomotionMode.CRAWLING;
            YELLOW_CRAZY_ANT_PROFILE.trophic_level = InvertebrateProfile.TrophicLevel.OMNIVORE;
            // Binary and unknown traits are left as default false or N_A
        }

}
