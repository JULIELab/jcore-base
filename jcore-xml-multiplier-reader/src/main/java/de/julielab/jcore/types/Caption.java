

/* First created by JCasGen Thu Mar 22 17:37:32 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** the caption of figures, tables etc.

feature captionTitle is a Title-Annotation of the title of the caption, if existent.

feature captionType is an Enumeration, stating to what type of entity the caption belongs, e.g. figure or table
 * Updated by JCasGen Thu Mar 22 17:37:32 CET 2018
 * XML source: C:/Users/Philipp/workspace4/jcore-xml-multiplier-reader/src/test/resources/FileTypeSystemDescriptor.xml
 * @generated */
public class Caption extends Zone {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Caption.class);
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
  protected Caption() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Caption(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Caption(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Caption(JCas jcas, int begin, int end) {
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
  //* Feature: captionTitle

  /** getter for captionTitle - gets The title of a figure / table caption, if it exists
   * @generated
   * @return value of the feature 
   */
  public Title getCaptionTitle() {
    if (Caption_Type.featOkTst && ((Caption_Type)jcasType).casFeat_captionTitle == null)
      jcasType.jcas.throwFeatMissing("captionTitle", "de.julielab.jcore.types.Caption");
    return (Title)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Caption_Type)jcasType).casFeatCode_captionTitle)));}
    
  /** setter for captionTitle - sets The title of a figure / table caption, if it exists 
   * @generated
   * @param v value to set into the feature 
   */
  public void setCaptionTitle(Title v) {
    if (Caption_Type.featOkTst && ((Caption_Type)jcasType).casFeat_captionTitle == null)
      jcasType.jcas.throwFeatMissing("captionTitle", "de.julielab.jcore.types.Caption");
    jcasType.ll_cas.ll_setRefValue(addr, ((Caption_Type)jcasType).casFeatCode_captionTitle, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: captionType

  /** getter for captionType - gets The type of entity, this caption belongs to, e.g. figure or table
   * @generated
   * @return value of the feature 
   */
  public String getCaptionType() {
    if (Caption_Type.featOkTst && ((Caption_Type)jcasType).casFeat_captionType == null)
      jcasType.jcas.throwFeatMissing("captionType", "de.julielab.jcore.types.Caption");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Caption_Type)jcasType).casFeatCode_captionType);}
    
  /** setter for captionType - sets The type of entity, this caption belongs to, e.g. figure or table 
   * @generated
   * @param v value to set into the feature 
   */
  public void setCaptionType(String v) {
    if (Caption_Type.featOkTst && ((Caption_Type)jcasType).casFeat_captionType == null)
      jcasType.jcas.throwFeatMissing("captionType", "de.julielab.jcore.types.Caption");
    jcasType.ll_cas.ll_setStringValue(addr, ((Caption_Type)jcasType).casFeatCode_captionType, v);}    
  }

    