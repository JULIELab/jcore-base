
/* First created by JCasGen Thu Mar 22 17:37:32 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** item of a list
 * Updated by JCasGen Thu Mar 22 17:37:32 CET 2018
 * @generated */
public class ListItem_Type extends Zone_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = ListItem.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.types.ListItem");
 
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
      jcas.throwFeatMissing("itemList", "de.julielab.jcore.types.ListItem");
    return ll_cas.ll_getRefValue(addr, casFeatCode_itemList);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setItemList(int addr, int v) {
        if (featOkTst && casFeat_itemList == null)
      jcas.throwFeatMissing("itemList", "de.julielab.jcore.types.ListItem");
    ll_cas.ll_setRefValue(addr, casFeatCode_itemList, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getItemList(int addr, int i) {
        if (featOkTst && casFeat_itemList == null)
      jcas.throwFeatMissing("itemList", "de.julielab.jcore.types.ListItem");
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
      jcas.throwFeatMissing("itemList", "de.julielab.jcore.types.ListItem");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_itemList), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_itemList), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_itemList), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_level;
  /** @generated */
  final int     casFeatCode_level;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getLevel(int addr) {
        if (featOkTst && casFeat_level == null)
      jcas.throwFeatMissing("level", "de.julielab.jcore.types.ListItem");
    return ll_cas.ll_getIntValue(addr, casFeatCode_level);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setLevel(int addr, int v) {
        if (featOkTst && casFeat_level == null)
      jcas.throwFeatMissing("level", "de.julielab.jcore.types.ListItem");
    ll_cas.ll_setIntValue(addr, casFeatCode_level, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public ListItem_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_itemList = jcas.getRequiredFeatureDE(casType, "itemList", "uima.cas.FSArray", featOkTst);
    casFeatCode_itemList  = (null == casFeat_itemList) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_itemList).getCode();

 
    casFeat_level = jcas.getRequiredFeatureDE(casType, "level", "uima.cas.Integer", featOkTst);
    casFeatCode_level  = (null == casFeat_level) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_level).getCode();

  }
}



    