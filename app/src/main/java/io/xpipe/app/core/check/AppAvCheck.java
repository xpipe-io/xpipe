package io.xpipe.app.core.check;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.resources.AppResources;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.core.process.OsType;

import javafx.scene.layout.Region;

import lombok.Getter;

import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class AppAvCheck {

    private static Optional<AvType> detect() {
        for (AvType value : AvType.values()) {
            if (value.isActive()) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    public static void check() throws Throwable {
        // Only show this on first launch on windows
        if (OsType.getLocal() != OsType.WINDOWS || !AppProperties.get().isInitialLaunch()) {
            return;
        }

        var found = detect();
        if (found.isEmpty()) {
            return;
        }

        var modal = ModalOverlay.of(Comp.of(() -> {
            AtomicReference<Region> markdown = new AtomicReference<>();
            AppResources.with(AppResources.XPIPE_MODULE, "misc/antivirus.md", file -> {
                markdown.set(new MarkdownComp(Files.readString(file), s -> {
                            var t = found.get();
                            return s.formatted(
                                    t.getName(),
                                    t.getName(),
                                    t.getDescription(),
                                    AppProperties.get().getVersion(),
                                    AppProperties.get().getVersion(),
                                    t.getName());
                        }, false)
                        .prefWidth(550)
                        .prefHeight(600)
                        .createRegion());
            });
            return markdown.get();
        }));
        modal.addButton(ModalButton.quit());
        modal.addButton(ModalButton.ok());
        modal.showAndWait();
    }

    @Getter
    public enum AvType {
        BITDEFENDER("Bitdefender") {
            @Override
            public String getDescription() {
                return "Bitdefender sometimes isolates XPipe and some shell programs, effectively making it unusable.";
            }

            @Override
            public boolean isActive() {
                return WindowsRegistry.local()
                        .valueExists(WindowsRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\Bitdefender", "InstallDir");
            }
        },
        MALWAREBYTES("Malwarebytes") {
            @Override
            public String getDescription() {
                return "The free Malwarebytes version performs less invasive scans, so it shouldn't be a problem. If you are running the paid Malwarebytes Pro version, you will have access to the `Exploit Protection` under the `Real-time Protection` mode. When this setting is active, any shell access is slowed down, resulting in XPipe becoming very slow.";
            }

            @Override
            public boolean isActive() {
                return WindowsRegistry.local()
                        .valueExists(WindowsRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\Malwarebytes", "id");
            }
        },
        MCAFEE("McAfee") {
            @Override
            public String getDescription() {
                return "McAfee slows down XPipe considerably. It also sometimes preemptively disables some Win32 commands that XPipe depends on, leading to errors.";
            }

            @Override
            public boolean isActive() {
                return WindowsRegistry.local()
                        .valueExists(WindowsRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\McAfee", "mi");
            }
        };

        private final String name;

        AvType(String name) {
            this.name = name;
        }

        public abstract String getDescription();

        public abstract boolean isActive();
    }
}
