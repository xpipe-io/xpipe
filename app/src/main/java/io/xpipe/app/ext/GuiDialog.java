package io.xpipe.app.ext;

import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.DataStoreEntry;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.function.Consumer;

@Value
@AllArgsConstructor
public class GuiDialog {

    OptionsBuilder options;
    Consumer<DataStoreEntry> onFinish;

    public GuiDialog(OptionsBuilder options) {
        this.options = options;
        this.onFinish = null;
    }
}
