package io.xpipe.app.prefs;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.TileButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.update.UpdateAvailableDialog;
import io.xpipe.app.update.XPipeDistributionType;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.app.util.ThreadHelper;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;

public class UpdateCheckComp extends SimpleComp {

    private final ObservableValue<Boolean> updateReady;
    private final ObservableValue<Boolean> checking;

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
        checking = PlatformThread.sync(XPipeDistributionType.get().getUpdateHandler().getBusy());
    }

    private void showAlert() {
        ThreadHelper.runFailableAsync(() -> {
            XPipeDistributionType.get().getUpdateHandler().refreshUpdateCheckSilent(false, false);
            UpdateAvailableDialog.showIfNeeded();
        });
    }

    private void refresh() {
        ThreadHelper.runFailableAsync(() -> {
            XPipeDistributionType.get().getUpdateHandler().refreshUpdateCheck(false, false);
            XPipeDistributionType.get().getUpdateHandler().prepareUpdate();
        });
    }

    @Override
    protected Region createSimple() {
        var name = Bindings.createStringBinding(
                () -> {
                    if (checking.getValue()) {
                        return AppI18n.get("checkingForUpdates");
                    }

                    if (updateReady.getValue()) {
                        var prefix = XPipeDistributionType.get() == XPipeDistributionType.PORTABLE
                                ? AppI18n.get("updateReadyPortable")
                                : AppI18n.get("updateReady");
                        var version = "Version "
                                + XPipeDistributionType.get()
                                        .getUpdateHandler()
                                        .getPreparedUpdate()
                                        .getValue()
                                        .getVersion();
                        return prefix + " (" + version + ")";
                    }

                    return AppI18n.get("checkForUpdates");
                },
                updateReady, checking);
        var description = Bindings.createStringBinding(
                () -> {
                    if (checking.getValue()) {
                        return AppI18n.get("checkingForUpdatesDescription");
                    }

                    if (updateReady.getValue()) {
                        return XPipeDistributionType.get() == XPipeDistributionType.PORTABLE
                                ? AppI18n.get("updateReadyDescriptionPortable")
                                : AppI18n.get("updateReadyDescription");
                    }

                    return AppI18n.get("checkForUpdatesDescription");
                },
                updateReady, checking);
        var graphic = Bindings.createObjectBinding(
                () -> {
                    if (updateReady.getValue()) {
                        return "mdi2a-apple-airplay";
                    }

                    return "mdi2r-refresh";
                },
                updateReady);
        return new TileButtonComp(name, description, graphic, actionEvent -> {
                    actionEvent.consume();
                    if (updateReady.getValue()) {
                        showAlert();
                        return;
                    }

                    refresh();
                })
                .styleClass("update-button")
                .grow(true, false)
                .disable(checking)
                .createRegion();
    }
}
