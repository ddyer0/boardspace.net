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
package online.search.neuralnet.optimizer;

import online.search.neuralnet.math.Matrix;
import online.search.neuralnet.math.Vec;

/**
 * Updates Weights and biases based on:
 * v_prev = v
 * v = γ * v - η * dC/dW
 * W += -γ * v_prev + (1 + γ) * v
 * <p>
 * γ is the momentum (i.e. how much of the last gradient will we use again)
 * η is the learning rate
 * <p>
 */
@SuppressWarnings("unused")
public class Nesterov implements Optimizer {

    private final double learningRate;
    private final double momentum;
    private Matrix lastDW;
    private Vec lastDBias;

    public Nesterov(double learningRate, double momentum) {
        this.learningRate = learningRate;
        this.momentum = momentum;
    }

    public Nesterov(double learningRate) {
        this(learningRate, 0.9);
    }

    @Override
    public void updateWeights(Matrix weights, Matrix dCdW) {
        if (lastDW == null) {
            lastDW = new Matrix(dCdW.rows(), dCdW.cols());
        }
        Matrix lastDWCopy = lastDW.copy();
        lastDW.mul(momentum).sub(dCdW.mul(learningRate));
        weights.add(lastDWCopy.mul(-momentum).add(lastDW.copy().mul(1 + momentum)));
    }

    @Override
    public Vec updateBias(Vec bias, Vec dCdB) {
        if (lastDBias == null) {
            lastDBias = new Vec(dCdB.dimension());
        }
        Vec lastDBiasCopy = lastDBias;
        lastDBias = lastDBias.mul(momentum).sub(dCdB.mul(learningRate));
        return bias.add(lastDBiasCopy.mul(-momentum).add(lastDBias.mul(1 + momentum)));
    }

    @Override
    public Optimizer copy() {
        return new Nesterov(learningRate, momentum);
    }
}
