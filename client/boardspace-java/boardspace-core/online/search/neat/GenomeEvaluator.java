package online.search.neat;
/*
Copyright 2006-2025 by Dave Dyer

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

/**
 * Jan 20025
 * credit where credit is due.  This package is based on a set of classes that accompanied 
 * a series of videos about implementing NEAT in java.  The code provided there proved to 
 * be structurally correct, but in detail pretty useless.  In particular, it didn't include
 * any neural nets, and all the learning/mutation in it was measured against its own structure.
 * But still, it was a worthwhile starting point.
 * https://www.youtube.com/watch?v=PXxRjKNX1uw
 */
public interface GenomeEvaluator {
	public double evaluate(Genome g);
	public Genome createNetwork();
	public void setBest(Genome g);
	default public void startGeneration() {}
	default public void finishGeneration() {}
}
