package io.xpipe.app.util;

import io.xpipe.core.JacksonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

public class GithubReleaseDownloader {

    public static Path getDownloadTempFile(String repository, String id, Predicate<String> filter) throws Exception {
        var tempDir = ShellTemp.getLocalTempDataDirectory("github");
        var temp = tempDir.resolve(id);
        if (Files.exists(temp)) {
            return temp;
        }

        var request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(getDownloadUrl(repository, filter)))
                .build();
        var r = HttpHelper.client().send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (r.statusCode() >= 400) {
            throw new IOException(new String(r.body(), StandardCharsets.UTF_8));
        }

        Files.createDirectories(tempDir);
        Files.write(temp, r.body());
        return temp;
    }

    private static String getDownloadUrl(String repository, Predicate<String> filter) throws Exception {
        var request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://api.github.com/repos/" + repository + "/releases"))
                .build();
        var r = HttpHelper.client().send(request, HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() >= 400) {
            throw new IOException(r.body());
        }

        var json = JacksonMapper.getDefault().readTree(r.body());
        var latest = json.get(0);
        var assets = latest.required("assets");
        for (var asset : assets) {
            var name = asset.required("name").asText();
            if (filter.test(name)) {
                var url = asset.required("browser_download_url").asText();
                return url;
            }
        }

        throw new IllegalStateException("Unable to find download url for " + repository);
    }
}
