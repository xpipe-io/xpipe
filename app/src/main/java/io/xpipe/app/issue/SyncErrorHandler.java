package io.xpipe.app.issue;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class SyncErrorHandler implements ErrorHandler {

    private final Queue<ErrorEvent> eventQueue = new LinkedBlockingQueue<>();
    private final ErrorHandler errorHandler;
    private final AtomicBoolean busy = new AtomicBoolean();

    public SyncErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public void handle(ErrorEvent event) {
        synchronized (busy) {
            if (busy.get()) {
                synchronized (eventQueue) {
                    eventQueue.add(event);
                }
                return;
            }
            busy.set(true);
        }

        errorHandler.handle(event);
        synchronized (eventQueue) {
            eventQueue.forEach(errorEvent -> {
                System.out.println("Event happened during error handling: " + errorEvent.toString());
            });
            eventQueue.clear();
        }
        busy.set(false);
    }
}
