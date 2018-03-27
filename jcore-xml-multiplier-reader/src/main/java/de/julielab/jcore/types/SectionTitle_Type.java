
/* First created by JCasGen Thu Mar 22 17:37:33 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** Title of a text section in contrast to the title of the whole document.
 * Updated by JCasGen Thu Mar 22 17:37:33 CET 2018
 * @generated */
public class SectionTitle_Type extends Title_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = SectionTitle.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.types.SectionTitle");
 
  /** @generated */
  final Feature casFeat_depth;
  /** @generated */
  final int     casFeatCode_depth;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getDepth(int addr) {
        if (featOkTst && casFeat_depth == null)
      jcas.throwFeatMissing("depth", "de.julielab.jcore.types.SectionTitle");
    return ll_cas.ll_getIntValue(addr, casFeatCode_depth);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setDepth(int addr, int v) {
        if (featOkTst && casFeat_depth == null)
      jcas.throwFeatMissing("depth", "de.julielab.jcore.types.SectionTitle");
    ll_cas.ll_setIntValue(addr, casFeatCode_depth, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public SectionTitle_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_depth = jcas.getRequiredFeatureDE(casType, "depth", "uima.cas.Integer", featOkTst);
    casFeatCode_depth  = (null == casFeat_depth) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_depth).getCode();

  }
}



    