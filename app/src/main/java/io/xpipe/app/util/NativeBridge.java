package io.xpipe.app.util;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import io.xpipe.app.issue.ErrorEvent;

import java.util.Map;

public class NativeBridge {

    public static interface MacOsLibrary extends Library {

        public static MacOsLibrary INSTANCE = Native.load("xpipe_bridge", MacOsLibrary.class, Map.of());

        public abstract void setAppearance(NativeLong window, boolean dark);
    }
}
