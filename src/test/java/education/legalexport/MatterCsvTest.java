package education.legalexport;

import java.time.LocalDate;
import java.util.List;

public final class MatterCsvTest {
    public static void main(String[] args) {
        LocalDate asOf = LocalDate.of(2026, 8, 17);
        assertEquals("OVERDUE", MatterCsv.deadlineStatus(LocalDate.of(2026, 8, 16), asOf));
        assertEquals("DUE_SOON", MatterCsv.deadlineStatus(LocalDate.of(2026, 8, 22), asOf));
        assertEquals("ON_TRACK", MatterCsv.deadlineStatus(LocalDate.of(2026, 9, 5), asOf));
        String csv = MatterCsv.render(List.of(new MatterIntake(
                "MAT-201", "Academy, Inc.", LocalDate.of(2026, 8, 10), true, LocalDate.of(2026, 8, 22))), asOf);
        if (!csv.contains("\"Academy, Inc.\"") || !csv.contains("DELIVERED,2026-08-22,DUE_SOON")) {
            throw new AssertionError("CSV did not preserve the legal report decision: " + csv);
        }
        System.out.println("MatterCsvTest passed");
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) throw new AssertionError("expected " + expected + " but got " + actual);
    }
}
