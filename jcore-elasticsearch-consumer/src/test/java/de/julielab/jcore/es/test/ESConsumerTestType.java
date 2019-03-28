

/* First created by JCasGen Mon Feb 05 09:56:20 CET 2018 */
package de.julielab.jcore.es.test;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.tcas.Annotation;


/** This test type exist to exhibit features useful for testing the ElasticSearch consumer algorithms.
 * Updated by JCasGen Mon Feb 05 09:56:20 CET 2018
 * XML source: /Volumes/OUTERSPACE/Coding/git/jcore-base/jcore-elasticsearch-consumer/src/test/resources/de/julielab/jcore/consumer/es/testTypes.xml
 * @generated */
public class ESConsumerTestType extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(ESConsumerTestType.class);
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
  protected ESConsumerTestType() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public ESConsumerTestType(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public ESConsumerTestType(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public ESConsumerTestType(JCas jcas, int begin, int end) {
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
  //* Feature: stringFeature

  /** getter for stringFeature - gets 
   * @generated
   * @return value of the feature 
   */
  public String getStringFeature() {
    if (ESConsumerTestType_Type.featOkTst && ((ESConsumerTestType_Type)jcasType).casFeat_stringFeature == null)
      jcasType.jcas.throwFeatMissing("stringFeature", "de.julielab.jcore.es.test.ESConsumerTestType");
    return jcasType.ll_cas.ll_getStringValue(addr, ((ESConsumerTestType_Type)jcasType).casFeatCode_stringFeature);}
    
  /** setter for stringFeature - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setStringFeature(String v) {
    if (ESConsumerTestType_Type.featOkTst && ((ESConsumerTestType_Type)jcasType).casFeat_stringFeature == null)
      jcasType.jcas.throwFeatMissing("stringFeature", "de.julielab.jcore.es.test.ESConsumerTestType");
    jcasType.ll_cas.ll_setStringValue(addr, ((ESConsumerTestType_Type)jcasType).casFeatCode_stringFeature, v);}    
   
    
  //*--------------*
  //* Feature: stringArrayFeature

  /** getter for stringArrayFeature - gets 
   * @generated
   * @return value of the feature 
   */
  public StringArray getStringArrayFeature() {
    if (ESConsumerTestType_Type.featOkTst && ((ESConsumerTestType_Type)jcasType).casFeat_stringArrayFeature == null)
      jcasType.jcas.throwFeatMissing("stringArrayFeature", "de.julielab.jcore.es.test.ESConsumerTestType");
    return (StringArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((ESConsumerTestType_Type)jcasType).casFeatCode_stringArrayFeature)));}
    
  /** setter for stringArrayFeature - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setStringArrayFeature(StringArray v) {
    if (ESConsumerTestType_Type.featOkTst && ((ESConsumerTestType_Type)jcasType).casFeat_stringArrayFeature == null)
      jcasType.jcas.throwFeatMissing("stringArrayFeature", "de.julielab.jcore.es.test.ESConsumerTestType");
    jcasType.ll_cas.ll_setRefValue(addr, ((ESConsumerTestType_Type)jcasType).casFeatCode_stringArrayFeature, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for stringArrayFeature - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public String getStringArrayFeature(int i) {
    if (ESConsumerTestType_Type.featOkTst && ((ESConsumerTestType_Type)jcasType).casFeat_stringArrayFeature == null)
      jcasType.jcas.throwFeatMissing("stringArrayFeature", "de.julielab.jcore.es.test.ESConsumerTestType");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((ESConsumerTestType_Type)jcasType).casFeatCode_stringArrayFeature), i);
    return jcasType.ll_cas.ll_getStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((ESConsumerTestType_Type)jcasType).casFeatCode_stringArrayFeature), i);}

  /** indexed setter for stringArrayFeature - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setStringArrayFeature(int i, String v) { 
    if (ESConsumerTestType_Type.featOkTst && ((ESConsumerTestType_Type)jcasType).casFeat_stringArrayFeature == null)
      jcasType.jcas.throwFeatMissing("stringArrayFeature", "de.julielab.jcore.es.test.ESConsumerTestType");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((ESConsumerTestType_Type)jcasType).casFeatCode_stringArrayFeature), i);
    jcasType.ll_cas.ll_setStringArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((ESConsumerTestType_Type)jcasType).casFeatCode_stringArrayFeature), i, v);}
  }

    