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
