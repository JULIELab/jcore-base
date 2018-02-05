
/* First created by JCasGen Mon Feb 05 09:56:20 CET 2018 */
package de.julielab.jcore.es.test;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.cas.TOP_Type;

/** 
 * Updated by JCasGen Mon Feb 05 09:56:20 CET 2018
 * @generated */
public class AuthorTestType_Type extends TOP_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = AuthorTestType.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.es.test.AuthorTestType");
 
  /** @generated */
  final Feature casFeat_firstname;
  /** @generated */
  final int     casFeatCode_firstname;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getFirstname(int addr) {
        if (featOkTst && casFeat_firstname == null)
      jcas.throwFeatMissing("firstname", "de.julielab.jcore.es.test.AuthorTestType");
    return ll_cas.ll_getStringValue(addr, casFeatCode_firstname);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setFirstname(int addr, String v) {
        if (featOkTst && casFeat_firstname == null)
      jcas.throwFeatMissing("firstname", "de.julielab.jcore.es.test.AuthorTestType");
    ll_cas.ll_setStringValue(addr, casFeatCode_firstname, v);}
    
  
 
  /** @generated */
  final Feature casFeat_lastname;
  /** @generated */
  final int     casFeatCode_lastname;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getLastname(int addr) {
        if (featOkTst && casFeat_lastname == null)
      jcas.throwFeatMissing("lastname", "de.julielab.jcore.es.test.AuthorTestType");
    return ll_cas.ll_getStringValue(addr, casFeatCode_lastname);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setLastname(int addr, String v) {
        if (featOkTst && casFeat_lastname == null)
      jcas.throwFeatMissing("lastname", "de.julielab.jcore.es.test.AuthorTestType");
    ll_cas.ll_setStringValue(addr, casFeatCode_lastname, v);}
    
  
 
  /** @generated */
  final Feature casFeat_authorAddress;
  /** @generated */
  final int     casFeatCode_authorAddress;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAuthorAddress(int addr) {
        if (featOkTst && casFeat_authorAddress == null)
      jcas.throwFeatMissing("authorAddress", "de.julielab.jcore.es.test.AuthorTestType");
    return ll_cas.ll_getRefValue(addr, casFeatCode_authorAddress);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAuthorAddress(int addr, int v) {
        if (featOkTst && casFeat_authorAddress == null)
      jcas.throwFeatMissing("authorAddress", "de.julielab.jcore.es.test.AuthorTestType");
    ll_cas.ll_setRefValue(addr, casFeatCode_authorAddress, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public AuthorTestType_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_firstname = jcas.getRequiredFeatureDE(casType, "firstname", "uima.cas.String", featOkTst);
    casFeatCode_firstname  = (null == casFeat_firstname) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_firstname).getCode();

 
    casFeat_lastname = jcas.getRequiredFeatureDE(casType, "lastname", "uima.cas.String", featOkTst);
    casFeatCode_lastname  = (null == casFeat_lastname) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_lastname).getCode();

 
    casFeat_authorAddress = jcas.getRequiredFeatureDE(casType, "authorAddress", "de.julielab.jcore.es.test.AddressTestType", featOkTst);
    casFeatCode_authorAddress  = (null == casFeat_authorAddress) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_authorAddress).getCode();

  }
}



    