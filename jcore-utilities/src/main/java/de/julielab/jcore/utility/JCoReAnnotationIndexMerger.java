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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

public class JCoReAnnotationIndexMerger {
	private JCas aJCas;
	private LinkedHashSet<Type> annotationTypes;
	private List<FSIterator<? extends TOP>> annotationIterators;

	private boolean beginOffsetHasChanged;
	private TOP currentAnnotation;
	private int currentBegin;
	private int currentEnd;
	private int currentIndex;
	boolean firstToken;
	private int oldBeginOffset;
	/**
	 * Indicates whether the merged annotation output should be sorted by begin
	 * offset.
	 */
	private boolean sort;
	private AnnotationFS coveringAnnotation;

	public JCoReAnnotationIndexMerger(LinkedHashSet<Object> annotationIndexes, boolean sort,
			AnnotationFS coveringAnnotation, JCas aJCas) throws ClassNotFoundException {
		this.annotationTypes = new LinkedHashSet<Type>();
		annotationIndexes.forEach(i -> {
			if (i instanceof Integer)
				annotationTypes.add(aJCas.getCasType((Integer) i));
			else
				annotationTypes.add(aJCas.getTypeSystem().getType((String) i));
		});
		this.sort = sort;
		this.coveringAnnotation = coveringAnnotation;
		this.aJCas = aJCas;

		reset();
	}

	public boolean firstToken() {
		return firstToken;
	}

	public TOP getAnnotation() {
		return currentAnnotation;
	}

	public int getCurrentBegin() {
		return currentBegin;
	}

	public int getCurrentEnd() {
		return currentEnd;
	}

	public boolean hasBeginOffsetChanged() {
		return beginOffsetHasChanged;
	}

	public boolean incrementAnnotation() {
		if (currentIndex == -1) {
			// not initialized
			return moveIterator(true);
		}
		return moveIterator(false);
	}

	protected boolean moveIterator(boolean initialize) {
		int minBegin = Integer.MAX_VALUE;
		if (!initialize) {
			annotationIterators.get(currentIndex).moveToNext();
			firstToken = false;
			// If we don't sort, we do not check for the next minimal begin
			// annotation, we just continue until this
			// iterator isn't valid anymore. Extreme optimization would be to
			// then only look for the next valid iterator
			// instead of the minimal offset iterator, but this really shouldn't
			// be necessary.
			if (!sort && annotationIterators.get(currentIndex).isValid()) {
				setCurrentAnnotation();
				return true;
			}
		}
		for (int i = 0; i < annotationIterators.size(); i++) {
			FSIterator<? extends TOP> it = annotationIterators.get(i);
			if (initialize)
				it.moveToFirst();
			if (it.isValid()) {
				int itBegin = it.get() instanceof Annotation ? ((Annotation) it.get()).getBegin() : 0;
				if (itBegin < minBegin) {
					minBegin = itBegin;
					currentIndex = i;
				}
			}
		}
		// If we did find any value, this one should be one with the lowest
		// begin offset.
		if (currentIndex != -1 && annotationIterators.get(currentIndex).isValid()) {
			setCurrentAnnotation();
			beginOffsetHasChanged = currentBegin != oldBeginOffset;
			oldBeginOffset = currentBegin;
			return true;
		}
		currentAnnotation = null;
		return false;
	}

	@SuppressWarnings("unchecked")
	private void reset() throws ClassNotFoundException {
		annotationIterators = new ArrayList<>(annotationTypes.size());
		for (Type type : annotationTypes) {
			if (aJCas.getTypeSystem().subsumes(aJCas.getCasType(Annotation.type), type)) {
				if (null == coveringAnnotation)
					annotationIterators.add(aJCas.getAnnotationIndex(type).iterator());
				else {
					Type casType = type;
					// we don't use subiterators because type priorities
					// sometimes come unintuitive or just in our way; e.g. if we
					// want to get
					// "the gene that is covered by the argument mention
					// referring to this exact gene"
					// we could end up not retrieving the gene despite the fact
					// their offsets are exactly the same.
					Class<? extends Annotation> annotationClass = (Class<? extends Annotation>) Class
							.forName(casType.getName());
					// we can't use UimaFit's JCasUtil.selectCovered() here
					// because it will explicitly exclude the coveringAnnotation
					// itself which is not the behavior we need
					List<? extends Annotation> coveredAnnotations = JCoReAnnotationTools.getIncludedAnnotations(aJCas,
							(Annotation) coveringAnnotation, annotationClass);
					JCoReFSListIterator<? extends TOP> fsit = (JCoReFSListIterator<? extends TOP>) new JCoReFSListIterator<>(
							coveredAnnotations);
					annotationIterators.add(fsit);
				}
			} else {
				annotationIterators.add(aJCas.getJFSIndexRepository().getAllIndexedFS(type));
			}
		}
		beginOffsetHasChanged = true;
		oldBeginOffset = -1;
		currentIndex = -1;
		firstToken = true;
	}

	protected void setCurrentAnnotation() {
		currentAnnotation = annotationIterators.get(currentIndex).get();
		if (currentAnnotation instanceof Annotation) {
			Annotation a = (Annotation) currentAnnotation;
			currentBegin = a.getBegin();
			currentEnd = a.getEnd();
		} else {
			currentBegin = 0;
			currentEnd = 0;
		}
	}

	public void setCurrentBegin(int currentBegin) {
		this.currentBegin = currentBegin;
	}

	public void setCurrentEnd(int currentEnd) {
		this.currentEnd = currentEnd;
	}

}
