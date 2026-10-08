package io.xpipe.app.secret;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.pwman.PasswordManager;
import io.xpipe.app.pwman.PasswordManagerTestComp;
import io.xpipe.app.util.ValidationException;
import io.xpipe.app.util.Validators;

import javafx.beans.property.Property;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.nio.CharBuffer;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@JsonTypeName("passwordManager")
@Builder
@Jacksonized
@Value
public class SecretPasswordManagerStrategy implements SecretRetrievalStrategy {

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(
            Property<SecretPasswordManagerStrategy> p, SecretStrategyChoiceConfig config) {
        var options = new OptionsBuilder();
        var prefs = AppPrefs.get();
        var keyProperty = options.map(p, SecretPasswordManagerStrategy::getKey);
        var field = new PasswordManagerTestComp(keyProperty, false, true, true);
        return options.nameAndDescription("passwordManagerPasswordKey")
                .addComp(field, keyProperty)
                .nonNull()
                .bind(
                        () -> {
                            return new SecretPasswordManagerStrategy(keyProperty.getValue());
                        },
                        p);
    }

    String key;

    @Override
    public void checkComplete() throws ValidationException {
        Validators.nonNull(key);
    }

    @Override
    public SecretQuery query() {
        return new SecretQuery() {
            @Override
            public SecretQueryResult query(String prompt, boolean forceFocus) {
                var pm = AppPrefs.get().passwordManager().getValue();
                if (key == null || pm == null) {
                    ErrorEventFactory.fromMessage(
                                    "A password manager was requested but no password manager has been set in the settings menu")
                            .expected()
                            .handle();
                    return new SecretQueryResult(null, SecretQueryState.RETRIEVAL_FAILURE);
                }

                PasswordManager.Result r;
                try {
                    r = pm.query(key);
                } catch (Exception ex) {
                    ErrorEventFactory.fromThrowable(ex).handle();
                    return new SecretQueryResult(null, SecretQueryState.RETRIEVAL_FAILURE);
                }

                if (r == null
                        || r.getCredentials() == null
                        || r.getCredentials().getPassword() == null) {
                    return new SecretQueryResult(null, SecretQueryState.RETRIEVAL_FAILURE);
                }

                var valid = new AtomicBoolean(true);
                r.getCredentials().getPassword().withSecretValue(chars -> {
                    var seq = CharBuffer.wrap(chars);
                    var newline = seq.chars().anyMatch(value -> value == 10);
                    if (seq.length() == 0 || newline) {
                        valid.set(false);
                    }
                });
                if (!valid.get()) {
                    ErrorEventFactory.fromMessage("Received not exactly one output line:\n" + r
                                    + "\n\n"
                                    + "XPipe requires your password manager command to output only the raw password."
                                    + " If the output includes any formatting, messages, or your password key either matched multiple entries or "
                                    + "none,"
                                    + " you will have to change the command and/or password key.")
                            .expected()
                            .handle();
                    return new SecretQueryResult(null, SecretQueryState.RETRIEVAL_FAILURE);
                }

                return new SecretQueryResult(r.getCredentials().getPassword(), SecretQueryState.NORMAL);
            }

            @Override
            public Duration cacheDuration() {
                // To reduce password manager access, cache it
                var pm = AppPrefs.get().passwordManager().getValue();
                return pm != null ? pm.getCacheDuration() : Duration.ofSeconds(30);
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
