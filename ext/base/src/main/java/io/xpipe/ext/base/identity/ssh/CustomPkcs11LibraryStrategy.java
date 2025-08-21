package io.xpipe.ext.base.identity.ssh;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.FilePath;
import io.xpipe.core.KeyValue;
import io.xpipe.core.OsType;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.util.List;

@Value
@Jacksonized
@Builder
@JsonTypeName("customPkcs11")
@AllArgsConstructor
public class CustomPkcs11LibraryStrategy implements SshIdentityStrategy {

    @SuppressWarnings("unused")
    public static String getOptionsNameKey() {
        return "customPkcs11Library";
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<CustomPkcs11LibraryStrategy> p, SshIdentityStrategyChoiceConfig config) {

        var file = new SimpleObjectProperty<>(p.getValue() != null ? p.getValue().getFile() : null);
        return new OptionsBuilder().nameAndDescription("pkcs11Library")
                .addComp(new ContextualFileReferenceChoiceComp(new ReadOnlyObjectWrapper<>(DataStorage.get().local().ref()), file, null, List.of()),
                        file)
                .nonNull()
                .bind(() -> {
                    return new CustomPkcs11LibraryStrategy(file.get());
                }, p);
    }

    FilePath file;

    @Override
    public void prepareParent(ShellControl parent) throws Exception {
        parent.requireLicensedFeature(LicenseProvider.get().getFeature("pkcs11Identity"));

        if (!parent.getShellDialect().createFileExistsCommand(parent, file.toString()).executeAndCheck()) {
            throw ErrorEventFactory.expected(new IOException("PKCS11 library at " + file + " not found"));
        }
    }

    @Override
    public void buildCommand(CommandBuilder builder) {
        builder.setup(sc -> {
            var dir = file.getParent();
            if (sc.getOsType() == OsType.WINDOWS) {
                builder.addToPath(dir, true);
            } else {
                builder.addToEnvironmentPath("LD_LIBRARY_PATH", dir, true);
            }
        });
    }

    @Override
    public List<KeyValue> configOptions(ShellControl parent) {
        return List.of(new KeyValue("IdentitiesOnly", "no"), new KeyValue("PKCS11Provider", file.toString()), new KeyValue("IdentityFile", "none"),
                new KeyValue("IdentityAgent", "none"));
    }
}
