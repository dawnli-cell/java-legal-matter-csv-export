package education.legalexport;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class MatterCsv {
    private MatterCsv() {}

    public static String deadlineStatus(LocalDate deadline, LocalDate asOf) {
        long days = ChronoUnit.DAYS.between(asOf, deadline);
        if (days < 0) return "OVERDUE";
        if (days <= 7) return "DUE_SOON";
        return "ON_TRACK";
    }

    public static String render(List<MatterIntake> matters, LocalDate asOf) {
        StringBuilder csv = new StringBuilder(
                "matter_id,client_name,intake_date,signed_document_delivery,next_deadline,follow_up_status\n");
        for (MatterIntake matter : matters) {
            csv.append(quote(matter.matterId())).append(',')
                    .append(quote(matter.clientName())).append(',')
                    .append(matter.intakeDate()).append(',')
                    .append(matter.signedDocumentDelivered() ? "DELIVERED" : "PENDING").append(',')
                    .append(matter.nextDeadline()).append(',')
                    .append(deadlineStatus(matter.nextDeadline(), asOf)).append('\n');
        }
        return csv.toString();
    }

    private static String quote(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
