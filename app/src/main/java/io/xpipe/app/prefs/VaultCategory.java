package io.xpipe.app.prefs;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.util.LockChangeAlert;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.Validator;
import io.xpipe.core.util.XPipeInstallation;

import javafx.beans.binding.Bindings;

import lombok.SneakyThrows;

public class VaultCategory extends AppPrefsCategory {

    private static final boolean STORAGE_DIR_FIXED = System.getProperty(XPipeInstallation.DATA_DIR_PROP) != null;

    @Override
    protected String getId() {
        return "vault";
    }

    @SneakyThrows
    public Comp<?> create() {
        var prefs = AppPrefs.get();
        var builder = new OptionsBuilder();
        if (!STORAGE_DIR_FIXED) {
            var sub =
                    new OptionsBuilder().nameAndDescription("storageDirectory").addPath(prefs.storageDirectory);
            sub.withValidator(val -> {
                sub.check(Validator.absolutePath(val, prefs.storageDirectory));
                sub.check(Validator.directory(val, prefs.storageDirectory));
            });
            builder.addTitle("storage").sub(sub);
        }
        builder.addTitle("vaultSecurity")
                .sub(new OptionsBuilder()
                        .nameAndDescription("workspaceLock")
                        .addComp(
                                new ButtonComp(
                                        Bindings.createStringBinding(
                                                () -> {
                                                    return prefs.getLockCrypt().getValue() != null
                                                                    && !prefs.getLockCrypt()
                                                                            .getValue()
                                                                            .isEmpty()
                                                            ? AppI18n.get("changeLock")
                                                            : AppI18n.get("createLock");
                                                },
                                                prefs.getLockCrypt()),
                                        LockChangeAlert::show),
                                prefs.getLockCrypt())
                        .nameAndDescription("lockVaultOnHibernation")
                        .addToggle(prefs.lockVaultOnHibernation)
                        .hide(prefs.getLockCrypt()
                                .isNull()
                                .or(prefs.getLockCrypt().isEmpty()))
                        .nameAndDescription("encryptAllVaultData")
                        .addToggle(prefs.encryptAllVaultData));
        return builder.buildComp();
    }
}
