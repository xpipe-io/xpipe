package io.xpipe.app.issue;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Builder
@Getter
public class ErrorEvent {

    private static final Map<Throwable, ErrorEventBuilder> EVENT_BASES = new ConcurrentHashMap<>();
    private static final Set<Throwable> HANDLED = new CopyOnWriteArraySet<>();

    @Builder.Default
    private final boolean omitted = false;

    @Builder.Default
    private final boolean reportable = true;

    private final Throwable throwable;

    @Singular
    private final List<ErrorAction> customActions;

    private String description;
    private boolean terminal;

    @Setter
    private boolean shouldSendDiagnostics;

    @Singular
    private List<Path> attachments;

    private String email;
    private String userReport;
    private boolean unhandled;

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

    public static <T extends Throwable> T expectedIfEndsWith(T t, String... s) {
        return expectedIf(
                t,
                t.getMessage() != null
                        && Arrays.stream(s).map(String::toLowerCase).anyMatch(string -> t.getMessage()
                                .toLowerCase(Locale.ROOT)
                                .endsWith(string)));
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
            EVENT_BASES.put(t, ErrorEvent.fromThrowable(t).expected());
        }
        return t;
    }

    public static <T extends Throwable> T expected(T t) {
        EVENT_BASES.put(t, ErrorEvent.fromThrowable(t).expected());
        return t;
    }

    public static void preconfigure(ErrorEventBuilder event) {
        EVENT_BASES.put(event.throwable, event);
    }

    public void attachUserReport(String email, String text) {
        this.email = email;
        userReport = text;
    }

    public List<Throwable> getThrowableChain() {
        var list = new ArrayList<Throwable>();
        Throwable t = getThrowable();
        while (t != null) {
            list.addFirst(t);
            t = t.getCause();
        }
        return list;
    }

    private boolean shouldIgnore(Throwable throwable) {
        return (throwable != null && HANDLED.stream().anyMatch(t -> t == throwable) && !terminal)
                || (throwable != null && throwable.getCause() != throwable && shouldIgnore(throwable.getCause()));
    }

    public void handle() {
        // Check object identity to allow for multiple exceptions with same trace
        if (shouldIgnore(throwable)) {
            return;
        }

        EventHandler.get().modify(this);
        EventHandler.get().handle(this);
        HANDLED.add(throwable);
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

        public ErrorEventBuilder expected() {
            return reportable(false);
        }

        public ErrorEventBuilder discard() {
            return omit().expected();
        }

        public void handle() {
            build().handle();
        }
    }
}
