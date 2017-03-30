/*******************************************************************************
 * Copyright: (c)  2013  Mayo Foundation for Medical Education and 
 *  Research (MFMER). All rights reserved. MAYO, MAYO CLINIC, and the
 *  triple-shield Mayo logo are trademarks and service marks of MFMER.
 *   
 *  Except as contained in the copyright notice above, or as used to identify 
 *  MFMER as the author of this software, the trade names, trademarks, service
 *  marks, or product names of the copyright holder shall not be used in
 *  advertising, promotion or otherwise in connection with this software without
 *  prior written authorization of the copyright holder.
 *     
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *     
 *  http://www.apache.org/licenses/LICENSE-2.0 
 *     
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and 
 *  limitations under the License. 
 *******************************************************************************/
package org.ohnlp.ae.medxn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;

import de.julielab.jcore.types.Sentence;
import de.julielab.jcore.types.medical.Dose;
import de.julielab.jcore.types.medical.Duration;
import de.julielab.jcore.types.medical.Frequency;
//import de.julielab.jcore.types.ohnlp.ConceptMention;
//import de.julielab.jcore.types.ohnlp.Drug;
//import de.julielab.jcore.types.ohnlp.LookupWindow;
import de.julielab.jcore.types.medical.GeneralAttributeMention;
import de.julielab.jcore.types.medical.Medication;
import de.julielab.jcore.types.medical.Modus;
import de.julielab.jcore.utility.JCoReAnnotationTools;



/**
 * Associate medication with attributes. 
 * @author Sunghwan Sohn
 */
public class MedExtAnnotator extends JCasAnnotator_ImplBase {
	class MedDesc {
//		ConceptMention med;
		List<GeneralAttributeMention> attrs = new ArrayList<GeneralAttributeMention>();
	}
	
	Set<String> bogusMed;
	
