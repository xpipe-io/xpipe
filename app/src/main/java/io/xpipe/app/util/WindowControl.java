package io.xpipe.app.util;

import javafx.stage.Window;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import lombok.Getter;

import java.lang.reflect.Method;

@Getter
public class WindowControl {

    private final WinDef.HWND windowHandle;

    public WindowControl(Window stage) throws Exception {
        Method tkStageGetter = stage.getClass().getSuperclass().getDeclaredMethod("getPeer");
        tkStageGetter.setAccessible(true);
        Object tkStage = tkStageGetter.invoke(stage);
        Method getPlatformWindow = tkStage.getClass().getDeclaredMethod("getPlatformWindow");
        getPlatformWindow.setAccessible(true);
        Object platformWindow = getPlatformWindow.invoke(tkStage);
        Method getNativeHandle = platformWindow.getClass().getMethod("getNativeHandle");
        getNativeHandle.setAccessible(true);
        Object nativeHandle = getNativeHandle.invoke(platformWindow);
        var hwnd = new WinDef.HWND(new Pointer((long) nativeHandle));
        this.windowHandle = hwnd;
    }

    public WindowControl(WinDef.HWND windowHandle) {
        this.windowHandle = windowHandle;
    }

    public void move(int x, int y, int w, int h) {
        User32.INSTANCE.SetWindowPos(windowHandle, new WinDef.HWND(), x, y, w, h, 0);
    }

    public void setWindowAttribute(int attribute, boolean attributeValue) {
        DwmSupport.INSTANCE.DwmSetWindowAttribute(
                windowHandle, attribute, new WinDef.BOOLByReference(new WinDef.BOOL(attributeValue)), WinDef.BOOL.SIZE);
    }

    public void setWindowAttribute(int attribute, long attributeValue) {
        DwmSupport.INSTANCE.DwmSetWindowAttribute(
                windowHandle,
                attribute,
                new WinDef.DWORDByReference(new WinDef.DWORD(attributeValue)),
                WinDef.DWORD.SIZE);
    }

    public void redraw() {
        User32.INSTANCE.RedrawWindow(
                windowHandle, null, null, new WinDef.DWORD(User32.RDW_FRAME | User32.RDW_VALIDATE));
    }

    public interface DwmSupport extends Library {

        DwmSupport INSTANCE = Native.load("dwmapi", DwmSupport.class);

        WinNT.HRESULT DwmSetWindowAttribute(
                WinDef.HWND hwnd, int dwAttribute, PointerType pvAttribute, int cbAttribute);
    }
}
