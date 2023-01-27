package io.xpipe.app.comp.source;

import io.xpipe.app.comp.base.MultiStepComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.source.*;
import io.xpipe.core.store.DataFlow;
import io.xpipe.core.store.DataStore;
import io.xpipe.extension.DataSourceProvider;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.impl.HorizontalComp;
import io.xpipe.extension.fxcomps.impl.IconButtonComp;
import io.xpipe.extension.fxcomps.impl.VerticalComp;
import io.xpipe.extension.util.BusyProperty;
import io.xpipe.extension.util.ThreadHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiDsConfigStep extends MultiStepComp.Step<CompStructure<?>> {

    private final Property<? extends DataStore> input;
    private final ObservableValue<? extends DataSource<?>> baseSource;
    private final Property<? extends DataSource<?>> currentSource;
    private final Property<DataSourceProvider<?>> provider;
    private final Property<DataSourceType> type;
    private final Map<DataSourceType, Comp<?>> previewComps;
    private final BooleanProperty loading;

    public GuiDsConfigStep(
            Property<DataSourceProvider<?>> provider,
            Property<? extends DataStore> input,
            Property<? extends DataSource<?>> baseSource,
            Property<? extends DataSource<?>> currentSource,
            Property<DataSourceType> type,
            BooleanProperty loading) {
        super(null);
        this.input = input;
        this.baseSource = baseSource;
        this.currentSource = currentSource;
        this.type = type;
        this.provider = provider;
        this.loading = loading;
        this.previewComps = new HashMap<>();
        Arrays.stream(DataSourceType.values()).forEach(t -> previewComps.put(t, createPreviewComp(t)));
    }

    @Override
    public CompStructure<?> createBase() {
        var hide = Bindings.createBooleanBinding(
                () -> {
                    return currentSource.getValue() != null
                            && currentSource.getValue().getFlow() == DataFlow.OUTPUT;
                },
                currentSource);
        var top = new HorizontalComp(List.of(createTypeSelectorComp(), Comp.of(Region::new), createRefreshComp()))
                .apply(s -> {
                    HBox.setHgrow(s.get().getChildren().get(1), Priority.ALWAYS);
                    s.get().setAlignment(Pos.CENTER);
                    s.get().setSpacing(7);
                })
                .hide(hide);
        var preview = Bindings.createObjectBinding(
                () -> {
                    return previewComps.get(type.getValue()).hide(hide);
                },
                type);

        var layout = new VerticalComp(List.of(top, preview.getValue(), Comp.of(this::createConfigOptions)));
        layout.apply(vbox -> {
                    vbox.get().setAlignment(Pos.CENTER);
                    VBox.setVgrow(vbox.get().getChildren().get(1), Priority.ALWAYS);
                    AppFont.small(vbox.get());

                    currentSource.addListener((c, o, n) -> {
                        vbox.get().getChildren().set(1, preview.getValue().createRegion());
                        VBox.setVgrow(vbox.get().getChildren().get(1), Priority.ALWAYS);
                    });

                    provider.addListener((c, o, n) -> {
                        vbox.get().getChildren().set(2, createConfigOptions());
                    });
                })
                .styleClass("data-source-config");

        provider.addListener((c, o, n) -> {});

        return layout.createStructure();
    }

    @SuppressWarnings("unchecked")
    private <T extends DataSource<?>> Region createConfigOptions() {
        if (currentSource.getValue() == null || provider.getValue() == null) {
            return new Region();
        }

        Region r = null;
        try {
            r = ((DataSourceProvider<T>) provider.getValue()).configGui((Property<T>) baseSource, false);
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
        }

        return r != null ? r : new Region();
    }

    private Comp<?> createReportComp() {
        return new IconButtonComp("mdi2a-alert-circle-outline", () -> {});
    }

    @SuppressWarnings("unchecked")
    private <T extends DataSource<?>> Comp<?> createRefreshComp() {
        return new IconButtonComp("mdmz-refresh", () -> {
                    T src = currentSource.getValue() != null
                            ? currentSource.getValue().asNeeded()
                            : null;
                    currentSource.setValue(null);
                    ((Property<T>) currentSource).setValue(src);
                })
                .shortcut(new KeyCodeCombination(KeyCode.F5));
    }

    private Comp<?> createTypeSelectorComp() {
        return new DsTypeChoiceComp(baseSource, provider, type);
    }

    private Comp<?> createPreviewComp(DataSourceType t) {
        return switch (t) {
            case TABLE -> createTablePreviewComp();
            case STRUCTURE -> createStructurePreviewComp();
            case TEXT -> createTextPreviewComp();
            case RAW -> createRawPreviewComp();
            case COLLECTION -> createCollectionPreviewComp();
        };
    }

    @SuppressWarnings("unchecked")
    private <DI extends DataStore, DS extends TextDataSource<DI>> Comp<?> createTextPreviewComp() {
        var text = Bindings.createObjectBinding(
                () -> {
                    if (currentSource.getValue() == null || type.getValue() != DataSourceType.TEXT) {
                        return List.of("");
                    }

                    try (var con = ((DS) currentSource.getValue()).openReadConnection()) {
                        con.init();
                        return con.lines().limit(10).toList();
                    } catch (Exception e) {
                        ErrorEvent.fromThrowable(e).build().handle();
                        return List.of("");
                    }
                },
                currentSource);
        return new DsTextComp(text);
    }

    @SuppressWarnings("unchecked")
    private <DI extends DataStore, DS extends StructureDataSource<DI>> Comp<?> createStructurePreviewComp() {
        var structure = Bindings.createObjectBinding(
                () -> {
                    if (currentSource.getValue() == null || type.getValue() != DataSourceType.STRUCTURE) {
                        return TupleNode.builder().build();
                    }

                    try (var con = ((DS) currentSource.getValue()).openReadConnection()) {
                        con.init();
                        return con.read();
                    } catch (Exception e) {
                        ErrorEvent.fromThrowable(e).build().handle();
                        return TupleNode.builder().build();
                    }
                },
                currentSource);
        return new DsStructureComp(structure);
    }

    @Override
    public void onInit() {
        //        DataSource<?> src = currentSource.getValue() != null ? currentSource.getValue().asNeeded() : null;
        //        currentSource.setValue(null);
        //        currentSource.setValue(src != null ? src.asNeeded() : null);
    }

    private Comp<?> createTablePreviewComp() {
        var table = new SimpleObjectProperty<>(ArrayNode.of());
        currentSource.addListener((c, o, val) -> {
            ThreadHelper.runAsync(() -> {
                if (val == null
                        || type.getValue() != DataSourceType.TABLE
                        || !val.getFlow().hasInput()) {
                    return;
                }

                try (var ignored = new BusyProperty(loading);
                        var con = (TableReadConnection) val.openReadConnection()) {
                    con.init();
                    table.set(con.readRows(50));
                } catch (Exception e) {
                    ErrorEvent.fromThrowable(e).build().handle();
                    table.set(ArrayNode.of());
                }
            });
        });
        return new DsTableComp(table);
    }

    @SuppressWarnings("unchecked")
    private <DI extends DataStore, DS extends RawDataSource<DI>> Comp<?> createRawPreviewComp() {
        var bytes = Bindings.createObjectBinding(
                () -> {
                    if (currentSource.getValue() == null || type.getValue() != DataSourceType.RAW) {
                        return new byte[0];
                    }

                    try (var con = ((DS) currentSource.getValue()).openReadConnection()) {
                        con.init();
                        return con.readBytes(1000);
                    } catch (Exception e) {
                        ErrorEvent.fromThrowable(e).build().handle();
                        return new byte[0];
                    }
                },
                currentSource);
        return new DsRawComp(bytes);
    }

    @SuppressWarnings("unchecked")
    private <DI extends DataStore, DS extends CollectionDataSource<DI>> Comp<?> createCollectionPreviewComp() {
        var con = Bindings.createObjectBinding(
                () -> {
                    if (currentSource.getValue() == null || type.getValue() != DataSourceType.COLLECTION) {
                        return null;
                    }

                    /*
                    TODO: Fix
                     */
                    try {
                        return ((DS) currentSource.getValue()).openReadConnection();
                    } catch (Exception e) {
                        ErrorEvent.fromThrowable(e).build().handle();
                        return null;
                    }
                },
                currentSource,
                input);

        con.addListener((c, o, n) -> {
            if (o == null) {
                return;
            }

            try {
                o.close();
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).handle();
            }
        });

        return new DsCollectionComp(con);
    }
}
