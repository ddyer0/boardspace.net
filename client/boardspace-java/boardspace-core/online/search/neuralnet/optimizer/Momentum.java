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
 * v = γ * v_prev + η * dC/dW
 * W -= v
 * <p>
 * γ is the momentum (i.e. how much of the last gradient will we use again)
 * η is the learning rate
 */
public class Momentum implements Optimizer {

    private final double learningRate;
    private final double momentum;
    private Matrix lastDW;
    private Vec lastDBias;

    public Momentum(double learningRate, double momentum) {
        this.learningRate = learningRate;
        this.momentum = momentum;
    }

    public Momentum(double learningRate) {
        this(learningRate, 0.9);
    }

    @Override
    public void updateWeights(Matrix weights, Matrix dCdW) {
        if (lastDW == null) {
            lastDW = dCdW.copy().mul(learningRate);
        } else {
            lastDW.mul(momentum).add(dCdW.copy().mul(learningRate));
        }
        weights.sub(lastDW);
    }

    @Override
    public Vec updateBias(Vec bias, Vec dCdB) {
        if (lastDBias == null) {
            lastDBias = dCdB.mul(learningRate);
        } else {
            lastDBias = lastDBias.mul(momentum).add(dCdB.mul(learningRate));
        }
        return bias.sub(lastDBias);
    }

    @Override
    public Optimizer copy() {
        return new Momentum(learningRate, momentum);
    }
}
