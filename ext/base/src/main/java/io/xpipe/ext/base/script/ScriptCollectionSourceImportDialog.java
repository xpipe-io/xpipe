package io.xpipe.ext.base.script;

import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ShellDialectIcons;
import io.xpipe.app.hub.comp.*;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.FilePath;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

public class ScriptCollectionSourceImportDialog {

    private final DataStoreEntryRef<ScriptCollectionSourceStore> source;
    private final ObservableList<ScriptCollectionSourceEntry> available = FXCollections.observableArrayList();
    private final ObservableList<ScriptCollectionSourceEntry> shown = FXCollections.observableArrayList();
    private final ObservableList<ScriptCollectionSourceEntry> selected = FXCollections.observableArrayList();
    private final StringProperty filter = new SimpleStringProperty();
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final ObjectProperty<StoreCategoryWrapper> targetCategory = new SimpleObjectProperty<>();

    public ScriptCollectionSourceImportDialog(DataStoreEntryRef<ScriptCollectionSourceStore> source) {
        this.source = source;
        available.setAll(source.getStore().getSource().listScripts());
        update();

        filter.addListener((observable, oldValue, newValue) -> {
            update();
        });

        targetCategory.set(findDefaultCategory());
    }

    private StoreCategoryWrapper findDefaultCategory() {
        var all = StoreViewState.get().getSortedCategories(StoreViewState.get().getAllScriptsCategory())
                .filtered(w -> w.getParent() != null &&
                        !w.getCategory().getUuid().equals(DataStorage.PREDEFINED_SCRIPTS_CATEGORY_UUID) &&
                        !w.getCategory().getUuid().equals(DataStorage.SCRIPT_SOURCES_CATEGORY_UUID));
        return all.getList().size() > 0 ? all.getList().getFirst() : null;
    }

    public void show() {
        var filterField = new FilterComp(filter).hgrow();
        // Ugly solution to focus the filter on show
        filterField.apply(r -> {
            HBox.setHgrow(r, Priority.SOMETIMES);
            r.sceneProperty().subscribe(s -> {
                if (s != null) {
                    Platform.runLater(() -> {
                        Platform.runLater(() -> {
                            r.requestFocus();
                        });
                    });
                }
            });
        });

        var refresh = new ButtonComp(null, new FontIcon("mdmz-refresh"), () -> {
                    ThreadHelper.runAsync(() -> {
                        try (var ignored = new BooleanScope(busy).exclusive().start()) {
                            source.getStore().getSource().prepare();
                            var all = source.getStore().getSource().listScripts();
                            available.setAll(all);
                            update();
                        } catch (Exception e) {
                            ErrorEventFactory.fromThrowable(e).handle();
                        }
                    });
                })
                .maxHeight(100);

        var notFound = new LabelComp(AppI18n.observable("noScriptsFound"));
        notFound.show(Bindings.isEmpty(shown).and(busy.not()));

        var selector = new ListSelectorComp<>(
                shown,
                e -> e.getName() + " [" + e.getDialect().getDisplayName() + "]",
                e -> new LabelGraphic.ImageGraphic(ShellDialectIcons.getImageName(e.getDialect()), 16),
                selected,
                e -> false,
                () -> shown.size() > 0);
        selector.disable(busy);

        var stack = new StackComp(List.of(notFound, selector));
        stack.prefWidth(600);
        stack.prefHeight(650);

        var catChoice = new DataStoreCategoryChoiceComp(
                StoreViewState.get().getAllScriptsCategory(),
                StoreViewState.get().getActiveCategory(),
                targetCategory,
                false,
                w -> {
                    return w.getParent() != null && !w.equals(StoreViewState.get().getScriptSourcesCategory());
                });
        catChoice.hgrow();
        catChoice.maxHeight(100);

        var modal = ModalOverlay.of(
                Bindings.createStringBinding(
                        () -> {
                            return AppI18n.get("scriptSourceCollectionImportTitle", selected.size(), available.size());
                        },
                        available,
                        selected,
                        AppI18n.activeLanguage()),
                stack,
                null);
        modal.addButtonBarComp(refresh);
        modal.addButtonBarComp(filterField);
        modal.addButtonBarComp(catChoice);
        modal.addButton(ModalButton.ok(() -> {
                    ThreadHelper.runAsync(() -> {
                        finish();
                    });
                }))
                .augment(button ->
                        button.disableProperty().bind(Bindings.isEmpty(selected).or(targetCategory.isNull())));
        modal.show();
    }

    private void finish() {
        StoreViewState.get().selectCategoryIntoViewIfNeeded(targetCategory.getValue());

        var added = new ArrayList<DataStoreEntry>();
        for (ScriptCollectionSourceEntry e : selected) {
            var name = FilePath.of(e.getName()).getBaseName().toString();
            var textSource = ScriptTextSource.SourceReference.builder()
                    .ref(source)
                    .name(e.getName())
                    .build();

            var alreadyAdded = DataStorage.get().getStoreEntries().stream()
                    .anyMatch(entry ->
                            entry.getStore() instanceof ScriptStore ss && textSource.equals(ss.getTextSource()));
            if (alreadyAdded) {
                continue;
            }

            var store = ScriptStore.builder().textSource(textSource).build();
            var entry = DataStoreEntry.createNew(name, store);
            entry.setCategoryUuid(targetCategory.getValue().getCategory().getUuid());
            DataStorage.get().addStoreEntryIfNotPresent(entry);
            added.add(entry);
        }

        if (added.size() == 1) {
            StoreCreationDialog.showEdit(added.getFirst());
        }
    }

    private void update() {
        if (filter.get() == null) {
            shown.setAll(available);
            return;
        }

        var f = filter.get().toLowerCase();
        var filtered = available.stream()
                .filter(e -> e.getName().toLowerCase().contains(f))
                .toList();
        var newList = new ArrayList<ScriptCollectionSourceEntry>();
        newList.addAll(selected);
        newList.addAll(filtered);
        shown.setAll(newList);
    }
}
