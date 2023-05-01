package io.xpipe.app.issue;

import io.sentry.*;
import io.sentry.protocol.SentryId;
import io.sentry.protocol.User;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.update.XPipeDistributionType;
import org.apache.commons.io.FileUtils;

import java.nio.file.Files;
import java.util.Date;
import java.util.stream.Collectors;

public class SentryErrorHandler implements ErrorHandler {

    private static final ErrorHandler INSTANCE = new SyncErrorHandler(new SentryErrorHandler());

    public static ErrorHandler getInstance() {
        return INSTANCE;
    }

    private boolean init;

    public void handle(ErrorEvent ee) {
        // Assume that this object is wrapped by a synchronous error handler
        if (!init) {
            AppProperties.init();
            if (AppProperties.get().getSentryUrl() != null) {
                Sentry.init(options -> {
                    options.setDsn(AppProperties.get().getSentryUrl());
                    options.setEnableUncaughtExceptionHandler(false);
                    options.setAttachServerName(false);
                    // options.setDebug(true);
                    options.setRelease(AppProperties.get().getVersion());
                    options.setEnableShutdownHook(false);
                    options.setProguardUuid(AppProperties.get().getBuildUuid().toString());
                    options.setTag("os", System.getProperty("os.name"));
                    options.setTag("osVersion", System.getProperty("os.version"));
                    options.setTag("arch", System.getProperty("os.arch"));
                    options.setDist(XPipeDistributionType.get().getId());
                    if (AppProperties.get().isStaging()) {
                        options.setTag("staging", "true");
                    }
                });
            }
            init = true;
        }

        var id = createReport(ee);
        var text = ee.getUserReport();
        if (text != null && text.length() > 0) {
            var fb = new UserFeedback(id);
            fb.setComments(text);
            Sentry.captureUserFeedback(fb);
        }
    }

    private static SentryId createReport(ErrorEvent ee) {
        /*
        TODO: Ignore breadcrumbs for now
         */
        // ee.getTrackEvents().forEach(t -> s.addBreadcrumb(toBreadcrumb(t)));

        if (ee.getThrowable() != null) {
            return Sentry.captureException(ee.getThrowable(), sc -> fillScope(ee, sc));
        }

        if (ee.getDescription() != null) {
            return Sentry.captureMessage(ee.getDescription(), sc -> fillScope(ee, sc));
        }

        var event = new SentryEvent();
        return Sentry.captureEvent(event, sc -> fillScope(ee, sc));
    }

    private static void fillScope(ErrorEvent ee, Scope s) {
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

        s.setTag("initError", String.valueOf(OperationMode.isInStartup()));
        s.setTag("developerMode", AppPrefs.get() != null ? AppPrefs.get().developerMode().getValue().toString() : "false");
        s.setTag("terminal", Boolean.toString(ee.isTerminal()));
        s.setTag("omitted", Boolean.toString(ee.isOmitted()));
        if (ee.getThrowable() != null) {
            if (ee.getDescription() != null
                    && !ee.getDescription().equals(ee.getThrowable().getMessage())) {
                s.setTag("message", ee.getDescription().lines().collect(Collectors.joining(" ")));
            }
        }

        var user = new User();
        user.setId(AppCache.getCachedUserId().toString());
        s.setUser(user);
    }

    private static Breadcrumb toBreadcrumb(TrackEvent te) {
        var bc = new Breadcrumb(Date.from(te.getInstant()));
        bc.setLevel(
                te.getType().equals("trace") || te.getType().equals("debug")
                        ? SentryLevel.DEBUG
                        : te.getType().equals("warn")
                                ? SentryLevel.WARNING
                                : SentryLevel.valueOf(te.getType().toUpperCase()));
        bc.setType(bc.getLevel().toString().toLowerCase());
        bc.setCategory(te.getCategory());
        // bc.setData("thread", te.getThread().getName());
        bc.setMessage(te.getMessage());
        te.getTags().forEach((k, v) -> {
            var toUse = v;
            if (v instanceof Double d && (d.isNaN() || d.isInfinite())) {
                toUse = d.toString();
            }
            if (v instanceof Float f && (f.isNaN() || f.isInfinite())) {
                toUse = f.toString();
            }
            bc.setData(k, toUse);
        });
        if (te.getElements().size() > 0) {
            bc.setData("elements", te.getElements());
        }
        return bc;
    }
}
