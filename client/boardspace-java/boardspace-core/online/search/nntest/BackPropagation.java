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
// Back Propagation Neural Network Class (BackPropagation.class)
//
// Created by Anthony J. Papagelis & Dong Soo Kim
//
//  DateCreated:	15 September, 2001
//  Last Update:	24 October, 2001

public class BackPropagation extends Thread {
    // Private Variables

	// Error Function variable that is calculated using the CalculateOverallError() function
	private double	OverallError;

	// The minimum Error Function defined by the user
	private double	MinimumError;

	// The user-defined expected output pattern for a set of samples
	private double	ExpectedOutput[][];

	// The user-defined input pattern for a set of samples
	private double	Input[][];

	// User defined learning rate - used for updating the network weights
	private double	LearningRate;

	// Users defined momentum - used for updating the network weights
	private double	Momentum;

	// Number of layers in the network - includes the input, output and hidden layers
	private  int	NumberOfLayers;

	// Number of training sets
	private  int	NumberOfSamples;

	// Current training set/sample that is used to train network
	private  int	SampleNumber;

	// Maximum number of Epochs before the traing stops training - user defined
	private long	MaximumNumberOfIterations;

    // Public Variables
	public LAYER	Layer[];
	public  double	ActualOutput[][];

	// added by DSK, needed by applet.
	Welcome2 parent;
	long delay = 0;
	boolean die = false;

    // Calculate the node activations
	public void FeedForward(){

		int i;

		// Since no weights contribute to the output 
		// vector from the input layer,
		// assign the input vector from the input layer 
		// to all the node in the first hidden layer
           	for (i = 0; i < Layer[0].Node.length; i++)
			Layer[0].Node[i].Output = Layer[0].Input[i];

		Layer[1].Input = Layer[0].Input;
		for (i = 1; i < NumberOfLayers; i++) {
			Layer[i].FeedForward();

			// Unless we have reached the last layer, assign the layer i's output vector
			// to the (i+1) layer's input vector
			if (i != NumberOfLayers-1)
				Layer[i+1].Input = Layer[i].OutputVector();
		}

	} // FeedForward()

	// Back propagated the network outputy error through 
	// the network to update the weight values
	public void UpdateWeights() {


		CalculateSignalErrors();
		BackPropagateError();

	}


	private void CalculateSignalErrors() {

		int i,j,k,OutputLayer;
		double Sum;

		OutputLayer = NumberOfLayers-1;

	       	// Calculate all output signal error
		for (i = 0; i < Layer[OutputLayer].Node.length; i++) 
			Layer[OutputLayer].Node[i].SignalError 
				= (ExpectedOutput[SampleNumber][i] - 
					Layer[OutputLayer].Node[i].Output) * 
					Layer[OutputLayer].Node[i].Output * 
					(1-Layer[OutputLayer].Node[i].Output);

	       	// Calculate signal error for all nodes in the hidden layer
		// (back propagate the errors)
		for (i = NumberOfLayers-2; i > 0; i--) {
			for (j = 0; j < Layer[i].Node.length; j++) {
				Sum = 0;

				for (k = 0; k < Layer[i+1].Node.length; k++)
					Sum = Sum + Layer[i+1].Node[k].Weight[j] * 
						Layer[i+1].Node[k].SignalError;

				Layer[i].Node[j].SignalError 
					= Layer[i].Node[j].Output*(1 - 
						Layer[i].Node[j].Output)*Sum;
			}
		}

	}


	private void BackPropagateError() {

		int i,j,k;

		// Update Weights
		for (i = NumberOfLayers-1; i > 0; i--) {
			for (j = 0; j < Layer[i].Node.length; j++) {
				// Calculate Bias weight difference to node j
				Layer[i].Node[j].ThresholdDiff 
					= LearningRate * 
					Layer[i].Node[j].SignalError + 
					Momentum*Layer[i].Node[j].ThresholdDiff;

				// Update Bias weight to node j
				Layer[i].Node[j].Threshold = 
					Layer[i].Node[j].Threshold + 
					Layer[i].Node[j].ThresholdDiff;

				// Update Weights
				for (k = 0; k < Layer[i].Input.length; k++) {
					// Calculate weight difference between node j and k
					Layer[i].Node[j].WeightDiff[k] = 
						LearningRate * 
						Layer[i].Node[j].SignalError*Layer[i-1].Node[k].Output +
						Momentum*Layer[i].Node[j].WeightDiff[k];

					// Update weight between node j and k
					Layer[i].Node[j].Weight[k] = 
						Layer[i].Node[j].Weight[k] + 
						Layer[i].Node[j].WeightDiff[k];
				}
			}
		}
	}


