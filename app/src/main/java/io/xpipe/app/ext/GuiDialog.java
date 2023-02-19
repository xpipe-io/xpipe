package io.xpipe.app.ext;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.util.SimpleValidator;
import io.xpipe.app.util.Validator;
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
