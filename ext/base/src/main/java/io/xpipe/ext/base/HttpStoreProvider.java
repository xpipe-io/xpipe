package io.xpipe.ext.base;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.fxcomps.impl.DataStoreFlowChoiceComp;
import io.xpipe.app.util.DataStoreFormatter;
import io.xpipe.app.util.DialogHelper;
import io.xpipe.app.util.DynamicOptionsBuilder;
import io.xpipe.app.util.SimpleValidator;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.dialog.QueryConverter;
import io.xpipe.core.store.DataFlow;
import io.xpipe.core.store.DataStore;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpStoreProvider implements DataStoreProvider {

    @Override
    public boolean isShareable() {
        return true;
    }

    @Override
    public String getId() {
        return "http";
    }

    @Override
    public GuiDialog guiDialog(Property<DataStore> store) {
        var val = new SimpleValidator();
        HttpStore st = (HttpStore) store.getValue();

        Property<String> methodProp = new SimpleObjectProperty<>(st != null ? st.getMethod() : null);
        Property<String> requestProp = new SimpleObjectProperty<>(
                st != null && st.getUriString() != null ? st.getUriString().toString() : null);

        Property<DataFlow> flowProperty = new SimpleObjectProperty<>(st.getFlow());

        var q = new DynamicOptionsBuilder(AppI18n.observable("configuration"))
                .addString(AppI18n.observable("base.method"), methodProp)
                .nonNull(val)
                .addString(AppI18n.observable("base.uri"), requestProp)
                .nonNull(val)
                .addComp(
                        "base.usage",
                        new DataStoreFlowChoiceComp(
                                flowProperty, new DataFlow[] {DataFlow.INPUT, DataFlow.OUTPUT, DataFlow.TRANSFORMER}),
                        flowProperty)
                .bind(
                        () -> {
                            return new HttpStore(
                                    methodProp.getValue(), requestProp.getValue(), Map.of(), flowProperty.getValue());
                        },
                        store)
                .buildComp();
        return new GuiDialog(q, val);
    }

    @Override
    public String queryInformationString(DataStore store, int length) throws Exception {
        return getDisplayName();
    }

    @Override
    public String toSummaryString(DataStore store, int length) {
        HttpStore s = store.asNeeded();
        return s.getMethod() + " " + DataStoreFormatter.cut(s.getUriString(), length - 4);
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(HttpStore.class);
    }

    @Override
    public DataStore defaultStore() {
        return new HttpStore("GET", null, Map.of(), DataFlow.INPUT);
    }

    @Override
    public Dialog dialogForStore(DataStore store) {
        HttpStore s = store.asNeeded();
        var methodQ = Dialog.query("Method", false, true, false, s.getMethod(), QueryConverter.STRING);
        var urlQ = Dialog.query("URL", false, true, false, s.getUriString(), QueryConverter.STRING);

        var headers = new ArrayList<Map.Entry<String, String>>();
        var headerQ = Dialog.query(
                "Additional HTTP headers (optional)", true, false, false, null, QueryConverter.HTTP_HEADER);
        var headerD = Dialog.repeatIf(
                headerQ.evaluateTo(() -> {
                    if (headerQ.getResult() == null) {
                        return null;
                    }

                    headers.add(headerQ.getResult());
                    return headerQ.getResult();
                }),
                (String r) -> {
                    return r != null;
                });

        var flowQuery = DialogHelper.dataStoreFlowQuery(
                s.getFlow(), new DataFlow[] {DataFlow.INPUT, DataFlow.OUTPUT, DataFlow.TRANSFORMER});

        return Dialog.chain(methodQ, urlQ, headerD, flowQuery).evaluateTo(() -> {
            var map = new HashMap<String, String>();
            headers.forEach(h -> {
                map.put(h.getKey(), h.getValue());
            });
            return new HttpStore(methodQ.getResult(), urlQ.getResult(), map, flowQuery.getResult());
        });
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("http", "https", "http_request", "http_response");
    }
}
