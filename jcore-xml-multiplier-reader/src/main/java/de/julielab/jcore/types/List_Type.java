
/* First created by JCasGen Thu Mar 22 17:37:32 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** used for annotation of lists
 * Updated by JCasGen Thu Mar 22 17:37:32 CET 2018
 * @generated */
public class List_Type extends Zone_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = List.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.types.List");
 
  /** @generated */
  final Feature casFeat_itemList;
  /** @generated */
  final int     casFeatCode_itemList;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getItemList(int addr) {
        if (featOkTst && casFeat_itemList == null)
      jcas.throwFeatMissing("itemList", "de.julielab.jcore.types.List");
    return ll_cas.ll_getRefValue(addr, casFeatCode_itemList);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setItemList(int addr, int v) {
        if (featOkTst && casFeat_itemList == null)
      jcas.throwFeatMissing("itemList", "de.julielab.jcore.types.List");
    ll_cas.ll_setRefValue(addr, casFeatCode_itemList, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getItemList(int addr, int i) {
        if (featOkTst && casFeat_itemList == null)
      jcas.throwFeatMissing("itemList", "de.julielab.jcore.types.List");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_itemList), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_itemList), i);
	return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_itemList), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setItemList(int addr, int i, int v) {
        if (featOkTst && casFeat_itemList == null)
      jcas.throwFeatMissing("itemList", "de.julielab.jcore.types.List");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_itemList), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_itemList), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_itemList), i, v);
  }
 



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public List_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_itemList = jcas.getRequiredFeatureDE(casType, "itemList", "uima.cas.FSArray", featOkTst);
    casFeatCode_itemList  = (null == casFeat_itemList) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_itemList).getCode();

  }
}



    