package io.xpipe.app.issue;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.LicenseRequiredException;
import io.xpipe.app.util.ThreadHelper;

import java.time.Duration;
import java.util.stream.Stream;

public class GuiErrorHandler extends GuiErrorHandlerBase implements ErrorHandler {

    private final ErrorHandler log = new LogErrorHandler();

    @Override
    public void handle(ErrorEvent event) {
        log.handle(event);

        if (!startupGui(throwable -> {
            var second = ErrorEventFactory.fromThrowable(throwable).build();
            log.handle(second);
            ErrorAction.ignore().handle(second);
        })) {
            return;
        }

        if (event.isOmitted()) {
            ErrorAction.ignore().handle(event);
            if (AppLayoutModel.get() != null) {
                AppLayoutModel.get().showQueueEntry(
                        new AppLayoutModel.QueueEntry(AppI18n.observable("errorOccurred"), new LabelGraphic.IconGraphic("mdoal-error_outline"),
                                () -> {
                                    ThreadHelper.runAsync(() -> {
                                        handleGui(event);
                                    });
                                }), Duration.ofSeconds(10), true);
            }
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
                event.clearAttachments();
                ErrorAction.ignore().handle(event);
            }
        } else {
            ErrorHandlerDialog.showAndWait(event);
        }
    }
}
