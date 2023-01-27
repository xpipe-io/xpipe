package io.xpipe.cli.util;

import org.ocpsoft.prettytime.PrettyTime;

public class PrettyTimeHelper {

    private static PrettyTime INSTANCE;

    public static PrettyTime get() {
        return INSTANCE;
    }

    public static void init() {
        INSTANCE = new PrettyTime();
    }
}
