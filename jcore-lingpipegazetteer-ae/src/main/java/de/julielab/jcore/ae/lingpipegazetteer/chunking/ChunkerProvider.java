
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
