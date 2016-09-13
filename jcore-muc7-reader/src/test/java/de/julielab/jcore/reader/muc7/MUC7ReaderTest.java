/**
 * Copyright (c) 2015, JULIE Lab.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the GNU Lesser General Public License (LGPL) v3.0
 */

package de.julielab.jcore.reader.muc7;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.xml.sax.SAXException;

import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.muc7.Coref;
import de.julielab.jcore.types.muc7.ENAMEX;
import de.julielab.jcore.types.muc7.NUMEX;
import de.julielab.jcore.types.muc7.TIMEX;

public class MUC7ReaderTest extends TestCase {
	/**
	 * Path to the MedlineReader descriptor
	 */
	private static final String MUC7_READER_DESCRIPTOR = "src/test/resources/de/julielab/jcore/reader/muc7/desc/jcore-muc7-reader.xml";

	/**
	 * Object to be tested
	 */
	private CollectionReader muc7Reader;

	
	private CAS cas;

	
	/**
	 * Test data
	 */
	private final String DOC_ID = "nyt960214.0765";
	
	/**
	 * Test data
	 */
	private final int COREF_ID = 99;
	
	/**
	 * Test data
	 */
	private final String[] givenCorefChain = {"the plant", "the former weapons assembly plant", "Pantex", "the plant", "Pantex", "the plant", "Pantex Weapons Plant", "PANTEX", "PANTEX"};
	
	//TODO remark (matthies 09/15 during refactoring of Jules JCoRe):
	// The reader was changed so that only header, text & corefs are annotated (slug, date, numofwords, preamble, trailer
	// all fell away).
	// The test data wasn't updated accordingly.
	// --> 09/16: the latter remark is not valid anymore
	/**
	 * Test data
	 */
	private final String[] givenENAMEXData = {"PANTEX", "PANTEX", "FAA", "PANHANDLE", "NYTimes News Service clients", "HOLLACE WEINER", "Fort Worth Star-Telegram", "Federal Aviation Administration", "Pantex Weapons Plant", "Amarillo", "Texas", "Energy Department", "FAA", "Amarillo International Airport", "Defense Nuclear Facilities Safety Board", "Energy", "Pantex", "Trish Neusch", "Amarillo", "Mike McNulty", "FAA", "Amarillo International", "Pantex", "Energy Department", "Amarillo airport", "McNulty", "Air Force", "Texas", "Oklahoma", "Kansas", "New Mexico", "McNulty", "McNulty", "Dallas/Fort Worth Airport", "Amarillo", "McNulty", "Oklahoma", "McNulty"};
//	private final String[] givenENAMEXData = {"Federal Aviation Administration", "Pantex Weapons Plant", "Amarillo", "Texas", "Energy Department", "FAA", "Amarillo International Airport", "Defense Nuclear Facilities Safety Board", "Energy", "Pantex", "Trish Neusch", "Amarillo", "Mike McNulty", "FAA", "Amarillo International", "Pantex", "Energy Department", "Amarillo airport", "McNulty", "Air Force", "Texas", "Oklahoma", "Kansas", "New Mexico", "McNulty", "McNulty", "Dallas/Fort Worth Airport", "Amarillo", "McNulty", "Oklahoma", "McNulty"};

	/**
	 * Test data
	 */
	private final String[] givenTIMEXData = {"02-14", "1996", "later this year", "May 2", "late 1994", "For the past 14 months", "late summer", "early fall", "02-14-96", "2111EST"};
//	private final String[] givenTIMEXData = {"later this year", "May 2", "late 1994", "For the past 14 months", "late summer", "early fall"};
	
