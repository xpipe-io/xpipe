package io.xpipe.app.core;

import atlantafx.base.theme.NordDark;
import atlantafx.base.theme.NordLight;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.extension.I18n;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.event.TrackEvent;
import io.xpipe.extension.prefs.PrefsChoiceValue;
import javafx.application.Application;
import javafx.scene.Scene;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class AppStyle {

    private static final Map<Path, String> STYLESHEET_CONTENTS = new HashMap<>();
    private static final List<Scene> scenes = new ArrayList<>();
    private static String FONT_CONTENTS = "";

    public static void init() {
        if (STYLESHEET_CONTENTS.size() > 0) {
            return;
        }

        TrackEvent.info("Loading stylesheets ...");
        loadStylesheets();

        if (AppPrefs.get() != null) {
            AppPrefs.get().theme.addListener((c, o, n) -> {
                changeTheme(o, n);
            });
            AppPrefs.get().useSystemFont.addListener((c, o, n) -> {
                changeFontUsage(n);
            });
        }
    }

    private static void loadStylesheets() {
        AppResources.with(AppResources.XPIPE_MODULE, "font-config/font.css", path -> {
            var bytes = Files.readAllBytes(path);
            FONT_CONTENTS = "data:text/css;base64," + Base64.getEncoder().encodeToString(bytes);
        });

        for (var module : AppExtensionManager.getInstance().getContentModules()) {
            // Use data URLs because module path URLs are not accepted
            // by JavaFX as it does not use Path objects to load stylesheets
            AppResources.with(module.getName(), "style", path -> {
                if (!Files.exists(path)) {
                    return;
                }

                TrackEvent.trace("core", "Loading styles for module " + module.getName());
                Files.walkFileTree(path, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        try {
                            var bytes = Files.readAllBytes(file);
                            var s = "data:text/css;base64,"
                                    + Base64.getEncoder().encodeToString(bytes);
                            STYLESHEET_CONTENTS.put(file, s);
                        } catch (IOException ex) {
                            ErrorEvent.fromThrowable(ex).omitted(true).build().handle();
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            });
        }
    }

    private static void changeTheme(Theme oldTheme, Theme newTheme) {
        scenes.forEach(scene -> {
            Application.setUserAgentStylesheet(newTheme.getTheme().getUserAgentStylesheet());
        });
    }

    private static void changeFontUsage(boolean use) {
        if (!use) {
            scenes.forEach(scene -> {
                scene.getStylesheets().add(FONT_CONTENTS);
            });
        } else {
            scenes.forEach(scene -> {
                scene.getStylesheets().remove(FONT_CONTENTS);
            });
        }
    }

    public static void reloadStylesheets(Scene scene) {
        STYLESHEET_CONTENTS.clear();
        FONT_CONTENTS = "";

        init();
        scene.getStylesheets().clear();
        addStylesheets(scene);
    }

    public static void addStylesheets(Scene scene) {
        var t = AppPrefs.get() != null ? AppPrefs.get().theme.getValue() : Theme.LIGHT;
        Application.setUserAgentStylesheet(t.getTheme().getUserAgentStylesheet());
        TrackEvent.debug("Set theme " + t.getId() + " for scene");

        if (AppPrefs.get() != null && !AppPrefs.get().useSystemFont.get()) {
            scene.getStylesheets().add(FONT_CONTENTS);
        }

        STYLESHEET_CONTENTS.values().forEach(s -> {
            scene.getStylesheets().add(s);
        });
        TrackEvent.debug("Added stylesheets for scene");

        scenes.add(scene);
    }

    @AllArgsConstructor
    @Getter
    public enum Theme implements PrefsChoiceValue {
        LIGHT("light", new PrimerLight()),
        DARK("dark", new PrimerDark()),
        NORD_LIGHT("nordLight", new NordLight()),
        NORD_DARK("nordDark", new NordDark());
        // DARK("dark");

        private final String id;
        private final atlantafx.base.theme.Theme theme;

        @Override
        public String toTranslatedString() {
            return I18n.get(id + "Theme");
        }
    }
}
