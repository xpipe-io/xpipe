package io.xpipe.app.icon;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.SVGRenderingHints;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.parser.SVGLoader;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEvent;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;

public class SystemIconCache {

    private static final Path DIRECTORY = AppProperties.get().getDataDir().resolve("cache").resolve("icons").resolve("raster");
    private static final int[] sizes = new int[]{16, 24, 40, 80};

    private static boolean built = false;

    public static Path getDirectory(SystemIconSource source) {
        var target = DIRECTORY.resolve(source.getId());
        return target;
    }

    public static void refreshBuilt() throws IOException {
        try (var stream = Files.walk(DIRECTORY)) {
            built = stream.findAny().isPresent();
        }
    }

    public static void buildCache(Map<SystemIconSource, SystemIconSourceData> all) {
        try {
            for (var e : all.entrySet()) {
                var target = DIRECTORY.resolve(e.getKey().getId());
                Files.createDirectories(target);

                for (var icon : e.getValue().getIcons()) {
                    if (refreshChecksum(icon.getFile(),target,icon.getName(), icon.isDark())) {
                        continue;
                    }

                    rasterizeSizes(icon.getFile(),target,icon.getName(), icon.isDark());
                }
            }
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
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

    private static boolean rasterizeSizes(Path path, Path dir, String name, boolean dark) throws IOException {
        try {
            for (var size : sizes) {
                rasterize(path, dir, name, dark, size);
            }
            return true;
        } catch (Exception ex) {
            if (ex instanceof IOException) {
                throw ex;
            }

            ErrorEvent.fromThrowable(ex).omit().expected().handle();
            return false;
        }
    }

    private static void rasterize(Path path, Path dir, String name, boolean dark, int px) throws IOException {
        SVGLoader loader = new SVGLoader();
        URL svgUrl = path.toUri().toURL();
        SVGDocument svgDocument = loader.load(svgUrl);
        if (svgDocument == null) {
            return;
        }

        BufferedImage image = new BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(SVGRenderingHints.KEY_IMAGE_ANTIALIASING, SVGRenderingHints.VALUE_IMAGE_ANTIALIASING_ON);
        g.setRenderingHint(SVGRenderingHints.KEY_SOFT_CLIPPING, SVGRenderingHints.VALUE_SOFT_CLIPPING_ON);
        svgDocument.render((Component) null, g, new ViewBox(0, 0, px, px));
        g.dispose();

        var out = dir.resolve(name + "-" + px + (dark ? "-dark" : "") + ".png");
        ImageIO.write(image,"png", out.toFile());
    }
}
