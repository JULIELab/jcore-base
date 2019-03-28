
/* First created by JCasGen Mon Feb 05 09:56:20 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.tcas.Annotation_Type;

/** A document class specification for the CAS' document text.
 * Updated by JCasGen Mon Feb 05 09:56:20 CET 2018
 * @generated */
public class DocumentClass_Type extends Annotation_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = DocumentClass.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.types.DocumentClass");
 
  /** @generated */
  final Feature casFeat_classname;
  /** @generated */
  final int     casFeatCode_classname;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getClassname(int addr) {
        if (featOkTst && casFeat_classname == null)
      jcas.throwFeatMissing("classname", "de.julielab.jcore.types.DocumentClass");
    return ll_cas.ll_getStringValue(addr, casFeatCode_classname);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setClassname(int addr, String v) {
        if (featOkTst && casFeat_classname == null)
      jcas.throwFeatMissing("classname", "de.julielab.jcore.types.DocumentClass");
    ll_cas.ll_setStringValue(addr, casFeatCode_classname, v);}
    
  
 
  /** @generated */
  final Feature casFeat_confidence;
  /** @generated */
  final int     casFeatCode_confidence;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public double getConfidence(int addr) {
        if (featOkTst && casFeat_confidence == null)
      jcas.throwFeatMissing("confidence", "de.julielab.jcore.types.DocumentClass");
    return ll_cas.ll_getDoubleValue(addr, casFeatCode_confidence);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setConfidence(int addr, double v) {
        if (featOkTst && casFeat_confidence == null)
      jcas.throwFeatMissing("confidence", "de.julielab.jcore.types.DocumentClass");
    ll_cas.ll_setDoubleValue(addr, casFeatCode_confidence, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public DocumentClass_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_classname = jcas.getRequiredFeatureDE(casType, "classname", "uima.cas.String", featOkTst);
    casFeatCode_classname  = (null == casFeat_classname) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_classname).getCode();

 
    casFeat_confidence = jcas.getRequiredFeatureDE(casType, "confidence", "uima.cas.Double", featOkTst);
    casFeatCode_confidence  = (null == casFeat_confidence) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_confidence).getCode();

  }
}



    