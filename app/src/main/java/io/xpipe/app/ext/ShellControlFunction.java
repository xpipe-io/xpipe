package io.xpipe.app.ext;

import io.xpipe.app.process.ShellControl;
import io.xpipe.app.util.LicensedFeature;

public interface ShellControlFunction {

    ShellControl control() throws Exception;

    default ShellControlFunction withLicenseRequirement(LicensedFeature licensedFeature) {
        return new ShellControlFunction() {

            @Override
            public ShellControl control() throws Exception {
                var sc = ShellControlFunction.this.control();
                sc.requireLicensedFeature(licensedFeature);
                return sc;
            }
        };
    }
}
