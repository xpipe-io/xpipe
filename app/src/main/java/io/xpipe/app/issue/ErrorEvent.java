package io.xpipe.app.issue;

import io.xpipe.app.util.DocumentationLink;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;

import java.nio.file.Path;
import java.util.*;

@Builder
@Getter
public class ErrorEvent {

    // Throwable does not override equals, so this is an identity set
    // Weak references ensure that handled throwables can still be garbage collected
    private static final Set<Throwable> HANDLED =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

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
        // Guard against cause cycles
        var visited = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        var current = throwable;
        while (current != null && visited.add(current)) {
            if (HANDLED.contains(current) && !terminal) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public void handle() {
        // Check object identity to allow for multiple exceptions with same trace
        if (shouldIgnore(throwable)) {
            return;
        }

        EventHandler.get().modify(this);
        EventHandler.get().handle(this);
        if (throwable != null) {
            HANDLED.add(throwable);
        }
    }

    public void addAttachment(Path file) {
        attachments = new ArrayList<>(attachments);
        attachments.add(file);
    }

    public void clearAttachments() {
        attachments = new ArrayList<>();
    }

    public static class ErrorEventBuilder {

        public void apply(ErrorEventBuilder other) {
            var e = other.build();
            if (!e.reportable) {
                expected();
            }
            if (e.terminal) {
                term();
            }
            if (e.link != null) {
                link = e.link;
            }
            if (e.description != null) {
                description = e.description;
            }
            if (e.omitted) {
                omit();
            }
        }

        public ErrorEventBuilder description(String s) {
            if (description != null) {
                description = s + "\n\n" + description;
            } else {
                description = s;
            }
            return this;
        }

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

        public ErrorEventBuilder ignore() {
            if (throwable != null) {
                HANDLED.add(throwable);
            }
            return this;
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
                            .contains(string));
            if (contains) {
                expected();
            }
        }

        Throwable getThrowable() {
            return throwable;
        }
    }
}
