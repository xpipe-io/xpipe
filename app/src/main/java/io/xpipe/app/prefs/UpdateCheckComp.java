package io.xpipe.app.prefs;

import atlantafx.base.controls.Spacer;
import io.xpipe.app.comp.base.TileButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.update.UpdateAvailableAlert;
import io.xpipe.app.update.XPipeDistributionType;
import io.xpipe.app.util.ThreadHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

public class UpdateCheckComp extends SimpleComp {

    private final ObservableValue<Boolean> updateReady;

    public UpdateCheckComp() {
        updateReady = PlatformThread.sync(Bindings.createBooleanBinding(
                () -> {
                    return XPipeDistributionType.get()
                                    .getUpdateHandler()
                                    .getPreparedUpdate()
                                    .getValue()
                            != null;
                },
                XPipeDistributionType.get().getUpdateHandler().getPreparedUpdate()));
    }

    private void restart() {
        XPipeDistributionType.get().getUpdateHandler().refreshUpdateCheckSilent();
        UpdateAvailableAlert.showIfNeeded();
    }

    private void refresh() {
        ThreadHelper.runFailableAsync(() -> {
            XPipeDistributionType.get().getUpdateHandler().refreshUpdateCheck();
            XPipeDistributionType.get().getUpdateHandler().prepareUpdate();
        });
    }

    private ObservableValue<String> descriptionText() {
        return PlatformThread.sync(Bindings.createStringBinding(
                () -> {
                    if (XPipeDistributionType.get()
                                    .getUpdateHandler()
                                    .getPreparedUpdate()
                                    .getValue()
                            != null) {
                        return "Version " + XPipeDistributionType.get()
                                .getUpdateHandler()
                                .getPreparedUpdate()
                                .getValue()
                                .getVersion();
                    }

                    if (XPipeDistributionType.get()
                                            .getUpdateHandler()
                                            .getLastUpdateCheckResult()
                                            .getValue()
                                    != null
                            && XPipeDistributionType.get()
                                    .getUpdateHandler()
                                    .getLastUpdateCheckResult()
                                    .getValue()
                                    .isUpdate()) {
                        return AppI18n.get(
                                "updateAvailable",
                                XPipeDistributionType.get()
                                        .getUpdateHandler()
                                        .getLastUpdateCheckResult()
                                        .getValue()
                                        .getVersion());
                    }

                    if (XPipeDistributionType.get()
                                    .getUpdateHandler()
                                    .getLastUpdateCheckResult()
                                    .getValue()
                            != null) {
                        return AppI18n.readableDuration(
                                        new SimpleObjectProperty<>(XPipeDistributionType.get()
                                                .getUpdateHandler()
                                                .getLastUpdateCheckResult()
                                                .getValue()
                                                .getCheckTime()),
                                        s -> AppI18n.get("lastChecked") + " " + s)
                                .get();
                    } else {
                        return null;
                    }
                },
                XPipeDistributionType.get().getUpdateHandler().getLastUpdateCheckResult(),
                XPipeDistributionType.get().getUpdateHandler().getPreparedUpdate(),
                XPipeDistributionType.get().getUpdateHandler().getBusy()));
    }

    @Override
    protected Region createSimple() {
        var name = Bindings.createStringBinding(
                () -> {
                    if (updateReady.getValue()) {
                        return XPipeDistributionType.get() == XPipeDistributionType.PORTABLE ?  AppI18n.get("updateReadyPortable") : AppI18n.get("updateReady");
                    }

                    return AppI18n.get("checkForUpdates");
                },
                updateReady);
        var description = Bindings.createStringBinding(
                () -> {
                    if (updateReady.getValue()) {
                        return XPipeDistributionType.get() == XPipeDistributionType.PORTABLE ?  AppI18n.get("updateReadyDescriptionPortable") : AppI18n.get("updateReadyDescription");
                    }

                    return AppI18n.get("checkForUpdatesDescription");
                },
                updateReady);
        var graphic = Bindings.createObjectBinding(
                () -> {
                    if (updateReady.getValue()) {
                        return "mdi2a-apple-airplay";
                    }

                    return "mdi2r-refresh";
                },
                updateReady);
        var button = new TileButtonComp(name, description, graphic, actionEvent -> {
                    actionEvent.consume();
                    if (updateReady.getValue()) {
                        restart();
                        return;
                    }

                    refresh();
                })
                .styleClass("button-comp")
                .disable(PlatformThread.sync(
                        XPipeDistributionType.get().getUpdateHandler().getBusy()))
                .createRegion();

        var checked = new Label();
        checked.textProperty().bind(descriptionText());

        var box = new HBox(button, new Spacer(), checked, new Spacer(15));
        box.setAlignment(Pos.CENTER_LEFT);
        box.setFillHeight(true);
        box.getStyleClass().add("update-check");
        return box;
    }
}
