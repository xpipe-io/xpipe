package io.xpipe.app.prefs;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.icon.SystemIconManager;
import io.xpipe.app.icon.SystemIconSource;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.*;
import io.xpipe.core.FilePath;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

import java.nio.file.Files;
import java.util.*;

public class IconsCategory extends AppPrefsCategory {

    @Override
    protected String getId() {
        return "icons";
    }

    @Override
    protected LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2v-view-grid-plus-outline");
    }

    @Override
    protected Comp<?> create() {
        return new OptionsBuilder()
                .addTitle("customIcons")
                .sub(new OptionsBuilder()
                        .nameAndDescription("iconSources")
                        .longDescription(DocumentationLink.ICONS)
                        .addComp(createOverview().maxWidth(getCompWidth()))
                        .nameAndDescription("preferMonochromeIcons")
                        .addToggle(AppPrefs.get().preferMonochromeIcons))
                .buildComp();
    }

    private Comp<?> createOverview() {
        var sources = FXCollections.<SystemIconSource>observableArrayList();
        AppPrefs.get().getIconSources().subscribe((newValue) -> {
            sources.setAll(SystemIconManager.getAllSources());
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
                        if (sources.stream()
                                .noneMatch(s -> s instanceof SystemIconSource.GitRepository g
                                        && g.getRemote().equals(remote.get()))) {
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
                    var dir = new SimpleObjectProperty<FilePath>();
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
                        if (dir.get() == null) {
                            return;
                        }

                        var path = dir.get().asLocalPath();
                        if (Files.isRegularFile(path)) {
                            throw ErrorEventFactory.expected(
                                    new IllegalArgumentException(
                                            "A custom icon source must be a directory containing .svg files, not a single file"));
                        }

                        var source = SystemIconSource.Directory.builder()
                                .path(path)
                                .id(UUID.randomUUID().toString())
                                .build();
                        if (sources.stream()
                                .noneMatch(s -> s instanceof SystemIconSource.Directory d
                                        && d.getPath().equals(path))) {
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
                Comp.hseparator(),
                refreshButton,
                Comp.hseparator(),
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
        if (!AppPrefs.get().getIconSources().getValue().contains(source)) {
            delete.disable(new SimpleBooleanProperty(true));
        }

        var disabled = AppCache.getNonNull("disabledIconSources", Set.class, () -> Set.<String>of());
        var enabled = Comp.of(() -> {
            var cb = new CheckBox();
            cb.setSelected(!disabled.contains(source.getId()));
            cb.selectedProperty().addListener((observable, oldValue, newValue) -> {
                var set = new LinkedHashSet<>(
                        AppCache.getNonNull("disabledIconSources", Set.class, () -> Set.<String>of()));
                if (newValue) {
                    set.remove(source.getId());
                } else {
                    set.add(source.getId());
                }
                AppCache.update("disabledIconSources", set);
            });
            cb.setAlignment(Pos.BOTTOM_CENTER);
            cb.setPadding(new Insets(0, 0, 1, 0));
            AppFontSizes.sm(cb);
            cb.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                cb.setSelected(!cb.isSelected());
                e.consume();
            });
            return cb;
        });

        var buttons = new HorizontalComp(List.of(enabled, delete));
        buttons.apply(struc -> struc.get().setFillHeight(true));
        buttons.spacing(15);

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
