package io.xpipe.cli.test;

import io.xpipe.extension.test.ExtensionTest;
import lombok.Getter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestDataSources {

    @Getter
    public static enum Config {
        CSV(
                "username.csv",
                "csv",
                Map.of(
                        "delimiter", ";",
                        "header", "included"),
                "username_out.csv"),

        TEXT(
                "text.txt",
                "text",
                Map.of(
                        "charset", "utf8",
                        "newline", "lf"),
                "text_out.txt");

        Path inputFile;
        String provider;
        Map<String, String> config;
        Path outputReference;

        Config(String file, String provider, Map<String, String> config, String outputReference) {
            this.inputFile = ExtensionTest.getResourcePath(getClass(), file);
            this.provider = provider;
            this.config = config;
            this.outputReference = ExtensionTest.getResourcePath(getClass(), outputReference);
        }

        public void addDefault() {
            CliTestHelper.exec(addConfigArgs(List.of(
                    "source",
                    "add",
                    "--quiet",
                    "--type",
                    provider,
                    getInputFile().toString())));
        }

        List<String> addConfigArgs(List<String> args) {
            var list = new ArrayList<String>();
            for (var c : config.entrySet()) {
                list.add("-o");
                list.add(c.getKey() + "=" + c.getValue());
            }

            var copy = new ArrayList<>(args);
            copy.addAll(list);
            return copy;
        }
    }
}
