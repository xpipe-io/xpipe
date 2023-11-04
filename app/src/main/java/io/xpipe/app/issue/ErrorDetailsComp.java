package io.xpipe.app.issue;

import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.impl.TabPaneComp;
import io.xpipe.core.util.Deobfuscator;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import lombok.AllArgsConstructor;

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

    @Override
    protected Region createSimple() {
        var items = new ArrayList<TabPaneComp.Entry>();
        if (event.getThrowable() != null) {
            items.add(new TabPaneComp.Entry(AppI18n.observable("stackTrace"), "mdoal-code", Comp.of(this::createStrackTraceContent)));
        }

        var tb = new TabPaneComp(new SimpleObjectProperty<>(items.size() > 0 ? items.get(0) : null), items);
        tb.apply(r -> AppFont.small(r.get()));
        return tb.createRegion();
    }
}
