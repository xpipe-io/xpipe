package io.xpipe.app.comp.source;

import io.xpipe.app.comp.base.MultiStepComp;
import io.xpipe.app.comp.source.store.DsDbStoreChooserComp;
import io.xpipe.app.comp.source.store.DsStreamStoreChoiceComp;
import io.xpipe.app.util.Hyperlinks;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.store.DataStore;
import io.xpipe.extension.DataSourceProvider;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.event.TrackEvent;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.augment.GrowAugment;
import io.xpipe.extension.fxcomps.impl.StackComp;
import io.xpipe.extension.fxcomps.util.PlatformThread;
import io.xpipe.extension.util.BusyProperty;
import io.xpipe.extension.util.ThreadHelper;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class GuiDsStoreSelectStep extends MultiStepComp.Step<CompStructure<? extends Region>> {

    private final MultiStepComp parent;
    private final Property<DataSourceProvider<?>> provider;
    private final Property<DataStore> input;
    private final DataSourceProvider.Category category;
    private final ObjectProperty<? extends DataSource<?>> baseSource;
    private final BooleanProperty loading;

    public GuiDsStoreSelectStep(
            MultiStepComp parent,
            Property<DataSourceProvider<?>> provider,
            Property<DataStore> input,
            DataSourceProvider.Category category,
            ObjectProperty<? extends DataSource<?>> baseSource,
            BooleanProperty loading) {
        super(Hyperlinks.openLink(Hyperlinks.DOCS_DATA_INPUT));
        this.parent = parent;
        this.provider = provider;
        this.input = input;
        this.category = category;
        this.baseSource = baseSource;
        this.loading = loading;
    }

    private Region createLayout() {
        var layout = new BorderPane();

        var providerChoice = new DsProviderChoiceComp(category, provider, null);
        providerChoice.apply(GrowAugment.create(true, false));
        layout.setCenter(createCategoryChooserComp());

        var top = new VBox(providerChoice.createRegion(), new Separator());
        top.getStyleClass().add("top");
        layout.setTop(top);
        layout.getStyleClass().add("data-input-creation-step");
        return layout;
    }

    private Region createCategoryChooserComp() {
        if (category == DataSourceProvider.Category.STREAM) {
            return new DsStreamStoreChoiceComp(input, provider, true, true, DsStreamStoreChoiceComp.Mode.OPEN).createRegion();
        }

        if (category == DataSourceProvider.Category.DATABASE) {
            return new DsDbStoreChooserComp(input, provider).createRegion();
        }

        throw new AssertionError();
    }

    @Override
    public CompStructure<? extends Region> createBase() {
        //        var bgImg = AppImages.image("plus_bg.jpg");
        //        var background = new BackgroundImageComp(bgImg)
        //                .apply(struc -> struc.get().setOpacity(0.1));
        var layered = new StackComp(List.of(Comp.of(this::createLayout)));
        return layered.createStructure();
    }

    @Override
    public void onContinue() {}

    @Override
    public boolean canContinue() {
        if (input.getValue() == null || provider.getValue() == null) {
            return false;
        }

        if (baseSource.getValue() != null) {
            return true;
        }

        ThreadHelper.runAsync(() -> {
            try (var ignored = new BusyProperty(loading)) {
                var n = this.input.getValue();
                var ds = this.provider.getValue().createDefaultSource(n);
                if (ds == null) {
                    TrackEvent.warn("Default data source is null");
                    return;
                }

                PlatformThread.runLaterBlocking(() -> {
                    baseSource.setValue(ds.asNeeded());
                    parent.next();
                });
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).build().handle();
                PlatformThread.runLaterBlocking(() -> {
                    baseSource.setValue(null);
                });
            }
        });
        return false;
    }
}
