package io.xpipe.app.webtop;

import io.xpipe.app.process.ShellScript;
import io.xpipe.app.terminal.TerminalLaunch;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.io.FilenameUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WebtopAppListModel {

    private final ObservableList<WebtopApp> available = FXCollections.observableArrayList();
    private final ObservableList<WebtopApp> installed = FXCollections.observableArrayList();

    public void init() throws Exception {
        var availableDir = Path.of("/apps/available");
        try (var stream = Files.list(availableDir)) {
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
        try (var stream = Files.list(installedDir)) {
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

    public void install(List<WebtopApp> selected) throws Exception {
        var rem = new ArrayList<>(selected);
        rem.removeAll(installed);
        if (rem.isEmpty()) {
            return;
        }

        var command = "/apps/install.sh " + rem.stream().map(webtopApp -> webtopApp.getId() + ".sh").collect(Collectors.joining(" "));
        TerminalLaunch.builder().title("Install").localScript(ShellScript.of(command)).launch();
    }
}
