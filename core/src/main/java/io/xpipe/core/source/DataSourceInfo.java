package io.xpipe.core.source;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.data.type.TupleType;
import lombok.EqualsAndHashCode;
import lombok.Value;

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

        @Override
        public DataSourceType getType() {
            return DataSourceType.TABLE;
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
}
