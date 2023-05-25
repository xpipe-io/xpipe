package io.xpipe.app.issue;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.Hyperlinks;

public interface ErrorAction {

    public static ErrorAction reportOnGithub() {
        return new ErrorAction() {
            @Override
            public String getName() {
                return AppI18n.get("reportOnGithub");
            }

            @Override
            public String getDescription() {
                return AppI18n.get("reportOnGithubDescription");
            }

            @Override
            public boolean handle(ErrorEvent event) {
                var url = "https://github.com/xpipe-io/xpipe/issues/new";
                Hyperlinks.open(url);
                return false;
            }
        };
    }

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
                SentryErrorHandler.getInstance().handle(event);
                return true;
            }
        };
    }

    String getName();

    String getDescription();

    boolean handle(ErrorEvent event);
}
