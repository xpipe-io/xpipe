package io.xpipe.extension.util;

import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.source.DataSource;
import io.xpipe.extension.DataSourceProvider;

public interface UniformDataSourceProvider<T extends DataSource<?>> extends DataSourceProvider<T> {

    @Override
    default Dialog configDialog(T source, boolean all) {
        return Dialog.empty().evaluateTo(() -> source);
    }
}
