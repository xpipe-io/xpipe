package io.xpipe.api;

import java.util.function.IntConsumer;

public class IntConverter {

    private IntConsumer consumer;

    public IntConverter(IntConsumer consumer) {
        this.consumer = consumer;
    }

    public void onValue(byte[] value) {
        if (value.length > 4) {
            throw new IllegalArgumentException("Unable to fit " + value.length + " bytes into an integer");
        }

        int v = value[0] << 24 | (value[1] & 0xFF) << 16 | (value[2] & 0xFF) << 8 | (value[3] & 0xFF);
        consumer.accept(v);
    }
}
