package de.julielab.jcore.reader.db.jmx;

public class DBReaderInfo implements  DBReaderInfoMBean{
    private String currentDocumentId;
    private String componentId;

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public void setCurrentDocumentId(String currentDocumentId) {
        this.currentDocumentId = currentDocumentId;
    }

    @Override
    public String getCurrentDocumentId() {
        return currentDocumentId;
    }

    @Override
    public String getComponentId() {
        return componentId;
    }
}
