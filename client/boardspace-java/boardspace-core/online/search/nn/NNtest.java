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
package online.search.nn;


import lib.G;
import lib.Random;

public class NNtest {
	static boolean linear = true;
	
	public static double MysteryFunction(double a,double b, double c)
	{	// (Math.sqrt(a)>(b+c)*(b-c))?1:0
		return(a+(b*c));
	}
	static public double runEpoch(GenericNetwork n,Random r)
	{	double tot = 0;
		int steps = 10000;
		for(int j=0;j<steps;j++)
		{
		double a = r.nextDouble();
		double b = r.nextDouble();
		double c = r.nextDouble();
		double v = MysteryFunction(a,b,c);
		n.getLayer("IN").setInputs(a,b,c);
		n.calculate();
		double expected[] = new double[]{v};

		double v1[] = n.getValues();
		//dumpWeights(n);

		n.learn(expected);
		//dumpWeights(n);

		double val0 = v1[0];
		double val = linear ? val0 : val0>0.9 ? 1 : val0<0.1 ? 0 : 0.5;
		double err0 = linear ? Math.abs(val-v) : (val==v) ? 0 : 1;
		//if(err0!=0) { G.print("Val "+v+" "+val0+" "+err0); }
		tot += err0;
		}
		return(tot/steps*100);
	}


	public static void main(String args[])
	{
		GenericNetwork n = 
				//(GenericNetwork)GenericNetwork.loadNetwork(
				//"g:/temp/nn/nntest6-36000.txt"
				//"g:/temp/nn/nntest7-298-start.txt"
				//);
				new GenericNetwork(linear ? "OUTPUT_TRANSFER_FUNCTION LINEAR LEARNING_RATE .1":"",3,6,6,6,6,1);
		//n.initializeWeights(0.1);
		double prev = 0;
		int prevPass = 0;
		for(int pass = 0; true; pass++)
		{	
			double tot = 0;
			for(int j=0;j<1;j++) 
			{
			Random r = new Random(1000+pass);

			tot = runEpoch(n,r);
			}
			if(pass-prevPass>10000)
			{
				n.learningRate = n.learningRate/2;
				prevPass = pass;
				G.print("New learning rate "+n.learningRate);
			}
			if(pass % 1000 == 0 || tot<prev)
			{	G.print("Pass "+pass+" "+tot);
				if(tot<prev) { prevPass = pass; }
				n.dumpWeights(false);
				n.saveNetwork("g:/temp/nn/nntest8-"+pass+".txt","test network at "+pass);
			for(int i=0;i<5;i++)
			{	Random r1 = new Random();
				double a1 = r1.nextDouble();
				double b1 = r1.nextDouble();
				double c1 = r1.nextDouble();
				double v1 = MysteryFunction(a1,b1,c1);
				n.getLayer("IN").setInputs(new double[] {a1,b1,c1});
				n.calculate();
				double val0 = n.getValues()[0];
				n.calculate();
				double val = linear ? val0 : (val0<0.1 ? 0 : val0>0.9 ? 1 : 0.5);
				System.out.println("V "+v1 +" "+val+" "+(v1-val));
			}}
			prev = prev==0? tot : Math.min(tot, prev);

		}
	}
}
