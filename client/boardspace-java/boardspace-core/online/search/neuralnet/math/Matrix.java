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
package online.search.neuralnet.math;


import java.util.Arrays;
import java.util.StringJoiner;

import static java.lang.String.format;
import static java.lang.System.arraycopy;
import static java.util.Arrays.stream;

/**
 * Careful: not immutable. Most matrix operations are made on same object.
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "UnusedReturnValue"})
public class Matrix {

    private final double[][] data;
    private final int rows;
    private final int cols;

    public Matrix(double[][] data) {
        this.data = data;
        rows = data.length;
        cols = data[0].length;
    }

    public Matrix(int rows, int cols) {
        this(new double[rows][cols]);
    }

    public Vec multiply(Vec v) {
        double[] out = new double[rows];
        for (int y = 0; y < rows; y++)
            out[y] = new Vec(data[y]).dot(v);

        return new Vec(out);
    }

    public Matrix map(Function fn) {
        for (int y = 0; y < rows; y++)
            for (int x = 0; x < cols; x++)
                data[y][x] = fn.apply(data[y][x]);

        return this;
    }

    public int rows() {
        return rows;
    }

    public int cols() {
        return cols;
    }

    public Matrix mul(double s) {
        return map(value -> s * value);
    }

    public double[][] getData() {
        return data;
    }

    public Matrix add(Matrix other) {
        assertCorrectDimension(other);

        for (int y = 0; y < rows; y++)
            for (int x = 0; x < cols; x++)
                data[y][x] += other.data[y][x];

        return this;
    }

    public Matrix sub(Matrix other) {
        assertCorrectDimension(other);

        for (int y = 0; y < rows; y++)
            for (int x = 0; x < cols; x++)
                data[y][x] -= other.data[y][x];

        return this;
    }

    public Matrix fillFrom(Matrix other) {
        assertCorrectDimension(other);

        for (int y = 0; y < rows; y++)
            if (cols >= 0) arraycopy(other.data[y], 0, data[y], 0, cols);

        return this;
    }

    public double average() {
        return stream(data).flatMapToDouble(Arrays::stream).average().getAsDouble();
    }

    public double variance() {
        double avg = average();
        return stream(data).flatMapToDouble(Arrays::stream).map(a -> (a - avg) * (a - avg)).average().getAsDouble();
    }

    // -------------------------------------------------------------------------

    private void assertCorrectDimension(Matrix other) {
        if (rows != other.rows || cols != other.cols)
            throw new IllegalArgumentException(format("Matrix of different dim: Input is %d x %d, Vec is %d x %d", rows, cols, other.rows, other.cols));
    }

    public Matrix copy() {
        Matrix m = new Matrix(rows, cols);
        for (int y = 0; y < rows; y++)
            if (cols >= 0) arraycopy(data[y], 0, m.data[y], 0, cols);

        return m;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Matrix.class.getSimpleName() + "[", "]")
            .add("data=" + Arrays.deepToString(data))
            .toString();
    }
}

