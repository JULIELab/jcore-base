
/* First created by JCasGen Tue Mar 20 16:26:43 CET 2018 */
package de.julielab.jcore.types.pubmed;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;

/** Internal references with a special feature for PMC related reference types.
 * Updated by JCasGen Wed Mar 21 13:45:55 CET 2018
 * @generated */
public class InternalReference_Type extends de.julielab.jcore.types.InternalReference_Type {
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = InternalReference.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("de.julielab.jcore.types.pubmed.InternalReference");



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public InternalReference_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

  }
}



    