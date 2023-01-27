package io.xpipe.app.comp.source;

import io.xpipe.app.comp.base.MultiStepComp;
import io.xpipe.app.comp.storage.source.SourceEntryWrapper;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.core.source.TableMapping;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.fxcomps.impl.LabelComp;
import io.xpipe.extension.fxcomps.impl.VerticalComp;
import io.xpipe.extension.fxcomps.util.PlatformThread;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@Value
@EqualsAndHashCode(callSuper = true)
public class GuiDsTableMappingConfirmation extends SimpleComp {

    ObservableValue<TableMapping> mapping;

    public GuiDsTableMappingConfirmation(ObservableValue<TableMapping> mapping) {
        this.mapping = PlatformThread.sync(mapping);
    }

    public static boolean showWindowAndWait(SourceEntryWrapper source, TableMapping mapping) {
        var latch = new CountDownLatch(1);
        var confirmed = new AtomicBoolean();
        AppWindowHelper.showAndWaitForWindow(() -> {
            var stage = AppWindowHelper.sideWindow(
                    I18n.get("confirmTableMappingTitle"),
                    window -> {
                        var ms = new GuiDsTableMappingConfirmation(new SimpleObjectProperty<>(mapping));
                        var multi = new MultiStepComp() {
                            @Override
                            protected List<Entry> setup() {
                                return List.of(new Entry(null, new Step<>(null) {
                                    @Override
                                    public CompStructure<?> createBase() {
                                        return ms.createStructure();
                                    }
                                }));
                            }

                            @Override
                            protected void finish() {
                                confirmed.set(true);
                                window.close();
                            }
                        };
                        return multi.apply(s -> {
                            s.get().setPrefWidth(400);
                            s.get().setPrefHeight(500);
                            AppFont.medium(s.get());
                        });
                    },
                    false,
                    null);
            stage.setOnHidden(event -> latch.countDown());
            return stage;
        });
        return confirmed.get();
    }

    @Override
    protected Region createSimple() {
        var header = new LabelComp(I18n.observable("confirmTableMapping"))
                .apply(struc -> struc.get().setWrapText(true));
        var content = Comp.derive(new DsTableMappingComp(mapping), region -> {
                    var box = new HBox(region);
                    box.setAlignment(Pos.CENTER);
                    box.getStyleClass().add("grid-container");
                    return box;
                })
                .apply(struc -> AppFont.normal(struc.get()));
        var changeNotice = new LabelComp(I18n.observable("changeTableMapping"))
                .apply(struc -> struc.get().setWrapText(true));
        var changeButton = Comp.of(() -> {
            var hl = new Hyperlink("Customizing Data Flows");
            hl.setOnAction(e -> {});
            hl.setMaxWidth(250);
            return hl;
        });
        return new VerticalComp(List.of(
                        header,
                        content,
                        Comp.of(() -> new Separator(Orientation.HORIZONTAL)),
                        changeNotice,
                        changeButton))
                .styleClass("table-mapping-confirmation-comp")
                .createRegion();
    }
}
