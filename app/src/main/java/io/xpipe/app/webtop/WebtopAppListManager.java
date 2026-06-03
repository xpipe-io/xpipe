package io.xpipe.app.webtop;

import com.fasterxml.jackson.databind.JavaType;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.app.update.AppDistributionType;
import io.xpipe.app.util.JacksonMapper;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Files;
import java.nio.file.Path;
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

        var m = new  WebtopAppListManager();
        m.load();
        INSTANCE = m;

        if (!INSTANCE.installed.containsAll(INSTANCE.selected)) {
            WebtopAppListDialog.show();
        }
    }

    private final Set<WebtopApp> available = new LinkedHashSet<>();
    private final Set<WebtopApp> installed = new LinkedHashSet<>();
    private final Set<WebtopApp> selected = new LinkedHashSet<>();

    public void showDialogIfNeeded() {
        var req = getRequired();
        if (!installed.containsAll(req)) {
            WebtopAppListDialog.show();
        }
    }

    private Set<WebtopApp> getRequired() {
        var p = AppPrefs.get();
        var all = new LinkedHashSet<WebtopApp>();

        var pwman = p.passwordManager().getValue();
        if (pwman != null) {
            all.add(pwman.getRequiredWebtopApp());
        }

        return all.stream().filter(webtopApp -> webtopApp != null).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void load() throws Exception {
        var availableDir = Path.of("/apps/available");
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

        var selectedDir = AppSystemInfo.ofCurrent().getUserHome().resolve(".xpipe", "webtop", "installed");
        if (Files.isDirectory(selectedDir)) {
            try (var stream = Files.list(selectedDir).sorted()) {
                var selectedFiles = stream.toList();
                for (Path selectedFile : selectedFiles) {
                    var name = FilenameUtils.getBaseName(selectedFile.getFileName().toString());
                    var app = WebtopApp.fromString(name);
                    if (app.isPresent()) {
                        selected.add(app.get());
                    }
                }
            }
        }
    }

    public void install(List<WebtopApp> selected) throws Exception {
        this.installed.addAll(selected);
        this.selected.addAll(selected);

        var rem = new ArrayList<>(selected);
        rem.removeAll(installed);
        if (rem.isEmpty()) {
            return;
        }

        var command = "/apps/install.sh " + rem.stream()
                .map(webtopApp -> webtopApp.getId() + ".sh")
                .collect(Collectors.joining(" "));
        TerminalLaunch.builder().title("Install").localScript(ShellScript.of(command)).launch();
    }
}
