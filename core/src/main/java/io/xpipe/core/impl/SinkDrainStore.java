package io.xpipe.core.impl;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.store.DataFlow;
import io.xpipe.core.store.KnownFormatStreamDataStore;
import io.xpipe.core.store.StatefulDataStore;
import io.xpipe.core.util.JacksonizedValue;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;

@JsonTypeName("drain")
@SuperBuilder
@Jacksonized
@Getter
public class SinkDrainStore extends JacksonizedValue implements KnownFormatStreamDataStore, StatefulDataStore {

    public static enum State {
        NONE_CONNECTED,
        PRODUCER_CONNECTED,
        CONSUMER_CONNECTED,
        OPEN,
        CLOSED
    }

    private final StreamCharset charset;
    private final NewLine newLine;

    public State getState() {
        return getState("state", State.class, State.NONE_CONNECTED);
    }

    private void setState(State n) {
        setState("state", n);
    }

    public Pipe getOrOpenPipe() {
        return getOrComputeState("pipe", Pipe.class, () -> {
            try {
                return Pipe.open();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public DataFlow getFlow() {
        if (getState() == State.NONE_CONNECTED) {
            return DataFlow.INPUT_OR_OUTPUT;
        }

        if (getState() == State.PRODUCER_CONNECTED) {
            return DataFlow.INPUT;
        }

        if (getState() == State.CONSUMER_CONNECTED) {
            return DataFlow.OUTPUT;
        }

        return null;
    }

    private void waitForOpen() {
        while (getState() != State.OPEN) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Override
    public boolean shouldPersist() {
        return getState() != State.CLOSED;
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    public boolean canOpen() throws Exception {
        return getState() == State.PRODUCER_CONNECTED;
    }

    @Override
    public InputStream openInput() throws Exception {
        checkState(false);

        if (getState() == State.PRODUCER_CONNECTED) {
            setState(State.OPEN);
        }

        if (getState() == State.NONE_CONNECTED) {
            setState(State.CONSUMER_CONNECTED);
            //waitForOpen();
        }

        try {
            return new FilterInputStream(Channels.newInputStream(getOrOpenPipe().source())) {
                @Override
                public void close() throws IOException {
                    super.close();
                    setState(State.CLOSED);
                }
            };
        } catch (Exception ex) {
            setState(State.CLOSED);
            throw ex;
        }
    }

    @Override
    public OutputStream openOutput() throws Exception {
        checkState(true);

        if (getState() == State.CONSUMER_CONNECTED) {
            setState(State.OPEN);
        }

        if (getState() == State.NONE_CONNECTED) {
            setState(State.PRODUCER_CONNECTED);
            //waitForOpen();
        }

        try {
            return new FilterOutputStream(Channels.newOutputStream(getOrOpenPipe().sink())) {
                @Override
                public void close() throws IOException {
                    super.close();
                    setState(State.CLOSED);
                }
            };
        } catch (Exception ex) {
            setState(State.CLOSED);
            throw ex;
        }
    }

    private void checkState(boolean isProducer) {
        if (getState() == State.CLOSED) {
            throw new IllegalStateException("Drain has already been closed");
        }

        if (getState() == State.OPEN) {
            throw new IllegalStateException("Drain is already open");
        }

        if (getState() == State.PRODUCER_CONNECTED && isProducer) {
            throw new IllegalStateException("Producer is already connected");
        }

        if (getState() == State.CONSUMER_CONNECTED && !isProducer) {
            throw new IllegalStateException("Consumer is already connected");
        }
    }
}
