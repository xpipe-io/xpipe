package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.comp.StoreSection;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.process.ShellEnvironmentStoreState;
import io.xpipe.app.process.ShellStoreState;
import io.xpipe.app.process.ShellTtyState;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;

import lombok.Value;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Value
public class StoreStateFormat {

    public static ObservableValue<String> shellEnvironment(StoreSection section, boolean includeOsName) {
        return Bindings.createStringBinding(
                () -> {
                    var s = (ShellEnvironmentStoreState)
                            section.getWrapper().getPersistentState().getValue();
                    var def = Boolean.TRUE.equals(s.getSetDefault()) ? AppI18n.get("default") : null;
                    var name = DataStoreFormatter.join(
                            (includeOsName ? formattedOsName(s.getOsName()) : null), s.getShellName());
                    return new StoreStateFormat(null, name, def).format();
                },
                AppI18n.activeLanguage(),
                section.getWrapper().getPersistentState());
    }

    @SuppressWarnings("unchecked")
    public static <T extends ShellStoreState> ObservableValue<String> shellStore(
            StoreSection section, Function<T, String[]> f) {
        return BindingsHelper.map(section.getWrapper().getPersistentState(), o -> {
            var s = (T) o;
            var info = f.apply(s);
            if (s.getShellDialect() != null
                    && !s.getShellDialect().getDumbMode().supportsAnyPossibleInteraction()) {
                if (s.getOsName() != null) {
                    return new StoreStateFormat(
                                    LicenseProvider.get().checkOsName(s.getOsName()),
                                    formattedOsName(s.getOsName()),
                                    info)
                            .format();
                }

                if (s.getShellDialect().equals(ShellDialects.NO_INTERACTION)) {
                    return new StoreStateFormat(null, null, info).format();
                }

                return new StoreStateFormat(
                                LicenseProvider.get()
                                        .getFeature(s.getShellDialect().getLicenseFeatureId()),
                                s.getShellDialect().getDisplayName(),
                                info)
                        .format();
            }

            var joined = Stream.concat(
                            Stream.of(s.getTtyState() != null && s.getTtyState() != ShellTtyState.NONE ? "TTY" : null),
                            info != null ? Arrays.stream(info) : Stream.of())
                    .toArray(String[]::new);
            return new StoreStateFormat(
                            LicenseProvider.get().checkOsName(s.getOsName()), formattedOsName(s.getOsName()), joined)
                    .format();
        });
    }

    LicensedFeature licensedFeature;
    String name;
    String[] states;

    public StoreStateFormat(LicensedFeature licensedFeature, String name, String... states) {
        this.licensedFeature = licensedFeature;
        this.name = name;
        this.states = states;
    }

    public String format() {
        var licenseReq =
                licensedFeature != null ? licensedFeature.getDescriptionSuffix().orElse(null) : null;
        var lic = licenseReq != null ? "[" + licenseReq + "]" : null;
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
