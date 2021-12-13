package io.xpipe.core.source;

import com.fasterxml.jackson.annotation.JsonCreator;

public class DataSourceId {

    public static final char SEPARATOR = ':';

    public static DataSourceId create(String collectionName, String entryName) {
        if (collectionName == null) {
            throw new IllegalArgumentException("Collection name is null");
        }
        if (collectionName.contains("" + SEPARATOR)) {
            throw new IllegalArgumentException("Separator character " + SEPARATOR + " is not allowed in the collection name");
        }

        if (entryName == null) {
            throw new IllegalArgumentException("Collection name is null");
        }
        if (entryName.contains("" + SEPARATOR)) {
            throw new IllegalArgumentException("Separator character " + SEPARATOR + " is not allowed in the entry name");
        }

        return new DataSourceId(collectionName, entryName);
    }

    private final String collectionName;
    private final String entryName;

    @JsonCreator
    private DataSourceId(String collectionName, String entryName) {
        this.collectionName = collectionName;
        this.entryName = entryName;
    }

    public static DataSourceId fromString(String s) {
        if (s == null) {
            throw new IllegalArgumentException("String is null");
        }

        var split = s.split(String.valueOf(SEPARATOR));
        if (split.length != 2) {
            throw new IllegalArgumentException("Data source id must contain exactly one " + SEPARATOR);
        }

        if (split[0].length() == 0) {
            throw new IllegalArgumentException("Collection name must not be empty");
        }
        if (split[1].length() == 0) {
            throw new IllegalArgumentException("Entry name must not be empty");
        }

        return new DataSourceId(split[0], split[1]);
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
