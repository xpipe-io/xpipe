package io.xpipe.app.ext;

import lombok.Getter;

import java.nio.file.Path;

public abstract class ModuleInstall {

    @Getter
    private final String id;

    @Getter
    private final String module;

    protected ModuleInstall(String id, String module) {
        this.id = id;
        this.module = module;
    }

    public abstract String getLicenseFile();

    public abstract String getVendorURL();

    public abstract void installInternal(Path directory) throws Exception;
}
