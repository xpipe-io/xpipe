package io.xpipe.app.util;

import io.xpipe.app.ext.DataSourceProvider;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.source.DataSource;

public interface UniformDataSourceProvider<T extends DataSource<?>> extends DataSourceProvider<T> {

    @Override
    default Dialog configDialog(T source, boolean all) {
        return Dialog.empty().evaluateTo(() -> source);
    }
}
