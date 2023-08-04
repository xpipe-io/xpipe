package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEvent;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;

public class AppDataLock {

    private static FileChannel channel;
    private static FileLock lock;

    private static Path getLockFile() {
        return AppProperties.get().getDataDir().resolve("lock");
    }

    public static boolean lock() {
        try {
            var file = getLockFile().toFile();
            Files.createDirectories(file.toPath().getParent());
            if (!Files.exists(file.toPath())) {
                Files.createFile(file.toPath());
            }
            channel = new RandomAccessFile(file, "rw").getChannel();
            lock = channel.tryLock();
            return lock != null;
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).build().handle();
            return false;
        }
    }

    public static void unlock() {
        if (channel == null || lock == null) {
            return;
        }

        try {
            lock.release();
            channel.close();
            lock = null;
            channel = null;
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).build().handle();
        }
    }
}
