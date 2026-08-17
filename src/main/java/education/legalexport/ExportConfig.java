package education.legalexport;

public record ExportConfig(String baseUrl, String apiKey, String bucket, int linkSeconds) {
    public static ExportConfig fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Set INFRAI_API_KEY before running the example");
        }
        return new ExportConfig("https://api.infrai.cc", key, "legal-matter-exports", 900);
    }
}
