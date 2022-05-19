package io.xpipe.core.source;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.data.type.TupleType;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.OptionalInt;

/**
 * A data source info instances contains all required
 * essential information of a specific data source type.
 *
 * This information is usually determined only once on data
 * source creation, as this process might be expensive.
 *
 * @see DataSourceType
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public abstract class DataSourceInfo {

    public abstract DataSourceType getType();

    @EqualsAndHashCode(callSuper = false)
    @Value
    @JsonTypeName("table")
    public static class Table extends DataSourceInfo {
        TupleType dataType;
        int rowCount;

        @JsonCreator
        public Table(TupleType dataType, int rowCount) {
            this.dataType = dataType;
            this.rowCount = rowCount;
        }

        public OptionalInt getRowCountIfPresent() {
            return getRowCount() != -1 ? OptionalInt.of(getRowCount()) : OptionalInt.empty();
        }

        @Override
        public DataSourceType getType() {
            return DataSourceType.TABLE;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @Value
    @JsonTypeName("structure")
    public static class Structure extends DataSourceInfo {

        int entries;

        @JsonCreator
        public Structure(int entries) {
            this.entries = entries;
        }

        @Override
        public DataSourceType getType() {
            return DataSourceType.STRUCTURE;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @Value
    @JsonTypeName("collection")
    public static class Collection extends DataSourceInfo {

        int entries;

        @JsonCreator
        public Collection(int entries) {
            this.entries = entries;
        }

        @Override
        public DataSourceType getType() {
            return DataSourceType.COLLECTION;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @Value
    @JsonTypeName("text")
    public static class Text extends DataSourceInfo {
        int lineCount;

        @JsonCreator
        public Text(int lineCount) {
            this.lineCount = lineCount;
        }

        @Override
        public DataSourceType getType() {
            return DataSourceType.TEXT;
        }
    }


    @EqualsAndHashCode(callSuper = false)
    @Value
    @JsonTypeName("raw")
    public static class Raw extends DataSourceInfo {
        int byteCount;

        @JsonCreator
        public Raw(int byteCount) {
            this.byteCount = byteCount;
        }

        @Override
        public DataSourceType getType() {
            return DataSourceType.RAW;
        }
    }

    /**
     * Casts this instance to a table info.
     */
    public Table asTable() {
        if (!getType().equals(DataSourceType.TABLE)) {
            throw new IllegalStateException("Not a table");
        }

        return (Table) this;
    }

    /**
     * Casts this instance to a structure info.
     */
    public Structure asStructure() {
        if (!getType().equals(DataSourceType.STRUCTURE)) {
            throw new IllegalStateException("Not a structure");
        }

        return (Structure) this;
    }

    /**
     * Casts this instance to a text info.
     */
    public Text asText() {
        if (!getType().equals(DataSourceType.TEXT)) {
            throw new IllegalStateException("Not a text");
        }

        return (Text) this;
    }

    /**
     * Casts this instance to a raw info.
     */
    public Raw asRaw() {
        if (!getType().equals(DataSourceType.RAW)) {
            throw new IllegalStateException("Not raw");
        }

        return (Raw) this;
    }

    /**
     * Casts this instance to a collection info.
     */
    public Collection asCollection() {
        if (!getType().equals(DataSourceType.COLLECTION)) {
            throw new IllegalStateException("Not a collection");
        }

        return (Collection) this;
    }
}
