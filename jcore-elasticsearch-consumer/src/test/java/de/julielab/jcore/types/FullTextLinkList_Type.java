
/* First created by JCasGen Mon Feb 05 09:56:21 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/** 
 * Updated by JCasGen Mon Feb 05 09:56:21 CET 2018
 * @generated */
public class FullTextLinkList_Type extends Annotation_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = FullTextLinkList.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.types.FullTextLinkList");
 
  /** @generated */
  final Feature casFeat_fullTextLinks;
  /** @generated */
  final int     casFeatCode_fullTextLinks;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getFullTextLinks(int addr) {
        if (featOkTst && casFeat_fullTextLinks == null)
      jcas.throwFeatMissing("fullTextLinks", "de.julielab.jcore.types.FullTextLinkList");
    return ll_cas.ll_getRefValue(addr, casFeatCode_fullTextLinks);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setFullTextLinks(int addr, int v) {
        if (featOkTst && casFeat_fullTextLinks == null)
      jcas.throwFeatMissing("fullTextLinks", "de.julielab.jcore.types.FullTextLinkList");
    ll_cas.ll_setRefValue(addr, casFeatCode_fullTextLinks, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getFullTextLinks(int addr, int i) {
        if (featOkTst && casFeat_fullTextLinks == null)
      jcas.throwFeatMissing("fullTextLinks", "de.julielab.jcore.types.FullTextLinkList");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_fullTextLinks), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_fullTextLinks), i);
	return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_fullTextLinks), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setFullTextLinks(int addr, int i, int v) {
        if (featOkTst && casFeat_fullTextLinks == null)
      jcas.throwFeatMissing("fullTextLinks", "de.julielab.jcore.types.FullTextLinkList");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_fullTextLinks), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_fullTextLinks), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_fullTextLinks), i, v);
  }
 



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public FullTextLinkList_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_fullTextLinks = jcas.getRequiredFeatureDE(casType, "fullTextLinks", "uima.cas.FSArray", featOkTst);
    casFeatCode_fullTextLinks  = (null == casFeat_fullTextLinks) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_fullTextLinks).getCode();

  }
}



    