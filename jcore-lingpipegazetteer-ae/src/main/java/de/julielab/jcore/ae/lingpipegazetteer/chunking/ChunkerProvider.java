/** 
 * ChunkerProvider.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Affero General Public License (LGPL) v3.0  
 *
 * Author: landefeld
 * 
 * Current version: //TODO insert current version number 	
 * Since version:   //TODO insert version number of first appearance of this class
 *
 * Creation date: 16.09.2008 
 * 
 * //TODO insert short description
 **/

package de.julielab.jcore.ae.lingpipegazetteer.chunking;

import com.aliasi.chunk.Chunker;

import java.util.Set;

public interface ChunkerProvider {
	public Chunker getChunker();

	public Set<String> getStopWords();
	
	public boolean getUseApproximateMatching();
	
	public boolean getNormalize();
	
	public boolean getTransliterate();
	
	public boolean getCaseSensitive();
}
