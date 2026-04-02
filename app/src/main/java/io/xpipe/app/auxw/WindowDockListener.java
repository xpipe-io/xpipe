package io.xpipe.app.auxw;

public interface WindowDockListener {

    void onWindowMinimize();

    void onWindowShow();

    void onClose();

    void resizeView(int x, int y, int w, int h);
}
