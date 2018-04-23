package de.julielab.jcore.reader.xmi;

import de.julielab.xml.XmiBuilder;

public interface Initializable {
    void setStoreMaxXmiId(boolean storeMaxXmiId);
    void setXercesAttributeBufferSize(int size);
    void setReadsBaseDocument(boolean readsBaseDocument);
    void setNumAdditionalTables(int numAdditionalTables);
    void setNumDataRetrievedDataFields(int numDataRetrievedDataFields);
    void setBuilder(XmiBuilder builder);
    void setLogFinalXmi(boolean logFinalXmi);

    String[] getAdditionalTableNames();
    String[] getTables();
}
