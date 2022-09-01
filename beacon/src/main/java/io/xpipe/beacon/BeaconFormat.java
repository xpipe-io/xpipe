package io.xpipe.beacon;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

@UtilityClass
public class BeaconFormat {

    private static final int SEGMENT_SIZE = 65536;

    public static OutputStream writeBlocks(Socket socket) throws IOException {
        var out = socket.getOutputStream();
        return new OutputStream() {
            private final byte[] currentBytes = new byte[SEGMENT_SIZE];
            private int index;

            @Override
            public void close() throws IOException {
                finishBlock();
                out.flush();
            }

            @Override
            public void write(int b) throws IOException {
                if (index == currentBytes.length) {
                    finishBlock();
                }

                currentBytes[index] = (byte) b;
                index++;
            }

            private void finishBlock() throws IOException {
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

    public static InputStream readBlocks(Socket socket) throws IOException {
        var in = socket.getInputStream();
        return new InputStream() {

            private byte[] currentBytes;
            private int index;
            private boolean finished;

            @Override
            public int read() throws IOException {
                if ((currentBytes == null || index == currentBytes.length) && !finished) {
                    readBlock();
                }

                if (currentBytes != null && index == currentBytes.length && finished) {
                    return -1;
                }

                int out = currentBytes[index];
                index++;
                return out;
            }

            private void readBlock() throws IOException {
                var length = in.readNBytes(4);
                var lengthInt = ByteBuffer.wrap(length).getInt();

                if (BeaconConfig.printMessages()) {
                    System.out.println("Receiving data block of length " + lengthInt);
                }

                currentBytes = in.readNBytes(lengthInt);
                index = 0;
                if (lengthInt < SEGMENT_SIZE) {
                    finished = true;
                }
            }
        };
    }
}
