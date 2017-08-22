/** 
 * SynHit.java
 * 
 * Copyright (c) 2006, JULIE Lab. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 *
 * Author: tomanek, wermter
 * 
 * Current version: 2.0
 * Since version:   1.0
 *
 * Creation date: Dec 6, 2006 
 * 
 * An object used to store mapping results.
 **/

package de.julielab.jules.ae.genemapper;

import java.text.DecimalFormat;
import java.util.Random;

public class SynHit implements Comparable<SynHit>, Cloneable {
	
	public enum CompareType {RANDOM, IDSENSE_FRQ, SYNSENSE_FRQ, SCORE, SEMSCORE};

	private String synonym;

	private double mentionScore;
	
	private double semanticScore;
	
	private double overallScore;

	private String id; //Entrez Gene ID

	private String source;

	private int idSenseFreq;

	private int synSenseFreq;
	
	private String mappedMention; // the mention found in text and searched for

	// compare type is used during scoring if two synsets have same score
	// (see in compareTo(...) method)
	private CompareType compareType = CompareType.SCORE;

	/*
	 * this is a random id used for sorting TODO might be replaced by something
	 * more sophisticated
	 */
	int random;

	private String taxId; //NCBI Taxonomy ID


	/**
	 * @param syn
	 * @param score
	 * @param xid
	 * @param source
	 */
	public SynHit(String syn, double score, String xid, String source) {
		this.synonym = syn;
		this.mentionScore = score;
		this.id = xid;
		this.source = source;

//		Random r = new Random(System.currentTimeMillis());
//		this.random = r.nextInt();
	}

	/**
	 * @param syn
	 * @param score
	 * @param xid
	 * @param source
	 * @param taxId 
	 * @param idSenseFreq
	 * @param synSenseFreq
	 */
	// EF CHANGE: added taxId
	public SynHit(String syn, double score, String xid, String source,
			String taxId, int idSenseFreq, int synSenseFreq) {
		this.synonym = syn;
		this.mentionScore = score;
		this.id = xid;
		this.source = source;
		this.setTaxId(taxId);
		this.idSenseFreq = idSenseFreq;
		this.synSenseFreq = synSenseFreq;

//		Random r = new Random(System.currentTimeMillis());
//		this.random = r.nextInt();
	}

	/**
	 * @return the id of this SynHit
	 */
	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * @return
	 */
	public double getMentionScore() {
		return mentionScore;
	}

	public void setMentionScore(double score) {
		this.mentionScore = score;
	}

	public double getSemanticScore() {
		return this.semanticScore;
	}

	public void setSemanticScore(double score) {
		this.semanticScore = score;
	}
	
	public String getSynonym() {
		return synonym;
	}

	public void setSynonym(String syn) {
		this.synonym = syn;
	}

	public String toString() {
		DecimalFormat scoreFormat = new DecimalFormat("0.000");
		String result = "id=" + id + "\tscore=" + scoreFormat.format(mentionScore) + "\tsemScore=" + scoreFormat.format(semanticScore)
				+ "\tsyn=" + synonym + "\tidSenseFreq=" + idSenseFreq + "\ttaxId=" + taxId;
		result += "\tsynSenseFreq=" + synSenseFreq;
		return result;
	}

	/**
	 * the comparator for two SynHits: order by score as set by setCompareType method
	 * TODO: find rule
	 * how to order if several SynHits have same score currently, random number
	 * is chosen
	 * @param o 
	 * @return int 
	 */
	public int compareTo(SynHit o) {
		int c = 0;
		//int c = (new Double(s.getScore())).compareTo(new Double(this.getScore()));
		//if (c == 0) {
			// in case of same score:
			switch (this.compareType) {
				case RANDOM:
					c = (new Integer(o.random)).compareTo(this.random);
					break;
				case IDSENSE_FRQ:
					c = (new Integer(o.idSenseFreq)).compareTo(this.idSenseFreq);
					break;
				case SYNSENSE_FRQ:
					c = (new Integer(o.synSenseFreq)).compareTo(this.synSenseFreq);
					break;
				case SCORE:
					c = Double.compare(o.mentionScore, mentionScore);
					break;
				case SEMSCORE:
					c = (new Double(o.semanticScore)).compareTo(this.semanticScore);
					break;
				}
		//}
		return c;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public int getIdSenseFreq() {
		return idSenseFreq;
	}

	public void setIdSenseFreq(int idSenseFreq) {
		this.idSenseFreq = idSenseFreq;
	}

	public int getSynSenseFreq() {
		return synSenseFreq;
	}

	public void setSynSenseFreq(int synSenseFreq) {
		this.synSenseFreq = synSenseFreq;
	}

	public CompareType getCompareType() {
		return compareType;
	}

	/**
	 * sets CompareType for two SynHits, defined as follows:
	 * {@link #CompareType}
	 * @param type the CompareType
	 * 
	 */
	public void setCompareType(CompareType type) {
		this.compareType = type;
	}

	public String getMappedMention() {
		return mappedMention;
	}

	public void setMappedMention(String mappedSynonym) {
		this.mappedMention = mappedSynonym;
	}
	
	public boolean isExactMatch() {
		return (this.mappedMention.equals(this.synonym));
	}
	
	public Object clone() throws CloneNotSupportedException {
		SynHit h = (SynHit)super.clone();
		return h;
	}

	public String getTaxId() {
		return taxId;
	}

	public void setTaxId(String taxId) {
		this.taxId = taxId;
	}

	public double getOverallScore() {
		return overallScore;
	}

	public void setOverallScore(double overallScore) {
		this.overallScore = overallScore;
	}

	
}
