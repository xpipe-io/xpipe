package io.xpipe.app.issue;

import io.sentry.Sentry;
import io.xpipe.app.core.AppI18n;

public interface ErrorAction {

    public static ErrorAction sendDiagnostics() {
        return new ErrorAction() {
            @Override
            public String getName() {
                return AppI18n.get("reportError");
            }

            @Override
            public String getDescription() {
                return AppI18n.get("reportErrorDescription");
            }

            @Override
            public boolean handle(ErrorEvent event) {
                UserReportComp.show(event);
                return true;
            }
        };
    }

    public static ErrorAction ignore() {
        return new ErrorAction() {
            @Override
            public String getName() {
                return AppI18n.get("ignoreError");
            }

            @Override
            public String getDescription() {
                return AppI18n.get("ignoreErrorDescription");
            }

            @Override
            public boolean handle(ErrorEvent event) {
                event.clearAttachments();
                Sentry.withScope(scope -> {
                    SentryErrorHandler.captureEvent(event, scope);
                });
                return true;
            }
        };
    }

    String getName();

    String getDescription();

    boolean handle(ErrorEvent event);
}
