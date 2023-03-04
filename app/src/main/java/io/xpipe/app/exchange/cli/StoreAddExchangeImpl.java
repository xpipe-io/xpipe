package io.xpipe.app.exchange.cli;

import io.xpipe.app.exchange.DialogExchangeImpl;
import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.issue.ExceptionConverter;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.exchange.cli.StoreAddExchange;
import io.xpipe.core.dialog.Choice;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.dialog.QueryConverter;
import io.xpipe.core.store.DataStore;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.SneakyThrows;

import java.util.List;

public class StoreAddExchangeImpl extends StoreAddExchange
        implements MessageExchangeImpl<StoreAddExchange.Request, StoreAddExchange.Response> {

    @Override
    @SneakyThrows
    public StoreAddExchange.Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        Dialog creatorDialog = null;
        DataStoreProvider provider;
        if (msg.getStoreInput() != null) {
            creatorDialog = Dialog.empty().evaluateTo(msg::getStoreInput);
            provider = null;
        } else {
            if (msg.getType() == null) {
                throw new ClientException("Missing data store tight");
            }

            provider = DataStoreProviders.byName(msg.getType()).orElseThrow(() -> {
                return new ClientException("Unrecognized data store type: " + msg.getType());
            });

            creatorDialog = provider.dialogForStore(provider.defaultStore());
        }

        var name = new SimpleStringProperty(msg.getName());
        var completeDialog = createCompleteDialog(provider, creatorDialog, name);
        var config = DialogExchangeImpl.add(completeDialog, (DataStore store) -> {
            if (store == null) {
                return;
            }

            DataStorage.get().addStoreEntry(name.getValue(), store);
        });

        return StoreAddExchange.Response.builder().config(config).build();
    }

    private Dialog createCompleteDialog(DataStoreProvider provider, Dialog creator, StringProperty name) {
        var validator = Dialog.header(() -> {
                    DataStore store = creator.getResult();
                    if (store == null) {
                        return "Store is null";
                    }

                    try {
                        store.validate();
                    } catch (Exception ex) {
                        return ExceptionConverter.convertMessage(ex);
                    }

                    return null;
                })
                .map((String msg) -> {
                    return msg == null ? creator.getResult() : null;
                });

        var creatorAndValidator = Dialog.chain(creator, Dialog.busy(), validator);

        var nameQ = Dialog.retryIf(
                        Dialog.query("Store name", true, true, false, name.getValue(), QueryConverter.STRING),
                        (String r) -> {
                            return DataStorage.get().getStoreEntryIfPresent(r).isPresent()
                                    ? "Store with name " + r + " already exists"
                                    : null;
                        })
                .onCompletion((String n) -> name.setValue(n));

        var display = Dialog.header(() -> {
            if (provider == null) {
                return "Successfully created data store " + name.get();
            }

            DataStore s = creator.getResult();
            String d = "";
            try {
                d = provider.queryInformationString(s, 50);
            } catch (Exception ignored) {
            }
            if (d != null) {
                d = d.indent(2);
            }
            return "Successfully created data store " + name.get() + ":\n" + d;
        });

        if (provider == null) {
            return Dialog.chain(
                            creatorAndValidator, Dialog.skipIf(display, () -> creatorAndValidator.getResult() == null))
                    .evaluateTo(creatorAndValidator);
        }

        var aborted = new SimpleBooleanProperty();
        var addStore =
                Dialog.skipIf(Dialog.chain(nameQ, display), () -> aborted.get() || validator.getResult() == null);

        var prop = new SimpleObjectProperty<Dialog>();
        var fork = Dialog.skipIf(
                Dialog.fork(
                                "Choose how to continue",
                                List.of(
                                        new Choice('r', "Retry"),
                                        new Choice('i', "Ignore and continue"),
                                        new Choice('e', "Edit configuration"),
                                        new Choice('a', "Abort")),
                                true,
                                0,
                                (Integer choice) -> {
                                    if (choice == 0) {
                                        return Dialog.chain(Dialog.busy(), validator, prop.get());
                                    }
                                    if (choice == 1) {
                                        return null;
                                    }
                                    if (choice == 2) {
                                        return Dialog.chain(creatorAndValidator, prop.get());
                                    }
                                    if (choice == 3) {
                                        aborted.set(true);
                                        return null;
                                    }

                                    throw new AssertionError();
                                })
                        .evaluateTo(() -> null),
                () -> validator.getResult() != null);
        prop.set(fork);

        return Dialog.chain(creatorAndValidator, fork, addStore)
                .evaluateTo(() -> aborted.get() ? null : creator.getResult());
    }
}
