package com.dentalclinic.consultation;

import com.dentalclinic.consultation.controller.PatientClinicalVisitsController;
import com.dentalclinic.consultation.service.ClinicalVisitService;
import com.dentalclinic.clinic.entity.Clinic;
import com.dentalclinic.user.entity.AppUser;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.generate_statistics=true"})
@EnabledIfEnvironmentVariable(named="TREATMENT_ITEM_TEST_DATABASE",matches="true")
@Transactional
class ClinicalVisitDatabaseTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PatientClinicalVisitsController controller;
    @Autowired ClinicalVisitService service;
    @Autowired EntityManagerFactory emf;
    UUID clinic, user, patient, doctor;
    AppUser actor;

    @BeforeEach void fixture() {
        assertTrue(jdbc.queryForObject("select current_database()",String.class)
                .startsWith("dental_clinic_v19_verification_"));
        clinic=UUID.randomUUID(); user=UUID.randomUUID(); patient=UUID.randomUUID(); doctor=UUID.randomUUID();
        jdbc.update("insert into clinic(id,clinic_code,clinic_name) values (?,?,?)",clinic,clinic.toString(),"Visits test");
        jdbc.update("insert into app_user(id,clinic_id,first_name,last_name) values (?,?,?,?)",user,clinic,"Test","Doctor");
        jdbc.update("insert into patient(id,clinic_id,patient_number,first_name,gender,phone) values (?,?,?,'Patient','OTHER','123')",patient,clinic,patient.toString());
        jdbc.update("insert into doctor_profile(id,clinic_id,user_id) values (?,?,?)",doctor,clinic,user);
        actor=AppUser.builder().id(user).clinic(Clinic.builder().id(clinic).active(true).build()).build();
        login("CONSULTATION_VIEW","CASE_SHEET_VIEW");
    }
    void login(String... permissions) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor,null,
                Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    UUID visit(String status, String sheetStatus, String start) {
        UUID appointment=UUID.randomUUID(), consultation=UUID.randomUUID();
        jdbc.update("insert into appointment(id,clinic_id,appointment_number,patient_id,doctor_id,appointment_date,start_time,end_time) values (?,?,?,?,?,current_date,'09:00','09:30')",appointment,clinic,appointment.toString(),patient,doctor);
        jdbc.update("insert into consultation(id,clinic_id,appointment_id,patient_id,doctor_id,status,started_at) values (?,?,?,?,?,?,cast(? as timestamp))",consultation,clinic,appointment,patient,doctor,status,start);
        if(sheetStatus!=null) {
            UUID sheet=UUID.randomUUID();
            jdbc.update("insert into case_sheet(id,clinic_id,consultation_id,appointment_id,patient_id,doctor_id,case_sheet_number,status,chief_complaint) values (?,?,?,?,?,?,?,?,?)",sheet,clinic,consultation,appointment,patient,doctor,sheet.toString(),sheetStatus,"Pain");
        }
        return consultation;
    }
    @Test void includesAllStatusesAndOptionalSheetsWithAuthoritativeFields() {
        visit("STARTED","DRAFT","2026-01-01 09:00");
        visit("IN_PROGRESS",null,"2026-01-01 10:00");
        visit("COMPLETED","FINALIZED","2026-01-02 09:00");
        visit("CANCELLED","AMENDED","2026-01-03 09:00");
        var result=controller.visits(patient,null,0,20,null);
        assertEquals(4,result.totalElements());
        assertEquals(List.of("CANCELLED","COMPLETED","IN_PROGRESS","STARTED"),result.content().stream().map(v->v.consultationStatus().name()).toList());
        var draft=result.content().get(3);
        assertEquals("Pain",draft.chiefComplaint()); assertEquals(1,draft.visitNumber());
        assertEquals("Test Doctor",draft.doctorName()); assertEquals(doctor,draft.doctorId());
        assertNotNull(draft.appointmentNumber()); assertNotNull(draft.appointmentDate());
        assertEquals("09:00",draft.appointmentStartTime().toString());
        assertNull(result.content().get(2).caseSheetId()); assertNull(result.content().get(2).caseSheetStatus());
        assertNull(result.content().get(2).visitNumber()); assertNull(result.content().get(2).chiefComplaint());
        assertNull(draft.endedAt());
    }
    @Test void appointmentStatusesDoNotFilterClinicalRecordsAndAppointmentsAloneAreExcluded() {
        visit("STARTED",null,"2026-01-01 09:00");
        jdbc.update("update appointment set status='NO_SHOW' where clinic_id=?",clinic);
        assertEquals(1,controller.visits(patient,null,0,20,null).totalElements());
        jdbc.update("update appointment set status='CANCELLED' where clinic_id=?",clinic);
        assertEquals(1,controller.visits(patient,null,0,20,null).totalElements());
        UUID id=UUID.randomUUID();
        jdbc.update("insert into appointment(id,clinic_id,appointment_number,patient_id,doctor_id,appointment_date,start_time,end_time) values (?,?,?,?,?,current_date,'10:00','10:30')",id,clinic,id.toString(),patient,doctor);
        assertEquals(1,controller.visits(patient,null,0,20,null).totalElements());
    }
    @Test void sameDayTiesPaginationAndExclusionAreStable() {
        var ids=new ArrayList<UUID>();
        for(int i=0;i<5;i++) ids.add(visit("STARTED",null,"2026-01-01 09:00"));
        ids.sort(Comparator.comparing(UUID::toString).reversed());
        var first=controller.visits(patient,null,0,2,null);
        assertEquals(5,first.totalElements()); assertEquals(3,first.totalPages());
        assertEquals(ids.subList(0,2),first.content().stream().map(v->v.consultationId()).toList());
        assertEquals(ids.subList(2,4),controller.visits(patient,null,1,2,null).content().stream().map(v->v.consultationId()).toList());
        var excluded=controller.visits(patient,null,0,2,ids.get(0));
        assertEquals(4,excluded.totalElements()); assertEquals(ids.subList(1,3),excluded.content().stream().map(v->v.consultationId()).toList());
    }
    @Test void nullLegacyStartsSortLastWithoutInventedValues() {
        // Disposable database only; test transaction rolls back this temporary schema relaxation.
        jdbc.execute("alter table consultation alter column started_at drop not null");
        UUID nullVisit=visit("STARTED",null,null);
        UUID dated=visit("STARTED",null,"2026-01-01 09:00");
        var rows=controller.visits(patient,null,0,20,null).content();
        assertEquals(dated,rows.get(0).consultationId()); assertEquals(nullVisit,rows.get(1).consultationId());
        assertNull(rows.get(1).startedAt());
    }
    @Test void bothAuthoritiesAreRequired() {
        for(String[] granted:List.of(new String[]{},new String[]{"CONSULTATION_VIEW"},new String[]{"CASE_SHEET_VIEW"})) {
            login(granted); assertThrows(AccessDeniedException.class,()->controller.visits(patient,null,0,20,null));
        }
        login("CONSULTATION_VIEW","CASE_SHEET_VIEW"); assertEquals(0,controller.visits(patient,null,0,20,null).totalElements());
    }
    @Test void missingAndOtherClinicPatientsAre404EvenWithRequestedClinicOverride() {
        assertEquals(404,assertThrows(ResponseStatusException.class,()->controller.visits(UUID.randomUUID(),null,0,20,null)).getStatusCode().value());
        UUID other=UUID.randomUUID();
        jdbc.update("insert into clinic(id,clinic_code,clinic_name) values (?,?,?)",other,other.toString(),"Other");
        actor=AppUser.builder().id(user).clinic(Clinic.builder().id(other).active(true).build()).build();
        login("CONSULTATION_VIEW","CASE_SHEET_VIEW");
        assertEquals(404,assertThrows(ResponseStatusException.class,()->controller.visits(patient,clinic,0,20,null)).getStatusCode().value());
    }
    @Test void otherPatientConsultationsDoNotLeakIntoRowsOrCounts() {
        visit("STARTED",null,"2026-01-01 09:00");
        UUID original=patient; patient=UUID.randomUUID();
        jdbc.update("insert into patient(id,clinic_id,patient_number,first_name,gender,phone) values (?,?,?,'Other','OTHER','123')",patient,clinic,patient.toString());
        visit("COMPLETED",null,"2026-01-02 09:00");
        assertEquals(1,controller.visits(original,null,0,1,null).totalElements());
    }
    @Test void platformRequiresExplicitActiveClinic() {
        actor=AppUser.builder().id(user).build(); login("CONSULTATION_VIEW","CASE_SHEET_VIEW");
        assertThrows(IllegalArgumentException.class,()->controller.visits(patient,null,0,20,null));
        assertEquals(0,controller.visits(patient,clinic,0,20,null).totalElements());
    }
    @Test void paginationBoundsAndEmptyPages() {
        assertThrows(IllegalArgumentException.class,()->controller.visits(patient,null,-1,20,null));
        assertThrows(IllegalArgumentException.class,()->controller.visits(patient,null,0,0,null));
        assertThrows(IllegalArgumentException.class,()->controller.visits(patient,null,Integer.MAX_VALUE,100,null));
        var empty=controller.visits(patient,null,0,1000,null);
        assertEquals(100,empty.size()); assertEquals(0,empty.totalPages()); assertTrue(empty.content().isEmpty());
        assertTrue(controller.visits(patient,null,5,20,null).content().isEmpty());
    }
    @Test void projectionHasBoundedQueriesAndDoesNotWriteOrLoadCaseSheetEntities() {
        for(int i=0;i<5;i++) visit("STARTED","DRAFT","2026-01-01 09:00");
        var stats=emf.unwrap(SessionFactory.class).getStatistics(); stats.clear();
        var result=controller.visits(patient,null,0,2,null);
        assertEquals(5,result.totalElements());
        assertTrue(stats.getPrepareStatementCount()<=4,"Queries must not grow per visit: "+stats.getPrepareStatementCount());
        assertEquals(0,stats.getEntityInsertCount()); assertEquals(0,stats.getEntityUpdateCount()); assertEquals(0,stats.getEntityDeleteCount());
        assertEquals(0,stats.getEntityStatistics("com.dentalclinic.casesheet.entity.CaseSheet").getLoadCount());
    }
    @Autowired com.dentalclinic.common.exception.GlobalExceptionHandler errors;
    @Test void httpDefaultsSerializationErrorsAndAuthentication() throws Exception {
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(errors).build();
        String url="/api/patients/"+patient+"/clinical-visits";
        UUID id=visit("STARTED",null,"2026-01-01 09:00");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("number").value(0))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("size").value(20))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("content[0].consultationId").value(id.toString()));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url).param("excludeConsultationId",id.toString()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("totalElements").value(0));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/patients/"+UUID.randomUUID()+"/clinical-visits"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url).param("page","-1"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        login("CONSULTATION_VIEW");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        SecurityContextHolder.clearContext();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
    }}

