package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.fxcomps.impl.SecretFieldComp;
import io.xpipe.core.util.SecretValue;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;

import java.util.*;

public class AskpassAlert {

    private static final Set<UUID> cancelledRequests = new HashSet<>();
    private static final Set<UUID> requests = new HashSet<>();

    public static SecretValue query(String prompt, UUID requestId, UUID secretId) {
        if (cancelledRequests.contains(requestId)) {
            return null;
        }

        if (SecretCache.get(secretId).isPresent() && requests.contains(requestId)) {
            SecretCache.clear(secretId);
        }

        var found = SecretCache.get(secretId);
        if (found.isPresent()) {
            return found.get();
        }

        var prop = new SimpleObjectProperty<SecretValue>();
        var r = AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setTitle(AppI18n.get("askpassAlertTitle"));
                    alert.setHeaderText(prompt);
//                    alert.getDialogPane().setHeader(
//                            AppWindowHelper.alertContentText(prompt));
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);

                    var text = new SecretFieldComp(prop).createRegion();
                    alert.getDialogPane().setContent(new StackPane(text));
                })
                .filter(b -> b.getButtonData().isDefaultButton() && prop.getValue() != null)
                .map(t -> {
                    return prop.getValue() != null ? prop.getValue() : SecretHelper.encryptInPlace("");
                })
                .orElse(null);

        // If the result is null, assume that the operation was aborted by the user
        if (r != null) {
            requests.add(requestId);
            SecretCache.set(secretId, r);
        } else {
            cancelledRequests.add(requestId);
        }

        return r;
    }
}
