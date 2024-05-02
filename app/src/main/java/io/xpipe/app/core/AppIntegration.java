package io.xpipe.app.core;

import io.xpipe.app.Main;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.launcher.LauncherInput;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.OsType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.desktop.*;
import java.util.List;

public class AppIntegration {


    public static void setupDesktopIntegrations() {
        try {
            Desktop.getDesktop().addAppEventListener(new SystemSleepListener() {
                @Override
                public void systemAboutToSleep(SystemSleepEvent e) {
                    if (AppPrefs.get() != null && AppPrefs.get().lockVaultOnHibernation().get()) {
                        OperationMode.close();
                    }
                }

                @Override
                public void systemAwoke(SystemSleepEvent e) {

                }
            });

            // This will initialize the toolkit on macos and create the dock icon
            // macOS does not like applications that run fully in the background, so always do it
            if (OsType.getLocal().equals(OsType.MACOS)) {
                Desktop.getDesktop().setPreferencesHandler(e -> {
                    AppLayoutModel.get().selectSettings();
                });

                // URL open operations have to be handled in a special way on macOS!
                Desktop.getDesktop().setOpenURIHandler(e -> {
                    LauncherInput.handle(List.of(e.getURI().toString()));
                });

                // Do it this way to prevent IDE inspections from complaining
                var c = Class.forName(
                        ModuleLayer.boot().findModule("java.desktop").orElseThrow(), "com.apple.eawt.Application");
                var m = c.getDeclaredMethod("addAppEventListener", SystemEventListener.class);
                m.invoke(c.getMethod("getApplication").invoke(null), new AppReopenedListener() {
                    @Override
                    public void appReopened(AppReopenedEvent e) {
                        OperationMode.switchToAsync(OperationMode.GUI);
                    }
                });

                // Set dock icon explicitly on mac
                // This is necessary in case XPipe was started through a script as it will have no icon otherwise
                if (AppProperties.get().isDeveloperMode() && AppLogs.get().isWriteToSysout()) {
                    try {
                        var iconUrl = Main.class.getResourceAsStream("resources/img/logo/padded/logo_128x128.png");
                        if (iconUrl != null) {
                            var awtIcon = ImageIO.read(iconUrl);
                            Taskbar.getTaskbar().setIconImage(awtIcon);
                        }
                    } catch (Exception ex) {
                        ErrorEvent.fromThrowable(ex).omitted(true).build().handle();
                    }
                }
            }

            if (OsType.getLocal().equals(OsType.LINUX)) {
                try {
                    Toolkit xToolkit = Toolkit.getDefaultToolkit();
                    java.lang.reflect.Field awtAppClassNameField =
                            xToolkit.getClass().getDeclaredField("awtAppClassName");
                    awtAppClassNameField.setAccessible(true);
                    awtAppClassNameField.set(xToolkit, "XPipe");
                } catch (Exception e) {
                    ErrorEvent.fromThrowable(e).omit().handle();
                }
            }

        } catch (Throwable ex) {
            ErrorEvent.fromThrowable(ex).term().handle();
        }
    }

}
