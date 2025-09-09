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
    ENGLISH(Locale.ENGLISH, "en"),
    GERMAN(Locale.GERMAN, "de"),
    DUTCH(Locale.of("nl"), "nl"),
    SPANISH(Locale.of("es"), "es"),
    FRENCH(Locale.FRENCH, "fr"),
    ITALIAN(Locale.ITALIAN, "it"),
    PORTUGUESE(Locale.of("pt"), "pt"),
    RUSSIAN(Locale.of("ru"), "ru"),
    JAPANESE(Locale.of("ja"), "ja"),
    CHINESE_SIMPLIFIED(Locale.SIMPLIFIED_CHINESE, "zh-Hans"),
    CHINESE_TRADITIONAL(Locale.TRADITIONAL_CHINESE, "zh-Hant"),
    DANISH(Locale.of("da"), "da"),
    INDONESIAN(Locale.of("id"), "id"),
    SWEDISH(Locale.of("sv"), "sv"),
    POLISH(Locale.of("pl"), "pl"),
    KOREAN(Locale.of("ko"), "ko"),
    TURKISH(Locale.of("tr"), "tr");

    private final Locale locale;
    private final String id;

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
