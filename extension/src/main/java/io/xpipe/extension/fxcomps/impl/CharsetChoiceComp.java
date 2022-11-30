package io.xpipe.extension.fxcomps.impl;

import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.extension.I18n;
import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.util.CustomComboBoxBuilder;
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
                new Label(I18n.get("extension.none")),
                null);
        builder.addFilter((charset, filter) -> {
            return charset.getCharset().displayName().contains(filter);
        });
        builder.addHeader(I18n.get("extension.common"));
        for (var e : StreamCharset.COMMON) {
            builder.add(e);
        }

        builder.addHeader(I18n.get("extension.other"));
        for (var e : StreamCharset.RARE) {
            builder.add(e);
        }
        var comboBox =  builder.build();
        comboBox.setVisibleRowCount(16);
        return comboBox;
    }
}
