package online.search.neuralnet.optimizer;

import online.search.neuralnet.math.Matrix;
import online.search.neuralnet.math.Vec;

public interface Optimizer {

    void updateWeights(Matrix weights, Matrix dCdW);

    Vec updateBias(Vec bias, Vec dCdB);

    Optimizer copy();

}
