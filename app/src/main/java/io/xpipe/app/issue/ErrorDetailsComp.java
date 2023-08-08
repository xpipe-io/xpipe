package io.xpipe.app.issue;

import io.xpipe.app.comp.base.ListViewComp;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.TabPaneComp;
import io.xpipe.core.util.Deobfuscator;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import lombok.AllArgsConstructor;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

@AllArgsConstructor
public class ErrorDetailsComp extends SimpleComp {

    private ErrorEvent event;

    private Region createStrackTraceContent() {
        if (event.getThrowable() != null) {
            String stackTrace = Deobfuscator.deobfuscateToString(event.getThrowable());
            stackTrace = stackTrace.replace("\t", "");
            var tf = new TextArea(stackTrace);
            AppFont.verySmall(tf);
            tf.setWrapText(false);
            tf.setEditable(false);
            tf.setPadding(new Insets(10));
            return tf;
        }

        return null;
    }

    private Comp<?> createTrackEventHistory() {
        var list = FXCollections.observableList(event.getTrackEvents());
        return new ListViewComp<>(list, list, null, te -> {
            var label = new Label(te.getMessage());
            var i = DateTimeFormatter.ofPattern("HH:mm:ss:SSS")
                    .withZone(ZoneId.systemDefault())
                    .format(te.getInstant());
            var date = new Label(i);
            var spacer = new Region();
            var c = new HBox(label, spacer, date);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            return Comp.of(() -> c);
        });
    }

    @Override
    protected Region createSimple() {
        var items = new ArrayList<TabPaneComp.Entry>();
        if (event.getThrowable() != null) {
            items.add(new TabPaneComp.Entry(
                    AppI18n.observable("stackTrace"), "mdoal-code", Comp.of(this::createStrackTraceContent)));
        }

        //        if (event.getTrackEvents().size() > 0) {
        //            items.add(new TabPaneComp.Entry(
        //                    AppI18n.observable("events"), "mdi2c-clipboard-list-outline", createTrackEventHistory()));
        //        }

        var tb = new TabPaneComp(new SimpleObjectProperty<>(items.size() > 0 ? items.get(0) : null), items);
        tb.apply(r -> AppFont.small(r.get()));
        return tb.createRegion();
    }
}
