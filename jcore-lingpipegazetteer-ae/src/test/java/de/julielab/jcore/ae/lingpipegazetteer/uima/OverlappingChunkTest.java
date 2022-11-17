package de.julielab.jcore.ae.lingpipegazetteer.uima;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.ChunkFactory;
import de.julielab.jcore.ae.lingpipegazetteer.chunking.OverlappingChunk;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OverlappingChunkTest {
	@Test
	public void testGetBestchunk() {
		// Say the dictionary includes the term "L1CAM". An approximate matching algorithm might then also find the
		// variants "(L1CAM", "L1CAM)" and "(L1CAM)". We assume the first two variants have been filtered out (as it
		// happens by the filterChunks() method used by the GazetteerAnnotator). Then we are left with two choices,
		// namely the original term and the same term enclosed in parenthesis. We have the following possibilities:
		// 1. Just take the chunk with the higher score.
		// 2. Take the longer match.
		// The problem is now that for different cases, sometimes 1 and sometimes 2 is the better choice. If we had the
		// dictionary entries "LCAT14" as well as "LCAT" but the text to match would be "LCAT 14". Here the best score
		// would go to the shorter form "LCAT" but we would have preferred the longer form "LCAT 14" which, however, has
		// some penalty due to the inserted whitespace.
		// Due to these concerns the algorithm should discriminate between cases where a shorter possibility would be
		// preferable which is the case when the whole match is enclosed in parenthesis, for example, or where a longer
		// match seems to be the better choice.
		List<Chunk> chunking;
		Chunk correct;
		Chunk tooLong;
		Chunk tooShort;
		OverlappingChunk overlappingChunk;
		List<Chunk> bestChunks;

		correct = ChunkFactory.createChunk(1, 6, 0);// L1CAM
		tooLong = ChunkFactory.createChunk(0, 7, 2);// (L1CAM)
		chunking = new ArrayList<>();
		chunking.add(correct);
		chunking.add(tooLong);
		overlappingChunk = GazetteerAnnotator.groupOverlappingChunks(chunking, "(L1CAM)").get(0);
		bestChunks = overlappingChunk.getBestChunks();
		assertEquals(1, bestChunks.size());
		assertEquals(correct, bestChunks.get(0));

		tooShort = ChunkFactory.createChunk(0, 4, 0); // LCAT
		correct = ChunkFactory.createChunk(0, 7, 1); // LCAT 14
		chunking = new ArrayList<>();
		chunking.add(correct);
		chunking.add(tooShort);
		overlappingChunk = GazetteerAnnotator.groupOverlappingChunks(chunking, "LCAT 14").get(0);
		bestChunks = overlappingChunk.getBestChunks();
		assertEquals(1, bestChunks.size());
		assertEquals(correct, bestChunks.get(0));
		
	}
}
