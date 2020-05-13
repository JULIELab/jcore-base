
/* First created by JCasGen Tue Sep 04 09:17:47 MDT 2007 */
package de.julielab.jcore.ae.lingpipegazetteer.utils;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/** 
 * Updated by JCasGen Tue Sep 04 09:17:47 MDT 2007
 * @generated */
public class SplitAnnotation_Type extends SimpleAnnotation_Type {
  /** @generated */
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (SplitAnnotation_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = SplitAnnotation_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new SplitAnnotation(addr, SplitAnnotation_Type.this);
  			   SplitAnnotation_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new SplitAnnotation(addr, SplitAnnotation_Type.this);
  	  }
    };
  /** @generated */
  public final static int typeIndexID = SplitAnnotation.typeIndexID;
  /** @generated 
     @modifiable */
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.colorado.cleartk.types.SplitAnnotation");
 
  /** @generated */
  final Feature casFeat_annotations;
  /** @generated */
  final int     casFeatCode_annotations;
  /** @generated */ 
  public int getAnnotations(int addr) {
        if (featOkTst && casFeat_annotations == null)
      jcas.throwFeatMissing("annotations", "edu.colorado.cleartk.types.SplitAnnotation");
    return ll_cas.ll_getRefValue(addr, casFeatCode_annotations);
  }
  /** @generated */    
  public void setAnnotations(int addr, int v) {
        if (featOkTst && casFeat_annotations == null)
      jcas.throwFeatMissing("annotations", "edu.colorado.cleartk.types.SplitAnnotation");
    ll_cas.ll_setRefValue(addr, casFeatCode_annotations, v);}
    
   /** @generated */
  public int getAnnotations(int addr, int i) {
        if (featOkTst && casFeat_annotations == null)
      jcas.throwFeatMissing("annotations", "edu.colorado.cleartk.types.SplitAnnotation");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_annotations), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_annotations), i);
	return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_annotations), i);
  }
   
  /** @generated */ 
  public void setAnnotations(int addr, int i, int v) {
        if (featOkTst && casFeat_annotations == null)
      jcas.throwFeatMissing("annotations", "edu.colorado.cleartk.types.SplitAnnotation");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_annotations), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_annotations), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_annotations), i, v);
  }
 



  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public SplitAnnotation_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_annotations = jcas.getRequiredFeatureDE(casType, "annotations", "uima.cas.FSArray", featOkTst);
    casFeatCode_annotations  = (null == casFeat_annotations) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_annotations).getCode();

  }
}



    