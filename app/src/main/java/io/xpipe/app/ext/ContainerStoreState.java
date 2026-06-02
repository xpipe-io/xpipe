package io.xpipe.app.ext;

import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.process.ShellStoreState;
import io.xpipe.core.OsType;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@Jacksonized
public class ContainerStoreState extends ShellStoreState {

    public static ShellDialect findSuitableDialect(ShellControl sc) throws Exception {
        if (!sc.getShellDialect().getDumbMode().supportsAnyPossibleInteraction()) {
            return sc.getOsType() == OsType.WINDOWS ? ShellDialects.CMD : ShellDialects.SH;
        }

        if (sc.getOsType() != OsType.WINDOWS) {
            if (sc.view().findProgram("bash").isPresent()) {
                return ShellDialects.BASH;
            }

            if (sc.view().findProgram("zsh").isPresent()) {
                return ShellDialects.ZSH;
            }

            return ShellDialects.SH;
        } else {
            if (sc.view().findProgram("pwsh").isPresent()) {
                return ShellDialects.POWERSHELL_CORE;
            }

            if (sc.view().findProgram("powershell").isPresent()) {
                return ShellDialects.POWERSHELL;
            }

            return ShellDialects.CMD;
        }
    }

    String imageName;
    String containerState;
    ShellDialect availableShellDialect;
    Boolean shellMissing;

    public ShellDialect getEffectiveDialect() {
        if (availableShellDialect != null) {
            return availableShellDialect;
        }

        return ShellDialects.SH;
    }

    @Override
    public DataStoreState mergeCopy(DataStoreState newer) {
        var n = (ContainerStoreState) newer;
        var b = toBuilder();
        mergeBuilder(n, b);
        return b.build();
    }

    protected void mergeBuilder(ContainerStoreState css, ContainerStoreStateBuilder<?, ?> b) {
        super.mergeBuilder(css, b);
        b.containerState(useNewer(containerState, css.getContainerState()));
        b.imageName(useNewer(imageName, css.getImageName()));
        b.availableShellDialect(useNewer(availableShellDialect, css.getAvailableShellDialect()));
        b.shellMissing(useNewer(shellMissing, css.getShellMissing()));
    }
}
