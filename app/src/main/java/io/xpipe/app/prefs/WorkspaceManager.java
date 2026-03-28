package io.xpipe.app.prefs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.AppRestart;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.util.DesktopHelper;
import io.xpipe.app.util.DesktopShortcuts;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.JacksonMapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class WorkspaceManager {

    @Getter
    private final ObservableList<WorkspaceEntry> workspaces = FXCollections.observableArrayList();

    @Getter
    private WorkspaceEntry current;

    private void load() {
        var file = AppProperties.get().getDefaultDataDir().resolve("workspaces.json");
        if (Files.exists(file)) {
            try {
                var type = TypeFactory.defaultInstance().constructType(new TypeReference<List<WorkspaceEntry>>() {});
                List<WorkspaceEntry> parsed = JacksonMapper.getDefault().readValue(file.toFile(), type);
                for (WorkspaceEntry workspace : parsed) {
                    if (workspace.getName() == null || workspace.getDir() == null || !Files.exists(workspace.getDir())) {
                        continue;
                    }
                    workspaces.add(workspace);
                }
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).expected().handle();
            }
        }

        var d = AppProperties.get().getDataDir();
        var existing = workspaces.stream().filter(workspace -> workspace.getDir().equals(d)).findFirst();
        if (existing.isEmpty()) {
            var name = d.getFileName().toString().startsWith(".xpipe") ? "Default" : d.getFileName().toString();
            current = new WorkspaceEntry(name, d);
            workspaces.addFirst(current);
        } else {
            current = existing.get();
        }
    }

    private void save() {
        try {
            var file = AppProperties.get().getDefaultDataDir().resolve("workspaces.json");
            JacksonMapper.getDefault().writeValue(file.toFile(), workspaces);
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).expected().handle();
        }
    }

    public void addWorkspace(String name, Path dir) {
        workspaces.add(new WorkspaceEntry(name, dir));
        save();

        try {
            var file = DesktopShortcuts.createOpen(name, "open -d \"" + dir + "\" --accept-eula",
                    "-Dio.xpipe.app.dataDir=\"" + dir + "\" -Dio.xpipe.app.acceptEula=true");
            showConfirmModal(file, dir);
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).expected().handle();
        }
    }

    public void open(WorkspaceEntry workspace) {
        if (current.equals(workspace)) {
            return;
        }

        AppRestart.restart(workspace.getDir());
    }

    private static WorkspaceManager INSTANCE;

    public static WorkspaceManager get() {
        return INSTANCE;
    }

    public static void init() {
        INSTANCE = new WorkspaceManager();
        INSTANCE.load();
    }

    public static void reset() {
        INSTANCE.save();
        INSTANCE = null;
    }

    public void show() {
        LicenseProvider.get().getFeature("workspaces").throwIfUnsupported();

        var base = AppProperties.get().getDataDir().toString();
        var name = new SimpleObjectProperty<>("new-workspace");
        var path = new SimpleObjectProperty<>(base + "-new-workspace");
        name.subscribe((v) -> {
            if (v != null && path.get() != null && path.get().startsWith(base)) {
                var newPath = path.get().substring(0, base.length()) + "-"
                        + v.replaceAll(" ", "-").toLowerCase();
                path.set(newPath);
            }
        });
        var content = new OptionsBuilder()
                .nameAndDescription("workspaceName")
                .addString(name)
                .nameAndDescription("workspacePath")
                .addString(path)
                .buildComp()
                .prefWidth(500);
        var modal = ModalOverlay.of("workspaceCreationAlertTitle", content);
        modal.addButton(ModalButton.ok(() -> {
            ThreadHelper.runAsync(() -> {
                if (name.get() == null || path.get() == null) {
                    return;
                }

                addWorkspace(name.get(), Path.of(path.get()));
            });
        }));
        modal.show();
    }

    private void showConfirmModal(Path shortcut, Path workspaceDir) {
        var modal = ModalOverlay.of(
                "workspaceRestartTitle", AppDialog.dialogText(AppI18n.observable("workspaceRestartContent")));
        modal.addButton(new ModalButton(
                "browseShortcut",
                () -> {
                    DesktopHelper.browseFileInDirectory(shortcut);
                },
                false,
                false));
        modal.addButton(new ModalButton(
                "restart",
                () -> {
                    AppRestart.restart(workspaceDir);
                },
                true,
                true));
        modal.show();
    }
}
