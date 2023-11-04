package io.xpipe.app.issue;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Builder
@Getter
public class ErrorEvent {

    private static final Map<Throwable, ErrorEventBuilder> EVENT_BASES = new ConcurrentHashMap<>();
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

    public static <T extends Throwable> T unreportableIfEndsWith(T t, String... s) {
        return unreportableIf(t, t.getMessage() != null && Arrays.stream(s).map(String::toLowerCase)
                .anyMatch(string -> t.getMessage().toLowerCase(Locale.ROOT).endsWith(string)));
    }

    public static <T extends Throwable> T unreportableIfContains(T t, String... s) {
        return unreportableIf(t, t.getMessage() != null && Arrays.stream(s).map(String::toLowerCase)
                .anyMatch(string -> t.getMessage().toLowerCase(Locale.ROOT).contains(string)));
    }

    public static <T extends Throwable> T unreportableIf(T t, boolean b) {
        if (b) {
            EVENT_BASES.put(t, ErrorEvent.fromThrowable(t).expected());
        }
        return t;
    }

    public static <T extends Throwable> T unreportable(T t) {
        EVENT_BASES.put(t, ErrorEvent.fromThrowable(t).expected());
        return t;
    }

    public void attachUserReport(String email, String text) {
        this.email = email;
        userReport = text;
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
