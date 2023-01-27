package io.xpipe.ext.jdbc;

import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.dialog.QueryConverter;
import io.xpipe.ext.jdbc.address.JdbcBasicAddress;
import io.xpipe.ext.jdbc.auth.SimpleAuthMethod;
import io.xpipe.extension.util.DialogHelper;

public class JdbcDialogHelper {
    public static Dialog location(JdbcBasicAddress address) {
        var otherAddress = new DialogHelper.Address(
                address != null ? address.getHostname() : null, address != null ? address.getPort() : null);
        return DialogHelper.addressQuery(otherAddress)
                .map((DialogHelper.Address newAddress) -> JdbcBasicAddress.builder()
                        .hostname(newAddress.getHostname())
                        .port(newAddress.getPort())
                        .build());
    }

    public static Dialog simpleAuth(SimpleAuthMethod method) {
        return Dialog.lazy(() -> {
            var defU = method.getUsername();
            var usernameQ = Dialog.query("Username", false, false, false, defU, QueryConverter.STRING);
            var passwordQ = Dialog.querySecret("Password", false, true, method.getPassword());
            return Dialog.chain(Dialog.header("Provide Login Information:"), usernameQ, passwordQ)
                    .evaluateTo(() -> {
                        return new SimpleAuthMethod(usernameQ.getResult(), passwordQ.getResult());
                    });
        });
    }
}
