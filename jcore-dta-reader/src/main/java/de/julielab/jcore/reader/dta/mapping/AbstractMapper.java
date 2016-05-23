package de.julielab.jcore.reader.dta.mapping

public abstract class AbstractMapper{
    private static final String CLASIFICATION = "http://www.deutschestextarchiv.de/doku/klassifikation#";
    final static String mainClassification;
    final static String subClassification;
    final static Map<String, Class<? extends DocumentClassification>> classification2class;
    final static Class<? extends DocumentClassification> defaultClass;
            
    Mapper(String mainClassification, String subClassification, Map<String, Class<? extends DocumentClassification>> classification2class){
        this(mainClassification, subClassification, classification2class, null)
    }
    
    Mapper(String mainClassification, String subClassification, Map<String, Class<? extends DocumentClassification>> classification2class, Class<? extends DocumentClassification> defaultClass){
        this.mainClassification = MappingService.CLASIFICATION + mainClassification;
        this.subClassification = MappingService.CLASIFICATION + subClassification;
        this.classification2class = classification2class;
        this.defaultClass = defaultClass;
    }
}
