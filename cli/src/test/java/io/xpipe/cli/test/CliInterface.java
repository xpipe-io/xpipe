package io.xpipe.cli.test;

import io.xpipe.cli.Main;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.Value;
import org.junit.jupiter.api.Assertions;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class CliInterface {

    public static CliInterface get() {
        if (System.getenv("CLI_TEST_PROD") != null) {
            return new ProductionInterface();
        } else {
            return new TestInterface();
        }
    }

    public abstract static class OutputCapture implements Closeable, AutoCloseable {

        public abstract void checkEquals(Path compare) throws IOException;

        public abstract void checkEquals(InputStream compare) throws IOException;
    }

    public abstract OutputCapture execAndCapture(List<String> args, InputStream in) throws Exception;

    public abstract void exec(List<String> args, InputStream in) throws Exception;

    public static class ProductionInterface extends CliInterface {

        @Override
        public OutputCapture execAndCapture(List<String> args, InputStream in) throws Exception {
            var all = new ArrayList<String>();
            all.add("xpipe");
            all.addAll(args);

            var file = Files.createTempFile(null, null);
            Files.write(file, in.readAllBytes());

            var proc = new ProcessBuilder(all).redirectInput(file.toFile()).redirectError(ProcessBuilder.Redirect.PIPE).start();
            return new ProductionOutputCapture(proc);
        }

        @Override
        public void exec(List<String> args, InputStream in) throws Exception {
            var all = new ArrayList<String>();
            all.add("xpipe");
            all.addAll(args);

            var file = Files.createTempFile(null, null);
            Files.write(file, in.readAllBytes());

            var process = new ProcessBuilder(all)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .redirectInput(file.toFile())
                    .start();
            if (process.waitFor() != 0) {
                throw new IOException(all.toString() + " returned with " + process.exitValue());
            }
        }

        @Value
        @EqualsAndHashCode(callSuper = false)
        public static class ProductionOutputCapture extends CliInterface.OutputCapture {

            Process process;

            public void checkEquals(Path compare) throws IOException {
                try (var in = Files.newInputStream(compare)) {
                    checkEquals(in);
                }
            }

            public void checkEquals(InputStream compare) throws IOException {
                var b = compare.readAllBytes();
                Assertions.assertArrayEquals(b, get());
            }

            @SneakyThrows
            public byte[] get() throws IOException {
                var bytes = process.getInputStream().readAllBytes();
                System.out.println(new String(bytes));

                if (process.waitFor() != 0) {
                    throw new IOException("Returned with " + process.exitValue());
                }

                return bytes;
            }

            @Override
            @SneakyThrows
            public void close() {
                process.waitFor();
            }
        }
    }

    public static class TestInterface extends CliInterface {

        @Override
        public OutputCapture execAndCapture(List<String> args, InputStream in) throws Exception {
            var cap = new TestOutputCapture();
            System.setIn(in);
            Assertions.assertEquals(Main.mainInternal(args.toArray(String[]::new)), 0);
            return cap;
        }

        @Override
        public void exec(List<String> args, InputStream in) throws Exception {
            System.setIn(in);
            Assertions.assertEquals(Main.mainInternal(args.toArray(String[]::new)), 0);
        }

        @Value
        @EqualsAndHashCode(callSuper = false)
        public static class TestOutputCapture extends CliInterface.OutputCapture {

            ByteArrayOutputStream out;
            PrintStream stream;
            PrintStream sysout;

            public TestOutputCapture() {
                out = new ByteArrayOutputStream(100000);
                stream = new PrintStream(out);
                sysout = System.out;
                System.setOut(stream);
            }

            public void checkEquals(Path compare) throws IOException {
                try (var in = Files.newInputStream(compare)) {
                    checkEquals(in);
                }
            }

            public void checkEquals(InputStream compare) throws IOException {
                var b = compare.readAllBytes();
                Assertions.assertArrayEquals(b, get());
            }

            public byte[] get() {
                return out.toByteArray();
            }

            @Override
            public void close() {
                System.setOut(sysout);
            }
        }
    }
}
