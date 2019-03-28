
/* First created by JCasGen Mon Feb 05 09:56:20 CET 2018 */
package de.julielab.jcore.es.test;

import de.julielab.jcore.types.Header_Type;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/** 
 * Updated by JCasGen Mon Feb 05 09:56:20 CET 2018
 * @generated */
public class HeaderTestType_Type extends Header_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = HeaderTestType.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.es.test.HeaderTestType");
 
  /** @generated */
  final Feature casFeat_testAuthors;
  /** @generated */
  final int     casFeatCode_testAuthors;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getTestAuthors(int addr) {
        if (featOkTst && casFeat_testAuthors == null)
      jcas.throwFeatMissing("testAuthors", "de.julielab.jcore.es.test.HeaderTestType");
    return ll_cas.ll_getRefValue(addr, casFeatCode_testAuthors);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTestAuthors(int addr, int v) {
        if (featOkTst && casFeat_testAuthors == null)
      jcas.throwFeatMissing("testAuthors", "de.julielab.jcore.es.test.HeaderTestType");
    ll_cas.ll_setRefValue(addr, casFeatCode_testAuthors, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getTestAuthors(int addr, int i) {
        if (featOkTst && casFeat_testAuthors == null)
      jcas.throwFeatMissing("testAuthors", "de.julielab.jcore.es.test.HeaderTestType");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_testAuthors), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_testAuthors), i);
	return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_testAuthors), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setTestAuthors(int addr, int i, int v) {
        if (featOkTst && casFeat_testAuthors == null)
      jcas.throwFeatMissing("testAuthors", "de.julielab.jcore.es.test.HeaderTestType");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_testAuthors), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_testAuthors), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_testAuthors), i, v);
  }
 



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public HeaderTestType_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_testAuthors = jcas.getRequiredFeatureDE(casType, "testAuthors", "uima.cas.FSArray", featOkTst);
    casFeatCode_testAuthors  = (null == casFeat_testAuthors) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_testAuthors).getCode();

  }
}



    