

/* First created by JCasGen Thu Mar 22 17:37:32 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** The descriptor type for the manually added information.
 * Updated by JCasGen Thu Mar 22 17:37:32 CET 2018
 * XML source: C:/Users/Philipp/workspace4/jcore-xml-multiplier-reader/src/test/resources/FileTypeSystemDescriptor.xml
 * @generated */
public class ManualDescriptor extends Descriptor {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(ManualDescriptor.class);
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
  protected ManualDescriptor() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public ManualDescriptor(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public ManualDescriptor(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public ManualDescriptor(JCas jcas, int begin, int end) {
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
  //* Feature: metaInfo

  /** getter for metaInfo - gets meta information about thos document, for example who is the annotator and which semantic types are annotated in this document
   * @generated
   * @return value of the feature 
   */
  public String getMetaInfo() {
    if (ManualDescriptor_Type.featOkTst && ((ManualDescriptor_Type)jcasType).casFeat_metaInfo == null)
      jcasType.jcas.throwFeatMissing("metaInfo", "de.julielab.jcore.types.ManualDescriptor");
    return jcasType.ll_cas.ll_getStringValue(addr, ((ManualDescriptor_Type)jcasType).casFeatCode_metaInfo);}
    
  /** setter for metaInfo - sets meta information about thos document, for example who is the annotator and which semantic types are annotated in this document 
   * @generated
   * @param v value to set into the feature 
   */
  public void setMetaInfo(String v) {
    if (ManualDescriptor_Type.featOkTst && ((ManualDescriptor_Type)jcasType).casFeat_metaInfo == null)
      jcasType.jcas.throwFeatMissing("metaInfo", "de.julielab.jcore.types.ManualDescriptor");
    jcasType.ll_cas.ll_setStringValue(addr, ((ManualDescriptor_Type)jcasType).casFeatCode_metaInfo, v);}    
  }

    