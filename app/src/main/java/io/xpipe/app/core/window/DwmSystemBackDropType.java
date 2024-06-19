package io.xpipe.app.core.window;

public enum DwmSystemBackDropType {
    NONE(1),
    MICA(2),
    MICA_ALT(4),
    ACRYLIC(3);

    private final int value;

    DwmSystemBackDropType(int value) {
        this.value = value;
    }

    public int get() {
        return value;
    }
}
