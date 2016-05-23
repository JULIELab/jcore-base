package de.julielab.jcore.reader.dta.mapping

public abstract class MappingService{
    static final String CLASIFICATION = "http://www.deutschestextarchiv.de/doku/klassifikation#";
    static final AbstractMapper[] mappers = new AbstractMapper[]{new DTAMapper(), new DWDS1Mapper(), new DWDS2Mapper()};
  
  public static DocumentClassification[] getClassifications(final JCas jcas,
            final String xmlFileName,
            final Map<String, String[]> classInfo){
      ArrayList<? extends DocumentClassification> classificationList = new ArrayList<>();
      for(AbstractMapper : mappers){
          DocumentClassification classification = getClassification(jcas, xmlFileName, classInfo);
          if(classification != null)
            classifications.add(classification);
      }
      if(classificationList.isEmpty())
        return null;
      return classificationList.asArray(new DocumentClassification[classificationList.size()]);
  }
  
  static void getClassification(final JCas jcas,
            final String xmlFileName,
            final Map<String, String[]> classInfo
        )
            throws NoSuchMethodException, SecurityException,
            InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        if (classInfo.containsKey(mainClassification)) {
            if (classInfo.get(mainClassification).length != 1)
                throw new IllegalArgumentException(
                        "More than 1 " + mainClassification
                                + " classification in " + xmlFileName);
            if (classInfo.get(subClassification) == null)
                throw new IllegalArgumentException(
                        "No " + subClassification + " in " + xmlFileName);
            if (classInfo.get(subClassification).length != 1)
                throw new IllegalArgumentException(
                        "More than 1 " + subClassification
                                + " classification in " + xmlFileName);
            final String mainClass = classInfo.get(mainClassification)[0];
            final String subClass = classInfo.get(subClassification)[0];

            Class<? extends DocumentClassification> aClass = classification2class
                    .get(mainClass);
            if (aClass == null) {
                if (defaultClass == null)
                    throw new IllegalArgumentException(
                            mainClass + " not supported in " + xmlFileName);
                else {
                    aClass = defaultClass;
                }
            }
            final Constructor<? extends DocumentClassification> constructor = aClass
                    .getConstructor(new Class[] { JCas.class });
            final DocumentClassification classification = constructor
                    .newInstance(jcas);
            classification.setClassification(mainClass);
            classification.setSubClassification(subClass);
            classification.addToIndexes();
            return classification;
        }
        return null;
    }
}
