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
 * Updates Weights and biases based on a constant learning rate - i.e. W -= η * dC/dW
 */
public record GradientDescent(double learningRate) implements Optimizer {

    @Override
    public void updateWeights(Matrix weights, Matrix dCdW) {
        weights.sub(dCdW.mul(learningRate));
    }

    @Override
    public Vec updateBias(Vec bias, Vec dCdB) {
        return bias.sub(dCdB.mul(learningRate));
    }

    @Override
    public Optimizer copy() {
        // no need to make copies since this optimizer has
        // no state. Same instance can serve all layers.
        return this;
    }
}
