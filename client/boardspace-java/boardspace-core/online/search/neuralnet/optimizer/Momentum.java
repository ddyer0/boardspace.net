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
