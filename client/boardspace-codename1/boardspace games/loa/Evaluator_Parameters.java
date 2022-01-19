package loa;

public interface Evaluator_Parameters
{
    /* parameters for the evaluator */
    static double Q1_WEIGHT = 0.00001;
    static double Q1_WEIGHT2 = -0.00001;
    static double Q2_WEIGHT = -0.00001;
    static double QD_WEIGHT = -0.000005;
    static double Q3_WEIGHT = 0.00001;
    static double Q4_WEIGHT = 0.000005;
    static double STONE_WEIGHT = 0.00002;
    static double DUPLICATE_WEIGHT = -0.1;
    static double EULER_WEIGHT = -0.00001;
    static double LOAPS_WEIGHT = 0.001;
    static String[] evaluator_classes = 
        {
            "loa.player.Original_Static_Evaluator",
            "loa.player.Default_Static_Evaluator",
            "loa.player.Experimental_Static_Evaluator"
        };
}
