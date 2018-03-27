
/* First created by JCasGen Thu Mar 22 17:37:32 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** Used for document-internal references like literature references, footnotes etc.
 * Updated by JCasGen Thu Mar 22 17:37:32 CET 2018
 * @generated */
public class InternalReference_Type extends Annotation_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = InternalReference.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.types.InternalReference");
 
  /** @generated */
  final Feature casFeat_reftype;
  /** @generated */
  final int     casFeatCode_reftype;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getReftype(int addr) {
        if (featOkTst && casFeat_reftype == null)
      jcas.throwFeatMissing("reftype", "de.julielab.jcore.types.InternalReference");
    return ll_cas.ll_getStringValue(addr, casFeatCode_reftype);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setReftype(int addr, String v) {
        if (featOkTst && casFeat_reftype == null)
      jcas.throwFeatMissing("reftype", "de.julielab.jcore.types.InternalReference");
    ll_cas.ll_setStringValue(addr, casFeatCode_reftype, v);}
    
  
 
  /** @generated */
  final Feature casFeat_refid;
  /** @generated */
  final int     casFeatCode_refid;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getRefid(int addr) {
        if (featOkTst && casFeat_refid == null)
      jcas.throwFeatMissing("refid", "de.julielab.jcore.types.InternalReference");
    return ll_cas.ll_getStringValue(addr, casFeatCode_refid);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setRefid(int addr, String v) {
        if (featOkTst && casFeat_refid == null)
      jcas.throwFeatMissing("refid", "de.julielab.jcore.types.InternalReference");
    ll_cas.ll_setStringValue(addr, casFeatCode_refid, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public InternalReference_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_reftype = jcas.getRequiredFeatureDE(casType, "reftype", "de.julielab.jcore.types.ReferenceType", featOkTst);
    casFeatCode_reftype  = (null == casFeat_reftype) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_reftype).getCode();

 
    casFeat_refid = jcas.getRequiredFeatureDE(casType, "refid", "uima.cas.String", featOkTst);
    casFeatCode_refid  = (null == casFeat_refid) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_refid).getCode();

  }
}



    