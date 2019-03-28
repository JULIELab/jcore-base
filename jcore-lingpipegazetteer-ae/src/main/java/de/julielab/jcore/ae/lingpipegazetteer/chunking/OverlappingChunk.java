/** 
 * OverlappingChunk.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Affero General Public License (LGPL) v3.0
 *
 * Author: tomanek
 * 
 * Current version: 1.0 	
 * Since version:   1.0
 *
 * Creation date: Jan 15, 2008 
 * 
 * An object storing all chunks (found matches) which share 
 * some span in the text, i.e. are overlapping. This is needed 
 * and used for approximate matching to resolve conflicts.
 **/

package de.julielab.jcore.ae.lingpipegazetteer.chunking;

import com.aliasi.chunk.Chunk;

import java.util.*;

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
