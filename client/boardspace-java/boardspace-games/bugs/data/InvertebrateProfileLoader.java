
package bugs.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class InvertebrateProfileLoader {

    public static Set<InvertebrateProfile> loadProfiles(String filename) throws IOException {
        Set<InvertebrateProfile> profiles = new HashSet<>();
        BufferedReader reader = new BufferedReader(new FileReader(filename));

        InvertebrateProfile profile = null;
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.equals("---")) {
                if (profile != null) {
                    profiles.add(profile);
                }
                profile = new InvertebrateProfile();
                continue;
            }

            if (profile == null) {
                throw new IOException("File must start with --- separator");
            }

            int colonIndex = line.indexOf(":");
            if (colonIndex == -1) {
                throw new IOException("Invalid line (no colon): " + line);
            }

            String key = line.substring(0, colonIndex).trim();
            String value = line.substring(colonIndex + 1).trim();

            assignField(profile, key, value);
        }

        if (profile != null) {
            profiles.add(profile);
        }

        reader.close();
        return profiles;
    }

    private static void assignField(InvertebrateProfile profile, String key, String value) {
        try {
            switch (key) {
                case "uid": profile.uid = Integer.parseInt(value); break;
                case "directory": profile.directory = value; break;
                case "name": profile.name = value; break;
                case "rank": profile.rank = InvertebrateProfile.Rank.valueOf(value); break; // <-- Newly added
                case "common_name": profile.commonName = value; break;
                case "category": profile.category = value; break;
                case "categories":
                    for (String cat : value.split(",")) {
                        profile.categories.add(InvertebrateProfile.InvertebrateCategory.valueOf(cat.trim()));
                    }
                    break;
                case "metamorphosis_type": profile.metamorphosis_type = InvertebrateProfile.MetamorphosisType.valueOf(value); break;
                case "larvae_type": profile.larvae_type = InvertebrateProfile.LarvaeType.valueOf(value); break;
                case "body_symmetry": profile.body_symmetry = InvertebrateProfile.BodySymmetry.valueOf(value); break;
                case "body_shape": profile.body_shape = InvertebrateProfile.BodyShape.valueOf(value); break;
                case "feeding_timing": profile.feeding_timing = InvertebrateProfile.FeedingTiming.valueOf(value); break;
                case "exoskeleton_type": profile.exoskeleton_type = InvertebrateProfile.ExoskeletonType.valueOf(value); break;
                case "exoskeleton_color": profile.exoskeleton_color = InvertebrateProfile.ExoskeletonColor.valueOf(value); break;
                case "feeding_apparatus_type": profile.feeding_apparatus_type = InvertebrateProfile.FeedingApparatusType.valueOf(value); break;
                case "locomotion_mode": profile.locomotion_mode = InvertebrateProfile.LocomotionMode.valueOf(value); break;
                case "trophic_level": profile.trophic_level = InvertebrateProfile.TrophicLevel.valueOf(value); break;
                case "reproductive_cycle": profile.reproductive_cycle = InvertebrateProfile.ReproductiveCycle.valueOf(value); break;
                case "genetic_diversity": profile.genetic_diversity = InvertebrateProfile.GeneticDiversity.valueOf(value); break;
                case "invasion_risk": profile.invasion_risk = InvertebrateProfile.InvasionRisk.valueOf(value); break;
                case "pesticide_resistance": profile.pesticide_resistance = InvertebrateProfile.PesticideResistance.valueOf(value); break;
                case "toxic_to_humans": profile.toxic_to_humans = InvertebrateProfile.ToxicToHumans.valueOf(value); break;
                case "economic_impact": profile.economic_impact = InvertebrateProfile.EconomicImpact.valueOf(value); break;
                case "exoskeleton_strength": profile.exoskeleton_strength = InvertebrateProfile.ExoskeletonStrength.valueOf(value); break;
                case "warning_coloration": profile.warning_coloration = InvertebrateProfile.WarningColoration.valueOf(value); break;
                case "toxin_secretion": profile.toxin_secretion = InvertebrateProfile.ToxinSecretion.valueOf(value); break;
                case "venomous": profile.venomous = InvertebrateProfile.Venomous.valueOf(value); break;
                case "olfactory_capacity": profile.olfactory_capacity = InvertebrateProfile.OlfactoryCapacity.valueOf(value); break;
                case "tactile_sensing": profile.tactile_sensing = InvertebrateProfile.TactileSensing.valueOf(value); break;
                case "light_sensitivity": profile.light_sensitivity = InvertebrateProfile.LightSensitivity.valueOf(value); break;
                case "sound_reception": profile.sound_reception = InvertebrateProfile.SoundReception.valueOf(value); break;
                case "uv_sensitivity": profile.uv_sensitivity = InvertebrateProfile.UvSensitivity.valueOf(value); break;
                case "infrared_detection": profile.infrared_detection = InvertebrateProfile.InfraredDetection.valueOf(value); break;
                case "environmental_plasticity": profile.environmental_plasticity = InvertebrateProfile.EnvironmentalPlasticity.valueOf(value); break;
                case "salinity_tolerance": profile.salinity_tolerance = InvertebrateProfile.SalinityTolerance.valueOf(value); break;
                default:
                    System.err.println("Unknown field: " + key);
                    break;
            }
        } catch (Exception e) {
            System.err.println("Failed to assign field: " + key + " with value: " + value);
            e.printStackTrace();
        }
    }
}
