package test;

import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.extension.test.LocalExtensionTest;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import test.item.CommandCheckTestItem;
import test.item.ShellTestItem;

import java.util.stream.Stream;

public class CommandTests extends LocalExtensionTest {

    static Stream<Arguments> commandChecksProvider() {
        Stream.Builder<Arguments> argumentBuilder = Stream.builder();
        for (var arg : ShellTestItem.getAll().toList()) {
            for (var c : CommandCheckTestItem.values()) {
                argumentBuilder.add(Arguments.of(arg, Named.of(c.name(), c)));
            }
        }
        return argumentBuilder.build();
    }

    @ParameterizedTest
    @MethodSource("commandChecksProvider")
    public void testCommandChecks(ShellProcessControl shellTestItem, CommandCheckTestItem tc) throws Exception {
        try (var pc = shellTestItem.start()) {
            try (var c = pc.command(tc.getCommandFunction().apply(pc)).start()) {
                tc.getCommandCheck().accept(c);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("commandChecksProvider")
    public void testDoubleCommandChecks(ShellProcessControl shellTestItem, CommandCheckTestItem tc) throws Exception {
        try (var pc = shellTestItem.start()) {
            try (var c = pc.command(tc.getCommandFunction().apply(pc)).start()) {
                tc.getCommandCheck().accept(c);
            }

            try (var c = pc.command(tc.getCommandFunction().apply(pc)).start()) {
                tc.getCommandCheck().accept(c);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("commandChecksProvider")
    public void testSubCommandChecks(ShellProcessControl shellTestItem, CommandCheckTestItem tc) throws Exception {
        try (var pc = shellTestItem.start()) {
            try (ShellProcessControl sub = pc.subShell(pc.getShellType()).start()) {
                try (var c = sub.command(tc.getCommandFunction().apply(sub)).start()) {
                    tc.getCommandCheck().accept(c);
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("commandChecksProvider")
    public void testSubDoubleCommandChecks(ShellProcessControl shellTestItem, CommandCheckTestItem tc)
            throws Exception {
        try (var pc = shellTestItem.start()) {
            try (ShellProcessControl sub = pc.subShell(pc.getShellType()).start()) {
                try (var c = sub.command(tc.getCommandFunction().apply(sub)).start()) {
                    tc.getCommandCheck().accept(c);
                }

                try (var c = sub.command(tc.getCommandFunction().apply(sub)).start()) {
                    tc.getCommandCheck().accept(c);
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("commandChecksProvider")
    public void testDoubleSubCommandChecks(ShellProcessControl shellTestItem, CommandCheckTestItem tc)
            throws Exception {
        try (var pc = shellTestItem.start()) {
            try (ShellProcessControl sub = pc.subShell(pc.getShellType()).start()) {
                try (var c = sub.command(tc.getCommandFunction().apply(sub)).start()) {
                    tc.getCommandCheck().accept(c);
                }
            }

            try (ShellProcessControl sub = pc.subShell(pc.getShellType()).start()) {
                try (var c = sub.command(tc.getCommandFunction().apply(sub)).start()) {
                    tc.getCommandCheck().accept(c);
                }
            }
        }
    }
}