	public void initialize(UimaContext uimaContext) throws ResourceInitializationException {
		super.initialize(uimaContext);
		bogusMed = new HashSet<String>();
		
		try {
			InputStream in = getContext().getResourceAsStream("falseMedDict");
			if(in!=null) {
				try {
					BufferedReader fin = new BufferedReader(new InputStreamReader(in));
					String line = "";

					while((line = fin.readLine())!= null) {
						if( line.startsWith("#") 
								|| line.length()==0 
								|| Character.isWhitespace(line.charAt(0)) ) 
							continue;	
						bogusMed.add(line.toLowerCase().trim());
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (ResourceAccessException e) {
			e.printStackTrace();
		}
	}
	
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		Iterator<?> medItr= indexes.getAnnotationIndex(Medication.type).iterator(); //all drugs		
		List<Medication> drugs = new ArrayList<Medication>();

		//Get the list of drugs - if drug overlaps, use the longest one
		while(medItr.hasNext()) {
			Medication med = (Medication) medItr.next();
			boolean addFlag = true;
			
//			if(isFalseMed1(cm.getCoveredText())) continue; //added Nov-21
			
			//add the longest only (also remove the duplicate)
			if(drugs.size()==0) drugs.add(med);
			else {
				for(int i=0; i<drugs.size(); i++) {
					Medication x = (Medication) drugs.get(i);
					
					int condition = contains(med.getBegin(), med.getEnd(), 
							x.getBegin(), x.getEnd());
					if(condition==1) {
						drugs.remove(i);
						i--;
					}
					else if(condition==2) { 
						addFlag = false; 
						break;
					}
				}	        		
				if(addFlag)
					drugs.add(med);       			
			}       		
		}
			
		//IN [BN], IN (BN), BN [IN], BN (IN)
		//merge to one and assign both term type and rxcui
		String docText = jcas.getDocumentText();
		
//		for(int i=0; i<drugs.size()-1; i++) {	
//			//skip a partial overlap
//			if(drugs.get(i+1).getBegin() < drugs.get(i).getEnd() 
//					&& drugs.get(i+1).getEnd() > drugs.get(i).getEnd())
//				continue;
//			
//			String substring = docText.substring(
//					drugs.get(i).getEnd(), drugs.get(i+1).getBegin());		
//			
//			if(substring.matches("( +\\[ ?)|( +\\( ?)")) {
//				drugs.get(i+1).setBegin(drugs.get(i).getBegin());
//				drugs.get(i+1).setEnd(drugs.get(i+1).getEnd()+1);
//				drugs.get(i+1).setNormTarget(drugs.get(i).getNormTarget()+"::"+
//						drugs.get(i+1).getNormTarget());
//				drugs.get(i+1).setSemGroup(drugs.get(i).getSemGroup()+"::"+
//						drugs.get(i+1).getSemGroup());
//				drugs.get(i+1).getSentence().setBegin(drugs.get(i).getSentence().getBegin()); 
//				
//				drugs.remove(i);
//			}			
//		}
			
		//associate drug with attributes within the window
		//this window condition is for Mayo "current medication section"
		Integer drug_id = 1;
		Integer attr_id = 1;
		for(int i=0; i<drugs.size(); i++) {
//			MedDesc md = new MedDesc();
//			md.med = drugs.get(i);
			Medication actMed = drugs.get(i);
			
			int nextDrugBegin;
			if(i==drugs.size()-1) 
				nextDrugBegin = Integer.MAX_VALUE;
			else 
				nextDrugBegin = drugs.get(i+1).getBegin();
					
			int[] span = setWindow(jcas, actMed, nextDrugBegin);
			
			Iterator<?> gamItr = indexes.getAnnotationIndex(GeneralAttributeMention.type).iterator();
			GeneralAttributeMention beforeMedAttr = null; //attribute right before medication
			Map<String, ArrayList<GeneralAttributeMention>> attrMap = new HashMap<String, ArrayList<GeneralAttributeMention>>();
			int cnt=0;
			while(gamItr.hasNext()) {
				GeneralAttributeMention gam = (GeneralAttributeMention) gamItr.next();
				if(gam.getBegin()>=span[0] && gam.getEnd()<=span[1]) {
					assignAttribute(gam, attrMap);
//					md.attrs.add(ma);
					cnt++;
				}
				if(cnt==0) { 
					beforeMedAttr = gam;
				}
			}
			
			//remove time or volume that might belong to next drug
			//because of the [window] condition 
			//eg)	[Aspirin 81mg oral tablet.
			//		0.2 ML ]Somatuline 300 MG/ML Prefilled Syringe.				
//			if(md.attrs.size()>0 &&
//					md.attrs.get(md.attrs.size()-1).getTag().matches("time|volume"))
//				md.attrs.remove(md.attrs.size()-1);
//			
//			//add time or volume attributes before the drug
//			boolean flag = false;
//			if(md.attrs.size()>0 && beforeMedAttr!=null) {
//				for(GeneralAttributeMention ma : md.attrs) {
//					if(ma.getTag().equals("form")) {
//						//eg1) 24 HR Imdur 30 MG Extended Release Tablet
//						if(ma.getCoveredText().toLowerCase().matches("(.*?extended release.*?)" +
//								"|transdermal patch")
//								&& beforeMedAttr.getTag().equals("time")) {
//							flag = true;
//							break;
//						}
//						//eg2) 0.2 ML Somatuline 300 MG/ML Prefilled Syringe
//						else if(ma.getCoveredText().toLowerCase().matches("prefilled (syringe|applicator)" +
//								"|injectable solution" +
//								"|topical lotion") 
//								&& beforeMedAttr.getTag().equals("volume")) {
//							flag = true;
//							break;
//						}						
//					}
//				}
//				if(flag) md.attrs.add(0,beforeMedAttr);
//			}
//						
//			if(!isFalseMed2(md)) 
//				addToJCas(jcas, drugs.get(i), span);
			attr_id = setUpMedictation(actMed, drug_id++, attrMap, attr_id, jcas);
		}		
	}
	
	private Integer setUpMedictation(Medication actMed, Integer drug_id, Map<String, ArrayList<GeneralAttributeMention>> attrMap, Integer attr_id, JCas jcas) {
		actMed.setId(String.format("T%1$d", drug_id));
		for (String attr : attrMap.keySet()) {
			ArrayList<GeneralAttributeMention> gamList = attrMap.get(attr);
			FSArray attrArray = new FSArray(jcas, gamList.size());
			switch (attr) {
				case "duration":
					for (int i=0; i<gamList.size(); i++) {
						GeneralAttributeMention gam = gamList.get(i);
						Duration dur = new Duration(jcas);
						dur.setBegin(gam.getBegin());
						dur.setEnd(gam.getEnd());
						dur.setId(String.format("T%1$d%2$d", drug_id, attr_id));
						dur.setSpecificType("Duration");
						dur.addToIndexes();
						attrArray.set(i, dur);
						attr_id++;
					}
					attrArray.addToIndexes();
					actMed.setDuration(attrArray);
					break;
				case "dosage":
					for (int i=0; i<gamList.size(); i++) {
						GeneralAttributeMention gam = gamList.get(i);
						Dose dos = new Dose(jcas);
						dos.setBegin(gam.getBegin());
						dos.setEnd(gam.getEnd());
						dos.setId(String.format("T%1$d%2$d", drug_id, attr_id));
						dos.setSpecificType("Dose");
						dos.addToIndexes();
						attrArray.set(i, dos);
						attr_id++;
					}
					attrArray.addToIndexes();
					actMed.setDose(attrArray);
					break;
				case "route":
					for (int i=0; i<gamList.size(); i++) {
						GeneralAttributeMention gam = gamList.get(i);
						Modus mod = new Modus(jcas);
						mod.setBegin(gam.getBegin());
						mod.setEnd(gam.getEnd());
						mod.setId(String.format("T%1$d%2$d", drug_id, attr_id));
						mod.setSpecificType("Modus");
						mod.addToIndexes();
						attrArray.set(i, mod);
						attr_id++;
					}
					attrArray.addToIndexes();
					actMed.setModus(attrArray);
					break;
				case "frequency":
					for (int i=0; i<gamList.size(); i++) {
						GeneralAttributeMention gam = gamList.get(i);
						Frequency freq = new Frequency(jcas);
						freq.setBegin(gam.getBegin());
						freq.setEnd(gam.getEnd());
						freq.setId(String.format("T%1$d%2$d", drug_id, attr_id));
						freq.setSpecificType("Frequency");
						freq.addToIndexes();
						attrArray.set(i, freq);
						attr_id++;
					}
					attrArray.addToIndexes();
					actMed.setModus(attrArray);
					break;
				case "strength":
					break;
				default:
					break;
			}
		}
		return attr_id;
	}

	private void assignAttribute(GeneralAttributeMention gam, Map<String, ArrayList<GeneralAttributeMention>> attrMap) {
		String gamType = gam.getTag(); //duration, dosage, route, frequency, strength
		if ( !attrMap.containsKey(gamType) ) {
			ArrayList<GeneralAttributeMention> vals = new ArrayList<GeneralAttributeMention>();
			attrMap.put(gamType, vals);
		}
		attrMap.get(gamType).add(gam);
	}

//	protected void addToJCas(JCas jcas, Medication drug, int[] window) {
//		Drug d = new Drug(jcas);
//		d.setName(md.med);
//		d.setBegin(md.med.getBegin());
//		d.setEnd(md.med.getEnd());
//				
//		FSArray attributes = new FSArray(jcas, md.attrs.size());
//		for(int i=0; i<md.attrs.size(); i++) {
//			//System.out.println("attributes="+md.attrs.get(i).getCoveredText());
//			attributes.set(i, md.attrs.get(i));			
//		}
//
//		d.setAttrs(attributes);
//		d.addToIndexes();
//		
//		LookupWindow lw = new LookupWindow(jcas);
//		lw.setBegin(window[0]);
//		lw.setEnd(window[1]);
//		lw.addToIndexes();
//	}
	
	/**
	 * NOTE THAT THIS IS SPECIFIC FOR MAYO DATA
	 * return the offsets: begin of the given drug and the smaller
	 * of 1) the end of +2 sentences or 2) begin of the next drug
	 * 
	 * Mayo specific:
	 * "BUT if there is newline and it does not start with Instruction 
	 * or Indication, the end becomes newline" 
	 *     
	 * @param jcas
	 * @param drug 
	 * @param nextDrugBegin
	 * @return
	 */
	protected int[] setWindow(JCas jcas, Medication drug, int nextDrugBegin) {
		JFSIndexRepository indexes = jcas.getJFSIndexRepository();
		Iterator<?> senItr= indexes.getAnnotationIndex(Sentence.type).iterator();		
//		String[] str = drug.getSentence().split("::");
//		Sentence drugsen=(Sentence) drug.getSentence();
		Sentence drugsen = (Sentence) JCoReAnnotationTools.getIncludingAnnotation(jcas, drug, Sentence.class);
		int drugSenBegin = drugsen.getBegin();  
		int drugSenEnd = drugsen.getEnd();
		
		int[] ret = {-1, -1};
		
		ret[0] = drug.getBegin();
		ret[1] = drugSenEnd;

		int InstructionBegin = -1;
		int IndicationBegin = -1;
		int cnt=0;
		while(senItr.hasNext() && cnt<2) {
			Sentence sen = (Sentence) senItr.next();
			if(sen.getBegin()>drug.getEnd()) {
				cnt++;
				ret[1] = sen.getEnd();
				
				if(sen.getCoveredText().startsWith("Instruction"))
					InstructionBegin = sen.getBegin();
				else if(sen.getCoveredText().startsWith("Indication"))
					IndicationBegin = sen.getBegin();
			}				
		}
		
		ret[1] = ret[1]>nextDrugBegin ? nextDrugBegin : ret[1];
		
		//Check if a given drug window needs to be expanded before drug
		//and reset a window (eg, IV Lasix, one dose of IV Lasix)		
		//TODO currently simply expand -20 before a given drug or 
		//begin of the sentence and so might need more sophisticated rules
		String text = jcas.getDocumentText().substring(drugSenBegin, drug.getBegin());
		if(text.matches(".*\\s+IV\\s+") 
				|| text.matches(".*\\s+of\\s+(\\S+\\s+){0,1}")) {	
			int begin = drug.getBegin()-20;
			ret[0] = begin<drugSenBegin ? drugSenBegin : begin;
		}		
		
		//---- TODO: DON'T USE THIS CONDICTION IF NOT MAYO DATA
		//NOTE THAT this is specific for Mayo Current Medication section
		//If there is newline within a window and it does not start with 
		//Instruction or Indication, set the end offset newline 
//		Iterator<?> newline= indexes.getAnnotationIndex(NewlineToken.type).iterator();
//		while(newline.hasNext()) {
//			NewlineToken nt = (NewlineToken)newline.next();
//			if(nt.getBegin()>ret[0] && nt.getBegin()<ret[1]) 
//				if( !(InstructionBegin==nt.getEnd() || IndicationBegin==nt.getEnd()) ) {
//					ret[1] = nt.getBegin();
//					break;
//				}				
//		}
		//-----
				
		return ret;
	}

	/**
	 * Check if the given drug is true or not
	 * @param md
	 * @return true if potentially false medication
	 */
	protected boolean isFalseMed1(String med) {		
		if(bogusMed.contains(med.toLowerCase().replaceAll("\\W", " "))) {
			//starts with a lower case
			if(Character.isLowerCase(med.charAt(0))) 
				return true;			
			//ad hoc
			if(med.equals("Ms"))
				return true;
		}
		
		return false;
	}
	
//	/**
//	 * Check if the given drug is true or not
//	 * @param md
//	 * @return true if potentially false medication
//	 */
//	protected boolean isFalseMed2(MedDesc md) {
//		String medStr = md.med.getCoveredText();
//		
//		if(bogusMed.contains(medStr.toLowerCase().replaceAll("\\W", " "))) {			
//			//no attribute
//			//TODO update because some appear w/o attribute 
//			if(md.attrs.size()==0)
//				return true;			
//		}
//		
//		return false;
//	}

	/**
	 * Returns 1 if 1 contains or equal to 2
	 * Returns 2 if 2 contains or equal to 1
	 * Returns 0 otherwise
	 */
	protected int contains(int b1, int e1, int b2, int e2) {
		if(b1<=b2 &&  e1>=e2) return 1;
		else if(b2<=b1 &&  e2>=e1) return 2;
		else return 0;
	}
}
