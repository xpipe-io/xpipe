package io.xpipe.core.source;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the fundamental type of a data source.
 * This distinction is necessary as the general workflow differs for each type.
 */
public enum DataSourceType {

    @JsonProperty("table")
    TABLE,

    @JsonProperty("structure")
    STRUCTURE,

    @JsonProperty("text")
    TEXT,

    @JsonProperty("raw")
    RAW,

    @JsonProperty("collection")
    COLLECTION
}
