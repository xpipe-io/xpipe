package io.xpipe.app.core;

import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.extension.event.ErrorEvent;
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

public class AppResources {

    public static final String XPIPE_MODULE = "io.xpipe.app";

    private static ModuleFileSystem openFileSystem(String module) throws IOException {
        var layer = AppExtensionManager.getInstance() != null
                ? AppExtensionManager.getInstance().getExtendedLayer()
                : null;
        if (layer == null) {
            layer = ModuleLayer.boot();
        }
        return (ModuleFileSystem) FileSystems.newFileSystem(URI.create("module:/" + module), Map.of("layer", layer));
    }

    public static Optional<URL> getResourceURL(String module, String file) {
        try (var fs = openFileSystem(module)) {
            var f = fs.getPath(module.replace('.', '/') + "/resources/" + file);
            var url = f.getWrappedPath().toUri().toURL();
            return Optional.of(url);
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).omitted(true).build().handle();
            return Optional.empty();
        }
    }

    public static void with(String module, String file, Charsetter.FailableConsumer<Path, IOException> con) {
        if (AppProperties.get() != null
                && !AppProperties.get().isImage()
                && AppProperties.get().isDeveloperMode()) {
            // Check if resource was found. If we use external processed resources, we can't use local dev resources
            if (withLocalDevResource(module, file, con)) {
                return;
            }
        }

        withResource(module, file, con);
    }

    public static void withResource(
            String module, String file, ModuleLayer layer, Charsetter.FailableConsumer<Path, IOException> con) {
        try (var fs = FileSystems.newFileSystem(URI.create("module:/" + module), Map.of("layer", layer))) {
            var f = fs.getPath(module.replace('.', '/') + "/resources/" + file);
            con.accept(f);
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).omitted(true).build().handle();
        }
    }

    private static void withResource(String module, String file, Charsetter.FailableConsumer<Path, IOException> con) {
        try (var fs = openFileSystem(module)) {
            var f = fs.getPath(module.replace('.', '/') + "/resources/" + file);
            con.accept(f);
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).omitted(true).build().handle();
        }
    }

    private static boolean withLocalDevResource(
            String module, String file, Charsetter.FailableConsumer<Path, IOException> con) {
        try (var fs = openFileSystem(module)) {
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
            ErrorEvent.fromThrowable(e).omitted(true).build().handle();
        }
        return true;
    }
}
