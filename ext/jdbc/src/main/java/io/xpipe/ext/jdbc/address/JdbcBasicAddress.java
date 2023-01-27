package io.xpipe.ext.jdbc.address;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.extension.util.DataStoreFormatter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("basicAddress")
@SuperBuilder
@Jacksonized
@Getter
@AllArgsConstructor
public class JdbcBasicAddress implements JdbcAddress {

    private final String hostname;

    private final Integer port;

    @Override
    public String toAddressString() {
        return hostname + (port != null ? ":" + port : "");
    }

    @Override
    public String toDisplayString() {
        if (hostname == null) {
            return null;
        }

        return DataStoreFormatter.formatHostName(hostname + ":" + port, Integer.MAX_VALUE);
    }
}
