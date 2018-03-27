
/* First created by JCasGen Wed Mar 21 14:47:02 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** Footnotes of all kinds, i.e. footnotes found in running text or in tables etc.
 * Updated by JCasGen Wed Mar 21 14:47:02 CET 2018
 * @generated */
public class Footnote_Type extends Zone_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Footnote.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.types.Footnote");
 
  /** @generated */
  final Feature casFeat_footnoteTitle;
  /** @generated */
  final int     casFeatCode_footnoteTitle;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getFootnoteTitle(int addr) {
        if (featOkTst && casFeat_footnoteTitle == null)
      jcas.throwFeatMissing("footnoteTitle", "de.julielab.jcore.types.Footnote");
    return ll_cas.ll_getRefValue(addr, casFeatCode_footnoteTitle);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setFootnoteTitle(int addr, int v) {
        if (featOkTst && casFeat_footnoteTitle == null)
      jcas.throwFeatMissing("footnoteTitle", "de.julielab.jcore.types.Footnote");
    ll_cas.ll_setRefValue(addr, casFeatCode_footnoteTitle, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Footnote_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_footnoteTitle = jcas.getRequiredFeatureDE(casType, "footnoteTitle", "de.julielab.jcore.types.Title", featOkTst);
    casFeatCode_footnoteTitle  = (null == casFeat_footnoteTitle) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_footnoteTitle).getCode();

  }
}



    