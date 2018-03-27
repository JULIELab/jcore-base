
/* First created by JCasGen Sun Mar 18 12:36:58 CET 2018 */
package de.julielab.jcore.types.pubmed;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** The special Header for PubMed (http://www.pubmed.org)
        documents
 * Updated by JCasGen Wed Mar 21 14:47:03 CET 2018
 * @generated */
public class Header_Type extends de.julielab.jcore.types.Header_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Header.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.types.pubmed.Header");
 
  /** @generated */
  final Feature casFeat_citationStatus;
  /** @generated */
  final int     casFeatCode_citationStatus;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getCitationStatus(int addr) {
        if (featOkTst && casFeat_citationStatus == null)
      jcas.throwFeatMissing("citationStatus", "de.julielab.jcore.types.pubmed.Header");
    return ll_cas.ll_getStringValue(addr, casFeatCode_citationStatus);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setCitationStatus(int addr, String v) {
        if (featOkTst && casFeat_citationStatus == null)
      jcas.throwFeatMissing("citationStatus", "de.julielab.jcore.types.pubmed.Header");
    ll_cas.ll_setStringValue(addr, casFeatCode_citationStatus, v);}
    
  
 
  /** @generated */
  final Feature casFeat_otherIDs;
  /** @generated */
  final int     casFeatCode_otherIDs;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getOtherIDs(int addr) {
        if (featOkTst && casFeat_otherIDs == null)
      jcas.throwFeatMissing("otherIDs", "de.julielab.jcore.types.pubmed.Header");
    return ll_cas.ll_getRefValue(addr, casFeatCode_otherIDs);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setOtherIDs(int addr, int v) {
        if (featOkTst && casFeat_otherIDs == null)
      jcas.throwFeatMissing("otherIDs", "de.julielab.jcore.types.pubmed.Header");
    ll_cas.ll_setRefValue(addr, casFeatCode_otherIDs, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getOtherIDs(int addr, int i) {
        if (featOkTst && casFeat_otherIDs == null)
      jcas.throwFeatMissing("otherIDs", "de.julielab.jcore.types.pubmed.Header");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_otherIDs), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_otherIDs), i);
  return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_otherIDs), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setOtherIDs(int addr, int i, int v) {
        if (featOkTst && casFeat_otherIDs == null)
      jcas.throwFeatMissing("otherIDs", "de.julielab.jcore.types.pubmed.Header");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_otherIDs), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_otherIDs), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_otherIDs), i, v);
  }
 



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Header_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_citationStatus = jcas.getRequiredFeatureDE(casType, "citationStatus", "de.julielab.jcore.types.CitationStatus", featOkTst);
    casFeatCode_citationStatus  = (null == casFeat_citationStatus) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_citationStatus).getCode();

 
    casFeat_otherIDs = jcas.getRequiredFeatureDE(casType, "otherIDs", "uima.cas.FSArray", featOkTst);
    casFeatCode_otherIDs  = (null == casFeat_otherIDs) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_otherIDs).getCode();

  }
}



    