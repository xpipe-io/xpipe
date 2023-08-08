package io.xpipe.app.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class HttpHelper {

    public static Path downloadFile(String urlS) throws Exception {
        var url = URI.create(urlS).toURL();
        var bytes = HttpHelper.executeGet(url, aFloat -> {});
        var downloadFile = Files.createTempFile(null, null);
        Files.write(downloadFile, bytes);
        return downloadFile;
    }

    public static byte[] executeGet(URL targetURL, Consumer<Float> progress) throws Exception {
        HttpURLConnection connection = null;

        try {
            // Create connection
            connection = (HttpURLConnection) targetURL.openConnection();
            connection.setRequestMethod("GET");
            connection.addRequestProperty("User-Agent", "https://github.com/xpipe-io/xpipe");
            connection.addRequestProperty("Accept", "*/*");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new IOException("Got http " + responseCode + " for " + targetURL);
            }

            InputStream is = connection.getInputStream();
            int size = Integer.parseInt(connection.getHeaderField("Content-Length"));

            byte[] line;
            int bytes = 0;
            ByteBuffer b = ByteBuffer.allocate(size);
            while ((line = is.readNBytes(500000)).length > 0) {
                b.put(line);
                bytes += line.length;
                if (progress != null) {
                    progress.accept((float) bytes / (float) size);
                }
            }
            return b.array();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
