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
package mutton;

/**
 * This class generates permutations of N things.
 * Adapted from code provided by Michael Gilleland at http://www.merriampark.com/perm.htm
 */
public class PermutationGenerator {

	// The total number of things
	private int N;

	// The array of permutations
	private int [] a;

	/**
	 * Construct a new generator for N things.
	 */
	public PermutationGenerator (int n) {
		this.N = n;
		a = new int [n];

		reset();
	}

	/**
	 * Reset the generator to start over at the first combination.
	 */
	public void reset () {
		a[0] = -1;
	}

	/**
	 * Indicate if there are more combinations to come.
	 */
	public boolean hasMore () {
		// We are done if the current array counts down from n-1 to 0 in order.
		// Any other configuration indicates that there are more configs available.
		for (int i=0; i < N; i++) {
			if (a[i] != (N-1-i)) {
				return true;
			}
		}

		// We've hit the end of configs.
		return false;
	}

	/**
	 * Return the next combination.
	 */
	public int [] next () {
		if (a[0] < 0) {
			// Seed the initial permutation
			for (int i=0; i < N; i++) {
				a[i] = i;
			}
		} else if (hasMore()) {
			// Find largest index j with a[j] < a[j+1]
			int j = N - 2;
			while (a[j] > a[j+1]) {
				j--;
			}

			// Find index k such that a[k] is smallest integer
			// greater than a[j] to the right of a[j]
			int k = N - 1;
			while (a[j] > a[k]) {
				k--;
			}

			// Interchange a[j] and a[k]
			int temp = a[k];
			a[k] = a[j];
			a[j] = temp;

			// Put tail end of permutation after jth position in increasing order
			int r = N - 1;
			int s = j + 1;
			while (r > s) {
				temp = a[s];
				a[s] = a[r];
				a[r] = temp;
				r--;
				s++;
			}
		}

		return a;
	}
}
