package io.xpipe.app.secret;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Value;

@JsonTypeName("none")
@Value
public class SecretNoneStrategy implements SecretRetrievalStrategy {

    @Override
    public SecretQuery query() {
        return null;
    }

    public boolean expectsQuery() {
        return false;
    }
}
