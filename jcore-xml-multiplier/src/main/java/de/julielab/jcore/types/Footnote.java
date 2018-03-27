

/* First created by JCasGen Wed Mar 21 14:47:02 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** Footnotes of all kinds, i.e. footnotes found in running text or in tables etc.
 * Updated by JCasGen Wed Mar 21 14:47:02 CET 2018
 * XML source: C:/Users/Philipp/jcore-xml-multiplier/src/test/resources/FileTypeSystemDescriptor.xml
 * @generated */
public class Footnote extends Zone {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Footnote.class);
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
  protected Footnote() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Footnote(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Footnote(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Footnote(JCas jcas, int begin, int end) {
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
  //* Feature: footnoteTitle

  /** getter for footnoteTitle - gets 
   * @generated
   * @return value of the feature 
   */
  public Title getFootnoteTitle() {
    if (Footnote_Type.featOkTst && ((Footnote_Type)jcasType).casFeat_footnoteTitle == null)
      jcasType.jcas.throwFeatMissing("footnoteTitle", "de.julielab.jcore.types.Footnote");
    return (Title)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Footnote_Type)jcasType).casFeatCode_footnoteTitle)));}
    
  /** setter for footnoteTitle - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setFootnoteTitle(Title v) {
    if (Footnote_Type.featOkTst && ((Footnote_Type)jcasType).casFeat_footnoteTitle == null)
      jcasType.jcas.throwFeatMissing("footnoteTitle", "de.julielab.jcore.types.Footnote");
    jcasType.ll_cas.ll_setRefValue(addr, ((Footnote_Type)jcasType).casFeatCode_footnoteTitle, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    