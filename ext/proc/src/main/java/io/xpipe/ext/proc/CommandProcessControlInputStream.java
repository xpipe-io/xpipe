package io.xpipe.ext.proc;

import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

class CommandProcessControlInputStream extends InputStream {

    private static final Timer TIMEOUT_TIMER = new Timer(true);
    private static final int TIMEOUT = 5000;
    final byte[] startSearch;
    final byte[] endSearch;
    private final InputStream in;
    private final Consumer<FinishReason> finishFunction;
    private final ByteArrayOutputStream preStartContent = new ByteArrayOutputStream(128);
    LinkedList<Integer> inQueue = new LinkedList<Integer>();
    private boolean timerStarted;
    private boolean foundEnd;
    private boolean foundStart;
    private boolean finished;
    protected CommandProcessControlInputStream(
            InputStream in, byte[] startSearch, byte[] endSearch, Consumer<FinishReason> finishFunction) {
        this.in = in;
        this.startSearch = startSearch;
        this.endSearch = endSearch;
        this.finishFunction = finishFunction;
    }

    public byte[] getPreStartContent() {
        return preStartContent.toByteArray();
    }

    @Override
    public void close() throws IOException {
        in.close();
        finish(FinishReason.EXTERNAL_CLOSE);
    }

    private synchronized void finish(FinishReason r) {
        if (finished) {
            return;
        }

        finished = true;
        finishFunction.accept(r);
    }

    private boolean isStartMatchFound() {
        Iterator<Integer> inIter = inQueue.iterator();
        for (byte b : startSearch) if (!inIter.hasNext() || b != inIter.next()) return false;
        return true;
    }

    private boolean isEndMatchFound() {
        Iterator<Integer> inIter = inQueue.iterator();
        for (byte b : endSearch) if (!inIter.hasNext() || b != inIter.next()) return false;
        return true;
    }

    private void readAhead() throws IOException {
        // Work up some look-ahead.
        while (inQueue.size() < (!foundStart ? startSearch.length : endSearch.length)) {
            int next = in.read();
            if (next == -1) break;
            inQueue.offer(next);
        }
    }

    @Override
    public int read() throws IOException {
        if (!timerStarted) {
            timerStarted = true;
            TIMEOUT_TIMER.schedule(
                    new TimerTask() {
                        @Override
                        @SneakyThrows
                        public void run() {
                            if (!CommandProcessControlInputStream.this.foundStart) {
                                inQueue.forEach(integer -> preStartContent.write(integer));
                                finish(FinishReason.START_TIMEOUT);
                            }
                        }
                    },
                    TIMEOUT);
        }

        if (finished) {
            return -1;
        }

        if (foundEnd) {
            return -1;
        }

        readAhead();
        if (inQueue.size() == 0) {
            finish(FinishReason.INPUT_ENDED_PREMATURELY);
            return -1;
        }

        if (!foundStart) {
            if (isStartMatchFound()) {
                for (int i = 0; i < startSearch.length; i++) {
                    inQueue.remove();
                }

                foundStart = true;
                return read();
            } else {
                var r = inQueue.remove();
                preStartContent.write(r);
                // System.out.print((char) r.intValue());
                return read();
            }
        }

        if (!foundEnd) {
            if (isEndMatchFound()) {
                for (int i = 0; i < endSearch.length; i++) {
                    inQueue.remove();
                }

                foundEnd = true;
                finish(FinishReason.NORMAL_FINISH);
                return -1;
            } else {
                var r = inQueue.remove();
                // System.out.print((char) r.intValue());
                return r;
            }
        }

        return -1;
    }

    public static enum FinishReason {
        START_TIMEOUT,
        INPUT_ENDED_PREMATURELY,
        EXTERNAL_CLOSE,
        NORMAL_FINISH,
    }

    // TODO: Override the other read methods.
}
