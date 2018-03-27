
/* First created by JCasGen Wed Mar 21 14:47:02 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** the caption of figures, tables etc.

feature captionTitle is a Title-Annotation of the title of the caption, if existent.

feature captionType is an Enumeration, stating to what type of entity the caption belongs, e.g. figure or table
 * Updated by JCasGen Wed Mar 21 14:47:02 CET 2018
 * @generated */
public class Caption_Type extends Zone_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Caption.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.types.Caption");
 
  /** @generated */
  final Feature casFeat_captionTitle;
  /** @generated */
  final int     casFeatCode_captionTitle;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getCaptionTitle(int addr) {
        if (featOkTst && casFeat_captionTitle == null)
      jcas.throwFeatMissing("captionTitle", "de.julielab.jcore.types.Caption");
    return ll_cas.ll_getRefValue(addr, casFeatCode_captionTitle);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setCaptionTitle(int addr, int v) {
        if (featOkTst && casFeat_captionTitle == null)
      jcas.throwFeatMissing("captionTitle", "de.julielab.jcore.types.Caption");
    ll_cas.ll_setRefValue(addr, casFeatCode_captionTitle, v);}
    
  
 
  /** @generated */
  final Feature casFeat_captionType;
  /** @generated */
  final int     casFeatCode_captionType;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getCaptionType(int addr) {
        if (featOkTst && casFeat_captionType == null)
      jcas.throwFeatMissing("captionType", "de.julielab.jcore.types.Caption");
    return ll_cas.ll_getStringValue(addr, casFeatCode_captionType);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setCaptionType(int addr, String v) {
        if (featOkTst && casFeat_captionType == null)
      jcas.throwFeatMissing("captionType", "de.julielab.jcore.types.Caption");
    ll_cas.ll_setStringValue(addr, casFeatCode_captionType, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Caption_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_captionTitle = jcas.getRequiredFeatureDE(casType, "captionTitle", "de.julielab.jcore.types.Title", featOkTst);
    casFeatCode_captionTitle  = (null == casFeat_captionTitle) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_captionTitle).getCode();

 
    casFeat_captionType = jcas.getRequiredFeatureDE(casType, "captionType", "de.julielab.jcore.types.CaptionType", featOkTst);
    casFeatCode_captionType  = (null == casFeat_captionType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_captionType).getCode();

  }
}



    