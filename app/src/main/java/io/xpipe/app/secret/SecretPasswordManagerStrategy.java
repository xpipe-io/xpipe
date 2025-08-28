package io.xpipe.app.secret;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.HorizontalComp;
import io.xpipe.app.comp.base.SecretFieldComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.App;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.Validators;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.kordamp.ikonli.javafx.FontIcon;

import java.nio.CharBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@JsonTypeName("passwordManager")
@Builder
@Jacksonized
@Value
public class SecretPasswordManagerStrategy implements SecretRetrievalStrategy {

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(
            Property<SecretPasswordManagerStrategy> p, SecretStrategyChoiceConfig config) {
        var prefs = AppPrefs.get();
        var keyProperty =
                new SimpleObjectProperty<>(p.getValue() != null ? p.getValue().getKey() : null);
        var content = new HorizontalComp(List.of(
                new TextFieldComp(keyProperty)
                        .apply(struc -> struc.get()
                                .promptTextProperty()
                                .bind(Bindings.createStringBinding(
                                        () -> {
                                            return prefs.passwordManager()
                                                    .getValue()
                                                    != null
                                                    ? prefs.passwordManager()
                                                    .getValue()
                                                    .getKeyPlaceholder()
                                                    : "?";
                                        },
                                        prefs.passwordManager())))
                        .hgrow(),
                new ButtonComp(null, new FontIcon("mdomz-settings"), () -> {
                    AppPrefs.get().selectCategory("passwordManager");
                    App.getApp().getStage().requestFocus();
                })
                        .grow(false, true)))
                .apply(struc -> struc.get().setSpacing(10))
                .apply(struc -> struc.get().focusedProperty().addListener((c, o, n) -> {
                    if (n) {
                        struc.get().getChildren().getFirst().requestFocus();
                    }
                }));
        return new OptionsBuilder()
                .nameAndDescription("passwordManagerKey")
                .addComp(content, keyProperty)
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
            public SecretQueryResult query(String prompt) {
                var pm = AppPrefs.get().passwordManager().getValue();
                if (pm == null) {
                    ErrorEventFactory.fromMessage("A password manager was requested but no password manager has been set in the settings menu")
                            .expected()
                            .handle();
                    return new SecretQueryResult(null, SecretQueryState.RETRIEVAL_FAILURE);
                }

                var r = pm.retrieveCredentials(key);
                if (r == null || r.getPassword() == null) {
                    return new SecretQueryResult(null, SecretQueryState.RETRIEVAL_FAILURE);
                }

                r.getPassword().withSecretValue(chars -> {
                    var seq = CharBuffer.wrap(chars);
                    var newline = seq.chars().anyMatch(value -> value == 10);
                    if (seq.length() == 0 || newline) {
                        throw ErrorEventFactory.expected(new IllegalArgumentException("Received not exactly one output line:\n" +
                                r +
                                "\n\n" +
                                "XPipe requires your password manager command to output only the raw password." +
                                " If the output includes any formatting, messages, or your password key either matched multiple entries or " +
                                "none," +
                                " you will have to change the command and/or password key."));
                    }
                });
                return new SecretQueryResult(r.getPassword(), SecretQueryState.NORMAL);
            }

            @Override
            public Duration cacheDuration() {
                // To reduce password manager access, cache it for a few seconds
                return Duration.ofSeconds(10);
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
