package io.xpipe.extension.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Optional;

public class WindowsRegistry {

    public static Optional<String> readRegistry(String location, String key) {
        try {
            Process process =
                    Runtime.getRuntime().exec("reg query " + '"' + location + "\"" + (key != null ? " /v " + key : " /ve"));

            StreamReader reader = new StreamReader(process.getInputStream());
            reader.start();
            process.waitFor();
            reader.join();
            String output = reader.getResult();

            // Output has the following format:
            // \n<Version information>\n\n<key>\t<registry type>\t<value>
            if (output.contains("\t")) {
                String[] parsed = output.split("\t");
                return Optional.of(parsed[parsed.length - 1]);
            }

            if (output.contains("    ")) {
                String[] parsed = output.split("    ");
                return Optional.of(parsed[parsed.length - 1].substring(0, parsed[parsed.length - 1].length() - 4));
            }

            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    static class StreamReader extends Thread {
        private final InputStream is;
        private final StringWriter sw = new StringWriter();

        public StreamReader(InputStream is) {
            this.is = is;
        }

        public void run() {
            try {
                int c;
                while ((c = is.read()) != -1) sw.write(c);
            } catch (IOException e) {
                System.err.println(e.toString());
            }
        }

        public String getResult() {
            return sw.toString();
        }
    }
}