/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/
package de.julielab.jcore.ae.lingpipegazetteer.chunking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aliasi.chunk.Chunk;

public class OverlappingChunk {

	private int begin;

	public int begin() {
		return begin;
	}

	public int end() {
		return end;
	}

	private int end;

	ArrayList<Chunk> chunks;

	private Map<Chunk, String> coveredTextMap;

	private static Set<Character> ignoredAffixCharacters;
	static {
		ignoredAffixCharacters = new HashSet<>();
		ignoredAffixCharacters.add('(');
		ignoredAffixCharacters.add(')');
		ignoredAffixCharacters.add('[');
		ignoredAffixCharacters.add(']');
		ignoredAffixCharacters.add('{');
		ignoredAffixCharacters.add('}');
		ignoredAffixCharacters.add('-');
		ignoredAffixCharacters.add('+');
		ignoredAffixCharacters.add('/');
		ignoredAffixCharacters.add('\\');
		ignoredAffixCharacters.add('\'');
		ignoredAffixCharacters.add('"');

	}

	private String chunkedString;

	public OverlappingChunk(int begin, int end, Chunk chunk, String chunkedString) {
		this.begin = begin;
		this.end = end;
		this.chunkedString = chunkedString;
		chunks = new ArrayList<Chunk>();
		chunks.add(chunk);
		coveredTextMap = new HashMap<>();
	}

	public boolean isOverlappingSpan(int newBegin, int newEnd) {
		// returns true if spans are overlapping
		boolean overlapping = false;
		if (newBegin >= begin && newEnd <= end) {
			overlapping = true;
		}
		if (newBegin < begin && newEnd > begin) {
			overlapping = true;
		}
		if (newBegin < end && newEnd > end) {
			overlapping = true;
		}

		return overlapping;
	}

	public void addChunk(int newBegin, int newEnd, Chunk chunk) {
		// check whether span longer, if yes, modify begin/end
		int newSpan = newEnd - newBegin;
		int span = end - begin;
		if (newSpan > span) {
			this.begin = newBegin;
			this.end = newEnd;
		}

		// add chunk
		chunks.add(chunk);
	}

	public String toString() {
		String out = "max span: " + begin + "-" + end;
		return out;
	}

	public String toStringAll() {
		StringBuffer out = new StringBuffer();
		out.append("max span: " + begin + "-" + end);
		for (Chunk chunk : chunks) {
			out.append("\n" + chunk + "\t" + showChunk(chunk));
		}
		return out.toString();
	}

	private String showChunk(Chunk chunk) {
		int start = chunk.start();
		int end = chunk.end();
		double score = chunk.score();
		String chunkText = "start=" + start + " end=" + end + " score=" + score;
		return chunkText;
	}

	/**
	 * new: returns the best of all overlapping chunks, i.e. the one longest chunk with the lowest score. Longer chunks
	 * always win, independently of the score.
	 * 
	 * old: returns the best of all overlapping chunks, i.e. the one with the lowest score. If two chunks have the same
	 * score, the longest chunk is favorized.
	 * 
	 * @param chunkedString
	 * 
	 */
	public List<Chunk> getBestChunks() {
		List<Chunk> bestChunk = new ArrayList<>();
		for (Chunk chunk : chunks) {
			if (bestChunk.isEmpty()) {
				bestChunk.add(chunk);
			} else if (getChunkSpan(chunk) > getChunkSpan(bestChunk.get(0))) {
				// The list elements always have the same length and score. If the new chunk is longer than the first
				// element, it is longer than all elements and thus we just clear the whole list.
				bestChunk.clear();
				bestChunk.add(chunk);
			} else if (getChunkSpan(chunk) == getChunkSpan(bestChunk.get(0))) {
				if (chunk.score() < bestChunk.get(0).score()) {
					// The list elements always have the same length and score. If the new chunk is of lower score than
					// the first
					// element, it has lower score than all elements and thus we just clear the whole list.
					bestChunk.clear();
					bestChunk.add(chunk);
				} else if (chunk.score() == bestChunk.get(0).score()) {
					// This chunk has the exact same length and score like the best chunk(s) until now. Could just be an
					// ambiguous dictionary entry, i.e. the same entry text with a different type.
					bestChunk.add(chunk);
				}
			}
		}
		return bestChunk;

		// Chunk bestChunk = null;
		// for (Chunk chunk : chunks) {
		// if (bestChunk == null) {
		// bestChunk = chunk;
		// } else if (chunk.score() < bestChunk.score()) {
		// bestChunk = chunk;
		// } else if (chunk.score() == bestChunk.score()) {
		// if (getChunkSpan(bestChunk) < getChunkSpan(chunk)) {
		// bestChunk = chunk;
		// }
		// }
		// }
		// return bestChunk;
	}

	/**
	 * Returns the <em>effective</em> chunk length, i.e. leading and trailing parenthesis and dashes are ignored.
	 * 
	 * @param chunk
	 * @return
	 */
	private int getChunkSpan(Chunk chunk) {
		String coveredText = coveredTextMap.get(chunk);
		if (null == coveredText) {
			coveredText = chunkedString.substring(chunk.start(), chunk.end());
			StringBuilder normalizedTextBuilder = new StringBuilder();
			for (int i = 0; i < coveredText.length(); i++) {
				char c = coveredText.charAt(i);
				if (!ignoredAffixCharacters.contains(c))
					normalizedTextBuilder.append(c);
			}
			coveredTextMap.put(chunk, normalizedTextBuilder.toString());
			return normalizedTextBuilder.length();
		}
		return coveredText.length();
		// int newChunkStart = chunk.start();
		// int newChunkEnd = chunk.end();
		// char current;
		// do {
		// current = coveredText.charAt(newChunkStart - chunk.start());
		// newChunkStart++;
		// } while (ignoredAffixCharacters.contains(current) && newChunkStart < chunk.end() - 1);
		//
		// do {
		// current = coveredText.charAt((coveredText.length() - 1) + (newChunkEnd - chunk.end()));
		// newChunkEnd--;
		// } while (ignoredAffixCharacters.contains(current) && newChunkEnd > chunk.start() + 1);
		// return (newChunkEnd - newChunkStart);
	}
}
