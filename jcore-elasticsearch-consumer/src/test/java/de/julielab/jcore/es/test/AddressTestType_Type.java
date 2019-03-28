
/* First created by JCasGen Mon Feb 05 09:56:10 CET 2018 */
package de.julielab.jcore.es.test;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

/** 
 * Updated by JCasGen Mon Feb 05 09:56:10 CET 2018
 * @generated */
public class AddressTestType_Type extends TOP_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = AddressTestType.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.es.test.AddressTestType");
 
  /** @generated */
  final Feature casFeat_street;
  /** @generated */
  final int     casFeatCode_street;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getStreet(int addr) {
        if (featOkTst && casFeat_street == null)
      jcas.throwFeatMissing("street", "de.julielab.jcore.es.test.AddressTestType");
    return ll_cas.ll_getStringValue(addr, casFeatCode_street);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setStreet(int addr, String v) {
        if (featOkTst && casFeat_street == null)
      jcas.throwFeatMissing("street", "de.julielab.jcore.es.test.AddressTestType");
    ll_cas.ll_setStringValue(addr, casFeatCode_street, v);}
    
  
 
  /** @generated */
  final Feature casFeat_number;
  /** @generated */
  final int     casFeatCode_number;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getNumber(int addr) {
        if (featOkTst && casFeat_number == null)
      jcas.throwFeatMissing("number", "de.julielab.jcore.es.test.AddressTestType");
    return ll_cas.ll_getIntValue(addr, casFeatCode_number);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setNumber(int addr, int v) {
        if (featOkTst && casFeat_number == null)
      jcas.throwFeatMissing("number", "de.julielab.jcore.es.test.AddressTestType");
    ll_cas.ll_setIntValue(addr, casFeatCode_number, v);}
    
  
 
  /** @generated */
  final Feature casFeat_city;
  /** @generated */
  final int     casFeatCode_city;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getCity(int addr) {
        if (featOkTst && casFeat_city == null)
      jcas.throwFeatMissing("city", "de.julielab.jcore.es.test.AddressTestType");
    return ll_cas.ll_getStringValue(addr, casFeatCode_city);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setCity(int addr, String v) {
        if (featOkTst && casFeat_city == null)
      jcas.throwFeatMissing("city", "de.julielab.jcore.es.test.AddressTestType");
    ll_cas.ll_setStringValue(addr, casFeatCode_city, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public AddressTestType_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_street = jcas.getRequiredFeatureDE(casType, "street", "uima.cas.String", featOkTst);
    casFeatCode_street  = (null == casFeat_street) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_street).getCode();

 
    casFeat_number = jcas.getRequiredFeatureDE(casType, "number", "uima.cas.Integer", featOkTst);
    casFeatCode_number  = (null == casFeat_number) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_number).getCode();

 
    casFeat_city = jcas.getRequiredFeatureDE(casType, "city", "uima.cas.String", featOkTst);
    casFeatCode_city  = (null == casFeat_city) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_city).getCode();

  }
}



    