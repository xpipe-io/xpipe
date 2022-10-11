package io.xpipe.beacon;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

@UtilityClass
public class BeaconFormat {

    private static final int SEGMENT_SIZE = 65536;

    public static OutputStream writeBlocks(OutputStream out) throws IOException {
        return new OutputStream() {
            private final byte[] currentBytes = new byte[SEGMENT_SIZE];
            private int index;

            @Override
            public void close() throws IOException {
                if (isClosed()) {
                    return;
                }

                finishBlock();
                out.flush();
                index = -1;
            }

            @Override
            public void write(int b) throws IOException {
                if (isClosed()) {
                    throw new IllegalStateException("Output is closed");
                }

                if (index == currentBytes.length) {
                    finishBlock();
                }

                currentBytes[index] = (byte) b;
                index++;
            }

            private boolean isClosed() {
                return index == -1;
            }

            private void finishBlock() throws IOException {
                if (isClosed()) {
                    throw new IllegalStateException("Output is closed");
                }

                if (BeaconConfig.printMessages()) {
                    System.out.println("Sending data block of length " + index);
                }

                int length = index;
                var lengthBuffer = ByteBuffer.allocate(4).putInt(length);
                out.write(lengthBuffer.array());
                out.write(currentBytes, 0, length);
                index = 0;
            }
        };
    }

    public static InputStream readBlocks(InputStream in) throws IOException {
        return new InputStream() {

            private byte[] currentBytes;
            private int index;
            private boolean lastBlock;

            @Override
            public int read() throws IOException {
                if ((currentBytes == null || index == currentBytes.length) && !lastBlock) {
                    if (!readBlock()) {
                        return -1;
                    }
                }

                if (currentBytes != null && index == currentBytes.length && lastBlock) {
                    return -1;
                }

                int out = currentBytes[index] & 0xff;
                index++;
                return out;
            }

            private boolean readBlock() throws IOException {
                var length = in.readNBytes(4);
                if (length.length < 4) {
                    return false;
                }

                var lengthInt = ByteBuffer.wrap(length).getInt();

                if (BeaconConfig.printMessages()) {
                    System.out.println("Receiving data block of length " + lengthInt);
                }

                currentBytes = in.readNBytes(lengthInt);
                index = 0;
                if (lengthInt < SEGMENT_SIZE) {
                    lastBlock = true;
                }
                return true;
            }
        };
    }
}
