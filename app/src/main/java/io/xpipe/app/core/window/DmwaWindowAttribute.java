package io.xpipe.app.core.window;

public enum DmwaWindowAttribute {
    DWMWA_USE_IMMERSIVE_DARK_MODE(20),
    DWMWA_SYSTEMBACKDROP_TYPE(38);

    private final int value;

    DmwaWindowAttribute(int value) {
        this.value = value;
    }

    public int get() {
        return value;
    }
}
