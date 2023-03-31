package io.xpipe.app.comp.about;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.update.AppUpdater;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.XPipeDistributionType;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

public class UpdateCheckComp extends SimpleComp {

    private final ObservableBooleanValue updateAvailable;
    private final ObservableValue<Boolean> updateReady;

    public UpdateCheckComp() {
        updateAvailable = Bindings.createBooleanBinding(
                () -> {
                    return AppUpdater.get().getLastUpdateCheckResult().getValue() != null
                            && AppUpdater.get()
                                    .getLastUpdateCheckResult()
                                    .getValue()
                                    .isUpdate();
                },
                PlatformThread.sync(AppUpdater.get().getLastUpdateCheckResult()));
        updateReady = Bindings.createBooleanBinding(
                () -> {
                    return AppUpdater.get().getDownloadedUpdate().getValue() != null;
                },
                PlatformThread.sync(AppUpdater.get().getDownloadedUpdate()));
    }

    private void restart() {
        // Check if we're still on latest
        if (!AppUpdater.get().isDownloadedUpdateStillLatest()) {
            return;
        }

        AppUpdater.get().executeUpdateAndClose();
    }

    private void update() {
        AppUpdater.get().downloadUpdateAsync();
    }

    private void refresh() {
        AppUpdater.get().checkForUpdateAsync(true);
    }

    private ObservableValue<String> descriptionText() {
        return PlatformThread.sync(Bindings.createStringBinding(
                () -> {
                    if (AppUpdater.get().getDownloadedUpdate().getValue() != null) {
                        return AppI18n.get("updateRestart");
                    }

                    if (AppUpdater.get().getLastUpdateCheckResult().getValue() != null
                            && AppUpdater.get()
                                    .getLastUpdateCheckResult()
                                    .getValue()
                                    .isUpdate()) {
                        return AppI18n.get(
                                "updateAvailable",
                                AppUpdater.get()
                                        .getLastUpdateCheckResult()
                                        .getValue()
                                        .getVersion());
                    }

                    if (AppUpdater.get().getLastUpdateCheckResult().getValue() != null) {
                        return AppI18n.readableDuration(
                                        new SimpleObjectProperty<>(AppUpdater.get()
                                                .getLastUpdateCheckResult()
                                                .getValue()
                                                .getCheckTime()),
                                        s -> AppI18n.get("lastChecked") + " " + s)
                                .get();
                    } else {
                        return null;
                    }
                },
                AppUpdater.get().getLastUpdateCheckResult(),
                AppUpdater.get().getDownloadedUpdate(),
                AppUpdater.get().getBusy()));
    }

    @Override
    protected Region createSimple() {
        var button = new Button();
        button.disableProperty().bind(PlatformThread.sync(AppUpdater.get().getBusy()));
        button.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> {
                            if (updateReady.getValue()) {
                                return AppI18n.get("updateReady");
                            }

                            if (updateAvailable.getValue()) {
                                return XPipeDistributionType.get().supportsUpdate()
                                        ? AppI18n.get("downloadUpdate")
                                        : AppI18n.get("checkOutUpdate");
                            } else {
                                return AppI18n.get("checkForUpdates");
                            }
                        },
                        updateAvailable,
                        updateReady));
        button.graphicProperty()
                .bind(Bindings.createObjectBinding(
                        () -> {
                            if (updateReady.getValue()) {
                                return new FontIcon("mdi2r-restart");
                            }

                            if (updateAvailable.getValue()) {
                                return new FontIcon("mdi2d-download");
                            } else {
                                return new FontIcon("mdi2r-refresh");
                            }
                        },
                        updateAvailable,
                        updateReady));
        button.getStyleClass().add("button-comp");
        button.setOnAction(e -> {
            AppUpdater.get().refreshUpdateState();

            if (updateReady.getValue()) {
                restart();
                return;
            }

            if (updateAvailable.getValue() && !XPipeDistributionType.get().supportsUpdate()) {
                Hyperlinks.open(
                        AppUpdater.get().getLastUpdateCheckResult().getValue().getReleaseUrl());
            } else if (updateAvailable.getValue()) {
                update();
            } else {
                refresh();
            }
        });

        var checked = new Label();
        checked.textProperty().bind(descriptionText());

        var box = new HBox(button, checked);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setFillHeight(true);
        box.getStyleClass().add("update-check");
        return box;
    }
}
