package io.xpipe.app.resources;

import io.xpipe.app.ext.ContainerImageStore;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.DataStore;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.function.Predicate;

@Value
@EqualsAndHashCode(callSuper = true)
public class ContainerAutoSystemIcon extends SystemIcon {

    Predicate<String> imageCheck;

    public ContainerAutoSystemIcon(String iconName, String displayName, Predicate<String> imageCheck) {
        super(iconName, displayName);
        this.imageCheck = imageCheck;
    }

    @Override
    public boolean isApplicable(ShellControl sc) {
        var source = sc.getSourceStore();
        if (source.isEmpty()) {
            return false;
        }

        return isApplicable(source.get());
    }

    @Override
    public boolean isApplicable(DataStore store) {
        if (!(store instanceof ContainerImageStore containerImageStore)) {
            return false;
        }

        if (containerImageStore.getImageName() == null) {
            return false;
        }

        return imageCheck.test(containerImageStore.getImageName());
    }
}
