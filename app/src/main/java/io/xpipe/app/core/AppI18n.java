package io.xpipe.app.core;

import io.xpipe.app.comp.base.ModalOverlayComp;
import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.fxcomps.impl.TooltipAugment;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.SupportedLocale;
import io.xpipe.app.util.OptionsBuilder;
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
import org.ocpsoft.prettytime.PrettyTime;

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

    @Value
    static class LoadedTranslations {

        Map<String, String> translations;
        Map<String, String> markdownDocumentations;
        PrettyTime prettyTime;
    }

    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\w+?\\$");
    private static AppI18n INSTANCE;
    private LoadedTranslations english;
    private final Property<LoadedTranslations> currentLanguage = new SimpleObjectProperty<>();

    public static void init() throws Exception {
        if (INSTANCE == null) {
            INSTANCE = new AppI18n();
        }
        INSTANCE.load();
    }

    private void load() throws Exception {
        if (english == null) {
            english = load(Locale.ENGLISH);
        }

        if (AppPrefs.get() != null) {
            AppPrefs.get().language().subscribe(n -> {
                try {
                    currentLanguage.setValue(n != null ? load(n.getLocale()) : null);
                } catch (Exception e) {
                    ErrorEvent.fromThrowable(e).handle();
                }
            });
        }
    }

    public static AppI18n get() {
        return INSTANCE;
    }

    private LoadedTranslations getLoaded() {
        return currentLanguage.getValue() != null ? currentLanguage.getValue() : english;
    }

    public static ObservableValue<String> observable(String s, Object... vars) {
        if (s == null) {
            return null;
        }

        var key = INSTANCE.getKey(s);
        return Bindings.createStringBinding(() -> {
            return get(key, vars);
        }, INSTANCE.currentLanguage);
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

    @SneakyThrows
    private static String getCallerModuleName() {
        var callers = CallingClass.INSTANCE.getCallingClasses();
        for (Class<?> caller : callers) {
            if (caller.equals(CallingClass.class)
                    || caller.equals(ModuleHelper.class)
                    || caller.equals(ModalOverlayComp.class)
                    || caller.equals(AppI18n.class)
                    || caller.equals(TooltipAugment.class)
                    || caller.equals(PrefsChoiceValue.class)
                    || caller.equals(Translatable.class)
                    || caller.equals(AppWindowHelper.class)
                    || caller.equals(OptionsBuilder.class)) {
                continue;
            }
            var split = caller.getModule().getName().split("\\.");
            return split[split.length - 1];
        }
        return "";
    }

    public String getKey(String s) {
        var key = s;
        if (!s.contains(".")) {
            key = getCallerModuleName() + "." + s;
        }
        return key;
    }

    public String getLocalised(String s, Object... vars) {
        var key = getKey(s);

        if (english == null) {
            TrackEvent.warn("Translations not initialized for " + key);
            return s;
        }

        if (currentLanguage.getValue() != null && currentLanguage.getValue().getTranslations().containsKey(key)) {
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
        if (currentLanguage.getValue() != null && currentLanguage.getValue().getMarkdownDocumentations().containsKey(name)) {
            var localisedString = currentLanguage.getValue().getMarkdownDocumentations().get(name);
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

    private Path getModuleLangPath(String module) {
        return XPipeInstallation.getLangPath().resolve(module);
    }

    private LoadedTranslations load(Locale l) throws Exception {
        TrackEvent.info("Loading translations ...");

        var translations = new HashMap<String, String>();
        for (var module : AppExtensionManager.getInstance().getContentModules()) {
            var basePath = getModuleLangPath(FilenameUtils.getExtension(module.getName())).resolve("strings");
                if (!Files.exists(basePath)) {
                    continue;
                }

                AtomicInteger fileCounter = new AtomicInteger();
                AtomicInteger lineCounter = new AtomicInteger();
                var simpleName = FilenameUtils.getExtension(module.getName());
                String defaultPrefix = simpleName.equals("app") ? "app." : simpleName + ".";
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
                                var hasPrefix = key.toString().contains(".");
                                var usedPrefix = hasPrefix ? "" : defaultPrefix;
                                translations.put(usedPrefix + key, value.toString());
                                lineCounter.incrementAndGet();
                            });
                        } catch (IOException ex) {
                            ErrorEvent.fromThrowable(ex).omitted(true).build().handle();
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });

                TrackEvent.withDebug("Loading translations for module " + simpleName)
                        .tag("fileCount", fileCounter.get())
                        .tag("lineCount", lineCounter.get())
                        .handle();
        }

        var markdownDocumentations = new HashMap<String, String>();
        for (var module : AppExtensionManager.getInstance().getContentModules()) {
            var basePath = getModuleLangPath(FilenameUtils.getExtension(module.getName())).resolve("texts");
                if (!Files.exists(basePath)) {
                    continue;
                }

                var moduleName = FilenameUtils.getExtension(module.getName());
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
                            var usedPrefix = moduleName + ":";
                            markdownDocumentations.put(
                                    usedPrefix + name, new String(in.readAllBytes(), StandardCharsets.UTF_8));
                        } catch (IOException ex) {
                            ErrorEvent.fromThrowable(ex).omitted(true).build().handle();
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
        }

        var prettyTime = new PrettyTime(
                AppPrefs.get() != null
                        ? AppPrefs.get().language().getValue().getLocale()
                        : SupportedLocale.getEnglish().getLocale());

        return new LoadedTranslations(translations,markdownDocumentations, prettyTime);
    }

    @SuppressWarnings("removal")
    public static class CallingClass extends SecurityManager {
        public static final CallingClass INSTANCE = new CallingClass();

        public Class<?>[] getCallingClasses() {
            return getClassContext();
        }
    }
}
