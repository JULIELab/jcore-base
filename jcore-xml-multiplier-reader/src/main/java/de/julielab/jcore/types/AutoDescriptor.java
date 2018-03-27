

/* First created by JCasGen Thu Mar 22 17:37:32 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSArray;


/** The descriptor type for automatically (i.e. algorithmically) acquired meta information. It can be refined and extended.
 * Updated by JCasGen Thu Mar 22 17:37:32 CET 2018
 * XML source: C:/Users/Philipp/workspace4/jcore-xml-multiplier-reader/src/test/resources/FileTypeSystemDescriptor.xml
 * @generated */
public class AutoDescriptor extends Descriptor {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(AutoDescriptor.class);
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
  protected AutoDescriptor() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public AutoDescriptor(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public AutoDescriptor(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public AutoDescriptor(JCas jcas, int begin, int end) {
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
  //* Feature: documentClasses

  /** getter for documentClasses - gets Here the document classification result will be stored, for example for storing infos if the document is age-related or not.
   * @generated
   * @return value of the feature 
   */
  public FSArray getDocumentClasses() {
    if (AutoDescriptor_Type.featOkTst && ((AutoDescriptor_Type)jcasType).casFeat_documentClasses == null)
      jcasType.jcas.throwFeatMissing("documentClasses", "de.julielab.jcore.types.AutoDescriptor");
    return (FSArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AutoDescriptor_Type)jcasType).casFeatCode_documentClasses)));}
    
  /** setter for documentClasses - sets Here the document classification result will be stored, for example for storing infos if the document is age-related or not. 
   * @generated
   * @param v value to set into the feature 
   */
  public void setDocumentClasses(FSArray v) {
    if (AutoDescriptor_Type.featOkTst && ((AutoDescriptor_Type)jcasType).casFeat_documentClasses == null)
      jcasType.jcas.throwFeatMissing("documentClasses", "de.julielab.jcore.types.AutoDescriptor");
    jcasType.ll_cas.ll_setRefValue(addr, ((AutoDescriptor_Type)jcasType).casFeatCode_documentClasses, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for documentClasses - gets an indexed value - Here the document classification result will be stored, for example for storing infos if the document is age-related or not.
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public DocumentClass getDocumentClasses(int i) {
    if (AutoDescriptor_Type.featOkTst && ((AutoDescriptor_Type)jcasType).casFeat_documentClasses == null)
      jcasType.jcas.throwFeatMissing("documentClasses", "de.julielab.jcore.types.AutoDescriptor");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((AutoDescriptor_Type)jcasType).casFeatCode_documentClasses), i);
    return (DocumentClass)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((AutoDescriptor_Type)jcasType).casFeatCode_documentClasses), i)));}

  /** indexed setter for documentClasses - sets an indexed value - Here the document classification result will be stored, for example for storing infos if the document is age-related or not.
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setDocumentClasses(int i, DocumentClass v) { 
    if (AutoDescriptor_Type.featOkTst && ((AutoDescriptor_Type)jcasType).casFeat_documentClasses == null)
      jcasType.jcas.throwFeatMissing("documentClasses", "de.julielab.jcore.types.AutoDescriptor");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((AutoDescriptor_Type)jcasType).casFeatCode_documentClasses), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((AutoDescriptor_Type)jcasType).casFeatCode_documentClasses), i, jcasType.ll_cas.ll_getFSRef(v));}
  }

    