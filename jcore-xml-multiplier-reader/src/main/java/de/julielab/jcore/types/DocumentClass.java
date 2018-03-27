

/* First created by JCasGen Thu Mar 22 17:37:32 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** A document class specification for the CAS' document text.
 * Updated by JCasGen Thu Mar 22 17:37:32 CET 2018
 * XML source: C:/Users/Philipp/workspace4/jcore-xml-multiplier-reader/src/test/resources/FileTypeSystemDescriptor.xml
 * @generated */
public class DocumentClass extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(DocumentClass.class);
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
  protected DocumentClass() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public DocumentClass(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public DocumentClass(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public DocumentClass(JCas jcas, int begin, int end) {
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
  //* Feature: classname

  /** getter for classname - gets The name of the document class this CAS has been classified to.
   * @generated
   * @return value of the feature 
   */
  public String getClassname() {
    if (DocumentClass_Type.featOkTst && ((DocumentClass_Type)jcasType).casFeat_classname == null)
      jcasType.jcas.throwFeatMissing("classname", "de.julielab.jcore.types.DocumentClass");
    return jcasType.ll_cas.ll_getStringValue(addr, ((DocumentClass_Type)jcasType).casFeatCode_classname);}
    
  /** setter for classname - sets The name of the document class this CAS has been classified to. 
   * @generated
   * @param v value to set into the feature 
   */
  public void setClassname(String v) {
    if (DocumentClass_Type.featOkTst && ((DocumentClass_Type)jcasType).casFeat_classname == null)
      jcasType.jcas.throwFeatMissing("classname", "de.julielab.jcore.types.DocumentClass");
    jcasType.ll_cas.ll_setStringValue(addr, ((DocumentClass_Type)jcasType).casFeatCode_classname, v);}    
   
    
  //*--------------*
  //* Feature: confidence

  /** getter for confidence - gets Confidence value of the classification into this class.
   * @generated
   * @return value of the feature 
   */
  public double getConfidence() {
    if (DocumentClass_Type.featOkTst && ((DocumentClass_Type)jcasType).casFeat_confidence == null)
      jcasType.jcas.throwFeatMissing("confidence", "de.julielab.jcore.types.DocumentClass");
    return jcasType.ll_cas.ll_getDoubleValue(addr, ((DocumentClass_Type)jcasType).casFeatCode_confidence);}
    
  /** setter for confidence - sets Confidence value of the classification into this class. 
   * @generated
   * @param v value to set into the feature 
   */
  public void setConfidence(double v) {
    if (DocumentClass_Type.featOkTst && ((DocumentClass_Type)jcasType).casFeat_confidence == null)
      jcasType.jcas.throwFeatMissing("confidence", "de.julielab.jcore.types.DocumentClass");
    jcasType.ll_cas.ll_setDoubleValue(addr, ((DocumentClass_Type)jcasType).casFeatCode_confidence, v);}    
  }

    