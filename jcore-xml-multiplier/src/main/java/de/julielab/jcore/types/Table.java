

/* First created by JCasGen Wed Mar 21 14:47:03 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSArray;


/** An annotation for CAS elements that belong to a table, e.g. table title, table caption etc.
 * Updated by JCasGen Wed Mar 21 14:47:03 CET 2018
 * XML source: C:/Users/Philipp/jcore-xml-multiplier/src/test/resources/FileTypeSystemDescriptor.xml
 * @generated */
public class Table extends TextObject {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Table.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected Table() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Table(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Table(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Table(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** 
   * <!-- begin-user-doc -->
   * Write your own initialization here
   * <!-- end-user-doc -->
   *
   * @generated modifiable 
   */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: footnotes

  /** getter for footnotes - gets An array collecting all footnotes, appearing in this table
   * @generated
   * @return value of the feature 
   */
  public FSArray getFootnotes() {
    if (Table_Type.featOkTst && ((Table_Type)jcasType).casFeat_footnotes == null)
      jcasType.jcas.throwFeatMissing("footnotes", "de.julielab.jcore.types.Table");
    return (FSArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Table_Type)jcasType).casFeatCode_footnotes)));}
    
  /** setter for footnotes - sets An array collecting all footnotes, appearing in this table 
   * @generated
   * @param v value to set into the feature 
   */
  public void setFootnotes(FSArray v) {
    if (Table_Type.featOkTst && ((Table_Type)jcasType).casFeat_footnotes == null)
      jcasType.jcas.throwFeatMissing("footnotes", "de.julielab.jcore.types.Table");
    jcasType.ll_cas.ll_setRefValue(addr, ((Table_Type)jcasType).casFeatCode_footnotes, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for footnotes - gets an indexed value - An array collecting all footnotes, appearing in this table
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public Footnote getFootnotes(int i) {
    if (Table_Type.featOkTst && ((Table_Type)jcasType).casFeat_footnotes == null)
      jcasType.jcas.throwFeatMissing("footnotes", "de.julielab.jcore.types.Table");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Table_Type)jcasType).casFeatCode_footnotes), i);
    return (Footnote)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Table_Type)jcasType).casFeatCode_footnotes), i)));}

  /** indexed setter for footnotes - sets an indexed value - An array collecting all footnotes, appearing in this table
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setFootnotes(int i, Footnote v) { 
    if (Table_Type.featOkTst && ((Table_Type)jcasType).casFeat_footnotes == null)
      jcasType.jcas.throwFeatMissing("footnotes", "de.julielab.jcore.types.Table");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Table_Type)jcasType).casFeatCode_footnotes), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Table_Type)jcasType).casFeatCode_footnotes), i, jcasType.ll_cas.ll_getFSRef(v));}
  }

    