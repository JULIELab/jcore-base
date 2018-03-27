
/* First created by JCasGen Thu Mar 22 17:37:33 CET 2018 */
package de.julielab.jcore.types.pubmed;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** The special type for PubMed documents
 * Updated by JCasGen Thu Mar 22 17:37:33 CET 2018
 * @generated */
public class ManualDescriptor_Type extends de.julielab.jcore.types.ManualDescriptor_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = ManualDescriptor.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.types.pubmed.ManualDescriptor");
 
  /** @generated */
  final Feature casFeat_meSHList;
  /** @generated */
  final int     casFeatCode_meSHList;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getMeSHList(int addr) {
        if (featOkTst && casFeat_meSHList == null)
      jcas.throwFeatMissing("meSHList", "de.julielab.jcore.types.pubmed.ManualDescriptor");
    return ll_cas.ll_getRefValue(addr, casFeatCode_meSHList);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setMeSHList(int addr, int v) {
        if (featOkTst && casFeat_meSHList == null)
      jcas.throwFeatMissing("meSHList", "de.julielab.jcore.types.pubmed.ManualDescriptor");
    ll_cas.ll_setRefValue(addr, casFeatCode_meSHList, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getMeSHList(int addr, int i) {
        if (featOkTst && casFeat_meSHList == null)
      jcas.throwFeatMissing("meSHList", "de.julielab.jcore.types.pubmed.ManualDescriptor");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_meSHList), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_meSHList), i);
	return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_meSHList), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setMeSHList(int addr, int i, int v) {
        if (featOkTst && casFeat_meSHList == null)
      jcas.throwFeatMissing("meSHList", "de.julielab.jcore.types.pubmed.ManualDescriptor");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_meSHList), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_meSHList), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_meSHList), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_chemicalList;
  /** @generated */
  final int     casFeatCode_chemicalList;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getChemicalList(int addr) {
        if (featOkTst && casFeat_chemicalList == null)
      jcas.throwFeatMissing("chemicalList", "de.julielab.jcore.types.pubmed.ManualDescriptor");
    return ll_cas.ll_getRefValue(addr, casFeatCode_chemicalList);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setChemicalList(int addr, int v) {
        if (featOkTst && casFeat_chemicalList == null)
      jcas.throwFeatMissing("chemicalList", "de.julielab.jcore.types.pubmed.ManualDescriptor");
    ll_cas.ll_setRefValue(addr, casFeatCode_chemicalList, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getChemicalList(int addr, int i) {
        if (featOkTst && casFeat_chemicalList == null)
      jcas.throwFeatMissing("chemicalList", "de.julielab.jcore.types.pubmed.ManualDescriptor");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_chemicalList), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_chemicalList), i);
	return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_chemicalList), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setChemicalList(int addr, int i, int v) {
        if (featOkTst && casFeat_chemicalList == null)
      jcas.throwFeatMissing("chemicalList", "de.julielab.jcore.types.pubmed.ManualDescriptor");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_chemicalList), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_chemicalList), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_chemicalList), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_dBInfoList;
  /** @generated */
  final int     casFeatCode_dBInfoList;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getDBInfoList(int addr) {
        if (featOkTst && casFeat_dBInfoList == null)
      jcas.throwFeatMissing("dBInfoList", "de.julielab.jcore.types.pubmed.ManualDescriptor");
    return ll_cas.ll_getRefValue(addr, casFeatCode_dBInfoList);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setDBInfoList(int addr, int v) {
        if (featOkTst && casFeat_dBInfoList == null)
      jcas.throwFeatMissing("dBInfoList", "de.julielab.jcore.types.pubmed.ManualDescriptor");
    ll_cas.ll_setRefValue(addr, casFeatCode_dBInfoList, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getDBInfoList(int addr, int i) {
        if (featOkTst && casFeat_dBInfoList == null)
      jcas.throwFeatMissing("dBInfoList", "de.julielab.jcore.types.pubmed.ManualDescriptor");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_dBInfoList), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_dBInfoList), i);
	return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_dBInfoList), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setDBInfoList(int addr, int i, int v) {
        if (featOkTst && casFeat_dBInfoList == null)
      jcas.throwFeatMissing("dBInfoList", "de.julielab.jcore.types.pubmed.ManualDescriptor");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_dBInfoList), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_dBInfoList), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_dBInfoList), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_keywordList;
  /** @generated */
  final int     casFeatCode_keywordList;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getKeywordList(int addr) {
        if (featOkTst && casFeat_keywordList == null)
      jcas.throwFeatMissing("keywordList", "de.julielab.jcore.types.pubmed.ManualDescriptor");
    return ll_cas.ll_getRefValue(addr, casFeatCode_keywordList);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setKeywordList(int addr, int v) {
        if (featOkTst && casFeat_keywordList == null)
      jcas.throwFeatMissing("keywordList", "de.julielab.jcore.types.pubmed.ManualDescriptor");
    ll_cas.ll_setRefValue(addr, casFeatCode_keywordList, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getKeywordList(int addr, int i) {
        if (featOkTst && casFeat_keywordList == null)
      jcas.throwFeatMissing("keywordList", "de.julielab.jcore.types.pubmed.ManualDescriptor");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_keywordList), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_keywordList), i);
	return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_keywordList), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setKeywordList(int addr, int i, int v) {
        if (featOkTst && casFeat_keywordList == null)
      jcas.throwFeatMissing("keywordList", "de.julielab.jcore.types.pubmed.ManualDescriptor");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_keywordList), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_keywordList), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_keywordList), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_geneSymbolList;
  /** @generated */
  final int     casFeatCode_geneSymbolList;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getGeneSymbolList(int addr) {
        if (featOkTst && casFeat_geneSymbolList == null)
      jcas.throwFeatMissing("geneSymbolList", "de.julielab.jcore.types.pubmed.ManualDescriptor");
    return ll_cas.ll_getRefValue(addr, casFeatCode_geneSymbolList);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setGeneSymbolList(int addr, int v) {
        if (featOkTst && casFeat_geneSymbolList == null)
      jcas.throwFeatMissing("geneSymbolList", "de.julielab.jcore.types.pubmed.ManualDescriptor");
    ll_cas.ll_setRefValue(addr, casFeatCode_geneSymbolList, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public String getGeneSymbolList(int addr, int i) {
        if (featOkTst && casFeat_geneSymbolList == null)
      jcas.throwFeatMissing("geneSymbolList", "de.julielab.jcore.types.pubmed.ManualDescriptor");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_geneSymbolList), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_geneSymbolList), i);
	return ll_cas.ll_getStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_geneSymbolList), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setGeneSymbolList(int addr, int i, String v) {
        if (featOkTst && casFeat_geneSymbolList == null)
      jcas.throwFeatMissing("geneSymbolList", "de.julielab.jcore.types.pubmed.ManualDescriptor");
    if (lowLevelTypeChecks)
      ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_geneSymbolList), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_geneSymbolList), i);
    ll_cas.ll_setStringArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_geneSymbolList), i, v);
  }
 



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public ManualDescriptor_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_meSHList = jcas.getRequiredFeatureDE(casType, "meSHList", "uima.cas.FSArray", featOkTst);
    casFeatCode_meSHList  = (null == casFeat_meSHList) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_meSHList).getCode();

 
    casFeat_chemicalList = jcas.getRequiredFeatureDE(casType, "chemicalList", "uima.cas.FSArray", featOkTst);
    casFeatCode_chemicalList  = (null == casFeat_chemicalList) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_chemicalList).getCode();

 
    casFeat_dBInfoList = jcas.getRequiredFeatureDE(casType, "dBInfoList", "uima.cas.FSArray", featOkTst);
    casFeatCode_dBInfoList  = (null == casFeat_dBInfoList) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_dBInfoList).getCode();

 
    casFeat_keywordList = jcas.getRequiredFeatureDE(casType, "keywordList", "uima.cas.FSArray", featOkTst);
    casFeatCode_keywordList  = (null == casFeat_keywordList) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_keywordList).getCode();

 
    casFeat_geneSymbolList = jcas.getRequiredFeatureDE(casType, "geneSymbolList", "uima.cas.StringArray", featOkTst);
    casFeatCode_geneSymbolList  = (null == casFeat_geneSymbolList) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_geneSymbolList).getCode();

  }
}



    