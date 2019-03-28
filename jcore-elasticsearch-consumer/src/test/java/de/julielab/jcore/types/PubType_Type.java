
/* First created by JCasGen Mon Feb 05 09:56:22 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/** An abstract type which should be used to store information on the publication. See subtypes Journal and an accumulative type (OtherPub)
 * Updated by JCasGen Mon Feb 05 09:56:22 CET 2018
 * @generated */
public class PubType_Type extends Annotation_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = PubType.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.types.PubType");
 
  /** @generated */
  final Feature casFeat_name;
  /** @generated */
  final int     casFeatCode_name;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getName(int addr) {
        if (featOkTst && casFeat_name == null)
      jcas.throwFeatMissing("name", "de.julielab.jcore.types.PubType");
    return ll_cas.ll_getStringValue(addr, casFeatCode_name);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setName(int addr, String v) {
        if (featOkTst && casFeat_name == null)
      jcas.throwFeatMissing("name", "de.julielab.jcore.types.PubType");
    ll_cas.ll_setStringValue(addr, casFeatCode_name, v);}
    
  
 
  /** @generated */
  final Feature casFeat_pubDate;
  /** @generated */
  final int     casFeatCode_pubDate;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getPubDate(int addr) {
        if (featOkTst && casFeat_pubDate == null)
      jcas.throwFeatMissing("pubDate", "de.julielab.jcore.types.PubType");
    return ll_cas.ll_getRefValue(addr, casFeatCode_pubDate);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setPubDate(int addr, int v) {
        if (featOkTst && casFeat_pubDate == null)
      jcas.throwFeatMissing("pubDate", "de.julielab.jcore.types.PubType");
    ll_cas.ll_setRefValue(addr, casFeatCode_pubDate, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public PubType_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_name = jcas.getRequiredFeatureDE(casType, "name", "uima.cas.String", featOkTst);
    casFeatCode_name  = (null == casFeat_name) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_name).getCode();

 
    casFeat_pubDate = jcas.getRequiredFeatureDE(casType, "pubDate", "de.julielab.jcore.types.Date", featOkTst);
    casFeatCode_pubDate  = (null == casFeat_pubDate) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_pubDate).getCode();

  }
}



    