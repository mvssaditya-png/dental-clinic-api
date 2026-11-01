package com.dentalclinic.treatment;
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
@EnabledIfEnvironmentVariable(named="TREATMENT_ITEM_TEST_DATABASE",matches="true")
class TreatmentPlanItemMigrationTest {
 @Autowired DataSource dataSource;
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(ints={18,19})
 void legacyRowsAndCreationMetadataSurviveMigration(int baseline) throws Exception {
 var admin=new JdbcTemplate(dataSource);assertTrue(admin.queryForObject("select current_database()",String.class).startsWith("dental_clinic_v19_verification_"));
 String schema="legacy_"+UUID.randomUUID().toString().replace("-","");admin.execute("create schema "+schema);
 // Flyway's initSql applies to each connection; use existing datasource credentials without printing them.
 var old=Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).target(Integer.toString(baseline)).load();old.migrate();
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


 UUID plan=UUID.randomUUID(),procedure=UUID.randomUUID(),item=UUID.randomUUID();
 jdbc.update("insert into procedure_master(id,clinic_id,procedure_code,procedure_name) values (?,?,?,'Legacy procedure')",procedure,clinic,procedure.toString());
 jdbc.update("insert into treatment_plan(id,clinic_id,patient_id,doctor_id,case_sheet_id,treatment_plan_number) values (?,?,?,?,?,?)",plan,clinic,patient,doctor,parent,plan.toString());
 jdbc.update("insert into treatment_plan_item(id,clinic_id,treatment_plan_id,procedure_id,procedure_code_snapshot,procedure_name_snapshot,quantity,unit_price,gross_amount,final_amount,created_by,created_at) values (?,?,?,?,'LEGACY','Legacy procedure',2,12.35,24.70,24.70,?,'2026-01-01 09:00:00')",item,clinic,plan,procedure,user);
 jdbc.update("update treatment_plan set title='Legacy plan',subtotal=24.70,discount_type='FIXED',discount_value=2.00,discount_amount=2.00,final_amount=22.70,notes='Preserve clinical notes',estimated_total_visits=3 where id=?",plan);
 var legacyPlan=jdbc.queryForMap("select * from treatment_plan where id=?",plan);
 Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).load().migrate();
 var migratedPlan=jdbc.queryForMap("select * from treatment_plan where id=?",plan);
 migratedPlan.remove("version");migratedPlan.remove("removed_at");migratedPlan.remove("removed_by");
 assertEquals(legacyPlan,migratedPlan);
 var row=jdbc.queryForMap("select version,removed_at,removed_by,quantity,final_amount,created_at from treatment_plan_item where id=?",item);
 assertEquals(0L,row.get("version"));assertNull(row.get("removed_at"));assertNull(row.get("removed_by"));assertEquals(2,row.get("quantity"));assertEquals(new java.math.BigDecimal("24.70"),row.get("final_amount"));assertEquals("2026-01-01 09:00:00.0",row.get("created_at").toString());
 assertEquals(0,jdbc.queryForObject("select count(*) from treatment_plan_item_audit",Integer.class));
 var planRow=jdbc.queryForMap("select version,removed_at,removed_by,treatment_plan_number,status from treatment_plan where id=?",plan);
 assertEquals(0L,planRow.get("version"));assertNull(planRow.get("removed_at"));assertNull(planRow.get("removed_by"));assertEquals(plan.toString(),planRow.get("treatment_plan_number"));assertEquals("DRAFT",planRow.get("status"));
 assertEquals(0,jdbc.queryForObject("select count(*) from treatment_plan_audit",Integer.class));
 assertEquals(java.util.Set.of("SUPER_ADMIN","ADMIN","DOCTOR"),new java.util.HashSet<>(jdbc.queryForList("select r.role_code from role_permission rp join permission p on p.id=rp.permission_id join role r on r.id=rp.role_id where p.permission_code='TREATMENT_PLAN_HISTORY_VIEW'",String.class)));
 assertEquals(3,jdbc.queryForObject("select count(*) from role_permission rp join permission p on p.id=rp.permission_id where p.permission_code='CASE_SHEET_HISTORY_VIEW'",Integer.class));
 connection.setSchema("public");
 }
 }
}
