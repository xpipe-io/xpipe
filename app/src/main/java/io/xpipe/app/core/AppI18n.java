package io.xpipe.app.core;

import io.xpipe.app.comp.base.ModalOverlayComp;
import io.xpipe.app.comp.base.TooltipAugment;
import io.xpipe.app.core.window.AppWindowHelper;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.PlatformState;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.app.util.Translatable;
import io.xpipe.core.util.ModuleHelper;
import io.xpipe.core.util.XPipeInstallation;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import lombok.SneakyThrows;
import lombok.Value;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class AppI18n {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\w+?\\$");
    private static AppI18n INSTANCE;
    private final Property<LoadedTranslations> currentLanguage = new SimpleObjectProperty<>();
    private LoadedTranslations english;

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
        if (s == null) {
            return null;
        }

        var key = INSTANCE.getKey(s);
        return Bindings.createStringBinding(
                () -> {
                    return get(key, vars);
                },
                INSTANCE.currentLanguage);
    }

    public static String get(String s, Object... vars) {
        return INSTANCE.getLocalised(s, vars);
    }

    private static String getValue(String s, Object... vars) {
        Objects.requireNonNull(s);

        s = s.replace("\\n", "\n");
        for (var v : vars) {
            v = v != null ? v : "null";
            var matcher = VAR_PATTERN.matcher(s);
            if (matcher.find()) {
                var group = matcher.group();
                s = s.replace(group, v.toString());
            } else {
                TrackEvent.warn("No match found for value " + v + " in string " + s);
            }
        }
        return s;
    }

    private void load() throws Exception {
        if (english == null) {
            english = load(Locale.ENGLISH);
            Locale.setDefault(Locale.ENGLISH);
        }

        if (currentLanguage.getValue() == null && PlatformState.getCurrent() == PlatformState.RUNNING) {
            if (AppPrefs.get() != null) {
                // Perform initial update on platform thread
                PlatformThread.runLaterIfNeededBlocking(() -> {
                    AppPrefs.get().language().subscribe(n -> {
                        try {
                            currentLanguage.setValue(n != null ? load(n.getLocale()) : null);
                            Locale.setDefault(n != null ? n.getLocale() : Locale.ENGLISH);
                        } catch (Exception e) {
                            ErrorEvent.fromThrowable(e).handle();
                        }
                    });
                });
            }
        }
    }

    public LoadedTranslations getLoaded() {
        return currentLanguage.getValue() != null ? currentLanguage.getValue() : english;
    }

    public String getKey(String s) {
        var key = s;
        if (s.startsWith("app.") || s.startsWith("base.") || s.startsWith("proc.") || s.startsWith("uacc.") || s.startsWith("system.")) {
            key = key.substring(key.indexOf(".") + 1);
        }
        return key;
    }

    public String getLocalised(String s, Object... vars) {
        var key = getKey(s);

        if (english == null) {
            TrackEvent.warn("Translations not initialized for " + key);
            return s;
        }

        if (currentLanguage.getValue() != null
                && currentLanguage.getValue().getTranslations().containsKey(key)) {
            var localisedString = currentLanguage.getValue().getTranslations().get(key);
            return getValue(localisedString, vars);
        }

        if (english.getTranslations().containsKey(key)) {
            var localisedString = english.getTranslations().get(key);
            return getValue(localisedString, vars);
        }

        TrackEvent.warn("Translation key not found for " + key);
        return key;
    }

    private boolean matchesLocale(Path f, Locale l) {
        var name = FilenameUtils.getBaseName(f.getFileName().toString());
        var ending = "_" + l.toLanguageTag();
        return name.endsWith(ending);
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

    private LoadedTranslations load(Locale l) throws Exception {
        TrackEvent.info("Loading translations ...");

        var translations = new HashMap<String, String>();
        {
        var basePath = XPipeInstallation.getLangPath().resolve("strings");
        AtomicInteger fileCounter = new AtomicInteger();
        AtomicInteger lineCounter = new AtomicInteger();
        Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!matchesLocale(file, l)) {
                    return FileVisitResult.CONTINUE;
                }

                if (!file.getFileName().toString().endsWith(".properties")) {
                    return FileVisitResult.CONTINUE;
                }

                fileCounter.incrementAndGet();
                try (var in = Files.newInputStream(file)) {
                    var props = new Properties();
                    props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                    props.forEach((key, value) -> {
                        translations.put(key.toString(), value.toString());
                        lineCounter.incrementAndGet();
                    });
                } catch (IOException ex) {
                    ErrorEvent.fromThrowable(ex).omitted(true).build().handle();
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

        var markdownDocumentations = new HashMap<String, String>();
        {
            var basePath = XPipeInstallation.getLangPath().resolve("texts");
            Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!matchesLocale(file, l)) {
                        return FileVisitResult.CONTINUE;
                    }

                    if (!file.getFileName().toString().endsWith(".md")) {
                        return FileVisitResult.CONTINUE;
                    }

                    var name = file.getFileName()
                            .toString()
                            .substring(0, file.getFileName().toString().lastIndexOf("_"));
                    try (var in = Files.newInputStream(file)) {
                        markdownDocumentations.put(name, new String(in.readAllBytes(), StandardCharsets.UTF_8));
                    } catch (IOException ex) {
                        ErrorEvent.fromThrowable(ex).omitted(true).build().handle();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        return new LoadedTranslations(l, translations, markdownDocumentations);
    }

    @Value
    public static class LoadedTranslations {

        Locale locale;
        Map<String, String> translations;
        Map<String, String> markdownDocumentations;
    }
}
