
/* First created by JCasGen Wed Mar 21 14:47:03 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** Object, on our case, are annotations such as figures, tables, boxed text etc.
 * Updated by JCasGen Wed Mar 21 14:47:03 CET 2018
 * @generated */
public class TextObject_Type extends Zone_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = TextObject.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.types.TextObject");
 
  /** @generated */
  final Feature casFeat_objectType;
  /** @generated */
  final int     casFeatCode_objectType;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getObjectType(int addr) {
        if (featOkTst && casFeat_objectType == null)
      jcas.throwFeatMissing("objectType", "de.julielab.jcore.types.TextObject");
    return ll_cas.ll_getStringValue(addr, casFeatCode_objectType);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setObjectType(int addr, String v) {
        if (featOkTst && casFeat_objectType == null)
      jcas.throwFeatMissing("objectType", "de.julielab.jcore.types.TextObject");
    ll_cas.ll_setStringValue(addr, casFeatCode_objectType, v);}
    
  
 
  /** @generated */
  final Feature casFeat_objectId;
  /** @generated */
  final int     casFeatCode_objectId;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getObjectId(int addr) {
        if (featOkTst && casFeat_objectId == null)
      jcas.throwFeatMissing("objectId", "de.julielab.jcore.types.TextObject");
    return ll_cas.ll_getStringValue(addr, casFeatCode_objectId);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setObjectId(int addr, String v) {
        if (featOkTst && casFeat_objectId == null)
      jcas.throwFeatMissing("objectId", "de.julielab.jcore.types.TextObject");
    ll_cas.ll_setStringValue(addr, casFeatCode_objectId, v);}
    
  
 
  /** @generated */
  final Feature casFeat_objectLabel;
  /** @generated */
  final int     casFeatCode_objectLabel;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getObjectLabel(int addr) {
        if (featOkTst && casFeat_objectLabel == null)
      jcas.throwFeatMissing("objectLabel", "de.julielab.jcore.types.TextObject");
    return ll_cas.ll_getStringValue(addr, casFeatCode_objectLabel);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setObjectLabel(int addr, String v) {
        if (featOkTst && casFeat_objectLabel == null)
      jcas.throwFeatMissing("objectLabel", "de.julielab.jcore.types.TextObject");
    ll_cas.ll_setStringValue(addr, casFeatCode_objectLabel, v);}
    
  
 
  /** @generated */
  final Feature casFeat_objectCaption;
  /** @generated */
  final int     casFeatCode_objectCaption;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getObjectCaption(int addr) {
        if (featOkTst && casFeat_objectCaption == null)
      jcas.throwFeatMissing("objectCaption", "de.julielab.jcore.types.TextObject");
    return ll_cas.ll_getRefValue(addr, casFeatCode_objectCaption);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setObjectCaption(int addr, int v) {
        if (featOkTst && casFeat_objectCaption == null)
      jcas.throwFeatMissing("objectCaption", "de.julielab.jcore.types.TextObject");
    ll_cas.ll_setRefValue(addr, casFeatCode_objectCaption, v);}
    
  
 
  /** @generated */
  final Feature casFeat_objectTitle;
  /** @generated */
  final int     casFeatCode_objectTitle;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getObjectTitle(int addr) {
        if (featOkTst && casFeat_objectTitle == null)
      jcas.throwFeatMissing("objectTitle", "de.julielab.jcore.types.TextObject");
    return ll_cas.ll_getRefValue(addr, casFeatCode_objectTitle);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setObjectTitle(int addr, int v) {
        if (featOkTst && casFeat_objectTitle == null)
      jcas.throwFeatMissing("objectTitle", "de.julielab.jcore.types.TextObject");
    ll_cas.ll_setRefValue(addr, casFeatCode_objectTitle, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public TextObject_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_objectType = jcas.getRequiredFeatureDE(casType, "objectType", "uima.cas.String", featOkTst);
    casFeatCode_objectType  = (null == casFeat_objectType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_objectType).getCode();

 
    casFeat_objectId = jcas.getRequiredFeatureDE(casType, "objectId", "uima.cas.String", featOkTst);
    casFeatCode_objectId  = (null == casFeat_objectId) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_objectId).getCode();

 
    casFeat_objectLabel = jcas.getRequiredFeatureDE(casType, "objectLabel", "uima.cas.String", featOkTst);
    casFeatCode_objectLabel  = (null == casFeat_objectLabel) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_objectLabel).getCode();

 
    casFeat_objectCaption = jcas.getRequiredFeatureDE(casType, "objectCaption", "de.julielab.jcore.types.Caption", featOkTst);
    casFeatCode_objectCaption  = (null == casFeat_objectCaption) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_objectCaption).getCode();

 
    casFeat_objectTitle = jcas.getRequiredFeatureDE(casType, "objectTitle", "de.julielab.jcore.types.Title", featOkTst);
    casFeatCode_objectTitle  = (null == casFeat_objectTitle) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_objectTitle).getCode();

  }
}



    