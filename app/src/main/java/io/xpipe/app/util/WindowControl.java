package io.xpipe.app.util;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import javafx.stage.Stage;

import java.lang.reflect.Method;

public class WindowControl {

    private final WinDef.HWND windowHandle;

    public WindowControl(Stage stage) throws Exception {
        Method tkStageGetter = stage.getClass().getSuperclass().getDeclaredMethod("getPeer");
        tkStageGetter.setAccessible(true);
        Object tkStage = tkStageGetter.invoke(stage);
        Method getPlatformWindow = tkStage.getClass().getDeclaredMethod("getPlatformWindow");
        getPlatformWindow.setAccessible(true);
        Object platformWindow = getPlatformWindow.invoke(tkStage);
        Method getNativeHandle = platformWindow.getClass().getMethod("getNativeHandle");
        getNativeHandle.setAccessible(true);
        Object nativeHandle = getNativeHandle.invoke(platformWindow);
        var hwnd = new WinDef.HWND(Pointer.createConstant((long) nativeHandle));
        this.windowHandle = hwnd;
    }

    public WindowControl(WinDef.HWND windowHandle) {
        this.windowHandle = windowHandle;
    }

    public void move(int x, int y, int w, int h) {
        User32.INSTANCE.SetWindowPos(windowHandle, new WinDef.HWND(), x,y,w,h, 0);
    }
}
