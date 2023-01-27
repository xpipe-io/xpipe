package io.xpipe.ext.base.apps;

import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.WriteMode;
import io.xpipe.core.store.DataStore;
import io.xpipe.extension.*;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.fxcomps.augment.GrowAugment;
import io.xpipe.extension.fxcomps.impl.WriteModeChoiceComp;
import io.xpipe.extension.fxcomps.util.BindingsHelper;
import io.xpipe.extension.fxcomps.util.SimpleChangeListener;
import io.xpipe.extension.util.ChainedValidator;
import io.xpipe.extension.util.DynamicOptionsBuilder;
import io.xpipe.extension.util.XPipeDaemon;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class FileOutputTarget implements DataSourceTarget {

    @Override
    public String getId() {
        return "fileOutput";
    }

    @Override
    public InstructionsDisplay createRetrievalInstructions(DataSource<?> source, ObservableValue<DataSourceId> id) {
        var sourceProvider = DataSourceProviders.byDataSourceClass(source.getClass());

        var target = new SimpleObjectProperty<DataStore>();
        var provider = new SimpleObjectProperty<DataSourceProvider<?>>();
        var defaultProvider = Cache.getIfPresent("lastStreamOutputProvider", String.class)
                .flatMap(DataSourceProviders::byName)
                .orElse(null);
        provider.set(defaultProvider);

        ObservableValue<DataSource<?>> targetSource = Bindings.createObjectBinding(
                () -> {
                    try {
                        if (provider.get() != null && target.get() != null) {
                            return provider.get().createDefaultSource(target.get());
                        } else {
                            return null;
                        }
                    } catch (Exception e) {
                        ErrorEvent.fromThrowable(e).handle();
                        return null;
                    }
                },
                target,
                provider);
        ObservableList<WriteMode> availableModes = FXCollections.observableArrayList();
        SimpleChangeListener.apply(targetSource, val -> {
            availableModes.setAll(val != null ? val.getAvailableWriteModes() : List.of());
        });

        var layout = new BorderPane();
        var providerChoice = XPipeDaemon.getInstance()
                .sourceProviderChooser(provider, DataSourceProvider.Category.STREAM, sourceProvider.getPrimaryType());
        providerChoice.apply(GrowAugment.create(true, false));
        var providerChoiceRegion = providerChoice.createRegion();
        var top = new VBox(providerChoiceRegion, new Separator());
        top.getStyleClass().add("top");
        layout.setTop(top);
        layout.getStyleClass().add("data-input-creation-step");

        var chooser = XPipeDaemon.getInstance().streamStoreChooser(target, provider, true, true);
        var mode = new SimpleObjectProperty<WriteMode>();
        var modeComp = new WriteModeChoiceComp(mode, availableModes);
        target.addListener((c, o, n) -> {
            if (provider.get() == null) {
                provider.set(DataSourceProviders.byPreferredStore(
                                n,
                                DataSourceProviders.byDataSourceClass(source.getClass())
                                        .getPrimaryType())
                        .filter(provider1 -> provider1.getPrimaryType() == sourceProvider.getPrimaryType())
                        .orElse(null));
            }
        });

        var center = new DynamicOptionsBuilder(false)
                .addTitle("destination")
                .addComp(chooser, provider)
                .addTitle("additionalOptions")
                .addComp("writeMode", modeComp, mode)
                .buildComp();
        center.queryEntry("additionalOptions")
                .comp()
                .hide(BindingsHelper.persist(Bindings.greaterThanOrEqual(1, Bindings.size(availableModes))));
        center.queryEntry("writeMode")
                .comp()
                .hide(BindingsHelper.persist(Bindings.greaterThanOrEqual(1, Bindings.size(availableModes))));

        layout.setCenter(center.createRegion());
        var validator = new ChainedValidator(
                List.of(providerChoice.getValidator(), chooser.getValidator(), modeComp.getValidator()));
        return new InstructionsDisplay(
                layout,
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Cache.update(
                                    "lastStreamOutputProvider", provider.get().getId());
                            try (var connection = targetSource.getValue().openWriteConnection(mode.get())) {
                                connection.init();
                                try (var readConnection = source.openReadConnection()) {
                                    readConnection.init();
                                    readConnection.forward(connection);
                                }
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                },
                validator);
    }

    @Override
    public ObservableValue<String> getName() {
        return I18n.observable("base.fileOutput");
    }

    @Override
    public Category getCategory() {
        return Category.OTHER;
    }

    @Override
    public AccessType getAccessType() {
        return AccessType.ACTIVE;
    }

    @Override
    public String getSetupGuideURL() {
        return null;
    }

    @Override
    public String getGraphicIcon() {
        return "mdi2t-text";
    }
}
