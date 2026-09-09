package io.xpipe.app.core;

import io.xpipe.app.ext.ExtensionException;
import io.xpipe.app.ext.ModuleLayerLoader;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.LocalExec;

import io.xpipe.app.util.ModuleAccess;
import lombok.Getter;

import java.io.IOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AppExtensionManager {

    private static AppExtensionManager INSTANCE;
    private final List<Module> loadedModules = new ArrayList<>();

    @Getter
    private final Set<String> externalModules = new HashSet<>();

    @Getter
    private FileSystem externalModuleFileSystem;

    @Getter
    private ModuleLayer extendedLayer;

    public static synchronized void init() throws Exception {
        if (INSTANCE != null) {
            return;
        }

        INSTANCE = new AppExtensionManager();
        INSTANCE.loadAllExtensions();
        try {
            ModuleLayerLoader.loadAll(INSTANCE.extendedLayer, t -> {
                ErrorEventFactory.fromThrowable(t).handle();
            });
        } catch (Throwable t) {
            throw ExtensionException.corrupt("Service provider initialization failed", t);
        }
    }

    public static void reset() {
        if (INSTANCE.externalModuleFileSystem != null) {
            try {
                INSTANCE.externalModuleFileSystem.close();
            } catch (IOException e) {
                ErrorEventFactory.fromThrowable(e).handle();
            }
            INSTANCE.externalModuleFileSystem = null;
        }

        INSTANCE = null;
    }

    public static AppExtensionManager getInstance() {
        return INSTANCE;
    }

    private static String getLocalInstallVersion(AppInstallation localInstallation) {
        var exec = localInstallation.getCliExecutablePath();
        var out = LocalExec.readStdoutIfPossible(exec.toString(), "version");
        if (out.isEmpty()) {
            throw ErrorEventFactory.expected(
                    new ExtensionException("Unable to determine version from command \"" + exec + "\" version"));
        }

        var s = out.orElseThrow().strip();
        return !s.isEmpty() ? s : "?";
    }

    public Set<Module> getContentModules() {
        return Stream.concat(
                        Stream.of(ModuleLayer.boot()
                                .findModule(AppNames.packageName())
                                .orElseThrow()),
                        loadedModules.stream())
                .collect(Collectors.toSet());
    }

    private void loadAllExtensions() throws Exception {
        var currentLayer = ModuleLayer.boot();
        for (var ext : List.of("base", "system", "proc", "uacc", "auth")) {
            var extension = findAndParseExtension(ext, currentLayer)
                    .orElseThrow(() -> ExtensionException.corrupt("Missing module " + ext));
            loadedModules.add(extension);
            currentLayer = extension.getLayer();
        }
        extendedLayer = currentLayer;

        // Fix module access for dynamically loaded modules
        // We can't use Module Controllers as they require
        if (!AppProperties.get().isFullVersion()) {
            ModuleAccess.exportAndOpen(
                    ModuleLayer.boot().findModule("java.base").orElseThrow(),
                    "java.io",
                    extendedLayer.findModule(AppNames.extModuleName("proc")).orElseThrow());
            ModuleAccess.exportAndOpen(
                    ModuleLayer.boot().findModule("org.apache.commons.io").orElseThrow(),
                    "org.apache.commons.io.input",
                    extendedLayer.findModule(AppNames.extModuleName("proc")).orElseThrow());
        }
    }

    private Optional<Module> findAndParseExtension(String name, ModuleLayer parent) {
        var inModulePath = ModuleLayer.boot().findModule(AppNames.extModuleName(name));
        if (inModulePath.isPresent()) {
            TrackEvent.info("Loaded extension " + name + " from boot module path");
            return inModulePath;
        }

        if (!AppProperties.get().isFullVersion()) {
            try {
                if (externalModuleFileSystem == null) {
                    var localInstallation =
                            AppInstallation.ofDefault(AppProperties.get().isLocatePtb());
                    Path p = localInstallation.getBaseInstallationPath();
                    if (!Files.exists(p)) {
                        throw new IllegalStateException(
                                "Required local " + AppNames.ofCurrent().getName()
                                        + " installation was not found but is required for development. See https://github"
                                        + ".com/xpipe-io/xpipe/blob/master/CONTRIBUTING.md#development-setup");
                    }

                    if (AppProperties.get().isLocatorVersionCheck()) {
                        var iv = getLocalInstallVersion(localInstallation);
                        var installVersion = AppVersion.parse(iv)
                                .orElseThrow(() -> new IllegalArgumentException("Invalid installation version: " + iv));
                        var sv = !AppProperties.get().isImage()
                                ? Files.readString(Path.of("version")).strip()
                                : AppProperties.get().getVersion();
                        var sourceVersion = AppVersion.parse(sv)
                                .orElseThrow(() -> new IllegalArgumentException("Invalid source version: " + sv));
                        if (!installVersion.equals(sourceVersion)) {
                            throw new IllegalStateException("Incompatible development version. Source: " + sv
                                    + ", Installation: "
                                    + iv
                                    + "\n\nPlease try to check out the matching release version in the repository. See https://github"
                                    + ".com/xpipe-io/xpipe/blob/master/CONTRIBUTING.md#development-setup");
                        }
                    }

                    externalModuleFileSystem = FileSystems.newFileSystem(
                            URI.create("jrt:/"),
                            Map.of(
                                    "java.home",
                                    localInstallation.getRuntimePath().toString()));
                }

                var moduleName = "io.xpipe.ext." + name;
                var basePath = externalModuleFileSystem.getPath("modules", moduleName);
                var found = parseExtensionDirectory(basePath, parent);
                if (found.isPresent()) {
                    externalModules.add(moduleName);
                    TrackEvent.info("Loaded extension " + name + " from module " + basePath);
                    return found;
                }
            } catch (Throwable t) {
                ErrorEventFactory.fromThrowable(t).handle();
            }
        }

        TrackEvent.info("Unable to locate module " + name);
        return Optional.empty();
    }

    private Optional<Module> parseExtensionDirectory(Path dir, ModuleLayer parent) {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return Optional.empty();
        }

        TrackEvent.trace(String.format("Scanning directory %s for extensions", dir));
        try {
            ModuleFinder finder = ModuleFinder.of(dir);
            var found = finder.findAll();
            var hasModules = !found.isEmpty();

            TrackEvent.withTrace("Found modules").elements(found).handle();

            if (hasModules) {
                Configuration cf = parent.configuration()
                        .resolve(
                                finder,
                                ModuleFinder.of(),
                                found.stream().map(r -> r.descriptor().name()).collect(Collectors.toSet()));
                ClassLoader scl = ClassLoader.getSystemClassLoader();
                var controller = ModuleLayer.defineModulesWithOneLoader(cf, List.of(parent), scl);
                var layer = controller.layer();
                var mod = layer.modules().iterator().next();
                return Optional.of(mod);
            }
        } catch (Throwable t) {
            ErrorEventFactory.fromThrowable(t)
                    .description("Unable to load extension from " + dir + ". Is the installation corrupted?")
                    .handle();
        }
        return Optional.empty();
    }
}
