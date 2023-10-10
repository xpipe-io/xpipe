package io.xpipe.app.issue;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.LicenseRequiredException;

public class GuiErrorHandler extends GuiErrorHandlerBase implements ErrorHandler {

    private final ErrorHandler log = new LogErrorHandler();

    @Override
    public void handle(ErrorEvent event) {
        log.handle(event);

        if (!OperationMode.GUI.isSupported() || event.isOmitted()) {
            ErrorAction.ignore().handle(event);
            return;
        }

        handleGui(event);
    }

    private void handleGui(ErrorEvent event) {
        if (!startupGui(throwable -> {
            log.handle(ErrorEvent.fromThrowable(throwable).build());
            ErrorAction.ignore().handle(event);
        })) {
            return;
        }


        if (event.getThrowable() instanceof LicenseRequiredException lex) {
            LicenseProvider.get().showLicenseAlert(lex);
            event.setShouldSendDiagnostics(true);
        } else {
            ErrorHandlerComp.showAndTryWait(event, true);
        }
    }
}
