package io.xpipe.app.hub.entry;

import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.process.ShellStoreState;
import io.xpipe.app.process.ShellTtyState;
import io.xpipe.app.process.SystemState;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.LicensedFeature;

import lombok.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Value
public class StoreEntryInformation {

    public static StoreEntryInformation ofSystemState(SystemState s) {
        return ofSystemState(s, new LicensedFeature[0]);
    }

    public static StoreEntryInformation ofSystemState(SystemState s, LicensedFeature... additionalLicensedFeatures) {
        var l = new ArrayList<StoreEntryBadge>();

        var allFeatures = new ArrayList<LicensedFeature>();
        allFeatures.addAll(Arrays.stream(additionalLicensedFeatures)
                .filter(licensedFeature -> licensedFeature != null)
                .toList());

        var osFeature = LicenseProvider.get().checkOsName(s.getOsName());
        if (osFeature != null) {
            allFeatures.add(osFeature);
        }

        if (!allFeatures.isEmpty()) {
            l.add(StoreEntryBadge.ofLicense(osFeature));
        }

        if (s.getShellDialect() != null && !s.getShellDialect().getDumbMode().supportsAnyPossibleInteraction()) {
            if (s.getOsName() != null) {
                l.add(StoreEntryBadge.ofSystemName(s.getOsType(), s.getOsName()));
                return StoreEntryInformation.of(l);
            } else if (s.getShellDialect() == ShellDialects.DISABLED_INTERACTION) {
                return StoreEntryInformation.of(l);
            } else if (s.getShellDialect() != null) {
                l.add(StoreEntryBadge.ofSetting(s.getShellDialect().getDisplayName()));
                return StoreEntryInformation.of(l);
            } else {
                return StoreEntryInformation.of(l);
            }
        }

        if (s.getOsName() != null) {
            l.add(StoreEntryBadge.ofSystemName(s.getOsType(), s.getOsName()));
        } else if (s instanceof ShellStoreState sss && sss.getRunning() != null && sss.getRunning()) {
            // Don't add anything
        } else if (s.getShellDialect() == null && s.getOsName() == null) {
            l.add(StoreEntryBadge.ofUnknownSystemName().withCompressBehaviour(StoreEntryBadge.CompressBehaviour.HIDE));
        }

        if (s.getTtyState() != null && s.getTtyState() != ShellTtyState.NONE) {
            l.add(StoreEntryBadge.ofConnectionType("TTY"));
        }

        return StoreEntryInformation.of(l);
    }

    public static StoreEntryInformation of(StoreEntryBadge... badges) {
        return new StoreEntryInformation(Arrays.stream(badges)
                .filter(storeEntryBadge -> storeEntryBadge != null)
                .toList());
    }

    public static StoreEntryInformation of(List<StoreEntryBadge> badges) {
        return new StoreEntryInformation(badges.stream()
                .filter(storeEntryBadge -> storeEntryBadge != null)
                .toList());
    }

    public StoreEntryInformation prepend(StoreEntryInformation information) {
        var l = new ArrayList<StoreEntryBadge>();
        l.addAll(information.getBadges());
        l.addAll(badges);
        return new StoreEntryInformation(l);
    }

    public StoreEntryInformation append(StoreEntryInformation information) {
        var l = new ArrayList<>(badges);
        l.addAll(information.getBadges());
        return new StoreEntryInformation(l);
    }

    List<StoreEntryBadge> badges;

    public StoreEntryInformation censored() {
        return new StoreEntryInformation(badges.stream().map(b -> b.censored()).toList());
    }

    public String toJoinedString() {
        return badges.stream()
                .map(storeEntryBadge -> storeEntryBadge.getName())
                .filter(s -> s != null)
                .collect(Collectors.joining(" "));
    }

    public boolean isValid() {
        return badges.size() > 0;
    }
}
