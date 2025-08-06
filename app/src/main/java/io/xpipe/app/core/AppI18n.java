package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.SupportedLocale;
import io.xpipe.app.util.BindingsHelper;
import io.xpipe.app.util.GlobalObjectProperty;
import io.xpipe.app.util.PlatformState;
import io.xpipe.app.util.PlatformThread;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import java.util.*;

public class AppI18n {

    private static AppI18n INSTANCE;
    private final Property<AppI18nData> currentLanguage = new GlobalObjectProperty<>();
    private final Property<SupportedLocale> currentLocale = new GlobalObjectProperty<>();
    private final Map<String, ObservableValue<String>> observableCache = new HashMap<>();
    private AppI18nData english;

    public AppI18n() {
        currentLocale.bind(BindingsHelper.map(
                currentLanguage,
                appI18nData -> appI18nData != null ? appI18nData.getLocale() : SupportedLocale.getEnglish()));
    }

    public static ObservableValue<SupportedLocale> activeLanguage() {
        if (INSTANCE == null) {
            return new GlobalObjectProperty<>(SupportedLocale.getEnglish());
        }

        return INSTANCE.currentLocale;
    }

    public static void init() throws Exception {
        if (INSTANCE == null) {
            INSTANCE = new AppI18n();
        }
        INSTANCE.load();
    }

    public static AppI18n get() {
        return INSTANCE;
    }

    public static ObservableValue<String> observable(String s, Object... vars) {
        return INSTANCE.observableImpl(s, vars);
    }

    private ObservableValue<String> observableImpl(String s, Object... vars) {
        if (s == null) {
            return null;
        }

        synchronized (this) {
            var key = getKey(s);

            // Don't cache vars
            if (vars.length > 0) {
                var binding = Bindings.createStringBinding(
                        () -> {
                            return getLocalised(key, vars);
                        },
                        currentLanguage);
                return binding;
            }

            var found = observableCache.get(key);
            if (found != null) {
                return found;
            }

            var binding = Bindings.createStringBinding(
                    () -> {
                        return getLocalised(key, vars);
                    },
                    currentLanguage);
            observableCache.put(key, binding);
            return binding;
        }
    }

    public static String get(String s, Object... vars) {
        return INSTANCE.getLocalised(s, vars);
    }

    private void load() throws Exception {
        if (english == null) {
            english = AppI18nData.load(SupportedLocale.getEnglish());
            Locale.setDefault(Locale.ENGLISH);

            // Load bundled JDK locale resources by initializing the classes
            for (var value : SupportedLocale.values()) {
                value.getLocale().getDisplayName();
            }
        }

        if (currentLanguage.getValue() == null && PlatformState.getCurrent() == PlatformState.RUNNING) {
            if (AppPrefs.get() != null) {
                // Perform initial update on platform thread
                PlatformThread.runLaterIfNeededBlocking(() -> {
                    AppPrefs.get().language().subscribe(n -> {
                        try {
                            var newValue = n != null ? AppI18nData.load(n) : null;
                            PlatformThread.runLaterIfNeeded(() -> {
                                currentLanguage.setValue(newValue);
                                Locale.setDefault(n != null ? n.getLocale() : Locale.ENGLISH);
                            });
                        } catch (Exception e) {
                            ErrorEventFactory.fromThrowable(e).handle();
                        }
                    });
                });
            }
        }
    }

    private String getKey(String s) {
        var key = s;
        if (s.startsWith("app.")
                || s.startsWith("base.")
                || s.startsWith("proc.")
                || s.startsWith("uacc.")
                || s.startsWith("system.")) {
            key = key.substring(key.indexOf(".") + 1);
        }
        return key;
    }

    private String getLocalised(String s, Object... vars) {
        var key = getKey(s);
        if (english == null) {
            return key;
        }

        if (currentLanguage.getValue() != null) {
            var localisedString = currentLanguage.getValue().getLocalised(key, vars);
            if (localisedString.isPresent()) {
                return localisedString.get();
            }
        }

        if (english != null) {
            var localisedString = english.getLocalised(key, vars);
            if (localisedString.isPresent()) {
                return localisedString.get();
            }
        }

        TrackEvent.warn("Translation key not found for " + key);
        return key;
    }

    public String getMarkdownDocumentation(String name) {
        if (name.contains(":")) {
            name = name.substring(name.indexOf(":") + 1);
        }

        if (currentLanguage.getValue() != null
                && currentLanguage.getValue().getMarkdownDocumentations().containsKey(name)) {
            var localisedString =
                    currentLanguage.getValue().getMarkdownDocumentations().get(name);
            return localisedString;
        }

        if (english.getMarkdownDocumentations().containsKey(name)) {
            var localisedString = english.getMarkdownDocumentations().get(name);
            return localisedString;
        }

        TrackEvent.withWarn("Markdown documentation for key " + name + " not found")
                .handle();
        return "";
    }
}
