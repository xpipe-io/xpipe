package io.xpipe.cli.util;

import picocli.CommandLine;

import java.util.List;
import java.util.Optional;

public class ConfigOverride {

    private static List<KeyValue> config = List.of();

    public static Optional<String> get(String key) {
        return config.stream()
                .filter(kv -> kv.key().equalsIgnoreCase(key))
                .findFirst()
                .map(KeyValue::value);
    }

    public static List<KeyValue> get() {
        return config;
    }

    public static boolean hasOverrides() {
        return config.size() > 0;
    }

    @CommandLine.Option(
            names = {"-o", "--option"},
            split = ",",
            description =
                    "Sets configuration options such that they do not need to be set or confirmed later on. "
                            + "Inputs should be in a key=value format where multiple inputs can be separated with a comma, "
                            + "e.g. --option key1=value1,key2=value2. In case the -q/--quiet switch is set, "
                            + "all configuration options for the output format must be supplied through this option. "
                            + "Otherwise, when some options are not given but required, the parameters are queried interactively.",
            paramLabel = "<key=value>",
            converter = KeyValue.Converter.class)
    public void setConfig(List<KeyValue> kvs) {
        config = kvs;
    }
}
