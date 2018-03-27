
/* First created by JCasGen Wed Mar 21 14:47:03 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** An annotation for CAS elements that belong to a table, e.g. table title, table caption etc.
 * Updated by JCasGen Wed Mar 21 14:47:03 CET 2018
 * @generated */
public class Table_Type extends TextObject_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Table.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.types.Table");
 
  /** @generated */
  final Feature casFeat_footnotes;
  /** @generated */
  final int     casFeatCode_footnotes;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getFootnotes(int addr) {
        if (featOkTst && casFeat_footnotes == null)
      jcas.throwFeatMissing("footnotes", "de.julielab.jcore.types.Table");
    return ll_cas.ll_getRefValue(addr, casFeatCode_footnotes);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setFootnotes(int addr, int v) {
        if (featOkTst && casFeat_footnotes == null)
      jcas.throwFeatMissing("footnotes", "de.julielab.jcore.types.Table");
    ll_cas.ll_setRefValue(addr, casFeatCode_footnotes, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getFootnotes(int addr, int i) {
        if (featOkTst && casFeat_footnotes == null)
      jcas.throwFeatMissing("footnotes", "de.julielab.jcore.types.Table");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_footnotes), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_footnotes), i);
	return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_footnotes), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setFootnotes(int addr, int i, int v) {
        if (featOkTst && casFeat_footnotes == null)
      jcas.throwFeatMissing("footnotes", "de.julielab.jcore.types.Table");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_footnotes), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_footnotes), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_footnotes), i, v);
  }
 



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Table_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_footnotes = jcas.getRequiredFeatureDE(casType, "footnotes", "uima.cas.FSArray", featOkTst);
    casFeatCode_footnotes  = (null == casFeat_footnotes) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_footnotes).getCode();

  }
}



    