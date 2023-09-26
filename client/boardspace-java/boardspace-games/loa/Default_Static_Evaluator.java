/*
	Copyright 2006-2023 by Dave Dyer

    This file is part of the Boardspace project.
    
    Boardspace is free software: you can redistribute it and/or modify it under the terms of 
    the GNU General Public License as published by the Free Software Foundation, 
    either version 3 of the License, or (at your option) any later version.
    
    Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
    See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with Boardspace.
    If not, see https://www.gnu.org/licenses/. 
 */
package loa;

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
