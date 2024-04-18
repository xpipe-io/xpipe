package io.xpipe.app.prefs;

import io.xpipe.app.comp.base.TileButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.update.UpdateAvailableAlert;
import io.xpipe.app.update.XPipeDistributionType;
import io.xpipe.app.util.ThreadHelper;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
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

    private void performUpdateAndRestart() {
        XPipeDistributionType.get().getUpdateHandler().refreshUpdateCheckSilent();
        UpdateAvailableAlert.showIfNeeded();
    }

    private void refresh() {
        ThreadHelper.runFailableAsync(() -> {
            XPipeDistributionType.get().getUpdateHandler().refreshUpdateCheck();
            XPipeDistributionType.get().getUpdateHandler().prepareUpdate();
        });
    }

    @Override
    protected Region createSimple() {
        var name = Bindings.createStringBinding(
                () -> {
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
                updateReady);
        var description = Bindings.createStringBinding(
                () -> {
                    if (updateReady.getValue()) {
                        return XPipeDistributionType.get() == XPipeDistributionType.PORTABLE
                                ? AppI18n.get("updateReadyDescriptionPortable")
                                : AppI18n.get("updateReadyDescription");
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
        return new TileButtonComp(name, description, graphic, actionEvent -> {
                    actionEvent.consume();
                    if (updateReady.getValue()) {
                        performUpdateAndRestart();
                        return;
                    }

                    refresh();
                })
                .styleClass("update-button")
                .grow(true, false)
                .disable(PlatformThread.sync(
                        XPipeDistributionType.get().getUpdateHandler().getBusy()))
                .createRegion();
    }
}
