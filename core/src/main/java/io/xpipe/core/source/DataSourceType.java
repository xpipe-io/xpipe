package io.xpipe.core.source;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum DataSourceType {

    @JsonProperty("table")
    TABLE,

    @JsonProperty("structure")
    STRUCTURE,

    @JsonProperty("raw")
    RAW
}
