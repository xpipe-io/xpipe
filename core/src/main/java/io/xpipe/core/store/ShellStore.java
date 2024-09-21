package io.xpipe.core.store;

import io.xpipe.core.process.ShellControl;

public interface ShellStore extends DataStore, FileSystemStore, ValidatableStore<ShellValidationContext> {

    @Override
    default FileSystem createFileSystem() {
        return new ConnectionFileSystem(control());
    }

    ShellControl parentControl();

    ShellControl control(ShellControl parent);

    default ShellControl control() {
        return control(parentControl());
    }

    @Override
    default ShellValidationContext validate(ShellValidationContext context) throws Exception {
        var c = control(context.get());
        if (!isInStorage()) {
            c.withoutLicenseCheck();
        }
        return new ShellValidationContext(c.start());
    }

    @Override
    default ShellValidationContext createContext() throws Exception {
        return new ShellValidationContext(parentControl().start());
    }
}
