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
package de.julielab.jcore.ae.lingpipegazetteer.utils;

import org.apache.uima.jcas.tcas.Annotation;
//import opennlp.tools.util.Span;
import org.apache.uima.jcas.cas.FSArray;

//import edu.colorado.cleartk.types.ContiguousAnnotation;
//import edu.colorado.cleartk.types.SplitAnnotation;

/**
 * Copyright 2007 Regents of the University of Colorado.  
 * All Rights Reserved.  This software is provided under the terms of the 
 * <a href="https://www.cusys.edu/techtransfer_edit/downloads/Bulletin-SourceCodeAgreementNonprofitResearch.pdf">
 * CU Non-Profit Research License Agreement</a><p>
 *
 * @author Philip Ogren
 *
 */
public class AnnotationUtil
{
	/**
	 * Determines whether the big annotation contains the small annotation w.r.t. to
	 * character offsets (begin and end).
	 * 
	 */
	public static boolean contains(Annotation bigAnnotation, Annotation smallAnnotation)
	{
		if(bigAnnotation instanceof SplitAnnotation) {
			FSArray splits = ((SplitAnnotation) bigAnnotation).getAnnotations();
			
			for(int i=0; i<splits.size(); i++) {
				ContiguousAnnotation split = (ContiguousAnnotation) splits.get(i);
				if(contains(split, smallAnnotation))
					return true;
			}
			return false;
		}
		
		if(bigAnnotation == null || smallAnnotation == null) return false;
		if(bigAnnotation.getBegin() <= smallAnnotation.getBegin() &&
		   bigAnnotation.getEnd() >= smallAnnotation.getEnd())
			return true;
		else return false;
	}

	public static int size(Annotation annotation)
	{
		try
		{
			return annotation.getEnd() - annotation.getBegin();
		}
		catch(Exception e)
		{
			return 0;
		}
	}
	/*
	public static Span getAnnotationsExtent(List<? extends Annotation> annotations) {
		int start = Integer.MAX_VALUE;
		int end = 0;
		
		for( Annotation annotation : annotations ) {
			if( annotation.getBegin() < start )
				start = annotation.getBegin();
			if( annotation.getEnd() > end )
				end = annotation.getEnd();
		}
		
		return new Span(start, end);
	}
	*/
}
