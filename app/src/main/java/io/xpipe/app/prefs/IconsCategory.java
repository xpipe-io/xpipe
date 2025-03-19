package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.icon.SystemIconManager;
import io.xpipe.app.icon.SystemIconSource;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.*;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.TextField;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IconsCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "icons";
    }

    @Override
    protected Comp<?> create() {
        var prefs = AppPrefs.get();
        return new OptionsBuilder()
                .addTitle("customIcons")
                .sub(new OptionsBuilder().nameAndDescription("iconSources").addComp(createOverview()))
                .buildComp();
    }

    private Comp<?> createOverview() {
        var sources = FXCollections.<SystemIconSource>observableArrayList();
        AppPrefs.get().getIconSources().subscribe((newValue) -> {
            sources.setAll(SystemIconManager.getEffectiveSources());
        });
        var box = new ListBoxViewComp<>(sources, sources, s -> createSourceEntry(s, sources), true);

        var busy = new SimpleBooleanProperty(false);
        var refreshButton = new TileButtonComp("refreshSources", "refreshSourcesDescription", "mdi2r-refresh", e -> {
            ThreadHelper.runFailableAsync(() -> {
                try (var ignored = new BooleanScope(busy).start()) {
                    SystemIconManager.reload();
                }
            });
            e.consume();
        });
        refreshButton.disable(PlatformThread.sync(busy.or(Bindings.isEmpty(sources))));
        refreshButton.grow(true, false);

        var addGitButton =
                new TileButtonComp("addGitIconSource", "addGitIconSourceDescription", "mdi2a-access-point-plus", e -> {
                    var remote = new SimpleStringProperty();
                    var modal = ModalOverlay.of(
                            "repositoryUrl",
                            Comp.of(() -> {
                                        var creationName = new TextField();
                                        creationName.textProperty().bindBidirectional(remote);
                                        return creationName;
                                    })
                                    .prefWidth(350));
                    modal.withDefaultButtons(() -> {
                        if (remote.get() == null || remote.get().isBlank()) {
                            return;
                        }

                        // Don't use the git sync repo itself ...
                        if (remote.get()
                                .equals(AppPrefs.get().storageGitRemote().get())) {
                            return;
                        }

                        var source = SystemIconSource.GitRepository.builder()
                                .remote(remote.get())
                                .id(UUID.randomUUID().toString())
                                .build();
                        if (!sources.contains(source)) {
                            sources.add(source);
                            var nl = new ArrayList<>(
                                    AppPrefs.get().getIconSources().getValue());
                            nl.add(source);
                            AppPrefs.get().iconSources.setValue(nl);
                        }
                    });
                    modal.show();
                    e.consume();
                });
        addGitButton.grow(true, false);

        var addDirectoryButton = new TileButtonComp(
                "addDirectoryIconSource", "addDirectoryIconSourceDescription", "mdi2f-folder-plus", e -> {
                    var dir = new SimpleStringProperty();
                    var modal = ModalOverlay.of(
                            "iconDirectory",
                            new ContextualFileReferenceChoiceComp(
                                            new SimpleObjectProperty<>(
                                                    DataStorage.get().local().ref()),
                                            dir,
                                            null,
                                            List.of())
                                    .prefWidth(350));
                    modal.withDefaultButtons(() -> {
                        if (dir.get() == null || dir.get().isBlank()) {
                            return;
                        }

                        var path = Path.of(dir.get());
                        if (Files.isRegularFile(path)) {
                            throw new IllegalArgumentException("A custom icon directory requires to be a directory of .svg files, not a single file");
                        }

                        var source = SystemIconSource.Directory.builder()
                                .path(path)
                                .id(UUID.randomUUID().toString())
                                .build();
                        if (!sources.contains(source)) {
                            sources.add(source);
                            var nl = new ArrayList<>(
                                    AppPrefs.get().getIconSources().getValue());
                            nl.add(source);
                            AppPrefs.get().iconSources.setValue(nl);
                        }
                    });
                    modal.show();
                    e.consume();
                });
        addDirectoryButton.grow(true, false);

        var vbox = new VerticalComp(List.of(
                Comp.vspacer(10),
                box,
                Comp.separator(),
                refreshButton,
                Comp.separator(),
                addDirectoryButton,
                addGitButton));
        vbox.spacing(10);
        return vbox;
    }

    private Comp<?> createSourceEntry(SystemIconSource source, List<SystemIconSource> sources) {
        var delete = new IconButtonComp(new LabelGraphic.IconGraphic("mdal-delete_outline"), () -> {
            if (!AppDialog.confirm("iconSourceDeletion")) {
                return;
            }

            var nl = new ArrayList<>(AppPrefs.get().getIconSources().getValue());
            nl.remove(source);
            AppPrefs.get().iconSources.setValue(nl);
            sources.remove(source);
        });
        var buttons = new HorizontalComp(List.of(delete));
        buttons.spacing(5);
        if (!AppPrefs.get().getIconSources().getValue().contains(source)) {
            buttons.disable(new SimpleBooleanProperty(true));
        }

        var tile = new TileButtonComp(
                new SimpleStringProperty(
                        AppPrefs.get().getIconSources().getValue().contains(source)
                                ? source.getDisplayName()
                                : source.getId()),
                new SimpleStringProperty(source.getDescription()),
                new SimpleObjectProperty<>(source.getIcon()),
                actionEvent -> {
                    ThreadHelper.runFailableAsync(() -> {
                        source.open();
                    });
                });
        tile.setRight(buttons);
        tile.setIconSize(1.0);
        tile.grow(true, false);
        return tile;
    }
}
