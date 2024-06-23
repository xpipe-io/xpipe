package io.xpipe.app.core.window;

import javafx.stage.Window;

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import lombok.Getter;
import lombok.SneakyThrows;

import java.lang.reflect.Method;

@Getter
public class NativeWinWindowControl {

    private final WinDef.HWND windowHandle;

    @SneakyThrows
    public NativeWinWindowControl(Window stage) {
        Method tkStageGetter = Window.class.getDeclaredMethod("getPeer");
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

    public NativeWinWindowControl(WinDef.HWND windowHandle) {
        this.windowHandle = windowHandle;
    }

    public void move(int x, int y, int w, int h) {
        User32.INSTANCE.SetWindowPos(windowHandle, new WinDef.HWND(), x, y, w, h, 0);
    }

    public boolean setWindowAttribute(int attribute, boolean attributeValue) {
        var r = Dwm.INSTANCE.DwmSetWindowAttribute(
                windowHandle, attribute, new WinDef.BOOLByReference(new WinDef.BOOL(attributeValue)), WinDef.BOOL.SIZE);
        return r.longValue() == 0;
    }

    public boolean setWindowBackdrop(DwmSystemBackDropType backdrop) {
        var r = Dwm.INSTANCE.DwmSetWindowAttribute(
                windowHandle,
                DmwaWindowAttribute.DWMWA_SYSTEMBACKDROP_TYPE.get(),
                new WinDef.DWORDByReference(new WinDef.DWORD(backdrop.get())),
                WinDef.DWORD.SIZE);
        return r.longValue() == 0;
    }

    public interface Dwm extends Library {

        Dwm INSTANCE = com.sun.jna.Native.load("dwmapi", Dwm.class);

        WinNT.HRESULT DwmSetWindowAttribute(
                WinDef.HWND hwnd, int dwAttribute, PointerType pvAttribute, int cbAttribute);
    }
}
