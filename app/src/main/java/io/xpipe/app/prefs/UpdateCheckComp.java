package io.xpipe.app.prefs;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.TileButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.update.AppDistributionType;
import io.xpipe.app.update.UpdateAvailableDialog;
import io.xpipe.app.util.ThreadHelper;

import javafx.beans.binding.Bindings;
import javafx.scene.layout.Region;

public class UpdateCheckComp extends SimpleComp {

    private void showDialog() {
        ThreadHelper.runFailableAsync(() -> {
            AppDistributionType.get().getUpdateHandler().refreshUpdateCheckSilent(false, false);
            UpdateAvailableDialog.showIfNeeded(false);
        });
    }

    private void refresh() {
        ThreadHelper.runFailableAsync(() -> {
            AppDistributionType.get().getUpdateHandler().refreshUpdateCheck(false, false);
            AppDistributionType.get().getUpdateHandler().prepareUpdate();
        });
    }

    @Override
    protected Region createSimple() {
        var uh = AppDistributionType.get().getUpdateHandler();
        var name = Bindings.createStringBinding(
                () -> {
                    if (uh.getBusy().getValue()) {
                        var available = uh.getLastUpdateCheckResult().getValue();
                        if (available != null) {
                            return AppI18n.get("downloadingUpdate", available.getVersion());
                        }

                        return AppI18n.get("checkingForUpdates");
                    }

                    if (uh.getPreparedUpdate().getValue() != null) {
                        var prefix = !uh.supportsDirectInstallation()
                                ? AppI18n.get("updateReadyPortable")
                                : AppI18n.get("updateReady");
                        var version =
                                "Version " + uh.getPreparedUpdate().getValue().getVersion();
                        return prefix + " (" + version + ")";
                    }

                    return AppI18n.get("checkForUpdates");
                },
                AppI18n.activeLanguage(),
                uh.getLastUpdateCheckResult(),
                uh.getPreparedUpdate(),
                uh.getBusy());
        var description = Bindings.createStringBinding(
                () -> {
                    if (uh.getBusy().getValue()) {
                        var available = uh.getLastUpdateCheckResult().getValue();
                        if (available != null) {
                            return AppI18n.get("downloadingUpdateDescription");
                        }

                        return AppI18n.get("checkingForUpdatesDescription");
                    }

                    if (uh.getPreparedUpdate().getValue() != null) {
                        return AppDistributionType.get() == AppDistributionType.PORTABLE
                                ? AppI18n.get("updateReadyDescriptionPortable")
                                : AppI18n.get("updateReadyDescription");
                    }

                    return AppI18n.get("checkForUpdatesDescription");
                },
                AppI18n.activeLanguage(),
                uh.getLastUpdateCheckResult(),
                uh.getPreparedUpdate(),
                uh.getBusy());
        var graphic = Bindings.createObjectBinding(
                () -> {
                    if (uh.getPreparedUpdate().getValue() != null) {
                        return "mdi2b-button-cursor";
                    }

                    if (uh.getBusy().getValue() && uh.getLastUpdateCheckResult().getValue() != null) {
                        return "mdi2d-download";
                    }

                    return "mdi2r-refresh";
                },
                uh.getPreparedUpdate(),
                uh.getBusy(),
                uh.getLastUpdateCheckResult());
        return new TileButtonComp(name, description, graphic, actionEvent -> {
                    actionEvent.consume();
                    if (uh.getPreparedUpdate().getValue() != null) {
                        showDialog();
                        return;
                    }

                    refresh();
                })
                .styleClass("update-button")
                .disable(uh.getBusy())
                .createRegion();
    }
}
