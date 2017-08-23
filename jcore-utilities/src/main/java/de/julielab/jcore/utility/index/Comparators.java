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
package de.julielab.jcore.utility.index;

import java.util.Comparator;

import org.apache.uima.jcas.tcas.Annotation;

/**
 * This class offers some commonly used comparators on annotations.
 * 
 * @author faessler
 *
 */
public class Comparators {
	/**
	 * Compares annotations strictly by their offsets. Two annotations are
	 * equal if both begin and end offsets are equal. If not, they are
	 * sorted by begin offset or, when those are equal, be end offset.
	 * 
	 * @return The comparison value.
	 */
	public static <T extends Annotation> Comparator<T> exactOffsetMatchComparator() {
		return (o1, o2) -> {

			if (o1.getBegin() == o2.getBegin() && o1.getEnd() == o2.getEnd())
				return 0;
			if (o1.getBegin() == o2.getBegin())
				return o1.getEnd() - o2.getEnd();
			return o1.getBegin() - o2.getBegin();
		};
	}

	/**
	 * Compares annotations for any kind of overlapping. As long as two
	 * annotations overlap for at least a single position, they are deemed
	 * "equal" by this comparator. Otherwise, they are sorted by begin
	 * offset.
	 * 
	 * @return 0 if annotations overlap, the start offset difference
	 *         otherwise.
	 */
	public static <T extends Annotation> Comparator<T> overlapComparator() {
		return (o1, o2) -> {
			int b1 = o1.getBegin();
			int e1 = o1.getEnd();
			int b2 = o2.getBegin();
			int e2 = o2.getEnd();

			if ((b1 <= b2) && (e1 >= e2)) {
				return 0;
			} else if ((b1 >= b2) && (e1 <= e2)) {
				return 0;
			} else if ((b1 < e2) && (e1 > e2)) {
				return 0;
			} else if ((b1 < b2) && (e1 > b2)) {
				return 0;
			}
			return b1 - b2;
		};
	}
	
	
	public static <T extends Annotation> Comparator<T> beginOffsetComparator() {
		return (o1, o2) -> {
			return o1.getBegin() - o2.getBegin();
		};
	}
	
	public static <T extends Annotation> Comparator<T> endOffsetComparator() {
		return (o1, o2) -> {
			return o1.getEnd() - o2.getEnd();
		};
	}

	public static Comparator<Long> longOverlapComparator() {
		return (o1, o2) -> {
			int b1 = (int)(o1 >> 32);
			int e1 = (int)(o1 >> 0);
			int b2 = (int)(o2 >> 32);
			int e2 = (int)(o2 >> 0);

			if ((b1 <= b2) && (e1 >= e2)) {
				return 0;
			} else if ((b1 >= b2) && (e1 <= e2)) {
				return 0;
			} else if ((b1 < e2) && (e1 > e2)) {
				return 0;
			} else if ((b1 < b2) && (e1 > b2)) {
				return 0;
			}
			return b1 - b2;
		};
	}
}