package de.julielab.jcore.consumer.xmi;

import de.julielab.xml.XmiSplitterResult;
import org.apache.uima.cas.TypeSystem;

import java.util.Map;

/**
 * <p>This class primarily holds the complete XMI data of one CAS. Additionally, it contains the JeDIS sofa mapping,
 * current maximum XMI ID and possibly more meta information needed to create the annotation modules.</p>
 */
public class XmiBufferItem {
    private byte[] xmiData;
    private DocumentId docId;
    private Map<String, Integer> sofaIdMap;
    private int nextXmiId;
    private TypeSystem typeSystem;
    private XmiSplitterResult splitterResult;
    private boolean isProcessedForBinaryMappings;

    public XmiBufferItem(XmiSplitterResult splitterResult, DocumentId docId, Map<String, Integer> baseDocumentSofaIdMap, int nextXmiId, TypeSystem typeSystem) {

        this.splitterResult = splitterResult;
        this.docId = docId;

        this.sofaIdMap = baseDocumentSofaIdMap;
        this.nextXmiId = nextXmiId;
        this.typeSystem = typeSystem;
        this.isProcessedForBinaryMappings = false;
    }

    public XmiBufferItem(byte[] xmiData, DocumentId docId, Map<String, Integer> sofaIdMap, int nextXmiId, TypeSystem typeSystem) {

        this.xmiData = xmiData;
        this.docId = docId;
        this.sofaIdMap = sofaIdMap;
        this.nextXmiId = nextXmiId;
        this.typeSystem = typeSystem;
    }

    public XmiSplitterResult getSplitterResult() {
        return splitterResult;
    }

    public boolean isProcessedForBinaryMappings() {
        return isProcessedForBinaryMappings;
    }

    public TypeSystem getTypeSystem() {
        return typeSystem;
    }

    public DocumentId getDocId() {
        return docId;
    }

    public byte[] getXmiData() {
        return xmiData;
    }

    public Map<String, Integer> getSofaIdMap() {
        return sofaIdMap;
    }

    public int getNextXmiId() {
        return nextXmiId;
    }

    public void setProcessedForBinaryMappings(boolean processedForBinaryMappings) {
        isProcessedForBinaryMappings = processedForBinaryMappings;
    }
}
