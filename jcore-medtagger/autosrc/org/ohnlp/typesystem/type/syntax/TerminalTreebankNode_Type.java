
/* First created by JCasGen Tue Sep 24 19:27:48 CDT 2013 */
package org.ohnlp.typesystem.type.syntax;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** A Penn Treebank Node; as a terminal, there is an associated word, and the index of the word is a feature.
 * Updated by JCasGen Wed Oct 30 16:30:49 CDT 2013
 * @generated */
public class TerminalTreebankNode_Type extends TreebankNode_Type {
  /** @generated */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (TerminalTreebankNode_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = TerminalTreebankNode_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new TerminalTreebankNode(addr, TerminalTreebankNode_Type.this);
  			   TerminalTreebankNode_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new TerminalTreebankNode(addr, TerminalTreebankNode_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = TerminalTreebankNode.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("org.ohnlp.typesystem.type.syntax.TerminalTreebankNode");
 
  /** @generated */
  final Feature casFeat_index;
  /** @generated */
  final int     casFeatCode_index;
  /** @generated */ 
  public int getIndex(int addr) {
        if (featOkTst && casFeat_index == null)
      jcas.throwFeatMissing("index", "org.ohnlp.typesystem.type.syntax.TerminalTreebankNode");
    return ll_cas.ll_getIntValue(addr, casFeatCode_index);
  }
  /** @generated */    
  public void setIndex(int addr, int v) {
        if (featOkTst && casFeat_index == null)
      jcas.throwFeatMissing("index", "org.ohnlp.typesystem.type.syntax.TerminalTreebankNode");
    ll_cas.ll_setIntValue(addr, casFeatCode_index, v);}
    
  
 
  /** @generated */
  final Feature casFeat_tokenIndex;
  /** @generated */
  final int     casFeatCode_tokenIndex;
  /** @generated */ 
  public int getTokenIndex(int addr) {
        if (featOkTst && casFeat_tokenIndex == null)
      jcas.throwFeatMissing("tokenIndex", "org.ohnlp.typesystem.type.syntax.TerminalTreebankNode");
    return ll_cas.ll_getIntValue(addr, casFeatCode_tokenIndex);
  }
  /** @generated */    
  public void setTokenIndex(int addr, int v) {
        if (featOkTst && casFeat_tokenIndex == null)
      jcas.throwFeatMissing("tokenIndex", "org.ohnlp.typesystem.type.syntax.TerminalTreebankNode");
    ll_cas.ll_setIntValue(addr, casFeatCode_tokenIndex, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public TerminalTreebankNode_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_index = jcas.getRequiredFeatureDE(casType, "index", "uima.cas.Integer", featOkTst);
    casFeatCode_index  = (null == casFeat_index) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_index).getCode();

 
    casFeat_tokenIndex = jcas.getRequiredFeatureDE(casType, "tokenIndex", "uima.cas.Integer", featOkTst);
    casFeatCode_tokenIndex  = (null == casFeat_tokenIndex) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_tokenIndex).getCode();

  }
}



    