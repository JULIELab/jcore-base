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
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSArray;


/** 
 * Updated by JCasGen Tue Sep 04 09:17:47 MDT 2007
 * XML source: C:/Documents and Settings/Philip/My Documents/CSLR/workspace/ClearTK/desc/TypeSystem.xml
 * @generated */
public class SplitAnnotation extends SimpleAnnotation {
  /** @generated
   * @ordered 
   */
  public final static int typeIndexID = JCasRegistry.register(SplitAnnotation.class);
  /** @generated
   * @ordered 
   */
  public final static int type = typeIndexID;
  /** @generated  */
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected SplitAnnotation() {}
    
  /** Internal - constructor used by generator 
   * @generated */
  public SplitAnnotation(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public SplitAnnotation(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public SplitAnnotation(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** <!-- begin-user-doc -->
    * Write your own initialization here
    * <!-- end-user-doc -->
  @generated modifiable */
  private void readObject() {}
     
 
    
  //*--------------*
  //* Feature: annotations

  /** getter for annotations - gets 
   * @generated */
  public FSArray getAnnotations() {
    if (SplitAnnotation_Type.featOkTst && ((SplitAnnotation_Type)jcasType).casFeat_annotations == null)
      jcasType.jcas.throwFeatMissing("annotations", "edu.colorado.cleartk.types.SplitAnnotation");
    return (FSArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((SplitAnnotation_Type)jcasType).casFeatCode_annotations)));}
    
  /** setter for annotations - sets  
   * @generated */
  public void setAnnotations(FSArray v) {
    if (SplitAnnotation_Type.featOkTst && ((SplitAnnotation_Type)jcasType).casFeat_annotations == null)
      jcasType.jcas.throwFeatMissing("annotations", "edu.colorado.cleartk.types.SplitAnnotation");
    jcasType.ll_cas.ll_setRefValue(addr, ((SplitAnnotation_Type)jcasType).casFeatCode_annotations, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for annotations - gets an indexed value - 
   * @generated */
  public ContiguousAnnotation getAnnotations(int i) {
    if (SplitAnnotation_Type.featOkTst && ((SplitAnnotation_Type)jcasType).casFeat_annotations == null)
      jcasType.jcas.throwFeatMissing("annotations", "edu.colorado.cleartk.types.SplitAnnotation");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((SplitAnnotation_Type)jcasType).casFeatCode_annotations), i);
    return (ContiguousAnnotation)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((SplitAnnotation_Type)jcasType).casFeatCode_annotations), i)));}

  /** indexed setter for annotations - sets an indexed value - 
   * @generated */
  public void setAnnotations(int i, ContiguousAnnotation v) { 
    if (SplitAnnotation_Type.featOkTst && ((SplitAnnotation_Type)jcasType).casFeat_annotations == null)
      jcasType.jcas.throwFeatMissing("annotations", "edu.colorado.cleartk.types.SplitAnnotation");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((SplitAnnotation_Type)jcasType).casFeatCode_annotations), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((SplitAnnotation_Type)jcasType).casFeatCode_annotations), i, jcasType.ll_cas.ll_getFSRef(v));}
  }

    