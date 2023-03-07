package io.xpipe.app.exchange;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.fxcomps.impl.SecretFieldComp;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.AskpassExchange;
import io.xpipe.core.util.SecretValue;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;

public class AskpassExchangeImpl extends AskpassExchange
        implements MessageExchangeImpl<AskpassExchange.Request, AskpassExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        OperationMode.switchTo(OperationMode.GUI);

//        SecretValue set = AppCache.get(msg.getId(), SecretValue.class, () -> null);
//        if (set != null) {
//            return Response.builder().value(set).build();
//        }

        var prop =
                new SimpleObjectProperty<SecretValue>();
        var r = AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setTitle(AppI18n.get("askpassAlertTitle"));
                    alert.setHeaderText(msg.getPrompt());
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);

                    var text = new SecretFieldComp(prop).createRegion();
                    text.setStyle("-fx-border-width: 1px");
                    alert.getDialogPane().setContent(text);
                })
                .filter(b -> b.getButtonData().isDefaultButton() && prop.getValue() != null)
                .map(t -> {
                    //AppCache.update(msg.getId(), prop.getValue());
                    return prop.getValue();
                })
                .orElse(null);
        return Response.builder().value(r != null ? r.getSecretValue() : null).build();
    }
}
