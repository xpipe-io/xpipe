package io.xpipe.app.icon;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.SVGRenderingHints;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.parser.SVGLoader;
import lombok.Getter;
import org.apache.commons.io.FileUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

public class SystemIconCache {

    private static enum ImageColorScheme {
        TRANSPARENT,
        MIXED,
        LIGHT,
        DARK
    }

    private static final Path DIRECTORY =
            AppProperties.get().getDataDir().resolve("cache").resolve("icons").resolve("raster");
    private static final int[] sizes = new int[] {16, 24, 40, 80};
    private static final int VERSION = 2;

    @Getter
    private static boolean built = false;

    public static Path getDirectory(SystemIconSource source) {
        var target = DIRECTORY.resolve(source.getId());
        return target;
    }

    public static void refreshBuilt() throws IOException {
        if (!Files.exists(DIRECTORY)) {
            return;
        }

        try (var stream = Files.walk(DIRECTORY)) {
            built = stream.anyMatch(path -> Files.isRegularFile(path));
        }
    }

    public static void rebuildCache(Map<SystemIconSource, SystemIconSourceData> all) {
        try {
            var versionFile = DIRECTORY.resolve("version");
            var version =
                    Files.exists(versionFile) ? Files.readString(versionFile).strip() : null;
            if (!String.valueOf(VERSION).equals(version)) {
                if (Files.isDirectory(DIRECTORY)) {
                    FileUtils.cleanDirectory(DIRECTORY.toFile());
                } else {
                    Files.createDirectories(DIRECTORY);
                }
                Files.writeString(versionFile, String.valueOf(VERSION));
            }

            for (var e : all.entrySet()) {
                var target = DIRECTORY.resolve(e.getKey().getId());
                Files.createDirectories(target);

                Map<String, ImageColorScheme> colorSchemeMap = new HashMap<>();

                var baseIcons = e.getValue().getIcons().stream()
                        .filter(f -> f.getColorSchemeData() == SystemIconSourceFile.ColorSchemeData.DEFAULT)
                        .toList();
                for (var icon : baseIcons) {
                    var schemeFile = target.resolve(icon.getName() + ".scheme");
                    if (refreshChecksum(icon.getFile(), target, icon.getName(), false)) {
                        if (Files.exists(schemeFile)) {
                            var scheme = Files.readString(schemeFile);
                            var schemeValue = ImageColorScheme.valueOf(scheme.toUpperCase());
                            colorSchemeMap.put(icon.getName(), schemeValue);
                            continue;
                        }
                    }

                    var scheme = rasterizeSizes(icon.getFile(), target, icon.getName(), false);
                    if (scheme == ImageColorScheme.TRANSPARENT) {
                        var message = "Failed to rasterize icon "
                                + icon.getFile().getFileName().toString() + ": Rasterized image is transparent";
                        ErrorEventFactory.fromMessage(message).omit().expected().handle();
                        continue;
                    }

                    colorSchemeMap.put(icon.getName(), scheme);
                    Files.writeString(schemeFile, scheme.name().toLowerCase(Locale.ROOT));
                }

                var darkIconNames = e.getValue().getIcons().stream()
                        .filter(f -> f.getColorSchemeData() == SystemIconSourceFile.ColorSchemeData.DARK)
                        .map(f -> f.getName())
                        .collect(Collectors.toSet());
                var darkAvailableIcons = e.getValue().getIcons().stream()
                        .filter(f -> f.getColorSchemeData() == SystemIconSourceFile.ColorSchemeData.DARK)
                        .toList();
                for (var icon : darkAvailableIcons) {
                    var existingBaseScheme = colorSchemeMap.get(icon.getName());
                    var generateDarkIcon = existingBaseScheme == null || existingBaseScheme == ImageColorScheme.DARK;
                    if (generateDarkIcon) {
                        if (refreshChecksum(icon.getFile(), target, icon.getName(), true)) {
                            continue;
                        }

                        var scheme = rasterizeSizes(icon.getFile(), target, icon.getName(), true);
                        if (scheme == ImageColorScheme.TRANSPARENT) {
                            var message = "Failed to rasterize icon "
                                    + icon.getFile().getFileName().toString() + ": Rasterized image is transparent";
                            ErrorEventFactory.fromMessage(message)
                                    .omit()
                                    .expected()
                                    .handle();
                        }

                        continue;
                    }
                }

                for (var icon : baseIcons) {
                    var existingBaseScheme = colorSchemeMap.get(icon.getName());
                    var generateDarkModeInverse =
                            existingBaseScheme == ImageColorScheme.DARK && !darkIconNames.contains(icon.getName());
                    if (generateDarkModeInverse) {
                        rasterizeSizesInverted(icon.getFile(), target, icon.getName(), true);
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
        }
    }

    private static boolean refreshChecksum(Path source, Path dir, String name, boolean dark) throws Exception {
        var md5Name = name + (dark ? "-dark" : "") + ".md5";
        var bytes = Files.readAllBytes(source);
        var md = MessageDigest.getInstance("MD5");
        md.update(bytes);
        var digest = md.digest();
        var md5File = dir.resolve(md5Name);
        if (Files.exists(md5File) && Arrays.equals(Files.readAllBytes(md5File), digest)) {
            return true;
        } else {
            Files.write(md5File, digest);
            return false;
        }
    }

    private static ImageColorScheme rasterizeSizes(Path path, Path dir, String name, boolean dark) {
        TrackEvent.trace("Rasterizing image " + path.getFileName().toString());
        try {
            ImageColorScheme c = null;
            for (var size : sizes) {
                var image = rasterize(path, size);
                if (image == null) {
                    continue;
                }
                if (c == null) {
                    c = determineColorScheme(image);
                    if (c == ImageColorScheme.TRANSPARENT) {
                        return ImageColorScheme.TRANSPARENT;
                    }
                }
                write(dir, name, dark, size, image);
            }
            return c != null ? c : ImageColorScheme.TRANSPARENT;
        } catch (Exception ex) {
            var message = "Failed to rasterize icon icon " + path.getFileName().toString() + ": " + ex.getMessage();
            ErrorEventFactory.fromThrowable(ex)
                    .description(message)
                    .omit()
                    .expected()
                    .handle();
            return ImageColorScheme.TRANSPARENT;
        }
    }

    private static void rasterizeSizesInverted(Path path, Path dir, String name, boolean dark) throws IOException {
        try {
            for (var size : sizes) {
                var image = rasterize(path, size);
                if (image == null) {
                    continue;
                }

                var invert = invert(image);
                write(dir, name, dark, size, invert);
            }
        } catch (Exception ex) {
            if (ex instanceof IOException) {
                throw ex;
            }

            ErrorEventFactory.fromThrowable(ex).omit().expected().handle();
        }
    }

    private static BufferedImage rasterize(Path path, int px) throws IOException {
        SVGLoader loader = new SVGLoader();
        URL svgUrl = path.toUri().toURL();
        SVGDocument svgDocument = loader.load(svgUrl);
        if (svgDocument == null) {
            return null;
        }

        BufferedImage image = new BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(SVGRenderingHints.KEY_IMAGE_ANTIALIASING, SVGRenderingHints.VALUE_IMAGE_ANTIALIASING_ON);
        g.setRenderingHint(SVGRenderingHints.KEY_SOFT_CLIPPING, SVGRenderingHints.VALUE_SOFT_CLIPPING_ON);
        svgDocument.render((Component) null, g, new ViewBox(0, 0, px, px));
        g.dispose();
        return image;
    }

    private static BufferedImage write(Path dir, String name, boolean dark, int px, BufferedImage image)
            throws IOException {
        var out = dir.resolve(name + "-" + px + (dark ? "-dark" : "") + ".png");
        ImageIO.write(image, "png", out.toFile());
        return image;
    }

    private static BufferedImage invert(BufferedImage image) {
        var buffer = new BufferedImage(image.getWidth(), image.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int clr = image.getRGB(x, y);
                int alpha = (clr >> 24) & 0xff;
                int red = (clr & 0x00ff0000) >> 16;
                int green = (clr & 0x0000ff00) >> 8;
                int blue = clr & 0x000000ff;
                buffer.setRGB(x, y, new Color(255 - red, 255 - green, 255 - blue, alpha).getRGB());
            }
        }
        return buffer;
    }

    private static ImageColorScheme determineColorScheme(BufferedImage image) {
        var transparent = true;
        var counter = 0;
        var mean = 0.0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int clr = image.getRGB(x, y);
                int alpha = (clr >> 24) & 0xff;
                int red = (clr & 0x00ff0000) >> 16;
                int green = (clr & 0x0000ff00) >> 8;
                int blue = clr & 0x000000ff;

                if (alpha > 0) {
                    transparent = false;
                }

                if (alpha < 100) {
                    continue;
                }

                mean += (red + green + blue) * (alpha / 255.0);
                counter++;
            }
        }

        if (transparent) {
            return ImageColorScheme.TRANSPARENT;
        }

        if (counter == 0) {
            return ImageColorScheme.TRANSPARENT;
        }

        mean /= counter * 3;
        if (mean < 60) {
            return ImageColorScheme.DARK;
        } else if (mean > 195) {
            return ImageColorScheme.LIGHT;
        } else {
            return ImageColorScheme.MIXED;
        }
    }
}
