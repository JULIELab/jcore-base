

/* First created by JCasGen Mon Feb 05 09:56:20 CET 2018 */
package de.julielab.jcore.es.test;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSArray;
import de.julielab.jcore.types.Header;


/** 
 * Updated by JCasGen Mon Feb 05 09:56:20 CET 2018
 * XML source: /Volumes/OUTERSPACE/Coding/git/jcore-base/jcore-elasticsearch-consumer/src/test/resources/de/julielab/jcore/consumer/es/testTypes.xml
 * @generated */
public class HeaderTestType extends Header {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(HeaderTestType.class);
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
  protected HeaderTestType() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public HeaderTestType(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public HeaderTestType(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public HeaderTestType(JCas jcas, int begin, int end) {
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
  //* Feature: testAuthors

  /** getter for testAuthors - gets 
   * @generated
   * @return value of the feature 
   */
  public FSArray getTestAuthors() {
    if (HeaderTestType_Type.featOkTst && ((HeaderTestType_Type)jcasType).casFeat_testAuthors == null)
      jcasType.jcas.throwFeatMissing("testAuthors", "de.julielab.jcore.es.test.HeaderTestType");
    return (FSArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((HeaderTestType_Type)jcasType).casFeatCode_testAuthors)));}
    
  /** setter for testAuthors - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setTestAuthors(FSArray v) {
    if (HeaderTestType_Type.featOkTst && ((HeaderTestType_Type)jcasType).casFeat_testAuthors == null)
      jcasType.jcas.throwFeatMissing("testAuthors", "de.julielab.jcore.es.test.HeaderTestType");
    jcasType.ll_cas.ll_setRefValue(addr, ((HeaderTestType_Type)jcasType).casFeatCode_testAuthors, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for testAuthors - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public AuthorTestType getTestAuthors(int i) {
    if (HeaderTestType_Type.featOkTst && ((HeaderTestType_Type)jcasType).casFeat_testAuthors == null)
      jcasType.jcas.throwFeatMissing("testAuthors", "de.julielab.jcore.es.test.HeaderTestType");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((HeaderTestType_Type)jcasType).casFeatCode_testAuthors), i);
    return (AuthorTestType)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((HeaderTestType_Type)jcasType).casFeatCode_testAuthors), i)));}

  /** indexed setter for testAuthors - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setTestAuthors(int i, AuthorTestType v) { 
    if (HeaderTestType_Type.featOkTst && ((HeaderTestType_Type)jcasType).casFeat_testAuthors == null)
      jcasType.jcas.throwFeatMissing("testAuthors", "de.julielab.jcore.es.test.HeaderTestType");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((HeaderTestType_Type)jcasType).casFeatCode_testAuthors), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((HeaderTestType_Type)jcasType).casFeatCode_testAuthors), i, jcasType.ll_cas.ll_getFSRef(v));}
  }

    