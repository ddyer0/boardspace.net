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
//  Last Update:	14 October, 2001

public class LAYER {
	private 	double	Net;

	public	double 	Input[];		
	// Vector of inputs signals from previous 
	// layer to the current layer

	public	NODE	Node[];		
	// Vector of nodes in current layer

    	// The FeedForward function is called so that 
	// the outputs for all the nodes in the current 
	// layer are calculated
	public void FeedForward() {
		for (int i = 0; i < Node.length; i++) {
			Net = Node[i].Threshold;

			for (int j = 0; j < Node[i].Weight.length; j++)
				Net = Net + Input[j] * Node[i].Weight[j];

			Node[i].Output = Sigmoid(Net);
		}
	}

    	// The Sigmoid function calculates the 
	// activation/output from the current node
	private double Sigmoid (double Net) {
		return 1/(1+Math.exp(-Net));
	}


	// Return the output from all node in the layer
	// in a vector form
	public double[] OutputVector() {

		double Vector[];

		Vector = new double[Node.length];

		for (int i=0; i < Node.length; i++)
			Vector[i] = Node[i].Output;

		return (Vector);
	}

	public LAYER (int NumberOfNodes, int NumberOfInputs) {
		Node = new NODE[NumberOfNodes];

		for (int i = 0; i < NumberOfNodes; i++)
			Node[i] = new NODE(NumberOfInputs);

		Input = new double[NumberOfInputs];
	}

	// added by DSK
	public NODE[] get_nodes() { return Node; }
};
