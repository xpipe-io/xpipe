package io.xpipe.app.util;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ListSelectorComp;
import io.xpipe.app.comp.base.LoadingOverlayComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ScanProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;

import io.xpipe.core.process.ShellControl;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import lombok.Getter;

import java.util.ArrayList;
import java.util.function.Function;

import static javafx.scene.layout.Priority.ALWAYS;

public class ScanDialogBase {

    private final boolean expand;
    private final Runnable closeAction;
    private final ScanDialogAction action;
    private final ObservableList<DataStoreEntryRef<ShellStore>> entries;
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
            ObservableList<DataStoreEntryRef<ShellStore>> entries) {
        this.expand = expand;
        this.closeAction = closeAction;
        this.action = action;
        this.entries = entries;
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
                        ErrorEvent.fromThrowable(ex).handle();
                    }
                }
            }
        });
        closeAction.run();
    }

    private void onUpdate() {
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
            var n = AppI18n.get(s.getNameKey());
            if (s.getLicensedFeatureId() == null) {
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

        onUpdate();
        entries.addListener((ListChangeListener<? super DataStoreEntryRef<ShellStore>>) c -> onUpdate());

        var comp = LoadingOverlayComp.noProgress(Comp.of(() -> stackPane), busy).vgrow();
        return comp;
    }
}
