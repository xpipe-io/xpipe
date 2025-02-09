package io.xpipe.app.issue;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.*;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.app.util.PlatformInit;
import io.xpipe.app.util.ThreadHelper;

public class TerminalErrorHandler extends GuiErrorHandlerBase implements ErrorHandler {

    private final ErrorHandler log = new LogErrorHandler();

    @Override
    public void handle(ErrorEvent event) {
        log.handle(event);

        if (event.isOmitted() || OperationMode.isInShutdown()) {
            ErrorAction.ignore().handle(event);
            // Wait a bit to the beacon the ability to respond to any open requests with an error
            ThreadHelper.sleep(3000);
            OperationMode.halt(1);
            return;
        }

        if (!startupGui(throwable -> {
            handleWithSecondaryException(event, throwable);
            ErrorAction.ignore().handle(event);
        })) {
            // Exit if we couldn't initialize the GUI
            // Wait a bit to the beacon the ability to respond to any open requests with an error
            ThreadHelper.sleep(3000);
            OperationMode.halt(1);
            return;
        }

        handleGui(event);
    }

    private void handleGui(ErrorEvent event) {
        try {
            AppProperties.init();
            AppExtensionManager.init();
            PlatformInit.init(true);
            ErrorHandlerDialog.showAndWait(event);
        } catch (Throwable r) {
            event.clearAttachments();
            handleWithSecondaryException(event, r);
            return;
        }

        if (OperationMode.isInStartup() && !AppProperties.get().isDevelopmentEnvironment()) {
            handleProbableUpdate();
        }

        ThreadHelper.sleep(1000);
        OperationMode.halt(1);
    }

    private void handleWithSecondaryException(ErrorEvent event, Throwable t) {
        ErrorAction.ignore().handle(event);

        var second = ErrorEvent.fromThrowable(t).build();
        log.handle(second);
        ErrorAction.ignore().handle(second);
        ThreadHelper.sleep(1000);
        OperationMode.halt(1);
    }

    private void handleProbableUpdate() {
        if (AppProperties.get().isDevelopmentEnvironment()) {
            return;
        }

        try {
            var rel = AppDistributionType.get().getUpdateHandler().refreshUpdateCheck(false, false);
            if (rel != null && rel.isUpdate()) {
                var updateModal = ModalOverlay.of(
                        "updateAvailableTitle",
                        AppDialog.dialogText(AppI18n.get("updateAvailableContent", rel.getVersion())));
                updateModal.addButton(
                        new ModalButton("checkOutUpdate", () -> Hyperlinks.open(rel.getReleaseUrl()), false, true));
                updateModal.addButton(new ModalButton("ignore", null, true, false));
                AppDialog.showAndWait(updateModal);
            }
        } catch (Throwable t) {
            var event = ErrorEvent.fromThrowable(t).build();
            log.handle(event);
            ErrorAction.ignore().handle(event);
            ThreadHelper.sleep(1000);
            OperationMode.halt(1);
        }
    }
}
