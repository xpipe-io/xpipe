package io.xpipe.app.util;

public interface WindowDockListener {

    void onWindowMinimize();

    void onWindowShow();

    void onClose();

    void resizeView(int x, int y, int w, int h);
}