	/**
	 * Test data
	 */
	private final String[] givenNUMEXData = {"25 percent"};
	
	
	/**
	 *    * CAS array with CAS objects that where processed by the muc7Reader
	 */
	private ArrayList<JCas> cases = new ArrayList<JCas>();
	
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		muc7Reader = produceCollectionReader(MUC7_READER_DESCRIPTOR);
		processAllCases();
	}

	
	/**
	 * Processes all CASes by the muc7Reader
	 * 
	 * @throws CASException
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	private void processAllCases() throws CASException, SAXException, ParserConfigurationException {
		try {
		      while (muc7Reader.hasNext()) {
		    	 cas = CasCreationUtils.createCas((AnalysisEngineMetaData) muc7Reader.getMetaData());
		    	 muc7Reader.getNext(cas);
		    	 JCas jcas = cas.getJCas();
		    	 cases.add(jcas);
		      	}
		    } catch (CollectionException e) {
		      e.printStackTrace();
		    } catch (IOException e) {
		      e.printStackTrace();
		    } catch (ResourceInitializationException e) {
		      e.printStackTrace();
		    }
	} 
	/**
	 * Test if method getNextCas() has done its job
	 */	 
	public void testGetNextCas() {
 		
		//check for a TIMEX entity
 		String[] timexData = getTimexData(DOC_ID);
 		assertTrue("TIMEX", checkTimex(timexData));
		
		//check for a ENAMEX entity
 		String[] enamexData = getEnamexData(DOC_ID);
 		assertTrue("ENAMEX", checkEnamex(enamexData));
		
		//check for a NUMEX entity
 		String[] numexData = getNumexData(DOC_ID);
 		assertTrue("NUMEX", checkNumex(numexData));
 		
 		//TODO coreference doesn't works as of now
		//check for a coref chain
// 		String[] corefChain = getCorefChain(DOC_ID, COREF_ID);
// 		assertTrue("Coreference Chain", checkCorefChain(corefChain));
 		
	}

	/**
	 * 
	 * @param corefChain
	 * @return
	 */
	private boolean checkCorefChain(String[] corefChain) {
		
		if (corefChain.length == 0) {
			return false;
		}
		
		for (int i = 0; i < corefChain.length; i++) {
			System.out.println("COREF: -"+corefChain[i]+"- vs. -"+givenCorefChain[i]+"-");
			if (!corefChain[i].equals(givenCorefChain[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 
	 * @param timexData
	 * @return
	 */
	private boolean checkTimex(String[] timexData) {
		
		if (timexData.length == 0) {
			return false;
		}
		
		for (int i = 0; i < timexData.length; i++) {
			System.out.println("TIMEX: -"+timexData[i]+"- vs. -"+givenTIMEXData[i]+"-");
			if (!timexData[i].equals(givenTIMEXData[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 
	 * @param enamexData
	 * @return
	 */
	private boolean checkEnamex(String[] enamexData) {
		
		if (enamexData.length == 0) {
			return false;
		}
		
		for (int i = 0; i < enamexData.length; i++) {
			System.out.println("ENAMEX: -"+enamexData[i]+"- vs. -"+givenENAMEXData[i]+"-");
			if (!enamexData[i].equals(givenENAMEXData[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 
	 * @param numexData
	 * @return
	 */
	private boolean checkNumex(String[] numexData) {
		
		if (numexData.length == 0) {
			return false;
		}
		
		for (int i = 0; i < numexData.length; i++) {
			System.out.println("NUMEX: -"+numexData[i]+"- vs. -"+givenNUMEXData[i]+"-");
			if (!numexData[i].equals(givenNUMEXData[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 
	 * @param docID
	 * @return
	 */
	private String[] getNumexData(String docID) {
		ArrayList<String> numexAL = new ArrayList<String>();
		for (JCas jcas : cases) {
			Iterator<Header> headerIter = getTypeIterator(jcas, Header.type);
		    Header header = headerIter.next();
		    String casDocID = header.getDocId().trim();
		    if (casDocID.equals(docID)) {
		    	Iterator<NUMEX> numexIter = getTypeIterator(jcas, NUMEX.type);
			    while (numexIter.hasNext()) {
			    	NUMEX numex = numexIter.next();
			    	numexAL.add(numex.getCoveredText().replaceAll("\n", " "));
			    }
		    }
		}
		return toStringArray(numexAL);
	}

	/**
	 * 
	 * @param docID
	 * @return
	 */
	private String[] getEnamexData(String docID) {
		ArrayList<String> enamexAL = new ArrayList<String>();
		for (JCas jcas : cases) {
			Iterator<Header> headerIter = getTypeIterator(jcas, Header.type);
		    Header header = headerIter.next();
		    String casDocID = header.getDocId().trim();
		    if (casDocID.equals(docID)) {
		    	Iterator<ENAMEX> enamexIter = getTypeIterator(jcas, ENAMEX.type);
			    while (enamexIter.hasNext()) {
			    	ENAMEX enamex = enamexIter.next();
			    	enamexAL.add(enamex.getCoveredText().replaceAll("\n", " "));
			    }
		    }
		}
		return toStringArray(enamexAL);
	}

	/**
	 * 
	 * @param docID
	 * @return
	 */
	private String[] getTimexData(String docID) {
		ArrayList<String> timexAL = new ArrayList<String>();
		for (JCas jcas : cases) {
			Iterator<Header> headerIter = getTypeIterator(jcas, Header.type);
		    Header header = headerIter.next();
		    String casDocID = header.getDocId().trim();
		    if (casDocID.equals(docID)) {
		    	Iterator<TIMEX> timexIter = getTypeIterator(jcas, TIMEX.type);
			    while (timexIter.hasNext()) {
			    	TIMEX timex = timexIter.next();
			    	timexAL.add(timex.getCoveredText().replaceAll("\n", " "));
			    }
		    }
		}
		return toStringArray(timexAL);
	}


	/**
	 * 
	 * @param docID
	 * @param corefID
	 * @return
	 */
	private String[] getCorefChain(String docID, int corefID) {
		ArrayList<String> corefChain = new ArrayList<String>();
		for (JCas jcas : cases) {
			Iterator<Header> headerIter = getTypeIterator(jcas, Header.type);
		    Header header = headerIter.next();
		    String casDocID = header.getDocId().trim();
		    if (casDocID.equals(docID)) {
		    	buildCorefChain(corefID, corefChain, jcas);
		    	break;
		    }
		}
		return toStringArray(corefChain);
	}

	/**
	 * 
	 * @param corefID
	 * @param corefChain
	 * @param jcas
	 */
	private void buildCorefChain(int corefID, ArrayList<String> corefChain, JCas jcas) {
		Iterator<Coref> corefIter = getTypeIterator(jcas, Coref.type);
		while (corefIter.hasNext()) {
			Coref coref = corefIter.next();
			int casCorefID = coref.getId();
			if (casCorefID == corefID) {
				corefChain.add(coref.getCoveredText());
				if (coref.getRef() != null) {
					int refCorefID = coref.getRef().getId();
					buildCorefChain(refCorefID, corefChain, jcas);
				}
			}
		}
	}


	/**
	   * Gets an Iterator over the the CAS for the specific type
	   * 
	   * @param cas (the CAS)
	   * @param type (the type)
	   * @return the iterator
	   */
	  private Iterator getTypeIterator(JCas jcas, int type) {
		  Iterator iter = null;
		  iter = jcas.getJFSIndexRepository().getAnnotationIndex(type).iterator();
		  return iter;
	  }


	/**
	 * 
	 * @param stringArray
	 * @return
	 */
	private String[] toStringArray(ArrayList<String> stringArray) {
		String[] corefStringArray = new String[stringArray.size()];
		for (int i = 0; i < stringArray.size(); i++) {
			corefStringArray[i] = stringArray.get(i);
		}
		return corefStringArray;
	}
	
	
	  
	 /**
	  * Produces an UIMA Collection Reader.
	  * @param descriptor
	  * @return CollectionReader
	 * @throws IOException 
	 * @throws InvalidXMLException 
	 * @throws ResourceInitializationException 
	  */
	 private CollectionReader produceCollectionReader(String descriptor) throws InvalidXMLException, IOException, ResourceInitializationException {
		 CollectionReader collectionReader;
		 ResourceSpecifier spec;
		 spec = UIMAFramework.getXMLParser().parseResourceSpecifier(new XMLInputSource(descriptor));
		 collectionReader = UIMAFramework.produceCollectionReader(spec);
		 return collectionReader;
	  }
	
}
