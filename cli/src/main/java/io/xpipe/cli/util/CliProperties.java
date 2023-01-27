package io.xpipe.cli.util;

import java.util.Properties;

public class CliProperties {

    private static Properties props;

    public static void init() {
        if (props == null) {
            props = new Properties();
            System.getProperties().forEach((o, o2) -> {
                if (o.toString().startsWith("io.xpipe")) {
                    props.put(o, o2);
                }
            });
        } else {
            props.forEach((o, o2) -> {
                System.getProperties().put(o, o2);
            });
        }
    }
}