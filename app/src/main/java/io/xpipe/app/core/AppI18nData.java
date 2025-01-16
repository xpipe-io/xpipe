package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.SupportedLocale;
import io.xpipe.core.util.XPipeInstallation;
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

@Value
public class AppI18nData {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\w+?\\$");

    SupportedLocale locale;
    Map<String, String> translations;
    Map<String, String> markdownDocumentations;

    Optional<String> getLocalised(String s, Object... vars) {
        if (getTranslations().containsKey(s)) {
            var localisedString = getTranslations().get(s);
            return Optional.ofNullable(getValue(localisedString, vars));
        }
        return Optional.empty();
    }

    private String getValue(String s, Object... vars) {
        s = s.replace("\\n", "\n");
        if (vars.length > 0) {
            var matcher = VAR_PATTERN.matcher(s);
            for (var v : vars) {
                v = v != null ? v : "null";
                if (matcher.find()) {
                    var group = matcher.group();
                    s = s.replace(group, v.toString());
                } else {
                    TrackEvent.warn("No match found for value " + v + " in string " + s);
                }
            }
        }
        return s;
    }

    static AppI18nData load(SupportedLocale l) throws Exception {
        TrackEvent.info("Loading translations ...");

        var translations = new HashMap<String, String>();
        {
            var basePath = XPipeInstallation.getLangPath().resolve("strings");
            AtomicInteger fileCounter = new AtomicInteger();
            AtomicInteger lineCounter = new AtomicInteger();
            Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!matchesLocale(file, l.getLocale())) {
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
                    if (!matchesLocale(file, l.getLocale())) {
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

        return new AppI18nData(l, translations, markdownDocumentations);
    }

    private static boolean matchesLocale(Path f, Locale l) {
        var name = FilenameUtils.getBaseName(f.getFileName().toString());
        var ending = "_" + l.toLanguageTag();
        return name.endsWith(ending);
    }

}
