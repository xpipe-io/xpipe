package io.xpipe.app.core;

import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.OsType;

import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.stage.Stage;

import java.lang.ref.WeakReference;

public class AppThemeHandler {

    private static final PseudoClass LIGHT = PseudoClass.getPseudoClass("light");
    private static final PseudoClass DARK = PseudoClass.getPseudoClass("dark");
    private static final PseudoClass PRETTY = PseudoClass.getPseudoClass("pretty");
    private static final PseudoClass PERFORMANCE = PseudoClass.getPseudoClass("performance");
    private static boolean init;

    public static void initThemeHandlers(Stage stage) {
        if (stage.getScene() == null) {
            return;
        }

        if (AppPrefs.get() == null) {
            var root = stage.getScene().getRoot();
            applyClasses(root, AppTheme.getDefaultLightTheme(), false);
            return;
        }

        stage.getScene().rootProperty().subscribe(parent -> {
            applyClasses(
                    parent,
                    AppPrefs.get().theme().getValue(),
                    AppPrefs.get().performanceMode().getValue());
        });

        // Allow for GC
        var ref = new WeakReference<>(stage);

        AppPrefs.get().theme().subscribe(t -> {
            var val = ref.get();
            if (val != null) {
                var scene = val.getScene();
                if (scene != null) {
                    var r = scene.getRoot();
                    applyClasses(r, t, AppPrefs.get().performanceMode().get());
                }
            }
        });

        AppPrefs.get().performanceMode().subscribe(pm -> {
            var val = ref.get();
            if (val != null) {
                var scene = val.getScene();
                if (scene != null) {
                    var r = scene.getRoot();
                    applyClasses(r, AppPrefs.get().theme().getValue(), pm);
                }
            }
        });
    }

    private static void applyClasses(Node r, AppTheme t, boolean perf) {
        if (r == null) {
            return;
        }

        r.pseudoClassStateChanged(PseudoClass.getPseudoClass(OsType.ofLocal().getId()), true);

        if (t != null) {
            AppTheme.ALL.forEach(theme -> {
                r.pseudoClassStateChanged(
                        PseudoClass.getPseudoClass(theme.getCssId()),
                        theme.getCssId().equals(t.getCssId()));
            });
        }

        if (t != null) {
            r.pseudoClassStateChanged(LIGHT, !t.isDark());
            r.pseudoClassStateChanged(DARK, t.isDark());
        }

        r.pseudoClassStateChanged(PRETTY, !perf);
        r.pseudoClassStateChanged(PERFORMANCE, perf);
    }

    public static void init() {
        if (init) {
            TrackEvent.trace("Theme init requested again");
            return;
        }

        if (AppPrefs.get() == null) {
            TrackEvent.trace("Theme init prior to prefs init, setting theme to default");
            AppTheme.getDefaultLightTheme().apply();
            return;
        }

        try {
            var lastSystemDark = AppCache.getBoolean("lastDarkTheme", false);
            var nowDark = isDarkMode();
            AppCache.update("lastDarkTheme", nowDark);
            if (AppPrefs.get().theme().getValue() == null || lastSystemDark != nowDark) {
                TrackEvent.trace("Updating theme to system theme");
                setDefault();
            }

            Platform.getPreferences().addListener((MapChangeListener<? super String, ? super Object>) change -> {
                TrackEvent.withTrace("Platform preference changed")
                        .tag("change", change.toString())
                        .handle();
            });

            Platform.getPreferences().addListener((MapChangeListener<? super String, ? super Object>) change -> {
                if (change.getKey().equals("GTK.theme_name")) {
                    Platform.runLater(() -> {
                        updateThemeToThemeName(change.getValueRemoved(), change.getValueAdded());
                    });
                }
            });

            Platform.getPreferences().colorSchemeProperty().addListener((observableValue, colorScheme, t1) -> {
                Platform.runLater(() -> {
                    updateThemeToColorScheme(t1);
                });
            });
        } catch (IllegalStateException ex) {
            // The platform preferences are sometimes not initialized yet
            ErrorEventFactory.fromThrowable(ex).expected().omit().handle();
        } catch (Throwable t) {
            ErrorEventFactory.fromThrowable(t).omit().handle();
        }

        var t = AppPrefs.get().theme().getValue();
        t.apply();
        TrackEvent.debug("Set theme " + t.getId() + " for scene");

        AppPrefs.get().theme().addListener((c, o, n) -> {
            changeTheme(n);
        });

        init = true;
    }

    private static void updateThemeToThemeName(Object oldName, Object newName) {
        if (OsType.ofLocal() == OsType.LINUX && newName != null) {
            var toDark = (oldName == null || !oldName.toString().contains("-dark"))
                    && newName.toString().contains("-dark");
            var toLight = (oldName == null || oldName.toString().contains("-dark"))
                    && !newName.toString().contains("-dark");
            if (toDark) {
                updateThemeToColorScheme(ColorScheme.DARK);
            } else if (toLight) {
                updateThemeToColorScheme(ColorScheme.LIGHT);
            }
        }
    }

    private static boolean isDarkMode() {
        var nowDark = Platform.getPreferences().getColorScheme() == ColorScheme.DARK;
        if (nowDark) {
            return true;
        }

        var gtkTheme = Platform.getPreferences().get("GTK.theme_name");
        return gtkTheme != null && gtkTheme.toString().contains("-dark");
    }

    private static void updateThemeToColorScheme(ColorScheme colorScheme) {
        if (colorScheme == null) {
            return;
        }

        if (colorScheme == ColorScheme.DARK
                && !AppPrefs.get().theme().getValue().isDark()) {
            AppPrefs.get().theme.setValue(AppTheme.getDefaultDarkTheme());
        }

        if (colorScheme != ColorScheme.DARK && AppPrefs.get().theme().getValue().isDark()) {
            AppPrefs.get().theme.setValue(AppTheme.getDefaultLightTheme());
        }
    }

    public static void reset() {
        if (!init) {
            return;
        }

        PlatformThread.runLaterIfNeededBlocking(() -> {
            var nowDark = isDarkMode();
            AppCache.update("lastDarkTheme", nowDark);
        });
    }

    private static void setDefault() {
        try {
            var colorScheme = Platform.getPreferences().getColorScheme();
            if (colorScheme == ColorScheme.DARK) {
                AppPrefs.get().theme.setValue(AppTheme.getDefaultDarkTheme());
            } else {
                AppPrefs.get().theme.setValue(AppTheme.getDefaultLightTheme());
            }
        } catch (IllegalStateException ex) {
            // The platform preferences are sometimes not initialized yet
            ErrorEventFactory.fromThrowable(ex).expected().omit().handle();
        } catch (Exception ex) {
            // The color scheme query can fail if the toolkit is not initialized properly
            AppPrefs.get().theme.setValue(AppTheme.getDefaultLightTheme());
        }
    }

    private static void changeTheme(AppTheme newTheme) {
        if (newTheme == null) {
            return;
        }

        PlatformThread.runLaterIfNeeded(() -> {
            var window = AppMainWindow.get();
            if (window == null) {
                return;
            }

            TrackEvent.debug("Setting theme " + newTheme.getId() + " for scene");

            // Don't animate anything for performance reasons
            newTheme.apply();
        });
    }

}
