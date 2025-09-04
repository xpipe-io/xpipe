package io.xpipe.app.secret;

import io.xpipe.app.util.AskpassAlert;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Value;

import java.time.Duration;

@JsonTypeName("prompt")
@Value
public class SecretPromptStrategy implements SecretRetrievalStrategy {

    @Override
    public SecretQuery query() {
        return new SecretQuery() {
            @Override
            public SecretQueryResult query(String prompt) {
                return AskpassAlert.queryRaw(prompt, null, true);
            }

            @Override
            public Duration cacheDuration() {
                return null;
            }

            @Override
            public boolean retryOnFail() {
                return true;
            }

            @Override
            public boolean requiresUserInteraction() {
                return true;
            }
        };
    }
}
