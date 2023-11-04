package io.xpipe.app.update;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.HttpHelper;
import org.apache.commons.io.FileUtils;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.RateLimitHandler;
import org.kohsuke.github.authorization.AuthorizationProvider;

import java.io.IOException;
import java.net.URI;
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

        var github = new GitHubBuilder().withRateLimitHandler(RateLimitHandler.FAIL).withAuthorizationProvider(AuthorizationProvider.ANONYMOUS)
                .build();
        repository = github.getRepository(AppProperties.get().isStaging() ? "xpipe-io/xpipe-ptb" : "xpipe-io/xpipe");
        return repository;
    }

    public static Optional<Path> downloadInstaller(
            AppInstaller.InstallerAssetType iAsset, String version, boolean omitErrors
    ) {
        var release = AppDownloads.getRelease(version, omitErrors);
        if (release.isEmpty()) {
            return Optional.empty();
        }

        try {
            var asset = release.orElseThrow().listAssets().toList().stream().filter(ghAsset -> iAsset.isCorrectAsset(ghAsset.getName())).findAny();
            if (asset.isEmpty()) {
                ErrorEvent.fromMessage("No matching asset found for " + iAsset.getExtension());
                return Optional.empty();
            }

            var url = URI.create(asset.get().getBrowserDownloadUrl()).toURL();
            var bytes = HttpHelper.executeGet(url, aFloat -> {});
            var downloadFile = FileUtils.getTempDirectory().toPath().resolve(asset.get().getName());
            Files.write(downloadFile, bytes);

            TrackEvent.withInfo("installation", "Downloaded asset").tag("version", version).tag("url", url).tag("size",
                    FileUtils.byteCountToDisplaySize(bytes.length)).tag("target", downloadFile).handle();

            return Optional.of(downloadFile);
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t).omitted(omitErrors).handle();
            return Optional.empty();
        }
    }

    public static Optional<String> downloadChangelog(String version, boolean omitErrors) {
        var release = AppDownloads.getRelease(version, omitErrors);
        if (release.isEmpty()) {
            return Optional.empty();
        }

        try {
            var asset = release.get().listAssets().toList().stream().filter(ghAsset -> ghAsset.getName().equals("changelog.md")).findAny();

            if (asset.isEmpty()) {
                return Optional.empty();
            }

            var url = URI.create(asset.get().getBrowserDownloadUrl()).toURL();
            var bytes = HttpHelper.executeGet(url, aFloat -> {});
            return Optional.of(new String(bytes, StandardCharsets.UTF_8));
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t).omitted(omitErrors).handle();
            return Optional.empty();
        }
    }

    public static String getLatestVersion() throws IOException {
        return getLatestSuitableRelease().map(ghRelease -> ghRelease.getTagName()).orElse("?");
    }

    public static Optional<GHRelease> getLatestIncludingPreRelease() throws IOException {
        var repo = getRepository();
        return Optional.ofNullable(repo.listReleases().iterator().next());
    }

    public static Optional<GHRelease> getLatestRelease() throws IOException {
        var repo = getRepository();
        return Optional.ofNullable(repo.getLatestRelease());
    }

    public static Optional<GHRelease> getLatestSuitableRelease() throws IOException {
        var preIncluding = getLatestIncludingPreRelease();

        // If we are currently running a prerelease, always return this as the suitable release!
        if (preIncluding.isPresent() && preIncluding.get().isPrerelease() && AppProperties.get().getVersion().equals(
                preIncluding.get().getTagName())) {
            return preIncluding;
        }

        // If this release is not a prerelease, just return it to prevent querying another release
        if (preIncluding.isPresent() && !preIncluding.get().isPrerelease()) {
            return preIncluding;
        }

        return getLatestRelease();
    }

    public static Optional<GHRelease> getRelease(String version, boolean omitErrors) {
        try {
            var repo = getRepository();
            return Optional.ofNullable(repo.getReleaseByTagName(version));
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).omitted(omitErrors).handle();
            return Optional.empty();
        }
    }
}
