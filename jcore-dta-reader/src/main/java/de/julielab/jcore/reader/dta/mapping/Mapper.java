package de.julielab.jcore.reader.dta.mapping.Mapper

public abstract class Mapper{
    static final String CLASIFICATION = "http://www.deutschestextarchiv.de/doku/klassifikation#";
        final static String mainClassification;
        final static String subClassification;
            final static Class<? extends DocumentClassification> defaultClass;
            final static Map<String, Class<? extends DocumentClassification>> classification2class;
    
    
   public static void addClassification(final JCas jcas,
            final String xmlFileName,
            final List<DocumentClassification> classifications,
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
            classifications.add(classification);
        }
    }
}