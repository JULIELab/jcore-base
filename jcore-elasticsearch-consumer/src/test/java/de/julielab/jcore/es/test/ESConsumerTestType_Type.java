
/* First created by JCasGen Mon Feb 05 09:56:20 CET 2018 */
package de.julielab.jcore.es.test;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.Annotation_Type;

/** This test type exist to exhibit features useful for testing the ElasticSearch consumer algorithms.
 * Updated by JCasGen Mon Feb 05 09:56:20 CET 2018
 * @generated */
public class ESConsumerTestType_Type extends Annotation_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = ESConsumerTestType.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.es.test.ESConsumerTestType");
 
  /** @generated */
  final Feature casFeat_stringFeature;
  /** @generated */
  final int     casFeatCode_stringFeature;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getStringFeature(int addr) {
        if (featOkTst && casFeat_stringFeature == null)
      jcas.throwFeatMissing("stringFeature", "de.julielab.jcore.es.test.ESConsumerTestType");
    return ll_cas.ll_getStringValue(addr, casFeatCode_stringFeature);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setStringFeature(int addr, String v) {
        if (featOkTst && casFeat_stringFeature == null)
      jcas.throwFeatMissing("stringFeature", "de.julielab.jcore.es.test.ESConsumerTestType");
    ll_cas.ll_setStringValue(addr, casFeatCode_stringFeature, v);}
    
  
 
  /** @generated */
  final Feature casFeat_stringArrayFeature;
  /** @generated */
  final int     casFeatCode_stringArrayFeature;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getStringArrayFeature(int addr) {
        if (featOkTst && casFeat_stringArrayFeature == null)
      jcas.throwFeatMissing("stringArrayFeature", "de.julielab.jcore.es.test.ESConsumerTestType");
    return ll_cas.ll_getRefValue(addr, casFeatCode_stringArrayFeature);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setStringArrayFeature(int addr, int v) {
        if (featOkTst && casFeat_stringArrayFeature == null)
      jcas.throwFeatMissing("stringArrayFeature", "de.julielab.jcore.es.test.ESConsumerTestType");
    ll_cas.ll_setRefValue(addr, casFeatCode_stringArrayFeature, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public String getStringArrayFeature(int addr, int i) {
        if (featOkTst && casFeat_stringArrayFeature == null)
      jcas.throwFeatMissing("stringArrayFeature", "de.julielab.jcore.es.test.ESConsumerTestType");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_stringArrayFeature), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_stringArrayFeature), i);
	return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_stringArrayFeature), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setStringArrayFeature(int addr, int i, String v) {
        if (featOkTst && casFeat_stringArrayFeature == null)
      jcas.throwFeatMissing("stringArrayFeature", "de.julielab.jcore.es.test.ESConsumerTestType");
    if (lowLevelTypeChecks)
      ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_stringArrayFeature), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_stringArrayFeature), i);
    ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_stringArrayFeature), i, v);
  }
 



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public ESConsumerTestType_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_stringFeature = jcas.getRequiredFeatureDE(casType, "stringFeature", "uima.cas.String", featOkTst);
    casFeatCode_stringFeature  = (null == casFeat_stringFeature) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_stringFeature).getCode();

 
    casFeat_stringArrayFeature = jcas.getRequiredFeatureDE(casType, "stringArrayFeature", "uima.cas.StringArray", featOkTst);
    casFeatCode_stringArrayFeature  = (null == casFeat_stringArrayFeature) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_stringArrayFeature).getCode();

  }
}



    