	private void CalculateOverallError() {

		int i,j;

		OverallError = 0;
       	
		for (i = 0; i < NumberOfSamples; i++)
			for (j = 0; j < Layer[NumberOfLayers-1].Node.length; j++) {
           			OverallError = 
					OverallError + 
					0.5*( Math.pow(ExpectedOutput[i][j] - 
						ActualOutput[i][j],2) );
		}
	}
	

	public  BackPropagation(int NumberOfNodes[], 
					double InputSamples[][], 
					double OutputSamples[][], 
					double LearnRate, 
					double Moment, 
					double MinError, 
					long MaxIter) {

		int i,j;

		// Initiate variables
		NumberOfSamples = InputSamples.length;
		MinimumError = MinError;
		LearningRate = LearnRate;
		Momentum = Moment;
		NumberOfLayers = NumberOfNodes.length;
		MaximumNumberOfIterations = MaxIter;

		// Create network layers
		Layer = new LAYER[NumberOfLayers];

		// Assign the number of node to the input layer
		Layer[0] = new LAYER(NumberOfNodes[0],NumberOfNodes[0]);

		// Assign number of nodes to each layer
		for (i = 1; i < NumberOfLayers; i++) 
			Layer[i] = new LAYER(NumberOfNodes[i],NumberOfNodes[i-1]);

		Input = new double[NumberOfSamples][Layer[0].Node.length];
		ExpectedOutput = new double[NumberOfSamples][Layer[NumberOfLayers-1].Node.length];
		ActualOutput = new double[NumberOfSamples][Layer[NumberOfLayers-1].Node.length];

		// Assign input set
		for (i = 0; i < NumberOfSamples; i++)
			for (j = 0; j < Layer[0].Node.length; j++)
				Input[i][j] = InputSamples[i][j];

		// Assign output set
		for (i = 0; i < NumberOfSamples; i++)
			for (j = 0; j < Layer[NumberOfLayers-1].Node.length; j++)
				ExpectedOutput[i][j] = OutputSamples[i][j];
	}

	public void TrainNetwork() {

		int i;
		long k=0;

		// System.out.println("" + MaximumNumberOfIterations);

		/*
		try { System.in.read(); }
		catch(IOException _e) { }
		*/
		do{
			// For each pattern
			for (SampleNumber = 0; SampleNumber < NumberOfSamples; SampleNumber++) {
           			for (i = 0; i < Layer[0].Node.length; i++)
					Layer[0].Input[i] = Input[SampleNumber][i];

				FeedForward();
				// Assign calculated output vector from network to ActualOutput
           			for (i = 0; i < Layer[NumberOfLayers-1].Node.length; i++)
           				ActualOutput[SampleNumber][i] = 
						Layer[NumberOfLayers-1].Node[i].Output;
				UpdateWeights();

				// if we've been told to stop training, then
				// notify the parent that we're exiting
				// then stop thread execution
				if (die){ 
					if (parent != null) {
						parent.net_done();
					} // if
					
					return; 
				} // if

				// added by DSK
				if (parent != null) {
					parent.draw();
					try { sleep(delay); }
					catch(InterruptedException _e) { 
						System.out.println("Interrupted!");
					} // catch
				} // if
			}

			k++;
			// Calculate Error Function
			CalculateOverallError();
		} while ((OverallError > MinimumError) && (k < MaximumNumberOfIterations));

		// if we finished normally, which we must have if this
		// point is reached .. then let the parent know
		if (parent != null) {
			parent.draw();
			parent.net_done();
		} // if
	}

	// needed to implement the drawing of the network.
	public LAYER[] get_layers() { return Layer; }

	// called when testing the network.
	// does not interfere with the error plotting
	// of the applet.
	public int test(double[] input) {
		int winner = 0;
		NODE[] output_nodes;

		for (int j = 0; j < Layer[0].Node.length; j++)
			Layer[0].Input[j] = input[j];

		FeedForward();

		// get the last layer of nodes (the outputs)
		output_nodes = (Layer[Layer.length - 1]).get_nodes();

		for (int k=0; k < output_nodes.length; k++) {
			if (output_nodes[winner].Output < 
					output_nodes[k].Output) {
				winner = k;
			} // if
		} // for

		// if (parent != null) { parent.draw(); }

		return winner;
	} // test()

	// if drawing of the network is desired, this
	// should be called before TrainNetwork(), otherwise
	// the drawing will not be properly done.
	public void set_parent( Welcome2 applet ) {
		parent = applet;
	} // set_parent()

	// report the batch error.
	public double get_error() { 
		CalculateOverallError();

		return OverallError;
	} // get_error()

	// to change the delay in the network
	public void set_delay(long time) {
		if (time >= 0) { 
			delay = time; 
		} // if
	} // set_delay()

	// needed to implement threading.
	public void run() {
		TrainNetwork();
	} // run()

	// to notify the network to stop training.
	public void kill() { die = true; }
}

