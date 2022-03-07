package io.xpipe.beacon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class BeaconFormat {

    public static OutputStream writeBlocks(Socket socket) throws IOException {
        int size = 65536 - 4;
        var out = socket.getOutputStream();
        return new OutputStream() {
            private final byte[] currentBytes = new byte[size];
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
                if (BeaconConfig.debugEnabled()) {
                    System.out.println("Sending data block of length " + index);
                }

                int length = index;
                var lengthBuffer = ByteBuffer.allocate(4).putInt(length);
                out.write(lengthBuffer.array());
                out.write(currentBytes, 0, length);
                index = 0;
            }
        };
//        while (true) {
//            var bytes = in.readNBytes(size);
//            int length = bytes.length;
//            var lengthBuffer = ByteBuffer.allocate(4).putInt(length);
//            socket.getOutputStream().write(lengthBuffer.array());
//            socket.getOutputStream().write(bytes);
//
//            if (length == 0) {
//                return;
//            }
//        }
    }

    public static InputStream readBlocks(Socket socket) throws IOException {
        int size = 65536 - 4;
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

                if (BeaconConfig.debugEnabled()) {
                    System.out.println("Receiving data block of length " + lengthInt);
                }

                currentBytes = in.readNBytes(lengthInt);
                index = 0;
                if (lengthInt < size) {
                    finished = true;
                }
            }
        };
    }
}
