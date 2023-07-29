package io.xpipe.app.issue;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

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

    private Throwable throwable;

    @Singular
    private List<Path> attachments;

    private String userReport;

    public void attachUserReport(String text) {
        userReport = text;
    }

    public static ErrorEventBuilder fromThrowable(Throwable t) {
        var unreportable = UNREPORTABLE.remove(t);
        return builder().throwable(t).reportable(!unreportable).description(ExceptionConverter.convertMessage(t));
    }

    public static ErrorEventBuilder fromThrowable(String msg, Throwable t) {
        var unreportable = UNREPORTABLE.remove(t);
        return builder().throwable(t).reportable(!unreportable).description(msg);
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

        public void handle() {
            build().handle();
        }
    }

    private static Set<Throwable> UNREPORTABLE = new CopyOnWriteArraySet<>();

    public static <T extends Throwable> T unreportableIfEndsWith(T t, String... s) {
        return unreportableIf(t, t.getMessage() != null && Arrays.stream(s).anyMatch(string->t.getMessage().toLowerCase(Locale.ROOT).endsWith(string)));
    }

    public static <T extends Throwable> T unreportableIfContains(T t, String... s) {
        return unreportableIf(t, t.getMessage() != null && Arrays.stream(s).anyMatch(string->t.getMessage().toLowerCase(Locale.ROOT).contains(string)));
    }

    public static <T extends Throwable> T unreportableIf(T t, boolean b) {
        if (b) {
            UNREPORTABLE.add(t);
        }
        return t;
    }

    public static <T extends Throwable> T unreportable(T t) {
        UNREPORTABLE.add(t);
        return t;
    }
}
