package io.xpipe.app.util;

import com.sun.jna.Library;
import com.sun.jna.Native;
import io.xpipe.app.issue.ErrorEventFactory;

import java.util.Optional;

public class LinuxLibC {

    private static LibC library;
    private static boolean loadingFailed;

    public static synchronized Optional<LibC> getLibrary() {
        if (loadingFailed) {
            return Optional.empty();
        }

        if (library != null) {
            return Optional.of(library);
        }

        try {
            LibC libc = Native.load("c", LibC.class);
            return Optional.of((library = libc));
        }  catch (Throwable t) {
            ErrorEventFactory.fromThrowable(t).handle();
            loadingFailed = true;
            return Optional.empty();
        }
    }

    public interface LibC extends Library {

        int chmod(String path, int mode);

        int setsid();
    }
}

