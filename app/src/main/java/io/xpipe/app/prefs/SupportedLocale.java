package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Locale;

@AllArgsConstructor
@Getter
public enum SupportedLocale implements PrefsChoiceValue {
    ENGLISH(Locale.ENGLISH, "english");
    // GERMAN(Locale.GERMAN, "german");

    private final Locale locale;
    private final String id;

    @Override
    public String toTranslatedString() {
        return locale.getDisplayName();
    }

    @Override
    public String getId() {
        return id;
    }
}
