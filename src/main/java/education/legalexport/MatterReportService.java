package education.legalexport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

public final class MatterReportService {
    private final InfraiStorageClient storage;

    public MatterReportService(InfraiStorageClient storage) {
        this.storage = storage;
    }

    public ExportReceipt export(List<MatterIntake> matters, LocalDate asOf)
            throws IOException, InterruptedException {
        String key = "exports/matter-follow-up-" + asOf + ".csv";
        String requestId = "matter-report-" + asOf;
        byte[] csv = MatterCsv.render(matters, asOf).getBytes(StandardCharsets.UTF_8);
        storage.createBucket();
        String uploadUrl = storage.presignPut(key, requestId + "-put", "text/csv; charset=utf-8", csv.length);
        storage.upload(uploadUrl, csv);
        String downloadUrl = storage.presignDownload(key, requestId + "-get");
        return new ExportReceipt(key, downloadUrl, matters.size());
    }

    public record ExportReceipt(String objectKey, String downloadUrl, int rowCount) {}
}
