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
 * This class generates combinations of N things taken R at a time.
 * Adapted from code provided by Michael Gilleland at http://www.merriampark.com/comb.htm
 */
public class CombinationGenerator {

	// The total number of things (n in "N things taken R at a time")
	private int N;

	// The size of the sample (r in "N things taken R at a time")
	private int R;

	// The array of combinations
	private int [] a;

	/**
	 * Construct a new generator for N things taken R at a time.
	 */
	public CombinationGenerator (int n, int r) {
		this.N = n;
		this.R = r;
		a = new int [r];

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
		return (a[0] != (N-R));
	}

	/**
	 * Return the next combination.
	 */
	public int [] next () {
		if (a[0] < 0) {
			// Seed the initial combination
			for (int i=0; i < R; i++) {
				a[i] = i;
			}
		} else if (hasMore()) {
			// Walk from end to find where the carry will stop propagating
			int i = R - 1;
			while ((i >= 0) && (a[i] == (N - R + i))) {
				i -= 1;
			}

			// Increment the target digit ...
			a[i] = a[i] + 1;
		
			// And walk back up the number, resetting each digit to the next value.
			for (int j = i + 1; j < R; j++) {
				a[j] = a[j-1] + 1;
			}
		}

		return a;
	}
}
