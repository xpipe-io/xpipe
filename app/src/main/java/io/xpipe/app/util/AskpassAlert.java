package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.fxcomps.impl.SecretFieldComp;
import io.xpipe.core.util.SecretValue;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AskpassAlert {

    private static final Map<UUID, UUID> requestToId = new HashMap<>();

    public static SecretValue query(String prompt, UUID requestId, UUID secretId) {
        if (requestToId.containsKey(requestId)) {
            var id = requestToId.remove(requestId);
            SecretCache.clear(id);
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
                    // AppCache.update(msg.getId(), prop.getValue());
                    return prop.getValue();
                })
                .orElse(null);

        // If the result is null, assume that the operation was aborted by the user
        if (r != null) {
            requestToId.put(requestId, secretId);
            SecretCache.set(secretId, r);
        }

        return r;
    }
}
