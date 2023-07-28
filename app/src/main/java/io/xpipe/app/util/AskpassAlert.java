package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.fxcomps.impl.SecretFieldComp;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.SecretValue;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AskpassAlert {

    private static final Map<UUID, UUID> requestToId = new HashMap<>();
    private static final Map<UUID, SecretValue> passwords = new HashMap<>();

    public static SecretValue query(String prompt, DataStore store) {
        var rid = UUID.randomUUID();
        var secretId = UUID.nameUUIDFromBytes(ByteBuffer.allocate(4).putInt(store.hashCode()).array());
        return query(prompt, rid, secretId);
    }

    public static SecretValue query(String prompt, UUID requestId, UUID secretId) {
        if (requestToId.containsKey(requestId)) {
            var id = requestToId.remove(requestId);
            passwords.remove(id);
        }

        if (passwords.containsKey(secretId)) {
            return passwords.get(secretId);
        }

        var prop = new SimpleObjectProperty<SecretValue>();
        var r = AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setTitle(AppI18n.get("askpassAlertTitle"));
                    alert.setHeaderText(prompt);
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
            passwords.put(secretId, r);
            requestToId.put(requestId, secretId);
        }

        return r;
    }
}
