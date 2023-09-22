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
package zertz.common;

import lib.DStack;
import online.game.BoardProtocol;
import online.search.DefaultEvaluator;
import online.search.Evaluator;

public class StandardEvaluator extends DefaultEvaluator implements Evaluator,GameConstants
{
    double BPWeight = 0.0007;
    double InsideWeight = -0.05;
    double[] RingWeights = { -1, -0.5, -0.25, 0, 0, 0, 0 };
    double[] BallWeights = { -2, -1, -0.5, -0.4, -0.3, -0.2, 0 };
    
	public double[] getWeights() {
		DStack w = new DStack();
		w.push(BPWeight);
		w.push(InsideWeight);
		for(double m : RingWeights) { w.push(m);}
		for(double m : BallWeights) { w.push(m);}
		return(w.toArray());
	}

	public void setWeights(double[] v) {
		int idx = 0;
		BPWeight = v[idx++];
		InsideWeight = v[idx++];
		for(int i=0;i<RingWeights.length;i++) { RingWeights[i] = v[idx++]; }
		for(int i=0;i<BallWeights.length;i++) { BallWeights[i] = v[idx++]; }
	}
	
    int[] ringcounts = new int[7];
    int[] ballcounts = new int[7];
    int ballinside = 0;

    public void CountBoardStats(GameBoard board)
    {
        ballinside = 0;

        for (int i = 0; i < ringcounts.length; i++)
        {
            ringcounts[i] = 0;
            ballcounts[i] = 0;
        }

        for(zCell c = board.allCells; c!=null; c=c.next)
        {
        	int v = board.SpaceCount(c);
        	c.count = v;
        	if(v>=0) { ringcounts[v]++; }
        }
        	
        for (int i = board.balls_on_board - 1; i >= 0; i--)
        {
            zCell ballc = board.ball_location[i];
            char ch = ballc.contents;
            int col = zChip.BallColorIndex(ch);

            if (col >= 0)
            {
                int ct = ballc.count;	// count of adjavent spaces still on the board
                ballcounts[ct]++;		// count the balls with a particular count

                if (ct == 6)			// not adjacent to any edge
                {
                    int adj = 0;

                    for (int dir = 0; dir < 6; dir++)
                    {	zCell nc = ballc.exitTo(dir);
                        if (nc!=null)
                        {
                            char c = nc.contents;

                            if (c != NoSpace)
                            {	// count spaces ajacent to a ball which are on the edge
                                if (c == Empty)
                                {
                                    adj += (6 - nc.count);
                                }
                                else
                                {
                                    adj += 6;
                                }
                            }
                        }
                    }

                    ballinside += adj;
                }
            }
        }
    }

    private double BPScore()
    {
        double sum = 0.0;

        for (int i = 0; i < ringcounts.length; i++)
        {
            sum += (RingWeights[i] * ringcounts[i]);
            sum += (BallWeights[i] * ballcounts[i]);
        }

        sum += (InsideWeight * ballinside);

        return (sum * BPWeight);
    }
    public double evaluate(BoardProtocol bb,int playerindex,boolean print)
    {	GameBoard b = (GameBoard)bb;
        double val = 0;
        int otherplayerindex = playerindex^1;

        CountBoardStats(b);

            double BPS = BPScore();
            double oval = b.balls_to_win(otherplayerindex);
            double mval = b.balls_to_win(playerindex);
            val = BPS + (oval - mval);

            if (print)
            {
                String rstats = "";
                String bstats = "";

                for (int i = 0; i < ringcounts.length; i++)
                {
                    rstats += ("" + ringcounts[i] + " ");
                    bstats += ("" + ballcounts[i] + " ");
                }

                System.out.println("Rings: " + rstats + " Balls: " + bstats +
                    "+ " + ballinside);
                System.out.println("Exp Eval is BP " + BPS + " " + oval +
                    " - " + mval + " = " + (BPS + (oval - mval)));
            }

        return (val);
    }



}
