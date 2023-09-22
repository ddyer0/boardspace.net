/* copyright notice */package loa;

import lib.NamedObject;


class Original_Static_Evaluator extends NamedObject implements Static_Evaluator,
    Evaluator_Parameters
{
    public String getName()
    {
        return ("original");
    }

    public String getDescription()
    {
        return ("the simplest thing that could possibly work");
    }

    public double Evaluate(Player_Info pl)
    {
        double val = pl.Sum_Offcenter();

        return (val);
    }
}


class Default_Static_Evaluator extends NamedObject implements Static_Evaluator,
    Evaluator_Parameters
{
    public String getName()
    {
        return ("default");
    }

    public String getDescription()
    {
        return ("simple evaluator plus weights for quad types");
    }

    public double Evaluate(Player_Info pl)
    {
        Quad_Count qc = pl.qc;
        double val = pl.Sum_Offcenter() + (qc.qd * QD_WEIGHT) +
            (qc.q3 * Q3_WEIGHT) + (STONE_WEIGHT * pl.n_stones) +
            (EULER_WEIGHT * qc.Euler);

        return (val);
    }
}


class Experimantal_Static_Evaluator extends NamedObject
    implements Static_Evaluator, Evaluator_Parameters
{
    public String getName()
    {
        return ("experimental");
    }

    public String getDescription()
    {
        return ("the latest experiment");
    }

    public double Evaluate(Player_Info pl)
    {
        Quad_Count qc = pl.qc;
        double val = pl.Sum_Offcenter() + (qc.qd * QD_WEIGHT) +
            (qc.q3 * Q3_WEIGHT) + (STONE_WEIGHT * pl.n_stones) +
            (EULER_WEIGHT * qc.Euler);

        return (val);
    }
}
