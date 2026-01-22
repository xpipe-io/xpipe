package io.xpipe.ext.base.script;

import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ShellDialectChoiceComp;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.DerivedObservableList;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.BooleanScope;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

public class ScriptCollectionSourceImportDialog {

    private final ScriptCollectionSource source;
    private final ObservableList<ScriptCollectionSourceEntry> available = FXCollections.observableArrayList();
    private final DerivedObservableList<ScriptCollectionSourceEntry> shown = DerivedObservableList.arrayList(true);
    private final ObservableList<ScriptCollectionSourceEntry> selected = FXCollections.observableArrayList();
    private final StringProperty filter = new SimpleStringProperty();
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final IntegerProperty count = new SimpleIntegerProperty();

    public ScriptCollectionSourceImportDialog(ScriptCollectionSource source) {
        this.source = source;
        available.setAll(source.listScripts());
        update();

        filter.addListener((observable, oldValue, newValue) -> {
            update();
        });
    }

    public void show() {
        var filterField = new FilterComp(filter).hgrow();
        // Ugly solution to focus the filter on show
        filterField.apply(r -> {
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
            try (var ignored = new BooleanScope(busy).exclusive().start()) {
                source.prepare();
                var all = source.listScripts();
                available.setAll(all);
                update();
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).handle();
            }
        }).maxHeight(100);

        var notFound = new LabelComp(AppI18n.observable("noScriptsFound"));
        notFound.show(Bindings.isEmpty(shown.getList()).and(busy.not()));

        var selector = new ListSelectorComp<>(shown.getList(),
                e -> e.getName()+ " [" + e.getDialect().getDisplayName() + "]",
                e -> new LabelGraphic.ImageGraphic(ShellDialectChoiceComp.getImageName(e.getDialect()), 16),
                selected, e -> false,
                () -> shown.getList().size() > 0);
        selector.disable(busy);

        var stack = new StackComp(List.of(notFound, selector));
        stack.prefWidth(600);
        stack.prefHeight(700);

        var modal = ModalOverlay.of(
                Bindings.createStringBinding(() -> {
                    return AppI18n.get("scriptSourceCollectionImportTitle", count.get());
                }, count, AppI18n.activeLanguage()),
                stack,
                null);
        modal.addButtonBarComp(refresh);
        modal.addButtonBarComp(filterField);
        modal.addButton(ModalButton.ok(() -> {

                }))
                .augment(button -> button.disableProperty().bind(Bindings.isEmpty(selected)));
        modal.show();
    }

    private void update() {
        if (filter.get() == null) {
            shown.setContent(available);
            count.set(available.size());
            return;
        }

        var f = filter.get().toLowerCase();
        var filtered = available.stream().filter(e -> e.getName().toLowerCase().contains(f)).toList();
        var newList = new ArrayList<ScriptCollectionSourceEntry>();
        newList.addAll(selected);
        newList.addAll(filtered);
        shown.setContent(newList);
        count.set(newList.size());
    }
}
