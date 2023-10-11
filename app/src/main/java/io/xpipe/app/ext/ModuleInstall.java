package io.xpipe.app.ext;

import lombok.Getter;

import java.nio.file.Path;

@Getter
public abstract class ModuleInstall {

    private final String id;

    private final String module;

    protected ModuleInstall(String id, String module) {
        this.id = id;
        this.module = module;
    }

    public abstract String getLicenseFile();

    public abstract String getVendorURL();

    public abstract void installInternal(Path directory) throws Exception;
}
