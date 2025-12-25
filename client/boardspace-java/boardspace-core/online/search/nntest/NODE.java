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
package online.search.nntest;
// The following java code is based on a multi-layer 
// BackVariation Propagation Neural Network Class (BackPropagation.class)
//
// Created by Anthony J. Papagelis & Dong Soo Kim
//
//  DateCreated:	15 September, 2001
// Last Update:	14 October, 2001

public class NODE {
	public	double 	Output;		
	// Output signal from current node

	public	double 	Weight[];		
	// Vector of weights from previous nodes to current node

	public	double	Threshold;	
	// Node Threshold /Bias

	public	double	WeightDiff[];	
	// Weight difference between the nth and the (n-1) iteration

	public	double	ThresholdDiff;	
	// Threshold difference between the nth and the (n-1) iteration

	public	double	SignalError;	
	// Output signal error

    	// InitialiseWeights function assigns a randomly 
	// generated number, between -1 and 1, to the 
	// Threshold and Weights to the current node
	private void InitialiseWeights() {
		Threshold = -1+2*Math.random();	    	
		// Initialise threshold nodes with a random 
		// number between -1 and 1

		ThresholdDiff = 0;				
		// Initially, ThresholdDiff is assigned to 0 so 
		// that the Momentum term can work during the 1st 
		// iteration

        	for(int i = 0; i < Weight.length; i++) {
			Weight[i]= -1+2*Math.random();	
			// Initialise all weight inputs with a 
			// random number between -1 and 1

			WeightDiff[i] = 0;			
			// Initially, WeightDiff is assigned to 0 
			// so that the Momentum term can work during 
			// the 1st iteration
		}
	}

	public NODE (int NumberOfNodes) {
		Weight = new double[NumberOfNodes];		
		// Create an array of Weight with the same 
		// size as the vector of inputs to the node

		WeightDiff = new double[NumberOfNodes];	
		// Create an array of weightDiff with the same 
		// size as the vector of inputs to the node

		InitialiseWeights();				
		// Initialise the Weights and Thresholds to the node
	}

	// added by DSK
	public double[] get_weights() { return Weight; }
	public double get_output() { return Output; }
};
