package io.xpipe.core.source;

import com.fasterxml.jackson.annotation.JsonCreator;

public class DataSourceId {

    public static final char SEPARATOR = ':';

    private final String collectionName;
    private final String entryName;

    @JsonCreator
    public DataSourceId(String collectionName, String entryName) {
        this.collectionName = collectionName;
        this.entryName = entryName;
    }

    public DataSourceId withEntryName(String newName) {
        return new DataSourceId(collectionName, newName);
    }

    public static DataSourceId fromString(String s) {
        var split = s.split(String.valueOf(SEPARATOR));
        if (split.length != 2) {
            throw new IllegalArgumentException();
        }

        if (split[1].length() == 0) {
            throw new IllegalArgumentException();
        }

        return new DataSourceId(split[0].length() > 0 ? split[0] : null, split[1]);
    }

    public boolean hasCollection() {
        return collectionName != null;
    }

    @Override
    public String toString() {
        return (collectionName != null ? collectionName : "") + SEPARATOR + entryName;
    }

    public String toReferenceValue() {
        return toString().toLowerCase();
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String getEntryName() {
        return entryName;
    }
}
