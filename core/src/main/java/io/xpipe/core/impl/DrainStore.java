package io.xpipe.core.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.store.KnownFormatStreamDataStore;
import io.xpipe.core.util.JacksonizedValue;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;

@JsonTypeName("drain")
@SuperBuilder
@Jacksonized
@Getter
public class DrainStore extends JacksonizedValue implements KnownFormatStreamDataStore {

    private final String description;
    private final StreamCharset charset;
    private final NewLine newLine;

    @JsonIgnore
    private boolean open;
    @JsonIgnore
    private Pipe pipe;

    private boolean used;

    private void waitForOpen() {
        while (!open) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Override
    public boolean canOpen() throws Exception {
        return false;
    }

    @Override
    public InputStream openInput() throws Exception {
        if (used) {
            throw new IllegalStateException("Drain has already been used");
        }

        waitForOpen();
        return Channels.newInputStream(pipe.source());
    }

    @Override
    public OutputStream openOutput() throws Exception {
        used = true;
        pipe = Pipe.open();
        open = true;
        return Channels.newOutputStream(pipe.sink());
    }
}
