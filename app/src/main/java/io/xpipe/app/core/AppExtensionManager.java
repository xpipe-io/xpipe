package io.xpipe.app.core;

import io.xpipe.app.exchange.MessageExchangeImpls;
import io.xpipe.app.ext.ModuleInstall;
import io.xpipe.app.ext.XPipeServiceProviders;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.util.ModuleHelper;
import io.xpipe.core.util.XPipeInstallation;
import lombok.Value;

import java.io.IOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AppExtensionManager {

    private static AppExtensionManager INSTANCE;
    private final List<Extension> loadedExtensions = new ArrayList<>();
    private final List<ModuleLayer> leafModuleLayers = new ArrayList<>();
    private final List<Path> extensionBaseDirectories = new ArrayList<>();
    private ModuleLayer baseLayer = ModuleLayer.boot();
    private ModuleLayer extendedLayer;

    public static void init() throws Exception {
        if (INSTANCE != null) {
            return;
        }

        INSTANCE = new AppExtensionManager();
        INSTANCE.determineExtensionDirectories();
        INSTANCE.loadBasicExtension();
        INSTANCE.loadExtensions();
        INSTANCE.loadContent();
    }

    private void loadBasicExtension() {
        var baseModule = loadExtension("base", ModuleLayer.boot());
        if (baseModule.isEmpty()) {
            throw new IllegalStateException("Missing base module");
        }

        baseLayer = baseModule.get().getModule().getLayer();
        loadedExtensions.add(baseModule.get());
    }

    public static ModuleLayer initBare() {
        if (INSTANCE != null) {
            return INSTANCE.extendedLayer;
        }

        INSTANCE = new AppExtensionManager();
        INSTANCE.determineExtensionDirectories();
        INSTANCE.loadBasicExtension();
        var proc = INSTANCE.loadExtension("proc", INSTANCE.baseLayer).orElseThrow();
        var procx = INSTANCE.loadExtension("procx", proc.getModule().getLayer()).orElseThrow();
        INSTANCE.leafModuleLayers.add(procx.getModule().getLayer());
        INSTANCE.extendedLayer = procx.getModule().getLayer();
        return INSTANCE.extendedLayer;
    }

    private void determineExtensionDirectories() {
        if (!AppProperties.get().isImage()) {
            extensionBaseDirectories.add(Path.of(System.getProperty("user.dir")).resolve("app").resolve("build").resolve("ext_dev"));
        }

        if (!AppProperties.get().isFullVersion()) {
            var localInstallation = XPipeInstallation.getLocalDefaultInstallationBasePath(true);
            Path p = Path.of(localInstallation);
            if (!Files.exists(p)) {
                throw new IllegalStateException("Required local X-Pipe installation not found");
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

    public static void reset() {
        INSTANCE = null;
    }

    public static AppExtensionManager getInstance() {
        return INSTANCE;
    }

    public Set<Module> getContentModules() {
        return Stream.concat(
                        Stream.of(ModuleLayer.boot().findModule("io.xpipe.app").orElseThrow()),
                        loadedExtensions.stream().map(extension -> extension.module))
                .collect(Collectors.toSet());
    }

    public boolean isInstalled(ModuleInstall install) {
        var target =
                AppExtensionManager.getInstance().getGeneratedModulesDirectory(install.getModule(), install.getId());
        return Files.exists(target) && Files.isRegularFile(target.resolve("finished"));
    }

    public void installIfNeeded(ModuleInstall install) throws Exception {
        var target =
                AppExtensionManager.getInstance().getGeneratedModulesDirectory(install.getModule(), install.getId());
        if (Files.exists(target) && Files.isRegularFile(target.resolve("finished"))) {
            return;
        }

        Files.createDirectories(target);
        install.installInternal(target);
        Files.createFile(target.resolve("finished"));
    }

    public Path getGeneratedModulesDirectory(String module, String ext) {
        var base = AppProperties.get()
                .getDataDir()
                .resolve("generated_extensions")
                .resolve(AppProperties.get().getVersion())
                .resolve(module);
        return ext != null ? base.resolve(ext) : base;
    }

    private void loadExtensions() throws IOException {
        for (Path extensionBaseDirectory : extensionBaseDirectories) {
            loadExtensionBaseDirectory(extensionBaseDirectory);
        }

        if (leafModuleLayers.size() > 0) {
            var scl = ClassLoader.getSystemClassLoader();
            var cfs = leafModuleLayers.stream().map(ModuleLayer::configuration).toList();
            var finder = ModuleFinder.ofSystem();
            var cf = Configuration.resolve(finder, cfs, finder, List.of());
            extendedLayer = ModuleLayer.defineModulesWithOneLoader(cf, leafModuleLayers, scl)
                    .layer();
        } else {
            extendedLayer = baseLayer;
        }
    }

    private void loadContent() throws IOException {
        addNativeLibrariesToPath();
        XPipeServiceProviders.load(extendedLayer);
        MessageExchangeImpls.loadAll();
    }

    private void loadExtensionBaseDirectory(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }

        try (var s = Files.list(dir)) {
            s.forEach(sub -> {
                if (Files.isDirectory(sub)) {
                    var extension = loadExtensionDirectory(sub, baseLayer);
                    if (extension.isEmpty()) {
                        return;
                    }
                    loadedExtensions.add(extension.get());

                    var xModule = loadExtension(
                            extension.get().getId() + "x",
                            extension.get().getModule().getLayer());
                    if (xModule.isPresent()) {
                        loadedExtensions.add(xModule.get());
                        leafModuleLayers.add(xModule.get().getModule().getLayer());
                    } else {
                        leafModuleLayers.add(extension.get().getModule().getLayer());
                    }
                }
            });
        } catch (IOException ex) {
            ErrorEvent.fromThrowable(ex).handle();
        }
    }

    private Optional<Extension> loadExtension(String name, ModuleLayer parent) {
        for (Path extensionBaseDirectory : extensionBaseDirectories) {
            var found = loadExtensionDirectory(extensionBaseDirectory.resolve(name), parent);
            if (found.isPresent()) {
                return found;
            }
        }

        return Optional.empty();
    }

    private Optional<Extension> loadExtensionDirectory(Path dir, ModuleLayer parent) {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return Optional.empty();
        }

        if (loadedExtensions.stream().anyMatch(extension -> extension.dir.equals(dir))
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

                var ext = getExtension(layer, dir);
                if (ext.isEmpty()) {
                    TrackEvent.withWarn("Found extension directory with no extension")
                            .tag("dir", dir.toString())
                            .handle();
                } else {
                    if (loadedExtensions.stream()
                            .anyMatch(extension -> extension.getName().equals(ext.get().name))) {
                        return Optional.empty();
                    }

                    ext.get().getModule().getPackages().forEach(pkg -> {
                        ModuleHelper.exportAndOpen(pkg, ext.get().getModule());
                    });

                    TrackEvent.withInfo("Loaded extension module")
                            .tag("name", ext.get().getName())
                            .tag("dir", dir.toString())
                            .tag("dependencies", ext.get().getDependencies())
                            .handle();

                    return Optional.of(ext.get());
                }
            }
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t)
                    .description("Unable to load extension " + dir.getFileName().toString())
                    .handle();
        }
        return Optional.empty();
    }

    private ModuleLayer loadGenerated(ModuleLayer parent, Path dir) throws IOException {
        try (var d = Files.list(dir)) {
            var all = d.toArray(Path[]::new);
            ModuleFinder finder = ModuleFinder.of(all);
            var found = finder.findAll();

            Configuration cf = parent.configuration()
                    .resolve(
                            finder,
                            ModuleFinder.of(),
                            found.stream().map(r -> r.descriptor().name()).collect(Collectors.toSet()));
            ClassLoader scl = ClassLoader.getSystemClassLoader();
            var layer = ModuleLayer.defineModulesWithOneLoader(cf, List.of(parent), scl)
                    .layer();
            return layer;
        }
    }

    private Optional<Extension> getExtension(ModuleLayer l, Path dir) {
        return l.modules().stream()
                .map(m -> {
                    AtomicReference<Extension> ext = new AtomicReference<>();
                    AppResources.withResource(m.getName(), "extension.properties", l, path -> {
                        if (Files.exists(path)) {
                            var props = new Properties();
                            try (var in = Files.newInputStream(path)) {
                                props.load(in);
                            }
                            var name = props.get("name").toString();
                            var deps = l.modules().size() - 1;
                            ext.set(new Extension(dir, dir.getFileName().toString(),name, m, deps));
                        }
                    });
                    return Optional.ofNullable(ext.get());
                })
                .flatMap(Optional::stream)
                .findFirst();
    }

    private void addNativeLibrariesToPath() throws IOException {
        var libsDir =
                AppProperties.get().isImage() ? XPipeInstallation.getLocalDynamicLibraryDirectory() : Path.of("lib");
        if (Files.exists(libsDir)) {
            // FileUtils.deleteDirectory(libsDir.toFile());
        } else {
            Files.createDirectories(libsDir);
        }

        for (var ext : loadedExtensions) {
            AppResources.withResource(ext.getModule().getName(), "lib", extendedLayer, path -> {
                if (Files.exists(path)) {
                    Files.list(path).forEach(lib -> {
                        try {
                            var target = libsDir.resolve(lib.getFileName().toString());
                            if (!Files.exists(target)) {
                                Files.copy(lib, target);
                            }
                        } catch (IOException e) {
                            ErrorEvent.fromThrowable(e).handle();
                        }
                    });
                }
            });
        }
    }

    public ModuleLayer getExtendedLayer() {
        return extendedLayer;
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
