package io.xpipe.app.prefs;

import io.xpipe.app.ext.PrefsChoiceValue;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Locale;

@AllArgsConstructor
@Getter
public enum SupportedLocale implements PrefsChoiceValue {
    ENGLISH(Locale.ENGLISH, "en", false),
    GERMAN(Locale.GERMAN, "de", false),
    DUTCH(Locale.of("nl"), "nl", false),
    SPANISH(Locale.of("es"), "es", false),
    FRENCH(Locale.FRENCH, "fr", true),
    ITALIAN(Locale.ITALIAN, "it", false),
    PORTUGUESE(Locale.of("pt"), "pt", false),
    RUSSIAN(Locale.of("ru"), "ru", true),
    JAPANESE(Locale.of("ja"), "ja", false),
    CHINESE(Locale.CHINESE, "zh", true),
    DANISH(Locale.of("da"), "da", false),
    INDONESIAN(Locale.of("id"), "id", false),
    SWEDISH(Locale.of("sv"), "sv", false),
    POLISH(Locale.of("pl"), "pl", false),
    KOREAN(Locale.of("ko"), "ko", false),
    TURKISH(Locale.of("tr"), "tr", true);

    private final Locale locale;
    private final String id;
    private final boolean setDefault;

    public static SupportedLocale getInitial() {
        var s = Locale.getDefault();
        return Arrays.stream(values())
                .filter(supportedLocale -> supportedLocale.isSetDefault()
                        && supportedLocale.getLocale().getLanguage().equals(s.getLanguage()))
                .findFirst()
                .orElse(getEnglish());
    }

    public static SupportedLocale getEnglish() {
        return Arrays.stream(values())
                .filter(supportedLocale -> supportedLocale.getId().equals("en"))
                .findFirst()
                .orElseThrow();
    }

    @Override
    public ObservableValue<String> toTranslatedString() {
        return new SimpleStringProperty(locale.getDisplayName(locale));
    }

    @Override
    public String getId() {
        return id;
    }
}
