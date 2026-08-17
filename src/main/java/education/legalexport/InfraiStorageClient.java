package education.legalexport;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiStorageClient {
    // Canonical REST idiom: infrai.storage.object.presign
    private static final Pattern OK = Pattern.compile("\\\"ok\\\"\\s*:\\s*(true|false)");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ExportConfig config;

    public InfraiStorageClient(ExportConfig config) {
        this.config = config;
    }

    public void createBucket() throws IOException, InterruptedException {
        call("POST", "/v1/storage/bucket/create", "{\"name\":" + json(config.bucket()) + "}", null);
    }

    public String presignPut(String key, String requestId, String contentType, int maxBytes)
            throws IOException, InterruptedException {
        String body = "{\"op\":\"put\",\"expires_seconds\":" + config.linkSeconds()
                + ",\"content_type\":" + json(contentType) + ",\"max_bytes\":" + maxBytes
                + ",\"idempotency_key\":" + json(requestId) + "}";
        return field(call("POST", objectPath(key), body, null), "url");
    }

    public String presignDownload(String key, String requestId) throws IOException, InterruptedException {
        String body = "{\"op\":\"get\",\"expires_seconds\":" + config.linkSeconds()
                + ",\"response_disposition\":\"attachment\",\"idempotency_key\":" + json(requestId) + "}";
        return field(call("POST", objectPath(key), body, null), "url");
    }

    public void upload(String signedUrl, byte[] csv) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(signedUrl))
                .header("Content-Type", "text/csv; charset=utf-8")
                .method("PUT", HttpRequest.BodyPublishers.ofByteArray(csv)).build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("Signed upload returned HTTP " + response.statusCode());
        }
    }

    private String objectPath(String key) {
        return "/v1/storage/object/presign/" + segment(config.bucket()) + "/" + segment(key);
    }

    private String call(String method, String path, String body, String unused)
            throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(config.baseUrl() + path))
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            Matcher ok = OK.matcher(envelope);
            if (!ok.find()) throw new IOException("Response was not an Infrai envelope");
            if (!Boolean.parseBoolean(ok.group(1))) {
                if (response.statusCode() == 429 && attempt < 3) {
                    Thread.sleep(retryDelay(response, attempt));
                    continue;
                }
                throw new InfraiException(fieldOr(envelope, "code", "REQUEST_REJECTED"),
                        fieldOr(envelope, "message", "Request rejected"), response.statusCode());
            }
            return envelope;
        }
        throw new IOException("Retry budget exhausted");
    }

    private static long retryDelay(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(value -> {
                    try { return Long.parseLong(value) * 1000L; }
                    catch (NumberFormatException ignored) { return 500L * (1L << attempt); }
                }).orElse(500L * (1L << attempt));
    }

    private static String field(String json, String name) throws IOException {
        String value = fieldOr(json, name, null);
        if (value == null) throw new IOException("Envelope data omitted " + name);
        return value;
    }

    private static String fieldOr(String json, String name, String fallback) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(name)
                + "\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"").matcher(json);
        return matcher.find() ? matcher.group(1).replace("\\/", "/").replace("\\\"", "\"") : fallback;
    }

    private static String segment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
    }

    private static String json(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    public static final class InfraiException extends IOException {
        private final String code;
        private final int status;

        public InfraiException(String code, String message, int status) {
            super(message);
            this.code = code;
            this.status = status;
        }

        public String code() { return code; }
        public int status() { return status; }
    }
}
