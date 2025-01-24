package io.xpipe.app.resources;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.parser.SVGLoader;
import io.xpipe.app.core.AppProperties;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

public class SystemIconManager {

    private static final Path DIRECTORY = AppProperties.get().getDataDir().resolve("cache").resolve("icons").resolve("pool");

    private static final List<SystemIconSource> SOURCES = java.util.List.of(
            new SystemIconSource.GitRepository("https://github.com/selfhst/icons", "selfhst"));

    public static void init() throws Exception {
        Files.createDirectories(DIRECTORY);
        for (var source : SOURCES) {
            source.init();
            SystemIconCache.walkTree(source.getPath(), source.getId());
        }
    }

    public static Path getPoolPath() {
        return AppProperties.get().getDataDir().resolve("cache").resolve("icons").resolve("pool");
    }
}
