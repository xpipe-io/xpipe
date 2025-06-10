package io.xpipe.core.process;

import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.util.FailableConsumer;

import lombok.Getter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

@Getter
public class WrapperShellControl implements ShellControl {

    protected final ShellControl parent;

    public WrapperShellControl(ShellControl parent) {
        this.parent = parent;
    }

    @Override
    public ShellView view() {
        return parent.view();
    }

    @Override
    public Optional<ShellControl> getParentControl() {
        return parent.getParentControl();
    }

    @Override
    public ShellTtyState getTtyState() {
        return parent.getTtyState();
    }

    @Override
    public void setNonInteractive() {
        parent.setNonInteractive();
    }

    @Override
    public boolean isInteractive() {
        return parent.isInteractive();
    }

    @Override
    public ElevationHandler getElevationHandler() {
        return parent.getElevationHandler();
    }

    @Override
    public void setElevationHandler(ElevationHandler ref) {
        parent.setElevationHandler(ref);
    }

    @Override
    public void closeStdout() throws IOException {
        parent.closeStdout();
    }

    @Override
    public List<UUID> getExitUuids() {
        return parent.getExitUuids();
    }

    @Override
    public void setWorkingDirectory(WorkingDirectoryFunction workingDirectory) {
        parent.setWorkingDirectory(workingDirectory);
    }

    @Override
    public Optional<DataStore> getSourceStore() {
        return parent.getSourceStore();
    }

    @Override
    public Optional<UUID> getSourceStoreId() {
        return parent.getSourceStoreId();
    }

    @Override
    public ShellControl withSourceStore(DataStore store) {
        return parent.withSourceStore(store);
    }

    @Override
    public List<ShellTerminalInitCommand> getTerminalInitCommands() {
        return parent.getTerminalInitCommands();
    }

    @Override
    public ParentSystemAccess getParentSystemAccess() {
        return parent.getParentSystemAccess();
    }

    @Override
    public void setParentSystemAccess(ParentSystemAccess access) {
        parent.setParentSystemAccess(access);
    }

    @Override
    public ParentSystemAccess getLocalSystemAccess() {
        return parent.getLocalSystemAccess();
    }

    @Override
    public boolean isLocal() {
        return parent.isLocal();
    }

    @Override
    public ShellControl getMachineRootSession() throws Exception {
        return parent.getMachineRootSession();
    }

    @Override
    public String getOsName() {
        return parent.getOsName();
    }

    @Override
    public ReentrantLock getLock() {
        return parent.getLock();
    }

    @Override
    public void requireLicensedFeature(String id) {
        parent.requireLicensedFeature(id);
    }

    @Override
    public ShellDialect getOriginalShellDialect() {
        return parent.getOriginalShellDialect();
    }

    @Override
    public void setOriginalShellDialect(ShellDialect dialect) {
        parent.setOriginalShellDialect(dialect);
    }

    @Override
    public ShellControl onInit(FailableConsumer<ShellControl, Exception> pc) {
        return parent.onInit(pc);
    }

    @Override
    public ShellControl onExit(Consumer<ShellControl> pc) {
        return parent.onExit(pc);
    }

    @Override
    public ShellControl onKill(Runnable pc) {
        return parent.onKill(pc);
    }

    @Override
    public ShellControl onStartupFail(Consumer<Throwable> t) {
        return parent.onStartupFail(t);
    }

    @Override
    public UUID getUuid() {
        return parent.getUuid();
    }

    @Override
    public ShellControl withExceptionConverter(ExceptionConverter converter) {
        return parent.withExceptionConverter(converter);
    }

    @Override
    public void resetData() {
        parent.resetData();
    }

    @Override
    public String prepareTerminalOpen(TerminalInitScriptConfig config, WorkingDirectoryFunction workingDirectory)
            throws Exception {
        return parent.prepareTerminalOpen(config, workingDirectory);
    }

    @Override
    public void refreshRunningState() {
        parent.refreshRunningState();
    }

