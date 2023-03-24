package io.xpipe.app.issue;

import io.sentry.*;
import io.sentry.protocol.SentryId;
import io.sentry.protocol.User;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.util.XPipeDistributionType;
import org.apache.commons.io.FileUtils;

import java.nio.file.Files;
import java.util.Date;

public class SentryErrorHandler {

    public static void init() {
        AppProperties.init();
        if (AppProperties.get().getSentryUrl() != null) {
            Sentry.init(options -> {
                options.setDsn(AppProperties.get().getSentryUrl());
                options.setEnableUncaughtExceptionHandler(false);
                options.setAttachServerName(false);
                // options.setDebug(true);
                options.setDist(XPipeDistributionType.get().getName());
                options.setRelease(AppProperties.get().getVersion());
                options.setEnableShutdownHook(false);
                options.setProguardUuid(AppProperties.get().getBuildUuid().toString());
                options.setTag("os", System.getProperty("os.name"));
            });

            var user = new User();
            user.setId(AppCache.getCachedUserId().toString());
            Sentry.setUser(user);
        }

        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            ErrorEvent.fromThrowable(ex).build().handle();
        });
    }

    public static void report(ErrorEvent e, String text) {
        var id = report(e);
        if (text != null && text.length() > 0) {
            var fb = new UserFeedback(id);
            fb.setComments(text);
            Sentry.captureUserFeedback(fb);
        }
    }

    public static SentryId report(ErrorEvent ee) {
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

        s.setTag("terminal", Boolean.toString(ee.isTerminal()));
        s.setTag("omitted", Boolean.toString(ee.isOmitted()));
        if (ee.getThrowable() != null) {
            if (ee.getDescription() != null
                    && !ee.getDescription().equals(ee.getThrowable().getMessage())) {
                s.setTag("message", ee.getDescription());
            }
        }
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
