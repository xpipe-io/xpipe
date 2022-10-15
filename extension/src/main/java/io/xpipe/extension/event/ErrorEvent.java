package io.xpipe.extension.event;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
public class ErrorEvent {

    private final List<TrackEvent> trackEvents = EventHandler.get().snapshotEvents();
    private String description;
    private boolean terminal;
    @Builder.Default
    private boolean omitted = false;
    @Builder.Default
    private boolean reportable = true;
    private Throwable throwable;
    @Singular
    private List<Path> attachments;

    public static ErrorEventBuilder fromThrowable(Throwable t) {
        return builder().throwable(t).description(ExceptionConverter.convertMessage(t));
    }

    public static ErrorEventBuilder fromThrowable(String msg, Throwable t) {
        return builder().throwable(t).description(msg);
    }

    public static ErrorEventBuilder fromMessage(String msg) {
        return builder().description(msg);
    }

    public void handle() {
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

        public ErrorEventBuilder omit() {
            return omitted(true);
        }

        public void handle() {
            build().handle();
        }
    }
}
