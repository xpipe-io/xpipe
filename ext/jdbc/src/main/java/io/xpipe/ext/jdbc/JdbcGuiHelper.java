package io.xpipe.ext.jdbc;

import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.extension.ExtensionException;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.impl.ShellStoreChoiceComp;
import io.xpipe.extension.util.DynamicOptionsBuilder;
import io.xpipe.extension.util.Validator;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

public class JdbcGuiHelper {
    public static Comp<?> url(
            String protocol, Class<? extends JdbcUrlStore> c, Property<DataStore> store, Validator val) {
        JdbcUrlStore st = (JdbcUrlStore) store.getValue();

        var proxyProperty = new SimpleObjectProperty<>(st != null ? st.getProxy() : ShellStore.local());
        var proxyGui = new DynamicOptionsBuilder(false)
                .addComp("proxy", ShellStoreChoiceComp.proxy(proxyProperty), proxyProperty)
                .buildComp();

        var url = new SimpleStringProperty(st != null ? st.getUrl() : null);
        return new DynamicOptionsBuilder(false)
                .addString(I18n.observable("jdbc.url"), url)
                .nonNull(val)
                .addComp(proxyGui)
                .bind(
                        () -> {
                            var cleaned = url.get();
                            if (cleaned != null && cleaned.startsWith("jdbc:")) {
                                cleaned = cleaned.substring(5);
                            }
                            if (cleaned != null && cleaned.startsWith(protocol + "://")) {
                                cleaned = cleaned.substring(protocol.length() + 3);
                            }
                            try {
                                return (JdbcUrlStore) c.getDeclaredConstructor(ShellStore.class, String.class)
                                        .newInstance(proxyProperty.get(), cleaned);
                            } catch (Exception e) {
                                throw new ExtensionException(e);
                            }
                        },
                        store)
                .buildComp();
    }
}
