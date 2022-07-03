package io.xpipe.extension;

import io.xpipe.fxcomps.Comp;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class GuiDialog {

    Comp<?> comp;
    Validator validator;

    public GuiDialog(Comp<?> comp) {
        this.comp = comp;
        this.validator = new SimpleValidator();
    }
}
