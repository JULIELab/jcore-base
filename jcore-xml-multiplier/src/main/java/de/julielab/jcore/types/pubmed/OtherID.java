

/* First created by JCasGen Sun Mar 18 12:36:58 CET 2018 */
package de.julielab.jcore.types.pubmed;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import de.julielab.jcore.types.Annotation;


/** PubMed/Medline abstracts sometimes have other IDs besided their PMID from different sources. This type discloses the respective ID and source. For details see https://www.nlm.nih.gov/bsd/mms/medlineelements.html#oid
 * Updated by JCasGen Wed Mar 21 14:47:03 CET 2018
 * XML source: C:/Users/Philipp/jcore-xml-multiplier/src/test/resources/FileTypeSystemDescriptor.xml
 * @generated */
public class OtherID extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(OtherID.class);
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
  protected OtherID() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public OtherID(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public OtherID(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public OtherID(JCas jcas, int begin, int end) {
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
  //* Feature: id

  /** getter for id - gets The "other" ID of the document (e.g. Pubmed Central).
   * @generated
   * @return value of the feature 
   */
  public String getId() {
    if (OtherID_Type.featOkTst && ((OtherID_Type)jcasType).casFeat_id == null)
      jcasType.jcas.throwFeatMissing("id", "de.julielab.jcore.types.pubmed.OtherID");
    return jcasType.ll_cas.ll_getStringValue(addr, ((OtherID_Type)jcasType).casFeatCode_id);}
    
  /** setter for id - sets The "other" ID of the document (e.g. Pubmed Central). 
   * @generated
   * @param v value to set into the feature 
   */
  public void setId(String v) {
    if (OtherID_Type.featOkTst && ((OtherID_Type)jcasType).casFeat_id == null)
      jcasType.jcas.throwFeatMissing("id", "de.julielab.jcore.types.pubmed.OtherID");
    jcasType.ll_cas.ll_setStringValue(addr, ((OtherID_Type)jcasType).casFeatCode_id, v);}    
   
    
  //*--------------*
  //* Feature: source

  /** getter for source - gets The source that assigned the ID found at the 'id' feature to this document.
   * @generated
   * @return value of the feature 
   */
  public String getSource() {
    if (OtherID_Type.featOkTst && ((OtherID_Type)jcasType).casFeat_source == null)
      jcasType.jcas.throwFeatMissing("source", "de.julielab.jcore.types.pubmed.OtherID");
    return jcasType.ll_cas.ll_getStringValue(addr, ((OtherID_Type)jcasType).casFeatCode_source);}
    
  /** setter for source - sets The source that assigned the ID found at the 'id' feature to this document. 
   * @generated
   * @param v value to set into the feature 
   */
  public void setSource(String v) {
    if (OtherID_Type.featOkTst && ((OtherID_Type)jcasType).casFeat_source == null)
      jcasType.jcas.throwFeatMissing("source", "de.julielab.jcore.types.pubmed.OtherID");
    jcasType.ll_cas.ll_setStringValue(addr, ((OtherID_Type)jcasType).casFeatCode_source, v);}    
  }

    