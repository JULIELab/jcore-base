/** 
 * 
 * Copyright (c) 2017, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: 
 * 
 * Description:
 **/
package de.julielab.jcore.utility;

import org.apache.commons.lang.NotImplementedException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

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
