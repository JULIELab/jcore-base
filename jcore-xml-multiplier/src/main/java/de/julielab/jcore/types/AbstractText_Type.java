
/* First created by JCasGen Wed Mar 21 14:47:02 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** Annotation of the complete abstract.
 * Updated by JCasGen Wed Mar 21 14:47:02 CET 2018
 * @generated */
public class AbstractText_Type extends Zone_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = AbstractText.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.types.AbstractText");
 
  /** @generated */
  final Feature casFeat_structuredAbstractParts;
  /** @generated */
  final int     casFeatCode_structuredAbstractParts;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getStructuredAbstractParts(int addr) {
        if (featOkTst && casFeat_structuredAbstractParts == null)
      jcas.throwFeatMissing("structuredAbstractParts", "de.julielab.jcore.types.AbstractText");
    return ll_cas.ll_getRefValue(addr, casFeatCode_structuredAbstractParts);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setStructuredAbstractParts(int addr, int v) {
        if (featOkTst && casFeat_structuredAbstractParts == null)
      jcas.throwFeatMissing("structuredAbstractParts", "de.julielab.jcore.types.AbstractText");
    ll_cas.ll_setRefValue(addr, casFeatCode_structuredAbstractParts, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getStructuredAbstractParts(int addr, int i) {
        if (featOkTst && casFeat_structuredAbstractParts == null)
      jcas.throwFeatMissing("structuredAbstractParts", "de.julielab.jcore.types.AbstractText");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_structuredAbstractParts), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_structuredAbstractParts), i);
	return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_structuredAbstractParts), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setStructuredAbstractParts(int addr, int i, int v) {
        if (featOkTst && casFeat_structuredAbstractParts == null)
      jcas.throwFeatMissing("structuredAbstractParts", "de.julielab.jcore.types.AbstractText");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_structuredAbstractParts), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_structuredAbstractParts), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_structuredAbstractParts), i, v);
  }
 



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public AbstractText_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_structuredAbstractParts = jcas.getRequiredFeatureDE(casType, "structuredAbstractParts", "uima.cas.FSArray", featOkTst);
    casFeatCode_structuredAbstractParts  = (null == casFeat_structuredAbstractParts) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_structuredAbstractParts).getCode();

  }
}



    