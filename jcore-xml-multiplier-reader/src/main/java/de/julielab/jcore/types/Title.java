

/* First created by JCasGen Thu Mar 22 17:37:33 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** Title annotates titles covering various text units, including the whole paper, sections and subsections.
 * Updated by JCasGen Thu Mar 22 17:37:33 CET 2018
 * XML source: C:/Users/Philipp/workspace4/jcore-xml-multiplier-reader/src/test/resources/FileTypeSystemDescriptor.xml
 * @generated */
public class Title extends Zone {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Title.class);
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
  protected Title() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Title(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Title(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Title(JCas jcas, int begin, int end) {
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
  //* Feature: titleType

  /** getter for titleType - gets The type of the title:
table: title of a table
figure: title of a figure
caption: title of a caption
footnote: title of a footnote
   * @generated
   * @return value of the feature 
   */
  public String getTitleType() {
    if (Title_Type.featOkTst && ((Title_Type)jcasType).casFeat_titleType == null)
      jcasType.jcas.throwFeatMissing("titleType", "de.julielab.jcore.types.Title");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Title_Type)jcasType).casFeatCode_titleType);}
    
  /** setter for titleType - sets The type of the title:
table: title of a table
figure: title of a figure
caption: title of a caption
footnote: title of a footnote 
   * @generated
   * @param v value to set into the feature 
   */
  public void setTitleType(String v) {
    if (Title_Type.featOkTst && ((Title_Type)jcasType).casFeat_titleType == null)
      jcasType.jcas.throwFeatMissing("titleType", "de.julielab.jcore.types.Title");
    jcasType.ll_cas.ll_setStringValue(addr, ((Title_Type)jcasType).casFeatCode_titleType, v);}    
  }

    