package io.xpipe.app.update;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.event.TrackEvent;
import io.xpipe.extension.util.HttpHelper;
import org.apache.commons.io.FileUtils;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.RateLimitHandler;
import org.kohsuke.github.authorization.AuthorizationProvider;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class AppDownloads {

    private static GHRepository repository;

    @SuppressWarnings("deprecation")
    private static GHRepository getRepository() throws IOException {
        if (repository != null) {
            return repository;
        }

        var github = new GitHubBuilder()
                .withRateLimitHandler(RateLimitHandler.FAIL)
                .withAuthorizationProvider(AuthorizationProvider.ANONYMOUS)
                .build();
        repository = github.getRepository("xpipe-io/xpipe");
        return repository;
    }

    public static Path downloadInstaller(AppInstaller.InstallerAssetType iAsset, String version) throws Exception {
        var release = AppDownloads.getRelease(version);
        var asset = release.orElseThrow().listAssets().toList().stream()
                .filter(ghAsset -> iAsset.isCorrectAsset(ghAsset.getName()))
                .findAny();

        var url = new URL(asset.get().getBrowserDownloadUrl());
        var bytes = HttpHelper.executeGet(url, aFloat -> {});
        var downloadFile =
                FileUtils.getTempDirectory().toPath().resolve(asset.get().getName());
        Files.write(downloadFile, bytes);

        TrackEvent.withInfo("installation", "Downloaded asset")
                .tag("version", version)
                .tag("url", url)
                .tag("size", FileUtils.byteCountToDisplaySize(bytes.length))
                .tag("target", downloadFile)
                .handle();

        return downloadFile;
    }

    public static Optional<String> downloadChangelog(String version) throws Exception {
        var release = AppDownloads.getRelease(version);
        var asset = release.orElseThrow().listAssets().toList().stream()
                .filter(ghAsset -> ghAsset.getName().equals("changelog.md"))
                .findAny();

        if (asset.isEmpty()) {
            return Optional.empty();
        }

        var url = new URL(asset.get().getBrowserDownloadUrl());
        var bytes = HttpHelper.executeGet(url, aFloat -> {});
        return Optional.of(new String(bytes, StandardCharsets.UTF_8));
    }

    public static String getLatestVersion() {
        return getLatestSuitableRelease()
                .map(ghRelease -> ghRelease.getTagName())
                .orElse("?");
    }

    public static Optional<GHRelease> getLatestSuitableRelease() {
        try {
            var repo = getRepository();

            // Always choose most up-to-date release as we assume that there are only full releases and prereleases
            if (AppPrefs.get().updateToPrereleases().get()) {
                return Optional.ofNullable(repo.listReleases().iterator().next());
            }

            return Optional.ofNullable(repo.getLatestRelease());
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).omit().handle();
            return Optional.empty();
        }
    }

    public static Optional<GHRelease> getRelease(String version) {
        try {
            var repo = getRepository();
            return Optional.ofNullable(repo.getReleaseByTagName(version));
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).omit().handle();
            return Optional.empty();
        }
    }
}
