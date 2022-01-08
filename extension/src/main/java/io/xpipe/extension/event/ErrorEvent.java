package io.xpipe.extension.event;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.nio.file.Path;
import java.util.List;

@Builder
@Getter
public class ErrorEvent {

    public static ErrorEventBuilder fromThrowable(Throwable t) {
        return builder().throwable(t)
                .description(ExceptionConverter.convertMessage(t));
    }

    public static ErrorEventBuilder fromThrowable(String msg, Throwable t) {
        return builder().throwable(t)
                .description(msg);
    }

    public void handle() {
        EventHandler.get().handle(this);
    }

    private String description;

    private boolean terminal;

    @Builder.Default
    private boolean omitted = false;

    @Builder.Default
    private boolean reportable = true;

    private Throwable throwable;

    @Singular
    private List<Path> diagnostics;

    @Singular
    private List<Path> sensitiveDiagnostics;

    private final List<TrackEvent> trackEvents = EventHandler.get().snapshotEvents();
}
