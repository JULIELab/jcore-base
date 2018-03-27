

/* First created by JCasGen Thu Mar 22 17:37:32 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSArray;


/** Annotation of the complete abstract.
 * Updated by JCasGen Thu Mar 22 17:37:32 CET 2018
 * XML source: C:/Users/Philipp/workspace4/jcore-xml-multiplier-reader/src/test/resources/FileTypeSystemDescriptor.xml
 * @generated */
public class AbstractText extends Zone {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(AbstractText.class);
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
  protected AbstractText() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public AbstractText(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public AbstractText(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public AbstractText(JCas jcas, int begin, int end) {
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
  //* Feature: structuredAbstractParts

  /** getter for structuredAbstractParts - gets A List of all parts of a structured abstract. Empty, if the abstract consists of only one part and has no titles.
   * @generated
   * @return value of the feature 
   */
  public FSArray getStructuredAbstractParts() {
    if (AbstractText_Type.featOkTst && ((AbstractText_Type)jcasType).casFeat_structuredAbstractParts == null)
      jcasType.jcas.throwFeatMissing("structuredAbstractParts", "de.julielab.jcore.types.AbstractText");
    return (FSArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AbstractText_Type)jcasType).casFeatCode_structuredAbstractParts)));}
    
  /** setter for structuredAbstractParts - sets A List of all parts of a structured abstract. Empty, if the abstract consists of only one part and has no titles. 
   * @generated
   * @param v value to set into the feature 
   */
  public void setStructuredAbstractParts(FSArray v) {
    if (AbstractText_Type.featOkTst && ((AbstractText_Type)jcasType).casFeat_structuredAbstractParts == null)
      jcasType.jcas.throwFeatMissing("structuredAbstractParts", "de.julielab.jcore.types.AbstractText");
    jcasType.ll_cas.ll_setRefValue(addr, ((AbstractText_Type)jcasType).casFeatCode_structuredAbstractParts, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for structuredAbstractParts - gets an indexed value - A List of all parts of a structured abstract. Empty, if the abstract consists of only one part and has no titles.
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public AbstractSection getStructuredAbstractParts(int i) {
    if (AbstractText_Type.featOkTst && ((AbstractText_Type)jcasType).casFeat_structuredAbstractParts == null)
      jcasType.jcas.throwFeatMissing("structuredAbstractParts", "de.julielab.jcore.types.AbstractText");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((AbstractText_Type)jcasType).casFeatCode_structuredAbstractParts), i);
    return (AbstractSection)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((AbstractText_Type)jcasType).casFeatCode_structuredAbstractParts), i)));}

  /** indexed setter for structuredAbstractParts - sets an indexed value - A List of all parts of a structured abstract. Empty, if the abstract consists of only one part and has no titles.
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setStructuredAbstractParts(int i, AbstractSection v) { 
    if (AbstractText_Type.featOkTst && ((AbstractText_Type)jcasType).casFeat_structuredAbstractParts == null)
      jcasType.jcas.throwFeatMissing("structuredAbstractParts", "de.julielab.jcore.types.AbstractText");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((AbstractText_Type)jcasType).casFeatCode_structuredAbstractParts), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((AbstractText_Type)jcasType).casFeatCode_structuredAbstractParts), i, jcasType.ll_cas.ll_getFSRef(v));}
  }

    