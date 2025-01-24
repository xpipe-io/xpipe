package io.xpipe.app.resources;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.parser.SVGLoader;
import io.xpipe.app.core.AppProperties;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

public class SystemIconCache {

    private static final Path DIRECTORY = AppProperties.get().getDataDir().resolve("cache").resolve("icons").resolve("raster");
    private static final int[] sizes = new int[]{16, 24, 40, 80};

    public static void init() throws Exception {
        Files.createDirectories(DIRECTORY);
        walkTree(Path.of("C:\\Projects\\xpipe\\xpipex\\img\\selfhst"),"selfhst");
    }

    public static void walkTree(Path dir, String source) throws Exception {
        var target = DIRECTORY.resolve(source);
        Files.createDirectories(target);

        var files = Files.walk(dir, FileVisitOption.FOLLOW_LINKS).toList();
        for (var file : files) {
            if (file.getFileName().toString().endsWith(".svg")) {
                rasterize(file, source);
            }
        }
    }

    private static void rasterize(Path path, String source) throws Exception {
        var name = FilenameUtils.getBaseName(path.getFileName().toString());
        var dark = !name.endsWith("-light");
        var md5Name = name.replaceFirst("-light$", "") + (dark ? "-dark" : "");
        var bytes = Files.readAllBytes(path);
        var md = MessageDigest.getInstance("MD5");
        md.update(bytes);
        var digest = md.digest();
        var dir = DIRECTORY.resolve(source);
        var md5File = dir.resolve(md5Name);
        if (Files.exists(md5File) && Arrays.equals(Files.readAllBytes(md5File), digest)) {
            return;
        } else {
            Files.write(md5File, digest);
        }

        for (var size : sizes) {
            rasterize(path, source, size);
        }
    }

    private static void rasterize(Path path, String source, int px) throws IOException {
        var name = FilenameUtils.getBaseName(path.getFileName().toString());
        var dark = !name.endsWith("-light");
        var fixedName = name.replaceFirst("-light$", "");
        rasterize(path, source,fixedName, px, dark);
    }

    private static void rasterize(Path path, String source, String name, int px, boolean dark) throws IOException {
        SVGLoader loader = new SVGLoader();
        URL svgUrl = path.toUri().toURL();
        SVGDocument svgDocument = loader.load(svgUrl);
        if (svgDocument == null) {
            return;
        }

        BufferedImage image = new BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        svgDocument.render(null, g);
        g.dispose();

        var dir = DIRECTORY.resolve(source);
        Files.createDirectories(dir);
        var out = dir.resolve(name + "-" + px + (dark ? "-dark" : "") + ".png");
        ImageIO.write(image,"png", out.toFile());
    }
}
