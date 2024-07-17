package io.xpipe.app.util;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import io.xpipe.app.issue.ErrorEvent;

import java.util.Map;

public class NativeBridge {

    private static MacOsLibrary macOsLibrary;

    public static MacOsLibrary getMacOsLibrary() {
        if (macOsLibrary == null) {
            try {
                var l = Native.load("xpipe_bridge", MacOsLibrary.class, Map.of());
                macOsLibrary = l;
            } catch (Throwable t) {
                ErrorEvent.fromThrowable(t).handle();
                macOsLibrary = new MacOsLibrary() {
                    @Override
                    public void setAppearance(NativeLong window, boolean seamlessFrame, boolean dark) {

                    }
                };
            }
        }
        return macOsLibrary;
    }

    public static interface MacOsLibrary extends Library {

        public abstract void setAppearance(NativeLong window, boolean seamlessFrame, boolean dark);
    }
}
