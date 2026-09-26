package com.dentalclinic.security;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class V16PermissionMigrationTest {
    @Test void migrationContainsExactlyTheApprovedNineteenGrants() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V16__mvp_authorization_permissions.sql"));
        Set<String> all = Set.of("CONSULTATION_VIEW", "CONSULTATION_MANAGE", "PROCEDURE_VIEW", "PROCEDURE_MANAGE", "INVOICE_ISSUE", "INVOICE_CANCEL");
        Map<String, Set<String>> expected = Map.of(
                "SUPER_ADMIN", all, "ADMIN", all,
                "DOCTOR", Set.of("CONSULTATION_VIEW", "CONSULTATION_MANAGE", "PROCEDURE_VIEW"),
                "RECEPTIONIST", Set.of("CONSULTATION_VIEW", "PROCEDURE_VIEW", "INVOICE_ISSUE"),
                "ATTENDER", Set.of("CONSULTATION_VIEW"));
        Map<String, Set<String>> actual = new HashMap<>();
        var matcher = Pattern.compile("\\('([A-Z_]+)', '([A-Z_]+)'\\)").matcher(sql);
        int count = 0;
        while (matcher.find()) { actual.computeIfAbsent(matcher.group(1), k -> new HashSet<>()).add(matcher.group(2)); count++; }
        assertEquals(expected, actual); assertEquals(19, count);
        assertTrue(sql.contains("r.clinic_id IS NULL"));
        assertFalse(sql.contains("UPDATE ")); assertFalse(sql.contains("DELETE "));
    }
}
