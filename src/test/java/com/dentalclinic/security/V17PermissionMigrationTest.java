package com.dentalclinic.security;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class V17PermissionMigrationTest {
 @Test void migrationOnlyGrantsFourPlatformPermissions() throws Exception {
  String sql = Files.readString(Path.of("src/main/resources/db/migration/V17__clinic_management_permissions.sql"));
  for (String p : new String[]{"CLINIC_VIEW", "CLINIC_CREATE", "CLINIC_EDIT", "CLINIC_STATUS_MANAGE"}) assertTrue(sql.contains("'" + p + "'"));
  assertTrue(sql.contains("r.role_code = 'SUPER_ADMIN'")); assertTrue(sql.contains("r.clinic_id IS NULL"));
  assertTrue(sql.contains("r.is_system_role = TRUE")); assertTrue(sql.contains("inserted_count <> 4"));
  assertFalse(sql.contains("UPDATE ")); assertFalse(sql.contains("DELETE "));
 }
}

