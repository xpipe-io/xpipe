package io.xpipe.app.util;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ListSelectorComp;
import io.xpipe.app.comp.base.LoadingOverlayComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ScanProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import lombok.Getter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.function.Function;

import static javafx.scene.layout.Priority.ALWAYS;

public class ScanDialogBase {

    private final boolean expand;
    private final Runnable closeAction;
    private final ScanDialogAction action;
    private final ObservableList<DataStoreEntryRef<ShellStore>> entries;
    private final boolean showButton;
    private final ObservableList<ScanProvider.ScanOpportunity> available =
            FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    private final ListProperty<ScanProvider.ScanOpportunity> selected =
            new SimpleListProperty<>(FXCollections.synchronizedObservableList(FXCollections.observableArrayList()));

    @Getter
    private final BooleanProperty busy = new SimpleBooleanProperty();

    public ScanDialogBase(
            boolean expand,
            Runnable closeAction,
            ScanDialogAction action,
            ObservableList<DataStoreEntryRef<ShellStore>> entries,
            boolean showButton) {
        this.expand = expand;
        this.closeAction = closeAction;
        this.action = action;
        this.entries = entries;
        this.showButton = showButton;
    }

    public void finish() throws Exception {
        if (entries.isEmpty()) {
            closeAction.run();
            return;
        }

        BooleanScope.executeExclusive(busy, () -> {
            for (var entry : entries) {
                if (expand) {
                    entry.get().setExpanded(true);
                }
                var copy = new ArrayList<>(selected);
                for (var a : copy) {
                    // If the user decided to remove the selected entry
                    // while the scan is running, just return instantly
                    if (!DataStorage.get().getStoreEntriesSet().contains(entry.get())) {
                        return;
                    }

                    // Previous scan operation could have exited the shell
                    var sc = entry.getStore().getOrStartSession();

                    // Multi-selection compat check
                    if (entries.size() > 1) {
                        var supported = a.getProvider().create(entry.get(), sc);
                        if (supported == null || supported.isDisabled()) {
                            continue;
                        }
                    }

                    try {
                        a.getProvider().scan(entry.get(), sc);
                    } catch (Throwable ex) {
                        ErrorEventFactory.fromThrowable(ex).handle();
                    }
                }
            }
        });
        closeAction.run();
    }

    public void reset() {
        available.clear();
        selected.clear();
    }

    public void onUpdate() {
        available.clear();
        selected.clear();

        if (entries.isEmpty()) {
            return;
        }

        ThreadHelper.runFailableAsync(() -> {
            BooleanScope.executeExclusive(busy, () -> {
                for (var entry : entries) {
                    boolean r;
                    try {
                        var sc = entry.getStore().getOrStartSession();
                        r = action.scan(available, selected, entry.get(), sc);
                    } catch (Throwable t) {
                        closeAction.run();
                        throw t;
                    }
                    if (!r) {
                        closeAction.run();
                        entry.getStore().stopSessionIfNeeded();
                    }
                }
            });
        });
    }

    public Comp<?> createComp() {
        StackPane stackPane = new StackPane();
        stackPane.getStyleClass().add("scan-list");
        VBox.setVgrow(stackPane, ALWAYS);

        Function<ScanProvider.ScanOpportunity, String> nameFunc = (ScanProvider.ScanOpportunity s) -> {
            var n = s.getName().getValue();
            if (s.getLicensedFeatureId() == null || s.isDisabled()) {
                return n;
            }

            var suffix = LicenseProvider.get().getFeature(s.getLicensedFeatureId());
            return n + suffix.getDescriptionSuffix().map(d -> " (" + d + ")").orElse("");
        };
        var r = new ListSelectorComp<>(
                        available,
                        nameFunc,
                        selected,
                        scanOperation -> scanOperation.isDisabled(),
                        () -> available.size() > 3)
                .createRegion();
        stackPane.getChildren().add(r);

        if (showButton) {
            var button = new Button();
            button.textProperty().bind(AppI18n.observable("start"));
            button.setGraphic(new FontIcon("mdi2p-play"));
            button.setOnAction(e -> {
                onUpdate();
            });

            var show = PlatformThread.sync(busy.not().and(Bindings.isEmpty(available)));
            button.visibleProperty().bind(show);
            button.managedProperty().bind(show);

            button.disableProperty().bind(Bindings.isEmpty(entries));

            stackPane.getChildren().add(button);
        } else {
            onUpdate();
            entries.addListener((ListChangeListener<? super DataStoreEntryRef<ShellStore>>) c -> onUpdate());
        }

        var comp = new LoadingOverlayComp(Comp.of(() -> stackPane), busy, true).vgrow();
        return comp;
    }
}
