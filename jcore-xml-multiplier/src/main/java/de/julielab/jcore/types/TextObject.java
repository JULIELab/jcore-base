

/* First created by JCasGen Wed Mar 21 14:47:03 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** Object, on our case, are annotations such as figures, tables, boxed text etc.
 * Updated by JCasGen Wed Mar 21 14:47:03 CET 2018
 * XML source: C:/Users/Philipp/jcore-xml-multiplier/src/test/resources/FileTypeSystemDescriptor.xml
 * @generated */
public class TextObject extends Zone {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(TextObject.class);
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
  protected TextObject() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public TextObject(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public TextObject(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public TextObject(JCas jcas, int begin, int end) {
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
  //* Feature: objectType

  /** getter for objectType - gets such as figure, table, boxed-text etc.
   * @generated
   * @return value of the feature 
   */
  public String getObjectType() {
    if (TextObject_Type.featOkTst && ((TextObject_Type)jcasType).casFeat_objectType == null)
      jcasType.jcas.throwFeatMissing("objectType", "de.julielab.jcore.types.TextObject");
    return jcasType.ll_cas.ll_getStringValue(addr, ((TextObject_Type)jcasType).casFeatCode_objectType);}
    
  /** setter for objectType - sets such as figure, table, boxed-text etc. 
   * @generated
   * @param v value to set into the feature 
   */
  public void setObjectType(String v) {
    if (TextObject_Type.featOkTst && ((TextObject_Type)jcasType).casFeat_objectType == null)
      jcasType.jcas.throwFeatMissing("objectType", "de.julielab.jcore.types.TextObject");
    jcasType.ll_cas.ll_setStringValue(addr, ((TextObject_Type)jcasType).casFeatCode_objectType, v);}    
   
    
  //*--------------*
  //* Feature: objectId

  /** getter for objectId - gets the id of the object as found in the text
   * @generated
   * @return value of the feature 
   */
  public String getObjectId() {
    if (TextObject_Type.featOkTst && ((TextObject_Type)jcasType).casFeat_objectId == null)
      jcasType.jcas.throwFeatMissing("objectId", "de.julielab.jcore.types.TextObject");
    return jcasType.ll_cas.ll_getStringValue(addr, ((TextObject_Type)jcasType).casFeatCode_objectId);}
    
  /** setter for objectId - sets the id of the object as found in the text 
   * @generated
   * @param v value to set into the feature 
   */
  public void setObjectId(String v) {
    if (TextObject_Type.featOkTst && ((TextObject_Type)jcasType).casFeat_objectId == null)
      jcasType.jcas.throwFeatMissing("objectId", "de.julielab.jcore.types.TextObject");
    jcasType.ll_cas.ll_setStringValue(addr, ((TextObject_Type)jcasType).casFeatCode_objectId, v);}    
   
    
  //*--------------*
  //* Feature: objectLabel

  /** getter for objectLabel - gets the label of an object
   * @generated
   * @return value of the feature 
   */
  public String getObjectLabel() {
    if (TextObject_Type.featOkTst && ((TextObject_Type)jcasType).casFeat_objectLabel == null)
      jcasType.jcas.throwFeatMissing("objectLabel", "de.julielab.jcore.types.TextObject");
    return jcasType.ll_cas.ll_getStringValue(addr, ((TextObject_Type)jcasType).casFeatCode_objectLabel);}
    
  /** setter for objectLabel - sets the label of an object 
   * @generated
   * @param v value to set into the feature 
   */
  public void setObjectLabel(String v) {
    if (TextObject_Type.featOkTst && ((TextObject_Type)jcasType).casFeat_objectLabel == null)
      jcasType.jcas.throwFeatMissing("objectLabel", "de.julielab.jcore.types.TextObject");
    jcasType.ll_cas.ll_setStringValue(addr, ((TextObject_Type)jcasType).casFeatCode_objectLabel, v);}    
   
    
  //*--------------*
  //* Feature: objectCaption

  /** getter for objectCaption - gets the caption that comes with the object
   * @generated
   * @return value of the feature 
   */
  public Caption getObjectCaption() {
    if (TextObject_Type.featOkTst && ((TextObject_Type)jcasType).casFeat_objectCaption == null)
      jcasType.jcas.throwFeatMissing("objectCaption", "de.julielab.jcore.types.TextObject");
    return (Caption)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((TextObject_Type)jcasType).casFeatCode_objectCaption)));}
    
  /** setter for objectCaption - sets the caption that comes with the object 
   * @generated
   * @param v value to set into the feature 
   */
  public void setObjectCaption(Caption v) {
    if (TextObject_Type.featOkTst && ((TextObject_Type)jcasType).casFeat_objectCaption == null)
      jcasType.jcas.throwFeatMissing("objectCaption", "de.julielab.jcore.types.TextObject");
    jcasType.ll_cas.ll_setRefValue(addr, ((TextObject_Type)jcasType).casFeatCode_objectCaption, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: objectTitle

  /** getter for objectTitle - gets The title annotation of the text object, if it exists. The title might correspond to the objectLabel (which is of type String and thus no annotation on its own).
   * @generated
   * @return value of the feature 
   */
  public Title getObjectTitle() {
    if (TextObject_Type.featOkTst && ((TextObject_Type)jcasType).casFeat_objectTitle == null)
      jcasType.jcas.throwFeatMissing("objectTitle", "de.julielab.jcore.types.TextObject");
    return (Title)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((TextObject_Type)jcasType).casFeatCode_objectTitle)));}
    
  /** setter for objectTitle - sets The title annotation of the text object, if it exists. The title might correspond to the objectLabel (which is of type String and thus no annotation on its own). 
   * @generated
   * @param v value to set into the feature 
   */
  public void setObjectTitle(Title v) {
    if (TextObject_Type.featOkTst && ((TextObject_Type)jcasType).casFeat_objectTitle == null)
      jcasType.jcas.throwFeatMissing("objectTitle", "de.julielab.jcore.types.TextObject");
    jcasType.ll_cas.ll_setRefValue(addr, ((TextObject_Type)jcasType).casFeatCode_objectTitle, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    