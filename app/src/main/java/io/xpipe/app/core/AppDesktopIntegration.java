package io.xpipe.app.core;

import io.xpipe.app.Main;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorageUserHandler;
import io.xpipe.app.util.PlatformState;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.OsType;

import java.awt.*;
import java.awt.desktop.*;
import java.util.List;
import javax.imageio.ImageIO;

public class AppDesktopIntegration {

    public static void init() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().addAppEventListener(new SystemSleepListener() {
                    @Override
                    public void systemAboutToSleep(SystemSleepEvent e) {}

                    @Override
                    public void systemAwoke(SystemSleepEvent e) {
                        var handler = DataStorageUserHandler.getInstance();
                        if (AppPrefs.get() != null
                                && AppPrefs.get().lockVaultOnHibernation().get()
                                && handler != null
                                && handler.getActiveUser() != null) {
                            // If we run this at the same time as the system is sleeping, there might be exceptions
                            // because the platform does not like being shut down while sleeping
                            // This assures that it will be run later, on system wake
                            ThreadHelper.runAsync(() -> {
                                ThreadHelper.sleep(1000);
                                OperationMode.close();
                            });
                        }
                    }
                });
            }

            // This will initialize the toolkit on macOS and create the dock icon
            // macOS does not like applications that run fully in the background, so always do it
            if (OsType.getLocal().equals(OsType.MACOS) && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().setPreferencesHandler(e -> {
                    if (PlatformState.getCurrent() != PlatformState.RUNNING) {
                        return;
                    }

                    if (AppLayoutModel.get() != null) {
                        AppLayoutModel.get().selectSettings();
                    }
                });

                // URL open operations have to be handled in a special way on macOS!
                Desktop.getDesktop().setOpenURIHandler(e -> {
                    AppOpenArguments.handle(List.of(e.getURI().toString()));
                });

                Desktop.getDesktop().addAppEventListener(new AppReopenedListener() {
                    @Override
                    public void appReopened(AppReopenedEvent e) {
                        OperationMode.switchToAsync(OperationMode.GUI);
                    }
                });

                // Set dock icon explicitly on macOS
                // This is necessary in case XPipe was started through a script as it will have no icon otherwise
                if (AppProperties.get().isDeveloperMode()
                        && AppLogs.get().isWriteToSysout()
                        && Taskbar.isTaskbarSupported()) {
                    try {
                        var iconUrl = Main.class.getResourceAsStream("resources/img/logo/padded/logo_128x128.png");
                        if (iconUrl != null) {
                            var awtIcon = ImageIO.read(iconUrl);
                            Taskbar.getTaskbar().setIconImage(awtIcon);
                        }
                    } catch (Exception ex) {
                        ErrorEventFactory.fromThrowable(ex)
                                .omitted(true)
                                .build()
                                .handle();
                    }
                }
            }
        } catch (Throwable ex) {
            ErrorEventFactory.fromThrowable(ex).term().handle();
        }
    }
}
