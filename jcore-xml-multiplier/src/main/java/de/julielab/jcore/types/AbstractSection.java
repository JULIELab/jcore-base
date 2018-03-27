

/* First created by JCasGen Wed Mar 21 14:47:02 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Wed Mar 21 14:47:02 CET 2018
 * XML source: C:/Users/Philipp/jcore-xml-multiplier/src/test/resources/FileTypeSystemDescriptor.xml
 * @generated */
public class AbstractSection extends Zone {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(AbstractSection.class);
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
  protected AbstractSection() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public AbstractSection(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public AbstractSection(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public AbstractSection(JCas jcas, int begin, int end) {
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
  //* Feature: abstractSectionHeading

  /** getter for abstractSectionHeading - gets The title of a part of a structured abstract, e.g. "Background", "Methods", "Results", ...
   * @generated
   * @return value of the feature 
   */
  public Title getAbstractSectionHeading() {
    if (AbstractSection_Type.featOkTst && ((AbstractSection_Type)jcasType).casFeat_abstractSectionHeading == null)
      jcasType.jcas.throwFeatMissing("abstractSectionHeading", "de.julielab.jcore.types.AbstractSection");
    return (Title)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((AbstractSection_Type)jcasType).casFeatCode_abstractSectionHeading)));}
    
  /** setter for abstractSectionHeading - sets The title of a part of a structured abstract, e.g. "Background", "Methods", "Results", ... 
   * @generated
   * @param v value to set into the feature 
   */
  public void setAbstractSectionHeading(Title v) {
    if (AbstractSection_Type.featOkTst && ((AbstractSection_Type)jcasType).casFeat_abstractSectionHeading == null)
      jcasType.jcas.throwFeatMissing("abstractSectionHeading", "de.julielab.jcore.types.AbstractSection");
    jcasType.ll_cas.ll_setRefValue(addr, ((AbstractSection_Type)jcasType).casFeatCode_abstractSectionHeading, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    