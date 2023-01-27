package io.xpipe.ext.base.apps;

import io.xpipe.app.comp.source.GuiDsTableMappingConfirmation;
import io.xpipe.core.source.*;
import io.xpipe.extension.DataSourceProvider;
import io.xpipe.extension.DataSourceProviders;
import io.xpipe.extension.I18n;
import io.xpipe.extension.DataSourceTarget;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.fxcomps.impl.WriteModeChoiceComp;
import io.xpipe.extension.fxcomps.util.BindingsHelper;
import io.xpipe.extension.util.ChainedValidator;
import io.xpipe.extension.util.DynamicOptionsBuilder;
import io.xpipe.extension.util.XPipeDaemon;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

public class DataSourceOutputTarget implements DataSourceTarget {

    @Override
    public String getId() {
        return "dataSourceOutput";
    }

    @Override
    public InstructionsDisplay createRetrievalInstructions(DataSource<?> source, ObservableValue<DataSourceId> id) {
        var target = new SimpleObjectProperty<DataSource<?>>();
        var sourceType =
                DataSourceProviders.byDataSourceClass(source.getClass()).getPrimaryType();
        var chooser = XPipeDaemon.getInstance()
                .namedSourceChooser(
                        new SimpleObjectProperty<>(s -> s != source
                                && s.getFlow().hasOutput()
                                && DataSourceProviders.byDataSourceClass(s.getClass())
                                                .getPrimaryType()
                                        == sourceType),
                        target,
                        DataSourceProvider.Category.STREAM);
        chooser.apply(struc -> VBox.setVgrow(struc.get(), Priority.ALWAYS));

        var mode = new SimpleObjectProperty<WriteMode>();

        ObservableList<WriteMode> availableModes = FXCollections.observableArrayList();
        var modeComp = new WriteModeChoiceComp(mode, availableModes);
        target.addListener((observable, oldValue, newValue) -> {
            availableModes.setAll(newValue.getAvailableWriteModes());
        });

        var center = new DynamicOptionsBuilder(false)
                .addTitle("destination")
                .addComp(chooser, target)
                .addTitle("additionalOptions")
                .addComp("writeMode", modeComp, mode)
                .buildComp();
        center.queryEntry("additionalOptions")
                .comp()
                .visible(BindingsHelper.persist(Bindings.lessThan(1, Bindings.size(availableModes))));
        center.queryEntry("writeMode")
                .comp()
                .visible(BindingsHelper.persist(Bindings.lessThan(1, Bindings.size(availableModes))));

        var validator = new ChainedValidator(List.of(chooser.getValidator(), modeComp.getValidator()));
        return new InstructionsDisplay(
                center.createRegion(),
                () -> {
                    try (var readConnection = source.openReadConnection()) {
                        readConnection.init();
                        if (readConnection instanceof TableReadConnection r) {
                            var type = r.getDataType();
                            var mapping = ((TableDataSource<?>) source).createMapping(type);
                            if (!(mapping.isEmpty() /* || mapping.isIdentity() */)) {
                                if (!GuiDsTableMappingConfirmation.showWindowAndWait(null, mapping.get())) {
                                    return;
                                }
                            }
                        }

                        try (var connection = target.get().openWriteConnection(mode.get())) {
                            connection.init();
                            readConnection.forward(connection);
                        }

                    } catch (Exception e) {
                        ErrorEvent.fromThrowable(e).handle();
                    }
                },
                validator);
    }

    @Override
    public ObservableValue<String> getName() {
        return I18n.observable("base.dataSourceOutput");
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
        return "mdi2t-trello";
    }
}
