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

import online.search.neuralnet.math.Vec;

@SuppressWarnings("unused")
public interface CostFunction {

    String getName();

    double getTotal(Vec expected, Vec actual);

    Vec getDerivative(Vec expected, Vec actual);


    // --------------------------------------------------------------

    /**
     * Cost function: Mean square error, C = 1/n * ∑(y−exp)^2
     */
    class MSE implements CostFunction {
        @Override
        public String getName() {
            return "MSE";
        }

        @Override
        public double getTotal(Vec expected, Vec actual) {
            Vec diff = expected.sub(actual);
            return diff.dot(diff) / actual.dimension();
        }

        @Override
        public Vec getDerivative(Vec expected, Vec actual) {
            return actual.sub(expected).mul(2.0 / actual.dimension());
        }
    }

    /**
     * Cost function: Quadratic, C = ∑(y−exp)^2
     */
    class Quadratic implements CostFunction {
        @Override
        public String getName() {
            return "Quadratic";
        }

        @Override
        public double getTotal(Vec expected, Vec actual) {
            Vec diff = actual.sub(expected);
            return diff.dot(diff);
        }

        @Override
        public Vec getDerivative(Vec expected, Vec actual) {
            return actual.sub(expected).mul(2);
        }
    }

    /**
     * Cost function: HalfQuadratic, C = 0.5 ∑(y−exp)^2
     */
    class HalfQuadratic implements CostFunction {
        @Override
        public String getName() {
            return "HalfQuadratic";
        }

        @Override
        public double getTotal(Vec expected, Vec actual) {
            Vec diff = expected.sub(actual);
            return diff.dot(diff) * 0.5;
        }

        @Override
        public Vec getDerivative(Vec expected, Vec actual) {
            return actual.sub(expected);
        }
    }


}
