package io.xpipe.app.core;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.resources.AppResources;
import io.xpipe.core.process.OsType;

import javafx.scene.Node;
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
        new FontIcon("mdi2m-magnify");
        new FontIcon("mdi2d-database-plus");
        new FontIcon("mdi2p-professional-hexagon");
        new FontIcon("mdi2c-chevron-double-right");

        TrackEvent.info("Loading bundled fonts ...");
        AppResources.with(
                AppResources.XPIPE_MODULE,
                "fonts",
                path -> Files.walkFileTree(path, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        try (var in = Files.newInputStream(file)) {
                            Font.loadFont(in, OsType.getLocal() == OsType.LINUX ? 11 : 12);
                        } catch (Throwable t) {
                            // Font loading can fail in rare cases. This is however not important, so we can just ignore
                            // it
                        }
                        return FileVisitResult.CONTINUE;
                    }
                }));
    }
}
