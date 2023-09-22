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

import static java.lang.Math.exp;
import static java.lang.Math.log;

import online.search.neuralnet.math.Function;
import online.search.neuralnet.math.Vec;

@SuppressWarnings("unused")
public class Activation {

    private final String name;
    private Function fn;
    private Function dFn;

    public Activation(String name) {
        this.name = name;
    }

    public Activation(String name, Function fn, Function dFn) {
        this.name = name;
        this.fn = fn;
        this.dFn = dFn;
    }

    // For most activation function it suffice to map each separate element. 
    // I.e. they depend only on the single component in the vector.
    public Vec fn(Vec in) {
        return in.map(fn);
    }

    public Vec dFn(Vec out) {
        return out.map(dFn);
    }

    // Also when calculating the Error change rate in terms of the input (dCdI)
    // it is just a matter of multiplying, i.e. ∂C/∂I = ∂C/∂O * ∂O/∂I.
    public Vec dCdI(Vec out, Vec dCdO) {
        return dCdO.elementProduct(dFn(out));
    }

    public String getName() {
        return name;
    }


    // --------------------------------------------------------------------------
    // --- A few predefined ones ------------------------------------------------
    // --------------------------------------------------------------------------
    // The simple properties of most activation functions as stated above makes
    // it easy to create the majority of them by just providing lambdas for
    // fn and the diff dfn.

    public static Activation ReLU = new Activation(
        "ReLU",
        x -> x <= 0 ? 0 : x,                // fn
        x -> x <= 0 ? 0 : 1                 // dFn
    );

    public static Activation Leaky_ReLU = new Activation(
        "Leaky_ReLU",
        x -> x <= 0 ? 0.01 * x : x,         // fn
        x -> x <= 0 ? 0.01 : 1              // dFn
    );


    public static Activation Sigmoid = new Activation(
        "Sigmoid",
        Activation::sigmoidFn,                      // fn
        x -> sigmoidFn(x) * (1.0 - sigmoidFn(x))    // dFn
    );


    public static Activation Softplus = new Activation(
        "Softplus",
        x -> log(1.0 + exp(x)),             // fn
        Activation::sigmoidFn               // dFn
    );

    public static Activation Identity = new Activation(
        "Identity",
        x -> x,                             // fn
        x -> 1                              // dFn
    );


    // --------------------------------------------------------------------------
    // Softmax needs a little extra love since element output depends on more
    // than one component of the vector. Simple element mapping will not suffice.
    // --------------------------------------------------------------------------
    public static Activation Softmax = new Activation("Softmax") {
        @Override
        public Vec fn(Vec in) {
            double[] data = in.getData();
            double sum = 0;
            double max = in.max();    // Trick: translate the input by largest element to avoid overflow.
            for (double a : data)
                sum += exp(a - max);

            double finalSum = sum;
            return in.map(a -> exp(a - max) / finalSum);
        }

        @Override
        public Vec dCdI(Vec out, Vec dCdO) {
            double x = out.elementProduct(dCdO).sumElements();
            Vec sub = dCdO.sub(x);
            return out.elementProduct(sub);
        }
    };


    // --------------------------------------------------------------------------

    private static double sigmoidFn(double x) {
        return 1.0 / (1.0 + exp(-x));
    }

}
