package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.core.FailableConsumer;
import io.xpipe.modulefs.ModuleFileSystem;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AppResources {

    public static final String MAIN_MODULE = "io." + AppNames.ofMain().getSnakeName() + ".app";

    private static final Map<String, ModuleFileSystem> fileSystems = new ConcurrentHashMap<>();

    public static void reset() {
        fileSystems.forEach((s, moduleFileSystem) -> {
            try {
                moduleFileSystem.close();
            } catch (IOException ex) {
                // Usually when updating, an exit signal is sent to this application.
                // However, it takes a while to shut down but the installer is deleting files meanwhile.
                // It can happen that the jar does not exist anymore
                ErrorEventFactory.fromThrowable(ex).expected().omit().handle();
            }
        });
        fileSystems.clear();
    }

    private static ModuleFileSystem openFileSystemIfNeeded(String module) throws IOException {
        var layer = AppExtensionManager.getInstance() != null
                ? AppExtensionManager.getInstance().getExtendedLayer()
                : null;

        // Only cache file systems with extended layer
        if (layer != null && fileSystems.containsKey(module)) {
            return fileSystems.get(module);
        }

        if (layer == null) {
            layer = ModuleLayer.boot();
        }

        var fs = (ModuleFileSystem) FileSystems.newFileSystem(URI.create("module:/" + module), Map.of("layer", layer));
        if (AppExtensionManager.getInstance() != null) {
            fileSystems.put(module, fs);
        }
        return fs;
    }

    public static Optional<URL> getResourceURL(String module, String file) {
        try {
            var fs = openFileSystemIfNeeded(module);
            var f = fs.getPath(module.replace('.', '/') + "/resources/" + file);
            var url = f.getWrappedPath().toUri().toURL();
            return Optional.of(url);
        } catch (IOException e) {
            ErrorEventFactory.fromThrowable(e).omitted(true).build().handle();
            return Optional.empty();
        }
    }

    public static void with(String module, String file, FailableConsumer<Path, IOException> con) {
        if (AppProperties.get() != null && AppProperties.get().isDevelopmentEnvironment()) {
            // Check if resource was found. If we use external processed resources, we can't use local dev resources
            if (withLocalDevResource(module, file, con)) {
                return;
            }
        }

        withResource(module, file, con);
    }

    private static void withResource(String module, String file, FailableConsumer<Path, IOException> con) {
        var path = module.startsWith(AppNames.ofCurrent().getGroupName()) ? module.replace('.', '/') + "/resources/" + file : file;
        try {
            var fs = openFileSystemIfNeeded(module);
            var f = fs.getPath(path);
            con.accept(f);
        } catch (IOException e) {
            ErrorEventFactory.fromThrowable(e).omitted(true).build().handle();
        }
    }

    private static boolean withLocalDevResource(String module, String file, FailableConsumer<Path, IOException> con) {
        try {
            var fs = openFileSystemIfNeeded(module);
            var url = fs.getPath("").getWrappedPath().toUri().toURL();
            if (!url.getProtocol().equals("jar")) {
                return false;
            }

            JarURLConnection connection = (JarURLConnection) url.openConnection();
            URL fileUrl = connection.getJarFileURL();
            var jarFile = Path.of(fileUrl.toURI());
            var resDir = jarFile.getParent()
                    .getParent()
                    .getParent()
                    .resolve("src")
                    .resolve("main")
                    .resolve("resources");
            var f = resDir.resolve(module.replace('.', '/') + "/resources/" + file);
            if (!Files.exists(f)) {
                return false;
            }

            con.accept(f);
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).omitted(true).build().handle();
        }
        return true;
    }
}
