package io.xpipe.app.issue;

import io.xpipe.app.core.AppDistributionType;
import io.xpipe.app.core.AppLogs;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.LicenseProvider;

import io.sentry.*;
import io.sentry.protocol.Geo;
import io.sentry.protocol.SentryId;
import io.sentry.protocol.User;
import org.apache.commons.io.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SentryErrorHandler implements ErrorHandler {

    private static final ErrorHandler INSTANCE = new SyncErrorHandler(new SentryErrorHandler());
    private boolean init;

    public static ErrorHandler getInstance() {
        return INSTANCE;
    }

    private static boolean hasUserReport(ErrorEvent ee) {
        var email = ee.getEmail();
        var hasEmail = email != null && !email.isBlank();
        var text = ee.getUserReport();
        var hasText = text != null && !text.isBlank();
        return hasEmail || hasText;
    }

    private static boolean doesExceedCommentSize(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        return text.length() > 5000;
    }

    private static Throwable adjustCopy(Throwable throwable, boolean clear) {
        if (throwable == null) {
            return null;
        }

        if (!clear) {
            return throwable;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(throwable);

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            var copy = (Throwable) ois.readObject();

            var msgField = Throwable.class.getDeclaredField("detailMessage");
            msgField.setAccessible(true);
            msgField.set(copy, null);

            if (copy instanceof FileSystemException) {
                var fileField = FileSystemException.class.getDeclaredField("file");
                fileField.setAccessible(true);
                fileField.set(copy, null);

                var otherField = FileSystemException.class.getDeclaredField("other");
                otherField.setAccessible(true);
                otherField.set(copy, null);
            }

            if (copy instanceof InvalidPathException) {
                var inputField = InvalidPathException.class.getDeclaredField("input");
                inputField.setAccessible(true);
                inputField.set(copy, "");
            }

            var causeField = Throwable.class.getDeclaredField("cause");
            causeField.setAccessible(true);
            causeField.set(copy, adjustCopy(throwable.getCause(), true));

            var suppressedField = Throwable.class.getDeclaredField("suppressedExceptions");
            suppressedField.setAccessible(true);
            var suppressed = throwable.getSuppressed();
            if (suppressed.length > 0) {
                suppressedField.set(copy, Arrays.stream(suppressed).map(s -> adjustCopy(s, true)).toList());
            }

            return copy;
        } catch (Throwable e) {
            // This can fail for example when the underlying exception is not serializable
            // and comes from some third party library
            if (AppLogs.get() != null) {
                AppLogs.get().logException("Unable to adjust exception", e);
            }
            return throwable;
        }
    }

    private static SentryId captureEvent(ErrorEvent ee) {
        if (!hasUserReport(ee) && "User Report".equals(ee.getDescription())) {
            return null;
        }

        if (ee.getThrowable() != null) {
            var adjusted = adjustCopy(ee.getThrowable(), !ee.isShouldSendDiagnostics() && !ee.isLicenseRequired());
            return Sentry.captureException(adjusted, sc -> fillScope(ee, sc));
        }

        if (ee.getDescription() != null) {
            return Sentry.captureMessage(ee.getDescription(), sc -> fillScope(ee, sc));
        }

        var event = new SentryEvent();
        return Sentry.captureEvent(event, sc -> fillScope(ee, sc));
    }

    private static void fillScope(ErrorEvent ee, IScope s) {
        if (ee.isShouldSendDiagnostics()) {
            // Write all buffered output to log files to ensure that we get all information
            if (AppLogs.get() != null) {
                AppLogs.get().flush();
            }

            var atts = ee.getAttachments().stream()
                    .map(d -> {
                        try {
                            var toUse = d;
                            if (Files.isDirectory(d)) {
                                toUse = AttachmentHelper.compressZipfile(
                                        d,
                                        FileUtils.getTempDirectory()
                                                .toPath()
                                                .resolve(d.getFileName().toString() + ".zip"));
                            }
                            return new Attachment(toUse.toString());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            return null;
                        }
                    })
                    .filter(attachment -> attachment != null)
                    .toList();
            atts.forEach(attachment -> s.addAttachment(attachment));
        }

        if (doesExceedCommentSize(ee.getUserReport())) {
            try {
                var report = Files.createTempFile("report", ".txt");
                Files.writeString(report, ee.getUserReport());
                s.addAttachment(new Attachment(report.toString()));
            } catch (Exception ex) {
                AppLogs.get().logException("Unable to create report file", ex);
            }
        }

        s.setTag(
                "hasLicense",
                String.valueOf(
                        LicenseProvider.get() != null ? LicenseProvider.get().hasPaidLicense() : null));
        s.setTag(
                "updatesEnabled",
                AppPrefs.get() != null
                        ? AppPrefs.get().automaticallyUpdate().getValue().toString()
                        : "unknown");
        s.setTag(
                "securityUpdatesEnabled",
                AppPrefs.get() != null
                        ? AppPrefs.get().checkForSecurityUpdates().getValue().toString()
                        : "unknown");
        s.setTag("initError", String.valueOf(OperationMode.isInStartup()));
        s.setTag(
                "developerMode",
                AppPrefs.get() != null
                        ? AppPrefs.get().developerMode().getValue().toString()
                        : "false");
        s.setTag("terminal", Boolean.toString(ee.isTerminal()));
        s.setTag("omitted", Boolean.toString(ee.isOmitted()));
        s.setTag(
                "logs",
                Boolean.toString(
                        ee.isShouldSendDiagnostics() && !ee.getAttachments().isEmpty()));
        s.setTag("inStartup", Boolean.toString(OperationMode.isInStartup()));
        s.setTag("inShutdown", Boolean.toString(OperationMode.isInShutdown()));
        s.setTag("unhandled", Boolean.toString(ee.isUnhandled()));

        s.setTag("diagnostics", Boolean.toString(ee.isShouldSendDiagnostics()));
        s.setTag("licenseRequired", Boolean.toString(ee.isLicenseRequired()));
        s.setTag(
                "fallbackShell",
                AppPrefs.get() != null
                        ? String.valueOf(AppPrefs.get().useLocalFallbackShell().get())
                        : "unknown");
        s.setTag("initial", AppProperties.get() != null ? AppProperties.get().isInitialLaunch() + "" : "false");

        var exMessage = ee.getThrowable() != null ? ee.getThrowable().getMessage() : null;
        if (ee.getDescription() != null
                && !ee.getDescription().equals(exMessage)
                && (ee.isShouldSendDiagnostics() || ee.isLicenseRequired())) {
            s.setTag("message", ee.getDescription().lines().collect(Collectors.joining(" ")));
        }

        var user = new User();
        user.setId(AppProperties.get().getUuid().toString());
        user.setGeo(new Geo());
        s.setUser(user);
    }

    public void handle(ErrorEvent ee) {
        // Assume that this object is wrapped by a synchronous error handler
        if (!init) {
            AppProperties.init();
            if (AppProperties.get().getSentryUrl() != null) {
                Sentry.init(options -> {
                    options.setDsn(AppProperties.get().getSentryUrl());
                    options.setEnableUncaughtExceptionHandler(false);
                    options.setAttachServerName(false);
                    options.setRelease(AppProperties.get().getVersion());
                    options.setEnableShutdownHook(false);
                    options.setProguardUuid(AppProperties.get().getBuildUuid().toString());
                    options.setTag("os", System.getProperty("os.name"));
                    options.setTag("osVersion", System.getProperty("os.version"));
                    options.setTag("arch", AppProperties.get().getArch());
                    options.setDist(AppDistributionType.get().getId());
                    options.setTag("staging", String.valueOf(AppProperties.get().isStaging()));
                    options.setSendModules(false);
                    options.setAttachThreads(false);
                    options.setEnableDeduplication(false);
                    options.setCacheDirPath(
                            AppProperties.get().getDataDir().resolve("cache").toString());
                });
            }
            init = true;
        }

        var id = captureEvent(ee);
        if (id == null) {
            return;
        }

        var email = ee.getEmail();
        var hasEmail = email != null && !email.isBlank();
        var text = ee.getUserReport();
        if (hasUserReport(ee)) {
            var fb = new UserFeedback(id);
            if (hasEmail) {
                fb.setEmail(email);
            }
            if (doesExceedCommentSize(text)) {
                fb.setComments("<Attachment>");
            } else {
                fb.setComments(text);
            }
            Sentry.captureUserFeedback(fb);
        }
        Sentry.flush(3000);
    }
}
