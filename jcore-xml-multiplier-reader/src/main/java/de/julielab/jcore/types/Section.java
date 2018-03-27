

/* First created by JCasGen Thu Mar 22 17:37:33 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSArray;


/** a section is a part of the text that often has a heading, an id, a section type, figures, tables, citations and footnotes that occur in this section
 * Updated by JCasGen Thu Mar 22 17:37:33 CET 2018
 * XML source: C:/Users/Philipp/workspace4/jcore-xml-multiplier-reader/src/test/resources/FileTypeSystemDescriptor.xml
 * @generated */
public class Section extends Zone {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Section.class);
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
  protected Section() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Section(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Section(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Section(JCas jcas, int begin, int end) {
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
  //* Feature: sectionHeading

  /** getter for sectionHeading - gets the title of the section
   * @generated
   * @return value of the feature 
   */
  public Title getSectionHeading() {
    if (Section_Type.featOkTst && ((Section_Type)jcasType).casFeat_sectionHeading == null)
      jcasType.jcas.throwFeatMissing("sectionHeading", "de.julielab.jcore.types.Section");
    return (Title)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Section_Type)jcasType).casFeatCode_sectionHeading)));}
    
  /** setter for sectionHeading - sets the title of the section 
   * @generated
   * @param v value to set into the feature 
   */
  public void setSectionHeading(Title v) {
    if (Section_Type.featOkTst && ((Section_Type)jcasType).casFeat_sectionHeading == null)
      jcasType.jcas.throwFeatMissing("sectionHeading", "de.julielab.jcore.types.Section");
    jcasType.ll_cas.ll_setRefValue(addr, ((Section_Type)jcasType).casFeatCode_sectionHeading, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: sectionType

  /** getter for sectionType - gets the type of the section (e.g. results)
   * @generated
   * @return value of the feature 
   */
  public String getSectionType() {
    if (Section_Type.featOkTst && ((Section_Type)jcasType).casFeat_sectionType == null)
      jcasType.jcas.throwFeatMissing("sectionType", "de.julielab.jcore.types.Section");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Section_Type)jcasType).casFeatCode_sectionType);}
    
  /** setter for sectionType - sets the type of the section (e.g. results) 
   * @generated
   * @param v value to set into the feature 
   */
  public void setSectionType(String v) {
    if (Section_Type.featOkTst && ((Section_Type)jcasType).casFeat_sectionType == null)
      jcasType.jcas.throwFeatMissing("sectionType", "de.julielab.jcore.types.Section");
    jcasType.ll_cas.ll_setStringValue(addr, ((Section_Type)jcasType).casFeatCode_sectionType, v);}    
   
    
  //*--------------*
  //* Feature: textObjects

  /** getter for textObjects - gets the text objects (figure, table, boxed text etc.) that are associated with a particular section
   * @generated
   * @return value of the feature 
   */
  public FSArray getTextObjects() {
    if (Section_Type.featOkTst && ((Section_Type)jcasType).casFeat_textObjects == null)
      jcasType.jcas.throwFeatMissing("textObjects", "de.julielab.jcore.types.Section");
    return (FSArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Section_Type)jcasType).casFeatCode_textObjects)));}
    
  /** setter for textObjects - sets the text objects (figure, table, boxed text etc.) that are associated with a particular section 
   * @generated
   * @param v value to set into the feature 
   */
  public void setTextObjects(FSArray v) {
    if (Section_Type.featOkTst && ((Section_Type)jcasType).casFeat_textObjects == null)
      jcasType.jcas.throwFeatMissing("textObjects", "de.julielab.jcore.types.Section");
    jcasType.ll_cas.ll_setRefValue(addr, ((Section_Type)jcasType).casFeatCode_textObjects, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for textObjects - gets an indexed value - the text objects (figure, table, boxed text etc.) that are associated with a particular section
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public TextObject getTextObjects(int i) {
    if (Section_Type.featOkTst && ((Section_Type)jcasType).casFeat_textObjects == null)
      jcasType.jcas.throwFeatMissing("textObjects", "de.julielab.jcore.types.Section");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Section_Type)jcasType).casFeatCode_textObjects), i);
    return (TextObject)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Section_Type)jcasType).casFeatCode_textObjects), i)));}

  /** indexed setter for textObjects - sets an indexed value - the text objects (figure, table, boxed text etc.) that are associated with a particular section
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setTextObjects(int i, TextObject v) { 
    if (Section_Type.featOkTst && ((Section_Type)jcasType).casFeat_textObjects == null)
      jcasType.jcas.throwFeatMissing("textObjects", "de.julielab.jcore.types.Section");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Section_Type)jcasType).casFeatCode_textObjects), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Section_Type)jcasType).casFeatCode_textObjects), i, jcasType.ll_cas.ll_getFSRef(v));}
   
    
  //*--------------*
  //* Feature: sectionId

  /** getter for sectionId - gets the id of the section, for example as mentioned in the original file, or level of the section
   * @generated
   * @return value of the feature 
   */
  public String getSectionId() {
    if (Section_Type.featOkTst && ((Section_Type)jcasType).casFeat_sectionId == null)
      jcasType.jcas.throwFeatMissing("sectionId", "de.julielab.jcore.types.Section");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Section_Type)jcasType).casFeatCode_sectionId);}
    
  /** setter for sectionId - sets the id of the section, for example as mentioned in the original file, or level of the section 
   * @generated
   * @param v value to set into the feature 
   */
  public void setSectionId(String v) {
    if (Section_Type.featOkTst && ((Section_Type)jcasType).casFeat_sectionId == null)
      jcasType.jcas.throwFeatMissing("sectionId", "de.julielab.jcore.types.Section");
    jcasType.ll_cas.ll_setStringValue(addr, ((Section_Type)jcasType).casFeatCode_sectionId, v);}    
   
    
  //*--------------*
  //* Feature: depth

  /** getter for depth - gets depth of the section, e.g. 0 -> section, 1 -> subsection, 2 -> sub-subsection, ...
   * @generated
   * @return value of the feature 
   */
  public int getDepth() {
    if (Section_Type.featOkTst && ((Section_Type)jcasType).casFeat_depth == null)
      jcasType.jcas.throwFeatMissing("depth", "de.julielab.jcore.types.Section");
    return jcasType.ll_cas.ll_getIntValue(addr, ((Section_Type)jcasType).casFeatCode_depth);}
    
  /** setter for depth - sets depth of the section, e.g. 0 -> section, 1 -> subsection, 2 -> sub-subsection, ... 
   * @generated
   * @param v value to set into the feature 
   */
  public void setDepth(int v) {
    if (Section_Type.featOkTst && ((Section_Type)jcasType).casFeat_depth == null)
      jcasType.jcas.throwFeatMissing("depth", "de.julielab.jcore.types.Section");
    jcasType.ll_cas.ll_setIntValue(addr, ((Section_Type)jcasType).casFeatCode_depth, v);}    
   
    
  //*--------------*
  //* Feature: label

  /** getter for label - gets The section label, if given. This might, for example, just be the section number.
   * @generated
   * @return value of the feature 
   */
  public String getLabel() {
    if (Section_Type.featOkTst && ((Section_Type)jcasType).casFeat_label == null)
      jcasType.jcas.throwFeatMissing("label", "de.julielab.jcore.types.Section");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Section_Type)jcasType).casFeatCode_label);}
    
  /** setter for label - sets The section label, if given. This might, for example, just be the section number. 
   * @generated
   * @param v value to set into the feature 
   */
  public void setLabel(String v) {
    if (Section_Type.featOkTst && ((Section_Type)jcasType).casFeat_label == null)
      jcasType.jcas.throwFeatMissing("label", "de.julielab.jcore.types.Section");
    jcasType.ll_cas.ll_setStringValue(addr, ((Section_Type)jcasType).casFeatCode_label, v);}    
  }

    