/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the  Eclipse Public License (EPL) v3.0
 */

package de.julielab.jcore.ae.mstparser.main;

import java.io.IOException;

import edu.upenn.seas.mstparser.DependencyParser;

public interface MSTParserWrapper {
	public DependencyParser loadModel();
	public String predict(DependencyParser mstParser, String inputSentence) throws IOException;
}
