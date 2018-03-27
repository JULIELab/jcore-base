
/* First created by JCasGen Tue Mar 27 14:21:41 CEST 2018 */
package de.julielab.jcore.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.Annotation_Type;

/** Type for Pubmed XML files that contain multiple medline citations.
 * Updated by JCasGen Tue Mar 27 14:21:41 CEST 2018
 * @generated */
public class PubmedXMLFile_Type extends Annotation_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = PubmedXMLFile.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.types.PubmedXMLFile");
 
  /** @generated */
  final Feature casFeat_fileToRead;
  /** @generated */
  final int     casFeatCode_fileToRead;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getFileToRead(int addr) {
        if (featOkTst && casFeat_fileToRead == null)
      jcas.throwFeatMissing("fileToRead", "de.julielab.jcore.types.PubmedXMLFile");
    return ll_cas.ll_getStringValue(addr, casFeatCode_fileToRead);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setFileToRead(int addr, String v) {
        if (featOkTst && casFeat_fileToRead == null)
      jcas.throwFeatMissing("fileToRead", "de.julielab.jcore.types.PubmedXMLFile");
    ll_cas.ll_setStringValue(addr, casFeatCode_fileToRead, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public PubmedXMLFile_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_fileToRead = jcas.getRequiredFeatureDE(casType, "fileToRead", "uima.cas.String", featOkTst);
    casFeatCode_fileToRead  = (null == casFeat_fileToRead) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_fileToRead).getCode();

  }
}



    