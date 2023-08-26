package io.xpipe.app.issue;

import io.xpipe.core.util.FailableRunnable;
import io.xpipe.core.util.FailableSupplier;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Builder
@Getter
public class ErrorEvent {

    private final List<TrackEvent> trackEvents = EventHandler.get().snapshotEvents();
    private String description;
    private boolean terminal;

    @Builder.Default
    private final boolean omitted = false;

    @Builder.Default
    private final boolean reportable = true;

    private final Throwable throwable;

    @Singular
    private List<Path> attachments;

    private String userReport;

    @Singular
    private final List<ErrorAction> customActions;

    public void attachUserReport(String text) {
        userReport = text;
    }

    public static ErrorEventBuilder fromThrowable(Throwable t) {
        if (EVENT_BASES.containsKey(t)) {
            return EVENT_BASES.remove(t).description(ExceptionConverter.convertMessage(t));
        }

        return builder().throwable(t).description(ExceptionConverter.convertMessage(t));
    }

    public static ErrorEventBuilder fromThrowable(String msg, Throwable t) {
        if (EVENT_BASES.containsKey(t)) {
            return EVENT_BASES.remove(t).description(msg);
        }

        return builder().throwable(t).description(msg);
    }

    public static ErrorEventBuilder fromMessage(String msg) {
        return builder().description(msg);
    }

    public void handle() {
        EventHandler.get().modify(this);
        EventHandler.get().handle(this);
    }

    public void addAttachment(Path file) {
        attachments = new ArrayList<>(attachments);
        attachments.add(file);
    }

    public void clearAttachments() {
        attachments = new ArrayList<>();
    }

    public static class ErrorEventBuilder {

        public ErrorEventBuilder term() {
            return terminal(true);
        }

        public ErrorEventBuilder omit() {
            return omitted(true);
        }

        public ErrorEventBuilder unreportable() {
            return reportable(false);
        }

        public ErrorEventBuilder discard() {
            return omit().unreportable();
        }

        public void handle() {
            build().handle();
        }
    }

    private static final Map<Throwable, ErrorEventBuilder> EVENT_BASES = new ConcurrentHashMap<>();

    public static <T extends Throwable> T unreportableIfEndsWith(T t, String... s) {
        return unreportableIf(
                t,
                t.getMessage() != null
                        && Arrays.stream(s).map(String::toLowerCase).anyMatch(string -> t.getMessage()
                                .toLowerCase(Locale.ROOT)
                                .endsWith(string)));
    }

    public static <T extends Throwable> T unreportableIfContains(T t, String... s) {
        return unreportableIf(
                t,
                t.getMessage() != null
                        && Arrays.stream(s).map(String::toLowerCase).anyMatch(string -> t.getMessage()
                                .toLowerCase(Locale.ROOT)
                                .contains(string)));
    }

    public static <T extends Throwable> T unreportableIf(T t, boolean b) {
        if (b) {
            EVENT_BASES.put(t, ErrorEvent.fromThrowable(t).unreportable());
        }
        return t;
    }

    public static <T extends Throwable> T unreportable(T t) {
        EVENT_BASES.put(t, ErrorEvent.fromThrowable(t).unreportable());
        return t;
    }

    public static <T extends Throwable> void unreportableScope(FailableRunnable<T> t) throws T {
        try {
            t.run();
        } catch (Throwable ex) {
            unreportable(ex);
            throw ex;
        }
    }

    public static <V, T extends Throwable> V unreportableScope(FailableSupplier<V, T> t) throws T {
        try {
            return t.get();
        } catch (Throwable ex) {
            unreportable(ex);
            throw ex;
        }
    }
}
