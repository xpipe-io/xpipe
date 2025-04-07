package io.xpipe.app.core;

import io.xpipe.app.ext.ExtensionException;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.resources.AppResources;
import io.xpipe.core.process.OsType;
import io.xpipe.core.util.ModuleLayerLoader;
import io.xpipe.core.util.XPipeInstallation;

import lombok.Getter;
import lombok.Value;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AppExtensionManager {

    private static AppExtensionManager INSTANCE;
    private final List<Extension> loadedExtensions = new ArrayList<>();
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
                ErrorEvent.fromThrowable(t).handle();
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

    private void loadBaseExtension() {
        var baseModule = findAndParseExtension("base", ModuleLayer.boot());
        if (baseModule.isEmpty()) {
            throw ExtensionException.corrupt("Missing base module");
        }

        baseLayer = baseModule.get().getModule().getLayer();
        loadedExtensions.add(baseModule.get());
    }

    private void determineExtensionDirectories() throws Exception {
        if (!AppProperties.get().isImage()) {
            extensionBaseDirectories.add(Path.of(System.getProperty("user.dir"))
                    .resolve("app")
                    .resolve("build")
                    .resolve("ext_dev"));
        }

        if (!AppProperties.get().isFullVersion()) {
            var localInstallation = XPipeInstallation.getLocalDefaultInstallationBasePath(
                    AppProperties.get().isStaging() || AppProperties.get().isLocatePtb());
            Path p = Path.of(localInstallation);
            if (!Files.exists(p)) {
                throw new IllegalStateException(
                        "Required local XPipe installation was not found but is required for development. See https://github.com/xpipe-io/xpipe/blob/master/CONTRIBUTING.md#development-setup");
            }

            var iv = getLocalInstallVersion();
            var installVersion = AppVersion.parse(iv)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid installation version: " + iv));
            var sv = !AppProperties.get().isImage()
                    ? Files.readString(Path.of("version")).trim()
                    : AppProperties.get().getVersion();
            var sourceVersion = AppVersion.parse(sv)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid source version: " + sv));
            if (AppProperties.get().isLocatorVersionCheck() && !installVersion.equals(sourceVersion)) {
                throw new IllegalStateException(
                        "Incompatible development version. Source: " + sv + ", Installation: " + iv
                                + "\n\nPlease try to check out the matching release version in the repository. See https://github.com/xpipe-io/xpipe/blob/master/CONTRIBUTING.md#development-setup");
            }

            var extensions = XPipeInstallation.getLocalExtensionsDirectory(p);
            extensionBaseDirectories.add(extensions);
        }

        var userDir = AppProperties.get().getDataDir().resolve("extensions");
        extensionBaseDirectories.add(userDir);

        var currentInstallation = XPipeInstallation.getCurrentInstallationBasePath();
        var productionRoot = XPipeInstallation.getLocalExtensionsDirectory(currentInstallation);
        extensionBaseDirectories.add(productionRoot);
    }

    private static String getLocalInstallVersion() throws Exception {
        var localInstallation = XPipeInstallation.getLocalDefaultInstallationBasePath();
        var exec = Path.of(localInstallation, XPipeInstallation.getDaemonExecutablePath(OsType.getLocal()));
        var fc = new ProcessBuilder(exec.toString(), "version").redirectError(ProcessBuilder.Redirect.DISCARD);
        var proc = fc.start();
        var out = new String(proc.getInputStream().readAllBytes());
        proc.waitFor(1, TimeUnit.SECONDS);
        return out.trim();
    }

    public Set<Module> getContentModules() {
        return Stream.concat(
                        Stream.of(ModuleLayer.boot().findModule("io.xpipe.app").orElseThrow()),
                        loadedExtensions.stream().map(extension -> extension.module))
                .collect(Collectors.toSet());
    }

    private void loadAllExtensions() {
        for (var ext : List.of("system", "proc", "uacc")) {
            var extension = findAndParseExtension(ext, baseLayer)
                    .orElseThrow(() -> ExtensionException.corrupt("Missing module " + ext));
            loadedExtensions.add(extension);
            leafModuleLayers.add(extension.getModule().getLayer());
        }

        var scl = ClassLoader.getSystemClassLoader();
        var cfs = leafModuleLayers.stream().map(ModuleLayer::configuration).toList();
        var finder = ModuleFinder.ofSystem();
        var cf = Configuration.resolve(finder, cfs, finder, List.of());
        extendedLayer = ModuleLayer.defineModulesWithOneLoader(cf, leafModuleLayers, scl)
                .layer();
    }

    private Optional<Extension> findAndParseExtension(String name, ModuleLayer parent) {
        var inModulePath = ModuleLayer.boot().findModule("io.xpipe.ext." + name);
        if (inModulePath.isPresent()) {
            TrackEvent.info("Loaded extension " + name + " from boot module path");
            return Optional.of(new Extension(null, inModulePath.get().getName(), name, inModulePath.get(), 0));
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

    private Optional<Extension> parseExtensionDirectory(Path dir, ModuleLayer parent) {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return Optional.empty();
        }

        if (loadedExtensions.stream().anyMatch(extension -> dir.equals(extension.dir))
                || loadedExtensions.stream()
                        .anyMatch(extension ->
                                extension.id.equals(dir.getFileName().toString()))) {
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

                var ext = getExtensionFromDir(layer, dir);
                if (ext.isEmpty()) {
                    if (AppProperties.get().isFullVersion()) {
                        throw ExtensionException.corrupt("Unable to load extension from directory " + dir);
                    }
                } else {
                    if (loadedExtensions.stream()
                            .anyMatch(extension -> extension.getName().equals(ext.get().name))) {
                        return Optional.empty();
                    }

                    TrackEvent.withInfo("Loaded extension module")
                            .tag("name", ext.get().getName())
                            .tag("dir", dir.toString())
                            .tag("dependencies", ext.get().getDependencies())
                            .handle();

                    return ext;
                }
            }
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t)
                    .description("Unable to load extension from " + dir + ". Is the installation corrupted?")
                    .handle();
        }
        return Optional.empty();
    }

    private Optional<Extension> getExtensionFromDir(ModuleLayer l, Path dir) {
        return l.modules().stream()
                .map(m -> {
                    AtomicReference<Extension> ext = new AtomicReference<>();
                    AppResources.withResourceInLayer(m.getName(), "extension.properties", l, path -> {
                        if (Files.exists(path)) {
                            var props = new Properties();
                            try (var in = Files.newInputStream(path)) {
                                props.load(in);
                            }
                            var name = props.get("name").toString();
                            var deps = l.modules().size() - 1;
                            ext.set(new Extension(dir, dir.getFileName().toString(), name, m, deps));
                        }
                    });
                    return Optional.ofNullable(ext.get());
                })
                .flatMap(Optional::stream)
                .findFirst();
    }

    @Value
    private static class Extension {
        Path dir;
        String id;
        String name;
        Module module;
        int dependencies;
    }
}
