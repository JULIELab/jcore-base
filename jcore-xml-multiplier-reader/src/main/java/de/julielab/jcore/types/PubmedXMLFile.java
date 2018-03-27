

/* First created by JCasGen Thu Mar 22 17:37:32 CET 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** Type for Pubmed XML files that contain multiple medline citations.
 * Updated by JCasGen Thu Mar 22 17:37:32 CET 2018
 * XML source: C:/Users/Philipp/workspace4/jcore-xml-multiplier-reader/src/test/resources/FileTypeSystemDescriptor.xml
 * @generated */
public class PubmedXMLFile extends DocumentAnnotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(PubmedXMLFile.class);
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
  protected PubmedXMLFile() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public PubmedXMLFile(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public PubmedXMLFile(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public PubmedXMLFile(JCas jcas, int begin, int end) {
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
    if (PubmedXMLFile_Type.featOkTst && ((PubmedXMLFile_Type)jcasType).casFeat_fileToRead == null)
      jcasType.jcas.throwFeatMissing("fileToRead", "de.julielab.jcore.types.PubmedXMLFile");
    return jcasType.ll_cas.ll_getStringValue(addr, ((PubmedXMLFile_Type)jcasType).casFeatCode_fileToRead);}
    
  /** setter for fileToRead - sets Filename of the Pubmed XML file 
   * @generated
   * @param v value to set into the feature 
   */
  public void setFileToRead(String v) {
    if (PubmedXMLFile_Type.featOkTst && ((PubmedXMLFile_Type)jcasType).casFeat_fileToRead == null)
      jcasType.jcas.throwFeatMissing("fileToRead", "de.julielab.jcore.types.PubmedXMLFile");
    jcasType.ll_cas.ll_setStringValue(addr, ((PubmedXMLFile_Type)jcasType).casFeatCode_fileToRead, v);}    
  }

    