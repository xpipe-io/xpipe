package io.xpipe.app.prefs;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.update.UpdateAvailableAlert;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.app.update.XPipeDistributionType;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.javafx.FontIcon;

public class UpdateCheckComp extends SimpleComp {

    private final ObservableValue<Boolean> updateReady;

    public UpdateCheckComp() {
        updateReady = PlatformThread.sync(Bindings.createBooleanBinding(
                () -> {
                    return XPipeDistributionType.get().getUpdateHandler().getPreparedUpdate().getValue() != null;
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
                    if (XPipeDistributionType.get().getUpdateHandler().getPreparedUpdate().getValue() != null) {
                        return null;
                    }

                    if (XPipeDistributionType.get().getUpdateHandler().getLastUpdateCheckResult().getValue() != null
                            && XPipeDistributionType.get().getUpdateHandler()
                                    .getLastUpdateCheckResult()
                                    .getValue()
                                    .isUpdate()) {
                        return AppI18n.get(
                                "updateAvailable",
                                XPipeDistributionType.get().getUpdateHandler()
                                        .getLastUpdateCheckResult()
                                        .getValue()
                                        .getVersion());
                    }

                    if (XPipeDistributionType.get().getUpdateHandler().getLastUpdateCheckResult().getValue() != null) {
                        return AppI18n.readableDuration(
                                        new SimpleObjectProperty<>(XPipeDistributionType.get().getUpdateHandler()
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
        var button = new Button();
        button.disableProperty().bind(PlatformThread.sync(XPipeDistributionType.get().getUpdateHandler().getBusy()));
        button.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> {
                            if (updateReady.getValue()) {
                                return AppI18n.get("updateReady");
                            }

                            return AppI18n.get("checkForUpdates");
                        },
                        updateReady));
        button.graphicProperty()
                .bind(Bindings.createObjectBinding(
                        () -> {
                            if (updateReady.getValue()) {
                                return new FontIcon("mdi2a-apple-airplay");
                            }

                            return new FontIcon("mdi2r-refresh");
                        },
                        updateReady));
        button.getStyleClass().add("button-comp");
        button.setOnAction(e -> {
            if (updateReady.getValue()) {
                restart();
                return;
            }

            refresh();
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
