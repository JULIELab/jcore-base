/** 
 * Interval.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the BSD-2-Clause License
 *
 * Author: faessler
 * 
 * Current version: 1.2
 * Since version: 1.0
 *
 * Creation date: Feb 21, 2007 
 * 
 * An object for representing intervals [a,b] for integer a and b. In addition it contains
 * a String annotation wich may hold the value of an arbitrary annotation.
 **/

package de.julielab.jcore.ae.jnet.uima;

public class Interval {

	private final int a;
	private final int b;
	private final String annotation;

	public Interval(final int a, final int b, final String annotation) {
		this.a = a;
		this.b = b;
		this.annotation = annotation;
	}

	public Interval(final int a, final int b) {
		this(a, b, null);
	}

	public Interval() {
		this(0, 0, null);
	}

	public int getBegin() {
		return a;
	}

	public int getEnd() {
		return b;
	}

	public String getAnnotation() {
		return annotation;
	}

	public boolean isIn(final int a, final int b) {
		return ((a >= this.a) && (b <= this.b));
	}

	public boolean isIn(final Interval interval) {
		return isIn(interval.a, interval.b);
	}

}
