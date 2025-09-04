package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;

import javafx.scene.text.Font;

import org.kordamp.ikonli.javafx.FontIcon;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class AppFont {

    public static void init() {
        // Load ikonli fonts
        TrackEvent.info("Loading ikonli fonts ...");
        new FontIcon("mdi2s-stop");
        new FontIcon("mdal-360");
        new FontIcon("bi-alarm");

        TrackEvent.info("Loading bundled fonts ...");
        AppResources.with(
                AppResources.MAIN_MODULE,
                "fonts",
                path -> Files.walkFileTree(path, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        try (var in = Files.newInputStream(file)) {
                            Font.loadFont(in, 11);
                        } catch (Throwable t) {
                            // Font loading can fail in rare cases. This is however not important, so we can just ignore
                            // it
                            ErrorEventFactory.fromThrowable(t).expected().omit().handle();
                        }
                        return FileVisitResult.CONTINUE;
                    }
                }));
    }
}
