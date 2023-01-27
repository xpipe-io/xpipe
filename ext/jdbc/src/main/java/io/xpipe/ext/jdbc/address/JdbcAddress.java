package io.xpipe.ext.jdbc.address;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface JdbcAddress {

    public String toAddressString();

    public String toDisplayString();
}
