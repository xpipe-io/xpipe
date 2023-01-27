package io.xpipe.ext.jdbc.postgres;

import io.xpipe.core.impl.LocalProcessControlProvider;
import io.xpipe.core.store.ShellStore;
import io.xpipe.ext.jdbc.address.JdbcBasicAddress;
import io.xpipe.ext.jdbc.auth.SimpleAuthMethod;
import io.xpipe.extension.DataStoreActionProvider;
import io.xpipe.extension.I18n;
import io.xpipe.extension.util.ApplicationHelper;
import io.xpipe.extension.util.HostHelper;
import javafx.beans.value.ObservableValue;

import java.util.ArrayList;
import java.util.List;

public class PostgresPsqlAction implements DataStoreActionProvider<PostgresSimpleStore> {
    @Override
    public Class<PostgresSimpleStore> getApplicableClass() {
        return PostgresSimpleStore.class;
    }

    @Override
    public boolean isMajor() {
        return true;
    }

    @Override
    public ObservableValue<String> getName(PostgresSimpleStore store) {
        return I18n.observable("psqlShell");
    }

    @Override
    public String getIcon(PostgresSimpleStore store) {
        return "mdi2c-code-greater-than";
    }

    @Override
    public void execute(PostgresSimpleStore store) throws Exception {
        var command = new ArrayList<>(List.of(
                "psql",
                "-d",
                store.getDatabase(),
                "-U",
                ((SimpleAuthMethod) store.getAuth()).getUsername(),
                "-w",
                "-p",
                String.valueOf(((JdbcBasicAddress) store.getAddress()).getPort())));

        var local = HostHelper.isLocalHost(((JdbcBasicAddress) store.getAddress()).getHostname());
        if (!local) {
            command.addAll(List.of(
                    "-h",
                    ((JdbcBasicAddress) store.getAddress()).getHostname()));
        }

        String openCommand;
        var host = store.getProxy() != null ? store.getProxy() : ShellStore.local();
        try (var pc = host.create().start()) {
            ApplicationHelper.checkSupport(pc, "psql", "PostgreSQL CLI Tools");
            var t = pc.getShellType();
            String passwordPrefix = "";
            if (store.getAuth() instanceof SimpleAuthMethod p && p.getPassword() != null) {
                var passwordCommand = t.getSetVariableCommand(
                        "PGPASSWORD",
                        p.getPassword().getSecretValue());
                passwordPrefix = passwordCommand + "\n";
            }

            var shellCommand = t.flatten(command);
            openCommand = pc.command(passwordPrefix + shellCommand).sensitive().prepareTerminalOpen();
        }
        LocalProcessControlProvider.get().openInTerminal("SQL Shell", openCommand);
    }
}
