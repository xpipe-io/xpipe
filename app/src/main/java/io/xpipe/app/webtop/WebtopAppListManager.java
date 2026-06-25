package io.xpipe.app.webtop;

import io.xpipe.app.core.AppInstallation;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.update.AppDistributionType;
import io.xpipe.app.util.GlobalTimer;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class WebtopAppListManager {

    private static WebtopAppListManager INSTANCE;

    public static WebtopAppListManager get() {
        return INSTANCE;
    }

    @SneakyThrows
    public static void init() {
        if (AppDistributionType.get() != AppDistributionType.WEBTOP) {
            return;
        }

        var m = new WebtopAppListManager();
        m.load();
        INSTANCE = m;
        INSTANCE.showDialogIfNeeded();

        AppPrefs.get().passwordManager().addListener((observable, oldValue, newValue) -> {
            INSTANCE.showDialogIfNeeded();
        });

        AppPrefs.get().externalEditor().addListener((observable, oldValue, newValue) -> {
            INSTANCE.showDialogIfNeeded();
        });

        AppPrefs.get().terminalType().addListener((observable, oldValue, newValue) -> {
            INSTANCE.showDialogIfNeeded();
        });

        AppPrefs.get().terminalMultiplexer().addListener((observable, oldValue, newValue) -> {
            INSTANCE.showDialogIfNeeded();
        });

        GlobalTimer.scheduleUntil(Duration.ofSeconds(10), false, () -> {
            try {
                INSTANCE.refreshInstalled();
                return false;
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).handle();
                return true;
            }
        });
    }

    private final Set<WebtopApp> available = new LinkedHashSet<>();
    private final Set<WebtopApp> installed = new LinkedHashSet<>();
    private final Set<WebtopApp> selected = new LinkedHashSet<>();

    public boolean showDialogIfNeeded(WebtopApp app) {
        var req = queryRequired();
        var l = new ArrayList<WebtopApp>();
        l.addAll(req);
        if (app != null && !l.contains(app)) {
            l.add(app);
        }
        if (!installed.containsAll(l)) {
            WebtopAppListDialog.show(l);
            return true;
        } else {
            return false;
        }
    }

    public boolean showDialogIfNeeded() {
        return showDialogIfNeeded(null);
    }

    void refreshSelected() throws Exception {
        selected.clear();
        var selectedDir = AppSystemInfo.ofCurrent().getUserHome().resolve(".xpipe", "webtop", "installed");
        if (Files.isDirectory(selectedDir)) {
            try (var stream = Files.list(selectedDir).sorted()) {
                var selectedFiles = stream.toList();
                for (Path selectedFile : selectedFiles) {
                    var name = selectedFile.getFileName().toString();
                    var app = WebtopApp.fromString(name);
                    if (app.isPresent()) {
                        selected.add(app.get());
                    }
                }
            }
        }
    }

    private void refreshInstalled() throws Exception {
        installed.clear();
        var installedDir = Path.of("/apps/installed");
        if (Files.isDirectory(installedDir)) {
            try (var stream = Files.list(installedDir).sorted()) {
                var installedFiles = stream.toList();
                for (Path installedFile : installedFiles) {
                    var name = FilenameUtils.getBaseName(installedFile.getFileName().toString());
                    var app = WebtopApp.fromString(name);
                    if (app.isPresent()) {
                        installed.add(app.get());
                    }
                }
            }
        }
    }

    private Set<WebtopApp> queryRequired() {
        var p = AppPrefs.get();
        var all = new LinkedHashSet<WebtopApp>();

        var pwman = p.passwordManager().getValue();
        if (pwman != null) {
            all.add(pwman.getRequiredWebtopApp());
        }

        var multiplexer = p.terminalMultiplexer().getValue();
        if (multiplexer != null) {
            all.add(multiplexer.getRequiredWebtopApp());
        }

        var terminal = p.terminalType().getValue();
        if (terminal != null) {
            all.add(terminal.getRequiredWebtopApp());
        }

        var editor = p.externalEditor().getValue();
        if (editor != null) {
            all.add(editor.getRequiredWebtopApp());
        }

        var env = System.getenv("XPIPE_PREINSTALLED_WEBTOP_APPS");
        if (env != null && !env.isEmpty()) {
            var split = env.split(",");
            for (String s : split) {
                var app = WebtopApp.fromString(s);
                if (app.isPresent()) {
                    all.add(app.get());
                }
            }
        }

        var fromStores = DataStorage.get().getStoreEntries().stream()
                .map(entry -> entry.getProvider() != null ? entry.getProvider().getRequiredWebtopApp(entry) : null)
                .filter(Objects::nonNull)
                .toList();
        all.addAll(fromStores);

        return all.stream().filter(webtopApp -> webtopApp != null).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void load() throws Exception {
        var availableDir = Path.of("/apps/available");
        if (Files.isDirectory(availableDir)) {
            try (var stream = Files.list(availableDir).sorted()) {
                var availableFiles = stream.toList();
                for (Path availableFile : availableFiles) {
                    var name = FilenameUtils.getBaseName(availableFile.getFileName().toString());
                    var app = WebtopApp.fromString(name);
                    if (app.isPresent()) {
                        available.add(app.get());
                    }
                }
            }
        } else {
            available.addAll(Arrays.asList(WebtopApp.values()));
        }

        refreshInstalled();
        refreshSelected();
    }

    public void install(List<WebtopApp> toInstall) throws Exception {
        var rem = new ArrayList<>(toInstall);
        rem.removeAll(installed);
        if (rem.isEmpty()) {
            return;
        }

        var selectedDir = AppSystemInfo.ofCurrent().getUserHome().resolve(".xpipe", "webtop", "installed");
        Files.createDirectories(selectedDir);
        for (WebtopApp webtopApp : toInstall) {
            if (!Files.exists(selectedDir.resolve(webtopApp.getId()))) {
                Files.createFile(selectedDir.resolve(webtopApp.getId()));
            }
            this.selected.add(webtopApp);
        }

        var requiresRestart = rem.stream().anyMatch(webtopApp -> webtopApp.isRequiresRestart());
        var command = "/apps/install.sh " + rem.stream()
                .map(webtopApp -> webtopApp.getId())
                .collect(Collectors.joining(" "));
        var exec = AppInstallation.ofCurrent().getCliExecutablePath();
        var endCommand = requiresRestart ? exec + " daemon stop --wait\n" + exec + " open" : exec + " open";
        TerminalLaunch.builder().title("Install packages").localScript(ShellScript.lines(command, endCommand)).pauseOnExit(false).launch();
    }
}
