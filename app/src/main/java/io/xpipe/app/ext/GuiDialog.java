package io.xpipe.app.ext;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.Validator;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class GuiDialog {

    OptionsBuilder options;
}
