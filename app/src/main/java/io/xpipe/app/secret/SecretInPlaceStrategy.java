package io.xpipe.app.secret;

import io.xpipe.app.comp.base.SecretFieldComp;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.util.Validators;
import io.xpipe.core.InPlaceSecretValue;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

import java.time.Duration;
import java.util.Arrays;

@JsonTypeName("inPlace")
@Builder
@Value
public class SecretInPlaceStrategy implements SecretRetrievalStrategy {

    @SuppressWarnings("unused")
    public static String getOptionsNameKey(SecretStrategyChoiceConfig config) {
        return config.getPasswordKey() != null ? config.getPasswordKey() : "password";
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<SecretInPlaceStrategy> p, SecretStrategyChoiceConfig config) {
        var options = new OptionsBuilder();
        var original = p.getValue() != null ? p.getValue().getValue() : null;
        var secretProperty = options.map(p, SecretInPlaceStrategy::getValue);
        return new OptionsBuilder()
                .addComp(new SecretFieldComp(secretProperty, true), secretProperty)
                .nonNull()
                .bind(
                        () -> {
                            var newSecret = secretProperty.get();
                            var changed = !Arrays.equals(
                                    newSecret != null ? newSecret.getSecret() : new char[0],
                                    original != null ? original.getSecret() : new char[0]);
                            var val = changed ? secretProperty.getValue() : original;
                            return new SecretInPlaceStrategy(val);
                        },
                        p);
    }

    InPlaceSecretValue value;

    public SecretInPlaceStrategy(InPlaceSecretValue value) {
        this.value = value;
    }

    @Override
    public void checkComplete() throws ValidationException {
        Validators.nonNull(value);
    }

    @Override
    public SecretQuery query() {
        return new SecretQuery() {
            @Override
            public SecretQueryResult query(String prompt) {
                return new SecretQueryResult(value, SecretQueryState.NORMAL);
            }

            @Override
            public Duration cacheDuration() {
                return Duration.ZERO;
            }

            @Override
            public boolean retryOnFail() {
                return false;
            }

            @Override
            public boolean requiresUserInteraction() {
                return false;
            }
        };
    }
}
