package io.xpipe.app.pwman;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.prefs.PasswordManagerTestComp;
import io.xpipe.app.process.*;
import io.xpipe.app.terminal.TerminalLaunch;
import io.xpipe.core.OsType;
import javafx.beans.property.*;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Path;
import java.util.List;

@JsonTypeName("protonPass")
@Builder
@Jacksonized
@Getter
public class ProtonPasswordManager implements PasswordManager {

    @Override
    public PasswordManagerKeyConfiguration getKeyConfiguration() {
        return PasswordManagerKeyConfiguration.of(false, false, true, keyStrategy, getSocketLocation());
    }

    @Override
    public boolean selectInitial() throws Exception {
        return LocalShell.getShell().view().findProgram("pass-cli").isPresent();
    }

    private static ShellControl SHELL;

    private final PasswordManagerKeyStrategy keyStrategy;

    private static Path getSocketLocation() {
        var socket = switch (OsType.ofLocal()) {
            case OsType.Linux ignored -> AppSystemInfo.ofLinux().getUserHome().resolve(".1password", "agent.sock");
            case OsType.MacOs macOs -> AppSystemInfo.ofMacOs().getUserHome().resolve("Library", "Group Containers", "2BUA8C4S2C.com.1password", "t", "agent.sock");
            case OsType.Windows windows -> null;
        };
        return socket;
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<ProtonPasswordManager> p) {
        var keyStrategy = new SimpleObjectProperty<>(p.getValue().getKeyStrategy());

        var keyStrategyChoice = OptionsChoiceBuilder.builder()
                .allowNull(true)
                .available(List.of(PasswordManagerKeyStrategy.Agent.class))
                .property(keyStrategy)
                .customConfiguration(PasswordManagerKeyStrategy.OptionsConfig.builder()
                        .defaultSocketLocation(getSocketLocation())
                        .allowSocketChoice(false)
                        .build())
                .build();

        return new OptionsBuilder()
                .nameAndDescription("passwordManagerTest")
                .addComp(new PasswordManagerTestComp(true))
                .nameAndDescription("passwordManagerKeyStrategy")
                .sub(keyStrategyChoice.build(), keyStrategy)
                .bind(() -> {
                    return ProtonPasswordManager.builder().keyStrategy(keyStrategy.getValue()).build();
                }, p);
    }

    private static synchronized ShellControl getOrStartShell() throws Exception {
        if (SHELL == null) {
            SHELL = ProcessControlProvider.get().createLocalProcessControl(true);
        }
        SHELL.start();
        return SHELL;
    }

    @Override
    public synchronized Result query(String key) {
        try {
            CommandSupport.isInLocalPathOrThrow("ProtonPass CLI", "pass-cli");
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e)
                    .expected()
                    .link("https://proton.me/pass")
                    .handle();
            return null;
        }

        try {
            var sc = getOrStartShell();
            var loggedIn = sc.command(CommandBuilder.of().add("pass-cli", "info")).executeAndCheck();
            if (!loggedIn) {
                var script = ShellScript.lines(
                        "pass-cli login");
                TerminalLaunch.builder()
                        .title("Proton Pass login")
                        .localScript(script)
                        .logIfEnabled(false)
                        .preferTabs(false)
                        .pauseOnExit(true)
                        .launch();
                return null;
            }

            return null;
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return AppI18n.get("protonPassPasswordPlaceholder");
    }

    @Override
    public String getWebsite() {
        return "https://proton.me/pass";
    }
}
