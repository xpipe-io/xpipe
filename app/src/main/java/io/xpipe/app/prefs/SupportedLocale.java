package io.xpipe.app.prefs;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.ext.PrefsChoiceValue;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Locale;

@AllArgsConstructor
@Getter
public class SupportedLocale implements PrefsChoiceValue {

    public static List<SupportedLocale> ALL = AppProperties.get().getLanguages().stream()
            .map(s -> new SupportedLocale(Locale.of(s), s))
            .toList();
    private final Locale locale;
    private final String id;

    public static SupportedLocale getEnglish() {
        return ALL.stream()
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
