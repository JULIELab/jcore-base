/**
BSD 2-Clause License

Copyright (c) 2017, JULIE Lab
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/
package de.julielab.jcore.ae.lingpipegazetteer.utils;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

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



    