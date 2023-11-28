package io.xpipe.app.issue;

import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.LicenseRequiredException;

public class GuiErrorHandler extends GuiErrorHandlerBase implements ErrorHandler {

    private final ErrorHandler log = new LogErrorHandler();

    @Override
    public void handle(ErrorEvent event) {
        log.handle(event);

        if (event.isOmitted()) {
            ErrorAction.ignore().handle(event);
            return;
        }

        if (!startupGui(throwable -> {
            var second = ErrorEvent.fromThrowable(throwable).build();
            log.handle(second);
            ErrorAction.ignore().handle(second);
        })) {
            return;
        }

        handleGui(event);
    }

    private void handleGui(ErrorEvent event) {
        if (event.getThrowable() instanceof LicenseRequiredException lex) {
            LicenseProvider.get().showLicenseAlert(lex);
            event.setShouldSendDiagnostics(true);
            ErrorAction.ignore().handle(event);
        } else {
            ErrorHandlerComp.showAndTryWait(event, true);
        }
    }
}
