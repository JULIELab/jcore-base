/** 
 * Interval.java
 * 
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
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

package de.julielab.jules.coordinationtagger;

public class Interval {

	private int a;
	private int b;
	private String annotation;
	
	public Interval(int a, int b, String annotation) {
		this.a = a;
		this.b = b;
		this.annotation = annotation;
	}
	
	public Interval(int a, int b) {
		this(a,b,null);
	}
	
	public Interval() {
		this(0,0,null);
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
	
	public boolean isIn(int a, int b) {
		return (a>=this.a && b <=this.b);
	}
	
	public boolean isIn(Interval interval) {
		return isIn(interval.a, interval.b);
	}

}

