package io.xpipe.app.issue;

import io.xpipe.app.util.DocumentationLink;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

@Builder
@Getter
public class ErrorEvent {

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

    private String link;

    private String email;
    private String userReport;
    private boolean unhandled;

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

        public ErrorEventBuilder documentationLink(DocumentationLink documentationLink) {
            return link(documentationLink.getLink());
        }

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

        public ErrorEvent handle() {
            var event = build();
            event.handle();
            return event;
        }

        public void expectedIfContains(String... s) {
            var contains = throwable != null
                    && throwable.getMessage() != null
                    && Arrays.stream(s).map(String::toLowerCase).anyMatch(string -> throwable
                            .getMessage()
                            .toLowerCase(Locale.ROOT)
                            .endsWith(string));
            if (contains) {
                expected();
            }
        }

        Throwable getThrowable() {
            return throwable;
        }

        String getLink() {
            return link;
        }
    }
}
