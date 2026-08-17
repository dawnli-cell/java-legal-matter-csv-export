package education.legalexport;

import java.time.LocalDate;

public record MatterIntake(
        String matterId,
        String clientName,
        LocalDate intakeDate,
        boolean signedDocumentDelivered,
        LocalDate nextDeadline) {
}
