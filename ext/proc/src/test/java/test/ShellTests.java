package test;

import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.process.ShellType;
import io.xpipe.extension.util.LocalExtensionTest;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import test.item.ShellCheckTestItem;
import test.item.ShellTestItem;

import java.util.stream.Stream;

public class ShellTests extends LocalExtensionTest {

    static Stream<Arguments> shellChecksProvider() {
        Stream.Builder<Arguments> argumentBuilder = Stream.builder();
        for (var arg : ShellTestItem.getAll().toList()) {
            for (var c : ShellCheckTestItem.values()) {
                argumentBuilder.add(Arguments.of(arg, Named.of(c.name(), c)));
            }
        }
        return argumentBuilder.build();
    }

    @ParameterizedTest
    @MethodSource("shellChecksProvider")
    public void testShellChecks(ShellProcessControl shellTestItem, ShellCheckTestItem ti) throws Exception {
        try (var pc = shellTestItem.start()) {
            ti.getShellCheck().accept(pc);
        }
    }

    @ParameterizedTest
    @MethodSource("shellChecksProvider")
    public void testDoubleShellChecks(ShellProcessControl shellTestItem, ShellCheckTestItem ti) throws Exception {
        try (var pc = shellTestItem.start()) {
            ti.getShellCheck().accept(pc);
            pc.start();
            ti.getShellCheck().accept(pc);
        }
    }

    @ParameterizedTest
    @MethodSource("shellChecksProvider")
    public void testSubShellChecks(ShellProcessControl shellTestItem, ShellCheckTestItem ti) throws Exception {
        try (var pc = shellTestItem.start()) {
            try (ShellProcessControl sub = pc.subShell(pc.getShellType()).start()) {
                ti.getShellCheck().accept(sub);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("shellChecksProvider")
    public void testSubDoubleShellChecks(ShellProcessControl shellTestItem, ShellCheckTestItem ti) throws Exception {
        try (var pc = shellTestItem.start()) {
            try (ShellProcessControl sub = pc.subShell(pc.getShellType()).start()) {
                ti.getShellCheck().accept(sub);
                sub.start();
                ti.getShellCheck().accept(sub);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("shellChecksProvider")
    public void testDoubleSubShellChecks(ShellProcessControl shellTestItem, ShellCheckTestItem ti) throws Exception {
        try (var pc = shellTestItem.start()) {
            ShellType t = pc.getShellType();
            try (ShellProcessControl sub = pc.subShell(t).start()) {
                ti.getShellCheck().accept(sub);
            }
            try (ShellProcessControl sub = pc.subShell(t).start()) {
                ti.getShellCheck().accept(sub);
            }
        }
    }
}
