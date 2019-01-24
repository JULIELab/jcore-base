package de.julielab.jcore.ae.checkpoint;

import de.julielab.jcore.types.ext.DBProcessingMetaData;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;

public class DocumentId {
    private String[] id;

    public String[] getId() {
        return id;
    }

    public DocumentId(String... id) {
        this.id = id;
    }

    public DocumentId(DBProcessingMetaData dbProcessingMetaData) {
        if (dbProcessingMetaData == null || dbProcessingMetaData.getPrimaryKey() == null || dbProcessingMetaData.getPrimaryKey().size() == 0)
            throw new IllegalArgumentException("The DBProcessingMetaData was null or its primary key was null or it was empty.");
        id = new String[dbProcessingMetaData.getPrimaryKey().size()];
        for (int i = 0; i < dbProcessingMetaData.getPrimaryKey().size(); i++) {
            String primaryKeyElement = dbProcessingMetaData.getPrimaryKey(i);
            id[i] = primaryKeyElement;
        }
    }

    @Override
    public String toString() {
        return StringUtils.join(id, ", ");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentId that = (DocumentId) o;
        return Arrays.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(id);
    }
}
