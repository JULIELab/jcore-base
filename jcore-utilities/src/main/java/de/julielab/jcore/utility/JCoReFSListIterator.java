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
package de.julielab.jcore.utility;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.lang.NotImplementedException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;

/**
 * An iterator that takes a list of FeatureStructures and returns or navigates
 * its elements exactly in the order the list defines. Actually, the iterator
 * operates on the exact list, so changes are write-through. External changes to
 * the list while iterating may lead to undefined behavior of the iterator.<br>
 * Since no natural order of the list elements is assumed, the
 * {@link #moveTo(FeatureStructure)} method is currently not implemented since
 * it couldn't behave as defined in the contract of <tt>FSIterator</tt>.
 * 
 * @author faessler
 * 
 */
public class JCoReFSListIterator<T extends FeatureStructure> implements FSIterator<T> {

	private List<T> fsList;
	private int pos;

	public JCoReFSListIterator(List<T> list) {
		fsList = list;
		if (fsList == null)
			fsList = Collections.emptyList();
		pos = -1;
	}

	@Override
	public boolean hasNext() {
		return !fsList.isEmpty() && pos < fsList.size() - 1 && pos + 1 >= 0;
	}

	@Override
	public T next() {
		++pos;
		if (isValid())
			return get();
		return null;
	}

	@Override
	public void remove() {
		fsList.remove(pos);
	}

	@Override
	public boolean isValid() {
		return pos >= 0 && pos < fsList.size();
	}

	@Override
	public T get() throws NoSuchElementException {
		if (pos < 0 || pos >= fsList.size())
			throw new NoSuchElementException("List size: " + fsList.size() + ", index: " + pos);
		return fsList.get(pos);
	}

	@Override
	public void moveToNext() {
		++pos;
	}

	@Override
	public void moveToPrevious() {
		--pos;

	}

	@Override
	public void moveToFirst() {
		pos = 0;
	}

	@Override
	public void moveToLast() {
		pos = fsList.size() - 1;

	}

	@Override
	public void moveTo(FeatureStructure fs) {
		throw new NotImplementedException();
	}

	@Override
	public FSIterator<T> copy() {
		return new JCoReFSListIterator<>(fsList);
	}

}
