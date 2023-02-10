package io.xpipe.app.core;

import io.xpipe.app.exchange.MessageExchangeImpls;
import io.xpipe.core.util.ModuleHelper;
import io.xpipe.core.util.XPipeInstallation;
import io.xpipe.extension.ModuleInstall;
import io.xpipe.extension.XPipeServiceProviders;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.event.TrackEvent;
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
    private ModuleLayer baseLayer;
    private ModuleLayer extendedLayer;

    public static void init() throws Exception {
        if (INSTANCE != null) {
            return;
        }

        INSTANCE = new AppExtensionManager();
        INSTANCE.load();
    }

    public static void initBare() {
        if (INSTANCE != null) {
            return;
        }

        INSTANCE = new AppExtensionManager();
        INSTANCE.extendedLayer = ModuleLayer.boot();
    }

    public static ModuleLayer loadOnlyBundledExtension(String name) {
        INSTANCE = new AppExtensionManager();
        INSTANCE.baseLayer = INSTANCE.loadBundledExtension("base");
        return INSTANCE.loadBundledExtension(name);
    }

    public ModuleLayer loadBundledExtension(String name) {
        var productionRoot = XPipeInstallation.getLocalExtensionsDirectory();
        var userDir = AppProperties.get().isImage()
                ? productionRoot.resolve(name)
                : Path.of(System.getProperty("user.dir"))
                        .resolve("ext")
                        .resolve(name)
                        .resolve("build")
                        .resolve("libs_dev");
        var layer = loadDirectory(userDir);
        return layer.size() > 0 ? layer.get(0) : null;
    }

    public static void reset() {
        INSTANCE = null;
    }

    public static AppExtensionManager getInstance() {
        return INSTANCE;
    }

    public Set<Module> getContentModules() {
        return Stream.concat(
                        Stream.of(
                                ModuleLayer.boot().findModule("io.xpipe.app").orElseThrow(),
                                ModuleLayer.boot()
                                        .findModule("io.xpipe.extension")
                                        .orElseThrow()),
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

    private List<ModuleLayer> loadDirectory(Path dir) {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return List.of();
        }

        if (loadedExtensions.stream().anyMatch(extension -> extension.dir.equals(dir))) {
            return List.of();
        }

        var layers = new ArrayList<ModuleLayer>();
        try (var s = Files.list(dir)) {
            s.forEach(sub -> {
                if (Files.isDirectory(sub)) {
                    layers.addAll(loadDirectory(sub));
                }
            });
        } catch (IOException ex) {
            ErrorEvent.fromThrowable(ex).handle();
        }

        TrackEvent.trace(String.format("Scanning directory %s for extensions", dir));

        try {
            ModuleFinder finder = ModuleFinder.of(dir);
            var found = finder.findAll();
            var hasModules = found.size() > 0;

            TrackEvent.withTrace("Found modules").elements(found).handle();

            if (hasModules) {
                ModuleLayer parent = baseLayer != null ? baseLayer : ModuleLayer.boot();
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
                    ext.get().getModule().getPackages().forEach(pkg -> {
                        ModuleHelper.exportAndOpen(pkg, ext.get().getModule());
                    });

                    layers.add(layer);
                    loadedExtensions.add(ext.get());

                    TrackEvent.withInfo("Loaded extension module")
                            .tag("name", ext.get().getName())
                            .tag("dir", dir.toString())
                            .tag("dependencies", ext.get().getDependencies())
                            .handle();

                    var gen = getGeneratedModulesDirectory(ext.get().getModule().getName(), null);
                    if (Files.exists(gen)) {
                        var genLayer = loadGenerated(layer, gen);
                        layers.add(genLayer);

                        TrackEvent.withTrace("Found generated modules")
                                .elements(genLayer.modules())
                                .handle();
                    }
                }
            }
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t)
                    .description("Unable to load extension " + dir.getFileName().toString())
                    .handle();
        }

        return layers;
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
                            ext.set(new Extension(dir, name, m, deps));
                        }
                    });
                    return Optional.ofNullable(ext.get());
                })
                .flatMap(Optional::stream)
                .findFirst();
    }

    private void load() throws IOException {
        baseLayer = loadBundledExtension("base");

        var ep = new ArrayList<>(AppProperties.get().getExtensionPaths());
        List<ModuleLayer> extended = new ArrayList<>();
        for (var p : ep) {
            extended.addAll(loadDirectory(p));
        }

        var userDir = AppProperties.get().getDataDir().resolve("extensions");
        var userExtensions = loadDirectory(userDir);
        extended.addAll(userExtensions);

        if (AppProperties.get().isImage()) {
            var bundledDir = XPipeInstallation.getLocalExtensionsDirectory();
            var bundledExtensions = loadDirectory(bundledDir);
            extended.addAll(bundledExtensions);
        }

        if (extended.size() > 0) {
            var scl = ClassLoader.getSystemClassLoader();
            var cfs = extended.stream().map(ModuleLayer::configuration).toList();
            var finder = ModuleFinder.ofSystem();
            var cf = Configuration.resolve(finder, cfs, finder, List.of());
            extendedLayer =
                    ModuleLayer.defineModulesWithOneLoader(cf, extended, scl).layer();
        } else {
            extendedLayer = ModuleLayer.boot();
        }

        addNativeLibrariesToPath();

        XPipeServiceProviders.load(extendedLayer);
        MessageExchangeImpls.loadAll();
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
        String name;
        Module module;
        int dependencies;
    }
}
