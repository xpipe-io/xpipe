package io.xpipe.core.util;

public class ModuleHelper {

    public static boolean isImage() {
        return ModuleHelper.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getProtocol()
                .equals("jrt");
    }
}
