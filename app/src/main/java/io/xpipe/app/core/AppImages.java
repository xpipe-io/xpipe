package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

import org.apache.commons.io.FilenameUtils;

import java.awt.image.BufferedImage;
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
import java.util.function.Predicate;

public class AppImages {

    public static final Image DEFAULT_IMAGE = new WritableImage(1, 1);
    private static final Map<String, Image> images = new HashMap<>();

    public static void remove(Predicate<String> filter) {
        images.keySet().removeIf(filter);
    }

    public static void init() {
        if (images.size() > 0) {
            return;
        }

        TrackEvent.info("Loading images ...");
        for (var module : AppExtensionManager.getInstance().getContentModules()) {
            loadDirectory(module.getName(), "img", true);
        }
    }

    public static void loadDirectory(String module, String dir, boolean loadImages) {
        var start = Instant.now();
        AppResources.with(module, dir, basePath -> {
            if (!Files.exists(basePath)) {
                return;
            }

            var simpleName = FilenameUtils.getExtension(module);
            String defaultPrefix = simpleName + ":";
            Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    var relativeFileName = FilenameUtils.separatorsToUnix(
                            basePath.relativize(file).toString());
                    var key = defaultPrefix + relativeFileName;
                    if (images.containsKey(key)) {
                        return FileVisitResult.CONTINUE;
                    }

                    if (loadImages) {
                        images.put(key, loadImage(file));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        });
        var elapsed = Duration.between(start, Instant.now());
        TrackEvent.trace("Loaded images in " + module + ":" + dir + " in " + elapsed.toMillis() + " ms");
    }

    public static void loadRasterImages(Path directory, String prefix) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }

        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                var relativeFileName = FilenameUtils.separatorsToUnix(
                        directory.relativize(file).toString());
                var key = prefix + "/" + relativeFileName;
                if (images.containsKey(key)) {
                    return FileVisitResult.CONTINUE;
                }

                images.put(key, loadImage(file));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static boolean hasNormalImage(String file) {
        if (file == null) {
            return false;
        }

        if (images.containsKey(file)) {
            return true;
        }

        var key = file.contains(":") ? file : "app:" + file;
        if (images.containsKey(key)) {
            return true;
        }

        return false;
    }

    public static Image image(String file) {
        if (file == null) {
            return DEFAULT_IMAGE;
        }

        if (images.containsKey(file)) {
            return images.get(file);
        }

        var key = file.contains(":") ? file : "app:" + file;

        if (images.containsKey(key)) {
            return images.get(key);
        }

        TrackEvent.warn("Normal image " + key + " not found");
        return DEFAULT_IMAGE;
    }

    public static BufferedImage toAwtImage(Image fxImage) {
        BufferedImage img =
                new BufferedImage((int) fxImage.getWidth(), (int) fxImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < fxImage.getWidth(); x++) {
            for (int y = 0; y < fxImage.getHeight(); y++) {
                int rgb = fxImage.getPixelReader().getArgb(x, y);
                img.setRGB(x, y, rgb);
            }
        }
        return img;
    }

    public static Image loadImage(Path p) {
        if (p == null) {
            return DEFAULT_IMAGE;
        }

        if (!Files.isRegularFile(p)) {
            TrackEvent.error("Image file " + p + " not found.");
            return DEFAULT_IMAGE;
        }

        try (var in = Files.newInputStream(p)) {
            return new Image(in, -1, -1, true, true);
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).omitted(true).build().handle();
            return DEFAULT_IMAGE;
        }
    }
}
