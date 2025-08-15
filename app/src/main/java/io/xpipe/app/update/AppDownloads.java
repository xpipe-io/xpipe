package io.xpipe.app.update;

import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.*;
import io.xpipe.core.JacksonMapper;
import io.xpipe.core.OsType;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class AppDownloads {

    public static Path downloadInstaller(String version) throws Exception {
        try {
            var release = AppRelease.of(version);
            var builder = HttpRequest.newBuilder();
            var httpRequest = builder.uri(URI.create(release.getUrl())).GET().build();
            var client = HttpHelper.client();
            var response = client.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                throw new IOException(new String(response.body(), StandardCharsets.UTF_8));
            }

            var downloadFile = FileUtils.getTempDirectory().toPath().resolve(release.getFile());
            Files.write(downloadFile, response.body());
            TrackEvent.withInfo("Downloaded asset")
                    .tag("version", version)
                    .tag("url", release.getUrl())
                    .tag("size", FileUtils.byteCountToDisplaySize(response.body().length))
                    .tag("target", downloadFile)
                    .handle();

            return downloadFile;
        } catch (IOException ex) {
            // All sorts of things can go wrong when downloading, this is expected
            ErrorEventFactory.expected(ex);
            throw ex;
        }
    }

    public static String downloadChangelog(String version) throws Exception {
        var uri = URI.create(
                "https://api.xpipe.io/changelog?from=" + AppProperties.get().getVersion()
                        + "&to="
                        + version
                        + "&stage="
                        + AppProperties.get().isStaging());
        var builder = HttpRequest.newBuilder();
        var httpRequest = builder.uri(uri).GET().build();
        var client = HttpHelper.client();
        var response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException(response.body());
        }
        var json = JacksonMapper.getDefault().readTree(response.body());
        var changelog = json.required("changelog").asText();
        return changelog;
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
        req.put("dist", AppDistributionType.get().getId());
        req.put(
                "lang",
                AppPrefs.get() != null ? AppPrefs.get().language().getValue().getId() : null);
        var url = URI.create("https://api.xpipe.io/version");

        var builder = HttpRequest.newBuilder();
        var httpRequest = builder.uri(url)
                .POST(HttpRequest.BodyPublishers.ofString(req.toPrettyString()))
                .build();
        var client = HttpHelper.client();
        var response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException(response.body());
        }

        var dateEntry = response.headers().firstValue("Date");
        if (dateEntry.isPresent()) {
            LicenseProvider.get().updateDate(dateEntry.get());
        }

        var json = JacksonMapper.getDefault().readTree(response.body());
        var ver = json.required("version").asText();
        var ptbAvailable = json.get("ptbAvailable");
        if (ptbAvailable != null) {
            var b = ptbAvailable.asBoolean();
            if (b && AppLayoutModel.get() != null && !AppProperties.get().isStaging()) {
                GlobalTimer.delay(
                        () -> {
                            AppLayoutModel.get().getPtbAvailable().set(true);
                        },
                        Duration.ofSeconds(20));
            }
        }
        return ver;
    }

    public static AppRelease queryLatestRelease(boolean first, boolean securityOnly) throws Exception {
        try {
            var ver = queryLatestVersion(first, securityOnly);
            return AppRelease.of(ver);
        } catch (Exception e) {
            throw ErrorEventFactory.expected(e);
        }
    }
}
