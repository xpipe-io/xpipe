package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

public class AppImages {

    public static final Image DEFAULT_IMAGE = new WritableImage(1, 1);
    private static final Map<String, Image> images = new HashMap<>();
    private static final Map<String, String> svgImages = new HashMap<>();

    public static void init() {
        TrackEvent.info("Loading images ...");
        for (var module : AppExtensionManager.getInstance().getContentModules()) {
            AppResources.with(module.getName(), "img", basePath -> {
                if (!Files.exists(basePath)) {
                    return;
                }

                var simpleName = FilenameUtils.getExtension(module.getName());
                String defaultPrefix = simpleName + ":";
                Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        var relativeFileName = FilenameUtils.separatorsToUnix(basePath.relativize(file).toString());
                        try {
                            if (FilenameUtils.getExtension(file.toString()).equals("svg")) {
                                var s = Files.readString(file);
                                svgImages.put(
                                        defaultPrefix + relativeFileName,
                                        s);
                            } else {
                                images.put(
                                        defaultPrefix + relativeFileName,
                                        loadImage(file));
                            }
                        } catch (IOException ex) {
                            ErrorEvent.fromThrowable(ex).omitted(true).build().handle();
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            });
        }
    }

    public static String svgImage(String file) {
        if (file == null) {
            return "";
        }

        var key = file.contains(":") ? file : "app:" + file;

        if (svgImages.containsKey(key)) {
            return svgImages.get(key);
        }

        TrackEvent.warn("Svg image " + key + " not found");
        return "";
    }

    public static boolean hasNormalImage(String file) {
        if (file == null) {
            return false;
        }

        var key = file.contains(":") ? file : "app:" + file;
        return images.containsKey(key);
    }

    public static boolean hasSvgImage(String file) {
        if (file == null) {
            return false;
        }

        var key = file.contains(":") ? file : "app:" + file;
        return svgImages.containsKey(key);
    }

    public static Image image(String file) {
        if (file == null) {
            return DEFAULT_IMAGE;
        }

        var key = file.contains(":") ? file : "app:" + file;

        if (images.containsKey(key)) {
            return images.get(key);
        }

        TrackEvent.warn("Normal image " + key + " not found");
        return DEFAULT_IMAGE;
    }

    private static Image loadImage(Path p) {
        if (p == null) {
            return DEFAULT_IMAGE;
        }

        if (!Files.isRegularFile(p)) {
            LoggerFactory.getLogger(AppImages.class).error("Image file " + p + " not found.");
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
