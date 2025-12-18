package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.core.FailableConsumer;
import io.xpipe.core.OsType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

public class AppExecutableCache {

    public static synchronized Path getOrInstall(String name, String url, FailableConsumer<Path, Exception> function) throws Exception {
        // We want a recent version installed
        // Just checking the path might result in old packages that don't support certain options
        var file = AppProperties.get()
                .getDataBinDir()
                .resolve(name + (OsType.ofLocal() == OsType.WINDOWS ? ".exe" : ""));
        var exists = Files.exists(file);
        if (exists) {
            var date = Files.getLastModifiedTime(file).toInstant();
            var elapsed = Duration.between(date, Instant.now());
            if (elapsed.toDays() < 30) {
                return file;
            }
        }

        var queueEntry = new AppLayoutModel.QueueEntry(
                AppI18n.observable("downloadInProgress", name),
                new LabelGraphic.IconGraphic("mdi2d-download"),
                () -> {
                    Hyperlinks.open(url);
                });
        AppLayoutModel.get().getQueueEntries().add(queueEntry);

        try {
            function.accept(file);
            return file;
        } catch (Exception e) {
            // We can still use the old file
            if (exists) {
                ErrorEventFactory.fromThrowable(e).omit().expected().handle();
                return file;
            } else  {
                throw e;
            }
        } finally {
            AppLayoutModel.get().getQueueEntries().remove(queueEntry);
        }
    }
}
