package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEventFactory;
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
        loadBundledProviderIcons();
        loadOsIcons();
        loadWelcomeImages();
        loadMiscImages();
        loadLogos();
    }

    private static void loadBundledProviderIcons() {
        var exts = AppExtensionManager.getInstance().getContentModules();
        for (Module ext : exts) {
            AppResources.with(ext.getName(), "img/", basePath -> {
                if (!Files.exists(basePath)) {
                    return;
                }

                var skipLarge = AppDisplayScale.hasOnlyDefaultDisplayScale();
                Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        var name = file.getFileName().toString();
                        if (name.contains("-80") && skipLarge) {
                            return FileVisitResult.CONTINUE;
                        }

                        AppImages.loadImage(file, name);
                        return FileVisitResult.CONTINUE;
                    }
                });
            });
        }
    }

    private static void loadOsIcons() {
        AppResources.with(AppResources.MAIN_MODULE, "os", basePath -> {
            var skipLarge = AppDisplayScale.hasOnlyDefaultDisplayScale();
            Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    var name = file.getFileName().toString();
                    if (name.contains("-40") && skipLarge) {
                        return FileVisitResult.CONTINUE;
                    }

                    AppImages.loadImage(file, "os/" + name);
                    return FileVisitResult.CONTINUE;
                }
            });
        });
    }

    private static void loadWelcomeImages() {
        AppResources.with(AppResources.MAIN_MODULE, "welcome", basePath -> {
            var skipLarge = AppDisplayScale.hasOnlyDefaultDisplayScale();
            Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    var name = file.getFileName().toString();
                    if (name.contains("-244") && skipLarge) {
                        return FileVisitResult.CONTINUE;
                    }

                    AppImages.loadImage(file, "welcome/" + name);
                    return FileVisitResult.CONTINUE;
                }
            });
        });
    }

    private static void loadLogos() {
        AppResources.with(AppResources.MAIN_MODULE, "logo", basePath -> {
            Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    var relativeFileName = FilenameUtils.separatorsToUnix(
                            basePath.getParent().relativize(file).toString());
                    AppImages.loadImage(file, relativeFileName);
                    return FileVisitResult.CONTINUE;
                }
            });
        });
    }

    private static void loadMiscImages() {
        AppResources.with(AppResources.MAIN_MODULE, "", basePath -> {
            loadImage(basePath.resolve("action.png"), "action.png");
            loadImage(basePath.resolve("error.png"), "error.png");
        });
    }

    public static boolean hasImage(String file) {
        if (file == null) {
            return false;
        }

        if (images.containsKey(file)) {
            return true;
        }

        var key = file.contains(":") ? file.split(":", 2)[1] : file;
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

        var key = file.contains(":") ? file.split(":", 2)[1] : file;
        if (images.containsKey(key)) {
            return images.get(key);
        }

        TrackEvent.warn("Image " + key + " not found");
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

    public static void loadImage(Path p, String key) {
        if (p == null) {
            return;
        }

        images.put(key, loadImage(p));
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
            ErrorEventFactory.fromThrowable(e).omitted(true).build().handle();
            return DEFAULT_IMAGE;
        }
    }
}
