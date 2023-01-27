package io.xpipe.app.storage;

public class DataNames {

    public static String cleanName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
