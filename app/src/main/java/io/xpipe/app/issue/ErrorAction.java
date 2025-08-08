package io.xpipe.app.issue;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.core.FailableSupplier;

public interface ErrorAction {

    static ErrorAction openDocumentation(String link) {
        return new ErrorAction() {
            @Override
            public String getName() {
                return AppI18n.get("openDocumentation");
            }

            @Override
            public String getDescription() {
                return AppI18n.get("openDocumentationDescription");
            }

            @Override
            public boolean handle(ErrorEvent event) {
                Hyperlinks.open(link);
                return false;
            }
        };
    }

    static ErrorAction translated(String key, FailableSupplier<Boolean> r) {
        return new ErrorAction() {
            @Override
            public String getName() {
                return AppI18n.get(key);
            }

            @Override
            public String getDescription() {
                return AppI18n.get(key + "Description");
            }

            @Override
            public boolean handle(ErrorEvent event) throws Exception {
                return r.get();
            }
        };
    }

    static IgnoreAction ignore() {
        return new IgnoreAction();
    }

    String getName();

    String getDescription();

    boolean handle(ErrorEvent event) throws Exception;

    class IgnoreAction implements ErrorAction {
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
            if (!event.isReportable()) {
                return true;
            }

            SentryErrorHandler.getInstance().handle(event);
            return true;
        }
    }
}
