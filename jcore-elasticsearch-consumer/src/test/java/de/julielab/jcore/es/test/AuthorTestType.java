

/* First created by JCasGen Mon Feb 05 09:56:12 CET 2018 */
package de.julielab.jcore.es.test;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.TOP;


/** 
 * Updated by JCasGen Mon Feb 05 09:56:12 CET 2018
 * XML source: /Volumes/OUTERSPACE/Coding/git/jcore-base/jcore-elasticsearch-consumer/src/test/resources/de/julielab/jcore/consumer/es/testTypes.xml
 * @generated */
public class AuthorTestType extends TOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(AuthorTestType.class);
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
  protected AuthorTestType() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public AuthorTestType(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public AuthorTestType(JCas jcas) {
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
  //* Feature: firstname

  /** getter for firstname - gets 
   * @generated
   * @return value of the feature 
   */
  public String getFirstname() {
    if (AuthorTestType_Type.featOkTst && ((AuthorTestType_Type)jcasType).casFeat_firstname == null)
      jcasType.jcas.throwFeatMissing("firstname", "de.julielab.jcore.es.test.AuthorTestType");
    return jcasType.ll_cas.ll_getStringValue(addr, ((AuthorTestType_Type)jcasType).casFeatCode_firstname);}
    
  /** setter for firstname - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setFirstname(String v) {
    if (AuthorTestType_Type.featOkTst && ((AuthorTestType_Type)jcasType).casFeat_firstname == null)
      jcasType.jcas.throwFeatMissing("firstname", "de.julielab.jcore.es.test.AuthorTestType");
    jcasType.ll_cas.ll_setStringValue(addr, ((AuthorTestType_Type)jcasType).casFeatCode_firstname, v);}    
   
    
  //*--------------*
  //* Feature: lastname

  /** getter for lastname - gets 
   * @generated
   * @return value of the feature 
   */
  public String getLastname() {
    if (AuthorTestType_Type.featOkTst && ((AuthorTestType_Type)jcasType).casFeat_lastname == null)
      jcasType.jcas.throwFeatMissing("lastname", "de.julielab.jcore.es.test.AuthorTestType");
    return jcasType.ll_cas.ll_getStringValue(addr, ((AuthorTestType_Type)jcasType).casFeatCode_lastname);}
    
  /** setter for lastname - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLastname(String v) {
    if (AuthorTestType_Type.featOkTst && ((AuthorTestType_Type)jcasType).casFeat_lastname == null)
      jcasType.jcas.throwFeatMissing("lastname", "de.julielab.jcore.es.test.AuthorTestType");
    jcasType.ll_cas.ll_setStringValue(addr, ((AuthorTestType_Type)jcasType).casFeatCode_lastname, v);}    
   
    
  //*--------------*
  //* Feature: authorAddress

  /** getter for authorAddress - gets 
   * @generated
   * @return value of the feature 
   */
  public AddressTestType getAuthorAddress() {
    if (AuthorTestType_Type.featOkTst && ((AuthorTestType_Type)jcasType).casFeat_authorAddress == null)
      jcasType.jcas.throwFeatMissing("authorAddress", "de.julielab.jcore.es.test.AuthorTestType");
    return (AddressTestType)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AuthorTestType_Type)jcasType).casFeatCode_authorAddress)));}
    
  /** setter for authorAddress - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAuthorAddress(AddressTestType v) {
    if (AuthorTestType_Type.featOkTst && ((AuthorTestType_Type)jcasType).casFeat_authorAddress == null)
      jcasType.jcas.throwFeatMissing("authorAddress", "de.julielab.jcore.es.test.AuthorTestType");
    jcasType.ll_cas.ll_setRefValue(addr, ((AuthorTestType_Type)jcasType).casFeatCode_authorAddress, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    