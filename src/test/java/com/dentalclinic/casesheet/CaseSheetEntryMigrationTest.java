package com.dentalclinic.casesheet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.flywaydb.core.Flyway;
import javax.sql.DataSource;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.jpa.show-sql=false"})
@EnabledIfEnvironmentVariable(named="CASE_SHEET_TEST_DATABASE",matches="true")
class CaseSheetEntryMigrationTest {
 @Autowired DataSource dataSource;
 @Test void legacyRowsAndCreationMetadataSurviveMigration() throws Exception {
 var admin=new JdbcTemplate(dataSource);assertTrue(admin.queryForObject("select current_database()",String.class).startsWith("dental_clinic_v18_verification_"));
 String schema="legacy_"+UUID.randomUUID().toString().replace("-","");admin.execute("create schema "+schema);
 // Flyway's initSql applies to each connection; use existing datasource credentials without printing them.
 var old=Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target("17").load();old.migrate();
 try(var connection=dataSource.getConnection()){
 connection.setSchema(schema);var isolated=new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection,true);var jdbc=new JdbcTemplate(isolated);
 UUID clinic=UUID.randomUUID(),user=UUID.randomUUID(),patient=UUID.randomUUID(),doctor=UUID.randomUUID(),appointment=UUID.randomUUID(),consultation=UUID.randomUUID(),parent=UUID.randomUUID();
 jdbc.update("insert into clinic(id,clinic_code,clinic_name) values (?,?,?)",clinic,clinic.toString(),"Test clinic");
 jdbc.update("insert into app_user(id,clinic_id,first_name) values (?,?,?)",user,clinic,"Test actor");
 jdbc.update("insert into patient(id,clinic_id,patient_number,first_name,gender,phone) values (?,?,?,'Patient','OTHER','123')",patient,clinic,patient.toString());
 jdbc.update("insert into doctor_profile(id,clinic_id,user_id) values (?,?,?)",doctor,clinic,user);
 jdbc.update("insert into appointment(id,clinic_id,appointment_number,patient_id,doctor_id,appointment_date,start_time,end_time) values (?,?,?,?,?,current_date,'09:00','09:30')",appointment,clinic,appointment.toString(),patient,doctor);
 jdbc.update("insert into consultation(id,clinic_id,appointment_id,patient_id,doctor_id) values (?,?,?,?,?)",consultation,clinic,appointment,patient,doctor);
 jdbc.update("insert into case_sheet(id,clinic_id,consultation_id,appointment_id,patient_id,doctor_id,case_sheet_number,chief_complaint) values (?,?,?,?,?,?,?,'Pain')",parent,clinic,consultation,appointment,patient,doctor,parent.toString());

 UUID diagnosis=UUID.randomUUID(),finding=UUID.randomUUID();
 jdbc.update("insert into case_sheet_diagnosis(id,clinic_id,case_sheet_id,diagnosis_name,created_by,created_at) values (?,?,?,'Legacy diagnosis',?,'2026-01-01 09:00:00')",diagnosis,clinic,parent,user);
 jdbc.update("insert into case_sheet_clinical_finding(id,clinic_id,case_sheet_id,finding_type,created_by,created_at) values (?,?,?,'Legacy finding',?,'2026-01-01 09:00:00')",finding,clinic,parent,user);
 Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).load().migrate();
 for(String table:java.util.List.of("case_sheet_diagnosis","case_sheet_clinical_finding")){
 var row=jdbc.queryForMap("select version,removed_at,updated_at,created_by,created_at from "+table+" where case_sheet_id=?",parent);assertEquals(0L,row.get("version"));assertNull(row.get("removed_at"));assertNull(row.get("updated_at"));assertEquals(user,row.get("created_by"));assertEquals("2026-01-01 09:00:00.0",row.get("created_at").toString());}
 assertEquals(0,jdbc.queryForObject("select count(*) from audit_log",Integer.class));
 assertEquals(3,jdbc.queryForObject("select count(*) from role_permission rp join permission p on p.id=rp.permission_id where p.permission_code='CASE_SHEET_HISTORY_VIEW'",Integer.class));
 connection.setSchema("public");
 }
 }
}
