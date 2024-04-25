package io.xpipe.app.issue;

import io.xpipe.app.core.*;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.update.XPipeDistributionType;
import io.xpipe.app.util.Hyperlinks;

import io.xpipe.app.util.ThreadHelper;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

public class TerminalErrorHandler extends GuiErrorHandlerBase implements ErrorHandler {

    private final ErrorHandler log = new LogErrorHandler();

    @Override
    public void handle(ErrorEvent event) {
        log.handle(event);

        if (event.isOmitted() || OperationMode.isInShutdown()) {
            ErrorAction.ignore().handle(event);
            OperationMode.halt(1);
            return;
        }

        if (!startupGui(throwable -> {
            handleWithSecondaryException(event, throwable);
            ErrorAction.ignore().handle(event);
        })) {
            // Exit if we couldn't initialize the GUI
            OperationMode.halt(1);
            return;
        }

        handleGui(event);
    }

    private void handleGui(ErrorEvent event) {
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
            handleWithSecondaryException(event, r);
            return;
        }

        if (OperationMode.isInStartup() && !AppProperties.get().isDevelopmentEnvironment()) {
            handleProbableUpdate();
        }

        OperationMode.halt(1);
    }

    private void handleWithSecondaryException(ErrorEvent event, Throwable t) {
        ErrorAction.ignore().handle(event);

        var second = ErrorEvent.fromThrowable(t).build();
        log.handle(second);
        ErrorAction.ignore().handle(second);
        OperationMode.halt(1);
    }

    private void handleProbableUpdate() {
        if (AppProperties.get().isDevelopmentEnvironment()) {
            return;
        }

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
                    ThreadHelper.sleep(1000);
                }
            }
        } catch (Throwable t) {
            var event = ErrorEvent.fromThrowable(t).build();
            log.handle(event);
            ErrorAction.ignore().handle(event);
            OperationMode.halt(1);
        }
    }
}
