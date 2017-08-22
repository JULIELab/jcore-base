/** 
 * DocResult.java
 * 
 * Copyright (c) 2008, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek
 * 
 * Current version: //TODO insert current version number 	
 * Since version:   //TODO insert version number of first appearance of this class
 *
 * Creation date: Jan 28, 2008 
 * 
 * //TODO insert short description
 **/

package de.julielab.jules.ae.genemapper.eval.tools;

import java.text.DecimalFormat;

public class DocResult {

	int tp;

	int fp;

	int fn;

	public DocResult(int tp, int fp, int fn) {
		super();
		this.tp = tp;
		this.fp = fp;
		this.fn = fn;
	}

	public DocResult() {

	}

	public double getRecall() {
		double ret;

		if (tp + fn > 0) {
			ret = (double) tp / (tp + fn);
		} else {
			ret = 0;
		}
		return ret;
	}

	public double getPrecision() {
		double ret;

		if (tp + fp > 0) {
			ret = (double) tp / (tp + fp);
		} else {
			ret = 0;
		}
		return ret;
	}

	public double getFscore() {
		double precision = getPrecision();
		double recall = getRecall();
		double ret;

		if (precision + recall > 0) {
			ret = 2.0 * precision * recall / (precision + recall);
		} else {
			ret = 0;
		}
		return ret;
	}

	public String toString() {
		DecimalFormat df = new DecimalFormat("0.000");
		return "R: " + df.format(getRecall()) + "\tP: "
				+ df.format(getPrecision()) + "\tF: " + df.format(getFscore());
	}

	
	public String toDetailedString() {
		return "tp: " + tp + "\tfp: " + fp + "\tfn: " + fn + "\t|\t" + toString();
	}
	
	
	public int getFn() {
		return fn;
	}

	public void setFn(int fn) {
		this.fn = fn;
	}

	public int getFp() {
		return fp;
	}

	public void setFp(int fp) {
		this.fp = fp;
	}

	public int getTp() {
		return tp;
	}

	public void setTp(int tp) {
		this.tp = tp;
	}
}
