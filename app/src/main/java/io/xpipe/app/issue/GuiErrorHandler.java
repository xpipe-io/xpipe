package io.xpipe.app.issue;

import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.LicenseRequiredException;

import java.util.stream.Stream;

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
        var lex = event.getThrowableChain().stream()
                .flatMap(throwable -> throwable instanceof LicenseRequiredException le ? Stream.of(le) : Stream.of())
                .findFirst();
        if (lex.isPresent()) {
            LicenseProvider.get().showLicenseAlert(lex.get());
            if (!LicenseProvider.get().hasPaidLicense()) {
                event.setLicenseRequired(true);
                event.clearAttachments();
                ErrorAction.ignore().handle(event);
            }
        } else {
            ErrorHandlerDialog.showAndWait(event);
        }
    }
}
