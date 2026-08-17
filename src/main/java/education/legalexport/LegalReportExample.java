package education.legalexport;

import java.time.LocalDate;
import java.util.List;

public final class LegalReportExample {
    public static void main(String[] args) throws Exception {
        LocalDate asOf = LocalDate.of(2026, 8, 17);
        List<MatterIntake> matters = List.of(
                new MatterIntake("MAT-104", "Rivera Learning Trust", LocalDate.of(2026, 8, 1), true, LocalDate.of(2026, 8, 16)),
                new MatterIntake("MAT-105", "North Course Studio", LocalDate.of(2026, 8, 10), false, LocalDate.of(2026, 8, 22)),
                new MatterIntake("MAT-106", "Tutor House", LocalDate.of(2026, 8, 12), true, LocalDate.of(2026, 9, 5)));
        MatterReportService service = new MatterReportService(
                new InfraiStorageClient(ExportConfig.fromEnvironment()));
        MatterReportService.ExportReceipt receipt = service.export(matters, asOf);
        System.out.println("Rows: " + receipt.rowCount());
        System.out.println("Download: " + receipt.downloadUrl());
    }
}
