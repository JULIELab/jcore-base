package de.julielab.jcore.consumer.es;

import de.julielab.jcore.consumer.es.preanalyzed.Document;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;

public class DocumentTextFieldGenerator extends FieldGenerator {
    public DocumentTextFieldGenerator(FilterRegistry filterRegistry) {
        super(filterRegistry);
    }

    @Override
    public Document addFields(JCas aJCas, Document doc) throws CASException, FieldGenerationException {
        doc.addField("documentText",aJCas.getDocumentText() );
        return doc;
    }
}
