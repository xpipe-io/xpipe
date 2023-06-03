package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.util.CustomComboBoxBuilder;
import io.xpipe.core.charsetter.StreamCharset;
import javafx.beans.property.Property;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

public class CharsetChoiceComp extends SimpleComp {

    private final Property<StreamCharset> charset;

    public CharsetChoiceComp(Property<StreamCharset> charset) {
        this.charset = charset;
    }

    @Override
    protected Region createSimple() {
        var builder = new CustomComboBoxBuilder<>(
                charset,
                streamCharset -> {
                    return new Label(streamCharset.getCharset().displayName()
                            + (streamCharset.hasByteOrderMark() ? " (BOM)" : ""));
                },
                streamCharset -> streamCharset.getNames().get(0), new Label(AppI18n.get("app.none")),
                null);
        builder.addFilter((charset, filter) -> {
            return charset.getCharset().displayName().contains(filter);
        });
        builder.addHeader(AppI18n.get("app.common"));
        for (var e : StreamCharset.COMMON) {
            builder.add(e);
        }

        builder.addHeader(AppI18n.get("app.other"));
        for (var e : StreamCharset.RARE) {
            builder.add(e);
        }
        var comboBox = builder.build();
        comboBox.setVisibleRowCount(16);
        return comboBox;
    }
}
