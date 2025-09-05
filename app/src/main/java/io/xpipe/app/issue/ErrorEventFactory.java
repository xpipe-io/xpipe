package io.xpipe.app.issue;

import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.process.ProcessOutputException;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.core.OsType;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import javax.net.ssl.SSLHandshakeException;

public class ErrorEventFactory {

    private static final Map<Throwable, ErrorEvent.ErrorEventBuilder> EVENT_BASES = new IdentityHashMap<>();

    public static ErrorEvent.ErrorEventBuilder fromThrowable(Throwable t) {
        var b = retrieveBuilder(t);
        return b;
    }

    public static ErrorEvent.ErrorEventBuilder fromThrowable(String msg, Throwable t) {
        var b = retrieveBuilder(t);
        return b.description(msg);
    }

    public static ErrorEvent.ErrorEventBuilder fromMessage(String msg) {
        return ErrorEvent.builder().description(msg);
    }

    public static <T extends Throwable> T expectedIfContains(T t, String... s) {
        return expectedIf(
                t,
                t.getMessage() != null
                        && Arrays.stream(s).map(String::toLowerCase).anyMatch(string -> t.getMessage()
                                .toLowerCase(Locale.ROOT)
                                .contains(string)));
    }

    public static <T extends Throwable> T expectedIf(T t, boolean b) {
        if (b) {
            preconfigure(fromThrowable(t).expected());
        }
        return t;
    }

    public static <T extends Throwable> T expected(T t) {
        preconfigure(fromThrowable(t).expected());
        return t;
    }

    public static synchronized void preconfigure(ErrorEvent.ErrorEventBuilder event) {
        EVENT_BASES.put(event.getThrowable(), event);
    }

    private static synchronized ErrorEvent.ErrorEventBuilder retrieveBuilder(Throwable t) {
        var b = EVENT_BASES.remove(t);
        if (b == null) {
            b = ErrorEvent.builder().throwable(t);
        }

        if (t instanceof SSLHandshakeException) {
            if (b.getLink() == null) {
                b.documentationLink(DocumentationLink.TLS_DECRYPTION);
            }
            b.expected();
        }

        // Indicates that the session is scheduled to end and new processes won't be started
        if (OsType.getLocal() == OsType.WINDOWS
                && t instanceof ProcessOutputException pex
                && pex.getExitCode() == -1073741205) {
            b.expected();
        }

        // On Linux shutdown, active file descriptors are getting closed. This breaks shell commands
        if (OsType.getLocal() == OsType.LINUX
                && AppOperationMode.isInShutdown()
                && t instanceof IllegalStateException ise
                && "Parent stream is closed".equals(ise.getMessage())) {
            b.expected();
        }

        return b;
    }
}
