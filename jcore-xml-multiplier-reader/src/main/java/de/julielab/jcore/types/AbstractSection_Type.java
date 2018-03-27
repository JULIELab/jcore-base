
/* First created by JCasGen Thu Mar 22 17:37:32 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** 
 * Updated by JCasGen Thu Mar 22 17:37:32 CET 2018
 * @generated */
public class AbstractSection_Type extends Zone_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = AbstractSection.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.types.AbstractSection");
 
  /** @generated */
  final Feature casFeat_abstractSectionHeading;
  /** @generated */
  final int     casFeatCode_abstractSectionHeading;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAbstractSectionHeading(int addr) {
        if (featOkTst && casFeat_abstractSectionHeading == null)
      jcas.throwFeatMissing("abstractSectionHeading", "de.julielab.jcore.types.AbstractSection");
    return ll_cas.ll_getRefValue(addr, casFeatCode_abstractSectionHeading);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAbstractSectionHeading(int addr, int v) {
        if (featOkTst && casFeat_abstractSectionHeading == null)
      jcas.throwFeatMissing("abstractSectionHeading", "de.julielab.jcore.types.AbstractSection");
    ll_cas.ll_setRefValue(addr, casFeatCode_abstractSectionHeading, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public AbstractSection_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_abstractSectionHeading = jcas.getRequiredFeatureDE(casType, "abstractSectionHeading", "de.julielab.jcore.types.Title", featOkTst);
    casFeatCode_abstractSectionHeading  = (null == casFeat_abstractSectionHeading) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_abstractSectionHeading).getCode();

  }
}



    