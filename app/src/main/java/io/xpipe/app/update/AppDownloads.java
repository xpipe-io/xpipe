package io.xpipe.app.update;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.HttpHelper;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.core.process.OsType;
import io.xpipe.core.util.JacksonMapper;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.commons.io.FileUtils;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.RateLimitHandler;
import org.kohsuke.github.authorization.AuthorizationProvider;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
        repository = github.getRepository(AppProperties.get().isStaging() ? "xpipe-io/xpipe-ptb" : "xpipe-io/xpipe");
        return repository;
    }

    public static Optional<Path> downloadInstaller(
            AppInstaller.InstallerAssetType iAsset, String version, boolean omitErrors) {
        var release = AppDownloads.getRelease(version, omitErrors);
        if (release.isEmpty()) {
            return Optional.empty();
        }

        try {
            var asset = release.orElseThrow().listAssets().toList().stream()
                    .filter(ghAsset -> iAsset.isCorrectAsset(ghAsset.getName()))
                    .findAny();
            if (asset.isEmpty()) {
                ErrorEvent.fromMessage("No matching asset found for " + iAsset.getExtension());
                return Optional.empty();
            }

            var url = URI.create(asset.get().getBrowserDownloadUrl()).toURL();
            var bytes = HttpHelper.executeGet(url, aFloat -> {});
            var downloadFile =
                    FileUtils.getTempDirectory().toPath().resolve(asset.get().getName());
            Files.write(downloadFile, bytes);

            TrackEvent.withInfo("Downloaded asset")
                    .tag("version", version)
                    .tag("url", url)
                    .tag("size", FileUtils.byteCountToDisplaySize(bytes.length))
                    .tag("target", downloadFile)
                    .handle();

            return Optional.of(downloadFile);
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t).omitted(omitErrors).expected().handle();
            return Optional.empty();
        }
    }

    public static Optional<String> downloadChangelog(String version, boolean omitErrors) {
        var release = AppDownloads.getRelease(version, omitErrors);
        if (release.isEmpty()) {
            return Optional.empty();
        }

        try {
            var url = URI.create("https://api.xpipe.io/changelog?from="
                            + AppProperties.get().getVersion() + "&to=" + version + "&stage="
                            + AppProperties.get().isStaging())
                    .toURL();
            var bytes = HttpHelper.executeGet(url, aFloat -> {});
            var string = new String(bytes, StandardCharsets.UTF_8);
            var json = JacksonMapper.getDefault().readTree(string);
            var changelog = json.required("changelog").asText();
            return Optional.of(changelog);
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t).omit().expected().handle();
        }

        try {
            var asset = release.get().listAssets().toList().stream()
                    .filter(ghAsset -> ghAsset.getName().equals("changelog.md"))
                    .findAny();

            if (asset.isEmpty()) {
                return Optional.empty();
            }

            var url = URI.create(asset.get().getBrowserDownloadUrl()).toURL();
            var bytes = HttpHelper.executeGet(url, aFloat -> {});
            return Optional.of(new String(bytes, StandardCharsets.UTF_8));
        } catch (Throwable t) {
            ErrorEvent.fromThrowable(t).omitted(omitErrors).expected().handle();
            return Optional.empty();
        }
    }

    private static String queryLatestVersion(boolean first, boolean securityOnly) throws Exception {
        var req = JsonNodeFactory.instance.objectNode();
        req.put("securityOnly", securityOnly);
        req.put("initial", AppProperties.get().isInitialLaunch() && first);
        req.put("ptb", AppProperties.get().isStaging());
        req.put("os", OsType.getLocal().getId());
        req.put("arch", AppProperties.get().getArch());
        req.put("uuid", AppProperties.get().getUuid().toString());
        req.put("version", AppProperties.get().getVersion());
        req.put("first", first);
        req.put("license", LicenseProvider.get().getLicenseId());
        var url = URI.create("https://api.xpipe.io/version");

        var builder = HttpRequest.newBuilder();
        var httpRequest = builder.uri(url)
                .POST(HttpRequest.BodyPublishers.ofString(req.toPrettyString()))
                .build();
        var client = HttpClient.newHttpClient();
        var response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(response.body());
        }

        var json = JacksonMapper.getDefault().readTree(response.body());
        var ver = json.required("version").asText();
        return ver;
    }

    public static Optional<GHRelease> queryLatestRelease(boolean first, boolean securityOnly) throws Exception {
        try {
            var ver = queryLatestVersion(first, securityOnly);
            var repo = getRepository();
            var rel = repo.getReleaseByTagName(ver);
            return Optional.ofNullable(rel);
        } catch (Exception e) {
            throw ErrorEvent.expected(e);
        }
    }

    public static Optional<GHRelease> getRelease(String version, boolean omitErrors) {
        try {
            var repo = getRepository();
            return Optional.ofNullable(repo.getReleaseByTagName(version));
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).omitted(omitErrors).expected().handle();
            return Optional.empty();
        }
    }
}
