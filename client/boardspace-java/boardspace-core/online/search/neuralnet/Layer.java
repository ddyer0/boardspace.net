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
package online.search.neuralnet;

import online.search.neuralnet.math.Matrix;
import online.search.neuralnet.math.Vec;
import online.search.neuralnet.optimizer.Optimizer;

/**
 * A single layer in the network.
 * Contains the weights and biases coming into this layer.
 */
public class Layer {

    private final int size;
    private final ThreadLocal<Vec> out = new ThreadLocal<>();
    private final Activation activation;
    private Optimizer optimizer;
    private Matrix weights;
    private Vec bias;
    private double l2 = 0;

    private Layer precedingLayer;

    // Not yet realized changes to the weights and biases ("observed things not yet learned")
    private transient Matrix deltaWeights;
    private transient Vec deltaBias;
    private transient int deltaWeightsAdded = 0;
    private transient int deltaBiasAdded = 0;

    public Layer(int size, Activation activation) {
        this(size, activation, 0);
    }

    public Layer(int size, Activation activation, double initialBias) {
        this.size = size;
        bias = new Vec(size).map(x -> initialBias);
        deltaBias = new Vec(size);
        this.activation = activation;
    }

    public Layer(int size, Activation activation, Vec bias) {
        this.size = size;
        this.bias = bias;
        deltaBias = new Vec(size);
        this.activation = activation;
    }

    public int size() {
        return size;
    }

    /**
     * Feed the in-vector, i, through this layer.
     * Stores a copy of the out vector.
     *
     * @param i The input vector
     * @return The out vector o (i.e. the result of o = iW + b)
     */
    public Vec evaluate(Vec i) {
        if (!hasPrecedingLayer()) {
            out.set(i);    // No calculation i input layer, just store data
        } else {
            out.set(activation.fn(i.mul(weights).add(bias)));
        }
        return out.get();
    }

    public Vec getOut() {
        return out.get();
    }

    public Activation getActivation() {
        return activation;
    }

    public void setWeights(Matrix weights) {
        this.weights = weights;
        deltaWeights = new Matrix(weights.rows(), weights.cols());
    }

    public void setOptimizer(Optimizer optimizer) {
        this.optimizer = optimizer;
    }

    public void setL2(double l2) {
        this.l2 = l2;
    }

    public Matrix getWeights() {
        return weights;
    }

    public Layer getPrecedingLayer() {
        return precedingLayer;
    }

    public void setPrecedingLayer(Layer precedingLayer) {
        this.precedingLayer = precedingLayer;
    }

    public boolean hasPrecedingLayer() {
        return precedingLayer != null;
    }

    public Vec getBias() {
        return bias;
    }

    /**
     * Add upcoming changes to the Weights and Biases.
     * This does not mean that the network is updated.
     */
    public synchronized void addDeltaWeightsAndBiases(Matrix dW, Vec dB) {
        deltaWeights.add(dW);
        deltaWeightsAdded++;
        deltaBias = deltaBias.add(dB);
        deltaBiasAdded++;
    }

    /**
     * Takes an average of all added Weights and Biases and tell the
     * optimizer to apply them to the current weights and biases.
     * <p>
     * Also applies L2 regularization on the weights if used.
     */
    public synchronized void updateWeightsAndBias() {
        if (deltaWeightsAdded > 0) {
            if (l2 > 0)
                weights.map(value -> value - l2 * value);

            Matrix average_dW = deltaWeights.mul(1.0 / deltaWeightsAdded);
            optimizer.updateWeights(weights, average_dW);
            deltaWeights.map(a -> 0);   // Clear
            deltaWeightsAdded = 0;
        }

        if (deltaBiasAdded > 0) {
            Vec average_bias = deltaBias.mul(1.0 / deltaBiasAdded);
            bias = optimizer.updateBias(bias, average_bias);
            deltaBias = deltaBias.map(a -> 0);  // Clear
            deltaBiasAdded = 0;
        }
    }


    // ------------------------------------------------------------------


    public LayerState getState() {
        return new LayerState(this);
    }

    @SuppressWarnings("unused")
    public static class LayerState {

        double[][] weights;
        double[] bias;
        String activation;

        public LayerState(Layer layer) {
            weights = layer.getWeights() != null ? layer.getWeights().getData() : null;
            bias = layer.getBias().getData();
            activation = layer.activation.getName();
        }

        public double[][] getWeights() {
            return weights;
        }

        public double[] getBias() {
            return bias;
        }
    }
}
