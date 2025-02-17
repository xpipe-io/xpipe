package io.xpipe.app.update;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.util.Hyperlinks;

import java.util.List;

public class WebtopUpdater extends PortableUpdater {

    public WebtopUpdater() {
        super(true);
    }

    @Override
    public List<ModalButton> createActions() {
        var l = super.createActions();
        l.add(new ModalButton(
                "upgradeInstructions",
                () -> {
                    if (getPreparedUpdate().getValue() == null) {
                        return;
                    }

                    Hyperlinks.open(Hyperlinks.DOCS_WEBTOP_UPDATE);
                },
                false,
                false));
        return l;
    }
}
