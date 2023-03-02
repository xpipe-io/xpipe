package io.xpipe.app.core;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.fxcomps.impl.FancyTooltipAugment;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.SupportedLocale;
import io.xpipe.app.util.DynamicOptionsBuilder;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.Translatable;
import io.xpipe.core.util.ModuleHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableValue;
import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;
import org.ocpsoft.prettytime.PrettyTime;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class AppI18n {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\w+?\\$");
    private Map<String, String> translations;
    private PrettyTime prettyTime;
private static AppI18n INSTANCE = new AppI18n();

    public static void init() {
        var i = INSTANCE;
        if (i.translations != null) {
            return;
        }

        i.load();

        if (AppPrefs.get() != null) {
            AppPrefs.get().language.addListener((c, o, n) -> {
                i.clear();
                i.load();
            });
        }
    }

    public static AppI18n getInstance() {
        return ((AppI18n) INSTANCE);
    }

    public static StringBinding readableDuration(String s, ObservableValue<Instant> instant) {
        return readableDuration(instant, rs -> getValue(getInstance().getLocalised(s), rs));
    }

    public static StringBinding readableDuration(ObservableValue<Instant> instant, UnaryOperator<String> op) {
        return Bindings.createStringBinding(
                () -> {
                    if (instant.getValue() == null) {
                        return "null";
                    }

                    return op.apply(getInstance().prettyTime.format(instant.getValue().minus(Duration.ofSeconds(1))));
                },
                instant);
    }

    public static ObservableValue<String> observable(String s, Object... vars) {
        if (s == null) {
            return null;
        }

        var key = INSTANCE.getKey(s);
        return Bindings.createStringBinding(() -> {
            return get(key, vars);
        });
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

    private void clear() {
        translations.clear();
        prettyTime = null;
    }

    @SuppressWarnings("removal")
    public static class CallingClass extends SecurityManager {
        public static final CallingClass INSTANCE = new CallingClass();

        public Class<?>[] getCallingClasses() {
            return getClassContext();
        }
    }

    @SneakyThrows
    private static String getCallerModuleName() {
        var callers = CallingClass.INSTANCE.getCallingClasses();
        for (Class<?> caller : callers) {
            if (caller.equals(CallingClass.class)
                    || caller.equals(ModuleHelper.class)
                    || caller.equals(AppI18n.class)
                    || caller.equals(FancyTooltipAugment.class)
                    || caller.equals(PrefsChoiceValue.class)
                    || caller.equals(Translatable.class)
                    || caller.equals(OptionsBuilder.class)
                    || caller.equals(DynamicOptionsBuilder.class)) {
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

    public boolean containsKey(String s) {
        var key = getKey(s);
        if (translations == null) {
            return false;
        }

        return translations.containsKey(key);
    }

    public String getLocalised(String s, Object... vars) {
        var key = getKey(s);

        if (translations == null) {
            TrackEvent.warn("Translations not initialized for" + key);
            return s;
        }

        if (!translations.containsKey(key)) {
            TrackEvent.warn("Translation key not found for " + key);
            return key;
        }

        var localisedString = translations.get(key);
        return getValue(localisedString, vars);
    }

    public boolean isLoaded() {
        return translations != null;
    }

    private boolean matchesLocale(Path f) {
        var l = AppPrefs.get() != null
                ? AppPrefs.get().language.getValue().getLocale()
                : SupportedLocale.ENGLISH.getLocale();
        var name = FilenameUtils.getBaseName(f.getFileName().toString());
        var ending = "_" + l.toLanguageTag();
        return name.endsWith(ending);
    }

    private void load() {
        TrackEvent.info("Loading translations ...");

        translations = new HashMap<>();
        for (var module : AppExtensionManager.getInstance().getContentModules()) {
            AppResources.with(module.getName(), "lang", basePath -> {
                if (!Files.exists(basePath)) {
                    return;
                }

                AtomicInteger fileCounter = new AtomicInteger();
                AtomicInteger lineCounter = new AtomicInteger();
                var simpleName = FilenameUtils.getExtension(module.getName());
                String defaultPrefix = simpleName.equals("app") ? "app." : simpleName + ".";
                Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (!matchesLocale(file)) {
                            return FileVisitResult.CONTINUE;
                        }

                        fileCounter.incrementAndGet();
                        try (var in = Files.newInputStream(file)) {
                            var props = new Properties();
                            props.load(in);
                            props.forEach((key, value) -> {
                                var hasPrefix = key.toString().contains(".");
                                var usedPrefix = hasPrefix ? "" : defaultPrefix;
                                translations.put(usedPrefix + key.toString(), value.toString());
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
            });
        }
        this.prettyTime = new PrettyTime(
                AppPrefs.get() != null
                        ? AppPrefs.get().language.getValue().getLocale()
                        : SupportedLocale.ENGLISH.getLocale());
    }
}
