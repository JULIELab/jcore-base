
/* First created by JCasGen Wed Mar 21 14:47:03 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** Title annotates titles covering various text units, including the whole paper, sections and subsections.
 * Updated by JCasGen Wed Mar 21 14:47:03 CET 2018
 * @generated */
public class Title_Type extends Zone_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Title.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.types.Title");
 
  /** @generated */
  final Feature casFeat_titleType;
  /** @generated */
  final int     casFeatCode_titleType;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getTitleType(int addr) {
        if (featOkTst && casFeat_titleType == null)
      jcas.throwFeatMissing("titleType", "de.julielab.jcore.types.Title");
    return ll_cas.ll_getStringValue(addr, casFeatCode_titleType);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTitleType(int addr, String v) {
        if (featOkTst && casFeat_titleType == null)
      jcas.throwFeatMissing("titleType", "de.julielab.jcore.types.Title");
    ll_cas.ll_setStringValue(addr, casFeatCode_titleType, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Title_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_titleType = jcas.getRequiredFeatureDE(casType, "titleType", "de.julielab.jcore.types.TitleType", featOkTst);
    casFeatCode_titleType  = (null == casFeat_titleType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_titleType).getCode();

  }
}



    