    @Override
    public void closeStdin() throws IOException {
        parent.closeStdin();
    }

    @Override
    public boolean isAnyStreamClosed() {
        return parent.isAnyStreamClosed();
    }

    @Override
    public boolean isRunning(boolean refresh) {
        return parent.isRunning(true);
    }

    @Override
    public ShellDialect getShellDialect() {
        return parent.getShellDialect();
    }

    @Override
    public void setUser(String user) {
        parent.setUser(user);
    }

    @Override
    public boolean isInitializing() {
        return parent.isInitializing();
    }

    @Override
    public void setDumbOpen(ShellOpenFunction openFunction) {
        parent.setDumbOpen(openFunction);
    }

    @Override
    public void setTerminalOpen(ShellOpenFunction openFunction) {
        parent.setTerminalOpen(openFunction);
    }

    @Override
    public void writeLine(String line) throws IOException {
        parent.writeLine(line);
    }

    @Override
    public void writeLine(String line, boolean log) throws IOException {
        parent.writeLine(line, log);
    }

    @Override
    public void write(byte[] b) throws IOException {
        parent.write(b);
    }

    @Override
    public void close() throws Exception {
        parent.close();
    }

    @Override
    public void shutdown() throws Exception {
        parent.shutdown();
    }

    @Override
    public void kill() {
        parent.kill();
    }

    @Override
    public void killExternal() {
        parent.killExternal();
    }

    @Override
    public ShellControl start() throws Exception {
        return parent.start();
    }

    @Override
    public LocalProcessInputStream getStdout() {
        return parent.getStdout();
    }

    @Override
    public LocalProcessOutputStream getStdin() {
        return parent.getStdin();
    }

    @Override
    public LocalProcessInputStream getStderr() {
        return parent.getStderr();
    }

    @Override
    public Charset getCharset() {
        return parent.getCharset();
    }

    @Override
    public ShellControl withErrorFormatter(Function<String, String> formatter) {
        return parent.withErrorFormatter(formatter);
    }

    @Override
    public void checkLicenseOrThrow() {
        parent.checkLicenseOrThrow();
    }

    @Override
    public String prepareIntermediateTerminalOpen(
            TerminalInitFunction content, TerminalInitScriptConfig config, WorkingDirectoryFunction workingDirectory)
            throws Exception {
        return parent.prepareIntermediateTerminalOpen(content, config, workingDirectory);
    }

    @Override
    public FilePath getSystemTemporaryDirectory() {
        return parent.getSystemTemporaryDirectory();
    }

    @Override
    public ShellControl withSecurityPolicy(ShellSecurityPolicy policy) {
        return parent.withSecurityPolicy(policy);
    }

    @Override
    public ShellSecurityPolicy getEffectiveSecurityPolicy() {
        return parent.getEffectiveSecurityPolicy();
    }

    @Override
    public String buildElevatedCommand(CommandConfiguration input, String prefix, UUID requestId, CountDown countDown)
            throws Exception {
        return parent.buildElevatedCommand(input, prefix, requestId, countDown);
    }

    @Override
    public void restart() throws Exception {
        parent.restart();
    }

    @Override
    public OsType.Any getOsType() {
        return parent.getOsType();
    }

    @Override
    public ShellControl elevated(ElevationFunction elevationFunction) {
        return parent.elevated(elevationFunction);
    }

    @Override
    public ShellControl withInitSnippet(ShellTerminalInitCommand snippet) {
        return parent.withInitSnippet(snippet);
    }

    @Override
    public Optional<ShellControl> getActiveReplacementBackgroundSession() throws Exception {
        return parent.getActiveReplacementBackgroundSession();
    }

    @Override
    public ShellControl subShell() {
        return parent.subShell();
    }

    @Override
    public CommandControl command(CommandBuilder builder) {
        return parent.command(builder);
    }

    @Override
    public void exitAndWait() throws IOException {
        parent.exitAndWait();
    }
}
