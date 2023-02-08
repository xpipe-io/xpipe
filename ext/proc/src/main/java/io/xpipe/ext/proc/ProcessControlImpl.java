package io.xpipe.ext.proc;

import io.xpipe.core.process.ProcessControl;
import io.xpipe.extension.event.TrackEvent;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public abstract class ProcessControlImpl implements ProcessControl {

    protected Charset charset;
    protected boolean running;
    protected boolean sensitive;

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void writeLine(String line) throws IOException {
        if (isStdinClosed()) {
            throw new IllegalStateException("Input is closed");
        }

        // Censor actual written line to prevent leaking sensitive information
        TrackEvent.withTrace("proc", "Writing line").tag("line", line).handle();
        getStdin().write((line + "\n").getBytes(getCharset()));
        getStdin().flush();
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (isStdinClosed()) {
            throw new IllegalStateException("Input is closed");
        }

        getStdin().write(b);
        getStdin().flush();
    }

    @Override
    public final Charset getCharset() {
        return charset != null ? charset : StandardCharsets.US_ASCII;
    }
}
