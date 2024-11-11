package io.xpipe.app.util;

import io.xpipe.app.comp.store.StoreSection;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.process.ShellEnvironmentStoreState;
import io.xpipe.core.process.ShellStoreState;
import io.xpipe.core.process.ShellTtyState;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;

import lombok.Value;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

@Value
public class ShellStoreFormat {

    public static ObservableValue<String> shellEnvironment(StoreSection section, boolean includeOsName) {
        return Bindings.createStringBinding(
                () -> {
                    var s = (ShellEnvironmentStoreState)
                            section.getWrapper().getPersistentState().getValue();
                    var def = Boolean.TRUE.equals(s.getSetDefault()) ? AppI18n.get("default") : null;
                    var name = DataStoreFormatter.join(
                            (includeOsName ? formattedOsName(s.getOsName()) : null), s.getShellName());
                    return new ShellStoreFormat(null, name, new String[] {def}).format();
                },
                AppPrefs.get().language(),
                section.getWrapper().getPersistentState());
    }

    @SuppressWarnings("unchecked")
    public static <T extends ShellStoreState> ObservableValue<String> shellStore(
            StoreSection section, Function<T, String> f) {
        return BindingsHelper.map(section.getWrapper().getPersistentState(), o -> {
            var s = (T) o;
            var info = f.apply(s);
            if (s.getShellDialect() != null
                    && !s.getShellDialect().getDumbMode().supportsAnyPossibleInteraction()) {
                if (s.getOsName() != null) {
                    return new ShellStoreFormat(
                                    LicenseProvider.get().checkOsName(s.getOsName()),
                                    formattedOsName(s.getOsName()),
                                    new String[] {info})
                            .format();
                }

                if (s.getShellDialect().equals(ShellDialects.NO_INTERACTION)) {
                    return new ShellStoreFormat(null, null, new String[] {info}).format();
                }

                return new ShellStoreFormat(
                                LicenseProvider.get()
                                        .getFeature(s.getShellDialect().getLicenseFeatureId()),
                                s.getShellDialect().getDisplayName(),
                                new String[] {info})
                        .format();
            }

            return new ShellStoreFormat(
                            LicenseProvider.get().checkOsName(s.getOsName()),
                            formattedOsName(s.getOsName()),
                            new String[] {
                                s.getTtyState() != null && s.getTtyState() != ShellTtyState.NONE ? "TTY" : null, info
                            })
                    .format();
        });
    }

    LicensedFeature licensedFeature;
    String name;
    String[] states;

    public String format() {
        var licenseReq =
                licensedFeature != null ? licensedFeature.getDescriptionSuffix().orElse(null) : null;
        var lic = licenseReq != null ? "[" + licenseReq + "+]" : null;
        var name = this.name;
        var state = getStates() != null
                ? Arrays.stream(getStates())
                        .filter(s -> s != null)
                        .map(s -> "[" + s + "]")
                        .collect(Collectors.joining(" "))
                : null;
        if (state != null && state.isEmpty()) {
            state = null;
        }
        return DataStoreFormatter.join(lic, name, state);
    }

    public static String formattedOsName(String osName) {
        if (osName == null) {
            return null;
        }

        osName = osName.replaceAll("^Microsoft ", "");
        osName = osName.replaceAll("Enterprise Evaluation", "Enterprise");
        return osName;
    }
}
