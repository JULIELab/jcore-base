
/* First created by JCasGen Wed Mar 21 14:47:03 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** 
 * Updated by JCasGen Wed Mar 21 14:47:03 CET 2018
 * @generated */
public class RelatedArticleList_Type extends Annotation_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = RelatedArticleList.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.types.RelatedArticleList");
 
  /** @generated */
  final Feature casFeat_relatedArticles;
  /** @generated */
  final int     casFeatCode_relatedArticles;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getRelatedArticles(int addr) {
        if (featOkTst && casFeat_relatedArticles == null)
      jcas.throwFeatMissing("relatedArticles", "de.julielab.jcore.types.RelatedArticleList");
    return ll_cas.ll_getRefValue(addr, casFeatCode_relatedArticles);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setRelatedArticles(int addr, int v) {
        if (featOkTst && casFeat_relatedArticles == null)
      jcas.throwFeatMissing("relatedArticles", "de.julielab.jcore.types.RelatedArticleList");
    ll_cas.ll_setRefValue(addr, casFeatCode_relatedArticles, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getRelatedArticles(int addr, int i) {
        if (featOkTst && casFeat_relatedArticles == null)
      jcas.throwFeatMissing("relatedArticles", "de.julielab.jcore.types.RelatedArticleList");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_relatedArticles), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_relatedArticles), i);
	return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_relatedArticles), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setRelatedArticles(int addr, int i, int v) {
        if (featOkTst && casFeat_relatedArticles == null)
      jcas.throwFeatMissing("relatedArticles", "de.julielab.jcore.types.RelatedArticleList");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_relatedArticles), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_relatedArticles), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_relatedArticles), i, v);
  }
 



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public RelatedArticleList_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_relatedArticles = jcas.getRequiredFeatureDE(casType, "relatedArticles", "uima.cas.FSArray", featOkTst);
    casFeatCode_relatedArticles  = (null == casFeat_relatedArticles) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_relatedArticles).getCode();

  }
}



    