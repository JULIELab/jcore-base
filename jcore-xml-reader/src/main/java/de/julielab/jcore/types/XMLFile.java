

/* First created by JCasGen Tue Mar 27 14:21:41 CEST 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.tcas.Annotation;


/** Type for Pubmed XML files that contain multiple medline citations.
 * Updated by JCasGen Tue Mar 27 14:21:41 CEST 2018
 * XML source: C:/jcore-base-fork/jcore-xml-multiplier/src/test/resources/MultiplierTypeSystem.xml
 * @generated */
public class XMLFile extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(XMLFile.class);
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
  protected XMLFile() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public XMLFile(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public XMLFile(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public XMLFile(JCas jcas, int begin, int end) {
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
  //* Feature: fileToRead

  /** getter for fileToRead - gets Filename of the Pubmed XML file
   * @generated
   * @return value of the feature 
   */
  public String getFileToRead() {
    if (XMLFile_Type.featOkTst && ((XMLFile_Type)jcasType).casFeat_fileToRead == null)
      jcasType.jcas.throwFeatMissing("fileToRead", "de.julielab.jcore.types.XMLFile");
    return jcasType.ll_cas.ll_getStringValue(addr, ((XMLFile_Type)jcasType).casFeatCode_fileToRead);}
    
  /** setter for fileToRead - sets Filename of the Pubmed XML file 
   * @generated
   * @param v value to set into the feature 
   */
  public void setFileToRead(String v) {
    if (XMLFile_Type.featOkTst && ((XMLFile_Type)jcasType).casFeat_fileToRead == null)
      jcasType.jcas.throwFeatMissing("fileToRead", "de.julielab.jcore.types.XMLFile");
    jcasType.ll_cas.ll_setStringValue(addr, ((XMLFile_Type)jcasType).casFeatCode_fileToRead, v);}
  }

    