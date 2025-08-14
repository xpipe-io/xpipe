package io.xpipe.app.core;

import io.xpipe.app.ext.ExtensionException;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.LocalExec;
import io.xpipe.app.util.ModuleAccess;
import io.xpipe.core.ModuleLayerLoader;

import lombok.Getter;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AppExtensionManager {

    private static AppExtensionManager INSTANCE;
    private final List<Module> loadedModules = new ArrayList<>();
    private final List<ModuleLayer> leafModuleLayers = new ArrayList<>();
    private final List<Path> extensionBaseDirectories = new ArrayList<>();
    private ModuleLayer baseLayer = ModuleLayer.boot();

    @Getter
    private ModuleLayer extendedLayer;

    public static synchronized void init() throws Exception {
        if (INSTANCE != null) {
            return;
        }

        INSTANCE = new AppExtensionManager();
        INSTANCE.determineExtensionDirectories();
        INSTANCE.loadBaseExtension();
        INSTANCE.loadAllExtensions();
        try {
            ProcessControlProvider.init(INSTANCE.extendedLayer);
            ModuleLayerLoader.loadAll(INSTANCE.extendedLayer, t -> {
                ErrorEventFactory.fromThrowable(t).handle();
            });
        } catch (Throwable t) {
            throw ExtensionException.corrupt("Service provider initialization failed", t);
        }
    }

    public static void reset() {
        INSTANCE = null;
    }

    public static AppExtensionManager getInstance() {
        return INSTANCE;
    }

    private static String getLocalInstallVersion(AppInstallation localInstallation) throws Exception {
        var exec = localInstallation.getDaemonExecutablePath();
        var out = LocalExec.readStdoutIfPossible(exec.toString(), "version");
        return out.orElseThrow().strip();
    }

    private void loadBaseExtension() {
        var baseModule = findAndParseExtension("base", ModuleLayer.boot());
        if (baseModule.isEmpty()) {
            throw ExtensionException.corrupt("Missing base module");
        }

        baseLayer = baseModule.get().getLayer();
        loadedModules.add(baseModule.get());
    }

    private void determineExtensionDirectories() throws Exception {
        if (!AppProperties.get().isFullVersion()) {
            var localInstallation =
                    !AppProperties.get().isStaging() && AppProperties.get().isLocatePtb()
                            ? AppInstallation.ofDefault(true)
                            : AppInstallation.ofCurrent();
            Path p = localInstallation.getBaseInstallationPath();
            if (!Files.exists(p)) {
                throw new IllegalStateException(
                        "Required local XPipe installation was not found but is required for development. See https://github"
                                + ".com/xpipe-io/xpipe/blob/master/CONTRIBUTING.md#development-setup");
            }

            var iv = getLocalInstallVersion(localInstallation);
            var installVersion = AppVersion.parse(iv)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid installation version: " + iv));
            var sv = !AppProperties.get().isImage()
                    ? Files.readString(Path.of("version")).strip()
                    : AppProperties.get().getVersion();
            var sourceVersion = AppVersion.parse(sv)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid source version: " + sv));
            if (AppProperties.get().isLocatorVersionCheck() && !installVersion.equals(sourceVersion)) {
                throw new IllegalStateException("Incompatible development version. Source: " + sv
                        + ", Installation: "
                        + iv
                        + "\n\nPlease try to check out the matching release version in the repository. See https://github"
                        + ".com/xpipe-io/xpipe/blob/master/CONTRIBUTING.md#development-setup");
            }

            var extensions = localInstallation.getExtensionsPath();
            extensionBaseDirectories.add(extensions);
        }
    }

    public Set<Module> getContentModules() {
        return Stream.concat(
                        Stream.of(ModuleLayer.boot()
                                .findModule(AppNames.packageName(null))
                                .orElseThrow()),
                        loadedModules.stream())
                .collect(Collectors.toSet());
    }

    private void loadAllExtensions() throws Exception {
        for (var ext : List.of("system", "proc", "uacc")) {
            var extension = findAndParseExtension(ext, baseLayer)
                    .orElseThrow(() -> ExtensionException.corrupt("Missing module " + ext));
            loadedModules.add(extension);
            leafModuleLayers.add(extension.getLayer());
        }

        var scl = ClassLoader.getSystemClassLoader();
        var cfs = leafModuleLayers.stream().map(ModuleLayer::configuration).toList();
        var finder = ModuleFinder.ofSystem();
        var cf = Configuration.resolve(finder, cfs, finder, List.of());
        extendedLayer = ModuleLayer.defineModulesWithOneLoader(cf, leafModuleLayers, scl)
                .layer();

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

        for (Path extensionBaseDirectory : extensionBaseDirectories) {
            var extensionDir = extensionBaseDirectory.resolve(name);
            var found = parseExtensionDirectory(extensionDir, parent);
            if (found.isPresent()) {
                TrackEvent.info("Loaded extension " + name + " from module " + extensionDir);
                return found;
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
            var hasModules = found.size() > 0;

            TrackEvent.withTrace("Found modules").elements(found).handle();

            if (hasModules) {
                Configuration cf = parent.configuration()
                        .resolve(
                                finder,
                                ModuleFinder.of(),
                                found.stream().map(r -> r.descriptor().name()).collect(Collectors.toSet()));
                ClassLoader scl = ClassLoader.getSystemClassLoader();
                var layer = ModuleLayer.defineModulesWithOneLoader(cf, List.of(parent), scl)
                        .layer();
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
