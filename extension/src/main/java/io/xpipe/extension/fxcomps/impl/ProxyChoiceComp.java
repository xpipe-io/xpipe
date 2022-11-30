package io.xpipe.extension.fxcomps.impl;

import io.xpipe.core.store.ShellStore;
import io.xpipe.extension.XPipeProxy;
import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.util.SimpleValidator;
import io.xpipe.extension.util.Validatable;
import io.xpipe.extension.util.Validator;
import javafx.beans.property.Property;
import javafx.scene.layout.Region;
import net.synedra.validatorfx.Check;

public class ProxyChoiceComp extends SimpleComp implements Validatable {

    private final Property<ShellStore> selected;
    private final Validator validator = new SimpleValidator();
    private final Check check;

    public ProxyChoiceComp(Property<ShellStore> selected) {
        this.selected = selected;
        check = Validator.exceptionWrapper(validator, selected, () -> XPipeProxy.checkSupport(selected.getValue()));
    }

    @Override
    protected Region createSimple() {
        var choice = new ShellStoreChoiceComp<>(selected, ShellStore.class, shellStore -> true, shellStore -> true);
        choice.apply(struc -> check.decorates(struc.get()));
        return choice.createRegion();
    }

    @Override
    public Validator getValidator() {
        return validator;
    }
}
