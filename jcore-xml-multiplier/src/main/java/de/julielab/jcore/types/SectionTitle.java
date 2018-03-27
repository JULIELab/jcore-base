

/* First created by JCasGen Wed Mar 21 14:47:03 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** Title of a text section in contrast to the title of the whole document.
 * Updated by JCasGen Wed Mar 21 14:47:03 CET 2018
 * XML source: C:/Users/Philipp/jcore-xml-multiplier/src/test/resources/FileTypeSystemDescriptor.xml
 * @generated */
public class SectionTitle extends Title {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(SectionTitle.class);
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
  protected SectionTitle() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public SectionTitle(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public SectionTitle(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public SectionTitle(JCas jcas, int begin, int end) {
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
  //* Feature: depth

  /** getter for depth - gets depth of the section, e.g. 0 -> section, 1 -> subsection, 2 -> sub-subsection, ...
   * @generated
   * @return value of the feature 
   */
  public int getDepth() {
    if (SectionTitle_Type.featOkTst && ((SectionTitle_Type)jcasType).casFeat_depth == null)
      jcasType.jcas.throwFeatMissing("depth", "de.julielab.jcore.types.SectionTitle");
    return jcasType.ll_cas.ll_getIntValue(addr, ((SectionTitle_Type)jcasType).casFeatCode_depth);}
    
  /** setter for depth - sets depth of the section, e.g. 0 -> section, 1 -> subsection, 2 -> sub-subsection, ... 
   * @generated
   * @param v value to set into the feature 
   */
  public void setDepth(int v) {
    if (SectionTitle_Type.featOkTst && ((SectionTitle_Type)jcasType).casFeat_depth == null)
      jcasType.jcas.throwFeatMissing("depth", "de.julielab.jcore.types.SectionTitle");
    jcasType.ll_cas.ll_setIntValue(addr, ((SectionTitle_Type)jcasType).casFeatCode_depth, v);}    
  }

    