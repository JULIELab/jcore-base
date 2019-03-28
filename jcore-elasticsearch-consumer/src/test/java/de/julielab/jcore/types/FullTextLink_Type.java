
/* First created by JCasGen Mon Feb 05 09:56:20 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/** 
 * Updated by JCasGen Mon Feb 05 09:56:20 CET 2018
 * @generated */
public class FullTextLink_Type extends Annotation_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = FullTextLink.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.types.FullTextLink");
 
  /** @generated */
  final Feature casFeat_url;
  /** @generated */
  final int     casFeatCode_url;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getUrl(int addr) {
        if (featOkTst && casFeat_url == null)
      jcas.throwFeatMissing("url", "de.julielab.jcore.types.FullTextLink");
    return ll_cas.ll_getStringValue(addr, casFeatCode_url);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setUrl(int addr, String v) {
        if (featOkTst && casFeat_url == null)
      jcas.throwFeatMissing("url", "de.julielab.jcore.types.FullTextLink");
    ll_cas.ll_setStringValue(addr, casFeatCode_url, v);}
    
  
 
  /** @generated */
  final Feature casFeat_iconUrl;
  /** @generated */
  final int     casFeatCode_iconUrl;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getIconUrl(int addr) {
        if (featOkTst && casFeat_iconUrl == null)
      jcas.throwFeatMissing("iconUrl", "de.julielab.jcore.types.FullTextLink");
    return ll_cas.ll_getStringValue(addr, casFeatCode_iconUrl);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setIconUrl(int addr, String v) {
        if (featOkTst && casFeat_iconUrl == null)
      jcas.throwFeatMissing("iconUrl", "de.julielab.jcore.types.FullTextLink");
    ll_cas.ll_setStringValue(addr, casFeatCode_iconUrl, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public FullTextLink_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_url = jcas.getRequiredFeatureDE(casType, "url", "uima.cas.String", featOkTst);
    casFeatCode_url  = (null == casFeat_url) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_url).getCode();

 
    casFeat_iconUrl = jcas.getRequiredFeatureDE(casType, "iconUrl", "uima.cas.String", featOkTst);
    casFeatCode_iconUrl  = (null == casFeat_iconUrl) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_iconUrl).getCode();

  }
}



    