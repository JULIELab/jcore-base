
/* First created by JCasGen Mon Feb 05 09:56:22 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** The descriptor type for the manually added information.
 * Updated by JCasGen Mon Feb 05 09:56:22 CET 2018
 * @generated */
public class ManualDescriptor_Type extends Descriptor_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = ManualDescriptor.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.types.ManualDescriptor");
 
  /** @generated */
  final Feature casFeat_metaInfo;
  /** @generated */
  final int     casFeatCode_metaInfo;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getMetaInfo(int addr) {
        if (featOkTst && casFeat_metaInfo == null)
      jcas.throwFeatMissing("metaInfo", "de.julielab.jcore.types.ManualDescriptor");
    return ll_cas.ll_getStringValue(addr, casFeatCode_metaInfo);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setMetaInfo(int addr, String v) {
        if (featOkTst && casFeat_metaInfo == null)
      jcas.throwFeatMissing("metaInfo", "de.julielab.jcore.types.ManualDescriptor");
    ll_cas.ll_setStringValue(addr, casFeatCode_metaInfo, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public ManualDescriptor_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_metaInfo = jcas.getRequiredFeatureDE(casType, "metaInfo", "uima.cas.String", featOkTst);
    casFeatCode_metaInfo  = (null == casFeat_metaInfo) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_metaInfo).getCode();

  }
}



    