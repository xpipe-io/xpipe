package io.xpipe.app.ext;

import lombok.Getter;

import java.util.List;

public abstract class DownloadModuleInstall extends ModuleInstall {

    private final String licenseFile;
    private final String vendorURL;

    @Getter
    private final List<String> assets;

    public DownloadModuleInstall(String id, String module, String licenseFile, String vendorURL, List<String> assets) {
        super(id, module);
        this.licenseFile = licenseFile;
        this.vendorURL = vendorURL;
        this.assets = assets;
    }

    @Override
    public String getLicenseFile() {
        return licenseFile;
    }

    @Override
    public String getVendorURL() {
        return vendorURL;
    }
}
