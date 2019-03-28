

/* First created by JCasGen Mon Feb 05 09:56:09 CET 2018 */
package de.julielab.jcore.es.test;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.cas.TOP_Type;


/** 
 * Updated by JCasGen Mon Feb 05 09:56:09 CET 2018
 * XML source: /Volumes/OUTERSPACE/Coding/git/jcore-base/jcore-elasticsearch-consumer/src/test/resources/de/julielab/jcore/consumer/es/testTypes.xml
 * @generated */
public class AddressTestType extends TOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(AddressTestType.class);
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
  protected AddressTestType() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public AddressTestType(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public AddressTestType(JCas jcas) {
    super(jcas);
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
  //* Feature: street

  /** getter for street - gets 
   * @generated
   * @return value of the feature 
   */
  public String getStreet() {
    if (AddressTestType_Type.featOkTst && ((AddressTestType_Type)jcasType).casFeat_street == null)
      jcasType.jcas.throwFeatMissing("street", "de.julielab.jcore.es.test.AddressTestType");
    return jcasType.ll_cas.ll_getStringValue(addr, ((AddressTestType_Type)jcasType).casFeatCode_street);}
    
  /** setter for street - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setStreet(String v) {
    if (AddressTestType_Type.featOkTst && ((AddressTestType_Type)jcasType).casFeat_street == null)
      jcasType.jcas.throwFeatMissing("street", "de.julielab.jcore.es.test.AddressTestType");
    jcasType.ll_cas.ll_setStringValue(addr, ((AddressTestType_Type)jcasType).casFeatCode_street, v);}    
   
    
  //*--------------*
  //* Feature: number

  /** getter for number - gets 
   * @generated
   * @return value of the feature 
   */
  public int getNumber() {
    if (AddressTestType_Type.featOkTst && ((AddressTestType_Type)jcasType).casFeat_number == null)
      jcasType.jcas.throwFeatMissing("number", "de.julielab.jcore.es.test.AddressTestType");
    return jcasType.ll_cas.ll_getIntValue(addr, ((AddressTestType_Type)jcasType).casFeatCode_number);}
    
  /** setter for number - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setNumber(int v) {
    if (AddressTestType_Type.featOkTst && ((AddressTestType_Type)jcasType).casFeat_number == null)
      jcasType.jcas.throwFeatMissing("number", "de.julielab.jcore.es.test.AddressTestType");
    jcasType.ll_cas.ll_setIntValue(addr, ((AddressTestType_Type)jcasType).casFeatCode_number, v);}    
   
    
  //*--------------*
  //* Feature: city

  /** getter for city - gets 
   * @generated
   * @return value of the feature 
   */
  public String getCity() {
    if (AddressTestType_Type.featOkTst && ((AddressTestType_Type)jcasType).casFeat_city == null)
      jcasType.jcas.throwFeatMissing("city", "de.julielab.jcore.es.test.AddressTestType");
    return jcasType.ll_cas.ll_getStringValue(addr, ((AddressTestType_Type)jcasType).casFeatCode_city);}
    
  /** setter for city - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setCity(String v) {
    if (AddressTestType_Type.featOkTst && ((AddressTestType_Type)jcasType).casFeat_city == null)
      jcasType.jcas.throwFeatMissing("city", "de.julielab.jcore.es.test.AddressTestType");
    jcasType.ll_cas.ll_setStringValue(addr, ((AddressTestType_Type)jcasType).casFeatCode_city, v);}    
  }

    