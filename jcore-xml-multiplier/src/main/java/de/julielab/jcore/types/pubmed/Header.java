

/* First created by JCasGen Sun Mar 18 12:36:58 CET 2018 */
package de.julielab.jcore.types.pubmed;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSArray;


/** The special Header for PubMed (http://www.pubmed.org)
        documents
 * Updated by JCasGen Wed Mar 21 14:47:03 CET 2018
 * XML source: C:/Users/Philipp/jcore-xml-multiplier/src/test/resources/FileTypeSystemDescriptor.xml
 * @generated */
public class Header extends de.julielab.jcore.types.Header {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Header.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected Header() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Header(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Header(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Header(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** 
   * <!-- begin-user-doc -->
   * Write your own initialization here
   * <!-- end-user-doc -->
   *
   * @generated modifiable 
   */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: citationStatus

  /** getter for citationStatus - gets Indicates the status of citation of a PubMed document
   * @generated
   * @return value of the feature 
   */
  public String getCitationStatus() {
    if (Header_Type.featOkTst && ((Header_Type)jcasType).casFeat_citationStatus == null)
      jcasType.jcas.throwFeatMissing("citationStatus", "de.julielab.jcore.types.pubmed.Header");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Header_Type)jcasType).casFeatCode_citationStatus);}
    
  /** setter for citationStatus - sets Indicates the status of citation of a PubMed document 
   * @generated
   * @param v value to set into the feature 
   */
  public void setCitationStatus(String v) {
    if (Header_Type.featOkTst && ((Header_Type)jcasType).casFeat_citationStatus == null)
      jcasType.jcas.throwFeatMissing("citationStatus", "de.julielab.jcore.types.pubmed.Header");
    jcasType.ll_cas.ll_setStringValue(addr, ((Header_Type)jcasType).casFeatCode_citationStatus, v);}    
   
    
  //*--------------*
  //* Feature: otherIDs

  /** getter for otherIDs - gets Other IDs (then the PubMed ID) may delivered by partners of the NLM to PubMed/Medline abstracts. If available, this feature discloses such other IDs and the respective sources.
   * @generated
   * @return value of the feature 
   */
  public FSArray getOtherIDs() {
    if (Header_Type.featOkTst && ((Header_Type)jcasType).casFeat_otherIDs == null)
      jcasType.jcas.throwFeatMissing("otherIDs", "de.julielab.jcore.types.pubmed.Header");
    return (FSArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Header_Type)jcasType).casFeatCode_otherIDs)));}
    
  /** setter for otherIDs - sets Other IDs (then the PubMed ID) may delivered by partners of the NLM to PubMed/Medline abstracts. If available, this feature discloses such other IDs and the respective sources. 
   * @generated
   * @param v value to set into the feature 
   */
  public void setOtherIDs(FSArray v) {
    if (Header_Type.featOkTst && ((Header_Type)jcasType).casFeat_otherIDs == null)
      jcasType.jcas.throwFeatMissing("otherIDs", "de.julielab.jcore.types.pubmed.Header");
    jcasType.ll_cas.ll_setRefValue(addr, ((Header_Type)jcasType).casFeatCode_otherIDs, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for otherIDs - gets an indexed value - Other IDs (then the PubMed ID) may delivered by partners of the NLM to PubMed/Medline abstracts. If available, this feature discloses such other IDs and the respective sources.
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public OtherID getOtherIDs(int i) {
    if (Header_Type.featOkTst && ((Header_Type)jcasType).casFeat_otherIDs == null)
      jcasType.jcas.throwFeatMissing("otherIDs", "de.julielab.jcore.types.pubmed.Header");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Header_Type)jcasType).casFeatCode_otherIDs), i);
    return (OtherID)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Header_Type)jcasType).casFeatCode_otherIDs), i)));}

  /** indexed setter for otherIDs - sets an indexed value - Other IDs (then the PubMed ID) may delivered by partners of the NLM to PubMed/Medline abstracts. If available, this feature discloses such other IDs and the respective sources.
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setOtherIDs(int i, OtherID v) { 
    if (Header_Type.featOkTst && ((Header_Type)jcasType).casFeat_otherIDs == null)
      jcasType.jcas.throwFeatMissing("otherIDs", "de.julielab.jcore.types.pubmed.Header");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Header_Type)jcasType).casFeatCode_otherIDs), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Header_Type)jcasType).casFeatCode_otherIDs), i, jcasType.ll_cas.ll_getFSRef(v));}
  }

    