package io.xpipe.app.ext;

import io.xpipe.app.platform.OptionsBuilder;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class GuiDialog {

    OptionsBuilder options;
}
