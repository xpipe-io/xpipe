package io.xpipe.app.issue;

import io.xpipe.app.core.*;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.update.XPipeDistributionType;
import io.xpipe.app.util.Hyperlinks;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

public class TerminalErrorHandler extends GuiErrorHandlerBase implements ErrorHandler {

    private final ErrorHandler log = new LogErrorHandler();

    @Override
    public void handle(ErrorEvent event) {
        log.handle(event);

        if (!OperationMode.GUI.isSupported() || event.isOmitted()) {
            SentryErrorHandler.getInstance().handle(event);
            OperationMode.halt(1);
            return;
        }

        handleGui(event);
    }

    private void handleGui(ErrorEvent event) {
        if (!startupGui(throwable -> {
            handleSecondaryException(event, throwable);
            ErrorAction.ignore().handle(event);
        })) {
            return;
        }

        try {
            AppProperties.init();
            AppState.init();
            AppExtensionManager.init(false);
            AppI18n.init();
            AppStyle.init();
            AppTheme.init();
            ErrorHandlerComp.showAndTryWait(event, true);
        } catch (Throwable r) {
            event.clearAttachments();
            handleSecondaryException(event, r);
            return;
        }

        if (OperationMode.isInStartup()) {
            handleProbableUpdate();
        }

        OperationMode.halt(1);
    }

    private void handleSecondaryException(ErrorEvent event, Throwable t) {
        log.handle(event);
        SentryErrorHandler.getInstance().handle(event);

        var second = ErrorEvent.fromThrowable(t).build();
        log.handle(second);
        SentryErrorHandler.getInstance().handle(ErrorEvent.fromThrowable(t).build());
        OperationMode.halt(1);
    }

    private void handleProbableUpdate() {
        try {
            var rel = XPipeDistributionType.get().getUpdateHandler().refreshUpdateCheck();
            if (rel != null && rel.isUpdate()) {
                var update = AppWindowHelper.showBlockingAlert(alert -> {
                            alert.setAlertType(Alert.AlertType.INFORMATION);
                            alert.setTitle(AppI18n.get("updateAvailableTitle"));
                            alert.setHeaderText(AppI18n.get("updateAvailableHeader", rel.getVersion()));
                            alert.getDialogPane()
                                    .setContent(
                                            AppWindowHelper.alertContentText(AppI18n.get("updateAvailableContent")));
                            alert.getButtonTypes().clear();
                            alert.getButtonTypes()
                                    .add(new ButtonType(AppI18n.get("checkOutUpdate"), ButtonBar.ButtonData.YES));
                            alert.getButtonTypes().add(new ButtonType(AppI18n.get("ignore"), ButtonBar.ButtonData.NO));
                        })
                        .map(buttonType -> buttonType.getButtonData().isDefaultButton())
                        .orElse(false);
                if (update) {
                    Hyperlinks.open(rel.getReleaseUrl());
                }
            }
        } catch (Throwable t) {
            var event = ErrorEvent.fromThrowable(t).build();
            log.handle(event);
            SentryErrorHandler.getInstance().handle(event);
            OperationMode.halt(1);
        }
    }
}
