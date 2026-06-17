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
    ENGLISH(Locale.ENGLISH, "en" ,"en_US"),
    GERMAN(Locale.GERMAN, "de", "de_DE"),
    DUTCH(Locale.of("nl"), "nl", "nl_NL"),
    SPANISH(Locale.of("es"), "es", "es_ES"),
    FRENCH(Locale.FRENCH, "fr", "fr_FR"),
    ITALIAN(Locale.ITALIAN, "it",  "it_IT"),
    PORTUGUESE(Locale.of("pt"), "pt", "pt_PT"),
    RUSSIAN(Locale.of("ru"), "ru", "ru_RU"),
    JAPANESE(Locale.of("ja"), "ja", "ja_JP"),
    CHINESE_SIMPLIFIED(Locale.SIMPLIFIED_CHINESE, "zh-Hans",  "zh_CN"),
    CHINESE_TRADITIONAL(Locale.TRADITIONAL_CHINESE, "zh-Hant",  "zh_TW"),
    DANISH(Locale.of("da"), "da", "da_DK"),
    INDONESIAN(Locale.of("id"), "id", "id_ID"),
    SWEDISH(Locale.of("sv"), "sv", "sv_SE"),
    POLISH(Locale.of("pl"), "pl", "pl_PL"),
    KOREAN(Locale.of("ko"), "ko", "ko_KR"),
    TURKISH(Locale.of("tr"), "tr", "tr_TR"),
    VIETNAMESE(Locale.of("vi"), "vi", "vi_VN"),;

    private final Locale locale;
    private final String id;
    private final String withCountryTag;

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
