package io.xpipe.app.core.window;

import lombok.Getter;

@Getter
public enum DmwaWindowAttribute {
    DWMWA_USE_IMMERSIVE_DARK_MODE(20),
    DWMWA_BORDER_COLOR(34),
    DWMWA_SYSTEMBACKDROP_TYPE(38),
    DWMWA_WINDOW_CORNER_PREFERENCE(33);

    private final int value;

    DmwaWindowAttribute(int value) {
        this.value = value;
    }
}
