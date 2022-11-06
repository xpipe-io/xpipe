package io.xpipe.core.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.store.DataFlow;
import io.xpipe.core.store.KnownFormatStreamDataStore;
import io.xpipe.core.util.JacksonizedValue;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;

/*
TODO: Properly enter closed State
 */

@JsonTypeName("drain")
@SuperBuilder
@Jacksonized
@Getter
public class SinkDrainStore extends JacksonizedValue implements KnownFormatStreamDataStore {

    public static enum State {
        NONE_CONNECTED,
        PRODUCER_CONNECTED,
        CONSUMER_CONNECTED,
        OPEN,
        CLOSED
    }

    private final String description;
    private final StreamCharset charset;
    private final NewLine newLine;

    @JsonIgnore
    @Setter
    @Builder.Default
    private State state = State.NONE_CONNECTED;

    @JsonIgnore
    private Pipe pipe;

    @Override
    public DataFlow getFlow() {
        if (state == State.NONE_CONNECTED) {
            return DataFlow.INPUT_OR_OUTPUT;
        }

        if (state == State.PRODUCER_CONNECTED) {
            return DataFlow.INPUT;
        }

        if (state == State.CONSUMER_CONNECTED) {
            return DataFlow.OUTPUT;
        }

        return null;
    }

    private void waitForOpen() {
        while (state != State.OPEN) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Override
    public boolean canOpen() throws Exception {
        return state == State.PRODUCER_CONNECTED;
    }

    @Override
    public InputStream openInput() throws Exception {
        checkState(false);

        if (state == State.PRODUCER_CONNECTED) {
            state = State.OPEN;
        }

        if (state == State.NONE_CONNECTED) {
            state = State.CONSUMER_CONNECTED;
            waitForOpen();
        }

        try {
            openPipe();
            return Channels.newInputStream(pipe.source());
        } catch (Exception ex) {
            state = State.CLOSED;
            throw ex;
        }
    }

    @Override
    public OutputStream openOutput() throws Exception {
        checkState(true);

        if (state == State.CONSUMER_CONNECTED) {
            state = State.OPEN;
        }

        if (state == State.NONE_CONNECTED) {
            state = State.PRODUCER_CONNECTED;
            waitForOpen();
        }

        try {
            openPipe();
            return Channels.newOutputStream(pipe.sink());
        } catch (Exception ex) {
            state = State.CLOSED;
            throw ex;
        }
    }

    private void openPipe() throws IOException {
        if (pipe == null) {
            pipe = Pipe.open();
        }
    }

    private void checkState(boolean isProducer) {
        if (state == State.CLOSED) {
            throw new IllegalStateException("Drain has already been closed");
        }

        if (state == State.OPEN) {
            throw new IllegalStateException("Drain is already open");
        }

        if (state == State.PRODUCER_CONNECTED && isProducer) {
            throw new IllegalStateException("Producer is already connected");
        }

        if (state == State.CONSUMER_CONNECTED && !isProducer) {
            throw new IllegalStateException("Consumer is already connected");
        }
    }
}
