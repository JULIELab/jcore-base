

/* First created by JCasGen Mon Feb 05 09:56:22 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** An abstract type which should be used to store information on the publication. See subtypes Journal and an accumulative type (OtherPub)
 * Updated by JCasGen Mon Feb 05 09:56:22 CET 2018
 * XML source: /Volumes/OUTERSPACE/Coding/git/jcore-base/jcore-elasticsearch-consumer/src/test/resources/de/julielab/jcore/consumer/es/testTypes.xml
 * @generated */
public class PubType extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(PubType.class);
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
  protected PubType() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public PubType(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public PubType(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public PubType(JCas jcas, int begin, int end) {
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
  //* Feature: name

  /** getter for name - gets The name of the publication type (e.g. journal, technical report, book).
   * @generated
   * @return value of the feature 
   */
  public String getName() {
    if (PubType_Type.featOkTst && ((PubType_Type)jcasType).casFeat_name == null)
      jcasType.jcas.throwFeatMissing("name", "de.julielab.jcore.types.PubType");
    return jcasType.ll_cas.ll_getStringValue(addr, ((PubType_Type)jcasType).casFeatCode_name);}
    
  /** setter for name - sets The name of the publication type (e.g. journal, technical report, book). 
   * @generated
   * @param v value to set into the feature 
   */
  public void setName(String v) {
    if (PubType_Type.featOkTst && ((PubType_Type)jcasType).casFeat_name == null)
      jcasType.jcas.throwFeatMissing("name", "de.julielab.jcore.types.PubType");
    jcasType.ll_cas.ll_setStringValue(addr, ((PubType_Type)jcasType).casFeatCode_name, v);}    
   
    
  //*--------------*
  //* Feature: pubDate

  /** getter for pubDate - gets The date on which the document was published.
   * @generated
   * @return value of the feature 
   */
  public Date getPubDate() {
    if (PubType_Type.featOkTst && ((PubType_Type)jcasType).casFeat_pubDate == null)
      jcasType.jcas.throwFeatMissing("pubDate", "de.julielab.jcore.types.PubType");
    return (Date)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((PubType_Type)jcasType).casFeatCode_pubDate)));}
    
  /** setter for pubDate - sets The date on which the document was published. 
   * @generated
   * @param v value to set into the feature 
   */
  public void setPubDate(Date v) {
    if (PubType_Type.featOkTst && ((PubType_Type)jcasType).casFeat_pubDate == null)
      jcasType.jcas.throwFeatMissing("pubDate", "de.julielab.jcore.types.PubType");
    jcasType.ll_cas.ll_setRefValue(addr, ((PubType_Type)jcasType).casFeatCode_pubDate, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    