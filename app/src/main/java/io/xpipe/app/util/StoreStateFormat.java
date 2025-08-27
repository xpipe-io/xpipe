package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.comp.StoreSection;
import io.xpipe.app.process.*;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;

import lombok.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Value
public class StoreStateFormat {

    List<LicensedFeature> licensedFeatures;
    String name;
    String[] states;

    public StoreStateFormat(List<LicensedFeature> licensedFeatures, String name, String... states) {
        this.licensedFeatures = licensedFeatures;
        this.name = name;
        this.states = states;
    }

    public static ObservableValue<String> shellEnvironment(StoreSection section, boolean includeOsName, LicensedFeature licensedFeature) {
        return Bindings.createStringBinding(
                () -> {
                    var s = (ShellEnvironmentStoreState)
                            section.getWrapper().getPersistentState().getValue();
                    var def = Boolean.TRUE.equals(s.getSetDefault()) ? AppI18n.get("default") : null;
                    var name = DataStoreFormatter.join(
                            (includeOsName ? formattedOsName(s.getOsName()) : null), s.getShellName());
                    return new StoreStateFormat(licensedFeature != null ? List.of(licensedFeature) : List.of(), name, def).format();
                },
                AppI18n.activeLanguage(),
                section.getWrapper().getPersistentState());
    }

    @SuppressWarnings("unchecked")
    public static <T extends SystemState> ObservableValue<String> shellStore(
            StoreSection section, Function<T, String[]> f, LicensedFeature licensedFeature) {
        return BindingsHelper.map(section.getWrapper().getPersistentState(), o -> {
            var s = (T) o;
            var info = f.apply(s);

            var osFeature = LicenseProvider.get().checkOsName(s.getOsName());
            var features = new ArrayList<LicensedFeature>();
            if (osFeature != null) {
                features.add(osFeature);
            }
            if (licensedFeature != null) {
                features.add(licensedFeature);
            }

            if (s.getShellDialect() != null
                    && !s.getShellDialect().getDumbMode().supportsAnyPossibleInteraction()) {
                if (s.getOsName() != null) {
                    return new StoreStateFormat(
                            features,
                                    formattedOsName(s.getOsName()),
                                    info)
                            .format();
                }

                if (s.getShellDialect() == ShellDialects.NO_INTERACTION) {
                    return new StoreStateFormat(List.of(), null, info).format();
                }

                return new StoreStateFormat(
                        features,
                                s.getShellDialect().getDisplayName(),
                                info)
                        .format();
            }

            var joined = Stream.concat(
                            Stream.of(s.getTtyState() != null && s.getTtyState() != ShellTtyState.NONE ? "TTY" : null),
                            info != null ? Arrays.stream(info) : Stream.of())
                    .toArray(String[]::new);
            return new StoreStateFormat(
                    features, formattedOsName(s.getOsName()), joined)
                    .format();
        });
    }

    public static String formattedOsName(String osName) {
        if (osName == null) {
            return null;
        }

        osName = osName.replaceAll("^Microsoft ", "");
        osName = osName.replaceAll("Enterprise Evaluation", "Enterprise");
        return osName;
    }

    public String format() {
        var licenseReq = licensedFeatures.stream()
                .map(licensedFeature -> licensedFeature.getDescriptionSuffix())
                .flatMap(Optional::stream)
                .findFirst()
                .orElse(null);
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
}
