package io.xpipe.ext.jdbcx.mssql;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.ext.jdbc.address.JdbcBasicAddress;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("mssqlInstance")
@SuperBuilder
@Jacksonized
@Getter
public class MssqlAddress extends JdbcBasicAddress {

    String instance;

    @Override
    public String toAddressString() {
        return getHostname() + (instance != null ? "\\" + instance : "") + (getPort() != null ? ":" + getPort() : "");
    }
}
